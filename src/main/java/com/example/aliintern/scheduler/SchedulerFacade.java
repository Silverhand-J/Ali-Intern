package com.example.aliintern.scheduler;

import com.example.aliintern.scheduler.cache.CacheAccessProxy;
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

import java.util.function.Supplier;

/**
 * 调度层核心门面类
 * 统一调度四大核心模块：访问统计 -> 热点识别 -> 策略决策 -> 缓存访问
 * 
 * 标准处理流程：
 * 1. 访问统计：记录访问频次，返回双窗口计数
 * 2. 热点识别：根据统计结果判断热度等级
 * 3. 策略决策：基于热度等级生成缓存策略
 * 4. 缓存访问：根据策略执行多级缓存访问
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulerFacade {

    private final AccessStatisticsService accessStatisticsService;
    private final HotspotDetector hotspotDetector;
    private final DecisionStrategyEngine decisionStrategyEngine;
    private final CacheAccessProxy cacheAccessProxy;

    /**
     * 处理请求（完整流程）
     * 核心调度流程：访问统计 -> 热点识别 -> 策略决策 -> 缓存访问
     *
     * @param context  请求上下文
     * @param dbLoader 数据库回源函数（仅在缓存未命中时调用）
     * @param <T>      返回值类型
     * @return 数据（可能来自缓存或 DB）
     */
    public <T> T process(RequestContext context, Supplier<T> dbLoader) {
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
        log.debug("Decision made: cacheMode={}, ttlLevel={}", 
                decision.getCacheMode(), decision.getTtlLevel());

        // 4. 缓存访问：根据策略执行多级缓存访问
        T result = cacheAccessProxy.access(context.getCacheKey(), dbLoader, decision);
        log.info("Request {} completed, hotspot={}, cacheMode={}", 
                context.getRequestId(), hotspotLevel, decision.getCacheMode());

        return result;
    }

    /**
     * 使缓存失效
     * 
     * @param key 缓存键
     */
    public void invalidateCache(String key) {
        log.info("Invalidating cache for key: {}", key);
        cacheAccessProxy.invalidate(key);
    }
}
