<template>
  <div class="cart-page">
    <div class="page-header">
      <h2>购物车</h2>
      <span class="header-tip">收藏与购物车可以互相转移，勾选后可一起结算</span>
    </div>

    <el-tabs v-model="activeTab">
      <!-- ==================== 购物车 ==================== -->
      <el-tab-pane name="cart">
        <template #label>
          购物车
          <span v-if="cartItems.length" class="tab-count">({{ cartItems.length }})</span>
        </template>

        <el-empty v-if="!cartItems.length" description="购物车还是空的">
          <el-button type="primary" @click="$router.push('/products')">去逛逛</el-button>
        </el-empty>

        <template v-else>
          <div class="toolbar">
            <el-checkbox
              :model-value="allSelected"
              :indeterminate="indeterminate"
              :disabled="!selectableItems.length"
              @change="toggleSelectAll"
            >
              全选
            </el-checkbox>
            <span class="toolbar-count">已选 {{ selectedIds.length }} 件</span>
            <el-button
              v-if="unavailableCount > 0"
              text
              type="warning"
              @click="clearUnavailable"
            >
              清理失效商品（{{ unavailableCount }}）
            </el-button>
            <div class="toolbar-right">
              <span class="total">
                合计：<em>{{ formatMoney(selectedTotal) }}</em>
              </span>
              <el-button
                type="danger"
                :disabled="!selectedIds.length"
                @click="openCheckout"
              >
                去结算
              </el-button>
            </div>
          </div>

          <div class="item-list">
            <div
              v-for="item in cartItems"
              :key="item.productId"
              class="cart-item"
              :class="{ unavailable: !item.available }"
            >
              <el-checkbox
                class="item-check"
                :model-value="isSelected(item.productId)"
                :disabled="!item.available"
                @change="(checked) => toggleSelect(item.productId, checked)"
              />
              <img
                class="item-cover"
                :src="item.coverImage || PLACEHOLDER_IMAGE"
                :alt="item.title"
                @click="goToProduct(item.productId)"
              />
              <div class="item-main">
                <div class="item-title" @click="goToProduct(item.productId)">
                  {{ item.title }}
                </div>
                <div class="item-meta">
                  <el-tag v-if="item.productCondition" size="small" effect="plain">
                    {{ item.productCondition }}
                  </el-tag>
                  <el-tag v-if="item.tradeType" size="small" type="info" effect="plain">
                    {{ item.tradeType }}
                  </el-tag>
                  <span class="seller">卖家：{{ item.sellerName || '—' }}</span>
                </div>
                <div v-if="!item.available" class="item-warning">
                  <el-tag type="danger" size="small" effect="plain">
                    {{ item.unavailableReason }}
                  </el-tag>
                  <span class="warning-tip">该商品无法结算，可移出购物车</span>
                </div>
              </div>
              <div class="item-price">
                <em>{{ formatMoney(item.price) }}</em>
                <span v-if="item.originalPrice" class="origin-price">
                  {{ formatMoney(item.originalPrice) }}
                </span>
              </div>
              <div class="item-actions">
                <el-button text type="primary" @click="toFavorite(item)">移入收藏</el-button>
                <el-button text type="danger" @click="removeItem(item)">删除</el-button>
              </div>
            </div>
          </div>
        </template>
      </el-tab-pane>

      <!-- ==================== 我的收藏 ==================== -->
      <el-tab-pane name="favorite">
        <template #label>
          我的收藏
          <span v-if="favorites.length" class="tab-count">({{ favorites.length }})</span>
        </template>

        <el-empty v-if="!favorites.length" description="还没有收藏商品">
          <el-button type="primary" @click="$router.push('/products')">去逛逛</el-button>
        </el-empty>

        <template v-else>
          <div class="toolbar">
            <span class="toolbar-count">共收藏 {{ favorites.length }} 件</span>
            <div class="toolbar-right">
              <el-button :loading="addingAll" @click="addAllFavorites">
                全部加入购物车
              </el-button>
            </div>
          </div>

          <div class="item-list">
            <div
              v-for="item in favorites"
              :key="item.id"
              class="cart-item"
              :class="{ unavailable: item.status !== 1 }"
            >
              <img
                class="item-cover"
                :src="item.coverImage || PLACEHOLDER_IMAGE"
                :alt="item.title"
                @click="goToProduct(item.id)"
              />
              <div class="item-main">
                <div class="item-title" @click="goToProduct(item.id)">{{ item.title }}</div>
                <div class="item-meta">
                  <el-tag v-if="item.productCondition" size="small" effect="plain">
                    {{ item.productCondition }}
                  </el-tag>
                  <span class="seller">卖家：{{ item.username || '—' }}</span>
                </div>
                <div v-if="item.status !== 1" class="item-warning">
                  <el-tag type="danger" size="small" effect="plain">
                    {{ item.status === 2 ? '已售出' : '已下架' }}
                  </el-tag>
                  <span class="warning-tip">该商品已无法购买</span>
                </div>
              </div>
              <div class="item-price">
                <em>{{ formatMoney(item.price) }}</em>
                <span v-if="item.originalPrice" class="origin-price">
                  {{ formatMoney(item.originalPrice) }}
                </span>
              </div>
              <div class="item-actions">
                <el-button
                  text
                  type="primary"
                  :disabled="item.status !== 1"
                  @click="addToCartFromFavorite(item)"
                >
                  加入购物车
                </el-button>
                <el-button text type="danger" @click="removeFavorite(item)">取消收藏</el-button>
              </div>
            </div>
          </div>
        </template>
      </el-tab-pane>
    </el-tabs>

    <!-- 结算确认：先把买了什么、花多少钱列清楚，再提交订单 -->
    <el-dialog v-model="checkoutVisible" title="确认订单" width="560px">
      <div class="checkout-list">
        <div v-for="item in checkoutItems" :key="item.productId" class="checkout-item">
          <img :src="item.coverImage || PLACEHOLDER_IMAGE" :alt="item.title" />
          <span class="checkout-title">{{ item.title }}</span>
          <span class="checkout-price">{{ formatMoney(item.price) }}</span>
        </div>
      </div>
      <div class="checkout-total">
        共 {{ checkoutItems.length }} 件，合计 <em>{{ formatMoney(selectedTotal) }}</em>
      </div>
      <el-input
        v-model="remark"
        type="textarea"
        :rows="2"
        maxlength="255"
        show-word-limit
        placeholder="给卖家留言（可选），例如约定的交易时间地点"
      />
      <el-alert
        class="checkout-notice"
        type="info"
        :closable="false"
        show-icon
        title="提交后商品即被锁定，请在「我的订单」中完成支付"
        description="本项目为课程作品，支付为模拟支付，不会产生任何真实扣款。"
      />
      <template #footer>
        <el-button @click="checkoutVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitOrder">
          提交订单
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import cartApi from '@/api/cart'
import orderApi from '@/api/order'
import productApi from '@/api/product'
import { useUserStore } from '@/store/index'
import { formatMoney } from '@/utils/format'

