package com.example.campus.vo;

import com.example.campus.entity.MeetPoint;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 面交点视图对象：面交点基础信息 + 到参照点的距离
 * <p>
 * 距离由服务端按 Haversine 球面距离计算，前端不重复实现地理算法
 *
 * @author ZCode
 * @date 2026/11/10
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "MeetPointVO", description = "面交点（含距离）")
public class MeetPointVO extends MeetPoint {

    private static final long serialVersionUID = 1L;

    /** 到参照点的距离（米，四舍五入取整） */
    @ApiModelProperty("到参照点的距离（米）")
    private Integer distanceMeters;
}
