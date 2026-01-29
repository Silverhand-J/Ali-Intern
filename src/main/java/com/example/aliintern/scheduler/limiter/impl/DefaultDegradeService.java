package com.example.aliintern.scheduler.limiter.impl;

import com.example.aliintern.scheduler.common.enums.RequestType;
import com.example.aliintern.scheduler.common.model.RequestContext;
import com.example.aliintern.scheduler.limiter.DegradeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 降级服务默认实现
 */
@Slf4j
@Service
public class DefaultDegradeService implements DegradeService {

    // 当前降级等级
    private final AtomicInteger degradeLevel = new AtomicInteger(0);

    @Override
    public boolean shouldDegrade(RequestContext context) {
        int level = degradeLevel.get();

        // 不降级
        if (level == 0) {
            return false;
        }

        // 强一致性写请求不降级
        if (context.getRequestType() == RequestType.STRONG_CONSISTENCY_WRITE) {
            return false;
        }

        // 根据降级等级决定是否降级
        return switch (level) {
            case 1 -> context.getRequestType() == RequestType.PERSONALIZED_READ;
            case 2 -> context.getRequestType() == RequestType.PERSONALIZED_READ
                    || context.getRequestType() == RequestType.NON_CRITICAL_WRITE;
            case 3 -> true;
            default -> false;
        };
    }

    @Override
    public Object getDegradedData(RequestContext context) {
        log.debug("Returning degraded data for request: {}", context.getRequestId());
        // TODO: 实现降级数据获取逻辑
        // 返回缓存的旧数据或简化字段版本
        return null;
    }

    @Override
    public boolean isSystemOverloaded() {
        // TODO: 实现系统过载检测逻辑
        return degradeLevel.get() > 0;
    }

    @Override
    public int getDegradeLevel() {
        return degradeLevel.get();
    }

    /**
     * 设置降级等级
     *
     * @param level 降级等级
     */
    public void setDegradeLevel(int level) {
        degradeLevel.set(level);
        log.info("Degrade level changed to: {}", level);
    }
}
