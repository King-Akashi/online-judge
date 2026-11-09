package com.minijudge.problem.model;

import com.minijudge.contracts.Language;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Positive;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "problems")
public class Problem {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ProblemDifficulty difficulty;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Language language;

    @Column(nullable = false, columnDefinition = "text")
    private String statement;

    @Column(name = "constraints_text", nullable = false, columnDefinition = "text")
    private String constraintsText;

    @Column(name = "starter_code", nullable = false, columnDefinition = "text")
    private String starterCode;

    @Positive
    @Column(name = "time_limit_ms", nullable = false)
    private int timeLimitMs;

    @Positive
    @Column(name = "memory_limit_mb", nullable = false)
    private int memoryLimitMb;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "problem", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.LAZY)
    @OrderBy("orderIndex ASC")
    private List<TestCase> testCases = new ArrayList<>();

    protected Problem() {
    }

    public Problem(String title, ProblemDifficulty difficulty, Language language,
                   String statement, String constraintsText, String starterCode,
                   int timeLimitMs, int memoryLimitMb, Instant createdAt) {
        this.title = title;
        this.difficulty = difficulty;
        this.language = language;
        this.statement = statement;
        this.constraintsText = constraintsText;
        this.starterCode = starterCode;
        this.timeLimitMs = timeLimitMs;
        this.memoryLimitMb = memoryLimitMb;
        this.createdAt = createdAt;
    }

    public void addTestCase(TestCase testCase) {
        testCases.add(testCase);
        testCase.setProblem(this);
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public ProblemDifficulty getDifficulty() {
        return difficulty;
    }

    public Language getLanguage() {
        return language;
    }

    public String getStatement() {
        return statement;
    }

    public String getConstraintsText() {
        return constraintsText;
    }

    public String getStarterCode() {
        return starterCode;
    }

    public int getTimeLimitMs() {
        return timeLimitMs;
    }

    public int getMemoryLimitMb() {
        return memoryLimitMb;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<TestCase> getTestCases() {
        return List.copyOf(testCases);
    }
}
