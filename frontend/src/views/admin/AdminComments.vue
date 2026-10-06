<template>
  <div class="admin-comments">
    <div class="page-header">
      <div class="header-title">
        <h1>评论管理</h1>
        <p>审核平台商品评价，处理违规内容</p>
      </div>
    </div>

    <div class="search-section">
      <el-card class="search-card">
        <el-form :inline="true" :model="searchForm" class="search-form">
          <el-form-item label="商品ID">
            <el-input
              v-model="searchForm.productId"
              placeholder="按商品 ID 过滤"
              clearable
              @keyup.enter="handleSearch"
              @clear="handleSearch"
            />
          </el-form-item>
          <el-form-item label="评论状态">
            <el-select
              v-model="searchForm.status"
              placeholder="全部状态"
              clearable
              @change="handleSearch"
            >
              <el-option label="正常" :value="1" />
              <el-option label="隐藏" :value="0" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">
              <el-icon><Search /></el-icon>
              查询
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>

    <el-card>
      <el-table v-loading="loading" :data="comments" stripe style="width: 100%">
        <el-table-column prop="id" label="评论ID" width="80" align="center" />
        <el-table-column label="商品" min-width="180">
          <template #default="{ row }">
            <div class="product-cell">
              <span class="product-title">{{ row.productTitle || `商品 #${row.productId}` }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="username" label="评论人" width="110" />
        <el-table-column label="评分" width="150">
          <template #default="{ row }">
            <el-rate :model-value="row.rating" disabled size="small" />
          </template>
        </el-table-column>
        <el-table-column prop="content" label="评论内容" min-width="220" show-overflow-tooltip />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '正常' : '隐藏' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="评论时间" width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 1"
              type="warning"
              size="small"
              plain
              @click="updateStatus(row, 0)"
            >
              隐藏
            </el-button>
            <el-button v-else type="success" size="small" plain @click="updateStatus(row, 1)">
              恢复
            </el-button>
            <el-button type="danger" size="small" plain @click="removeComment(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

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
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import adminApi from '@/api/admin'
import { formatDate } from '@/utils/format'

/**
 * 评论管理（管理端）
 * <p>
 * comment.status 的「隐藏」位此前一直没有入口；本页提供全平台评论的查询、
 * 隐藏/恢复与删除。隐藏的评论不在前台展示，保留数据便于追溯。
 */
const loading = ref(false)
const comments = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

const searchForm = reactive({
  productId: '',
  status: ''
})

const formatDateTime = (time) => (time ? formatDate(time, 'YYYY-MM-DD HH:mm:ss') : '-')

const loadComments = async () => {
  loading.value = true
  try {
    const response = await adminApi.getAdminComments({
      page: currentPage.value,
      pageSize: pageSize.value,
      productId: searchForm.productId || undefined,
      status: searchForm.status === '' ? undefined : searchForm.status
    })
    comments.value = response.records || []
    total.value = response.total || 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  loadComments()
}

const handlePageChange = (page) => {
  currentPage.value = page
  loadComments()
}

const handlePageSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  loadComments()
}

const updateStatus = async (row, status) => {
  try {
    await adminApi.updateCommentStatus(row.id, { status })
    ElMessage.success(status === 1 ? '评论已恢复展示' : '评论已隐藏')
    loadComments()
  } catch (error) {
    // 失败原因由拦截器提示
  }
}

const removeComment = (row) => {
  ElMessageBox.confirm('删除后不可恢复，确定删除该评论吗？', '删除评论', { type: 'warning' })
    .then(async () => {
      await adminApi.deleteComment(row.id)
      ElMessage.success('评论已删除')
      loadComments()
    })
    .catch(() => {})
}

onMounted(loadComments)
</script>

<style scoped>
.admin-comments {
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

.search-section {
  margin-bottom: 24px;
}

.product-title {
  color: #303133;
}

.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>
