package com.minijudge.submission.api;

import com.minijudge.contracts.ApiError;
import com.minijudge.submission.service.SubmissionNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

@RestControllerAdvice
public class SubmissionApiExceptionHandler {

    @ExceptionHandler(SubmissionNotFoundException.class)
    public ResponseEntity<ApiError> notFound(
            SubmissionNotFoundException exception, WebRequest request) {
        return error(HttpStatus.NOT_FOUND, "NOT_FOUND", "Submission not found",
                exception.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(
            MethodArgumentNotValidException exception, WebRequest request) {
        String detail = exception.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + " " + fieldError.getDefaultMessage())
                .sorted()
                .findFirst()
                .orElse("Request validation failed");
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "Request validation failed", detail, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> malformed(
            HttpMessageNotReadableException exception, WebRequest request) {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "Request validation failed", "Request body is missing or malformed", request);
    }

    private ResponseEntity<ApiError> error(
            HttpStatus status, String type, String title, String detail, WebRequest request) {
        String path = ((ServletWebRequest) request).getRequest().getRequestURI();
        return ResponseEntity.status(status).body(
                new ApiError(type, title, status.value(), detail, path));
    }
}
