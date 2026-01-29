package com.example.aliintern.scheduler.classifier;

import com.example.aliintern.scheduler.common.enums.RequestType;
import com.example.aliintern.scheduler.common.model.RequestContext;

/**
 * 请求分类器接口
 * 负责对进入调度层的请求进行快速分析和分类
 */
public interface RequestClassifier {

    /**
     * 对请求进行分类
     *
     * @param context 请求上下文
     * @return 请求类型
     */
    RequestType classify(RequestContext context);

    /**
     * 判断请求是否需要进入访问统计模块
     *
     * @param requestType 请求类型
     * @return true-需要统计, false-跳过统计
     */
    boolean requiresStatistics(RequestType requestType);

    /**
     * 判断请求是否可缓存
     *
     * @param requestType 请求类型
     * @return true-可缓存, false-不可缓存
     */
    boolean isCacheable(RequestType requestType);
}
