package com.example.payment.domain;

public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException() { super("Insufficient funds"); }
}