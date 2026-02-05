package com.example.aliintern.scheduler.hotspot;

import com.example.aliintern.scheduler.common.enums.HotspotLevel;
import com.example.aliintern.scheduler.common.model.StatResult;

/**
 * 热点识别模块接口
 * 
 * 职责边界（必须严格遵守）：
 * - 输入：访问统计结果（StatResult）
 * - 输出：热点等级（HotspotLevel）
 * 
 * 严格限制：
 * - 不进行任何 Redis / MySQL 操作
 * - 不参与缓存写入、限流、业务逻辑
 * - 不修改 StatResult
 * - 不感知业务类型
 * - 只负责"判断热度等级"
 */
public interface HotspotDetector {

    /**
     * 核心方法：根据访问统计结果识别热点等级
     * 
     * 采用"双窗口阈值判断"策略：
     * - 瞬时热度（短窗口）用于识别突发热点
     * - 稳定热度（长窗口）用于识别长期热点
     * - 当多个条件命中时，取最高热度等级
     *
     * @param stat 访问统计结果（包含 countShort 和 countLong）
     * @return 热点等级（COLD / WARM / HOT / EXTREMELY_HOT）
     */
    HotspotLevel detect(StatResult stat);

    /**
     * 获取热点阈值配置
     *
     * @param level 热点等级
     * @return 该等级对应的访问次数阈值（长窗口）
     */
    Long getThreshold(HotspotLevel level);
}
