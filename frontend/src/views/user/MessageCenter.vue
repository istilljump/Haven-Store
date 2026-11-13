<template>
  <div class="message-center">
    <div class="mc-header">
      <h2>我的私信</h2>
      <el-button text :icon="Refresh" @click="refreshAll">刷新</el-button>
    </div>

    <div class="mc-body">
      <!-- 左侧：会话列表 -->
      <aside class="conversation-list">
        <el-empty
          v-if="!conversations.length"
          description="还没有私信，去商品页联系卖家试试"
          :image-size="80"
        />
        <div
          v-for="item in conversations"
          :key="conversationId(item)"
          class="conversation-item"
          :class="{ active: isActive(item) }"
          @click="openConversation(item)"
        >
          <div class="conv-avatar">
            <el-avatar :src="item.peerAvatar" :size="40">
              {{ (item.peerName || '?').charAt(0) }}
            </el-avatar>
            <!-- 未读数用普通元素 + v-if 渲染：el-badge 的隐藏依赖过渡动画，
                 在标签页不可见等场景下动画可能不推进，会残留旧的未读数字 -->
            <span v-if="item.unreadCount" class="conv-unread">
              {{ item.unreadCount > 99 ? '99+' : item.unreadCount }}
            </span>
          </div>
          <div class="conv-main">
            <div class="conv-top">
              <span class="conv-name">{{ item.peerName }}</span>
              <span class="conv-time">{{ formatRelativeTime(item.lastTime) }}</span>
            </div>
            <div v-if="item.productId" class="conv-product">
              <el-tag size="small" type="warning" effect="plain">咨询商品</el-tag>
              <span class="conv-product-title">{{ item.productTitle || '商品已删除' }}</span>
            </div>
            <div class="conv-last">{{ item.lastContent }}</div>
          </div>
          <el-button
            class="conv-delete"
            text
            :icon="Delete"
            title="删除会话"
            @click.stop="removeConversation(item)"
          />
        </div>
      </aside>

      <!-- 右侧：聊天窗口 -->
      <section class="chat-pane">
        <el-empty v-if="!active" description="选择左侧会话开始聊天" />

        <template v-else>
          <div class="chat-header">
            <span class="chat-title">{{ active.peerName }}</span>
            <div v-if="active.productId" class="chat-product">
              <el-tag size="small" type="warning" effect="plain">咨询商品</el-tag>
              <router-link class="chat-product-link" :to="`/products/${active.productId}`">
                {{ active.productTitle || '商品已删除' }}
              </router-link>
            </div>
          </div>

          <div ref="chatBodyRef" class="chat-body">
            <el-empty
              v-if="!messages.length"
              description="还没有消息，打个招呼吧"
              :image-size="70"
            />
            <div
              v-for="message in messages"
              :key="message.id"
              class="chat-row"
              :class="{ self: message.self }"
            >
              <!-- [轮8] 报价消息渲染为报价卡片（嵌在聊天气泡位），普通消息保持原气泡 -->
              <div v-if="message.offerPrice != null" class="bubble bubble-offer">
                <div class="offer-head">
                  <span class="offer-title">议价报价</span>
                  <el-tag size="small" :type="offerStatusTagType(message)">
                    {{ offerStatusText(message) }}
                  </el-tag>
                </div>
                <div class="offer-price">¥{{ message.offerPrice }}</div>
                <div class="offer-content">{{ message.content }}</div>
                <div v-if="showOfferExpire(message)" class="offer-expire">
                  {{ offerExpireText(message) }}
                </div>
                <!-- [轮8] 接收方（买家）且待处理且未过期：可直接按报价下单 -->
                <el-button
                  v-if="canAcceptOffer(message)"
                  class="offer-accept"
                  type="primary"
                  size="small"
                  @click="acceptOffer(message)"
                >
                  接受报价并下单
                </el-button>
              </div>
              <div v-else class="bubble">{{ message.content }}</div>
              <div class="bubble-time">{{ formatDate(message.createTime, 'MM-DD HH:mm') }}</div>
            </div>
          </div>

          <div class="chat-input">
            <el-input
              v-model="draft"
              type="textarea"
              :rows="3"
              maxlength="500"
              show-word-limit
              placeholder="输入消息，Enter 发送，Shift + Enter 换行"
              @keydown.enter.exact.prevent="send"
            />
            <!-- [轮8] 卖家发报价入口：仅当会话商品是我发布的且议价开关开启时显示 -->
            <el-button v-if="canOffer" :disabled="sending" @click="openOfferDialog">报价</el-button>
            <el-button type="primary" :loading="sending" @click="send">发送</el-button>
          </div>
        </template>
      </section>
    </div>
  </div>
