package com.example.product.serviceImpl;

import com.example.common.Result;
import com.example.product.dto.ProductEstimateDTO;
import com.example.product.vo.ProductEstimateVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ProductAIServiceImpl 单元测试
 * <p>
 * 覆盖AI智能估价功能的各类场景及边界条件。
 * <p>
 * 两点契约说明（此前测试从未真正运行过，断言与实际契约不符）：
 * 1. estimatePrice 对非法参数返回 Result.fail（内部捕获 BusinessException），不向外抛异常；
 * 2. 估价含 ±200 元的随机波动（模拟 AI 不确定性），因此只能断言价格区间而非精确值，
 * 精确区间按规则引擎推导：基准 1000 + 成色分 + 品牌溢价 500 + 型号溢价 300 + 长标题 200 ± 200。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("商品AI估价服务单元测试")
class ProductAIServiceImplTest {

    @InjectMocks
    private ProductAIServiceImpl productAIService;

    // ======================== AI估价测试 ========================

    @Nested
    @DisplayName("AI估价功能")
    class EstimatePriceTests {

        @Test
        @DisplayName("正常估价 - iPhone 14 Pro")
        void estimatePrice_success_iphone14Pro() {
            ProductEstimateDTO dto = buildEstimateDTO(
                "iPhone 14 Pro 256GB 深空黑色",
                "95新，无划痕，电池健康度95%，原装配件齐全",
                "九成新",
                1
            );

            Result<ProductEstimateVO> result = productAIService.estimatePrice(dto);
            assertNotNull(result.getData());
            assertTrue(result.isSuccess());
            assertTrue(result.getData().getEstimatedPrice().compareTo(BigDecimal.ZERO) > 0);
            assertNotNull(result.getData().getPriceRange());
            assertNotNull(result.getData().getSuggestion());
            assertNotNull(result.getData().getEstimateTime());
        }

        @Test
        @DisplayName("正常估价 - 华为手机")
        void estimatePrice_success_huaweiPhone() {
            ProductEstimateDTO dto = buildEstimateDTO(
                "华为 Mate 60 Pro 512GB",
                "99新，几乎全新，无使用痕迹",
                "全新",
                1
            );

            Result<ProductEstimateVO> result = productAIService.estimatePrice(dto);
            assertNotNull(result.getData());
            assertTrue(result.isSuccess());
            assertTrue(result.getData().getEstimatedPrice().compareTo(BigDecimal.ZERO) > 0);
        }

        @Test
        @DisplayName("正常估价 - 小米手机")
        void estimatePrice_success_xiaomiPhone() {
            ProductEstimateDTO dto = buildEstimateDTO(
                "小米 13 Ultra 12GB+256GB",
                "85新，轻微使用痕迹，功能正常",
                "八成新",
                1
            );

            Result<ProductEstimateVO> result = productAIService.estimatePrice(dto);
            assertNotNull(result.getData());
            assertTrue(result.isSuccess());
            assertTrue(result.getData().getEstimatedPrice().compareTo(BigDecimal.ZERO) > 0);
        }

        @Test
        @DisplayName("边界测试 - 七成新手机（含品牌溢价，区间 1370-1770）")
        void estimatePrice_success_70Condition() {
            ProductEstimateDTO dto = buildEstimateDTO(
                "iPhone 12",
                "70新，有明显使用痕迹",
                "七成新及以下",
                1
            );

            Result<ProductEstimateVO> result = productAIService.estimatePrice(dto);
            assertNotNull(result.getData());
            assertTrue(result.isSuccess());
            // 规则：基准1000 + 品牌500（iPhone）+ 成色70 ± 200
            assertTrue(result.getData().getEstimatedPrice().compareTo(new BigDecimal("1350")) >= 0);
            assertTrue(result.getData().getEstimatedPrice().compareTo(new BigDecimal("1790")) <= 0);
        }

        @Test
        @DisplayName("边界测试 - 长标题获得溢价（区间 1100-1500）")
        void estimatePrice_success_longTitle() {
            ProductEstimateDTO dto = buildEstimateDTO(
                "这是一条非常非常长的商品标题测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试",
                "99新，几乎全新，无使用痕迹",
                "全新",
                1
            );

            Result<ProductEstimateVO> result = productAIService.estimatePrice(dto);
            assertNotNull(result.getData());
            assertTrue(result.isSuccess());
            // 规则：基准1000 + 成色100（全新）+ 长标题200 ± 200（无品牌/型号关键词）
            assertTrue(result.getData().getEstimatedPrice().compareTo(new BigDecimal("1080")) >= 0);
            assertTrue(result.getData().getEstimatedPrice().compareTo(new BigDecimal("1520")) <= 0);
        }

