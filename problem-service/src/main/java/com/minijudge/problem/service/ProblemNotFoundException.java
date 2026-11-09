package com.minijudge.problem.service;

import java.util.UUID;

public class ProblemNotFoundException extends RuntimeException {

    public ProblemNotFoundException(UUID id) {
        super("Problem not found: " + id);
    }
}
