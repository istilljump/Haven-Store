/**
 * 消息路由配置（私信中心 + 系统通知中心）
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
  },
  {
    // 通知中心：系统公告与通知的收件箱
    path: 'notifications',
    name: 'Notifications',
    component: () => import('@/views/user/Notifications.vue'),
    meta: {
      title: '通知中心',
      requiresAuth: true
    }
  }
]

export default messageRoutes
