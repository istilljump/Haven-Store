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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ProductAIServiceImpl 单元测试
 * <p>
 * 覆盖AI智能估价功能的各类场景及边界条件
 *
 * @author ZCode
 * @date 2026/09/21
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("商品AI估价服务单元测试")
class ProductAIServiceImplTest {

    @InjectMocks
    private ProductAIService productAIService;

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
            assertTrue(result.getData().getEstimatedPrice().compareTo(BigDecimal.ZERO) > 0);
            assertTrue(result.getData().getConfidence() >= 80 && result.getData().getConfidence() <= 99);
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
            assertTrue(result.getData().getEstimatedPrice().compareTo(BigDecimal.ZERO) > 0);
        }

        @Test
        @DisplayName("边界测试 - 七成新手机")
        void estimatePrice_success_70Condition() {
            ProductEstimateDTO dto = buildEstimateDTO(
                "iPhone 12",
                "70新，有明显使用痕迹",
                "七成新及以下",
                1
            );
            
            Result<ProductEstimateVO> result = productAIService.estimatePrice(dto);
            assertNotNull(result.getData());
            // 七成新价格应该相对较低
            assertTrue(result.getData().getEstimatedPrice().compareTo(new BigDecimal(800)) <= 0);
        }

        @Test
        @DisplayName("边界测试 - 标题过长")
        void estimatePrice_success_longTitle() {
            ProductEstimateDTO dto = buildEstimateDTO(
                "这是一条非常非常长的商品标题测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试",
                "99新，几乎全新，无使用痕迹",
                "全新",
                1
            );
            
            Result<ProductEstimateVO> result = productAIService.estimatePrice(dto);
            assertNotNull(result.getData());
            // 长标题应该获得溢价
            assertTrue(result.getData().getEstimatedPrice().compareTo(new BigDecimal(1500)) > 0);
        }

        @Test
        @DisplayName("边界测试 - 标题过短")
        void estimatePrice_success_shortTitle() {
            ProductEstimateDTO dto = buildEstimateDTO(
                "手机",
                "99新，几乎全新，无使用痕迹",
                "全新",
                1
            );
            
            Result<ProductEstimateVO> result = productAIService.estimatePrice(dto);
            assertNotNull(result.getData());
            // 短标题价格应该较低
            assertTrue(result.getData().getEstimatedPrice().compareTo(new BigDecimal(800)) <= 0);
        }

        @Test
        @DisplayName("无效参数 - 标题为空")
        void estimatePrice_emptyTitle() {
            ProductEstimateDTO dto = buildEstimateDTO(
                "",
                "99新，几乎全新，无使用痕迹",
                "全新",
                1
            );
            
            BusinessException ex = assertThrows(BusinessException.class, () -> productAIService.estimatePrice(dto));
            assertEquals(ResultCodeEnum.PARAM_ERROR.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("无效参数 - 描述为空")
        void estimatePrice_emptyDescription() {
            ProductEstimateDTO dto = buildEstimateDTO(
                "iPhone 14",
                "",
                "全新",
                1
            );
            
            BusinessException ex = assertThrows(BusinessException.class, () -> productAIService.estimatePrice(dto));
            assertEquals(ResultCodeEnum.PARAM_ERROR.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("无效参数 - 无效成色")
        void estimatePrice_invalidCondition() {
            ProductEstimateDTO dto = buildEstimateDTO(
                "iPhone 14",
                "99新，几乎全新，无使用痕迹",
                "无效成色",
                1
            );
            
            BusinessException ex = assertThrows(BusinessException.class, () -> productAIService.estimatePrice(dto));
            assertEquals(ResultCodeEnum.PARAM_ERROR.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("无效参数 - 分类ID为空")
        void estimatePrice_emptyCategoryId() {
            ProductEstimateDTO dto = buildEstimateDTO(
                "iPhone 14",
                "99新，几乎全新，无使用痕迹",
                "全新",
                null
            );
            
            BusinessException ex = assertThrows(BusinessException.class, () -> productAIService.estimatePrice(dto));
            assertEquals(ResultCodeEnum.PARAM_ERROR.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("缓存功能测试 - 相同参数应返回相同结果")
        void estimatePrice_cacheTest() {
            ProductEstimateDTO dto = buildEstimateDTO(
                "iPhone 14",
                "99新，几乎全新，无使用痕迹",
                "全新",
                1
            );
            
            Result<ProductEstimateVO> result1 = productAIService.estimatePrice(dto);
            Result<ProductEstimateVO> result2 = productAIService.estimatePrice(dto);
            
            assertNotNull(result1.getData());
            assertNotNull(result2.getData());
            // 相同参数估价应该接近（因为缓存机制）
            assertEquals(result1.getData().getEstimatedPrice(), result2.getData().getEstimatedPrice());
        }

        @Test
        @DisplayName("缓存功能测试 - 不同参数应返回不同结果")
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
            
            assertNotNull(result1.getData());
            assertNotNull(result2.getData());
            // 不同成色的价格应该不同
            assertTrue(result1.getData().getEstimatedPrice().compareTo(result2.getData().getEstimatedPrice()) > 0);
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