package com.example.aliintern.scheduler.statistics;

import com.example.aliintern.scheduler.common.model.AccessStatistics;
import com.example.aliintern.scheduler.common.model.StatResult;

/**
 * 访问统计模块接口
 * 负责对有缓存潜力的请求key进行低成本、高并发的访问频次统计
 * 
 * 核心职责：
 * - 统计某个key在固定时间窗口内的访问次数
 * - 支持多实例部署
 * - 保证高并发安全、低延迟
 * - 统计数据自动过期
 * 
 * 严格限制：
 * - 不判断冷热
 * - 不决定是否缓存
 * - 不操作Redis业务数据
 * - 只负责"记录事实"
 */
public interface AccessStatisticsService {

    /**
     * 记录一次访问并返回双窗口统计结果
     * 
     * 使用Redis Lua脚本保证原子性：
     * 1. 对1秒窗口key执行INCR + 条件EXPIRE
     * 2. 对60秒窗口key执行INCR + 条件EXPIRE
     * 
     * Key格式：stat:{bizType}:{bizKey}:{window}
     * 示例：stat:product:12345:1s, stat:product:12345:60s
     *
     * @param bizType 业务类型（如：product, order, user）
     * @param bizKey  业务键（如：商品ID、订单ID）
     * @return StatResult 包含count1s和count60s的统计结果
     */
    StatResult record(String bizType, String bizKey);

    /**
     * 记录一次访问（兼容旧接口）
     *
     * @param cacheKey 缓存键
     */
    void recordAccess(String cacheKey);

    /**
     * 获取访问统计信息
     *
     * @param cacheKey 缓存键
     * @return 访问统计数据
     */
    AccessStatistics getStatistics(String cacheKey);

    /**
     * 获取当前时间窗口内的访问次数
     *
     * @param cacheKey 缓存键
     * @return 访问次数
     */
    Long getAccessCount(String cacheKey);

    /**
     * 清除过期的统计数据
     */
    void cleanExpiredData();
}
