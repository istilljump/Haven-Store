<template>
  <div class="product-publish">
    <div class="publish-container">
      <h2 class="page-title">发布二手商品</h2>
      
      <el-form
        ref="productFormRef"
        :model="productForm"
        :rules="productRules"
        label-width="120px"
        class="product-form"
      >
        <!-- 商品标题 -->
        <el-form-item label="商品标题" prop="title">
          <el-input
            v-model="productForm.title"
            placeholder="请输入商品标题，简洁明了"
            maxlength="100"
            show-word-limit
          />
        </el-form-item>

        <!-- 商品分类 -->
        <el-form-item label="商品分类" prop="categoryId">
          <el-select
            v-model="productForm.categoryId"
            placeholder="请选择商品分类"
            style="width: 100%"
          >
            <el-option label="数码产品" :value="1" />
            <el-option label="服装鞋帽" :value="2" />
            <el-option label="书籍文具" :value="3" />
            <el-option label="家具家电" :value="4" />
            <el-option label="运动器材" :value="5" />
            <el-option label="其他" :value="6" />
          </el-select>
        </el-form-item>

        <!-- 商品描述 -->
        <el-form-item label="商品描述" prop="description">
          <el-input
            v-model="productForm.description"
            type="textarea"
            :rows="4"
            placeholder="请详细描述商品信息，如品牌、型号、使用情况等"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <!-- 商品价格 -->
        <el-form-item label="商品价格" prop="price">
          <el-input-number
            v-model="productForm.price"
            :min="0"
            :precision="2"
            :step="10"
            style="width: 200px"
          />
          <span class="price-unit">元</span>
        </el-form-item>

        <!-- 成色选择 -->
        <el-form-item label="商品成色" prop="productCondition">
          <el-radio-group v-model="productForm.productCondition">
            <el-radio label="全新">全新</el-radio>
            <el-radio label="九成新">九成新</el-radio>
            <el-radio label="八成新">八成新</el-radio>
            <el-radio label="七成新">七成新</el-radio>
            <el-radio label="七成新及以下">七成新及以下</el-radio>
          </el-radio-group>
        </el-form-item>

        <!-- 交易方式 -->
        <el-form-item label="交易方式" prop="tradeType">
          <el-radio-group v-model="productForm.tradeType">
            <el-radio label="online">线上交易</el-radio>
            <el-radio label="offline">线下交易</el-radio>
          </el-radio-group>
        </el-form-item>

        <!-- 线下交易地址（仅在选中线下交易时显示） -->
        <el-form-item 
          v-if="productForm.tradeType === 'offline'"
          label="交易地址" 
          prop="address"
        >
          <el-input
            v-model="productForm.address"
            placeholder="请输入详细交易地址，如：XX省XX市XX区XX街道XX号"
            maxlength="200"
            show-word-limit
          />
          <div style="margin-top: 10px;">
            <el-button type="primary" @click="openMapSelector">
              <el-icon><Location /></el-icon>
              地图选点
            </el-button>
            <span class="map-tip">点击按钮选择在地图上标注位置</span>
          </div>
        </el-form-item>

        <!-- 提交按钮 -->
        <el-form-item>
          <el-button
            type="primary"
            :loading="submitting"
            @click="submitProduct"
            style="width: 150px"
          >
            {{ submitting ? '提交中...' : '发布商品' }}
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { publishProduct } from '@/api/product'

const router = useRouter()

// 表单引用
const productFormRef = ref()

// 提交状态
const submitting = ref(false)

// 表单数据
const productForm = reactive({
  title: '',
  categoryId: null,
  description: '',
  price: 0,
  productCondition: '',
  tradeType: 'online',
  address: '',
  longitude: null,
  latitude: null
})

// 表单校验规则
const productRules = {
  title: [
    { required: true, message: '请输入商品标题', trigger: 'blur' },
    { min: 2, max: 100, message: '标题长度在 2 到 100 个字符', trigger: 'blur' }
  ],
  categoryId: [
    { required: true, message: '请选择商品分类', trigger: 'change' }
  ],
  description: [
    { required: false, message: '请输入商品描述', trigger: 'blur' },
    { max: 1000, message: '商品描述长度不能超过1000个字符', trigger: 'blur' }
  ],
  price: [
    { required: true, message: '请输入商品价格', trigger: 'blur' },
    { type: 'number', min: 0.01, message: '商品价格必须大于0', trigger: 'blur' }
  ],
  productCondition: [
    { required: true, message: '请选择商品成色', trigger: 'change' }
  ],
  tradeType: [
    { required: true, message: '请选择交易方式', trigger: 'change' }
  ],
  // 地址校验规则（仅在交易方式为线下时生效）
  address: [
    {
      required: true,
      validator: (rule, value, callback) => {
        if (productForm.tradeType === 'offline' && !value) {
          callback(new Error('线下交易需要填写详细地址'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

// 打开地图选择器
const openMapSelector = () => {
  ElMessage.info('地图选择功能开发中...')
  // TODO: 后续可以实现地图组件调用
  // 模拟地图选点，实际应用中应该调用地图API
  // 比如高德地图、百度地图等
}

// 提交表单
const submitProduct = async () => {
  try {
    // 表单校验
    await productFormRef.value.validate()
    
    submitting.value = true
    
    // 准备提交数据 - 按照后端DTO格式
    const submitData = {
      title: productForm.title,
      categoryId: productForm.categoryId,
      description: productForm.description || '',
      price: Number(productForm.price),
      productCondition: productForm.productCondition,
      tradeType: productForm.tradeType,
      // 线下交易相关信息
      ...(productForm.tradeType === 'offline' && {
        address: productForm.address,
        longitude: productForm.longitude,
        latitude: productForm.latitude
      })
    }
    
    console.log('提交商品信息：', submitData)
    
    // 调用API发布商品
    const result = await publishProduct(submitData)
    
    submitting.value = false
    ElMessage.success('商品发布成功！')
    
    // 跳转到首页或其他页面
    router.push('/')
    
  } catch (error) {
    console.error('商品发布失败：', error)
    submitting.value = false
    // API调用中的错误会由拦截器处理，这里主要是处理表单校验错误
  }
}
</script>

<style scoped>
.product-publish {
  padding: 20px;
  min-height: calc(100vh - 60px);
}

.publish-container {
  max-width: 800px;
  margin: 0 auto;
  background: white;
  border-radius: 12px;
  padding: 30px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.page-title {
  text-align: center;
  color: #333;
  margin-bottom: 30px;
  font-size: 24px;
  font-weight: 600;
}

.product-form {
  max-width: 600px;
  margin: 0 auto;
}

.price-unit {
  margin-left: 8px;
  color: #666;
}

.map-tip {
  margin-left: 10px;
  color: #666;
  font-size: 14px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .publish-container {
    padding: 20px;
    margin: 10px;
  }
  
  .page-title {
    font-size: 20px;
    margin-bottom: 20px;
  }
  
  .map-tip {
    display: block;
    margin-top: 5px;
  }
}
</style>