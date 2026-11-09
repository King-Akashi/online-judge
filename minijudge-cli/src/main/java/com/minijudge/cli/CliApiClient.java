package com.minijudge.cli;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.minijudge.cli.dto.ProblemDetails;
import com.minijudge.cli.dto.ProblemSummary;
import com.minijudge.cli.dto.SubmissionResponse;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

public class CliApiClient {

    private static final TypeReference<List<ProblemSummary>> PROBLEM_LIST =
            new TypeReference<>() {
            };
    private static final TypeReference<List<SubmissionResponse>> SUBMISSION_LIST =
            new TypeReference<>() {
            };

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final URI problemService;
    private final URI submissionService;

    public CliApiClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        this.problemService = URI.create(System.getenv().getOrDefault(
                "MINIJUDGE_PROBLEM_URL", "http://localhost:8081"));
        this.submissionService = URI.create(System.getenv().getOrDefault(
                "MINIJUDGE_SUBMISSION_URL", "http://localhost:8082"));
    }

    public List<ProblemSummary> listProblems() {
        return get(problemService.resolve("/api/v1/problems"), PROBLEM_LIST);
    }

    public ProblemDetails getProblem(UUID id) {
        return get(problemService.resolve("/api/v1/problems/" + id), ProblemDetails.class);
    }

    public SubmissionResponse createSubmission(UUID problemId, String sourceCode) {
        String body;
        try {
            body = objectMapper.writeValueAsString(
                    new CreateSubmissionRequest(problemId, "JAVA", sourceCode));
        } catch (JsonProcessingException exception) {
            throw new CliApiException("Could not encode the submission request", exception);
        }
        return send(
                HttpRequest.newBuilder(submissionService.resolve("/api/v1/submissions"))
                        .timeout(Duration.ofSeconds(15))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                        .build(),
                SubmissionResponse.class);
    }

    public SubmissionResponse getSubmission(UUID id) {
        return get(submissionService.resolve("/api/v1/submissions/" + id),
                SubmissionResponse.class);
    }

    public List<SubmissionResponse> submissionHistory() {
        return get(submissionService.resolve("/api/v1/submissions"), SUBMISSION_LIST);
    }

    private <T> T get(URI uri, Class<T> responseType) {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();
        return send(request, responseType);
    }

    private <T> T get(URI uri, TypeReference<T> responseType) {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();
        HttpResponse<String> response = send(request);
        try {
            return objectMapper.readValue(response.body(), responseType);
        } catch (JsonProcessingException exception) {
            throw new CliApiException("Service returned an invalid response", exception);
        }
    }

    private <T> T send(HttpRequest request, Class<T> responseType) {
        HttpResponse<String> response = send(request);
        try {
            return objectMapper.readValue(response.body(), responseType);
        } catch (JsonProcessingException exception) {
            throw new CliApiException("Service returned an invalid response", exception);
        }
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw apiError(response);
            }
            return response;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new CliApiException("Request was interrupted", exception);
        } catch (IOException exception) {
            throw new CliApiException(
                    "Could not connect to MiniJudge services: " + request.uri(), exception);
        }
    }

    private CliApiException apiError(HttpResponse<String> response) {
        try {
            JsonNode error = objectMapper.readTree(response.body());
            String title = error.path("title").asText("Request failed");
            String detail = error.path("detail").asText("");
            return new CliApiException(detail.isBlank()
                    ? title + " (HTTP " + response.statusCode() + ")"
                    : title + ": " + detail);
        } catch (JsonProcessingException exception) {
            return new CliApiException("Request failed with HTTP " + response.statusCode());
        }
    }

    private record CreateSubmissionRequest(UUID problemId, String language, String sourceCode) {
    }
}
