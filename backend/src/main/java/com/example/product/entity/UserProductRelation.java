package com.example.product.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户-商品关联实体（对应数据库 user_product_relation 表）
 * <p>
 * 目前只有「收藏」一种关系类型，唯一索引 (user_id, product_id, relation_type)
 * 保证同一用户不会重复收藏同一商品
 *
 * @author ZCode
 * @date 2026/09/22
 */
@Data
@TableName("user_product_relation")
public class UserProductRelation implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 关联 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户 ID */
    private Long userId;

    /** 商品 ID */
    private Long productId;

    /** 关系类型：collect 收藏 */
    private String relationType;

    /** 创建时间（插入时自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间（插入与更新时自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
