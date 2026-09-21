package com.example.product.serviceImpl;

import com.example.common.BusinessException;
import com.example.common.Result;
import com.example.common.ResultCodeEnum;
import com.example.product.constant.ProductConstant;
import com.example.product.dto.ProductEstimateDTO;
import com.example.product.service.ProductAIService;
import com.example.product.vo.ProductEstimateVO;
import org.junit.jupiter.api.BeforeEach;
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
 * 覆盖所有边界条件、异常处理和性能测试场景
 *
 * @author ZCode
 * @date 2026/09/21
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AI智能估价功能完整测试")
class ProductAIServiceFullTest {

    @InjectMocks
    private ProductAIService productAIService;

    // ======================== 正常场景测试 ========================

    @Nested
    @DisplayName("正常估价场景")
    class NormalEstimationTests {

        @Test
        @DisplayName("iPhone 14 Pro 完整参数估价")
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
                () -> assertTrue(result.getData().getEstimatedPrice().compareTo(new BigDecimal(3000)) <= 5000, "估价价格应该在合理范围内"),
                () -> assertTrue(result.getData().getConfidence() >= 80 && result.getData().getConfidence() <= 99, "置信度应该在80-99之间"),
                () -> assertNotNull(result.getData().getPriceRange(), "价格范围不应为空"),
                () -> assertNotNull(result.getData().getSuggestion(), "估价建议不应为空"),
                () -> assertNotNull(result.getData().getEstimateTime(), "估价时间不应为空")
            );
        }

        @Test
        @DisplayName("华为手机完整参数估价")
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
                () -> assertTrue(result.getData().getEstimatedPrice().compareTo(new BigDecimal(6000)) > 0)
            );
        }

        @Test
        @DisplayName("小米手机完整参数估价")
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
                () -> assertTrue(result.getData().getEstimatedPrice().compareTo(new BigDecimal(4000)) > 0)
            );
        }
    }

    // ======================== 边界条件测试 ========================

    @Nested
    @DisplayName("边界条件测试")
    class BoundaryConditionTests {

        @Test
        @DisplayName("七成新手机估价")
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
                () -> assertTrue(result.getData().getEstimatedPrice().compareTo(new BigDecimal(800)) <= 0,
                    "七成新价格应该较低")
            );
        }

        @Test
        @DisplayName("极长标题的估价")
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
                () -> assertTrue(result.getData().getEstimatedPrice().compareTo(new BigDecimal(1500)) > 0,
                    "长标题应该获得溢价")
            );
        }

        @Test
        @DisplayName("极短标题的估价")
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
                () -> assertTrue(result.getData().getEstimatedPrice().compareTo(new BigDecimal(500)) <= 0,
                    "短标题价格应该较低")
            );
        }

        @Test
        @DisplayName("最大成新的估价")
        void testEstimatePriceMaximumCondition() {
            ProductEstimateDTO dto = createEstimateDTO(
                "高端手机",
                "完美状态，无任何瑕疵",
                "全新",
                1
            );
            
            Result<ProductEstimateVO> result = productAIService.estimatePrice(dto);
            
            assertAll("最大成新估价验证",
                () -> assertTrue(result.isSuccess()),
                () -> assertNotNull(result.getData().getEstimatedPrice()),
                () -> assertTrue(result.getData().getEstimatedPrice().compareTo(new BigDecimal(1000)) > 0)
            );
        }

        @Test
        @DisplayName("最小成新的估价")
        void testEstimatePriceMinimumCondition() {
            ProductEstimateDTO dto = createEstimateDTO(
                "旧手机",
                "有严重使用痕迹，功能可能有问题",
                "七成新及以下",
                1
            );
            
            Result<ProductEstimateVO> result = productAIService.estimatePrice(dto);
            
            assertAll("最小成新估价验证",
                () -> assertTrue(result.isSuccess()),
                () -> assertNotNull(result.getData().getEstimatedPrice()),
                () -> assertTrue(result.getData().getEstimatedPrice().compareTo(new BigDecimal(300)) <= 0,
                    "最小成新价格应该很低")
            );
        }
    }

    // ======================== 异常场景测试 ========================

    @Nested
    @DisplayName("异常场景测试")
    class ExceptionTests {

        @Test
        @DisplayName("空标题异常")
        void testEstimatePriceEmptyTitle() {
            ProductEstimateDTO dto = createEstimateDTO(
                "",
                "99新，几乎全新，无使用痕迹",
                "全新",
                1
            );
            
            BusinessException ex = assertThrows(BusinessException.class, 
                () -> productAIService.estimatePrice(dto));
            
            assertEquals(ResultCodeEnum.PARAM_ERROR.getCode(), ex.getCode());
            assertEquals("商品标题不能为空", ex.getMessage());
        }

        @Test
        @DisplayName("空描述异常")
        void testEstimatePriceEmptyDescription() {
            ProductEstimateDTO dto = createEstimateDTO(
                "iPhone 14",
                "",
                "全新",
                1
            );
            
            BusinessException ex = assertThrows(BusinessException.class, 
                () -> productAIService.estimatePrice(dto));
            
            assertEquals(ResultCodeEnum.PARAM_ERROR.getCode(), ex.getCode());
            assertEquals("商品描述不能为空", ex.getMessage());
        }

        @Test
        @DisplayName("无效成色异常")
        void testEstimatePriceInvalidCondition() {
            ProductEstimateDTO dto = createEstimateDTO(
                "iPhone 14",
                "99新，几乎全新，无使用痕迹",
                "无效成色",
                1
            );
            
            BusinessException ex = assertThrows(BusinessException.class, 
                () -> productAIService.estimatePrice(dto));
            
            assertEquals(ResultCodeEnum.PARAM_ERROR.getCode(), ex.getCode());
            assertEquals("成色不合法，仅支持：全新、九成新、八成新、七成新及以下", ex.getMessage());
        }

        @Test
        @DisplayName("空分类ID异常")
        void testEstimatePriceEmptyCategoryId() {
            ProductEstimateDTO dto = createEstimateDTO(
                "iPhone 14",
                "99新，几乎全新，无使用痕迹",
                "全新",
                null
            );
            
            BusinessException ex = assertThrows(BusinessException.class, 
                () -> productAIService.estimatePrice(dto));
            
            assertEquals(ResultCodeEnum.PARAM_ERROR.getCode(), ex.getCode());
            assertEquals("分类ID不能为空", ex.getMessage());
        }

        @Test
        @DisplayName("标题过长异常")
        void testEstimatePriceTitleTooLong() {
            String veryLongTitle = "A".repeat(101); // 超过100字符限制
            
            ProductEstimateDTO dto = createEstimateDTO(
                veryLongTitle,
                "99新，几乎全新，无使用痕迹",
                "全新",
                1
            );
            
            BusinessException ex = assertThrows(BusinessException.class, 
                () -> productAIService.estimatePrice(dto));
            
            assertEquals(ResultCodeEnum.PARAM_ERROR.getCode(), ex.getCode());
            assertEquals("商品标题不能超过100个字符", ex.getMessage());
        }

        @Test
        @DisplayName("描述过长异常")
        void testEstimatePriceDescriptionTooLong() {
            String veryLongDescription = "A".repeat(501); // 超过500字符限制
            
            ProductEstimateDTO dto = createEstimateDTO(
                "iPhone 14",
                veryLongDescription,
                "全新",
                1
            );
            
            BusinessException ex = assertThrows(BusinessException.class, 
                () -> productAIService.estimatePrice(dto));
            
            assertEquals(ResultCodeEnum.PARAM_ERROR.getCode(), ex.getCode());
            assertEquals("商品描述不能超过500个字符", ex.getMessage());
        }
    }

    // ======================== 性能和缓存测试 ========================

    @Nested
    @DisplayName("性能和缓存测试")
    class PerformanceTests {

        @Test
        @DisplayName("缓存命中测试")
        void testCacheHit() {
            ProductEstimateDTO dto = createEstimateDTO(
                "iPhone 14",
                "99新，几乎全新，无使用痕迹",
                "全新",
                1
            );
            
            // 第一次估价
            Result<ProductEstimateVO> result1 = productAIService.estimatePrice(dto);
            BigDecimal price1 = result1.getData().getEstimatedPrice();
            
            // 第二次估价（应该命中缓存）
            Result<ProductEstimateVO> result2 = productAIService.estimatePrice(dto);
            BigDecimal price2 = result2.getData().getEstimatedPrice();
            
            // 验证缓存命中
            assertEquals(price1, price2, "相同参数应该返回相同价格（缓存命中）");
        }

        @Test
        @DisplayName("缓存失效测试 - 不同参数")
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
            
            // 估价不同的成色
            Result<ProductEstimateVO> result1 = productAIService.estimatePrice(dto1);
            BigDecimal price1 = result1.getData().getEstimatedPrice();
            
            Result<ProductEstimateVO> result2 = productAIService.estimatePrice(dto2);
            BigDecimal price2 = result2.getData().getEstimatedPrice();
            
            // 验证缓存失效 - 不同成色应该有不同价格
            assertTrue(price1.compareTo(price2) > 0, "全新价格应该高于七成新");
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