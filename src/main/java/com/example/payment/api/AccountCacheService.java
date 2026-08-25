package com.example.payment.api;

import com.example.payment.domain.Account;
import com.example.payment.util.RedisUnavailableException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class AccountCacheService {
    private static final Duration TTL = Duration.ofSeconds(60);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public AccountCacheService(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public CachedAccount get(Long id) {
        try {
            String value = redis.opsForValue().get(key(id));
            return value == null ? null : objectMapper.readValue(value, CachedAccount.class);
        } catch (DataAccessException | JsonProcessingException ex) {
            return null;
        }
    }

    public void put(Account account) {
        try {
            CachedAccount value = new CachedAccount(account.getId(), account.getAccountNumber(),
                    account.getOwnerName(), account.getCurrency(), account.getStatusText());
            redis.opsForValue().set(key(account.getId()), objectMapper.writeValueAsString(value), TTL);
        } catch (DataAccessException | JsonProcessingException ex) {
            throw new RedisUnavailableException(ex);
        }
    }

    public void evict(Long id) {
        try {
            redis.delete(key(id));
        } catch (DataAccessException ex) {
            throw new RedisUnavailableException(ex);
        }
    }

    private String key(Long id) {
        return "account:" + id;
    }

    public record CachedAccount(Long id, String accountNumber, String ownerName, String currency, String status) { }
}
