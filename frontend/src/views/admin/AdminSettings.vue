<template>
  <div class="admin-settings">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-title">
        <h1>系统设置</h1>
        <p>配置系统参数和规则</p>
      </div>
      <div class="header-actions">
        <el-button @click="resetSettings">
          <el-icon><Refresh /></el-icon>
          重置默认
        </el-button>
        <el-button type="primary" @click="saveSettings">
          <el-icon><Check /></el-icon>
          保存设置
        </el-button>
      </div>
    </div>

    <!-- 设置选项卡 -->
    <div class="settings-container">
      <el-card class="settings-card">
        <el-tabs v-model="activeTab" type="card">
          <!-- 站点设置 -->
          <el-tab-pane label="站点设置" name="site">
            <div class="settings-section">
              <h3>基本信息</h3>
              <el-form :model="settings.site" label-width="120px">
                <el-form-item label="站点名称">
                  <el-input 
                    v-model="settings.site.name" 
                    placeholder="请输入站点名称"
                    maxlength="50"
                    show-word-limit
                  />
                </el-form-item>
                <el-form-item label="站点描述">
                  <el-input 
                    v-model="settings.site.description" 
                    type="textarea"
                    :rows="3"
                    placeholder="请输入站点描述"
                    maxlength="200"
                    show-word-limit
                  />
                </el-form-item>
                <el-form-item label="站点Logo">
                  <el-upload
                    class="upload-demo"
                    drag
                    :auto-upload="false"
                    :show-file-list="false"
                    accept="image/*"
                    :on-change="handleLogoChange"
                  >
                    <el-icon class="el-icon--upload"><upload-filled /></el-icon>
                    <div class="el-upload__text">
                      将文件拖到此处，或<em>点击上传</em>
                    </div>
                    <template #tip>
                      <div class="el-upload__tip">
                        只能上传jpg/png文件，且不超过5MB
                      </div>
                    </template>
                  </el-upload>
                  <div v-if="settings.site.logo" class="logo-preview">
                    <img :src="settings.site.logo" alt="Logo预览" />
                    <el-button 
                      type="danger" 
                      size="small" 
                      link
                      @click="removeLogo"
                    >
                      删除
                    </el-button>
                  </div>
                </el-form-item>
                <el-form-item label="备案号">
                  <el-input 
                    v-model="settings.site.icp" 
                    placeholder="请输入ICP备案号"
                    maxlength="50"
                  />
                </el-form-item>
              </el-form>
            </div>
          </el-tab-pane>

          <!-- 上传设置 -->
          <el-tab-pane label="上传设置" name="upload">
            <div class="settings-section">
              <h3>文件上传</h3>
              <el-form :model="settings.upload" label-width="120px">
                <el-form-item label="文件大小限制">
                  <el-input-number 
                    v-model="settings.upload.maxFileSize" 
                    :min="1024" 
                    :max="10240"
                    controls-position="right"
                  >
                    <template #suffix>KB</template>
                  </el-input-number>
                  <span class="form-tip">（最大允许上传的文件大小，单位KB）</span>
                </el-form-item>
                <el-form-item label="允许的文件类型">
                  <el-checkbox-group v-model="settings.upload.allowedTypes">
                    <el-checkbox label="jpg">JPG</el-checkbox>
                    <el-checkbox label="jpeg">JPEG</el-checkbox>
                    <el-checkbox label="png">PNG</el-checkbox>
                    <el-checkbox label="gif">GIF</el-checkbox>
                    <el-checkbox label="webp">WEBP</el-checkbox>
                  </el-checkbox-group>
                </el-form-item>
                <el-form-item label="最大图片数量">
                  <el-input-number 
                    v-model="settings.upload.maxImages" 
                    :min="1" 
                    :max="20"
                    controls-position="right"
                  />
                  <span class="form-tip">（单个商品最多上传的图片数量）</span>
                </el-form-item>
              </el-form>
            </div>
          </el-tab-pane>

          <!-- 交易设置 -->
          <el-tab-pane label="交易设置" name="trade">
            <div class="settings-section">
              <h3>交易规则</h3>
              <el-form :model="settings.trade" label-width="120px">
                <el-form-item label="AI估价">
                  <el-switch v-model="settings.trade.autoEstimate" />
                  <span class="form-tip">（启用AI智能估价功能）</span>
                </el-form-item>
                <el-form-item label="估价置信度">
                  <el-slider 
                    v-model="settings.trade.estimateConfidence" 
                    :min="0" 
                    :max="100"
                    :step="5"
                    :format-tooltip="value => `${value}%`"
                  />
                  <span class="form-tip">（AI估价结果的置信度阈值）</span>
                </el-form-item>
                <el-form-item label="商品价格上限">
                  <el-input-number 
                    v-model="settings.trade.maxPrice" 
                    :min="1000" 
                    :max="10000000"
                    :precision="2"
                    controls-position="right"
                  >
                    <template #suffix>元</template>
                  </el-input-number>
                  <span class="form-tip">（单个商品的最大价格限制）</span>
                </el-form-item>
                <el-form-item label="自动下架时间">
                  <el-input-number 
                    v-model="settings.trade.autoOfflineHours" 
                    :min="24" 
                    :max="720"
                    controls-position="right"
                  >
                    <template #suffix>小时</template>
                  </el-input-number>
                  <span class="form-tip">（商品发布后自动下架的时间，0表示不自动下架）</span>
                </el-form-item>
              </el-form>
            </div>
          </el-tab-pane>

          <!-- 联系方式 -->
          <el-tab-pane label="联系方式" name="contact">
            <div class="settings-section">
              <h3>客服信息</h3>
              <el-form :model="settings.contact" label-width="120px">
                <el-form-item label="客服邮箱">
                  <el-input 
                    v-model="settings.contact.email" 
                    placeholder="请输入客服邮箱"
                    maxlength="100"
                  />
                </el-form-item>
                <el-form-item label="客服电话">
                  <el-input 
                    v-model="settings.contact.phone" 
                    placeholder="请输入客服电话"
                    maxlength="20"
                  />
                </el-form-item>
                <el-form-item label="公司地址">
                  <el-input 
                    v-model="settings.contact.address" 
                    type="textarea"
                    :rows="3"
                    placeholder="请输入公司地址"
                    maxlength="200"
                    show-word-limit
                  />
                </el-form-item>
                <el-form-item label="工作时间">
                  <el-time-picker 
                    v-model="settings.contact.workTime" 
                    is-range 
                    range-separator="至"
                    start-placeholder="开始时间"
                    end-placeholder="结束时间"
                    format="HH:mm"
                    value-format="HH:mm"
                  />
                </el-form-item>
              </el-form>
            </div>
          </el-tab-pane>

          <!-- 安全设置 -->
          <el-tab-pane label="安全设置" name="security">
            <div class="settings-section">
              <h3>账户安全</h3>
              <el-form :model="settings.security" label-width="120px">
                <el-form-item label="密码强度要求">
                  <el-select v-model="settings.security.passwordStrength" placeholder="请选择密码强度要求">
                    <el-option label="低（6位以上）" value="low" />
                    <el-option label="中（8位以上，包含数字字母）" value="medium" />
                    <el-option label="高（8位以上，包含数字字母符号）" value="high" />
                  </el-select>
                </el-form-item>
                <el-form-item label="登录失败锁定">
                  <el-input-number 
                    v-model="settings.security.loginAttempts" 
                    :min="3" 
                    :max="10"
                    controls-position="right"
                  />
                  <span class="form-tip">（登录失败次数达到该值后锁定账户）</span>
                </el-form-item>
                <el-form-item label="账户锁定时间">
                  <el-input-number 
                    v-model="settings.security.lockDuration" 
                    :min="5" 
                    :max="1440"
                    controls-position="right"
                  >
                    <template #suffix>分钟</template>
                  </el-input-number>
                  <span class="form-tip">（账户锁定持续的时间）</span>
                </el-form-item>
                <el-form-item label="会话超时">
                  <el-input-number 
                    v-model="settings.security.sessionTimeout" 
                    :min="30" 
                    :max="1440"
                    controls-position="right"
                  >
                    <template #suffix>分钟</template>
                  </el-input-number>
                  <span class="form-tip">（用户会话超时时间）</span>
                </el-form-item>
              </el-form>
            </div>
          </el-tab-pane>
        </el-tabs>
      </el-card>
    </div>

    <!-- 保存成功提示 -->
    <el-notification
      v-model="showSuccess"
      title="保存成功"
      message="系统设置已成功保存"
      type="success"
      duration="3000"
      @close="showSuccess = false"
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Check, UploadFilled } from '@element-plus/icons-vue'
import adminApi from '@/api/admin'
import { validateEmail, validatePhone } from '@/utils/validate'

