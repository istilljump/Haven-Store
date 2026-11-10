package com.example.search.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 搜索补全候选项
 * <p>
 * 下拉条目 = 词 + 小字标签（【教材】【数码】）+ 成交热提示（【本校近期成交12件】），
 * source 标明来源便于前端区分样式与排查
 *
 * @author ZCode
 * @date 2026/11/11
 */
@Data
@ApiModel(value = "SearchSuggestItemVO", description = "搜索补全候选项")
public class SearchSuggestItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 候选词 */
    @ApiModelProperty("候选词")
    private String word;

    /** 小字标签：所属品类名（如 教材），可为空 */
    @ApiModelProperty("小字标签：所属品类")
    private String tagLabel;

    /** 成交热提示（如 本校近期成交12件），无成交为 null */
    @ApiModelProperty("成交热提示")
    private String dealHint;

    /** 来源：WORD_PREFIX 词库前缀 / PINYIN 拼音 / CAMPUS_HOT 本校热搜 / CORRECTION 纠错建议 */
    @ApiModelProperty("来源")
    private String source;

    public SearchSuggestItemVO() {
    }

    public SearchSuggestItemVO(String word, String tagLabel, String source) {
        this.word = word;
        this.tagLabel = tagLabel;
        this.source = source;
    }
}