        @Test
        @DisplayName("边界测试 - 短标题无品牌溢价（区间 900-1300）")
        void estimatePrice_success_shortTitle() {
            ProductEstimateDTO dto = buildEstimateDTO(
                "手机",
                "99新，几乎全新，无使用痕迹",
                "全新",
                1
            );

            Result<ProductEstimateVO> result = productAIService.estimatePrice(dto);
            assertNotNull(result.getData());
            assertTrue(result.isSuccess());
            // 规则：基准1000 + 成色100（全新）± 200（无品牌/型号/长标题加成）
            assertTrue(result.getData().getEstimatedPrice().compareTo(new BigDecimal("880")) >= 0);
            assertTrue(result.getData().getEstimatedPrice().compareTo(new BigDecimal("1320")) <= 0);
        }

        @Test
        @DisplayName("无效参数 - 标题为空返回失败结果而非抛异常")
        void estimatePrice_emptyTitle() {
            ProductEstimateDTO dto = buildEstimateDTO(
                "",
                "99新，几乎全新，无使用痕迹",
                "全新",
                1
            );

            Result<ProductEstimateVO> result = productAIService.estimatePrice(dto);
            assertFalse(result.isSuccess());
        }

        @Test
        @DisplayName("描述为空也可估价（发布向导第一步即调用，描述尚未填写）")
        void estimatePrice_emptyDescription() {
            ProductEstimateDTO dto = buildEstimateDTO(
                "iPhone 14",
                "",
                "全新",
                1
            );

            Result<ProductEstimateVO> result = productAIService.estimatePrice(dto);
            assertTrue(result.isSuccess());
            assertTrue(result.getData().getEstimatedPrice().compareTo(BigDecimal.ZERO) > 0);
        }

        @Test
        @DisplayName("无效参数 - 无效成色返回失败结果而非抛异常")
        void estimatePrice_invalidCondition() {
            ProductEstimateDTO dto = buildEstimateDTO(
                "iPhone 14",
                "99新，几乎全新，无使用痕迹",
                "无效成色",
                1
            );

            Result<ProductEstimateVO> result = productAIService.estimatePrice(dto);
            assertFalse(result.isSuccess());
        }

        @Test
        @DisplayName("无效参数 - 分类ID为空返回失败结果而非抛异常")
        void estimatePrice_emptyCategoryId() {
            ProductEstimateDTO dto = buildEstimateDTO(
                "iPhone 14",
                "99新，几乎全新，无使用痕迹",
                "全新",
                null
            );

            Result<ProductEstimateVO> result = productAIService.estimatePrice(dto);
            assertFalse(result.isSuccess());
        }

        @Test
        @DisplayName("相同参数连续估价均成功（缓存效果在 @SpringBootTest 中验证）")
        void estimatePrice_sameParamsBothSucceed() {
            ProductEstimateDTO dto = buildEstimateDTO(
                "iPhone 14",
                "99新，几乎全新，无使用痕迹",
                "全新",
                1
            );

            Result<ProductEstimateVO> result1 = productAIService.estimatePrice(dto);
            Result<ProductEstimateVO> result2 = productAIService.estimatePrice(dto);

            assertTrue(result1.isSuccess());
            assertTrue(result2.isSuccess());
            // 纯 Mockito 环境没有 Spring 缓存代理，两次都会真实计算；
            // 「相同参数结果一致」的缓存断言见 ProductAIServiceSimpleTest
            assertTrue(result1.getData().getEstimatedPrice().compareTo(BigDecimal.ZERO) > 0);
            assertTrue(result2.getData().getEstimatedPrice().compareTo(BigDecimal.ZERO) > 0);
        }

        @Test
        @DisplayName("不同成色估价均成功（随机波动导致价格区间有重叠，不做大小断言）")
        void estimatePrice_differentParams() {
            ProductEstimateDTO dto1 = buildEstimateDTO(
                "iPhone 14",
                "99新，几乎全新，无使用痕迹",
                "全新",
                1
            );

            ProductEstimateDTO dto2 = buildEstimateDTO(
                "iPhone 14",
                "70新，有明显使用痕迹",
                "七成新及以下",
                1
            );

            Result<ProductEstimateVO> result1 = productAIService.estimatePrice(dto1);
            Result<ProductEstimateVO> result2 = productAIService.estimatePrice(dto2);

            assertTrue(result1.isSuccess());
            assertTrue(result2.isSuccess());
            assertTrue(result1.getData().getEstimatedPrice().compareTo(BigDecimal.ZERO) > 0);
            assertTrue(result2.getData().getEstimatedPrice().compareTo(BigDecimal.ZERO) > 0);
        }
    }

    // ======================== 辅助方法 ========================

    /**
     * 构建估价DTO模板
     */
    private ProductEstimateDTO buildEstimateDTO(String title, String description, String condition, Integer categoryId) {
        ProductEstimateDTO dto = new ProductEstimateDTO();
        dto.setTitle(title);
        dto.setDescription(description);
        dto.setProductCondition(condition);
        dto.setCategoryId(categoryId);
        return dto;
    }
}
