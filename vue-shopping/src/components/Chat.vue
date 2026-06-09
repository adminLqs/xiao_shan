<template>
  <div class="chat-page">
    <!-- 加载骨架 -->
    <div v-if="loading" class="chat-loading">
      <div v-for="i in 5" :key="i" class="skeleton-msg" :class="i % 2 === 0 ? 'skeleton-right' : 'skeleton-left'">
        <div class="skeleton" style="width:60%;height:40px;border-radius:12px;"></div>
      </div>
    </div>

    <!-- 聊天内容 -->
    <template v-else>
      <!-- 顶部：对方信息 -->
      <div class="chat-header">
        <button class="header-back" @click="router.back()">
          <i class="fas fa-chevron-left"></i>
        </button>
        <img :src="targetAvatar || defaultAvatar" class="target-avatar" />
        <div class="target-info">
          <div class="target-name">{{ targetName }}</div>
          <div class="target-status">
            <span v-if="isTyping" class="typing-text">正在输入...</span>
            <span v-else :class="{ online: targetOnline }">
              {{ targetOnline ? '在线' : '离线 ' + formatOfflineTime(offlineMinutes) }}
            </span>
          </div>
        </div>
        <button class="header-more" @click="showMoreMenu = !showMoreMenu">
          <i class="fas fa-ellipsis-v"></i>
        </button>
      </div>

      <!-- 更多菜单 -->
      <div v-if="showMoreMenu" class="more-menu" @click.self="showMoreMenu = false">
        <div class="menu-item" @click="toggleMute">
          <i :class="['fas', isMuted ? 'fa-volume-mute' : 'fa-volume-up']"></i>
          <span>{{ isMuted ? '取消免打扰' : '消息免打扰' }}</span>
        </div>
      </div>

      <!-- 消息列表 -->
      <div class="chat-messages ready" ref="msgListRef" @scroll="onScroll">
        <!-- 下拉刷新 -->
        <div v-if="pullRefresh" class="pull-refresh" @touchstart.passive="onTouchStart" @touchmove="onTouchMove" @touchend="onTouchEnd">
          <i :class="['fas', pullRefreshIcon]"></i>
          <span>{{ pullRefreshText }}</span>
        </div>

        <div class="chat-date" v-if="messages.length === 0">暂无消息，发送第一条消息吧</div>

        <div v-for="(msg, idx) in messages" :key="msg.id">
          <div class="chat-date" v-if="showDate(idx)">{{ formatDate(msg.sendTime) }}</div>

          <div class="msg-row" :class="isSender(msg) ? 'msg-right' : 'msg-left'" @longpress="onLongPress(msg)">
            <img v-if="!isSender(msg)" :src="targetAvatar || defaultAvatar" class="msg-avatar" />

            <div class="msg-body">
              <!-- 撤回提示 -->
              <div v-if="msg.isRecalled" class="recalled-message">
                <span>{{ isSender(msg) ? '你撤回了一条消息' : '对方撤回了一条消息' }}</span>
              </div>

              <!-- 正常消息 -->
              <div v-else class="msg-bubble" :class="isSender(msg) ? 'bubble-right' : 'bubble-left'">
                <!-- 商品卡片 -->
                <div v-if="msg.type === 'PRODUCT_CARD' && msg.productId" class="product-card" @click="goToProduct(msg.productId)">
                  <div class="card-image" v-if="getProductInfo(msg.productId)">
                    <img :src="getProductInfo(msg.productId).image" />
                  </div>
                  <div class="card-image placeholder" v-else>
                    <i class="fas fa-image"></i>
                  </div>
                  <div class="card-info">
                    <div class="card-title">{{ getProductInfo(msg.productId)?.name || '商品' }}</div>
                    <div class="card-price">¥{{ getProductInfo(msg.productId)?.price || '0.00' }}</div>
                    <div class="card-sales">已售{{ getProductInfo(msg.productId)?.salesCount || 0 }}件</div>
                  </div>
                </div>

                <!-- 订单卡片 -->
                <div v-else-if="msg.type === 'ORDER_CARD' && msg.orderId" class="order-card" @click="goToOrder(msg.orderId)">
                  <div class="order-icon">
                    <i class="fas fa-receipt"></i>
                  </div>
                  <div class="order-info">
                    <div class="order-number">订单号: {{ getOrderInfo(msg.orderId)?.orderNumber || '***' }}</div>
                    <div class="order-status">状态: {{ getOrderStatusText(getOrderInfo(msg.orderId)?.status) }}</div>
                    <div class="order-amount">金额: ¥{{ getOrderInfo(msg.orderId)?.totalAmount || '0.00' }}</div>
                  </div>
                </div>

                <!-- 文本消息 -->
                <div v-else-if="msg.text" class="msg-text">{{ msg.text }}</div>

                <!-- 图片消息 -->
                <div v-if="msg.images?.length" class="msg-images">
                  <img v-for="(img, i) in msg.images" :key="i" :src="img"
                    class="msg-image" @click="previewImage(img)" />
                </div>

                <!-- 视频消息 -->
                <div v-if="msg.videos?.length" class="msg-videos">
                  <video v-for="(video, i) in msg.videos" :key="i" :src="video"
                    class="msg-video" controls playsinline />
                </div>
              </div>

              <div class="msg-time">{{ formatMsgTime(msg.sendTime) }}</div>
            </div>

            <img v-if="isSender(msg)" :src="myAvatar || defaultAvatar" class="msg-avatar" />
          </div>
        </div>

      </div>

      <!-- 长按操作菜单 -->
      <div v-if="showActionMenu" class="action-menu" @click.self="showActionMenu = false">
        <div class="menu-content">
          <button class="menu-btn copy" @click="copyMessage(selectedMsg)">
            <i class="fas fa-copy"></i>
            <span>复制</span>
          </button>
          <button v-if="isSender(selectedMsg)" class="menu-btn recall" @click="recallMessage(selectedMsg)">
            <i class="fas fa-undo"></i>
            <span>撤回</span>
          </button>
        </div>
      </div>

      <!-- 快捷回复面板 -->
      <div v-if="showQuickReplies" class="quick-replies-panel">
        <div class="panel-header">
          <span>常用语</span>
          <button class="close-btn" @click="showQuickReplies = false">
            <i class="fas fa-times"></i>
          </button>
        </div>
        <div class="quick-list">
          <div v-for="reply in quickReplies" :key="reply.id"
            class="quick-item" @click="sendQuickReply(reply)">
            {{ reply.content }}
          </div>
          <div class="quick-item add" @click="showAddQuickReply = true">
            <i class="fas fa-plus"></i>
            <span>添加快捷回复</span>
          </div>
        </div>
      </div>

      <!-- 添加快捷回复弹窗 -->
      <div v-if="showAddQuickReply" class="modal-overlay" @click.self="showAddQuickReply = false">
        <div class="modal-content">
          <h3>添加快捷回复</h3>
          <textarea v-model="newQuickReply" name="quickReplyContent" placeholder="输入快捷回复内容" rows="3"></textarea>
          <div class="modal-actions">
            <button class="btn-cancel" @click="showAddQuickReply = false">取消</button>
            <button class="btn-confirm" @click="addQuickReply">确定</button>
          </div>
        </div>
      </div>

      <!-- 表情包面板 -->
      <div v-if="showEmojiPanel" class="emoji-panel">
        <div class="emoji-tabs">
          <span :class="{ active: emojiTab === 'face' }" @click="emojiTab='face'">😊</span>
          <span :class="{ active: emojiTab === 'hand' }" @click="emojiTab='hand'">👍</span>
          <span :class="{ active: emojiTab === 'heart' }" @click="emojiTab='heart'">❤️</span>
          <span :class="{ active: emojiTab === 'all' }" @click="emojiTab='all'">🔥</span>
          <span :class="{ active: emojiTab === 'animal' }" @click="emojiTab='animal'">🐶</span>
          <span :class="{ active: emojiTab === 'food' }" @click="emojiTab='food'">🍔</span>
          <span :class="{ active: emojiTab === 'more' }" @click="emojiTab='more'">🌈</span>
        </div>
        <div class="emoji-grid">
          <span v-for="emoji in emojis" :key="emoji" class="emoji-item" @click="insertEmoji(emoji)">
            {{ emoji }}
          </span>
        </div>
      </div>

      <!-- 商品/订单分享面板 -->
      <div v-if="showSharePanel" class="share-panel">
        <div class="panel-header">
          <span>分享</span>
          <button class="close-btn" @click="showSharePanel = false">
            <i class="fas fa-times"></i>
          </button>
        </div>
        <div class="share-list">
          <div class="share-item" @click="selectProduct">
            <i class="fas fa-shopping-cart"></i>
            <span>商品</span>
          </div>
          <div class="share-item" @click="selectOrder">
            <i class="fas fa-receipt"></i>
            <span>订单</span>
          </div>
        </div>
      </div>

      <!-- 底部输入区 -->
      <div class="chat-input-bar">
        <!-- 工具栏弹窗 -->
        <div v-if="showToolbar" class="toolbar-popup">
          <div class="toolbar-item" @click="triggerUpload">
            <i class="fas fa-image"></i>
            <span>图片</span>
          </div>
          <div class="toolbar-item" @click="triggerVideo">
            <i class="fas fa-video"></i>
            <span>视频</span>
          </div>
          <div class="toolbar-item" @click="showSharePanel = true">
            <i class="fas fa-share-alt"></i>
            <span>分享</span>
          </div>
          <div class="toolbar-item" @click="loadQuickReplies">
            <i class="fas fa-comment-alt"></i>
            <span>快捷回复</span>
          </div>
        </div>



        <div class="input-row">
          <button class="btn-emoji" @click="toggleEmoji">
            <i class="fas fa-smile"></i>
          </button>
          <input
            v-model="inputText"
            name="chatMessage"
            class="chat-input"
            placeholder="输入消息..."
            maxlength="500"
            @keyup.enter="sendMessage"
            @input="handleInput"
          />
          <button class="btn-plus-right" @click="toggleToolbar">
            <i :class="showToolbar ? 'fas fa-times' : 'fas fa-plus'"></i>
          </button>
          <button class="btn-send" @click="sendMessage" :disabled="!inputText.trim() && uploadingFiles.length === 0">
            发送
          </button>
        </div>
        <input type="file" ref="fileInputRef" name="chatImage" multiple accept="image/*" style="display:none" @change="handleFileSelect" />
        <input type="file" ref="videoInputRef" name="chatVideo" accept="video/*" style="display:none" @change="handleVideoSelect" />
      </div>
    </template>

    <!-- 图片预览 -->
    <ImagePreview
      v-if="showPreview"
      :mediaList="allMediaList"
      :currentIndex="previewIndex"
      @close="closePreview"
      @update:index="previewIndex = $event"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ImagePreview from './ImagePreview.vue'
