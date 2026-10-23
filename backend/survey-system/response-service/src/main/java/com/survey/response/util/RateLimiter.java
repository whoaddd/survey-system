package com.survey.response.util;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RateLimiter {

    private final StringRedisTemplate redisTemplate;

    /**
     * 固定窗口计数限流：key 在窗口内计数，超过 limit 返回 false。
     * @param key     业务维度 key，如 "rl:submit:ip:1.2.3.4"
     * @param limit   窗口内最大次数
     * @param window  窗口时长
     */
    public boolean tryAcquire(String key, int limit, Duration window) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, window);
        }
        return count != null && count <= limit;
    }
}