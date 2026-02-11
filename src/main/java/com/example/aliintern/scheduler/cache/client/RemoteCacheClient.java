package com.example.aliintern.scheduler.cache.client;

import com.example.aliintern.scheduler.config.SchedulerProperties;
import com.example.aliintern.scheduler.common.enums.CacheTtlLevel;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Redis 缓存客户端
 * 封装 Redis 访问操作
 * 
 * 职责：
 * - 提供 Redis 缓存读写接口
 * - 管理 TTL（基于 Redis 的 expire）
 * - 异常容错，不影响主流程
 * - 序列化/反序列化处理
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RemoteCacheClient {

    private final StringRedisTemplate redisTemplate;
    private final SchedulerProperties schedulerProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 从 Redis 获取数据
     * 
     * @param key   缓存键
     * @param clazz 值类型
     * @return 缓存值，未命中返回 null
     */
    public <T> T get(String key, Class<T> clazz) {
        if (key == null) {
            return null;
        }
        
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json != null) {
                log.debug("Redis 缓存命中: key={}", key);
                // 如果是 String 类型，直接返回
                if (clazz == String.class) {
                    return clazz.cast(json);
                }
                // 否则反序列化
                return objectMapper.readValue(json, clazz);
            }
            log.debug("Redis 缓存未命中: key={}", key);
            return null;
        } catch (Exception e) {
            log.warn("Redis 缓存读取异常: key={}, error={}", key, e.getMessage());
            return null;
        }
    }

    /**
     * 从 Redis 获取数据（泛型版本，无需指定 Class）
     * 注意：此方法返回的是 String，调用方需要自行处理类型转换
     * 
     * @param key 缓存键
     * @return 缓存值（String），未命中返回 null
     */
    public String get(String key) {
        if (key == null) {
            return null;
        }
        
        try {
            String value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                log.debug("Redis 缓存命中: key={}", key);
            } else {
                log.debug("Redis 缓存未命中: key={}", key);
            }
            return value;
        } catch (Exception e) {
            log.warn("Redis 缓存读取异常: key={}, error={}", key, e.getMessage());
            return null;
        }
    }

    /**
     * 写入 Redis 缓存
     * 
     * @param key      缓存键
     * @param value    缓存值
     * @param ttlLevel TTL 等级
     */
    public void put(String key, Object value, CacheTtlLevel ttlLevel) {
        if (key == null || value == null) {
            return;
        }
        
        try {
            String json;
            if (value instanceof String) {
                json = (String) value;
            } else {
                json = objectMapper.writeValueAsString(value);
            }
            
            Duration ttl = getTtlDuration(ttlLevel);
            redisTemplate.opsForValue().set(key, json, ttl);
            
            log.debug("Redis 缓存写入成功: key={}, ttlLevel={}, ttl={}s", 
                    key, ttlLevel, ttl.getSeconds());
        } catch (Exception e) {
            log.warn("Redis 缓存写入失败: key={}, error={}", key, e.getMessage());
        }
    }

    /**
     * 获取 Redis 的 TTL（Duration）
     */
    private Duration getTtlDuration(CacheTtlLevel level) {
        SchedulerProperties.CacheConfig.TtlConfig.TtlLevelConfig remote = 
                schedulerProperties.getCache().getTtl().getRemote();
        
        if (level == null) {
            return Duration.ofSeconds(remote.getNormalTtl());
        }
        
        long seconds = switch (level) {
            case SHORT -> remote.getShortTtl();
            case NORMAL -> remote.getNormalTtl();
            case LONG -> remote.getLongTtl();
        };
        
        return Duration.ofSeconds(seconds);
    }

    /**
     * 删除 Redis 缓存
     * 
     * @param key 缓存键
     */
    public void delete(String key) {
        if (key == null) {
            return;
        }
        
        try {
            redisTemplate.delete(key);
            log.debug("Redis 缓存删除: key={}", key);
        } catch (Exception e) {
            log.warn("Redis 缓存删除失败: key={}, error={}", key, e.getMessage());
        }
    }

    /**
     * 检查 Redis 是否可用
     * 
     * @return true 表示可用
     */
    public boolean isAvailable() {
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            return true;
        } catch (Exception e) {
            log.warn("Redis 不可用: {}", e.getMessage());
            return false;
        }
    }
}

