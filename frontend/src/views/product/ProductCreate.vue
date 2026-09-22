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

            <el-form-item label="商品分类" prop="categoryId">
              <el-select
                v-model="form.categoryId"
                placeholder="请选择商品分类"
                style="width: 100%"
                :loading="categoryLoading"
              >
                <el-option
                  v-for="item in categoryOptions"
                  :key="item.id"
                  :label="item.name"
                  :value="item.id"
                />
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
              <!-- 取值与后端 ProductConstant.VALID_CONDITIONS 保持一致，否则会被业务层拒绝 -->
              <el-select v-model="form.condition" placeholder="请选择新旧程度" style="width: 200px">
                <el-option label="全新" value="全新" />
                <el-option label="九成新" value="九成新" />
                <el-option label="八成新" value="八成新" />
                <el-option label="七成新及以下" value="七成新及以下" />
              </el-select>
            </el-form-item>

            <el-form-item label="交易方式" prop="tradeType">
              <!-- 取值与后端 ProductConstant.VALID_TRADE_TYPES 保持一致 -->
              <el-select v-model="form.tradeType" placeholder="请选择交易方式" style="width: 200px">
                <el-option label="线上交易" value="线上" />
                <el-option label="线下交易" value="线下" />
              </el-select>
            </el-form-item>

            <!-- 线下交易必须提供地址与坐标，后端会强校验；线上交易不需要 -->
            <template v-if="form.tradeType === '线下'">
              <el-form-item label="交易地址" prop="address">
                <el-input v-model="form.address" placeholder="请输入线下交易地址" />
              </el-form-item>
              <el-form-item label="地理位置">
                <el-button :loading="locating" @click="locateCurrentPosition">
                  <el-icon><Location /></el-icon>
                  使用当前位置
                </el-button>
                <span class="coordinate-hint">
                  {{ form.longitude && form.latitude
                    ? `已获取：${form.longitude}, ${form.latitude}`
                    : '未获取（线下交易需要经纬度，用于「附近商品」查询）' }}
                </span>
              </el-form-item>
            </template>

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
                <span class="value">{{ getCategoryText(form.categoryId) }}</span>
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
                <span class="value">{{ getTradeMethodText(form.tradeType) }}</span>
              </div>
              <div v-if="form.tradeType === '线下'" class="info-item">
                <span class="label">交易地址：</span>
                <span class="value">{{ form.address || '-' }}</span>
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
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Location } from '@element-plus/icons-vue'
import { useAuthStore } from '@/store/auth'
import productApi from '@/api/product'

