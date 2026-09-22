/**
 * 首页路由配置
 */
const homeRoutes = [
  {
    path: 'home',
    name: 'Home',
    component: () => import('@/views/Home.vue'),
    meta: {
      title: '首页',
      requiresAuth: false
    }
  }
]

export default homeRoutes