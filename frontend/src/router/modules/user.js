/**
 * 用户路由配置
 */
const userRoutes = [
  {
    // 用户管理属于管理后台功能，统一收敛到 /admin/users。
    // 此前这里是一个独立的「用户列表」页，数据是硬编码假数据，且「添加用户/查看/编辑」
    // 三个按钮都跳向不存在的路由；更严重的是它挂在需要登录的后台之外，
    // 未登录用户也能打开新增用户表单。改为重定向后旧链接依然可用，且受后台守卫保护。
    path: 'users',
    name: 'Users',
    redirect: '/admin/users'
  },
  {
    path: 'profile',
    name: 'Profile',
    component: () => import('@/views/user/UserProfile.vue'),
    meta: {
      title: '个人资料',
      requiresAuth: true
    }
  }
]

export default userRoutes