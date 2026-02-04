package com.example.aliintern.scheduler.hotspot;

import com.example.aliintern.scheduler.common.enums.HotspotLevel;
import com.example.aliintern.scheduler.common.model.StatResult;
import com.example.aliintern.scheduler.config.SchedulerProperties;
import com.example.aliintern.scheduler.hotspot.impl.DefaultHotspotDetector;
import com.example.aliintern.scheduler.statistics.AccessStatisticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 热点识别模块单元测试
 * 
 * 测试双窗口阈值判断逻辑的正确性
 */
@ExtendWith(MockitoExtension.class)
class DefaultHotspotDetectorTest {

    @Mock
    private AccessStatisticsService accessStatisticsService;

    private SchedulerProperties schedulerProperties;
    private DefaultHotspotDetector detector;

    @BeforeEach
    void setUp() {
        // 使用默认配置
        schedulerProperties = new SchedulerProperties();
        // 默认值：
        // EXTREMELY_HOT: short >= 100 OR long >= 1000
        // HOT: short >= 20 OR long >= 300
        // WARM: short >= 5 OR long >= 60
        
        detector = new DefaultHotspotDetector(accessStatisticsService, schedulerProperties);
    }

    // ==================== COLD 级别测试 ====================

    @Test
    @DisplayName("COLD: 零访问量应返回 COLD")
    void detect_ZeroAccess_ReturnsCold() {
        StatResult stat = StatResult.of(0L, 0L);
        assertEquals(HotspotLevel.COLD, detector.detect(stat));
    }

    @Test
    @DisplayName("COLD: 低于 WARM 阈值应返回 COLD")
    void detect_BelowWarmThreshold_ReturnsCold() {
        // 1s < 5 AND 60s < 60
        StatResult stat = StatResult.of(4L, 59L);
        assertEquals(HotspotLevel.COLD, detector.detect(stat));
    }

    @Test
    @DisplayName("COLD: null StatResult 应返回 COLD")
    void detect_NullStat_ReturnsCold() {
        assertEquals(HotspotLevel.COLD, detector.detect(null));
    }

    // ==================== WARM 级别测试 ====================

    @Test
    @DisplayName("WARM: 1秒窗口达到阈值应返回 WARM")
    void detect_Warm1sThreshold_ReturnsWarm() {
        // 1s >= 5, 60s < 60
        StatResult stat = StatResult.of(5L, 50L);
        assertEquals(HotspotLevel.WARM, detector.detect(stat));
    }

    @Test
    @DisplayName("WARM: 60秒窗口达到阈值应返回 WARM")
    void detect_Warm60sThreshold_ReturnsWarm() {
        // 1s < 5, 60s >= 60
        StatResult stat = StatResult.of(3L, 60L);
        assertEquals(HotspotLevel.WARM, detector.detect(stat));
    }

    @Test
    @DisplayName("WARM: 双窗口均达到 WARM 阈值应返回 WARM")
    void detect_BothWarmThreshold_ReturnsWarm() {
        // 1s >= 5, 60s >= 60, but < HOT
        StatResult stat = StatResult.of(10L, 100L);
        assertEquals(HotspotLevel.WARM, detector.detect(stat));
    }

    // ==================== HOT 级别测试 ====================

    @Test
    @DisplayName("HOT: 1秒窗口达到阈值应返回 HOT")
    void detect_Hot1sThreshold_ReturnsHot() {
        // 1s >= 20, 60s < 300
        StatResult stat = StatResult.of(20L, 200L);
        assertEquals(HotspotLevel.HOT, detector.detect(stat));
    }

    @Test
    @DisplayName("HOT: 60秒窗口达到阈值应返回 HOT")
    void detect_Hot60sThreshold_ReturnsHot() {
        // 1s < 20, 60s >= 300
        StatResult stat = StatResult.of(15L, 300L);
        assertEquals(HotspotLevel.HOT, detector.detect(stat));
    }

