package com.minijudge.submission.model;

import com.minijudge.contracts.Language;
import com.minijudge.contracts.SubmissionStatus;
import com.minijudge.contracts.Verdict;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "submissions")
public class Submission {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "problem_id", nullable = false)
    private UUID problemId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Language language;

    @Column(name = "source_code", nullable = false, columnDefinition = "text")
    private String sourceCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SubmissionStatus status;

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

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected Submission() {
    }

    public Submission(UUID problemId, Language language, String sourceCode, Instant now) {
        this.problemId = problemId;
        this.language = language;
        this.sourceCode = sourceCode;
        this.status = SubmissionStatus.QUEUED;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public boolean isTerminal() {
        return status == SubmissionStatus.COMPLETED || status == SubmissionStatus.FAILED;
    }

    public void markRunning(Instant now) {
        if (status == SubmissionStatus.QUEUED) {
            status = SubmissionStatus.RUNNING;
            updatedAt = now;
        }
    }

    public void complete(Verdict verdict, long executionTimeMs, long memoryUsedBytes,
                         int passedTests, int totalTests, Instant now) {
        if (isTerminal()) {
            return;
        }
        status = SubmissionStatus.COMPLETED;
        this.verdict = verdict;
        this.executionTimeMs = executionTimeMs;
        this.memoryUsedBytes = memoryUsedBytes;
        this.passedTests = passedTests;
        this.totalTests = totalTests;
        updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProblemId() {
        return problemId;
    }

    public Language getLanguage() {
        return language;
    }

    public SubmissionStatus getStatus() {
        return status;
    }

    public Verdict getVerdict() {
        return verdict;
    }

    public Long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public Long getMemoryUsedBytes() {
        return memoryUsedBytes;
    }

    public Integer getPassedTests() {
        return passedTests;
    }

    public Integer getTotalTests() {
        return totalTests;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
