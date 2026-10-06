<template>
  <div class="admin-reports">
    <div class="page-header">
      <div class="header-title">
        <h1>举报处理</h1>
        <p>处理用户提交的商品/评论举报</p>
      </div>
    </div>

    <div class="search-section">
      <el-card class="search-card">
        <el-form :inline="true" :model="searchForm" class="search-form">
          <el-form-item label="处理状态">
            <el-select v-model="searchForm.status" placeholder="全部状态" clearable @change="handleSearch">
              <el-option label="待处理" :value="0" />
              <el-option label="已处理" :value="1" />
              <el-option label="已驳回" :value="2" />
            </el-select>
          </el-form-item>
          <el-form-item label="对象类型">
            <el-select v-model="searchForm.targetType" placeholder="全部类型" clearable @change="handleSearch">
              <el-option label="商品" value="product" />
              <el-option label="评论" value="comment" />
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
      <el-table v-loading="loading" :data="reports" stripe style="width: 100%">
        <el-table-column prop="id" label="举报ID" width="80" align="center" />
        <el-table-column label="类型" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.targetType === 'product' ? 'primary' : 'info'" size="small">
              {{ row.targetType === 'product' ? '商品' : '评论' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="被举报内容" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.targetSummary">{{ row.targetSummary }}</span>
            <span v-else class="target-missing">（对象已删除）</span>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="原因" width="100" />
        <el-table-column prop="description" label="补充说明" min-width="160" show-overflow-tooltip />
        <el-table-column prop="reporterUsername" label="举报人" width="100" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]?.type" size="small">
              {{ statusMap[row.status]?.label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="举报时间" width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="110" align="center">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 0"
              type="primary"
              size="small"
              @click="openHandleDialog(row)"
            >
              处理
            </el-button>
            <el-button
              v-else
              type="info"
              size="small"
              link
              @click="openHandleDialog(row)"
            >
              查看结果
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

    <!-- 处理对话框 -->
    <el-dialog v-model="handleDialog" :title="handleTarget.status === 0 ? '处理举报' : '举报处理结果'" width="460px">
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item label="被举报内容">
          {{ handleTarget.targetSummary || '（对象已删除）' }}
        </el-descriptions-item>
        <el-descriptions-item label="举报原因">{{ handleTarget.reason }}</el-descriptions-item>
        <el-descriptions-item label="补充说明">{{ handleTarget.description || '无' }}</el-descriptions-item>
        <el-descriptions-item label="举报人">{{ handleTarget.reporterUsername || '-' }}</el-descriptions-item>
        <el-descriptions-item v-if="handleTarget.handleAction" label="处理动作">
          {{ ACTION_TEXT[handleTarget.handleAction] || handleTarget.handleAction }}
        </el-descriptions-item>
        <el-descriptions-item v-if="handleTarget.handleNote" label="处理备注">
          {{ handleTarget.handleNote }}
        </el-descriptions-item>
      </el-descriptions>

      <template v-if="handleTarget.status === 0">
        <el-form label-width="90px" style="margin-top: 14px">
          <el-form-item label="处理动作">
            <el-radio-group v-model="handleForm.action">
              <el-radio
                v-if="handleTarget.targetType === 'product'"
                label="takeDownProduct"
              >下架商品</el-radio>
              <el-radio
                v-if="handleTarget.targetType === 'comment'"
                label="hideComment"
              >隐藏评论</el-radio>
              <el-radio label="dismiss">驳回举报</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="处理备注">
            <el-input
              v-model="handleForm.note"
              type="textarea"
              :rows="2"
              maxlength="255"
              show-word-limit
              placeholder="选填，记录处理依据"
            />
          </el-form-item>
        </el-form>
      </template>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="handleDialog = false">关闭</el-button>
          <el-button
            v-if="handleTarget.status === 0"
            type="primary"
            :loading="handling"
            @click="submitHandle"
          >
            提交处理
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import adminApi from '@/api/admin'
import { formatDate } from '@/utils/format'

/**
 * 举报处理台（管理端）
 * <p>
 * 与用户端商品详情页的「举报该商品」闭环：待处理举报排在最前，
 * 处理动作支持下架商品 / 隐藏评论 / 驳回，处理记录保留动作与备注。
 */
const ACTION_TEXT = {
  takeDownProduct: '下架被举报商品',
  hideComment: '隐藏被举报评论',
  dismiss: '驳回举报（未采取措施）'
}

const STATUS_MAP = {
  0: { label: '待处理', type: 'warning' },
  1: { label: '已处理', type: 'success' },
  2: { label: '已驳回', type: 'info' }
}

const loading = ref(false)
const reports = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const statusMap = STATUS_MAP

const searchForm = reactive({
  status: '',
  targetType: ''
})

const handleDialog = ref(false)
const handling = ref(false)
const handleTarget = reactive({})
const handleForm = reactive({ action: 'dismiss', note: '' })

const formatDateTime = (time) => (time ? formatDate(time, 'YYYY-MM-DD HH:mm:ss') : '-')

const loadReports = async () => {
  loading.value = true
  try {
    const response = await adminApi.getAdminReports({
      page: currentPage.value,
      pageSize: pageSize.value,
      status: searchForm.status === '' ? undefined : searchForm.status,
      targetType: searchForm.targetType || undefined
    })
    reports.value = response.records || []
    total.value = response.total || 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  loadReports()
}

const handlePageChange = (page) => {
  currentPage.value = page
  loadReports()
}

const handlePageSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  loadReports()
}

const openHandleDialog = (row) => {
  Object.assign(handleTarget, row)
  handleForm.action = row.targetType === 'product' ? 'takeDownProduct'
    : row.targetType === 'comment' ? 'hideComment' : 'dismiss'
  handleForm.note = ''
  handleDialog.value = true
}

const submitHandle = async () => {
  handling.value = true
  try {
    await adminApi.handleReport(handleTarget.id, {
      action: handleForm.action,
      note: handleForm.note || undefined
    })
    ElMessage.success('举报已处理')
    handleDialog.value = false
    loadReports()
  } catch (error) {
    // 失败原因由拦截器提示
  } finally {
    handling.value = false
  }
}

onMounted(loadReports)
</script>

<style scoped>
.admin-reports {
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

.target-missing {
  color: #909399;
}

.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>
