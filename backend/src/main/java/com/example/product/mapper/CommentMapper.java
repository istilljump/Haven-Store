package com.example.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.product.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品评论数据访问对象
 *
 * @author ZCode
 * @date 2026/09/22
 */
@Mapper
public interface CommentMapper extends BaseMapper<Comment> {
}
