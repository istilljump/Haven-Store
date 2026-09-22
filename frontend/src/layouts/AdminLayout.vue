<template>
  <div class="admin-layout">
    <!-- 登录页面 -->
    <router-view v-if="$route.path === '/admin/login'" />
    
    <!-- 管理后台布局 -->
    <el-container v-else>
      <!-- 顶部导航 -->
      <el-header class="admin-header">
        <div class="header-left">
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
        <!-- 侧边栏 -->
        <el-aside width="240px" class="admin-sidebar">
          <el-scrollbar>
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

              <el-menu-item index="/admin/categories">
                <el-icon><Collection /></el-icon>
                <span>分类管理</span>
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
          </el-scrollbar>
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
  User,
  Setting,
  SwitchButton,
  Odometer,
  Goods,
  Collection,
  Bell
} from '@element-plus/icons-vue'
import { getUserInfo, removeToken, removeUserInfo } from '@/utils/auth'

const router = useRouter()
const route = useRoute()

// 当前登录用户信息
// 唯一数据源是 utils/auth 的 getUserInfo（即登录时写入 localStorage 的 LoginUserVO），
// 与路由守卫读取的是同一份数据，避免出现「守卫认为已登录、页面认为未登录」的错位
const userInfo = ref(getUserInfo() || {})

// 当前路由标题
const currentRouteTitle = computed(() => {
  const routeMap = {
    '/admin/dashboard': '数据概览',
    '/admin/users': '用户管理',
    '/admin/products': '商品管理',
    '/admin/categories': '分类管理',
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
      ElMessage.info('个人功能开发中')
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
  }).then(() => {
    removeToken()
    removeUserInfo()
    ElMessage.success('退出成功')
    router.push('/admin/login')
  }).catch(() => {})
}

// 说明：/admin/** 的登录态与管理员角色校验已统一收敛到 router/index.js 的全局守卫，
// 组件内不再重复注册守卫（此前每次挂载都注册一遍，且用的是错误的 localStorage key）

// 初始化：以 localStorage 中的最新数据校准一次，避免 store 与本地存储不同步
onMounted(() => {
  userInfo.value = getUserInfo() || {}
})
</script>

<style scoped>
.admin-layout {
  height: 100vh;
  background: #f0f2f5;
}

.admin-header {
  height: 64px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
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
}

.logo .el-icon {
  font-size: 24px;
  color: #409eff;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  border-radius: 4px;
  cursor: pointer;
  transition: background 0.3s;
}

.user-info:hover {
  background: #f5f5f5;
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

.admin-menu .el-menu-item {
  height: 50px;
  line-height: 50px;
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

.admin-main {
  background: #f0f2f5;
  padding: 20px;
  overflow-y: auto;
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
@media (max-width: 768px) {
  .admin-header {
    padding: 0 10px;
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