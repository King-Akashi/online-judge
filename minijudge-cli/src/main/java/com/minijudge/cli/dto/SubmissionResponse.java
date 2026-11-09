package com.minijudge.cli.dto;

import java.time.Instant;
import java.util.UUID;

public record SubmissionResponse(
        UUID id,
        UUID problemId,
        String language,
        String status,
        String verdict,
        Long executionTimeMs,
        Long memoryUsedBytes,
        Integer passedTests,
        Integer totalTests,
        Instant createdAt,
        Instant updatedAt) {
}
