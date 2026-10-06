/**
 * 前台公开配置API接口
 * 免登录读取站点信息与行为开关（上传限制、AI 估价开关等）
 */

import request from '@/utils/request'

/**
 * 获取公开配置
 * @returns {Promise} 返回 {siteName, siteDescription, siteLogo, uploadMaxFileSizeKB, uploadMaxImages, autoEstimate}
 */
export function getPublicSettings() {
  return request.get('/public/settings', {}, { loading: false })
}

/**
 * 默认导出
 */
export default {
  getPublicSettings
}
