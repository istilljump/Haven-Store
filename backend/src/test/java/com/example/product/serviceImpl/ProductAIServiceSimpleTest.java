package com.example.product.serviceImpl;

import com.example.common.Result;
import com.example.product.dto.ProductEstimateDTO;
import com.example.product.vo.ProductEstimateVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 简化的AI估价服务集成测试
 * <p>
 * 用于验证AI智能估价功能的基本功能
 *
 * @author ZCode
 * @date 2026/09/21
 */
@SpringBootTest
@Transactional
class ProductAIServiceSimpleTest {

    @Autowired
    private com.example.product.service.ProductAIService productAIService;

    @Test
    @DisplayName("测试AI估价功能 - iPhone 14 Pro")
    void testEstimatePriceiPhone14Pro() {
        // 构建估价参数
        ProductEstimateDTO dto = new ProductEstimateDTO();
        dto.setTitle("iPhone 14 Pro 256GB 深空黑色");
        dto.setDescription("95新，无划痕，电池健康度95%，原装配件齐全");
        dto.setProductCondition("九成新");
        dto.setCategoryId(1);

        // 执行估价
        Result<ProductEstimateVO> result = productAIService.estimatePrice(dto);

        // 验证结果
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertNotNull(result.getData().getEstimatedPrice());
        assertTrue(result.getData().getEstimatedPrice().compareTo(BigDecimal.ZERO) > 0);
        assertNotNull(result.getData().getPriceRange());
        assertNotNull(result.getData().getSuggestion());
        
        System.out.println("估价结果：" + result.getData());
    }

    @Test
    @DisplayName("测试AI估价功能 - 边界条件")
    void testEstimatePriceBoundaryConditions() {
        // 测试无效参数 - 空标题
        ProductEstimateDTO dto = new ProductEstimateDTO();
        dto.setTitle("");
        dto.setDescription("99新，几乎全新，无使用痕迹");
        dto.setProductCondition("全新");
        dto.setCategoryId(1);

        // 执行估价，应该抛出异常
        assertThrows(Exception.class, () -> productAIService.estimatePrice(dto));
    }

    @Test
    @DisplayName("测试AI估价功能 - 缓存效果")
    void testEstimatePriceCache() {
        // 构建相同的估价参数
        ProductEstimateDTO dto = new ProductEstimateDTO();
        dto.setTitle("iPhone 14");
        dto.setDescription("99新，几乎全新，无使用痕迹");
        dto.setProductCondition("全新");
        dto.setCategoryId(1);

        // 第一次估价
        Result<ProductEstimateVO> result1 = productAIService.estimatePrice(dto);
        assertNotNull(result1);
        assertTrue(result1.isSuccess());
        BigDecimal price1 = result1.getData().getEstimatedPrice();

        // 第二次估价（应该命中缓存）
        Result<ProductEstimateVO> result2 = productAIService.estimatePrice(dto);
        assertNotNull(result2);
        assertTrue(result2.isSuccess());
        BigDecimal price2 = result2.getData().getEstimatedPrice();

        // 由于缓存机制，两次估价应该相同
        assertEquals(price1, price2);
        
        System.out.println("缓存测试：第一次价格=" + price1 + ", 第二次价格=" + price2);
    }
}