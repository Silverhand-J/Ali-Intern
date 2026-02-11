package com.example.aliintern.scheduler.cache.impl;

import com.example.aliintern.scheduler.cache.CacheAccessProxy;
import com.example.aliintern.scheduler.cache.client.LocalCacheClient;
import com.example.aliintern.scheduler.cache.client.RemoteCacheClient;
import com.example.aliintern.scheduler.common.enums.CacheMode;
import com.example.aliintern.scheduler.common.model.DispatchDecision;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

/**
 * 默认缓存访问代理实现
 * 
 * 职责：
 * - 根据 DispatchDecision 执行多级缓存访问
 * - 管理本地缓存和 Redis 的读写顺序
 * - 处理异常容错，确保主流程不受影响
 * 
 * 访问策略：
 * - NONE: 直接回源 DB
 * - LOCAL_ONLY: 仅访问本地缓存
 * - REMOTE_ONLY: 仅访问 Redis
 * - LOCAL_AND_REMOTE: 先本地，再 Redis，最后 DB
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultCacheAccessProxy implements CacheAccessProxy {

    private final LocalCacheClient localCache;
    private final RemoteCacheClient remoteCache;

    @Override
    public <T> T access(String key, Supplier<T> dbLoader, DispatchDecision decision) {
        if (key == null || dbLoader == null || decision == null) {
            log.warn("无效参数: key={}, dbLoader={}, decision={}", key, dbLoader, decision);
            if (dbLoader != null) {
                return dbLoader.get();
            }
            return null;
        }

        CacheMode mode = decision.getCacheMode();
        
        // 根据 CacheMode 决定访问路径
        return switch (mode) {
            case NONE -> accessDbOnly(key, dbLoader);
            case LOCAL_ONLY -> accessLocalOnly(key, dbLoader, decision);
            case REMOTE_ONLY -> accessRemoteOnly(key, dbLoader, decision);
            case LOCAL_AND_REMOTE -> accessLocalAndRemote(key, dbLoader, decision);
        };
    }

    @Override
    public void invalidate(String key) {
        if (key == null) {
            return;
        }
        
        log.info("删除缓存: key={}", key);
        
        // 同时删除本地缓存和 Redis
        try {
            localCache.invalidate(key);
        } catch (Exception e) {
            log.warn("删除本地缓存失败: key={}, error={}", key, e.getMessage());
        }
        
        try {
            remoteCache.delete(key);
        } catch (Exception e) {
            log.warn("删除 Redis 缓存失败: key={}, error={}", key, e.getMessage());
        }
    }

    // ==================== 私有方法：不同的访问模式 ====================

    /**
     * 模式 1: 不使用缓存，直接回源
     */
    private <T> T accessDbOnly(String key, Supplier<T> dbLoader) {
        log.debug("访问模式: NONE, 直接回源 DB, key={}", key);
        return dbLoader.get();
    }

    /**
     * 模式 2: 仅使用本地缓存
     */
    private <T> T accessLocalOnly(String key, Supplier<T> dbLoader, DispatchDecision decision) {
        log.debug("访问模式: LOCAL_ONLY, key={}", key);
        
        // 1. 尝试从本地缓存获取
        T value = localCache.get(key);
        if (value != null) {
            return value;
        }
        
        // 2. 本地缓存未命中，回源 DB
        value = dbLoader.get();
        
        // 3. 回源成功，写入本地缓存
        if (value != null) {
            try {
                localCache.put(key, value, decision.getTtlLevel());
            } catch (Exception e) {
                log.warn("写入本地缓存失败: key={}, error={}", key, e.getMessage());
            }
        }
        
        return value;
    }

    /**
     * 模式 3: 仅使用 Redis
     */
    private <T> T accessRemoteOnly(String key, Supplier<T> dbLoader, DispatchDecision decision) {
        log.debug("访问模式: REMOTE_ONLY, key={}", key);
        
        // 1. 尝试从 Redis 获取
        String cachedValue = remoteCache.get(key);
        if (cachedValue != null) {
            // 注意：这里返回的是 String，调用方需要自行转换
            // 为了类型安全，实际使用时可能需要传入 Class<T> 参数
            return (T) cachedValue;
        }
        
        // 2. Redis 未命中，回源 DB
        T value = dbLoader.get();
        
        // 3. 回源成功，写入 Redis
        if (value != null) {
            try {
                remoteCache.put(key, value, decision.getTtlLevel());
            } catch (Exception e) {
                log.warn("写入 Redis 失败: key={}, error={}", key, e.getMessage());
            }
        }
        
        return value;
    }

    /**
     * 模式 4: 双层缓存（本地 + Redis）
     */
    private <T> T accessLocalAndRemote(String key, Supplier<T> dbLoader, DispatchDecision decision) {
        log.debug("访问模式: LOCAL_AND_REMOTE, key={}", key);
        
        // 1. 尝试从本地缓存获取
        T value = null;
        try {
            value = localCache.get(key);
            if (value != null) {
                return value;
            }
        } catch (Exception e) {
            log.warn("本地缓存读取异常，降级到 Redis: key={}, error={}", key, e.getMessage());
        }
        
        // 2. 本地未命中，尝试从 Redis 获取
        String cachedValue = remoteCache.get(key);
        if (cachedValue != null) {
            // Redis 命中，回填本地缓存
            try {
                localCache.put(key, cachedValue, decision.getTtlLevel());
            } catch (Exception e) {
                log.warn("回填本地缓存失败: key={}, error={}", key, e.getMessage());
            }
            return (T) cachedValue;
        }
        
        // 3. Redis 也未命中，回源 DB
        value = dbLoader.get();
        
        // 4. 回源成功，写入 Redis 和本地缓存
        if (value != null) {
            // 写 Redis
            try {
                remoteCache.put(key, value, decision.getTtlLevel());
            } catch (Exception e) {
                log.warn("写入 Redis 失败: key={}, error={}", key, e.getMessage());
            }
            
            // 写本地缓存
            try {
                localCache.put(key, value, decision.getTtlLevel());
            } catch (Exception e) {
                log.warn("写入本地缓存失败: key={}, error={}", key, e.getMessage());
            }
        }
        
        return value;
    }
}
