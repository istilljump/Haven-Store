<template>
  <div class="notifications-page">
    <div class="page-container">
      <div class="page-head">
        <h2>通知中心</h2>
        <div class="head-actions">
          <el-button size="small" :disabled="!unreadCount" @click="markAllRead">全部已读</el-button>
          <el-button size="small" type="danger" plain :disabled="!messages.length" @click="clearCurrent">删除选中</el-button>
        </div>
      </div>

      <el-tabs v-model="activeTab" @tab-change="loadMessages">
        <el-tab-pane label="全部" name="all" />
        <el-tab-pane name="unread">
          <template #label>
            未读
            <el-badge v-if="unreadCount" :value="unreadCount" class="tab-badge" />
          </template>
        </el-tab-pane>
      </el-tabs>

      <div v-if="loading" class="msg-list" v-loading="loading" style="min-height: 200px" />
      <el-empty v-else-if="!messages.length" description="暂无消息" />
      <div v-else class="msg-list">
        <div
          v-for="msg in messages"
          :key="msg.id"
          class="msg-item"
          :class="{ unread: !msg.isRead, checked: selectedId === msg.id }"
          @click="openMessage(msg)"
        >
          <div class="msg-icon">
            <el-icon :size="22">
              <Bell v-if="Number(msg.messageType) === 1" />
              <Promotion v-else-if="Number(msg.messageType) === 2" />
              <Van v-else-if="Number(msg.messageType) === 3" />
              <ChatDotRound v-else />
            </el-icon>
          </div>
          <div class="msg-body">
            <div class="msg-title-row">
              <span class="msg-title">{{ msg.title }}</span>
              <el-tag v-if="Number(msg.messageType) === 2" size="mini" type="warning">公告</el-tag>
              <span v-if="!msg.isRead" class="unread-dot" />
            </div>
            <div class="msg-content">{{ msg.content }}</div>
            <div class="msg-time">{{ formatTime(msg.createTime) }}</div>
          </div>
          <el-button
            class="msg-delete"
            link
            type="danger"
            icon="Delete"
            @click.stop="removeMessage(msg)"
          />
        </div>
      </div>

      <div class="pagination-row" v-if="total > query.size">
        <el-pagination
          v-model:current-page="query.page"
          :page-size="query.size"
          :total="total"
          layout="prev, pager, next"
          @current-change="loadMessages"
        />
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Bell, Promotion, Van, ChatDotRound } from '@element-plus/icons-vue'
import {
  getSystemMessages,
  markSystemMessageRead,
  markAllSystemMessagesRead,
  deleteSystemMessage,
  getSystemUnreadCount
} from '@/api/message'
import { useUserStore } from '@/store'
import { formatRelativeTime } from '@/utils/format'

/**
 * 通知中心（系统消息收件箱）
 * <p>
 * 此前管理后台可以群发公告，前台用户却没有任何查看入口；
 * 本页对接 /message/list、/message/{id}/read、/message/all/read、/message/unread/count，
 * 与顶栏铃铛角标共用 store 里的 systemUnreadCount 状态。
 */
export default {
  name: 'Notifications',
  components: { Bell, Promotion, Van, ChatDotRound },
  setup() {
    const userStore = useUserStore()
    const activeTab = ref('all')
    const messages = ref([])
    const total = ref(0)
    const loading = ref(false)
    const query = reactive({ page: 1, size: 10 })
    const selectedId = ref(null)

    const unreadCount = computed(() => userStore.systemUnreadCount)

    const formatTime = (time) => formatRelativeTime(time)

    const loadMessages = async () => {
      loading.value = true
      try {
        const params = { page: query.page, size: query.size }
        if (activeTab.value === 'unread') {
          params.size = 100
        }
        const pageData = await getSystemMessages(params)
        let records = pageData?.records || []
        if (activeTab.value === 'unread') {
          records = records.filter(m => !m.isRead)
          total.value = records.length
        } else {
          total.value = Number(pageData?.total) || 0
        }
        messages.value = records
        await refreshBadge()
      } finally {
        loading.value = false
      }
    }

    const refreshBadge = async () => {
      try {
        const count = await getSystemUnreadCount()
        userStore.setSystemUnreadCount(count)
      } catch (err) {
        // 角标刷新失败不影响列表展示
      }
    }

    const openMessage = async (msg) => {
      selectedId.value = msg.id
      if (!msg.isRead) {
        try {
          await markSystemMessageRead(msg.id)
          msg.isRead = 1
          await refreshBadge()
        } catch (err) {
          // 标记失败不阻断阅读
        }
      }
    }

    const markAllRead = async () => {
      try {
        await markAllSystemMessagesRead()
        ElMessage.success('已全部标记为已读')
        await loadMessages()
      } catch (err) {
        // 拦截器已提示
      }
    }

    const removeMessage = (msg) => {
      ElMessageBox.confirm('确定删除这条消息吗？', '提示', { type: 'warning' })
        .then(async () => {
          await deleteSystemMessage(msg.id)
          ElMessage.success('已删除')
          await loadMessages()
        })
        .catch(() => {})
    }

    const clearCurrent = () => {
      if (!selectedId.value) {
        ElMessage.info('请先点击选中一条消息')
        return
      }
      const msg = messages.value.find(m => m.id === selectedId.value)
      if (msg) removeMessage(msg)
    }

    onMounted(loadMessages)

    return {
      activeTab,
      messages,
      total,
      loading,
      query,
      selectedId,
      unreadCount,
      formatTime,
      loadMessages,
      openMessage,
      markAllRead,
      removeMessage,
      clearCurrent
    }
  }
}
</script>

<style scoped>
.notifications-page {
  background: #f5f7fa;
  min-height: 100%;
  padding: 20px;
}

.page-container {
  max-width: 860px;
  margin: 0 auto;
  background: #fff;
  border-radius: 10px;
  padding: 20px 24px;
}

.page-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.page-head h2 {
  margin: 0;
  font-size: 20px;
  color: #303133;
}

.tab-badge {
  margin-left: 6px;
}

.msg-list {
  margin-top: 8px;
}

.msg-item {
  display: flex;
  gap: 12px;
  padding: 14px 12px;
  border-bottom: 1px solid #f0f2f5;
  cursor: pointer;
  border-radius: 6px;
  transition: background 0.15s;
}

.msg-item:hover,
.msg-item.checked {
  background: #f5f7fa;
}

.msg-item.unread .msg-title {
  font-weight: 600;
}

.msg-icon {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #ecf5ff;
  color: #409eff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.msg-body {
  flex: 1;
  min-width: 0;
}

.msg-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.msg-title {
  font-size: 15px;
  color: #303133;
}

.unread-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #f56c6c;
}

.msg-content {
  margin-top: 4px;
  font-size: 13px;
  color: #606266;
  line-height: 1.5;
  word-break: break-word;
}

.msg-time {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}

.msg-delete {
  flex-shrink: 0;
  align-self: flex-start;
  margin-top: 8px;
}

.pagination-row {
  display: flex;
  justify-content: center;
  padding: 16px 0 4px;
}
</style>
