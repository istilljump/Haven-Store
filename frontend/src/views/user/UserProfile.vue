<template>
  <div class="user-profile-container">
    <div class="profile-header">
      <el-card>
        <div class="header-content">
          <div class="avatar-section">
            <el-avatar :src="user?.avatar" size="large" :icon="UserFilled" />
            <el-button type="primary" size="small" @click="showUploadDialog">
              更换头像
            </el-button>
          </div>
          <div class="user-info">
            <h2>{{ user?.username }}</h2>
            <p>{{ user?.email }}</p>
            <el-tag :type="getRoleTagType(user?.role)">
              {{ getRoleText(user?.role) }}
            </el-tag>
          </div>
        </div>
      </el-card>
    </div>

    <div class="profile-content">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="基本信息" name="basic">
          <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="form.username" disabled />
            </el-form-item>
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" />
            </el-form-item>
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="form.phone" />
            </el-form-item>
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="form.nickname" />
            </el-form-item>
            <el-form-item label="个人简介" prop="bio">
              <el-input type="textarea" v-model="form.bio" rows="4" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveBasicInfo">保存</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="修改密码" name="password">
          <el-form :model="passwordForm" :rules="passwordRules" ref="passwordFormRef" label-width="100px">
            <el-form-item label="原密码" prop="oldPassword">
              <el-input v-model="passwordForm.oldPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="passwordForm.newPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="确认新密码" prop="confirmPassword">
              <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="changePassword">修改密码</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="我的发布" name="products">
          <div class="products-section">
            <div class="section-header">
              <h3>我发布的商品</h3>
              <el-button type="primary" @click="goToCreateProduct">发布新商品</el-button>
            </div>
            <div class="products-grid">
              <div v-if="loading" class="loading">
                <el-skeleton :rows="3" animated />
              </div>
              <div v-else-if="products.length === 0" class="empty">
                <el-empty description="暂无发布商品" />
              </div>
              <div v-else class="grid">
                <div v-for="product in products" :key="product.id" class="product-card">
                  <div class="product-image">
                    <img :src="product.image || '/placeholder.png'" :alt="product.title" />
                    <div class="price">¥{{ product.price }}</div>
                  </div>
                  <div class="product-info">
                    <h4 class="title">{{ product.title }}</h4>
                    <p class="description">{{ product.description }}</p>
                    <div class="meta">
                      <span class="status" :class="product.status">
                        {{ getProductStatusText(product.status) }}
                      </span>
                      <span class="time">{{ product.time }}</span>
                    </div>
                    <div class="actions">
                      <el-button size="small" type="danger" @click="deleteProduct(product.id)">删除</el-button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UserFilled } from '@element-plus/icons-vue'
import { useAuthStore } from '@/store/auth'
import { useUserStore } from '@/store'

