/**
 * Vuex状态管理
 * 集中管理应用的状态
 */

import { createStore } from 'vuex'
import { getToken, setToken, removeToken, getUserInfo, setUserInfo, removeUserInfo } from '@/utils/auth'
import userApi from '@/api/user'

const store = createStore({
  state: {
    // 用户信息
    userInfo: null,
    // Token
    token: getToken(),
    // 加载状态
    loading: false,
    // 错误信息
    error: null,
    // 全局配置
    config: {
      siteName: '二手商品交易市场',
      version: '1.0.0',
      apiBaseUrl: '/api'
    }
  },

  getters: {
    // 是否已登录
    isLoggedIn: state => !!state.userInfo,
    // 获取用户信息
    user: state => state.userInfo,
    // 获取用户ID
    userId: state => state.userInfo?.userId,
    // 获取用户名
    username: state => state.userInfo?.username,
    // 是否是管理员
    isAdmin: state => state.userInfo?.isAdmin || false,
    // 加载状态
    isLoading: state => state.loading,
    // 错误信息
    error: state => state.error
  },

  mutations: {
    // 设置Token
    SET_TOKEN(state, token) {
      state.token = token
      setToken(token)
    },

    // 设置用户信息
    SET_USER_INFO(state, userInfo) {
      state.userInfo = userInfo
      setUserInfo(userInfo)
    },

    // 清除用户信息
    CLEAR_USER(state) {
      state.userInfo = null
      state.token = null
      removeToken()
      removeUserInfo()
    },

    // 设置加载状态
    SET_LOADING(state, loading) {
      state.loading = loading
    },

    // 设置错误信息
    SET_ERROR(state, error) {
      state.error = error
    },

    // 清除错误信息
    CLEAR_ERROR(state) {
      state.error = null
    },

    // 更新用户信息
    UPDATE_USER_INFO(state, userInfo) {
      state.userInfo = { ...state.userInfo, ...userInfo }
      setUserInfo(state.userInfo)
    }
  },

  actions: {
    // 用户登录
    async login({ commit }, loginData) {
      try {
        commit('SET_LOADING', true)
        commit('CLEAR_ERROR')

        const response = await userApi.login(loginData)
        
        // 保存Token和用户信息
        commit('SET_TOKEN', response.token)
        commit('SET_USER_INFO', response.userInfo)

        return response
      } catch (error) {
        commit('SET_ERROR', error.message)
        throw error
      } finally {
        commit('SET_LOADING', false)
      }
    },

    // 用户注册
    async register({ commit }, registrationData) {
      try {
        commit('SET_LOADING', true)
        commit('CLEAR_ERROR')

        const response = await userApi.register(registrationData)
        
        return response
      } catch (error) {
        commit('SET_ERROR', error.message)
        throw error
      } finally {
        commit('SET_LOADING', false)
      }
    },

    // 获取用户信息
    async getUserInfo({ commit, state }) {
      try {
        if (!state.token) {
          throw new Error('未登录')
        }

        commit('SET_LOADING', true)
        commit('CLEAR_ERROR')

        const response = await userApi.getCurrentUserInfo()
        commit('SET_USER_INFO', response.data)
        
        return response.data
      } catch (error) {
        commit('SET_ERROR', error.message)
        // 如果Token过期，清除用户信息
        if (error.message.includes('未授权') || error.message.includes('Token')) {
          commit('CLEAR_USER')
          throw new Error('登录已过期，请重新登录')
        }
        throw error
      } finally {
        commit('SET_LOADING', false)
      }
    },

    // 更新用户信息
    async updateUserInfo({ commit }, userInfo) {
      try {
        commit('SET_LOADING', true)
        commit('CLEAR_ERROR')

        const response = await userApi.updateUserInfo(userInfo)
        // 更新本地状态
        commit('UPDATE_USER_INFO', response.data)
        
        return response
      } catch (error) {
        commit('SET_ERROR', error.message)
        throw error
      } finally {
        commit('SET_LOADING', false)
      }
    },

    // 修改密码
    async changePassword({ commit }, passwordData) {
      try {
        commit('SET_LOADING', true)
        commit('CLEAR_ERROR')

        const response = await userApi.changePassword(passwordData)
        
        return response
      } catch (error) {
        commit('SET_ERROR', error.message)
        throw error
      } finally {
        commit('SET_LOADING', false)
      }
    },

    // 用户登出
    logout({ commit }) {
      commit('CLEAR_USER')
    },

    // 上传头像
    async uploadAvatar({ commit }, formData) {
      try {
        commit('SET_LOADING', true)
        commit('CLEAR_ERROR')

        const response = await userApi.uploadAvatar(formData)
        
        // 更新用户信息中的头像
        if (response.data && response.data.avatar) {
          commit('UPDATE_USER_INFO', { avatar: response.data.avatar })
        }
        
        return response
      } catch (error) {
        commit('SET_ERROR', error.message)
        throw error
      } finally {
        commit('SET_LOADING', false)
      }
    },

    // 清除错误
    clearError({ commit }) {
      commit('CLEAR_ERROR')
    },

    // 加载应用配置
    async loadConfig({ commit }) {
      try {
        // 这里可以从API加载配置信息
        // 简化版本，使用默认配置
        commit('SET_CONFIG', {
          siteName: '二手商品交易市场',
          version: '1.0.0',
          apiBaseUrl: '/api'
        })
      } catch (error) {
        console.error('加载配置失败:', error)
      }
    }
  },

  modules: {
    // 可以添加其他模块，如商品模块、订单模块等
  }
})

// 持久化处理（可选）
store.subscribe((mutation, state) => {
  // 可以在这里将状态持久化到localStorage
  // 例如：保存用户信息
})

export default store