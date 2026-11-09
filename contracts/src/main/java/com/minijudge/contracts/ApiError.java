package com.minijudge.contracts;

import java.util.Objects;

public record ApiError(String type, String title, int status, String detail, String instance) {

    public ApiError {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(detail, "detail");
        Objects.requireNonNull(instance, "instance");
        if (status < 400 || status > 599) {
            throw new IllegalArgumentException("status must be an HTTP error status");
        }
    }
}
