/**
 * 验证工具函数
 * 包含各种常用的验证逻辑
 */

/**
 * 验证手机号
 * @param {string} phone - 手机号
 * @returns {boolean} 是否为有效的手机号
 */
export function isValidPhone(phone) {
  if (!phone) return false
  
  const phoneRegex = /^1[3-9]\d{9}$/
  return phoneRegex.test(phone)
}

/**
 * 验证用户名
 * @param {string} username - 用户名
 * @returns {boolean} 是否为有效的用户名
 */
export function isValidUsername(username) {
  if (!username) return false
  
  // 用户名长度3-20位，只能包含字母、数字、下划线
  const usernameRegex = /^[a-zA-Z0-9_]{3,20}$/
  return usernameRegex.test(username)
}

/**
 * 验证密码
 * @param {string} password - 密码
 * @param {number} minLength - 最小长度，默认6
 * @param {number} maxLength - 最大长度，默认20
 * @returns {boolean} 是否为有效的密码
 */
export function isValidPassword(password, minLength = 6, maxLength = 20) {
  if (!password) return false
  
  // 密码长度检查
  if (password.length < minLength || password.length > maxLength) {
    return false
  }
  
  // 简单密码强度检查：至少包含数字和字母
  const hasNumber = /\d/.test(password)
  const hasLetter = /[a-zA-Z]/.test(password)
  
  return hasNumber && hasLetter
}

/**
 * 验证邮箱
 * @param {string} email - 邮箱地址
 * @returns {boolean} 是否为有效的邮箱地址
 */
export function isValidEmail(email) {
  if (!email) return false
  
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  return emailRegex.test(email)
}

/**
 * 验证金额
 * @param {number|string} amount - 金额
 * @returns {boolean} 是否为有效的金额
 */
export function isValidAmount(amount) {
  if (typeof amount === 'string') {
    amount = parseFloat(amount)
  }
  
  return !isNaN(amount) && amount > 0 && amount <= 10000000
}

/**
 * 验证经纬度
 * @param {number} longitude - 经度
 * @param {number} latitude - 纬度
 * @returns {boolean} 是否为有效的经纬度
 */
export function isValidCoordinates(longitude, latitude) {
  // 验证经度范围：-180到180
  if (typeof longitude !== 'number' || longitude < -180 || longitude > 180) {
    return false
  }
  
  // 验证纬度范围：-90到90
  if (typeof latitude !== 'number' || latitude < -90 || latitude > 90) {
    return false
  }
  
  return true
}

/**
 * 验证URL
 * @param {string} url - URL地址
 * @returns {boolean} 是否为有效的URL
 */
export function isValidUrl(url) {
  if (!url) return false
  
  try {
    new URL(url)
    return true
  } catch {
    return false
  }
}

/**
 * 验证身份证号
 * @param {string} idCard - 身份证号
 * @returns {boolean} 是否为有效的身份证号
 */
export function isValidIdCard(idCard) {
  if (!idCard) return false
  
  const idCardRegex = /(^\d{15}$)|(^\d{18}$)|(^\d{17}(\d|X|x)$)/
  return idCardRegex.test(idCard)
}

/**
 * 验证QQ号
 * @param {string} qq - QQ号
 * @returns {boolean} 是否为有效的QQ号
 */
export function isValidQQ(qq) {
  if (!qq) return false
  
  const qqRegex = /^[1-9][0-9]{4,11}$/
  return qqRegex.test(qq)
}

/**
 * 验证微信号
 * @param {string} wechat - 微信号
 * @returns {boolean} 是否为有效的微信号
 */
export function isValidWechat(wechat) {
  if (!wechat) return false
  
  const wechatRegex = /^[a-zA-Z][a-zA-Z0-9_-]{5,19}$/
  return wechatRegex.test(wechat)
}

/**
 * 验证IP地址
 * @param {string} ip - IP地址
 * @returns {boolean} 是否为有效的IP地址
 */
export function isValidIP(ip) {
  if (!ip) return false
  
  const ipRegex = /^(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])$/
  return ipRegex.test(ip)
}

/**
 * 验证端口
 * @param {number} port - 端口号
 * @returns {boolean} 是否为有效的端口号
 */
export function isValidPort(port) {
  return typeof port === 'number' && port >= 1 && port <= 65535
}

/**
 * 验证图片URL
 * @param {string} url - 图片URL
 * @returns {boolean} 是否为有效的图片URL
 */
export function isValidImageUrl(url) {
  if (!url) return false
  
  const imageExtensions = ['.jpg', '.jpeg', '.png', '.gif', '.webp', '.bmp']
  const hasValidExtension = imageExtensions.some(ext => url.toLowerCase().endsWith(ext))
  
  if (!hasValidExtension) {
    return false
  }
  
  return isValidUrl(url)
}

/**
 * 验证分页参数
 * @param {number} page - 页码
 * @param {number} pageSize - 每页条数
 * @param {number} maxPageSize - 最大每页条数，默认50
 * @returns {boolean} 是否为有效的分页参数
 */
export function isValidPagination(page, pageSize, maxPageSize = 50) {
  const pageNum = parseInt(page)
  const size = parseInt(pageSize)
  
  return (
    pageNum >= 1 &&
    size >= 1 &&
    size <= maxPageSize
  )
}

/**
 * 验证商品价格区间
 * @param {number} minPrice - 最低价
 * @param {number} maxPrice - 最高价
 * @returns {boolean} 是否为有效的价格区间
 */
export function isValidPriceRange(minPrice, maxPrice) {
  const min = parseFloat(minPrice)
  const max = parseFloat(maxPrice)
  
  return (
    min > 0 &&
    max > 0 &&
    min <= max &&
    min <= 1000000 &&
    max <= 1000000
  )
}

/**
 * 验证搜索关键词
 * @param {string} keyword - 关键词
 * @param {number} minLength - 最小长度，默认1
 * @param {number} maxLength - 最大长度，默认50
 * @returns {boolean} 是否为有效的搜索关键词
 */
export function isValidKeyword(keyword, minLength = 1, maxLength = 50) {
  if (!keyword) return false
  
  const length = keyword.trim().length
  return length >= minLength && length <= maxLength
}

/**
 * 验证文本是否包含敏感词
 * @param {string} text - 文本
 * @param {Array<string>} sensitiveWords - 敏感词列表
 * @returns {boolean} 是否包含敏感词
 */
export function containsSensitiveWords(text, sensitiveWords = []) {
  if (!text || !sensitiveWords.length) return false
  
  const textLower = text.toLowerCase()
  return sensitiveWords.some(word => 
    word && textLower.includes(word.toLowerCase())
  )
}

/**
 * 验证文件大小
 * @param {number} size - 文件大小（字节）
 * @param {number} maxSize - 最大大小（字节），默认10MB
 * @returns {boolean} 是否为有效的文件大小
 */
export function isValidFileSize(size, maxSize = 10 * 1024 * 1024) {
  return typeof size === 'number' && size > 0 && size <= maxSize
}

/**
 * 验证文件类型
 * @param {string} filename - 文件名
 * @param {Array<string>} allowedTypes - 允许的文件类型
 * @returns {boolean} 是否为允许的文件类型
 */
export function isValidFileType(filename, allowedTypes = []) {
  if (!filename) return false
  
  const extension = filename.split('.').pop()?.toLowerCase()
  return extension && allowedTypes.includes(extension)
}