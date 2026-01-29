package com.example.aliintern.scheduler.admission;

import com.example.aliintern.scheduler.common.model.RequestContext;

/**
 * 缓存准入控制接口
 * 在策略决策引擎允许缓存的前提下，判断数据是否值得占用有限的缓存空间
 */
public interface CacheAdmissionControl {

    /**
     * 判断是否允许写入缓存
     *
     * @param context 请求上下文
     * @return true-允许写入, false-拒绝写入
     */
    boolean allowCacheWrite(RequestContext context);

    /**
     * 根据访问模式调整TTL
     *
     * @param context 请求上下文
     * @param originalTtl 原始TTL
     * @return 调整后的TTL
     */
    Long adjustTtl(RequestContext context, Long originalTtl);

    /**
     * 判断是否为稳定热点
     *
     * @param cacheKey 缓存键
     * @return true-稳定热点, false-临时访问
     */
    boolean isStableHotspot(String cacheKey);
}
