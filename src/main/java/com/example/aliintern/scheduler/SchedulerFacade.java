package com.example.aliintern.scheduler;

import com.example.aliintern.scheduler.common.enums.HotspotLevel;
import com.example.aliintern.scheduler.common.model.DispatchDecision;
import com.example.aliintern.scheduler.common.model.RequestContext;
import com.example.aliintern.scheduler.common.model.StatResult;
import com.example.aliintern.scheduler.hotspot.HotspotDetector;
import com.example.aliintern.scheduler.statistics.AccessStatisticsService;
import com.example.aliintern.scheduler.strategy.DecisionStrategyEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 调度层核心门面类
 * 统一调度三大核心模块：访问统计 -> 热点识别 -> 策略决策
 * 
 * 标准处理流程：
 * 1. 访问统计：记录访问频次，返回双窗口计数
 * 2. 热点识别：根据统计结果判断热度等级
 * 3. 策略决策：基于热度等级生成缓存策略
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulerFacade {

    private final AccessStatisticsService accessStatisticsService;
    private final HotspotDetector hotspotDetector;
    private final DecisionStrategyEngine decisionStrategyEngine;

    /**
     * 处理请求
     * 核心调度流程：访问统计 -> 热点识别 -> 策略决策
     *
     * @param context 请求上下文
     * @return 策略决策结果
     */
    public DispatchDecision process(RequestContext context) {
        log.info("Processing request: {}", context.getRequestId());

        // 1. 访问统计：记录访问频次，获取双窗口统计结果
        StatResult stat = accessStatisticsService.record(
                context.getRequestType().name(), 
                context.getCacheKey()
        );
        log.debug("Access recorded for key: {}, countShort={}, countLong={}", 
                context.getCacheKey(), stat.getCount1s(), stat.getCount60s());

        // 2. 热点识别：根据统计结果判断热点等级
        HotspotLevel hotspotLevel = hotspotDetector.detect(stat);
        context.setHotspotLevel(hotspotLevel);
        log.debug("Hotspot level detected: {}", hotspotLevel);

        // 3. 策略决策：基于热度等级生成缓存策略
        DispatchDecision decision = decisionStrategyEngine.decide(hotspotLevel);
        log.info("Request {} processed, cacheMode: {}, ttlLevel: {}",
                context.getRequestId(), decision.getCacheMode(), decision.getTtlLevel());

        return decision;
    }
}
