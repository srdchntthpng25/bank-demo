package com.example.payment.api;

import com.example.payment.util.ApiTooManyRequestsException;
import com.example.payment.util.RedisUnavailableException;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
public class RedisLockService {
    private static final Duration LOCK_TTL = Duration.ofSeconds(5);
    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end", Long.class);

    private final StringRedisTemplate redis;

    public RedisLockService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public Lock acquireAccount(Long accountId) {
        return acquire("lock:account:" + accountId);
    }

    public Lock acquireIdempotency(String key) {
        return acquire("payment:idem:" + key);
    }

    private Lock acquire(String key) {
        String token = UUID.randomUUID().toString();
        try {
            Boolean acquired = redis.opsForValue().setIfAbsent(key, token, LOCK_TTL);
            if (!Boolean.TRUE.equals(acquired)) {
                throw new ApiTooManyRequestsException("Resource is busy", 5);
            }
            return new Lock(key, token);
        } catch (ApiTooManyRequestsException ex) {
            throw ex;
        } catch (DataAccessException ex) {
            throw new RedisUnavailableException(ex);
        }
    }

    public final class Lock implements AutoCloseable {
        private final String key;
        private final String token;

        private Lock(String key, String token) {
            this.key = key;
            this.token = token;
        }

        @Override
        public void close() {
            try {
                redis.execute(RELEASE_SCRIPT, List.of(key), token);
            } catch (DataAccessException ignored) { }
        }
    }
}