    @Test
    @DisplayName("HOT: 双窗口均达到 HOT 阈值应返回 HOT")
    void detect_BothHotThreshold_ReturnsHot() {
        // 1s >= 20, 60s >= 300, but < EXTREMELY_HOT
        StatResult stat = StatResult.of(50L, 500L);
        assertEquals(HotspotLevel.HOT, detector.detect(stat));
    }

    // ==================== EXTREMELY_HOT 级别测试 ====================

    @Test
    @DisplayName("EXTREMELY_HOT: 1秒窗口达到阈值应返回 EXTREMELY_HOT")
    void detect_ExtremelyHot1sThreshold_ReturnsExtremelyHot() {
        // 1s >= 100
        StatResult stat = StatResult.of(100L, 500L);
        assertEquals(HotspotLevel.EXTREMELY_HOT, detector.detect(stat));
    }

    @Test
    @DisplayName("EXTREMELY_HOT: 60秒窗口达到阈值应返回 EXTREMELY_HOT")
    void detect_ExtremelyHot60sThreshold_ReturnsExtremelyHot() {
        // 60s >= 1000
        StatResult stat = StatResult.of(50L, 1000L);
        assertEquals(HotspotLevel.EXTREMELY_HOT, detector.detect(stat));
    }

    @Test
    @DisplayName("EXTREMELY_HOT: 超高访问量应返回 EXTREMELY_HOT")
    void detect_VeryHighAccess_ReturnsExtremelyHot() {
        StatResult stat = StatResult.of(500L, 5000L);
        assertEquals(HotspotLevel.EXTREMELY_HOT, detector.detect(stat));
    }

    // ==================== 边界条件测试 ====================

    @Test
    @DisplayName("边界: 恰好在 WARM/COLD 边界应返回 WARM")
    void detect_ExactlyWarmThreshold_ReturnsWarm() {
        StatResult stat = StatResult.of(5L, 59L);
        assertEquals(HotspotLevel.WARM, detector.detect(stat));
    }

    @Test
    @DisplayName("边界: 恰好在 HOT/WARM 边界应返回 HOT")
    void detect_ExactlyHotThreshold_ReturnsHot() {
        StatResult stat = StatResult.of(20L, 299L);
        assertEquals(HotspotLevel.HOT, detector.detect(stat));
    }

    @Test
    @DisplayName("边界: 恰好在 EXTREMELY_HOT/HOT 边界应返回 EXTREMELY_HOT")
    void detect_ExactlyExtremelyHotThreshold_ReturnsExtremelyHot() {
        StatResult stat = StatResult.of(100L, 999L);
        assertEquals(HotspotLevel.EXTREMELY_HOT, detector.detect(stat));
    }

    // ==================== 优先级测试 ====================

    @Test
    @DisplayName("优先级: 当多个条件满足时取最高等级")
    void detect_MultipleConditionsMet_ReturnsHighest() {
        // 1s满足WARM(>=5), 60s满足HOT(>=300) -> 应返回HOT
        StatResult stat = StatResult.of(10L, 350L);
        assertEquals(HotspotLevel.HOT, detector.detect(stat));
    }

    // ==================== 配置变更测试 ====================

    @Test
    @DisplayName("配置: 修改阈值后检测结果应改变")
    void detect_CustomThreshold_ReturnsCorrectLevel() {
        // 自定义低阈值
        schedulerProperties.getHotspot().setHotShortThreshold(10L);
        schedulerProperties.getHotspot().setHotLongThreshold(100L);
        
        // 原本是 WARM，现在应该是 HOT
        StatResult stat = StatResult.of(10L, 50L);
        assertEquals(HotspotLevel.HOT, detector.detect(stat));
    }

    // ==================== getThreshold 测试 ====================

    @Test
    @DisplayName("getThreshold: 返回正确的阈值配置")
    void getThreshold_ReturnsCorrectValues() {
        assertEquals(0L, detector.getThreshold(HotspotLevel.COLD));
        assertEquals(60L, detector.getThreshold(HotspotLevel.WARM));
        assertEquals(300L, detector.getThreshold(HotspotLevel.HOT));
        assertEquals(1000L, detector.getThreshold(HotspotLevel.EXTREMELY_HOT));
    }
}
