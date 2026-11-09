package com.minijudge.problem.api.dto;

import com.minijudge.contracts.Language;
import com.minijudge.problem.model.ProblemDifficulty;

import java.util.UUID;

public record ProblemSummary(
        UUID id,
        String title,
        ProblemDifficulty difficulty,
        Language language,
        int timeLimitMs,
        int memoryLimitMb) {
}
