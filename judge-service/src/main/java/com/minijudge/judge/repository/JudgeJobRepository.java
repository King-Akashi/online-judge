package com.minijudge.judge.repository;

import com.minijudge.judge.model.JudgeJob;
import com.minijudge.judge.model.JudgeJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface JudgeJobRepository extends JpaRepository<JudgeJob, UUID> {

    List<JudgeJob> findTop50ByStatusAndStartedAtBeforeOrderByStartedAtAsc(
            JudgeJobStatus status, Instant before);
}
