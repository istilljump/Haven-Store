/**
 * 用户路由配置
 */
const userRoutes = [
  {
    path: 'users',
    name: 'Users',
    component: () => import('@/views/user/UserList.vue'),
    meta: {
      title: '用户列表',
      requiresAuth: false
    }
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