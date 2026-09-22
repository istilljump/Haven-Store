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

export const useUserStore = defineStore('user', () => {
  // 状态
  const userInfo = ref(null)
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
      setToken(response.token)
      setUserInfo(response.userInfo)

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
      setUserInfo(response.data)
      
      return response.data
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
      // 更新本地状态
      updateUserInfo(response.data)
      
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
      
      // 更新用户信息中的头像
      if (response.data && response.data.avatar) {
        updateUserInfo({ avatar: response.data.avatar })
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
    siteName: '二手商品交易市场',
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