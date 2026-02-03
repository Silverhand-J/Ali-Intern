package com.example.aliintern.scheduler.controller;

import com.example.aliintern.scheduler.common.model.StatResult;
import com.example.aliintern.scheduler.statistics.AccessStatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 访问统计测试接口
 * 仅用于开发测试，生产环境应移除或限制访问
 */
@Slf4j
@RestController
@RequestMapping("/test/stat")
@RequiredArgsConstructor
public class StatTestController {

    private final AccessStatisticsService statisticsService;

    /**
     * 记录一次访问并返回统计结果
     * 
     * 示例请求：
     * GET /test/stat/record?bizType=product&bizKey=12345
     * 
     * @param bizType 业务类型
     * @param bizKey  业务键
     * @return 统计结果
     */
    @GetMapping("/record")
    public Map<String, Object> record(
            @RequestParam String bizType,
            @RequestParam String bizKey) {
        
        log.info("测试访问记录: bizType={}, bizKey={}", bizType, bizKey);
        
        StatResult result = statisticsService.record(bizType, bizKey);
        
        Map<String, Object> response = new HashMap<>();
        response.put("bizType", bizType);
        response.put("bizKey", bizKey);
        response.put("count1s", result.getCount1s());
        response.put("count60s", result.getCount60s());
        response.put("redisKey1s", String.format("stat:%s:%s:1s", bizType, bizKey));
        response.put("redisKey60s", String.format("stat:%s:%s:60s", bizType, bizKey));
        
        return response;
    }

    /**
     * 批量记录访问（模拟并发场景）
     * 
     * 示例请求：
     * GET /test/stat/batch?bizType=product&bizKey=12345&count=100
     * 
     * @param bizType 业务类型
     * @param bizKey  业务键
     * @param count   记录次数
     * @return 最终统计结果
     */
    @GetMapping("/batch")
    public Map<String, Object> batchRecord(
            @RequestParam String bizType,
            @RequestParam String bizKey,
            @RequestParam(defaultValue = "10") int count) {
        
        log.info("批量测试访问记录: bizType={}, bizKey={}, count={}", bizType, bizKey, count);
        
        StatResult result = null;
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < count; i++) {
            result = statisticsService.record(bizType, bizKey);
        }
        
        long duration = System.currentTimeMillis() - startTime;
        
        Map<String, Object> response = new HashMap<>();
        response.put("bizType", bizType);
        response.put("bizKey", bizKey);
        response.put("recordCount", count);
        response.put("count1s", result != null ? result.getCount1s() : 0);
        response.put("count60s", result != null ? result.getCount60s() : 0);
        response.put("durationMs", duration);
        response.put("avgLatencyMs", count > 0 ? (double) duration / count : 0);
        
        return response;
    }
}
