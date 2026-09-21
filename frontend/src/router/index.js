/**
 * 主路由文件
 */

import { createRouter, createWebHistory } from 'vue-router'

// 布局组件
import MainLayout from '@/layouts/MainLayout.vue'

// 引入路由模块
import homeRoutes from './modules/home'
import authRoutes from './modules/auth'
import productRoutes from './modules/product'
import userRoutes from './modules/user'

// 管理员路由
import adminRoutes from './admin'

// 404页面
import NotFound from '@/views/NotFound.vue'

const routes = [
  {
    path: '/',
    component: MainLayout,
    redirect: '/home',
    children: [
      ...homeRoutes,
      ...productRoutes,
      ...userRoutes
    ]
  },
  {
    path: '/auth',
    redirect: '/auth/login'
  },
  ...authRoutes,
  {
    path: '/404',
    name: 'NotFound',
    component: NotFound
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/404'
  },
  // 管理员路由
  ...adminRoutes
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    } else {
      return { top: 0 }
    }
  }
})

// 路由守卫
router.beforeEach((to, from, next) => {
  // 设置页面标题
  document.title = to.meta.title || '二手商品交易市场'
  next()
})

export default router