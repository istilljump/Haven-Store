/**
 * 管理员相关API接口
 * 包含用户管理、商品管理、分类管理、系统消息等功能
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
 * @returns {Promise} 返回统计数据和最近活动
 */
export function getAdminDashboard() {
  return request.get('/admin/dashboard')
}

/**
 * 获取用户列表
 * @param {Object} queryParams - 查询参数
 * @param {number} queryParams.page - 页码
 * @param {number} queryParams.pageSize - 每页条数
 * @param {string} queryParams.status - 用户状态
 * @param {string} queryParams.keyword - 搜索关键词
 * @returns {Promise} 返回用户列表
 */
export function getAdminUsers(queryParams) {
  return request.get('/admin/users', {
    params: queryParams
  })
}

/**
 * 更新用户状态
 * @param {number} userId - 用户ID
 * @param {Object} statusData - 状态信息
 * @param {number} statusData.status - 用户状态 (1-正常, 2-禁用)
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
 * @param {string} queryParams.status - 商品状态
 * @param {number} queryParams.categoryId - 分类ID
 * @param {string} queryParams.keyword - 搜索关键词
 * @returns {Promise} 返回商品列表
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
 * @param {number} statusData.status - 商品状态 (1-在售, 2-已售出, 3-已下架)
 * @returns {Promise} 返回更新结果
 */
export function updateProductStatus(productId, statusData) {
  return request.put(`/admin/products/${productId}/status`, statusData)
}

/**
 * 获取分类列表
 * @returns {Promise} 返回分类列表
 */
export function getAdminCategories() {
  return request.get('/admin/categories')
}

/**
 * 添加分类
 * @param {Object} categoryData - 分类信息
 * @param {string} categoryData.name - 分类名称
 * @param {number} categoryData.sort - 排序
 * @returns {Promise} 返回添加结果
 */
export function addCategory(categoryData) {
  return request.post('/admin/categories', categoryData)
}

/**
 * 更新分类
 * @param {number} categoryId - 分类ID
 * @param {Object} categoryData - 分类信息
 * @param {string} categoryData.name - 分类名称
 * @param {number} categoryData.sort - 排序
 * @param {number} categoryData.status - 状态
 * @returns {Promise} 返回更新结果
 */
export function updateCategory(categoryId, categoryData) {
  return request.put(`/admin/categories/${categoryId}`, categoryData)
}

/**
 * 删除分类
 * @param {number} categoryId - 分类ID
 * @returns {Promise} 返回删除结果
 */
export function deleteCategory(categoryId) {
  return request.delete(`/admin/categories/${categoryId}`)
}

/**
 * 发送系统消息
 * @param {Object} messageData - 消息数据
 * @param {string} messageData.title - 消息标题
 * @param {string} messageData.content - 消息内容
 * @param {string} messageData.userType - 接收用户类型 ('all'-'所有', 'admin'-'管理员', 'user'-'普通用户')
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
 * @param {string} queryParams.status - 消息状态
 * @param {string} queryParams.userType - 用户类型
 * @returns {Promise} 返回消息列表
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
 * 获取用户统计图表数据
 * @returns {Promise} 返回统计数据
 */
export function getUserStats() {
  return request.get('/admin/stats/users')
}

/**
 * 获取商品统计图表数据
 * @returns {Promise} 返回统计数据
 */
export function getProductStats() {
  return request.get('/admin/stats/products')
}

/**
 * 获取交易统计图表数据
 * @returns {Promise} 返回统计数据
 */
export function getTradeStats() {
  return request.get('/admin/stats/trades')
}

/**
 * 导出用户数据
 * @param {Object} queryParams - 查询参数
 * @returns {Promise} 返回导出结果
 */
export function exportUsers(queryParams) {
  return request.download('/admin/export/users', {
    params: queryParams,
    filename: '用户数据.xlsx'
  })
}

/**
 * 导出商品数据
 * @param {Object} queryParams - 查询参数
 * @returns {Promise} 返回导出结果
 */
export function exportProducts(queryParams) {
  return request.download('/admin/export/products', {
    params: queryParams,
    filename: '商品数据.xlsx'
  })
}

/**
 * 获取系统日志
 * @param {Object} queryParams - 查询参数
 * @param {string} queryParams.level - 日志级别
 * @param {string} queryParams.startTime - 开始时间
 * @param {string} queryParams.endTime - 结束时间
 * @returns {Promise} 返回日志数据
 */
export function getSystemLogs(queryParams) {
  return request.get('/admin/logs', {
    params: queryParams
  })
}

/**
 * 清理缓存
 * @returns {Promise} 返回清理结果
 */
export function clearCache() {
  return request.post('/admin/cache/clear')
}

/**
 * 获取服务器信息
 * @returns {Promise} 返回服务器信息
 */
export function getServerInfo() {
  return request.get('/admin/server/info')
}

/**
 * 执行数据库备份
 * @returns {Promise} 返回备份结果
 */
export function backupDatabase() {
  return request.post('/admin/backup/database')
}

/**
 * 获取备份列表
 * @returns {Promise} 返回备份列表
 */
export function getBackupList() {
  return request.get('/admin/backup/list')
}