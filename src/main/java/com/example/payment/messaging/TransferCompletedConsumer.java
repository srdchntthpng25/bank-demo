package com.example.payment.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.dao.DataAccessException;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
public class TransferCompletedConsumer {
    private static final Logger log = LoggerFactory.getLogger(TransferCompletedConsumer.class);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public TransferCompletedConsumer(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    @JmsListener(destination = "${payment.messaging.transfer-completed-queue}")
    public void consume(String payload) {
        try {
            JsonNode event = objectMapper.readTree(payload);
            String eventId = event.path("eventId").asText(null);
            if (eventId == null || eventId.isBlank()) {
                throw new IllegalArgumentException("Transfer eventId is required");
            }
            Long added = redis.opsForSet().add("processed:events", eventId);
            if (!Long.valueOf(1L).equals(added)) {
                return;
            }
            log.info("Processed transfer event {} for transfer {}", eventId, event.path("transferId").asLong());
        } catch (DataAccessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unable to process transfer event", ex);
        }
    }
}