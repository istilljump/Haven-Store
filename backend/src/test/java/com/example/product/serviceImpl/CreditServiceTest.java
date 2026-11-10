package com.example.product.serviceImpl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.example.order.entity.OrderItem;
import com.example.order.entity.ProductOrder;
import com.example.order.mapper.OrderItemMapper;
import com.example.order.mapper.ProductOrderMapper;
import com.example.product.entity.Comment;
import com.example.product.entity.Product;
import com.example.product.mapper.CommentMapper;
import com.example.product.mapper.ProductMapper;
import com.example.product.vo.CreditVO;
import com.example.report.entity.Report;
import com.example.report.mapper.ReportMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * 卖家信用画像单元测试
 * <p>
 * 信用分公式为确定性契约：base 60 + 成交×3 +（评分-3）×10 - 有效举报×5，夹取 [0,100]。
 * Mock 统计查询，断言公式精确值
 *
 * @author ZCode
 * @date 2026/11/11
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("卖家信用画像单元测试")
class CreditServiceTest {

    @Mock
    private OrderItemMapper orderItemMapper;

    @Mock
    private ProductOrderMapper productOrderMapper;

    @Mock
    private CommentMapper commentMapper;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private ReportMapper reportMapper;

    @InjectMocks
    private CreditServiceImpl creditService;

    /**
     * 纯 Mockito 环境没有 MyBatis-Plus 的实体元数据缓存，
     * 手工注册 LambdaQueryWrapper 涉及的实体（MP 官方单测同款做法）
     */
    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Product.class);
        TableInfoHelper.initTableInfo(assistant, OrderItem.class);
        TableInfoHelper.initTableInfo(assistant, ProductOrder.class);
        TableInfoHelper.initTableInfo(assistant, Comment.class);
        TableInfoHelper.initTableInfo(assistant, Report.class);
    }

    @Test
    @DisplayName("无成交卖家返回基线 60 分（待积累）")
    void credit_baseline() {
        when(orderItemMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(productMapper.selectList(any())).thenReturn(Collections.emptyList());

        CreditVO vo = creditService.getCredit(99L);

        assertEquals(60, vo.getCreditScore());
        assertEquals("待积累", vo.getLevel());
        assertEquals(0, vo.getCompletedDeals());
        assertNull(vo.getAvgRating());
    }

    @Test
    @DisplayName("成交8件+5分好评 → 满分 100（优秀），公式夹取上界")
    void credit_fullScore() {
        stubStats(8L, 5.0, 0L);

        CreditVO vo = creditService.getCredit(2L);

        // 60 + 8*3 + (5-3)*10 = 104 → 夹取 100
        assertEquals(100, vo.getCreditScore());
        assertEquals("优秀", vo.getLevel());
        assertEquals(8, vo.getCompletedDeals());
        assertEquals(5.0, vo.getAvgRating());
    }

    @Test
    @DisplayName("无评论按 3 分中性处理：成交2件 → 66 分（良好）")
    void credit_nullRatingNeutral() {
        stubStats(2L, null, 0L);

        CreditVO vo = creditService.getCredit(3L);

        assertEquals(66, vo.getCreditScore());
        assertEquals("一般", vo.getLevel());
        assertNull(vo.getAvgRating());
    }

    @Test
    @DisplayName("有效举报扣分：成交2件+3分+1条举报 → 61 分（良好）")
    void credit_reportPenalty() {
        stubStats(2L, 3.0, 1L);

        CreditVO vo = creditService.getCredit(3L);

        // 60 + 6 + 0 - 5 = 61
        assertEquals(61, vo.getCreditScore());
        assertEquals("一般", vo.getLevel());
        assertEquals(1, vo.getValidReports());
    }

    @Test
    @DisplayName("低分评价拉低信用：成交2件+1分差评 → 46 分（待积累）")
    void credit_lowRating() {
        stubStats(2L, 1.0, 0L);

        CreditVO vo = creditService.getCredit(3L);

        // 60 + 6 + (1-3)*10 = 46
        assertEquals(46, vo.getCreditScore());
        assertEquals("待积累", vo.getLevel());
    }

    /**
     * 快捷桩：订单明细 1 条（订单已支付）、卖家商品 1 件、
     * 评论均分与举报数按入参返回
     */
    private void stubStats(Long deals, Double avgRating, Long reports) {
        OrderItem item = new OrderItem();
        item.setOrderId(100L);
        lenient().when(orderItemMapper.selectList(any()))
                .thenReturn(deals > 0 ? Arrays.asList(item) : Collections.emptyList());
        lenient().when(productOrderMapper.selectCount(any())).thenReturn(deals);
        lenient().when(productMapper.selectList(any()))
                .thenReturn(Collections.singletonList(new Product()));
        lenient().when(reportMapper.selectCount(any())).thenReturn(reports);
        if (avgRating == null) {
            Map<String, Object> row = new HashMap<>();
            row.put("avg_rating", 0);
            lenient().when(commentMapper.selectMaps(any())).thenReturn(Arrays.asList(row));
        } else {
            Map<String, Object> row = new HashMap<>();
            row.put("avg_rating", avgRating);
            lenient().when(commentMapper.selectMaps(any())).thenReturn(Arrays.asList(row));
        }
    }
}
