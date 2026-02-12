package com.example.aliintern.scheduler.controller;

import com.example.aliintern.scheduler.SchedulerFacade;
import com.example.aliintern.scheduler.common.model.RequestContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 端到端测试接口
 * 完整测试调度层四大模块的集成流程
 */
@Slf4j
@RestController
@RequestMapping("/test/e2e")
@RequiredArgsConstructor
public class EndToEndTestController {

    private final SchedulerFacade schedulerFacade;
    
    // 模拟 DB 访问计数器
    private final AtomicInteger dbAccessCount = new AtomicInteger(0);
    
    // 模拟数据库
    private final Map<String, String> mockDatabase = new HashMap<>();

    /**
     * 端到端测试：完整的调度流程（含缓存访问）
     * 
     * 测试流程：
     * 1. 访问统计 -> 2. 热点识别 -> 3. 策略决策 -> 4. 缓存访问
     * 
     * 示例请求：
     * GET /test/e2e/full?bizType=product&bizKey=12345
     * 
     * @param bizType 业务类型
     * @param bizKey  业务键
     * @return 完整流程结果
     */
    @GetMapping("/full")
    public Map<String, Object> testFullFlow(
            @RequestParam String bizType,
            @RequestParam String bizKey) {
        
        long startTime = System.currentTimeMillis();
        int dbCallsBefore = dbAccessCount.get();
        
        // 构建请求上下文
        RequestContext context = RequestContext.builder()
                .requestId(UUID.randomUUID().toString())
                .cacheKey(bizKey)
                .build();
        
        // 调用完整流程
        String result = schedulerFacade.process(context, () -> {
            // 模拟 DB 回源
            int dbCalls = dbAccessCount.incrementAndGet();
            log.info("DB 回源: bizType={}, bizKey={}, dbCalls={}", bizType, bizKey, dbCalls);
            
            // 模拟 DB 查询延迟
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // 从模拟数据库获取数据
            String value = mockDatabase.computeIfAbsent(bizKey, k -> "db-value-" + k + "-" + System.currentTimeMillis());
            return value;
        });
        
        long duration = System.currentTimeMillis() - startTime;
        int dbCallsAfter = dbAccessCount.get();
        
        Map<String, Object> response = new HashMap<>();
        response.put("bizType", bizType);
        response.put("bizKey", bizKey);
        response.put("value", result);
        response.put("hotspotLevel", context.getHotspotLevel() != null ? context.getHotspotLevel().name() : null);
        response.put("dbCalled", dbCallsAfter > dbCallsBefore);
        response.put("totalDbCalls", dbCallsAfter);
        response.put("durationMs", duration);
        
        return response;
    }

    /**
     * 模拟热点场景测试
     * 
     * 通过连续访问同一个 key，模拟从 COLD -> WARM -> HOT -> EXTREMELY_HOT 的升级过程
     * 
     * 示例请求：
     * GET /test/e2e/hotspot-evolution?bizType=product&bizKey=99999
     * 
     * @param bizType 业务类型
     * @param bizKey  业务键
     * @return 热点演化过程
     */
    @GetMapping("/hotspot-evolution")
    public Map<String, Object> testHotspotEvolution(
            @RequestParam String bizType,
            @RequestParam String bizKey) {
        
        long startTime = System.currentTimeMillis();
        List<Map<String, Object>> evolution = new ArrayList<>();
        
        // 访问次数：5, 10, 25, 50, 150（模拟从冷到极热的过程）
        int[] accessCounts = {5, 10, 25, 50, 150};
        
        for (int targetCount : accessCounts) {
            RequestContext context = RequestContext.builder()
                    .requestId(UUID.randomUUID().toString())
                    .cacheKey(bizKey)
                    .build();
            
            // 连续访问
            for (int i = 0; i < targetCount; i++) {
                schedulerFacade.process(context, () -> {
                    dbAccessCount.incrementAndGet();
                    return "value-" + bizKey;
                });
            }
            
            Map<String, Object> snapshot = new HashMap<>();
            snapshot.put("accessCount", targetCount);
            snapshot.put("hotspotLevel", context.getHotspotLevel() != null ? context.getHotspotLevel().name() : "UNKNOWN");
            
            evolution.add(snapshot);
        }
        
        long duration = System.currentTimeMillis() - startTime;
        
        Map<String, Object> response = new HashMap<>();
        response.put("bizType", bizType);
        response.put("bizKey", bizKey);
        response.put("evolution", evolution);
        response.put("durationMs", duration);
        
        return response;
    }

