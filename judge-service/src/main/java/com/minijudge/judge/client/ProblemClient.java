package com.minijudge.judge.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
public class ProblemClient {

    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";
    private static final ParameterizedTypeReference<List<ProblemTestCase>> TEST_CASE_LIST =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;
    private final String internalApiKey;

    public ProblemClient(
            RestClient.Builder builder,
            @Value("${minijudge.problem-service.url}") String problemServiceUrl,
            @Value("${minijudge.internal-api-key:}") String internalApiKey) {
        this.restClient = builder.baseUrl(problemServiceUrl).build();
        this.internalApiKey = internalApiKey;
    }

    public ProblemDetailsResponse getProblem(UUID problemId) {
        return Objects.requireNonNull(restClient.get()
                .uri("/api/v1/problems/{problemId}", problemId)
                .retrieve()
                .body(ProblemDetailsResponse.class), "Problem Service returned an empty problem");
    }

    public List<ProblemTestCase> getTestCases(UUID problemId) {
        if (internalApiKey.isBlank()) {
            throw new IllegalStateException("MINIJUDGE_INTERNAL_API_KEY is not configured");
        }
        return Objects.requireNonNull(restClient.get()
                .uri("/internal/v1/problems/{problemId}/test-cases", problemId)
                .header(INTERNAL_TOKEN_HEADER, internalApiKey)
                .retrieve()
                .body(TEST_CASE_LIST), "Problem Service returned an empty test suite");
    }
}
