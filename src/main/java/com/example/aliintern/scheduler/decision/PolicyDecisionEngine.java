package com.example.aliintern.scheduler.decision;

import com.example.aliintern.scheduler.common.model.PolicyDecision;
import com.example.aliintern.scheduler.common.model.RequestContext;

/**
 * 策略决策引擎接口
 * 综合请求类型、热点等级、系统当前状态等多维信息，做出最终执行决策
 */
public interface PolicyDecisionEngine {

    /**
     * 做出策略决策
     *
     * @param context 请求上下文
     * @return 策略决策结果
     */
    PolicyDecision makeDecision(RequestContext context);

    /**
     * 根据热点等级计算推荐的缓存TTL
     *
     * @param context 请求上下文
     * @return 推荐的TTL（秒）
     */
    Long calculateRecommendedTtl(RequestContext context);

    /**
     * 判断是否需要限流
     *
     * @param context 请求上下文
     * @return true-需要限流, false-不需要限流
     */
    boolean requiresRateLimit(RequestContext context);
}
