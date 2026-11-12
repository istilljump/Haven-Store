<template>
  <div class="order-page">
    <div class="page-header">
      <h2>我的订单</h2>
      <el-radio-group v-model="role" size="small" @change="handleRoleChange">
        <el-radio-button value="buyer">我买到的</el-radio-button>
        <el-radio-button value="seller">我卖出的</el-radio-button>
      </el-radio-group>
    </div>

    <!-- 卖家视角按订单聚合，没有单条订单的状态筛选语义，这里只在买家视角展示 -->
    <el-tabs v-if="role === 'buyer'" v-model="activeStatus">
      <el-tab-pane label="全部" name="all" />
      <el-tab-pane label="待支付" name="1" />
      <el-tab-pane label="已支付" name="2" />
      <el-tab-pane label="已完成" name="4" />
      <el-tab-pane label="已取消" name="3" />
    </el-tabs>

    <el-empty
      v-if="!orders.length && !loading"
      :description="role === 'buyer' ? '还没有订单，去购物车结算试试' : '还没有卖出过商品'"
    >
      <el-button v-if="role === 'buyer'" type="primary" @click="$router.push('/cart')">
        去购物车
      </el-button>
    </el-empty>

    <div v-else class="order-list">
      <div v-for="order in orders" :key="order.orderNo" class="order-card">
        <div class="order-head">
          <span class="order-no" @click="$router.push(`/orders/${order.orderNo}`)"
            >订单号：{{ order.orderNo }}</span
          >
          <span class="order-time">{{ order.createTime }}</span>
          <el-tag class="order-status" :type="statusTagType(order.status)" size="small">
            {{ order.statusDesc }}
          </el-tag>
        </div>

        <div class="order-items">
          <div v-for="item in order.items" :key="item.productId" class="order-item">
            <img
              :src="item.coverImage || PLACEHOLDER_IMAGE"
              :alt="item.title"
              @click="$router.push(`/products/${item.productId}`)"
            />
            <div class="order-item-main">
              <div class="order-item-title" @click="$router.push(`/products/${item.productId}`)">
                {{ item.title }}
              </div>
              <div class="order-item-meta">
                <span
                  >{{ role === 'buyer' ? '卖家' : '买家' }}：{{
                    role === 'buyer' ? item.sellerName || '—' : order.buyerName
                  }}</span
                >
                <el-tag v-if="item.productDeleted" type="info" size="small" effect="plain">
                  商品已被删除
                </el-tag>
              </div>
            </div>
            <span class="order-item-price">{{ formatMoney(item.price) }}</span>
          </div>
        </div>

        <div v-if="order.remark" class="order-remark">买家留言：{{ order.remark }}</div>

        <div class="order-foot">
          <span class="order-summary">
            共 {{ order.itemCount }} 件，实付 <em>{{ formatMoney(order.totalAmount) }}</em>
          </span>
          <div class="order-actions">
            <el-button text type="primary" @click="$router.push(`/orders/${order.orderNo}`)">
              查看详情
            </el-button>
            <template v-if="role === 'buyer'">
              <el-button v-if="order.status === 1" text @click="cancelOrder(order)">
                取消订单
              </el-button>
              <el-button v-if="order.status === 1" type="danger" @click="payOrder(order)">
                去支付
              </el-button>
              <el-button v-if="order.status === 2" type="primary" @click="confirmOrder(order)">
                确认收货
              </el-button>
              <span v-if="order.status === 1" class="pay-tip">支付为模拟支付，不会真实扣款</span>
            </template>
          </div>
        </div>
      </div>

      <el-pagination
        v-if="total > pageSize"
        class="order-pagination"
        v-model:current-page="page"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="loadOrders"
      />
    </div>
  </div>
</template>

