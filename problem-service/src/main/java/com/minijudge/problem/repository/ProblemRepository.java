package com.minijudge.problem.repository;

import com.minijudge.problem.model.Problem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProblemRepository extends JpaRepository<Problem, UUID> {

    List<Problem> findAllByOrderByTitleAsc();

    boolean existsByTitleIgnoreCase(String title);

    @EntityGraph(attributePaths = "testCases")
    Optional<Problem> findWithTestCasesById(UUID id);
}
