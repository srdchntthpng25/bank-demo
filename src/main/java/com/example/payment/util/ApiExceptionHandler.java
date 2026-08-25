package com.example.payment.util;

import com.example.payment.domain.InsufficientFundsException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.MDC;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleMalformedBody(HttpMessageNotReadableException ex, HttpServletRequest request) {
        ProblemDetail problem = buildProblem("https://errors.bank.local/malformed-request", "Malformed request", 400,
                "Request body could not be parsed", request.getRequestURI());
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(ApiBadRequestException.class)
    public ResponseEntity<ProblemDetail> handleBadRequest(ApiBadRequestException ex, HttpServletRequest request) {
        ProblemDetail problem = buildProblem("https://errors.bank.local/bad-request", "Bad request", 400,
                ex.getMessage(), request.getRequestURI());
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        ProblemDetail problem = buildProblem("https://errors.bank.local/not-found", "Resource not found", 404,
                ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(ApiConflictException.class)
    public ResponseEntity<ProblemDetail> handleConflict(ApiConflictException ex, HttpServletRequest request) {
        ProblemDetail problem = buildProblem("https://errors.bank.local/conflict", "Conflict", 409,
                ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @ExceptionHandler(ApiTooManyRequestsException.class)
    public ResponseEntity<ProblemDetail> handleTooManyRequests(ApiTooManyRequestsException ex, HttpServletRequest request) {
        ProblemDetail problem = buildProblem("https://errors.bank.local/too-many-requests", "Too many requests", 429,
                ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
                .body(problem);
    }

    @ExceptionHandler(RedisUnavailableException.class)
    public ResponseEntity<ProblemDetail> handleRedisUnavailable(RedisUnavailableException ex, HttpServletRequest request) {
        ProblemDetail problem = buildProblem("https://errors.bank.local/service-unavailable", "Service unavailable", 503,
                "Payment operation is temporarily unavailable", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(problem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        ProblemDetail problem = buildProblem("https://errors.bank.local/validation-error", "Validation failed", 422,
                "Request body is invalid", request.getRequestURI());
        return ResponseEntity.unprocessableEntity().body(problem);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        ProblemDetail problem = buildProblem("https://errors.bank.local/constraint-violation", "Validation failed", 400,
                "Request parameters are invalid", request.getRequestURI());
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        ProblemDetail problem = buildProblem("https://errors.bank.local/invalid-request", "Invalid request", 422,
                ex.getMessage(), request.getRequestURI());
        return ResponseEntity.unprocessableEntity().body(problem);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> handleStatus(ResponseStatusException ex, HttpServletRequest request) {
        ProblemDetail problem = buildProblem("https://errors.bank.local/operation-not-allowed", ex.getReason(), ex.getStatusCode().value(),
                ex.getReason(), request.getRequestURI());
        return ResponseEntity.status(ex.getStatusCode()).body(problem);
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ProblemDetail> handleInsufficientFunds(InsufficientFundsException ex, HttpServletRequest request) {
        ProblemDetail problem = buildProblem("https://errors.bank.local/insufficient-funds", "Insufficient funds", 422,
                ex.getMessage(), request.getRequestURI());
        return ResponseEntity.unprocessableEntity().body(problem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGeneral(Exception ex, HttpServletRequest request) {
        ProblemDetail problem = buildProblem("https://errors.bank.local/internal-error", "Internal server error", 500,
                "Unexpected server error", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    private ProblemDetail buildProblem(String type, String title, int status, String detail, String instance) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.valueOf(status), detail);
        problem.setType(URI.create(type));
        problem.setTitle(title);
        problem.setInstance(URI.create(instance));
        String traceId = MDC.get("X-Request-Id");
        problem.setProperty("traceId", traceId == null ? UUID.randomUUID().toString() : traceId);
        problem.setProperty("timestamp", Instant.now().toString());
        return problem;
    }
}