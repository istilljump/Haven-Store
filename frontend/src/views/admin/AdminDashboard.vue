<template>
  <div class="admin-dashboard">
    <!-- 页面头部 -->
    <div class="dashboard-header">
      <div class="header-title">
        <h1>管理后台</h1>
        <p>欢迎使用 Haven-Store 管理系统</p>
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
                <div class="stats-number">{{ dashboardData.statistics.totalUsers }}</div>
                <div class="stats-label">总用户数</div>
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
                <div class="stats-number">{{ dashboardData.statistics.totalProducts }}</div>
                <div class="stats-label">总商品数</div>
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
                <div class="stats-number">{{ dashboardData.statistics.todayProducts }}</div>
                <div class="stats-label">今日新增</div>
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
                <div class="stats-number">{{ dashboardData.statistics.activeUsers }}</div>
                <div class="stats-label">正常状态用户</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 数据图表：近 7 日趋势 + 分类分布 -->
    <div id="dashboard-charts" class="chart-section">
      <el-row :gutter="20">
        <el-col :span="14">
          <el-card class="module-card" shadow="hover">
            <template #header>
              <div class="card-header">
                <span>近 7 日新增趋势</span>
              </div>
            </template>
            <div ref="trendChartRef" class="chart-box" />
          </el-card>
        </el-col>
        <el-col :span="10">
          <el-card class="module-card" shadow="hover">
            <template #header>
              <div class="card-header">
                <span>商品分类分布</span>
              </div>
            </template>
            <div ref="categoryChartRef" class="chart-box" />
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
                <div class="product-price">¥{{ Number(product.price || 0).toFixed(2) }}</div>
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
import { ref, reactive, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Refresh,
  SwitchButton,
  User,
  Goods,
  Calendar,
  ChatDotRound,
  ArrowRight,
  Bell,
  Collection,
  Setting,
  TrendCharts
} from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import adminApi from '@/api/admin'
import { removeToken, removeUserInfo } from '@/utils/auth'
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

// ==================== 图表 ====================
const trendChartRef = ref(null)
const categoryChartRef = ref(null)
let trendChart = null
let categoryChart = null

// [轮7授权] 自定暗色注入：按当前主题返回图例/轴文字/分割线/饼图描边颜色
// （画布颜色硬编码于本文件，属已授权的图表初始化配置级改动；系列色保持品牌色不变）
const isDarkTheme = () => document.documentElement.classList.contains('dark')
const chartPalette = () =>
  isDarkTheme()
    ? {
        legendText: '#aeb8c7',
        axisLabel: '#8d97a8',
        axisLine: '#2a3442',
        splitLine: '#232c37',
        pieBorder: '#171e28'
      }
    : {
        legendText: '#606266',
        axisLabel: '#909399',
        axisLine: '#dcdfe6',
        splitLine: '#e0e6ed',
        pieBorder: '#ffffff'
      }

// 主题切换重绘用的数据缓存（避免重复请求接口）
let lastTrendData = null
let lastCategoryStats = null

const renderTrendChart = (trend) => {
  if (!trendChartRef.value) return
  if (!trendChart) {
    trendChart = echarts.init(trendChartRef.value)
  }
  lastTrendData = trend
  const pal = chartPalette()
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: {
      data: ['新增用户', '新增商品', '新增订单'],
      top: 0,
      textStyle: { color: pal.legendText }
    },
    grid: { left: 40, right: 20, top: 34, bottom: 28 },
    xAxis: {
      type: 'category',
      data: trend.map((p) => p.date.slice(5)),
      axisLabel: { color: pal.axisLabel },
      axisLine: { lineStyle: { color: pal.axisLine } }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      axisLabel: { color: pal.axisLabel },
      splitLine: { lineStyle: { color: pal.splitLine } }
    },
    series: [
      {
        name: '新增用户',
        type: 'line',
        smooth: true,
        data: trend.map((p) => p.newUsers),
        itemStyle: { color: '#409eff' }
      },
      {
        name: '新增商品',
        type: 'line',
        smooth: true,
        data: trend.map((p) => p.newProducts),
        itemStyle: { color: '#67c23a' }
      },
      {
        name: '新增订单',
        type: 'line',
        smooth: true,
        data: trend.map((p) => p.newOrders),
        itemStyle: { color: '#e6a23c' }
      }
    ]
  })
}

const renderCategoryChart = (stats) => {
  if (!categoryChartRef.value) return
  if (!categoryChart) {
    categoryChart = echarts.init(categoryChartRef.value)
  }
  lastCategoryStats = stats
  const pal = chartPalette()
  categoryChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} 件（{d}%）' },
    legend: {
      type: 'scroll',
      orient: 'vertical',
      right: 0,
      top: 'middle',
      textStyle: { color: pal.legendText }
    },
    series: [
      {
        type: 'pie',
        radius: ['38%', '66%'],
        center: ['42%', '50%'],
        avoidLabelOverlap: true,
        itemStyle: { borderRadius: 6, borderColor: pal.pieBorder, borderWidth: 2 },
        label: { show: false },
        data: stats.map((s) => ({ name: s.categoryName || `分类${s.categoryId}`, value: s.count }))
      }
    ]
  })
}

