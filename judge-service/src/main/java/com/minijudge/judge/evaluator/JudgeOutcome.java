package com.minijudge.judge.evaluator;

import com.minijudge.contracts.Verdict;

public record JudgeOutcome(
        Verdict verdict,
        long executionTimeMs,
        long memoryUsedBytes,
        int passedTests,
        int totalTests) {
}
