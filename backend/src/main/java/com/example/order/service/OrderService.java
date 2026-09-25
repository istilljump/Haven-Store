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
     * 查询订单详情（仅买家本人可见）
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
}
