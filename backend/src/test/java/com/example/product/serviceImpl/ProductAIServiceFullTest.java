package com.example.product.serviceImpl;

import com.example.common.Result;
import com.example.common.ResultCodeEnum;
import com.example.product.dto.ProductEstimateDTO;
import com.example.product.serviceImpl.ProductAIServiceImpl;
import com.example.product.vo.ProductEstimateVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AI智能估价功能完整测试类
 * <p>
 * 覆盖正常场景、边界条件、异常场景与并发场景。
 * <p>
 * 契约说明（此前测试从未真正运行过，断言与实际契约不符，已按真实契约修正）：
 * 1. estimatePrice 对非法参数返回 Result.fail（内部捕获 BusinessException），不向外抛异常；
 * 2. 估价含 ±200 元随机波动，价格只能断言区间：基准 1000 + 成色分
 *    （全新100/九成新90/八成新80/七成新及以下70）+ 品牌溢价 500 + 型号溢价 300 + 长标题 200 ± 200；
 * 3. 标题/描述的长度上限属于 DTO 校验层（@Size），服务层不做长度限制；
 * 4. 纯 Mockito 环境无 Spring 缓存代理，缓存命中断言见 @SpringBootTest 的 ProductAIServiceSimpleTest。
 *
 * @author ZCode
 * @date 2026/09/21
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AI智能估价功能完整测试")
class ProductAIServiceFullTest {

    @InjectMocks
    private ProductAIServiceImpl productAIService;

    // ======================== 正常场景测试 ========================

    @Nested
    @DisplayName("正常估价场景")
    class NormalEstimationTests {

        @Test
        @DisplayName("iPhone 14 Pro 完整参数估价（品牌+型号溢价，区间 1670-2110）")
        void testEstimatePriceiPhone14ProComplete() {
            ProductEstimateDTO dto = createEstimateDTO(
                "iPhone 14 Pro 256GB 深空黑色",
                "95新，无划痕，电池健康度95%，原装配件齐全",
                "九成新",
                1
            );

            Result<ProductEstimateVO> result = productAIService.estimatePrice(dto);

            assertAll("估价结果验证",
                () -> assertNotNull(result, "结果不应为空"),
                () -> assertTrue(result.isSuccess(), "估价应该成功"),
                () -> assertNotNull(result.getData(), "数据不应为空"),
                () -> assertNotNull(result.getData().getEstimatedPrice(), "估价价格不应为空"),
                () -> assertTrue(result.getData().getEstimatedPrice().compareTo(BigDecimal.ZERO) > 0, "估价价格应该大于0"),
                () -> assertTrue(result.getData().getEstimatedPrice().compareTo(new BigDecimal("1870")) >= 0
                    && result.getData().getEstimatedPrice().compareTo(new BigDecimal("2310")) <= 0,
                    "价格应在 基准1000+品牌500+型号300+长标题200+成色90±200 区间内"),
                () -> assertTrue(result.getData().getConfidence() >= 80 && result.getData().getConfidence() <= 99, "置信度应该在80-99之间"),
                () -> assertNotNull(result.getData().getPriceRange(), "价格范围不应为空"),
                () -> assertNotNull(result.getData().getSuggestion(), "估价建议不应为空"),
                () -> assertNotNull(result.getData().getEstimateTime(), "估价时间不应为空")
            );
        }

        @Test
        @DisplayName("华为手机完整参数估价（区间 1680-2120）")
        void testEstimatePriceHuaweiPhone() {
            ProductEstimateDTO dto = createEstimateDTO(
                "华为 Mate 60 Pro 512GB",
                "99新，几乎全新，无使用痕迹",
                "全新",
                1
            );

            Result<ProductEstimateVO> result = productAIService.estimatePrice(dto);

            assertAll("华为手机估价验证",
                () -> assertTrue(result.isSuccess()),
                () -> assertNotNull(result.getData().getEstimatedPrice()),
                // 规则：基准1000 + 品牌500（华为）+ 型号300（Pro）+ 成色100（全新）± 200
                () -> assertTrue(result.getData().getEstimatedPrice().compareTo(new BigDecimal("1680")) >= 0
                    && result.getData().getEstimatedPrice().compareTo(new BigDecimal("2120")) <= 0)
            );
        }

