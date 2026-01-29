package com.example.aliintern.scheduler.proxy;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Redis访问代理接口
 * 对所有Redis读写操作进行统一封装与治理
 */
public interface RedisAccessProxy {

    /**
     * 获取缓存值
     *
     * @param key 缓存键
     * @return 缓存值
     */
    String get(String key);

    /**
     * 设置缓存值
     *
     * @param key 缓存键
     * @param value 缓存值
     * @param ttl 过期时间
     * @param timeUnit 时间单位
     */
    void set(String key, String value, long ttl, TimeUnit timeUnit);

    /**
     * 批量获取缓存值
     *
     * @param keys 缓存键列表
     * @return 缓存值映射
     */
    Map<String, String> multiGet(List<String> keys);

    /**
     * 删除缓存
     *
     * @param key 缓存键
     * @return 是否删除成功
     */
    boolean delete(String key);

    /**
     * 判断缓存是否存在
     *
     * @param key 缓存键
     * @return true-存在, false-不存在
     */
    boolean exists(String key);

    /**
     * 自增计数
     *
     * @param key 缓存键
     * @return 自增后的值
     */
    Long increment(String key);

    /**
     * 带过期时间的自增计数
     *
     * @param key 缓存键
     * @param ttl 过期时间
     * @param timeUnit 时间单位
     * @return 自增后的值
     */
    Long incrementWithExpire(String key, long ttl, TimeUnit timeUnit);
}
