<template>
  <div class="messages-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-title">消息中心</h1>
      <button class="clear-btn" @click="clearAllMessages">清空</button>
    </div>

    <!-- 消息列表 -->
    <div class="messages-container" v-if="messages.length > 0">
      <div 
        v-for="message in messages" 
        :key="message.id" 
        class="message-item"
        :class="{ unread: !message.read }"
        @click="viewMessage(message)"
      >
        <div class="message-avatar">
          <i :class="message.icon"></i>
        </div>
        <div class="message-content">
          <div class="message-header">
            <span class="message-title">{{ message.title }}</span>
            <span class="message-time">{{ message.time }}</span>
          </div>
          <p class="message-text">{{ message.content }}</p>
        </div>
        <span v-if="!message.read" class="unread-dot"></span>
      </div>
    </div>

    <!-- 空状态 -->
    <div class="empty-state" v-else>
      <div class="empty-icon">
        <i class="fas fa-bell"></i>
      </div>
      <p class="empty-text">暂无消息</p>
      <p class="empty-hint">有新消息时会在这里显示</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { authAPI } from '@/api/authAPI'
import Message from '@/utils/message'

const router = useRouter()

interface MessageItem {
  id: number
  title: string
  content: string
  time: string
  read: boolean
  icon: string
  type: string
}

const messages = ref<MessageItem[]>([
  {
    id: 1,
    title: '订单发货通知',
    content: '您的订单 #20260521001 已发货，快递单号：SF1234567890',
    time: '10分钟前',
    read: false,
    icon: 'fas fa-truck',
    type: 'order'
  },
  {
    id: 2,
    title: '优惠券到账',
    content: '恭喜您获得满100减20优惠券，有效期至2026年6月21日',
    time: '1小时前',
    read: false,
    icon: 'fas fa-gift',
    type: 'coupon'
  },
  {
    id: 3,
    title: '商品降价提醒',
    content: '您关注的商品「Apple iPhone 15」已降价500元，快去看看吧',
    time: '3小时前',
    read: true,
    icon: 'fas fa-tag',
    type: 'product'
  },
  {
    id: 4,
    title: '系统公告',
    content: '商城将于5月22日0:00-6:00进行系统维护，期间暂停服务',
    time: '昨天',
    read: true,
    icon: 'fas fa-info-circle',
    type: 'system'
  },
  {
    id: 5,
    title: '评价奖励',
    content: '感谢您对商品的评价，已获得10积分奖励',
    time: '2天前',
    read: true,
    icon: 'fas fa-star',
    type: 'review'
  }
])

const loadMessages = async () => {
  try {
    const response = await authAPI.getMessages()
    if (response.success && response.data?.messages) {
      messages.value = response.data.messages
    }
  } catch (error) {
    Message.error('加载消息失败')
}

const viewMessage = (message: MessageItem) => {
  message.read = true
  
  switch (message.type) {
    case 'order':
      router.push({ name: 'UserOrders' })
      break
    case 'coupon':
      router.push({ name: 'UserCoupons' })
      break
    case 'product':
      router.push({ name: 'UserDashboard' })
      break
    default:
      Message.info('暂无更多详情')
  }
}

const clearAllMessages = async () => {
  try {
    await Message.confirm('确定要清空所有消息吗？', '清空确认')
    
    try {
      const response = await authAPI.clearMessages()
      if (response.success) {
        messages.value = []
        Message.success('清空成功')
      }
    } catch (error) {
      Message.error('清空失败')
    }
  } catch (error) {
  }
}

onMounted(async () => {
  await loadMessages()
})
</script>

<style scoped>
.messages-page {
  min-height: 100vh;
  background-color: #f5f7fa;
}

/* 页面头部 */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  background: white;
  border-bottom: 1px solid #f0f0f0;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.clear-btn {
  padding: 6px 14px;
  background: transparent;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  font-size: 13px;
  color: #64748b;
  cursor: pointer;
}

.clear-btn:active {
  background: #f8fafc;
}

/* 消息列表 */
.messages-container {
  padding: 12px;
}

.message-item {
  display: flex;
  align-items: center;
  padding: 14px;
  background: white;
  border-radius: 12px;
  margin-bottom: 10px;
  position: relative;
}

.message-item:active {
  background: #f8fafc;
}

.message-item.unread {
  background: #fef3c7;
}

.message-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: linear-gradient(135deg, #4a6491, #3a5479);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
  flex-shrink: 0;
}

.message-avatar i {
  font-size: 18px;
  color: white;
}

.message-content {
  flex: 1;
  min-width: 0;
}

.message-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}

.message-title {
  font-size: 15px;
  font-weight: 600;
  color: #333;
}

.message-time {
  font-size: 12px;
  color: #94a3b8;
  flex-shrink: 0;
  margin-left: 8px;
}

.message-text {
  font-size: 13px;
  color: #64748b;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.unread-dot {
  position: absolute;
  top: 16px;
  right: 16px;
  width: 8px;
  height: 8px;
  background: #ff4757;
  border-radius: 50%;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
}

.empty-icon {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: #f1f5f9;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
}

.empty-icon i {
  font-size: 36px;
  color: #94a3b8;
}

.empty-text {
  font-size: 16px;
  color: #64748b;
  margin: 0 0 8px 0;
}

.empty-hint {
  font-size: 13px;
  color: #94a3b8;
  margin: 0;
}
</style>
