package com.minijudge.problem.api;

import com.minijudge.problem.api.dto.JudgeTestCase;
import com.minijudge.problem.service.InternalApiKeyException;
import com.minijudge.problem.service.InternalApiUnavailableException;
import com.minijudge.problem.service.ProblemService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal/v1/problems")
public class InternalProblemController {

    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

    private final ProblemService problemService;
    private final String internalApiKey;

    public InternalProblemController(
            ProblemService problemService,
            @Value("${minijudge.internal-api-key:}") String internalApiKey) {
        this.problemService = problemService;
        this.internalApiKey = internalApiKey;
    }

    @GetMapping("/{problemId}/test-cases")
    public List<JudgeTestCase> getTestCases(
            @PathVariable UUID problemId,
            @RequestHeader(name = INTERNAL_TOKEN_HEADER, required = false) String token) {
        if (internalApiKey.isBlank()) {
            throw new InternalApiUnavailableException();
        }
        if (token == null || !MessageDigest.isEqual(
                internalApiKey.getBytes(StandardCharsets.UTF_8),
                token.getBytes(StandardCharsets.UTF_8))) {
            throw new InternalApiKeyException();
        }
        return problemService.getTestCases(problemId);
    }
}
