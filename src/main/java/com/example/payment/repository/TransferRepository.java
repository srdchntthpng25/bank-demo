package com.example.payment.repository;

import com.example.payment.domain.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TransferRepository extends JpaRepository<Transfer, Long> { Optional<Transfer> findByIdempotencyKey(String key); }