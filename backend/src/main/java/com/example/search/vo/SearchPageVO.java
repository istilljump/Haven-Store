package com.example.search.vo;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.product.vo.ProductListVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 增强搜索响应：纠错信息 + 分页结果（条目内嵌行情标签）+ 埋点 ID
 * <p>
 * 原 /product/search 接口原样保留，本响应是其「配套增强层」：
 * 条目在 ProductListVO 基础上追加 marketMedian（品类中位价）与
 * pricePosition（价格分位），供卡片行情小字与划算指数展示
 *
 * @author ZCode
 * @date 2026/11/11
 */
@Data
@ApiModel(value = "SearchPageVO", description = "增强搜索响应")
public class SearchPageVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 纠错信息（未纠错为 null） */
    @ApiModelProperty("纠错信息")
    private SearchCorrectionVO correction;

    /** 分页结果（条目含 marketMedian/pricePosition 行情标签字段） */
    @ApiModelProperty("分页结果")
    private Page<ProductListVO> page;

    /** 本次搜索埋点 ID（前端点击结果时随信标回传，便于「点击→成交」归因） */
    @ApiModelProperty("埋点ID")
    private Long logId;
}
