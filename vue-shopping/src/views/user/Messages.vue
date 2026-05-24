<template>
  <div class="messages-page page-container">
    <!-- 顶部导航栏 -->
    <div class="page-navbar">
      <div class="page-nav-right"></div>
      <div class="page-nav-title">
        <i class="fas fa-bell"></i>
        <span>消息中心</span>
      </div>
      <button class="page-nav-action" @click="markAllRead">
        <i class="fas fa-check-circle"></i>
      </button>
    </div>

    <!-- 骨架屏加载 -->
    <div v-if="loading" class="message-list skeleton-order-list">
      <div v-for="n in 3" :key="n" class="message-item skeleton-card">
        <div class="msg-avatar">
          <div class="skeleton" style="width: 48px; height: 48px; border-radius: 50%;"></div>
        </div>
        <div class="msg-content">
          <div class="msg-header">
            <div class="skeleton-line medium" style="width: 120px;"></div>
            <div class="skeleton-line short" style="width: 60px;"></div>
          </div>
          <div class="skeleton-line" style="margin-top: 8px;"></div>
        </div>
      </div>
    </div>

    <div v-else>
      <!-- 未读提醒 -->
      <div class="unread-reminder" v-if="unreadCount > 0" @click="markAllRead">
        <i class="fas fa-check-circle"></i>
        <span>全部已读 ({{ unreadCount }})</span>
        <i class="fas fa-chevron-right"></i>
      </div>

      <!-- 消息列表 -->
      <div class="message-list">
        <div
          v-for="msg in messages"
          :key="msg.id"
          class="message-item"
          :class="{ unread: !msg.read }"
          @click="goToDetail(msg)"
        >
          <div class="msg-avatar">
            <i :class="['fas', msg.icon]"></i>
          </div>
          <div class="msg-content">
            <div class="msg-header">
              <span class="msg-title">{{ msg.title }}</span>
              <span class="msg-time">{{ msg.time }}</span>
            </div>
            <div class="msg-preview">{{ msg.preview }}</div>
            <div v-if="!msg.read" class="unread-dot"></div>
          </div>
        </div>

        <!-- 空状态 -->
        <div v-if="messages.length === 0" class="empty-messages">
          <i class="fas fa-inbox"></i>
          <p>暂无消息</p>
        </div>
      </div>
    </div>

    <!-- 系统通知设置入口 -->
    <div class="settings-entry">
      <div class="settings-card" @click="goToNotificationSettings">
        <div class="settings-icon">
          <i class="fas fa-cog"></i>
        </div>
        <span class="settings-text">通知设置</span>
        <i class="fas fa-chevron-right"></i>
      </div>
    </div>

    <!-- 底部留空 -->
    <div class="bottom-space"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { authAPI } from '@/api/authAPI'
import Message from '@/utils/message'

const router = useRouter()

const loading = ref(true)
const messages = ref<any[]>([])

const unreadCount = computed(() => {
  return messages.value.filter(m => !m.read).length
})

// const loadMessages = async () => {
//   try {
//     const response = await authAPI.getUserMessages()
//     if (response.success && response.data) {
//       messages.value = response.data.map((item: any) => ({
//         id: item.id,
//         type: item.type,
//         icon: getIconByType(item.type),
//         title: item.title,
//         preview: item.content?.substring(0, 50) || '查看详情',
//         time: formatTime(item.createdAt),
//         read: item.read,
//         orderId: item.orderId,
//         refundId: item.refundId
//       }))
//     } else {
//       throw new Error(response.message || '加载失败')
//     }
//   } catch (error: any) {
//     Message.error(error.message || '加载失败')
//   }
// }

const getIconByType = (type: string) => {
  const icons: Record<string, string> = {
    ORDER: 'fa-shopping-cart',
    REFUND: 'fa-undo',
    SYSTEM: 'fa-bell',
    COMMENT: 'fa-comment',
    COUPON: 'fa-ticket-alt',
    SHOP: 'fa-store'
  }
  return icons[type] || 'fa-bell'
}

const formatTime = (dateStr: string) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minute = 60 * 1000
  const hour = 60 * minute
  const day = 24 * hour
  if (diff < minute) return '刚刚'
  if (diff < hour) return Math.floor(diff / minute) + '分钟前'
  if (diff < day) return Math.floor(diff / hour) + '小时前'
  if (diff < 2 * day) return '昨天'
  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

const markAllRead = () => {
  // 实现标记全部已读的逻辑
  Message.info('标记全部已读')
}

// const markAllRead = async () => {
//   try {
//     const response = await authAPI.markAllMessagesRead()
//     if (response.success) {
//       messages.value.forEach(m => m.read = true)
//       Message.success('已全部标记为已读')
//     } else {
//       throw new Error(response.message || '操作失败')
//     }
//   } catch (error: any) {
//     Message.error(error.message || '操作失败')
//   }
// }

const goToDetail = (msg: any) => {
  msg.read = true
  if (msg.orderId) {
    router.push({ name: 'OrderDetail', params: { orderId: msg.orderId } })
  } else if (msg.refundId) {
    router.push({ name: 'RefundChat', params: { refundId: msg.refundId } })
  }
}

const goToNotificationSettings = () => {
  Message.info('通知设置开发中')
}

onMounted(() => {
  loading.value = false
})
</script>

<style scoped>
@import url('@/static/css/user/消息中心.css');
</style>
