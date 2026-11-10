<template>
  <div class="product-list-container">
    <div class="header">
      <h1>商品列表</h1>
      <el-button type="primary" @click="goToCreate">发布商品</el-button>
    </div>

    <div class="filter-section">
      <el-row :gutter="14" class="filter-row">
        <el-col :xs="24" :sm="6">
          <SearchSuggest
            v-model="searchForm.keyword"
            :campus-id="searchForm.campus || null"
            placeholder="搜标题 / 描述 / 品牌 / 型号"
            @search="handleSearch"
          />
        </el-col>
        <el-col :xs="12" :sm="4">
          <el-select
            v-model="searchForm.category"
            placeholder="选择分类"
            clearable
            @change="handleSearch"
          >
            <el-option
              v-for="item in categoryOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-col>
        <el-col :xs="12" :sm="4">
          <el-select
            v-model="searchForm.campus"
            placeholder="选择校区"
            clearable
            @change="handleSearch"
          >
            <el-option
              v-for="item in campusOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-col>
        <el-col :xs="12" :sm="4">
          <el-select
            v-model="searchForm.priceRange"
            placeholder="价格区间"
            clearable
            @change="handleSearch"
          >
            <el-option label="0-100元" value="0-100" />
            <el-option label="100-500元" value="100-500" />
            <el-option label="500-1000元" value="500-1000" />
            <el-option label="1000元以上" value="1000+" />
          </el-select>
        </el-col>
        <el-col :xs="12" :sm="3">
          <el-select v-model="searchForm.sort" @change="handleSearch">
            <el-option label="最新发布" value="latest" />
            <el-option label="价格从低到高" value="priceAsc" />
            <el-option label="价格从高到低" value="priceDesc" />
          </el-select>
        </el-col>
        <el-col :xs="12" :sm="3">
          <div class="filter-buttons">
            <el-button
              :type="nearbyMode ? 'success' : 'default'"
              :loading="locating"
              @click="toggleNearby"
            >
              {{ nearbyMode ? '附近模式中' : '附近商品' }}
            </el-button>
          </div>
        </el-col>
      </el-row>
      <div class="filter-actions">
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </div>
      <!-- 进阶能力（后台开关）：搜索结果按成色二次筛选 -->
      <div v-if="conditionFilterEnabled" class="condition-chips">
        <el-tag
          v-for="cond in CONDITION_OPTIONS"
          :key="cond"
          :type="searchForm.condition === cond ? 'primary' : 'info'"
          :effect="searchForm.condition === cond ? 'dark' : 'plain'"
          class="condition-chip"
          @click="toggleCondition(cond)"
        >
          {{ cond }}
        </el-tag>
      </div>
      <!-- 纠错提示：命中本地纠错词表时告知用户实际检索词 -->
      <el-alert
        v-if="correction"
        :title="`已为你显示「${correction.corrected}」的搜索结果（输入：${correction.original}）`"
        type="warning"
        :closable="true"
        @close="correction = null"
        style="margin-top: 10px"
      />
      <el-alert
        v-if="nearbyMode"
        :title="`已开启附近模式：以你的位置为中心 ${radius} 公里内的在售商品`"
        type="success"
        :closable="true"
        @close="toggleNearby"
        style="margin-top: 10px"
      />
    </div>

    <div class="product-grid">
      <div v-if="loading" class="grid">
        <el-skeleton v-for="n in 8" :key="n" class="card-skeleton" animated>
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
        <el-button type="primary" @click="fetchProducts">重新加载</el-button>
      </el-empty>

      <el-empty
        v-else-if="products.length === 0"
        :description="
          nearbyMode ? '附近暂无在售商品，试试扩大范围或关闭附近模式' : '没有找到相关商品'
        "
      />

      <div v-else class="grid">
        <template v-if="nearbyMode">
          <div
            v-for="item in products"
            :key="item.id"
            class="nearby-card"
            @click="viewProduct(item.id)"
          >
            <AppImage class="nearby-cover" :src="item.coverImage" :alt="item.title" />
            <div class="nearby-info">
              <div class="nearby-title">{{ item.title }}</div>
              <div class="nearby-meta">
                <span class="nearby-price">¥{{ item.price }}</span>
                <el-tag size="mini" type="success">距离 {{ item.distanceKm }} km</el-tag>
              </div>
            </div>
          </div>
        </template>
        <template v-else>
          <div
            v-for="product in products"
            :key="product.id"
            class="card-wrap"
            @click="trackClick(product)"
          >
            <ProductCard :product="product" />
            <!-- 搜索结果条目内嵌行情标签：品类中位价 + 划算指数 -->
            <div v-if="product.marketMedian" class="market-hint">
              该品类近期中位价 ¥{{ product.marketMedian }}
              <span
                v-if="product.pricePosition !== null && product.pricePosition !== undefined"
                :class="product.pricePosition >= 50 ? 'hint-good' : 'hint-warn'"
              >
                ·
                {{
                  product.pricePosition >= 50
                    ? `低于近期 ${product.pricePosition}% 成交价`
                    : `高于近期 ${100 - product.pricePosition}% 成交价`
                }}
              </span>
            </div>
          </div>
        </template>
      </div>
    </div>

    <div class="pagination" v-if="total > pageSize">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[12, 24, 36, 48]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

    <!-- 搜索后互补闲置推荐（R3）：字典命中才展示，不同类凑数 -->
    <RecommendSection
      v-if="complementary.length"
      title="买了它的人还在看"
      subtitle="互补闲置推荐 · 经常和你的搜索一起交易"
      :items="complementary"
    />
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import ProductCard from '@/components/ProductCard.vue'
import AppImage from '@/components/AppImage.vue'
import SearchSuggest from '@/components/SearchSuggest.vue'
import RecommendSection from '@/components/RecommendSection.vue'
import productApi from '@/api/product'
import campusApi from '@/api/campus'
import searchApi from '@/api/search'
import recommendApi from '@/api/recommend'
import { getPublicSettings } from '@/api/public'
import { MAP_CONFIG } from '@/utils/constants'

