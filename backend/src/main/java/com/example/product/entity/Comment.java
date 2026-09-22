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
 * 商品评论实体（对应数据库 comment 表）
 *
 * @author ZCode
 * @date 2026/09/22
 */
@Data
@TableName("comment")
public class Comment implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 评论 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 评论用户 ID */
    private Long userId;

    /** 商品 ID */
    private Long productId;

    /** 评论内容 */
    private String content;

    /** 评分 1-5 */
    private Integer rating;

    /** 状态：1 正常，0 隐藏（管理端下架用，当前无对应入口） */
    private Integer status;

    /** 创建时间（插入时自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间（插入与更新时自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
