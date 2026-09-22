<template>
  <div class="user-list-container">
    <div class="header">
      <h1>用户列表</h1>
      <el-button type="primary" @click="goToCreate">添加用户</el-button>
    </div>
    
    <div class="filter-section">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-input
            v-model="searchForm.keyword"
            placeholder="搜索用户名"
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-col>
        <el-col :span="6">
          <el-select v-model="searchForm.role" placeholder="用户角色" clearable>
            <el-option label="普通用户" value="user" />
            <el-option label="管理员" value="admin" />
            <el-option label="超级管理员" value="super_admin" />
          </el-select>
        </el-col>
        <el-col :span="6">
          <el-select v-model="searchForm.status" placeholder="用户状态" clearable>
            <el-option label="正常" value="active" />
            <el-option label="禁用" value="inactive" />
          </el-select>
        </el-col>
        <el-col :span="6">
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-col>
      </el-row>
    </div>
    
    <div class="user-table">
      <el-table :data="users" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" width="150" />
        <el-table-column prop="email" label="邮箱" width="200" />
        <el-table-column prop="role" label="角色" width="120">
          <template #default="{ row }">
            <el-tag :type="getRoleTagType(row.role)">
              {{ getRoleText(row.role) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'active' ? 'success' : 'danger'">
              {{ row.status === 'active' ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="registerTime" label="注册时间" width="180" />
        <el-table-column prop="lastLoginTime" label="最后登录" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="viewUser(row.id)">查看</el-button>
            <el-button size="small" type="warning" @click="editUser(row.id)">编辑</el-button>
            <el-button 
              size="small" 
              :type="row.status === 'active' ? 'danger' : 'success'" 
              @click="toggleUserStatus(row)"
            >
              {{ row.status === 'active' ? '禁用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
    
    <div class="pagination">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
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

export default {
  name: 'UserList',
  components: {
    Search
  },
  setup() {
    const router = useRouter()
    
    const users = ref([])
    const loading = ref(false)
    const total = ref(0)
    const currentPage = ref(1)
    const pageSize = ref(20)
    
    const searchForm = reactive({
      keyword: '',
      role: '',
      status: ''
    })
    
    const fetchUsers = async () => {
      loading.value = true
      try {
        // TODO: 实现用户列表API调用
        // const response = await api.getUsers({
        //   page: currentPage.value,
        //   size: pageSize.value,
        //   ...searchForm
        // })
        // users.value = response.data.list
        // total.value = response.data.total
        
        // 模拟数据
        await new Promise(resolve => setTimeout(resolve, 1000))
        users.value = [
          {
            id: 1,
            username: 'admin',
            email: 'admin@example.com',
            role: 'super_admin',
            status: 'active',
            registerTime: '2023-01-01 10:00:00',
            lastLoginTime: '2024-01-15 14:30:00'
          },
          {
            id: 2,
            username: 'zhangsan',
            email: 'zhangsan@example.com',
            role: 'user',
            status: 'active',
            registerTime: '2023-02-15 09:15:00',
            lastLoginTime: '2024-01-14 16:20:00'
          },
          {
            id: 3,
            username: 'lisi',
            email: 'lisi@example.com',
            role: 'user',
            status: 'inactive',
            registerTime: '2023-03-20 14:45:00',
            lastLoginTime: '2023-12-01 10:30:00'
          }
        ]
        total.value = 3
      } catch (error) {
        console.error('获取用户列表失败:', error)
      } finally {
        loading.value = false
      }
    }
    
    const getRoleTagType = (role) => {
      const roleMap = {
        'super_admin': 'danger',
        'admin': 'warning',
        'user': 'info'
      }
      return roleMap[role] || 'info'
    }
    
    const getRoleText = (role) => {
      const roleMap = {
        'super_admin': '超级管理员',
        'admin': '管理员',
        'user': '普通用户'
      }
      return roleMap[role] || role
    }
    
    const handleSearch = () => {
      currentPage.value = 1
      fetchUsers()
    }
    
    const resetSearch = () => {
      searchForm.keyword = ''
      searchForm.role = ''
      searchForm.status = ''
      handleSearch()
    }
    
    const handleSizeChange = (val) => {
      pageSize.value = val
      fetchUsers()
    }
    
    const handleCurrentChange = (val) => {
      currentPage.value = val
      fetchUsers()
    }
    
    const viewUser = (id) => {
      router.push(`/users/${id}`)
    }
    
    const editUser = (id) => {
      router.push(`/users/${id}/edit`)
    }
    
    const toggleUserStatus = async (user) => {
      try {
        // TODO: 实现用户状态切换API调用
        // await api.toggleUserStatus(user.id, user.status === 'active' ? 'inactive' : 'active')
        
        // 模拟操作
        await new Promise(resolve => setTimeout(resolve, 500))
        user.status = user.status === 'active' ? 'inactive' : 'active'
        
        ElMessage.success(`用户已${user.status === 'active' ? '启用' : '禁用'}`)
      } catch (error) {
        console.error('切换用户状态失败:', error)
        ElMessage.error('操作失败')
      }
    }
    
    const goToCreate = () => {
      router.push('/users/create')
    }
    
    onMounted(() => {
      fetchUsers()
    })
    
    return {
      users,
      loading,
      total,
      currentPage,
      pageSize,
      searchForm,
      handleSearch,
      resetSearch,
      handleSizeChange,
      handleCurrentChange,
      viewUser,
      editUser,
      toggleUserStatus,
      goToCreate,
      getRoleTagType,
      getRoleText
    }
  }
}
</script>

<style scoped>
.user-list-container {
  padding: 20px;
  max-width: 1400px;
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
  margin-bottom: 20px;
  padding: 20px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.user-table {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 20px;
  padding: 20px;
}
</style>