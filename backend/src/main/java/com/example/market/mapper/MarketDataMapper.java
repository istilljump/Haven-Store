package com.example.market.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

/**
 * 成交行情数据访问层
 * <p>
 * 行情 = 平台自己的历史成交：order_item 保存的是下单那一刻的价格快照，
 * 联 product_order（仅统计已支付/已完成，待支付与已取消不算成交）、
 * 联 product 取分类与成色。商品被物理删除后其历史成交不再进入行情样本
 * （join 不命中），属预期行为
 *
 * @author ZCode
 * @date 2026/11/10
 */
@Mapper
public interface MarketDataMapper {

    /**
     * 查询「分类 + 成色」在统计窗口内的成交价列表（升序返回）
     * <p>
     * 样本量级为几十条，直接取回内存计算中位数/分位数，
     * 避免 MySQL 8 以下兼容的窗口函数写法，计算逻辑可单测
     *
     * @param categoryId 分类 ID
     * @param condition  成色（全新/九成新/八成新/七成新及以下）
     * @param days       统计窗口（天）
     * @return 成交价列表（升序）
     */
    @Select("SELECT oi.price FROM order_item oi "
            + "INNER JOIN product_order po ON po.id = oi.order_id "
            + "INNER JOIN product p ON p.id = oi.product_id "
            + "WHERE po.status IN (2, 4) "
            + "AND p.category_id = #{categoryId} "
            + "AND p.product_condition = #{condition} "
            + "AND po.pay_time IS NOT NULL "
            + "AND po.pay_time >= DATE_SUB(NOW(), INTERVAL #{days} DAY) "
            + "ORDER BY oi.price ASC")
    List<BigDecimal> selectDealPrices(@Param("categoryId") Integer categoryId,
                                      @Param("condition") String condition,
                                      @Param("days") int days);
}
