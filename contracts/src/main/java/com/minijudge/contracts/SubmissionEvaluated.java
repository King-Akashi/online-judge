package com.minijudge.contracts;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record SubmissionEvaluated(
        UUID eventId,
        UUID submissionId,
        Verdict verdict,
        long executionTimeMs,
        long memoryUsedBytes,
        int passedTests,
        int totalTests,
        Instant occurredAt,
        int schemaVersion) {

    public SubmissionEvaluated {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(submissionId, "submissionId");
        Objects.requireNonNull(verdict, "verdict");
        Objects.requireNonNull(occurredAt, "occurredAt");
        if (executionTimeMs < 0 || memoryUsedBytes < 0 || passedTests < 0
                || totalTests < 0 || passedTests > totalTests) {
            throw new IllegalArgumentException("Evaluation metrics are invalid");
        }
        if (schemaVersion != 1) {
            throw new IllegalArgumentException("schemaVersion must be 1");
        }
    }
}
