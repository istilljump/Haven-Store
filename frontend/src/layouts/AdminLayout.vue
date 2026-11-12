<template>
  <div class="admin-layout">
    <!-- 登录页面 -->
    <router-view v-if="$route.path === '/admin/login'" />

    <!-- 管理后台布局 -->
    <el-container v-else>
      <!-- 顶部导航 -->
      <el-header class="admin-header">
        <div class="header-left">
          <!-- 移动端侧边栏开关（此前侧滑样式写好了但没有触发按钮） -->
          <el-icon class="sidebar-toggle" @click="sidebarOpen = !sidebarOpen"><Fold /></el-icon>
          <div class="logo" @click="goToDashboard">
            <el-icon><Monitor /></el-icon>
            <span>Haven-Store</span>
          </div>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item>
              <el-icon><HomeFilled /></el-icon>
              管理后台
            </el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentRouteTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <div class="user-info">
              <el-avatar :src="userInfo.avatar || '/default-avatar.png'" />
              <span class="username">{{ userInfo.nickname || userInfo.username }}</span>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>
                  个人资料
                </el-dropdown-item>
                <el-dropdown-item command="settings">
                  <el-icon><Setting /></el-icon>
                  系统设置
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 主体区域 -->
      <el-container class="admin-container">
        <!-- 侧边栏：移动端通过 .open 类滑入 -->
        <el-aside width="240px" class="admin-sidebar" :class="{ open: sidebarOpen }">
          <div class="sidebar-mask" @click="sidebarOpen = false" />
          <el-menu
            :default-active="activeMenu"
            :router="true"
            class="admin-menu"
            background-color="#001529"
            text-color="#fff"
            active-text-color="#409eff"
          >
            <el-menu-item index="/admin/dashboard">
              <el-icon><Odometer /></el-icon>
              <span>数据概览</span>
            </el-menu-item>

            <el-menu-item index="/admin/users">
              <el-icon><User /></el-icon>
              <span>用户管理</span>
            </el-menu-item>

            <el-menu-item index="/admin/products">
              <el-icon><Goods /></el-icon>
              <span>商品管理</span>
            </el-menu-item>

            <el-menu-item index="/admin/orders">
              <el-icon><Tickets /></el-icon>
              <span>订单管理</span>
            </el-menu-item>

            <el-menu-item index="/admin/categories">
              <el-icon><Collection /></el-icon>
              <span>分类管理</span>
            </el-menu-item>

            <el-menu-item index="/admin/comments">
              <el-icon><ChatLineSquare /></el-icon>
              <span>评论管理</span>
            </el-menu-item>

            <el-menu-item index="/admin/reports">
              <el-icon><WarningFilled /></el-icon>
              <span>举报处理</span>
            </el-menu-item>

            <el-menu-item index="/admin/messages">
              <el-icon><Bell /></el-icon>
              <span>系统消息</span>
            </el-menu-item>

            <el-menu-item index="/admin/settings">
              <el-icon><Setting /></el-icon>
              <span>系统设置</span>
            </el-menu-item>
          </el-menu>
        </el-aside>

        <!-- 主内容区域 -->
        <el-main class="admin-main">
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Monitor,
  HomeFilled,
  ArrowDown,
  Fold,
  User,
  Setting,
  SwitchButton,
  Odometer,
  Goods,
  Tickets,
  ChatLineSquare,
  WarningFilled,
  Collection,
  Bell
} from '@element-plus/icons-vue'
import { useUserStore } from '@/store/index'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// 移动端侧边栏滑出开关
const sidebarOpen = ref(false)

// 当前登录用户信息
// 身份统一取自 store（响应式）：/admin/login 与其它 /admin/** 页面共用同一个
// AdminLayout 实例，管理员登录后布局不会重新挂载，若读的是 localStorage 快照，
// 顶栏就会一直停在上一个账号的昵称上（刷新才恢复）
const userInfo = computed(() => userStore.user || {})

// 当前路由标题
const currentRouteTitle = computed(() => {
  const routeMap = {
    '/admin/dashboard': '数据概览',
    '/admin/users': '用户管理',
    '/admin/products': '商品管理',
    '/admin/orders': '订单管理',
    '/admin/categories': '分类管理',
    '/admin/comments': '评论管理',
    '/admin/reports': '举报处理',
    '/admin/messages': '系统消息',
    '/admin/settings': '系统设置'
  }
  return routeMap[route.path] || '管理后台'
})

// 当前激活的菜单
const activeMenu = computed(() => {
  return route.path
})

// 路由跳转
const goToDashboard = () => {
  router.push('/admin/dashboard')
}

// 下拉菜单命令处理
const handleCommand = (command) => {
  switch (command) {
    case 'profile':
      // 管理员也有前台个人资料页（改资料/改密码/头像）
      router.push('/profile')
      break
    case 'settings':
      router.push('/admin/settings')
      break
    case 'logout':
      handleLogout()
      break
  }
}

