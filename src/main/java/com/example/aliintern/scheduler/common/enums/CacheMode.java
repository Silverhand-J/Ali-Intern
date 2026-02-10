package com.example.aliintern.scheduler.common.enums;

/**
 * 缓存使用模式
 * 表达多级缓存的准入策略
 */
public enum CacheMode {

    /**
     * 不使用任何缓存
     * 每次请求直接回源（DB/下游服务）
     */
    NONE,

    /**
     * 只使用本地缓存（L1）
     * 适用于：单实例场景或数据一致性要求极低的场景
     */
    LOCAL_ONLY,

    /**
     * 只使用 Redis（L2）
     * 适用于：中等热度数据，需要跨实例共享
     */
    REMOTE_ONLY,

    /**
     * 同时使用本地缓存 + Redis
     * 适用于：高频热点数据，追求极致性能
     */
    LOCAL_AND_REMOTE
}
