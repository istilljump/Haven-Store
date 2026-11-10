package com.example.search.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 搜索运行时统计 Mapper
 * <p>
 * 「本校热搜词」与「成交热度」都不落词库表，直接对 search_log / 成交明细
 * 做实时聚合——种子成交只有几十条量级，单查询毫秒级，
 * 换来「每笔成交立即反映到补全标签」的飞轮效果与零维护成本
 *
 * @author ZCode
 * @date 2026/11/11
 */
@Mapper
public interface SearchStatMapper {

    /**
     * 本校热门搜索词：近 30 天、有结果、长度 2~20 的搜索词按次数倒序
     *
     * @param campusId 校区 ID
     * @param limit    返回条数
     * @return [{keyword, cnt}]
     */
    @Select("SELECT keyword, COUNT(*) AS cnt FROM search_log "
            + "WHERE campus_id = #{campusId} AND result_count > 0 "
            + "AND CHAR_LENGTH(keyword) BETWEEN 2 AND 20 "
            + "AND create_time >= DATE_SUB(NOW(), INTERVAL 30 DAY) "
            + "GROUP BY keyword ORDER BY cnt DESC, keyword ASC LIMIT #{limit}")
    List<Map<String, Object>> selectCampusHotWords(@Param("campusId") Long campusId,
                                                   @Param("limit") int limit);

    /**
     * 词的成交热度：近 90 天已支付/已完成订单中，商品标题包含该词的成交件数
     *
     * @param word     词
     * @param campusId 校区 ID（可空：空则统计全平台）
     * @return 成交件数
     */
    @Select("<script>SELECT COUNT(*) FROM order_item oi "
            + "INNER JOIN product_order po ON po.id = oi.order_id "
            + "INNER JOIN product p ON p.id = oi.product_id "
            + "WHERE po.status IN (2, 4) "
            + "AND po.pay_time IS NOT NULL "
            + "AND po.pay_time &gt;= DATE_SUB(NOW(), INTERVAL 90 DAY) "
            + "AND p.title LIKE CONCAT('%', #{word}, '%') "
            + "<if test='campusId != null'>AND p.campus_id = #{campusId}</if>"
            + "</script>")
    int countDealByWord(@Param("word") String word, @Param("campusId") Long campusId);
}
