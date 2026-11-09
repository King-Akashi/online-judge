package com.minijudge.cli.dto;

import java.util.List;
import java.util.UUID;

public record ProblemDetails(
        UUID id,
        String title,
        String difficulty,
        String language,
        String statement,
        String constraints,
        String starterCode,
        int timeLimitMs,
        int memoryLimitMb,
        List<SampleTestCase> sampleTestCases) {
}
