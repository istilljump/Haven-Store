import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import router from './router'
import App from './App.vue'

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