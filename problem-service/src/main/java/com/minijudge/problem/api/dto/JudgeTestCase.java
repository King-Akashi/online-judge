package com.minijudge.problem.api.dto;

import java.util.UUID;

public record JudgeTestCase(
        UUID id,
        String input,
        String expectedOutput,
        boolean sample,
        int orderIndex) {
}
