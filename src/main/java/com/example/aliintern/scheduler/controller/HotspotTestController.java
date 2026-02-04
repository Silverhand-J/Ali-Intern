package com.example.aliintern.scheduler.controller;

import com.example.aliintern.scheduler.common.enums.HotspotLevel;
import com.example.aliintern.scheduler.common.model.StatResult;
import com.example.aliintern.scheduler.hotspot.HotspotDetector;
import com.example.aliintern.scheduler.statistics.AccessStatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 热点识别测试接口
 * 仅用于开发测试，生产环境应移除或限制访问
 */
@Slf4j
@RestController
@RequestMapping("/test/hotspot")
@RequiredArgsConstructor
public class HotspotTestController {

    private final AccessStatisticsService statisticsService;
    private final HotspotDetector hotspotDetector;

    /**
     * 综合测试：访问统计 + 热点识别
     * 
     * 示例请求：
     * GET /test/hotspot/detect?bizType=product&bizKey=12345
     * 
     * @param bizType 业务类型
     * @param bizKey  业务键
     * @return 统计结果 + 热点等级
     */
    @GetMapping("/detect")
    public Map<String, Object> detectHotspot(
            @RequestParam String bizType,
            @RequestParam String bizKey) {
        
        log.info("测试热点检测: bizType={}, bizKey={}", bizType, bizKey);
        
        // 记录访问并获取统计
        StatResult stat = statisticsService.record(bizType, bizKey);
        
        // 识别热点等级
        HotspotLevel level = hotspotDetector.detect(stat);
        
        Map<String, Object> response = new HashMap<>();
        response.put("bizType", bizType);
        response.put("bizKey", bizKey);
        response.put("countShort", stat.getCount1s());
        response.put("countLong", stat.getCount60s());
        response.put("hotspotLevel", level.name());
        response.put("hotspotDescription", getHotspotDescription(level));
        response.put("threshold", hotspotDetector.getThreshold(level));
        
        return response;
    }

    /**
     * 模拟不同热度场景
     * 
     * 示例请求：
     * GET /test/hotspot/simulate?bizType=product&bizKey=12345&scenario=HOT
     * 
     * @param bizType  业务类型
     * @param bizKey   业务键
     * @param scenario 场景：COLD, WARM, HOT, EXTREMELY_HOT
     * @return 测试结果
     */
    @GetMapping("/simulate")
    public Map<String, Object> simulateHotspot(
            @RequestParam String bizType,
            @RequestParam String bizKey,
            @RequestParam(defaultValue = "WARM") String scenario) {
        
        log.info("模拟热点场景: bizType={}, bizKey={}, scenario={}", bizType, bizKey, scenario);
        
        // 根据场景决定访问次数
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
        
        Map<String, Object> response = new HashMap<>();
        response.put("scenario", scenario);
        response.put("simulateCount", count);
        response.put("bizType", bizType);
        response.put("bizKey", bizKey);
        response.put("countShort", result != null ? result.getCount1s() : 0);
        response.put("countLong", result != null ? result.getCount60s() : 0);
        response.put("detectedLevel", level.name());
        response.put("expectedLevel", scenario);
        response.put("matched", level.name().equals(scenario));
        response.put("durationMs", duration);
        
        return response;
    }

    /**
     * 查看当前热点阈值配置
     * 
     * 示例请求：
     * GET /test/hotspot/thresholds
     * 
     * @return 阈值配置信息
     */
    @GetMapping("/thresholds")
    public Map<String, Object> getThresholds() {
        Map<String, Object> response = new HashMap<>();
        
        response.put("COLD", Map.of(
                "threshold", hotspotDetector.getThreshold(HotspotLevel.COLD),
                "description", "冷数据，偶发访问"
        ));
        
        response.put("WARM", Map.of(
                "threshold", hotspotDetector.getThreshold(HotspotLevel.WARM),
                "description", "中等热度，稳定访问"
        ));
        
        response.put("HOT", Map.of(
                "threshold", hotspotDetector.getThreshold(HotspotLevel.HOT),
                "description", "高频热点"
        ));
        
        response.put("EXTREMELY_HOT", Map.of(
                "threshold", hotspotDetector.getThreshold(HotspotLevel.EXTREMELY_HOT),
                "description", "极热数据，突发流量"
        ));
        
        return response;
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取热点等级描述
     */
    private String getHotspotDescription(HotspotLevel level) {
        return switch (level) {
            case COLD -> "冷数据，偶发访问，无需缓存";
            case WARM -> "中等热度，稳定访问，建议缓存";
            case HOT -> "高频热点，强烈建议缓存";
            case EXTREMELY_HOT -> "极热数据，突发流量，必须缓存并特殊处理";
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