const activeTab = ref('site')
const showSuccess = ref(false)
const loading = ref(false)

// 默认设置
const defaultSettings = {
  site: {
    name: '二手商品交易市场',
    description: '专业的二手商品交易平台',
    logo: '/logo.png',
    icp: '京ICP备123456789号'
  },
  upload: {
    maxFileSize: 5242880 / 1024, // 5MB in KB
    allowedTypes: ['jpg', 'jpeg', 'png', 'gif'],
    maxImages: 9
  },
  trade: {
    autoEstimate: true,
    estimateConfidence: 80,
    maxPrice: 1000000,
    autoOfflineHours: 0
  },
  contact: {
    email: 'admin@secondhand.com',
    phone: '400-123-4567',
    address: '北京市朝阳区国贸CBD',
    workTime: ['09:00', '18:00']
  },
  security: {
    passwordStrength: 'medium',
    loginAttempts: 5,
    lockDuration: 30,
    sessionTimeout: 120
  }
}

// 系统设置
const settings = reactive(JSON.parse(JSON.stringify(defaultSettings)))

// 加载系统设置
const loadSettings = async () => {
  try {
    loading.value = true
    const response = await adminApi.getAdminSettings()
    Object.assign(settings, response)
  } catch (error) {
    console.error('加载系统设置失败:', error)
    ElMessage.error('加载系统设置失败')
  } finally {
    loading.value = false
  }
}

