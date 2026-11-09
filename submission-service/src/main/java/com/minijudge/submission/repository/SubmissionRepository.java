package com.minijudge.submission.repository;

import com.minijudge.submission.model.Submission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SubmissionRepository extends JpaRepository<Submission, UUID> {

    List<Submission> findTop100ByOrderByCreatedAtDesc();
}
