<template>
  <div class="product-list-container">
    <div class="header">
      <h1>商品列表</h1>
      <el-button type="primary" @click="goToCreate">发布商品</el-button>
    </div>
    
    <div class="filter-section">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-input
            v-model="searchForm.keyword"
            placeholder="搜索商品名称"
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-col>
        <el-col :span="6">
          <el-select v-model="searchForm.category" placeholder="选择分类" clearable>
            <el-option
              v-for="item in categoryOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-col>
        <el-col :span="6">
          <el-select v-model="searchForm.priceRange" placeholder="价格区间" clearable>
            <el-option label="0-100元" value="0-100" />
            <el-option label="100-500元" value="100-500" />
            <el-option label="500-1000元" value="500-1000" />
            <el-option label="1000元以上" value="1000+" />
          </el-select>
        </el-col>
        <el-col :span="6">
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-col>
      </el-row>
    </div>
    
    <div class="product-grid">
      <div v-if="loading" class="loading">
        <el-skeleton :rows="3" animated />
      </div>
      
      <div v-else-if="products.length === 0" class="empty">
        <el-empty description="暂无商品" />
      </div>
      
      <div v-else class="grid">
        <div v-for="product in products" :key="product.id" class="product-card" @click="viewProduct(product.id)">
          <div class="product-image">
            <img :src="product.coverImage || PLACEHOLDER_IMAGE" :alt="product.title" />
            <div class="price">¥{{ product.price }}</div>
          </div>
          <div class="product-info">
            <h3 class="title">{{ product.title }}</h3>
            <p class="description">{{ product.description }}</p>
            <div class="meta">
              <span class="author">{{ product.username }}</span>
              <span class="time">{{ formatTime(product.createTime) }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <div class="pagination">
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
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import productApi from '@/api/product'
import { formatRelativeTime } from '@/utils/format'

export default {
  name: 'ProductList',
  components: {
    Search
  },
  setup() {
    const router = useRouter()
    
    // 无图片时的占位图（文件位于 public/default-product.png）
    const PLACEHOLDER_IMAGE = '/default-product.png'
    
    const products = ref([])
    const loading = ref(false)
    const total = ref(0)
    const currentPage = ref(1)
    const pageSize = ref(12)
    const categoryOptions = ref([])
    
    const searchForm = reactive({
      keyword: '',
      category: '',
      priceRange: ''
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
        console.error('加载商品分类失败:', error)
      }
    }
    
    const fetchProducts = async () => {
      loading.value = true
      try {
        const range = PRICE_RANGE_MAP[searchForm.priceRange] || [null, null]
        // 空字符串会被后端当作"未筛选"，此处直接转成 undefined，避免发出无意义的空参数
        const data = await productApi.searchProducts({
          keyword: searchForm.keyword || undefined,
          categoryId: searchForm.category || undefined,
          minPrice: range[0] === null ? undefined : range[0],
          maxPrice: range[1] === null ? undefined : range[1],
          page: currentPage.value,
          pageSize: pageSize.value
        })
        products.value = data.records || []
        total.value = data.total || 0
      } catch (error) {
        console.error('获取商品列表失败:', error)
        products.value = []
        total.value = 0
      } finally {
        loading.value = false
      }
    }
    
    const formatTime = (time) => formatRelativeTime(time) || '刚刚'
    
    const handleSearch = () => {
      currentPage.value = 1
      fetchProducts()
    }
    
    const resetSearch = () => {
      searchForm.keyword = ''
      searchForm.category = ''
      searchForm.priceRange = ''
      handleSearch()
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
      loadCategories()
      fetchProducts()
    })
    
    return {
      products,
      loading,
      total,
      currentPage,
      pageSize,
      searchForm,
      categoryOptions,
      PLACEHOLDER_IMAGE,
      formatTime,
      handleSearch,
      resetSearch,
      handleSizeChange,
      handleCurrentChange,
      viewProduct,
      goToCreate
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
  margin-bottom: 30px;
  padding: 20px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.product-grid {
  margin-bottom: 30px;
}

.loading {
  padding: 40px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.product-card {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.2s;
}

.product-card:hover {
  transform: translateY(-2px);
}

.product-image {
  position: relative;
  height: 200px;
  background: #f5f5f5;
}

.product-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.price {
  position: absolute;
  top: 10px;
  right: 10px;
  background: #ff6b6b;
  color: white;
  padding: 4px 8px;
  border-radius: 4px;
  font-weight: bold;
}

.product-info {
  padding: 15px;
}

.title {
  margin: 0 0 10px 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.description {
  margin: 0 0 15px 0;
  color: #666;
  font-size: 14px;
  line-height: 1.4;
}

.meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #999;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.empty {
  text-align: center;
  padding: 60px 20px;
}
</style>