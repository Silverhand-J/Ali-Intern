package com.example.aliintern.scheduler.strategy;

import com.example.aliintern.scheduler.common.enums.CacheMode;
import com.example.aliintern.scheduler.common.enums.CacheTtlLevel;
import com.example.aliintern.scheduler.common.enums.HotspotLevel;
import com.example.aliintern.scheduler.common.model.DispatchDecision;
import com.example.aliintern.scheduler.config.SchedulerProperties;
import com.example.aliintern.scheduler.strategy.impl.DefaultDecisionStrategyEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 决策策略引擎单元测试
 * 
 * 测试覆盖：
 * 1. 默认策略映射正确性
 * 2. null 输入处理
 * 3. 配置变更生效
 * 4. 策略一致性
 */
class DefaultDecisionStrategyEngineTest {

    private SchedulerProperties schedulerProperties;
    private DefaultDecisionStrategyEngine engine;

    @BeforeEach
    void setUp() {
        schedulerProperties = new SchedulerProperties();
        engine = new DefaultDecisionStrategyEngine(schedulerProperties);
        engine.init();
    }

    // ==================== 默认策略映射测试 ====================

    @Test
    @DisplayName("COLD 级别：应返回 NONE + SHORT")
    void decide_Cold_ReturnsNoneAndShort() {
        DispatchDecision decision = engine.decide(HotspotLevel.COLD);

        assertNotNull(decision);
        assertEquals(CacheMode.NONE, decision.getCacheMode());
        assertEquals(CacheTtlLevel.SHORT, decision.getTtlLevel());
    }

    @Test
    @DisplayName("WARM 级别：应返回 REMOTE_ONLY + SHORT")
    void decide_Warm_ReturnsRemoteOnlyAndShort() {
        DispatchDecision decision = engine.decide(HotspotLevel.WARM);

        assertNotNull(decision);
        assertEquals(CacheMode.REMOTE_ONLY, decision.getCacheMode());
        assertEquals(CacheTtlLevel.SHORT, decision.getTtlLevel());
    }

    @Test
    @DisplayName("HOT 级别：应返回 LOCAL_AND_REMOTE + NORMAL")
    void decide_Hot_ReturnsLocalAndRemoteAndNormal() {
        DispatchDecision decision = engine.decide(HotspotLevel.HOT);

        assertNotNull(decision);
        assertEquals(CacheMode.LOCAL_AND_REMOTE, decision.getCacheMode());
        assertEquals(CacheTtlLevel.NORMAL, decision.getTtlLevel());
    }

    @Test
    @DisplayName("EXTREMELY_HOT 级别：应返回 LOCAL_AND_REMOTE + LONG")
    void decide_ExtremelyHot_ReturnsLocalAndRemoteAndLong() {
        DispatchDecision decision = engine.decide(HotspotLevel.EXTREMELY_HOT);

        assertNotNull(decision);
        assertEquals(CacheMode.LOCAL_AND_REMOTE, decision.getCacheMode());
        assertEquals(CacheTtlLevel.LONG, decision.getTtlLevel());
    }

    // ==================== 边界条件测试 ====================

    @Test
    @DisplayName("null 输入：应返回 COLD 的默认策略")
    void decide_NullInput_ReturnsDefaultStrategy() {
        DispatchDecision decision = engine.decide(null);

        assertNotNull(decision);
        assertEquals(CacheMode.NONE, decision.getCacheMode());
        assertEquals(CacheTtlLevel.SHORT, decision.getTtlLevel());
    }

    // ==================== 配置变更测试 ====================

    @Test
    @DisplayName("配置变更：修改 HOT 策略为 REMOTE_ONLY + LONG")
    void decide_CustomConfig_ReturnsCustomStrategy() {
        // 修改配置
        schedulerProperties.getStrategy().setHotCacheMode("REMOTE_ONLY");
        schedulerProperties.getStrategy().setHotTtlLevel("LONG");

        // 重新初始化引擎
        engine.init();

        DispatchDecision decision = engine.decide(HotspotLevel.HOT);

        assertEquals(CacheMode.REMOTE_ONLY, decision.getCacheMode());
        assertEquals(CacheTtlLevel.LONG, decision.getTtlLevel());
    }

    @Test
    @DisplayName("无效配置：应使用默认值")
    void decide_InvalidConfig_UsesFallbackDefault() {
        // 设置无效配置
        schedulerProperties.getStrategy().setColdCacheMode("INVALID_MODE");
        schedulerProperties.getStrategy().setColdTtlLevel("INVALID_TTL");

        // 重新初始化引擎
        engine.init();

        DispatchDecision decision = engine.decide(HotspotLevel.COLD);

        // 应使用 fallback 默认值
        assertEquals(CacheMode.NONE, decision.getCacheMode());
        assertEquals(CacheTtlLevel.SHORT, decision.getTtlLevel());
    }

    // ==================== 一致性测试 ====================

    @Test
    @DisplayName("相同输入：应返回相同结果（纯函数特性）")
    void decide_SameInput_ReturnsSameResult() {
        DispatchDecision decision1 = engine.decide(HotspotLevel.HOT);
        DispatchDecision decision2 = engine.decide(HotspotLevel.HOT);

        assertEquals(decision1.getCacheMode(), decision2.getCacheMode());
        assertEquals(decision1.getTtlLevel(), decision2.getTtlLevel());
    }

    // ==================== 策略升级测试 ====================

    @Test
    @DisplayName("策略升级：COLD -> WARM -> HOT -> EXTREMELY_HOT 递进正确")
    void decide_StrategyEscalation_FollowsExpectedPattern() {
        DispatchDecision cold = engine.decide(HotspotLevel.COLD);
        DispatchDecision warm = engine.decide(HotspotLevel.WARM);
        DispatchDecision hot = engine.decide(HotspotLevel.HOT);
        DispatchDecision extremelyHot = engine.decide(HotspotLevel.EXTREMELY_HOT);

        // COLD: 不缓存
        assertEquals(CacheMode.NONE, cold.getCacheMode());

        // WARM: 开始使用 Redis
        assertEquals(CacheMode.REMOTE_ONLY, warm.getCacheMode());

        // HOT: 升级到本地 + Redis
        assertEquals(CacheMode.LOCAL_AND_REMOTE, hot.getCacheMode());

        // EXTREMELY_HOT: 保持本地 + Redis，但 TTL 升级
        assertEquals(CacheMode.LOCAL_AND_REMOTE, extremelyHot.getCacheMode());
        
        // TTL 递进
        assertEquals(CacheTtlLevel.SHORT, cold.getTtlLevel());
        assertEquals(CacheTtlLevel.SHORT, warm.getTtlLevel());
        assertEquals(CacheTtlLevel.NORMAL, hot.getTtlLevel());
        assertEquals(CacheTtlLevel.LONG, extremelyHot.getTtlLevel());
    }
}
