<template>
  <div class="product-create-container">
    <div class="header">
      <h1>发布商品</h1>
      <el-button @click="goBack">返回列表</el-button>
    </div>

    <div class="create-form">
      <el-steps :active="currentStep" finish-status="success">
        <el-step title="基本信息" />
        <el-step title="商品图片" />
        <el-step title="商品描述" />
        <el-step title="发布" />
      </el-steps>

      <div class="form-content">
        <!-- 第一步：基本信息 -->
        <div v-if="currentStep === 0" class="step-content">
          <el-form :model="form" :rules="rules" ref="formRef" label-width="120px">
            <el-form-item label="商品标题" prop="title">
              <el-input v-model="form.title" placeholder="请输入商品标题" maxlength="100" show-word-limit />
            </el-form-item>

            <el-form-item label="商品分类" prop="category">
              <el-select v-model="form.category" placeholder="请选择商品分类" style="width: 100%">
                <el-option label="数码产品" value="digital" />
                <el-option label="手机通讯" value="phone" />
                <el-option label="电脑办公" value="computer" />
                <el-option label="家用电器" value="home" />
                <el-option label="图书教材" value="book" />
                <el-option label="生活用品" value="daily" />
                <el-option label="学习用品" value="study" />
                <el-option label="其他" value="other" />
              </el-select>
            </el-form-item>

            <el-form-item label="商品价格" prop="price">
              <el-input-number 
                v-model="form.price" 
                :min="0" 
                :max="999999" 
                :precision="2" 
                style="width: 200px"
              />
              <span class="currency">元</span>
            </el-form-item>

            <el-form-item label="新旧程度" prop="condition">
              <el-select v-model="form.condition" placeholder="请选择新旧程度" style="width: 200px">
                <el-option label="全新" value="new" />
                <el-option label="95新" value="95_new" />
                <el-option label="9成新" value="9_new" />
                <el-option label="8成新" value="8_new" />
                <el-option label="7成新及以下" value="7_new" />
              </el-select>
            </el-form-item>

            <el-form-item label="交易方式" prop="tradeMethod">
              <el-select v-model="form.tradeMethod" placeholder="请选择交易方式" style="width: 200px">
                <el-option label="当面交易" value="face_to_face" />
                <el-option label="快递邮寄" value="express" />
                <el-option label="两者都支持" value="both" />
              </el-select>
            </el-form-item>

            <el-form-item label="联系人" prop="contactName">
              <el-input v-model="form.contactName" placeholder="请输入联系人姓名" />
            </el-form-item>

            <el-form-item label="联系电话" prop="contactPhone">
              <el-input v-model="form.contactPhone" placeholder="请输入联系电话" />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" @click="nextStep">下一步</el-button>
            </el-form-item>
          </el-form>
        </div>

        <!-- 第二步：商品图片 -->
        <div v-if="currentStep === 1" class="step-content">
          <el-form label-width="120px">
            <el-form-item label="商品图片">
              <el-upload
                ref="uploadRef"
                class="image-uploader"
                action="/api/upload"
                list-type="picture-card"
                :auto-upload="false"
                :on-change="handleImageChange"
                :on-remove="handleImageRemove"
                :on-success="handleUploadSuccess"
                :on-error="handleUploadError"
                :file-list="imageList"
              >
                <el-icon><Plus /></el-icon>
              </el-upload>
              
              <div class="image-tips">
                <p>• 最多可上传9张图片</p>
                <p>• 建议上传清晰实物图</p>
                <p>• 单张图片大小不超过5MB</p>
                <p>• 支持JPG、PNG格式</p>
              </div>
            </el-form-item>

            <el-form-item>
              <el-button @click="prevStep">上一步</el-button>
              <el-button type="primary" @click="nextStep">下一步</el-button>
            </el-form-item>
          </el-form>
        </div>

        <!-- 第三步：商品描述 -->
        <div v-if="currentStep === 2" class="step-content">
          <el-form label-width="120px">
            <el-form-item label="商品描述" prop="description">
              <el-input
                v-model="form.description"
                type="textarea"
                :rows="6"
                placeholder="请详细描述商品的状况、购买时间、使用情况等信息"
                maxlength="1000"
                show-word-limit
              />
            </el-form-item>

            <el-form-item label="品牌型号">
              <el-input v-model="form.brand" placeholder="品牌" style="width: 200px; margin-right: 20px" />
              <el-input v-model="form.model" placeholder="型号" style="width: 200px" />
            </el-form-item>

            <el-form-item label="购买时间">
              <el-date-picker
                v-model="form.purchaseTime"
                type="date"
                placeholder="请选择购买时间"
                style="width: 200px"
              />
            </el-form-item>

            <el-form-item label="商品特色">
              <el-checkbox-group v-model="form.features">
                <el-checkbox label="支持验机">支持验机</el-checkbox>
                <el-checkbox label="发票齐全">发票齐全</el-checkbox>
                <el-checkbox label="包装齐全">包装齐全</el-checkbox>
                <el-checkbox label="官方保修">官方保修</el-checkbox>
                <el-checkbox label="无拆修记录">无拆修记录</el-checkbox>
              </el-checkbox-group>
            </el-form-item>

            <el-form-item label="备注说明">
              <el-input
                v-model="form.remarks"
                type="textarea"
                :rows="4"
                placeholder="其他需要说明的事项"
                maxlength="500"
                show-word-limit
              />
            </el-form-item>

            <el-form-item>
              <el-button @click="prevStep">上一步</el-button>
              <el-button type="primary" @click="nextStep">下一步</el-button>
            </el-form-item>
          </el-form>
        </div>

        <!-- 第四步：确认发布 -->
        <div v-if="currentStep === 3" class="step-content">
          <div class="confirm-info">
            <h3>商品信息确认</h3>
            <div class="info-card">
              <div class="info-item">
                <span class="label">商品标题：</span>
                <span class="value">{{ form.title }}</span>
              </div>
              <div class="info-item">
                <span class="label">商品分类：</span>
                <span class="value">{{ getCategoryText(form.category) }}</span>
              </div>
              <div class="info-item">
                <span class="label">商品价格：</span>
                <span class="value price">¥{{ form.price }}</span>
              </div>
              <div class="info-item">
                <span class="label">新旧程度：</span>
                <span class="value">{{ getConditionText(form.condition) }}</span>
              </div>
              <div class="info-item">
                <span class="label">交易方式：</span>
                <span class="value">{{ getTradeMethodText(form.tradeMethod) }}</span>
              </div>
              <div class="info-item">
                <span class="label">联系人：</span>
                <span class="value">{{ form.contactName }}</span>
              </div>
              <div class="info-item">
                <span class="label">联系电话：</span>
                <span class="value">{{ form.contactPhone }}</span>
              </div>
              <div class="info-item">
                <span class="label">商品描述：</span>
                <span class="value">{{ form.description }}</span>
              </div>
              <div class="info-item">
                <span class="label">图片数量：</span>
                <span class="value">{{ imageList.length }}张</span>
              </div>
            </div>
          </div>

          <div class="form-actions">
            <el-button @click="prevStep">上一步</el-button>
            <el-button type="primary" @click="submitProduct">确认发布</el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { useAuthStore } from '@/store/auth'
