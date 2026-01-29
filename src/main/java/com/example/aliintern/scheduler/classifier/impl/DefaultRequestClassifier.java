package com.example.aliintern.scheduler.classifier.impl;

import com.example.aliintern.scheduler.classifier.RequestClassifier;
import com.example.aliintern.scheduler.common.enums.RequestType;
import com.example.aliintern.scheduler.common.model.RequestContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 请求分类器默认实现
 */
@Slf4j
@Component
public class DefaultRequestClassifier implements RequestClassifier {

    @Override
    public RequestType classify(RequestContext context) {
        // TODO: 实现请求分类逻辑
        // 基于请求的可复用性、一致性要求、业务风险等级以及访问模式等多个判断维度
        log.debug("Classifying request: {}", context.getRequestId());
        return RequestType.HIGH_REUSE_READ;
    }

    @Override
    public boolean requiresStatistics(RequestType requestType) {
        // 高复用读请求和个性化读请求需要进入统计模块
        return switch (requestType) {
            case HIGH_REUSE_READ, PERSONALIZED_READ -> true;
            case STRONG_CONSISTENCY_WRITE, NON_CRITICAL_WRITE, ASYNC_CALLBACK -> false;
        };
    }

    @Override
    public boolean isCacheable(RequestType requestType) {
        // 仅高复用读请求和个性化读请求可缓存
        return switch (requestType) {
            case HIGH_REUSE_READ, PERSONALIZED_READ -> true;
            case STRONG_CONSISTENCY_WRITE, NON_CRITICAL_WRITE, ASYNC_CALLBACK -> false;
        };
    }
}
