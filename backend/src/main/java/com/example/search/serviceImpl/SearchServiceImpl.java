package com.example.search.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.admin.dto.SystemSettingDTO;
import com.example.admin.service.SystemSettingService;
import com.example.market.service.MarketDataService;
import com.example.market.vo.MarketDealStat;
import com.example.product.constant.ProductConstant;
import com.example.product.service.ProductService;
import com.example.product.vo.ProductListVO;
import com.example.search.entity.SearchCorrection;
import com.example.search.entity.SearchLog;
import com.example.search.entity.SearchSuggestWord;
import com.example.search.mapper.SearchCorrectionMapper;
import com.example.search.mapper.SearchLogMapper;
import com.example.search.mapper.SearchStatMapper;
import com.example.search.mapper.SearchSuggestWordMapper;
import com.example.search.service.SearchLogService;
import com.example.search.service.SearchService;
import com.example.search.vo.SearchCorrectionVO;
import com.example.search.vo.SearchLogVO;
import com.example.search.vo.SearchPageVO;
import com.example.search.vo.SearchSuggestItemVO;
import com.example.search.vo.SearchSuggestResultVO;
import com.example.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 增强搜索服务实现类
 * <p>
 * 三个设计决策（对应 docs/012 方案）：
 * 1. 词库只存静态字典，成交热度/本校热度全部运行时统计——飞轮零成本接入；
 * 2. 纠错只走本地词表，精确/包含命中才纠，宁可不纠不可错纠；
 * 3. 行情标签整页批量计算（页内品类去重一次取明细），杜绝 N+1；
 * 埋点与成交归因全部 try/catch，任何失败只告警不影响搜索与支付主流程
 *
 * @author ZCode
 * @date 2026/11/11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService, SearchLogService {

    /** 补全候选默认上限 */
    private static final int DEFAULT_SUGGEST_LIMIT = 8;

    /** 成交归因窗口（天）：点击后 7 天内成交才算归因 */
    private static final int DEAL_ATTRIBUTION_DAYS = 7;

    /** 来源：词库前缀 */
    private static final String SOURCE_WORD_PREFIX = "WORD_PREFIX";

    /** 来源：拼音 */
    private static final String SOURCE_PINYIN = "PINYIN";

    /** 来源：本校热搜 */
    private static final String SOURCE_CAMPUS_HOT = "CAMPUS_HOT";

    /** 来源：纠错建议 */
    private static final String SOURCE_CORRECTION = "CORRECTION";

    private final SearchSuggestWordMapper suggestWordMapper;

    private final SearchCorrectionMapper correctionMapper;

    private final SearchLogMapper searchLogMapper;

    private final SearchStatMapper searchStatMapper;

    private final ProductService productService;

    private final MarketDataService marketDataService;

    private final SystemSettingService systemSettingService;

    @Override
    public SearchSuggestResultVO suggest(String q, Long campusId, int limit) {
        int max = limit <= 0 ? DEFAULT_SUGGEST_LIMIT : limit;
        SearchSuggestResultVO result = new SearchSuggestResultVO();
        String query = q == null ? "" : q.trim();
        if (query.isEmpty()) {
            return result;
        }
        // LinkedHashMap 按插入序去重：词为键，首次出现的来源优先
        Map<String, SearchSuggestItemVO> merged = new LinkedHashMap<>();
        Set<String> seen = new HashSet<>();

        // 1. 纠错建议（命中药表才给，词首挂 CORRECTION 来源）
        SearchCorrectionVO correction = findCorrection(query);
        if (correction != null && !correction.getCorrected().equalsIgnoreCase(query)) {
            merged.put(correction.getCorrected(),
                    new SearchSuggestItemVO(correction.getCorrected(), null, SOURCE_CORRECTION));
            seen.add(correction.getCorrected());
            result.setCorrection(correction);
        }

        // 2. 词库前缀命中（词本身前缀）
        List<SearchSuggestWord> wordHits = suggestWordMapper.selectList(
                new LambdaQueryWrapper<SearchSuggestWord>()
                        .eq(SearchSuggestWord::getStatus, 1)
                        .likeRight(SearchSuggestWord::getWord, query)
                        .orderByAsc(SearchSuggestWord::getId)
                        .last("LIMIT 5"));
        addWords(merged, seen, wordHits, SOURCE_WORD_PREFIX);

        // 3. 拼音前缀命中（全拼/首字母，如 jiaoc -> 教材）
        List<SearchSuggestWord> pinyinHits = suggestWordMapper.selectList(
                new LambdaQueryWrapper<SearchSuggestWord>()
                        .eq(SearchSuggestWord::getStatus, 1)
                        .likeRight(SearchSuggestWord::getPinyin, query)
                        .orderByAsc(SearchSuggestWord::getId)
                        .last("LIMIT 5"));
        addWords(merged, seen, pinyinHits, SOURCE_PINYIN);

        // 4. 本校热门搜索词（30 天、有结果、2~20 字，过滤掉与输入相同的词）
        if (campusId != null) {
            List<Map<String, Object>> hot = searchStatMapper.selectCampusHotWords(campusId, 3);
            for (Map<String, Object> row : hot) {
                Object keyword = row.get("keyword");
                if (keyword == null) {
                    continue;
                }
                String word = String.valueOf(keyword);
                if (word.equals(query) || seen.contains(word)) {
                    continue;
                }
                seen.add(word);
                merged.put(word, new SearchSuggestItemVO(word, null, SOURCE_CAMPUS_HOT));
            }
        }

        // 5. 成交热提示：对最终候选（≤8 词）实时统计「(本校)近期成交 N 件」
        List<SearchSuggestItemVO> items = new ArrayList<>(merged.values());
        for (SearchSuggestItemVO item : items) {
            if (item.getSource().equals(SOURCE_CAMPUS_HOT)) {
                // 热搜词本身就是热度信号，不重复打成交标签
                continue;
            }
            int dealCount = searchStatMapper.countDealByWord(item.getWord(), campusId);
            if (dealCount > 0) {
                item.setDealHint((campusId != null ? "本校近期成交" : "近期成交") + dealCount + "件");
            }
        }
        // 排序：来源优先级（纠错>词库>拼音>热搜）内保持插入序，同源成交热的靠前
        Map<String, Integer> sourceRank = new LinkedHashMap<>();
        sourceRank.put(SOURCE_CORRECTION, 0);
        sourceRank.put(SOURCE_WORD_PREFIX, 1);
        sourceRank.put(SOURCE_PINYIN, 2);
        sourceRank.put(SOURCE_CAMPUS_HOT, 3);
        items.sort(Comparator
                .comparingInt((SearchSuggestItemVO i) -> sourceRank.getOrDefault(i.getSource(), 9))
                .thenComparing(i -> i.getDealHint() == null ? 1 : 0));
        result.setItems(items.size() > max ? new ArrayList<>(items.subList(0, max)) : items);
        return result;
    }

    /**
     * 合并词库命中到候选集（跳过重复词与与输入相同的词）
     */
    private void addWords(Map<String, SearchSuggestItemVO> merged, Set<String> seen,
                          List<SearchSuggestWord> words, String source) {
        for (SearchSuggestWord w : words) {
            if (w.getWord() == null || seen.contains(w.getWord())) {
                continue;
            }
            seen.add(w.getWord());
            merged.put(w.getWord(), new SearchSuggestItemVO(w.getWord(), w.getTagLabel(), source));
        }
    }

    @Override
    public SearchCorrectionVO findCorrection(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        String q = keyword.trim();
        // 先精确命中，再包含命中（如「二手自形车」包含「自形车」）
        SearchCorrection hit = correctionMapper.selectOne(
                new LambdaQueryWrapper<SearchCorrection>()
                        .eq(SearchCorrection::getStatus, 1)
                        .eq(SearchCorrection::getWrongWord, q)
                        .last("LIMIT 1"));
        if (hit == null) {
            List<SearchCorrection> contains = correctionMapper.selectList(
                    new LambdaQueryWrapper<SearchCorrection>()
                            .eq(SearchCorrection::getStatus, 1)
                            .isNotNull(SearchCorrection::getWrongWord)
                            .apply("INSTR({0}, wrong_word) > 0", q)
                            .orderByAsc(SearchCorrection::getId)
                            .last("LIMIT 1"));
            hit = contains.isEmpty() ? null : contains.get(0);
        }
        if (hit == null || !StringUtils.hasText(hit.getCorrectWord())
                || hit.getCorrectWord().equals(q)) {
            return null;
        }
        return new SearchCorrectionVO(q, hit.getCorrectWord(), hit.getType());
    }

    @Override
    public SearchPageVO search(String keyword, Long campusId, String productCondition, Integer categoryId,
                               java.math.BigDecimal minPrice, java.math.BigDecimal maxPrice, String sort,
                               Integer page, Integer pageSize) {
        SearchPageVO vo = new SearchPageVO();
        // 1. 纠错（命中则用纠错词检索，响应携带纠错信息供前端提示）
        SearchCorrectionVO correction = findCorrection(keyword);
        vo.setCorrection(correction);
        String usedKeyword = correction != null ? correction.getCorrected() : keyword;
        // 2. 成色二次筛选为进阶能力：后台开关关闭时忽略该参数（宁缺毋假）
        String condition = null;
        if (StringUtils.hasText(productCondition) && isConditionFilterEnabled()
                && ProductConstant.VALID_CONDITIONS.contains(productCondition)) {
            condition = productCondition;
        }
        // 3. 复用既有商品搜索（原 /product/search 的服务层，不推翻重写）
        Page<ProductListVO> result = productService.searchProducts(
                usedKeyword, categoryId, campusId, minPrice, maxPrice, sort, condition,
                page == null ? 1 : page, pageSize == null ? 12 : pageSize);
        // 4. 行情标签：页内品类去重后一次批量取成交明细，内存算中位价与价格分位
        attachMarketHints(result.getRecords());
        vo.setPage(result);
        // 5. 写埋点（失败只告警）
        try {
            SearchLog searchLog = new SearchLog();
            searchLog.setUserId(UserHolder.getUserId());
            searchLog.setKeyword(StringUtils.hasText(keyword) ? keyword.trim() : null);
            searchLog.setCorrectedKeyword(correction == null ? null : correction.getCorrected());
            searchLog.setCampusId(campusId);
            searchLog.setResultCount((int) result.getTotal());
            searchLog.setDealFlag(0);
            searchLogMapper.insert(searchLog);
            vo.setLogId(searchLog.getId());
        } catch (Exception e) {
            log.warn("搜索埋点写入失败（不影响搜索结果）：{}", e.getMessage());
        }
        return vo;
    }

    /**
     * 给整页搜索结果附加行情标签：
     * marketMedian = 该品类近 90 天成交中位价（不分成色，用于卡片小字）；
     * pricePosition = 「低于该商品同品类同成色近期 X% 成交价」，样本不足为 null
     */
    private void attachMarketHints(List<ProductListVO> records) {
        try {
            if (records == null || records.isEmpty()) {
                return;
            }
            List<Integer> categoryIds = records.stream()
                    .map(ProductListVO::getCategoryId)
                    .filter(java.util.Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            if (categoryIds.isEmpty()) {
                return;
            }
            List<MarketDealStat> stats = marketDataService.getDealStats(categoryIds);
            if (stats.isEmpty()) {
                return;
            }
            // 品类 → 全部成交价（不分成色，取中位价做卡片小字）
            Map<Integer, List<BigDecimal>> byCategory = stats.stream()
                    .collect(Collectors.groupingBy(MarketDealStat::getCategoryId,
                            Collectors.mapping(MarketDealStat::getPrice,
                                    Collectors.collectingAndThen(Collectors.toList(), l -> {
                                        l.sort(BigDecimal::compareTo);
                                        return l;
                                    }))));
            // 品类×成色 → 成交价（算价格分位）
            Map<String, List<BigDecimal>> byCategoryCondition = stats.stream()
                    .collect(Collectors.groupingBy(s -> s.getCategoryId() + "|" + s.getProductCondition(),
                            Collectors.mapping(MarketDealStat::getPrice,
                                    Collectors.collectingAndThen(Collectors.toList(), l -> {
                                        l.sort(BigDecimal::compareTo);
                                        return l;
                                    }))));
            for (ProductListVO item : records) {
                if (item.getCategoryId() == null) {
                    continue;
                }
                List<BigDecimal> categoryPrices = byCategory.get(item.getCategoryId());
                if (categoryPrices != null && !categoryPrices.isEmpty()) {
                    item.setMarketMedian(medianOf(categoryPrices));
                }
                String key = item.getCategoryId() + "|" + item.getProductCondition();
                List<BigDecimal> condPrices = byCategoryCondition.get(key);
                if (condPrices != null && !condPrices.isEmpty() && item.getPrice() != null) {
                    long above = condPrices.stream().filter(p -> p.compareTo(item.getPrice()) > 0).count();
                    item.setPricePosition((int) Math.round(above * 100.0 / condPrices.size()));
                }
            }
        } catch (Exception e) {
            log.warn("搜索结果行情标签计算失败（不影响搜索结果）：{}", e.getMessage());
        }
    }

    /**
     * 中位数（升序样本，偶数取中间两数平均，两位小数）
     */
    private BigDecimal medianOf(List<BigDecimal> sortedPrices) {
        int n = sortedPrices.size();
        BigDecimal mid = n % 2 == 1
                ? sortedPrices.get(n / 2)
                : sortedPrices.get(n / 2 - 1).add(sortedPrices.get(n / 2))
                        .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        return mid.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public void recordClick(Long logId, Long productId) {
        if (logId == null || productId == null) {
            return;
        }
        try {
            searchLogMapper.update(null, new LambdaUpdateWrapper<SearchLog>()
                    .eq(SearchLog::getId, logId)
                    .set(SearchLog::getClickedProductId, productId));
        } catch (Exception e) {
            log.warn("点击信标回写失败（静默忽略）：{}", e.getMessage());
        }
    }

    @Override
    public void markDeals(List<Long> productIds) {
        try {
            if (productIds == null || productIds.isEmpty()) {
                return;
            }
            searchLogMapper.update(null, new LambdaUpdateWrapper<SearchLog>()
                    .in(SearchLog::getClickedProductId, productIds)
                    .eq(SearchLog::getDealFlag, 0)
                    .apply("create_time >= DATE_SUB(NOW(), INTERVAL " + DEAL_ATTRIBUTION_DAYS + " DAY)")
                    .set(SearchLog::getDealFlag, 1));
            log.info("搜索埋点成交归因完成，商品：{}", productIds);
        } catch (Exception e) {
            log.warn("搜索埋点成交归因失败（不影响支付主流程）：{}", e.getMessage());
        }
    }

    @Override
    public List<SearchLogVO> recentLogs(String keyword, int limit) {
        LambdaQueryWrapper<SearchLog> wrapper = new LambdaQueryWrapper<SearchLog>()
                .orderByDesc(SearchLog::getId)
                .last("LIMIT " + Math.max(1, Math.min(limit, 50)));
        if (StringUtils.hasText(keyword)) {
            wrapper.eq(SearchLog::getKeyword, keyword.trim());
        }
        return searchLogMapper.selectList(wrapper).stream().map(l -> {
            SearchLogVO vo = new SearchLogVO();
            vo.setId(l.getId());
            vo.setKeyword(l.getKeyword());
            vo.setCorrectedKeyword(l.getCorrectedKeyword());
            vo.setCampusId(l.getCampusId());
            vo.setResultCount(l.getResultCount());
            vo.setClickedProductId(l.getClickedProductId());
            vo.setDealFlag(l.getDealFlag());
            vo.setCreateTime(l.getCreateTime());
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 成色二次筛选开关（进阶能力，缺省关闭；老配置缺字段按缺省处理）
     */
    private boolean isConditionFilterEnabled() {
        SystemSettingDTO settings = systemSettingService.get();
        return settings.getTrade() != null
                && Boolean.TRUE.equals(settings.getTrade().getSearchConditionFilter());
    }
}