/** 商城统一的缺省封面：与商品列表页保持一致 */
const PLACEHOLDER_IMAGE = '/default-product.png'

export default {
  name: 'Cart',
  setup() {
    const router = useRouter()
    const userStore = useUserStore()

    const activeTab = ref('cart')
    const cartItems = ref([])
    const favorites = ref([])
    /** 购物车中已勾选的商品 ID */
    const selectedIds = ref([])
    const checkoutVisible = ref(false)
    const remark = ref('')
    const submitting = ref(false)
    const addingAll = ref(false)

    /** 可结算的购物车条目 */
    const selectableItems = computed(() => cartItems.value.filter((item) => item.available))
    /** 失效条目（已售出/已下架/自己发布的） */
    const unavailableCount = computed(() => cartItems.value.length - selectableItems.value.length)
    const allSelected = computed(() => selectableItems.value.length > 0
      && selectedIds.value.length === selectableItems.value.length)
    const indeterminate = computed(() => selectedIds.value.length > 0 && !allSelected.value)
    const checkoutItems = computed(() => cartItems.value.filter(
      (item) => selectedIds.value.includes(item.productId)
    ))
    const selectedTotal = computed(() => checkoutItems.value.reduce(
      (sum, item) => sum + Number(item.price || 0), 0
    ))

    const isSelected = (productId) => selectedIds.value.includes(productId)

    /** 把购物车件数同步给 store，头部角标即时更新 */
    const syncCartCount = async () => {
      try {
        userStore.setCartItemCount(await cartApi.getCartCount())
      } catch (error) {
        console.error('获取购物车件数失败:', error)
      }
    }

    const loadCart = async () => {
      try {
        cartItems.value = (await cartApi.getCartList()) || []
        // 勾选状态只保留仍然存在且可结算的条目，避免取消勾选后残留
        const validIds = cartItems.value.filter((item) => item.available)
          .map((item) => item.productId)
        selectedIds.value = selectedIds.value.filter((id) => validIds.includes(id))
      } catch (error) {
        console.error('加载购物车失败:', error)
      }
    }

    const loadFavorites = async () => {
      try {
        const data = await productApi.getFavoriteProducts({ page: 1, pageSize: 100 })
        favorites.value = (data && data.records) || []
      } catch (error) {
        console.error('加载收藏失败:', error)
      }
    }

    const loadAll = async () => {
      await Promise.all([loadCart(), loadFavorites()])
      await syncCartCount()
    }

    const toggleSelect = (productId, checked) => {
      if (checked) {
        if (!selectedIds.value.includes(productId)) {
          selectedIds.value = [...selectedIds.value, productId]
        }
      } else {
        selectedIds.value = selectedIds.value.filter((id) => id !== productId)
      }
    }

    const toggleSelectAll = (checked) => {
      selectedIds.value = checked
        ? selectableItems.value.map((item) => item.productId)
        : []
    }

    const goToProduct = (productId) => router.push(`/products/${productId}`)

    const removeItem = (item) => {
      ElMessageBox.confirm(`确定把「${item.title}」移出购物车吗？`, '移出购物车', {
        type: 'warning', confirmButtonText: '移出', cancelButtonText: '取消'
      }).then(async () => {
        try {
          await cartApi.removeFromCart(item.productId)
          ElMessage.success('已移出购物车')
          await loadCart()
          await syncCartCount()
        } catch (error) {
          console.error('移出购物车失败:', error)
        }
      }).catch(() => {})
    }

    const toFavorite = async (item) => {
      try {
        await cartApi.moveToFavorite(item.productId)
        ElMessage.success('已移入收藏')
        await loadAll()
      } catch (error) {
        console.error('移入收藏失败:', error)
      }
    }

    /** 一键清理失效商品，省得一条条删 */
    const clearUnavailable = () => {
      const invalid = cartItems.value.filter((item) => !item.available)
      if (!invalid.length) return
      ElMessageBox.confirm(
        `确定清理这 ${invalid.length} 件已无法购买的商品吗？`,
        '清理失效商品',
        { type: 'warning', confirmButtonText: '清理', cancelButtonText: '取消' }
      ).then(async () => {
        try {
          for (const item of invalid) {
            await cartApi.removeFromCart(item.productId)
          }
          ElMessage.success(`已清理 ${invalid.length} 件失效商品`)
          await loadAll()
        } catch (error) {
          console.error('清理失效商品失败:', error)
        }
      }).catch(() => {})
    }

    const addToCartFromFavorite = async (item) => {
      try {
        const added = await cartApi.addToCart(item.id)
        ElMessage.success(added ? '已加入购物车' : '该商品已在购物车中')
        await loadAll()
      } catch (error) {
        console.error('加入购物车失败:', error)
      }
    }

    const removeFavorite = (item) => {
      ElMessageBox.confirm(`确定取消收藏「${item.title}」吗？`, '取消收藏', {
        type: 'warning', confirmButtonText: '取消收藏', cancelButtonText: '再想想'
      }).then(async () => {
        try {
          await productApi.unfavoriteProduct(item.id)
          ElMessage.success('已取消收藏')
          await loadFavorites()
        } catch (error) {
          console.error('取消收藏失败:', error)
        }
      }).catch(() => {})
    }

    /** 收藏批量加入购物车：分别提示成功与跳过，不做笼统的"已加入" */
    const addAllFavorites = async () => {
      addingAll.value = true
      try {
        const result = await cartApi.addAllFavoritesToCart()
        const added = (result && result.added) || 0
        const skipped = (result && result.skipped) || 0
        if (added === 0 && skipped === 0) {
          ElMessage.info('收藏里的商品都已在购物车中')
        } else {
          ElMessage.success(
            skipped > 0
              ? `已加入购物车 ${added} 件，另有 ${skipped} 件因已售出或已下架被跳过`
              : `已加入购物车 ${added} 件`
          )
        }
        await loadAll()
        if (added > 0) {
          activeTab.value = 'cart'
        }
      } catch (error) {
        console.error('批量加入购物车失败:', error)
      } finally {
        addingAll.value = false
      }
    }

    const openCheckout = () => {
      if (!selectedIds.value.length) {
        ElMessage.warning('请先勾选要结算的商品')
        return
      }
      remark.value = ''
      checkoutVisible.value = true
    }

    const submitOrder = async () => {
      submitting.value = true
      try {
        const payload = { productIds: [...selectedIds.value] }
        if (remark.value.trim()) {
          payload.remark = remark.value.trim()
        }
        const order = await orderApi.createOrder(payload)
        checkoutVisible.value = false
        ElMessage.success(`订单已创建：${order.orderNo}，请完成支付`)
        // 下单后商品已被移出购物车，回到购物车页需要重新拉取
        selectedIds.value = []
        await loadAll()
        router.push('/orders')
      } catch (error) {
        console.error('创建订单失败:', error)
      } finally {
        submitting.value = false
      }
    }

    onMounted(loadAll)

    return {
      activeTab,
      cartItems,
      favorites,
      selectedIds,
      checkoutVisible,
      remark,
      submitting,
      addingAll,
      selectableItems,
      unavailableCount,
      allSelected,
      indeterminate,
      checkoutItems,
      selectedTotal,
      isSelected,
      toggleSelect,
      toggleSelectAll,
      goToProduct,
      removeItem,
      toFavorite,
      clearUnavailable,
      addToCartFromFavorite,
      removeFavorite,
      addAllFavorites,
      openCheckout,
      submitOrder,
      formatMoney,
      PLACEHOLDER_IMAGE
    }
  }
}
</script>

