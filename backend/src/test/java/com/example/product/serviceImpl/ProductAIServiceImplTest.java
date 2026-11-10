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
 * 契约说明（2026-11 起估价去随机化，改为确定性契约）：
 * 1. estimatePrice 对非法参数返回 Result.fail（内部捕获 BusinessException），不向外抛异常；
 * 2. 估价是确定性的：同输入必同输出。纯 Mockito 环境下行情依赖为 null 自动降级为规则基线，
 *    精确值 = 基准 1000 + 品牌溢价 500（标题含 iPhone/华为/小米）+ 型号溢价 300（含 Pro/Max/Plus）
 *    + 长标题 200（标题 > 20 字）+ 成色分（全新100/九成新90/八成新80/七成新及以下70）；
 * 3. basisType 固定为 RULE_BASELINE（行情路径 MARKET_DATA 需要 DB 行情数据，见冒烟测试）。
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
        @DisplayName("边界测试 - 七成新手机（含品牌溢价，精确值 1570）")
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
            // 规则：基准1000 + 品牌500（iPhone）+ 成色70，确定性输出
            assertEquals(0, result.getData().getEstimatedPrice().compareTo(new BigDecimal("1570")));
            assertEquals("RULE_BASELINE", result.getData().getBasisType());
        }

        @Test
        @DisplayName("边界测试 - 长标题获得溢价（精确值 1300）")
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
            // 规则：基准1000 + 成色100（全新）+ 长标题200（无品牌/型号关键词），确定性输出
            assertEquals(0, result.getData().getEstimatedPrice().compareTo(new BigDecimal("1300")));
        }

        @Test
        @DisplayName("边界测试 - 短标题无品牌溢价（精确值 1100）")
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
            // 规则：基准1000 + 成色100（全新）（无品牌/型号/长标题加成），确定性输出
            assertEquals(0, result.getData().getEstimatedPrice().compareTo(new BigDecimal("1100")));
            // 置信度同步去随机化：规则基线固定 80
            assertEquals(80, result.getData().getConfidence());
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
        @DisplayName("相同参数连续估价结果完全一致（确定性契约，缓存断言在 @SpringBootTest 中验证）")
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
            // 估价已去随机化，因此两次结果必须逐字段一致
            assertEquals(0, result1.getData().getEstimatedPrice().compareTo(result2.getData().getEstimatedPrice()));
            assertEquals(result1.getData().getPriceRange(), result2.getData().getPriceRange());
            // 「相同参数结果一致」的缓存断言见 ProductAIServiceSimpleTest
        }

        @Test
        @DisplayName("不同成色估价均成功（确定性定价：成色越好估价越高）")
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
            // 全新(100) 必须高于 七成新及以下(70)：基准与品牌相同，差值即成色分差
            // 两条均含品牌关键词（iPhone）：1600 = 1000+500+100；1570 = 1000+500+70
            assertTrue(result1.getData().getEstimatedPrice()
                    .compareTo(result2.getData().getEstimatedPrice()) > 0);
            assertEquals(0, result1.getData().getEstimatedPrice().compareTo(new BigDecimal("1600")));
            assertEquals(0, result2.getData().getEstimatedPrice().compareTo(new BigDecimal("1570")));
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