export default {
  name: 'ProductCreate',
  components: {
    Plus,
    Location
  },
  setup() {
    const router = useRouter()
    const authStore = useAuthStore()
    const formRef = ref(null)
    const uploadRef = ref(null)
    
    const currentStep = ref(0)
    const imageList = ref([])
    const uploading = ref(false)
    const locating = ref(false)
    const categoryOptions = ref([])
    const categoryLoading = ref(false)
    
    const form = reactive({
      title: '',
      categoryId: null,
      price: 0,
      condition: '',
      tradeType: '',
      address: '',
      longitude: null,
      latitude: null,
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
      categoryId: [
        { required: true, message: '请选择商品分类', trigger: 'change' }
      ],
      price: [
        { required: true, message: '请输入商品价格', trigger: 'blur' },
        { type: 'number', min: 0.01, message: '价格必须大于0', trigger: 'blur' }
      ],
      condition: [
        { required: true, message: '请选择新旧程度', trigger: 'change' }
      ],
      tradeType: [
        { required: true, message: '请选择交易方式', trigger: 'change' }
      ],
      address: [
        // 仅线下交易需要地址，线上时该字段不展示也不校验
        {
          validator: (rule, value, callback) => {
            if (form.tradeType === '线下' && !value) {
              callback(new Error('线下交易需要填写交易地址'))
              return
            }
            callback()
          },
          trigger: 'blur'
        }
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
        // 说明：后端尚未提供图片上传接口（商品表 cover_image 也暂未开放写入），
        // 因此这里只做提示，不阻塞流程，避免用户卡在图片步骤无法发布
        if (imageList.value.length === 0) {
          ElMessage.warning('未选择商品图片，发布后商品将没有封面图')
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
    
    const getCategoryText = (categoryId) => {
      const matched = categoryOptions.value.find(item => item.id === categoryId)
      return matched ? matched.name : '-'
    }
    
    // 取值已与后端对齐（成色直接用中文值），这里保留映射函数以便展示时统一兜底
    const CONDITION_TEXT = {
      '全新': '全新',
      '九成新': '九成新',
      '八成新': '八成新',
      '七成新及以下': '七成新及以下'
    }
    
    const TRADE_TYPE_TEXT = {
      '线上': '线上交易',
      '线下': '线下交易'
    }
    
    const getConditionText = (condition) => CONDITION_TEXT[condition] || condition || '-'
    
    const getTradeMethodText = (method) => TRADE_TYPE_TEXT[method] || method || '-'
    
    // 加载可用的商品分类（后端只返回启用状态的分类）
    const loadCategories = async () => {
      try {
        categoryLoading.value = true
        categoryOptions.value = await productApi.getProductCategories()
      } catch (error) {
        console.error('加载商品分类失败:', error)
      } finally {
        categoryLoading.value = false
      }
    }
    
    // 获取当前定位（线下交易需要经纬度，用于「附近商品」检索）
    const locateCurrentPosition = () => {
      if (!navigator.geolocation) {
        ElMessage.error('当前浏览器不支持定位，请手动填写地址')
        return
      }
      locating.value = true
      navigator.geolocation.getCurrentPosition(
        (position) => {
          // 后端精度要求 6 位小数（DECIMAL(10,6)）
          form.longitude = Number(position.coords.longitude.toFixed(6))
          form.latitude = Number(position.coords.latitude.toFixed(6))
          ElMessage.success('已获取当前位置')
          locating.value = false
        },
        (error) => {
          console.error('定位失败:', error)
          ElMessage.error('定位失败，请检查浏览器定位权限')
          locating.value = false
        },
        { enableHighAccuracy: true, timeout: 10000 }
      )
    }
    
    const submitProduct = async () => {
      // 线下交易必须拿到坐标，否则后端会拒绝（这里提前拦截，给出更明确的提示）
      if (form.tradeType === '线下' && (!form.longitude || !form.latitude)) {
        ElMessage.warning('线下交易请先获取地理位置')
        return
      }

      try {
        await ElMessageBox.confirm('确认发布该商品吗？发布后将无法修改', '确认发布', {
          type: 'warning'
        })

        uploading.value = true

        // 组装后端入参（字段名与 ProductAddDTO 一一对应）
        // 说明：联系人/品牌/型号/购入时间等前端收集的信息，后端暂无对应字段，
        // 统一并入商品描述，避免用户填写的内容被静默丢弃
        const extraInfo = [
          form.contactName ? `联系人：${form.contactName}` : '',
          form.contactPhone ? `联系电话：${form.contactPhone}` : '',
          form.brand ? `品牌：${form.brand}` : '',
          form.model ? `型号：${form.model}` : '',
          form.purchaseTime ? `购入时间：${form.purchaseTime}` : '',
          form.remarks ? `备注：${form.remarks}` : ''
        ].filter(Boolean)

        const description = [form.description, ...extraInfo].filter(Boolean).join('\n')

        const productData = {
          title: form.title,
          categoryId: form.categoryId,
          price: form.price,
          productCondition: form.condition,
          tradeType: form.tradeType,
          address: form.tradeType === '线下' ? form.address : null,
          longitude: form.tradeType === '线下' ? form.longitude : null,
          latitude: form.tradeType === '线下' ? form.latitude : null,
          description
        }

        const result = await productApi.addProduct(productData)

        ElMessage.success('商品发布成功')

        // 后端返回 {productId, estimatedPrice}，有 ID 时直接跳到详情页
        if (result && result.productId) {
          router.push(`/products/${result.productId}`)
        } else {
          router.push('/products')
        }

      } catch (error) {
        if (error !== 'cancel') {
          console.error('发布商品失败:', error)
        }
      } finally {
        uploading.value = false
      }
    }
    
    const goBack = () => {
      router.go(-1)
    }
    
    // 检查用户登录状态（未登录直接引导到登录页，不再渲染表单）
    if (!authStore.isLoggedIn) {
      ElMessage.warning('请先登录')
      router.push('/auth/login')
      return
    }

    // 分类下拉的数据来自后端「启用状态的分类」，不再硬编码
    onMounted(loadCategories)
    
    return {
      currentStep,
      formRef,
      uploadRef,
      form,
      rules,
      imageList,
      uploading,
      locating,
      categoryOptions,
      categoryLoading,
      nextStep,
      prevStep,
      handleImageChange,
      handleImageRemove,
      handleUploadSuccess,
      handleUploadError,
      getCategoryText,
      getConditionText,
      getTradeMethodText,
      locateCurrentPosition,
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