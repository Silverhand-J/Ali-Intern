package com.example.aliintern.scheduler;

import com.example.aliintern.scheduler.admission.CacheAdmissionControl;
import com.example.aliintern.scheduler.classifier.RequestClassifier;
import com.example.aliintern.scheduler.common.enums.RequestType;
import com.example.aliintern.scheduler.common.model.PolicyDecision;
import com.example.aliintern.scheduler.common.model.RequestContext;
import com.example.aliintern.scheduler.decision.PolicyDecisionEngine;
import com.example.aliintern.scheduler.hotspot.HotspotDetector;
import com.example.aliintern.scheduler.limiter.DegradeService;
import com.example.aliintern.scheduler.limiter.RateLimiterService;
import com.example.aliintern.scheduler.statistics.AccessStatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 调度层核心门面类
 * 统一调度各模块，处理请求的完整生命周期
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulerFacade {

    private final RequestClassifier requestClassifier;
    private final AccessStatisticsService accessStatisticsService;
    private final HotspotDetector hotspotDetector;
    private final PolicyDecisionEngine policyDecisionEngine;
    private final CacheAdmissionControl cacheAdmissionControl;
    private final RateLimiterService rateLimiterService;
    private final DegradeService degradeService;

    /**
     * 处理请求
     * 完整的调度流程：分类 -> 统计 -> 热点识别 -> 策略决策 -> 准入控制
     *
     * @param context 请求上下文
     * @return 策略决策结果
     */
    public PolicyDecision process(RequestContext context) {
        log.info("Processing request: {}", context.getRequestId());

        // 1. 请求分类
        RequestType requestType = requestClassifier.classify(context);
        context.setRequestType(requestType);
        log.debug("Request classified as: {}", requestType);

        // 2. 检查是否需要降级
        if (degradeService.shouldDegrade(context)) {
            log.info("Request {} degraded", context.getRequestId());
            return buildDegradedDecision(context);
        }

        // 3. 检查是否被限流
        if (rateLimiterService.isRateLimited(context)) {
            log.info("Request {} rate limited", context.getRequestId());
            return buildRateLimitedDecision(context);
        }

        // 4. 访问统计（仅对需要统计的请求）
        if (requestClassifier.requiresStatistics(requestType)) {
            accessStatisticsService.recordAccess(context.getCacheKey());
        }

        // 5. 热点识别
        context.setHotspotLevel(hotspotDetector.detectHotspotLevel(context.getCacheKey()));

        // 6. 策略决策
        PolicyDecision decision = policyDecisionEngine.makeDecision(context);

        // 7. 缓存准入控制
        if (cacheAdmissionControl.allowCacheWrite(context)) {
            Long adjustedTtl = cacheAdmissionControl.adjustTtl(context, decision.getCacheTtl());
            decision.setCacheTtl(adjustedTtl);
            context.setCacheAllowed(true);
        } else {
            context.setCacheAllowed(false);
        }

        log.info("Request {} processed, decision: {}", context.getRequestId(), decision.getCacheStrategy());
        return decision;
    }

    private PolicyDecision buildDegradedDecision(RequestContext context) {
        return PolicyDecision.builder()
                .cacheKey(context.getCacheKey())
                .degradeRequired(true)
                .degradeStrategy("RETURN_CACHED_DATA")
                .reason("System overloaded, degrading request")
                .build();
    }

    private PolicyDecision buildRateLimitedDecision(RequestContext context) {
        return PolicyDecision.builder()
                .cacheKey(context.getCacheKey())
                .rateLimitRequired(true)
                .reason("Request rate limited")
                .build();
    }
}
