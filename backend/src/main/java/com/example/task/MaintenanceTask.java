package com.example.task;

import com.example.admin.dto.SystemSettingDTO;
import com.example.admin.service.SystemSettingService;
import com.example.order.service.OrderService;
import com.example.product.entity.Product;
import com.example.product.enums.ProductStatusEnum;
import com.example.product.mapper.ProductMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 平台维护定时任务
 * <p>
 * 两个职责：
 * 1. 超时未支付订单自动取消（固定 30 分钟），并释放被锁定的商品；
 * 2. 按管理后台「交易设置」的 autoOfflineHours 自动下架超期在售商品（0 表示关闭）。
 * <p>
 * 说明：任务全部使用条件更新与幂等逻辑，与用户手上的并发操作（支付、下架）互不冲突
 *
 * @author ZCode
 * @date 2026/10/05
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MaintenanceTask {

    /** 超时未支付订单的取消时限（分钟） */
    private static final int ORDER_TIMEOUT_MINUTES = 30;

    /** 固定轮询间隔：10 分钟（毫秒） */
    private static final long FIXED_DELAY_MS = 10 * 60 * 1000L;

    /** 启动延迟：1 分钟，等依赖服务就绪 */
    private static final long INITIAL_DELAY_MS = 60 * 1000L;

    private final OrderService orderService;

    private final ProductMapper productMapper;

    private final SystemSettingService systemSettingService;

    /**
     * 超时未支付订单自动取消
     */
    @Scheduled(fixedDelay = FIXED_DELAY_MS, initialDelay = INITIAL_DELAY_MS)
    public void cancelTimeoutOrders() {
        try {
            int cancelled = orderService.cancelTimeoutOrders(ORDER_TIMEOUT_MINUTES);
            if (cancelled > 0) {
                log.info("定时任务：自动取消超时未支付订单 {} 笔（超时 {} 分钟）", cancelled, ORDER_TIMEOUT_MINUTES);
            }
        } catch (Exception e) {
            log.error("定时任务：自动取消超时订单执行失败", e);
        }
    }

    /**
     * 超期在售商品自动下架
     * <p>
     * autoOfflineHours = 0 时该能力关闭，直接跳过
     */
    @Scheduled(fixedDelay = FIXED_DELAY_MS, initialDelay = INITIAL_DELAY_MS)
    public void autoOfflineExpiredProducts() {
        try {
            SystemSettingDTO settings = systemSettingService.get();
            Integer hours = settings.getTrade() == null ? null : settings.getTrade().getAutoOfflineHours();
            if (hours == null || hours <= 0) {
                return;
            }
            LocalDateTime deadline = LocalDateTime.now().minusHours(hours);
            int changed = productMapper.update(null, new LambdaUpdateWrapper<Product>()
                    .eq(Product::getStatus, ProductStatusEnum.ON_SHELF.getCode())
                    .lt(Product::getCreateTime, deadline)
                    .set(Product::getStatus, ProductStatusEnum.OFF_SHELF.getCode()));
            if (changed > 0) {
                log.info("定时任务：{} 件发布超过 {} 小时的在售商品已自动下架", changed, hours);
            }
        } catch (Exception e) {
            log.error("定时任务：商品自动下架执行失败", e);
        }
    }
}
