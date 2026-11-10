<template>
  <div class="home">
    <!-- Hero：品牌 + 增强搜索入口（补全/纠错）+ 校区快速筛选 -->
    <section class="hero">
      <div class="hero-inner">
        <h1 class="hero-title">{{ siteName }}</h1>
        <p class="hero-slogan">毕业不甩卖，闲置有归宿 · 校园闲置循环平台</p>
        <div class="hero-search-row">
          <div class="hero-search">
            <SearchSuggest
              v-model="keyword"
              :campus-id="campusId"
              @search="goSearch"
            />
          </div>
          <el-select
            v-model="campusId"
            class="hero-campus"
            placeholder="全校"
            size="large"
            clearable
            @change="onCampusChange"
          >
            <el-option
              v-for="campus in campusOptions"
              :key="campus.id"
              :label="campus.name"
              :value="campus.id"
            />
          </el-select>
        </div>
        <div class="hero-actions">
          <el-button type="primary" round @click="$router.push('/products/create')"
            >免费发布闲置</el-button
          >
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

    <!-- 本校行情速览：TOP3 闲置品类与近期成交中位价（数据飞轮的首页窗口） -->
    <section class="section">
      <div class="section-head">
        <h2>本校行情速览</h2>
        <div class="board-tools">
          <el-select v-model="boardCampusId" size="small" style="width: 160px" @change="loadBoard">
            <el-option
              v-for="campus in campusOptions"
              :key="campus.id"
              :label="campus.name"
              :value="campus.id"
            />
          </el-select>
          <el-button size="small" :loading="boardLoading" @click="loadBoard">刷新</el-button>
        </div>
      </div>
      <div v-if="board.length" class="board-grid">
        <div
          v-for="(stat, idx) in board"
          :key="stat.categoryId"
          class="board-card"
          @click="goCategory(stat.categoryId)"
        >
          <div class="board-rank">TOP{{ idx + 1 }}</div>
          <div class="board-name">{{ stat.categoryName }}</div>
          <div class="board-meta">近90天成交 {{ stat.dealCount }} 件</div>
          <div class="board-price">中位价 ¥{{ stat.medianPrice }}</div>
        </div>
      </div>
      <el-empty v-else-if="!boardLoading" description="该校区暂无成交行情" :image-size="60" />
    </section>

    <!-- 冷启动推荐（R1 校区成交活跃 → R2 本校高频品类）；推荐不可用时回退最新发布 -->
    <RecommendSection
      v-if="recommendItems && recommendItems.length"
      title="为你推荐"
      :subtitle="campusId ? '按你所在校区的成交行情挑选' : '选择校区可获得本校专属推荐'"
      :items="recommendItems"
    />
    <section v-else class="section">
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
import SearchSuggest from '@/components/SearchSuggest.vue'
import RecommendSection from '@/components/RecommendSection.vue'
import { searchProducts, getProductCategories } from '@/api/product'
import { getPublicSettings } from '@/api/public'
import campusApi from '@/api/campus'
import marketApi from '@/api/market'
import recommendApi from '@/api/recommend'

/**
 * 首页（第二轮：增强搜索 × 场景推荐 × 行情看板）
 * <p>
 * 搜索框升级为增强搜索（词库/拼音/本校热搜补全 + 纠错），并自带校区快速筛选；
 * 新增「本校行情速览」看板（TOP3 品类 + 成交中位价，成交后点刷新即可见飞轮效果）；
 * 「最新发布」升级为冷启动推荐（R1/R2 规则 + 可解释文案），推荐不可用时回退最新流。
 */
const CAMPUS_STORAGE_KEY = 'haven_campus'

const router = useRouter()

const siteName = ref('Haven-Store')
const keyword = ref('')
const categories = ref([])
const products = ref([])
const loading = ref(false)
const loadError = ref(false)

const campusOptions = ref([])
const campusId = ref(null)
const recommendItems = ref([])
const board = ref([])
const boardLoading = ref(false)
const boardCampusId = ref(null)

const goSearch = (value) => {
  const kw = (value !== undefined ? value : keyword.value).trim()
  const query = {}
  if (kw) query.keyword = kw
  if (campusId.value) query.campusId = campusId.value
  router.push({ path: '/products', query })
}

const goCategory = (categoryId) => {
  router.push({ path: '/products', query: { categoryId, campusId: campusId.value || undefined } })
}

const onCampusChange = (value) => {
  // 校区是全站上下文：选中后写入本地，推荐与看板即刻切换
  if (value) {
    localStorage.setItem(CAMPUS_STORAGE_KEY, String(value))
  } else {
    localStorage.removeItem(CAMPUS_STORAGE_KEY)
  }
  boardCampusId.value = value || (campusOptions.value[0] && campusOptions.value[0].id)
  loadBoard()
  loadRecommend()
}

const loadBoard = async () => {
  if (!boardCampusId.value) {
    board.value = []
    return
  }
  boardLoading.value = true
  try {
    board.value = (await marketApi.getCampusSummary({ campusId: boardCampusId.value, topN: 3 })) || []
  } catch (err) {
    board.value = []
  } finally {
    boardLoading.value = false
  }
}

const loadRecommend = async () => {
  try {
    recommendItems.value =
      (await recommendApi.getHomeRecommend({ campusId: campusId.value || undefined })) || []
  } catch (err) {
    recommendItems.value = []
  }
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
  // 恢复用户上次选择的校区（推荐与看板共用这一份校区上下文）
  const saved = Number(localStorage.getItem(CAMPUS_STORAGE_KEY))
  try {
    campusOptions.value = (await campusApi.getCampuses()) || []
    if (campusOptions.value.length) {
      campusId.value = campusOptions.value.some((c) => c.id === saved)
        ? saved
        : campusOptions.value[0].id
      boardCampusId.value = campusId.value
      loadBoard()
    }
  } catch (err) {
    campusOptions.value = []
  }
  loadRecommend()
  try {
    const settings = await getPublicSettings()
    if (settings?.siteName) {
      siteName.value = settings.siteName
    }
    // 推荐总开关关闭时不请求推荐接口，回退最新发布流
    if (settings && settings.recommendEnabled === false) {
      recommendItems.value = []
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

.hero-search-row {
  max-width: 720px;
  margin: 0 auto 20px;
  display: flex;
  gap: 10px;
}

.hero-search {
  flex: 1;
  text-align: left;
}

.hero-campus {
  width: 170px;
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

.board-tools {
  display: flex;
  align-items: center;
  gap: 8px;
}

.board-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
}

.board-card {
  background: #fff;
  border-radius: 10px;
  padding: 16px 18px;
  cursor: pointer;
  border: 1px solid #ebeef5;
  transition: all 0.2s;
}

.board-card:hover {
  border-color: #409eff;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.12);
}

.board-rank {
  display: inline-block;
  font-size: 12px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #f59e0b, #f97316);
  border-radius: 4px;
  padding: 1px 8px;
}

.board-name {
  margin-top: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.board-meta {
  margin-top: 4px;
  font-size: 13px;
  color: #909399;
}

.board-price {
  margin-top: 2px;
  font-size: 14px;
  color: #ff6b6b;
  font-weight: 600;
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

  .hero-search-row {
    flex-direction: column;
  }

  .hero-campus {
    width: 100%;
  }

  .category-grid {
    grid-template-columns: repeat(4, 1fr);
  }

  .product-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .board-grid {
    grid-template-columns: 1fr;
  }
}
</style>
