package com.example.aliintern.scheduler.common.enums;

/**
 * 热点等级枚举
 * 用于热点识别模块对数据热度进行分级
 */
public enum HotspotLevel {

    /**
     * 冷数据 - 访问频次低
     */
    COLD,

    /**
     * 温数据 - 访问频次中等
     */
    WARM,

    /**
     * 热数据 - 访问频次高，需要缓存
     */
    HOT,

    /**
     * 极热数据 - 访问量突然飙升，需要特殊处理
     */
    EXTREMELY_HOT
}
