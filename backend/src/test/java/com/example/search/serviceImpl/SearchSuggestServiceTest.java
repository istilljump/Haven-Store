package com.example.search.serviceImpl;

import com.example.admin.dto.SystemSettingDTO;
import com.example.admin.service.SystemSettingService;
import com.example.market.service.MarketDataService;
import com.example.product.constant.ProductConstant;
import com.example.product.service.ProductService;
import com.example.product.vo.ProductListVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.search.entity.SearchCorrection;
import com.example.search.entity.SearchSuggestWord;
import com.example.search.mapper.SearchCorrectionMapper;
import com.example.search.mapper.SearchLogMapper;
import com.example.search.mapper.SearchStatMapper;
import com.example.search.mapper.SearchSuggestWordMapper;
import com.example.search.vo.SearchCorrectionVO;
import com.example.search.vo.SearchPageVO;
import com.example.search.vo.SearchSuggestResultVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * 增强搜索单元测试
 * <p>
 * 覆盖补全三级来源排序、拼音命中、成交热提示、本地词表纠错、
 * 成色二次筛选开关（进阶能力）与行情标签批量计算
 *
 * @author ZCode
 * @date 2026/11/11
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("增强搜索单元测试")
class SearchSuggestServiceTest {

    @Mock
    private SearchSuggestWordMapper suggestWordMapper;

    @Mock
    private SearchCorrectionMapper correctionMapper;

    @Mock
    private SearchLogMapper searchLogMapper;

    @Mock
    private SearchStatMapper searchStatMapper;

    @Mock
    private ProductService productService;

    @Mock
    private MarketDataService marketDataService;

    @Mock
    private SystemSettingService systemSettingService;

    @InjectMocks
    private SearchServiceImpl searchService;

    @Test
    @DisplayName("补全 - 词库前缀命中第一优先，附品类标签")
    void suggest_wordPrefixFirst() {
        when(correctionMapper.selectOne(any())).thenReturn(null);
        when(correctionMapper.selectList(any())).thenReturn(new ArrayList<>());
        when(suggestWordMapper.selectList(any())).thenReturn(Arrays.asList(word("教材", "图书教材", "jiaocai,jc")));
        when(searchStatMapper.countDealByWord(eq("教材"), isNull())).thenReturn(2);

        SearchSuggestResultVO result = searchService.suggest("教", null, 8);

        assertEquals(1, result.getItems().size());
        assertEquals("教材", result.getItems().get(0).getWord());
        assertEquals("图书教材", result.getItems().get(0).getTagLabel());
        assertEquals("近期成交2件", result.getItems().get(0).getDealHint());
        assertNull(result.getCorrection());
    }

    @Test
    @DisplayName("补全 - 拼音前缀命中（jiaoc → 教材），带本校成交提示")
    void suggest_pinyinPrefix() {
        when(correctionMapper.selectOne(any())).thenReturn(null);
        when(correctionMapper.selectList(any())).thenReturn(new ArrayList<>());
        when(suggestWordMapper.selectList(any())).thenReturn(new ArrayList<>())
                .thenReturn(Arrays.asList(word("教材", "图书教材", "jiaocai,jc")));
        when(searchStatMapper.countDealByWord(eq("教材"), eq(1L))).thenReturn(12);

        SearchSuggestResultVO result = searchService.suggest("jiaoc", 1L, 8);

        assertEquals(1, result.getItems().size());
        assertEquals("教材", result.getItems().get(0).getWord());
        assertEquals("PINYIN", result.getItems().get(0).getSource());
        assertEquals("本校近期成交12件", result.getItems().get(0).getDealHint());
    }

    @Test
    @DisplayName("补全 - 本校热搜词进入候选且不重复打成交标签")
    void suggest_campusHotWords() {
        when(correctionMapper.selectOne(any())).thenReturn(null);
        when(correctionMapper.selectList(any())).thenReturn(new ArrayList<>());
        when(suggestWordMapper.selectList(any())).thenReturn(new ArrayList<>());
        Map<String, Object> hot = new HashMap<>();
        hot.put("keyword", "吉他");
        hot.put("cnt", 5L);
        when(searchStatMapper.selectCampusHotWords(eq(1L), anyInt())).thenReturn(Arrays.asList(hot));

        SearchSuggestResultVO result = searchService.suggest("热", 1L, 8);

        assertEquals(1, result.getItems().size());
        assertEquals("吉他", result.getItems().get(0).getWord());
        assertEquals("CAMPUS_HOT", result.getItems().get(0).getSource());
        assertNull(result.getItems().get(0).getDealHint());
    }

    @Test
    @DisplayName("纠错 - 精确命中（自形车 → 自行车）")
    void correction_exactHit() {
        when(correctionMapper.selectOne(any()))
                .thenReturn(correction("自形车", "自行车", "SHAPE"));

        SearchCorrectionVO vo = searchService.findCorrection("自形车");

        assertNotNull(vo);
        assertEquals("自行车", vo.getCorrected());
        assertEquals("SHAPE", vo.getType());
    }

    @Test
    @DisplayName("纠错 - 未命中返回 null，宁可不纠不可错纠")
    void correction_miss() {
        when(correctionMapper.selectOne(any())).thenReturn(null);
        when(correctionMapper.selectList(any())).thenReturn(new ArrayList<>());

        assertNull(searchService.findCorrection("完全无关的词"));
    }

