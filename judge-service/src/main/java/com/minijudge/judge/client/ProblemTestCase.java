package com.minijudge.judge.client;

import java.util.UUID;

public record ProblemTestCase(
        UUID id,
        String input,
        String expectedOutput,
        boolean sample,
        int orderIndex) {
}
