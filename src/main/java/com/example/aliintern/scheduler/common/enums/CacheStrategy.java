package com.example.aliintern.scheduler.common.enums;

/**
 * 缓存策略枚举
 * 用于策略决策引擎输出的执行决策
 */
public enum CacheStrategy {

    /**
     * 跳过缓存，直连数据库
     */
    SKIP_CACHE,

    /**
     * 优先读缓存，未命中则查数据库
     */
    CACHE_FIRST,

    /**
     * 强制走缓存，必要时返回兜底数据
     */
    CACHE_ONLY,

    /**
     * 仅写入数据库，不更新缓存
     */
    WRITE_DB_ONLY,

    /**
     * 同时更新缓存和数据库
     */
    WRITE_THROUGH
}