import { authAPI } from '@/api/authAPI'
import { useAuthStore } from '@/stores/auth'
import Message from '@/utils/message'
import defaultAvatar from '@/static/images/user-avatar.jpg'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const currentUserId = ref(authStore.userId)
const myAvatar = ref(defaultAvatar)

const targetId = computed(() => Number(route.params.targetId))

const targetName = ref('')
const targetAvatar = ref(defaultAvatar)
const targetOnline = ref(true)
const offlineMinutes = ref(0)
const isTyping = ref(false)
const isMuted = ref(false)

const loading = ref(true)
const loadingMore = ref(false)
const hasMore = ref(true)
const currentPage = ref(1)
const pageSize = 20

const inputText = ref('')
const messages = ref<any[]>([])
const msgListRef = ref<HTMLElement | null>(null)
const showPreview = ref(false)
const previewIndex = ref(0)
const fileInputRef = ref<HTMLInputElement | null>(null)
const videoInputRef = ref<HTMLInputElement | null>(null)
const uploadingImages = ref<string[]>([])
const uploadingFiles = ref<File[]>([])

// UI状态
const showToolbar = ref(false)
const showEmojiPanel = ref(false)
const showQuickReplies = ref(false)
const showSharePanel = ref(false)
const showActionMenu = ref(false)
const showMoreMenu = ref(false)
const showAddQuickReply = ref(false)
const selectedMsg = ref<any>(null)

