package com.example.aliintern.scheduler.limiter.impl;

import com.example.aliintern.scheduler.common.model.RequestContext;
import com.example.aliintern.scheduler.limiter.RateLimiterService;
import com.example.aliintern.scheduler.proxy.RedisAccessProxy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 限流服务默认实现
 * 基于Redis的滑动窗口限流
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisRateLimiterService implements RateLimiterService {

    private final RedisAccessProxy redisAccessProxy;

    private static final String RATE_LIMIT_PREFIX = "rate_limit:";

    @Override
    public boolean isRateLimited(RequestContext context) {
        // TODO: 实现限流逻辑
        log.debug("Checking rate limit for request: {}", context.getRequestId());
        return false;
    }

    @Override
    public boolean tryAcquire(String key, int permitsPerSecond) {
        String rateLimitKey = RATE_LIMIT_PREFIX + key;
        Long currentCount = redisAccessProxy.incrementWithExpire(rateLimitKey, 1, TimeUnit.SECONDS);

        boolean acquired = currentCount != null && currentCount <= permitsPerSecond;
        log.debug("Rate limit tryAcquire for key {}: count={}, limit={}, acquired={}",
                key, currentCount, permitsPerSecond, acquired);
        return acquired;
    }

    @Override
    public Long getCurrentQps(String key) {
        String rateLimitKey = RATE_LIMIT_PREFIX + key;
        String count = redisAccessProxy.get(rateLimitKey);
        return count != null ? Long.parseLong(count) : 0L;
    }
}
