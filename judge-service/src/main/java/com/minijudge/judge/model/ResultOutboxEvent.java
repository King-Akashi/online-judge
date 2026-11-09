package com.minijudge.judge.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "result_outbox")
public class ResultOutboxEvent {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @Column(name = "submission_id", nullable = false)
    private UUID submissionId;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    protected ResultOutboxEvent() {
    }

    public ResultOutboxEvent(UUID eventId, UUID submissionId, String payload, Instant createdAt) {
        this.eventId = eventId;
        this.submissionId = submissionId;
        this.payload = payload;
        this.createdAt = createdAt;
        this.nextAttemptAt = createdAt;
    }

    public void markPublished(Instant at) {
        publishedAt = at;
        lastError = null;
    }

    public void markFailed(Instant at, String error) {
        attempts++;
        nextAttemptAt = at.plusSeconds(Math.min(60, 1L << Math.min(attempts, 6)));
        lastError = error.length() > 1000 ? error.substring(0, 1000) : error;
    }

    public UUID getEventId() {
        return eventId;
    }

    public UUID getSubmissionId() {
        return submissionId;
    }

    public String getPayload() {
        return payload;
    }
}
