package com.example.payment.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "account")
public class Account {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "account_number", nullable = false, unique = true, length = 20) private String accountNumber;
    @Column(name = "owner_name", nullable = false, length = 120) private String ownerName;
    @Column(nullable = false, length = 3) private String currency;
    @Column(nullable = false, precision = 19, scale = 4) private BigDecimal balance = BigDecimal.ZERO;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10) private AccountStatus status = AccountStatus.ACTIVE;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected Account() { }

    public Account(String accountNumber, String ownerName, String currency, BigDecimal initialBalance) {
        this.accountNumber = accountNumber;
        this.ownerName = ownerName;
        this.currency = currency;
        this.balance = initialBalance == null ? BigDecimal.ZERO : initialBalance;
    }

    @PrePersist void onCreate() { createdAt = Instant.now(); updatedAt = createdAt; }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public String getAccountNumber() { return accountNumber; }
    public String getOwnerName() { return ownerName; }
    public String getCurrency() { return currency; }
    public BigDecimal getBalance() { return balance; }
    public AccountStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getStatusText() { return status.name(); }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public void setStatus(AccountStatus status) { this.status = status; }
    public void credit(BigDecimal amount) { balance = balance.add(amount); }
    public void debit(BigDecimal amount) { if (balance.compareTo(amount) < 0) throw new InsufficientFundsException(); balance = balance.subtract(amount); }
    public boolean isActive() { return status == AccountStatus.ACTIVE; }
}