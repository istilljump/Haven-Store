package com.example.campus.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 面交点实体（对应数据库 meet_point 表）
 * <p>
 * 校园内推荐的安全面交位置（快递驿站、图书馆前、校门口等），
 * 发布与详情页据此推荐「离商品最近的面交点」，让同城面交有明确的赴约地点
 *
 * @author ZCode
 * @date 2026/11/10
 */
@Data
@TableName("meet_point")
@ApiModel(value = "MeetPoint", description = "面交点")
public class MeetPoint implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 面交点 ID */
    @TableId(type = IdType.AUTO)
    @ApiModelProperty("面交点ID")
    private Long id;

    /** 所属校区 ID */
    @ApiModelProperty("所属校区ID")
    private Long campusId;

    /** 面交点名称 */
    @ApiModelProperty("面交点名称")
    private String name;

    /** 经度 */
    @ApiModelProperty("经度")
    private BigDecimal longitude;

    /** 纬度 */
    @ApiModelProperty("纬度")
    private BigDecimal latitude;

    /** 位置说明 */
    @ApiModelProperty("位置说明")
    private String description;

    /** 排序值（同名次并列时按 ID） */
    @ApiModelProperty("排序值")
    private Integer sort;
}
