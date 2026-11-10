package com.example.search.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 搜索埋点（对应数据库 search_log 表）
 * <p>
 * 只记录词与 ID（user_id 可空游客），不存任何敏感字段；明细不对外暴露，
 * 仅供本校热搜聚合与「点击 → 成交」归因统计（推荐模块的数据原料）
 *
 * @author ZCode
 * @date 2026/11/11
 */
@Data
@TableName("search_log")
public class SearchLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 埋点 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 搜索用户 ID（游客为空） */
    private Long userId;

    /** 原始搜索词 */
    private String keyword;

    /** 纠错后实际用于检索的词（未纠错为空） */
    private String correctedKeyword;

    /** 搜索时所选校区（可空） */
    private Long campusId;

    /** 本次搜索结果条数（热搜聚合时过滤 0 结果词） */
    private Integer resultCount;

    /** 用户点击的结果商品（可空，点击信标回填） */
    private Long clickedProductId;

    /** 是否成交：0 否 / 1 是（支付成功后按点击商品回写） */
    private Integer dealFlag;

    /** 搜索时间 */
    @JsonIgnore
    private LocalDateTime createTime;
}