/**
 * 商品列表页
 * <p>
 * 在原有关键词/分类/价格区间筛选的基础上新增：
 * 1. 排序（最新/价格升降，对接 /product/search 的 sort 参数）；
 * 2. 附近模式（浏览器定位 + /product/nearby，按距离升序）；
 * 3. 加载失败重试与统一的商品卡片组件。
 */
export default {
  name: 'ProductList',
  components: { Search, ProductCard, AppImage, SearchSuggest, RecommendSection },
  setup() {
    const router = useRouter()
    const route = useRoute()

    const products = ref([])
    const loading = ref(false)
    const loadError = ref(false)
    const total = ref(0)
    const currentPage = ref(1)
    const pageSize = ref(12)
    const categoryOptions = ref([])
    const campusOptions = ref([])

    // 增强搜索的配套状态：纠错信息、埋点 ID、互补推荐、进阶成色筛选
    const correction = ref(null)
    const lastLogId = ref(null)
    const complementary = ref([])
    const conditionFilterEnabled = ref(false)
    const CONDITION_OPTIONS = ['全新', '九成新', '八成新', '七成新及以下']

    const nearbyMode = ref(false)
    const locating = ref(false)
    const radius = ref(MAP_CONFIG.DEFAULT_RADIUS)
    const location = ref(null)

    const searchForm = reactive({
      keyword: '',
      category: '',
      campus: '',
      priceRange: '',
      sort: 'latest',
      condition: ''
    })

    // 价格区间 -> 后端 minPrice/maxPrice 的映射（'1000+' 表示下限 1000、无上限）
    const PRICE_RANGE_MAP = {
      '0-100': [0, 100],
      '100-500': [100, 500],
      '500-1000': [500, 1000],
      '1000+': [1000, null]
    }

    // 加载分类下拉数据（只取启用状态的分类）
    const loadCategories = async () => {
      try {
        categoryOptions.value = await productApi.getProductCategories()
      } catch (error) {
        // 分类下拉加载失败不阻断商品列表
      }
    }

    // 加载校区下拉数据（校园场景筛选；失败不阻断商品列表）
    const loadCampuses = async () => {
      try {
        campusOptions.value = await campusApi.getCampuses()
      } catch (error) {
        // 校区数据加载失败时仅隐藏筛选入口
      }
    }

    const fetchProducts = async () => {
      loading.value = true
      loadError.value = false
      try {
        if (nearbyMode.value && location.value) {
          const data = await productApi.getNearbyProducts({
            centerLongitude: location.value.longitude,
            centerLatitude: location.value.latitude,
            radius: radius.value,
            pageNum: currentPage.value,
            pageSize: pageSize.value
          })
          products.value = (data.records || []).map((p) => ({
            ...p,
            distanceKm: Number(p.distance).toFixed(1)
          }))
          total.value = data.total || 0
          complementary.value = []
        } else {
          const range = PRICE_RANGE_MAP[searchForm.priceRange] || [null, null]
          // 增强搜索：自动纠错 + 结果内嵌行情标签 + 埋点（/search/products）
          const res = await searchApi.searchProductsEnhanced({
            keyword: searchForm.keyword || undefined,
            categoryId: searchForm.category || undefined,
            campusId: searchForm.campus || undefined,
            minPrice: range[0] === null ? undefined : range[0],
            maxPrice: range[1] === null ? undefined : range[1],
            sort: searchForm.sort || undefined,
            productCondition: searchForm.condition || undefined,
            page: currentPage.value,
            pageSize: pageSize.value
          })
          products.value = (res && res.page && res.page.records) || []
          total.value = (res && res.page && res.page.total) || 0
          correction.value = (res && res.correction) || null
          lastLogId.value = (res && res.logId) || null
          loadComplementary()
        }
      } catch (error) {
        products.value = []
        total.value = 0
        loadError.value = true
        complementary.value = []
      } finally {
        loading.value = false
      }
    }

    /**
     * 搜索后互补闲置推荐（R3）：用纠错后的检索词解析品类，
     * 排除当前结果已出现的商品；字典未命中返回空，区块自动隐藏
     */
    const loadComplementary = async () => {
      complementary.value = []
      const kw = (correction.value && correction.value.corrected) || searchForm.keyword
      if (!kw) {
        return
      }
      try {
        const items = await recommendApi.getComplementaryRecommend({
          keyword: kw,
          campusId: searchForm.campus || undefined,
          excludeIds: products.value.map((p) => p.id)
        })
        complementary.value = items || []
      } catch (error) {
        complementary.value = []
      }
    }

    /**
     * 点击信标：把点击的结果商品回填到本次搜索埋点（fire-and-forget）
     */
    const trackClick = (product) => {
      if (lastLogId.value && product && product.id) {
        searchApi.logSearchClick({ logId: lastLogId.value, productId: product.id }).catch(() => {})
      }
    }

    /**
     * 进阶成色筛选（后台开关开启时展示 chips）
     */
    const toggleCondition = (cond) => {
      searchForm.condition = searchForm.condition === cond ? '' : cond
      handleSearch()
    }

    const handleSearch = () => {
      currentPage.value = 1
      fetchProducts()
    }

    const resetSearch = () => {
      searchForm.keyword = ''
      searchForm.category = ''
      searchForm.campus = ''
      searchForm.priceRange = ''
      searchForm.sort = 'latest'
      searchForm.condition = ''
      handleSearch()
    }

    /**
     * 切换附近模式：请求浏览器定位授权，成功后按距离加载商品
     */
    const toggleNearby = () => {
      if (nearbyMode.value) {
        nearbyMode.value = false
        location.value = null
        handleSearch()
        return
      }
      if (!navigator.geolocation) {
        ElMessage.warning('当前浏览器不支持定位，无法使用附近模式')
        return
      }
      locating.value = true
      navigator.geolocation.getCurrentPosition(
        (position) => {
          locating.value = false
          location.value = {
            longitude: Number(position.coords.longitude.toFixed(6)),
            latitude: Number(position.coords.latitude.toFixed(6))
          }
          nearbyMode.value = true
          handleSearch()
        },
        () => {
          locating.value = false
          ElMessage.warning('获取位置失败，请检查浏览器定位授权')
        },
        { timeout: 8000 }
      )
    }

    const handleSizeChange = (val) => {
      pageSize.value = val
      fetchProducts()
    }

    const handleCurrentChange = (val) => {
      currentPage.value = val
      fetchProducts()
    }

    const viewProduct = (id) => {
      router.push(`/products/${id}`)
    }

    const goToCreate = () => {
      router.push('/products/create')
    }

    onMounted(() => {
      // 首页/详情页跳转过来的预置筛选（?keyword=xxx、?categoryId=x、?campusId=x、?mode=nearby）
      if (route.query.keyword) {
        searchForm.keyword = String(route.query.keyword)
      }
      if (route.query.categoryId) {
        searchForm.category = Number(route.query.categoryId)
      }
      if (route.query.campusId) {
        searchForm.campus = Number(route.query.campusId)
      }
      loadCategories()
      loadCampuses()
      fetchProducts()
      if (route.query.mode === 'nearby') {
        toggleNearby()
      }
      // 进阶能力开关：后台开启后展示成色二次筛选 chips
      getPublicSettings()
        .then((settings) => {
          conditionFilterEnabled.value = !!(settings && settings.searchConditionFilter === true)
        })
        .catch(() => {})
    })

    return {
      products,
      loading,
      loadError,
      total,
      currentPage,
      pageSize,
      searchForm,
      categoryOptions,
      campusOptions,
      nearbyMode,
      locating,
      radius,
      correction,
      complementary,
      conditionFilterEnabled,
      CONDITION_OPTIONS,
      handleSearch,
      resetSearch,
      toggleNearby,
      toggleCondition,
      trackClick,
      handleSizeChange,
      handleCurrentChange,
      viewProduct,
      goToCreate,
      fetchProducts
    }
  }
}
</script>

