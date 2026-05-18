<!-- views/seller/ChatDetail.vue -->
<template>
  <div class="chat-detail">
    <!-- 头部 -->
    <div class="chat-header">
      <button class="back-btn" @click="goBack">
        <span class="back-icon">←</span>
      </button>
      <h2 class="header-title">与用户 {{ userId }} 的对话</h2>
      <div class="header-placeholder"></div>
    </div>

    <!-- 消息列表 -->
    <div class="message-list" ref="messageListRef">
      <div v-if="loading" class="status-tip">加载中...</div>
      <div v-else-if="displayMessages.length === 0" class="status-tip">
        暂无消息，发送一条消息开始对话吧
      </div>

      <div
        v-for="msg in displayMessages"
        :key="msg.id"
        class="message-item"
        :class="{ 'user-message': msg.isFromUser, 'seller-message': !msg.isFromUser }"
      >
        <div class="message-bubble">
          <div class="message-content">{{ msg.content }}</div>
          <div class="message-time">{{ msg.time }}</div>
        </div>
      </div>
    </div>

    <!-- 输入区域 -->
    <div class="chat-input-area">
      <input
        type="text"
        v-model="inputMessage"
        placeholder="请输入回复内容..."
        @keyup.enter="sendMessage"
        class="chat-input"
      />
      <button class="send-btn" @click="sendMessage" :disabled="!inputMessage.trim() || sending">
        {{ sending ? '发送中...' : '发送' }}
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
  import { useRoute, useRouter } from 'vue-router'
  import Message from '@/utils/message'
  import { authAPI } from '@/api/authAPI'
  import { useWebSocketStore } from '@/stores/websocket'

  // ==================== 类型定义 ====================

  /** 原始消息数据结构（从后端返回） */
  interface RawChatMessage {
    id: number
    senderId: number | null
    receiverId: number | null
    content: string
    isRead: boolean
    createdAt: string
  }

  /** 展示用消息结构 */
  interface DisplayMessage {
    id: number
    content: string
    isFromUser: boolean   // true: 用户发送（左侧）, false: 商家发送（右侧）
    time: string
  }

  // ==================== 路由 ====================
  const route = useRoute()
  const router = useRouter()
  const webSocketStore = useWebSocketStore() 

  // ==================== 响应式数据 ====================
  const loading = ref(false)
  const sending = ref(false)
  const inputMessage = ref('')
  const rawMessages = ref<RawChatMessage[]>([])
  const messageListRef = ref<HTMLElement | null>(null)

  // 当前对话的用户ID
  const userId = ref<number>(0)

  // ==================== 计算属性 ====================

  /**
   * 格式化时间
   */
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

  /**
   * 转换为展示用消息列表（按时间升序）
   *
   * 判断逻辑：
   * - senderId 有值 → 用户发送（显示在左边）
   * - senderId 为 null → 商家发送（显示在右边）
   */
  const displayMessages = computed<DisplayMessage[]>(() => {
    if (!rawMessages.value || !Array.isArray(rawMessages.value)) {
      return []
    }

    return rawMessages.value
      .filter(msg => msg && msg.id)
      .map(msg => ({
        id: msg.id,
        content: msg.content,
        // 关键判断：senderId 有值说明是用户发送，显示在左边
        isFromUser: msg.senderId !== null && msg.senderId !== undefined,
        time: formatTime(msg.createdAt)
      }))
      .sort((a, b) => {
        // 按时间升序排列
        const timeA = rawMessages.value.find(m => m.id === a.id)?.createdAt || ''
        const timeB = rawMessages.value.find(m => m.id === b.id)?.createdAt || ''
        return new Date(timeA).getTime() - new Date(timeB).getTime()
      })
  })

  // ==================== 方法 ====================

  /**
   * 滚动到底部
   */
  const scrollToBottom = (): void => {
    nextTick(() => {
      if (messageListRef.value) {
        messageListRef.value.scrollTop = messageListRef.value.scrollHeight
      }
    })
  }

  /**
   * 加载聊天记录
   */
  const loadMessages = async (): Promise<void> => {
    if (!userId.value) return
    
    loading.value = true
    try {
      const response = await authAPI.getConversation(userId.value, true)

      let messageData: RawChatMessage[] = []
      if (response?.success && response?.data) {
        // 后端返回格式: { success: true, data: { messages: [...], total: x, userId: x } }
        if (response.data.messages && Array.isArray(response.data.messages)) {
          messageData = response.data.messages
        }
        // 兼容其他格式
        else if (Array.isArray(response.data)) {
          messageData = response.data
        }
      }

      rawMessages.value = messageData
      await scrollToBottom()
    } catch (error) {
      Message.error('加载失败')
    } finally {
      loading.value = false
    }
  }

  /**
   * 发送回复消息（商家 → 用户）
   */
  const sendMessage = async (): Promise<void> => {
    const content = inputMessage.value.trim()
    if (!content) return

    // 乐观更新：立即显示发送的消息
    const tempId = Date.now()
    const tempMessage: RawChatMessage = {
      id: tempId,
      senderId: null,           // 商家发送，senderId 为 null
      receiverId: userId.value, // 接收方为用户
      content: content,
      isRead: true,
      createdAt: new Date().toISOString()
    }
    rawMessages.value = [...rawMessages.value, tempMessage]
    inputMessage.value = ''
    await scrollToBottom()

    sending.value = true
    try {
      const response = await authAPI.sellerReplyChatMessage({
        userId: userId.value,
        content: content
      })

      if (!response?.success) {
        // 发送失败，移除临时消息
        rawMessages.value = rawMessages.value.filter(m => m.id !== tempId)
        Message.error(response?.message || '发送失败')
        await scrollToBottom()
      }
    } catch (error) {
      // 发送失败，移除临时消息
      rawMessages.value = rawMessages.value.filter(m => m.id !== tempId)
      Message.error('发送失败，请重试')
      await scrollToBottom()
    } finally {
      sending.value = false
    }
  }

  /**
   * 返回上一页
   */
  const goBack = (): void => {
    router.back()
  }

  /**
   * 监听新消息事件（用户发来的新消息）
   */
  const handleNewMessage = (event: Event): void => {
    const customEvent = event as CustomEvent
    const newMsg = customEvent.detail

    // 如果消息来自当前对话的用户
    if (newMsg && newMsg.fromUserId === userId.value) {
      loadMessages()
    }
  }

  // ==================== 初始化 ====================
  const init = async (): Promise<void> => {
    const userIdParam = route.query.userId
    if (!userIdParam) {
      Message.error('参数错误')
      router.back()
      return
    }

    userId.value = Number(userIdParam)
    if (isNaN(userId.value) || userId.value <= 0) {
      Message.error('参数错误')
      router.back()
      return
    }

    await loadMessages()
  }

  // ==================== 生命周期 ====================
  onMounted(() => {
    init()
    webSocketStore.connectSeller()
    window.addEventListener('new-chat-message', handleNewMessage)
  })

  onUnmounted(() => {
    window.removeEventListener('new-chat-message', handleNewMessage)
    webSocketStore.disconnect()
  })
</script>

<style scoped>
  @import url('@/static/css/seller/回复页.css');
</style>