package com.minijudge.judge.model;

import com.minijudge.contracts.Verdict;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "judge_jobs")
public class JudgeJob {

    @Id
    @Column(name = "submission_id")
    private UUID submissionId;

    @Column(name = "request_event_id", nullable = false)
    private UUID requestEventId;

    @Column(name = "request_payload", columnDefinition = "text")
    private String requestPayload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private JudgeJobStatus status;

    @Enumerated(EnumType.STRING)
    @Column(length = 8)
    private Verdict verdict;

    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    @Column(name = "memory_used_bytes")
    private Long memoryUsedBytes;

    @Column(name = "passed_tests")
    private Integer passedTests;

    @Column(name = "total_tests")
    private Integer totalTests;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Version
    private long version;

    protected JudgeJob() {
    }

    public JudgeJob(UUID submissionId, UUID requestEventId, String requestPayload, Instant startedAt) {
        this.submissionId = submissionId;
        this.requestEventId = requestEventId;
        this.requestPayload = requestPayload;
        this.status = JudgeJobStatus.RUNNING;
        this.startedAt = startedAt;
    }

    public void retry(UUID requestEventId, String requestPayload, Instant startedAt) {
        this.requestEventId = requestEventId;
        this.requestPayload = requestPayload;
        this.startedAt = startedAt;
    }

    public void markRecoveryQueued(Instant queuedAt) {
        startedAt = queuedAt;
    }

    public void resultPending(Verdict verdict, long executionTimeMs, long memoryUsedBytes,
                              int passedTests, int totalTests, Instant completedAt) {
        this.status = JudgeJobStatus.RESULT_PENDING;
        this.verdict = verdict;
        this.executionTimeMs = executionTimeMs;
        this.memoryUsedBytes = memoryUsedBytes;
        this.passedTests = passedTests;
        this.totalTests = totalTests;
        this.completedAt = completedAt;
    }

    public void complete() {
        this.status = JudgeJobStatus.COMPLETED;
    }

    public UUID getSubmissionId() {
        return submissionId;
    }

    public JudgeJobStatus getStatus() {
        return status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public String getRequestPayload() {
        return requestPayload;
    }
}
