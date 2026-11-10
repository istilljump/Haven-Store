package com.example.search.service;

import com.example.search.vo.SearchCorrectionVO;
import com.example.search.vo.SearchLogVO;
import com.example.search.vo.SearchPageVO;
import com.example.search.vo.SearchSuggestResultVO;

import java.util.List;

/**
 * 增强搜索服务
 * <p>
 * 「校园闲置增强搜索」的对外能力：实时补全（词库/拼音/本校热搜 + 成交热提示）、
 * 本地词表纠错、结果内嵌行情标签、搜索埋点与「点击 → 成交」归因。
 * 全部基于本地词表与平台自身数据，不调用任何外部接口
 *
 * @author ZCode
 * @date 2026/11/11
 */
public interface SearchService {

    /**
     * 搜索补全：词库前缀 > 拼音前缀 > 本校热搜，候选词附带成交热提示与纠错建议
     *
     * @param q        输入串（可空）
     * @param campusId 校区上下文（可空，影响热搜来源与成交提示文案）
     * @param limit    候选上限
     * @return 补全响应
     */
    SearchSuggestResultVO suggest(String q, Long campusId, int limit);

    /**
     * 增强搜索：纠错 → 复用商品搜索分页 → 结果附加行情标签 → 写埋点
     *
     * @param keyword          原始关键词
     * @param campusId         校区筛选（可空）
     * @param productCondition 成色二次筛选（仅后台开关开启时生效，可为空）
     * @param categoryId       分类筛选（可空）
     * @param minPrice         价格下限（可空）
     * @param maxPrice         价格上限（可空）
     * @param sort             排序方式（可空：latest/priceAsc/priceDesc）
     * @param page             页码
     * @param pageSize         每页条数
     * @return 纠错信息 + 分页 + 埋点 ID
     */
    SearchPageVO search(String keyword, Long campusId, String productCondition, Integer categoryId,
                        java.math.BigDecimal minPrice, java.math.BigDecimal maxPrice, String sort,
                        Integer page, Integer pageSize);

    /**
     * 点击信标：把用户点中的结果商品回填到对应埋点（fire-and-forget，失败静默）
     *
     * @param logId     埋点 ID
     * @param productId 点击的商品 ID
     */
    void recordClick(Long logId, Long productId);

    /**
     * 成交归因：支付成功后把「点击过这些商品」的近期埋点标记为已成交
     *
     * @param productIds 本次成交的商品 ID 列表
     */
    void markDeals(List<Long> productIds);

    /**
     * 近期埋点查询（演示/验证用，按关键词倒序）
     *
     * @param keyword 关键词（可空）
     * @param limit   条数上限
     * @return 埋点列表
     */
    List<SearchLogVO> recentLogs(String keyword, int limit);

    /**
     * 纠错查询（供补全与搜索共用）
     *
     * @param keyword 原始输入
     * @return 纠错结果；未命中返回 null
     */
    SearchCorrectionVO findCorrection(String keyword);
}
