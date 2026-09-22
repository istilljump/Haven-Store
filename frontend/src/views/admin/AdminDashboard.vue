<template>
  <div class="admin-dashboard">
    <!-- 页面头部 -->
    <div class="dashboard-header">
      <div class="header-title">
        <h1>管理后台</h1>
        <p>欢迎使用二手商品交易市场管理系统</p>
      </div>
      <div class="header-actions">
        <el-button type="primary" @click="refreshData">
          <el-icon><Refresh /></el-icon>
          刷新数据
        </el-button>
        <el-button @click="goToLogout">
          <el-icon><SwitchButton /></el-icon>
          退出登录
        </el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-cards">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-card class="stats-card" shadow="hover">
            <div class="stats-content">
              <div class="stats-icon">
                <el-icon color="#409eff"><User /></el-icon>
              </div>
              <div class="stats-info">
                <div class="stats-number">{{ dashboardData.statistics?.totalUsers || 0 }}</div>
                <div class="stats-label">总用户数</div>
                <div class="stats-trend">
                  <span class="trend-up">
                    <el-icon><ArrowUp /></el-icon>
                    12.5%
                  </span>
                  较上月
                </div>
              </div>
            </div>
          </el-card>
        </el-col>
        
        <el-col :span="6">
          <el-card class="stats-card" shadow="hover">
            <div class="stats-content">
              <div class="stats-icon">
                <el-icon color="#67c23a"><Goods /></el-icon>
              </div>
              <div class="stats-info">
                <div class="stats-number">{{ dashboardData.statistics?.totalProducts || 0 }}</div>
                <div class="stats-label">总商品数</div>
                <div class="stats-trend">
                  <span class="trend-up">
                    <el-icon><ArrowUp /></el-icon>
                    8.3%
                  </span>
                  较上月
                </div>
              </div>
            </div>
          </el-card>
        </el-col>
        
        <el-col :span="6">
          <el-card class="stats-card" shadow="hover">
            <div class="stats-content">
              <div class="stats-icon">
                <el-icon color="#e6a23c"><Calendar /></el-icon>
              </div>
              <div class="stats-info">
                <div class="stats-number">{{ dashboardData.statistics?.todayProducts || 0 }}</div>
                <div class="stats-label">今日新增</div>
                <div class="stats-trend">
                  <span class="trend-down">
                    <el-icon><ArrowDown /></el-icon>
                    5.2%
                  </span>
                  较昨日
                </div>
              </div>
            </div>
          </el-card>
        </el-col>
        
        <el-col :span="6">
          <el-card class="stats-card" shadow="hover">
            <div class="stats-content">
              <div class="stats-icon">
                <el-icon color="#f56c6c"><ChatDotRound /></el-icon>
              </div>
              <div class="stats-info">
                <div class="stats-number">{{ dashboardData.statistics?.activeUsers || 0 }}</div>
                <div class="stats-label">活跃用户</div>
                <div class="stats-trend">
                  <span class="trend-up">
                    <el-icon><ArrowUp /></el-icon>
                    15.8%
                  </span>
                  较上月
                </div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 功能模块 -->
    <div class="function-modules">
      <el-row :gutter="20">
        <el-col :span="12">
          <el-card class="module-card" shadow="hover">
            <template #header>
              <div class="card-header">
                <span>最近发布的商品</span>
                <el-button link type="primary" @click="goToProducts">
                  查看全部 <el-icon><ArrowRight /></el-icon>
                </el-button>
              </div>
            </template>
            <div class="recent-products">
              <div 
                v-for="product in dashboardData.recentProducts" 
                :key="product.id" 
                class="product-item"
              >
                <div class="product-info">
                  <div class="product-title">{{ product.title }}</div>
                  <div class="product-meta">
                    <span :class="getStatusClass(product.status)">
                      {{ getStatusText(product.status) }}
                    </span>
                    <span class="product-time">{{ formatTime(product.createTime) }}</span>
                  </div>
                </div>
                <div class="product-price">¥{{ product.price.toFixed(2) }}</div>
              </div>
              <div v-if="!dashboardData.recentProducts?.length" class="empty-data">
                暂无最近发布的商品
              </div>
            </div>
          </el-card>
        </el-col>
        
        <el-col :span="12">
          <el-card class="module-card" shadow="hover">
            <template #header>
              <div class="card-header">
                <span>系统消息</span>
                <el-button link type="primary" @click="goToMessages">
                  查看全部 <el-icon><ArrowRight /></el-icon>
                </el-button>
              </div>
            </template>
            <div class="system-messages">
              <div 
                v-for="message in dashboardData.systemMessages" 
                :key="message.id" 
                class="message-item"
              >
                <div class="message-icon">
                  <el-icon><Bell /></el-icon>
                </div>
                <div class="message-content">
                  <div class="message-title">{{ message.title }}</div>
                  <div class="message-time">{{ formatTime(message.createTime) }}</div>
                </div>
              </div>
              <div v-if="!dashboardData.systemMessages?.length" class="empty-data">
                暂无系统消息
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 快捷操作 -->
    <div class="quick-actions">
      <el-card class="module-card" shadow="hover">
        <template #header>
          <span>快捷操作</span>
        </template>
        <div class="action-grid">
          <div class="action-item" @click="goToUsers">
            <el-icon class="action-icon"><User /></el-icon>
            <div class="action-title">用户管理</div>
            <div class="action-desc">管理用户账号和权限</div>
          </div>
          <div class="action-item" @click="goToProducts">
            <el-icon class="action-icon"><Goods /></el-icon>
            <div class="action-title">商品管理</div>
            <div class="action-desc">审核和管理商品信息</div>
          </div>
          <div class="action-item" @click="goToCategories">
            <el-icon class="action-icon"><Collection /></el-icon>
            <div class="action-title">分类管理</div>
            <div class="action-desc">设置商品分类和排序</div>
          </div>
          <div class="action-item" @click="goToMessages">
            <el-icon class="action-icon"><Bell /></el-icon>
            <div class="action-title">消息管理</div>
            <div class="action-desc">发送和管理系统消息</div>
          </div>
          <div class="action-item" @click="goToSettings">
            <el-icon class="action-icon"><Setting /></el-icon>
            <div class="action-title">系统设置</div>
            <div class="action-desc">配置系统参数和规则</div>
          </div>
          <div class="action-item" @click="goToDataStats">
            <el-icon class="action-icon"><TrendCharts /></el-icon>
            <div class="action-title">数据统计</div>
            <div class="action-desc">查看平台数据统计</div>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { 
  Refresh, 
  SwitchButton, 
  User, 
  Goods, 
  Calendar, 
  ChatDotRound, 
  ArrowUp, 
  ArrowDown,
  ArrowRight,
  Bell,
  Collection,
  Setting,
  TrendCharts
} from '@element-plus/icons-vue'
import adminApi from '@/api/admin'
import { formatRelativeTime } from '@/utils/format'

