/**
 * 主路由文件
 */

import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'

import { getToken, getUserInfo } from '@/utils/auth'

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

/**
 * 全局路由守卫
 * <p>
 * 分三条线处理：
 *   1. 管理后台（/admin/**）：未登录跳管理登录页；已登录但非管理员拒绝；已登录管理员访问登录页自动进后台
 *   2. 需要登录的前台页（meta.requiresAuth）：未登录跳前台登录页，并带上回跳地址
 *   3. 其余页面：直接放行
 * <p>
 * 说明：登录态唯一来源是 utils/auth（localStorage），与登录页写入的是同一份数据，
 * 避免出现「守卫认为未登录、页面认为已登录」的错位。
 */
router.beforeEach((to, from, next) => {
  // 页面标题
  document.title = to.meta.title ? `${to.meta.title} - Haven-Store` : 'Haven-Store'

  const token = getToken()
  const userInfo = getUserInfo()
  const isAdmin = userInfo?.isAdmin === true

  // ---------- 管理后台 ----------
  if (to.path.startsWith('/admin')) {
    // 管理登录页：已登录的管理员不必再登录，直接进后台
    if (to.path === '/admin/login') {
      if (token && isAdmin) {
        next('/admin/dashboard')
        return
      }
      next()
      return
    }
    // 其余后台页：必须先登录且是管理员
    if (!token || !userInfo) {
      next('/admin/login')
      return
    }
    if (!isAdmin) {
      ElMessage.error('您没有管理员权限')
      next('/home')
      return
    }
    next()
    return
  }

  // ---------- 需要登录的前台页面 ----------
  if (to.meta.requiresAuth && !token) {
    next(`/auth/login?redirect=${encodeURIComponent(to.fullPath)}`)
    return
  }

  next()
})

export default router