package com.minijudge.contracts;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record SubmissionRequested(
        UUID eventId,
        UUID submissionId,
        UUID problemId,
        Language language,
        String sourceCode,
        Instant occurredAt,
        int schemaVersion) {

    public SubmissionRequested {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(submissionId, "submissionId");
        Objects.requireNonNull(problemId, "problemId");
        Objects.requireNonNull(language, "language");
        Objects.requireNonNull(sourceCode, "sourceCode");
        Objects.requireNonNull(occurredAt, "occurredAt");
        if (schemaVersion != 1) {
            throw new IllegalArgumentException("schemaVersion must be 1");
        }
    }
}
