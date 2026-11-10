package com.example.market.serviceImpl;

import com.example.market.mapper.MarketDataMapper;
import com.example.market.service.MarketDataService;
import com.example.market.vo.MarketBasisVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

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
