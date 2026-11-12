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

/* [UI美化] Hero：紫色渐变统一为项目主色蓝系，叠加柔光装饰，观感更柔和统一 */
/* [UI美化-本轮] 品牌几何装饰：双圆环点缀（纯 CSS，pointer-events 不影响交互） */
.hero {
  position: relative;
  overflow: hidden;
  background:
    radial-gradient(900px 400px at 15% -20%, rgba(255, 255, 255, 0.16), transparent 55%),
    radial-gradient(700px 380px at 105% 120%, rgba(255, 255, 255, 0.12), transparent 50%),
    linear-gradient(135deg, #409eff 0%, #2563eb 100%);
  padding: 60px 20px 68px;
  color: #fff;
}

.hero::before {
  content: '';
  position: absolute;
  left: -60px;
  bottom: -110px;
  width: 220px;
  height: 220px;
  border: 2px solid rgba(255, 255, 255, 0.16);
  border-radius: 50%;
  pointer-events: none;
}

.hero::after {
  content: '';
  position: absolute;
  right: -90px;
  top: -90px;
  width: 260px;
  height: 260px;
  border: 2px solid rgba(255, 255, 255, 0.18);
  border-radius: 50%;
  pointer-events: none;
}

/* [UI美化-本轮] 容器统一加宽至 1200px */
.hero-inner {
  max-width: 1200px;
  margin: 0 auto;
  text-align: center;
}

.hero-title {
  margin: 0 0 12px;
  font-size: 40px;
  font-weight: 700;
  letter-spacing: 1px;
  text-shadow: 0 2px 8px rgba(16, 24, 40, 0.12);
}

.hero-slogan {
  margin: 0 0 28px;
  font-size: 16px;
  opacity: 0.92;
  letter-spacing: 2px;
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

/* [UI美化-本轮] 区块容器统一加宽至 1200px；区块标题：主色竖条强调 */
.section {
  max-width: 1200px;
  margin: 0 auto;
  padding: 32px 20px;
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-left: 10px;
  position: relative;
}

.section-head::before {
  content: '';
  position: absolute;
  left: 0;
  top: 4px;
  bottom: 4px;
  width: 3px;
  border-radius: 2px;
  background: #409eff;
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

/* [UI美化] 行情看板卡：白卡 + hover 上浮，与商品卡微交互一致 */
.board-card {
  background: #fff;
  border-radius: 10px;
  padding: 18px 20px;
  cursor: pointer;
  border: 1px solid #ebeef5;
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease,
    border-color 0.2s ease;
}

.board-card:hover {
  border-color: #d9ecff;
  transform: translateY(-3px);
  /* [UI美化-本轮] hover 层次加深半档 */
  box-shadow: 0 12px 28px rgba(16, 24, 40, 0.14);
}

.board-rank {
  display: inline-block;
  font-size: 12px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #f59e0b, #f97316);
  border-radius: 4px;
  padding: 2px 8px;
  letter-spacing: 0.5px;
}

.board-name {
  margin-top: 10px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.board-meta {
  margin-top: 6px;
  /* [UI美化-本轮] 成交件数转辅助层级，与主数字区分 */
  font-size: 12px;
  color: #909399;
}

/* [UI美化-本轮] 中位价大号等宽强调（数据展示升级：仅重排现有字段） */
.board-price {
  margin-top: 8px;
  font-size: 22px;
  color: #f56c6c;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.category-grid {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 12px;
}

/* [UI美化] 分类入口：hover 淡蓝底 + 轻微上浮，替代纯描边变化 */
.category-item {
  background: #fff;
  border-radius: 10px;
  text-align: center;
  padding: 18px 4px;
  cursor: pointer;
  border: 1px solid transparent;
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease,
    border-color 0.2s ease,
    color 0.2s ease;
}

.category-item:hover {
  border-color: #d9ecff;
  background: #f7fbff;
  color: #409eff;
  transform: translateY(-3px);
  box-shadow: 0 6px 16px rgba(16, 24, 40, 0.08);
}

.category-name {
  font-size: 14px;
  color: #303133;
  transition: color 0.2s ease;
}

.category-item:hover .category-name {
  color: #409eff;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

/* [UI美化-本轮] 首屏一次性渐入（stagger 延迟 40ms 递进，全局 hs-fade-up keyframes） */
.product-grid > *,
.board-grid > *,
.category-grid > * {
  animation: hs-fade-up 0.35s ease backwards;
}
.product-grid > *:nth-child(2) {
  animation-delay: 0.04s;
}
.product-grid > *:nth-child(3) {
  animation-delay: 0.08s;
}
.product-grid > *:nth-child(4) {
  animation-delay: 0.12s;
}
.product-grid > *:nth-child(n + 5) {
  animation-delay: 0.16s;
}
.board-grid > *:nth-child(2) {
  animation-delay: 0.04s;
}
.board-grid > *:nth-child(3) {
  animation-delay: 0.08s;
}
.category-grid > *:nth-child(2) {
  animation-delay: 0.04s;
}
.category-grid > *:nth-child(3) {
  animation-delay: 0.08s;
}
.category-grid > *:nth-child(4) {
  animation-delay: 0.12s;
}
.category-grid > *:nth-child(n + 5) {
  animation-delay: 0.16s;
}

/* [UI美化-本轮] 宽屏（≥1600px）：商品网格放宽到 6 列 */
@media (min-width: 1600px) {
  .product-grid {
    grid-template-columns: repeat(6, 1fr);
  }
}

.product-skeleton {
  background: #fff;
  border-radius: 10px;
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

  /* [UI美化-本轮] 小屏：看板工具条下拉收窄，避免与区块标题拥挤（覆盖内联 width:160px） */
  .board-tools :deep(.el-select) {
    width: 120px !important;
  }

  .section-head {
    gap: 10px;
  }

  .section-head h2 {
    white-space: nowrap;
    flex-shrink: 0;
  }
}
</style>
