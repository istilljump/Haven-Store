<template>
  <div class="not-found">
    <div class="not-found-content">
      <h1>404</h1>
      <h2>页面未找到</h2>
      <p>抱歉，您访问的页面不存在。</p>
      <el-button type="primary" @click="goHome">
        返回首页
      </el-button>
    </div>
  </div>
</template>

<script>
import { useRouter } from 'vue-router'

export default {
  name: 'NotFound',
  setup() {
    const router = useRouter()

    const goHome = () => {
      router.push('/home')
    }

    return {
      goHome
    }
  }
}
</script>

<style scoped>
.not-found {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
  text-align: center;
}

.not-found-content {
  max-width: 400px;
}

.not-found-content h1 {
  font-size: 72px;
  color: #409eff;
  margin-bottom: 20px;
}

.not-found-content h2 {
  font-size: 24px;
  margin-bottom: 16px;
  color: #333;
}

.not-found-content p {
  font-size: 16px;
  color: #666;
  margin-bottom: 24px;
  line-height: 1.5;
}
</style>