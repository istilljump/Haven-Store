package com.example.product.serviceImpl;

import com.example.common.BusinessException;
import com.example.common.Result;
import com.example.common.ResultCodeEnum;
import com.example.product.constant.ProductConstant;
import com.example.product.dto.ProductNearbyQueryDTO;
import com.example.product.service.ProductService;
import com.example.product.vo.ProductNearbyVO;
import com.example.utils.UserHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * LBS附近查询功能完整测试类
 * <p>
 * 覆盖所有边界条件、异常处理和性能测试场景
 *
 * @author ZCode
 * @date 2026/09/21
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LBS附近查询功能完整测试")
class ProductServiceFullTest {

    @Mock
    private com.example.product.mapper.ProductMapper productMapper;

    @InjectMocks
    private com.example.product.service.ProductService productService;

    /** 模拟登录用户 ID */
    private static final Long MOCK_USER_ID = 1L;

    @BeforeEach
    void setUp() {
        // 模拟登录态
        UserHolder.setUserId(MOCK_USER_ID);
        // 模拟附近商品查询返回空列表
        when(productMapper.selectNearbyProducts(
                any(BigDecimal.class), any(BigDecimal.class), any(Integer.class), 
                any(Integer.class), any(Integer.class), any(Integer.class), any(Integer.class),
                any(BigDecimal.class), any(BigDecimal.class), any(BigDecimal.class), any(BigDecimal.class)
        )).thenReturn(List.of(createTestNearbyProduct()));
        // 模拟附近商品总数查询
        when(productMapper.selectNearbyProductCount(
                any(BigDecimal.class), any(BigDecimal.class), any(Integer.class), any(Integer.class),
                any(BigDecimal.class), any(BigDecimal.class), any(BigDecimal.class), any(BigDecimal.class)
        )).thenReturn(1L);
    }

    @AfterEach
    void tearDown() {
        UserHolder.remove();
    }

    // ======================== 正常场景测试 ========================

    @Nested
    @DisplayName("正常查询场景")
    class NormalQueryTests {

        @Test
        @DisplayName("标准附近商品查询")
        void testNormalNearbyQuery() {
            ProductNearbyQueryDTO dto = createNearbyQueryDTO(
                116.316833, 39.981013, 5, 1, 10
            );
            
            Result<?> result = productService.findNearbyProducts(dto);
            
            assertAll("正常查询验证",
                () -> assertNotNull(result, "结果不应为空"),
                () -> assertEquals(ResultCodeEnum.SUCCESS.getCode(), result.getCode(), "查询应该成功"),
                () -> assertNotNull(result.getData(), "数据不应为空")
            );
        }

        @Test
        @DisplayName("不同半径查询")
        void testDifferentRadiusQuery() {
            int[] radii = {1, 5, 10, 20, 50};
            
            for (int radius : radii) {
                ProductNearbyQueryDTO dto = createNearbyQueryDTO(
                    116.316833, 39.981013, radius, 1, 10
                );
                
                Result<?> result = productService.findNearbyProducts(dto);
                
                assertAll("半径" + radius + "的查询验证",
                    () -> assertNotNull(result),
                    () -> assertEquals(ResultCodeEnum.SUCCESS.getCode(), result.getCode())
                );
            }
        }

        @Test
        @DisplayName("不同分页参数查询")
        void testDifferentPaginationQuery() {
            int[] pages = {1, 2, 3, 5};
            int[] sizes = {5, 10, 20, 50};
            
            for (int page : pages) {
                for (int size : sizes) {
                    ProductNearbyQueryDTO dto = createNearbyQueryDTO(
                        116.316833, 39.981013, 10, page, size
                    );
                    
                    Result<?> result = productService.findNearbyProducts(dto);
                    
                    assertAll("分页[" + page + "," + size + "]的查询验证",
                        () -> assertNotNull(result),
                        () -> assertEquals(ResultCodeEnum.SUCCESS.getCode(), result.getCode())
                    );
                }
            }
        }
    }

    // ======================== 边界条件测试 ========================

    @Nested
    @DisplayName("边界条件测试")
    class BoundaryConditionTests {