// 下拉刷新
const pullRefresh = ref(false)
const pullRefreshText = ref('下拉刷新')
const pullRefreshIcon = ref('fa-chevron-down')
let touchStartY = 0

// 快捷回复
const quickReplies = ref<any[]>([])
const newQuickReply = ref('')

// 表情包分类
const emojiCategories = {
  face: ['😀','😃','😄','😁','😅','🤣','😂','😊','😇','🙂','😍','🤩','😘','😗','😚','😋','😛','😜','🤪','😝'],
  hand: ['👍','👎','👏','🙌','🤝','💪','👋','🤞','✌️','🤟','👌','🤌','🖖','🤙','🫶'],
  heart: ['❤️','🧡','💛','💚','💙','💜','🖤','🤍','🤎','💕','💞','💖','💘','💝'],
  all: ['🔥','⭐','✨','🎉','🎊','💰','💯','✅','❌','💤','😴','🤔','😭','😤','😡','🥺'],
  animal: ['🐶','🐱','🐼','🐨','🐰','🦊','🐸','🐵','🐮','🐷','🐹','🐭'],
  food: ['🍔','🍟','🍕','🍰','🎂','🍩','🍿','🍺','☕','🧋','🍉','🍓'],
  more: ['🌈','🌸','🌺','🍀','🎵','🎶','📚','💡','🔑','🎁','🏆','💎']
}

