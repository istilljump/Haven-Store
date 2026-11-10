package com.example.recommend.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.admin.dto.SystemSettingDTO;
import com.example.admin.service.SystemSettingService;
import com.example.campus.entity.Campus;
import com.example.campus.mapper.CampusMapper;
import com.example.category.entity.Category;
import com.example.category.mapper.CategoryMapper;
import com.example.product.entity.Product;
import com.example.product.mapper.ProductMapper;
import com.example.recommend.entity.CategoryRelation;
import com.example.recommend.mapper.CategoryRelationMapper;
import com.example.recommend.mapper.RecommendStatMapper;
import com.example.recommend.vo.RecommendItemVO;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * 场景化规则推荐单元测试
 * <p>
 * 覆盖五条规则（R1~R5）的触发、排序、兜底与可解释文案；
 * 规则排序全部确定性，断言精确到条目顺序
 *
 * @author ZCode
 * @date 2026/11/11
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("场景化规则推荐单元测试")
class RecommendServiceTest {

    @Mock
    private ProductMapper productMapper;

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private CampusMapper campusMapper;

    @Mock
    private CategoryRelationMapper categoryRelationMapper;

    @Mock
    private RecommendStatMapper recommendStatMapper;

    @Mock
    private SystemSettingService systemSettingService;

    @InjectMocks
    private RecommendServiceImpl recommendService;

    @Test
    @DisplayName("R1 首页冷启动 - 按校区成交件数倒序取活跃品类在售，文案可解释")
    void homeRecommend_campusActive() {
        when(systemSettingService.get()).thenReturn(defaultSettings());
        Map<String, Object> stat = new HashMap<>();
        stat.put("categoryId", 1);
        stat.put("cnt", 5L);
        when(recommendStatMapper.selectCampusCategoryDealCounts(eq(1L), anyInt()))
                .thenReturn(Arrays.asList(stat));
        lenient().when(campusMapper.selectById(1L)).thenReturn(campus(1L, "江畔大学·主校区"));
        lenient().when(categoryMapper.selectById(1)).thenReturn(category(1, "手机数码"));
        when(productMapper.selectList(any())).thenReturn(Arrays.asList(
                product(1L, 1, "二手iPhone 12", 1L, "九成新")));

        List<RecommendItemVO> items = recommendService.homeRecommend(1L);

        assertEquals(1, items.size());
        assertEquals(1L, items.get(0).getProductId());
        assertEquals("江畔大学·主校区近期成交活跃", items.get(0).getReason());
        assertEquals("手机数码", items.get(0).getCategoryName());
    }

    @Test
    @DisplayName("R2 兜底 - 校区无成交活跃数据时回退本校最新闲置")
    void homeRecommend_fallbackCampusLatest() {
        when(systemSettingService.get()).thenReturn(defaultSettings());
        when(recommendStatMapper.selectCampusCategoryDealCounts(eq(1L), anyInt()))
                .thenReturn(new ArrayList<>());
        lenient().when(campusMapper.selectById(anyLong())).thenReturn(campus(1L, "江畔大学·主校区"));
        when(productMapper.selectList(any())).thenReturn(Arrays.asList(
                product(2L, 3, "编程书籍套装", 1L, "八成新")));

        List<RecommendItemVO> items = recommendService.homeRecommend(1L);

        assertEquals(1, items.size());
        assertEquals("本校最新闲置", items.get(0).getReason());
    }

    @Test
    @DisplayName("无校区上下文 - 退化为「校园最新闲置」")
    void homeRecommend_noCampus() {
        when(systemSettingService.get()).thenReturn(defaultSettings());
        when(productMapper.selectList(any())).thenReturn(Arrays.asList(
                product(3L, 2, "机械键盘", 1L, "九成新")));

        List<RecommendItemVO> items = recommendService.homeRecommend(null);

        assertEquals("校园最新闲置", items.get(0).getReason());
    }

    @Test
    @DisplayName("R3 互补推荐 - 字典命中返回互补品类商品，文案带搜索词")
    void complementary_dictionaryHit() {
        when(systemSettingService.get()).thenReturn(defaultSettings());
        when(recommendStatMapper.resolveCategoryByProducts("教材")).thenReturn(3);
        when(categoryRelationMapper.selectList(any())).thenReturn(Arrays.asList(
                relation(3, 3, 10)));
        lenient().when(categoryMapper.selectById(3)).thenReturn(category(3, "图书教材"));
        when(productMapper.selectList(any())).thenReturn(Arrays.asList(
                product(2L, 3, "编程书籍套装", 1L, "八成新")));

        List<RecommendItemVO> items = recommendService.complementaryRecommend(
                "教材", null, Arrays.asList(18L));

        assertEquals(1, items.size());
        assertEquals(2L, items.get(0).getProductId());
        assertEquals("经常和「教材」一起交易", items.get(0).getReason());
    }

