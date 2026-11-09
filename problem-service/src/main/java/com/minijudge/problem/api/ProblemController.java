package com.minijudge.problem.api;

import com.minijudge.problem.api.dto.CreateProblemRequest;
import com.minijudge.problem.api.dto.ProblemDetails;
import com.minijudge.problem.api.dto.ProblemSummary;
import com.minijudge.problem.service.ProblemService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/problems")
public class ProblemController {

    private final ProblemService problemService;

    public ProblemController(ProblemService problemService) {
        this.problemService = problemService;
    }

    @PostMapping
    public ResponseEntity<ProblemDetails> create(@Valid @RequestBody CreateProblemRequest request) {
        ProblemDetails created = problemService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{problemId}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public List<ProblemSummary> list() {
        return problemService.list();
    }

    @GetMapping("/{problemId}")
    public ProblemDetails get(@PathVariable UUID problemId) {
        return problemService.get(problemId);
    }
}
