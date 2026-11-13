package com.example.order.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.order.dto.CreateOrderDTO;
import com.example.order.vo.OrderVO;

/**
 * 订单模块业务逻辑接口
 * <p>
 * 订单生命周期：待支付 → 已支付 → 已完成；待支付可取消。
 * 下单即锁定商品（置为已售出），取消时释放回在售，避免同一件二手商品被多人买走
 *
 * @author ZCode
 * @date 2026/09/25
 */
public interface OrderService {

    /**
     * 用购物车中勾选的商品创建订单（下单即锁定商品，并从购物车移除）
     *
     * @param dto 下单入参
     * @return 新建的订单
     */
    OrderVO createOrder(CreateOrderDTO dto);

    /**
     * 查询我买到的订单
     *
     * @param status   订单状态（为空表示全部）
     * @param page     页码
     * @param pageSize 每页条数
     * @return 分页订单
     */
    Page<OrderVO> listMyOrders(Integer status, Integer page, Integer pageSize);

    /**
     * 查询包含我发布的商品的订单（卖家视角）
     *
     * @param page     页码
     * @param pageSize 每页条数
     * @return 分页订单
     */
    Page<OrderVO> listSoldOrders(Integer page, Integer pageSize);

    /**
     * 查询订单详情
     * <p>
     * 买家本人或订单内商品的卖家（含卖家本人）可见
     *
     * @param orderNo 订单号
     * @return 订单详情
     */
    OrderVO getOrderDetail(String orderNo);

    /**
     * 支付订单
     * <p>
     * 本项目未接入任何真实支付渠道，此处仅做订单状态流转，不产生真实扣款
     *
     * @param orderNo 订单号
     */
    void pay(String orderNo);

    /**
     * 取消订单（仅待支付可取消，商品回到在售）
     *
     * @param orderNo 订单号
     */
    void cancel(String orderNo);

    /**
     * 确认完成（买家确认收货）
     *
     * @param orderNo 订单号
     */
    void confirmFinish(String orderNo);

    /**
     * 卖家输码面交确认（轮8）：卖家输入买家出示的 6 位面交码完成交付
     *
     * @param orderNo 订单号
     * @param code    6 位数字确认码
     */
    void confirmByCode(String orderNo, String code);

    /**
     * 按议价价创建单商品订单（轮8：买家接受私信报价后调用）
     * <p>
     * 与购物车下单同一套校验与锁定语义（不能买自己的、商品在售、条件更新 CAS 锁定），
     * 差异仅在于成交价使用议价价——order_item 快照价即议价价，自然回流行情样本。
     *
     * @param productId       商品 ID
     * @param buyerId         买家 ID（报价接收人）
     * @param negotiatedPrice 议价成交价
     * @return 订单 VO
     */
    OrderVO createFromOffer(Long productId, Long buyerId, java.math.BigDecimal negotiatedPrice);

    /**
     * 取消超时未支付的订单并释放商品（供定时任务调用，无登录态）
     *
     * @param timeoutMinutes 超时分钟数（下单后超过该时长仍未支付即取消）
     * @return 本次取消的订单数
     */
    int cancelTimeoutOrders(int timeoutMinutes);

    /**
     * 支付后超时自动完成兜底（轮8，供定时任务调用，无登录态）
     *
     * @param autoFinishDays 自动完成天数（支付后超过该天数未确认即完成）
     * @return 本次自动完成的订单数
     */
    int autoFinishPaidOrders(int autoFinishDays);
}
