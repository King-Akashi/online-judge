package com.minijudge.problem.service;

public class InternalApiUnavailableException extends RuntimeException {

    public InternalApiUnavailableException() {
        super("The internal API is not configured");
    }
}