<style scoped>
.cart-page {
  max-width: 1100px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 8px;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
}

.header-tip {
  font-size: 13px;
  color: #909399;
}

.tab-count {
  color: #909399;
  font-size: 13px;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  margin-bottom: 12px;
  background: #f7f8fa;
  border-radius: 6px;
}

.toolbar-count {
  font-size: 13px;
  color: #606266;
}

.toolbar-right {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 14px;
}

.total em,
.checkout-total em {
  color: #f56c6c;
  font-size: 18px;
  font-style: normal;
  font-weight: 600;
}

.item-list {
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  overflow: hidden;
}

.cart-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px;
  border-bottom: 1px solid #f2f3f5;
  background: #fff;
}

.cart-item:last-child {
  border-bottom: none;
}

.cart-item.unavailable {
  background: #fafafa;
  opacity: 0.75;
}

.item-check {
  flex-shrink: 0;
}

.item-cover {
  width: 84px;
  height: 84px;
  object-fit: cover;
  border-radius: 6px;
  cursor: pointer;
  flex-shrink: 0;
  background: #f5f5f5;
}

.item-main {
  flex: 1;
  min-width: 0;
}

.item-title {
  font-size: 15px;
  color: #303133;
  cursor: pointer;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-title:hover {
  color: #409eff;
}

.item-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
}

.item-warning {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 6px;
}

.warning-tip {
  font-size: 12px;
  color: #f56c6c;
}

.item-price {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  flex-shrink: 0;
}

.item-price em {
  color: #f56c6c;
  font-size: 16px;
  font-style: normal;
  font-weight: 600;
}

.origin-price {
  font-size: 12px;
  color: #c0c4cc;
  text-decoration: line-through;
}

.item-actions {
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.checkout-list {
  max-height: 260px;
  overflow-y: auto;
  margin-bottom: 12px;
}

.checkout-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
  border-bottom: 1px solid #f2f3f5;
}

.checkout-item img {
  width: 48px;
  height: 48px;
  object-fit: cover;
  border-radius: 4px;
  flex-shrink: 0;
}

.checkout-title {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
}

.checkout-price {
  color: #f56c6c;
  font-size: 14px;
}

.checkout-total {
  text-align: right;
  margin-bottom: 12px;
  font-size: 14px;
}

.checkout-notice {
  margin-top: 12px;
}
</style>
