package com.example.admin.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 管理后台系统消息列表项
 * <p>
 * 一条群发记录在数据库中会按接收者展开为多行，
 * 本对象是「按标题+内容+接收群体」聚合后的一条公告
 *
 * @author ZCode
 * @date 2026/09/22
 */
@Data
@ApiModel(value = "AdminMessageVO", description = "管理后台系统消息列表项")
public class AdminMessageVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 消息 ID（取同组记录中最小的一条，用于前端定位） */
    @ApiModelProperty("消息ID")
    private Long id;

    /** 消息标题 */
    @ApiModelProperty("消息标题")
    private String title;

    /** 消息内容 */
    @ApiModelProperty("消息内容")
    private String content;

    /** 消息类型：1 系统通知，2 公告，3 交易消息，4 其他 */
    @ApiModelProperty("消息类型")
    private Integer messageType;

    /** 接收群体：all 所有用户 / admin 仅管理员 / user 仅普通用户 */
    @ApiModelProperty("接收群体")
    private String userType;

    /** 送达人数（同组记录数） */
    @ApiModelProperty("送达人数")
    private Integer receiverCount;

    /** 状态：1 发送中，2 已送达 */
    @ApiModelProperty("状态：1发送中 2已送达")
    private Integer status;

    /** 发送时间 */
    @ApiModelProperty("发送时间")
    private LocalDateTime createTime;
}
