<template>
  <div class="order-detail-page">
    <div class="page-container" v-loading="loading">
      <el-page-header content="订单详情" @back="$router.back()" />

      <el-empty v-if="!loading && !order" description="订单不存在或无权查看" />

      <template v-else-if="order">
        <!-- 状态卡 -->
        <div class="status-card" :class="`status-${order.status}`">
          <div class="status-main">
            <span class="status-text">{{ order.statusDesc }}</span>
            <span class="status-hint">{{ statusHint }}</span>
          </div>
          <div class="status-no">订单号：{{ order.orderNo }}</div>
        </div>

        <!-- [轮8] 买家面交确认码卡片：仅买家本人 + 已支付 + 后端已下发 confirmCode 时展示 -->
        <div v-if="showConfirmCode" class="block confirm-code-card">
          <h3>面交确认码</h3>
          <div class="confirm-code-value">{{ order.confirmCode }}</div>
          <p class="confirm-code-tip">
            面交时向卖家出示此码，卖家输码后订单完成；也可收货后手动确认，7 天未确认自动完成
          </p>
        </div>

        <!-- 商品明细 -->
        <div class="block">
          <h3>商品明细</h3>
          <div v-for="item in order.items" :key="item.productId" class="order-item">
            <AppImage class="item-cover" :src="item.coverImage" :alt="item.title" />
            <div class="item-info">
              <div
                class="item-title"
                :class="{ deleted: item.productDeleted }"
                @click="goProduct(item)"
              >
                {{ item.title }}<span v-if="item.productDeleted">（商品已删除）</span>
              </div>
              <div class="item-seller">卖家：{{ item.sellerName || '未知' }}</div>
            </div>
            <div class="item-price">¥{{ formatPrice(item.price) }}</div>
          </div>
          <div class="order-total">
            共 {{ order.itemCount }} 件，合计：
            <span class="total-amount">¥{{ formatPrice(order.totalAmount) }}</span>
          </div>
        </div>

        <!-- 订单信息 -->
        <div class="block">
          <h3>订单信息</h3>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="买家">{{ order.buyerName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="买家留言">{{ order.remark || '无' }}</el-descriptions-item>
            <el-descriptions-item label="下单时间">{{
              order.createTime || '-'
            }}</el-descriptions-item>
            <el-descriptions-item v-if="order.payTime" label="支付时间">{{
              order.payTime
            }}</el-descriptions-item>
            <el-descriptions-item v-if="order.finishTime" label="完成时间">{{
              order.finishTime
            }}</el-descriptions-item>
            <!-- [轮8] 完成方式：1 买家确认收货 / 2 卖家输码面交 / 3 超时自动完成 -->
            <el-descriptions-item v-if="order.confirmedBy" label="完成方式">{{
              confirmByLabel
            }}</el-descriptions-item>
            <el-descriptions-item v-if="order.cancelTime" label="取消时间">{{
              order.cancelTime
            }}</el-descriptions-item>
          </el-descriptions>
          <p class="pay-notice">本项目为模拟支付，不产生任何真实扣款。</p>
        </div>

        <!-- 操作区 -->
        <div class="action-bar" v-if="canPay || canCancel || canConfirm || canConfirmByCode">
          <el-button v-if="canPay" type="primary" :loading="acting" @click="payOrder"
            >模拟支付</el-button
          >
          <el-button v-if="canCancel" :loading="acting" @click="cancelOrder">取消订单</el-button>
          <el-button v-if="canConfirm" type="success" :loading="acting" @click="confirmOrder"
            >确认收货</el-button
          >
          <!-- [轮8] 卖家输码面交：已支付且尚无完成方式时展示 -->
          <el-button
            v-if="canConfirmByCode"
            type="success"
            plain
            :loading="acting"
            @click="confirmByCode"
            >输码确认交付</el-button
          >
        </div>
      </template>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppImage from '@/components/AppImage.vue'
import {
  getOrderDetail,
  payOrder as payOrderApi,
  cancelOrder as cancelOrderApi,
  confirmOrder as confirmOrderApi,
  confirmByCode as confirmByCodeApi
} from '@/api/order'
import { formatMoney } from '@/utils/format'
import { ORDER_STATUS } from '@/utils/constants'
import { useUserStore } from '@/store/index'

/**
 * 订单详情页
 * <p>
 * 此前订单信息全部堆在列表卡片里；本页按订单号展示完整明细与时间线。
 * 买家本人或订单内商品的卖家可查看（后端校验）；支付/取消/确认收货仅买家可操作。
 */
export default {
  name: 'OrderDetail',
  components: { AppImage },
  setup() {
    const route = useRoute()
    const router = useRouter()
    const userStore = useUserStore()
    const order = ref(null)
    const loading = ref(false)
    const acting = ref(false)

    const canPay = computed(() => order.value?.status === ORDER_STATUS.PENDING_PAY)
    const canCancel = computed(() => order.value?.status === ORDER_STATUS.PENDING_PAY)
    const canConfirm = computed(() => order.value?.status === ORDER_STATUS.PAID)

    // [轮8] 买家本人视角（后端仅向订单买家本人下发 confirmCode，这里按 buyerId 再做一层判断）
    const isBuyerView = computed(
      () =>
        userStore.userId != null &&
        order.value != null &&
        Number(order.value.buyerId) === Number(userStore.userId)
    )

    // [轮8] 卖家视角：订单内任一商品的卖家是当前登录用户
    const isSellerView = computed(() => {
      if (userStore.userId == null || !order.value?.items) return false
      return order.value.items.some((item) => Number(item.sellerId) === Number(userStore.userId))
    })

    // [轮8] 买家面交确认码卡片：买家本人 + 已支付 + 后端已下发确认码
    const showConfirmCode = computed(
      () =>
        isBuyerView.value && order.value?.status === ORDER_STATUS.PAID && !!order.value?.confirmCode
    )

    // [轮8] 卖家输码入口：卖家视角 + 已支付 + 尚未产生完成方式
    const canConfirmByCode = computed(
      () =>
        isSellerView.value &&
        order.value?.status === ORDER_STATUS.PAID &&
        order.value?.confirmedBy == null
    )

    // [轮8] 完成方式文案：1 买家确认收货 / 2 卖家输码面交 / 3 超时自动完成
    const confirmByLabel = computed(() => {
      const labelMap = { 1: '买家确认收货', 2: '卖家输码面交', 3: '超时自动完成' }
      return labelMap[order.value?.confirmedBy] || '-'
    })

    const statusHint = computed(() => {
      switch (order.value?.status) {
        case ORDER_STATUS.PENDING_PAY:
          return '请在 30 分钟内完成支付，超时订单将自动取消'
        case ORDER_STATUS.PAID:
          return '卖家已收款，请尽快线下交接或等待发货'
        case ORDER_STATUS.CANCELLED:
          return '订单已取消，商品已回到在售'
        case ORDER_STATUS.FINISHED:
          return '交易已完成，感谢使用'
        default:
          return ''
      }
    })

    const formatPrice = (value) => formatMoney(value)

    const loadOrder = async () => {
      loading.value = true
      try {
        order.value = await getOrderDetail(route.params.orderNo)
      } catch (err) {
        order.value = null
      } finally {
        loading.value = false
      }
    }

    const goProduct = (item) => {
      if (!item.productDeleted) {
        router.push(`/products/${item.productId}`)
      }
    }

    const payOrder = () => {
      ElMessageBox.confirm('确认为该订单支付吗？（模拟支付，不产生真实扣款）', '模拟支付', {
        type: 'info'
      })
        .then(async () => {
          acting.value = true
          try {
            await payOrderApi(order.value.orderNo)
            ElMessage.success('支付成功')
            await loadOrder()
          } finally {
            acting.value = false
          }
        })
        .catch(() => {})
    }

    const cancelOrder = () => {
      ElMessageBox.confirm('取消后商品将回到在售状态，确定取消该订单吗？', '取消订单', {
        type: 'warning'
      })
        .then(async () => {
          acting.value = true
          try {
            await cancelOrderApi(order.value.orderNo)
            ElMessage.success('订单已取消')
            await loadOrder()
          } finally {
            acting.value = false
          }
        })
        .catch(() => {})
    }

    const confirmOrder = () => {
      ElMessageBox.confirm('请确认已收到商品并验货无误，确认后交易完成。', '确认收货', {
        type: 'warning'
      })
        .then(async () => {
          acting.value = true
          try {
            await confirmOrderApi(order.value.orderNo)
            ElMessage.success('交易已完成')
            await loadOrder()
          } finally {
            acting.value = false
          }
        })
        .catch(() => {})
    }

    // [轮8] 卖家输码确认交付：6 位数字校验（错码累计 5 次后端会锁定），成功后刷新详情
    const confirmByCode = () => {
      ElMessageBox.prompt('请输入买家当面出示的 6 位面交确认码', '输码确认交付', {
        type: 'info',
        confirmButtonText: '确认交付',
        cancelButtonText: '取消',
        inputPattern: /^\d{6}$/,
        inputErrorMessage: '请输入 6 位数字确认码'
      })
        .then(async ({ value }) => {
          acting.value = true
          try {
            await confirmByCodeApi(order.value.orderNo, value)
            ElMessage.success('面交确认成功，订单已完成')
            await loadOrder()
          } finally {
            acting.value = false
          }
        })
        .catch(() => {})
    }

    onMounted(loadOrder)

    return {
      order,
      loading,
      acting,
      canPay,
      canCancel,
      canConfirm,
      statusHint,
      formatPrice,
      goProduct,
      payOrder,
      cancelOrder,
      confirmOrder,
      // [轮8] 面交码相关绑定
      showConfirmCode,
      canConfirmByCode,
      confirmByLabel,
      confirmByCode
    }
  }
}
</script>

<style scoped>
.order-detail-page {
  background: var(--hs-bg-page, #f5f7fa);
  min-height: 100%;
  padding: 20px;
}

/* [UI美化] 主面板补贴地薄影，与灰底分层 */
.page-container {
  max-width: 860px;
  margin: 0 auto;
  background: var(--hs-bg-card, #fff);
  border-radius: 10px;
  padding: 20px 24px 28px;
  box-shadow: var(--hs-shadow-sm);
}

/* [UI美化-本轮] 状态横幅升级：左侧状态色条随状态自动变色（currentColor） */
.status-card {
  margin-top: 18px;
  border-radius: 10px;
  padding: 18px 22px;
  background: #ecf5ff;
  color: #409eff;
  border-left: 4px solid currentColor;
}

.status-card.status-2 {
  background: #f0f9eb;
  color: #67c23a;
}

.status-card.status-3 {
  background: var(--hs-bg-price-soft, #fef0f0);
  color: var(--hs-price, #f56c6c);
}

.status-card.status-4 {
  background: #f0f9eb;
  color: #67c23a;
}

.status-main {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.status-text {
  font-size: 22px;
  /* [UI美化-本轮] 状态文字加重，与提示文字拉开层级 */
  font-weight: 700;
}

.status-hint {
  font-size: 13px;
  opacity: 0.85;
}

/* [UI美化-本轮] 订单号：细线分隔 + 数字等宽 */
.status-no {
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid rgba(255, 255, 255, 0.6);
  font-size: 13px;
  opacity: 0.8;
  font-variant-numeric: tabular-nums;
}

.block {
  margin-top: 22px;
}

/* [UI美化] 区块标题加重，对齐标题层级规范 */
.block h3 {
  margin: 0 0 12px;
  font-size: 16px;
  color: var(--hs-text-primary, #303133);
  font-weight: 600;
}

.order-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px dashed var(--hs-border, #ebeef5);
}

.item-cover {
  width: 72px;
  height: 72px;
  border-radius: 6px;
  flex-shrink: 0;
}

.item-info {
  flex: 1;
  min-width: 0;
}

/* [UI美化] 标题悬浮加平滑过渡 */
.item-title {
  font-size: 14px;
  color: var(--hs-text-primary, #303133);
  cursor: pointer;
  transition: color 0.2s ease;
}

.item-title:hover {
  color: #409eff;
}

.item-title.deleted {
  color: var(--hs-text-secondary, #909399);
  cursor: default;
}

.item-seller {
  margin-top: 4px;
  font-size: 12px;
  color: var(--hs-text-secondary, #909399);
}

.item-price {
  color: var(--hs-price, #f56c6c);
  font-weight: 600;
}

.order-total {
  text-align: right;
  padding-top: 12px;
  font-size: 14px;
  color: var(--hs-text-regular, #606266);
}

.total-amount {
  color: var(--hs-price, #f56c6c);
  font-size: 18px;
  font-weight: 600;
}

.pay-notice {
  margin: 10px 0 0;
  font-size: 12px;
  color: #e6a23c;
}

.action-bar {
  margin-top: 22px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

/* [轮8] 买家面交确认码卡片：10px 圆角面板 + 大号等宽码值（Token 配色，暗色自动适配） */
.confirm-code-card {
  border: 1px solid var(--hs-border);
  border-radius: 10px;
  background: var(--hs-bg-panel);
  padding: 14px 16px 16px;
}

.confirm-code-value {
  font-size: 30px;
  font-weight: 700;
  letter-spacing: 8px;
  color: var(--hs-text-primary);
  font-variant-numeric: tabular-nums;
}

.confirm-code-tip {
  margin: 8px 0 0;
  font-size: 12px;
  line-height: 1.6;
  color: var(--hs-text-secondary);
}

@media (max-width: 768px) {
  .page-container {
    padding: 14px;
  }
}
</style>
