/**
 * 验证相关工具函数
 */

/**
 * 验证邮箱格式
 * @param {string} email - 邮箱地址
 * @returns {boolean} 是否为有效邮箱
 */
export function validateEmail(email) {
  if (!email) return false
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  return emailRegex.test(email)
}

/**
 * 验证手机号格式（中国大陆手机号）
 * @param {string} phone - 手机号
 * @returns {boolean} 是否为有效手机号
 */
export function validatePhone(phone) {
  if (!phone) return false
  const phoneRegex = /^1[3-9]\d{9}$/
  return phoneRegex.test(phone)
}

/**
 * 验证密码强度
 * @param {string} password - 密码
 * @param {string} strength - 密码强度要求 'low'|'medium'|'high'
 * @returns {boolean} 密码是否符合要求
 */
export function validatePassword(password, strength = 'medium') {
  if (!password) return false
  
  switch (strength) {
    case 'low':
      return password.length >= 6
    case 'medium':
      return password.length >= 8 && /[a-zA-Z]/.test(password) && /\d/.test(password)
    case 'high':
      return password.length >= 8 && 
             /[a-zA-Z]/.test(password) && 
             /\d/.test(password) && 
             /[!@#$%^&*(),.?":{}|<>]/.test(password)
    default:
      return password.length >= 6
  }
}

/**
 * 验证用户名格式
 * @param {string} username - 用户名
 * @returns {boolean} 是否为有效用户名
 */
export function validateUsername(username) {
  if (!username) return false
  // 3-20个字符，只能包含字母、数字、下划线
  const usernameRegex = /^[a-zA-Z0-9_]{3,20}$/
  return usernameRegex.test(username)
}

/**
 * 验证昵称格式
 * @param {string} nickname - 昵称
 * @returns {boolean} 是否为有效昵称
 */
export function validateNickname(nickname) {
  if (!nickname) return false
  // 2-20个字符，不支持特殊字符
  const nicknameRegex = /^[\u4e00-\u9fa5a-zA-Z0-9_]{2,20}$/
  return nicknameRegex.test(nickname)
}

/**
 * 验证身份证号格式
 * @param {string} idCard - 身份证号
 * @returns {boolean} 是否为有效身份证号
 */
export function validateIdCard(idCard) {
  if (!idCard) return false
  const idCardRegex = /(^\d{15}$)|(^\d{18}$)|(^\d{17}(\d|X|x)$)/
  return idCardRegex.test(idCard)
}

/**
 * 验证QQ号格式
 * @param {string} qq - QQ号
 * @returns {boolean} 是否为有效QQ号
 */
export function validateQQ(qq) {
  if (!qq) return false
  // 5-12位数字
  const qqRegex = /^[1-9][0-9]{4,11}$/
  return qqRegex.test(qq)
}

/**
 * 验证URL格式
 * @param {string} url - URL地址
 * @returns {boolean} 是否为有效URL
 */
export function validateUrl(url) {
  if (!url) return false
  try {
    new URL(url)
    return true
  } catch {
    return false
  }
}

/**
 * 验证IP地址格式
 * @param {string} ip - IP地址
 * @returns {boolean} 是否为有效IP地址
 */
export function validateIp(ip) {
  if (!ip) return false
  const ipRegex = /^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/
  return ipRegex.test(ip)
}

/**
 * 验证银行卡号格式
 * @param {string} cardNumber - 银行卡号
 * @returns {boolean} 是否为有效银行卡号
 */
export function validateBankCard(cardNumber) {
  if (!cardNumber) return false
  // 16-19位数字
  const cardRegex = /^[0-9]{16,19}$/
  return cardRegex.test(cardNumber)
}

/**
 * 验证邮政编码格式
 * @param {string} zipCode - 邮政编码
 * @returns {boolean} 是否为有效邮政编码
 */
export function validateZipCode(zipCode) {
  if (!zipCode) return false
  // 6位数字
  const zipRegex = /^[0-9]{6}$/
  return zipRegex.test(zipCode)
}

/**
 * 验证日期格式
 * @param {string} date - 日期字符串
 * @param {string} format - 日期格式
 * @returns {boolean} 是否为有效日期
 */
export function validateDate(date, format = 'YYYY-MM-DD') {
  if (!date) return false
  const dateRegex = /^\d{4}-\d{2}-\d{2}$/
  if (!dateRegex.test(date)) return false
  
  const [year, month, day] = date.split('-').map(Number)
  const d = new Date(year, month - 1, day)
  
  return d.getFullYear() === year && 
         d.getMonth() === month - 1 && 
         d.getDate() === day
}