package com.example.product.serviceImpl;

import com.example.common.Result;
import com.example.common.ResultCodeEnum;
import com.example.product.dto.ProductNearbyQueryDTO;
import com.example.product.vo.ProductNearbyVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 简化的商品服务集成测试
 * <p>
 * 用于验证LBS附近查询功能的基本功能
 *
 * @author ZCode
 * @date 2026/09/21
 */
@SpringBootTest
@Transactional
class ProductServiceSimpleTest {

    @Autowired
    private com.example.product.service.ProductService productService;

    @Test
    @DisplayName("测试LBS附近查询功能")
    void testNearbyQuery() {
        // 构建查询参数
        ProductNearbyQueryDTO dto = new ProductNearbyQueryDTO();
        dto.setCenterLongitude(BigDecimal.valueOf(116.316833));
        dto.setCenterLatitude(BigDecimal.valueOf(39.981013));
        dto.setRadius(5);
        dto.setPageNum(1);
        dto.setPageSize(10);

        // 执行查询
        Result<?> result = productService.findNearbyProducts(dto);

        // 验证结果
        assertNotNull(result);
        assertEquals(ResultCodeEnum.SUCCESS.getCode(), result.getCode());
        
        // 这里可以添加更多的验证逻辑
        System.out.println("查询成功，结果：" + result);
    }

    @Test
    @DisplayName("测试边界条件查询")
    void testBoundaryConditions() {
        // 测试无效参数
        ProductNearbyQueryDTO dto = new ProductNearbyQueryDTO();
        // 故意不设置经纬度，测试边界处理
        dto.setRadius(5);

        // 执行查询，应该抛出异常
        assertThrows(Exception.class, () -> productService.findNearbyProducts(dto));
    }
}