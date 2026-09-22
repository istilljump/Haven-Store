/**
 * 用户相关API接口
 * 包含用户注册、登录、信息管理等功能
 */

import request from '@/utils/request'

/**
 * 用户注册
 * @param {Object} registrationData - 注册信息
 * @param {string} registrationData.username - 用户名
 * @param {string} registrationData.password - 密码
 * @param {string} registrationData.confirmPassword - 确认密码
 * @param {string} registrationData.phone - 手机号
 * @param {string} registrationData.nickname - 昵称
 * @returns {Promise} 返回注册结果
 */
export function register(registrationData) {
  return request.post('/user/register', registrationData)
}

/**
 * 用户登录
 * @param {Object} loginData - 登录信息
 * @param {string} loginData.username - 用户名
 * @param {string} loginData.password - 密码
 * @returns {Promise} 返回登录结果
 */
export function login(loginData) {
  return request.post('/user/login', loginData)
}

/**
 * 获取当前用户信息
 * @returns {Promise} 返回用户信息
 */
export function getCurrentUserInfo() {
  return request.get('/user/info')
}

/**
 * 更新用户信息
 * @param {Object} userInfo - 用户信息
 * @returns {Promise} 返回更新结果
 */
export function updateUserInfo(userInfo) {
  return request.put('/user/info', userInfo)
}

/**
 * 修改密码
 * @param {Object} passwordData - 密码信息
 * @param {string} passwordData.oldPassword - 原密码
 * @param {string} passwordData.newPassword - 新密码
 * @param {string} passwordData.confirmPassword - 确认新密码
 * @returns {Promise} 返回修改结果
 */
export function changePassword(passwordData) {
  return request.put('/user/password', passwordData)
}

/**
 * 上传头像
 * @param {FormData} formData - 头像文件数据
 * @param {function} onProgress - 上传进度回调
 * @returns {Promise} 返回上传结果
 */
export function uploadAvatar(formData, onProgress = null) {
  return request.upload('/user/avatar', formData, onProgress)
}

/**
 * 获取用户发布的商品列表
 * @param {Object} queryParams - 查询参数
 * @param {number} queryParams.page - 页码
 * @param {number} queryParams.pageSize - 每页条数
 * @param {string} queryParams.status - 商品状态
 * @returns {Promise} 返回商品列表
 */
export function getUserProducts(queryParams) {
  return request.get('/user/products', {
    params: queryParams
  })
}

/**
 * 获取用户的收藏列表
 * @param {Object} queryParams - 查询参数
 * @param {number} queryParams.page - 页码
 * @param {number} queryParams.pageSize - 每页条数
 * @returns {Promise} 返回收藏列表
 */
export function getUserFavorites(queryParams) {
  return request.get('/user/favorites', {
    params: queryParams
  })
}

/**
 * 获取用户的消息列表
 * @param {Object} queryParams - 查询参数
 * @param {number} queryParams.page - 页码
 * @param {number} queryParams.pageSize - 每页条数
 * @param {string} queryParams.unread - 是否未读
 * @returns {Promise} 返回消息列表
 */
export function getUserMessages(queryParams) {
  return request.get('/user/messages', {
    params: queryParams
  })
}

/**
 * 标记消息为已读
 * @param {number} messageId - 消息ID
 * @returns {Promise} 返回操作结果
 */
export function markMessageAsRead(messageId) {
  return request.put(`/user/messages/${messageId}/read`)
}

/**
 * 标记所有消息为已读
 * @returns {Promise} 返回操作结果
 */
export function markAllMessagesAsRead() {
  return request.put('/user/messages/read-all')
}

/**
 * 删除消息
 * @param {number} messageId - 消息ID
 * @returns {Promise} 返回操作结果
 */
export function deleteMessage(messageId) {
  return request.delete(`/user/messages/${messageId}`)
}

/**
 * 获取用户订单列表
 * @param {Object} queryParams - 查询参数
 * @param {number} queryParams.page - 页码
 * @param {number} queryParams.pageSize - 每页条数
 * @param {string} queryParams.status - 订单状态
 * @returns {Promise} 返回订单列表
 */
export function getUserOrders(queryParams) {
  return request.get('/user/orders', {
    params: queryParams
  })
}

/**
 * 获取订单详情
 * @param {number} orderId - 订单ID
 * @returns {Promise} 返回订单详情
 */
export function getOrderDetail(orderId) {
  return request.get(`/user/orders/${orderId}`)
}

/**
 * 取消订单
 * @param {number} orderId - 订单ID
 * @param {string} reason - 取消原因
 * @returns {Promise} 返回操作结果
 */
export function cancelOrder(orderId, reason) {
  return request.put(`/user/orders/${orderId}/cancel`, { reason })
}

/**
 * 确认收货
 * @param {number} orderId - 订单ID
 * @returns {Promise} 返回操作结果
 */
export function confirmOrder(orderId) {
  return request.put(`/user/orders/${orderId}/confirm`)
}

/**
 * 默认导出：便于以 userApi.xxx() 的形式统一调用
 */
export default {
  register,
  login,
  getCurrentUserInfo,
  updateUserInfo,
  changePassword,
  uploadAvatar,
  getUserProducts,
  getUserFavorites,
  getUserMessages,
  markMessageAsRead,
  markAllMessagesAsRead,
  deleteMessage,
  getUserOrders,
  getOrderDetail,
  cancelOrder,
  confirmOrder
}