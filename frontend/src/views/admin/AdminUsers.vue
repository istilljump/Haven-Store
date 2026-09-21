<template>
  <div class="admin-users">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-title">
        <h1>用户管理</h1>
        <p>管理系统用户账号和权限</p>
      </div>
      <div class="header-actions">
        <el-button type="primary" @click="exportUsers">
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
              placeholder="用户名/昵称/手机号"
              clearable
              @keyup.enter="handleSearch"
            />
          </el-form-item>
          <el-form-item label="用户状态">
            <el-select v-model="searchForm.status" placeholder="全部状态" clearable>
              <el-option label="全部" value="" />
              <el-option label="正常" value="1" />
              <el-option label="禁用" value="2" />
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

    <!-- 用户列表 -->
    <div class="users-section">
      <el-card>
        <template #header>
          <div class="card-header">
            <span>用户列表 (共 {{ total }} 条)</span>
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
          :data="userList"
          stripe
          style="width: 100%"
        >
          <el-table-column prop="id" label="用户ID" width="80" align="center" />
          <el-table-column prop="username" label="用户名" min-width="120">
            <template #default="{ row }">
              <div class="user-info">
                <el-avatar :size="24" :src="row.avatar || '/default-avatar.png'" class="user-avatar" />
                <span class="username">{{ row.username }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="nickname" label="昵称" min-width="100" />
          <el-table-column prop="phone" label="手机号" min-width="120" />
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag 
                :type="row.status === 1 ? 'success' : 'danger'" 
                size="small"
              >
                {{ row.status === 1 ? '正常' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="isAdmin" label="角色" width="80" align="center">
            <template #default="{ row }">
              <el-tag 
                :type="row.isAdmin ? 'warning' : 'info'" 
                size="small"
              >
                {{ row.isAdmin ? '管理员' : '普通用户' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="注册时间" width="160">
            <template #default="{ row }">
              {{ formatDateTime(row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150" align="center">
            <template #default="{ row }">
              <el-button 
                v-if="row.status === 1 && !row.isAdmin"
                type="danger" 
                size="small" 
                @click="handleDisableUser(row)"
              >
                禁用
              </el-button>
              <el-button 
                v-else-if="row.status === 2 && !row.isAdmin"
                type="success" 
                size="small" 
                @click="handleEnableUser(row)"
              >
                启用
              </el-button>
              <el-button 
                type="info" 
                size="small" 
                link
                @click="viewUserDetail(row)"
              >
                详情
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

    <!-- 用户详情对话框 -->
    <el-dialog
      v-model="userDetailDialog"
      title="用户详情"
      width="500px"
      @close="closeUserDetail"
    >
      <div v-if="selectedUser" class="user-detail">
        <div class="detail-item">
          <label>用户ID:</label>
          <span>{{ selectedUser.id }}</span>
        </div>
        <div class="detail-item">
          <label>用户名:</label>
          <span>{{ selectedUser.username }}</span>
        </div>
        <div class="detail-item">
          <label>昵称:</label>
          <span>{{ selectedUser.nickname }}</span>
        </div>
        <div class="detail-item">
          <label>手机号:</label>
          <span>{{ selectedUser.phone }}</span>
        </div>
        <div class="detail-item">
          <label>角色:</label>
          <el-tag :type="selectedUser.isAdmin ? 'warning' : 'info'" size="small">
            {{ selectedUser.isAdmin ? '管理员' : '普通用户' }}
          </el-tag>
        </div>
        <div class="detail-item">
          <label>状态:</label>
          <el-tag :type="selectedUser.status === 1 ? 'success' : 'danger'" size="small">
            {{ selectedUser.status === 1 ? '正常' : '禁用' }}
          </el-tag>
        </div>
        <div class="detail-item">
          <label>注册时间:</label>
          <span>{{ formatDateTime(selectedUser.createTime) }}</span>
        </div>
        <div class="detail-item">
          <label>最后更新:</label>
          <span>{{ formatDateTime(selectedUser.updateTime) }}</span>
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
const userList = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const userDetailDialog = ref(false)
const confirmDialog = ref(false)

// 选中的用户
const selectedUser = ref(null)

// 确认对话框相关
const confirmTitle = ref('')
const confirmMessage = ref('')
const confirmButtonText = ref('')
const confirmAction = ref(null)

// 搜索表单
const searchForm = reactive({
  keyword: '',
  status: ''
})

// 加载用户列表
const loadUsers = async () => {
  try {
    loading.value = true
    
    const params = {
      page: currentPage.value,
      pageSize: pageSize.value,
      ...searchForm
    }
    
    const response = await adminApi.getAdminUsers(params)
    
    userList.value = response.records
    total.value = response.total
  } catch (error) {
    console.error('加载用户列表失败:', error)
    ElMessage.error('加载用户列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索处理
const handleSearch = () => {
  currentPage.value = 1
  loadUsers()
}

// 重置搜索
const resetSearch = () => {
  Object.keys(searchForm).forEach(key => {
    searchForm[key] = ''
  })
  currentPage.value = 1
  loadUsers()
}

// 分页处理
const handlePageChange = (page) => {
  currentPage.value = page
  loadUsers()
}

const handlePageSizeChange = (size) => {
  pageSize.value = parseInt(size)
  currentPage.value = 1
  loadUsers()
}

// 禁用用户
const handleDisableUser = (user) => {
  confirmDialog.value = true
  confirmTitle.value = '禁用用户'
  confirmMessage.value = `确定要禁用用户 "${user.username}" 吗？`
  confirmButtonText.value = '禁用'
  confirmAction.value = () => doDisableUser(user)
}

// 启用用户
const handleEnableUser = (user) => {
  confirmDialog.value = true
  confirmTitle.value = '启用用户'
  confirmMessage.value = `确定要启用用户 "${user.username}" 吗？`
  confirmButtonText.value = '启用'
  confirmAction.value = () => doEnableUser(user)
}

// 执行禁用用户操作
const doDisableUser = async (user) => {
  try {
    await adminApi.updateUserStatus(user.id, { status: 2 })
    ElMessage.success(`用户 "${user.username}" 已被禁用`)
    loadUsers()
  } catch (error) {
    ElMessage.error('禁用用户失败')
  } finally {
    confirmDialog.value = false
  }
}

// 执行启用用户操作
const doEnableUser = async (user) => {
  try {
    await adminApi.updateUserStatus(user.id, { status: 1 })
    ElMessage.success(`用户 "${user.username}" 已被启用`)
    loadUsers()
  } catch (error) {
    ElMessage.error('启用用户失败')
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

// 查看用户详情
const viewUserDetail = (user) => {
  selectedUser.value = { ...user }
  userDetailDialog.value = true
}

// 关闭用户详情
const closeUserDetail = () => {
  selectedUser.value = null
  userDetailDialog.value = false
}

// 导出用户数据
const exportUsers = async () => {
  try {
    const params = {
      ...searchForm,
      export: true
    }
    
    await adminApi.exportUsers(params)
    ElMessage.success('用户数据导出成功')
  } catch (error) {
    ElMessage.error('导出用户数据失败')
  }
}

// 格式化日期时间
const formatDateTime = (timeStr) => {
  return formatDate(timeStr, 'YYYY-MM-DD HH:mm:ss')
}

// 初始化
onMounted(() => {
  loadUsers()
})
</script>

<style scoped>
.admin-users {
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

.users-section {
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

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-avatar {
  border: 1px solid #e4e7ed;
}

.username {
  font-weight: 500;
}

.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.user-detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-item {
  display: flex;
  align-items: center;
}

.detail-item label {
  font-weight: 500;
  color: #606266;
  width: 80px;
  flex-shrink: 0;
}

.detail-item span {
  color: #303133;
  flex: 1;
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
}
</style>