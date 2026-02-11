package com.example.aliintern.scheduler.cache;

import com.example.aliintern.scheduler.common.model.DispatchDecision;

import java.util.function.Supplier;

/**
 * 缓存访问代理接口
 * 
 * 职责：
 * - 根据 DispatchDecision 执行具体的缓存访问
 * - 管理多级缓存的读写顺序
 * - 处理缓存未命中的回源逻辑
 * 
 * 约束：
 * - 不参与策略决策
 * - 不做热点识别
 * - 不引入限流/降级
 * - 纯执行层，只负责缓存访问
 */
public interface CacheAccessProxy {

    /**
     * 缓存访问核心方法
     * 
     * 执行流程：
     * 1. 根据 decision.cacheMode 决定是否访问本地缓存
     * 2. 本地未命中，根据 decision.cacheMode 决定是否访问 Redis
     * 3. 仍未命中，调用 dbLoader 回源
     * 4. 回源成功后，根据 decision 决定是否写缓存
     * 
     * @param key       缓存键
     * @param dbLoader  数据库回源函数（仅在缓存未命中时调用）
     * @param decision  策略决策结果（来自 DecisionStrategyEngine）
     * @param <T>       返回值类型
     * @return 数据（可能来自缓存或 DB）
     */
    <T> T access(String key, Supplier<T> dbLoader, DispatchDecision decision);

    /**
     * 删除缓存（用于数据更新时的缓存失效）
     * 
     * @param key 缓存键
     */
    void invalidate(String key);
}
