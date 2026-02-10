package com.example.aliintern.scheduler.common.model;

import com.example.aliintern.scheduler.common.enums.CacheMode;
import com.example.aliintern.scheduler.common.enums.CacheTtlLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 策略决策结果
 * 
 * 职责：
 * - 描述缓存行为意图（不涉及具体执行）
 * - 只包含缓存模式和 TTL 等级
 * 
 * 严格约束：
 * - 不包含具体 TTL 秒数
 * - 不包含限流/降级/异步刷新等执行细节
 * - 不感知 Redis/本地缓存 API
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DispatchDecision {

    /**
     * 缓存使用模式
     * 决定使用哪一层缓存（本地/远程/双层/不缓存）
     */
    private CacheMode cacheMode;

    /**
     * 缓存 TTL 等级
     * 只表达"付出多少缓存成本"，具体秒数由配置决定
     */
    private CacheTtlLevel ttlLevel;

    /**
     * 创建一个"不缓存"的决策
     */
    public static DispatchDecision noCache() {
        return DispatchDecision.builder()
                .cacheMode(CacheMode.NONE)
                .ttlLevel(CacheTtlLevel.SHORT)
                .build();
    }

    /**
     * 创建决策
     */
    public static DispatchDecision of(CacheMode cacheMode, CacheTtlLevel ttlLevel) {
        return DispatchDecision.builder()
                .cacheMode(cacheMode)
                .ttlLevel(ttlLevel)
                .build();
    }
}
