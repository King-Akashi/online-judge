package com.minijudge.submission.api.dto;

import com.minijudge.contracts.Language;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateSubmissionRequest(
        @NotNull UUID problemId,
        @NotNull Language language,
        @NotBlank @Size(max = 100_000) String sourceCode) {
}
