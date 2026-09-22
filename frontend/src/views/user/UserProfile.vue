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
                    <img :src="product.coverImage || '/default-product.png'" :alt="product.title" />
                    <div class="price">¥{{ product.price }}</div>
                  </div>
                  <div class="product-info">
                    <h4 class="title">{{ product.title }}</h4>
                    <p class="description">{{ product.description }}</p>
                    <div class="meta">
                      <span class="status" :class="'s' + product.status">
                        {{ getProductStatusText(product.status) }}
                      </span>
                      <span class="time">{{ product.time }}</span>
                    </div>
                    <div class="actions">
                      <el-button size="small" @click="router.push(`/products/${product.id}/edit`)">编辑</el-button>
                      <el-button
                        v-if="product.status === 1"
                        size="small"
                        type="danger"
                        @click="offlineProduct(product.id)"
                      >下架</el-button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
    <!-- 更换头像对话框 -->
    <el-dialog v-model="avatarDialog" title="更换头像" width="420px">
      <div class="avatar-upload">
        <el-upload
          action="/api/user/avatar"
          :headers="avatarHeaders"
          accept="image/*"
          :show-file-list="false"
          :on-success="handleAvatarSuccess"
          :on-error="handleAvatarError"
        >
          <img :src="user?.avatar || '/default-avatar.png'" class="avatar-preview" alt="头像预览" />
          <div class="avatar-tip">点击图片选择新头像（jpg/png/gif/webp，不超过 5MB）</div>
        </el-upload>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UserFilled } from '@element-plus/icons-vue'
import { useAuthStore } from '@/store/auth'
import { useUserStore } from '@/store'
import { getToken } from '@/utils/auth'
import productApi from '@/api/product'
import userApi from '@/api/user'

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
    const saving = ref(false)
    const avatarDialog = ref(false)
    
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
      } catch (error) {
        // 表单校验未通过，错误已就地提示
        return
      }
      try {
        saving.value = true
        // 用户名是登录账号不可改，因此只提交可编辑字段
        await userApi.updateUserInfo({
          nickname: form.nickname,
          phone: form.phone,
          email: form.email,
          bio: form.bio
        })
        // 重新拉取，保证页面展示与后端一致（手机号等会被后端做唯一性处理）
        await userStore.getUserInfo()
        ElMessage.success('基本信息保存成功')
      } catch (error) {
        console.error('保存基本信息失败:', error)
      } finally {
        saving.value = false
      }
    }
    
    const changePassword = async () => {
      try {
        await passwordFormRef.value.validate()
      } catch (error) {
        return
      }
      try {
        saving.value = true
        await userApi.changePassword({
          oldPassword: passwordForm.oldPassword,
          newPassword: passwordForm.newPassword,
          confirmPassword: passwordForm.confirmPassword
        })
        ElMessage.success('密码修改成功')
        passwordForm.oldPassword = ''
        passwordForm.newPassword = ''
        passwordForm.confirmPassword = ''
        passwordFormRef.value && passwordFormRef.value.resetFields()
        activeTab.value = 'basic'
      } catch (error) {
        console.error('修改密码失败:', error)
      } finally {
        saving.value = false
      }
    }
    
    const showUploadDialog = () => {
      avatarDialog.value = true
    }

    // 头像上传走 el-upload 自己的 XHR，不会经过 axios 拦截器，需要手动带 Token
    const avatarHeaders = computed(() => ({ Authorization: 'Bearer ' + (getToken() || '') }))

    // 上传成功：后端返回新头像地址，刷新用户信息后更新头像展示
    const handleAvatarSuccess = async (response) => {
      const url = response && response.data
      if (!url) {
        ElMessage.error('头像上传失败')
        return
      }
      try {
        await userStore.getUserInfo()
        ElMessage.success('头像已更新')
        avatarDialog.value = false
      } catch (error) {
        console.error('刷新用户信息失败:', error)
      }
    }

    const handleAvatarError = () => {
      ElMessage.error('头像上传失败，请检查图片格式与大小')
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
    
    // 我的发布：来自 GET /product/mine（当前登录用户发布的商品）
    const loadUserProducts = async () => {
      loading.value = true
      try {
        const data = await productApi.getMyProducts({ page: 1, pageSize: 50 })
        products.value = data.records || []
      } catch (error) {
        console.error('获取用户商品列表失败:', error)
        products.value = []
      } finally {
        loading.value = false
      }
    }
    
    // 状态取值与后端 ProductStatusEnum 一致：1 在售 / 2 已售出 / 3 已下架
    const getProductStatusText = (status) => {
      const statusMap = { 1: '在售', 2: '已售出', 3: '已下架' }
      return statusMap[status] || '未知'
    }
    
    const getProductStatusType = (status) => {
      const statusMap = { 1: 'success', 2: 'warning', 3: 'info' }
      return statusMap[status] || 'info'
    }
    
    const goToCreateProduct = () => {
      router.push('/products/create')
    }
    
    // 下架商品（软下架：状态改为已下架，数据保留，可在管理端或重新上架恢复）
    const offlineProduct = async (id) => {
      try {
        await ElMessageBox.confirm('确定要下架这个商品吗？下架后不会出现在商品列表中。', '确认下架', {
          type: 'warning'
        })
        await productApi.removeProduct(id)
        ElMessage.success('商品已下架')
        loadUserProducts()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('下架商品失败:', error)
        }
      }
    }
    
    return {
      saving,
      avatarDialog,
      avatarHeaders,
      handleAvatarSuccess,
      handleAvatarError,
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
      getProductStatusType,
      goToCreateProduct,
      offlineProduct
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

.avatar-upload {
  text-align: center;
}

.avatar-preview {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  border: 1px solid #e4e7ed;
  cursor: pointer;
  object-fit: cover;
}

.avatar-tip {
  margin-top: 12px;
  color: #909399;
  font-size: 13px;
}

.status.s1 {
  background: #67c23a;
  color: white;
}

.status.s2 {
  background: #e6a23c;
  color: white;
}

.status.s3 {
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