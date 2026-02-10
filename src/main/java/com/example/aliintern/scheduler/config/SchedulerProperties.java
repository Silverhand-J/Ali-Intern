package com.example.aliintern.scheduler.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 调度层统一配置类
 * 
 * 集中管理所有调度层相关配置，禁止创建独立的模块配置类
 * 配置前缀：scheduler
 * 
 * 包含模块：
 * - 访问统计模块（stat）
 * - 热点识别模块（hotspot）
 * - 策略决策引擎（decision）
 */
@Data
@Component
@ConfigurationProperties(prefix = "scheduler")
public class SchedulerProperties {

    /**
     * 访问统计模块配置
     */
    private final StatConfig stat = new StatConfig();

    /**
     * 热点识别模块配置
     */
    private final HotspotConfig hotspot = new HotspotConfig();

    /**
     * 策略决策引擎配置
     */
    private final StrategyConfig strategy = new StrategyConfig();

    // ==================== 访问统计模块配置 ====================
    
    /**
     * 访问统计模块配置
     * 配置前缀：scheduler.stat
     */
    @Data
    public static class StatConfig {
        
        // ========== 时间窗口配置 ==========
        
        /**
         * 短窗口时长（秒）
         * 用于瞬时热点检测，默认 2 秒
         * 支持小数配置（如：0.5 表示 500ms，适用于秒杀场景）
         */
        private Double shortWindowSeconds = 2.0;
        
        /**
         * 长窗口时长（秒）
         * 用于稳定热度判断，默认 120 秒
         */
        private Integer longWindowSeconds = 120;
        
        // ========== Redis 容错配置 ==========
        
        /**
         * Redis 操作超时时间（毫秒）
         * 防止 Redis 慢查询阻塞线程，默认 3000ms
         */
        private Integer redisTimeout = 3000;
        
        /**
         * Redis 操作最大重试次数
         * 默认 2 次
         */
        private Integer maxRetries = 2;
        
        /**
         * 异常降级开关
         * 当 Redis 不可用时是否返回空结果而非抛异常
         * 默认 true（开启降级）
         */
        private Boolean fallbackEnabled = true;
        
        // ========== Key 配置 ==========
        
        /**
         * 统计 Key 前缀
         * 默认 "stat"，可按环境区分（如：stat_prod, stat_test）
         * Key格式：{keyPrefix}:{bizType}:{bizKey}:{window}
         */
        private String keyPrefix = "stat";
    }

    // ==================== 热点识别模块配置 ====================
    
    /**
     * 热点识别模块配置
     * 配置前缀：scheduler.hotspot
     */
    @Data
    public static class HotspotConfig {
        
        // ========== EXTREMELY_HOT 阈值 ==========
        
        /**
         * EXTREMELY_HOT 级别 - 短窗口阈值
         * 当 countShort >= 此值时判定为 EXTREMELY_HOT（突发流量）
         */
        private Long extremelyHotShortThreshold = 100L;
        
        /**
         * EXTREMELY_HOT 级别 - 长窗口阈值
         * 当 countLong >= 此值时判定为 EXTREMELY_HOT
         */
        private Long extremelyHotLongThreshold = 1000L;
        
        // ========== HOT 阈值 ==========
        
        /**
         * HOT 级别 - 短窗口阈值
         * 当 countShort >= 此值时判定为 HOT
         */
        private Long hotShortThreshold = 20L;
        
        /**
         * HOT 级别 - 长窗口阈值
         * 当 countLong >= 此值时判定为 HOT
         */
        private Long hotLongThreshold = 300L;
        
        // ========== WARM 阈值 ==========
        
        /**
         * WARM 级别 - 短窗口阈值
         * 当 countShort >= 此值时判定为 WARM
         */
        private Long warmShortThreshold = 5L;
        
        /**
         * WARM 级别 - 长窗口阈值
         * 当 countLong >= 此值时判定为 WARM
         */
        private Long warmLongThreshold = 60L;
    }

    // ==================== 策略决策引擎配置 ====================
    
    /**
     * 策略决策引擎配置
     * 配置前缀：scheduler.strategy
     */
    @Data
    public static class StrategyConfig {
        
        // ========== COLD 级别策略 ==========
        
        /**
         * COLD 级别 - 缓存模式
         * 默认 NONE（不缓存）
         */
        private String coldCacheMode = "NONE";
        
        /**
         * COLD 级别 - TTL 等级
         * 默认 SHORT
         */
        private String coldTtlLevel = "SHORT";
        
        // ========== WARM 级别策略 ==========
        
        /**
         * WARM 级别 - 缓存模式
         * 默认 REMOTE_ONLY（仅 Redis）
         */
        private String warmCacheMode = "REMOTE_ONLY";
        
        /**
         * WARM 级别 - TTL 等级
         * 默认 SHORT
         */
        private String warmTtlLevel = "SHORT";
        
        // ========== HOT 级别策略 ==========
        
        /**
         * HOT 级别 - 缓存模式
         * 默认 LOCAL_AND_REMOTE（本地 + Redis）
         */
        private String hotCacheMode = "LOCAL_AND_REMOTE";
        
        /**
         * HOT 级别 - TTL 等级
         * 默认 NORMAL
         */
        private String hotTtlLevel = "NORMAL";
        
        // ========== EXTREMELY_HOT 级别策略 ==========
        
        /**
         * EXTREMELY_HOT 级别 - 缓存模式
         * 默认 LOCAL_AND_REMOTE（本地 + Redis）
         */
        private String extremelyHotCacheMode = "LOCAL_AND_REMOTE";
        
        /**
         * EXTREMELY_HOT 级别 - TTL 等级
         * 默认 LONG
         */
        private String extremelyHotTtlLevel = "LONG";
    }
}
