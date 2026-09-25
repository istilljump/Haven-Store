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
              <router-link
                class="chat-product-link"
                :to="`/products/${active.productId}`"
              >
                {{ active.productTitle || '商品已删除' }}
              </router-link>
            </div>
          </div>

          <div ref="chatBodyRef" class="chat-body">
            <el-empty v-if="!messages.length" description="还没有消息，打个招呼吧" :image-size="70" />
            <div
              v-for="message in messages"
              :key="message.id"
              class="chat-row"
              :class="{ self: message.self }"
            >
              <div class="bubble">{{ message.content }}</div>
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
            <el-button type="primary" :loading="sending" @click="send">发送</el-button>
          </div>
        </template>
      </section>
    </div>
  </div>
</template>

<script>
import { ref, nextTick, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Refresh } from '@element-plus/icons-vue'
import messageApi from '@/api/message'
import { useUserStore } from '@/store/index'
import { formatDate, formatRelativeTime } from '@/utils/format'

/** 会话自动刷新间隔：聊天页停留时用轮询拉新消息，避免为此引入 WebSocket */
const POLL_INTERVAL_MS = 10000

export default {
  name: 'MessageCenter',
  components: {
    Delete,
    Refresh
  },
  setup() {
    const route = useRoute()
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

    let pollTimer = null

    /** 会话唯一标识：对方用户 + 关联商品 */
    const conversationId = (item) => `${item.peerId}-${item.productId || 0}`

    /** 判断某会话是否为当前打开的会话 */
    const isActive = (item) => active.value !== null
      && active.value.peerId === item.peerId
      && (active.value.productId || null) === (item.productId || null)

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

    /** 发送私信 / 咨询 */
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
        await messageApi.sendPrivateMessage(payload)
        draft.value = ''
        await loadChat()
        await loadConversations()
      } catch (error) {
        console.error('发送私信失败:', error)
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
      ).then(async () => {
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
      }).catch(() => {})
    }

    /** 轮询刷新：页面不可见时不打扰后端 */
    const startPolling = () => {
      pollTimer = setInterval(() => {
        if (document.visibilityState !== 'visible') return
        refreshAll()
      }, POLL_INTERVAL_MS)
    }

    watch(() => route.query, () => {
      if (route.name === 'Messages') {
        openFromRoute()
      }
    })

    onMounted(async () => {
      await loadConversations()
      await refreshUnreadCount()
      await openFromRoute()
      startPolling()
    })

    onBeforeUnmount(() => {
      if (pollTimer) {
        clearInterval(pollTimer)
        pollTimer = null
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
      formatRelativeTime
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

.mc-header h2 {
  margin: 0;
  font-size: 20px;
}

.mc-body {
  display: flex;
  gap: 16px;
  height: 620px;
}

.conversation-list {
  width: 320px;
  flex-shrink: 0;
  overflow-y: auto;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  background: #fff;
}

.conversation-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px;
  cursor: pointer;
  border-bottom: 1px solid #f2f3f5;
  position: relative;
}

.conversation-item:hover {
  background: #f7f8fa;
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
  border-radius: 9px;
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
  color: #303133;
}

.conv-time {
  font-size: 12px;
  color: #909399;
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
  color: #909399;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conv-delete {
  opacity: 0;
}

.conversation-item:hover .conv-delete {
  opacity: 1;
}

.chat-pane {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  background: #fff;
}

.chat-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border-bottom: 1px solid #f2f3f5;
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
  background: #fafafa;
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

.bubble {
  max-width: 70%;
  padding: 8px 12px;
  border-radius: 8px;
  background: #fff;
  border: 1px solid #ebeef5;
  color: #303133;
  line-height: 1.5;
  word-break: break-word;
  white-space: pre-wrap;
}

.chat-row.self .bubble {
  background: #409eff;
  border-color: #409eff;
  color: #fff;
}

.bubble-time {
  margin-top: 4px;
  font-size: 12px;
  color: #c0c4cc;
}

.chat-input {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  padding: 12px 16px;
  border-top: 1px solid #f2f3f5;
}

.chat-input .el-textarea {
  flex: 1;
}
</style>
