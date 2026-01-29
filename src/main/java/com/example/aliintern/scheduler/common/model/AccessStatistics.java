package com.example.aliintern.scheduler.common.model;

import lombok.Builder;
import lombok.Data;

/**
 * 访问统计数据
 * 用于记录某个key的访问频次信息
 */
@Data
@Builder
public class AccessStatistics {

    /**
     * 缓存键
     */
    private String cacheKey;

    /**
     * 当前时间窗口内的访问次数
     */
    private Long accessCount;

    /**
     * 时间窗口开始时间
     */
    private Long windowStartTime;

    /**
     * 时间窗口大小（毫秒）
     */
    private Long windowSize;

    /**
     * 最近一次访问时间
     */
    private Long lastAccessTime;

    /**
     * 历史平均访问频率
     */
    private Double averageAccessRate;
}
