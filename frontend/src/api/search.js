import request from '@/utils/request'

/**
 * 增强搜索 API（校园闲置增强搜索模块）
 * <p>
 * 补全 / 纠错 / 行情标签 / 埋点。原 /product/search 接口保留可用，
 * 前端统一切换到本模块的 /search/products（响应多带 correction/logId/行情标签）
 */

/**
 * 搜索实时补全
 * @param {Object} params - 查询参数
 * @param {string} params.q - 输入串
 * @param {number} [params.campusId] - 校区上下文（影响热搜与成交提示）
 * @param {number} [params.limit] - 候选上限，默认 8
 * @returns {Promise} 返回 {items:[{word, tagLabel, dealHint, source}], correction}
 */
export function getSuggest(params) {
  return request.get('/search/suggest', params)
}

/**
 * 增强搜索
 * @param {Object} params - 查询参数
 * @param {string} [params.keyword] - 关键词（会自动纠错）
 * @param {number} [params.campusId] - 校区筛选
 * @param {string} [params.productCondition] - 成色二次筛选（仅后台开关开启时生效）
 * @param {number} [params.page] - 页码
 * @param {number} [params.pageSize] - 每页条数
 * @returns {Promise} 返回 {correction, page:{records,total}, logId}
 */
export function searchProductsEnhanced(params) {
  return request.get('/search/products', params)
}

/**
 * 搜索点击信标（fire-and-forget，失败静默）
 * @param {Object} data - {logId, productId}
 * @returns {Promise}
 */
export function logSearchClick(data) {
  return request.post('/search/log/click', data)
}

/**
 * 近期埋点查询（演示/验证用）
 * @param {Object} params - {keyword?, limit?}
 * @returns {Promise} 返回埋点列表（含 dealFlag 归因结果）
 */
export function getSearchLogs(params) {
  return request.get('/search/log/list', params)
}

export default {
  getSuggest,
  searchProductsEnhanced,
  logSearchClick,
  getSearchLogs
}
