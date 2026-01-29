package com.example.aliintern.scheduler.common.model;

import com.example.aliintern.scheduler.common.enums.CacheStrategy;
import lombok.Builder;
import lombok.Data;

/**
 * 策略决策结果
 * 策略决策引擎输出的执行决策
 */
@Data
@Builder
public class PolicyDecision {

    /**
     * 缓存键
     */
    private String cacheKey;

    /**
     * 缓存策略
     */
    private CacheStrategy cacheStrategy;

    /**
     * 缓存TTL（秒）
     */
    private Long cacheTtl;

    /**
     * 是否需要限流
     */
    private Boolean rateLimitRequired;

    /**
     * 是否需要降级
     */
    private Boolean degradeRequired;

    /**
     * 降级策略描述
     */
    private String degradeStrategy;

    /**
     * 决策原因
     */
    private String reason;
}
