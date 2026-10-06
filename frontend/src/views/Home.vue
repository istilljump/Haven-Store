<template>
  <div class="home">
    <!-- Hero：品牌 + 搜索入口 -->
    <section class="hero">
      <div class="hero-inner">
        <h1 class="hero-title">{{ siteName }}</h1>
        <p class="hero-slogan">安全便捷的二手商品交易平台 · 让闲置好物流转起来</p>
        <div class="hero-search">
          <el-input
            v-model="keyword"
            placeholder="搜索你想要的宝贝：iPhone、教材、吉他…"
            size="large"
            clearable
            @keyup.enter="goSearch"
          >
            <template #append>
              <el-button type="primary" @click="goSearch">搜索</el-button>
            </template>
          </el-input>
        </div>
        <div class="hero-actions">
          <el-button type="primary" round @click="$router.push('/products/create')">免费发布闲置</el-button>
          <el-button round @click="$router.push('/products?mode=nearby')">附近好物</el-button>
        </div>
      </div>
    </section>

    <!-- 分类导航 -->
    <section class="section">
      <div class="section-head">
        <h2>商品分类</h2>
      </div>
      <div class="category-grid">
        <div
          v-for="category in categories"
          :key="category.id"
          class="category-item"
          @click="goCategory(category.id)"
        >
          <span class="category-name">{{ category.name }}</span>
        </div>
      </div>
    </section>

    <!-- 最新发布 -->
    <section class="section">
      <div class="section-head">
        <h2>最新发布</h2>
        <el-button link type="primary" @click="$router.push('/products')">
          查看全部 <el-icon><ArrowRight /></el-icon>
        </el-button>
      </div>
      <div v-if="loading" class="product-grid">
        <el-skeleton v-for="n in 8" :key="n" class="product-skeleton" animated>
          <template #template>
            <el-skeleton-item variant="image" style="width: 100%; height: 180px" />
            <div style="padding: 12px">
              <el-skeleton-item variant="text" style="width: 90%" />
              <el-skeleton-item variant="text" style="width: 50%; margin-top: 8px" />
            </div>
          </template>
        </el-skeleton>
      </div>
      <el-empty v-else-if="loadError" description="商品加载失败">
        <el-button type="primary" @click="loadData">重新加载</el-button>
      </el-empty>
      <el-empty v-else-if="!products.length" description="还没有商品，快来发布第一件闲置吧" />
      <div v-else class="product-grid">
        <ProductCard v-for="item in products" :key="item.id" :product="item" />
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight } from '@element-plus/icons-vue'
import ProductCard from '@/components/ProductCard.vue'
import { searchProducts, getProductCategories } from '@/api/product'
import { getPublicSettings } from '@/api/public'

/**
 * 首页
 * <p>
 * 此前首页只有一句 slogan，用户落地后找不到商品入口；
 * 现在提供搜索框、分类导航与最新商品流（复用 /product/search 与 /product/categories 接口），
 * 并把「附近好物」与「发布闲置」两个核心动作放在首屏。
 */
const router = useRouter()

const siteName = ref('Haven-Store')
const keyword = ref('')
const categories = ref([])
const products = ref([])
const loading = ref(false)
const loadError = ref(false)

const goSearch = () => {
  const query = keyword.value.trim() ? { keyword: keyword.value.trim() } : {}
  router.push({ path: '/products', query })
}

const goCategory = (categoryId) => {
  router.push({ path: '/products', query: { categoryId } })
}

const loadData = async () => {
  loading.value = true
  loadError.value = false
  try {
    const [productPage, categoryList] = await Promise.all([
      searchProducts({ page: 1, pageSize: 8, sort: 'latest' }),
      getProductCategories()
    ])
    products.value = productPage?.records || []
    categories.value = (categoryList || []).slice(0, 8)
  } catch (err) {
    // 错误提示由拦截器统一弹出；页面内提供重试入口
    loadError.value = true
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  loadData()
  try {
    const settings = await getPublicSettings()
    if (settings?.siteName) {
      siteName.value = settings.siteName
    }
  } catch (err) {
    // 站点信息拿不到就用默认名，不影响首页
  }
})
</script>

<style scoped>
.home {
  background: #f5f7fa;
  min-height: 100%;
}

.hero {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 56px 20px 64px;
  color: #fff;
}

.hero-inner {
  max-width: 1080px;
  margin: 0 auto;
  text-align: center;
}

.hero-title {
  margin: 0 0 10px;
  font-size: 40px;
  font-weight: 700;
}

.hero-slogan {
  margin: 0 0 26px;
  font-size: 16px;
  opacity: 0.92;
}

.hero-search {
  max-width: 560px;
  margin: 0 auto 20px;
}

.hero-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
}

.section {
  max-width: 1080px;
  margin: 0 auto;
  padding: 32px 20px;
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-head h2 {
  margin: 0;
  font-size: 20px;
  color: #303133;
}

.category-grid {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 12px;
}

.category-item {
  background: #fff;
  border-radius: 8px;
  text-align: center;
  padding: 18px 4px;
  cursor: pointer;
  border: 1px solid transparent;
  transition: all 0.2s;
}

.category-item:hover {
  border-color: #409eff;
  color: #409eff;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.12);
}

.category-name {
  font-size: 14px;
  color: #303133;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.product-skeleton {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  padding-bottom: 8px;
}

@media (max-width: 768px) {
  .hero-title {
    font-size: 28px;
  }

  .hero {
    padding: 36px 16px 44px;
  }

  .category-grid {
    grid-template-columns: repeat(4, 1fr);
  }

  .product-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
