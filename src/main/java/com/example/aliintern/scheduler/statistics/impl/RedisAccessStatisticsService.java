package com.example.aliintern.scheduler.statistics.impl;

import com.example.aliintern.scheduler.common.model.AccessStatistics;
import com.example.aliintern.scheduler.common.model.StatResult;
import com.example.aliintern.scheduler.statistics.AccessStatisticsService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * 访问统计模块实现
 * 使用Redis + Lua脚本实现高并发、原子性的访问计数
 * 
 * 设计要点：
 * 1. 双窗口统计：1秒窗口（瞬时热点）+ 60秒窗口（稳定热度）
 * 2. Key格式：stat:{bizType}:{bizKey}:{window}
 * 3. 使用Lua脚本保证INCR + EXPIRE的原子性
 * 4. 不使用本地内存，支持多实例部署
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisAccessStatisticsService implements AccessStatisticsService {

    private final StringRedisTemplate redisTemplate;

    /**
     * 统计Key前缀
     */
    private static final String STAT_KEY_PREFIX = "stat";

    /**
     * 1秒窗口标识
     */
    private static final String WINDOW_1S = "1s";

    /**
     * 60秒窗口标识
     */
    private static final String WINDOW_60S = "60s";

    /**
     * 1秒窗口过期时间（秒）
     */
    private static final int TTL_1S = 1;

    /**
     * 60秒窗口过期时间（秒）
     */
    private static final int TTL_60S = 60;

    /**
     * Redis Lua脚本：原子性执行 INCR + 条件 EXPIRE
     * 
     * 逻辑：
     * 1. 对key执行INCR
     * 2. 如果是第一次递增（count == 1），设置过期时间
     * 3. 返回当前计数值
     */
    private static final String INCR_WITH_EXPIRE_SCRIPT = 
            "local count = redis.call('INCR', KEYS[1]) " +
            "if count == 1 then " +
            "    redis.call('EXPIRE', KEYS[1], ARGV[1]) " +
            "end " +
            "return count";

    private DefaultRedisScript<Long> incrWithExpireScript;

    @PostConstruct
    public void init() {
        // 初始化Lua脚本
        incrWithExpireScript = new DefaultRedisScript<>();
        incrWithExpireScript.setScriptText(INCR_WITH_EXPIRE_SCRIPT);
        incrWithExpireScript.setResultType(Long.class);
        log.info("访问统计模块初始化完成，Lua脚本已加载");
    }

    @Override
    public StatResult record(String bizType, String bizKey) {
        if (bizType == null || bizType.isEmpty() || bizKey == null || bizKey.isEmpty()) {
            log.warn("无效的统计参数: bizType={}, bizKey={}", bizType, bizKey);
            return StatResult.empty();
        }

        try {
            // 构建双窗口的Redis Key
            String key1s = buildStatKey(bizType, bizKey, WINDOW_1S);
            String key60s = buildStatKey(bizType, bizKey, WINDOW_60S);

            // 执行Lua脚本更新计数（原子操作）
            Long count1s = executeIncrWithExpire(key1s, TTL_1S);
            Long count60s = executeIncrWithExpire(key60s, TTL_60S);

            log.debug("访问统计记录完成: bizType={}, bizKey={}, count1s={}, count60s={}", 
                    bizType, bizKey, count1s, count60s);

            return StatResult.of(count1s, count60s);
        } catch (Exception e) {
            log.error("访问统计记录失败: bizType={}, bizKey={}, error={}", 
                    bizType, bizKey, e.getMessage(), e);
            return StatResult.empty();
        }
    }

    /**
     * 执行带过期时间的原子递增操作
     *
     * @param key Redis Key
     * @param ttl 过期时间（秒）
     * @return 递增后的计数值
     */
    private Long executeIncrWithExpire(String key, int ttl) {
        Long result = redisTemplate.execute(
                incrWithExpireScript,
                Collections.singletonList(key),
                String.valueOf(ttl)
        );
        return result != null ? result : 0L;
    }

    /**
     * 构建统计Key
     * 格式：stat:{bizType}:{bizKey}:{window}
     *
     * @param bizType 业务类型
     * @param bizKey  业务键
     * @param window  时间窗口标识
     * @return Redis Key
     */
    private String buildStatKey(String bizType, String bizKey, String window) {
        return String.format("%s:%s:%s:%s", STAT_KEY_PREFIX, bizType, bizKey, window);
    }

    @Override
    public void recordAccess(String cacheKey) {
        // 兼容旧接口：使用默认业务类型
        if (cacheKey == null || cacheKey.isEmpty()) {
            log.warn("无效的缓存键: {}", cacheKey);
            return;
        }
        record("default", cacheKey);
        log.debug("Recording access for key: {}", cacheKey);
    }

    @Override
    public AccessStatistics getStatistics(String cacheKey) {
        if (cacheKey == null || cacheKey.isEmpty()) {
            log.warn("无效的缓存键: {}", cacheKey);
            return buildEmptyStatistics(cacheKey);
        }

        try {
            // 获取60秒窗口的统计数据
            String key60s = buildStatKey("default", cacheKey, WINDOW_60S);
            String countStr = redisTemplate.opsForValue().get(key60s);
            Long accessCount = countStr != null ? Long.parseLong(countStr) : 0L;

            log.debug("Getting statistics for key: {}, count: {}", cacheKey, accessCount);
            
            return AccessStatistics.builder()
                    .cacheKey(cacheKey)
                    .accessCount(accessCount)
                    .windowStartTime(System.currentTimeMillis())
                    .windowSize(60000L)
                    .lastAccessTime(System.currentTimeMillis())
                    .averageAccessRate(accessCount / 60.0)
                    .build();
        } catch (Exception e) {
            log.error("获取统计信息失败: cacheKey={}, error={}", cacheKey, e.getMessage(), e);
            return buildEmptyStatistics(cacheKey);
        }
    }

    @Override
    public Long getAccessCount(String cacheKey) {
        if (cacheKey == null || cacheKey.isEmpty()) {
            log.warn("无效的缓存键: {}", cacheKey);
            return 0L;
        }

        try {
            String key60s = buildStatKey("default", cacheKey, WINDOW_60S);
            String countStr = redisTemplate.opsForValue().get(key60s);
            Long count = countStr != null ? Long.parseLong(countStr) : 0L;
            
            log.debug("Getting access count for key: {}, count: {}", cacheKey, count);
            return count;
        } catch (Exception e) {
            log.error("获取访问次数失败: cacheKey={}, error={}", cacheKey, e.getMessage(), e);
            return 0L;
        }
    }

    @Override
    public void cleanExpiredData() {
        // Redis的Key自动过期机制已处理数据清理
        // 此方法保留用于未来可能的主动清理需求
        log.debug("Cleaning expired statistics data - handled by Redis TTL mechanism");
    }

    /**
     * 构建空的统计结果
     */
    private AccessStatistics buildEmptyStatistics(String cacheKey) {
        return AccessStatistics.builder()
                .cacheKey(cacheKey)
                .accessCount(0L)
                .windowStartTime(System.currentTimeMillis())
                .windowSize(60000L)
                .lastAccessTime(System.currentTimeMillis())
                .averageAccessRate(0.0)
                .build();
    }
}
