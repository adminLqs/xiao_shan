<template>
  <div class="refund-chat-page page-container">
    <div v-if="loading" class="refund-skeleton">
      <div class="skeleton-card"></div>
      <div class="skeleton-card"></div>
      <div class="skeleton-card"></div>
      <div class="skeleton-card"></div>
    </div>

    <div v-else-if="refundData" class="refund-content">
      <div class="status-bar" :class="getStatusClass(refundData.refundStatus)">
        <span class="status-icon">{{ getStatusIcon(refundData.refundStatus) }}</span>
        <span class="status-text">{{ getStatusText(refundData.refundStatus) }}</span>
      </div>

      <div class="voucher-card">
        <div class="card-header">
          <span class="card-title">商品信息</span>
        </div>
        <div class="card-body">
          <div class="product-item">
            <img :src="refundData.productImage || defaultProductImage" class="item-image">
            <div class="item-info">
              <div class="item-name">{{ refundData.productName }}</div>
              <div class="tags-group">
                <span v-if="refundData.skuName" class="tag-spec">{{ refundData.skuName }}</span>
                <span class="tag-quantity">x{{ refundData.quantity || 0 }}</span>
              </div>
            </div>
            <div class="item-price">¥{{ formatPrice(refundData.price ?? 0) }}</div>
          </div>
        </div>
      </div>

      <div class="voucher-card buyer-card">
        <div class="card-header buyer-header">
          <span class="card-title">买家申请</span>
        </div>
        <div class="card-body">
          <div class="info-grid">
            <div class="info-item">
              <span class="info-label">退款类型</span>
              <span class="info-value">{{ refundData.refundType === 'REFUND' ? '仅退款' : '退货退款' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">退款金额</span>
              <span class="info-value price">¥{{ formatPrice(refundData.amount || refundData.refundAmount) }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">退款原因</span>
              <span class="info-value">{{ refundData.reason || refundData.refundReason || '-' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">申请时间</span>
              <span class="info-value time">{{ formatRelativeTime(refundData.applyTime) }}</span>
            </div>
          </div>

          <div v-if="refundData.description" class="desc-section">
            <span class="desc-label">问题描述</span>
            <p class="desc-content">{{ refundData.description }}</p>
          </div>

          <div v-if="parseEvidenceImages(refundData?.evidenceImages || '').length > 0 || (refundData.videos && refundData.videos.length > 0)" class="evidence-section">
            <span class="evidence-label">凭证图片/视频</span>
            <div class="evidence-grid">
              <img v-for="(img, index) in parseEvidenceImages(refundData?.evidenceImages || '')" :key="'img-' + index" :src="img" class="evidence-img" @click="previewImage(img)">
              <div v-for="video in (refundData.videos || [])" :key="'video-' + video.id" class="evidence-video" @click="previewVideo(video.videoUrl)">
                <img :src="video.coverUrl || '/images/video-placeholder.png'" class="video-cover">
                <div class="video-play"><i class="fas fa-play"></i></div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="voucher-card seller-card">
        <div class="card-header seller-header">
          <span class="card-title">商家处理</span>
          <button class="btn-intervene-inline" @click="handleIntervene">平台介入</button>
        </div>
        <div class="card-body">
          <div v-if="refundData.refundStatus === 'WAITING_RETURN'" class="decision-section approved">
            <span class="decision-text">
              {{ refundData.refundType === 'AFTER_SALE' ? '同意退货，等待买家寄回' : '同意退款' }}
            </span>
            <div v-if="refundData.reviewNotes" class="decision-note">审核意见：{{ refundData.reviewNotes }}</div>
          </div>

          <div v-else-if="refundData.refundStatus === 'FAILED'" class="decision-section rejected">
            <span class="decision-text">拒绝退款</span>
            <div v-if="refundData.reviewNotes" class="decision-note">拒绝原因：{{ refundData.reviewNotes }}</div>
          </div>

          <div v-else-if="isSeller && refundData.refundStatus === 'PROCESSING'" class="decision-section pending">
            <div class="decision-options">
              <label class="option-item">
                <input type="radio" v-model="sellerDecision" value="approve">
                <span class="option-icon">✓</span>
                <span class="option-text">同意退款</span>
              </label>
              <label class="option-item">
                <input type="radio" v-model="sellerDecision" value="reject">
                <span class="option-icon">✗</span>
                <span class="option-text">拒绝退款</span>
              </label>
            </div>
            <div v-if="sellerDecision === 'reject'" class="reject-reason-input">
              <textarea v-model="rejectReason" placeholder="请输入拒绝原因..." maxlength="500"></textarea>
            </div>
          </div>

          <div v-else-if="refundData.refundStatus === 'PROCESSING'" class="decision-section waiting">
            <span class="decision-text">商家处理中</span>
            <span class="decision-hint">请耐心等待商家审核</span>
          </div>

          <div v-if="refundData.sellerResponse" class="response-section">
            <span class="response-label">商家回复</span>
            <p class="response-content">{{ refundData.sellerResponse }}</p>
          </div>
        </div>
      </div>

      <div v-if="refundData.returnStatus === 'RETURNING'" class="voucher-card return-logistics-card">
        <div class="card-header">
          <span class="card-title">退货物流</span>
        </div>
        <div class="card-body">
          <div class="return-logistics-info">
            <div class="logistics-row">
              <span class="label">物流公司</span>
              <span class="value">{{ refundData.returnLogisticsName || '-' }}</span>
            </div>
            <div class="logistics-row">
              <span class="label">物流单号</span>
              <span class="value">{{ refundData.returnTrackingNumber || '-' }}</span>
            </div>
          </div>
          <button class="btn-view-return-logistics" @click="viewReturnLogistics">
            查看退货物流
          </button>
        </div>
      </div>

      <div class="voucher-card history-card">
        <div class="card-header history-header" @click="toggleHistory">
          <span class="card-title">协商历史</span>
          <span class="toggle-icon" :class="{ expanded: showHistory }">▼</span>
        </div>
        <div v-if="showHistory" class="card-body history-body">
          <div v-if="chatHistory.length === 0" class="empty-history">
            <p>暂无协商记录</p>
          </div>
          <div v-else class="history-list">
            <div v-for="msg in chatHistory" :key="msg.id" class="history-item">
              <div class="msg-header">
                <span class="msg-role">{{ msg.senderType === 'SELLER' ? '商家' : '买家' }}</span>
                <span class="msg-time">{{ formatDateTime(msg.sendTime) }}</span>
              </div>
              <p class="msg-content">{{ msg.message }}</p>
              <div v-if="msg.images && msg.images.length > 0" class="msg-images">
                <img v-for="(img, i) in msg.images" :key="i"
                  :src="getImageUrl(img)"
                  class="msg-img"
                  @click="previewImage(getImageUrl(img))">
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 结果卡片 -->
      <div v-if="isCompleted && refundData?.refundStatus !== 'FAILED'" class="result-card success">
        <div class="result-icon">✓</div>
        <div class="result-title">退款成功</div>
        <div class="result-amount">¥{{ formatPrice(refundData?.refundAmount || refundData?.amount || 0) }}</div>
        <div class="result-info">
          <div class="info-row">
            <span class="info-label">退款方式</span>
            <span class="info-value">原路返回</span>
          </div>
          <div class="info-row">
            <span class="info-label">预计到账</span>
            <span class="info-value">{{ getEstimatedTime() }}</span>
          </div>
        </div>
      </div>

      <div v-else-if="refundData?.refundStatus === 'FAILED'" class="result-card failed">
        <div class="result-icon">✗</div>
        <div class="result-title">退款已拒绝</div>
        <div v-if="refundData?.reviewNotes" class="result-reason">{{ refundData.reviewNotes }}</div>
        <div v-if="refundData?.returnStatus === 'RETURNING'" class="result-hint">
          退货物流已寄出，如有问题请联系客服处理
        </div>
      </div>

      <!-- 底部操作栏 -->
      <div v-if="isSeller && !isSellerCompleted" class="bottom-action-bar">
        <template v-if="refundData.returnStatus === 'RETURNING'">
          <button class="btn-reject" @click="showRejectModal = true">拒绝</button>
          <button class="btn-approve" @click="handleConfirmReceive" :disabled="confirmingReceive">
            {{ confirmingReceive ? '处理中...' : '确认退款' }}
          </button>
        </template>

        <template v-if="refundData.refundStatus === 'PROCESSING'">
          <button class="btn-reject" @click="handleReject" :disabled="sellerDecision !== 'reject' || !rejectReason.trim()">拒绝</button>
          <button class="btn-message" @click="showMessageDialog = true"><i class="fas fa-comment"></i> 发消息</button>
          <button class="btn-approve" @click="handleApproveRefund" :disabled="sellerDecision !== 'approve'">同意</button>
        </template>
      </div>

      <div v-if="!isSeller && !isBuyerCompleted" class="bottom-action-bar">
        <template v-if="refundData.refundStatus === 'PROCESSING'">
          <button class="btn-message" @click="showMessageDialog = true"><i class="fas fa-comment"></i> 发消息</button>
        </template>
      </div>

      <div v-if="!isSeller && refundData?.refundStatus === 'FAILED' && canReapply" class="bottom-action-bar">
        <button class="btn-appeal" @click="handleReapply">再次申请</button>
      </div>
    </div>

    <div v-else class="empty-state">
      <i class="fas fa-search"></i>
      <p>退款记录不存在</p>
      <button class="btn-primary" @click="goBack">返回</button>
    </div>

    <div v-if="showRejectModal" class="modal-overlay" @click="closeRejectModal">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>{{ refundData?.userAppeal ? '再次审核' : '拒绝退款' }}</h3>
          <button class="modal-close" @click="closeRejectModal"><i class="fas fa-times"></i></button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label class="form-label">{{ refundData?.userAppeal ? '审核意见' : '拒绝原因' }}</label>
            <textarea v-model="rejectReason" class="form-control" :placeholder="refundData?.userAppeal ? '请输入审核意见' : '请输入拒绝退款的原因'" :maxlength="500"></textarea>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-cancel" @click="closeRejectModal">取消</button>
          <button class="btn-confirm btn-danger" :disabled="!rejectReason.trim() || rejecting" @click="confirmReject">{{ rejecting ? '处理中...' : (refundData?.userAppeal ? '确认审核' : '确认拒绝') }}</button>
        </div>
      </div>
    </div>

    <div v-if="showAppealModal" class="modal-overlay" @click="closeAppealModal">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>提交申诉</h3>
          <button class="modal-close" @click="closeAppealModal"><i class="fas fa-times"></i></button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label class="form-label">申诉理由</label>
            <textarea v-model="appealContent" class="form-control" placeholder="请详细说明您的申诉理由..." :maxlength="500"></textarea>
          </div>
          <div class="form-group">
            <label class="form-label">申诉凭证（可选）</label>
            <button class="btn-upload-evidence" @click="uploadEvidence"><i class="fas fa-plus"></i> 添加图片凭证</button>
            <div v-if="appealImages.length > 0" class="evidence-preview">
              <img v-for="(img, index) in appealImages" :key="index" :src="img" class="preview-image">
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-cancel" @click="closeAppealModal">取消</button>
          <button class="btn-confirm" :disabled="!appealContent.trim()" @click="confirmAppeal">提交申诉</button>
        </div>
      </div>
    </div>

    <div v-if="showMessageDialog" class="modal-overlay" @click.self="showMessageDialog = false">
      <div class="modal-content message-dialog">
        <div class="modal-header">
          <h3>发送消息</h3>
          <button class="modal-close" @click="showMessageDialog = false"><i class="fas fa-times"></i></button>
        </div>
        <div class="modal-body">
          <textarea v-model="messageInput" class="message-textarea" placeholder="请输入消息..." maxlength="500"></textarea>
          <div class="upload-row">
            <button class="btn-upload-img" @click="uploadImage"><i class="fas fa-image"></i> 上传图片</button>
            <div v-if="selectedImages.length > 0" class="preview-images">
              <img v-for="(img, i) in selectedImages" :key="i" :src="img" class="preview-thumb">
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-cancel" @click="showMessageDialog = false">取消</button>
          <button class="btn-send" @click="sendMessage" :disabled="!messageInput.trim()">发送</button>
        </div>
      </div>
    </div>

    <ImagePreview
      v-if="showMediaPreview"
      :media-list="allMediaList"
      :current-index="mediaPreviewIndex"
      @close="showMediaPreview = false"
      @update:index="mediaPreviewIndex = $event"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, inject, watch, type Ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { authAPI } from '@/api/authAPI'
import ImagePreview from '@/components/ImagePreview.vue'
import Message from '@/utils/message'
import defaultAvatar from '@/static/images/user-avatar.jpg'
import sellerDefaultAvatar from '@/static/images/seller-avatar.jpg'
import defaultProductImage from '@/static/images/云杉购图标.jpg'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const refundType = inject<Ref<'REFUND' | 'AFTER_SALE'>>('refundType', ref('REFUND'))
const currentStep = inject<Ref<number>>('currentStep', ref(2))
const parentLoading = inject<Ref<boolean>>('loading', ref(true))

const currentUserId = computed(() => authStore.userId)
const isSeller = computed(() => authStore.isSeller)
const isSellerInOrder = computed(() => refundData.value?.sellerId === currentUserId.value)
const isBuyerInOrder = computed(() => refundData.value?.buyerId === currentUserId.value || refundData.value?.userId === currentUserId.value)
const hasPermission = computed(() => isSellerInOrder.value || isBuyerInOrder.value)

const buyerAvatar = computed(() => refundData.value?.buyerAvatar || defaultAvatar)
const sellerAvatar = computed(() => refundData.value?.sellerAvatar || sellerDefaultAvatar)

interface RefundVideo {
  id: number
  refundId: number
  videoUrl: string
  coverUrl: string
}

interface RefundData {
  id: number
  orderItemId: number
  orderId: number
  orderNumber: string
  userId: number
  buyerId: number
  sellerId: number
  buyerAvatar?: string
  buyerName?: string
  sellerAvatar?: string
  sellerName?: string
  refundType: string
  refundStatus: string
  refundAmount: number
  refundReason: string
  description: string
  evidenceImages: string
  videos?: RefundVideo[]
  applyTime: string
  reviewTime: string
  completeTime: string
  reviewNotes: string
  reviewedBy: string
  sellerResponse: string
  userAppeal: string
  appealEvidence: string
  productName?: string
  productImage?: string
  price?: number
  quantity?: number
  reason?: string
  amount?: number
  returnTrackingNumber?: string
  returnLogisticsName?: string
  returnStatus?: string
  skuName?: string
}

interface ChatMessage {
  id: number
  refundId: number
  senderType: string
  senderId: number
  senderAvatar?: string
  senderName?: string
  message: string
  images: string[]
  sendTime: string
  messageType: string
}

const loading = ref(true)
const refundData = ref<RefundData | null>(null)
const chatHistory = ref<ChatMessage[]>([])

const messageInput = ref('')
const selectedImages = ref<string[]>([])
const selectedImageFiles = ref<File[]>([])

const showRejectModal = ref(false)
const showAppealModal = ref(false)
const showMessageDialog = ref(false)
const showMediaPreview = ref(false)
const mediaPreviewIndex = ref(0)

const allMediaList = computed(() => {
  const list: { type: 'image' | 'video'; url: string; cover?: string }[] = []
  parseEvidenceImages(refundData.value?.evidenceImages || '').forEach(url => {
    list.push({ type: 'image', url })
  })
  refundData.value?.videos?.forEach(v => {
    list.push({ type: 'video', url: v.videoUrl, cover: v.coverUrl })
  })
  chatHistory.value.forEach(msg => {
    if (msg.images) {
      msg.images.forEach(img => {
        const url = getImageUrl(img)
        if (url) list.push({ type: 'image', url })
      })
    }
  })
  return list
})

const showHistory = ref(true)
const sellerDecision = ref('')

const approving = ref(false)
const rejecting = ref(false)
const confirmingReceive = ref(false)

const rejectReason = ref('')
const appealContent = ref('')
const appealImages = ref<string[]>([])

const parseEvidenceImages = (images: string | string[]): string[] => {
  if (!images) return []
  if (Array.isArray(images)) return images
  try {
    const parsed = JSON.parse(images)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return images.split(',').filter(img => img.trim())
  }
}

const getImageUrl = (img: unknown): string => {
  if (typeof img === 'string') return img
  if (img && typeof img === 'object') {
    return (img as { image?: string }).image || (img as { url?: string }).url || ''
  }
  return ''
}

const formatPrice = (price: number): string => {
  return price.toFixed(2)
}

const formatDateTime = (dateTime: string): string => {
  if (!dateTime) return '-'
  return dateTime.replace('T', ' ').substring(0, 16)
}

const formatRelativeTime = (dateTime: string): string => {
  if (!dateTime) return '-'
  const date = new Date(dateTime)
  const now = new Date()
  const diffMs = now.getTime() - date.getTime()
  const minutes = Math.floor(diffMs / (1000 * 60))
  const hours = Math.floor(diffMs / (1000 * 60 * 60))
  const days = Math.floor(diffMs / (1000 * 60 * 60 * 24))

  if (minutes < 60) return '刚刚'
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  return formatDateTime(dateTime)
}

const getWaitDuration = (applyTime: string): string => {
  if (!applyTime) return ''
  const applyDate = new Date(applyTime)
  const now = new Date()
  const diffMs = now.getTime() - applyDate.getTime()
  const hours = Math.floor(diffMs / (1000 * 60 * 60))
  if (hours < 1) return '已等待不到1小时'
  if (hours < 24) return `已等待 ${hours} 小时`
  const days = Math.floor(hours / 24)
  return `已等待 ${days} 天 ${hours % 24} 小时`
}

const getStatusText = (status: string): string => {
  const isReturn = refundData.value?.refundType === 'RETURN' || refundData.value?.refundType === 'AFTER_SALE'
  const returnStatus = refundData.value?.returnStatus
  const waitDuration = getWaitDuration(refundData.value?.applyTime || '')

  if (status === 'FAILED') return '已拒绝'
  if (status === 'SUCCESS') return '已退款'

  if (isReturn && returnStatus) {
    const returnStatusMap: Record<string, string> = {
      WAITING_RETURN: '待退货',
      RETURNING: '退货中',
      RECEIVED: '已收货'
    }
    if (returnStatusMap[returnStatus]) {
      return returnStatusMap[returnStatus]
    }
  }

  const map: Record<string, string> = {
    PROCESSING: `商家处理中 · ${waitDuration}`,
    FAILED: '已拒绝',
    SUCCESS: '已退款',
    WAITING_RETURN: '待退货',
    RETURNING: '退货中'
  }
  return map[status] || status
}

// 判断是否显示完成卡片
const isCompleted = computed(() => {
  const status = refundData.value?.refundStatus
  if (status === 'SUCCESS') return true
  return false
})

// 判断商家端是否完成（隐藏操作栏）
const isSellerCompleted = computed(() => {
  const status = refundData.value?.refundStatus
  const returnStatus = refundData.value?.returnStatus

  if (status === 'FAILED') return true

  if (returnStatus === 'RETURNING') return false

  return isCompleted.value
    || status === 'WAITING_RETURN'
})

// 判断是否可以再次申请退款（同订单项无进行中退款且申请次数<3）
const canReapply = ref(true)

// 判断买家端是否完成（隐藏操作栏）
const isBuyerCompleted = computed(() => {
  const status = refundData.value?.refundStatus
  const returnStatus = refundData.value?.returnStatus

  if (returnStatus === 'RETURNING') return false

  return isCompleted.value
    || status === 'FAILED'
    || returnStatus === 'RECEIVED'
    || status === 'WAITING_RETURN'
})

// 格式化预计到账时间
const getEstimatedTime = (): string => {
  if (!refundData.value?.applyTime) return '3-7个工作日'
  const applyDate = new Date(refundData.value.applyTime)
  applyDate.setDate(applyDate.getDate() + 7)
  return `${applyDate.getMonth() + 1}月${applyDate.getDate()}日`
}

const getStatusClass = (status: string): string => {
  const isReturn = refundData.value?.refundType === 'RETURN' || refundData.value?.refundType === 'AFTER_SALE'
  const returnStatus = refundData.value?.returnStatus

  if (isReturn && returnStatus) {
    const returnClassMap: Record<string, string> = {
      WAITING_RETURN: 'waiting',
      RETURNING: 'processing',
      RECEIVED: 'processing'
    }
    if (returnClassMap[returnStatus]) {
      return returnClassMap[returnStatus]
    }
  }

  const map: Record<string, string> = {
    PROCESSING: 'processing',
    FAILED: 'failed',
    SUCCESS: 'completed',
    WAITING_RETURN: 'waiting',
    RETURNING: 'processing'
  }
  return map[status] || ''
}

const getStatusIcon = (status: string): string => {
  const isReturn = refundData.value?.refundType === 'RETURN' || refundData.value?.refundType === 'AFTER_SALE'
  const returnStatus = refundData.value?.returnStatus

  if (isReturn && returnStatus) {
    const returnIconMap: Record<string, string> = {
      WAITING_RETURN: '📦',
      RETURNING: '🚚',
      RECEIVED: '📥'
    }
    if (returnIconMap[returnStatus]) {
      return returnIconMap[returnStatus]
    }
  }

  const map: Record<string, string> = {
    PROCESSING: '⏳',
    FAILED: '✗',
    SUCCESS: '✓',
    WAITING_RETURN: '📦',
    RETURNING: '🚚'
  }
  return map[status] || '📋'
}

const goBack = (): void => {
  router.back()
}

const loadRefundDetail = async (): Promise<void> => {
  const refundId = Number(route.params.refundId)
  if (!refundId) {
    loading.value = false
    return
  }

  try {
    const response = await authAPI.getRefundDetail(refundId)
    if (response.success && response.data) {
      refundData.value = response.data
      await loadChatHistory(refundId)
      updateStepFromStatus(response.data)

      if (response.data.refundStatus === 'FAILED' && response.data.orderItemId) {
        await checkReapplyStatus(response.data.orderItemId)
      }
    }
  } catch (error) {
    console.error('加载退款详情失败', error)
    Message.error('加载退款详情失败')
  } finally {
    loading.value = false
    parentLoading.value = false
  }
}

const updateStepFromStatus = (data: RefundData): void => {
  const status = data.refundStatus
  const returnStatus = data.returnStatus
  const type = data.refundType

  refundType.value = (type === 'AFTER_SALE' || type === 'RETURN') ? 'AFTER_SALE' : 'REFUND'

  let step = 2
  if (refundType.value === 'AFTER_SALE') {
    if (status === 'WAITING_RETURN') {
      step = 3
    }
    if (returnStatus === 'RETURNING') {
      step = 3
    }
    if (status === 'SUCCESS') {
      step = 4
    }
  } else {
    if (status === 'SUCCESS') {
      step = 3
    }
  }
  currentStep.value = step
}

const loadChatHistory = async (refundId: number): Promise<void> => {
  try {
    const response = await authAPI.getRefundChatHistory(refundId)
    if (response.success && response.data) {
      chatHistory.value = response.data
    }
  } catch (error) {
    console.error('加载聊天记录失败', error)
  }
}

const toggleHistory = (): void => {
  showHistory.value = !showHistory.value
}

const handleIntervene = async (): Promise<void> => {
  try {
    await Message.confirm('确定要申请平台介入吗？平台将在24小时内处理', '平台介入')

    const response = await authAPI.applyIntervene(refundData.value?.id as number)
    if (response.success) {
      Message.success(response.message || '已申请平台介入')
    } else {
      Message.error(response.message || '申请失败')
    }
  } catch {
  }
}

const checkRefundCount = async (orderItemId: number): Promise<boolean> => {
  try {
    const res = await authAPI.getSellerRefundsByOrderItem(orderItemId)
    if (res.success && res.data) {
      if (res.data.length >= 3) {
        Message.warning('已超过申请上限（3次），无法再次申请')
        return false
      }

      const hasActiveRefund = res.data.some((refund: { refundStatus: string }) => {
        const status = refund.refundStatus
        return status === 'PROCESSING' || status === 'WAITING_RETURN' || status === 'RETURNING'
      })
      if (hasActiveRefund) {
        Message.warning('当前订单项存在处理中的售后订单，无法再次申请')
        return false
      }
    }
    return true
  } catch {
    Message.error('检查退款记录失败')
    return false
  }
}

const checkReapplyStatus = async (orderItemId: number): Promise<void> => {
  try {
    const res = await authAPI.getSellerRefundsByOrderItem(orderItemId)
    if (res.success && res.data) {
      if (res.data.length >= 3) {
        canReapply.value = false
        return
      }

      const hasActiveRefund = res.data.some((refund: { refundStatus: string }) => {
        const status = refund.refundStatus
        return status === 'PROCESSING' || status === 'WAITING_RETURN' || status === 'RETURNING'
      })
      if (hasActiveRefund) {
        canReapply.value = false
        return
      }
    }
    canReapply.value = true
  } catch {
    canReapply.value = false
  }
}

const handleReapply = async (): Promise<void> => {
  if (!refundData.value?.orderItemId) {
    Message.error('订单项信息不存在')
    return
  }

  const canApply = await checkRefundCount(refundData.value.orderItemId)
  if (!canApply) return

  router.push({
    name: 'RefundApply',
    params: { orderItemId: String(refundData.value.orderItemId) }
  })
}

const handleApproveRefund = async (): Promise<void> => {
  if (approving.value) return

  const confirmMsg = refundData.value?.refundType === 'AFTER_SALE'
    ? '同意退货后等待买家寄回商品，确认同意？'
    : '同意后退款将原路返回给买家，确认同意？'

  try {
    await Message.confirm(confirmMsg, '确认同意退款')

    approving.value = true
    const response = await authAPI.approveRefund(Number(route.params.refundId), '')
    if (response.success) {
      Message.success('已同意退款')
      await loadRefundDetail()

      const refundType = refundData.value?.refundType
      if (refundType === 'REFUND') {
        setTimeout(() => {
          if (isSeller.value) {
            router.push({ name: 'SellerOrders', query: { status: 'REFUNDED' } })
          } else {
            router.push({ name: 'AfterSaleList' })
          }
        }, 1500)
      }
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      Message.error(error.message || '操作失败')
    }
  } finally {
    approving.value = false
  }
}

const viewReturnLogistics = (): void => {
  if (!refundData.value) return
  const routeName = isSeller.value ? 'SellerLogistics' : 'UserLogistics'
  router.push({
    name: routeName,
    query: {
      trackingNumber: refundData.value.returnTrackingNumber || '',
      logisticsName: refundData.value.returnLogisticsName || '',
      refundId: String(refundData.value.id),
      type: 'return',
      sellerId: String(refundData.value.sellerId),
      userId: String(refundData.value.buyerId)
    }
  })
}

const handleConfirmReceive = async (): Promise<void> => {
  if (confirmingReceive.value || !refundData.value) return

  try {
    await Message.confirm('确认已收到退货商品？确认后将自动处理退款。', '确认退款')

    confirmingReceive.value = true
    const response = await authAPI.confirmReceiveAndRefund(Number(refundData.value.id))
    if (response.success) {
      Message.success('已确认收货并退款')
      await loadRefundDetail()

      setTimeout(() => {
        if (isSeller.value) {
          router.push({ name: 'SellerOrders', query: { status: 'REFUNDED' } })
        } else {
          router.push({ name: 'AfterSaleList' })
        }
      }, 1500)
    } else {
      Message.error(response.message || '操作失败')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      Message.error('操作失败')
    }
  } finally {
    confirmingReceive.value = false
  }
}

const handleReject = async (): Promise<void> => {
  if (rejecting.value) return
  rejecting.value = true

  try {
    const response = await authAPI.rejectRefund(Number(refundData.value?.orderId || 0), Number(route.params.refundId), rejectReason.value)
    if (response.success) {
      Message.success('退款已拒绝')
      await loadRefundDetail()

      setTimeout(() => {
        if (isSeller.value) {
          router.push({ name: 'SellerOrders', query: { status: 'REFUNDING' } })
        } else {
          router.push({ name: 'AfterSaleList' })
        }
      }, 1500)
    } else {
      Message.error(response.message || '操作失败')
    }
  } catch (error) {
    Message.error('操作失败')
  } finally {
    rejecting.value = false
    rejectReason.value = ''
    sellerDecision.value = ''
  }
}

const closeRejectModal = (): void => {
  showRejectModal.value = false
}

const confirmReject = async (): Promise<void> => {
  if (!rejectReason.value.trim()) {
    Message.error('请输入拒绝原因')
    return
  }
  handleReject()
  closeRejectModal()
}

const closeAppealModal = (): void => {
  showAppealModal.value = false
}

const confirmAppeal = async (): Promise<void> => {
  if (!appealContent.value.trim()) {
    Message.error('请输入申诉理由')
    return
  }

  try {
    const response = await authAPI.sendRefundMessage(Number(route.params.refundId), {
      message: appealContent.value,
      images: JSON.stringify(appealImages.value)
    })
    if (response.success) {
      Message.success('申诉已提交')
      await loadRefundDetail()
      await loadChatHistory(Number(route.params.refundId))
      closeAppealModal()
    } else {
      Message.error(response.message || '操作失败')
    }
  } catch (error) {
    Message.error('操作失败')
  }
}

const uploadEvidence = (): void => {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = 'image/*'
  input.multiple = true
  input.onchange = async (e) => {
    const files = (e.target as HTMLInputElement).files
    if (!files) return
    for (const file of Array.from(files)) {
      const formData = new FormData()
      formData.append('file', file)
      try {
        const res = await authAPI.uploadFile(formData)
        if (res.success) {
          appealImages.value.push(res.data.url)
        } else {
          Message.error(res.message || '上传失败')
        }
      } catch {
        Message.error('上传失败')
      }
    }
  }
  input.click()
}

const uploadImage = (): void => {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = 'image/*'
  input.multiple = true
  input.onchange = (e) => {
    const files = (e.target as HTMLInputElement).files
    if (!files) return
    for (const file of Array.from(files)) {
      selectedImages.value.push(URL.createObjectURL(file))
      selectedImageFiles.value.push(file)
    }
  }
  input.click()
}

const sendMessage = async (): Promise<void> => {
  if (!messageInput.value.trim()) return

  try {
    const formData = new FormData()
    formData.append('message', messageInput.value.trim())
    selectedImageFiles.value.forEach(file => {
      formData.append('images', file)
    })

    const response = await authAPI.sendRefundMessageWithFiles(Number(route.params.refundId), formData)
    if (response.success) {
      Message.success('消息发送成功')
      messageInput.value = ''
      selectedImages.value = []
      selectedImageFiles.value = []
      showMessageDialog.value = false
      await loadChatHistory(Number(route.params.refundId))
    } else {
      Message.error(response.message || '发送失败')
    }
  } catch (error) {
    Message.error('发送失败')
  }
}

const previewImage = (url: string): void => {
  mediaPreviewIndex.value = allMediaList.value.findIndex(item => item.url === url)
  showMediaPreview.value = true
}

const previewVideo = (url: string): void => {
  mediaPreviewIndex.value = allMediaList.value.findIndex(item => item.url === url)
  showMediaPreview.value = true
}

const handleRefreshChat = (event: Event) => {
  const detail = (event as CustomEvent).detail as { refundId: number }
  if (detail && detail.refundId) {
    const currentRefundId = Number(route.params.refundId)
    if (currentRefundId === detail.refundId) {
      loadChatHistory(currentRefundId)
    }
  }
}

const handleReturnSubmitted = (event: Event) => {
  const data = (event as CustomEvent).detail
  if (data && data.refundId) {
    const currentRefundId = Number(route.params.refundId)
    if (currentRefundId === Number(data.refundId)) {
      loadRefundDetail()
    }
  }
}

const handleStatusUpdate = (status: string): void => {
  if (status === 'SUCCESS') {
    setTimeout(() => {
      if (isSeller.value) {
        router.push({ name: 'SellerOrders', query: { status: 'REFUNDED' } })
      } else {
        router.push({ name: 'AfterSaleList' })
      }
    }, 1500)
  }
}

const handleRefundUpdate = () => {
  loadRefundDetail()
  if (refundData.value?.refundStatus === 'SUCCESS') {
    handleStatusUpdate('SUCCESS')
  }
}

const handleRefundChatMessage = (event: Event) => {
  const data = (event as CustomEvent).detail as { refundId: number }
  if (data && data.refundId) {
    const currentRefundId = Number(route.params.refundId)
    if (currentRefundId === data.refundId) {
      authAPI.getRefundDetail(currentRefundId).then(response => {
        if (!response.success || !response.data) return

        const latestData = response.data
        const status = latestData.refundStatus
        const type = latestData.refundType
        const returnStatus = latestData.returnStatus

        // 买家端 + 退货退款 + 待退货 → 跳转退货页（退货中状态时不自动跳转）
        if (!isSeller.value && type === 'AFTER_SALE' && status === 'WAITING_RETURN' && returnStatus !== 'RETURNING') {
          router.replace({
            name: 'ReturnGoods',
            params: {
              refundId: String(latestData.id),
              orderItemId: String(latestData.orderItemId)
            }
          })
          return
        }

        // 退款成功后自动跳转
        if (status === 'SUCCESS') {
          loadRefundDetail()
          loadChatHistory(currentRefundId)
          handleStatusUpdate(status)
          return
        }

        // 其他情况刷新数据和聊天
        loadRefundDetail()
        loadChatHistory(currentRefundId)
      }).catch(() => {
        // API调用失败时刷新页面数据
        loadRefundDetail()
        loadChatHistory(currentRefundId)
      })
    }
  }
}

onMounted(() => {
  loadRefundDetail()
  window.addEventListener('refresh-refund-chat', handleRefreshChat)
  window.addEventListener('refund-chat-message', handleRefundChatMessage)
  window.addEventListener('return-submitted', handleReturnSubmitted)
  window.addEventListener('refund-update', handleRefundUpdate)
})

onUnmounted(() => {
  window.removeEventListener('refresh-refund-chat', handleRefreshChat)
  window.removeEventListener('refund-chat-message', handleRefundChatMessage)
  window.removeEventListener('return-submitted', handleReturnSubmitted)
  window.removeEventListener('refund-update', handleRefundUpdate)
})
</script>

<style scoped>
@import url('@/static/css/user/退款售后');

</style>
