package com.minijudge.problem.api.dto;

import jakarta.validation.constraints.NotNull;

public record CreateTestCaseRequest(
        @NotNull String input,
        @NotNull String expectedOutput,
        @NotNull Boolean sample) {
}
