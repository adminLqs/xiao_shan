<template>
  <div class="chat-management">
    <div class="page-header">
      <h2>客服消息</h2>
      <button class="read-all-btn" @click="markAllRead" :disabled="loadingAll">
        {{ loadingAll ? '处理中...' : '全部已读' }}
      </button>
    </div>

    <div class="message-list" v-if="!loading">
      <!-- 用户列表 -->
      <div 
        v-for="group in userList" 
        :key="group.userId" 
        class="user-group"
        @click="openChatDetail(group.userId)"
      >
        <div class="user-header">
          <span class="user-icon">👤</span>
          <span class="user-id">用户 ID: {{ group.userId }}</span>
          <span class="message-count">{{ group.totalMessages }}条消息</span>
          <span v-if="group.unreadCount > 0" class="unread-badge">{{ group.unreadCount }}条未读</span>
          <span class="arrow">›</span>
        </div>
        
        <!-- 最新消息预览 -->
        <div class="latest-message" v-if="group.latestMessage">
          <span class="latest-content">{{ group.latestMessage.content }}</span>
          <span class="latest-time">{{ formatTime(group.latestMessage.createdAt) }}</span>
        </div>
      </div>
      
      <div v-if="userList.length === 0" class="empty-state">
        <div class="empty-icon">💬</div>
        <p>暂无客服消息</p>
      </div>
    </div>

    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
      <span>加载中...</span>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { ref, computed, onMounted, onUnmounted } from 'vue'
  import { useRouter } from 'vue-router'
  import Message from '@/utils/message'
  import { authAPI } from '@/api/authAPI'

  const router = useRouter()

  // 消息接口
  interface ChatMessage {
    id: number
    userId: number
    content: string
    isRead: boolean
    createdAt: string
  }

  // 用户分组接口
  interface UserGroup {
    userId: number
    totalMessages: number
    unreadCount: number
    latestMessage: ChatMessage | null
    allMessages: ChatMessage[]
  }

  const loading = ref(false)
  const loadingAll = ref(false)
  const rawMessages = ref<ChatMessage[]>([])

  // 处理消息分组
  const userList = computed<UserGroup[]>(() => {
    // 确保 rawMessages.value 是数组
    if (!rawMessages.value || !Array.isArray(rawMessages.value)) {
      return []
    }
    
    const groups: Map<number, UserGroup> = new Map()
    
    for (const msg of rawMessages.value) {
      if (!msg || !msg.userId) continue
      
      if (!groups.has(msg.userId)) {
        groups.set(msg.userId, {
          userId: msg.userId,
          totalMessages: 0,
          unreadCount: 0,
          latestMessage: null,
          allMessages: []
        })
      }
      
      const group = groups.get(msg.userId)!
      group.allMessages.push(msg)
      group.totalMessages++
      if (!msg.isRead) {
        group.unreadCount++
      }
      
      // 更新最新消息（按时间比较）
      if (!group.latestMessage || new Date(msg.createdAt) > new Date(group.latestMessage.createdAt)) {
        group.latestMessage = msg
      }
    }
    
    // 转换为数组并按最新消息时间倒序排序
    return Array.from(groups.values()).sort((a, b) => {
      const aTime = a.latestMessage?.createdAt || ''
      const bTime = b.latestMessage?.createdAt || ''
      return bTime.localeCompare(aTime)
    })
  })

  // 格式化时间
  const formatTime = (time: string): string => {
    if (!time) return ''
    try {
      const date = new Date(time)
      if (isNaN(date.getTime())) return ''
      const month = (date.getMonth() + 1).toString().padStart(2, '0')
      const day = date.getDate().toString().padStart(2, '0')
      const hours = date.getHours().toString().padStart(2, '0')
      const minutes = date.getMinutes().toString().padStart(2, '0')
      return `${month}-${day} ${hours}:${minutes}`
    } catch {
      return ''
    }
  }

  // 加载消息列表
  const loadMessages = async (): Promise<void> => {
    loading.value = true
    try {
      const response = await authAPI.getConversations()
      
      let messageData: ChatMessage[] = []
      
      if (response && response.success && response.data) {
        // 后端返回格式: { success: true, data: { conversations: [...], totalUnread: x } }
        const conversations = response.data.conversations
        
        if (conversations && Array.isArray(conversations)) {
          // 将 conversations 转换为 ChatMessage 格式
          messageData = conversations.map((conv: any) => ({
            id: conv.userId,
            userId: conv.userId,
            content: conv.lastContent || '',
            isRead: conv.unreadCount === 0,
            createdAt: conv.lastTime || new Date().toISOString()
          }))
        }
      }
      
      rawMessages.value = messageData
    } catch (error) {
      console.error('加载消息失败:', error)
      Message.error('加载失败')
    } finally {
      loading.value = false
    }
  }

  // 全部标记已读
  const markAllRead = async (): Promise<void> => {
    try {
      await Message.confirm('确定要将所有消息标记为已读吗？', '确认')
      
      loadingAll.value = true
      const response = await authAPI.markAllMessagesRead()
      
      if (response && response.success) {
        Message.success('全部已读')
        await loadMessages()
      } else {
        Message.error(response?.message || '操作失败')
      }
    } catch {
      // 用户取消操作
    } finally {
      loadingAll.value = false
    }
  }

  // 打开与用户的对话详情
  const openChatDetail = (userId: number): void => {
    router.push({
      name: 'SellerChatDetail',
      query: { userId: String(userId) }
    })
  }

  // 监听新消息事件
  const handleNewChatMessage = (): void => {
    loadMessages()
  }

  onMounted(() => {
    loadMessages()
    window.addEventListener('new-chat-message', handleNewChatMessage)
  })

  onUnmounted(() => {
    window.removeEventListener('new-chat-message', handleNewChatMessage)
  })
</script>

<style scoped>
@import url('@/static/css/seller/消息列表.css');
</style>