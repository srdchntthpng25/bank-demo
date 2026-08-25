package com.example.payment.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "outbox_event")
public class OutboxEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "aggregate_type", nullable = false, length = 50) private String aggregateType;
    @Column(name = "aggregate_id", nullable = false, length = 50) private String aggregateId;
    @Column(name = "event_type", nullable = false, length = 50) private String eventType;
    @Column(nullable = false, columnDefinition = "nvarchar(max)") private String payload;
    @Column(nullable = false, length = 10) private String status = "PENDING";
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "published_at") private Instant publishedAt;
    protected OutboxEvent() { }
    public OutboxEvent(String aggregateId, String eventType, String payload) { aggregateType = "TRANSFER"; this.aggregateId = aggregateId; this.eventType = eventType; this.payload = payload; }
    @PrePersist void onCreate() { createdAt = Instant.now(); }
    public String getPayload() { return payload; }
    public void markPublished() { status = "PUBLISHED"; publishedAt = Instant.now(); }
}