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
                <span class="value">{{ product.publishTime || '暂无' }}</span>
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
                    <span>发布在售: {{ product.seller.productCount }} 件</span>
                    <span v-if="product.contactPhone">|</span>
                    <span v-if="product.contactPhone">联系电话: {{ product.contactPhone }}</span>
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
                type="warning"
                :disabled="product.status !== 1 || isOwner"
                :loading="addingToCart"
                @click="addToCart"
              >
                加入购物车
              </el-button>
              <el-button 
                type="danger" 
                :disabled="product.status !== 1"
                @click="toggleFavorite"
                :icon="isFavorited ? StarFilled : Star"
              >
                {{ isFavorited ? '已收藏' : '收藏' }}
              </el-button>
              <el-button 
                type="primary" 
                :disabled="product.status !== 1"
                @click="inquiry"
              >
                我要购买
              </el-button>
              <el-button v-if="isOwner" @click="goToEdit">
                编辑商品
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
                <span class="label">评分:</span>
                <span class="value">{{ product.ratingAvg === null || product.ratingAvg === undefined ? '暂无评分' : product.ratingAvg + ' 分' }}</span>
              </div>
              <div class="stat-item">
                <span class="label">评价数:</span>
                <span class="value">{{ product.commentCount || 0 }}</span>
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
                  <el-descriptions-item label="分类">{{ orDash(product.category) }}</el-descriptions-item>
                  <el-descriptions-item label="新旧程度">{{ orDash(product.condition) }}</el-descriptions-item>
                  <el-descriptions-item label="品牌">{{ orDash(product.brand) }}</el-descriptions-item>
                  <el-descriptions-item label="型号">{{ orDash(product.model) }}</el-descriptions-item>
                  <el-descriptions-item label="购买时间">{{ orDash(product.purchaseTime) }}</el-descriptions-item>
                  <el-descriptions-item label="商品特色">{{ orDash(product.features) }}</el-descriptions-item>
                  <el-descriptions-item label="交易方式">{{ orDash(product.tradeMethod) }}</el-descriptions-item>
                  <el-descriptions-item label="备注说明">{{ orDash(product.remarks) }}</el-descriptions-item>
                  <el-descriptions-item label="发布时间">{{ product.publishTime || '暂无' }}</el-descriptions-item>
                  <el-descriptions-item label="卖家">{{ orDash(product.seller.username) }}</el-descriptions-item>
                  <el-descriptions-item label="商品描述" :span="2">{{ orDash(product.description) }}</el-descriptions-item>
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
          
          <el-tab-pane :label="`评价记录 (${commentTotal})`" name="reviews">
            <div class="reviews-content">
              <!-- 发表评价：仅登录用户、非商品发布者、且尚未评价过时可写 -->
              <div v-if="canComment" class="comment-form">
                <div class="comment-form-title">发表评价</div>
                <div class="comment-rate">
                  <span class="label">评分：</span>
                  <el-rate v-model="commentForm.rating" />
                </div>
                <el-input
                  v-model="commentForm.content"
                  type="textarea"
                  :rows="3"
                  maxlength="500"
                  show-word-limit
                  placeholder="说说这件商品的实际情况，帮助其他买家参考"
                />
                <el-button
                  type="primary"
                  class="comment-submit"
                  :loading="commentSubmitting"
                  @click="submitComment"
                >
                  发表评价
                </el-button>
              </div>
              <el-alert
                v-else-if="isOwner"
                title="这是您自己发布的商品，不能评价"
                type="info"
                :closable="false"
                show-icon
                class="comment-tip"
              />
              <el-alert
                v-else-if="product.commented"
                title="您已评价过该商品"
                type="success"
                :closable="false"
                show-icon
                class="comment-tip"
              />

              <div v-if="comments.length > 0">
                <div v-for="review in comments" :key="review.id" class="review-item">
                  <div class="review-header">
                    <el-avatar :src="review.avatar || '/default-avatar.png'" size="small" />
                    <div class="review-user">
                      <div class="username">{{ review.username }}</div>
                      <div class="review-time">{{ review.createTime }}</div>
                    </div>
                    <div class="review-rating">
                      <el-rate :model-value="review.rating" disabled />
                    </div>
                  </div>
                  <div class="review-content">
                    {{ review.content }}
                  </div>
                </div>
                <div class="comment-pagination">
                  <el-pagination
                    v-model:current-page="commentPage"
                    :page-size="commentPageSize"
                    :total="commentTotal"
                    layout="prev, pager, next"
                    @current-change="loadComments"
                  />
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

    <!-- 联系卖家：private 为普通私信，consult 为针对该商品的咨询 -->
    <el-dialog
      v-model="messageDialogVisible"
      :title="messageDialogConfig.title"
      width="480px"
    >
      <p class="message-dialog-tip">{{ messageDialogConfig.tip }}</p>
      <el-input
        v-model="messageDraft"
        type="textarea"
        :rows="4"
        maxlength="500"
        show-word-limit
        :placeholder="messageDialogConfig.placeholder"
      />
      <template #footer>
        <el-button @click="messageDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="messageSending" @click="submitMessage">
          发送
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Star, StarFilled } from '@element-plus/icons-vue'
import productApi from '@/api/product'
import messageApi from '@/api/message'
import cartApi from '@/api/cart'
import { useAuthStore } from '@/store/auth'
import { useUserStore } from '@/store/index'

