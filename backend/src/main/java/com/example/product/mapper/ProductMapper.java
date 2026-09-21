package com.example.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.product.entity.Product;
import com.example.product.vo.ProductNearbyVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 二手商品模块数据访问层
 * <p>
 * 继承 MyBatis-Plus BaseMapper 即拥有通用 CRUD 能力；
 * 全部通过 MyBatis-Plus API（预编译参数化查询）访问数据库，杜绝 SQL 注入风险
 *
 * @author ZCode
 * @date 2026/09/21
 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    /**
     * 查询附近商品
     * @param centerLongitude 中心点经度
     * @param centerLatitude 中心点纬度
     * @param radius 查询半径（公里）
     * @param status 商品状态（1上架）
     * @param pageNum 分页页码
     * @param pageSize 每页条数
     * @return 附近商品列表
     */
    List<ProductNearbyVO> selectNearbyProducts(
            @Param("centerLongitude") BigDecimal centerLongitude,
            @Param("centerLatitude") BigDecimal centerLatitude,
            @Param("radius") Integer radius,
            @Param("status") Integer status,
            @Param("pageNum") Integer pageNum,
            @Param("pageSize") Integer pageSize,
            @Param("minLongitude") BigDecimal minLongitude,
            @Param("maxLongitude") BigDecimal maxLongitude,
            @Param("minLatitude") BigDecimal minLatitude,
            @Param("maxLatitude") BigDecimal maxLatitude);

    /**
     * 获取附近商品总数
     * @param centerLongitude 中心点经度
     * @param centerLatitude 中心点纬度
     * @param radius 查询半径（公里）
     * @return 附近商品总数
     */
    Long selectNearbyProductCount(
            @Param("centerLongitude") BigDecimal centerLongitude,
            @Param("centerLatitude") BigDecimal centerLatitude,
            @Param("radius") Integer radius,
            @Param("minLongitude") BigDecimal minLongitude,
            @Param("maxLongitude") BigDecimal maxLongitude,
            @Param("minLatitude") BigDecimal minLatitude,
            @Param("maxLatitude") BigDecimal maxLatitude);
}