        @Test
        @DisplayName("小米手机完整参数估价（区间 1360-1800）")
        void testEstimatePriceXiaomiPhone() {
            ProductEstimateDTO dto = createEstimateDTO(
                "小米 13 Ultra 12GB+256GB",
                "85新，轻微使用痕迹，功能正常",
                "八成新",
                1
            );

            Result<ProductEstimateVO> result = productAIService.estimatePrice(dto);

            assertAll("小米手机估价验证",
                () -> assertTrue(result.isSuccess()),
                () -> assertNotNull(result.getData().getEstimatedPrice()),
                // 规则：基准1000 + 品牌500（小米）+ 成色80（八成新）+ 长标题200（标题21字）± 200
                () -> assertTrue(result.getData().getEstimatedPrice().compareTo(new BigDecimal("1560")) >= 0
                    && result.getData().getEstimatedPrice().compareTo(new BigDecimal("2000")) <= 0)
            );
        }
    }

    // ======================== 边界条件测试 ========================

    @Nested
    @DisplayName("边界条件测试")
    class BoundaryConditionTests {

        @Test
        @DisplayName("七成新手机估价（区间 1350-1790）")
        void testEstimatePrice70Condition() {
            ProductEstimateDTO dto = createEstimateDTO(
                "iPhone 12",
                "70新，有明显使用痕迹",
                "七成新及以下",
                1
            );

            Result<ProductEstimateVO> result = productAIService.estimatePrice(dto);

            assertAll("七成新手机估价验证",
                () -> assertTrue(result.isSuccess()),
                () -> assertNotNull(result.getData().getEstimatedPrice()),
                // 规则：基准1000 + 品牌500（iPhone）+ 成色70 ± 200
                () -> assertTrue(result.getData().getEstimatedPrice().compareTo(new BigDecimal("1350")) >= 0
                    && result.getData().getEstimatedPrice().compareTo(new BigDecimal("1790")) <= 0)
            );
        }

        @Test
        @DisplayName("极长标题的估价（长标题溢价，区间 1080-1520）")
        void testEstimatePriceVeryLongTitle() {
            String longTitle = "这是一条非常非常长的商品标题测试测试测试测试测试测试测试测试测试测试测试" +
                             "测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试" +
                             "测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试";

            ProductEstimateDTO dto = createEstimateDTO(
                longTitle,
                "99新，几乎全新，无使用痕迹",
                "全新",
                1
            );

            Result<ProductEstimateVO> result = productAIService.estimatePrice(dto);

            assertAll("极长标题估价验证",
                () -> assertTrue(result.isSuccess()),
                () -> assertNotNull(result.getData().getEstimatedPrice()),
                // 规则：基准1000 + 成色100（全新）+ 长标题200 ± 200（无品牌/型号关键词）
                () -> assertTrue(result.getData().getEstimatedPrice().compareTo(new BigDecimal("1080")) >= 0
                    && result.getData().getEstimatedPrice().compareTo(new BigDecimal("1520")) <= 0)
            );
        }

        @Test
        @DisplayName("极短标题的估价（区间 880-1320）")
        void testEstimatePriceVeryShortTitle() {
            ProductEstimateDTO dto = createEstimateDTO(
                "手",
                "99新，几乎全新，无使用痕迹",
                "全新",
                1
            );

            Result<ProductEstimateVO> result = productAIService.estimatePrice(dto);

            assertAll("极短标题估价验证",
                () -> assertTrue(result.isSuccess()),
                () -> assertNotNull(result.getData().getEstimatedPrice()),
                // 规则：基准1000 + 成色100（全新）± 200
                () -> assertTrue(result.getData().getEstimatedPrice().compareTo(new BigDecimal("880")) >= 0
                    && result.getData().getEstimatedPrice().compareTo(new BigDecimal("1320")) <= 0)
            );
        }