// 处理Logo上传
const handleLogoChange = (file) => {
  const isImage = file.raw.type.startsWith('image/')
  const isLt5M = file.raw.size / 1024 / 1024 < 5

  if (!isImage) {
    ElMessage.error('只能上传图片文件!')
    return false
  }
  
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过 5MB!')
    return false
  }

  // 生成预览URL
  const reader = new FileReader()
  reader.onload = (e) => {
    settings.site.logo = e.target.result
  }
  reader.readAsDataURL(file.raw)
}

// 删除Logo
const removeLogo = () => {
  settings.site.logo = '/logo.png'
}

// 重置设置
const resetSettings = () => {
  ElMessageBox.confirm('确定要重置所有设置为默认值吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    Object.assign(settings, JSON.parse(JSON.stringify(defaultSettings)))
    ElMessage.success('设置已重置为默认值')
  }).catch(() => {})
}

// 保存设置
const saveSettings = async () => {
  try {
    loading.value = true
    
    // 验证设置
    if (!validateEmail(settings.contact.email)) {
      ElMessage.error('请输入正确的邮箱地址')
      return
    }
    
    if (!validatePhone(settings.contact.phone)) {
      ElMessage.error('请输入正确的手机号码')
      return
    }
    
    // 保存到后端
    await adminApi.updateAdminSettings(settings)
    
    showSuccess.value = true
    ElMessage.success('系统设置保存成功')
  } catch (error) {
    console.error('保存系统设置失败:', error)
    ElMessage.error('保存系统设置失败')
  } finally {
    loading.value = false
  }
}

// 初始化
onMounted(() => {
  loadSettings()
})
</script>

<style scoped>
.admin-settings {
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

.settings-container {
  margin-bottom: 24px;
}

.settings-card {
  height: 600px;
  overflow-y: auto;
}

.settings-section {
  padding: 20px;
}

.settings-section h3 {
  margin: 0 0 20px 0;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  border-bottom: 2px solid #409eff;
  padding-bottom: 8px;
}

.form-tip {
  color: #909399;
  font-size: 12px;
  margin-left: 8px;
}

.logo-preview {
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-preview img {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  object-fit: cover;
  border: 1px solid #e4e7ed;
}

.upload-demo {
  width: 100%;
}

/* 自定义滚动条 */
.settings-card::-webkit-scrollbar {
  width: 8px;
}

.settings-card::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 4px;
}

.settings-card::-webkit-scrollbar-thumb {
  background: #909399;
  border-radius: 4px;
}

.settings-card::-webkit-scrollbar-thumb:hover {
  background: #606266;
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
  
  .settings-card {
    height: 500px;
  }
}
</style>