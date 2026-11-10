package com.example.search.service;

import java.util.List;

/**
 * 搜索埋点服务（独立于增强搜索主流程，供订单模块做成交归因）
 *
 * @author ZCode
 * @date 2026/11/11
 */
public interface SearchLogService {

    /**
     * 成交归因：支付成功后把「点击过这些商品」的近期埋点标记为已成交
     *
     * @param productIds 本次成交的商品 ID 列表
     */
    void markDeals(List<Long> productIds);
}
