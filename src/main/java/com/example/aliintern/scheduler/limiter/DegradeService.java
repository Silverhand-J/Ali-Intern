package com.example.aliintern.scheduler.limiter;

import com.example.aliintern.scheduler.common.model.RequestContext;

/**
 * 降级服务接口
 * 在系统过载时，简化部分请求以保护核心链路
 */
public interface DegradeService {

    /**
     * 判断是否需要降级
     *
     * @param context 请求上下文
     * @return true-需要降级, false-正常处理
     */
    boolean shouldDegrade(RequestContext context);

    /**
     * 获取降级数据
     *
     * @param context 请求上下文
     * @return 降级后的数据
     */
    Object getDegradedData(RequestContext context);

    /**
     * 判断系统是否处于过载状态
     *
     * @return true-过载, false-正常
     */
    boolean isSystemOverloaded();

    /**
     * 获取降级等级
     *
     * @return 当前降级等级（0-不降级, 1-轻度降级, 2-中度降级, 3-重度降级）
     */
    int getDegradeLevel();
}
