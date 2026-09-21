package com.example.product.serviceImpl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.category.entity.Category;
import com.example.category.mapper.CategoryMapper;
import com.example.common.BusinessException;
import com.example.common.Result;
import com.example.common.ResultCodeEnum;
import com.example.product.constant.ProductConstant;
import com.example.product.dto.ProductAddDTO;
import com.example.product.dto.ProductNearbyQueryDTO;
import com.example.product.entity.Product;
import com.example.product.enums.ProductStatusEnum;
import com.example.product.mapper.ProductMapper;
import com.example.product.service.ProductService;
import com.example.product.vo.ProductAddVO;
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
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * ProductServiceImpl 单元测试
 * <p>
 * 覆盖LBS附近查询功能的各类场景及边界条件
 *
 * @author ZCode
 * @date 2026/09/21
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("商品服务单元测试 - LBS附近查询功能")
class ProductServiceImplTest {

    @Mock
    private ProductMapper productMapper;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private ProductServiceImpl productService;

    /** 模拟登录用户 ID */
    private static final Long MOCK_USER_ID = 1L;

    /** 模拟存在的分类 */
    private final Category existCategory = new Category();

    @BeforeEach
    void setUp() {
        // 模拟登录态
        UserHolder.setUserId(MOCK_USER_ID);
        // 模拟分类存在
        existCategory.setId(1);
        existCategory.setName("手机数码");
        when(categoryMapper.selectById(1)).thenReturn(existCategory);
        // 模拟插入成功（MyBatis-Plus insert 回填主键）
        when(productMapper.insert(any(Product.class))).thenAnswer(invocation -> {
            Product p = invocation.getArgument(0);
            p.setId(100L);
            return 1;
        });
        // 模拟附近商品查询
        when(productMapper.selectNearbyProducts(
                any(BigDecimal.class), any(BigDecimal.class), any(Integer.class), 
                any(Integer.class), any(Integer.class), any(Integer.class),
                any(BigDecimal.class), any(BigDecimal.class), any(BigDecimal.class), any(BigDecimal.class)
        )).thenReturn(List.of(createTestNearbyProduct()));
        // 模拟附近商品总数查询
        when(productMapper.selectNearbyProductCount(
                any(BigDecimal.class), any(BigDecimal.class), any(Integer.class),
                any(BigDecimal.class), any(BigDecimal.class), any(BigDecimal.class), any(BigDecimal.class)
        )).thenReturn(1L);
    }

    @AfterEach
    void tearDown() {
        UserHolder.remove();
    }

    // ======================== LBS附近查询测试 ========================

    @Nested
    @DisplayName("LBS附近查询功能")
    class NearbyQueryTests {

        @Test
        @DisplayName("正常查询附近商品")
        void findNearbyProducts_success() {
            ProductNearbyQueryDTO dto = buildNearbyQueryDTO();
            Result<Page<ProductNearbyVO>> result = productService.findNearbyProducts(dto);
            assertNotNull(result.getData());
            assertEquals(1, result.getData().getTotal());
            assertNotNull(result.getData().getRecords());
            assertEquals(1, result.getData().getRecords().size());
        }

        @Test
        @DisplayName("经纬度边界值测试 - 最小经度")
        void findNearbyProducts_minLongitude() {
            ProductNearbyQueryDTO dto = buildNearbyQueryDTO();
            dto.setCenterLongitude(BigDecimal.valueOf(ProductConstant.LONGITUDE_MIN));
            Result<Page<ProductNearbyVO>> result = productService.findNearbyProducts(dto);
            assertNotNull(result.getData());
        }

        @Test
        @DisplayName("经纬度边界值测试 - 最大经度")
        void findNearbyProducts_maxLongitude() {
            ProductNearbyQueryDTO dto = buildNearbyQueryDTO();
            dto.setCenterLongitude(BigDecimal.valueOf(ProductConstant.LONGITUDE_MAX));
            Result<Page<ProductNearbyVO>> result = productService.findNearbyProducts(dto);
            assertNotNull(result.getData());
        }

        @Test
        @DisplayName("经纬度边界值测试 - 最小纬度")
        void findNearbyProducts_minLatitude() {
            ProductNearbyQueryDTO dto = buildNearbyQueryDTO();
            dto.setCenterLatitude(BigDecimal.valueOf(ProductConstant.LATITUDE_MIN));
            Result<Page<ProductNearbyVO>> result = productService.findNearbyProducts(dto);
            assertNotNull(result.getData());
        }

        @Test
        @DisplayName("经纬度边界值测试 - 最大纬度")
        void findNearbyProducts_maxLatitude() {
            ProductNearbyQueryDTO dto = buildNearbyQueryDTO();
            dto.setCenterLatitude(BigDecimal.valueOf(ProductConstant.LATITUDE_MAX));
            Result<Page<ProductNearbyVO>> result = productService.findNearbyProducts(dto);
            assertNotNull(result.getData());
        }

        @Test
        @DisplayName("查询半径边界值测试 - 最小半径")
        void findNearbyProducts_minRadius() {
            ProductNearbyQueryDTO dto = buildNearbyQueryDTO();
            dto.setRadius(1);
            Result<Page<ProductNearbyVO>> result = productService.findNearbyProducts(dto);
            assertNotNull(result.getData());
        }

        @Test
        @DisplayName("查询半径边界值测试 - 最大半径")
        void findNearbyProducts_maxRadius() {
            ProductNearbyQueryDTO dto = buildNearbyQueryDTO();
            dto.setRadius(50);
            Result<Page<ProductNearbyVO>> result = productService.findNearbyProducts(dto);
            assertNotNull(result.getData());
        }

        @Test
        @DisplayName("无效经纬度 - 经度超出范围")
        void findNearbyProducts_invalidLongitude() {
            ProductNearbyQueryDTO dto = buildNearbyQueryDTO();
            dto.setCenterLongitude(BigDecimal.valueOf(ProductConstant.LONGITUDE_MAX + 1));
            BusinessException ex = assertThrows(BusinessException.class, () -> productService.findNearbyProducts(dto));
            assertEquals(ResultCodeEnum.PARAM_ERROR.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("无效经纬度 - 纬度超出范围")
        void findNearbyProducts_invalidLatitude() {
            ProductNearbyQueryDTO dto = buildNearbyQueryDTO();
            dto.setCenterLatitude(BigDecimal.valueOf(ProductConstant.LATITUDE_MAX + 1));
            BusinessException ex = assertThrows(BusinessException.class, () -> productService.findNearbyProducts(dto));
            assertEquals(ResultCodeEnum.PARAM_ERROR.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("无效查询参数 - 经纬度为空")
        void findNearbyProducts_nullCoordinates() {
            ProductNearbyQueryDTO dto = new ProductNearbyQueryDTO();
            BusinessException ex = assertThrows(BusinessException.class, () -> productService.findNearbyProducts(dto));
            assertEquals(ResultCodeEnum.PARAM_ERROR.getCode(), ex.getCode());
        }
    }

    // ======================== 辅助方法 ========================

    /**
     * 构建附近查询DTO模板
     */
    private ProductNearbyQueryDTO buildNearbyQueryDTO() {
        ProductNearbyQueryDTO dto = new ProductNearbyQueryDTO();
        dto.setCenterLongitude(BigDecimal.valueOf(116.316833));
        dto.setCenterLatitude(BigDecimal.valueOf(39.981013));
        dto.setRadius(5);
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