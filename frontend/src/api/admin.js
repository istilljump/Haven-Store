/**
 * 管理员相关API接口
 * 包含用户管理、商品管理、分类管理、系统消息、系统设置等功能
 *
 * 说明：本文件只保留后端已实现的接口。此前这里还定义过 export/backup/cache/serverInfo
 * 等一批后端并不存在的接口，属于「画饼」式定义，已随本次联调一并清理，
 * 避免后续误调用后拿到 404 却不知道原因。
 */

import request from '@/utils/request'

/**
 * 管理员登录
 * @param {Object} loginData - 登录信息
 * @param {string} loginData.username - 管理员用户名
 * @param {string} loginData.password - 密码
 * @returns {Promise} 返回登录结果
 */
export function adminLogin(loginData) {
  return request.post('/admin/login', loginData)
}

/**
 * 获取管理员后台首页数据
 * @returns {Promise} 返回统计数据、最近商品与最近系统消息
 */
export function getAdminDashboard() {
  return request.get('/admin/dashboard')
}

/**
 * 获取用户列表
 * @param {Object} queryParams - 查询参数
 * @param {number} queryParams.page - 页码
 * @param {number} queryParams.pageSize - 每页条数
 * @param {number|string} queryParams.status - 用户状态（1正常 0禁用）
 * @param {string} queryParams.keyword - 搜索关键词（用户名/昵称/手机号）
 * @returns {Promise} 返回分页用户列表（{records, total}）
 */
export function getAdminUsers(queryParams) {
  return request.get('/admin/users', {
    params: queryParams
  })
}

/**
 * 新增用户
 * @param {Object} userData - 用户信息
 * @param {string} userData.username - 用户名
 * @param {string} userData.password - 密码
 * @param {string} [userData.phone] - 手机号（可选）
 * @param {string} [userData.nickname] - 昵称（可选）
 * @param {boolean} [userData.isAdmin] - 是否创建为管理员
 * @returns {Promise} 返回新增结果
 */
export function createUser(userData) {
  return request.post('/admin/users', userData)
}

/**
 * 更新用户状态（启用/禁用）
 * @param {number} userId - 用户ID
 * @param {Object} statusData - 状态信息
 * @param {number} statusData.status - 用户状态（1正常 0禁用）
 * @returns {Promise} 返回更新结果
 */
export function updateUserStatus(userId, statusData) {
  return request.put(`/admin/users/${userId}/status`, statusData)
}

/**
 * 获取商品列表
 * @param {Object} queryParams - 查询参数
 * @param {number} queryParams.page - 页码
 * @param {number} queryParams.pageSize - 每页条数
 * @param {number|string} queryParams.status - 商品状态（1在售 2已售出 3已下架）
 * @param {number|string} queryParams.categoryId - 分类ID
 * @param {string} queryParams.keyword - 搜索关键词（商品标题）
 * @returns {Promise} 返回分页商品列表（{records, total}）
 */
export function getAdminProducts(queryParams) {
  return request.get('/admin/products', {
    params: queryParams
  })
}

/**
 * 更新商品状态
 * @param {number} productId - 商品ID
 * @param {Object} statusData - 状态信息
 * @param {number} statusData.status - 商品状态（1在售 2已售出 3已下架）
 * @returns {Promise} 返回更新结果
 */
export function updateProductStatus(productId, statusData) {
  return request.put(`/admin/products/${productId}/status`, statusData)
}

/**
 * 获取分类列表
 * @returns {Promise} 返回分类数组
 */
export function getAdminCategories() {
  return request.get('/admin/categories')
}

/**
 * 添加分类
 * @param {Object} categoryData - 分类信息
 * @param {string} categoryData.name - 分类名称
 * @param {number} categoryData.sort - 排序
 * @param {number} categoryData.status - 状态（1启用 0禁用）
 * @returns {Promise} 返回添加结果
 */
export function addCategory(categoryData) {
  return request.post('/admin/categories', categoryData)
}

/**
 * 更新分类
 * @param {number} categoryId - 分类ID
 * @param {Object} categoryData - 分类信息
 * @returns {Promise} 返回更新结果
 */
export function updateCategory(categoryId, categoryData) {
  return request.put(`/admin/categories/${categoryId}`, categoryData)
}

/**
 * 删除分类
 * @param {number} categoryId - 分类ID
 * @returns {Promise} 返回删除结果；分类下存在商品时会被后端拒绝
 */
export function deleteCategory(categoryId) {
  return request.delete(`/admin/categories/${categoryId}`)
}

/**
 * 发送系统消息（按接收群体群发）
 * @param {Object} messageData - 消息数据
 * @param {string} messageData.title - 消息标题
 * @param {string} messageData.content - 消息内容
 * @param {string} messageData.userType - 接收群体（'all' 所有用户 / 'admin' 仅管理员 / 'user' 仅普通用户）
 * @returns {Promise} 返回发送结果
 */
export function sendSystemMessage(messageData) {
  return request.post('/admin/messages', messageData)
}

/**
 * 获取系统消息列表
 * @param {Object} queryParams - 查询参数
 * @param {number} queryParams.page - 页码
 * @param {number} queryParams.pageSize - 每页条数
 * @param {number|string} queryParams.status - 消息状态（1发送中 2已送达）
 * @param {string} queryParams.userType - 接收群体
 * @returns {Promise} 返回分页消息列表（{records, total}）
 */
export function getAdminMessages(queryParams) {
  return request.get('/admin/messages', {
    params: queryParams
  })
}

/**
 * 获取系统设置
 * @returns {Promise} 返回系统设置
 */
export function getAdminSettings() {
  return request.get('/admin/settings')
}

/**
 * 更新系统设置
 * @param {Object} settings - 系统设置
 * @returns {Promise} 返回更新结果
 */
export function updateAdminSettings(settings) {
  return request.put('/admin/settings', settings)
}

/**
 * 导出用户数据（CSV）
 * @param {Object} queryParams - 查询参数（与列表页筛选项一致）
 * @returns {Promise} 触发浏览器下载
 */
export function exportUsers(queryParams) {
  return request.download('/admin/export/users', '用户数据.csv', {
    params: queryParams
  })
}

/**
 * 导出商品数据（CSV）
 * @param {Object} queryParams - 查询参数（与列表页筛选项一致）
 * @returns {Promise} 触发浏览器下载
 */
export function exportProducts(queryParams) {
  return request.download('/admin/export/products', '商品数据.csv', {
    params: queryParams
  })
}

/**
 * 默认导出：便于以 adminApi.xxx() 的形式统一调用
 */
export default {
  adminLogin,
  getAdminDashboard,
  getAdminUsers,
  createUser,
  updateUserStatus,
  getAdminProducts,
  updateProductStatus,
  getAdminCategories,
  addCategory,
  updateCategory,
  deleteCategory,
  sendSystemMessage,
  getAdminMessages,
  getAdminSettings,
  updateAdminSettings,
  exportUsers,
  exportProducts
}
