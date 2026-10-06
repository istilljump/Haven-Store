<template>
  <div class="admin-orders">
    <div class="page-header">
      <div class="header-title">
        <h1>订单管理</h1>
        <p>查看全平台订单与交易明细</p>
      </div>
      <div class="header-actions">
        <el-button @click="exportOrders">
          <el-icon><Download /></el-icon>
          导出数据
        </el-button>
      </div>
    </div>

    <div class="search-section">
      <el-card class="search-card">
        <el-form :inline="true" :model="searchForm" class="search-form">
          <el-form-item label="订单号">
            <el-input
              v-model="searchForm.keyword"
              placeholder="按订单号搜索"
              clearable
              @keyup.enter="handleSearch"
              @clear="handleSearch"
            />
          </el-form-item>
          <el-form-item label="订单状态">
            <el-select
              v-model="searchForm.status"
              placeholder="全部状态"
              clearable
              @change="handleSearch"
            >
              <el-option
                v-for="option in ORDER_STATUS_OPTIONS"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">
              <el-icon><Search /></el-icon>
              搜索
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>

    <el-card>
      <el-table v-loading="loading" :data="orders" stripe style="width: 100%">
        <el-table-column prop="orderNo" label="订单号" min-width="200" />
        <el-table-column prop="buyerName" label="买家" min-width="110" />
        <el-table-column prop="itemCount" label="件数" width="70" align="center" />
        <el-table-column label="总金额" width="110" align="right">
          <template #default="{ row }">
            <span class="amount">¥{{ formatMoney(row.totalAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]?.type || 'info'" size="small">
              {{ row.statusDesc }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="下单时间" width="165">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="viewOrder(row)"> 详情 </el-button>
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

    <!-- 订单详情对话框 -->
    <el-dialog v-model="detailDialog" title="订单详情" width="640px">
      <template v-if="detailOrder">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="订单号" :span="2">{{
            detailOrder.orderNo
          }}</el-descriptions-item>
          <el-descriptions-item label="买家">{{
            detailOrder.buyerName || '-'
          }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusMap[detailOrder.status]?.type || 'info'" size="small">
              {{ detailOrder.statusDesc }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="下单时间">{{
            formatDateTime(detailOrder.createTime)
          }}</el-descriptions-item>
          <el-descriptions-item label="支付时间">{{
            formatDateTime(detailOrder.payTime)
          }}</el-descriptions-item>
          <el-descriptions-item label="完成时间">{{
            formatDateTime(detailOrder.finishTime)
          }}</el-descriptions-item>
          <el-descriptions-item label="取消时间">{{
            formatDateTime(detailOrder.cancelTime)
          }}</el-descriptions-item>
          <el-descriptions-item label="买家留言" :span="2">{{
            detailOrder.remark || '无'
          }}</el-descriptions-item>
        </el-descriptions>

        <h4 class="items-title">商品明细</h4>
        <div v-for="item in detailOrder.items" :key="item.productId" class="detail-item">
          <AppImage class="item-cover" :src="item.coverImage" :alt="item.title" />
          <div class="item-info">
            <div class="item-title">{{ item.title }}</div>
            <div class="item-seller">卖家：{{ item.sellerName || '-' }}</div>
          </div>
          <span class="item-price">¥{{ formatMoney(item.price) }}</span>
        </div>
        <div class="detail-total">
          共 {{ detailOrder.itemCount }} 件，合计：
          <span class="amount-strong">¥{{ formatMoney(detailOrder.totalAmount) }}</span>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, Search } from '@element-plus/icons-vue'
import AppImage from '@/components/AppImage.vue'
import adminApi from '@/api/admin'
import { ORDER_STATUS_MAP } from '@/utils/constants'
import { formatMoney, formatDate } from '@/utils/format'

/**
 * 订单管理（管理端）
 * <p>
 * 此前管理后台完全看不到订单；本页提供全平台订单的分页查询、
 * 状态/订单号筛选、明细查看与 CSV 导出（后端 /admin/orders/**）。
 */
const loading = ref(false)
const orders = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const statusMap = ORDER_STATUS_MAP

const searchForm = reactive({
  keyword: '',
  status: ''
})

const detailDialog = ref(false)
const detailOrder = ref(null)

const formatDateTime = (time) => (time ? formatDate(time, 'YYYY-MM-DD HH:mm:ss') : '-')

const loadOrders = async () => {
  loading.value = true
  try {
    const response = await adminApi.getAdminOrders({
      page: currentPage.value,
      pageSize: pageSize.value,
      status: searchForm.status || undefined,
      keyword: searchForm.keyword || undefined
    })
    orders.value = response.records || []
    total.value = response.total || 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  loadOrders()
}

const handlePageChange = (page) => {
  currentPage.value = page
  loadOrders()
}

const handlePageSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  loadOrders()
}

const viewOrder = async (order) => {
  try {
    detailOrder.value = await adminApi.getAdminOrderDetail(order.orderNo)
    detailDialog.value = true
  } catch (error) {
    // 失败原因由拦截器提示
  }
}

const exportOrders = async () => {
  try {
    await adminApi.exportOrders({
      status: searchForm.status || undefined,
      keyword: searchForm.keyword || undefined
    })
    ElMessage.success('订单数据导出成功')
  } catch (error) {
    // 失败原因由拦截器提示
  }
}

onMounted(loadOrders)
</script>

<style scoped>
.admin-orders {
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

.amount {
  color: #f56c6c;
  font-weight: 600;
}

.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.items-title {
  margin: 16px 0 8px;
  color: #303133;
}

.detail-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
  border-bottom: 1px dashed #ebeef5;
}

.item-cover {
  width: 56px;
  height: 56px;
  border-radius: 6px;
  flex-shrink: 0;
}

.item-info {
  flex: 1;
  min-width: 0;
}

.item-title {
  font-size: 14px;
  color: #303133;
}

.item-seller {
  margin-top: 2px;
  font-size: 12px;
  color: #909399;
}

.item-price {
  color: #f56c6c;
  font-weight: 600;
}

.detail-total {
  text-align: right;
  padding-top: 10px;
  color: #606266;
}

.amount-strong {
  color: #f56c6c;
  font-size: 17px;
  font-weight: 700;
}
</style>