const emojiTab = ref('face')

const emojis = computed(() => emojiCategories[emojiTab.value as keyof typeof emojiCategories] || emojiCategories.face)

// 商品/订单信息缓存
const productCache = ref<Map<number, any>>(new Map())
const orderCache = ref<Map<number, any>>(new Map())

// 媒体预览
const allMediaList = computed(() => {
  const list: { type: 'image' | 'video'; url: string; cover?: string }[] = []
  messages.value.forEach(msg => {
    if (msg.images?.length) {
      msg.images.forEach((img: string) => list.push({ type: 'image', url: img }))
    }
    if (msg.videos?.length) {
      msg.videos.forEach((v: any) => list.push({ type: 'video', url: v, cover: v.coverUrl }))
    }
  })
  return list
})

/**
 * 格式化离线时长
 * @param {number} minutes - 离线分钟数
 * @returns {string} 格式化后的字符串
 */
const formatOfflineTime = (minutes: number): string => {
  if (minutes <= 0) return '刚刚离线'
  if (minutes < 60) return `${minutes}分钟`
  if (minutes < 1440) return `${Math.floor(minutes / 60)}小时`
  if (minutes < 43200) return `${Math.floor(minutes / 1440)}天`
  if (minutes < 518400) return `${Math.floor(minutes / 43200)}个月`
  return `${Math.floor(minutes / 518400)}年`
}

const isSender = (msg: any) => msg.senderId === currentUserId.value

const showDate = (idx: number) => {
  if (idx === 0) return true
  const prev = new Date(messages.value[idx - 1]?.sendTime)
  const curr = new Date(messages.value[idx]?.sendTime)
  return prev.getDate() !== curr.getDate() ||
         prev.getMonth() !== curr.getMonth() ||
         prev.getFullYear() !== curr.getFullYear()
}

const formatMsgTime = (time: string) => {
  if (!time) return ''
  const d = new Date(time)
  if (isNaN(d.getTime())) return ''
  return `${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
}

const formatDate = (time: string) => {
  if (!time) return ''
  const d = new Date(time)
  if (isNaN(d.getTime())) return ''
  const now = new Date()
  const diff = now.getTime() - d.getTime()
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))

  if (days === 0) return '今天'
  if (days === 1) return '昨天'
  if (days === 2) return '前天'
  if (days < 7) return `${days}天前`
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`
}

const getProductInfo = (productId: number) => {
  return productCache.value.get(productId)
}

const getOrderInfo = (orderId: number) => {
  return orderCache.value.get(orderId)
}

const getOrderStatusText = (status: string) => {
  const statusMap: Record<string, string> = {
    'PENDING': '待付款',
    'PAID': '已付款',
    'PROCESSING': '处理中',
    'SHIPPED': '已发货',
    'COMPLETED': '已完成',
    'CANCELLED': '已取消'
  }
  return statusMap[status] || status
}

const loadTarget = async () => {
  try {
    const res = await authAPI.getChatTarget(targetId.value)
    if (res.success && res.data) {
      targetName.value = res.data.name || '用户'
      targetAvatar.value = res.data.avatar || defaultAvatar
    }
  } catch (error: any) {
    Message.error('加载用户信息失败')
  }
}

const loadMyInfo = async () => {
  try {
    let res
    // 根据用户角色调用不同的接口
    if (authStore.isSeller) {
      // 商家调用商家接口
      res = await authAPI.getSellerProfile()
      if (res.success && res.data) {
        const profile = res.data.profile || res.data
        myAvatar.value = profile.storeAvatar || profile.avatar || defaultAvatar
      }
    } else {
      // 用户调用用户接口
      res = await authAPI.getUserProfile()
      if (res.success && res.data) {
        const profile = res.data.profile || res.data
        myAvatar.value = profile.avatar || profile.avatarUrl || defaultAvatar
      }
    }
  } catch (error: any) {
    // 静默失败，使用默认头像
  }
}

