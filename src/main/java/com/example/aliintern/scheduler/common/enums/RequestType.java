package com.example.aliintern.scheduler.common.enums;

/**
 * 请求类型枚举
 * 用于请求分类器对请求进行分类
 */
public enum RequestType {

    /**
     * 高复用读请求（如商品详情页、搜索结果页）
     */
    HIGH_REUSE_READ,

    /**
     * 个性化读请求（如用户推荐、历史浏览）
     */
    PERSONALIZED_READ,

    /**
     * 强一致性写请求（如订单提交、支付回调、库存扣减）
     */
    STRONG_CONSISTENCY_WRITE,

    /**
     * 非关键写请求或辅助请求
     */
    NON_CRITICAL_WRITE,

    /**
     * 异步回调类请求
     */
    ASYNC_CALLBACK
}
