package com.example.aliintern.scheduler.cache;

import com.example.aliintern.scheduler.cache.client.LocalCacheClient;
import com.example.aliintern.scheduler.cache.client.RemoteCacheClient;
import com.example.aliintern.scheduler.cache.impl.DefaultCacheAccessProxy;
import com.example.aliintern.scheduler.common.enums.CacheMode;
import com.example.aliintern.scheduler.common.enums.CacheTtlLevel;
import com.example.aliintern.scheduler.common.model.DispatchDecision;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 缓存访问代理单元测试
 * 
 * 测试覆盖：
 * 1. 四种缓存模式的访问路径
 * 2. 缓存命中/未命中场景
 * 3. 异常容错处理
 * 4. 缓存失效操作
 */
@ExtendWith(MockitoExtension.class)
class CacheAccessProxyTest {

    @Mock
    private LocalCacheClient localCache;

    @Mock
    private RemoteCacheClient remoteCache;

    private CacheAccessProxy proxy;

    @BeforeEach
    void setUp() {
        proxy = new DefaultCacheAccessProxy(localCache, remoteCache);
    }

    // ==================== 模式 1: NONE ====================

    @Test
    void testAccessDbOnly_ShouldCallDbDirectly() {
        // Given
        DispatchDecision decision = DispatchDecision.builder()
                .cacheMode(CacheMode.NONE)
                .ttlLevel(CacheTtlLevel.SHORT)
                .build();
        
        Supplier<String> dbLoader = () -> "db-value";
        
        // When
        String result = proxy.access("test-key", dbLoader, decision);
        
        // Then
        assertEquals("db-value", result);
        verify(localCache, never()).get(any());
        verify(remoteCache, never()).get(any());
    }

    // ==================== 模式 2: LOCAL_ONLY ====================

    @Test
    void testAccessLocalOnly_WhenHit_ShouldReturnFromLocal() {
        // Given
        DispatchDecision decision = DispatchDecision.builder()
                .cacheMode(CacheMode.LOCAL_ONLY)
                .ttlLevel(CacheTtlLevel.NORMAL)
                .build();
        
        when(localCache.get("test-key")).thenReturn("local-value");
        Supplier<String> dbLoader = () -> {
            fail("DB should not be called");
            return null;
        };
        
        // When
        String result = proxy.access("test-key", dbLoader, decision);
        
        // Then
        assertEquals("local-value", result);
        verify(localCache).get("test-key");
        verify(localCache, never()).put(any(), any(), any());
    }

    @Test
    void testAccessLocalOnly_WhenMiss_ShouldLoadDbAndCache() {
        // Given
        DispatchDecision decision = DispatchDecision.builder()
                .cacheMode(CacheMode.LOCAL_ONLY)
                .ttlLevel(CacheTtlLevel.NORMAL)
                .build();
        
        when(localCache.get("test-key")).thenReturn(null);
        Supplier<String> dbLoader = () -> "db-value";
        
        // When
        String result = proxy.access("test-key", dbLoader, decision);
        
        // Then
        assertEquals("db-value", result);
        verify(localCache).get("test-key");
        verify(localCache).put("test-key", "db-value", CacheTtlLevel.NORMAL);
    }

    // ==================== 模式 3: REMOTE_ONLY ====================

    @Test
    void testAccessRemoteOnly_WhenHit_ShouldReturnFromRedis() {
        // Given
        DispatchDecision decision = DispatchDecision.builder()
                .cacheMode(CacheMode.REMOTE_ONLY)
                .ttlLevel(CacheTtlLevel.SHORT)
                .build();
        
        when(remoteCache.get("test-key")).thenReturn("redis-value");
        Supplier<String> dbLoader = () -> {
            fail("DB should not be called");
            return null;
        };
        
        // When
        String result = proxy.access("test-key", dbLoader, decision);
        
        // Then
        assertEquals("redis-value", result);
        verify(remoteCache).get("test-key");
        verify(remoteCache, never()).put(any(), any(), any());
    }

    @Test
    void testAccessRemoteOnly_WhenMiss_ShouldLoadDbAndCache() {
        // Given
        DispatchDecision decision = DispatchDecision.builder()
                .cacheMode(CacheMode.REMOTE_ONLY)
                .ttlLevel(CacheTtlLevel.SHORT)
                .build();
        
        when(remoteCache.get("test-key")).thenReturn(null);
        Supplier<String> dbLoader = () -> "db-value";
        
        // When
        String result = proxy.access("test-key", dbLoader, decision);
        
        // Then
        assertEquals("db-value", result);
        verify(remoteCache).get("test-key");
        verify(remoteCache).put("test-key", "db-value", CacheTtlLevel.SHORT);
    }

    // ==================== 模式 4: LOCAL_AND_REMOTE ====================

