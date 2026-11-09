package com.minijudge.problem.api.dto;

import com.minijudge.contracts.Language;
import com.minijudge.problem.model.ProblemDifficulty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateProblemRequest(
        @NotBlank @Size(max = 200) String title,
        @NotNull ProblemDifficulty difficulty,
        @NotNull Language language,
        @NotBlank String statement,
        @NotBlank String constraints,
        @NotBlank String starterCode,
        @NotNull @Positive Integer timeLimitMs,
        @NotNull @Positive Integer memoryLimitMb,
        @NotEmpty List<@Valid CreateTestCaseRequest> testCases) {
}
