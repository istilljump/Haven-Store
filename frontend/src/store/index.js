/**
 * Pinia状态管理
 * 集中管理应用的状态
 */

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
// 注意：这里对 utils/auth 的三个同名方法做别名导入。
// 因为下面 store 内部也定义了同名方法（setToken / setUserInfo / getUserInfo），
// 若直接同名导入会被内层 const 遮蔽，方法体里再调用自己就会无限递归（栈溢出）。
import {
  getToken,
  setToken as saveToken,
  removeToken,
  getUserInfo as loadUserInfo,
  setUserInfo as saveUserInfo,
  removeUserInfo
} from '@/utils/auth'
import userApi from '@/api/user'

/**
 * 归一化用户信息
 * <p>
 * 说明：登录接口（/user/login、/admin/login）返回的是扁平结构 LoginUserVO，
 * 含 isAdmin 布尔值；而 /user/info 返回的是 User 实体，只有 role 数字。
 * 两种来源统一收敛成本结构，业务代码只认 isAdmin / userId，不再各自判断。
 *
 * @param {Object|null} raw - 接口原始返回
 * @returns {Object|null} 归一化后的用户信息，入参为空时返回 null
 */
function normalizeUserInfo(raw) {
  if (!raw) return null
  return {
    ...raw,
    // User 实体用 id，LoginUserVO 用 userId，这里统一补齐
    userId: raw.userId ?? raw.id ?? null,
    // User 实体的 role：1 管理员 / 0 普通用户（见 UserConstant.ROLE_ADMIN）
    isAdmin: raw.isAdmin !== undefined ? raw.isAdmin : raw.role === 1
  }
}

export const useUserStore = defineStore('user', () => {
  // 状态
  // 说明：登录成功后用户信息已写入 localStorage，这里在 store 初始化时回填。
  // 若不回填，刷新页面后 userInfo 会变回 null，表现为「已登录却被当成未登录」。
  const userInfo = ref(normalizeUserInfo(loadUserInfo()))
  const token = ref(getToken())
  const loading = ref(false)
  const error = ref(null)

  // 计算属性
  const isLoggedIn = computed(() => !!userInfo.value)
  const user = computed(() => userInfo.value)
  const userId = computed(() => userInfo.value?.userId)
  const username = computed(() => userInfo.value?.username)
  const isAdmin = computed(() => userInfo.value?.isAdmin || false)
  const isLoading = computed(() => loading.value)
  const errorMessage = computed(() => error.value)

  // 方法
  const setToken = (newToken) => {
    token.value = newToken
    saveToken(newToken)
  }

  const setUserInfo = (info) => {
    userInfo.value = info
    saveUserInfo(info)
  }

  const clearUser = () => {
    userInfo.value = null
    token.value = null
    removeToken()
    removeUserInfo()
  }

  const setLoading = (isLoading) => {
    loading.value = isLoading
  }

  const setError = (errorMessage) => {
    error.value = errorMessage
  }

  const clearError = () => {
    error.value = null
  }

  const updateUserInfo = (updatedInfo) => {
    userInfo.value = { ...userInfo.value, ...updatedInfo }
    setUserInfo(userInfo.value)
  }

  // Actions
  const login = async (loginData) => {
    try {
      setLoading(true)
      clearError()

      const response = await userApi.login(loginData)

      // 保存Token和用户信息
      // 注意：/user/login 返回的是扁平结构（LoginUserVO），没有嵌套的 userInfo 字段，
      // 此前写成 response.userInfo 会存进 undefined，导致登录后导航栏仍显示未登录
      setToken(response.token)
      setUserInfo(normalizeUserInfo(response))

      return response
    } catch (err) {
      setError(err.message)
      throw err
    } finally {
      setLoading(false)
    }
  }

  const register = async (registrationData) => {
    try {
      setLoading(true)
      clearError()

      const response = await userApi.register(registrationData)
      
      return response
    } catch (err) {
      setError(err.message)
      throw err
    } finally {
      setLoading(false)
    }
  }

  const getUserInfo = async () => {
    try {
      if (!token.value) {
        throw new Error('未登录')
      }

      setLoading(true)
      clearError()

      const response = await userApi.getCurrentUserInfo()
      // 注意：request.js 的响应拦截器已返回 res.data，这里不能再取一层 .data，
      // 否则存进去的是 undefined，用户信息会被静默丢弃
      const info = normalizeUserInfo(response)
      setUserInfo(info)

      return info
    } catch (err) {
      setError(err.message)
      // 如果Token过期，清除用户信息
      if (err.message.includes('未授权') || err.message.includes('Token')) {
        clearUser()
        throw new Error('登录已过期，请重新登录')
      }
      throw err
    } finally {
      setLoading(false)
    }
  }

  const updateUserInfoAsync = async (userInfoData) => {
    try {
      setLoading(true)
      clearError()

      const response = await userApi.updateUserInfo(userInfoData)
      // 更新本地状态（拦截器已解包，直接用返回值）
      updateUserInfo(normalizeUserInfo(response))
      
      return response
    } catch (err) {
      setError(err.message)
      throw err
    } finally {
      setLoading(false)
    }
  }

  const changePassword = async (passwordData) => {
    try {
      setLoading(true)
      clearError()

      const response = await userApi.changePassword(passwordData)
      
      return response
    } catch (err) {
      setError(err.message)
      throw err
    } finally {
      setLoading(false)
    }
  }

  const logout = () => {
    clearUser()
  }

  const uploadAvatar = async (formData) => {
    try {
      setLoading(true)
      clearError()

      const response = await userApi.uploadAvatar(formData)

      // 更新用户信息中的头像（拦截器已解包）
      if (response && response.avatar) {
        updateUserInfo({ avatar: response.avatar })
      }
      
      return response
    } catch (err) {
      setError(err.message)
      throw err
    } finally {
      setLoading(false)
    }
  }

  const clearErrorState = () => {
    clearError()
  }

  return {
    // 状态
    userInfo,
    token,
    loading,
    error,
    
    // 计算属性
    isLoggedIn,
    user,
    userId,
    username,
    isAdmin,
    isLoading,
    errorMessage,
    
    // 方法
    setToken,
    setUserInfo,
    clearUser,
    setLoading,
    setError,
    clearError,
    updateUserInfo,
    
    // Actions
    login,
    register,
    getUserInfo,
    updateUserInfoAsync,
    changePassword,
    logout,
    uploadAvatar,
    clearErrorState
  }
})

export const useConfigStore = defineStore('config', () => {
  const config = ref({
    siteName: 'Haven-Store',
    version: '1.0.0',
    apiBaseUrl: '/api'
  })

  const updateConfig = (newConfig) => {
    config.value = { ...config.value, ...newConfig }
  }

  return {
    config,
    updateConfig
  }
})