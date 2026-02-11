package com.example.aliintern.scheduler.controller;

import com.example.aliintern.scheduler.cache.CacheAccessProxy;
import com.example.aliintern.scheduler.common.enums.CacheMode;
import com.example.aliintern.scheduler.common.enums.CacheTtlLevel;
import com.example.aliintern.scheduler.common.model.DispatchDecision;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 缓存访问代理测试接口
 * 用于测试和演示缓存访问代理的功能
 */
@Slf4j
@RestController
@RequestMapping("/test/cache")
@RequiredArgsConstructor
public class CacheProxyTestController {

    private final CacheAccessProxy cacheAccessProxy;
    
    // 模拟 DB 访问计数器
    private final AtomicInteger dbAccessCount = new AtomicInteger(0);

    /**
     * 测试不同缓存模式的访问行为
     * 
     * 示例请求：
     * GET /test/cache/access?key=product:12345&mode=LOCAL_AND_REMOTE&ttl=NORMAL
     * 
     * @param key  缓存键
     * @param mode 缓存模式（NONE/LOCAL_ONLY/REMOTE_ONLY/LOCAL_AND_REMOTE）
     * @param ttl  TTL 等级（SHORT/NORMAL/LONG）
     * @return 访问结果
     */
    @GetMapping("/access")
    public Map<String, Object> testCacheAccess(
            @RequestParam String key,
            @RequestParam(defaultValue = "LOCAL_AND_REMOTE") String mode,
            @RequestParam(defaultValue = "NORMAL") String ttl) {
        
        long startTime = System.currentTimeMillis();
        int dbCallsBefore = dbAccessCount.get();
        
        // 构建决策
        DispatchDecision decision = DispatchDecision.builder()
                .cacheMode(CacheMode.valueOf(mode))
                .ttlLevel(CacheTtlLevel.valueOf(ttl))
                .build();
        
        // 模拟数据库回源
        String value = cacheAccessProxy.access(key, () -> {
            int dbCalls = dbAccessCount.incrementAndGet();
            log.info("模拟 DB 回源: key={}, dbCalls={}", key, dbCalls);
            try {
                Thread.sleep(50); // 模拟 DB 查询延迟
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "db-value-" + UUID.randomUUID().toString().substring(0, 8);
        }, decision);
        
        long duration = System.currentTimeMillis() - startTime;
        int dbCallsAfter = dbAccessCount.get();
        
        Map<String, Object> response = new HashMap<>();
        response.put("key", key);
        response.put("value", value);
        response.put("cacheMode", mode);
        response.put("ttlLevel", ttl);
        response.put("dbCalled", dbCallsAfter > dbCallsBefore);
        response.put("totalDbCalls", dbCallsAfter);
        response.put("durationMs", duration);
        
        return response;
    }

    /**
     * 测试缓存命中率
     * 连续访问同一个 key，观察缓存效果
     * 
     * 示例请求：
     * GET /test/cache/hit-test?key=product:99999&mode=LOCAL_AND_REMOTE&count=10
     * 
     * @param key   缓存键
     * @param mode  缓存模式
     * @param count 访问次数
     * @return 命中率统计
     */
    @GetMapping("/hit-test")
    public Map<String, Object> testCacheHitRate(
            @RequestParam String key,
            @RequestParam(defaultValue = "LOCAL_AND_REMOTE") String mode,
            @RequestParam(defaultValue = "10") int count) {
        
        long startTime = System.currentTimeMillis();
        int dbCallsBefore = dbAccessCount.get();
        
        DispatchDecision decision = DispatchDecision.builder()
                .cacheMode(CacheMode.valueOf(mode))
                .ttlLevel(CacheTtlLevel.NORMAL)
                .build();
        
        String firstValue = null;
        for (int i = 0; i < count; i++) {
            final int iteration = i;
            String value = cacheAccessProxy.access(key, () -> {
                dbAccessCount.incrementAndGet();
                log.info("DB 回源: key={}, iteration={}", key, iteration);
                return "db-value-" + System.currentTimeMillis();
            }, decision);
            
            if (firstValue == null) {
                firstValue = value;
            }
        }
        
        long duration = System.currentTimeMillis() - startTime;
        int dbCallsAfter = dbAccessCount.get();
        int actualDbCalls = dbCallsAfter - dbCallsBefore;
        
        Map<String, Object> response = new HashMap<>();
        response.put("key", key);
        response.put("cacheMode", mode);
        response.put("totalAccess", count);
        response.put("dbCalls", actualDbCalls);
        response.put("cacheHits", count - actualDbCalls);
        response.put("hitRate", String.format("%.2f%%", (count - actualDbCalls) * 100.0 / count));
        response.put("durationMs", duration);
        response.put("avgLatencyMs", duration / (double) count);
        
        return response;
    }

    /**
     * 测试缓存失效
     * 
     * 示例请求：
     * DELETE /test/cache/invalidate?key=product:12345
     * 
     * @param key 缓存键
     * @return 失效结果
     */
    @DeleteMapping("/invalidate")
    public Map<String, Object> testCacheInvalidate(@RequestParam String key) {
        long startTime = System.currentTimeMillis();
        
        cacheAccessProxy.invalidate(key);
        
        long duration = System.currentTimeMillis() - startTime;
        
        Map<String, Object> response = new HashMap<>();
        response.put("key", key);
        response.put("action", "invalidated");
        response.put("durationMs", duration);
        
        return response;
    }

    /**
     * 对比不同缓存模式的性能
     * 
     * 示例请求：
     * GET /test/cache/compare?key=test:performance
     * 
     * @param key 缓存键（会自动加后缀区分不同模式）
     * @return 性能对比结果
     */
    @GetMapping("/compare")
    public Map<String, Object> compareCacheModes(@RequestParam String key) {
        Map<String, Object> results = new HashMap<>();
        
        CacheMode[] modes = CacheMode.values();
        
        for (CacheMode mode : modes) {
            String modeKey = key + ":" + mode.name();
            
            // 第一次访问（缓存未命中）
            long start1 = System.nanoTime();
            DispatchDecision decision = DispatchDecision.builder()
                    .cacheMode(mode)
                    .ttlLevel(CacheTtlLevel.NORMAL)
                    .build();
            
            cacheAccessProxy.access(modeKey, () -> "value-" + mode.name(), decision);
            long duration1 = System.nanoTime() - start1;
            
            // 第二次访问（应该命中缓存，除了 NONE）
            long start2 = System.nanoTime();
            cacheAccessProxy.access(modeKey, () -> "value-" + mode.name(), decision);
            long duration2 = System.nanoTime() - start2;
            
            Map<String, Object> modeResult = new HashMap<>();
            modeResult.put("firstAccessNs", duration1);
            modeResult.put("secondAccessNs", duration2);
            modeResult.put("speedup", duration2 > 0 ? String.format("%.2fx", duration1 / (double) duration2) : "N/A");
            
            results.put(mode.name(), modeResult);
        }
        
        return results;
    }

    /**
     * 测试并发访问
     * 
     * 示例请求：
     * GET /test/cache/concurrent?key=product:hot&threads=5&iterations=20
     * 
     * @param key        缓存键
     * @param threads    并发线程数
     * @param iterations 每个线程的迭代次数
     * @return 并发测试结果
     */
    @GetMapping("/concurrent")
    public Map<String, Object> testConcurrentAccess(
            @RequestParam String key,
            @RequestParam(defaultValue = "5") int threads,
            @RequestParam(defaultValue = "20") int iterations) throws InterruptedException {
        
        int dbCallsBefore = dbAccessCount.get();
        long startTime = System.currentTimeMillis();
        
        DispatchDecision decision = DispatchDecision.builder()
                .cacheMode(CacheMode.LOCAL_AND_REMOTE)
                .ttlLevel(CacheTtlLevel.NORMAL)
                .build();
        
        Thread[] threadArray = new Thread[threads];
        
        for (int t = 0; t < threads; t++) {
            final int threadId = t;
            threadArray[t] = new Thread(() -> {
                for (int i = 0; i < iterations; i++) {
                    final int iteration = i;
                    cacheAccessProxy.access(key, () -> {
                        dbAccessCount.incrementAndGet();
                        return "db-value-thread" + threadId + "-iter" + iteration;
                    }, decision);
                }
            });
            threadArray[t].start();
        }
        
        for (Thread thread : threadArray) {
            thread.join();
        }
        
        long duration = System.currentTimeMillis() - startTime;
        int dbCallsAfter = dbAccessCount.get();
        int actualDbCalls = dbCallsAfter - dbCallsBefore;
        int totalAccess = threads * iterations;
        
        Map<String, Object> response = new HashMap<>();
        response.put("key", key);
        response.put("threads", threads);
        response.put("iterationsPerThread", iterations);
        response.put("totalAccess", totalAccess);
        response.put("dbCalls", actualDbCalls);
        response.put("cacheHits", totalAccess - actualDbCalls);
        response.put("hitRate", String.format("%.2f%%", (totalAccess - actualDbCalls) * 100.0 / totalAccess));
        response.put("durationMs", duration);
        response.put("qps", Math.round(totalAccess * 1000.0 / duration));
        
        return response;
    }

    /**
     * 重置 DB 访问计数器
     */
    @PostMapping("/reset")
    public Map<String, Object> resetCounter() {
        int oldValue = dbAccessCount.getAndSet(0);
        
        Map<String, Object> response = new HashMap<>();
        response.put("previousCount", oldValue);
        response.put("currentCount", 0);
        
        return response;
    }
}
