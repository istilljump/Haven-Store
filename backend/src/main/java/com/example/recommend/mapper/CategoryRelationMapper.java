package com.example.recommend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.recommend.entity.CategoryRelation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 互补品类字典数据访问层
 *
 * @author ZCode
 * @date 2026/11/11
 */
@Mapper
public interface CategoryRelationMapper extends BaseMapper<CategoryRelation> {
}
