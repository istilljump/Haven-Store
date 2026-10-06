/**
 * 举报相关API接口
 * 用户对商品/评论发起举报；举报的查询与处理在管理后台（admin.js）
 */

import request from '@/utils/request'

/**
 * 提交举报
 * @param {Object} reportData - 举报信息
 * @param {string} reportData.targetType - 举报对象类型（'product' 商品 / 'comment' 评论）
 * @param {number} reportData.targetId - 举报对象 ID
 * @param {string} reportData.reason - 举报原因
 * @param {string} [reportData.description] - 补充说明
 * @returns {Promise} 返回提交结果
 */
export function submitReport(reportData) {
  return request.post('/report', reportData)
}

/**
 * 默认导出
 */
export default {
  submitReport
}
