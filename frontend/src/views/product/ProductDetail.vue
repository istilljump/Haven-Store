<template>
  <div class="product-detail-container">
    <div v-if="loading" class="loading">
      <el-skeleton :rows="5" animated />
    </div>
    
    <div v-else-if="productNotFound" class="not-found">
      <el-empty description="商品不存在">
        <el-button type="primary" @click="goBack">返回列表</el-button>
      </el-empty>
    </div>
    
    <div v-else class="product-detail">
      <div class="breadcrumb">
        <el-breadcrumb>
          <el-breadcrumb-item @click="goToHome">首页</el-breadcrumb-item>
          <el-breadcrumb-item @click="goToProducts">商品列表</el-breadcrumb-item>
          <el-breadcrumb-item>商品详情</el-breadcrumb-item>
        </el-breadcrumb>
      </div>

      <div class="product-content">
        <div class="product-main">
          <!-- 商品图片轮播 -->
          <div class="product-images">
            <el-carousel v-if="product.images && product.images.length > 0" :interval="4000" type="card" height="400px">
              <el-carousel-item v-for="(image, index) in product.images" :key="index">
                <img :src="image" :alt="product.title" class="carousel-image" />
              </el-carousel-item>
            </el-carousel>
            <div v-else class="no-image">
              <el-empty description="暂无图片" />
            </div>
          </div>

          <!-- 商品信息 -->
          <div class="product-info-section">
            <div class="price-section">
              <span class="price">¥{{ product.price }}</span>
              <span class="original-price" v-if="product.originalPrice">
                原价: ¥{{ product.originalPrice }}
              </span>
            </div>

            <div class="title-section">
              <h1>{{ product.title }}</h1>
              <p class="description">{{ product.description }}</p>
            </div>

            <div class="meta-section">
              <div class="meta-item">
                <span class="label">分类:</span>
                <span class="value">{{ product.category }}</span>
              </div>
              <div class="meta-item">
                <span class="label">状态:</span>
                <el-tag :type="getStatusType(product.status)">
                  {{ getStatusText(product.status) }}
                </el-tag>
              </div>
              <div class="meta-item">
                <span class="label">发布时间:</span>
                <span class="value">{{ product.publishTime }}</span>
              </div>
              <div class="meta-item">
                <span class="label">浏览次数:</span>
                <span class="value">{{ product.viewCount }}</span>
              </div>
            </div>

            <div class="seller-section">
              <div class="seller-info">
                <el-avatar :src="product.seller.avatar" size="small" />
                <div class="seller-details">
                  <div class="seller-name">{{ product.seller.username }}</div>
                  <div class="seller-meta">
                    <span>信用: {{ product.seller.credit }}</span>
                    <span>|</span>
                    <span>发布商品: {{ product.seller.productCount }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 商品详情和操作 -->
        <div class="product-sidebar">
          <!-- 联系卖家 -->
          <div class="contact-seller">
            <h3>联系卖家</h3>
            <div class="seller-actions">
              <el-button type="primary" size="large" @click="contactSeller">
                私信联系
              </el-button>
              <el-button type="success" size="large" @click="callSeller">
                电话联系
              </el-button>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="product-actions">
            <h3>商品操作</h3>
            <div class="action-buttons">
              <el-button 
                type="danger" 
                :disabled="product.status !== 'active'"
                @click="toggleFavorite"
                :icon="isFavorited ? StarFilled : Star"
              >
                {{ isFavorited ? '已收藏' : '收藏' }}
              </el-button>
              <el-button 
                type="primary" 
                :disabled="product.status !== 'active'"
                @click="inquiry"
              >
                我要购买
              </el-button>
            </div>
          </div>

          <!-- 商品统计 -->
          <div class="product-stats">
            <h3>商品统计</h3>
            <div class="stats-list">
              <div class="stat-item">
                <span class="label">浏览次数:</span>
                <span class="value">{{ product.viewCount }}</span>
              </div>
              <div class="stat-item">
                <span class="label">收藏次数:</span>
                <span class="value">{{ product.favoriteCount }}</span>
              </div>
              <div class="stat-item">
                <span class="label">咨询次数:</span>
                <span class="value">{{ product.inquiryCount }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 商品详情内容 -->
      <div class="product-detail-content">
        <el-tabs v-model="activeTab">
          <el-tab-pane label="商品详情" name="detail">
            <div class="detail-content">
              <div v-if="product.detailHtml" v-html="product.detailHtml"></div>
              <div v-else>
                <el-descriptions title="商品描述" border>
                  <el-descriptions-item label="品牌">{{ product.brand }}</el-descriptions-item>
                  <el-descriptions-item label="型号">{{ product.model }}</el-descriptions-item>
                  <el-descriptions-item label="新旧程度">{{ product.condition }}</el-descriptions-item>
                  <el-descriptions-item label="购买时间">{{ product.purchaseTime }}</el-descriptions-item>
                  <el-descriptions-item label="交易方式">{{ product.tradeMethod }}</el-descriptions-item>
                  <el-descriptions-item label="备注">{{ product.remarks }}</el-descriptions-item>
                </el-descriptions>
              </div>
            </div>
          </el-tab-pane>
          
          <el-tab-pane label="购买须知" name="notice">
            <div class="notice-content">
              <el-alert
                title="交易提示"
                type="info"
                description="本平台仅为用户提供信息发布服务，交易请谨慎，建议选择官方推荐的交易方式。"
                show-icon
                :closable="false"
              />
              
              <div class="notice-list">
                <h4>购买前请注意：</h4>
                <ul>
                  <li>仔细阅读商品描述和图片信息</li>
                  <li>与卖家充分沟通商品状况</li>
                  <li>建议选择安全的交易方式</li>
                  <li>保留相关交易凭证</li>
                </ul>
                
                <h4>退款政策：</h4>
                <p>本平台不支持退款，请在购买前确认商品状况。</p>
              </div>
            </div>
          </el-tab-pane>
          
          <el-tab-pane label="评价记录" name="reviews">
            <div class="reviews-content">
              <div v-if="product.reviews && product.reviews.length > 0">
                <div v-for="review in product.reviews" :key="review.id" class="review-item">
                  <div class="review-header">
                    <el-avatar :src="review.user.avatar" size="small" />
                    <div class="review-user">
                      <div class="username">{{ review.user.username }}</div>
                      <div class="review-time">{{ review.createTime }}</div>
                    </div>
                    <div class="review-rating">
                      <el-rate v-model="review.rating" disabled />
                    </div>
                  </div>
                  <div class="review-content">
                    {{ review.content }}
                  </div>
                </div>
              </div>
              <div v-else class="no-reviews">
                <el-empty description="暂无评价" />
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Star, StarFilled } from '@element-plus/icons-vue'

export default {
  name: 'ProductDetail',
  components: {
    Star,
    StarFilled
  },
  setup() {
    const router = useRouter()
    const route = useRoute()
    
    const loading = ref(true)
    const productNotFound = ref(false)
    const activeTab = ref('detail')
    const isFavorited = ref(false)
    
    const product = reactive({
      id: null,
      title: '',
      description: '',
      price: 0,
      originalPrice: 0,
      category: '',
      status: 'active',
      publishTime: '',
      viewCount: 0,
      favoriteCount: 0,
      inquiryCount: 0,
      images: [],
      seller: {
        id: null,
        username: '',
        avatar: '',
        credit: 0,
        productCount: 0
      },
      detailHtml: '',
      brand: '',
      model: '',
      condition: '',
      purchaseTime: '',
      tradeMethod: '',
      remarks: '',
      reviews: []
    })
    
    const productId = computed(() => route.params.id)
    
    const fetchProductDetail = async () => {
      loading.value = true
      try {
        // TODO: 调用API获取商品详情
        // const response = await api.getProductDetail(productId.value)
        // Object.assign(product, response.data)
        
        // 模拟数据
        await new Promise(resolve => setTimeout(resolve, 1000))
        Object.assign(product, {
          id: productId.value,
          title: '二手iPhone 12 Pro Max',
          description: '95新，官方拆机无维修记录，原装配件齐全，电池健康度90%以上。支持当面验机。',
          price: 6500,
          originalPrice: 8999,
          category: '数码产品',
          status: 'active',
          publishTime: '2024-01-15 14:30:00',
          viewCount: 156,
          favoriteCount: 23,
          inquiryCount: 8,
          images: ['/images/products/iphone-12.png'],
          seller: {
            id: 1,
            username: '数码达人',
            avatar: '',
            credit: 4.8,
            productCount: 15
          },
          brand: 'Apple',
          model: 'iPhone 12 Pro Max',
          condition: '95新',
          purchaseTime: '2023年6月',
          tradeMethod: '当面交易',
          remarks: '无磕碰无划痕，功能完好，支持验机',
          reviews: [
            {
              id: 1,
              user: {
                username: '买家A',
                avatar: ''
              },
              rating: 5,
              content: '手机成色很好，功能正常，卖家很诚信。',
              createTime: '2024-01-14 10:30:00'
            }
          ]
        })
      } catch (error) {
        console.error('获取商品详情失败:', error)
        productNotFound.value = true
      } finally {
        loading.value = false
      }
    }
    
    const getStatusType = (status) => {
      const statusMap = {
        'active': 'success',
        'sold': 'warning',
        'deleted': 'danger'
      }
      return statusMap[status] || 'info'
    }
    
    const getStatusText = (status) => {
      const statusMap = {
        'active': '在售',
        'sold': '已售出',
        'deleted': '已下架'
      }
      return statusMap[status] || status
    }
    
    const toggleFavorite = async () => {
      try {
        // TODO: 调用API收藏/取消收藏
        if (isFavorited.value) {
          ElMessage.success('已取消收藏')
        } else {
          ElMessage.success('已收藏')
        }
        isFavorited.value = !isFavorited.value
      } catch (error) {
        console.error('操作失败:', error)
      }
    }
    
    const contactSeller = () => {
      // TODO: 实现私信功能
      ElMessage.info('私信功能开发中')
    }
    
    const callSeller = () => {
      // TODO: 实现电话联系功能
      ElMessage.info('电话联系功能开发中')
    }
    
    const inquiry = () => {
      // TODO: 实现咨询功能
      ElMessage.info('咨询功能开发中')
    }
    
    const goBack = () => {
      router.go(-1)
    }
    
    const goToHome = () => {
      router.push('/')
    }
    
    const goToProducts = () => {
      router.push('/products')
    }
    
    onMounted(() => {
      if (productId.value) {
        fetchProductDetail()
      } else {
        productNotFound.value = true
        loading.value = false
      }
    })
    
    return {
      loading,
      productNotFound,
      product,
      activeTab,
      isFavorited,
      getStatusType,
      getStatusText,
      toggleFavorite,
      contactSeller,
      callSeller,
      inquiry,
      goBack,
      goToHome,
      goToProducts
    }
  }
}
</script>

<style scoped>
.product-detail-container {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

.loading {
  padding: 60px 20px;
}

.not-found {
  padding: 60px 20px;
  text-align: center;
}

.breadcrumb {
  margin-bottom: 20px;
}

.product-detail {
  background: white;
  border-radius: 8px;
  overflow: hidden;
}

.product-content {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 30px;
  margin-bottom: 30px;
}

.product-main {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.product-images {
  border-radius: 8px;
  overflow: hidden;
  background: #f5f5f5;
}

.carousel-image {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.no-image {
  height: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.price-section {
  margin-bottom: 20px;
}

.price {
  font-size: 32px;
  font-weight: bold;
  color: #ff6b6b;
  margin-right: 15px;
}

.original-price {
  color: #999;
  text-decoration: line-through;
}

.title-section {
  margin-bottom: 20px;
}

.title-section h1 {
  margin: 0 0 10px 0;
  color: #333;
  font-size: 24px;
}

.description {
  color: #666;
  line-height: 1.6;
  margin-bottom: 20px;
}

.meta-section {
  margin-bottom: 20px;
}

.meta-item {
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.meta-item .label {
  color: #999;
  min-width: 80px;
}

.meta-item .value {
  color: #333;
}

.seller-section {
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
}

.seller-info {
  display: flex;
  align-items: center;
  gap: 15px;
}

.seller-details .seller-name {
  font-weight: 600;
  color: #333;
  margin-bottom: 5px;
}

.seller-meta {
  color: #666;
  font-size: 14px;
}

.product-sidebar {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.contact-seller,
.product-actions,
.product-stats {
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
}

.contact-seller h3,
.product-actions h3,
.product-stats h3 {
  margin: 0 0 15px 0;
  color: #333;
}

.seller-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.action-buttons {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.stats-list .stat-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
  padding: 8px 0;
  border-bottom: 1px solid #eee;
}

.product-detail-content {
  background: white;
  border-radius: 8px;
  padding: 30px;
}

.detail-content,
.notice-content,
.reviews-content {
  line-height: 1.8;
}

.notice-list {
  margin-top: 20px;
}

.notice-list h4 {
  color: #333;
  margin: 20px 0 10px 0;
}

.notice-list ul {
  margin: 0;
  padding-left: 20px;
}

.review-item {
  padding: 20px;
  border-bottom: 1px solid #eee;
}

.review-header {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 15px;
}

.review-user {
  flex: 1;
}

.review-user .username {
  font-weight: 600;
  color: #333;
  margin-bottom: 5px;
}

.review-user .review-time {
  color: #999;
  font-size: 14px;
}

.review-rating {
  flex-shrink: 0;
}

.review-content {
  color: #666;
  line-height: 1.6;
}

.no-reviews {
  padding: 40px 20px;
  text-align: center;
}

@media (max-width: 768px) {
  .product-content {
    grid-template-columns: 1fr;
  }
  
  .price-section .price {
    font-size: 24px;
  }
  
  .title-section h1 {
    font-size: 20px;
  }
}
</style>