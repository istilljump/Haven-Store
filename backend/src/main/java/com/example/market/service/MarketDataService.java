package com.example.market.service;

import com.example.market.vo.MarketBasisVO;
import com.example.market.vo.MarketCategoryStatVO;
import com.example.market.vo.MarketDealStat;

import java.math.BigDecimal;
import java.util.List;

/**
 * 成交行情服务
 * <p>
 * 「数据驱动定价」的数据底座：把平台历史成交聚合成可解释的行情基线，
 * 供 AI 估价（MARKET_DATA 路径）与买家侧划算指数（价格分位）共用。
 * 样本不足时返回 null，由调用方决定降级策略（回规则基线 / 不展示）
 *
 * @author ZCode
 * @date 2026/11/10
 */
public interface MarketDataService {

    /**
     * 查询「分类 + 成色」的成交行情基线
     *
     * @param categoryId 分类 ID
     * @param condition  成色
     * @return 行情基线；窗口内无成交时返回 null
     */
    MarketBasisVO getBasis(Integer categoryId, String condition);

    /**
     * 计算价格分位：「低于该价格」的成交占比（0-100）
     * <p>
     * 语义：返回 X 表示「该价格低于近期 X% 的同类成交价」，X 越大越划算
     *
     * @param categoryId 分类 ID
     * @param condition  成色
     * @param price      待比较的价格
     * @return 0-100 的整数；窗口内无成交时返回 null
     */
    Integer cheaperThanPercent(Integer categoryId, String condition, BigDecimal price);

    /**
     * 批量取多个品类的成交明细（品类 × 成色 × 价格）
     * <p>
     * 供增强搜索整页商品一次性计算行情标签，避免逐商品 N+1
     *
     * @param categoryIds 品类 ID 集合（空集合返回空列表）
     * @return 明细行列表
     */
    List<MarketDealStat> getDealStats(List<Integer> categoryIds);

    /**
     * 校区品类行情 TOP N：近 90 天成交件数最多的品类及其中位价
     * <p>
     * 首页「本校行情速览」卡片的数据源，成交件数并列时按品类 ID 升序
     *
     * @param campusId 校区 ID
     * @param topN     取前 N 个品类
     * @return 品类行情列表（按成交件数倒序）
     */
    List<MarketCategoryStatVO> getCampusTopCategories(Long campusId, int topN);
}
