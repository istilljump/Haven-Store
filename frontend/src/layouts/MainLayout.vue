<template>
  <div class="main-layout">
    <!-- 顶部导航栏 -->
    <el-header height="60px" class="header">
      <div class="header-content">
        <div class="logo">
          <h1>{{ config.siteName }}</h1>
        </div>
        
        <div class="nav-menu">
          <el-menu
            mode="horizontal"
            :router="true"
            :default-active="$route.path"
            class="nav-menu-inner"
          >
            <el-menu-item index="/home">首页</el-menu-item>
            <el-menu-item index="/products">商品</el-menu-item>
            <el-menu-item index="/users">用户</el-menu-item>
          </el-menu>
        </div>

        <div class="user-menu">
          <template v-if="isLoggedIn">
            <el-dropdown>
              <span class="user-info">
                <el-avatar :src="user?.avatar" size="small" />
                <span class="username">{{ user?.username }}</span>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-if="isAdmin" index="/admin">
                    管理后台
                  </el-dropdown-item>
                  <el-dropdown-item index="/profile">
                    个人资料
                  </el-dropdown-item>
                  <el-dropdown-item index="/settings">
                    设置
                  </el-dropdown-item>
                  <el-dropdown-item divided @click="handleLogout">
                    退出登录
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button type="primary" @click="$router.push('/auth/login')">
              登录
            </el-button>
            <el-button @click="$router.push('/auth/register')">
              注册
            </el-button>
          </template>
        </div>
      </div>
    </el-header>

    <!-- 主要内容区域 -->
    <el-main class="main-content">
      <router-view />
    </el-main>

    <!-- 页脚 -->
    <el-footer height="60px" class="footer">
      <div class="footer-content">
        <p>&copy; 2024 {{ config.siteName }}. All rights reserved.</p>
      </div>
    </el-footer>
  </div>
</template>

<script>
import { computed } from 'vue'
import { useAuthStore } from '@/store/auth'
import { useConfigStore } from '@/store/index'
import { useRouter } from 'vue-router'

export default {
  name: 'MainLayout',
  setup() {
    const authStore = useAuthStore()
    // 站点配置来自 config store（此前误用 user store，且缺少 import，会抛 ReferenceError 导致白屏）
    const configStore = useConfigStore()
    const router = useRouter()

    const config = computed(() => configStore.config)
    const isLoggedIn = computed(() => authStore.isLoggedIn)
    const user = computed(() => authStore.user)
    // 后端登录态返回的是布尔值 isAdmin，这里同时兼容角色字符串写法
    const isAdmin = computed(() => authStore.user?.isAdmin === true
      || authStore.user?.role === 'admin'
      || authStore.user?.role === 'super_admin')

    const handleLogout = () => {
      authStore.logout()
      router.push('/auth/login')
    }

    return {
      config,
      isLoggedIn,
      user,
      isAdmin,
      handleLogout
    }
  }
}
</script>

<style scoped>
.main-layout {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.header {
  background-color: #fff;
  border-bottom: 1px solid #e6e6e6;
  padding: 0 20px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 100%;
  max-width: 1200px;
  margin: 0 auto;
}

.logo h1 {
  margin: 0;
  font-size: 24px;
  color: #409eff;
}

.nav-menu {
  flex: 1;
  margin-left: 40px;
}

.nav-menu-inner {
  border-bottom: none;
}

.user-menu {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.username {
  font-size: 14px;
}

.main-content {
  flex: 1;
  background-color: #f5f7fa;
  padding: 20px;
}

.footer {
  background-color: #fff;
  border-top: 1px solid #e6e6e6;
  padding: 0 20px;
}

.footer-content {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  max-width: 1200px;
  margin: 0 auto;
}

.footer-content p {
  margin: 0;
  color: #666;
}
</style>