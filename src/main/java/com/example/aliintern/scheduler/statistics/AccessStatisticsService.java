package com.example.aliintern.scheduler.statistics;

import com.example.aliintern.scheduler.common.model.AccessStatistics;

/**
 * 访问统计模块接口
 * 负责对有缓存潜力的请求key进行低成本、高并发的访问频次统计
 */
public interface AccessStatisticsService {

    /**
     * 记录一次访问
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
