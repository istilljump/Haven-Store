<template>
  <div class="admin-messages">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-title">
        <h1>系统消息</h1>
        <p>管理和发送平台系统消息</p>
      </div>
      <div class="header-actions">
        <el-button type="primary" @click="showSendDialog">
          <el-icon><Plus /></el-icon>
          发送消息
        </el-button>
      </div>
    </div>

    <!-- 搜索和筛选 -->
    <div class="search-section">
      <el-card class="search-card">
        <el-form :inline="true" :model="searchForm" class="search-form">
          <el-form-item label="消息类型">
            <el-select v-model="searchForm.userType" placeholder="全部类型" clearable>
              <el-option label="全部" value="" />
              <el-option label="所有用户" value="all" />
              <el-option label="仅管理员" value="admin" />
              <el-option label="仅普通用户" value="user" />
            </el-select>
          </el-form-item>
          <el-form-item label="消息状态">
            <el-select v-model="searchForm.status" placeholder="全部状态" clearable>
              <el-option label="全部" value="" />
              <el-option label="发送中" value="1" />
              <el-option label="已送达" value="2" />
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

    <!-- 消息列表 -->
    <div class="messages-section">
      <el-card>
        <template #header>
          <div class="card-header">
            <span>消息列表 (共 {{ total }} 条)</span>
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
          :data="messageList"
          stripe
          style="width: 100%"
        >
          <el-table-column prop="id" label="消息ID" width="80" align="center" />
          <el-table-column prop="title" label="消息标题" min-width="200">
            <template #default="{ row }">
              <div class="message-title">
                <el-icon class="message-icon" :class="getUserTypeClass(row.userType)">
                  <Bell />
                </el-icon>
                <span>{{ row.title }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="接收用户" width="150" align="center">
            <template #default="{ row }">
              <el-tag :type="getUserTypeClass(row.userType)" size="small">
                {{ getUserTypeName(row.userType) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="发送时间" width="160">
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
          <el-table-column label="操作" width="150" align="center">
            <template #default="{ row }">
              <el-button 
                type="primary" 
                size="small" 
                link
                @click="viewMessageDetail(row)"
              >
                查看
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

    <!-- 发送消息对话框 -->
    <el-dialog
      v-model="sendDialog"
      title="发送系统消息"
      width="500px"
      @close="closeSendDialog"
    >
      <el-form
        ref="sendFormRef"
        :model="sendForm"
        :rules="sendRules"
        label-width="80px"
      >
        <el-form-item label="消息标题" prop="title">
          <el-input 
            v-model="sendForm.title" 
            placeholder="请输入消息标题"
            maxlength="50"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="接收用户" prop="userType">
          <el-radio-group v-model="sendForm.userType">
            <el-radio label="all">所有用户</el-radio>
            <el-radio label="admin">仅管理员</el-radio>
            <el-radio label="user">仅普通用户</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="消息内容" prop="content">
          <el-input
            v-model="sendForm.content"
            type="textarea"
            :rows="6"
            placeholder="请输入消息内容"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="sendDialog = false">取消</el-button>
          <el-button type="primary" @click="sendMessage">
            发送消息
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 消息详情对话框 -->
    <el-dialog
      v-model="messageDetailDialog"
      title="消息详情"
      width="500px"
      @close="closeMessageDetail"
    >
      <div v-if="selectedMessage" class="message-detail">
        <div class="detail-item">
          <label>消息ID:</label>
          <span>{{ selectedMessage.id }}</span>
        </div>
        <div class="detail-item">
          <label>消息标题:</label>
          <span>{{ selectedMessage.title }}</span>
        </div>
        <div class="detail-item">
          <label>接收用户:</label>
          <el-tag :type="getUserTypeClass(selectedMessage.userType)" size="small">
            {{ getUserTypeName(selectedMessage.userType) }}
          </el-tag>
        </div>
        <div class="detail-item">
          <label>发送时间:</label>
          <span>{{ formatDateTime(selectedMessage.createTime) }}</span>
        </div>
        <div class="detail-item">
          <label>消息状态:</label>
          <el-tag :type="getStatusClass(selectedMessage.status)" size="small">
            {{ getStatusText(selectedMessage.status) }}
          </el-tag>
        </div>
        <div class="detail-item">
          <label>消息内容:</label>
          <div class="message-content">
            {{ selectedMessage.content }}
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- 消息统计 -->
    <div class="message-stats">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-card class="stats-card" shadow="hover">
            <div class="stats-content">
              <div class="stats-icon">
                <el-icon color="#409eff"><ChatDotRound /></el-icon>
              </div>
              <div class="stats-info">
                <div class="stats-number">{{ total }}</div>
                <div class="stats-label">总消息数</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stats-card" shadow="hover">
            <div class="stats-content">
              <div class="stats-icon">
                <el-icon color="#67c23a"><Bell /></el-icon>
              </div>
              <div class="stats-info">
                <div class="stats-number">{{ sentCount }}</div>
                <div class="stats-label">已发送</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stats-card" shadow="hover">
            <div class="stats-content">
              <div class="stats-icon">
                <el-icon color="#e6a23c"><Clock /></el-icon>
              </div>
              <div class="stats-info">
                <div class="stats-number">{{ pendingCount }}</div>
                <div class="stats-label">发送中</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stats-card" shadow="hover">
            <div class="stats-content">
              <div class="stats-icon">
                <el-icon color="#909399"><Document /></el-icon>
              </div>
              <div class="stats-info">
                <div class="stats-number">{{ todayCount }}</div>
                <div class="stats-label">今日新增</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Search, Refresh, Bell, ChatDotRound, Clock, Document } from '@element-plus/icons-vue'
import adminApi from '@/api/admin'
import { formatDate } from '@/utils/format'

const loading = ref(false)
const sendFormRef = ref()
const sendDialog = ref(false)
const messageDetailDialog = ref(false)
const messageList = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

// 选中的消息
const selectedMessage = ref(null)

// 发送消息表单
const sendForm = reactive({
  title: '',
  content: '',
  userType: 'all'
})

// 搜索表单
const searchForm = reactive({
  userType: '',
  status: ''
})

// 表单验证规则
const sendRules = {
  title: [
    { required: true, message: '请输入消息标题', trigger: 'blur' },
    { min: 2, max: 50, message: '消息标题长度为 2-50 个字符', trigger: 'blur' }
  ],
  content: [
    { required: true, message: '请输入消息内容', trigger: 'blur' },
    { min: 10, max: 500, message: '消息内容长度为 10-500 个字符', trigger: 'blur' }
  ],
  userType: [
    { required: true, message: '请选择接收用户', trigger: 'change' }
  ]
}

// 统计数据
const sentCount = computed(() => 
  messageList.value.filter(m => m.status === 2).length
)

const pendingCount = computed(() => 
  messageList.value.filter(m => m.status === 1).length
)

const todayCount = computed(() => {
  const today = new Date().toDateString()
  return messageList.value.filter(m => 
    new Date(m.createTime).toDateString() === today
  ).length
})

// 加载消息列表
const loadMessages = async () => {
  try {
    loading.value = true
    
    const params = {
      page: currentPage.value,
      pageSize: pageSize.value,
      ...searchForm
    }
    
    const response = await adminApi.getAdminMessages(params)
    
    messageList.value = response.records
    total.value = response.total
  } catch (error) {
    console.error('加载消息列表失败:', error)
    ElMessage.error('加载消息列表失败')
  } finally {
    loading.value = false
  }
}

// 显示发送消息对话框
const showSendDialog = () => {
  Object.keys(sendForm).forEach(key => {
    sendForm[key] = key === 'userType' ? 'all' : ''
  })
  sendDialog.value = true
}

// 发送消息
const sendMessage = async () => {
  try {
    await sendFormRef.value.validate()
    
    loading.value = true
    
    await adminApi.sendSystemMessage(sendForm)
    ElMessage.success('消息发送成功')
    sendDialog.value = false
    loadMessages()
  } catch (error) {
    console.error('发送消息失败:', error)
    ElMessage.error('发送消息失败')
  } finally {
    loading.value = false
  }
}

// 关闭发送消息对话框
const closeSendDialog = () => {
  sendFormRef.value?.resetFields()
  sendDialog.value = false
}

// 查看消息详情
const viewMessageDetail = (message) => {
  selectedMessage.value = { ...message }
  messageDetailDialog.value = true
}

// 关闭消息详情
const closeMessageDetail = () => {
  selectedMessage.value = null
  messageDetailDialog.value = false
}

// 获取用户类型样式类
const getUserTypeClass = (userType) => {
  const typeMap = {
    'all': 'primary',
    'admin': 'warning',
    'user': 'success'
  }
  return typeMap[userType] || 'info'
}

// 获取用户类型文本
const getUserTypeName = (userType) => {
  const typeMap = {
    'all': '所有用户',
    'admin': '仅管理员',
    'user': '仅普通用户'
  }
  return typeMap[userType] || '未知'
}

// 获取状态样式类
const getStatusClass = (status) => {
  const statusMap = {
    1: 'warning',
    2: 'success'
  }
  return statusMap[status] || 'info'
}

// 获取状态文本
const getStatusText = (status) => {
  const statusMap = {
    1: '发送中',
    2: '已送达'
  }
  return statusMap[status] || '未知'
}

// 搜索处理
const handleSearch = () => {
  currentPage.value = 1
  loadMessages()
}

// 重置搜索
const resetSearch = () => {
  Object.keys(searchForm).forEach(key => {
    searchForm[key] = ''
  })
  currentPage.value = 1
  loadMessages()
}

// 分页处理
const handlePageChange = (page) => {
  currentPage.value = page
  loadMessages()
}

const handlePageSizeChange = (size) => {
  pageSize.value = parseInt(size)
  currentPage.value = 1
  loadMessages()
}

// 格式化日期时间
const formatDateTime = (timeStr) => {
  return formatDate(timeStr, 'YYYY-MM-DD HH:mm:ss')
}

// 初始化
onMounted(() => {
  loadMessages()
})
</script>

<style scoped>
.admin-messages {
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

.messages-section {
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

.message-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.message-icon {
  color: #409eff;
}

.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.message-detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 8px 0;
}

.detail-item label {
  font-weight: 500;
  color: #606266;
  min-width: 80px;
  flex-shrink: 0;
}

.detail-item span {
  color: #303133;
  flex: 1;
}

.message-content {
  background: #f5f7fa;
  padding: 12px;
  border-radius: 8px;
  color: #606266;
  line-height: 1.6;
}

/* 消息统计 */
.message-stats {
  margin-top: 24px;
}

.stats-card {
  height: 100px;
}

.stats-content {
  display: flex;
  align-items: center;
  height: 100%;
}

.stats-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.stats-info {
  margin-left: 16px;
  flex: 1;
}

.stats-number {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.stats-label {
  color: #909399;
  font-size: 14px;
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
  
  .message-stats .el-col {
    margin-bottom: 16px;
  }
}
</style>