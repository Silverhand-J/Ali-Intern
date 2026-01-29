package com.example.aliintern.scheduler.limiter;

import com.example.aliintern.scheduler.common.model.RequestContext;

/**
 * 限流服务接口
 * 在系统接近或超过安全承载能力时，主动削减部分请求
 */
public interface RateLimiterService {

    /**
     * 判断请求是否被限流
     *
     * @param context 请求上下文
     * @return true-被限流, false-放行
     */
    boolean isRateLimited(RequestContext context);

    /**
     * 尝试获取令牌
     *
     * @param key 限流键
     * @param permitsPerSecond 每秒允许的请求数
     * @return true-获取成功, false-获取失败
     */
    boolean tryAcquire(String key, int permitsPerSecond);

    /**
     * 获取当前QPS
     *
     * @param key 限流键
     * @return 当前QPS
     */
    Long getCurrentQps(String key);
}
