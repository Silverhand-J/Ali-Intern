package com.example.aliintern.scheduler.hotspot.impl;

import com.example.aliintern.scheduler.common.enums.HotspotLevel;
import com.example.aliintern.scheduler.common.model.StatResult;
import com.example.aliintern.scheduler.config.SchedulerProperties;
import com.example.aliintern.scheduler.hotspot.HotspotDetector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 热点识别模块实现（基于双窗口阈值判断）
 * 
 * 识别策略：
 * 1. 瞬时热度（短窗口）用于识别突发热点
 * 2. 稳定热度（长窗口）用于识别长期热点
 * 3. 当多个条件命中时，取最高热度等级
 * 
 * 模块边界：
 * - 只读取 StatResult
 * - 只返回 HotspotLevel
 * - 不操作 Redis / MySQL
 * - 不感知业务类型
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultHotspotDetector implements HotspotDetector {

    private final SchedulerProperties schedulerProperties;

    /**
     * 核心方法：根据访问统计结果识别热点等级
     * 
     * 判断顺序（从高到低）：
     * 1. EXTREMELY_HOT: countShort >= 100 OR countLong >= 1000
     * 2. HOT: countShort >= 20 OR countLong >= 300
     * 3. WARM: countShort >= 5 OR countLong >= 60
     * 4. COLD: 其他情况
     */
    @Override
    public HotspotLevel detect(StatResult stat) {
        if (stat == null) {
            log.warn("StatResult 为空，返回 COLD");
            return HotspotLevel.COLD;
        }

        long count1s = stat.getCount1s() != null ? stat.getCount1s() : 0L;
        long count60s = stat.getCount60s() != null ? stat.getCount60s() : 0L;

        log.debug("热点检测输入: countShort={}, countLong={}", count1s, count60s);

        // 按优先级从高到低判断
        HotspotLevel level = doDetect(count1s, count60s);
        
        log.debug("热点检测结果: level={}", level);
        return level;
    }

    /**
     * 执行双窗口阈值判断
     */
    private HotspotLevel doDetect(long count1s, long count60s) {
        SchedulerProperties.HotspotConfig config = schedulerProperties.getHotspot();
        
        // EXTREMELY_HOT: 突发流量或超高频访问
        if (count1s >= config.getExtremelyHotShortThreshold() 
                || count60s >= config.getExtremelyHotLongThreshold()) {
            return HotspotLevel.EXTREMELY_HOT;
        }

        // HOT: 高频热点
        if (count1s >= config.getHotShortThreshold() 
                || count60s >= config.getHotLongThreshold()) {
            return HotspotLevel.HOT;
        }

        // WARM: 中等热度
        if (count1s >= config.getWarmShortThreshold() 
                || count60s >= config.getWarmLongThreshold()) {
            return HotspotLevel.WARM;
        }

        // COLD: 冷数据
        return HotspotLevel.COLD;
    }

    @Override
    public Long getThreshold(HotspotLevel level) {
        SchedulerProperties.HotspotConfig config = schedulerProperties.getHotspot();
        // 返回长窗口阈值作为参考值
        return switch (level) {
            case COLD -> 0L;
            case WARM -> config.getWarmLongThreshold();
            case HOT -> config.getHotLongThreshold();
            case EXTREMELY_HOT -> config.getExtremelyHotLongThreshold();
        };
    }
}
