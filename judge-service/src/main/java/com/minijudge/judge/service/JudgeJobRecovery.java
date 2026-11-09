package com.minijudge.judge.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minijudge.contracts.SubmissionRequested;
import com.minijudge.judge.messaging.JudgeEventPublisher;
import com.minijudge.judge.model.JudgeJob;
import com.minijudge.judge.model.JudgeJobStatus;
import com.minijudge.judge.repository.JudgeJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class JudgeJobRecovery {

    private static final Logger LOGGER = LoggerFactory.getLogger(JudgeJobRecovery.class);

    private final JudgeJobRepository judgeJobRepository;
    private final ObjectMapper objectMapper;
    private final JudgeEventPublisher eventPublisher;
    private final long staleAfterMinutes;

    public JudgeJobRecovery(
            JudgeJobRepository judgeJobRepository,
            ObjectMapper objectMapper,
            JudgeEventPublisher eventPublisher,
            @Value("${minijudge.recovery.stale-after-minutes:5}") long staleAfterMinutes) {
        this.judgeJobRepository = judgeJobRepository;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
        this.staleAfterMinutes = staleAfterMinutes;
    }

    @Scheduled(fixedDelayString = "${minijudge.recovery.poll-interval-ms:60000}")
    @Transactional
    public void requeueStaleJobs() {
        Instant cutoff = Instant.now().minus(staleAfterMinutes, ChronoUnit.MINUTES);
        List<JudgeJob> staleJobs = judgeJobRepository
                .findTop50ByStatusAndStartedAtBeforeOrderByStartedAtAsc(
                        JudgeJobStatus.RUNNING, cutoff);
        for (JudgeJob job : staleJobs) {
            String requestPayload = job.getRequestPayload();
            if (requestPayload == null || requestPayload.isBlank()) {
                LOGGER.error("Cannot recover judge job for submission {}: missing request payload",
                        job.getSubmissionId());
                continue;
            }
            try {
                SubmissionRequested request = objectMapper.readValue(
                        requestPayload, SubmissionRequested.class);
                if (request == null) {
                    LOGGER.error("Cannot recover judge job for submission {}: empty request payload",
                            job.getSubmissionId());
                    continue;
                }
                eventPublisher.republishRequested(request);
                job.markRecoveryQueued(Instant.now());
                LOGGER.warn("Requeued stale judge job for submission {}", job.getSubmissionId());
            } catch (JsonProcessingException exception) {
                LOGGER.error("Cannot recover judge job for submission {}: invalid request payload",
                        job.getSubmissionId(), exception);
            } catch (IllegalStateException exception) {
                LOGGER.warn("Cannot requeue stale judge job for submission {}",
                        job.getSubmissionId(), exception);
            }
        }
    }
}
