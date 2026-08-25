package com.example.payment.repository;

import com.example.payment.domain.LedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerRepository extends JpaRepository<LedgerEntry, Long> {
    Page<LedgerEntry> findByAccount_IdOrderByCreatedAtDesc(Long accountId, Pageable pageable);
}