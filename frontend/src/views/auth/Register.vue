<template>
  <div class="register-page">
    <h2 class="register-title">创建账号</h2>
    <p class="register-subtitle">加入 Haven-Store，让闲置好物流转起来</p>
    <el-form :model="form" :rules="rules" ref="formRef" label-position="top" @submit.prevent="handleSubmit">
      <el-form-item label="用户名" prop="username">
        <el-input
          v-model="form.username"
          placeholder="3-20 个字符，注册后不可修改"
          maxlength="20"
          show-word-limit
          clearable
        />
      </el-form-item>
      <el-form-item label="昵称" prop="nickname">
        <el-input v-model="form.nickname" placeholder="展示给其他用户的名字" maxlength="30" clearable />
      </el-form-item>
      <el-form-item label="手机号" prop="phone">
        <el-input v-model="form.phone" placeholder="11 位手机号，用于线下交易联系" maxlength="11" clearable />
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input
          v-model="form.password"
          type="password"
          placeholder="6-20 位；建议同时包含字母和数字"
          show-password
          autocomplete="new-password"
        />
      </el-form-item>
      <el-form-item label="确认密码" prop="confirmPassword">
        <el-input
          v-model="form.confirmPassword"
          type="password"
          placeholder="请再次输入密码"
          show-password
          autocomplete="new-password"
        />
      </el-form-item>
      <el-button type="primary" class="register-submit" :loading="submitting" native-type="submit">
        注 册
      </el-button>
      <div class="register-footer">
        已有账号？
        <router-link to="/auth/login">直接登录</router-link>
      </div>
    </el-form>
  </div>
</template>

<script>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store'

/**
 * 用户注册页
 * <p>
 * 字段与后端 RegisterDTO 严格对齐：username / password / confirmPassword / phone / nickname。
 * 注册成功后跳转登录页并带上用户名，免去重复输入。
 */
export default {
  name: 'Register',
  setup() {
    const router = useRouter()
    const userStore = useUserStore()
    const formRef = ref(null)
    const submitting = ref(false)

    const form = reactive({
      username: '',
      nickname: '',
      phone: '',
      password: '',
      confirmPassword: ''
    })

    const validateConfirmPassword = (rule, value, callback) => {
      if (value !== form.password) {
        callback(new Error('两次输入的密码不一致'))
      } else {
        callback()
      }
    }

    const rules = {
      username: [
        { required: true, message: '请输入用户名', trigger: 'blur' },
        { min: 3, max: 20, message: '长度须为 3-20 个字符', trigger: 'blur' }
      ],
      nickname: [
        { required: true, message: '请输入昵称', trigger: 'blur' },
        { max: 30, message: '昵称不能超过 30 个字符', trigger: 'blur' }
      ],
      phone: [
        { required: true, message: '请输入手机号', trigger: 'blur' },
        { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
      ],
      password: [
        { required: true, message: '请输入密码', trigger: 'blur' },
        { min: 6, max: 20, message: '长度须为 6-20 个字符', trigger: 'blur' }
      ],
      confirmPassword: [
        { required: true, message: '请确认密码', trigger: 'blur' },
        { validator: validateConfirmPassword, trigger: 'blur' }
      ]
    }

    const handleSubmit = async () => {
      const valid = await formRef.value.validate().catch(() => false)
      if (!valid) return
      submitting.value = true
      try {
        await userStore.register({
          username: form.username.trim(),
          nickname: form.nickname.trim(),
          phone: form.phone.trim(),
          password: form.password,
          confirmPassword: form.confirmPassword
        })
        ElMessage.success('注册成功，请登录')
        router.push({ path: '/auth/login', query: { username: form.username.trim() } })
      } catch (err) {
        // 错误提示（用户名重复、手机号被占用等）由 axios 拦截器统一弹出，这里不再重复提示
      } finally {
        submitting.value = false
      }
    }

    return { formRef, form, rules, submitting, handleSubmit }
  }
}
</script>

<style scoped>
.register-page {
  width: 100%;
}

.register-title {
  margin: 0 0 6px;
  font-size: 22px;
  color: #303133;
  text-align: center;
}

.register-subtitle {
  margin: 0 0 22px;
  font-size: 13px;
  color: #909399;
  text-align: center;
}

.register-submit {
  width: 100%;
  margin-top: 4px;
}

.register-footer {
  margin-top: 14px;
  text-align: center;
  font-size: 13px;
  color: #909399;
}

.register-footer a {
  color: #409eff;
  text-decoration: none;
}
</style>
