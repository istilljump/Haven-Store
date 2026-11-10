package com.example.recommend.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 推荐运行时统计 Mapper
 * <p>
 * 校区品类成交活跃度与「搜索词 → 品类」解析，全部实时聚合平台自身数据，
 * 不做离线维护——成交一发生，推荐排序即刻反映
 *
 * @author ZCode
 * @date 2026/11/11
 */
@Mapper
public interface RecommendStatMapper {

    /**
     * 校区内近 90 天成交件数最多的品类（R1/R2 依据）
     *
     * @param campusId 校区 ID
     * @param limit    品类数上限
     * @return [{categoryId, cnt}]，件数倒序
     */
    @Select("SELECT p.category_id AS categoryId, COUNT(*) AS cnt "
            + "FROM order_item oi "
            + "INNER JOIN product_order po ON po.id = oi.order_id "
            + "INNER JOIN product p ON p.id = oi.product_id "
            + "WHERE po.status IN (2, 4) "
            + "AND po.pay_time IS NOT NULL "
            + "AND po.pay_time >= DATE_SUB(NOW(), INTERVAL 90 DAY) "
            + "AND p.campus_id = #{campusId} "
            + "GROUP BY p.category_id ORDER BY cnt DESC, p.category_id ASC LIMIT #{limit}")
    List<Map<String, Object>> selectCampusCategoryDealCounts(@Param("campusId") Long campusId,
                                                             @Param("limit") int limit);

    /**
     * 「搜索词 → 品类」解析第一优先级：在售商品标题/描述命中的主流品类
     *
     * @param keyword 搜索词
     * @return 品类 ID；无命中返回 null
     */
    @Select("SELECT p.category_id FROM product p "
            + "WHERE p.status = 1 "
            + "AND (p.title LIKE CONCAT('%', #{keyword}, '%') OR p.description LIKE CONCAT('%', #{keyword}, '%')) "
            + "GROUP BY p.category_id ORDER BY COUNT(*) DESC, p.category_id ASC LIMIT 1")
    Integer resolveCategoryByProducts(@Param("keyword") String keyword);

    /**
     * 「搜索词 → 品类」解析兜底：补全词库的品牌词标签（如 自行车→运动户外）
     *
     * @param word 搜索词
     * @return 品类 ID；无命中返回 null
     */
    @Select("SELECT c.id FROM category c "
            + "INNER JOIN search_suggest_word w ON w.tag_label = c.name "
            + "WHERE w.word = #{word} AND w.status = 1 AND c.status = 1 LIMIT 1")
    Integer resolveCategoryBySuggestWord(@Param("word") String word);
}
