package com.minijudge.submission.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minijudge.contracts.SubmissionEvaluated;
import com.minijudge.contracts.SubmissionRequested;
import com.minijudge.contracts.SubmissionStarted;
import com.minijudge.contracts.SubmissionStatus;
import com.minijudge.submission.api.dto.CreateSubmissionRequest;
import com.minijudge.submission.api.dto.SubmissionResponse;
import com.minijudge.submission.model.OutboxEvent;
import com.minijudge.submission.model.ProcessedEvent;
import com.minijudge.submission.model.Submission;
import com.minijudge.submission.repository.OutboxEventRepository;
import com.minijudge.submission.repository.ProcessedEventRepository;
import com.minijudge.submission.repository.SubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    public SubmissionService(
            SubmissionRepository submissionRepository,
            OutboxEventRepository outboxEventRepository,
            ProcessedEventRepository processedEventRepository,
            ObjectMapper objectMapper) {
        this.submissionRepository = submissionRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.processedEventRepository = processedEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public SubmissionResponse create(CreateSubmissionRequest request) {
        Instant now = Instant.now();
        Submission submission = submissionRepository.save(new Submission(
                request.problemId(), request.language(), request.sourceCode(), now));
        SubmissionRequested event = new SubmissionRequested(
                UUID.randomUUID(),
                submission.getId(),
                request.problemId(),
                request.language(),
                request.sourceCode(),
                now,
                1);
        try {
            outboxEventRepository.save(new OutboxEvent(
                    event.eventId(),
                    SubmissionRequested.class.getSimpleName(),
                    objectMapper.writeValueAsString(event),
                    now));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize the submission event", exception);
        }
        return toResponse(submission);
    }

    @Transactional(readOnly = true)
    public SubmissionResponse get(UUID id) {
        return toResponse(findById(id));
    }

    @Transactional(readOnly = true)
    public List<SubmissionResponse> history() {
        return submissionRepository.findTop100ByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void markRunning(SubmissionStarted event) {
        if (processedEventRepository.existsById(event.eventId())) {
            return;
        }
        Submission submission = findById(event.submissionId());
        if (!submission.isTerminal() && submission.getStatus() == SubmissionStatus.QUEUED) {
            submission.markRunning(event.occurredAt());
        }
        processedEventRepository.save(new ProcessedEvent(event.eventId(), Instant.now()));
    }

    @Transactional
    public void complete(SubmissionEvaluated event) {
        if (processedEventRepository.existsById(event.eventId())) {
            return;
        }
        Submission submission = findById(event.submissionId());
        if (!submission.isTerminal()) {
            submission.complete(
                    event.verdict(),
                    event.executionTimeMs(),
                    event.memoryUsedBytes(),
                    event.passedTests(),
                    event.totalTests(),
                    event.occurredAt());
        }
        processedEventRepository.save(new ProcessedEvent(event.eventId(), Instant.now()));
    }

    private Submission findById(UUID id) {
        return submissionRepository.findById(id)
                .orElseThrow(() -> new SubmissionNotFoundException(id));
    }

    private SubmissionResponse toResponse(Submission submission) {
        return new SubmissionResponse(
                submission.getId(),
                submission.getProblemId(),
                submission.getLanguage(),
                submission.getStatus(),
                submission.getVerdict(),
                submission.getExecutionTimeMs(),
                submission.getMemoryUsedBytes(),
                submission.getPassedTests(),
                submission.getTotalTests(),
                submission.getCreatedAt(),
                submission.getUpdatedAt());
    }
}
