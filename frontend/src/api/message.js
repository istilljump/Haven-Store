/**
 * 私信与商品咨询API接口
 * 私信与咨询共用同一套接口：带上 productId 即为针对该商品的咨询
 */

import request from '@/utils/request'

/**
 * 发送私信
 * @param {Object} messageData - 私信内容
 * @param {number} messageData.toUserId - 接收者用户ID
 * @param {string} messageData.content - 私信内容
 * @param {number} [messageData.productId] - 关联商品ID（商品咨询填写）
 * @returns {Promise} 返回新私信ID
 */
export function sendPrivateMessage(messageData) {
  return request.post('/message/private/send', messageData)
}

/**
 * 获取当前用户的会话列表（按最近消息时间倒序）
 * @returns {Promise} 返回会话列表
 */
export function getConversations() {
  return request.get('/message/private/conversations')
}

/**
 * 获取与某人的聊天记录
 * 注意：调用后后端会把对方发来的消息标记为已读
 * @param {number} peerId - 对方用户ID
 * @param {number} [productId] - 关联商品ID（普通私信不传）
 * @returns {Promise} 返回按时间正序的聊天记录
 */
export function getChat(peerId, productId) {
  const params = productId ? { peerId, productId } : { peerId }
  return request.get('/message/private/chat', params)
}

/**
 * 获取未读私信总数
 * @returns {Promise} 返回未读条数
 */
export function getUnreadCount() {
  return request.get('/message/private/unread/count')
}

/**
 * 删除会话（删除后会话双方均不再可见）
 * @param {number} peerId - 对方用户ID
 * @param {number} [productId] - 关联商品ID（普通私信不传）
 * @returns {Promise} 返回操作结果
 */
export function deleteConversation(peerId, productId) {
  const params = productId ? { peerId, productId } : { peerId }
  return request.delete('/message/private/chat', { params })
}

/**
 * 默认导出：便于以 messageApi.xxx() 的形式统一调用
 */
export default {
  sendPrivateMessage,
  getConversations,
  getChat,
  getUnreadCount,
  deleteConversation
}
