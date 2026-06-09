<template>
  <div class="notification-list-page page-container">
    <div class="page-navbar">
      <button class="page-nav-back" @click="router.back()">
        <i class="fas fa-chevron-left"></i>
      </button>
      <div class="page-nav-title">
        <span>{{ pageTitle }}</span>
      </div>
      <div class="page-nav-right">
        <button class="mark-all-btn" @click="markAllAsRead">
          全部已读
        </button>
      </div>
    </div>

    <div v-show="loading" class="skeleton-list">
      <div v-for="i in 5" :key="i" class="skeleton-card">
        <div class="skeleton-line medium"></div>
        <div class="skeleton-line short" style="margin-top:8px;"></div>
        <div class="skeleton-line short" style="margin-top:8px;"></div>
      </div>
    </div>

    <div v-show="!loading" class="notification-content" ref="listRef" @scroll="onScroll">
      <div v-if="loadingMore" class="loading-more-indicator">
        <div class="loading-spinner"></div>
        <span>加载中...</span>
      </div>

      <div v-if="notifications.length === 0" class="empty-state">
        <i class="fas fa-inbox"></i>
        <p>暂无通知</p>
      </div>

      <div v-else class="notification-list">
        <div
          v-for="item in notifications"
          :key="item.id"
          class="notification-item"
          :class="{ 'unread': !(item.read || item.isRead) }"
          @click="handleItemClick(item)"
        >
          <div class="item-icon" :style="{ background: getIconBg(item.type) }">
            <i :class="['fas', getIconClass(item.type)]"></i>
          </div>
          <div class="item-content">
            <div class="item-title">{{ item.title }}</div>
            <div class="item-desc">{{ item.content }}</div>
            <div class="item-time">{{ formatTime(item.createdAt) }}</div>
          </div>
          <div class="item-right">
            <span v-if="!(item.read || item.isRead)" class="unread-dot"></span>
            <i class="fas fa-chevron-right"></i>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { authAPI } from '@/api/authAPI'
import Message from '@/utils/message'

const router = useRouter()
const route = useRoute()

const loading = ref(true)
const loadingMore = ref(false)
const page = ref(1)
const pageSize = ref(20)
const hasMore = ref(true)
const notifications = ref<any[]>([])
const listRef = ref<HTMLElement | null>(null)

const notificationType = computed(() => route.query.type as string || 'ALL')
const isSeller = computed(() => route.path.startsWith('/seller'))

const pageTitle = computed(() => {
  const typeMap: Record<string, string> = {
    ORDER: '订单通知',
    SYSTEM: '系统通知',
    ALL: '全部通知'
  }
  return typeMap[notificationType.value] || '通知列表'
})

const getIconClass = (type: string) => {
  const iconMap: Record<string, string> = {
    ORDER: 'fa-receipt',
    SYSTEM: 'fa-bell'
  }
  return iconMap[type] || 'fa-bell'
}

const getIconBg = (type: string) => {
  const bgMap: Record<string, string> = {
    ORDER: '#f59e0b',
    SYSTEM: '#8b5cf6'
  }
  return bgMap[type] || '#64748b'
}

const loadNotifications = async (isLoadMore = false) => {
  if (!isLoadMore) {
    loading.value = true
    page.value = 1
    hasMore.value = true
  } else {
    loadingMore.value = true
  }

  try {
    const api = isSeller.value ? authAPI.getSellerNotifications : authAPI.getNotifications
    const res = await api({
      page: page.value,
      pageSize: pageSize.value,
      type: notificationType.value
    })

    if (res.success && res.data) {
        const data = res.data
        const list = data.list || data.records || data.content || data || []
        const newList = Array.isArray(list) ? list : []
        const reversed = [...newList].reverse()

        if (isLoadMore) {
          notifications.value = [...reversed, ...notifications.value]
        } else {
          notifications.value = reversed
        }

        hasMore.value = newList.length >= pageSize.value
      }
  } catch {
    Message.error('加载通知失败')
  } finally {
    loading.value = false
    loadingMore.value = false

    if (!isLoadMore && notifications.value.length > 0) {
      await nextTick()
      scrollToBottom()
    }
  }
}

const loadMore = async () => {
  const el = listRef.value
  const prevScrollHeight = el?.scrollHeight || 0

  page.value++
  await loadNotifications(true)

  await nextTick()
  if (el) {
    el.scrollTop = el.scrollHeight - prevScrollHeight
  }
}

const scrollToBottom = () => {
  if (listRef.value) {
    listRef.value.scrollTop = listRef.value.scrollHeight
  }
}

const onScroll = () => {
  if (!listRef.value) return
  const { scrollTop, scrollHeight, clientHeight } = listRef.value

  if (scrollTop <= 50 && hasMore.value && !loadingMore.value && !loading.value) {
    loadMore()
  }
}

