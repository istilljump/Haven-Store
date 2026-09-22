/**
 * 认证状态管理
 */

import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { getToken, setToken, removeToken } from '@/utils/auth'
import { useUserStore } from './index'

export const useAuthStore = defineStore('auth', () => {
  const userStore = useUserStore()
  
  const token = ref(getToken())
  const user = computed(() => userStore.user)
  const isLoggedIn = computed(() => userStore.isLoggedIn)
  const isAdmin = computed(() => userStore.isAdmin)

  const setAuthToken = (newToken) => {
    token.value = newToken
    setToken(newToken)
  }

  const logout = () => {
    userStore.clearUser()
    token.value = null
  }

  const checkAuth = async () => {
    if (!token.value) {
      return false
    }
    
    try {
      await userStore.getUserInfo()
      return true
    } catch (error) {
      logout()
      return false
    }
  }

  return {
    token,
    user,
    isLoggedIn,
    isAdmin,
    setAuthToken,
    logout,
    checkAuth
  }
})