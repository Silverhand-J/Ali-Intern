package com.example.aliintern.scheduler.controller;

import com.example.aliintern.scheduler.common.enums.HotspotLevel;
import com.example.aliintern.scheduler.common.model.DispatchDecision;
import com.example.aliintern.scheduler.common.model.StatResult;
import com.example.aliintern.scheduler.hotspot.HotspotDetector;
import com.example.aliintern.scheduler.statistics.AccessStatisticsService;
import com.example.aliintern.scheduler.strategy.DecisionStrategyEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 策略决策引擎测试接口
 * 仅用于开发测试，生产环境应移除或限制访问
 */
@Slf4j
@RestController
@RequestMapping("/test/strategy")
@RequiredArgsConstructor
public class StrategyTestController {

    private final AccessStatisticsService statisticsService;
    private final HotspotDetector hotspotDetector;
    private final DecisionStrategyEngine decisionStrategyEngine;

    /**
     * 完整流程测试：访问统计 -> 热点识别 -> 策略决策
     * 
     * 示例请求：
     * GET /test/strategy/full?bizType=product&bizKey=12345
     * 
     * @param bizType 业务类型
     * @param bizKey  业务键
     * @return 完整流程结果
     */
    @GetMapping("/full")
    public Map<String, Object> testFullProcess(
            @RequestParam String bizType,
            @RequestParam String bizKey) {
        
        log.info("测试完整流程: bizType={}, bizKey={}", bizType, bizKey);
        
        // 1. 访问统计
        StatResult stat = statisticsService.record(bizType, bizKey);
        
        // 2. 热点识别
        HotspotLevel level = hotspotDetector.detect(stat);
        
        // 3. 策略决策
        DispatchDecision decision = decisionStrategyEngine.decide(level);
        
        Map<String, Object> response = new HashMap<>();
        response.put("bizType", bizType);
        response.put("bizKey", bizKey);
        response.put("statResult", Map.of(
                "countShort", stat.getCount1s(),
                "countLong", stat.getCount60s()
        ));
        response.put("hotspotLevel", level.name());
        response.put("decision", Map.of(
                "cacheMode", decision.getCacheMode().name(),
                "ttlLevel", decision.getTtlLevel().name()
        ));
        
        return response;
    }

    /**
     * 直接测试策略决策（指定热点等级）
     * 
     * 示例请求：
     * GET /test/strategy/decide?level=HOT
     * 
     * @param level 热点等级（COLD/WARM/HOT/EXTREMELY_HOT）
     * @return 策略决策结果
     */
    @GetMapping("/decide")
    public Map<String, Object> testDecide(@RequestParam String level) {
        log.info("测试策略决策: level={}", level);
        
        HotspotLevel hotspotLevel;
        try {
            hotspotLevel = HotspotLevel.valueOf(level.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("无效的热点等级: {}", level);
            hotspotLevel = HotspotLevel.COLD;
        }
        
        DispatchDecision decision = decisionStrategyEngine.decide(hotspotLevel);
        
        Map<String, Object> response = new HashMap<>();
        response.put("inputLevel", hotspotLevel.name());
        response.put("cacheMode", decision.getCacheMode().name());
        response.put("ttlLevel", decision.getTtlLevel().name());
        response.put("description", getDecisionDescription(decision));
        
        return response;
    }

    /**
     * 查看所有级别的策略映射
     * 
     * 示例请求：
     * GET /test/strategy/mappings
     * 
     * @return 所有热点等级对应的策略
     */
    @GetMapping("/mappings")
    public Map<String, Object> getStrategyMappings() {
        Map<String, Object> mappings = new HashMap<>();
        
        for (HotspotLevel level : HotspotLevel.values()) {
            DispatchDecision decision = decisionStrategyEngine.decide(level);
            mappings.put(level.name(), Map.of(
                    "cacheMode", decision.getCacheMode().name(),
                    "ttlLevel", decision.getTtlLevel().name(),
                    "description", getDecisionDescription(decision)
            ));
        }
        
        return mappings;
    }

    /**
     * 模拟不同热度场景
     * 
     * 示例请求：
     * GET /test/strategy/simulate?bizType=product&bizKey=12345&scenario=HOT
     * 
     * @param bizType  业务类型
     * @param bizKey   业务键
     * @param scenario 场景（COLD/WARM/HOT/EXTREMELY_HOT）
     * @return 模拟结果
     */
    @GetMapping("/simulate")
    public Map<String, Object> simulateScenario(
            @RequestParam String bizType,
            @RequestParam String bizKey,
            @RequestParam(defaultValue = "WARM") String scenario) {
        
        log.info("模拟场景: bizType={}, bizKey={}, scenario={}", bizType, bizKey, scenario);
        
        // 根据场景决定模拟访问次数
        int count = getSimulateCount(scenario);
        
        StatResult result = null;
        long startTime = System.currentTimeMillis();
        
        // 批量记录访问
        for (int i = 0; i < count; i++) {
            result = statisticsService.record(bizType, bizKey);
        }
        
        long duration = System.currentTimeMillis() - startTime;
        
        // 识别热点等级
        HotspotLevel level = hotspotDetector.detect(result);
        
        // 生成策略决策
        DispatchDecision decision = decisionStrategyEngine.decide(level);
        
        Map<String, Object> response = new HashMap<>();
        response.put("scenario", scenario);
        response.put("simulateCount", count);
        response.put("bizType", bizType);
        response.put("bizKey", bizKey);
        response.put("statResult", Map.of(
                "countShort", result != null ? result.getCount1s() : 0,
                "countLong", result != null ? result.getCount60s() : 0
        ));
        response.put("detectedLevel", level.name());
        response.put("decision", Map.of(
                "cacheMode", decision.getCacheMode().name(),
                "ttlLevel", decision.getTtlLevel().name(),
                "description", getDecisionDescription(decision)
        ));
        response.put("matched", level.name().equals(scenario.toUpperCase()));
        response.put("durationMs", duration);
        
        return response;
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取决策描述
     */
    private String getDecisionDescription(DispatchDecision decision) {
        return switch (decision.getCacheMode()) {
            case NONE -> "不使用缓存，直接回源";
            case LOCAL_ONLY -> "仅使用本地缓存（L1）";
            case REMOTE_ONLY -> "仅使用 Redis（L2），短期缓存";
            case LOCAL_AND_REMOTE -> "本地缓存 + Redis 双层缓存，" + 
                    (decision.getTtlLevel().name().equals("LONG") ? "长 TTL" : "正常 TTL");
        };
    }

    /**
     * 根据场景获取模拟访问次数
     */
    private int getSimulateCount(String scenario) {
        return switch (scenario.toUpperCase()) {
            case "COLD" -> 3;           // 低于 WARM 阈值（5）
            case "WARM" -> 10;          // 达到 WARM 阈值（5）但低于 HOT（20）
            case "HOT" -> 25;           // 达到 HOT 阈值（20）但低于 EXTREMELY_HOT（100）
            case "EXTREMELY_HOT" -> 150; // 达到 EXTREMELY_HOT 阈值（100）
            default -> 10;
        };
    }
}
