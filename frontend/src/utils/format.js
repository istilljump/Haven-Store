/**
 * 格式化相关工具函数
 */

/**
 * 格式化日期时间
 * @param {string|Date} date - 日期时间
 * @param {string} format - 格式化模式，默认 'YYYY-MM-DD HH:mm:ss'
 * @returns {string} 格式化后的日期时间字符串
 */
export function formatDate(date, format = 'YYYY-MM-DD HH:mm:ss') {
  if (!date) return ''
  
  const d = new Date(date)
  if (isNaN(d.getTime())) return date
  
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hours = String(d.getHours()).padStart(2, '0')
  const minutes = String(d.getMinutes()).padStart(2, '0')
  const seconds = String(d.getSeconds()).padStart(2, '0')
  
  return format
    .replace('YYYY', year)
    .replace('MM', month)
    .replace('DD', day)
    .replace('HH', hours)
    .replace('mm', minutes)
    .replace('ss', seconds)
}

/**
 * 格式化相对时间（相对于现在）
 * @param {string|Date} date - 日期时间
 * @returns {string} 相对时间描述
 */
export function formatRelativeTime(date) {
  if (!date) return ''
  
  const now = new Date()
  const target = new Date(date)
  const diffMs = now - target
  const diffSeconds = Math.floor(diffMs / 1000)
  const diffMinutes = Math.floor(diffSeconds / 60)
  const diffHours = Math.floor(diffMinutes / 60)
  const diffDays = Math.floor(diffHours / 24)
  const diffMonths = Math.floor(diffDays / 30)
  const diffYears = Math.floor(diffMonths / 12)
  
  if (diffSeconds < 60) {
    return '刚刚'
  } else if (diffMinutes < 60) {
    return `${diffMinutes}分钟前`
  } else if (diffHours < 24) {
    return `${diffHours}小时前`
  } else if (diffDays < 7) {
    return `${diffDays}天前`
  } else if (diffDays < 30) {
    return `${Math.floor(diffDays / 7)}周前`
  } else if (diffMonths < 12) {
    return `${diffMonths}个月前`
  } else {
    return `${diffYears}年前`
  }
}

/**
 * 格式化文件大小
 * @param {number} bytes - 字节数
 * @param {number} decimals - 小数位数，默认2
 * @returns {string} 格式化后的文件大小
 */
export function formatFileSize(bytes, decimals = 2) {
  if (bytes === 0) return '0 Bytes'
  
  const k = 1024
  const dm = decimals < 0 ? 0 : decimals
  const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB']
  
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  
  return parseFloat((bytes / Math.pow(k, i)).toFixed(dm)) + ' ' + sizes[i]
}

/**
 * 格式化金额
 * @param {number} amount - 金额
 * @param {number} decimals - 小数位数，默认2
 * @param {string} currency - 货币符号，默认¥
 * @returns {string} 格式化后的金额
 */
export function formatMoney(amount, decimals = 2, currency = '¥') {
  return `${currency}${Number(amount).toFixed(decimals).replace(/\B(?=(\d{3})+(?!\d))/g, ',')}`
}

/**
 * 格式化手机号
 * @param {string} phone - 手机号
 * @returns {string} 格式化后的手机号
 */
export function formatPhone(phone) {
  if (!phone) return ''
  return phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2')
}

/**
 * 格式化邮箱
 * @param {string} email - 邮箱
 * @returns {string} 格式化后的邮箱
 */
export function formatEmail(email) {
  if (!email) return ''
  const [name, domain] = email.split('@')
  if (name.length <= 2) return email
  return `${name.charAt(0)}***${name.charAt(name.length - 1)}@${domain}`
}