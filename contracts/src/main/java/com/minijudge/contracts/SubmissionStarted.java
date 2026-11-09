package com.minijudge.contracts;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record SubmissionStarted(
        UUID eventId,
        UUID submissionId,
        Instant occurredAt,
        int schemaVersion) {

    public SubmissionStarted {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(submissionId, "submissionId");
        Objects.requireNonNull(occurredAt, "occurredAt");
        if (schemaVersion != 1) {
            throw new IllegalArgumentException("schemaVersion must be 1");
        }
    }
}
