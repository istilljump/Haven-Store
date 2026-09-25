/**
 * 购物车API接口
 * 购物车与收藏共用后端的用户-商品关联表，两者的互转由这里的方法完成
 */

import request from '@/utils/request'

/**
 * 加入购物车
 * @param {number} productId - 商品ID
 * @returns {Promise} 返回 true 表示新加入，false 表示原本就在购物车中
 */
export function addToCart(productId) {
  return request.post(`/cart/${productId}`)
}

/**
 * 移出购物车
 * @param {number} productId - 商品ID
 * @returns {Promise} 返回操作结果
 */
export function removeFromCart(productId) {
  return request.delete(`/cart/${productId}`)
}

/**
 * 获取购物车列表
 * @returns {Promise} 返回购物车条目（含是否可结算与不可结算原因）
 */
export function getCartList() {
  return request.get('/cart/list')
}

/**
 * 获取购物车件数（头部角标）
 * @returns {Promise} 返回件数
 */
export function getCartCount() {
  return request.get('/cart/count')
}

/**
 * 把购物车中的商品移入收藏
 * @param {number} productId - 商品ID
 * @returns {Promise} 返回操作结果
 */
export function moveToFavorite(productId) {
  return request.post(`/cart/${productId}/move-to-favorite`)
}

/**
 * 把收藏里的商品批量加入购物车
 * @returns {Promise} 返回 {added, skipped}
 */
export function addAllFavoritesToCart() {
  return request.post('/cart/favorites/add-all')
}

/**
 * 默认导出：便于以 cartApi.xxx() 的形式统一调用
 */
export default {
  addToCart,
  removeFromCart,
  getCartList,
  getCartCount,
  moveToFavorite,
  addAllFavoritesToCart
}
