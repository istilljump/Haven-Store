package com.example.product.serviceImpl;

import com.example.category.entity.Category;
import com.example.category.mapper.CategoryMapper;
import com.example.common.BusinessException;
import com.example.common.Result;
import com.example.product.constant.ProductConstant;
import com.example.product.dto.ProductAddDTO;
import com.example.product.entity.Product;
import com.example.product.mapper.ProductMapper;
import com.example.product.vo.ProductAddVO;
import com.example.utils.UserHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * ProductServiceImpl 单元测试
 * <p>
 * 覆盖商品发布接口的线上/线下两种场景及各类参数校验边界
 *
 * @author ZCode
 * @date 2026/09/21
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("商品发布服务单元测试")
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
        lenient().when(categoryMapper.selectById(1)).thenReturn(existCategory);
        // 模拟插入成功（MyBatis-Plus insert 回填主键）
        lenient().when(productMapper.insert(any(Product.class))).thenAnswer(invocation -> {
            Product p = invocation.getArgument(0);
            p.setId(100L);
            return 1;
        });
    }

    @AfterEach
    void tearDown() {
        UserHolder.remove();
    }

    @Test
    @DisplayName("线上交易正常发布（不传地址经纬度）")
    void addProduct_online_success() {
        ProductAddDTO dto = buildOnlineDTO();
        Result<ProductAddVO> result = productService.addProduct(dto);
        assertNotNull(result.getData());
        assertNotNull(result.getData().getProductId());
    }

    @Test
    @DisplayName("线上交易前端误传经纬度→强制清空，不存脏坐标")
    void addProduct_online_withDirtyCoords_shouldClean() {
        ProductAddDTO dto = buildOnlineDTO();
        // 前端误传了经纬度
        dto.setLongitude(BigDecimal.ZERO);
        dto.setLatitude(BigDecimal.ZERO);
        dto.setAddress("误传地址");

        productService.addProduct(dto);

        // 验证插入的 Product 经纬度和地址被清空
        Mockito.verify(productMapper).insert(org.mockito.ArgumentMatchers.argThat(product ->
                product.getLongitude() == null
                        && product.getLatitude() == null
                        && product.getAddress() == null
        ));
    }

    @Test
    @DisplayName("价格为0→Service层兜底拦截400")
    void addProduct_priceZero() {
        ProductAddDTO dto = buildOnlineDTO();
        dto.setPrice(BigDecimal.ZERO);
        assertParamError("商品价格必须大于0", dto);
    }

    @Test
    @DisplayName("分类不存在→400")
    void addProduct_categoryNotExist() {
        ProductAddDTO dto = buildOnlineDTO();
        dto.setCategoryId(999);
        assertParamError("商品分类不存在", dto);
    }

    /**
     * 构建线上交易 DTO 模板
     */
    private ProductAddDTO buildOnlineDTO() {
        ProductAddDTO dto = new ProductAddDTO();
        dto.setTitle("线上商品");
        dto.setCategoryId(1);
        dto.setPrice(new BigDecimal("99.00"));
        dto.setProductCondition("全新");
        dto.setTradeType(ProductConstant.TRADE_TYPE_ONLINE);
        return dto;
    }

    /**
     * 断言抛出 BusinessException 且 code=400，msg 包含预期信息
     */
    private void assertParamError(String expectedMsg, ProductAddDTO dto) {
        BusinessException ex = assertThrows(BusinessException.class, () -> productService.addProduct(dto));
        assertEquals(400, ex.getCode());
        assertEquals(expectedMsg, ex.getMessage());
    }
}