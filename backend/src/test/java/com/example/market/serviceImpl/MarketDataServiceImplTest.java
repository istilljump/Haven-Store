package com.example.market.serviceImpl;

import com.example.market.mapper.MarketDataMapper;
import com.example.market.vo.MarketBasisVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * 成交行情服务单元测试
 * <p>
 * 覆盖行情基线聚合（中位数/区间/样本量）、价格分位（划算指数口径）
 * 与样本不足/参数缺失的降级行为。纯统计计算，断言精确值
 *
 * @author ZCode
 * @date 2026/11/10
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("成交行情服务单元测试")
class MarketDataServiceImplTest {

    @Mock
    private MarketDataMapper marketDataMapper;

    @InjectMocks
    private MarketDataServiceImpl marketDataService;

    @Test
    @DisplayName("行情基线 - 奇数样本取中间值（中位价 3600）")
    void getBasis_oddSample() {
        when(marketDataMapper.selectDealPrices(1, "九成新", 90))
                .thenReturn(toPrices("2900", "3300", "3600", "3900", "4200"));

        MarketBasisVO basis = marketDataService.getBasis(1, "九成新");

        assertEquals("MARKET_DATA", basis.getBasisType());
        assertEquals(5, basis.getSampleSize());
        assertEquals(90, basis.getDays());
        assertEquals(0, basis.getMinPrice().compareTo(new BigDecimal("2900")));
        assertEquals(0, basis.getMaxPrice().compareTo(new BigDecimal("4200")));
        assertEquals(0, basis.getMedianPrice().compareTo(new BigDecimal("3600.00")));
    }

    @Test
    @DisplayName("行情基线 - 偶数样本取中间两数平均（中位价 25.00）")
    void getBasis_evenSample() {
        when(marketDataMapper.selectDealPrices(3, "八成新", 90))
                .thenReturn(toPrices("15", "22", "28", "30"));

        MarketBasisVO basis = marketDataService.getBasis(3, "八成新");

        assertEquals(4, basis.getSampleSize());
        assertEquals(0, basis.getMedianPrice().compareTo(new BigDecimal("25.00")));
    }

    @Test
    @DisplayName("行情基线 - 窗口内无成交返回 null（调用方降级）")
    void getBasis_emptyPrices() {
        when(marketDataMapper.selectDealPrices(anyInt(), anyString(), anyInt()))
                .thenReturn(Collections.emptyList());

        assertNull(marketDataService.getBasis(1, "七成新及以下"));
    }

    @Test
    @DisplayName("行情基线 - 参数缺失直接返回 null，不打库")
    void getBasis_nullParams() {
        assertNull(marketDataService.getBasis(null, "九成新"));
        assertNull(marketDataService.getBasis(1, null));
        assertNull(marketDataService.getBasis(1, " "));
    }

    @Test
    @DisplayName("价格分位 - 低于 3/5 件成交价为 60（划算指数口径）")
    void cheaperThanPercent_basic() {
        when(marketDataMapper.selectDealPrices(1, "九成新", 90))
                .thenReturn(toPrices("2900", "3300", "3600", "3900", "4200"));

        Integer percent = marketDataService.cheaperThanPercent(1, "九成新", new BigDecimal("3500"));

        assertEquals(60, percent);
    }

    @Test
    @DisplayName("价格分位 - 高于全部成交为 0，等于中位价为 33（3 件中 1 件更高）")
    void cheaperThanPercent_boundary() {
        when(marketDataMapper.selectDealPrices(6, "八成新", 90))
                .thenReturn(toPrices("45", "65", "160"));

        // 高于全部成交（999 > 160）→ 没有任何成交比它高 → 0
        assertEquals(0, marketDataService.cheaperThanPercent(6, "八成新", new BigDecimal("999")));
        // 等于中位价 65 → 上方仅 160 一件 → 1/3 ≈ 33
        assertEquals(33, marketDataService.cheaperThanPercent(6, "八成新", new BigDecimal("65")));
        // 等于最高价 160 → 没有成交比它高 → 0
        assertEquals(0, marketDataService.cheaperThanPercent(6, "八成新", new BigDecimal("160")));
    }

    @Test
    @DisplayName("价格分位 - 无样本或参数缺失返回 null")
    void cheaperThanPercent_degraded() {
        when(marketDataMapper.selectDealPrices(anyInt(), anyString(), anyInt()))
                .thenReturn(Collections.emptyList());

        assertNull(marketDataService.cheaperThanPercent(1, "九成新", new BigDecimal("100")));
        assertNull(marketDataService.cheaperThanPercent(null, "九成新", new BigDecimal("100")));
        assertNull(marketDataService.cheaperThanPercent(1, "九成新", null));
    }

    @Test
    @DisplayName("行情趋势 - 按日聚合计数与中位价，日期升序")
    void trend_dailyAggregation() {
        List<Map<String, Object>> rows = Arrays.asList(
                trendRow(java.sql.Date.valueOf("2026-11-02"), "200"),
                trendRow(java.sql.Date.valueOf("2026-11-01"), "100"),
                trendRow(java.sql.Date.valueOf("2026-11-01"), "300"));
        when(marketDataMapper.selectTrendDeals(1, "九成新", 90)).thenReturn(rows);

        List<com.example.market.vo.MarketTrendPointVO> points =
                marketDataService.getPriceTrend(1, "九成新", 90);

        assertEquals(2, points.size());
        assertEquals("2026-11-01", points.get(0).getDate());
        assertEquals(2, points.get(0).getCount());
        assertEquals(0, points.get(0).getMedianPrice().compareTo(new BigDecimal("200.00")));
        assertEquals("2026-11-02", points.get(1).getDate());
        assertEquals(1, points.get(1).getCount());
    }

    @Test
    @DisplayName("行情趋势 - 非法窗口或无成交返回空列表")
    void trend_degraded() {
        assertTrue(marketDataService.getPriceTrend(1, "九成新", 0).isEmpty());
        assertTrue(marketDataService.getPriceTrend(null, "九成新", 90).isEmpty());
        when(marketDataMapper.selectTrendDeals(1, "七成新及以下", 90))
                .thenReturn(Collections.emptyList());
        assertTrue(marketDataService.getPriceTrend(1, "七成新及以下", 90).isEmpty());
    }

    /**
     * 快捷构造趋势明细行（dealDate + price）
     */
    private Map<String, Object> trendRow(java.sql.Date dealDate, String price) {
        Map<String, Object> row = new HashMap<>();
        row.put("dealDate", dealDate);
        row.put("price", new BigDecimal(price));
        return row;
    }

    /**
     * 快捷构造 BigDecimal 价格列表
     */
    private List<BigDecimal> toPrices(String... values) {
        return Arrays.stream(values).map(BigDecimal::new).collect(java.util.stream.Collectors.toList());
    }
}
