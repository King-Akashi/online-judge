package com.minijudge.cli.dto;

import java.util.UUID;

public record ProblemSummary(
        UUID id,
        String title,
        String difficulty,
        String language,
        int timeLimitMs,
        int memoryLimitMb) {
}