<style scoped>
.product-list-container {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

/* 搜索结果条目包装：卡片 + 行情小字 */
.card-wrap {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.market-hint {
  font-size: 12px;
  color: #909399;
  padding: 0 2px;
}

.market-hint .hint-good {
  color: #67c23a;
}

.market-hint .hint-warn {
  color: #e6a23c;
}

/* 进阶成色二次筛选 chips */
.condition-chips {
  margin-top: 10px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.condition-chip {
  cursor: pointer;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header h1 {
  margin: 0;
  color: #333;
}

.filter-section {
  margin-bottom: 24px;
  padding: 16px 20px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.filter-row {
  row-gap: 10px;
}

.filter-buttons {
  display: flex;
  justify-content: flex-start;
}

.filter-actions {
  margin-top: 10px;
}

.product-grid {
  margin-bottom: 30px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.card-skeleton {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
}

/* 附近模式卡片：带距离标签 */
.nearby-card {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  border: 1px solid #ebeef5;
  transition:
    transform 0.2s,
    box-shadow 0.2s;
}

.nearby-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.09);
}

.nearby-cover {
  width: 100%;
  aspect-ratio: 4 / 3;
}

.nearby-info {
  padding: 10px 12px 12px;
}

.nearby-title {
  font-size: 14px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.nearby-meta {
  margin-top: 6px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.nearby-price {
  color: #f56c6c;
  font-weight: 600;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

@media (max-width: 992px) {
  .grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  .grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