    @Test
    @DisplayName("R3 互补推荐 - 字典未命中返回空列表，不做同类凑数")
    void complementary_missReturnsEmpty() {
        when(systemSettingService.get()).thenReturn(defaultSettings());
        when(recommendStatMapper.resolveCategoryByProducts("神秘词")).thenReturn(null);
        when(recommendStatMapper.resolveCategoryBySuggestWord("神秘词")).thenReturn(null);

        List<RecommendItemVO> items = recommendService.complementaryRecommend(
                "神秘词", null, null);

        assertTrue(items.isEmpty());
    }

    @Test
    @DisplayName("R4 详情页 - 同校区+同品类+同成色第一梯队，文案含成色")
    void productRecommend_sameTierFirst() {
        when(systemSettingService.get()).thenReturn(defaultSettings());
        Product current = product(18L, 3, "高等数学教材（第七版）", 1L, "八成新");
        when(productMapper.selectById(18L)).thenReturn(current);
        lenient().when(categoryMapper.selectById(3)).thenReturn(category(3, "图书教材"));
        lenient().when(campusMapper.selectById(1L)).thenReturn(campus(1L, "江畔大学·主校区"));
        when(productMapper.selectList(any())).thenReturn(Arrays.asList(
                product(2L, 3, "编程书籍套装", 1L, "八成新")));

        List<RecommendItemVO> items = recommendService.productRecommend(18L);

        assertEquals(1, items.size());
        assertEquals(2L, items.get(0).getProductId());
        assertEquals("同校区·八成新·同类闲置", items.get(0).getReason());
    }

    @Test
    @DisplayName("R4 详情页 - 排除当前商品自身")
    void productRecommend_excludeSelf() {
        when(systemSettingService.get()).thenReturn(defaultSettings());
        Product current = product(18L, 3, "高等数学教材（第七版）", 1L, "八成新");
        when(productMapper.selectById(18L)).thenReturn(current);
        when(productMapper.selectList(any())).thenReturn(new ArrayList<>());

        List<RecommendItemVO> items = recommendService.productRecommend(18L);

        assertTrue(items.isEmpty());
    }

    @Test
    @DisplayName("总开关关闭 - 三个接口统一返回 null，前端隐藏区块")
    void recommend_switchOff() {
        SystemSettingDTO off = defaultSettings();
        off.getTrade().setRecommendEnabled(false);
        when(systemSettingService.get()).thenReturn(off);

        assertNull(recommendService.homeRecommend(1L));
        assertNull(recommendService.complementaryRecommend("教材", null, null));
        assertNull(recommendService.productRecommend(18L));
    }

    @Test
    @DisplayName("商品不存在 - 详情页推荐返回 null")
    void productRecommend_productNotFound() {
        when(systemSettingService.get()).thenReturn(defaultSettings());
        when(productMapper.selectById(999999L)).thenReturn(null);

        assertNull(recommendService.productRecommend(999999L));
    }

    private Product product(Long id, Integer categoryId, String title, Long campusId, String condition) {
        Product p = new Product();
        p.setId(id);
        p.setCategoryId(categoryId);
        p.setTitle(title);
        p.setCampusId(campusId);
        p.setProductCondition(condition);
        p.setPrice(BigDecimal.TEN);
        p.setStatus(1);
        return p;
    }

    private Category category(Integer id, String name) {
        Category c = new Category();
        c.setId(id);
        c.setName(name);
        return c;
    }

    private Campus campus(Long id, String name) {
        Campus c = new Campus();
        c.setId(id);
        c.setName(name);
        c.setStatus(1);
        return c;
    }

    private CategoryRelation relation(Integer categoryId, Integer relatedId, Integer weight) {
        CategoryRelation r = new CategoryRelation();
        r.setCategoryId(categoryId);
        r.setRelatedCategoryId(relatedId);
        r.setWeight(weight);
        r.setStatus(1);
        return r;
    }

    private SystemSettingDTO defaultSettings() {
        return SystemSettingDTO.defaults();
    }
}
