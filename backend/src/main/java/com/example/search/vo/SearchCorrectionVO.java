package com.example.search.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 搜索纠错结果
 *
 * @author ZCode
 * @date 2026/11/11
 */
@Data
@ApiModel(value = "SearchCorrectionVO", description = "搜索纠错结果")
public class SearchCorrectionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户原始输入 */
    @ApiModelProperty("用户原始输入")
    private String original;

    /** 纠错后实际用于检索的词 */
    @ApiModelProperty("纠错后检索词")
    private String corrected;

    /** 纠错类型：PINYIN 拼音错拼 / SHAPE 形近词 */
    @ApiModelProperty("纠错类型")
    private String type;

    public SearchCorrectionVO() {
    }

    public SearchCorrectionVO(String original, String corrected, String type) {
        this.original = original;
        this.corrected = corrected;
        this.type = type;
    }
}
