package com.example.product.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 卖家信用画像（轮4：可信交易轻量层）
 * <p>
 * 三维确定性公式（无新表、无外部依赖）：
 * 成交件数（order_item 已支付/已完成）、收到评价均分（status=1 评论）、
 * 有效举报扣分（已处理且动作非驳回的举报）。
 * 全部实时统计平台自身数据，信用分随交易即时变化——与行情同源的飞轮逻辑
 *
 * @author ZCode
 * @date 2026/11/11
 */
@Data
@ApiModel(value = "CreditVO", description = "卖家信用画像")
public class CreditVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 近 90 天成交件数 */
    @ApiModelProperty("近90天成交件数")
    private Integer completedDeals;

    /** 收到的评价均分（无评价为 null） */
    @ApiModelProperty("评价均分")
    private Double avgRating;

    /** 有效举报次数（已处理的商品举报） */
    @ApiModelProperty("有效举报次数")
    private Integer validReports;

    /** 信用分（0-100） */
    @ApiModelProperty("信用分（0-100）")
    private Integer creditScore;

    /** 信用等级：优秀/良好/一般/待积累 */
    @ApiModelProperty("信用等级")
    private String level;

    /** 评价标签聚合 Top（轮8：卖家收到的评价标签按次数取前 3） */
    private java.util.List<String> topTags;
}
