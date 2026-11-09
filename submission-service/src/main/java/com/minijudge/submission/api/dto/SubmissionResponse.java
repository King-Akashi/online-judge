package com.minijudge.submission.api.dto;

import com.minijudge.contracts.Language;
import com.minijudge.contracts.SubmissionStatus;
import com.minijudge.contracts.Verdict;

import java.time.Instant;
import java.util.UUID;

public record SubmissionResponse(
        UUID id,
        UUID problemId,
        Language language,
        SubmissionStatus status,
        Verdict verdict,
        Long executionTimeMs,
        Long memoryUsedBytes,
        Integer passedTests,
        Integer totalTests,
        Instant createdAt,
        Instant updatedAt) {
}
