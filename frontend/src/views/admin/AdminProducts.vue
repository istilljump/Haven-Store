<template>
  <div class="admin-products">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-title">
        <h1>商品管理</h1>
        <p>审核和管理平台商品信息</p>
      </div>
      <div class="header-actions">
        <el-button type="primary" @click="exportProducts">
          <el-icon><Download /></el-icon>
          导出数据
        </el-button>
      </div>
    </div>

    <!-- 搜索和筛选 -->
    <div class="search-section">
      <el-card class="search-card">
        <el-form :inline="true" :model="searchForm" class="search-form">
          <el-form-item label="搜索关键词">
            <el-input
              v-model="searchForm.keyword"
              placeholder="商品标题/描述"
              clearable
              @keyup.enter="handleSearch"
            />
          </el-form-item>
          <el-form-item label="商品状态">
            <el-select v-model="searchForm.status" placeholder="全部状态" clearable>
              <el-option label="全部" value="" />
              <el-option label="在售" value="1" />
              <el-option label="已售出" value="2" />
              <el-option label="已下架" value="3" />
            </el-select>
          </el-form-item>
          <el-form-item label="商品分类">
            <el-select v-model="searchForm.categoryId" placeholder="全部分类" clearable>
              <el-option label="全部分类" value="" />
              <el-option 
                v-for="category in categories" 
                :key="category.id" 
                :label="category.name" 
                :value="category.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">
              <el-icon><Search /></el-icon>
              搜索
            </el-button>
            <el-button @click="resetSearch">
              <el-icon><Refresh /></el-icon>
              重置
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>

    <!-- 商品列表 -->
    <div class="products-section">
      <el-card>
        <template #header>
          <div class="card-header">
            <span>商品列表 (共 {{ total }} 条)</span>
            <div class="header-right">
              <el-select v-model="pageSize" size="small" @change="handlePageSizeChange">
                <el-option label="10条/页" value="10" />
                <el-option label="20条/页" value="20" />
                <el-option label="50条/页" value="50" />
              </el-select>
            </div>
          </div>
        </template>

        <el-table
          v-loading="loading"
          :data="productList"
          stripe
          style="width: 100%"
        >
          <el-table-column prop="id" label="商品ID" width="80" align="center" />
          <el-table-column label="商品图片" width="100" align="center">
            <template #default="{ row }">
              <el-image
                :src="row.coverImage || '/default-product.png'"
                :preview-src-list="row.coverImage ? [row.coverImage] : []"
                :preview-teleported="true"
                fit="cover"
                style="width: 60px; height: 60px; border-radius: 8px;"
              >
                <!-- 加载失败时显示占位图，而不是 Element Plus 默认的「FAILED」文案 -->
                <template #error>
                  <img src="/default-product.png" alt="暂无图片"
                       style="width: 60px; height: 60px; border-radius: 8px;" />
                </template>
              </el-image>
            </template>
          </el-table-column>
          <el-table-column prop="title" label="商品标题" min-width="200">
            <template #default="{ row }">
              <div class="product-title">
                <div class="title">{{ row.title }}</div>
                <div class="category">分类: {{ getCategoryName(row.categoryId) }}</div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="username" label="发布者" width="120" />
          <el-table-column prop="price" label="价格" width="100" align="center">
            <template #default="{ row }">
              <span class="price">¥{{ row.price.toFixed(2) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="productCondition" label="成色" width="100" />
          <el-table-column prop="tradeType" label="交易方式" width="100">
            <template #default="{ row }">
              <el-tag :type="getTradeTypeClass(row.tradeType)" size="small">
                {{ getTradeTypeName(row.tradeType) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="发布时间" width="160">
            <template #default="{ row }">
              {{ formatDateTime(row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="getStatusClass(row.status)" size="small">
                {{ getStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200" align="center">
            <template #default="{ row }">
              <el-button 
                type="primary" 
                size="small" 
                link
                @click="viewProductDetail(row)"
              >
                查看
              </el-button>
              <el-button 
                v-if="row.status === 1"
                type="warning" 
                size="small" 
                @click="handleOfflineProduct(row)"
              >
                下架
              </el-button>
              <el-button 
                v-else-if="row.status === 3"
                type="success" 
                size="small" 
                @click="handleOnlineProduct(row)"
              >
                上架
              </el-button>
              <el-button 
                type="danger" 
                size="small" 
                link
                @click="deleteProduct(row)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <div class="pagination-container">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[10, 20, 50]"
            :total="total"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handlePageSizeChange"
            @current-change="handlePageChange"
          />
        </div>
      </el-card>
    </div>

    <!-- 商品详情对话框 -->
    <el-dialog
      v-model="productDetailDialog"
      title="商品详情"
      width="600px"
      @close="closeProductDetail"
    >
      <div v-if="selectedProduct" class="product-detail">
        <div class="detail-section">
          <h3>基本信息</h3>
          <div class="detail-grid">
            <div class="detail-item">
              <label>商品ID:</label>
              <span>{{ selectedProduct.id }}</span>
            </div>
            <div class="detail-item">
              <label>商品标题:</label>
              <span>{{ selectedProduct.title }}</span>
            </div>
            <div class="detail-item">
              <label>发布者:</label>
              <span>{{ selectedProduct.username }}</span>
            </div>
            <div class="detail-item">
              <label>发布时间:</label>
              <span>{{ formatDateTime(selectedProduct.createTime) }}</span>
            </div>
            <div class="detail-item">
              <label>商品价格:</label>
              <span class="price">¥{{ selectedProduct.price.toFixed(2) }}</span>
            </div>
            <div class="detail-item">
              <label>商品成色:</label>
              <span>{{ selectedProduct.productCondition }}</span>
            </div>
            <div class="detail-item">
              <label>交易方式:</label>
              <el-tag :type="getTradeTypeClass(selectedProduct.tradeType)" size="small">
                {{ getTradeTypeName(selectedProduct.tradeType) }}
              </el-tag>
            </div>
            <div class="detail-item">
              <label>商品状态:</label>
              <el-tag :type="getStatusClass(selectedProduct.status)" size="small">
                {{ getStatusText(selectedProduct.status) }}
              </el-tag>
            </div>
          </div>
        </div>

        <div class="detail-section">
          <h3>商品描述</h3>
          <div class="description">
            {{ selectedProduct.description || '暂无描述' }}
          </div>
        </div>

        <div v-if="selectedProduct.address" class="detail-section">
          <h3>交易地址</h3>
          <div class="address">
            {{ selectedProduct.address }}
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- 确认对话框 -->
    <el-dialog
      v-model="confirmDialog"
      :title="confirmTitle"
      width="400px"
    >
      <div class="confirm-content">
        {{ confirmMessage }}
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="confirmDialog = false">取消</el-button>
          <el-button type="primary" @click="handleConfirmAction">
            {{ confirmButtonText }}
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download, Search, Refresh } from '@element-plus/icons-vue'
import adminApi from '@/api/admin'
import { formatDate } from '@/utils/format'

const loading = ref(false)
const productList = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const productDetailDialog = ref(false)
const confirmDialog = ref(false)
const categories = ref([])

// 选中的商品
const selectedProduct = ref(null)

// 确认对话框相关
const confirmTitle = ref('')
const confirmMessage = ref('')
const confirmButtonText = ref('')
const confirmAction = ref(null)

// 搜索表单
const searchForm = reactive({
  keyword: '',
  status: '',
  categoryId: ''
})

// 加载商品列表
const loadProducts = async () => {
  try {
    loading.value = true
    
    const params = {
      page: currentPage.value,
      pageSize: pageSize.value,
      ...searchForm
    }
    
    const response = await adminApi.getAdminProducts(params)
    
    productList.value = response.records
    total.value = response.total
  } catch (error) {
    console.error('加载商品列表失败:', error)
    ElMessage.error('加载商品列表失败')
  } finally {
    loading.value = false
  }
}

// 加载分类列表
const loadCategories = async () => {
  try {
    const response = await adminApi.getAdminCategories()
    categories.value = response
  } catch (error) {
    console.error('加载分类列表失败:', error)
  }
}

// 获取分类名称
const getCategoryName = (categoryId) => {
  const category = categories.value.find(c => c.id === categoryId)
  return category ? category.name : '未知分类'
}

// 搜索处理
const handleSearch = () => {
  currentPage.value = 1
  loadProducts()
}

// 重置搜索
const resetSearch = () => {
  Object.keys(searchForm).forEach(key => {
    searchForm[key] = ''
  })
  currentPage.value = 1
  loadProducts()
}

// 分页处理
const handlePageChange = (page) => {
  currentPage.value = page
  loadProducts()
}

const handlePageSizeChange = (size) => {
  pageSize.value = parseInt(size)
  currentPage.value = 1
  loadProducts()
}

// 商品状态管理
const handleOfflineProduct = (product) => {
  confirmDialog.value = true
  confirmTitle.value = '下架商品'
  confirmMessage.value = `确定要下架商品 "${product.title}" 吗？`
  confirmButtonText.value = '下架'
  confirmAction.value = () => doOfflineProduct(product)
}

const handleOnlineProduct = (product) => {
  confirmDialog.value = true
  confirmTitle.value = '上架商品'
  confirmMessage.value = `确定要上架商品 "${product.title}" 吗？`
  confirmButtonText.value = '上架'
  confirmAction.value = () => doOnlineProduct(product)
}

// 执行下架操作
const doOfflineProduct = async (product) => {
  try {
    await adminApi.updateProductStatus(product.id, { status: 3 })
    ElMessage.success(`商品 "${product.title}" 已被下架`)
    loadProducts()
  } catch (error) {
    ElMessage.error('下架商品失败')
  } finally {
    confirmDialog.value = false
  }
}

// 执行上架操作
const doOnlineProduct = async (product) => {
  try {
    await adminApi.updateProductStatus(product.id, { status: 1 })
    ElMessage.success(`商品 "${product.title}" 已被上架`)
    loadProducts()
  } catch (error) {
    ElMessage.error('上架商品失败')
  } finally {
    confirmDialog.value = false
  }
}

// 删除商品
const deleteProduct = (product) => {
  confirmDialog.value = true
  confirmTitle.value = '删除商品'
  confirmMessage.value = `确定要删除商品 "${product.title}" 吗？删除后无法恢复！`
  confirmButtonText.value = '删除'
  confirmAction.value = () => doDeleteProduct(product)
}

// 执行删除操作
const doDeleteProduct = async (product) => {
  try {
    await adminApi.deleteProduct(product.id)
    ElMessage.success(`商品 "${product.title}" 已被删除`)
    // 删除后按当前页重新拉取，而不是只改本地数组（否则刷新就"复活"）
    loadProducts()
  } catch (error) {
    console.error('删除商品失败:', error)
  } finally {
    confirmDialog.value = false
  }
}

// 确认操作
const handleConfirmAction = async () => {
  if (confirmAction.value) {
    await confirmAction.value()
  }
}

// 查看商品详情
const viewProductDetail = (product) => {
  selectedProduct.value = { ...product }
  productDetailDialog.value = true
}

// 关闭商品详情
const closeProductDetail = () => {
  selectedProduct.value = null
  productDetailDialog.value = false
}

// 导出商品数据
const exportProducts = async () => {
  try {
    const params = {
      ...searchForm,
      export: true
    }
    
    await adminApi.exportProducts(params)
    ElMessage.success('商品数据导出成功')
  } catch (error) {
    ElMessage.error('导出商品数据失败')
  }
}

// 获取状态样式类
const getStatusClass = (status) => {
  const statusMap = {
    1: 'success',
    2: 'warning',
    3: 'info'
  }
  // ElTag 的 type 只接受 primary/success/info/warning/danger，'default' 会触发 prop 校验警告
  return statusMap[status] || 'info'
}

// 获取状态文本
const getStatusText = (status) => {
  const statusMap = {
    1: '在售',
    2: '已售出',
    3: '已下架'
  }
  return statusMap[status] || '未知'
}

// 获取交易方式样式类（取值与后端 ProductConstant 一致：线上/线下）
const getTradeTypeClass = (tradeType) => {
  const typeMap = {
    '线上': 'primary',
    '线下': 'success'
  }
  // ElTag 的 type 只接受 primary/success/info/warning/danger
  return typeMap[tradeType] || 'info'
}

// 获取交易方式文本
const getTradeTypeName = (tradeType) => {
  const typeMap = {
    '线上': '线上交易',
    '线下': '线下交易'
  }
  return typeMap[tradeType] || '未知'
}

// 格式化日期时间
const formatDateTime = (timeStr) => {
  return formatDate(timeStr, 'YYYY-MM-DD HH:mm:ss')
}

// 初始化
onMounted(() => {
  loadProducts()
  loadCategories()
})
</script>

<style scoped>
.admin-products {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}

.header-title h1 {
  font-size: 28px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 8px 0;
}

.header-title p {
  color: #909399;
  font-size: 14px;
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.search-section {
  margin-bottom: 24px;
}

.search-card {
  border-radius: 8px;
}

.search-form {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.products-section {
  margin-bottom: 24px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.product-title {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.title {
  font-weight: 500;
  color: #303133;
}

.category {
  font-size: 12px;
  color: #909399;
}

.price {
  color: #f56c6c;
  font-weight: 600;
}

.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.product-detail {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.detail-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.detail-section h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.detail-item {
  display: flex;
  align-items: center;
  padding: 8px 0;
}

.detail-item label {
  font-weight: 500;
  color: #606266;
  width: 100px;
  flex-shrink: 0;
}

.detail-item span {
  color: #303133;
  flex: 1;
}

.description {
  padding: 12px;
  background: #f5f7fa;
  border-radius: 8px;
  color: #606266;
  line-height: 1.6;
}

.address {
  padding: 12px;
  background: #f0f9ff;
  border-radius: 8px;
  color: #409eff;
}

.confirm-content {
  padding: 16px 0;
  color: #606266;
  text-align: center;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    gap: 16px;
  }
  
  .header-actions {
    width: 100%;
    justify-content: flex-end;
  }
  
  .search-form {
    flex-direction: column;
    align-items: stretch;
  }
  
  .search-form .el-form-item {
    margin-bottom: 12px;
  }
  
  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>