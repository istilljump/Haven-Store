package com.example.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.order.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单明细数据访问对象
 *
 * @author ZCode
 * @date 2026/09/25
 */
@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {
}
