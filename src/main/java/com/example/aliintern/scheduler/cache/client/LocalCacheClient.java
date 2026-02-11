package com.example.aliintern.scheduler.cache.client;

import com.example.aliintern.scheduler.config.SchedulerProperties;
import com.example.aliintern.scheduler.common.enums.CacheTtlLevel;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 本地缓存客户端
 * 基于 Caffeine 实现的本地缓存访问封装
 * 
 * 职责：
 * - 提供本地缓存读写接口
 * - 管理 TTL（基于 Caffeine 的 expireAfterWrite）
 * - 异常容错，不影响主流程
 */
@Slf4j
@Component
public class LocalCacheClient {

    private final Cache<String, Object> cache;
    private final SchedulerProperties schedulerProperties;

    public LocalCacheClient(SchedulerProperties schedulerProperties) {
        this.schedulerProperties = schedulerProperties;
        
        // 初始化 Caffeine 缓存
        // 最大容量 10000，使用最长 TTL 作为统一过期时间
        // 注意：这里为了简化，使用固定 TTL，实际可以根据需要优化为动态 TTL
        this.cache = Caffeine.newBuilder()
                .maximumSize(10000)
                .expireAfterWrite(schedulerProperties.getCache().getTtl().getLocal().getLongTtl(), TimeUnit.SECONDS)
                .recordStats()
                .build();
        
        log.info("本地缓存初始化完成，最大容量: 10000");
    }

    /**
     * 从本地缓存获取数据
     * 
     * @param key 缓存键
     * @return 缓存值，未命中返回 null
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        if (key == null) {
            return null;
        }
        
        try {
            Object value = cache.getIfPresent(key);
            if (value != null) {
                log.debug("本地缓存命中: key={}", key);
                return (T) value;
            }
            log.debug("本地缓存未命中: key={}", key);
            return null;
        } catch (Exception e) {
            log.warn("本地缓存读取异常: key={}, error={}", key, e.getMessage());
            return null;
        }
    }

    /**
     * 写入本地缓存
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
            cache.put(key, value);
            log.debug("本地缓存写入成功: key={}, ttlLevel={}", key, ttlLevel);
        } catch (Exception e) {
            log.warn("本地缓存写入失败: key={}, error={}", key, e.getMessage());
        }
    }

    /**
     * 删除本地缓存
     * 
     * @param key 缓存键
     */
    public void invalidate(String key) {
        if (key == null) {
            return;
        }
        
        try {
            cache.invalidate(key);
            log.debug("本地缓存删除: key={}", key);
        } catch (Exception e) {
            log.warn("本地缓存删除失败: key={}, error={}", key, e.getMessage());
        }
    }

    /**
     * 获取缓存统计信息
     */
    public String getStats() {
        return cache.stats().toString();
    }
}

