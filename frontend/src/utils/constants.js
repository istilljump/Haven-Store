/**
 * 应用常量配置
 * 包含各种常量定义
 */

// API基础配置
export const API_CONFIG = {
  BASE_URL: '/api',
  TIMEOUT: 10000,
  RETRY_COUNT: 2,
  AUTH_TOKEN_KEY: 'Authorization'
}

// 用户相关常量
export const USER_CONFIG = {
  TOKEN_KEY: 'access_token',
  USER_INFO_KEY: 'user_info',
  LOCALSTORAGE_KEYS: ['access_token', 'user_info'],
  DEFAULT_AVATAR: '/default-avatar.png'
}

// 商品相关常量
export const PRODUCT_CONFIG = {
  ITEMS_PER_PAGE: 10,
  MAX_ITEMS_PER_PAGE: 50,
  DEFAULT_CATEGORY: 1,
  DEFAULT_PAGE: 1,
  DEFAULT_PRICE_RANGE: [0, 10000],
  MAX_PRICE: 1000000,
  MAX_UPLOAD_SIZE: 5 * 1024 * 1024, // 5MB
  ALLOWED_IMAGE_TYPES: ['jpg', 'jpeg', 'png', 'gif', 'webp'],
  MAX_IMAGES_COUNT: 9
}

// 搜索相关常量
export const SEARCH_CONFIG = {
  MIN_KEYWORD_LENGTH: 1,
  MAX_KEYWORD_LENGTH: 50,
  MAX_HISTORY_COUNT: 10
}

// 地图相关常量
export const MAP_CONFIG = {
  DEFAULT_CENTER: [116.404, 39.915], // 北京
  DEFAULT_ZOOM: 12,
  MAX_RADIUS: 50, // 最大搜索半径（公里）
  MIN_RADIUS: 0.5, // 最小搜索半径（公里）
  DEFAULT_RADIUS: 5 // 默认搜索半径（公里）
}

// 页面配置
export const PAGE_CONFIG = {
  TITLES: {
    HOME: '首页',
    LOGIN: '登录',
    REGISTER: '注册',
    PRODUCTS: '商品列表',
    PRODUCT_DETAIL: '商品详情',
    PUBLISH: '发布商品',
    PROFILE: '个人中心',
    MESSAGES: '消息中心',
    ORDERS: '订单管理',
    FAVORITES: '我的收藏',
    ABOUT: '关于我们',
    ADMIN: '管理后台',
    NOT_FOUND: '页面未找到'
  },
  PAGINATION: {
    DEFAULT_PAGE: 1,
    DEFAULT_PAGE_SIZE: 10,
    PAGE_SIZE_OPTIONS: [10, 20, 50]
  }
}

// 时间配置
export const TIME_CONFIG = {
  FORMATS: {
    DATE: 'YYYY-MM-DD',
    TIME: 'HH:mm:ss',
    DATETIME: 'YYYY-MM-DD HH:mm:ss',
    SHORT_DATETIME: 'MM-DD HH:mm',
    TIME_ONLY: 'HH:mm'
  },
  RELATIVE_FORMATS: {
    NOW: '刚刚',
    MINUTES: '{n}分钟前',
    HOURS: '{n}小时前',
    DAYS: '{n}天前',
    OLDER: 'YYYY-MM-DD'
  },
  PAGINATION: {
    PAGE_SIZE: 20,
    MAX_AGE: 30 * 24 * 60 * 60 * 1000 // 30天
  }
}

// 颜色主题配置
export const THEME_CONFIG = {
  PRIMARY: '#409eff',
  SUCCESS: '#67c23a',
  WARNING: '#e6a23c',
  DANGER: '#f56c6c',
  INFO: '#909399',
  TEXT_PRIMARY: '#303133',
  TEXT_REGULAR: '#606266',
  TEXT_SECONDARY: '#909399',
  TEXT_PLACEHOLDER: '#c0c4cc',
  BORDER_COLOR: '#dcdfe6',
  BG_COLOR: '#f5f7fa'
}

// 状态配置
export const STATUS_CONFIG = {
  SUCCESS: 200,
  UNAUTHORIZED: 401,
  FORBIDDEN: 403,
  NOT_FOUND: 404,
  INTERNAL_ERROR: 500,
  BAD_REQUEST: 400,
  CONFLICT: 409
}

