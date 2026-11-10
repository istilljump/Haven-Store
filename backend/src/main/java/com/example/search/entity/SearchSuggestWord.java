package com.example.search.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 搜索补全词库（对应数据库 search_suggest_word 表）
 * <p>
 * 静态可运营字典：品类词 + 拼音（全拼/首字母），由种子脚本维护；
 * 成交热度与本校热度不做离线维护，全部运行时统计（见 SearchStatMapper），
 * 每笔成交自动反映到补全权重与「近期成交 N 件」标签——数据飞轮零成本接入
 *
 * @author ZCode
 * @date 2026/11/11
 */
@Data
@TableName("search_suggest_word")
public class SearchSuggestWord implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 词 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 词本身 */
    private String word;

    /** 词类型：CATEGORY 品类词 */
    private String wordType;

    /** 小字标签（所属品类名，如【教材】【数码】） */
    private String tagLabel;

    /** 拼音（全拼+首字母，如 jiaocai,jc），供拼音补全 */
    private String pinyin;

    /** 状态：1 启用，0 停用 */
    private Integer status;
}