import productApi from '@/api/product'

export default {
  name: 'ProductCreate',
  components: {
    Plus
  },
  setup() {
    const router = useRouter()
    const authStore = useAuthStore()
    const formRef = ref(null)
    const uploadRef = ref(null)
    
    const currentStep = ref(0)
    const imageList = ref([])
    const uploading = ref(false)
    
    const form = reactive({
      title: '',
      category: '',
      price: 0,
      condition: '',
      tradeMethod: '',
      contactName: '',
      contactPhone: '',
      description: '',
      brand: '',
      model: '',
      purchaseTime: '',
      features: [],
      remarks: ''
    })
    
    const rules = {
      title: [
        { required: true, message: '请输入商品标题', trigger: 'blur' },
        { min: 2, max: 100, message: '标题长度在 2 到 100 个字符', trigger: 'blur' }
      ],
      category: [
        { required: true, message: '请选择商品分类', trigger: 'change' }
      ],
      price: [
        { required: true, message: '请输入商品价格', trigger: 'blur' },
        { type: 'number', min: 0, message: '价格必须大于等于0', trigger: 'blur' }
      ],
      condition: [
        { required: true, message: '请选择新旧程度', trigger: 'change' }
      ],
      tradeMethod: [
        { required: true, message: '请选择交易方式', trigger: 'change' }
      ],
      contactName: [
        { required: true, message: '请输入联系人姓名', trigger: 'blur' },
        { min: 2, max: 20, message: '姓名长度在 2 到 20 个字符', trigger: 'blur' }
      ],
      contactPhone: [
        { required: true, message: '请输入联系电话', trigger: 'blur' },
        { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
      ],
      description: [
        { required: true, message: '请输入商品描述', trigger: 'blur' },
        { min: 10, max: 1000, message: '描述长度在 10 到 1000 个字符', trigger: 'blur' }
      ]
    }
    
    const nextStep = async () => {
      if (currentStep.value === 0) {
        try {
          await formRef.value.validate()
          currentStep.value = 1
        } catch (error) {
          console.error('表单验证失败:', error)
        }
      } else if (currentStep.value === 1) {
        // 验证图片
        if (imageList.value.length === 0) {
          ElMessage.warning('请至少上传一张商品图片')
          return
        }
        currentStep.value = 2
      } else if (currentStep.value === 2) {
        currentStep.value = 3
      }
    }
    
    const prevStep = () => {
      if (currentStep.value > 0) {
        currentStep.value--
      }
    }
    
    const handleImageChange = (file) => {
      // 限制图片数量
      if (imageList.value.length >= 9) {
        ElMessage.warning('最多只能上传9张图片')
        return false
      }
      
      // 检查图片大小
      const isLt5M = file.size / 1024 / 1024 < 5
      if (!isLt5M) {
        ElMessage.error('图片大小不能超过5MB')
        return false
      }
      
      return true
    }
    
    const handleImageRemove = (file) => {
      const index = imageList.value.findIndex(item => item.uid === file.uid)
      if (index !== -1) {
        imageList.value.splice(index, 1)
      }
    }
    
    const handleUploadSuccess = (response, file) => {
      const index = imageList.value.findIndex(item => item.uid === file.uid)
      if (index !== -1) {
        imageList.value[index].url = response.url
      }
    }
    
    const handleUploadError = (error, file) => {
      ElMessage.error('图片上传失败')
    }
    
    const getCategoryText = (category) => {
      const categoryMap = {
        'digital': '数码产品',
        'phone': '手机通讯',
        'computer': '电脑办公',
        'home': '家用电器',
        'book': '图书教材',
        'daily': '生活用品',
        'study': '学习用品',
        'other': '其他'
      }
      return categoryMap[category] || category
    }
    
    const getConditionText = (condition) => {
      const conditionMap = {
        'new': '全新',
        '95_new': '95新',
        '9_new': '9成新',
        '8_new': '8成新',
        '7_new': '7成新及以下'
      }
      return conditionMap[condition] || condition
    }
    
    const getTradeMethodText = (method) => {
      const methodMap = {
        'face_to_face': '当面交易',
        'express': '快递邮寄',
        'both': '两者都支持'
      }
      return methodMap[method] || method
    }
    
    const submitProduct = async () => {
      try {
        await ElMessageBox.confirm('确认发布该商品吗？发布后将无法修改', '确认发布', {
          type: 'warning'
        })
        
        uploading.value = true
        
        // 构建商品数据
        const productData = {
          title: form.title,
          category: form.category,
          price: form.price,
          condition: form.condition,
          tradeMethod: form.tradeMethod,
          contactName: form.contactName,
          contactPhone: form.contactPhone,
          description: form.description,
          brand: form.brand,
          model: form.model,
          purchaseTime: form.purchaseTime,
          features: form.features,
          remarks: form.remarks,
          images: imageList.value.filter(item => item.url).map(item => item.url)
        }
        
        // TODO: 调用API发布商品
        // const response = await productApi.createProduct(productData)
        
        // 模拟API调用
        await new Promise(resolve => setTimeout(resolve, 2000))
        
        ElMessage.success('商品发布成功')
        
        // 跳转到商品详情页
        // router.push(`/products/${response.data.id}`)
        router.push('/products')
        
      } catch (error) {
        if (error !== 'cancel') {
          console.error('发布商品失败:', error)
          ElMessage.error('发布商品失败')
        }
      } finally {
        uploading.value = false
      }
    }
    
    const goBack = () => {
      router.go(-1)
    }
    
    // 检查用户登录状态
    if (!authStore.isLoggedIn) {
      ElMessage.warning('请先登录')
      router.push('/auth/login')
      return
    }
    
    return {
      currentStep,
      formRef,
      uploadRef,
      form,
      rules,
      imageList,
      uploading,
      nextStep,
      prevStep,
      handleImageChange,
      handleImageRemove,
      handleUploadSuccess,
      handleUploadError,
      getCategoryText,
      getConditionText,
      getTradeMethodText,
      submitProduct,
      goBack
    }
  }
}
</script>

<style scoped>
.product-create-container {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}

.header h1 {
  margin: 0;
  color: #333;
}

.create-form {
  background: white;
  border-radius: 8px;
  padding: 30px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.form-content {
  margin-top: 30px;
}

.step-content {
  min-height: 400px;
}

.currency {
  margin-left: 10px;
  color: #666;
}

.image-uploader {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
}

.image-tips {
  margin-top: 20px;
  padding: 15px;
  background: #f8f9fa;
  border-radius: 4px;
}

.image-tips p {
  margin: 5px 0;
  color: #666;
  font-size: 14px;
}

.confirm-info {
  text-align: center;
}

.confirm-info h3 {
  margin: 0 0 30px 0;
  color: #333;
}

.info-card {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 20px;
  text-align: left;
}

.info-item {
  display: flex;
  padding: 10px 0;
  border-bottom: 1px solid #eee;
}

.info-item:last-child {
  border-bottom: none;
}

.info-item .label {
  color: #666;
  min-width: 120px;
  text-align: right;
  margin-right: 20px;
}

.info-item .value {
  color: #333;
  flex: 1;
}

.info-item .price {
  color: #ff6b6b;
  font-weight: bold;
}

.form-actions {
  margin-top: 30px;
  text-align: center;
}

@media (max-width: 768px) {
  .product-create-container {
    padding: 10px;
  }
  
  .create-form {
    padding: 20px;
  }
  
  .info-item {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .info-item .label {
    min-width: auto;
    margin-right: 0;
    margin-bottom: 5px;
  }
}
</style>