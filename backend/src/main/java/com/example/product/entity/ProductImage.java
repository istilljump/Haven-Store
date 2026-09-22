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
 * 商品图片实体（对应数据库 product_image 表）
 * <p>
 * 一个商品可以有多张图，sort 最小的作为封面（同时冗余写入 product.cover_image，
 * 供列表页直接使用，避免列表查询再关联一次图片表）
 *
 * @author ZCode
 * @date 2026/09/22
 */
@Data
@TableName("product_image")
public class ProductImage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 图片 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商品 ID */
    private Long productId;

    /** 图片访问地址 */
    private String imageUrl;

    /** 排序值（越小越靠前） */
    private Integer sort;

    /** 创建时间（插入时自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
