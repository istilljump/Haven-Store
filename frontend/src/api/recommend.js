import request from '@/utils/request'

/**
 * 场景化推荐 API
 * <p>
 * 三个推荐位置共用 RecommendItemVO（每条强制携带可解释 reason）；
 * 后台「交易设置」的推荐开关关闭时三个接口统一返回 null，前端隐藏区块
 */

/**
 * 首页冷启动推荐（R1 校区成交活跃 → R2 本校高频品类 → 最新闲置兜底）
 * @param {Object} params - {campusId?}
 * @returns {Promise} 返回推荐列表；开关关闭时为 null
 */
export function getHomeRecommend(params) {
  return request.get('/recommend/home', params)
}

/**
 * 搜索后互补闲置推荐（搜自行车 → 车锁/头盔）
 * @param {Object} params - {keyword, campusId?, excludeIds?}
 * @returns {Promise} 返回互补推荐列表；字典未命中返回空数组
 */
export function getComplementaryRecommend(params) {
  return request.get('/recommend/complementary', params)
}

/**
 * 商品详情页推荐（R4 同校同款 → R5 互补）
 * @param {number} productId - 当前商品 ID
 * @returns {Promise} 返回推荐列表；开关关闭时为 null
 */
export function getProductRecommend(productId) {
  return request.get(`/recommend/for-product/${productId}`)
}

export default {
  getHomeRecommend,
  getComplementaryRecommend,
  getProductRecommend
}
