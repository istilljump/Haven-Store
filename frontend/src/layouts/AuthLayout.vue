<template>
  <div class="auth-layout">
    <div class="auth-container">
      <div class="auth-header">
        <h1>{{ config.siteName }}</h1>
        <p>欢迎来到二手商品交易市场</p>
      </div>
      
      <div class="auth-content">
        <router-view />
      </div>
      
      <div class="auth-footer">
        <p>&copy; 2024 {{ config.siteName }}. All rights reserved.</p>
      </div>
    </div>
  </div>
</template>

<script>
import { useConfigStore } from '@/store'
import { computed } from 'vue'

export default {
  name: 'AuthLayout',
  setup() {
    const configStore = useConfigStore()
    const config = computed(() => configStore.config)

    return {
      config
    }
  }
}
</script>

<style scoped>
.auth-layout {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.auth-container {
  background: white;
  border-radius: 10px;
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.1);
  width: 100%;
  max-width: 450px;
  overflow: hidden;
}

.auth-header {
  text-align: center;
  padding: 40px 30px 20px;
  border-bottom: 1px solid #f0f0f0;
}

.auth-header h1 {
  margin: 0;
  color: #333;
  font-size: 28px;
  font-weight: 600;
}

.auth-header p {
  margin: 10px 0 0;
  color: #666;
  font-size: 16px;
}

.auth-content {
  padding: 40px 30px;
}

.auth-footer {
  text-align: center;
  padding: 20px 30px;
  border-top: 1px solid #f0f0f0;
}

.auth-footer p {
  margin: 0;
  color: #999;
  font-size: 14px;
}
</style>