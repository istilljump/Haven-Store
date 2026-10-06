<template>
  <div class="product-list-container">
    <div class="header">
      <h1>商品列表</h1>
      <el-button type="primary" @click="goToCreate">发布商品</el-button>
    </div>

    <div class="filter-section">
      <el-row :gutter="14" class="filter-row">
        <el-col :xs="24" :sm="7">
          <el-input
            v-model="searchForm.keyword"
            placeholder="搜标题 / 描述 / 品牌 / 型号"
            clearable
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-col>
        <el-col :xs="12" :sm="5">
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
        <el-col :xs="12" :sm="5">
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
        <el-col :xs="12" :sm="4">
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
          <ProductCard v-for="product in products" :key="product.id" :product="product" />
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
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import ProductCard from '@/components/ProductCard.vue'
import AppImage from '@/components/AppImage.vue'
import productApi from '@/api/product'
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
  components: { Search, ProductCard, AppImage },
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

    const nearbyMode = ref(false)
    const locating = ref(false)
    const radius = ref(MAP_CONFIG.DEFAULT_RADIUS)
    const location = ref(null)

    const searchForm = reactive({
      keyword: '',
      category: '',
      priceRange: '',
      sort: 'latest'
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
        } else {
          const range = PRICE_RANGE_MAP[searchForm.priceRange] || [null, null]
          // 空字符串会被后端当作"未筛选"，此处直接转成 undefined，避免发出无意义的空参数
          const data = await productApi.searchProducts({
            keyword: searchForm.keyword || undefined,
            categoryId: searchForm.category || undefined,
            minPrice: range[0] === null ? undefined : range[0],
            maxPrice: range[1] === null ? undefined : range[1],
            sort: searchForm.sort || undefined,
            page: currentPage.value,
            pageSize: pageSize.value
          })
          products.value = data.records || []
          total.value = data.total || 0
        }
      } catch (error) {
        products.value = []
        total.value = 0
        loadError.value = true
      } finally {
        loading.value = false
      }
    }

    const handleSearch = () => {
      currentPage.value = 1
      fetchProducts()
    }

    const resetSearch = () => {
      searchForm.keyword = ''
      searchForm.category = ''
      searchForm.priceRange = ''
      searchForm.sort = 'latest'
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
      // 首页/详情页跳转过来的预置筛选（?keyword=xxx、?categoryId=x、?mode=nearby）
      if (route.query.keyword) {
        searchForm.keyword = String(route.query.keyword)
      }
      if (route.query.categoryId) {
        searchForm.category = Number(route.query.categoryId)
      }
      loadCategories()
      fetchProducts()
      if (route.query.mode === 'nearby') {
        toggleNearby()
      }
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
      nearbyMode,
      locating,
      radius,
      handleSearch,
      resetSearch,
      toggleNearby,
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
