package com.example.aliintern.scheduler.strategy.impl;

import com.example.aliintern.scheduler.common.enums.CacheMode;
import com.example.aliintern.scheduler.common.enums.CacheTtlLevel;
import com.example.aliintern.scheduler.common.enums.HotspotLevel;
import com.example.aliintern.scheduler.common.model.DispatchDecision;
import com.example.aliintern.scheduler.config.SchedulerProperties;
import com.example.aliintern.scheduler.strategy.DecisionStrategyEngine;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.EnumMap;

/**
 * 决策策略引擎默认实现
 * 
 * 核心设计：
 * - 使用 EnumMap 做 O(1) 查找
 * - 启动时根据配置一次性初始化策略映射表
 * - 纯内存操作，无 IO，线程安全
 * 
 * 职责边界：
 * - 只做 HotspotLevel → DispatchDecision 映射
 * - 不操作 Redis / 本地缓存
 * - 不执行限流 / 降级 / 异步刷新
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultDecisionStrategyEngine implements DecisionStrategyEngine {

    private final SchedulerProperties schedulerProperties;

    /**
     * 策略映射表（启动时初始化，运行时只读）
     */
    private EnumMap<HotspotLevel, DispatchDecision> strategyMap;

    @PostConstruct
    public void init() {
        strategyMap = new EnumMap<>(HotspotLevel.class);

        SchedulerProperties.StrategyConfig config = schedulerProperties.getStrategy();

        // 初始化各级别策略
        strategyMap.put(HotspotLevel.COLD, buildDecision(
                config.getColdCacheMode(),
                config.getColdTtlLevel()
        ));

        strategyMap.put(HotspotLevel.WARM, buildDecision(
                config.getWarmCacheMode(),
                config.getWarmTtlLevel()
        ));

        strategyMap.put(HotspotLevel.HOT, buildDecision(
                config.getHotCacheMode(),
                config.getHotTtlLevel()
        ));

        strategyMap.put(HotspotLevel.EXTREMELY_HOT, buildDecision(
                config.getExtremelyHotCacheMode(),
                config.getExtremelyHotTtlLevel()
        ));

        log.info("DecisionStrategyEngine initialized with strategy map: {}", strategyMap);
    }

    @Override
    public DispatchDecision decide(HotspotLevel level) {
        if (level == null) {
            log.warn("HotspotLevel is null, using COLD as default");
            level = HotspotLevel.COLD;
        }

        DispatchDecision decision = strategyMap.get(level);
        log.debug("Decision for level {}: {}", level, decision);
        
        return decision;
    }

    /**
     * 根据配置字符串构建决策对象
     */
    private DispatchDecision buildDecision(String cacheModeStr, String ttlLevelStr) {
        CacheMode cacheMode;
        CacheTtlLevel ttlLevel;

        try {
            cacheMode = CacheMode.valueOf(cacheModeStr);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid cacheMode: {}, using NONE as default", cacheModeStr);
            cacheMode = CacheMode.NONE;
        }

        try {
            ttlLevel = CacheTtlLevel.valueOf(ttlLevelStr);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid ttlLevel: {}, using SHORT as default", ttlLevelStr);
            ttlLevel = CacheTtlLevel.SHORT;
        }

        return DispatchDecision.of(cacheMode, ttlLevel);
    }
}
