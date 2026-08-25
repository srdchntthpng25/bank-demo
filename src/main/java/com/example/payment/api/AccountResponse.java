package com.example.payment.api;

import java.math.BigDecimal;
import java.time.Instant;

public record AccountResponse(
        Long id,
        String accountNumber,
        String ownerName,
        String currency,
        BigDecimal balance,
        String status,
        Instant createdAt
) {
}

record AccountBalanceResponse(Long accountId, BigDecimal balance, String currency, Instant asOf) {}
record AccountTransactionItem(Long id, String entryType, BigDecimal amount, BigDecimal balanceAfter, Long transferId, Instant createdAt) {}
record AccountTransactionPage(Long accountId, int page, int size, long totalElements, int totalPages, java.util.List<AccountTransactionItem> items) {}
record DepositResponse(Long accountId, BigDecimal balance, Long ledgerEntryId) {}
record TransferResponse(Long transferId, String status, Long fromAccountId, Long toAccountId, BigDecimal amount, String currency, Instant createdAt) {}
