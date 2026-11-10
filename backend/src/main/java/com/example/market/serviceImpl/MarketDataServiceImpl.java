package com.example.market.serviceImpl;

import com.example.market.mapper.MarketDataMapper;
import com.example.market.service.MarketDataService;
import com.example.market.vo.MarketBasisVO;
import com.example.market.vo.MarketCategoryStatVO;
import com.example.market.vo.MarketDealStat;
import com.example.market.vo.MarketTrendPointVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 成交行情服务实现类
 * <p>
 * 纯统计口径（中位数 + 分位数），不引入外部 AI 依赖：
 * 可解释（依据原样透出）、可测试（纯函数计算）、可复现（同数据同结果），
 * 与项目「零外部服务、行为有断言」的交付承诺一致
 *
 * @author ZCode
 * @date 2026/11/10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarketDataServiceImpl implements MarketDataService {

    /** 行情统计窗口（天）：只看近三个月的成交，过期成交不再参与定价 */
    public static final int STATISTICS_WINDOW_DAYS = 90;

    /** 行情依据类型：有成交样本 */
    public static final String BASIS_TYPE_MARKET = "MARKET_DATA";

    /** 成交行情数据访问对象 */
    private final MarketDataMapper marketDataMapper;

    @Override
    public MarketBasisVO getBasis(Integer categoryId, String condition) {
        if (categoryId == null || !StringUtils.hasText(condition)) {
            return null;
        }
        List<BigDecimal> prices = marketDataMapper.selectDealPrices(
                categoryId, condition, STATISTICS_WINDOW_DAYS);
        if (prices == null || prices.isEmpty()) {
            return null;
        }
        MarketBasisVO basis = new MarketBasisVO();
        basis.setBasisType(BASIS_TYPE_MARKET);
        basis.setSampleSize(prices.size());
        basis.setDays(STATISTICS_WINDOW_DAYS);
        basis.setMinPrice(prices.get(0));
        basis.setMaxPrice(prices.get(prices.size() - 1));
        basis.setMedianPrice(median(prices));
        return basis;
    }

    @Override
    public Integer cheaperThanPercent(Integer categoryId, String condition, BigDecimal price) {
        if (categoryId == null || !StringUtils.hasText(condition) || price == null) {
            return null;
        }
        List<BigDecimal> prices = marketDataMapper.selectDealPrices(
                categoryId, condition, STATISTICS_WINDOW_DAYS);
        if (prices == null || prices.isEmpty()) {
            return null;
        }
        // 「低于该价格」= 成交价严格大于该价格的样本占比，与「低于近期 X% 成交价」的文案语义一致
        long above = prices.stream().filter(p -> p.compareTo(price) > 0).count();
        return (int) Math.round(above * 100.0 / prices.size());
    }

    @Override
    public List<MarketDealStat> getDealStats(List<Integer> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return new ArrayList<>();
        }
        return marketDataMapper.selectDealStats(categoryIds, STATISTICS_WINDOW_DAYS);
    }

    @Override
    public List<MarketCategoryStatVO> getCampusTopCategories(Long campusId, int topN) {
        if (campusId == null || topN <= 0) {
            return new ArrayList<>();
        }
        List<Map<String, Object>> rows = marketDataMapper.selectCampusCategoryDeals(
                campusId, STATISTICS_WINDOW_DAYS);
        if (rows == null || rows.isEmpty()) {
            return new ArrayList<>();
        }
        // 按品类聚合：件数 + 中位价（rows 量级 = 种子成交几十条，内存聚合毫秒级）
        Map<Object, List<Map<String, Object>>> grouped = rows.stream()
                .filter(r -> r.get("categoryId") != null)
                .collect(Collectors.groupingBy(r -> r.get("categoryId"), LinkedHashMap::new, Collectors.toList()));
        List<MarketCategoryStatVO> stats = new ArrayList<>();
        for (Map.Entry<Object, List<Map<String, Object>>> entry : grouped.entrySet()) {
            List<BigDecimal> prices = entry.getValue().stream()
                    .map(r -> toBigDecimal(r.get("price")))
                    .filter(p -> p != null)
                    .sorted()
                    .collect(Collectors.toList());
            if (prices.isEmpty()) {
                continue;
            }
            MarketCategoryStatVO vo = new MarketCategoryStatVO();
            vo.setCategoryId(((Number) entry.getKey()).intValue());
            Object name = entry.getValue().get(0).get("categoryName");
            vo.setCategoryName(name == null ? null : String.valueOf(name));
            vo.setDealCount(prices.size());
            vo.setMedianPrice(median(prices));
            stats.add(vo);
        }
        // 成交件数倒序，并列按品类 ID 升序（确定性，可断言）
        stats.sort(Comparator.comparingInt(MarketCategoryStatVO::getDealCount).reversed()
                .thenComparingInt(MarketCategoryStatVO::getCategoryId));
        return stats.size() > topN ? new ArrayList<>(stats.subList(0, topN)) : stats;
    }

    @Override
    public List<MarketTrendPointVO> getPriceTrend(Integer categoryId, String condition, int days) {
        if (categoryId == null || !StringUtils.hasText(condition) || days <= 0) {
            return new ArrayList<>();
        }
        int window = Math.min(days, 365);
        List<Map<String, Object>> rows = marketDataMapper.selectTrendDeals(categoryId, condition, window);
        if (rows == null || rows.isEmpty()) {
            return new ArrayList<>();
        }
        // 按成交日聚合：当日件数 + 当日中位价；只返回有成交的日期点，日期升序
        Map<String, List<BigDecimal>> byDate = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            Object date = row.get("dealDate");
            BigDecimal price = toBigDecimal(row.get("price"));
            if (date == null || price == null) {
                continue;
            }
            String day = String.valueOf(date).substring(0, 10);
            byDate.computeIfAbsent(day, k -> new ArrayList<>()).add(price);
        }
        List<MarketTrendPointVO> points = new ArrayList<>();
        for (Map.Entry<String, List<BigDecimal>> entry : byDate.entrySet()) {
            List<BigDecimal> prices = entry.getValue().stream().sorted().collect(Collectors.toList());
            MarketTrendPointVO point = new MarketTrendPointVO();
            point.setDate(entry.getKey());
            point.setCount(prices.size());
            point.setMedianPrice(median(prices));
            points.add(point);
        }
        // 日期升序（SQL 已按 pay_time 排序，但聚合后仍显式排序以保证契约稳定）
        points.sort(Comparator.comparing(MarketTrendPointVO::getDate));
        return points;
    }

    /**
     * Map 行的成交价安全转 BigDecimal
     */
    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 中位数：偶数样本取中间两数的平均，保留两位小数（与 DECIMAL(10,2) 对齐）
     */
    private BigDecimal median(List<BigDecimal> sortedPrices) {
        int n = sortedPrices.size();
        BigDecimal mid;
        if (n % 2 == 1) {
            mid = sortedPrices.get(n / 2);
        } else {
            mid = sortedPrices.get(n / 2 - 1).add(sortedPrices.get(n / 2))
                    .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        }
        return mid.setScale(2, RoundingMode.HALF_UP);
    }
}
