package com.minijudge.judge.repository;

import com.minijudge.judge.model.ResultOutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ResultOutboxRepository extends JpaRepository<ResultOutboxEvent, UUID> {

    List<ResultOutboxEvent> findTop50ByPublishedAtIsNullAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            Instant now);
}