// [轮7授权] 主题切换时用缓存数据重绘图表（配色随主题更新，不重新请求接口）
const handleThemeChangeForCharts = () => {
  if (lastTrendData) renderTrendChart(lastTrendData)
  if (lastCategoryStats) renderCategoryChart(lastCategoryStats)
}

const loadCharts = async () => {
  try {
    const [trend, categoryStats] = await Promise.all([
      adminApi.getTrendStats(7),
      adminApi.getCategoryStats()
    ])
    await nextTick()
    renderTrendChart(trend || [])
    renderCategoryChart(categoryStats || [])
  } catch (error) {
    // 图表加载失败不影响看板其余内容
  }
}

const handleResize = () => {
  trendChart?.resize()
  categoryChart?.resize()
}

// 加载后台数据
const loadDashboardData = async () => {
  try {
    const response = await adminApi.getAdminDashboard()
    Object.assign(dashboardData, response)
  } catch (error) {
    // 失败原因由请求拦截器统一弹出
  }
}

// 刷新数据
const refreshData = () => {
  loadDashboardData()
  loadCharts()
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
  // 数据图表就在本页（近 7 日趋势 + 分类分布），滚动过去即可
  document.getElementById('dashboard-charts')?.scrollIntoView({ behavior: 'smooth' })
}

// 退出登录：清空登录态后回到管理登录页
// 此前跳的是 /admin/logout，该路由并不存在，点击必然 404
const goToLogout = () => {
  ElMessageBox.confirm('确定要退出管理员账号吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(() => {
      removeToken()
      removeUserInfo()
      ElMessage.success('退出成功')
      router.push('/admin/login')
    })
    .catch(() => {})
}

// 组件挂载时加载数据与图表；窗口尺寸变化时重绘；主题切换时按新配色重绘
onMounted(() => {
  loadDashboardData()
  loadCharts()
  window.addEventListener('resize', handleResize)
  window.addEventListener('haven-theme-change', handleThemeChangeForCharts)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  window.removeEventListener('haven-theme-change', handleThemeChangeForCharts)
  trendChart?.dispose()
  categoryChart?.dispose()
  trendChart = null
  categoryChart = null
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
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 24px;
}

.header-title h1 {
  font-size: 22px;
  font-weight: 600;
  color: var(--hs-text-primary, #303133);
  margin: 0 0 8px 0;
}

.header-title p {
  color: var(--hs-text-secondary, #909399);
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

/* 图表区 */
.chart-section {
  margin-bottom: 24px;
}

.chart-box {
  width: 100%;
  height: 320px;
}

/* [UI美化] 统计卡 hover 轻浮起，反馈更灵敏 */
.stats-card {
  height: 120px;
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease;
}

.stats-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(16, 24, 40, 0.1);
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
  background: var(--hs-bg-panel, #f8fafd);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

.stats-info {
  margin-left: 16px;
  flex: 1;
}

/* [UI美化] 统计数字加重并使用等宽数字，读数更稳 */
.stats-number {
  font-size: 28px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  color: var(--hs-text-primary, #303133);
  margin-bottom: 4px;
}

.stats-label {
  color: var(--hs-text-secondary, #909399);
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
  color: var(--hs-price, #f56c6c);
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
  border-bottom: 1px solid var(--hs-border, #ebeef5);
}

.product-item:last-child {
  border-bottom: none;
}

.product-info {
  flex: 1;
}

.product-title {
  font-weight: 500;
  color: var(--hs-text-primary, #303133);
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
  color: var(--hs-text-secondary, #909399);
}

.product-time {
  color: var(--hs-text-secondary, #909399);
}

.product-price {
  font-weight: 600;
  color: var(--hs-price, #f56c6c);
}

.system-messages {
  height: 220px;
  overflow-y: auto;
}

.message-item {
  display: flex;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid var(--hs-border, #ebeef5);
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
  color: var(--hs-text-primary, #303133);
  margin-bottom: 4px;
}

.message-time {
  color: var(--hs-text-secondary, #909399);
  font-size: 12px;
}

.empty-data {
  text-align: center;
  color: var(--hs-text-secondary, #909399);
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

/* [UI美化] 快捷操作块统一浅灰面板色，hover 换淡蓝底并加轻浮起 */
.action-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 20px;
  border-radius: 8px;
  background: var(--hs-bg-panel, #f8fafd);
  border: 1px solid var(--hs-border-light, #f0f2f7);
  cursor: pointer;
  transition: all 0.2s ease;
}

.action-item:hover {
  background: #f5f9ff;
  border-color: #c6e2ff;
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(16, 24, 40, 0.1);
}

.action-icon {
  font-size: 24px;
  color: #409eff;
  margin-bottom: 8px;
}

.action-title {
  font-weight: 500;
  color: var(--hs-text-primary, #303133);
  margin-bottom: 4px;
}

.action-desc {
  font-size: 12px;
  color: var(--hs-text-secondary, #909399);
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

  /* [UI美化-本轮] 小屏堆叠：统计卡/图表/功能模块的固定 span 列强制占满整行 */
  .stats-cards .el-col,
  .chart-section .el-col,
  .function-modules .el-col {
    flex: 0 0 100%;
    max-width: 100%;
  }

  .stats-cards .el-col {
    margin-bottom: 12px;
  }

  .chart-section .el-col,
  .function-modules .el-col {
    margin-bottom: 16px;
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
