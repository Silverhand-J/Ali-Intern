package com.example.aliintern.scheduler.strategy;

import com.example.aliintern.scheduler.common.enums.HotspotLevel;
import com.example.aliintern.scheduler.common.model.DispatchDecision;

/**
 * 决策策略引擎接口
 * 
 * 职责定位：
 * - 输入：热点等级（HotspotLevel）
 * - 输出：缓存策略意图（DispatchDecision）
 * - 核心：纯决策、无副作用、无 IO
 * 
 * 设计原则：
 * - 只做 HotspotLevel → 缓存策略映射
 * - 不操作 Redis / 本地缓存
 * - 不执行限流 / 降级 / 异步刷新
 * - 不感知 QPS / 线程 / 系统状态
 * 
 * 模块边界：
 * - 访问统计模块：统计访问频次
 * - 热点识别模块：识别热点等级
 * - 决策策略引擎（本模块）：决定缓存策略
 * - 缓存访问代理：执行具体的缓存读写操作
 */
public interface DecisionStrategyEngine {

    /**
     * 根据热点等级决定缓存策略
     * 
     * @param level 热点等级（COLD/WARM/HOT/EXTREMELY_HOT）
     * @return 缓存策略决策（缓存模式 + TTL 等级）
     */
    DispatchDecision decide(HotspotLevel level);
}
