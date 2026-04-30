package com.kid.A0.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;

@Service
public class RedisRateLimit {
    private final StringRedisTemplate redisTemplate;

    private static final String LUA_SCRIPT =
            "local current = redis.call('INCR', KEYS[1]); " +
                    "if current == 1 then " +
                    "  redis.call('EXPIRE', KEYS[1], ARGV[1]); " +
                    "end " +
                    "return current;";
    public RedisRateLimit(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isAllowed(String key, int limit) {
        String redisKey = "rl:" + key + ":" + (Instant.now().getEpochSecond() / 60);

        Long currentCount = redisTemplate.execute(
                new DefaultRedisScript<>(LUA_SCRIPT, Long.class),
                Collections.singletonList(redisKey),
                "65" // TTL in seconds
        );

        return (currentCount != null && currentCount <= limit);
    }
}
