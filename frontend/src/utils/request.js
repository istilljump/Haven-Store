/**
 * HTTP请求工具
 * 统一处理API请求，包含错误处理、重试机制、请求拦截等
 */

import axios from 'axios'
import { ElMessage } from 'element-plus'
import { store } from '@/store'
import { ERROR_CODES, ERROR_MESSAGES } from './constants'

// 创建axios实例
const service = axios.create({
  baseURL: '/api', // API基础URL
  timeout: 10000,  // 请求超时时间
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
service.interceptors.request.use(
  config => {
    // 添加认证token
    const token = store.getters.token
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }

    // 显示加载状态
    if (config.loading !== false) {
      store.commit('SET_LOADING', true)
    }

    // 记录请求开始时间（用于计算耗时）
    config.metadata = { startTime: new Date() }

    return config
  },
  error => {
    // 请求错误时关闭loading
    store.commit('SET_LOADING', false)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  response => {
    // 关闭loading
    if (response.config.loading !== false) {
      store.commit('SET_LOADING', false)
    }

    // 计算请求耗时
    const endTime = new Date()
    const duration = endTime - response.config.metadata.startTime
    console.log(`请求完成: ${response.config.url}, 耗时: ${duration}ms`)

    // 处理响应数据
    const res = response.data

    // 检查响应状态码
    if (res.code === 200) {
      return res.data
    } else {
      // 处理业务错误
      handleErrorResponse(res)
      return Promise.reject(new Error(res.msg || '请求失败'))
    }
  },
  error => {
    // 关闭loading
    if (error.config?.loading !== false) {
      store.commit('SET_LOADING', false)
    }

    console.error('请求错误:', error)
    
    // 重试机制
    if (error.config && !error.config.__isRetryRequest) {
      return retryRequest(error)
    }

    // 处理响应错误
    return handleErrorResponse(error.response)
  }
)

/**
 * 重试请求
 * @param {Error} error - 错误对象
 * @returns {Promise} 重试后的Promise
 */
async function retryRequest(error) {
  const config = error.config
  
  // 设置最大重试次数
  const maxRetries = 2
  config.__retryCount = config.__retryCount || 0
  
  if (config.__retryCount >= maxRetries) {
    return Promise.reject(error)
  }

  config.__retryCount += 1
  config.__isRetryRequest = true
  
  console.log(`重试请求: ${config.url}, 重试次数: ${config.__retryCount}`)
  
  // 指数退避等待时间
  const delay = Math.pow(2, config.__retryCount) * 1000
  await new Promise(resolve => setTimeout(resolve, delay))
  
  return service(config)
}

/**
 * 处理响应错误
 * @param {Object} response - 响应对象
 * @returns {Promise} 返回Promise.reject
 */
function handleErrorResponse(response) {
  if (!response) {
    ElMessage.error('网络连接失败，请检查网络')
    return Promise.reject(new Error('网络连接失败'))
  }

  const { status, data } = response

  // 根据状态码处理错误
  switch (status) {
    case ERROR_CODES.UNAUTHORIZED:
      // Token过期或无效，清除用户信息并跳转到登录页
      store.dispatch('logout')
      ElMessage.error('登录已过期，请重新登录')
      window.location.href = '/login'
      break
      
    case ERROR_CODES.FORBIDDEN:
      ElMessage.error(ERROR_MESSAGES[ERROR_CODES.FORBIDDEN] || '权限不足')
      break
      
    case ERROR_CODES.NOT_FOUND:
      ElMessage.error(ERROR_MESSAGES[ERROR_CODES.NOT_FOUND] || '请求的资源不存在')
      break
      
    case ERROR_CODES.VALIDATION_ERROR:
      // 处理参数验证错误
      if (data.errors) {
        const firstError = Object.values(data.errors)[0][0]
        ElMessage.error(firstError)
      } else {
        ElMessage.error(data.msg || '参数验证失败')
      }
      break
      
    case ERROR_CODES.INTERNAL_ERROR:
      ElMessage.error(ERROR_MESSAGES[ERROR_CODES.INTERNAL_ERROR] || '服务器内部错误')
      break
      
    case ERROR_CODES.SERVICE_UNAVAILABLE:
      ElMessage.error(ERROR_MESSAGES[ERROR_CODES.SERVICE_UNAVAILABLE] || '服务暂时不可用')
      break
      
    default:
      ElMessage.error(data?.msg || ERROR_MESSAGES.DEFAULT)
  }

  store.commit('SET_ERROR', data?.msg || '请求失败')
  return Promise.reject(new Error(data?.msg || '请求失败'))
}

/**
 * GET请求方法
 * @param {string} url - 请求URL
 * @param {Object} params - 查询参数
 * @param {Object} config - 请求配置
 * @returns {Promise} 返回Promise
 */
export function get(url, params = {}, config = {}) {
  return service({
    method: 'get',
    url,
    params,
    ...config
  })
}

/**
 * POST请求方法
 * @param {string} url - 请求URL
 * @param {Object} data - 请求数据
 * @param {Object} config - 请求配置
 * @returns {Promise} 返回Promise
 */
export function post(url, data = {}, config = {}) {
  return service({
    method: 'post',
    url,
    data,
    ...config
  })
}

/**
 * PUT请求方法
 * @param {string} url - 请求URL
 * @param {Object} data - 请求数据
 * @param {Object} config - 请求配置
 * @returns {Promise} 返回Promise
 */
export function put(url, data = {}, config = {}) {
  return service({
    method: 'put',
    url,
    data,
    ...config
  })
}

/**
 * DELETE请求方法
 * @param {string} url - 请求URL
 * @param {Object} config - 请求配置
 * @returns {Promise} 返回Promise
 */
export function deleteRequest(url, config = {}) {
  return service({
    method: 'delete',
    url,
    ...config
  })
}

/**
 * PATCH请求方法
 * @param {string} url - 请求URL
 * @param {Object} data - 请求数据
 * @param {Object} config - 请求配置
 * @returns {Promise} 返回Promise
 */
export function patch(url, data = {}, config = {}) {
  return service({
    method: 'patch',
    url,
    data,
    ...config
  })
}

/**
 * 上传文件方法
 * @param {string} url - 上传URL
 * @param {FormData} formData - 表单数据
 * @param {function} onProgress - 上传进度回调
 * @param {Object} config - 请求配置
 * @returns {Promise} 返回Promise
 */
export function upload(url, formData, onProgress = null, config = {}) {
  return service({
    method: 'post',
    url,
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    onUploadProgress: onProgress ? (progressEvent) => {
      const progress = (progressEvent.loaded / progressEvent.total) * 100
      onProgress(Math.round(progress))
    } : null,
    ...config
  })
}

/**
 * 下载文件方法
 * @param {string} url - 下载URL
 * @param {string} filename - 文件名
 * @param {Object} config - 请求配置
 * @returns {Promise} 返回Promise
 */
export function download(url, filename = 'download', config = {}) {
  return service({
    method: 'get',
    url,
    responseType: 'blob',
    ...config
  }).then(response => {
    // 创建下载链接
    const url = window.URL.createObjectURL(new Blob([response.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', filename)
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
  })
}

/**
 * 取消请求方法
 * @param {Object} source - Axios CancelToken源
 * @returns {Object} 返回取消令牌
 */
export function cancelRequest() {
  const source = axios.CancelToken.source()
  return source
}

/**
 * 导出HTTP服务实例
 */
export { service }

/**
 * 请求方法汇总
 */
export default {
  get,
  post,
  put,
  delete: deleteRequest,
  patch,
  upload,
  download,
  service,
  cancelRequest
}