export default {
  name: 'UserProfile',
  components: {
    UserFilled
  },
  setup() {
    const router = useRouter()
    const authStore = useAuthStore()
    const userStore = useUserStore()
    
    const activeTab = ref('basic')
    const formRef = ref(null)
    const passwordFormRef = ref(null)
    
    const user = computed(() => authStore.user)
    
    const form = reactive({
      username: '',
      email: '',
      phone: '',
      nickname: '',
      bio: ''
    })
    
    const rules = {
      email: [
        { required: true, message: '请输入邮箱', trigger: 'blur' },
        { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
      ],
      phone: [
        { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
      ]
    }
    
    const passwordForm = reactive({
      oldPassword: '',
      newPassword: '',
      confirmPassword: ''
    })
    
    const passwordRules = {
      oldPassword: [
        { required: true, message: '请输入原密码', trigger: 'blur' }
      ],
      newPassword: [
        { required: true, message: '请输入新密码', trigger: 'blur' },
        { min: 6, message: '密码长度不能少于6个字符', trigger: 'blur' }
      ],
      confirmPassword: [
        { required: true, message: '请确认新密码', trigger: 'blur' },
        {
          validator: (rule, value, callback) => {
            if (value !== passwordForm.newPassword) {
              callback(new Error('两次输入密码不一致'))
            } else {
              callback()
            }
          },
          trigger: 'blur'
        }
      ]
    }
    
    const loading = ref(false)
    const products = ref([])
    
    // 初始化表单数据
    onMounted(async () => {
      try {
        await authStore.checkAuth()
        if (user.value) {
          Object.assign(form, {
            username: user.value.username,
            email: user.value.email,
            phone: user.value.phone || '',
            nickname: user.value.nickname || '',
            bio: user.value.bio || ''
          })
        }
        loadUserProducts()
      } catch (error) {
        router.push('/auth/login')
      }
    })
    
    const saveBasicInfo = async () => {
      try {
        await formRef.value.validate()
        
        // TODO: 调用API更新用户信息
        // await userStore.updateUserInfoAsync(form)
        
        ElMessage.success('基本信息保存成功')
      } catch (error) {
        console.error('保存基本信息失败:', error)
      }
    }
    
    const changePassword = async () => {
      try {
        await passwordFormRef.value.validate()
        
        // TODO: 调用API修改密码
        // await userStore.changePassword(passwordForm)
        
        ElMessage.success('密码修改成功')
        passwordForm.oldPassword = ''
        passwordForm.newPassword = ''
        passwordForm.confirmPassword = ''
        activeTab.value = 'basic'
      } catch (error) {
        console.error('修改密码失败:', error)
      }
    }
    
    const showUploadDialog = () => {
      // TODO: 实现头像上传功能
      ElMessage.info('头像上传功能开发中')
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
    
    const loadUserProducts = async () => {
      loading.value = true
      try {
        // TODO: 调用API获取用户商品列表
        // const response = await userStore.getUserProducts({ page: 1, pageSize: 12 })
        // products.value = response.data.list
        
        // 模拟数据
        await new Promise(resolve => setTimeout(resolve, 1000))
        products.value = [
          {
            id: 1,
            title: '二手iPhone 12',
            description: '95新，功能完好',
            price: 3500,
            image: '',
            status: 'active',
            time: '2小时前'
          }
        ]
      } catch (error) {
        console.error('获取用户商品列表失败:', error)
      } finally {
        loading.value = false
      }
    }
    
    const getProductStatusText = (status) => {
      const statusMap = {
        'active': '在售',
        'sold': '已售出',
        'deleted': '已删除'
      }
      return statusMap[status] || status
    }
    
    const goToCreateProduct = () => {
      router.push('/products/create')
    }
    
    const deleteProduct = async (id) => {
      try {
        await ElMessageBox.confirm('确定要删除这个商品吗？此操作不可恢复', '确认删除', {
          type: 'warning'
        })
        
        // TODO: 调用API删除商品
        // await productApi.deleteProduct(id)
        
        ElMessage.success('商品删除成功')
        loadUserProducts()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除商品失败:', error)
        }
      }
    }
    
    return {
      activeTab,
      formRef,
      passwordFormRef,
      user,
      form,
      rules,
      passwordForm,
      passwordRules,
      loading,
      products,
      saveBasicInfo,
      changePassword,
      showUploadDialog,
      getRoleTagType,
      getRoleText,
      loadUserProducts,
      getProductStatusText,
      goToCreateProduct,
      deleteProduct
    }
  }
}
</script>

<style scoped>
.user-profile-container {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.profile-header {
  margin-bottom: 30px;
}

.header-content {
  display: flex;
  align-items: center;
  gap: 30px;
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 15px;
}

.user-info h2 {
  margin: 0 0 10px 0;
  color: #333;
}

.user-info p {
  margin: 0 0 15px 0;
  color: #666;
}

.profile-content {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.products-section {
  margin-top: 20px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-header h3 {
  margin: 0;
  color: #333;
}

.products-grid {
  margin-top: 20px;
}

.loading {
  padding: 40px;
}

.empty {
  text-align: center;
  padding: 60px 20px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.product-card {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  border: 1px solid #eee;
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
  font-size: 14px;
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
  margin-bottom: 15px;
}

.status {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.status.active {
  background: #67c23a;
  color: white;
}

.status.sold {
  background: #e6a23c;
  color: white;
}

.status.deleted {
  background: #f56c6c;
  color: white;
}

.time {
  color: #999;
  font-size: 12px;
}

.actions {
  display: flex;
  gap: 10px;
}
</style>