// 成色选项
export const CONDITION_OPTIONS = [
  { value: '全新', label: '全新' },
  { value: '九成新', label: '九成新' },
  { value: '八成新', label: '八成新' },
  { value: '七成新', label: '七成新' },
  { value: '七成新及以下', label: '七成新及以下' }
]

// 交易方式选项
export const TRADE_TYPE_OPTIONS = [
  { value: 'online', label: '在线交易' },
  { value: 'offline', label: '线下交易' },
  { value: 'both', label: '均可' }
]

// 商品状态选项
export const PRODUCT_STATUS_OPTIONS = [
  { value: 1, label: '在售', color: 'success' },
  { value: 2, label: '已售出', color: 'danger' },
  { value: 3, label: '已下架', color: 'info' }
]

// 分页配置
export const PAGINATION_OPTIONS = [
  { label: '10条/页', value: 10 },
  { label: '20条/页', value: 20 },
  { label: '50条/页', value: 50 }
]

// 搜索类型
export const SEARCH_TYPES = {
  TITLE: 'title',
  DESCRIPTION: 'description',
  USER: 'user',
  ALL: 'all'
}

// 消息类型
export const MESSAGE_TYPES = {
  SYSTEM: 'system',
  COMMENT: 'comment',
  COLLECT: 'collect',
  TRADE: 'trade',
  REPLY: 'reply'
}

// 订单状态
export const ORDER_STATUS = {
  PENDING: 1, // 待支付
  PAID: 2, // 已支付
  SHIPPED: 3, // 已发货
  RECEIVED: 4, // 已收货
  COMPLETED: 5, // 已完成
  CANCELLED: 6, // 已取消
  REFUNDED: 7 // 已退款
}

// 订单状态选项
export const ORDER_STATUS_OPTIONS = [
  { value: 1, label: '待支付', color: 'warning' },
  { value: 2, label: '已支付', color: 'primary' },
  { value: 3, label: '已发货', color: 'info' },
  { value: 4, label: '已收货', color: 'success' },
  { value: 5, label: '已完成', color: 'success' },
  { value: 6, label: '已取消', color: 'danger' },
  { value: 7, label: '已退款', color: 'info' }
]

// 文件类型配置
export const FILE_TYPES = {
  IMAGE: {
    extensions: ['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp'],
    mimeTypes: [
      'image/jpeg',
      'image/png',
      'image/gif',
      'image/webp',
      'image/bmp'
    ]
  },
  DOCUMENT: {
    extensions: ['pdf', 'doc', 'docx', 'txt'],
    mimeTypes: [
      'application/pdf',
      'application/msword',
      'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
      'text/plain'
    ]
  },
  VIDEO: {
    extensions: ['mp4', 'avi', 'mov'],
    mimeTypes: [
      'video/mp4',
      'video/x-msvideo',
      'video/quicktime'
    ]
  }
}

// 缓存键名
export const CACHE_KEYS = {
  SEARCH_HISTORY: 'search_history',
  FAVORITE_LIST: 'favorite_list',
  LOCATION: 'location',
  USER_SETTINGS: 'user_settings',
  PRODUCT_CATEGORIES: 'product_categories'
}

// 事件类型
export const EVENT_TYPES = {
  PAGE_VIEW: 'page_view',
  USER_LOGIN: 'user_login',
  USER_REGISTER: 'user_register',
  PRODUCT_PUBLISH: 'product_publish',
  PRODUCT_VIEW: 'product_view',
  PRODUCT_FAVORITE: 'product_favorite',
  ORDER_CREATE: 'order_create',
  PAYMENT_SUCCESS: 'payment_success'
}

// 错误码映射
export const ERROR_CODES = {
  SUCCESS: 200,
  UNAUTHORIZED: 401,
  FORBIDDEN: 403,
  NOT_FOUND: 404,
  VALIDATION_ERROR: 422,
  INTERNAL_ERROR: 500,
  SERVICE_UNAVAILABLE: 503
}

// 响应消息映射
export const ERROR_MESSAGES = {
  [ERROR_CODES.UNAUTHORIZED]: '登录已过期，请重新登录',
  [ERROR_CODES.FORBIDDEN]: '权限不足',
  [ERROR_CODES.NOT_FOUND]: '请求的资源不存在',
  [ERROR_CODES.VALIDATION_ERROR]: '参数验证失败',
  [ERROR_CODES.INTERNAL_ERROR]: '服务器内部错误',
  [ERROR_CODES.SERVICE_UNAVAILABLE]: '服务暂时不可用',
  DEFAULT: '请求失败，请稍后重试'
}