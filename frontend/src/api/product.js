/**
 * 商品相关API接口
 * 包含商品列表、发布商品、附近查询、智能估价等功能
 */

import request from '@/utils/request'

/**
 * 发布商品
 * @param {Object} productData - 商品信息
 * @param {string} productData.title - 商品标题
 * @param {number} productData.categoryId - 商品分类ID
 * @param {string} productData.description - 商品描述
 * @param {number} productData.price - 商品价格
 * @param {string} productData.productCondition - 商品成色
 * @param {string} productData.tradeType - 交易方式
 * @param {string} productData.address - 交易地址（线下交易必需）
 * @param {number} productData.longitude - 经度
 * @param {number} productData.latitude - 纬度
 * @returns {Promise} 返回发布结果
 */
export function addProduct(productData) {
  return request.post('/product/add', productData)
}

/**
 * 获取附近商品列表
 * @param {Object} queryParams - 查询参数
 * @param {number} queryParams.longitude - 经度
 * @param {number} queryParams.latitude - 纬度
 * @param {number} queryParams.radius - 查询半径（公里）
 * @param {number} queryParams.page - 页码
 * @param {number} queryParams.pageSize - 每页条数
 * @returns {Promise} 返回附近商品列表
 */
export function getNearbyProducts(queryParams) {
  return request.post('/product/nearby', queryParams)
}

/**
 * AI智能估价
 * @param {Object} estimateParams - 估价参数
 * @param {string} estimateParams.title - 商品标题
 * @param {string} estimateParams.productCondition - 商品成色
 * @param {number} estimateParams.categoryId - 商品分类ID
 * @returns {Promise} 返回估价结果
 */
export function estimateProductPrice(estimateParams) {
  return request.post('/product/estimate', estimateParams)
}

/**
 * 获取商品分类列表
 * @returns {Promise} 返回商品分类列表
 */
export function getProductCategories() {
  return request.get('/product/categories')
}

/**
 * 获取商品详情
 * @param {number} productId - 商品ID
 * @returns {Promise} 返回商品详情
 */
export function getProductDetail(productId) {
  return request.get(`/product/detail/${productId}`)
}

/**
 * 搜索商品
 * @param {Object} searchParams - 搜索参数
 * @param {string} searchParams.keyword - 搜索关键词
 * @param {number} searchParams.categoryId - 分类ID
 * @param {number} searchParams.page - 页码
 * @param {number} searchParams.pageSize - 每页条数
 * @param {string} searchParams.sort - 排序方式
 * @returns {Promise} 返回搜索结果
 */
export function searchProducts(searchParams) {
  return request.get('/product/search', {
    params: searchParams
  })
}

/**
 * 更新商品信息
 * @param {number} productId - 商品ID
 * @param {Object} productData - 商品信息
 * @returns {Promise} 返回更新结果
 */
export function updateProduct(productId, productData) {
  return request.put(`/product/${productId}`, productData)
}

/**
 * 下架商品
 * @param {number} productId - 商品ID
 * @returns {Promise} 返回操作结果
 */
export function removeProduct(productId) {
  return request.delete(`/product/${productId}`)
}

/**
 * 收藏商品
 * @param {number} productId - 商品ID
 * @returns {Promise} 返回收藏结果
 */
export function favoriteProduct(productId) {
  return request.post(`/product/${productId}/favorite`)
}

/**
 * 取消收藏商品
 * @param {number} productId - 商品ID
 * @returns {Promise} 返回取消收藏结果
 */
export function unfavoriteProduct(productId) {
  return request.delete(`/product/${productId}/favorite`)
}

/**
 * 获取收藏的商品列表
 * @param {Object} queryParams - 查询参数
 * @param {number} queryParams.page - 页码
 * @param {number} queryParams.pageSize - 每页条数
 * @returns {Promise} 返回收藏商品列表
 */
export function getFavoriteProducts(queryParams) {
  return request.get('/product/favorites', {
    params: queryParams
  })
}

/**
 * 默认导出：便于以 productApi.xxx() 的形式统一调用
 */
export default {
  addProduct,
  getNearbyProducts,
  estimateProductPrice,
  getProductCategories,
  getProductDetail,
  searchProducts,
  updateProduct,
  removeProduct,
  favoriteProduct,
  unfavoriteProduct,
  getFavoriteProducts
}