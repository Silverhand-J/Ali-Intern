package com.example.aliintern.scheduler.hotspot;

import com.example.aliintern.scheduler.common.enums.HotspotLevel;

/**
 * 热点识别模块接口
 * 负责判断某个请求key是否为热点访问，并对热点程度进行分级
 */
public interface HotspotDetector {

    /**
     * 识别热点等级
     *
     * @param cacheKey 缓存键
     * @return 热点等级
     */
    HotspotLevel detectHotspotLevel(String cacheKey);

    /**
     * 判断是否为热点数据
     *
     * @param cacheKey 缓存键
     * @return true-热点数据, false-非热点数据
     */
    boolean isHotspot(String cacheKey);

    /**
     * 获取热点阈值配置
     *
     * @param level 热点等级
     * @return 该等级对应的访问次数阈值
     */
    Long getThreshold(HotspotLevel level);
}
