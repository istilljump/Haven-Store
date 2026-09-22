<template>
  <div class="home">
    <div class="home-container">
      <h1 class="home-title">Haven-Store</h1>
      <p class="home-description">安全便捷的二手商品交易平台</p>
      
      <div class="action-buttons">
        <el-button type="primary" size="large" @click="goToPublish">
          发布商品
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

// 发布商品页的真实路由是 /products/create（router/modules/product.js），
// 此前写的是 /publish，该路径不存在，会落到 404 兜底路由
const goToPublish = () => {
  router.push('/products/create')
}
</script>

<style scoped>
.home {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.home-container {
  text-align: center;
  color: white;
  padding: 40px;
}

.home-title {
  font-size: 3rem;
  margin-bottom: 16px;
  font-weight: 700;
}

.home-description {
  font-size: 1.2rem;
  margin-bottom: 40px;
  opacity: 0.9;
}

.action-buttons {
  margin-top: 30px;
}

@media (max-width: 768px) {
  .home-title {
    font-size: 2rem;
  }
  
  .home-description {
    font-size: 1rem;
  }
  
  .home-container {
    padding: 20px;
  }
}
</style>