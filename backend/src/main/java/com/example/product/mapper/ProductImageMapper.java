package com.example.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.product.entity.ProductImage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品图片数据访问对象
 *
 * @author ZCode
 * @date 2026/09/22
 */
@Mapper
public interface ProductImageMapper extends BaseMapper<ProductImage> {
}
