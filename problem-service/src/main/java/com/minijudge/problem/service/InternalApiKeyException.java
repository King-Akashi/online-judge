package com.minijudge.problem.service;

public class InternalApiKeyException extends RuntimeException {

    public InternalApiKeyException() {
        super("A valid internal API token is required");
    }
}