<script>
import { ref, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import orderApi from '@/api/order'
import { formatMoney } from '@/utils/format'

const PLACEHOLDER_IMAGE = '/default-product.png'

/** 订单状态与标签颜色的对应，避免在模板里写一堆三元表达式 */
const STATUS_TAG_TYPE = {
  1: 'warning',
  2: 'primary',
  3: 'info',
  4: 'success'
}

export default {
  name: 'OrderList',
  setup() {
    /** 视角：buyer 我买到的 / seller 我卖出的 */
    const role = ref('buyer')
    /** 买家视角的状态筛选：all 或订单状态码字符串 */
    const activeStatus = ref('all')
    const orders = ref([])
    const page = ref(1)
    const pageSize = ref(5)
    const total = ref(0)
    const loading = ref(false)

    const statusTagType = (status) => STATUS_TAG_TYPE[status] || 'info'

    const loadOrders = async () => {
      loading.value = true
      try {
        const params = { page: page.value, pageSize: pageSize.value }
        let data
        if (role.value === 'seller') {
          data = await orderApi.getSoldOrders(params)
        } else {
          if (activeStatus.value !== 'all') {
            params.status = Number(activeStatus.value)
          }
          data = await orderApi.getMyOrders(params)
        }
        orders.value = (data && data.records) || []
        total.value = (data && data.total) || 0
      } catch (error) {
        console.error('加载订单失败:', error)
      } finally {
        loading.value = false
      }
    }

    const reload = () => {
      page.value = 1
      loadOrders()
    }

    const handleRoleChange = () => {
      activeStatus.value = 'all'
      reload()
    }

    // 用 watch 而不是 el-tabs 的 tab-change 事件：行为不依赖组件库版本，切换筛选一定重新拉取
    watch(activeStatus, () => {
      reload()
    })

    /** 支付：明确告知是模拟支付，避免误导 */
    const payOrder = (order) => {
      ElMessageBox.confirm(
        `订单 ${order.orderNo} 应付金额 ${formatMoney(order.totalAmount)}。` +
          '本项目为课程作品，不会跳转真实支付渠道，也不会产生任何真实扣款。',
        '模拟支付',
        { type: 'info', confirmButtonText: '确认支付', cancelButtonText: '再想想' }
      )
        .then(async () => {
          try {
            await orderApi.payOrder(order.orderNo)
            ElMessage.success('支付成功（模拟支付）')
            await loadOrders()
          } catch (error) {
            console.error('支付失败:', error)
          }
        })
        .catch(() => {})
    }

    const cancelOrder = (order) => {
      ElMessageBox.confirm('取消后商品会重新回到在售状态，确定取消这笔订单吗？', '取消订单', {
        type: 'warning',
        confirmButtonText: '取消订单',
        cancelButtonText: '再想想'
      })
        .then(async () => {
          try {
            await orderApi.cancelOrder(order.orderNo)
            ElMessage.success('订单已取消')
            await loadOrders()
          } catch (error) {
            console.error('取消订单失败:', error)
          }
        })
        .catch(() => {})
    }

    const confirmOrder = (order) => {
      ElMessageBox.confirm('确认已收到商品？确认后订单将标记为已完成。', '确认收货', {
        type: 'info',
        confirmButtonText: '确认收货',
        cancelButtonText: '再等等'
      })
        .then(async () => {
          try {
            await orderApi.confirmOrder(order.orderNo)
            ElMessage.success('订单已完成')
            await loadOrders()
          } catch (error) {
            console.error('确认收货失败:', error)
          }
        })
        .catch(() => {})
    }

    onMounted(loadOrders)

    return {
      role,
      activeStatus,
      orders,
      page,
      pageSize,
      total,
      loading,
      statusTagType,
      loadOrders,
      handleRoleChange,
      payOrder,
      cancelOrder,
      confirmOrder,
      formatMoney,
      PLACEHOLDER_IMAGE
    }
  }
}
</script>

<style scoped>
.order-page {
  max-width: 1000px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 8px;
}

/* [UI美化] 页面标题对齐文字层级规范 */
.page-header h2 {
  margin: 0;
  font-size: 20px;
  color: var(--hs-text-primary);
  font-weight: 600;
}

.order-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* [UI美化-本轮] 订单卡首屏一次性渐入（stagger 40ms 递进，前 5 张） */
.order-list > * {
  animation: hs-fade-up 0.35s ease backwards;
}
.order-list > *:nth-child(2) {
  animation-delay: 0.04s;
}
.order-list > *:nth-child(3) {
  animation-delay: 0.08s;
}
.order-list > *:nth-child(4) {
  animation-delay: 0.12s;
}
.order-list > *:nth-child(n + 5) {
  animation-delay: 0.16s;
}

/* [UI美化] 订单卡片统一贴地薄影与 10px 圆角，悬浮轻微上浮 */
.order-card {
  border: 1px solid var(--hs-border);
  border-radius: 10px;
  background: var(--hs-bg-card);
  box-shadow: var(--hs-shadow-sm);
  overflow: hidden;
  transition: box-shadow 0.2s ease, transform 0.2s ease;
}

.order-card:hover {
  transform: translateY(-2px);
  /* [UI美化-本轮] hover 层次加深半档 */
  box-shadow: 0 12px 28px rgba(16, 24, 40, 0.14);
}

.order-head {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 10px 14px;
  background: #f8fafd;
  font-size: 13px;
  color: #606266;
}

.order-time {
  color: #909399;
}

.order-status {
  margin-left: auto;
}

.order-items {
  padding: 4px 14px;
}

.order-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid var(--hs-border-light);
}

.order-item:last-child {
  border-bottom: none;
}

.order-item img {
  width: 64px;
  height: 64px;
  object-fit: cover;
  border-radius: 6px;
  cursor: pointer;
  flex-shrink: 0;
  background: #f8fafd;
  /* [UI美化-本轮] 可点击封面补齐 hover 反馈：轻微透明度过渡 */
  transition: opacity 0.2s ease;
}

.order-item img:hover {
  opacity: 0.85;
}

.order-item-main {
  flex: 1;
  min-width: 0;
}

/* [UI美化] 标题悬浮加平滑过渡 */
.order-item-title {
  font-size: 15px;
  color: #303133;
  cursor: pointer;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color 0.2s ease;
}

.order-item-title:hover {
  color: #409eff;
}

.order-item-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}

.order-item-price {
  color: #f56c6c;
  font-size: 15px;
  flex-shrink: 0;
}

.order-remark {
  padding: 0 14px 10px;
  font-size: 13px;
  color: #909399;
}

.order-foot {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border-top: 1px solid var(--hs-border-light);
}

.order-summary {
  font-size: 14px;
}

.order-summary em {
  color: #f56c6c;
  font-size: 17px;
  font-style: normal;
  font-weight: 600;
  /* [UI美化-本轮] 合计金额数字等宽 */
  font-variant-numeric: tabular-nums;
}

.order-actions {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 10px;
}

.pay-tip {
  font-size: 12px;
  color: #c0c4cc;
}

.order-pagination {
  justify-content: center;
  margin-top: 6px;
}

/* [UI美化-本轮] 小屏适配：订单头/底允许换行，封面略缩，操作按钮不挤压 */
@media (max-width: 768px) {
  .order-head {
    flex-wrap: wrap;
    row-gap: 4px;
  }

  .order-item img {
    width: 56px;
    height: 56px;
  }

  .order-foot {
    flex-wrap: wrap;
    row-gap: 8px;
  }
}
</style>
