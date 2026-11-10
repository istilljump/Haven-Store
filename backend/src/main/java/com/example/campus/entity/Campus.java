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
 * 校区实体（对应数据库 campus 表）
 * <p>
 * 校园场景的地理锚点：列表页校区筛选、附近商品默认圆心都从这里取坐标。
 * 数据由建表脚本种子化，管理端暂不提供增删改（保持模块最小闭环）
 *
 * @author ZCode
 * @date 2026/11/10
 */
@Data
@TableName("campus")
@ApiModel(value = "Campus", description = "校区")
public class Campus implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 校区 ID */
    @TableId(type = IdType.AUTO)
    @ApiModelProperty("校区ID")
    private Long id;

    /** 校区名称 */
    @ApiModelProperty("校区名称")
    private String name;

    /** 中心经度（附近商品默认圆心） */
    @ApiModelProperty("中心经度")
    private BigDecimal centerLongitude;

    /** 中心纬度 */
    @ApiModelProperty("中心纬度")
    private BigDecimal centerLatitude;

    /** 排序值（越小越靠前） */
    @ApiModelProperty("排序值")
    private Integer sort;

    /** 状态：1 启用，0 禁用 */
    @ApiModelProperty("状态：1启用 0禁用")
    private Integer status;
}
