package com.example.payment.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AccountTest {
    @Test
    void creditAndDebitUpdateBalance() {
        Account account = new Account("0000000001", "Alice", "THB", new BigDecimal("1000.00"));

        account.credit(new BigDecimal("250.00"));
        account.debit(new BigDecimal("100.00"));

        assertEquals(new BigDecimal("1150.00"), account.getBalance());
    }

    @Test
    void debitRejectsInsufficientFunds() {
        Account account = new Account("0000000001", "Alice", "THB", new BigDecimal("100.00"));

        assertThrows(InsufficientFundsException.class, () -> account.debit(new BigDecimal("100.01")));
        assertEquals(new BigDecimal("100.00"), account.getBalance());
    }
}
