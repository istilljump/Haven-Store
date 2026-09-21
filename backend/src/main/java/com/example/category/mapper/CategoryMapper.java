package com.example.category.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.category.entity.Category;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品分类模块数据访问层
 * <p>
 * 继承 MyBatis-Plus BaseMapper 即拥有通用 CRUD 能力；
 * 全部通过 MyBatis-Plus API（预编译参数化查询）访问数据库，杜绝 SQL 注入风险
 *
 * @author ZCode
 * @date 2026/09/21
 */
@Mapper
public interface CategoryMapper extends BaseMapper<Category> {
}
