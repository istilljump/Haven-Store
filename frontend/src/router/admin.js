/**
 * 管理员路由配置
 * <p>
 * 注意：必须默认导出一个「路由数组」。
 * router/index.js 中是 `...adminRoutes` 展开合并的，
 * 若导出单个对象会抛 `adminRoutes is not iterable`，导致整个应用白屏。
 */

export default [
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    redirect: '/admin/login',
    children: [
      {
        path: 'login',
        name: 'AdminLogin',
        component: () => import('@/views/admin/AdminLogin.vue'),
        meta: {
          title: '管理员登录',
          requiresAuth: false
        }
      },
      {
        path: 'dashboard',
        name: 'AdminDashboard',
        component: () => import('@/views/admin/AdminDashboard.vue'),
        meta: {
          title: '管理后台首页',
          requiresAuth: true,
          requiresAdmin: true
        }
      },
      {
        path: 'users',
        name: 'AdminUsers',
        component: () => import('@/views/admin/AdminUsers.vue'),
        meta: {
          title: '用户管理',
          requiresAuth: true,
          requiresAdmin: true
        }
      },
      {
        path: 'products',
        name: 'AdminProducts',
        component: () => import('@/views/admin/AdminProducts.vue'),
        meta: {
          title: '商品管理',
          requiresAuth: true,
          requiresAdmin: true
        }
      },
      {
        path: 'categories',
        name: 'AdminCategories',
        component: () => import('@/views/admin/AdminCategories.vue'),
        meta: {
          title: '分类管理',
          requiresAuth: true,
          requiresAdmin: true
        }
      },
      {
        path: 'messages',
        name: 'AdminMessages',
        component: () => import('@/views/admin/AdminMessages.vue'),
        meta: {
          title: '系统消息',
          requiresAuth: true,
          requiresAdmin: true
        }
      },
      {
        path: 'settings',
        name: 'AdminSettings',
        component: () => import('@/views/admin/AdminSettings.vue'),
        meta: {
          title: '系统设置',
          requiresAuth: true,
          requiresAdmin: true
        }
      }
    ]
  }
]
