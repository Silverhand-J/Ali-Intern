package com.example.aliintern.scheduler.common.enums;

/**
 * 缓存 TTL 等级
 * 只表达"付出多少缓存成本"的意图，不涉及具体秒数
 * 
 * 具体 TTL 秒数由配置文件决定，例如：
 * - SHORT: 30s
 * - NORMAL: 60s
 * - LONG: 300s
 */
public enum CacheTtlLevel {

    /**
     * 短 TTL
     * 适用于：临时缓存、中等热度数据
     */
    SHORT,

    /**
     * 正常 TTL
     * 适用于：常规热点数据
     */
    NORMAL,

    /**
     * 长 TTL
     * 适用于：极热数据、需要长期保留的数据
     */
    LONG
}
