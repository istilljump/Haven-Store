/**
 * ============================================================
 * 主题模式基础设施（轮6 新增 · 完全隔离，零业务依赖）
 * ============================================================
 * 职责：三模式（system / light / dark）的主题模式管理与持久化。
 *   - system：跟随系统 prefers-color-scheme，实时监听系统切换
 *   - light：固定亮色
 *   - dark：固定暗色
 * 实现方式：切换 <html> 根元素的 .dark 类（Element Plus 官方暗色
 *   变量表与本站 dark.css 均挂在该类下），不触碰任何业务代码。
 *
 * 持久化：localStorage key `haven_theme_mode`，刷新后恢复上次选择。
 *
 * 使用方式（main.js）：import { initThemeMode } 后调用一次即可。
 */

const THEME_MODE_KEY = 'haven_theme_mode'

/**
 * [待办移交] 暗色总开关（前端常量版）
 * 设为 false 时：忽略持久化选择、强制亮色、隐藏切换入口；
 * 一行配置即可全局关闭暗色能力（可开关、可回退）。
 * 说明：用户已审定「后台设置总开关」，该形态需扩展后端
 * SystemSettingDTO + PublicSettingController + 管理端设置页字段，
 * 涉及业务代码，待明确授权后在后续轮次实施；当前先以此常量兜底。
 */
export const THEME_FEATURE_ENABLED = true

/** 三种合法模式 */
export const THEME_MODES = ['system', 'light', 'dark']

/** 系统暗色偏好媒体查询 */
const systemDarkQuery = window.matchMedia ? window.matchMedia('(prefers-color-scheme: dark)') : null

/** 读取当前模式（非法值回退 system；总开关关闭时强制 system 以解析为亮色兜底） */
export function getThemeMode() {
  if (!THEME_FEATURE_ENABLED) return 'light'
  try {
    const mode = localStorage.getItem(THEME_MODE_KEY)
    return THEME_MODES.includes(mode) ? mode : 'system'
  } catch (e) {
    return 'system'
  }
}

/** 根据模式计算是否应处于暗色 */
function resolveDark(mode) {
  if (mode === 'dark') return true
  if (mode === 'light') return false
  return systemDarkQuery ? systemDarkQuery.matches : false
}

/** 应用当前模式到 <html> 根元素（唯一副作用点） */
export function applyThemeMode() {
  const mode = getThemeMode()
  document.documentElement.classList.toggle('dark', resolveDark(mode))
  document.documentElement.setAttribute('data-theme-mode', mode)
}

/** 设置模式并持久化、立即应用（总开关关闭时不接受设置） */
export function setThemeMode(mode) {
  if (!THEME_FEATURE_ENABLED) return
  if (!THEME_MODES.includes(mode)) return
  try {
    localStorage.setItem(THEME_MODE_KEY, mode)
  } catch (e) {
    /* 存储不可用时仅本次会话内生效 */
  }
  applyThemeMode()
}

/**
 * 初始化：恢复持久化模式 + 监听系统偏好变化
 * 仅当用户处于 system 模式时，系统切换才实时跟随
 */
export function initThemeMode() {
  applyThemeMode()
  if (systemDarkQuery) {
    systemDarkQuery.addEventListener('change', () => {
      if (getThemeMode() === 'system') {
        applyThemeMode()
      }
    })
  }
}