</template>

<script>
import { ref, computed, nextTick, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Refresh } from '@element-plus/icons-vue'
import messageApi from '@/api/message'
import { getProductDetail } from '@/api/product'
import { getPublicSettings } from '@/api/public'
import { useUserStore } from '@/store/index'
import { formatDate, formatRelativeTime } from '@/utils/format'

/** 会话自动刷新间隔：聊天页停留时用轮询拉新消息，避免为此引入 WebSocket */
const POLL_INTERVAL_MS = 10000

export default {
  name: 'MessageCenter',
  // Delete/Refresh 仅通过 :icon 属性绑定使用，无需 components 注册
  components: {},
  setup() {
    const route = useRoute()
    const router = useRouter()
    const userStore = useUserStore()

    /** 会话列表 */
    const conversations = ref([])
    /** 当前打开的会话（null 表示未选中） */
    const active = ref(null)
    /** 当前会话的聊天记录 */
    const messages = ref([])
    /** 输入框内容 */
    const draft = ref('')
    /** 发送中标记，避免重复提交 */
    const sending = ref(false)
    /** 聊天记录容器，用于发送后滚到底部 */
    const chatBodyRef = ref(null)

    // [轮8] 议价总开关（后台交易设置，后端缺省开启；读取失败也按开启处理）
    const tradeNegotiationEnabled = ref(true)
    // [轮8] 当前会话商品摘要（sellerId / price），供「报价」入口归属判断与金额提示使用
    const activeProduct = ref(null)
    // [轮8] 报价倒计时基准时间（每秒跳动，驱动 mm:ss 刷新）
    const now = ref(Date.now())

    let pollTimer = null
    // [轮8] 报价倒计时定时器
    let offerTimer = null

    /** 会话唯一标识：对方用户 + 关联商品 */
    const conversationId = (item) => `${item.peerId}-${item.productId || 0}`

    /** 判断某会话是否为当前打开的会话 */
    const isActive = (item) =>
      active.value !== null &&
      active.value.peerId === item.peerId &&
      (active.value.productId || null) === (item.productId || null)

    const scrollToBottom = async () => {
      await nextTick()
      if (chatBodyRef.value) {
        chatBodyRef.value.scrollTop = chatBodyRef.value.scrollHeight
      }
    }

    /** 把最新未读总数同步给 store，头部角标随之更新 */
    const refreshUnreadCount = async () => {
      try {
        userStore.setUnreadMessageCount(await messageApi.getUnreadCount())
      } catch (error) {
        console.error('获取未读私信数失败:', error)
      }
    }

    /** 加载会话列表 */
    const loadConversations = async () => {
      try {
        const data = await messageApi.getConversations()
        conversations.value = data || []
      } catch (error) {
        console.error('加载会话列表失败:', error)
      }
    }

    /** 加载当前会话的聊天记录（后端会顺带把对方消息标记为已读） */
    const loadChat = async () => {
      if (!active.value) return
      try {
        const data = await messageApi.getChat(active.value.peerId, active.value.productId)
        messages.value = data || []
        await scrollToBottom()
      } catch (error) {
        console.error('加载聊天记录失败:', error)
      }
    }

    /** 打开某个会话 */
    const openConversation = async (item) => {
      active.value = {
        peerId: item.peerId,
        peerName: item.peerName,
        productId: item.productId || null,
        productTitle: item.productTitle
      }
      messages.value = []
      await loadChat()
      // 打开会话即消耗未读，刷新列表让会话上的小角标消失，并同步头部角标
      await loadConversations()
      await refreshUnreadCount()
    }

    /** 按路由 query（peerId / productId）自动打开会话，供商品页跳转过来时使用 */
    const openFromRoute = async () => {
      const peerId = Number(route.query.peerId)
      if (!peerId) return
      const productId = route.query.productId ? Number(route.query.productId) : null
      const matched = conversations.value.find(
        (item) => item.peerId === peerId && (item.productId || null) === productId
      )
      if (matched) {
        await openConversation(matched)
      } else {
        ElMessage.info('该会话暂无消息或已被删除')
      }
    }

    const refreshAll = async () => {
      await loadConversations()
      if (active.value) {
        await loadChat()
      }
      await refreshUnreadCount()
    }

    /** 发送私信 / 咨询（乐观插入：发送成功先上屏，会话列表增量更新，不再整列表重拉） */
    const send = async () => {
      const content = draft.value.trim()
      if (!content) {
        ElMessage.warning('请输入要发送的内容')
        return
      }
      if (!active.value) return

      sending.value = true
      try {
        const payload = { toUserId: active.value.peerId, content }
        // 商品咨询会话带上商品 ID，普通私信不带
        if (active.value.productId) {
          payload.productId = active.value.productId
        }
        const newId = await messageApi.sendPrivateMessage(payload)
        draft.value = ''
        // 乐观插入刚发出的消息（后端返回新消息 ID）
        messages.value.push({
          id: newId || `local-${Date.now()}`,
          fromUserId: userStore.userId,
          content,
          self: true,
          createTime: formatDate(new Date(), 'YYYY-MM-DD HH:mm:ss')
        })
        await scrollToBottom()
        // 会话列表只更新本地预览，不打断滚动位置；未读角标单独刷新
        const conv = conversations.value.find(
          (item) =>
            isActive(item) ||
            (item.peerId === active.value.peerId &&
              (item.productId || null) === (active.value.productId || null))
        )
        if (conv) {
          conv.lastContent = content
          conv.lastTime = new Date().toISOString()
        }
        await refreshUnreadCount()
      } catch (error) {
        // 发送失败由拦截器提示，草稿保留在输入框里
      } finally {
        sending.value = false
      }
    }

    /** 删除整个会话（双方均不再可见） */
    const removeConversation = (item) => {
      ElMessageBox.confirm(
        `确定删除与「${item.peerName}」的会话吗？删除后你们双方的聊天记录都会被清除。`,
        '删除会话',
        { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
      )
        .then(async () => {
          try {
            await messageApi.deleteConversation(item.peerId, item.productId)
            if (isActive(item)) {
              active.value = null
              messages.value = []
            }
            await loadConversations()
            await refreshUnreadCount()
            ElMessage.success('会话已删除')
          } catch (error) {
            console.error('删除会话失败:', error)
          }
        })
        .catch(() => {})
    }

    /** 轮询刷新：页面不可见时不打扰后端 */
    const startPolling = () => {
      pollTimer = setInterval(() => {
        if (document.visibilityState !== 'visible') return
        refreshAll()
      }, POLL_INTERVAL_MS)
    }

    // ==================== [轮8] 议价：报价卡片与卖家报价入口 ====================

    /** 拉取当前会话商品摘要（归属 + 现价），商品删除/无权查看时置空隐藏报价入口 */
    const loadActiveProductMeta = async () => {
      const productId = active.value?.productId
      if (!productId) {
        activeProduct.value = null
        return
      }
      try {
        const data = await getProductDetail(productId)
        // 会话可能已切换，仅当仍是当前会话商品时才写入
        if (active.value?.productId === productId) {
          activeProduct.value = { sellerId: data.sellerId, price: data.price }
        }
      } catch (error) {
        if (active.value?.productId === productId) {
          activeProduct.value = null
        }
      }
    }

    // 会话切换时跟随刷新商品摘要（纯增量监听，不影响既有打开会话逻辑）
    watch(active, loadActiveProductMeta)

    /** 读取交易开关（议价总开关），挂载时调用一次 */
    const loadTradeSettings = async () => {
      try {
        const settings = await getPublicSettings()
        tradeNegotiationEnabled.value = settings?.tradeNegotiationEnabled !== false
      } catch (error) {
        // 读取失败按缺省开启处理，业务校验仍由后端兜底
      }
    }

    /** 是否可发起报价：议价开启 + 会话关联商品 + 商品是我发布的（沿用商品详情页 sellerId 比对口径） */
    const canOffer = computed(
      () =>
        tradeNegotiationEnabled.value &&
        active.value?.productId != null &&
        activeProduct.value != null &&
        userStore.userId != null &&
        Number(activeProduct.value.sellerId) === Number(userStore.userId)
    )

    /** 报价是否已过有效期（后端是「30 分钟内有效」的本地时间字符串） */
    const isOfferExpired = (message) => {
      if (!message.offerExpireTime) return false
      const expireAt = new Date(String(message.offerExpireTime).replace(' ', 'T')).getTime()
      return Number.isFinite(expireAt) && expireAt <= now.value
    }

    /** 待处理报价才显示倒计时；已接受/已失效由徽标表达状态 */
    const showOfferExpire = (message) => message.offerStatus === 0 && !!message.offerExpireTime

    /** 有效期倒计时文案：距今 mm:ss，过期显示「已过期」 */
    const offerExpireText = (message) => {
      if (!message.offerExpireTime) return ''
      const expireAt = new Date(String(message.offerExpireTime).replace(' ', 'T')).getTime()
      const remaining = Math.floor((expireAt - now.value) / 1000)
      if (remaining <= 0) return '已过期'
      const mm = String(Math.floor(remaining / 60)).padStart(2, '0')
      const ss = String(remaining % 60).padStart(2, '0')
      return `剩余有效期 ${mm}:${ss}`
    }

    /** 报价状态徽标文案：0 待处理 / 1 已接受 / 2 已失效 */
    const offerStatusText = (message) =>
      ({ 0: '待处理', 1: '已接受', 2: '已失效' })[message.offerStatus] || '待处理'

    /** 报价状态徽标类型：待处理=primary / 已接受=success / 已失效=info */
    const offerStatusTagType = (message) =>
      ({ 0: 'primary', 1: 'success', 2: 'info' })[message.offerStatus] || 'primary'

    /** 接收方（买家）且待处理且未过期，才展示「接受报价并下单」 */
    const canAcceptOffer = (message) =>
      !message.self && message.offerStatus === 0 && !isOfferExpired(message)

    /** 买家接受报价并按议价价下单，成功后跳订单列表 */
    const acceptOffer = (message) => {
      ElMessageBox.confirm(`确认按 ¥${message.offerPrice} 接受该报价并创建订单吗？`, '接受报价', {
        type: 'info',
        confirmButtonText: '接受并下单',
        cancelButtonText: '再想想'
      })
        .then(async () => {
          // 校验失败（过期/不在售等）由拦截器弹出后端文案，这里只处理成功路径
          await messageApi.acceptOffer(message.id)
          ElMessage.success('下单成功，订单已创建')
          router.push('/orders')
        })
        .catch(() => {})
    }

    /** 卖家发报价：输入金额（前端仅校验正数，「低于现价」等业务校验由后端完成） */
    const openOfferDialog = () => {
      if (!active.value || !activeProduct.value) return
      ElMessageBox.prompt(
        `向买家「${active.value.peerName}」就当前咨询商品发起议价，报价金额须低于现价 ¥${activeProduct.value.price}`,
        '发送议价报价',
        {
          confirmButtonText: '发送报价',
          cancelButtonText: '取消',
          inputPlaceholder: '请输入报价金额',
          inputValidator: (value) => {
            const price = Number(value)
            if (!value || !Number.isFinite(price) || price <= 0) {
              return '请输入大于 0 的金额'
            }
            return true
          }
        }
      )
        .then(async ({ value }) => {
          await messageApi.sendOffer({
            toUserId: active.value.peerId,
            productId: active.value.productId,
            price: Number(value)
          })
          ElMessage.success('报价已发送，等待买家处理')
          // 刷新聊天记录让报价卡片立即可见，会话列表预览同步跟进
          await loadChat()
          await loadConversations()
        })
        .catch(() => {})
    }

    watch(
      () => route.query,
      () => {
        if (route.name === 'Messages') {
          openFromRoute()
        }
      }
    )

    onMounted(async () => {
      await loadConversations()
      await refreshUnreadCount()
      await openFromRoute()
      startPolling()
    })

    // [轮8] 议价配套：读取交易开关 + 启动报价倒计时跳秒（独立钩子，纯增量）
    onMounted(() => {
      loadTradeSettings()
      offerTimer = setInterval(() => {
        now.value = Date.now()
      }, 1000)
    })

    onBeforeUnmount(() => {
      if (pollTimer) {
        clearInterval(pollTimer)
        pollTimer = null
      }
    })

    // [轮8] 卸载时清理报价倒计时定时器（独立钩子，纯增量）
    onBeforeUnmount(() => {
      if (offerTimer) {
        clearInterval(offerTimer)
        offerTimer = null
      }
    })

    return {
      conversations,
      active,
      messages,
      draft,
      sending,
      chatBodyRef,
      conversationId,
      isActive,
      openConversation,
      removeConversation,
      refreshAll,
      send,
      formatDate,
      formatRelativeTime,
      // [轮8] 议价相关绑定
      canOffer,
      openOfferDialog,
      acceptOffer,
      canAcceptOffer,
      offerStatusText,
      offerStatusTagType,
      showOfferExpire,
      offerExpireText
    }
  }
}
</script>

<style scoped>
.message-center {
  max-width: 1100px;
  margin: 0 auto;
}

.mc-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

/* [UI美化] 页面标题对齐文字层级规范 */
.mc-header h2 {
  margin: 0;
  font-size: 20px;
  color: var(--hs-text-primary);
  font-weight: 600;
}

.mc-body {
  display: flex;
  gap: 16px;
  height: 620px;
}

/* [UI美化] 会话面板统一描边、8px 圆角与贴地薄影 */
.conversation-list {
  width: 320px;
  flex-shrink: 0;
  overflow-y: auto;
  border: 1px solid var(--hs-border);
  border-radius: 8px;
  background: var(--hs-bg-card);
  box-shadow: var(--hs-shadow-sm);
}

/* [UI美化] 会话条目分隔线换浅色系，悬浮底色加平滑过渡 */
.conversation-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px;
  cursor: pointer;
  border-bottom: 1px solid var(--hs-border-light);
  position: relative;
  transition: background 0.2s ease;
}

