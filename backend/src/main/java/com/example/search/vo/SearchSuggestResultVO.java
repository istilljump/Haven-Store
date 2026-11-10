package com.example.search.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 搜索补全响应
 *
 * @author ZCode
 * @date 2026/11/11
 */
@Data
@ApiModel(value = "SearchSuggestResultVO", description = "搜索补全响应")
public class SearchSuggestResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 补全候选列表（≤ limit，已按来源优先级与成交热度排序去重） */
    @ApiModelProperty("补全候选列表")
    private List<SearchSuggestItemVO> items = new ArrayList<>();

    /** 纠错建议（命中药表才有值） */
    @ApiModelProperty("纠错建议")
    private SearchCorrectionVO correction;
}
