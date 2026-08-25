package com.example.payment.messaging;

import com.example.payment.domain.OutboxEvent;
import com.example.payment.repository.OutboxRepository;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboxPublisher {
    private final OutboxRepository events; private final JmsTemplate jms;
    private final String transferCompletedQueue;

    public OutboxPublisher(OutboxRepository events, JmsTemplate jms,
                           @org.springframework.beans.factory.annotation.Value("${payment.messaging.transfer-completed-queue}") String transferCompletedQueue) {
        this.events = events;
        this.jms = jms;
        this.transferCompletedQueue = transferCompletedQueue;
    }
    @Scheduled(fixedDelayString = "${payment.outbox.poll-ms:1000}")
    @Transactional
    public void publishPending() {
        for (OutboxEvent event : events.findTop50ByStatusOrderByIdAsc("PENDING")) {
            try { jms.convertAndSend(transferCompletedQueue, event.getPayload()); event.markPublished(); events.save(event); }
            catch (RuntimeException ignored) { }
        }
    }
}