    /**
     * 缓存命中率测试
     * 
     * 连续访问同一个 key，观察缓存效果
     * 
     * 示例请求：
     * GET /test/e2e/cache-hit-rate?bizType=product&bizKey=88888&count=100
     * 
     * @param bizType 业务类型
     * @param bizKey  业务键
     * @param count   访问次数
     * @return 缓存命中率统计
     */
    @GetMapping("/cache-hit-rate")
    public Map<String, Object> testCacheHitRate(
            @RequestParam String bizType,
            @RequestParam String bizKey,
            @RequestParam(defaultValue = "100") int count) {
        
        long startTime = System.currentTimeMillis();
        int dbCallsBefore = dbAccessCount.get();
        
        RequestContext context = RequestContext.builder()
                .requestId(UUID.randomUUID().toString())
                .cacheKey(bizKey)
                .build();
        
        // 连续访问
        String firstValue = null;
        for (int i = 0; i < count; i++) {
            String value = schedulerFacade.process(context, () -> {
                dbAccessCount.incrementAndGet();
                return "db-value-" + bizKey + "-" + System.currentTimeMillis();
            });
            
            if (firstValue == null) {
                firstValue = value;
            }
        }
        
        long duration = System.currentTimeMillis() - startTime;
        int dbCallsAfter = dbAccessCount.get();
        int actualDbCalls = dbCallsAfter - dbCallsBefore;
        
        Map<String, Object> response = new HashMap<>();
        response.put("bizType", bizType);
        response.put("bizKey", bizKey);
        response.put("totalAccess", count);
        response.put("dbCalls", actualDbCalls);
        response.put("cacheHits", count - actualDbCalls);
        response.put("hitRate", String.format("%.2f%%", (count - actualDbCalls) * 100.0 / count));
        response.put("hotspotLevel", context.getHotspotLevel() != null ? context.getHotspotLevel().name() : "UNKNOWN");
        response.put("durationMs", duration);
        response.put("avgLatencyMs", String.format("%.2f", duration / (double) count));
        
        return response;
    }

    /**
     * 并发访问测试
     * 
     * 多线程并发访问同一个 key，测试并发场景下的调度表现
     * 
     * 示例请求：
     * GET /test/e2e/concurrent?bizType=product&bizKey=77777&threads=10&iterations=50
     * 
     * @param bizType    业务类型
     * @param bizKey     业务键
     * @param threads    并发线程数
     * @param iterations 每个线程的迭代次数
     * @return 并发测试结果
     */
    @GetMapping("/concurrent")
    public Map<String, Object> testConcurrentAccess(
            @RequestParam String bizType,
            @RequestParam String bizKey,
            @RequestParam(defaultValue = "10") int threads,
            @RequestParam(defaultValue = "50") int iterations) throws InterruptedException {
        
        long startTime = System.currentTimeMillis();
        int dbCallsBefore = dbAccessCount.get();
        
        Thread[] threadArray = new Thread[threads];
        
        for (int t = 0; t < threads; t++) {
            final int threadId = t;
            threadArray[t] = new Thread(() -> {
                for (int i = 0; i < iterations; i++) {
                    RequestContext context = RequestContext.builder()
                            .requestId(UUID.randomUUID().toString())
                            .cacheKey(bizKey)
                            .build();
                    
                    schedulerFacade.process(context, () -> {
                        dbAccessCount.incrementAndGet();
                        return "db-value-" + bizKey + "-thread" + threadId;
                    });
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
        response.put("bizType", bizType);
        response.put("bizKey", bizKey);
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
     * 缓存失效测试
     * 
     * 示例请求：
     * DELETE /test/e2e/invalidate?bizKey=12345
     * 
     * @param bizKey 业务键
     * @return 失效结果
     */
    @DeleteMapping("/invalidate")
    public Map<String, Object> testInvalidate(@RequestParam String bizKey) {
        long startTime = System.currentTimeMillis();
        
        schedulerFacade.invalidateCache(bizKey);
        
        // 同时清除模拟数据库
        mockDatabase.remove(bizKey);
        
        long duration = System.currentTimeMillis() - startTime;
        
        Map<String, Object> response = new HashMap<>();
        response.put("bizKey", bizKey);
        response.put("action", "invalidated");
        response.put("durationMs", duration);
        
        return response;
    }

    /**
     * 重置所有状态
     * 
     * 示例请求：
     * POST /test/e2e/reset
     */
    @PostMapping("/reset")
    public Map<String, Object> reset() {
        int oldDbCount = dbAccessCount.getAndSet(0);
        int oldDbSize = mockDatabase.size();
        mockDatabase.clear();
        
        Map<String, Object> response = new HashMap<>();
        response.put("previousDbCalls", oldDbCount);
        response.put("previousDbSize", oldDbSize);
        response.put("status", "reset");
        
        return response;
    }

    private RequestContext buildContext(String bizType, String bizKey) {
        return RequestContext.builder()
                .requestId(UUID.randomUUID().toString())
                .cacheKey(bizKey)
                .build();
    }
}