const loadOnlineStatus = async () => {
  try {
    const res = await authAPI.getOnlineStatus(targetId.value)
    if (res.success) {
      targetOnline.value = res.data?.online || false
      offlineMinutes.value = res.data?.offlineMinutes || 0
    }
  } catch {}
}

const loadMessages = async (page: number = 1, append: boolean = false) => {
  try {
    const res = await authAPI.getChatMessages(targetId.value, page, pageSize)
    if (res.success && res.data) {
      const newMessages = res.data.messages || []

      if (append) {
        messages.value = [...newMessages.reverse(), ...messages.value]
      } else {
        messages.value = [...newMessages].reverse()
      }
      hasMore.value = newMessages.length >= pageSize
      currentPage.value = page
    }
  } catch (error: any) {
    Message.error('加载聊天记录失败')
  }
}

const loadChat = async () => {
  try {
    loading.value = true
    await loadTarget()
    await loadOnlineStatus()
    await loadMyInfo()
    await loadMessages(1)
  } finally {
    loading.value = false
    await nextTick()
    scrollToBottom()
  }
}

const scrollToBottom = () => {
  if (msgListRef.value) {
    msgListRef.value.scrollTop = msgListRef.value.scrollHeight
  }
}

const MAX_IMAGES = 9
const MAX_VIDEOS = 1

const sendMessage = async () => {
  const hasText = inputText.value.trim().length > 0
  const hasMedia = uploadingFiles.value.length > 0

  if (!hasText && !hasMedia) return

  const files = [...uploadingFiles.value]
  const content = inputText.value.trim()

  // 清空输入
  inputText.value = ''
  uploadingImages.value = []
  uploadingFiles.value = []
  showToolbar.value = false

  try {
    const isVideo = files[0]?.type?.startsWith('video/')

    let res
    if (isVideo) {
      // 视频消息
      res = await authAPI.sendVideoMessage(targetId.value, files[0]!)
    } else if (files.length > 0) {
      // 图片消息
      res = await authAPI.sendImageMessage(targetId.value, files)
    } else {
      // 纯文字
      res = await authAPI.sendTextMessage(targetId.value, content)
    }

    // 直接追加消息到列表，不重新加载
    if (res.success && res.data) {
      const newMsg = {
        id: res.data.id,
        senderId: res.data.senderId,
        receiverId: res.data.receiverId,
        text: res.data.text || content,
        type: res.data.type || 'TEXT',
        images: res.data.images || [],
        videos: res.data.videos || [],
        productId: res.data.productId,
        orderId: res.data.orderId,
        sendTime: res.data.sendTime || new Date().toISOString(),
        time: res.data.time,
        isRecalled: false
      }
      messages.value.push(newMsg)
      await nextTick()
      scrollToBottom()
    }
  } catch (error: any) {
    Message.error('发送失败')
  }
}

const sendQuickReply = async (reply: any) => {
  inputText.value = reply.content
  showQuickReplies.value = false
  await sendMessage()
}

const addQuickReply = async () => {
  if (!newQuickReply.value.trim()) return

  try {
    const res = await authAPI.addQuickReply(newQuickReply.value)
    if (res.success) {
      quickReplies.value.push(res.data)
      newQuickReply.value = ''
      showAddQuickReply.value = false
      Message.success('添加成功')
    }
  } catch (error: any) {
    Message.error('添加失败')
  }
}

const loadQuickReplies = async () => {
  try {
    const res = await authAPI.getQuickReplies()
    if (res.success) {
      quickReplies.value = res.data
    }
    showQuickReplies.value = true
  } catch (error: any) {
    // 如果不是商家，可能没有权限，静默处理
    showQuickReplies.value = false
  }
}

const triggerUpload = () => {
  fileInputRef.value?.click()
}

const triggerVideo = () => {
  videoInputRef.value?.click()
}