        @Test
        @DisplayName("经度边界值 - 最小经度")
        void testMinLongitudeBoundary() {
            ProductNearbyQueryDTO dto = createNearbyQueryDTO(
                ProductConstant.LONGITUDE_MIN, 39.981013, 5, 1, 10
            );
            
            Result<?> result = productService.findNearbyProducts(dto);
            
            assertAll("最小经度边界验证",
                () -> assertNotNull(result),
                () -> assertEquals(ResultCodeEnum.SUCCESS.getCode(), result.getCode())
            );
        }

        @Test
        @DisplayName("经度边界值 - 最大经度")
        void testMaxLongitudeBoundary() {
            ProductNearbyQueryDTO dto = createNearbyQueryDTO(
                ProductConstant.LONGITUDE_MAX, 39.981013, 5, 1, 10
            );
            
            Result<?> result = productService.findNearbyProducts(dto);
            
            assertAll("最大经度边界验证",
                () -> assertNotNull(result),
                () -> assertEquals(ResultCodeEnum.SUCCESS.getCode(), result.getCode())
            );
        }

        @Test
        @DisplayName("纬度边界值 - 最小纬度")
        void testMinLatitudeBoundary() {
            ProductNearbyQueryDTO dto = createNearbyQueryDTO(
                116.316833, ProductConstant.LATITUDE_MIN, 5, 1, 10
            );
            
            Result<?> result = productService.findNearbyProducts(dto);
            
            assertAll("最小纬度边界验证",
                () -> assertNotNull(result),
                () -> assertEquals(ResultCodeEnum.SUCCESS.getCode(), result.getCode())
            );
        }

        @Test
        @DisplayName("纬度边界值 - 最大纬度")
        void testMaxLatitudeBoundary() {
            ProductNearbyQueryDTO dto = createNearbyQueryDTO(
                116.316833, ProductConstant.LATITUDE_MAX, 5, 1, 10
            );
            
            Result<?> result = productService.findNearbyProducts(dto);
            
            assertAll("最大纬度边界验证",
                () -> assertNotNull(result),
                () -> assertEquals(ResultCodeEnum.SUCCESS.getCode(), result.getCode())
            );
        }

        @Test
        @DisplayName("查询半径边界值 - 最小半径")
        void testMinRadiusBoundary() {
            ProductNearbyQueryDTO dto = createNearbyQueryDTO(
                116.316833, 39.981013, 1, 1, 10
            );
            
            Result<?> result = productService.findNearbyProducts(dto);
            
            assertAll("最小半径边界验证",
                () -> assertNotNull(result),
                () -> assertEquals(ResultCodeEnum.SUCCESS.getCode(), result.getCode())
            );
        }

        @Test
        @DisplayName("查询半径边界值 - 最大半径")
        void testMaxRadiusBoundary() {
            ProductNearbyQueryDTO dto = createNearbyQueryDTO(
                116.316833, 39.981013, 50, 1, 10
            );
            
            Result<?> result = productService.findNearbyProducts(dto);
            
            assertAll("最大半径边界验证",
                () -> assertNotNull(result),
                () -> assertEquals(ResultCodeEnum.SUCCESS.getCode(), result.getCode())
            );
        }

        @Test
        @DisplayName("分页边界值 - 最小页码")
        void testMinPageNumberBoundary() {
            ProductNearbyQueryDTO dto = createNearbyQueryDTO(
                116.316833, 39.981013, 5, 1, 10
            );
            
            Result<?> result = productService.findNearbyProducts(dto);
            
            assertAll("最小页码边界验证",
                () -> assertNotNull(result),
                () -> assertEquals(ResultCodeEnum.SUCCESS.getCode(), result.getCode())
            );
        }

        @Test
        @DisplayName("分页边界值 - 最小页大小")
        void testMinPageSizeBoundary() {
            ProductNearbyQueryDTO dto = createNearbyQueryDTO(
                116.316833, 39.981013, 5, 1, 1
            );
            
            Result<?> result = productService.findNearbyProducts(dto);
            
            assertAll("最小页大小边界验证",
                () -> assertNotNull(result),
                () -> assertEquals(ResultCodeEnum.SUCCESS.getCode(), result.getCode())
            );
        }

