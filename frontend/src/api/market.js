import request from '@/utils/request'

/**
 * 成交行情 API
 * <p>
 * 「数据驱动定价」的前端数据源：商品详情页的行情卡与划算指数
 * 依据这里返回的成交样本统计（近 90 天同类成交的中位价/区间/样本量）
 */

/**
 * 查询「分类 + 成色」的成交行情基线
 * @param {Object} params - 查询参数
 * @param {number} params.categoryId - 分类ID
 * @param {string} params.condition - 成色（全新/九成新/八成新/七成新及以下）
 * @returns {Promise} 返回行情基线；窗口内无成交时 data 为 null
 */
export function getMarketSummary(params) {
  return request.get('/market/summary', params)
}

/**
 * 校区品类行情 TOP N（首页「本校行情速览」看板）
 * @param {Object} params - {campusId, topN?}
 * @returns {Promise} 返回 [{categoryId, categoryName, dealCount, medianPrice}]
 */
export function getCampusSummary(params) {
  return request.get('/market/campus-summary', params)
}

/**
 * 行情趋势（详情页走势图）
 * @param {Object} params - {categoryId, condition, days?}
 * @returns {Promise} 返回按日聚合趋势点 [{date, count, medianPrice}]，日期升序
 */
export function getMarketTrend(params) {
  return request.get('/market/trend', params)
}

export default {
  getMarketSummary,
  getCampusSummary,
  getMarketTrend
}
