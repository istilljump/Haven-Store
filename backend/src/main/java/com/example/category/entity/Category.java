package com.example.category.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商品分类实体类（对应数据库 category 表）
 *
 * @author ZCode
 * @date 2026/09/21
 */
@Data
@ApiModel(value = "Category", description = "商品分类实体")
@TableName("category")
public class Category implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 分类 ID（主键，数据库自增；与 product.category_id 关联） */
    @ApiModelProperty("分类ID")
    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 分类名称（全局唯一） */
    @ApiModelProperty("分类名称")
    private String name;

    /** 排序值（数字越小越靠前） */
    @ApiModelProperty("排序值（越小越靠前）")
    private Integer sort;

    /** 分类状态：1 启用，0 禁用（管理后台可切换，禁用后不在分类选择中展示） */
    @ApiModelProperty("分类状态：1启用 0禁用")
    private Integer status;

    /** 创建时间（插入时由 MetaObjectHandler 自动填充） */
    @ApiModelProperty("创建时间")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间（插入与更新时由 MetaObjectHandler 自动填充） */
    @ApiModelProperty("更新时间")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
