package com.example.search.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 搜索埋点展示对象（演示与验证用，不暴露敏感字段）
 *
 * @author ZCode
 * @date 2026/11/11
 */
@Data
@ApiModel(value = "SearchLogVO", description = "搜索埋点")
public class SearchLogVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 埋点 ID */
    @ApiModelProperty("埋点ID")
    private Long id;

    /** 原始搜索词 */
    @ApiModelProperty("搜索词")
    private String keyword;

    /** 纠错后检索词 */
    @ApiModelProperty("纠错后检索词")
    private String correctedKeyword;

    /** 校区 ID */
    @ApiModelProperty("校区ID")
    private Long campusId;

    /** 结果条数 */
    @ApiModelProperty("结果条数")
    private Integer resultCount;

    /** 点击的商品 ID */
    @ApiModelProperty("点击商品ID")
    private Long clickedProductId;

    /** 是否成交：0/1 */
    @ApiModelProperty("是否成交")
    private Integer dealFlag;

    /** 搜索时间 */
    @ApiModelProperty("搜索时间")
    private LocalDateTime createTime;
}
