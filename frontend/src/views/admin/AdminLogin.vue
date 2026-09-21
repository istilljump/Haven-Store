<template>
  <div class="admin-login-container">
    <div class="admin-login-box">
      <div class="login-header">
        <div class="logo">
          <el-icon><Monitor /></el-icon>
          <h1>管理后台</h1>
        </div>
        <p>请使用管理员账号登录</p>
      </div>
      
      <el-form
        ref="loginFormRef"
        :model="loginForm"
        :rules="loginRules"
        class="login-form"
        @submit.prevent="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="请输入管理员用户名"
            prefix-icon="User"
            clearable
            size="large"
          />
        </el-form-item>
        
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            prefix-icon="Lock"
            show-password
            clearable
            size="large"
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            class="login-button"
            @click="handleLogin"
          >
            {{ loading ? '登录中...' : '登录' }}
          </el-button>
        </el-form-item>
        
        <div class="login-footer">
          <a href="/login" class="switch-to-user">返回用户登录</a>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Monitor, User, Lock } from '@element-plus/icons-vue'
import adminApi from '@/api/admin'
import { setToken, setUserInfo } from '@/utils/auth'

const router = useRouter()
const loginFormRef = ref()
const loading = ref(false)

// 登录表单数据
const loginForm = reactive({
  username: 'admin',
  password: '123456'
})

// 表单验证规则
const loginRules = {
  username: [
    { required: true, message: '请输入管理员用户名', trigger: 'blur' },
    { min: 2, max: 20, message: '用户名长度为 2-20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为 6-20 个字符', trigger: 'blur' }
  ]
}

// 处理登录
const handleLogin = async () => {
  try {
    // 表单验证
    await loginFormRef.value.validate()
    
    loading.value = true
    
    // 调用登录API
    const response = await adminApi.adminLogin(loginForm)
    
    // 保存Token和用户信息
    setToken(response.token)
    setUserInfo({
      userId: response.adminInfo.adminId,
      username: response.adminInfo.username,
      nickname: response.adminInfo.nickname,
      phone: response.adminInfo.phone,
      isAdmin: true,
      create_time: response.adminInfo.createTime
    })
    
    ElMessage.success('管理员登录成功')
    
    // 跳转到管理后台首页
    router.push('/admin/dashboard')
    
  } catch (error) {
    console.error('管理员登录失败:', error)
    ElMessage.error(error.message || '管理员登录失败，请检查账号和密码')
  } finally {
    loading.value = false
  }
}

// 键盘快捷键
const handleKeydown = (e) => {
  if (e.key === 'Enter') {
    handleLogin()
  }
}

onMounted(() => {
  // 添加键盘事件监听
  window.addEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.admin-login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.admin-login-box {
  background: white;
  border-radius: 12px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
  width: 100%;
  max-width: 400px;
  padding: 40px;
}

.login-header {
  text-align: center;
  margin-bottom: 40px;
}

.logo {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
}

.logo .el-icon {
  font-size: 32px;
  margin-right: 12px;
  color: #409eff;
}

.logo h1 {
  font-size: 28px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.login-header p {
  color: #606266;
  font-size: 14px;
  margin-top: 8px;
}

.login-form {
  margin-top: 20px;
}

.login-button {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 500;
  margin-top: 8px;
}

.login-footer {
  text-align: center;
  margin-top: 24px;
  margin-bottom: 8px;
}

.switch-to-user {
  color: #606266;
  text-decoration: none;
  font-size: 14px;
  transition: color 0.3s;
}

.switch-to-user:hover {
  color: #409eff;
}

/* 响应式设计 */
@media (max-width: 480px) {
  .admin-login-box {
    margin: 0 20px;
    padding: 30px 25px;
  }
  
  .logo h1 {
    font-size: 24px;
  }
}
</style>