package com.example.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.product.entity.UserProductRelation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户-商品关联数据访问对象
 *
 * @author ZCode
 * @date 2026/09/22
 */
@Mapper
public interface UserProductRelationMapper extends BaseMapper<UserProductRelation> {
}