const markAllAsRead = async () => {
  try {
    const api = isSeller.value ? authAPI.markAllSellerNotificationsAsRead : authAPI.markAllNotificationsAsRead
    const res = await api()
    if (res.success) {
      notifications.value.forEach(item => item.read = true)
      Message.success('已全部标记为已读')
      if (isSeller.value) {
        const unreadCount = notifications.value.filter(n => n.read === false || n.isRead === false).length
        window.dispatchEvent(new CustomEvent('notification-read', { detail: { count: unreadCount } }))
      }
    }
  } catch {
    Message.error('操作失败')
  }
}

const handleItemClick = async (item: any) => {
  const isReadField = item.read !== undefined ? 'read' : 'isRead'
  if (!item[isReadField]) {
    try {
      const res = await authAPI.markNotificationAsRead(item.id)
      if (res.success) {
        const index = notifications.value.findIndex(n => n.id === item.id)
        if (index > -1) {
          notifications.value[index].read = true
          notifications.value[index].isRead = true
        }
        if (isSeller.value) {
          window.dispatchEvent(new CustomEvent('notification-read', { detail: { count: 1 } }))
        }
      }
    } catch {
    }
  }

  const extraData = typeof item.extraData === 'string' ? JSON.parse(item.extraData || '{}') : item.extraData || {}
  const orderId = extraData.orderId || extraData.order_id

  switch (item.type) {
    case 'ORDER':
      if (!orderId) {
        Message.warning('订单信息不完整')
        return
      }
      router.push({
        name: isSeller.value ? 'SellerOrderDetail' : 'OrderDetail',
        params: isSeller.value ? { id: orderId } : { orderId }
      })
      break
    case 'SYSTEM':
      if (orderId) {
        router.push({
          name: isSeller.value ? 'SellerOrderDetail' : 'OrderDetail',
          params: isSeller.value ? { id: orderId } : { orderId }
        })
      }
      break
    default:
      break
    }
  }








const formatTime = (dateStr: string) => {
  if (!dateStr) return ''



  const date = new Date(dateStr)
  if (isNaN(date.getTime())) return ''
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))

  if (days === 0) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  } else if (days === 1) {
    return '昨天'
  } else if (days < 7) {
    return days + '天前'
  } else {
    return date.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
  }
}

onMounted(() => {
  document.body.style.overflow = 'hidden'
  loadNotifications()
})

onUnmounted(() => {
  document.body.style.overflow = ''
})
</script>

<style scoped>
@import url('https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css');

.page-container {
  min-height: 100vh;
  background: #f5f7fa;
  padding-top: 60px;
}

.page-navbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  padding-top: calc(12px + env(safe-area-inset-top));
  background: white;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  border-bottom: 1px solid #e8ecf0;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.page-nav-back {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  color: #4a6491;
  font-size: 16px;
}

.page-nav-title {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 17px;
  font-weight: 700;
  color: #1e293b;
  white-space: nowrap;
}

.page-nav-right {
  position: relative;
}

.mark-all-btn {
  font-size: 13px;
  color: #4a6491;
  border: none;
  background: #f5f7fa;
  padding: 6px 12px;
  border-radius: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.mark-all-btn:active {
  background: #e8ecf0;
  transform: scale(0.96);
}

.skeleton-list {
  padding: 16px;
}

.skeleton-card {
  padding: 16px;
  background: white;
  border-radius: 12px;
  margin-bottom: 12px;
}

.skeleton-line {
  height: 16px;
  background: #e5e7eb;
  border-radius: 4px;
  animation: skeleton-loading 1.5s ease-in-out infinite;
}

.skeleton-line.medium {
  width: 80%;
}

.skeleton-line.short {
  width: 50%;
}

@keyframes skeleton-loading {
  0%, 100% { opacity: 0.5; }
  50% { opacity: 1; }
}

.notification-content {
  padding: 0 0 20px;
  height: calc(100vh - 56px);
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}

.loading-more-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 12px;
  color: #94a3b8;
  font-size: 14px;
  gap: 8px;
}

.loading-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid #e2e8f0;
  border-top-color: #4a6491;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.empty-state {
  text-align: center;
  padding: 80px 20px;
  color: #94a3b8;
}

.empty-state i {
  font-size: 48px;
  margin-bottom: 12px;
  display: block;
}

.notification-list {
}

.notification-item {
  display: flex;
  align-items: center;
  padding: 14px 16px;
  background: white;
  margin-bottom: 8px;
  border-radius: 8px;
  transition: all 0.2s;
}

.notification-item:first-child {
  margin-top: 0;
}

.notification-item:active {
  background: #f8fafc;
}

.notification-item.unread {
  background: #fffbeb;
}

.item-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-right: 12px;
}

.item-icon i {
  font-size: 20px;
  color: white;
}

.item-content {
  flex: 1;
  min-width: 0;
}

.item-title {
  font-size: 15px;
  font-weight: 500;
  color: #1e293b;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-desc {
  font-size: 13px;
  color: #64748b;
  line-height: 1.5;
  margin-bottom: 4px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.item-time {
  font-size: 12px;
  color: #94a3b8;
}

.item-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.item-right i {
  font-size: 14px;
  color: #cbd5e1;
}

.unread-dot {
  width: 8px;
  height: 8px;
  background: #ef4444;
  border-radius: 50%;
}
</style>
