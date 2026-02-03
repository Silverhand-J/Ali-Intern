package com.example.aliintern.scheduler.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 访问统计结果
 * 用于返回双时间窗口的访问计数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatResult {

    /**
     * 最近1秒内的访问次数
     * 用于判断瞬时高频访问
     */
    private Long count1s;

    /**
     * 最近60秒内的访问次数
     * 用于判断稳定热度
     */
    private Long count60s;

    /**
     * 创建一个空的统计结果（计数均为0）
     */
    public static StatResult empty() {
        return StatResult.builder()
                .count1s(0L)
                .count60s(0L)
                .build();
    }

    /**
     * 创建统计结果
     *
     * @param count1s  1秒窗口计数
     * @param count60s 60秒窗口计数
     * @return StatResult实例
     */
    public static StatResult of(Long count1s, Long count60s) {
        return StatResult.builder()
                .count1s(count1s)
                .count60s(count60s)
                .build();
    }
}
