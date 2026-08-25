package com.example.payment.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity @Table(name = "ledger_entry")
public class LedgerEntry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "account_id", nullable = false) private Account account;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "transfer_id") private Transfer transfer;
    @Enumerated(EnumType.STRING) @Column(name = "entry_type", nullable = false, length = 6) private EntryType entryType;
    @Column(nullable = false, precision = 19, scale = 4) private BigDecimal amount;
    @Column(name = "balance_after", nullable = false, precision = 19, scale = 4) private BigDecimal balanceAfter;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    protected LedgerEntry() { }
    public LedgerEntry(Account account, Transfer transfer, EntryType type, BigDecimal amount) { this.account = account; this.transfer = transfer; entryType = type; this.amount = amount; balanceAfter = account.getBalance(); }
    @PrePersist void onCreate() { createdAt = Instant.now(); }
    @PreUpdate @PreRemove void immutable() { throw new UnsupportedOperationException("Ledger entries are immutable"); }

    public Long getId() { return id; }
    public String getEntryTypeText() { return entryType.name(); }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public Long getTransferId() { return transfer == null ? null : transfer.getId(); }
    public Instant getCreatedAt() { return createdAt; }
}