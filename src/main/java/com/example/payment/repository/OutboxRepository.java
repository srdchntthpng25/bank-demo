package com.example.payment.repository;

import com.example.payment.domain.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> { List<OutboxEvent> findTop50ByStatusOrderByIdAsc(String status); }