package com.minijudge.judge.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minijudge.contracts.SubmissionEvaluated;
import com.minijudge.judge.evaluator.JudgeOutcome;
import com.minijudge.judge.model.JudgeJob;
import com.minijudge.judge.model.JudgeJobStatus;
import com.minijudge.judge.model.ResultOutboxEvent;
import com.minijudge.judge.repository.JudgeJobRepository;
import com.minijudge.judge.repository.ResultOutboxRepository;
import com.minijudge.contracts.SubmissionRequested;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class JudgeJobStore {

    private final JudgeJobRepository judgeJobRepository;
    private final ResultOutboxRepository resultOutboxRepository;
    private final ObjectMapper objectMapper;

    public JudgeJobStore(
            JudgeJobRepository judgeJobRepository,
            ResultOutboxRepository resultOutboxRepository,
            ObjectMapper objectMapper) {
        this.judgeJobRepository = judgeJobRepository;
        this.resultOutboxRepository = resultOutboxRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public boolean begin(SubmissionRequested request) {
        String requestPayload = serializeRequest(request);
        Optional<JudgeJob> existing = judgeJobRepository.findById(request.submissionId());
        if (existing.isEmpty()) {
            judgeJobRepository.save(new JudgeJob(
                    request.submissionId(), request.eventId(), requestPayload, Instant.now()));
            return true;
        }
        JudgeJob job = existing.get();
        if (job.getStatus() != JudgeJobStatus.RUNNING) {
            return false;
        }
        job.retry(request.eventId(), requestPayload, Instant.now());
        return true;
    }

    private String serializeRequest(SubmissionRequested request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not persist the submission request", exception);
        }
    }

    @Transactional
    public void complete(
            SubmissionRequested request,
            SubmissionEvaluated result,
            JudgeOutcome outcome) {
        JudgeJob job = judgeJobRepository.findById(request.submissionId())
                .orElseThrow(() -> new IllegalStateException(
                        "Judge job was not persisted before evaluation"));
        Instant completedAt = result.occurredAt();
        job.resultPending(
                outcome.verdict(),
                outcome.executionTimeMs(),
                outcome.memoryUsedBytes(),
                outcome.passedTests(),
                outcome.totalTests(),
                completedAt);
        try {
            resultOutboxRepository.save(new ResultOutboxEvent(
                    result.eventId(),
                    result.submissionId(),
                    objectMapper.writeValueAsString(result),
                    completedAt));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize the judge result", exception);
        }
    }

    @Transactional
    public void markResultPublished(UUID submissionId) {
        judgeJobRepository.findById(submissionId)
                .orElseThrow(() -> new IllegalStateException(
                        "Published result has no corresponding judge job"))
                .complete();
    }
}