        @Test
        @DisplayName("分页边界值 - 最大页大小")
        void testMaxPageSizeBoundary() {
            ProductNearbyQueryDTO dto = createNearbyQueryDTO(
                116.316833, 39.981013, 5, 1, 100
            );
            
            Result<?> result = productService.findNearbyProducts(dto);
            
            assertAll("最大页大小边界验证",
                () -> assertNotNull(result),
                () -> assertEquals(ResultCodeEnum.SUCCESS.getCode(), result.getCode())
            );
        }
    }

    // ======================== 异常场景测试 ========================

    @Nested
        @DisplayName("异常场景测试")
    class ExceptionTests {

        @Test
        @DisplayName("无效经纬度 - 经度超出范围")
        void testInvalidLongitudeRange() {
            ProductNearbyQueryDTO dto = createNearbyQueryDTO(
                ProductConstant.LONGITUDE_MAX + 1, 39.981013, 5, 1, 10
            );
            
            BusinessException ex = assertThrows(BusinessException.class, 
                () -> productService.findNearbyProducts(dto));
            
            assertEquals(ResultCodeEnum.PARAM_ERROR.getCode(), ex.getCode());
            assertEquals("经度必须在-180到180之间", ex.getMessage());
        }

        @Test
        @DisplayName("无效经纬度 - 纬度超出范围")
        void testInvalidLatitudeRange() {
            ProductNearbyQueryDTO dto = createNearbyQueryDTO(
                116.316833, ProductConstant.LATITUDE_MAX + 1, 5, 1, 10
            );
            
            BusinessException ex = assertThrows(BusinessException.class, 
                () -> productService.findNearbyProducts(dto));
            
            assertEquals(ResultCodeEnum.PARAM_ERROR.getCode(), ex.getCode());
            assertEquals("纬度必须在-90到90之间", ex.getMessage());
        }

        @Test
        @DisplayName("无效经纬度 - 经度小于最小值")
        void testInvalidLongitudeMinRange() {
            ProductNearbyQueryDTO dto = createNearbyQueryDTO(
                ProductConstant.LONGITUDE_MIN - 1, 39.981013, 5, 1, 10
            );
            
            BusinessException ex = assertThrows(BusinessException.class, 
                () -> productService.findNearbyProducts(dto));
            
            assertEquals(ResultCodeEnum.PARAM_ERROR.getCode(), ex.getCode());
            assertEquals("经度必须在-180到180之间", ex.getMessage());
        }

        @Test
        @DisplayName("无效经纬度 - 纬度小于最小值")
        void testInvalidLatitudeMinRange() {
            ProductNearbyQueryDTO dto = createNearbyQueryDTO(
                116.316833, ProductConstant.LATITUDE_MIN - 1, 5, 1, 10
            );
            
            BusinessException ex = assertThrows(BusinessException.class, 
                () -> productService.findNearbyProducts(dto));
            
            assertEquals(ResultCodeEnum.PARAM_ERROR.getCode(), ex.getCode());
            assertEquals("纬度必须在-90到90之间", ex.getMessage());
        }

        @Test
        @DisplayName("无效查询参数 - 经纬度为空")
        void testEmptyCoordinates() {
            ProductNearbyQueryDTO dto = new ProductNearbyQueryDTO();
            dto.setRadius(5);
            dto.setPageNum(1);
            dto.setPageSize(10);
            
            BusinessException ex = assertThrows(BusinessException.class, 
                () -> productService.findNearbyProducts(dto));
            
            assertEquals(ResultCodeEnum.PARAM_ERROR.getCode(), ex.getCode());
            assertEquals("经纬度不能为空", ex.getMessage());
        }

