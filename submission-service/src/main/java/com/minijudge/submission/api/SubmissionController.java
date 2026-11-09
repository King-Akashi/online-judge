package com.minijudge.submission.api;

import com.minijudge.submission.api.dto.CreateSubmissionRequest;
import com.minijudge.submission.api.dto.SubmissionResponse;
import com.minijudge.submission.service.SubmissionService;
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
@RequestMapping("/api/v1/submissions")
public class SubmissionController {

    private final SubmissionService submissionService;

    public SubmissionController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @PostMapping
    public ResponseEntity<SubmissionResponse> create(
            @Valid @RequestBody CreateSubmissionRequest request) {
        SubmissionResponse submission = submissionService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{submissionId}")
                .buildAndExpand(submission.id())
                .toUri();
        return ResponseEntity.accepted().location(location).body(submission);
    }

    @GetMapping("/{submissionId}")
    public SubmissionResponse get(@PathVariable UUID submissionId) {
        return submissionService.get(submissionId);
    }

    @GetMapping
    public List<SubmissionResponse> history() {
        return submissionService.history();
    }
}
