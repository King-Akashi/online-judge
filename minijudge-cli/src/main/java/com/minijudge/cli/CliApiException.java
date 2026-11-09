package com.minijudge.cli;

public class CliApiException extends RuntimeException {

    public CliApiException(String message) {
        super(message);
    }

    public CliApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
