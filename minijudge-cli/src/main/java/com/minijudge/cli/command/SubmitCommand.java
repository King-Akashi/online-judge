package com.minijudge.cli.command;

import com.minijudge.cli.CliApiClient;
import com.minijudge.cli.CliApiException;
import com.minijudge.cli.dto.SubmissionResponse;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.Callable;

@Command(name = "submit", mixinStandardHelpOptions = true,
        description = "Submit a Java source file.")
public class SubmitCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Problem UUID")
    private UUID problemId;

    @Option(names = "--file", required = true, description = "Path to a Java source file")
    private Path sourceFile;

    @Option(names = "--no-wait", description = "Return after the submission is queued")
    private boolean noWait;

    @Option(names = "--poll-interval-ms", defaultValue = "1000",
            description = "Status polling interval in milliseconds")
    private long pollIntervalMs;

    @Option(names = "--timeout-seconds", defaultValue = "300",
            description = "Maximum time to wait for a final verdict")
    private long timeoutSeconds;

    @Override
    public Integer call() throws InterruptedException {
        if (pollIntervalMs <= 0 || timeoutSeconds <= 0) {
            throw new CliApiException("Polling interval and timeout must be positive");
        }
        String sourceCode;
        try {
            sourceCode = Files.readString(sourceFile, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new CliApiException("Could not read source file: " + sourceFile, exception);
        }

        CliApiClient client = new CliApiClient();
        SubmissionResponse submission = client.createSubmission(problemId, sourceCode);
        System.out.printf("Submission created: #%s%n", submission.id());
        System.out.printf("Status: %s%n", submission.status());
        if (noWait) {
            System.out.printf("%nTrack using:%nminijudge status %s%n", submission.id());
            return 0;
        }

        long deadline = System.nanoTime() + Duration.ofSeconds(timeoutSeconds).toNanos();
        while (System.nanoTime() < deadline) {
            Thread.sleep(pollIntervalMs);
            submission = client.getSubmission(submission.id());
            if ("COMPLETED".equals(submission.status())) {
                printResult(submission);
                return 0;
            }
            if ("FAILED".equals(submission.status())) {
                System.err.println("Submission failed because of an infrastructure error.");
                return 1;
            }
        }
        throw new CliApiException("Timed out waiting for a verdict; check with minijudge status "
                + submission.id());
    }

    private void printResult(SubmissionResponse submission) {
        System.out.printf("Verdict: %s%n", submission.verdict());
        System.out.printf("Tests: %d/%d passed%n",
                submission.passedTests(), submission.totalTests());
        System.out.printf("Execution time: %d ms%n", submission.executionTimeMs());
        if (submission.memoryUsedBytes() != null) {
            System.out.printf("Memory used: %d bytes%n", submission.memoryUsedBytes());
        }
    }
}
