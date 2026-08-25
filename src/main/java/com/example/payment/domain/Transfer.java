package com.example.payment.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity @Table(name = "transfer")
public class Transfer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "idempotency_key", nullable = false, unique = true, length = 64) private String idempotencyKey;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "from_account_id", nullable = false) private Account fromAccount;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "to_account_id", nullable = false) private Account toAccount;
    @Column(nullable = false, precision = 19, scale = 4) private BigDecimal amount;
    @Column(nullable = false, length = 3) private String currency;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 12) private TransferStatus status;
    @Column(name = "request_hash", nullable = false, length = 64) private String requestHash;
    @Column(name = "failure_reason", length = 255) private String failureReason;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    protected Transfer() { }
    public Transfer(String key, Account from, Account to, BigDecimal amount, String currency, String hash) { idempotencyKey = key; fromAccount = from; toAccount = to; this.amount = amount; this.currency = currency; requestHash = hash; status = TransferStatus.PENDING; }
    @PrePersist void onCreate() { createdAt = Instant.now(); }
    public Long getId() { return id; }
    public Account getFromAccount() { return fromAccount; }
    public Account getToAccount() { return toAccount; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public TransferStatus getStatus() { return status; }
    public String getRequestHash() { return requestHash; }
    public String getFailureReason() { return failureReason; }
    public Instant getCreatedAt() { return createdAt; }
    public String getStatusText() { return status.name(); }
    public void complete() { status = TransferStatus.COMPLETED; }
    public void fail(String reason) { status = TransferStatus.FAILED; failureReason = reason; }
}