/**
 * 私信路由配置
 */
const messageRoutes = [
  {
    // 私信中心：左侧会话列表 + 右侧聊天窗口
    path: 'messages',
    name: 'Messages',
    component: () => import('@/views/user/MessageCenter.vue'),
    meta: {
      title: '我的私信',
      requiresAuth: true
    }
  }
]

export default messageRoutes
