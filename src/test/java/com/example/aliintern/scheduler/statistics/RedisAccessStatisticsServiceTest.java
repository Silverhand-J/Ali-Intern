package com.example.aliintern.scheduler.statistics;

import com.example.aliintern.scheduler.common.model.StatResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 访问统计模块集成测试
 * 
 * 前提条件：需要本地运行 Redis 服务（localhost:6379）
 * 
 * 测试内容：
 * 1. 单次访问记录
 * 2. 多次访问累加
 * 3. 双窗口统计正确性
 * 4. 参数校验
 */
@SpringBootTest
class RedisAccessStatisticsServiceTest {

    @Autowired
    private AccessStatisticsService statisticsService;

    @Test
    @DisplayName("测试单次访问记录")
    void testSingleRecord() {
        // 使用唯一key避免测试间干扰
        String bizType = "test";
        String bizKey = "single_" + System.currentTimeMillis();

        StatResult result = statisticsService.record(bizType, bizKey);

        assertNotNull(result, "结果不应为null");
        assertEquals(1L, result.getCount1s(), "1秒窗口计数应为1");
        assertEquals(1L, result.getCount60s(), "60秒窗口计数应为1");
    }

    @Test
    @DisplayName("测试多次访问累加")
    void testMultipleRecords() {
        String bizType = "test";
        String bizKey = "multiple_" + System.currentTimeMillis();

        // 连续记录5次
        StatResult result = null;
        for (int i = 0; i < 5; i++) {
            result = statisticsService.record(bizType, bizKey);
        }

        assertNotNull(result);
        assertEquals(5L, result.getCount1s(), "1秒窗口计数应为5");
        assertEquals(5L, result.getCount60s(), "60秒窗口计数应为5");
    }

    @Test
    @DisplayName("测试不同业务类型隔离")
    void testBizTypeIsolation() {
        String bizKey = "isolation_" + System.currentTimeMillis();

        // 对不同业务类型记录
        StatResult productResult = statisticsService.record("product", bizKey);
        StatResult orderResult = statisticsService.record("order", bizKey);

        // 应该各自独立计数
        assertEquals(1L, productResult.getCount1s());
        assertEquals(1L, orderResult.getCount1s());
    }

    @Test
    @DisplayName("测试空参数处理")
    void testNullParameters() {
        StatResult result1 = statisticsService.record(null, "key");
        StatResult result2 = statisticsService.record("type", null);
        StatResult result3 = statisticsService.record("", "key");

        // 应返回空结果而不是抛异常
        assertEquals(0L, result1.getCount1s());
        assertEquals(0L, result2.getCount1s());
        assertEquals(0L, result3.getCount1s());
    }

    @Test
    @DisplayName("测试高并发场景模拟")
    void testConcurrentAccess() throws InterruptedException {
        String bizType = "test";
        String bizKey = "concurrent_" + System.currentTimeMillis();
        int threadCount = 10;
        int recordsPerThread = 100;

        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < recordsPerThread; j++) {
                    statisticsService.record(bizType, bizKey);
                }
            });
        }

        // 启动所有线程
        for (Thread thread : threads) {
            thread.start();
        }

        // 等待所有线程完成
        for (Thread thread : threads) {
            thread.join();
        }

        // 获取最终统计（可能部分在60秒窗口内）
        StatResult result = statisticsService.record(bizType, bizKey);
        
        // 60秒窗口应该累加所有记录（+1是最后一次record）
        long expectedTotal = threadCount * recordsPerThread + 1;
        assertEquals(expectedTotal, result.getCount60s(), 
                "60秒窗口应正确累加所有并发请求");
    }

    @Test
    @DisplayName("测试1秒窗口过期")
    void testWindowExpiration() throws InterruptedException {
        String bizType = "test";
        String bizKey = "expire_" + System.currentTimeMillis();

        // 第一次记录
        StatResult result1 = statisticsService.record(bizType, bizKey);
        assertEquals(1L, result1.getCount1s());

        // 等待超过1秒
        Thread.sleep(1100);

        // 第二次记录 - 1秒窗口应重新计数
        StatResult result2 = statisticsService.record(bizType, bizKey);
        assertEquals(1L, result2.getCount1s(), "1秒窗口过期后应重新从1开始");
        assertEquals(2L, result2.getCount60s(), "60秒窗口应继续累加");
    }
}
