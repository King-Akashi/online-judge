package com.minijudge.judge.executor;

public record CodeExecutionResult(
        int exitCode,
        String standardOutput,
        String standardError,
        long executionTimeMs,
        long memoryUsedBytes,
        boolean timedOut,
        boolean outputLimitExceeded) {

    public boolean succeeded() {
        return exitCode == 0 && !timedOut && !outputLimitExceeded;
    }
}