const handleFileSelect = async (event: any) => {
  const files = event.target.files
  if (!files) return

  if (files.length > 9) {
    Message.warning('一次最多发送9张图片')
    event.target.value = ''
    return
  }

  for (let i = 0; i < files.length; i++) {
    const file = files[i]
    if (!file.type.startsWith('image/')) {
      Message.warning('只支持图片格式')
      continue
    }

    if (file.size > 5 * 1024 * 1024) {
      Message.warning('单张图片不能超过5MB')
      continue
    }

    const compressedFile = await compressImage(file)
    uploadingFiles.value.push(compressedFile)
  }

  event.target.value = ''
  sendMessage()
}

const compressImage = (file: File): Promise<File> => {
  return new Promise((resolve) => {
    const img = new Image()
    const reader = new FileReader()

    reader.onload = (e) => {
      img.src = e.target?.result as string
      img.onload = () => {
        const canvas = document.createElement('canvas')
        const ctx = canvas.getContext('2d')

        const maxSize = 1024
        let width = img.width
        let height = img.height

        if (width > maxSize || height > maxSize) {
          if (width > height) {
            height = (height * maxSize) / width
            width = maxSize
          } else {
            width = (width * maxSize) / height
            height = maxSize
          }
        }

        canvas.width = width
        canvas.height = height

        ctx?.drawImage(img, 0, 0, width, height)

        canvas.toBlob((blob) => {
          if (blob) {
            resolve(new File([blob], file.name, { type: 'image/jpeg' }))
          } else {
            resolve(file)
          }
        }, 'image/jpeg', 0.8)
      }
    }

    reader.readAsDataURL(file)
  })
}

const handleVideoSelect = (event: any) => {
  const files = event.target.files
  if (!files || files.length === 0) return

  if (files.length > 1 || uploadingFiles.value.length > 0) {
    Message.warning('一次只能发送1个视频')
    event.target.value = ''
    return
  }

  const file = files[0]
  if (!file.type.startsWith('video/')) {
    Message.warning('只支持视频格式')
    event.target.value = ''
    return
  }

  if (file.size > 100 * 1024 * 1024) {
    Message.warning('视频不能超过100MB')
    event.target.value = ''
    return
  }

  uploadingFiles.value = [file]

  event.target.value = ''
  sendMessage()
}

const removeImage = (index: number) => {
  uploadingImages.value.splice(index, 1)
  uploadingFiles.value.splice(index, 1)
}

const previewImage = (url: string) => {
  const index = allMediaList.value.findIndex(item => item.url === url)
  if (index !== -1) {
    previewIndex.value = index
    showPreview.value = true
  }
}

const closePreview = () => {
  showPreview.value = false
}

const loadMoreMessages = async () => {
  if (loadingMore.value || !hasMore.value) return

  loadingMore.value = true
  const prevScrollTop = msgListRef.value?.scrollTop || 0
  const prevScrollHeight = msgListRef.value?.scrollHeight || 0

  await loadMessages(currentPage.value + 1, true)

  await nextTick()
  if (msgListRef.value) {
    const newScrollHeight = msgListRef.value.scrollHeight
    msgListRef.value.scrollTop = prevScrollTop + (newScrollHeight - prevScrollHeight)
  }
  loadingMore.value = false
}

const onScroll = () => {
  if (!msgListRef.value) return
  const { scrollTop } = msgListRef.value

  if (scrollTop <= 150 && hasMore.value && !loadingMore.value) {
    loadMoreMessages()
  }
}

const handleInput = () => {
  // 发送输入中状态
  sendTypingStatus()
}

let typingTimer: ReturnType<typeof setTimeout> | null = null

const sendTypingStatus = () => {
  if (typingTimer) clearTimeout(typingTimer)

  // 通过WebSocket发送输入中状态
  window.dispatchEvent(new CustomEvent('typing-status', {
    detail: { targetId: targetId.value, typing: true }
  }))

  typingTimer = setTimeout(() => {
    window.dispatchEvent(new CustomEvent('typing-status', {
      detail: { targetId: targetId.value, typing: false }
    }))
  }, 1500)
}

const onLongPress = (msg: any) => {
  selectedMsg.value = msg
  showActionMenu.value = true
}

const copyMessage = (msg: any) => {
  if (msg.text && !msg.isRecalled) {
    navigator.clipboard.writeText(msg.text)
    Message.success('复制成功')
  }
  showActionMenu.value = false
}

