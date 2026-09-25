package com.example.order.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.Result;
import com.example.order.dto.CreateOrderDTO;
import com.example.order.service.OrderService;
import com.example.order.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单模块控制器
 * <p>
 * 完整请求路径为上下文路径 + 模块前缀，例如创建订单：POST /api/order/create。
 * 所有接口均需登录，订单归属取自登录态，操作他人订单会被拒绝
 *
 * @author ZCode
 * @date 2026/09/25
 */
@Api(tags = "订单模块接口")
@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    /** 订单模块业务逻辑对象 */
    private final OrderService orderService;

    /**
     * 创建订单（用购物车勾选的商品结算）
     *
     * @param dto 下单入参
     * @return 新建的订单
     */
    @ApiOperation(value = "创建订单",
            notes = "需要登录；下单即锁定商品并从购物车移除，同一商品不会被两人买走")
    @PostMapping("/create")
    public Result<OrderVO> createOrder(@RequestBody @Validated CreateOrderDTO dto) {
        return Result.success(orderService.createOrder(dto));
    }

    /**
     * 我的订单（买家视角）
     *
     * @param status   订单状态（1 待支付，2 已支付，3 已取消，4 已完成；为空表示全部）
     * @param page     页码
     * @param pageSize 每页条数
     * @return 分页订单
     */
    @ApiOperation(value = "我的订单", notes = "需要登录；按下单时间倒序")
    @GetMapping("/list")
    public Result<Page<OrderVO>> listMyOrders(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(orderService.listMyOrders(status, page, pageSize));
    }

    /**
     * 我卖出的订单（卖家视角）
     *
     * @param page     页码
     * @param pageSize 每页条数
     * @return 分页订单
     */
    @ApiOperation(value = "我卖出的", notes = "需要登录；返回包含我发布的商品的订单，供卖家了解哪些已被买走")
    @GetMapping("/sold")
    public Result<Page<OrderVO>> listSoldOrders(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(orderService.listSoldOrders(page, pageSize));
    }

    /**
     * 订单详情
     *
     * @param orderNo 订单号
     * @return 订单详情
     */
    @ApiOperation(value = "订单详情", notes = "需要登录；仅买家本人可见")
    @GetMapping("/{orderNo}")
    public Result<OrderVO> getOrderDetail(@PathVariable String orderNo) {
        return Result.success(orderService.getOrderDetail(orderNo));
    }

    /**
     * 支付订单
     *
     * @param orderNo 订单号
     * @return 操作结果
     */
    @ApiOperation(value = "支付订单（模拟）",
            notes = "需要登录；本项目未接入真实支付渠道，仅做订单状态流转，不产生真实扣款")
    @PostMapping("/{orderNo}/pay")
    public Result<Void> pay(@PathVariable String orderNo) {
        orderService.pay(orderNo);
        return Result.success();
    }

    /**
     * 取消订单
     *
     * @param orderNo 订单号
     * @return 操作结果
     */
    @ApiOperation(value = "取消订单", notes = "需要登录；仅待支付可取消，取消后商品回到在售")
    @PostMapping("/{orderNo}/cancel")
    public Result<Void> cancel(@PathVariable String orderNo) {
        orderService.cancel(orderNo);
        return Result.success();
    }

    /**
     * 确认完成（确认收货）
     *
     * @param orderNo 订单号
     * @return 操作结果
     */
    @ApiOperation(value = "确认完成", notes = "需要登录；仅已支付的订单可确认，确认后订单结束")
    @PostMapping("/{orderNo}/confirm")
    public Result<Void> confirmFinish(@PathVariable String orderNo) {
        orderService.confirmFinish(orderNo);
        return Result.success();
    }
}
