package com.minijudge.problem.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "test_cases")
public class TestCase {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;

    @Column(nullable = false, columnDefinition = "text")
    private String input;

    @Column(name = "expected_output", nullable = false, columnDefinition = "text")
    private String expectedOutput;

    @Column(nullable = false)
    private boolean sample;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    protected TestCase() {
    }

    public TestCase(String input, String expectedOutput, boolean sample, int orderIndex) {
        this.input = input;
        this.expectedOutput = expectedOutput;
        this.sample = sample;
        this.orderIndex = orderIndex;
    }

    void setProblem(Problem problem) {
        this.problem = problem;
    }

    public UUID getId() {
        return id;
    }

    public String getInput() {
        return input;
    }

    public String getExpectedOutput() {
        return expectedOutput;
    }

    public boolean isSample() {
        return sample;
    }

    public int getOrderIndex() {
        return orderIndex;
    }
}
