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
            <!-- 购物车与私信都需要登录，未登录时不展示入口（路由守卫也有一层兜底） -->
            <el-menu-item v-if="isLoggedIn" index="/cart">
              购物车
              <el-badge
                v-if="cartCount > 0"
                :value="cartCount > 99 ? '99+' : cartCount"
                class="nav-badge"
              />
            </el-menu-item>
            <el-menu-item v-if="isLoggedIn" index="/messages">
              私信
              <el-badge
                v-if="unreadCount > 0"
                :value="unreadCount > 99 ? '99+' : unreadCount"
                class="nav-badge"
              />
            </el-menu-item>
          </el-menu>
        </div>

        <div class="user-menu">
          <template v-if="isLoggedIn">
            <el-dropdown @command="handleCommand">
              <span class="user-info">
                <el-avatar :src="user?.avatar" size="small" />
                <span class="username">{{ user?.username }}</span>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <!-- 说明：el-dropdown-item 的 index 属性不会触发导航，必须用 command + @command 处理 -->
                  <el-dropdown-item v-if="isAdmin" command="admin">
                    管理后台
                  </el-dropdown-item>
                  <el-dropdown-item command="profile">
                    个人资料
                  </el-dropdown-item>
                  <el-dropdown-item command="orders">
                    我的订单
                  </el-dropdown-item>
                  <el-dropdown-item divided command="logout">
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
import { computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useAuthStore } from '@/store/auth'
import { useConfigStore, useUserStore } from '@/store/index'
import { useRoute, useRouter } from 'vue-router'
import messageApi from '@/api/message'
import cartApi from '@/api/cart'

/** 头部角标（未读私信、购物车件数）的刷新间隔：登录态变化与路由切换时也会立即刷新 */
const BADGE_POLL_INTERVAL_MS = 30000

export default {
  name: 'MainLayout',
  setup() {
    const authStore = useAuthStore()
    // 站点配置来自 config store（此前误用 user store，且缺少 import，会抛 ReferenceError 导致白屏）
    const configStore = useConfigStore()
    // 未读私信数挂在 user store 上（auth store 只做登录态封装，没有这部分状态）
    const userStore = useUserStore()
    const router = useRouter()
    const route = useRoute()

    const config = computed(() => configStore.config)
    const isLoggedIn = computed(() => authStore.isLoggedIn)
    const user = computed(() => authStore.user)
    // 后端登录态返回的是布尔值 isAdmin，这里同时兼容角色字符串写法
    const isAdmin = computed(() => authStore.user?.isAdmin === true
      || authStore.user?.role === 'admin'
      || authStore.user?.role === 'super_admin')

    // 头部角标：直接读 store（私信中心页/购物车页改动后会写入新值，角标即时更新）
    const unreadCount = computed(() => userStore.unreadMessageCount)
    const cartCount = computed(() => userStore.cartItemCount)
    let badgeTimer = null

    const loadBadges = async () => {
      if (!authStore.isLoggedIn) {
        userStore.setUnreadMessageCount(0)
        userStore.setCartItemCount(0)
        return
      }
      // 两个角标互不影响：任一个取不到就保留旧值，不因为一个失败让另一个也不更新
      try {
        userStore.setUnreadMessageCount(await messageApi.getUnreadCount())
      } catch (error) {
        console.error('获取未读私信数失败:', error)
      }
      try {
        userStore.setCartItemCount(await cartApi.getCartCount())
      } catch (error) {
        console.error('获取购物车件数失败:', error)
      }
    }

    onMounted(() => {
      loadBadges()
      badgeTimer = setInterval(() => {
        if (document.visibilityState === 'visible') {
          loadBadges()
        }
      }, BADGE_POLL_INTERVAL_MS)
    })

    onBeforeUnmount(() => {
      if (badgeTimer) {
        clearInterval(badgeTimer)
        badgeTimer = null
      }
    })

    // 路由切换时刷新一次：登录/退出登录与读过消息、改过购物车后角标都能及时跟上
    watch(() => route.path, () => {
      loadBadges()
    })

    const handleLogout = () => {
      authStore.logout()
      router.push('/auth/login')
    }

    // 下拉菜单命令处理（index 属性不触发导航，统一走 command）
    const handleCommand = (command) => {
      switch (command) {
        case 'admin':
          router.push('/admin/dashboard')
          break
        case 'profile':
          router.push('/profile')
          break
        case 'orders':
          router.push('/orders')
          break
        case 'logout':
          handleLogout()
          break
      }
    }

    return {
      config,
      isLoggedIn,
      user,
      isAdmin,
      unreadCount,
      cartCount,
      handleLogout,
      handleCommand
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

/* 私信未读角标：往上抬一点，避免撑高横向菜单挤动布局 */
.nav-badge {
  margin-left: 6px;
  margin-top: -8px;
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