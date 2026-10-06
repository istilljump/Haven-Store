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
            <el-descriptions-item v-if="order.cancelTime" label="取消时间">{{
              order.cancelTime
            }}</el-descriptions-item>
          </el-descriptions>
          <p class="pay-notice">本项目为模拟支付，不产生任何真实扣款。</p>
        </div>

        <!-- 操作区 -->
        <div class="action-bar" v-if="canPay || canCancel || canConfirm">
          <el-button v-if="canPay" type="primary" :loading="acting" @click="payOrder"
            >模拟支付</el-button
          >
          <el-button v-if="canCancel" :loading="acting" @click="cancelOrder">取消订单</el-button>
          <el-button v-if="canConfirm" type="success" :loading="acting" @click="confirmOrder"
            >确认收货</el-button
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
  confirmOrder as confirmOrderApi
} from '@/api/order'
import { formatMoney } from '@/utils/format'
import { ORDER_STATUS } from '@/utils/constants'

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
    const order = ref(null)
    const loading = ref(false)
    const acting = ref(false)

    const canPay = computed(() => order.value?.status === ORDER_STATUS.PENDING_PAY)
    const canCancel = computed(() => order.value?.status === ORDER_STATUS.PENDING_PAY)
    const canConfirm = computed(() => order.value?.status === ORDER_STATUS.PAID)

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
      confirmOrder
    }
  }
}
</script>

<style scoped>
.order-detail-page {
  background: #f5f7fa;
  min-height: 100%;
  padding: 20px;
}

.page-container {
  max-width: 860px;
  margin: 0 auto;
  background: #fff;
  border-radius: 10px;
  padding: 20px 24px 28px;
}

.status-card {
  margin-top: 18px;
  border-radius: 8px;
  padding: 20px 24px;
  background: #ecf5ff;
  color: #409eff;
}

.status-card.status-2 {
  background: #f0f9eb;
  color: #67c23a;
}

.status-card.status-3 {
  background: #fef0f0;
  color: #f56c6c;
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
  font-weight: 600;
}

.status-hint {
  font-size: 13px;
  opacity: 0.85;
}

.status-no {
  margin-top: 8px;
  font-size: 13px;
  opacity: 0.8;
}

.block {
  margin-top: 22px;
}

.block h3 {
  margin: 0 0 12px;
  font-size: 16px;
  color: #303133;
}

.order-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px dashed #ebeef5;
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

.item-title {
  font-size: 14px;
  color: #303133;
  cursor: pointer;
}

.item-title:hover {
  color: #409eff;
}

.item-title.deleted {
  color: #909399;
  cursor: default;
}

.item-seller {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}

.item-price {
  color: #f56c6c;
  font-weight: 600;
}

.order-total {
  text-align: right;
  padding-top: 12px;
  font-size: 14px;
  color: #606266;
}

.total-amount {
  color: #f56c6c;
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

@media (max-width: 768px) {
  .page-container {
    padding: 14px;
  }
}
</style>
