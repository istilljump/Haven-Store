<template>
  <div class="auth-layout">
    <div class="auth-container">
      <div class="auth-header">
        <h1>{{ config.siteName }}</h1>
        <p>毕业不甩卖，闲置有归宿</p>
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
/* [UI美化] 认证页背景：紫色渐变统一为项目主色（Element 蓝）系渐变，加柔光装饰，与全站主色一致 */
.auth-layout {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    radial-gradient(1000px 500px at 10% -10%, rgba(255, 255, 255, 0.14), transparent 60%),
    radial-gradient(800px 400px at 110% 110%, rgba(255, 255, 255, 0.1), transparent 55%),
    linear-gradient(135deg, #409eff 0%, #2563eb 100%);
  padding: 20px;
}

.auth-container {
  background: white;
  border-radius: 14px;
  box-shadow: 0 20px 50px rgba(16, 24, 40, 0.18);
  width: 100%;
  max-width: 450px;
  overflow: hidden;
}

.auth-header {
  text-align: center;
  padding: 40px 30px 20px;
  border-bottom: 1px solid #f5f7fa;
}

.auth-header h1 {
  margin: 0;
  color: #303133;
  font-size: 28px;
  font-weight: 700;
}

.auth-header p {
  margin: 10px 0 0;
  color: #909399;
  font-size: 15px;
}

.auth-content {
  padding: 32px 30px;
}

.auth-footer {
  text-align: center;
  padding: 18px 30px;
  border-top: 1px solid #f5f7fa;
}

.auth-footer p {
  margin: 0;
  color: #c0c4cc;
  font-size: 13px;
}
</style>
