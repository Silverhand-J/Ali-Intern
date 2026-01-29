package com.example.aliintern.scheduler.hotspot.impl;

import com.example.aliintern.scheduler.common.enums.HotspotLevel;
import com.example.aliintern.scheduler.hotspot.HotspotDetector;
import com.example.aliintern.scheduler.statistics.AccessStatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 热点识别模块默认实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultHotspotDetector implements HotspotDetector {

    private final AccessStatisticsService accessStatisticsService;

    // 热点阈值配置（每分钟访问次数）
    private static final long WARM_THRESHOLD = 10;
    private static final long HOT_THRESHOLD = 100;
    private static final long EXTREMELY_HOT_THRESHOLD = 1000;

    @Override
    public HotspotLevel detectHotspotLevel(String cacheKey) {
        Long accessCount = accessStatisticsService.getAccessCount(cacheKey);
        log.debug("Detecting hotspot level for key: {}, accessCount: {}", cacheKey, accessCount);

        if (accessCount >= EXTREMELY_HOT_THRESHOLD) {
            return HotspotLevel.EXTREMELY_HOT;
        } else if (accessCount >= HOT_THRESHOLD) {
            return HotspotLevel.HOT;
        } else if (accessCount >= WARM_THRESHOLD) {
            return HotspotLevel.WARM;
        } else {
            return HotspotLevel.COLD;
        }
    }

    @Override
    public boolean isHotspot(String cacheKey) {
        HotspotLevel level = detectHotspotLevel(cacheKey);
        return level == HotspotLevel.HOT || level == HotspotLevel.EXTREMELY_HOT;
    }

    @Override
    public Long getThreshold(HotspotLevel level) {
        return switch (level) {
            case COLD -> 0L;
            case WARM -> WARM_THRESHOLD;
            case HOT -> HOT_THRESHOLD;
            case EXTREMELY_HOT -> EXTREMELY_HOT_THRESHOLD;
        };
    }
}
