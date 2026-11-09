package com.minijudge.judge.executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DockerCodeExecutor implements CodeExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DockerCodeExecutor.class);
    private static final Pattern MEMORY_USAGE = Pattern.compile(
            "(?:^|\\R)MINIJUDGE_MAX_RSS_KB=(\\d+)\\s*$");
    private static final int STDERR_LIMIT_BYTES = 128 * 1024;
    private final String dockerCommand;
    private final String image;
    private final long compileTimeoutMs;
    private final int maxOutputBytes;
    private final int pidsLimit;
    private final String cpus;

    public DockerCodeExecutor(
            @Value("${minijudge.execution.docker-command:docker}") String dockerCommand,
            @Value("${minijudge.execution.image:minijudge/java-runner:21}") String image,
            @Value("${minijudge.execution.compile-timeout-ms:15000}") long compileTimeoutMs,
            @Value("${minijudge.execution.max-output-bytes:1048576}") int maxOutputBytes,
            @Value("${minijudge.execution.pids-limit:64}") int pidsLimit,
            @Value("${minijudge.execution.cpus:1}") String cpus) {
        this.dockerCommand = dockerCommand;
        this.image = image;
        this.compileTimeoutMs = compileTimeoutMs;
        this.maxOutputBytes = maxOutputBytes;
        this.pidsLimit = pidsLimit;
        this.cpus = cpus;
    }

    @Override
    public CodeExecutionSession createSession(String sourceCode, int memoryLimitMb) throws IOException {
        Path workspace = Files.createTempDirectory("minijudge-");
        String volumeName = "minijudge-" + UUID.randomUUID().toString().replace("-", "");
        String preparationContainer = "minijudge-prepare-"
                + UUID.randomUUID().toString().replace("-", "");
        try {
            Path sourceFile = workspace.resolve("Main.java");
            Files.writeString(sourceFile, sourceCode, StandardCharsets.UTF_8);
            runUtility(List.of(dockerCommand, "volume", "create", volumeName));
            runUtility(List.of(
                    dockerCommand,
                    "create",
                    "--name",
                    preparationContainer,
                    "--network",
                    "none",
                    "--read-only",
                    "--cap-drop",
                    "ALL",
                    "--security-opt",
                    "no-new-privileges:true",
                    "--user",
                    "65534:65534",
                    "--mount",
                    "type=volume,source=" + volumeName + ",target=/workspace",
                    image,
                    "/bin/true"));
            runUtility(List.of(
                    dockerCommand,
                    "cp",
                    sourceFile.toAbsolutePath().toString(),
                    preparationContainer + ":/workspace/Main.java"));
            return new DockerExecutionSession(workspace, volumeName, memoryLimitMb);
        } catch (IOException | RuntimeException exception) {
            removeContainer(preparationContainer);
            removeVolume(volumeName);
            deleteWorkspace(workspace);
            throw exception;
        } finally {
            removeContainer(preparationContainer);
        }
    }

    private String runUtility(List<String> command) throws IOException {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        try {
            if (!process.waitFor(15, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IOException("Docker utility timed out: " + command.get(1));
            }
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (process.exitValue() != 0) {
                throw new IllegalStateException("Docker utility failed: " + output.strip());
            }
            return output.strip();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Docker utility was interrupted: " + command.get(1), exception);
        }
    }

    private void deleteWorkspace(Path workspace) {
        try (var paths = Files.walk(workspace)) {
            for (Path path : paths.sorted((left, right) -> right.compareTo(left)).toList()) {
                Files.deleteIfExists(path);
            }
        } catch (IOException exception) {
            LOGGER.error("Could not remove local execution workspace {}", workspace, exception);
        }
    }

    private void removeVolume(String volumeName) {
        try {
            runUtility(List.of(dockerCommand, "volume", "rm", volumeName));
        } catch (IOException | IllegalStateException exception) {
            LOGGER.error("Could not remove execution volume {}", volumeName, exception);
        }
    }

    private void removeContainer(String containerName) {
        try {
            Process cleanup = new ProcessBuilder(dockerCommand, "rm", "-f", containerName)
                    .redirectErrorStream(true)
                    .start();
            if (!cleanup.waitFor(10, TimeUnit.SECONDS)) {
                cleanup.destroyForcibly();
                LOGGER.error("Timed out removing execution container {}", containerName);
                return;
            }
            if (cleanup.exitValue() != 0) {
                String message = new String(
                        cleanup.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                if (!message.contains("No such container")) {
                    LOGGER.error("Could not remove execution container {}: {}",
                            containerName, message.strip());
                }
            }
        } catch (IOException exception) {
            LOGGER.error("Could not remove execution container {}", containerName, exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            LOGGER.error("Interrupted while removing execution container {}", containerName,
                    exception);
        }
    }

    private final class DockerExecutionSession implements CodeExecutionSession {

        private final Path workspace;
        private final String volumeName;
        private final int memoryLimitMb;

        private DockerExecutionSession(Path workspace, String volumeName, int memoryLimitMb) {
            this.workspace = workspace;
            this.volumeName = volumeName;
            this.memoryLimitMb = memoryLimitMb;
        }

        @Override
        public CodeExecutionResult compile() {
            return runContainer(
                    false,
                    "",
                    Duration.ofMillis(compileTimeoutMs),
                    "/usr/bin/time",
                    "-f",
                    "MINIJUDGE_MAX_RSS_KB=%M",
                    "javac",
                    "-proc:none",
                    "-encoding",
                    "UTF-8",
                    "-d",
                    "/workspace/classes",
                    "/workspace/Main.java");
        }

        @Override
        public CodeExecutionResult execute(String input, int timeLimitMs) {
            int maxHeapMb = Math.max(32, memoryLimitMb - 96);
            return runContainer(
                    true,
                    input,
                    Duration.ofMillis(Math.max(1000L, timeLimitMs) + 500L),
                    "/usr/bin/time",
                    "-f",
                    "MINIJUDGE_MAX_RSS_KB=%M",
                    "java",
                    "-Xmx" + maxHeapMb + "m",
                    "-XX:MaxMetaspaceSize=48m",
                    "-cp",
                    "/workspace/classes",
                    "Main");
        }

        @Override
        public void close() throws IOException {
            try {
                deleteWorkspace(workspace);
            } finally {
                removeVolume(volumeName);
            }
        }

        private CodeExecutionResult runContainer(
                boolean readOnlyWorkspace, String input, Duration timeout, String... command) {
            String containerName = "minijudge-" + UUID.randomUUID().toString().replace("-", "");
            List<String> arguments = new ArrayList<>();
            arguments.add(dockerCommand);
            arguments.add("run");
            arguments.add("--name");
            arguments.add(containerName);
            arguments.add("--network");
            arguments.add("none");
            arguments.add("--memory");
            arguments.add(memoryLimitMb + "m");
            arguments.add("--memory-swap");
            arguments.add(memoryLimitMb + "m");
            arguments.add("--cpus");
            arguments.add(cpus);
            arguments.add("--pids-limit");
            arguments.add(Integer.toString(pidsLimit));
            arguments.add("--read-only");
            arguments.add("--tmpfs");
            arguments.add("/tmp:rw,nosuid,nodev,size=64m,uid=65534,gid=65534,mode=700");
            arguments.add("--cap-drop");
            arguments.add("ALL");
            arguments.add("--security-opt");
            arguments.add("no-new-privileges:true");
            arguments.add("--user");
            arguments.add("65534:65534");
            arguments.add("--mount");
            arguments.add("type=volume,source=" + volumeName
                    + ",target=/workspace" + (readOnlyWorkspace ? ",readonly" : ""));
            arguments.add("--workdir");
            arguments.add("/workspace");
            if (input != null) {
                arguments.add("-i");
            }
            arguments.add(image);
            arguments.addAll(List.of(command));

            long startNanos = System.nanoTime();
            Process process = null;
            try {
                process = new ProcessBuilder(arguments).start();
                return collect(process, containerName, input, timeout, startNanos);
            } catch (IOException exception) {
                throw new IllegalStateException("Could not start Docker code execution", exception);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Docker code execution was interrupted", exception);
            } finally {
                if (process != null && process.isAlive()) {
                    process.destroyForcibly();
                }
                removeContainer(containerName);
            }
        }

        private CodeExecutionResult collect(
                Process process,
                String containerName,
                String input,
                Duration timeout,
                long startNanos) throws IOException, InterruptedException {
            try (ExecutorService ioExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
                Future<byte[]> stdoutFuture = ioExecutor.submit(
                        () -> readBounded(process.getInputStream(), maxOutputBytes));
                Future<byte[]> stderrFuture = ioExecutor.submit(
                        () -> readBounded(process.getErrorStream(), STDERR_LIMIT_BYTES));
                if (input != null) {
                    ioExecutor.submit(() -> {
                        try (var stream = process.getOutputStream()) {
                            stream.write(input.getBytes(StandardCharsets.UTF_8));
                        }
                        return null;
                    });
                } else {
                    process.getOutputStream().close();
                }

                if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                    removeContainer(containerName);
                    process.destroyForcibly();
                    process.waitFor(5, TimeUnit.SECONDS);
                    return new CodeExecutionResult(
                            -1, "", "", elapsedMillis(startNanos), 0, true, false);
                }

                byte[] stdout = getOutput(stdoutFuture, containerName, process);
                byte[] stderr = getOutput(stderrFuture, containerName, process);
                String standardOutput = new String(stdout, StandardCharsets.UTF_8);
                String standardError = new String(stderr, StandardCharsets.UTF_8);
                long memoryBytes = extractMemoryBytes(standardError);
                standardError = MEMORY_USAGE.matcher(standardError).replaceFirst("").stripTrailing();
                return new CodeExecutionResult(
                        process.exitValue(),
                        standardOutput,
                        standardError,
                        elapsedMillis(startNanos),
                        memoryBytes,
                        false,
                        false);
            } catch (OutputLimitExceededException exception) {
                removeContainer(containerName);
                process.destroyForcibly();
                return new CodeExecutionResult(
                        -1, "", exception.getMessage(), elapsedMillis(startNanos), 0, false, true);
            } catch (ExecutionException exception) {
                Throwable cause = exception.getCause();
                if (cause instanceof OutputLimitExceededException outputLimit) {
                    removeContainer(containerName);
                    process.destroyForcibly();
                    return new CodeExecutionResult(
                            -1, "", outputLimit.getMessage(), elapsedMillis(startNanos), 0,
                            false, true);
                }
                throw new IllegalStateException("Could not collect Docker execution output", cause);
            } catch (TimeoutException exception) {
                removeContainer(containerName);
                process.destroyForcibly();
                throw new IllegalStateException("Timed out collecting Docker execution output",
                        exception);
            }
        }

        private byte[] getOutput(Future<byte[]> future, String containerName, Process process)
                throws InterruptedException, ExecutionException, TimeoutException {
            try {
                return future.get(5, TimeUnit.SECONDS);
            } catch (ExecutionException exception) {
                if (exception.getCause() instanceof OutputLimitExceededException) {
                    removeContainer(containerName);
                    process.destroyForcibly();
                }
                throw exception;
            }
        }

        private byte[] readBounded(InputStream stream, int limit) throws IOException {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int total = 0;
            int read;
            while ((read = stream.read(buffer)) != -1) {
                total += read;
                if (total > limit) {
                    throw new OutputLimitExceededException("Program output exceeded the limit");
                }
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        }

        private long extractMemoryBytes(String standardError) {
            Matcher matcher = MEMORY_USAGE.matcher(standardError);
            if (!matcher.find()) {
                throw new IllegalStateException("Runner did not report process memory usage");
            }
            return Long.parseLong(matcher.group(1)) * 1024;
        }

        private long elapsedMillis(long startNanos) {
            return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
        }

    }

    private static final class OutputLimitExceededException extends RuntimeException {

        private OutputLimitExceededException(String message) {
            super(message);
        }
    }
}