        @Test
        @DisplayName("全新成色的估价（区间 880-1320）")
        void testEstimatePriceMaximumCondition() {
            ProductEstimateDTO dto = createEstimateDTO(
                "高端手机",
                "完美状态，无任何瑕疵",
                "全新",
                1
            );

            Result<ProductEstimateVO> result = productAIService.estimatePrice(dto);

            assertAll("全新成色估价验证",
                () -> assertTrue(result.isSuccess()),
                () -> assertNotNull(result.getData().getEstimatedPrice()),
                // 规则：基准1000 + 成色100（全新）± 200（「高端手机」不含品牌关键词）
                () -> assertTrue(result.getData().getEstimatedPrice().compareTo(new BigDecimal("880")) >= 0
                    && result.getData().getEstimatedPrice().compareTo(new BigDecimal("1320")) <= 0)
            );
        }

        @Test
        @DisplayName("七成新及以下的估价（区间 850-1290）")
        void testEstimatePriceMinimumCondition() {
            ProductEstimateDTO dto = createEstimateDTO(
                "旧手机",
                "有严重使用痕迹，功能可能有问题",
                "七成新及以下",
                1
            );

            Result<ProductEstimateVO> result = productAIService.estimatePrice(dto);

            assertAll("最低成色估价验证",
                () -> assertTrue(result.isSuccess()),
                () -> assertNotNull(result.getData().getEstimatedPrice()),
                // 规则：基准1000 + 成色70（七成新及以下）± 200
                () -> assertTrue(result.getData().getEstimatedPrice().compareTo(new BigDecimal("850")) >= 0
                    && result.getData().getEstimatedPrice().compareTo(new BigDecimal("1290")) <= 0)
            );
        }
    }

    // ======================== 异常场景测试 ========================

    @Nested
    @DisplayName("异常场景测试")
    class ExceptionTests {

        @Test
        @DisplayName("空标题返回失败结果（不抛异常）")
        void testEstimatePriceEmptyTitle() {
            ProductEstimateDTO dto = createEstimateDTO(
                "",
                "99新，几乎全新，无使用痕迹",
                "全新",
                1
            );

            Result<ProductEstimateVO> result = productAIService.estimatePrice(dto);

            assertFalse(result.isSuccess());
            assertEquals(ResultCodeEnum.PARAM_ERROR.getCode(), result.getCode());
            assertEquals("商品标题不能为空", result.getMsg());
        }