const router = useRouter()

// 后台数据
const dashboardData = reactive({
  statistics: {
    totalUsers: 0,
    totalProducts: 0,
    totalCategories: 0,
    todayProducts: 0,
    activeUsers: 0,
    pendingProducts: 0
  },
  recentProducts: [],
  systemMessages: []
})

// 加载后台数据
const loadDashboardData = async () => {
  try {
    const response = await adminApi.getAdminDashboard()
    Object.assign(dashboardData, response)
  } catch (error) {
    console.error('加载后台数据失败:', error)
    ElMessage.error('加载后台数据失败')
  }
}

// 刷新数据
const refreshData = () => {
  loadDashboardData()
}

// 格式化时间
const formatTime = (timeStr) => {
  return formatRelativeTime(timeStr)
}

// 获取状态样式类
const getStatusClass = (status) => {
  const statusMap = {
    1: 'status-active',
    2: 'status-sold',
    3: 'status-offline'
  }
  return statusMap[status] || 'status-default'
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

// 页面跳转
const goToUsers = () => {
  router.push('/admin/users')
}

const goToProducts = () => {
  router.push('/admin/products')
}

const goToCategories = () => {
  router.push('/admin/categories')
}

const goToMessages = () => {
  router.push('/admin/messages')
}

const goToSettings = () => {
  router.push('/admin/settings')
}

const goToDataStats = () => {
  router.push('/admin/stats')
}

const goToLogout = () => {
  router.push('/admin/logout')
}

// 组件挂载时加载数据
onMounted(() => {
  loadDashboardData()
})
</script>

<style scoped>
.admin-dashboard {
  padding: 20px;
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
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

/* 统计卡片 */
.stats-cards {
  margin-bottom: 24px;
}

.stats-card {
  height: 120px;
}

.stats-content {
  display: flex;
  align-items: center;
  height: 100%;
}

.stats-icon {
  width: 60px;
  height: 60px;
  border-radius: 12px;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

.stats-info {
  margin-left: 16px;
  flex: 1;
}

.stats-number {
  font-size: 28px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.stats-label {
  color: #909399;
  font-size: 14px;
  margin-bottom: 8px;
}

.stats-trend {
  display: flex;
  align-items: center;
  font-size: 12px;
}

.trend-up {
  color: #67c23a;
}

.trend-down {
  color: #f56c6c;
}

/* 功能模块 */
.function-modules {
  margin-bottom: 24px;
}

.module-card {
  height: 300px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.recent-products {
  height: 220px;
  overflow-y: auto;
}

.product-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #ebeef5;
}

.product-item:last-child {
  border-bottom: none;
}

.product-info {
  flex: 1;
}

.product-title {
  font-weight: 500;
  color: #303133;
  margin-bottom: 4px;
}

.product-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
}

.status-active {
  color: #67c23a;
}

.status-sold {
  color: #e6a23c;
}

.status-offline {
  color: #909399;
}

.product-time {
  color: #909399;
}

.product-price {
  font-weight: 600;
  color: #f56c6c;
}

.system-messages {
  height: 220px;
  overflow-y: auto;
}

.message-item {
  display: flex;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #ebeef5;
}

.message-item:last-child {
  border-bottom: none;
}

.message-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: #ecf5ff;
  color: #409eff;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
}

.message-content {
  flex: 1;
}

.message-title {
  font-weight: 500;
  color: #303133;
  margin-bottom: 4px;
}

.message-time {
  color: #909399;
  font-size: 12px;
}

.empty-data {
  text-align: center;
  color: #909399;
  padding: 40px 0;
  font-size: 14px;
}

/* 快捷操作 */
.quick-actions {
  height: 180px;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  height: 100%;
}

.action-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 20px;
  border-radius: 8px;
  background: #f8f9fa;
  border: 1px solid #e9ecef;
  cursor: pointer;
  transition: all 0.3s;
}

.action-item:hover {
  background: #e9ecef;
  transform: translateY(-2px);
}

.action-icon {
  font-size: 24px;
  color: #409eff;
  margin-bottom: 8px;
}

.action-title {
  font-weight: 500;
  color: #303133;
  margin-bottom: 4px;
}

.action-desc {
  font-size: 12px;
  color: #909399;
  text-align: center;
  line-height: 1.4;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .dashboard-header {
    flex-direction: column;
    gap: 16px;
    align-items: flex-start;
  }
  
  .header-actions {
    width: 100%;
    justify-content: flex-end;
  }
  
  .action-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 480px) {
  .stats-cards .el-col {
    margin-bottom: 16px;
  }
  
  .function-modules .el-col {
    margin-bottom: 16px;
  }
}
</style>