export default {
  name: 'ProductDetail',
  components: {
    Star,
    StarFilled
  },
  setup() {
    const router = useRouter()
    const route = useRoute()
    const authStore = useAuthStore()
    // 加入购物车后要同步头部角标，因此需要 user store
    const userStore = useUserStore()

    // 当前登录用户是否为该商品的发布者（决定是否展示「编辑」入口）
    const isOwner = computed(() => {
      const myId = authStore.user && authStore.user.userId
      const sellerId = product.seller && product.seller.id
      return myId != null && sellerId != null && Number(myId) === Number(sellerId)
    })
    
    const loading = ref(true)
    const productNotFound = ref(false)
    // 评价列表（独立分页，与商品详情分开请求）
    const comments = ref([])
    const commentTotal = ref(0)
    const commentPage = ref(1)
    const commentPageSize = ref(10)
    const commentSubmitting = ref(false)
    const commentForm = reactive({ content: '', rating: 5 })
    const activeTab = ref('detail')
    const isFavorited = ref(false)
    
    const product = reactive({
      id: null,
      title: '',
      description: '',
      price: 0,
      originalPrice: 0,
      category: '',
      status: null,
      publishTime: '',
      viewCount: 0,
      favoriteCount: 0,
      commentCount: 0,
      ratingAvg: null,
      commented: false,
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
      features: '',
      contactName: '',
      contactPhone: '',
      tradeMethod: '',
      remarks: ''
    })
    
    const productId = computed(() => route.params.id)
    
    // 详情数据来自后端 GET /product/detail/{id}（游客可访问）
    const fetchProductDetail = async () => {
      loading.value = true
      try {
        const data = await productApi.getProductDetail(productId.value)
        if (!data) {
          productNotFound.value = true
          return
        }
        productNotFound.value = false
        Object.assign(product, {
          id: data.id,
          title: data.title,
          description: data.description,
          price: data.price,
          originalPrice: data.originalPrice,
          category: data.categoryName || '未分类',
          status: data.status,
          publishTime: data.createTime,
          viewCount: data.viewCount || 0,
          // 收藏次数由 user_product_relation 实时统计
          favoriteCount: data.favoriteCount || 0,
          // 评价统计：平均分无评价时为 null
          commentCount: data.commentCount || 0,
          ratingAvg: data.ratingAvg === undefined ? null : data.ratingAvg,
          commented: data.commented === true,
          images: data.images || [],
          seller: {
            id: data.sellerId,
            username: data.sellerUsername || '未知用户',
            avatar: data.sellerAvatar || '',
            productCount: data.sellerProductCount || 0
          },
          brand: data.brand || '',
          model: data.model || '',
          condition: data.productCondition || '',
          purchaseTime: data.purchaseTime || '',
          features: data.features || '',
          remarks: data.remark || '',
          contactName: data.contactName || '',
          // 联系电话仅登录后由后端返回，未登录时为 null
          contactPhone: data.contactPhone || '',
          // 线下交易展示交易地址，线上交易无地址可展示
          tradeMethod: data.tradeType === '线下' && data.address
            ? `${data.tradeType}（${data.address}）` : (data.tradeType || '')
        })
        // 收藏状态由后端返回，未登录固定为 false
        isFavorited.value = data.favorited === true
      } catch (error) {
        console.error('获取商品详情失败:', error)
        productNotFound.value = true
      } finally {
        loading.value = false
      }
    }
    
    // 同一路由下切换商品 id（如从商品 1 跳到商品 2）不会重新挂载组件，
    // 需要监听参数变化重新拉取，否则会一直显示上一个商品
    watch(productId, (id) => {
      if (id) {
        // 切换商品时评价分页要回到第一页，否则会停留在上一个商品的页码
        commentPage.value = 1
        comments.value = []
        fetchProductDetail()
        loadComments()
      }
    })
    
    // 展示占位：后端没有该字段时显示「暂无」，避免出现空白或 undefined
    const orDash = (value) => (value === null || value === undefined || value === '') ? '暂无' : value
    
    // 状态取值与后端 ProductStatusEnum 一致：1 在售 / 2 已售出 / 3 已下架
    // 是否可发表评价：登录 + 非发布者 + 尚未评价过
    const canComment = computed(() => {
      return authStore.isLoggedIn && !isOwner.value && !product.commented
    })

    // 加载评价列表（游客也能看）
    const loadComments = async () => {
      if (!productId.value) return
      try {
        const data = await productApi.getProductComments(productId.value, {
          page: commentPage.value,
          pageSize: commentPageSize.value
        })
        comments.value = data.records || []
        commentTotal.value = data.total || 0
      } catch (error) {
        console.error('加载评价失败:', error)
        comments.value = []
        commentTotal.value = 0
      }
    }

    // 发表评价后同时刷新评价列表与商品统计（评分/评价数会变）
    const submitComment = async () => {
      if (!commentForm.content || commentForm.content.trim().length < 2) {
        ElMessage.warning('评价内容至少 2 个字符')
        return
      }
      try {
        commentSubmitting.value = true
        await productApi.addProductComment(productId.value, {
          content: commentForm.content.trim(),
          rating: commentForm.rating
        })
        ElMessage.success('评价已发表')
        commentForm.content = ''
        commentForm.rating = 5
        commentPage.value = 1
        await Promise.all([loadComments(), fetchProductDetail()])
      } catch (error) {
        console.error('发表评价失败:', error)
      } finally {
        commentSubmitting.value = false
      }
    }

    const getStatusType = (status) => {
      const statusMap = { 1: 'success', 2: 'warning', 3: 'info' }
      return statusMap[status] || 'info'
    }
    
    const getStatusText = (status) => {
      const statusMap = { 1: '在售', 2: '已售出', 3: '已下架' }
      return statusMap[status] || '未知'
    }
    
    const toggleFavorite = async () => {
      if (!authStore.isLoggedIn) {
        ElMessage.warning('请先登录后再收藏')
        router.push(`/auth/login?redirect=${encodeURIComponent(route.fullPath)}`)
        return
      }
      try {
        if (isFavorited.value) {
          await productApi.unfavoriteProduct(product.id)
          ElMessage.success('已取消收藏')
        } else {
          await productApi.favoriteProduct(product.id)
          ElMessage.success('已收藏')
        }
        isFavorited.value = !isFavorited.value
        // 收藏次数同步刷新，避免只改状态不更新计数
        product.favoriteCount = Math.max(0, (product.favoriteCount || 0) + (isFavorited.value ? 1 : -1))
      } catch (error) {
        console.error('收藏操作失败:', error)
      }
    }
    
    /** 私信/咨询对话框文案：两种入口共用同一套发送逻辑，只有文案与是否带商品上下文不同 */
    const MESSAGE_DIALOG_TEXT = {
      private: {
        title: '私信卖家',
        tip: '消息会直接发给卖家，之后可在「我的私信」里继续沟通。',
        placeholder: '想和卖家说点什么…'
      },
      consult: {
        title: '咨询该商品',
        tip: '这是针对该商品的咨询，卖家会在「我的私信」里看到商品信息。',
        placeholder: '想咨询这个商品的什么？'
      }
    }

    /** 对话框模式：private 普通私信 / consult 商品咨询 */
    const messageDialogMode = ref('private')
    const messageDialogVisible = ref(false)
    const messageDraft = ref('')
    const messageSending = ref(false)
    const messageDialogConfig = computed(() => MESSAGE_DIALOG_TEXT[messageDialogMode.value])

    const openMessageDialog = (mode) => {
      if (!authStore.isLoggedIn) {
        ElMessage.warning('请先登录后再联系卖家')
        router.push(`/auth/login?redirect=${encodeURIComponent(route.fullPath)}`)
        return
      }
      // 商品详情对自家商品也展示按钮，这里拦住自己给自己发消息
      if (isOwner.value) {
        ElMessage.warning('这是你自己发布的商品，无需联系自己')
        return
      }
      if (!product.seller || !product.seller.id) {
        ElMessage.warning('暂时拿不到卖家信息，请稍后重试')
        return
      }
      messageDialogMode.value = mode
      messageDraft.value = mode === 'consult'
        ? `你好，我想咨询「${product.title}」这个商品，请问还在吗？`
        : ''
      messageDialogVisible.value = true
    }

    const contactSeller = () => openMessageDialog('private')

    const inquiry = () => openMessageDialog('consult')

    /** 加入购物车：后端幂等，重复加入时提示"已在购物车中"而不是报错 */
    const addingToCart = ref(false)
    const addToCart = async () => {
      if (!authStore.isLoggedIn) {
        ElMessage.warning('请先登录后再加入购物车')
        router.push(`/auth/login?redirect=${encodeURIComponent(route.fullPath)}`)
        return
      }
      if (isOwner.value) {
        ElMessage.warning('这是你自己发布的商品，无需加入购物车')
        return
      }
      addingToCart.value = true
      try {
        const added = await cartApi.addToCart(product.id)
        ElMessage.success(added ? '已加入购物车' : '该商品已在购物车中')
        // 同步头部购物车角标
        userStore.setCartItemCount(await cartApi.getCartCount())
      } catch (error) {
        console.error('加入购物车失败:', error)
      } finally {
        addingToCart.value = false
      }
    }

    /** 发送私信/咨询，成功后跳到私信中心对应的会话 */
    const submitMessage = async () => {
      const content = messageDraft.value.trim()
      if (!content) {
        ElMessage.warning('请输入要发送的内容')
        return
      }
      messageSending.value = true
      try {
        const payload = { toUserId: product.seller.id, content }
        // 咨询带上商品 ID，这样会话列表里能区分出咨询的是哪件商品
        if (messageDialogMode.value === 'consult') {
          payload.productId = product.id
        }
        await messageApi.sendPrivateMessage(payload)
        messageDialogVisible.value = false
        ElMessage.success(messageDialogMode.value === 'consult' ? '咨询已发送' : '私信已发送')

        const query = { peerId: product.seller.id }
        if (payload.productId) {
          query.productId = payload.productId
        }
        router.push({ path: '/messages', query })
      } catch (error) {
        console.error('发送私信失败:', error)
      } finally {
        messageSending.value = false
      }
    }
    
    // 电话联系：使用商品上填写的联系电话（未登录时后端不返回，这里给出提示）
    const callSeller = () => {
      if (!authStore.isLoggedIn) {
        ElMessage.warning('请先登录后查看卖家联系方式')
        router.push(`/auth/login?redirect=${encodeURIComponent(route.fullPath)}`)
        return
      }
      if (!product.contactPhone) {
        ElMessage.info('卖家未填写联系电话')
        return
      }
      ElMessageBox.alert(
        `联系人：${product.contactName || product.seller.username}\n联系电话：${product.contactPhone}`,
        '联系方式',
        { confirmButtonText: '知道了' }
      ).catch(() => {})
    }
    
    const goBack = () => {
      router.go(-1)
    }
    
    const goToHome = () => {
      router.push('/')
    }
    
    const goToEdit = () => {
      router.push(`/products/${product.id}/edit`)
    }

    const goToProducts = () => {
      router.push('/products')
    }
    
    onMounted(() => {
      if (productId.value) {
        fetchProductDetail()
        loadComments()
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
      orDash,
      canComment,
      comments,
      commentTotal,
      commentPage,
      commentPageSize,
      commentSubmitting,
      commentForm,
      loadComments,
      submitComment,
      toggleFavorite,
      contactSeller,
      callSeller,
      inquiry,
      addToCart,
      addingToCart,
      messageDialogVisible,
      messageDraft,
      messageSending,
      messageDialogConfig,
      submitMessage,
      goBack,
      goToHome,
      goToProducts,
      goToEdit,
      isOwner
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

.message-dialog-tip {
  margin: 0 0 12px;
  color: #909399;
  font-size: 13px;
  line-height: 1.6;
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

.comment-form {
  padding: 16px;
  margin-bottom: 20px;
  background: #f8f9fa;
  border-radius: 8px;
}

.comment-form-title {
  font-weight: 600;
  margin-bottom: 12px;
  color: #303133;
}

.comment-rate {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}

.comment-rate .label {
  color: #606266;
  margin-right: 8px;
}

.comment-submit {
  margin-top: 12px;
}

.comment-tip {
  margin-bottom: 20px;
}

.comment-pagination {
  display: flex;
  justify-content: center;
  margin-top: 20px;
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