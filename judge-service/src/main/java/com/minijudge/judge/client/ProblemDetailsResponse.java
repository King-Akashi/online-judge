package com.minijudge.judge.client;

import com.minijudge.contracts.Language;

import java.util.List;
import java.util.UUID;

public record ProblemDetailsResponse(
        UUID id,
        String title,
        String difficulty,
        Language language,
        String statement,
        String constraints,
        String starterCode,
        int timeLimitMs,
        int memoryLimitMb,
        List<SampleTestCase> sampleTestCases) {

    public record SampleTestCase(String input, String expectedOutput) {
    }
}