        @Test
        @DisplayName("无效查询参数 - 页码小于1")
        void testInvalidPageNumber() {
            ProductNearbyQueryDTO dto = createNearbyQueryDTO(
                116.316833, 39.981013, 5, 0, 10
            );
            
            BusinessException ex = assertThrows(BusinessException.class, 
                () -> productService.findNearbyProducts(dto));
            
            assertEquals(ResultCodeEnum.PARAM_ERROR.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("无效查询参数 - 页大小小于1")
        void testInvalidPageSize() {
            ProductNearbyQueryDTO dto = createNearbyQueryDTO(
                116.316833, 39.981013, 5, 1, 0
            );
            
            BusinessException ex = assertThrows(BusinessException.class, 
                () -> productService.findNearbyProducts(dto));
            
            assertEquals(ResultCodeEnum.PARAM_ERROR.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("无效查询参数 - 页大小过大")
        void testInvalidPageSizeTooLarge() {
            ProductNearbyQueryDTO dto = createNearbyQueryDTO(
                116.316833, 39.981013, 5, 1, 1000
            );
            
            BusinessException ex = assertThrows(BusinessException.class, 
                () -> productService.findNearbyProducts(dto));
            
            assertEquals(ResultCodeEnum.PARAM_ERROR.getCode(), ex.getCode());
        }
    }

    // ======================== 性能测试 ========================

    @Nested
        @DisplayName("性能测试")
    class PerformanceTests {

        @Test
        @DisplayName("并发查询性能测试")
        void testConcurrentQueryPerformance() {
            int threadCount = 5;
            Thread[] threads = new Thread[threadCount];
            
            for (int i = 0; i < threadCount; i++) {
                threads[i] = new Thread(() -> {
                    ProductNearbyQueryDTO dto = createNearbyQueryDTO(
                        116.316833, 39.981013, 5, 1, 10
                    );
                    
                    long startTime = System.currentTimeMillis();
                    Result<?> result = productService.findNearbyProducts(dto);
                    long endTime = System.currentTimeMillis();
                    
                    assertAll("并发查询验证",
                        () -> assertNotNull(result),
                        () -> assertEquals(ResultCodeEnum.SUCCESS.getCode(), result.getCode()),
                        () -> assertTrue(endTime - startTime < 1000, "查询应该在1秒内完成")
                    );
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

        @Test
        @DisplayName("大数据量查询性能测试")
        void testLargeDataQueryPerformance() {
            // 模拟返回大量数据
            when(productMapper.selectNearbyProductCount(
                    any(BigDecimal.class), any(BigDecimal.class), any(Integer.class), any(Integer.class),
                    any(BigDecimal.class), any(BigDecimal.class), any(BigDecimal.class), any(BigDecimal.class)
            )).thenReturn(1000L);
            
            ProductNearbyQueryDTO dto = createNearbyQueryDTO(
                116.316833, 39.981013, 5, 1, 100
            );
            
            long startTime = System.currentTimeMillis();
            Result<?> result = productService.findNearbyProducts(dto);
            long endTime = System.currentTimeMillis();
            
            assertAll("大数据量查询验证",
                () -> assertNotNull(result),
                () -> assertEquals(ResultCodeEnum.SUCCESS.getCode(), result.getCode()),
                () -> assertTrue(endTime - startTime < 2000, "大数据量查询应该在2秒内完成")
            );
        }
    }

    // ======================== 辅助方法 ========================

    /**
     * 创建附近查询DTO模板
     */
    private ProductNearbyQueryDTO createNearbyQueryDTO(double centerLon, double centerLat, 
                                                     int radius, int pageNum, int pageSize) {
        ProductNearbyQueryDTO dto = new ProductNearbyQueryDTO();
        dto.setCenterLongitude(BigDecimal.valueOf(centerLon));
        dto.setCenterLatitude(BigDecimal.valueOf(centerLat));
        dto.setRadius(radius);
        dto.setPageNum(pageNum);
        dto.setPageSize(pageSize);
        return dto;
    }

    /**
     * 创建测试用的附近商品
     */
    private ProductNearbyVO createTestNearbyProduct() {
        ProductNearbyVO product = new ProductNearbyVO();
        product.setProductId(1L);
        product.setTitle("测试商品");
        product.setPrice(BigDecimal.valueOf(999.00));
        product.setProductCondition("九成新");
        product.setTradeType("线下");
        product.setDistance(1.5);
        product.setLongitude(BigDecimal.valueOf(116.316833));
        product.setLatitude(BigDecimal.valueOf(39.981013));
        return product;
    }
}