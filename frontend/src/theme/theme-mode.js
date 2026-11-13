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
 * 总开关：后台「交易设置 → 外观设置 → 暗色模式」（themeDarkEnabled，
 * 轮7 授权扩展）。前端在 initThemeMode 内异步拉取公开配置：
 *   - 开启（缺省）：三模式能力正常；
 *   - 关闭：强制亮色 + 隐藏切换入口（订阅机制通知 UI）。
 * 另保留 THEME_FEATURE_ENABLED 常量作为代码级兜底开关（一行可关闭）。
 *
 * 主题变更广播：applyThemeMode 每次应用后派发 window 事件
 * `haven-theme-change`，供 echarts 等按主题渲染的消费方重绘。
 *
 * 使用方式（main.js）：import { initThemeMode } 后调用一次即可。
 */

const THEME_MODE_KEY = 'haven_theme_mode'

/**
 * 暗色能力代码级兜底开关：设为 false 时忽略持久化选择、强制亮色、
 * 隐藏切换入口。后台总开关（themeDarkEnabled）在本文件内异步拉取并
 * 叠加于此常量之上（两者任一为 false 即整体关闭）。
 */
export const THEME_FEATURE_ENABLED = true

/** 三种合法模式 */
export const THEME_MODES = ['system', 'light', 'dark']

/** 系统暗色偏好媒体查询 */
const systemDarkQuery = window.matchMedia ? window.matchMedia('(prefers-color-scheme: dark)') : null

/** 远端总开关运行时状态（缺省开启；initThemeMode 异步同步后台配置） */
let remoteFeatureEnabled = true
const featureListeners = []

/** 读取暗色能力总状态（代码兜底开关 && 后台总开关） */
export function getThemeFeatureEnabled() {
  return THEME_FEATURE_ENABLED && remoteFeatureEnabled
}

/** 订阅总开关变化（ThemeToggle 用于显隐入口） */
export function subscribeThemeFeature(cb) {
  if (typeof cb === 'function') featureListeners.push(cb)
}

function notifyFeature() {
  featureListeners.forEach((cb) => {
    try {
      cb(getThemeFeatureEnabled())
    } catch (e) {
      /* 订阅者异常不影响主题应用 */
    }
  })
}

/** 读取当前模式（非法值回退 system；总开关关闭时强制亮色） */
export function getThemeMode() {
  if (!getThemeFeatureEnabled()) return 'light'
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

/** 应用当前模式到 <html> 根元素（唯一副作用点），并广播主题变更事件 */
export function applyThemeMode() {
  const mode = getThemeMode()
  const dark = resolveDark(mode)
  document.documentElement.classList.toggle('dark', dark)
  document.documentElement.setAttribute('data-theme-mode', mode)
  window.dispatchEvent(new CustomEvent('haven-theme-change', { detail: { mode, dark } }))
}

/** 设置模式并持久化、立即应用（总开关关闭时不接受设置） */
export function setThemeMode(mode) {
  if (!getThemeFeatureEnabled()) return
  if (!THEME_MODES.includes(mode)) return
  try {
    localStorage.setItem(THEME_MODE_KEY, mode)
  } catch (e) {
    /* 存储不可用时仅本次会话内生效 */
  }
  applyThemeMode()
}

/** 同步后台总开关状态（themeDarkEnabled 缺省开启） */
function setRemoteFeatureEnabled(enabled) {
  const changed = remoteFeatureEnabled !== enabled
  remoteFeatureEnabled = enabled
  applyThemeMode()
  if (changed) notifyFeature()
}

/**
 * 初始化：恢复持久化模式 + 监听系统偏好变化 + 异步同步后台总开关
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
  // 后台总开关：异步拉取公开配置，不阻塞首屏；失败保持缺省开启
  fetch('/api/public/settings')
    .then((r) => r.json())
    .then((res) => {
      const enabled = !(res && res.data && res.data.themeDarkEnabled === false)
      setRemoteFeatureEnabled(enabled)
    })
    .catch(() => {
      /* 配置拉取失败保持缺省开启 */
    })
}
