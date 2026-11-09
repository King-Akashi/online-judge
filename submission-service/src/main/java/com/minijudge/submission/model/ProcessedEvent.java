package com.minijudge.submission.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processed_events")
public class ProcessedEvent {

    @Id
    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    protected ProcessedEvent() {
    }

    public ProcessedEvent(UUID eventId, Instant receivedAt) {
        this.eventId = eventId;
        this.receivedAt = receivedAt;
    }
}
