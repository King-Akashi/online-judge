package com.minijudge.problem.api;

import com.minijudge.contracts.ApiError;
import com.minijudge.problem.service.InternalApiKeyException;
import com.minijudge.problem.service.InternalApiUnavailableException;
import com.minijudge.problem.service.InvalidProblemException;
import com.minijudge.problem.service.ProblemConflictException;
import com.minijudge.problem.service.ProblemNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ProblemNotFoundException.class)
    public ResponseEntity<ApiError> notFound(ProblemNotFoundException exception, WebRequest request) {
        return error(HttpStatus.NOT_FOUND, "NOT_FOUND", "Problem not found",
                exception.getMessage(), request);
    }

    @ExceptionHandler({ProblemConflictException.class, DataIntegrityViolationException.class})
    public ResponseEntity<ApiError> conflict(Exception exception, WebRequest request) {
        return error(HttpStatus.CONFLICT, "CONFLICT", "Problem already exists",
                "A problem with this title already exists", request);
    }

    @ExceptionHandler({InvalidProblemException.class, MethodArgumentNotValidException.class,
            HttpMessageNotReadableException.class})
    public ResponseEntity<ApiError> invalidRequest(Exception exception, WebRequest request) {
        String detail = switch (exception) {
            case InvalidProblemException invalid -> invalid.getMessage();
            case MethodArgumentNotValidException validation -> validation.getBindingResult()
                    .getFieldErrors().stream()
                    .map(fieldError -> fieldError.getField() + " " + fieldError.getDefaultMessage())
                    .sorted()
                    .findFirst()
                    .orElse("Request validation failed");
            default -> "Request body is missing or malformed";
        };
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "Request validation failed", detail, request);
    }

    @ExceptionHandler(InternalApiKeyException.class)
    public ResponseEntity<ApiError> unauthorized(
            InternalApiKeyException exception, WebRequest request) {
        return error(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED",
                "Internal authentication required", exception.getMessage(), request);
    }

    @ExceptionHandler(InternalApiUnavailableException.class)
    public ResponseEntity<ApiError> internalApiUnavailable(
            InternalApiUnavailableException exception, WebRequest request) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, "INTERNAL_API_UNAVAILABLE",
                "Internal API unavailable", exception.getMessage(), request);
    }

    private ResponseEntity<ApiError> error(
            HttpStatus status, String type, String title, String detail, WebRequest request) {
        String path = ((ServletWebRequest) request).getRequest().getRequestURI();
        return ResponseEntity.status(status).body(
                new ApiError(type, title, status.value(), detail, path));
    }
}
