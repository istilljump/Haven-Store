<template>
  <!-- 主题切换按钮：三模式循环（跟随系统 → 亮色 → 暗色） -->
  <!-- [审定转正] 入口位置：前台/后台顶栏图标钮（用户已确认） -->
  <el-tooltip v-if="FEATURE_ENABLED" :content="tipText" placement="bottom">
    <button
      class="theme-toggle"
      type="button"
      :aria-label="tipText"
      @click="cycleMode"
    >
      <el-icon><component :is="modeIcon" /></el-icon>
    </button>
  </el-tooltip>
</template>

<script setup>
/**
 * 主题三模式切换按钮（轮6 新增 · 主题基础设施，不含任何业务逻辑）
 * <p>
 * 图标随模式变化：跟随系统 = Monitor / 亮色 = Sunny / 暗色 = Moon；
 * 点击循环切换，状态由 theme-mode.js 持久化到 localStorage。
 */
import { computed, onBeforeUnmount, ref } from 'vue'
import { Monitor, Moon, Sunny } from '@element-plus/icons-vue'
import { THEME_FEATURE_ENABLED, THEME_MODES, applyThemeMode, getThemeMode, setThemeMode } from './theme-mode'

/** 总开关：与 theme-mode.js 的 THEME_FEATURE_ENABLED 同源，关闭时入口整体不渲染 */
const FEATURE_ENABLED = THEME_FEATURE_ENABLED

const MODE_META = {
  system: { icon: Monitor, tip: '主题：跟随系统' },
  light: { icon: Sunny, tip: '主题：亮色' },
  dark: { icon: Moon, tip: '主题：暗色' }
}

const mode = ref(getThemeMode())

const modeIcon = computed(() => MODE_META[mode.value].icon)
const tipText = computed(() => MODE_META[mode.value].tip)

/** 循环切换：system → light → dark → system */
const cycleMode = () => {
  const idx = THEME_MODES.indexOf(mode.value)
  const next = THEME_MODES[(idx + 1) % THEME_MODES.length]
  mode.value = next
  setThemeMode(next)
}

/* 同步系统偏好变化（system 模式下由 theme-mode.js 切换根类，此处仅刷新图标状态） */
const onSystemChange = () => {
  applyThemeMode()
}
window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', onSystemChange)
onBeforeUnmount(() => {
  window.matchMedia('(prefers-color-scheme: dark)').removeEventListener('change', onSystemChange)
})
</script>

<style scoped>
/* [UI美化-轮6] 主题切换按钮：与顶栏汉堡按钮同规格的无边框图标钮 */
.theme-toggle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  padding: 0;
  border: none;
  border-radius: 999px;
  background: transparent;
  color: #606266;
  font-size: 19px;
  cursor: pointer;
  transition:
    color 0.2s ease,
    background-color 0.2s ease;
}

.theme-toggle:hover {
  color: #409eff;
  background: #f0f7ff;
}

/* 暗色下的按钮观感（全局 dark.css 无法触达 scoped 值，此处用变量双写兜底） */
html.dark .theme-toggle {
  color: #c3cad6;
  background: transparent;
}
html.dark .theme-toggle:hover {
  color: var(--hs-primary-light, #6cb8ff);
  background: rgba(77, 166, 255, 0.12);
}
</style>
