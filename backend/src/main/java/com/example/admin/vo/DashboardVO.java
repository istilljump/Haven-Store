package com.example.admin.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 管理后台首页数据
 * <p>
 * 说明：recentProducts 与 systemMessages 始终为数组（无数据时为空数组而非 null），
 * 前端模板直接对它们做 v-for 与 .length 判断，不传 null 可以省掉一堆空值兜底
 *
 * @author ZCode
 * @date 2026/09/22
 */
@Data
@ApiModel(value = "DashboardVO", description = "管理后台首页数据")
public class DashboardVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 平台统计数据 */
    @ApiModelProperty("平台统计数据")
    private Statistics statistics = new Statistics();

    /** 最近发布的商品 */
    @ApiModelProperty("最近发布的商品")
    private List<RecentProduct> recentProducts = new ArrayList<>();

    /** 最近发送的系统消息 */
    @ApiModelProperty("最近发送的系统消息")
    private List<RecentMessage> systemMessages = new ArrayList<>();

    /** 平台统计数据 */
    @Data
    public static class Statistics implements Serializable {
        private static final long serialVersionUID = 1L;
        /** 用户总数 */
        @ApiModelProperty("用户总数")
        private Long totalUsers = 0L;
        /** 账号正常的用户数 */
        @ApiModelProperty("正常状态用户数")
        private Long activeUsers = 0L;
        /** 商品总数 */
        @ApiModelProperty("商品总数")
        private Long totalProducts = 0L;
        /** 今日新增商品数 */
        @ApiModelProperty("今日新增商品数")
        private Long todayProducts = 0L;
        /** 在售商品数 */
        @ApiModelProperty("在售商品数")
        private Long onShelfProducts = 0L;
        /** 分类总数 */
        @ApiModelProperty("分类总数")
        private Long totalCategories = 0L;
    }

    /** 最近发布商品 */
    @Data
    public static class RecentProduct implements Serializable {
        private static final long serialVersionUID = 1L;
        /** 商品 ID */
        @ApiModelProperty("商品ID")
        private Long id;
        /** 商品标题 */
        @ApiModelProperty("商品标题")
        private String title;
        /** 价格（元） */
        @ApiModelProperty("价格（元）")
        private BigDecimal price;
        /** 状态：1 在售，2 已售出，3 已下架 */
        @ApiModelProperty("状态")
        private Integer status;
        /** 发布时间 */
        @ApiModelProperty("发布时间")
        private LocalDateTime createTime;
    }

    /** 最近系统消息 */
    @Data
    public static class RecentMessage implements Serializable {
        private static final long serialVersionUID = 1L;
        /** 消息 ID */
        @ApiModelProperty("消息ID")
        private Long id;
        /** 消息标题 */
        @ApiModelProperty("消息标题")
        private String title;
        /** 发送时间 */
        @ApiModelProperty("发送时间")
        private LocalDateTime createTime;
    }
}