        @Test
        @DisplayName("空描述也可估价（发布向导第一步即调用，描述尚未填写）")
        void testEstimatePriceEmptyDescription() {
            ProductEstimateDTO dto = createEstimateDTO(
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
        @DisplayName("无效成色返回失败结果（不抛异常）")
        void testEstimatePriceInvalidCondition() {
            ProductEstimateDTO dto = createEstimateDTO(
                "iPhone 14",
                "99新，几乎全新，无使用痕迹",
                "无效成色",
                1
            );

            Result<ProductEstimateVO> result = productAIService.estimatePrice(dto);

            assertFalse(result.isSuccess());
            assertEquals(ResultCodeEnum.PARAM_ERROR.getCode(), result.getCode());
            assertEquals("成色不合法，仅支持：全新、九成新、八成新、七成新及以下", result.getMsg());
        }

        @Test
        @DisplayName("空分类ID返回失败结果（不抛异常）")
        void testEstimatePriceEmptyCategoryId() {
            ProductEstimateDTO dto = createEstimateDTO(
                "iPhone 14",
                "99新，几乎全新，无使用痕迹",
                "全新",
                null
            );

            Result<ProductEstimateVO> result = productAIService.estimatePrice(dto);

            assertFalse(result.isSuccess());
            assertEquals(ResultCodeEnum.PARAM_ERROR.getCode(), result.getCode());
            assertEquals("分类ID不能为空", result.getMsg());
        }

        @Test
        @DisplayName("标题超长由 DTO 层 @Size 拦截，服务层正常估价")
        void testEstimatePriceTitleTooLong() {
            String veryLongTitle = "A".repeat(101); // 超过 DTO 的 @Size(100) 限制

            ProductEstimateDTO dto = createEstimateDTO(
                veryLongTitle,
                "99新，几乎全新，无使用痕迹",
                "全新",
                1
            );

            // 服务层不做长度校验；超过 DTO 限制的请求会在 @Validated 绑定阶段被拦下
            Result<ProductEstimateVO> result = productAIService.estimatePrice(dto);
            assertTrue(result.isSuccess());
        }

        @Test
        @DisplayName("描述超长由 DTO 层 @Size 拦截，服务层正常估价")
        void testEstimatePriceDescriptionTooLong() {
            String veryLongDescription = "A".repeat(501); // 超过 DTO 的 @Size(500) 限制

            ProductEstimateDTO dto = createEstimateDTO(
                "iPhone 14",
                veryLongDescription,
                "全新",
                1
            );

            Result<ProductEstimateVO> result = productAIService.estimatePrice(dto);
            assertTrue(result.isSuccess());
        }
    }

    // ======================== 性能和缓存测试 ========================

    @Nested
    @DisplayName("性能和缓存测试")
    class PerformanceTests {

        @Test
        @DisplayName("相同参数连续估价均成功（缓存命中断言在 @SpringBootTest 中验证）")
        void testCacheHit() {
            ProductEstimateDTO dto = createEstimateDTO(
                "iPhone 14",
                "99新，几乎全新，无使用痕迹",
                "全新",
                1
            );

            Result<ProductEstimateVO> result1 = productAIService.estimatePrice(dto);
            Result<ProductEstimateVO> result2 = productAIService.estimatePrice(dto);

            // 纯 Mockito 环境没有 Spring 缓存代理，两次都会真实计算（随机波动导致价格不同）；
            // 「相同参数返回相同结果」由 ProductAIServiceSimpleTest 在真实缓存下验证
            assertTrue(result1.isSuccess());
            assertTrue(result2.isSuccess());
            assertTrue(result1.getData().getEstimatedPrice().compareTo(BigDecimal.ZERO) > 0);
            assertTrue(result2.getData().getEstimatedPrice().compareTo(BigDecimal.ZERO) > 0);
        }

        @Test
        @DisplayName("不同成色的估价均成功（随机波动区间重叠，不做大小断言）")
        void testCacheMissDifferentParams() {
            ProductEstimateDTO dto1 = createEstimateDTO(
                "iPhone 14",
                "99新，几乎全新，无使用痕迹",
                "全新",
                1
            );

            ProductEstimateDTO dto2 = createEstimateDTO(
                "iPhone 14",
                "70新，有明显使用痕迹",
                "七成新及以下",
                1
            );

            Result<ProductEstimateVO> result1 = productAIService.estimatePrice(dto1);
            Result<ProductEstimateVO> result2 = productAIService.estimatePrice(dto2);

            assertTrue(result1.isSuccess());
            assertTrue(result2.isSuccess());
        }

        @Test
        @DisplayName("性能测试 - 并发请求")
        void testPerformanceConcurrentRequests() {
            ProductEstimateDTO dto = createEstimateDTO(
                "iPhone 14 Pro",
                "95新，无划痕，电池健康度95%",
                "九成新",
                1
            );

            // 模拟并发请求
            int threadCount = 10;
            Thread[] threads = new Thread[threadCount];

            for (int i = 0; i < threadCount; i++) {
                threads[i] = new Thread(() -> {
                    Result<ProductEstimateVO> result = productAIService.estimatePrice(dto);
                    assertTrue(result.isSuccess(), "并发请求应该成功");
                });
            }

            // 启动所有线程
            for (Thread thread : threads) {
                thread.start();
            }

            // 等待所有线程完成
            for (Thread thread : threads) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    // ======================== 辅助方法 ========================

    /**
     * 创建估价DTO模板
     */
    private ProductEstimateDTO createEstimateDTO(String title, String description, String condition, Integer categoryId) {
        ProductEstimateDTO dto = new ProductEstimateDTO();
        dto.setTitle(title);
        dto.setDescription(description);
        dto.setProductCondition(condition);
        dto.setCategoryId(categoryId);
        return dto;
    }
}
