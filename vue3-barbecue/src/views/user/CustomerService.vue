<!-- views/user/CustomerService.vue -->
<template>
  <div class="chat-page">
    <div class="chat-header">
      <button class="back-btn" @click="goBack">
        <span class="back-icon">←</span>
      </button>
      <h2 class="header-title">在线客服</h2>
      <div class="header-placeholder"></div>
    </div>

    <div class="message-list" ref="messageListRef">
      <div 
        v-for="msg in messages" 
        :key="msg.id"
        class="message-item"
        :class="{ self: msg.userId === currentUserId }"
      >
        <div class="message-bubble">
          <div class="message-content">{{ msg.content }}</div>
          <div class="message-time">{{ formatTime(msg.createdAt) }}</div>
        </div>
      </div>
      <div v-if="loading" class="loading-more">加载中...</div>
    </div>

    <div class="chat-input-area">
      <input 
        type="text" 
        v-model="inputMessage" 
        placeholder="请输入消息..."
        @keyup.enter="sendMessage"
        class="chat-input"
      />
      <button class="send-btn" @click="sendMessage" :disabled="!inputMessage.trim()">
        发送
      </button>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { useUserStore } from '@/stores/auth'
import { authAPI } from '@/api/authAPI'

const router = useRouter()
const userStore = useUserStore()

const currentUserId = ref<number | null>(null)
const messages = ref<any[]>([])
const inputMessage = ref('')
const loading = ref(false)
const messageListRef = ref<HTMLElement | null>(null)

const formatTime = (time: string) => {
  if (!time) return ''
  const date = new Date(time)
  const hours = date.getHours().toString().padStart(2, '0')
  const minutes = date.getMinutes().toString().padStart(2, '0')
  return `${hours}:${minutes}`
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight
    }
  })
}

const loadMessages = async () => {
  loading.value = true
  try {
    const response = await authAPI.getUserChatMessages(currentUserId.value)
    const data = response.data || response
    if (data.success && data.data) {
      messages.value = data.data
      scrollToBottom()
    }
  } catch (error) {
    console.error('加载消息失败:', error)
  } finally {
    loading.value = false
  }
}

const sendMessage = async () => {
  const content = inputMessage.value.trim()
  if (!content) return
  
  try {
    const response = await authAPI.sendChatMessage({
      userId: currentUserId.value,
      content: content
    })
    const data = response.data || response
    
    if (data.success) {
      inputMessage.value = ''
      await loadMessages()
    } else {
      showToast({ message: data.message || '发送失败', type: 'fail' })
    }
  } catch (error) {
    console.error('发送失败:', error)
    showToast({ message: '发送失败', type: 'fail' })
  }
}

const goBack = () => {
  router.back()
}

onMounted(async () => {
  currentUserId.value = userStore.userId
  if (!currentUserId.value) {
    showToast({ message: '请先登录', type: 'fail' })
    return
  }
  await loadMessages()
})
</script>

<style scoped>
.chat-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background-color: #f5f5f5;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  background: linear-gradient(135deg, #e65100, #bf360c);
  color: white;
  flex-shrink: 0;
}

.back-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: white;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.header-title {
  font-size: 18px;
  font-weight: 600;
  margin: 0;
}

.header-placeholder {
  width: 40px;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.message-item {
  display: flex;
  margin-bottom: 12px;
}

.message-item.self {
  justify-content: flex-end;
}

.message-item.other {
  justify-content: flex-start;
}

.message-bubble {
  max-width: 70%;
  padding: 10px 14px;
  border-radius: 18px;
  position: relative;
}

.message-item.self .message-bubble {
  background: linear-gradient(135deg, #e65100, #bf360c);
  color: white;
  border-bottom-right-radius: 4px;
}

.message-item.other .message-bubble {
  background: white;
  color: #333;
  border-bottom-left-radius: 4px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.message-content {
  font-size: 14px;
  word-break: break-word;
}

.message-time {
  font-size: 10px;
  margin-top: 4px;
  opacity: 0.7;
  text-align: right;
}

.chat-input-area {
  display: flex;
  gap: 8px;
  padding: 12px 16px;
  background: white;
  border-top: 1px solid #eee;
  flex-shrink: 0;
}

.chat-input {
  flex: 1;
  padding: 10px 14px;
  border: 1px solid #ddd;
  border-radius: 24px;
  font-size: 14px;
  outline: none;
}

.chat-input:focus {
  border-color: #e65100;
}

.send-btn {
  padding: 10px 20px;
  background: linear-gradient(135deg, #e65100, #bf360c);
  color: white;
  border: none;
  border-radius: 24px;
  font-size: 14px;
  cursor: pointer;
}

.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.loading-more {
  text-align: center;
  padding: 10px;
  color: #999;
  font-size: 12px;
}
</style>