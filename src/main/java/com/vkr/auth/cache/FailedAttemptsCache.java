package com.vkr.auth.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class FailedAttemptsCache {

    private final RedisTemplate<String, Integer> redisTemplate;
    private static final String KEY_PREFIX = "auth:failed:";
    private static final Duration TTL = Duration.ofMinutes(5); // сброс через 5 минут бездействия

    public int getFailedAttempts(String username) {
        String key = KEY_PREFIX + username;
        Integer count = redisTemplate.opsForValue().get(key);
        return count != null ? count : 0;
    }

    public void increment(String username) {
        String key = KEY_PREFIX + username;
        Integer current = redisTemplate.opsForValue().get(key);
        int newValue = (current != null ? current : 0) + 1;
        redisTemplate.opsForValue().set(key, newValue, TTL);
        log.debug("Failed attempts for {}: {}", username, newValue);
    }

    public void reset(String username) {
        String key = KEY_PREFIX + username;
        redisTemplate.delete(key);
        log.debug("Reset failed attempts for {}", username);
    }
}