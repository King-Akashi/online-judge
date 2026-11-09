package com.minijudge.problem.service;

public class InvalidProblemException extends RuntimeException {

    public InvalidProblemException(String message) {
        super(message);
    }
}
