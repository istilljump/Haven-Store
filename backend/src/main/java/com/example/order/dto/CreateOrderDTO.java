package com.example.order.dto;

import com.example.order.constant.OrderConstant;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 创建订单入参
 *
 * @author ZCode
 * @date 2026/09/25
 */
@Data
public class CreateOrderDTO {

    /** 参与结算的商品 ID 列表（来自购物车勾选项） */
    @NotEmpty(message = "请选择要购买的商品")
    @Size(max = OrderConstant.MAX_ITEM_COUNT, message = "单笔订单最多结算 20 件商品")
    private List<Long> productIds;

    /** 买家留言（可选） */
    @Size(max = OrderConstant.REMARK_MAX_LENGTH, message = "留言不能超过 255 个字符")
    private String remark;
}
