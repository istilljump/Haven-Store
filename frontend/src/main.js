import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
// 轮6：Element Plus 官方暗色变量表（挂 html.dark，dark.css 在其上精调）
import 'element-plus/theme-chalk/dark/css-vars.css'
// UI美化：品牌资产 Token 层（基础品牌 Token，可整层热替换）
import './styles/brand-tokens.css'
// UI美化：全局视觉主题（设计变量 + Element Plus 组件统一观感），纯样式层，无业务逻辑
import './styles/theme.css'
// UI美化：暗色主题体系（界面级暗色色阶 + 组件暗色适配）
import './styles/dark.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
// 轮6：主题三模式基础设施（完全隔离，无业务依赖；恢复持久化的主题选择）
import { initThemeMode } from './theme/theme-mode'
import router from './router'
import App from './App.vue'

// 恢复用户上次选择的主题模式（system/light/dark，localStorage 持久化）
initThemeMode()

const app = createApp(App)
const pinia = createPinia()

// 注册Element Plus图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(pinia)
// 指定中文语言包：否则 MessageBox 的按钮会显示 OK / Cancel，
// 分页组件也会出现 "Total 2"、"12/page" 这类英文文案
app.use(ElementPlus, { locale: zhCn })
app.use(router)

app.mount('#app')
