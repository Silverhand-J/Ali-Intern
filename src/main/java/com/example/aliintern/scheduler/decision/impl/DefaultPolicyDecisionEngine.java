package com.example.aliintern.scheduler.decision.impl;

import com.example.aliintern.scheduler.common.enums.CacheStrategy;
import com.example.aliintern.scheduler.common.enums.HotspotLevel;
import com.example.aliintern.scheduler.common.enums.RequestType;
import com.example.aliintern.scheduler.common.model.PolicyDecision;
import com.example.aliintern.scheduler.common.model.RequestContext;
import com.example.aliintern.scheduler.decision.PolicyDecisionEngine;
import com.example.aliintern.scheduler.hotspot.HotspotDetector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 策略决策引擎默认实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultPolicyDecisionEngine implements PolicyDecisionEngine {

    private final HotspotDetector hotspotDetector;

    @Override
    public PolicyDecision makeDecision(RequestContext context) {
        log.debug("Making decision for request: {}", context.getRequestId());

        // 获取热点等级
        HotspotLevel hotspotLevel = hotspotDetector.detectHotspotLevel(context.getCacheKey());
        context.setHotspotLevel(hotspotLevel);

        // 根据请求类型和热点等级决定缓存策略
        CacheStrategy strategy = determineCacheStrategy(context.getRequestType(), hotspotLevel);
        Long ttl = calculateRecommendedTtl(context);

        return PolicyDecision.builder()
                .cacheKey(context.getCacheKey())
                .cacheStrategy(strategy)
                .cacheTtl(ttl)
                .rateLimitRequired(requiresRateLimit(context))
                .degradeRequired(false)
                .reason(buildDecisionReason(context.getRequestType(), hotspotLevel, strategy))
                .build();
    }

    @Override
    public Long calculateRecommendedTtl(RequestContext context) {
        HotspotLevel level = context.getHotspotLevel();
        if (level == null) {
            level = hotspotDetector.detectHotspotLevel(context.getCacheKey());
        }

        return switch (level) {
            case COLD -> 0L;
            case WARM -> 30L;
            case HOT -> 60L;
            case EXTREMELY_HOT -> 300L;
        };
    }

    @Override
    public boolean requiresRateLimit(RequestContext context) {
        // 强一致性写请求在高负载时可能需要限流
        return context.getRequestType() == RequestType.STRONG_CONSISTENCY_WRITE
                && context.getHotspotLevel() == HotspotLevel.EXTREMELY_HOT;
    }

    private CacheStrategy determineCacheStrategy(RequestType requestType, HotspotLevel hotspotLevel) {
        // 强一致性写请求跳过缓存
        if (requestType == RequestType.STRONG_CONSISTENCY_WRITE) {
            return CacheStrategy.SKIP_CACHE;
        }

        // 根据热点等级决定缓存策略
        return switch (hotspotLevel) {
            case COLD -> CacheStrategy.SKIP_CACHE;
            case WARM -> CacheStrategy.CACHE_FIRST;
            case HOT -> CacheStrategy.CACHE_FIRST;
            case EXTREMELY_HOT -> CacheStrategy.CACHE_ONLY;
        };
    }

    private String buildDecisionReason(RequestType requestType, HotspotLevel hotspotLevel, CacheStrategy strategy) {
        return String.format("RequestType=%s, HotspotLevel=%s -> Strategy=%s",
                requestType, hotspotLevel, strategy);
    }
}
