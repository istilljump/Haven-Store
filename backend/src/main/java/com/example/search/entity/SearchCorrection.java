package com.example.search.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 搜索纠错词表（对应数据库 search_correction 表）
 * <p>
 * 轻量本地词表：拼音错拼与形近词的人工种子，不调用任何外部纠错接口；
 * 只在精确/包含命中药表时才纠错——宁可不纠，不可错纠
 *
 * @author ZCode
 * @date 2026/11/11
 */
@Data
@TableName("search_correction")
public class SearchCorrection implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 纠错项 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 错误写法 */
    private String wrongWord;

    /** 正确写法（实际用于检索的词） */
    private String correctWord;

    /** 纠错类型：PINYIN 拼音错拼 / SHAPE 形近词 */
    private String type;

    /** 状态：1 启用，0 停用 */
    private Integer status;
}
