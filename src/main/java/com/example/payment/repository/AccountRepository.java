package com.example.payment.repository;

import com.example.payment.domain.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    interface AccountSnapshot {
        Long getId();
        java.math.BigDecimal getBalance();
        java.time.Instant getCreatedAt();
    }

    @Query("select a.id as id, a.balance as balance, a.createdAt as createdAt from Account a where a.id = :id")
    Optional<AccountSnapshot> findSnapshotById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id = :id")
    Optional<Account> findByIdForUpdate(@Param("id") Long id);
}