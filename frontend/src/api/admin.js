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
  return request.get('/admin/users', queryParams)
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
  return request.get('/admin/products', queryParams)
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
 * 删除商品
 * @param {number} productId - 商品ID
 * @returns {Promise} 返回删除结果
 */
export function deleteProduct(productId) {
  return request.delete(`/admin/products/${productId}`)
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
  return request.get('/admin/messages', queryParams)
}

/**
 * 删除系统消息（整组删除一条公告）
 * @param {number} messageId - 消息ID
 * @returns {Promise} 返回删除结果
 */
export function deleteMessage(messageId) {
  return request.delete(`/admin/messages/${messageId}`)
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

// ==================== 订单管理 ====================

/**
 * 获取订单列表（管理端）
 * @param {Object} queryParams - 查询参数
 * @param {number} [queryParams.status] - 订单状态（1待支付 2已支付 3已取消 4已完成）
 * @param {string} [queryParams.keyword] - 订单号关键词
 * @returns {Promise} 返回分页订单列表（{records, total}）
 */
export function getAdminOrders(queryParams) {
  return request.get('/admin/orders', queryParams)
}

/**
 * 获取订单详情（管理端，含明细与买卖双方信息）
 * @param {string} orderNo - 订单号
 * @returns {Promise} 返回订单详情
 */
export function getAdminOrderDetail(orderNo) {
  return request.get(`/admin/orders/${orderNo}`)
}

/**
 * 导出订单数据（CSV，含商品明细）
 * @param {Object} queryParams - 查询参数（与列表页筛选项一致）
 * @returns {Promise} 触发浏览器下载
 */
export function exportOrders(queryParams) {
  return request.download('/admin/export/orders', '订单数据.csv', {
    params: queryParams
  })
}

// ==================== 评论管理 ====================

/**
 * 获取评论列表（管理端）
 * @param {Object} queryParams - 查询参数
 * @param {number} [queryParams.productId] - 商品ID
 * @param {number} [queryParams.status] - 评论状态（1正常 0隐藏）
 * @returns {Promise} 返回分页评论列表（{records, total}）
 */
export function getAdminComments(queryParams) {
  return request.get('/admin/comments', queryParams)
}

/**
 * 变更评论状态（显示/隐藏）
 * @param {number} commentId - 评论ID
 * @param {Object} statusData - {status: 1正常 0隐藏}
 * @returns {Promise} 返回操作结果
 */
export function updateCommentStatus(commentId, statusData) {
  return request.put(`/admin/comments/${commentId}/status`, statusData)
}

/**
 * 删除评论
 * @param {number} commentId - 评论ID
 * @returns {Promise} 返回删除结果
 */
export function deleteComment(commentId) {
  return request.delete(`/admin/comments/${commentId}`)
}

// ==================== 举报处理 ====================

/**
 * 获取举报列表（管理端，待处理的排在前面）
 * @param {Object} queryParams - 查询参数
 * @param {number} [queryParams.status] - 处理状态（0待处理 1已处理 2已驳回）
 * @param {string} [queryParams.targetType] - 举报对象类型（product/comment）
 * @returns {Promise} 返回分页举报列表（{records, total}）
 */
export function getAdminReports(queryParams) {
  return request.get('/admin/reports', queryParams)
}

/**
 * 处理举报
 * @param {number} reportId - 举报ID
 * @param {Object} handleData - 处理入参
 * @param {string} handleData.action - 处理动作（takeDownProduct/hideComment/dismiss）
 * @param {string} [handleData.note] - 处理备注
 * @returns {Promise} 返回处理结果
 */
export function handleReport(reportId, handleData) {
  return request.put(`/admin/reports/${reportId}/handle`, handleData)
}

// ==================== 用户管理增强 ====================

/**
 * 编辑用户资料（昵称/手机号/角色）
 * @param {number} userId - 用户ID
 * @param {Object} userData - 编辑数据（nickname/phone/isAdmin，字段不传表示保持不变）
 * @returns {Promise} 返回更新结果
 */
export function updateUser(userId, userData) {
  return request.put(`/admin/users/${userId}`, userData)
}

/**
 * 重置用户密码
 * @param {number} userId - 用户ID
 * @param {Object} passwordData - {password: 新密码}
 * @returns {Promise} 返回重置结果
 */
export function resetUserPassword(userId, passwordData) {
  return request.put(`/admin/users/${userId}/password`, passwordData)
}

// ==================== 数据看板 ====================

/**
 * 获取近 N 日新增趋势（用户/商品/订单，按天聚合）
 * @param {number} days - 天数（1-30，默认 7）
 * @returns {Promise} 返回趋势点数组 [{date, newUsers, newProducts, newOrders}]
 */
export function getTrendStats(days = 7) {
  return request.get('/admin/stats/trend', { days })
}

/**
 * 获取商品分类分布
 * @returns {Promise} 返回 [{categoryId, categoryName, count}]
 */
export function getCategoryStats() {
  return request.get('/admin/stats/category')
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
  updateUser,
  resetUserPassword,
  getAdminProducts,
  updateProductStatus,
  deleteProduct,
  getAdminCategories,
  addCategory,
  updateCategory,
  deleteCategory,
  sendSystemMessage,
  getAdminMessages,
  deleteMessage,
  getAdminSettings,
  updateAdminSettings,
  exportUsers,
  exportProducts,
  getAdminOrders,
  getAdminOrderDetail,
  exportOrders,
  getAdminComments,
  updateCommentStatus,
  deleteComment,
  getAdminReports,
  handleReport,
  getTrendStats,
  getCategoryStats
}
