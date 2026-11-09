package com.minijudge.judge.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minijudge.contracts.Language;
import com.minijudge.contracts.SubmissionRequested;
import com.minijudge.judge.messaging.JudgeEventPublisher;
import com.minijudge.judge.model.JudgeJob;
import com.minijudge.judge.model.JudgeJobStatus;
import com.minijudge.judge.repository.JudgeJobRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JudgeJobRecoveryTest {

    @Test
    void republishesStaleJobsAndRefreshesTheirRecoveryTimestamp() throws JsonProcessingException {
        UUID submissionId = UUID.randomUUID();
        Instant startedAt = Instant.now().minusSeconds(600);
        SubmissionRequested request = new SubmissionRequested(
                UUID.randomUUID(),
                submissionId,
                UUID.randomUUID(),
                Language.JAVA,
                "class Main {}",
                startedAt,
                1);
        JudgeJob job = new JudgeJob(
                submissionId, request.eventId(), "request-payload", startedAt);
        JudgeJobRepository repository = mock(JudgeJobRepository.class);
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        JudgeEventPublisher publisher = mock(JudgeEventPublisher.class);
        when(repository.findTop50ByStatusAndStartedAtBeforeOrderByStartedAtAsc(
                eq(JudgeJobStatus.RUNNING), any(Instant.class))).thenReturn(List.of(job));
        when(objectMapper.readValue("request-payload", SubmissionRequested.class))
                .thenReturn(request);

        new JudgeJobRecovery(repository, objectMapper, publisher, 5).requeueStaleJobs();

        verify(publisher).republishRequested(request);
        assertThat(job.getStartedAt()).isAfter(startedAt);
    }

    @Test
    void leavesRecoveryTimestampUnchangedWhenRepublishingFails() throws JsonProcessingException {
        UUID submissionId = UUID.randomUUID();
        Instant startedAt = Instant.now().minusSeconds(600);
        SubmissionRequested request = new SubmissionRequested(
                UUID.randomUUID(),
                submissionId,
                UUID.randomUUID(),
                Language.JAVA,
                "class Main {}",
                startedAt,
                1);
        JudgeJob job = new JudgeJob(
                submissionId, request.eventId(), "request-payload", startedAt);
        JudgeJobRepository repository = mock(JudgeJobRepository.class);
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        JudgeEventPublisher publisher = mock(JudgeEventPublisher.class);
        when(repository.findTop50ByStatusAndStartedAtBeforeOrderByStartedAtAsc(
                eq(JudgeJobStatus.RUNNING), any(Instant.class))).thenReturn(List.of(job));
        when(objectMapper.readValue("request-payload", SubmissionRequested.class))
                .thenReturn(request);
        doThrow(new IllegalStateException("broker unavailable"))
                .when(publisher).republishRequested(request);

        new JudgeJobRecovery(repository, objectMapper, publisher, 5).requeueStaleJobs();

        assertThat(job.getStartedAt()).isEqualTo(startedAt);
    }

    @Test
    void skipsLegacyJobsWithoutPayloadAndContinuesRecoveringLaterJobs()
            throws JsonProcessingException {
        Instant staleStartedAt = Instant.now().minusSeconds(600);
        JudgeJob legacyJob = new JudgeJob(
                UUID.randomUUID(), UUID.randomUUID(), null, staleStartedAt);
        UUID submissionId = UUID.randomUUID();
        SubmissionRequested request = new SubmissionRequested(
                UUID.randomUUID(),
                submissionId,
                UUID.randomUUID(),
                Language.JAVA,
                "class Main {}",
                staleStartedAt,
                1);
        JudgeJob recoverableJob = new JudgeJob(
                submissionId, request.eventId(), "request-payload", staleStartedAt);
        JudgeJobRepository repository = mock(JudgeJobRepository.class);
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        JudgeEventPublisher publisher = mock(JudgeEventPublisher.class);
        when(repository.findTop50ByStatusAndStartedAtBeforeOrderByStartedAtAsc(
                eq(JudgeJobStatus.RUNNING), any(Instant.class)))
                .thenReturn(List.of(legacyJob, recoverableJob));
        when(objectMapper.readValue("request-payload", SubmissionRequested.class))
                .thenReturn(request);

        new JudgeJobRecovery(repository, objectMapper, publisher, 5).requeueStaleJobs();

        verify(publisher).republishRequested(request);
        assertThat(legacyJob.getStartedAt()).isEqualTo(staleStartedAt);
        assertThat(recoverableJob.getStartedAt()).isAfter(staleStartedAt);
    }
}
