package com.example.payment.api;

import com.example.payment.domain.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.payment.util.*;
import com.example.payment.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {
    private final AccountRepository accounts;
    private final TransferRepository transfers;
    private final LedgerRepository ledger;
    private final OutboxRepository outbox;
    private final RedisLockService locks;
    private final AccountCacheService accountCache;
    private final TransferRateLimitService transferRateLimit;
    private final ObjectMapper objectMapper;

    public PaymentService(AccountRepository accounts, TransferRepository transfers, LedgerRepository ledger, OutboxRepository outbox,
                          RedisLockService locks, AccountCacheService accountCache, TransferRateLimitService transferRateLimit,
                          ObjectMapper objectMapper) {
        this.accounts = accounts;
        this.transfers = transfers;
        this.ledger = ledger;
        this.outbox = outbox;
        this.locks = locks;
        this.accountCache = accountCache;
        this.transferRateLimit = transferRateLimit;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Account create(PaymentRequests.CreateAccount request) {
        if (request.initialBalance().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("initialBalance must be >= 0");
        }
        Account account = new Account("", request.ownerName(), request.currency(), request.initialBalance());
        account = accounts.save(account);
        account.setAccountNumber(String.format("%010d", account.getId()));
        account = accounts.save(account);
        if (request.initialBalance().compareTo(BigDecimal.ZERO) > 0) {
            ledger.save(new LedgerEntry(account, null, EntryType.CREDIT, request.initialBalance()));
        }
        accountCache.put(account);
        return account;
    }

    @Transactional(readOnly = true)
    public Account getAccount(Long id) {
        return accounts.findById(id).orElseThrow(() -> new ResourceNotFoundException("Account not found: " + id));
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccountResponse(Long id) {
        AccountCacheService.CachedAccount cached = accountCache.get(id);
        if (cached == null) {
            Account account = getAccount(id);
            accountCache.put(account);
            return toResponse(account);
        }
        AccountRepository.AccountSnapshot snapshot = accounts.findSnapshotById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + id));
        return new AccountResponse(cached.id(), cached.accountNumber(), cached.ownerName(), cached.currency(),
                snapshot.getBalance(), cached.status(), snapshot.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public AccountBalanceResponse getBalance(Long id) {
        Account account = getAccount(id);
        return new AccountBalanceResponse(account.getId(), account.getBalance(), account.getCurrency(), java.time.Instant.now());
    }

    @Transactional(readOnly = true)
    public AccountTransactionPage getTransactions(Long id, int page, int size) {
        getAccount(id);
        Page<LedgerEntry> result = ledger.findByAccount_IdOrderByCreatedAtDesc(id, PageRequest.of(page, size));
        List<AccountTransactionItem> items = result.getContent().stream()
                .map(entry -> new AccountTransactionItem(entry.getId(), entry.getEntryTypeText(), entry.getAmount(), entry.getBalanceAfter(), entry.getTransferId(), entry.getCreatedAt()))
                .toList();
        return new AccountTransactionPage(id, page, size, result.getTotalElements(), result.getTotalPages(), items);
    }

    @Transactional
    public Account updateStatus(Long id, String statusText) {
        try (RedisLockService.Lock ignored = locks.acquireAccount(id)) {
            Account account = getLocked(id);
            AccountStatus status = AccountStatus.valueOf(statusText);
            if (account.getBalance().compareTo(BigDecimal.ZERO) > 0 && status == AccountStatus.CLOSED) {
                throw new ApiConflictException("Cannot close an account with remaining balance");
            }
            account.setStatus(status);
            account = accounts.save(account);
            accountCache.evict(id);
            return account;
        }
    }

    @Transactional
    public DepositResponse deposit(Long id, BigDecimal amount) {
        try (RedisLockService.Lock ignored = locks.acquireAccount(id)) {
            Account account = getLocked(id);
            active(account);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("amount must be > 0");
            account.credit(amount);
            LedgerEntry entry = ledger.save(new LedgerEntry(account, null, EntryType.CREDIT, amount));
            accountCache.evict(id);
            return new DepositResponse(account.getId(), account.getBalance(), entry.getId());
        }
    }

    @Transactional
    public DepositResponse withdraw(Long id, BigDecimal amount) {
        try (RedisLockService.Lock ignored = locks.acquireAccount(id)) {
            Account account = getLocked(id);
            active(account);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("amount must be > 0");
            account.debit(amount);
            LedgerEntry entry = ledger.save(new LedgerEntry(account, null, EntryType.DEBIT, amount));
            accountCache.evict(id);
            return new DepositResponse(account.getId(), account.getBalance(), entry.getId());
        }
    }

    @Transactional
    public Transfer transfer(String key, PaymentRequests.Transfer request) {
        try (RedisLockService.Lock ignored = locks.acquireIdempotency(key)) {
            String hash = hash(request);
            var previous = transfers.findByIdempotencyKey(key);
            if (previous.isPresent()) {
                if (!previous.get().getRequestHash().equals(hash)) {
                    throw new ApiConflictException("Idempotency key reused with different payload");
                }
                return previous.get();
            }
            transferRateLimit.check(request.fromAccountId());
            if (request.fromAccountId().equals(request.toAccountId())) {
                throw new IllegalArgumentException("Cannot transfer to same account");
            }
            if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("amount must be > 0");
            }
            try (RedisLockService.Lock firstLock = locks.acquireAccount(Math.min(request.fromAccountId(), request.toAccountId()));
                 RedisLockService.Lock secondLock = locks.acquireAccount(Math.max(request.fromAccountId(), request.toAccountId()))) {
                Account first = request.fromAccountId() < request.toAccountId()
                        ? getLocked(request.fromAccountId()) : getLocked(request.toAccountId());
                Account second = request.fromAccountId() < request.toAccountId()
                        ? getLocked(request.toAccountId()) : getLocked(request.fromAccountId());
                Account from = request.fromAccountId().equals(first.getId()) ? first : second;
                Account to = request.toAccountId().equals(first.getId()) ? first : second;
            if (!from.getCurrency().equals(to.getCurrency()) || !from.getCurrency().equals(request.currency())) {
                throw new IllegalArgumentException("Currency mismatch");
            }
            active(from); active(to);
            if (from.getBalance().compareTo(request.amount()) < 0) {
                throw new InsufficientFundsException();
            }
            Transfer transfer = transfers.save(new Transfer(key, from, to, request.amount(), request.currency(), hash));
            from.debit(request.amount());
            to.credit(request.amount());
            ledger.save(new LedgerEntry(from, transfer, EntryType.DEBIT, request.amount()));
            ledger.save(new LedgerEntry(to, transfer, EntryType.CREDIT, request.amount()));
            transfer.complete();
            outbox.save(new OutboxEvent(transfer.getId().toString(), "TransferCompleted", transferCompletedPayload(transfer)));
            accountCache.evict(from.getId());
            accountCache.evict(to.getId());
            return transfers.save(transfer);
            }
        }
    }

    @Transactional(readOnly = true)
    public Transfer getTransfer(Long id) {
        return transfers.findById(id).orElseThrow(() -> new ResourceNotFoundException("Transfer not found: " + id));
    }

    public AccountResponse toResponse(Account account) {
        return new AccountResponse(account.getId(), account.getAccountNumber(), account.getOwnerName(), account.getCurrency(), account.getBalance(), account.getStatusText(), account.getCreatedAt());
    }

    public TransferResponse toResponse(Transfer transfer) {
        return new TransferResponse(transfer.getId(), transfer.getStatusText(), transfer.getFromAccount().getId(), transfer.getToAccount().getId(), transfer.getAmount(), transfer.getCurrency(), transfer.getCreatedAt());
    }

    private Account getLocked(Long id) {
        return accounts.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Account not found: " + id));
    }

    private void active(Account account) {
        if (!account.isActive()) {
            throw new IllegalArgumentException("Account is not ACTIVE");
        }
    }

    private String hash(PaymentRequests.Transfer request) {
        try {
            byte[] bytes = (request.fromAccountId() + ":" + request.toAccountId() + ":" + request.amount().toPlainString() + ":" + request.currency()).getBytes(StandardCharsets.UTF_8);
            StringBuilder result = new StringBuilder();
            for (byte value : MessageDigest.getInstance("SHA-256").digest(bytes)) {
                result.append(String.format("%02x", value));
            }
            return result.toString();
        } catch (Exception e) { throw new IllegalStateException("Unable to hash request", e); }
    }

    private String transferCompletedPayload(Transfer transfer) {
        try {
            return objectMapper.writeValueAsString(new TransferCompletedEvent(
                    "evt-" + UUID.randomUUID(), "TransferCompleted", transfer.getId(),
                    transfer.getFromAccount().getId(), transfer.getToAccount().getId(), transfer.getAmount(),
                    transfer.getCurrency(), transfer.getCreatedAt() == null ? Instant.now() : transfer.getCreatedAt()));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize transfer event", ex);
        }
    }

    private record TransferCompletedEvent(String eventId, String eventType, Long transferId, Long fromAccountId,
                                           Long toAccountId, BigDecimal amount, String currency, Instant occurredAt) { }
}