const recallMessage = async (msg: any) => {
  if (!msg.isRecalled) {
    try {
      const res = await authAPI.recallMessage(msg.id)
      if (res.success) {
        msg.isRecalled = true
        Message.success('撤回成功')
      } else {
        Message.error(res.message || '撤回失败')
      }
    } catch (error: any) {
      Message.error('撤回失败')
    }
  }
  showActionMenu.value = false
}

const toggleEmoji = () => {
  showEmojiPanel.value = !showEmojiPanel.value
  if (showEmojiPanel.value) showToolbar.value = false
}

const toggleToolbar = () => {
  showToolbar.value = !showToolbar.value
  if (showToolbar.value) showEmojiPanel.value = false
}

const toggleMute = async () => {
  isMuted.value = !isMuted.value
  showMoreMenu.value = false
  Message.info(isMuted.value ? '已开启免打扰' : '已关闭免打扰')
}

const insertEmoji = (emoji: string) => {
  inputText.value += emoji
}

const selectProduct = () => {
  showSharePanel.value = false
  Message.info('商品选择功能开发中')
}

const selectOrder = () => {
  showSharePanel.value = false
  Message.info('订单选择功能开发中')
}

const goToProduct = (productId: number) => {
  router.push({ name: 'ProductDetail', params: { id: productId } })
}

const goToOrder = (orderId: number) => {
  router.push({ name: 'OrderDetail', params: { id: orderId } })
}

const onTouchStart = (e: TouchEvent) => {
  const touch = e.touches[0]
  if (!touch) return
  touchStartY = touch.clientY
}

const onTouchMove = (e: TouchEvent) => {
  const touch = e.touches[0]
  if (!touch) return
  const currentY = touch.clientY
  const diff = touchStartY - currentY

  if (diff > 50) {
    pullRefresh.value = true
    if (diff > 100) {
      pullRefreshText.value = '松开刷新'
      pullRefreshIcon.value = 'fa-refresh'
    } else {
      pullRefreshText.value = '下拉刷新'
      pullRefreshIcon.value = 'fa-chevron-down'
    }
  }
}

const onTouchEnd = () => {
  if (pullRefreshText.value === '松开刷新') {
    pullRefreshText.value = '刷新中...'
    pullRefreshIcon.value = 'fa-spinner fa-spin'
    loadChat().then(() => {
      pullRefresh.value = false
      pullRefreshText.value = '下拉刷新'
      pullRefreshIcon.value = 'fa-chevron-down'
    })
  }
  pullRefresh.value = false
}

const handleNewMessage = (event: Event) => {
  const msg = (event as CustomEvent).detail

  if (msg && msg.senderId === targetId.value) {
    messages.value.push({
      id: msg.id || Date.now(),
      senderId: msg.senderId,
      receiverId: msg.receiverId || currentUserId.value,
      text: msg.text || msg.content,
      type: msg.type || 'TEXT',
      images: msg.images || [],
      videos: msg.videos || [],
      productId: msg.productId,
      orderId: msg.orderId,
      sendTime: msg.sendTime || new Date().toISOString(),
      time: msg.time,
      isRecalled: false
    })
    nextTick(() => scrollToBottom())
  }
}

const closeAllPanels = () => {
  showEmojiPanel.value = false
  showToolbar.value = false
  showSharePanel.value = false
  showQuickReplies.value = false
  showActionMenu.value = false
  showMoreMenu.value = false
  showAddQuickReply.value = false
}

const handleClickOutside = (event: MouseEvent) => {
  const target = event.target as HTMLElement
  const isInsidePanel = target.closest('.emoji-panel') ||
                        target.closest('.toolbar-popup') ||
                        target.closest('.share-panel') ||
                        target.closest('.quick-replies-panel') ||
                        target.closest('.action-menu') ||
                        target.closest('.more-menu') ||
                        target.closest('.modal-overlay')

  const isTriggerButton = target.closest('.btn-emoji') ||
                         target.closest('.btn-plus-right') ||
                         target.closest('.btn-more')

  if (!isInsidePanel && !isTriggerButton) {
    closeAllPanels()
  }
}

onMounted(() => {
  loadChat()
  window.addEventListener('new-chat-message', handleNewMessage)
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  window.removeEventListener('new-chat-message', handleNewMessage)
  document.removeEventListener('click', handleClickOutside)
  if (typingTimer) clearTimeout(typingTimer)
})
</script>

<style scoped>
@import url('@/static/css/user/聊天交互页.css');
</style>
