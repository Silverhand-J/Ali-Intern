package com.example.aliintern.scheduler.admission.impl;

import com.example.aliintern.scheduler.admission.CacheAdmissionControl;
import com.example.aliintern.scheduler.common.enums.HotspotLevel;
import com.example.aliintern.scheduler.common.model.RequestContext;
import com.example.aliintern.scheduler.statistics.AccessStatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 缓存准入控制默认实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultCacheAdmissionControl implements CacheAdmissionControl {

    private final AccessStatisticsService accessStatisticsService;

    // 最小访问次数阈值，低于此值不允许缓存
    private static final long MIN_ACCESS_THRESHOLD = 5;

    // 稳定热点判断的时间窗口数量
    private static final int STABLE_WINDOW_COUNT = 3;

    @Override
    public boolean allowCacheWrite(RequestContext context) {
        // 极热数据直接允许
        if (context.getHotspotLevel() == HotspotLevel.EXTREMELY_HOT) {
            return true;
        }

        // 检查访问次数是否达到阈值
        Long accessCount = accessStatisticsService.getAccessCount(context.getCacheKey());
        boolean allowed = accessCount >= MIN_ACCESS_THRESHOLD;

        log.debug("Cache admission for key {}: allowed={}, accessCount={}",
                context.getCacheKey(), allowed, accessCount);
        return allowed;
    }

    @Override
    public Long adjustTtl(RequestContext context, Long originalTtl) {
        // 根据热点稳定性调整TTL
        if (isStableHotspot(context.getCacheKey())) {
            // 稳定热点给予更长TTL
            return originalTtl * 2;
        } else {
            // 临时访问给予较短TTL
            return Math.min(originalTtl, 10L);
        }
    }

    @Override
    public boolean isStableHotspot(String cacheKey) {
        // TODO: 实现稳定热点判断逻辑
        // 检查该key在多个时间窗口内是否持续保持高访问频率
        log.debug("Checking stable hotspot for key: {}", cacheKey);
        return false;
    }
}
