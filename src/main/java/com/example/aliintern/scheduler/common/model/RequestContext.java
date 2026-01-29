package com.example.aliintern.scheduler.common.model;

import com.example.aliintern.scheduler.common.enums.CacheStrategy;
import com.example.aliintern.scheduler.common.enums.HotspotLevel;
import com.example.aliintern.scheduler.common.enums.RequestType;
import lombok.Builder;
import lombok.Data;

/**
 * 请求上下文
 * 封装请求在调度层各模块间传递的信息
 */
@Data
@Builder
public class RequestContext {

    /**
     * 请求唯一标识
     */
    private String requestId;

    /**
     * 缓存键（如 skuId）
     */
    private String cacheKey;

    /**
     * 请求类型
     */
    private RequestType requestType;

    /**
     * 热点等级
     */
    private HotspotLevel hotspotLevel;

    /**
     * 缓存策略
     */
    private CacheStrategy cacheStrategy;

    /**
     * 缓存TTL（秒）
     */
    private Long cacheTtl;

    /**
     * 是否允许缓存
     */
    private Boolean cacheAllowed;

    /**
     * 请求时间戳
     */
    private Long timestamp;

    /**
     * 用户ID（可选，用于个性化请求）
     */
    private String userId;

    /**
     * 请求来源（如 Web、App、API）
     */
    private String source;
}