    @Test
    void testAccessLocalAndRemote_WhenLocalHit_ShouldReturnFromLocal() {
        // Given
        DispatchDecision decision = DispatchDecision.builder()
                .cacheMode(CacheMode.LOCAL_AND_REMOTE)
                .ttlLevel(CacheTtlLevel.LONG)
                .build();
        
        when(localCache.get("test-key")).thenReturn("local-value");
        Supplier<String> dbLoader = () -> {
            fail("DB should not be called");
            return null;
        };
        
        // When
        String result = proxy.access("test-key", dbLoader, decision);
        
        // Then
        assertEquals("local-value", result);
        verify(localCache).get("test-key");
        verify(remoteCache, never()).get(any());
    }

    @Test
    void testAccessLocalAndRemote_WhenLocalMissRedisHit_ShouldBackfillLocal() {
        // Given
        DispatchDecision decision = DispatchDecision.builder()
                .cacheMode(CacheMode.LOCAL_AND_REMOTE)
                .ttlLevel(CacheTtlLevel.LONG)
                .build();
        
        when(localCache.get("test-key")).thenReturn(null);
        when(remoteCache.get("test-key")).thenReturn("redis-value");
        Supplier<String> dbLoader = () -> {
            fail("DB should not be called");
            return null;
        };
        
        // When
        String result = proxy.access("test-key", dbLoader, decision);
        
        // Then
        assertEquals("redis-value", result);
        verify(localCache).get("test-key");
        verify(remoteCache).get("test-key");
        verify(localCache).put("test-key", "redis-value", CacheTtlLevel.LONG);
    }

    @Test
    void testAccessLocalAndRemote_WhenBothMiss_ShouldLoadDbAndCacheBoth() {
        // Given
        DispatchDecision decision = DispatchDecision.builder()
                .cacheMode(CacheMode.LOCAL_AND_REMOTE)
                .ttlLevel(CacheTtlLevel.NORMAL)
                .build();
        
        when(localCache.get("test-key")).thenReturn(null);
        when(remoteCache.get("test-key")).thenReturn(null);
        Supplier<String> dbLoader = () -> "db-value";
        
        // When
        String result = proxy.access("test-key", dbLoader, decision);
        
        // Then
        assertEquals("db-value", result);
        verify(localCache).get("test-key");
        verify(remoteCache).get("test-key");
        verify(remoteCache).put("test-key", "db-value", CacheTtlLevel.NORMAL);
        verify(localCache).put("test-key", "db-value", CacheTtlLevel.NORMAL);
    }

    // ==================== 异常处理 ====================

    @Test
    void testAccess_WhenLocalCacheThrowsException_ShouldFallbackToRemote() {
        // Given
        DispatchDecision decision = DispatchDecision.builder()
                .cacheMode(CacheMode.LOCAL_AND_REMOTE)
                .ttlLevel(CacheTtlLevel.NORMAL)
                .build();
        
        when(localCache.get("test-key")).thenThrow(new RuntimeException("Local cache error"));
        when(remoteCache.get("test-key")).thenReturn("redis-value");
        Supplier<String> dbLoader = () -> {
            fail("DB should not be called");
            return null;
        };
        
        // When
        String result = proxy.access("test-key", dbLoader, decision);
        
        // Then
        assertEquals("redis-value", result);
        verify(localCache).get("test-key");
        verify(remoteCache).get("test-key");
    }

    @Test
    void testAccess_WhenInvalidParameters_ShouldCallDb() {
        // When: null key
        String result1 = proxy.access(null, () -> "db-value", DispatchDecision.noCache());
        assertEquals("db-value", result1);
        
        // When: null dbLoader
        String result2 = proxy.access("test-key", null, DispatchDecision.noCache());
        assertNull(result2);
        
        // When: null decision
        String result3 = proxy.access("test-key", () -> "db-value", null);
        assertEquals("db-value", result3);
    }

    // ==================== 缓存失效 ====================

    @Test
    void testInvalidate_ShouldDeleteFromBothCaches() {
        // When
        proxy.invalidate("test-key");
        
        // Then
        verify(localCache).invalidate("test-key");
        verify(remoteCache).delete("test-key");
    }

    @Test
    void testInvalidate_WhenLocalFails_ShouldStillDeleteRemote() {
        // Given
        doThrow(new RuntimeException("Local delete failed"))
                .when(localCache).invalidate("test-key");
        
        // When
        proxy.invalidate("test-key");
        
        // Then
        verify(localCache).invalidate("test-key");
        verify(remoteCache).delete("test-key");
    }

    @Test
    void testInvalidate_WhenKeyIsNull_ShouldDoNothing() {
        // When
        proxy.invalidate(null);
        
        // Then
        verify(localCache, never()).invalidate(any());
        verify(remoteCache, never()).delete(any());
    }
}
