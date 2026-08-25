package com.example.payment.util;

public class ApiTooManyRequestsException extends RuntimeException {
    private final long retryAfterSeconds;

    public ApiTooManyRequestsException(String message) {
        this(message, 5);
    }

    public ApiTooManyRequestsException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() { return retryAfterSeconds; }
}