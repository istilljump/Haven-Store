package com.example.product.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.order.entity.OrderItem;
import com.example.order.entity.ProductOrder;
import com.example.order.enums.OrderStatusEnum;
import com.example.order.mapper.OrderItemMapper;
import com.example.order.mapper.ProductOrderMapper;
import com.example.product.entity.Comment;
import com.example.product.entity.Product;
import com.example.product.mapper.CommentMapper;
import com.example.product.mapper.ProductMapper;
import com.example.product.service.CreditService;
import com.example.product.vo.CreditVO;
import com.example.report.entity.Report;
import com.example.report.mapper.ReportMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 卖家信用画像实现类（轮4：可信交易轻量层）
 * <p>
 * 信用分公式（确定性，同数据同结果）：
 * base 60 + 成交件数×3 +（评价均分-3）×10 - 有效举报×5，夹取 [0,100]；
 * 无成交返回基线 60 分。全部实时统计平台自身数据，随交易即时变化——
 * 与行情同源的飞轮逻辑，可解释、可断言，不引入任何外部信用服务
 *
 * @author ZCode
 * @date 2026/11/11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreditServiceImpl implements CreditService {

    /** 信用基线分（无成交卖家） */
    private static final int CREDIT_BASE = 60;

    /** 每笔成交加分 */
    private static final int DEAL_SCORE = 3;

    /** 评分高于/低于 3 分时每分增减分 */
    private static final int RATING_SCORE = 10;

    /** 每条有效举报扣分 */
    private static final int REPORT_PENALTY = 5;

    /** 订单明细数据访问对象（成交件数统计） */
    private final OrderItemMapper orderItemMapper;

    /** 订单数据访问对象（订单状态过滤） */
    private final ProductOrderMapper productOrderMapper;

    /** 评论数据访问对象（评价均分） */
    private final CommentMapper commentMapper;

    /** 商品数据访问对象（卖家商品集合） */
    private final ProductMapper productMapper;

    /** 举报数据访问对象（有效举报扣分） */
    private final ReportMapper reportMapper;

    @Override
    public CreditVO getCredit(Long userId) {
        CreditVO vo = new CreditVO();
        // 1. 近 90 天成交件数：order_item 快照 join 已支付/已完成订单
        int deals = countDeals(userId);
        vo.setCompletedDeals(deals);
        // 2. 评价均分：status=1 的正常评论
        Double avgRating = avgRating(userId);
        vo.setAvgRating(avgRating);
        // 3. 有效举报：已处理状态的商品举报
        int reports = validReports(userId);
        vo.setValidReports(reports);
        // 4. 确定性公式：夹取 [0,100]；无成交卖家等级固定「待积累」
        vo.setCreditScore(creditScore(deals, avgRating, reports));
        vo.setLevel(deals <= 0 ? "待积累" : levelOf(vo.getCreditScore()));
        return vo;
    }

    /**
     * 近 90 天成交件数（order_item 快照，join 已支付/已完成订单）
     */
    private int countDeals(Long userId) {
        List<Integer> paidStatuses = Arrays.asList(
                OrderStatusEnum.PAID.getCode(), OrderStatusEnum.FINISHED.getCode());
        List<Long> orderIds = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                        .eq(OrderItem::getSellerId, userId))
                .stream().map(OrderItem::getOrderId).distinct().collect(Collectors.toList());
        if (orderIds.isEmpty()) {
            return 0;
        }
        Long count = productOrderMapper.selectCount(new LambdaQueryWrapper<ProductOrder>()
                .in(ProductOrder::getId, orderIds)
                .in(ProductOrder::getStatus, paidStatuses)
                .apply("pay_time >= DATE_SUB(NOW(), INTERVAL 90 DAY)"));
        return count == null ? 0 : count.intValue();
    }

    /**
     * 卖家全部商品的正常评论均分（无评论返回 null，公式按 3 分处理）
     */
    private Double avgRating(Long userId) {
        QueryWrapper<Comment> wrapper = new QueryWrapper<Comment>()
                .select("IFNULL(AVG(rating), 0) AS avg_rating")
                .eq("status", 1)
                .inSql("product_id", "SELECT id FROM product WHERE user_id = " + userId);
        List<Map<String, Object>> rows = commentMapper.selectMaps(wrapper);
        if (rows.isEmpty() || rows.get(0) == null) {
            return null;
        }
        Object value = rows.get(0).get("avg_rating");
        if (!(value instanceof Number) || ((Number) value).doubleValue() <= 0) {
            return null;
        }
        return Math.round(((Number) value).doubleValue() * 10) / 10.0;
    }

    /**
     * 有效举报：target_type=product 且 target_id 属于该卖家商品、已处理状态
     */
    private int validReports(Long userId) {
        List<Long> productIds = productMapper.selectList(new LambdaQueryWrapper<Product>()
                        .select(Product::getId)
                        .eq(Product::getUserId, userId))
                .stream().map(Product::getId).collect(Collectors.toList());
        if (productIds.isEmpty()) {
            return 0;
        }
        Long count = reportMapper.selectCount(new LambdaQueryWrapper<Report>()
                .eq(Report::getTargetType, "product")
                .in(Report::getTargetId, productIds)
                .eq(Report::getStatus, 1));
        return count == null ? 0 : count.intValue();
    }

    /**
     * 信用分公式：确定性、可断言
     */
    private int creditScore(int deals, Double avgRating, int reports) {
        if (deals <= 0) {
            return CREDIT_BASE;
        }
        double rating = avgRating == null ? 3.0 : avgRating;
        int score = CREDIT_BASE + deals * DEAL_SCORE + (int) Math.round((rating - 3.0) * RATING_SCORE)
                - reports * REPORT_PENALTY;
        return Math.max(0, Math.min(100, score));
    }

    /**
     * 信用等级文案
     */
    private String levelOf(int score) {
        if (score >= 85) {
            return "优秀";
        }
        if (score >= 70) {
            return "良好";
        }
        if (score >= 50) {
            return "一般";
        }
        return "待积累";
    }
}
