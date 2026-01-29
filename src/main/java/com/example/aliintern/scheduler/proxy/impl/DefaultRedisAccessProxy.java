package com.example.aliintern.scheduler.proxy.impl;

import com.example.aliintern.scheduler.proxy.RedisAccessProxy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Redis访问代理默认实现
 * 封装Redis操作，处理性能优化与异常保护
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultRedisAccessProxy implements RedisAccessProxy {

    private final StringRedisTemplate redisTemplate;

    @Override
    public String get(String key) {
        try {
            log.debug("Redis GET: {}", key);
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("Redis GET failed for key: {}", key, e);
            // 快速失败，返回null触发降级
            return null;
        }
    }

    @Override
    public void set(String key, String value, long ttl, TimeUnit timeUnit) {
        try {
            log.debug("Redis SET: {}, ttl: {} {}", key, ttl, timeUnit);
            redisTemplate.opsForValue().set(key, value, ttl, timeUnit);
        } catch (Exception e) {
            log.error("Redis SET failed for key: {}", key, e);
            // 写入失败不影响主流程
        }
    }

    @Override
    public Map<String, String> multiGet(List<String> keys) {
        Map<String, String> result = new HashMap<>();
        try {
            log.debug("Redis MGET: {} keys", keys.size());
            List<String> values = redisTemplate.opsForValue().multiGet(keys);
            if (values != null) {
                for (int i = 0; i < keys.size(); i++) {
                    if (values.get(i) != null) {
                        result.put(keys.get(i), values.get(i));
                    }
                }
            }
        } catch (Exception e) {
            log.error("Redis MGET failed", e);
        }
        return result;
    }

    @Override
    public boolean delete(String key) {
        try {
            log.debug("Redis DEL: {}", key);
            Boolean deleted = redisTemplate.delete(key);
            return Boolean.TRUE.equals(deleted);
        } catch (Exception e) {
            log.error("Redis DEL failed for key: {}", key, e);
            return false;
        }
    }

    @Override
    public boolean exists(String key) {
        try {
            log.debug("Redis EXISTS: {}", key);
            Boolean exists = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.error("Redis EXISTS failed for key: {}", key, e);
            return false;
        }
    }

    @Override
    public Long increment(String key) {
        try {
            log.debug("Redis INCR: {}", key);
            return redisTemplate.opsForValue().increment(key);
        } catch (Exception e) {
            log.error("Redis INCR failed for key: {}", key, e);
            return 0L;
        }
    }

    @Override
    public Long incrementWithExpire(String key, long ttl, TimeUnit timeUnit) {
        try {
            log.debug("Redis INCR with expire: {}, ttl: {} {}", key, ttl, timeUnit);
            Long value = redisTemplate.opsForValue().increment(key);
            redisTemplate.expire(key, ttl, timeUnit);
            return value;
        } catch (Exception e) {
            log.error("Redis INCR with expire failed for key: {}", key, e);
            return 0L;
        }
    }
}
