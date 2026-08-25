package com.example.payment.api;

import com.example.payment.util.ApiTooManyRequestsException;
import com.example.payment.util.RedisUnavailableException;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class TransferRateLimitService {
    private static final long LIMIT = 10;
    private static final long WINDOW_MILLIS = 60_000;
    private static final DefaultRedisScript<Long> SCRIPT = new DefaultRedisScript<>(
            "local now=tonumber(ARGV[1]); "
                    + "local window=tonumber(ARGV[2]); "
                    + "redis.call('ZREMRANGEBYSCORE', KEYS[1], 0, now-window); "
                    + "local count=redis.call('ZCARD', KEYS[1]); "
                    + "if count >= tonumber(ARGV[3]) then "
                    + "local oldest=redis.call('ZRANGE', KEYS[1], 0, 0, 'WITHSCORES')[2]; "
                    + "return math.ceil((oldest+window-now)/1000); end; "
                    + "redis.call('ZADD', KEYS[1], now, ARGV[4]); "
                    + "redis.call('EXPIRE', KEYS[1], 60); return 0", Long.class);

    private final StringRedisTemplate redis;

    public TransferRateLimitService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public long check(Long accountId) {
        try {
            Long retryAfter = redis.execute(SCRIPT, List.of("ratelimit:transfer:" + accountId),
                    String.valueOf(Instant.now().toEpochMilli()), String.valueOf(WINDOW_MILLIS),
                    String.valueOf(LIMIT), UUID.randomUUID().toString());
            if (retryAfter != null && retryAfter > 0) {
                throw new ApiTooManyRequestsException("Transfer rate limit exceeded", retryAfter);
            }
            return 0;
        } catch (ApiTooManyRequestsException ex) {
            throw ex;
        } catch (DataAccessException ex) {
            throw new RedisUnavailableException(ex);
        }
    }
}
