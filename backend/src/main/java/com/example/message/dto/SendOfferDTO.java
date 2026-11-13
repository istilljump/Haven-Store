package com.example.message.dto;

import lombok.Data;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 卖家发送议价报价入参（轮8 业务迭代新增）
 *
 * @author ZCode
 * @date 2026/11
 */
@Data
public class SendOfferDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 报价接收人（该商品会话的买家） */
    @NotNull(message = "接收人不能为空")
    private Long toUserId;

    /** 报价关联商品 */
    @NotNull(message = "商品不能为空")
    private Long productId;

    /** 报价金额：须大于 0 且低于商品现价（价格让利才构成议价） */
    @NotNull(message = "报价金额不能为空")
    @DecimalMin(value = "0.01", message = "报价金额必须大于 0")
    private BigDecimal price;
}
