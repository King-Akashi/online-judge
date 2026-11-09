package com.minijudge.problem.api.dto;

import com.minijudge.contracts.Language;
import com.minijudge.problem.model.ProblemDifficulty;

import java.util.List;
import java.util.UUID;

public record ProblemDetails(
        UUID id,
        String title,
        ProblemDifficulty difficulty,
        Language language,
        String statement,
        String constraints,
        String starterCode,
        int timeLimitMs,
        int memoryLimitMb,
        List<SampleTestCase> sampleTestCases) {
}
