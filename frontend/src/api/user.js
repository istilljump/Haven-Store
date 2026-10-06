/**
 * 用户相关API接口
 * 包含用户注册、登录、信息管理等功能
 *
 * 说明：只保留后端已实现的接口（/user/register、/user/login、/user/info、
 * /user/password、/user/avatar）。此前这里还定义过 /user/products、/user/orders
 * 等一批后端并不存在的接口，调用必然 404，已清理；
 * 商品/订单/消息能力分别在 product.js、order.js、message.js 中
 */

import request from '@/utils/request'

/**
 * 用户注册
 * @param {Object} registrationData - 注册信息
 * @param {string} registrationData.username - 用户名（3-20字符）
 * @param {string} registrationData.password - 密码（6-20字符）
 * @param {string} registrationData.confirmPassword - 确认密码
 * @param {string} registrationData.phone - 手机号（11位）
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
 * 默认导出：便于以 userApi.xxx() 的形式统一调用
 */
export default {
  register,
  login,
  getCurrentUserInfo,
  updateUserInfo,
  changePassword,
  uploadAvatar
}
