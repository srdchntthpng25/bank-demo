package com.example.payment.util;

public class RedisUnavailableException extends RuntimeException {
    public RedisUnavailableException(Throwable cause) { super("Redis is unavailable", cause); }
}