package com.example.recommend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 互补品类关联字典（对应数据库 category_relation 表）
 * <p>
 * 「搜自行车 → 推车锁/头盔」的配置化知识：由种子脚本维护，
 * 支持同品类互补（教材→习题册）与跨品类互补（手机数码→电脑外设）
 *
 * @author ZCode
 * @date 2026/11/11
 */
@Data
@TableName("category_relation")
public class CategoryRelation implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 关联 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 源品类 ID（搜索词命中的品类） */
    private Integer categoryId;

    /** 互补品类 ID（推荐来源品类） */
    private Integer relatedCategoryId;

    /** 关联权重（越大越优先） */
    private Integer weight;

    /** 关联说明（人工可读，运营用） */
    private String note;

    /** 状态：1 启用，0 停用 */
    private Integer status;
}
