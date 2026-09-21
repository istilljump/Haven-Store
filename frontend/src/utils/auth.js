/**
 * 用户认证相关工具函数
 * 处理登录、注册、Token管理等
 */

import { ElMessage } from 'element-plus'
import router from '@/router'

// Token存储键名
const TOKEN_KEY = 'access_token'
const USER_INFO_KEY = 'user_info'

/**
 * 保存Token到localStorage
 * @param {string} token - 登录Token
 */
export function setToken(token) {
  if (!token) {
    console.warn('Token为空，跳过保存')
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
    console.warn('用户信息为空，跳过保存')
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
    console.error('解析用户信息失败:', error)
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

/**
 * 获取当前登录用户名
 * @returns {string} 返回用户名，如果未登录则返回空字符串
 */
export function getCurrentUsername() {
  const userInfo = getUserInfo()
  return userInfo?.username || ''
}

/**
 * 用户登录
 * @param {string} username - 用户名
 * @param {string} password - 密码
 * @returns {Promise<Object>} 返回登录结果
 */
export async function login(username, password) {
  try {
    const response = await fetch('/api/user/login', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ username, password })
    })

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }

    const result = await response.json()
    
    if (result.code === 200 && result.data) {
      // 保存Token和用户信息
      setToken(result.data.token)
      setUserInfo(result.data.userInfo)
      
      ElMessage.success('登录成功')
      return result.data
    } else {
      throw new Error(result.msg || '登录失败')
    }
  } catch (error) {
    console.error('登录请求失败:', error)
    ElMessage.error(error.message || '登录请求失败')
    throw error
  }
}

/**
 * 用户注册
 * @param {Object} registrationData - 注册信息
 * @param {string} registrationData.username - 用户名
 * @param {string} registrationData.password - 密码
 * @param {string} registrationData.confirmPassword - 确认密码
 * @param {string} registrationData.phone - 手机号
 * @param {string} registrationData.nickname - 昵称
 * @returns {Promise<Object>} 返回注册结果
 */
export async function register(registrationData) {
  try {
    const response = await fetch('/api/user/register', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(registrationData)
    })

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }

    const result = await response.json()
    
    if (result.code === 200) {
      ElMessage.success('注册成功，请登录')
      return result.data
    } else {
      throw new Error(result.msg || '注册失败')
    }
  } catch (error) {
    console.error('注册请求失败:', error)
    ElMessage.error(error.message || '注册请求失败')
    throw error
  }
}

/**
 * 用户登出
 * @returns {void}
 */
export function logout() {
  clearAuth()
  ElMessage.success('已退出登录')
  
  // 跳转到登录页
  router.push('/login')
}

/**
 * 跳转到登录页面
 * @param {string} redirectUrl - 登录后要跳转的URL
 */
export function redirectToLogin(redirectUrl = '') {
  clearAuth()
  const loginPath = '/login'
  const finalPath = redirectUrl ? `${loginPath}?redirect=${encodeURIComponent(redirectUrl)}` : loginPath
  router.push(finalPath)
}

/**
 * Token验证中间件
 * 在请求拦截器中使用
 * @returns {string|null} 返回Token或null
 */
export function getAuthHeader() {
  const token = getToken()
  if (token) {
    return `Bearer ${token}`
  }
  return null
}

/**
 * 检查Token是否过期
 * @returns {boolean} 返回Token是否过期
 */
export function isTokenExpired() {
  // 这里可以根据实际Token实现来检查过期时间
  // 简化版：只要有Token就认为有效
  return !getToken()
}

/**
 * 自动刷新Token（如果有刷新接口）
 * @returns {Promise<boolean>} 返回刷新是否成功
 */
export async function refreshToken() {
  try {
    // 这里应该调用刷新Token的API
    // 目前简化处理，返回false表示需要重新登录
    return false
  } catch (error) {
    console.error('刷新Token失败:', error)
    return false
  }
}

/**
 * 权限检查函数
 * @param {string|Array} permission - 权限标识
 * @returns {boolean} 返回是否有权限
 */
export function hasPermission(permission) {
  if (!permission) return true
  
  // 这里可以根据实际需求实现权限检查逻辑
  // 简化版：直接返回true，所有用户都有权限
  return true
}