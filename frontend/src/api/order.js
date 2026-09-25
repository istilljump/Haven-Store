/**
 * 订单API接口
 * 说明：本项目的支付为模拟支付，仅做订单状态流转，不产生任何真实扣款
 */

import request from '@/utils/request'

/**
 * 创建订单（用购物车勾选的商品结算）
 * @param {Object} orderData - 下单参数
 * @param {number[]} orderData.productIds - 参与结算的商品ID
 * @param {string} [orderData.remark] - 买家留言
 * @returns {Promise} 返回新建的订单
 */
export function createOrder(orderData) {
  return request.post('/order/create', orderData)
}

/**
 * 我的订单（买家视角）
 * @param {Object} queryParams - 查询参数
 * @param {number} [queryParams.status] - 订单状态（1待支付 2已支付 3已取消 4已完成）
 * @param {number} [queryParams.page] - 页码
 * @param {number} [queryParams.pageSize] - 每页条数
 * @returns {Promise} 返回分页订单（{records, total}）
 */
export function getMyOrders(queryParams) {
  return request.get('/order/list', queryParams)
}

/**
 * 我卖出的订单（卖家视角）
 * @param {Object} queryParams - 查询参数（page、pageSize）
 * @returns {Promise} 返回分页订单（{records, total}）
 */
export function getSoldOrders(queryParams) {
  return request.get('/order/sold', queryParams)
}

/**
 * 订单详情
 * @param {string} orderNo - 订单号
 * @returns {Promise} 返回订单详情
 */
export function getOrderDetail(orderNo) {
  return request.get(`/order/${orderNo}`)
}

/**
 * 支付订单（模拟支付）
 * @param {string} orderNo - 订单号
 * @returns {Promise} 返回操作结果
 */
export function payOrder(orderNo) {
  return request.post(`/order/${orderNo}/pay`)
}

/**
 * 取消订单
 * @param {string} orderNo - 订单号
 * @returns {Promise} 返回操作结果
 */
export function cancelOrder(orderNo) {
  return request.post(`/order/${orderNo}/cancel`)
}

/**
 * 确认完成（确认收货）
 * @param {string} orderNo - 订单号
 * @returns {Promise} 返回操作结果
 */
export function confirmOrder(orderNo) {
  return request.post(`/order/${orderNo}/confirm`)
}

/**
 * 默认导出：便于以 orderApi.xxx() 的形式统一调用
 */
export default {
  createOrder,
  getMyOrders,
  getSoldOrders,
  getOrderDetail,
  payOrder,
  cancelOrder,
  confirmOrder
}