// 退出登录
const handleLogout = () => {
  ElMessageBox.confirm('确定要退出管理员账号吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(() => {
      // 走 store 统一清理：Token、用户信息与各类未读角标一起归零，
      // 否则下一位登录者会看到上一位的角标数字
      userStore.clearUser()
      ElMessage.success('退出成功')
      router.push('/admin/login')
    })
    .catch(() => {})
}

// 说明：/admin/** 的登录态与管理员角色校验已统一收敛到 router/index.js 的全局守卫，
// 组件内不再重复注册守卫（此前每次挂载都注册一遍，且用的是错误的 localStorage key）
</script>

<style scoped>
.admin-layout {
  height: 100vh;
  background: #f0f2f5;
}

/* [UI美化] 顶栏：薄影替代硬边框，观感更轻盈 */
.admin-header {
  height: 64px;
  background: #fff;
  border-bottom: 1px solid transparent;
  box-shadow: 0 1px 4px rgba(16, 24, 40, 0.06);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  position: relative;
  z-index: 10;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-size: 18px;
  font-weight: 600;
  color: #001529;
  transition: opacity 0.2s ease;
}

.logo:hover {
  opacity: 0.85;
}

/* [UI美化-本轮] 品牌徽标：Logo 图标淡蓝圆角托底 */
.logo .el-icon {
  font-size: 20px;
  width: 34px;
  height: 34px;
  border-radius: 8px;
  background: #ecf5ff;
  color: #409eff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.header-right {
  display: flex;
  align-items: center;
}

/* [UI美化] 用户信息：胶囊 hover 反馈 */
.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 14px;
  border-radius: 999px;
  cursor: pointer;
  transition: background 0.2s ease;
}

.user-info:hover {
  background: #f5f7fa;
}

.username {
  font-size: 14px;
  color: #606266;
}

.admin-container {
  height: calc(100vh - 64px);
}

.admin-sidebar {
  background: #001529;
  overflow: hidden;
}

.admin-menu {
  border-right: none;
}

/* [UI美化] 侧边菜单：条目留白加高 + 选中项主色左侧短条强调 */
.admin-menu .el-menu-item {
  height: 50px;
  line-height: 50px;
  margin: 4px 10px;
  border-radius: 6px;
  transition:
    background-color 0.2s ease,
    color 0.2s ease;
}

.admin-menu .el-menu-item:hover {
  background: rgba(255, 255, 255, 0.08);
}

.admin-menu .el-menu-item.is-active {
  background: #409eff;
  color: #fff !important;
}

.admin-menu .el-sub-menu .el-menu-item {
  min-height: 40px;
  line-height: 40px;
  padding-left: 40px;
}

.message-badge {
  position: absolute;
  right: 20px;
  top: 50%;
  transform: translateY(-50%);
}

/* [UI美化] 内容区留白统一 20px，与前台一致 */
.admin-main {
  background: #f0f2f5;
  padding: 20px;
  overflow-y: auto;
}

/* [UI美化-本轮] 大屏（≥1600px）：内容区留白加宽，利用宽屏空间 */
@media (min-width: 1600px) {
  .admin-main {
    padding: 24px 32px;
  }
}

/* 面包屑样式 */
:deep(.el-breadcrumb__item) {
  font-size: 14px;
}

:deep(.el-breadcrumb__item.is-link) {
  color: #409eff;
  cursor: pointer;
}

:deep(.el-breadcrumb__item.is-link:hover) {
  color: #66b1ff;
}

/* 下拉菜单样式 */
:deep(.el-dropdown-menu__item) {
  display: flex;
  align-items: center;
  gap: 8px;
}

:deep(.el-dropdown-menu__item .el-icon) {
  font-size: 16px;
  color: #909399;
}

:deep(.el-dropdown-menu__item .el-icon:hover) {
  color: #409eff;
}

/* 响应式设计 */
.sidebar-toggle {
  display: none;
  font-size: 20px;
  margin-right: 10px;
  cursor: pointer;
  color: #606266;
  transition: color 0.2s ease;
}

.sidebar-toggle:hover {
  color: #409eff;
}

.sidebar-mask {
  display: none;
}

@media (max-width: 768px) {
  .admin-header {
    padding: 0 10px;
  }

  .sidebar-toggle {
    display: block;
  }

  .sidebar-mask {
    display: none;
    position: fixed;
    inset: 0;
    background: rgba(0, 0, 0, 0.35);
    z-index: 98;
  }

  .admin-sidebar.open .sidebar-mask {
    display: block;
  }

  .logo span {
    display: none;
  }

  .username {
    font-size: 12px;
  }

  .admin-sidebar {
    position: fixed;
    top: 64px;
    left: 0;
    bottom: 0;
    width: 200px;
    transform: translateX(-100%);
    transition: transform 0.3s;
    z-index: 1000;
  }

  .admin-sidebar.open {
    transform: translateX(0);
  }
}
</style>
