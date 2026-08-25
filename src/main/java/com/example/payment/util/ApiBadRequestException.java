package com.example.payment.util;

public class ApiBadRequestException extends RuntimeException {
    public ApiBadRequestException(String message) { super(message); }
}