    @Test
    @DisplayName("增强搜索 - 纠错后用纠正词检索并在响应中携带纠错信息")
    void search_usesCorrectedKeyword() {
        when(correctionMapper.selectOne(any()))
                .thenReturn(correction("自形车", "自行车", "SHAPE"));
        when(systemSettingService.get()).thenReturn(settingsWith(false));
        Page<ProductListVO> page = new Page<>(1, 12, 0);
        when(productService.searchProducts(eq("自行车"), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), eq(1), eq(12))).thenReturn(page);
        lenient().when(marketDataService.getDealStats(any())).thenReturn(new ArrayList<>());

        SearchPageVO vo = searchService.search("自形车", null, null, null, null, null, null, 1, 12);

        assertNotNull(vo.getCorrection());
        assertEquals("自行车", vo.getCorrection().getCorrected());
        assertNotNull(vo.getPage());
    }

    @Test
    @DisplayName("增强搜索 - 成色二次筛选开关关闭时忽略该参数（进阶能力缺省关闭）")
    void search_conditionFilterDisabled() {
        when(systemSettingService.get()).thenReturn(settingsWith(false));
        when(correctionMapper.selectOne(any())).thenReturn(null);
        when(correctionMapper.selectList(any())).thenReturn(new ArrayList<>());
        Page<ProductListVO> page = new Page<>(1, 12, 0);
        when(productService.searchProducts(isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), eq(1), eq(12))).thenReturn(page);
        lenient().when(marketDataService.getDealStats(any())).thenReturn(new ArrayList<>());

        searchService.search(null, null, "全新", null, null, null, null, 1, 12);

        // 条件里的第四个参数（productCondition）应为 null：开关关闭被忽略
        assertEquals(0, page.getTotal());
    }

    @Test
    @DisplayName("增强搜索 - 开关开启时成色参数生效")
    void search_conditionFilterEnabled() {
        when(systemSettingService.get()).thenReturn(settingsWith(true));
        when(correctionMapper.selectOne(any())).thenReturn(null);
        when(correctionMapper.selectList(any())).thenReturn(new ArrayList<>());
        Page<ProductListVO> page = new Page<>(1, 12, 0);
        when(productService.searchProducts(isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), eq("全新"), eq(1), eq(12))).thenReturn(page);
        lenient().when(marketDataService.getDealStats(any())).thenReturn(new ArrayList<>());

        SearchPageVO vo = searchService.search(null, null, "全新", null, null, null, null, 1, 12);

        assertNotNull(vo.getPage());
        assertTrue(ProductConstant.VALID_CONDITIONS.contains("全新"));
    }

    @Test
    @DisplayName("行情标签 - 页内批量计算品类中位价与价格分位（无 N+1）")
    void search_marketHintBatch() {
        when(systemSettingService.get()).thenReturn(settingsWith(false));
        when(correctionMapper.selectOne(any())).thenReturn(null);
        when(correctionMapper.selectList(any())).thenReturn(new ArrayList<>());
        ProductListVO item = new ProductListVO();
        item.setId(18L);
        item.setCategoryId(3);
        item.setProductCondition("八成新");
        item.setPrice(new BigDecimal("45"));
        Page<ProductListVO> page = new Page<>(1, 12, 1);
        page.setRecords(Arrays.asList(item));
        when(productService.searchProducts(isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), eq(1), eq(12))).thenReturn(page);
        // 图书教材·八成新 成交价 [15,22,28,30]：中位价 25，45 高于全部 → 分位 0
        when(marketDataService.getDealStats(any())).thenReturn(Arrays.asList(
                new com.example.market.vo.MarketDealStat(3, "八成新", new BigDecimal("15")),
                new com.example.market.vo.MarketDealStat(3, "八成新", new BigDecimal("22")),
                new com.example.market.vo.MarketDealStat(3, "八成新", new BigDecimal("28")),
                new com.example.market.vo.MarketDealStat(3, "八成新", new BigDecimal("30"))));

        SearchPageVO vo = searchService.search(null, null, null, null, null, null, null, 1, 12);

        assertEquals(0, vo.getPage().getRecords().get(0).getMarketMedian()
                .compareTo(new BigDecimal("25.00")));
        assertEquals(0, vo.getPage().getRecords().get(0).getPricePosition());
    }

    @Test
    @DisplayName("空输入的补全返回空结果，不查库不报错")
    void suggest_blankInput() {
        SearchSuggestResultVO result = searchService.suggest("  ", null, 8);

        assertNotNull(result);
        assertTrue(result.getItems().isEmpty());
        assertNull(result.getCorrection());
        assertFalse(result.getItems() == null);
    }

    private SearchSuggestWord word(String word, String tag, String pinyin) {
        SearchSuggestWord w = new SearchSuggestWord();
        w.setWord(word);
        w.setTagLabel(tag);
        w.setPinyin(pinyin);
        w.setStatus(1);
        return w;
    }

    private SearchCorrection correction(String wrong, String correct, String type) {
        SearchCorrection c = new SearchCorrection();
        c.setWrongWord(wrong);
        c.setCorrectWord(correct);
        c.setType(type);
        c.setStatus(1);
        return c;
    }

    private SystemSettingDTO settingsWith(Boolean conditionFilter) {
        SystemSettingDTO dto = SystemSettingDTO.defaults();
        dto.getTrade().setSearchConditionFilter(conditionFilter);
        return dto;
    }
}
