/**
 * 用户认证相关工具函数
 * <p>
 * 只负责 Token 与用户信息在 localStorage 的存取（全站登录态的唯一数据源），
 * 登录/注册等网络行为统一走 api/user.js + store，此处不再保留一套
 * 基于 fetch 的旧实现（其响应结构与后端不符，误用会静默丢数据）
 */

// Token存储键名
const TOKEN_KEY = 'access_token'
const USER_INFO_KEY = 'user_info'

/**
 * 保存Token到localStorage
 * @param {string} token - 登录Token
 */
export function setToken(token) {
  if (!token) {
    return
  }
  localStorage.setItem(TOKEN_KEY, token)
}

/**
 * 获取存储的Token
 * @returns {string|null} 返回Token字符串，如果不存在则返回null
 */
export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

/**
 * 清除Token
 */
export function removeToken() {
  localStorage.removeItem(TOKEN_KEY)
}

/**
 * 保存用户信息到localStorage
 * @param {Object} userInfo - 用户信息
 */
export function setUserInfo(userInfo) {
  if (!userInfo) {
    return
  }
  localStorage.setItem(USER_INFO_KEY, JSON.stringify(userInfo))
}

/**
 * 获取存储的用户信息
 * @returns {Object|null} 返回用户信息对象，如果不存在则返回null
 */
export function getUserInfo() {
  const userInfoStr = localStorage.getItem(USER_INFO_KEY)
  if (!userInfoStr) return null

  try {
    return JSON.parse(userInfoStr)
  } catch (error) {
    return null
  }
}

/**
 * 清除用户信息
 */
export function removeUserInfo() {
  localStorage.removeItem(USER_INFO_KEY)
}

/**
 * 清除所有认证信息
 */
export function clearAuth() {
  removeToken()
  removeUserInfo()
}

/**
 * 检查是否已登录
 * @returns {boolean} 返回登录状态
 */
export function isLoggedIn() {
  return !!getToken() && !!getUserInfo()
}

/**
 * 获取当前登录用户ID
 * @returns {number|null} 返回用户ID，如果未登录则返回null
 */
export function getCurrentUserId() {
  const userInfo = getUserInfo()
  return userInfo?.userId || null
}