.conversation-item:hover {
  background: var(--hs-bg-panel, #f8fafd);
}

.conversation-item.active {
  background: #ecf5ff;
}

.conv-main {
  flex: 1;
  min-width: 0;
}

.conv-avatar {
  position: relative;
  flex-shrink: 0;
}

.conv-unread {
  position: absolute;
  top: -6px;
  right: -6px;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 999px;
  background: #f56c6c;
  color: #fff;
  font-size: 12px;
  line-height: 18px;
  text-align: center;
}

.conv-top {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
}

.conv-name {
  font-weight: 600;
  color: var(--hs-text-primary, #303133);
}

.conv-time {
  font-size: 12px;
  color: var(--hs-text-secondary, #909399);
}

.conv-product {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 4px;
}

.conv-product-title {
  font-size: 12px;
  color: #e6a23c;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conv-last {
  margin-top: 4px;
  font-size: 13px;
  color: var(--hs-text-secondary, #909399);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* [UI美化] 删除按钮淡入更顺滑 */
.conv-delete {
  opacity: 0;
  transition: opacity 0.2s ease;
}

.conversation-item:hover .conv-delete {
  opacity: 1;
}

/* [UI美化] 聊天面板与会话面板观感对齐 */
.chat-pane {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  border: 1px solid var(--hs-border);
  border-radius: 8px;
  background: var(--hs-bg-card);
  box-shadow: var(--hs-shadow-sm);
}

.chat-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border-bottom: 1px solid var(--hs-border-light);
}

.chat-title {
  font-weight: 600;
  font-size: 16px;
}

.chat-product {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}

.chat-product-link {
  color: #409eff;
  text-decoration: none;
}

.chat-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: var(--hs-bg-panel, #f8fafd);
}

.chat-row {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  margin-bottom: 12px;
}

.chat-row.self {
  align-items: flex-end;
}

/* [UI美化] 气泡加方向性小圆角，贴近聊天形态 */
.bubble {
  max-width: 70%;
  padding: 8px 12px;
  border-radius: 8px;
  border-bottom-left-radius: 2px;
  background: var(--hs-bg-card, #fff);
  border: 1px solid var(--hs-border, #ebeef5);
  color: var(--hs-text-primary, #303133);
  line-height: 1.5;
  word-break: break-word;
  white-space: pre-wrap;
}

.chat-row.self .bubble {
  background: #409eff;
  border-color: #409eff;
  color: #fff;
  border-bottom-right-radius: 2px;
}

/* [轮8] 报价卡片：嵌在聊天气泡位的卡片形态，10px 圆角、Token 配色（暗色自动适配） */
.bubble-offer {
  min-width: 230px;
  max-width: 70%;
  padding: 12px 14px;
  border-radius: 10px;
  background: var(--hs-bg-card);
  border: 1px solid var(--hs-border);
}

/* [轮8] 自己发的报价卡同样使用卡片底色，避免被气泡蓝底吞掉 */
.chat-row.self .bubble-offer {
  background: var(--hs-bg-card);
  border-color: var(--hs-border);
  color: var(--hs-text-primary);
}

.offer-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.offer-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--hs-text-secondary);
}

/* [轮8] 报价金额用价格专用色放大展示 */
.offer-price {
  margin-top: 6px;
  font-size: 24px;
  font-weight: 700;
  color: var(--hs-price);
  font-variant-numeric: tabular-nums;
}

.offer-content {
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.5;
  color: var(--hs-text-regular);
  word-break: break-word;
}

/* [轮8] 倒计时数字等宽，避免跳动 */
.offer-expire {
  margin-top: 6px;
  font-size: 12px;
  color: var(--hs-text-secondary);
  font-variant-numeric: tabular-nums;
}

.offer-accept {
  margin-top: 8px;
  width: 100%;
}

.bubble-time {
  margin-top: 4px;
  font-size: 12px;
  color: var(--hs-text-placeholder, #c0c4cc);
}

.chat-input {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  padding: 12px 16px;
  border-top: 1px solid var(--hs-border-light);
}

.chat-input .el-textarea {
  flex: 1;
}

/* 移动端：双栏改上下堆叠，聊天窗自适应高度（此前固定 320px/620px 小屏挤坏） */
@media (max-width: 768px) {
  .message-center {
    padding: 12px;
  }

  .mc-body {
    flex-direction: column;
    height: auto;
  }

  .conversation-list {
    width: 100%;
    max-height: 240px;
  }

  .chat-pane {
    height: 62vh;
  }
}
</style>
