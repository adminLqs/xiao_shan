<template>
  <div class="refund-chat-page page-container">
    <div class="page-navbar">
      <button class="page-nav-back" @click="router.back()">
        <i class="fas fa-chevron-left"></i>
      </button>
      <div class="page-nav-title">
        <i class="fas fa-comments"></i>
        <span>退款沟通</span>
      </div>
      <div class="page-nav-right"></div>
    </div>

    <!-- 加载状态 - 骨架屏 -->
    <div v-if="loading" class="refund-skeleton">
      <!-- 状态卡片骨架 -->
      <div class="skeleton-status-card"></div>

      <!-- 退款信息卡片骨架 -->
      <div class="skeleton-card">
        <div class="skeleton-line short"></div>
        <div class="skeleton-line"></div>
        <div class="skeleton-line"></div>
        <div class="skeleton-line"></div>
      </div>

      <!-- 商品信息卡片骨架 -->
      <div class="skeleton-card">
        <div class="skeleton-line short"></div>
        <div class="skeleton-product">
          <div class="skeleton-product-image"></div>
          <div class="skeleton-product-detail">
            <div class="skeleton-line"></div>
            <div class="skeleton-line short"></div>
          </div>
        </div>
      </div>

      <!-- 退款申请卡片骨架 -->
      <div class="skeleton-card">
        <div class="skeleton-line short"></div>
        <div class="skeleton-line"></div>
        <div class="skeleton-line"></div>
      </div>
    </div>

    <!-- 退款详情 -->
    <div v-else-if="refundData" class="refund-content">

      <!-- 状态卡片 -->
      <div class="status-card" :class="getStatusClass(refundData.refundStatus)">
        <div class="status-dot"></div>
        <div class="status-info">
          <div class="status-text">{{ getStatusText(refundData.refundStatus) }}</div>
          <div class="status-desc">{{ getStatusDescDynamic(refundData.refundStatus) }}</div>
        </div>
      </div>

      <!-- 商品信息卡片 -->
      <div class="detail-card" v-if="refundData.productName">
        <div class="card-header">
          <i class="fas fa-box"></i>
          <span class="card-title">商品信息</span>
        </div>
        <div class="card-body">
          <div class="product-item">
            <img :src="refundData.productImage || '/images/default-product.png'" class="product-image">
            <div class="product-info">
              <div class="product-name">{{ refundData.productName }}</div>
              <div class="tags-group">
                <span v-if="refundData.skuName" class="tag-spec">{{ refundData.skuName }}</span>
                <span class="tag-quantity">x{{ refundData.quantity || 0 }}</span>
              </div>
            </div>
            <div class="product-price">¥{{ formatPrice(refundData.price ?? 0) }}</div>
          </div>
        </div>
      </div>

      <!-- 退款信息卡片 -->
      <div class="detail-card">
        <div class="card-header">
          <i class="fas fa-info-circle"></i>
          <span class="card-title">退款信息</span>
        </div>
        <div class="card-body">
          <div class="info-row">
            <span class="info-label">退款编号</span>
            <span class="info-value">{{ refundData.orderNumber || '-' }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">退款类型</span>
            <span class="info-value">{{ refundData.refundType === 'REFUND' ? '退款' : '售后' }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">退款金额</span>
            <span class="info-value price">¥{{ formatPrice(refundData.amount || refundData.refundAmount) }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">退款原因</span>
            <span class="info-value">{{ refundData.reason || refundData.refundReason || '-' }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">申请时间</span>
            <span class="info-value">{{ formatDateTime(refundData.applyTime) }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">沟通轮次</span>
            <span class="info-value">{{ refundData.communicationRound }} / 3</span>
          </div>
        </div>
      </div>

      <!-- 买家申请理由和凭证 -->
      <div class="detail-card">
        <div class="card-header">
          <i class="fas fa-file-text"></i>
          <span class="card-title">退款申请</span>
        </div>
        <div class="card-body">
          <div class="reason-section">
            <div class="reason-title">申请理由</div>
            <div class="reason-content">{{ refundData.description || '-' }}</div>
          </div>
          <div v-if="(parseEvidenceImages(refundData.evidenceImages).length > 0 || (refundData.videos && refundData.videos.length > 0))" class="evidence-section">
            <div class="evidence-title">凭证图片/视频</div>
            <div class="evidence-media">
              <img
                v-for="(img, index) in parseEvidenceImages(refundData.evidenceImages)"
                :key="'img-' + index"
                :src="img"
                class="evidence-image"
                @click="previewImage(img)"
              />
              <div
                v-for="video in (refundData.videos || [])"
                :key="'video-' + video.id"
                class="evidence-video"
                @click="previewVideo(video.videoUrl)"
              >
                <img :src="video.coverUrl || '/images/video-placeholder.png'" class="evidence-video-cover" />
                <div class="video-play-btn"><i class="fas fa-play"></i></div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 沟通记录 -->
      <div class="detail-card">
        <div class="card-header">
          <i class="fas fa-comments"></i>
          <span class="card-title">沟通记录</span>
        </div>
        <div class="card-body">
          <div v-if="chatHistory.length === 0" class="empty-chat">
            <p>暂无沟通记录</p>
          </div>
          <div v-else class="timeline-list">
            <div
              v-for="msg in chatHistory"
              :key="msg.id"
              class="timeline-item"
            >
              <div class="timeline-dot"></div>
              <div class="timeline-card">
                <div class="timeline-header">
                  <span class="timeline-user">{{ msg.senderName || (msg.senderType === 'SELLER' ? '商家' : '买家') }}</span>
                  <span class="timeline-time">{{ formatDateTime(msg.sendTime) }}</span>
                </div>
                <div v-if="msg.message && msg.message !== '[图片]'" class="timeline-content">{{ msg.message }}</div>
                <div v-if="msg.images && msg.images.length > 0" class="timeline-images">
                  <img
                    v-for="(img, i) in msg.images"
                    :key="i"
                    :src="img"
                    @click="previewImage(img)"
                  />
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 底部固定操作栏 -->
      <div class="bottom-action-bar">
        <template v-if="isSellerInOrder && refundData.refundStatus === 'PROCESSING'">
          <button class="btn-reject" @click="showRejectModal = true">拒绝</button>
          <button class="btn-message" @click="showMessageDialog = true"><i class="fas fa-comment"></i> 发消息</button>
          <button class="btn-approve" @click="showApproveModal = true">同意</button>
        </template>

        <template v-if="isBuyerInOrder && refundData.refundStatus === 'PROCESSING' && refundData.communicationRound < 3">
          <button class="btn-message" @click="showMessageDialog = true"><i class="fas fa-comment"></i> 发消息</button>
        </template>

        <template v-if="isBuyerInOrder && refundData.refundStatus === 'FAILED' && refundData.communicationRound < 3">
          <button class="btn-appeal" @click="showAppealModal = true">申诉</button>
        </template>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else class="empty-state">
      <i class="fas fa-search"></i>
      <p>退款记录不存在</p>
      <button class="btn-primary" @click="goBack">返回</button>
    </div>

    <!-- 拒绝退款弹窗 -->
    <div v-if="showRejectModal" class="modal-overlay" @click="closeRejectModal">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>拒绝退款</h3>
          <button class="modal-close" @click="closeRejectModal">
            <i class="fas fa-times"></i>
          </button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label class="form-label">拒绝原因</label>
            <textarea
              v-model="rejectReason"
              class="form-control"
              placeholder="请输入拒绝退款的原因..."
              :maxlength="500"
            ></textarea>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-cancel" @click="closeRejectModal">取消</button>
          <button
            class="btn-confirm btn-danger"
            :disabled="!rejectReason.trim() || rejecting"
            @click="confirmReject"
          >
            {{ rejecting ? '处理中...' : '确认拒绝' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 同意退款弹窗 -->
    <div v-if="showApproveModal" class="modal-overlay" @click="closeApproveModal">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>{{ refundData?.refundType === 'AFTER_SALE' ? '同意退货' : '同意退款' }}</h3>
          <button class="modal-close" @click="closeApproveModal">
            <i class="fas fa-times"></i>
          </button>
        </div>
        <div class="modal-body">
          <p v-if="refundData?.refundType === 'AFTER_SALE'">
            确认同意退货申请，等待买家寄回商品？
          </p>
          <p v-else>
            确认同意退款 ¥{{ formatPrice(refundData?.refundAmount ?? 0) }} 给买家？
          </p>
          <div class="form-group">
            <label class="form-label">备注（可选）</label>
            <textarea
              v-model="approveNotes"
              class="form-control"
              placeholder="添加备注..."
              :maxlength="200"
            ></textarea>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-cancel" @click="closeApproveModal">取消</button>
          <button class="btn-confirm" @click="confirmApprove" :disabled="approving">
            {{ approving ? '处理中...' : '确认同意' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 申诉弹窗 -->
    <div v-if="showAppealModal" class="modal-overlay" @click="closeAppealModal">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>提交申诉</h3>
          <button class="modal-close" @click="closeAppealModal">
            <i class="fas fa-times"></i>
          </button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label class="form-label">申诉理由</label>
            <textarea
              v-model="appealContent"
              class="form-control"
              placeholder="请详细说明您的申诉理由..."
              :maxlength="500"
            ></textarea>
          </div>
          <div class="form-group">
            <label class="form-label">申诉凭证（可选）</label>
            <button class="btn-upload-evidence" @click="uploadEvidence">
              <i class="fas fa-plus"></i>
              添加图片凭证
            </button>
            <div v-if="appealImages.length > 0" class="evidence-preview">
              <img
                v-for="(img, index) in appealImages"
                :key="index"
                :src="img"
                class="preview-image"
              >
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-cancel" @click="closeAppealModal">取消</button>
          <button
            class="btn-confirm"
            :disabled="!appealContent.trim()"
            @click="confirmAppeal"
          >提交申诉</button>
        </div>
      </div>
    </div>

    <!-- 消息输入弹窗 -->
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
              <img v-for="(img, i) in selectedImages" :key="i" :src="img" class="preview-thumb" />
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-cancel" @click="showMessageDialog = false">取消</button>
          <button class="btn-send" @click="sendMessage" :disabled="!messageInput.trim()">发送</button>
        </div>
      </div>
    </div>

    <!-- 图片预览弹窗 -->
    <div v-if="previewImageUrl" class="modal-overlay" @click="previewImageUrl = ''">
      <div class="image-preview-modal">
        <img :src="previewImageUrl" class="preview-img">
      </div>
    </div>

    <!-- 视频预览弹窗 -->
    <div v-if="previewVideoUrl" class="modal-overlay" @click="previewVideoUrl = ''">
      <div class="video-preview-modal">
        <video :src="previewVideoUrl" class="preview-video" controls autoplay></video>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { authAPI } from '@/api/authAPI'
import Message from '@/utils/message'
import defaultAvatar from '@/static/images/user-avatar.jpg'
import sellerDefaultAvatar from '@/static/images/seller-avatar.jpg'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const currentUserId = computed(() => authStore.userId)
const isSeller = computed(() => authStore.isSeller)
const isSellerInOrder = computed(() => refundData.value?.sellerId === currentUserId.value)
const isBuyerInOrder = computed(() => refundData.value?.buyerId === currentUserId.value || refundData.value?.userId === currentUserId.value)
const hasPermission = computed(() => isSellerInOrder.value || isBuyerInOrder.value)

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
  communicationRound: number
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
  skuName?: string
}

interface ChatMessage {
  id: number
  refundId: number
  senderType: string
  senderId: number
  message: string
  images: string[]
  sendTime: string
  messageType: string
  senderAvatar?: string
  senderName?: string
}

const loading = ref(true)
const refundData = ref<RefundData | null>(null)
const chatHistory = ref<ChatMessage[]>([])

// 不再需要聊天区域滚动引用

const messageInput = ref('')
const selectedImages = ref<string[]>([])
const selectedImageFiles = ref<File[]>([])

const showRejectModal = ref(false)
const showApproveModal = ref(false)
const showAppealModal = ref(false)
const showMessageDialog = ref(false)
const previewImageUrl = ref('')
const previewVideoUrl = ref('')

// 按钮防抖状态
const approving = ref(false)
const rejecting = ref(false)

const rejectReason = ref('')
const approveNotes = ref('')
const appealContent = ref('')
const appealImages = ref<string[]>([])

const statusConfig: Record<string, { text: string; desc: string; class: string }> = {
  PROCESSING: {
    text: '处理中',
    desc: '商家正在审核您的退款申请',
    class: 'processing'
  },
  REFUNDING: {
    text: '退款中',
    desc: '退款正在处理中',
    class: 'processing'
  },
  AFTER_SALE: {
    text: '售后中',
    desc: '售后申请处理中',
    class: 'processing'
  },
  WAITING_RETURN: {
    text: '待退货',
    desc: '等待您退货',
    class: 'processing'
  },
  RETURNING: {
    text: '待退货',
    desc: '等待您退货',
    class: 'processing'
  },
  APPROVED: {
    text: '已同意',
    desc: '商家已同意申请',
    class: 'processing'
  },
  SUCCESS: {
    text: '已完成',
    desc: '退款已成功到账',
    class: 'success'
  },
  COMPLETED: {
    text: '已退款',
    desc: '退款已成功到账',
    class: 'success'
  },
  FAILED: {
    text: '已拒绝',
    desc: '商家已拒绝您的退款申请',
    class: 'failed'
  }
}

const parseEvidenceImages = (images: string | string[] | undefined): string[] => {
  if (!images) return []
  if (Array.isArray(images)) return images
  if (typeof images === 'string') {
    try {
      const parsed = JSON.parse(images)
      if (Array.isArray(parsed)) {
        return parsed
      }
    } catch {
    }
    return images.split(',').filter(img => img.trim())
  }
  return []
}

const getStatusConfig = (status: string) => {
  return statusConfig[status] || statusConfig.PROCESSING
}

const getStatusClass = (status: string) => getStatusConfig(status)?.class || ''
const getStatusText = (status: string) => getStatusConfig(status)?.text || ''
const getStatusDesc = (status: string) => getStatusConfig(status)?.desc || ''

const getStatusDescDynamic = (status: string) => {
  const config = getStatusConfig(status)
  if (!config) return ''

  if (status === 'APPROVED') {
    if (refundData.value?.refundType === 'AFTER_SALE') {
      return '商家已同意退货，请填写退货信息'
    } else {
      return '商家已同意退款，正在处理中'
    }
  }

  return config.desc
}

const formatPrice = (price: number | undefined | null) => {
  if (price == null) return '0.00'
  return price.toFixed(2)
}

const formatDateTime = (dateStr: string) => {
  if (!dateStr) return '-'
  try {
    const date = new Date(dateStr)
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    const hours = String(date.getHours()).padStart(2, '0')
    const minutes = String(date.getMinutes()).padStart(2, '0')
    return `${year}-${month}-${day} ${hours}:${minutes}`
  } catch {
    return '-'
  }
}

const loadRefundDetail = async () => {
  loading.value = true
  try {
    const refundId = route.params.refundId
    if (!refundId) {
      Message.error('参数错误')
      return
    }

    const response = await authAPI.getRefundDetail(Number(refundId))

    if (response.success && response.data) {
      refundData.value = response.data as RefundData

      if (isSeller.value) {
        const sellerId = refundData.value?.sellerId
        if (!sellerId || sellerId !== currentUserId.value) {
          Message.error('您无权查看此退款')
          setTimeout(() => {
            router.back()
          }, 1500)
          return
        }
      }
    } else {
      Message.error(response.message || '获取退款详情失败')
    }

    await loadChatHistory(Number(refundId))

  } catch (error: any) {
    Message.error(error.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const loadChatHistory = async (refundId: number) => {
  try {
    const response = await authAPI.getRefundChatHistory(refundId)
    if (response.success && response.data) {
      chatHistory.value = response.data as ChatMessage[]
    }
  } catch (error: any) {
    Message.error('加载聊天记录失败')
  }
}

const sendMessage = async () => {
  if (!messageInput.value.trim() && selectedImageFiles.value.length === 0) {
    Message.warning('请输入消息内容或选择图片')
    return
  }

  const refundId = route.params.refundId
  if (!refundId) return

  try {
    const formData = new FormData()
    formData.append('message', messageInput.value.trim() || '[图片]')

    selectedImageFiles.value.forEach((file, index) => {
      formData.append('images', file, file.name)
    })

    const response = await authAPI.sendRefundMessageWithFiles(Number(refundId), formData)

    if (response.success) {
      Message.success('消息发送成功')
      messageInput.value = ''
      selectedImages.value = []
      selectedImageFiles.value = []
      showMessageDialog.value = false

      await loadChatHistory(Number(refundId))

      if (refundData.value) {
        refundData.value.communicationRound = (refundData.value.communicationRound || 0) + 1
      }
    } else {
      Message.error(response.message || '发送失败')
    }
  } catch (error: any) {
    Message.error(error.message || '发送失败')
  }
}



const confirmApprove = async () => {
  // 幂等性处理，防止重复点击
  if (approving.value) return
  approving.value = true

  const refundId = route.params.refundId

  if (!refundId) {
    approving.value = false
    return
  }

  try {
    const response = await authAPI.approveRefund(Number(refundId), approveNotes.value)
    if (response.success) {
      const refundType = refundData.value?.refundType
      const successMsg = refundType === 'AFTER_SALE' ? '已同意退货' : '已同意退款'
      Message.success(successMsg)
      closeApproveModal()

      // 刷新退款详情，更新页面状态
      await loadRefundDetail()

      if (isSellerInOrder.value) {
        return
      }

      if (refundType === 'AFTER_SALE') {
        router.push({
          name: 'ReturnGoods',
          query: {
            refundId: String(refundData.value?.id),
            orderItemId: String(refundData.value?.orderItemId)
          }
        })
      } else {
        await loadRefundDetail()
      }
    } else {
      Message.error(response.message || '操作失败')
    }
  } catch (error: any) {
    Message.error(error.message || '操作失败')
  } finally {
    approving.value = false
  }
}

const confirmReject = async () => {
  // 幂等性处理，防止重复点击
  if (rejecting.value) return
  rejecting.value = true

  const refundId = route.params.refundId
  const orderId = refundData.value?.orderId

  if (!refundId || !orderId) {
    rejecting.value = false
    return
  }

  try {
    const response = await authAPI.rejectRefund(orderId, Number(refundId), rejectReason.value)
    if (response.success) {
      Message.success('退款已拒绝')
      closeRejectModal()
      await loadRefundDetail()
    } else {
      Message.error(response.message || '操作失败')
    }
  } catch (error: any) {
    Message.error(error.message || '操作失败')
  } finally {
    rejecting.value = false
  }
}

const confirmAppeal = async () => {
  const refundId = route.params.refundId
  if (!refundId) return

  try {
    const response = await authAPI.submitAppeal(Number(refundId), appealContent.value.trim(), appealImages.value.join(','))
    if (response.success) {
      Message.success('申诉提交成功')
      closeAppealModal()
      await loadRefundDetail()
    } else {
      Message.error(response.message || '提交失败')
    }
  } catch (error: any) {
    Message.error(error.message || '提交失败')
  }
}

const uploadImage = () => {
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

const removeSelectedImage = (index: number) => {
  const imageUrl = selectedImages.value[index]
  if (imageUrl) {
    URL.revokeObjectURL(imageUrl)
  }
  selectedImages.value.splice(index, 1)
  selectedImageFiles.value.splice(index, 1)
}

const uploadEvidence = () => {
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
          Message.error(res.message || '图片上传失败')
        }
      } catch (error: any) {
        Message.error(error.message || '图片上传失败')
      }
    }
  }
  input.click()
}

const previewImage = (url: string) => {
  previewImageUrl.value = url
}

const previewVideo = (url: string) => {
  previewVideoUrl.value = url
}

const closeRejectModal = () => {
  showRejectModal.value = false
  rejectReason.value = ''
}

const closeApproveModal = () => {
  showApproveModal.value = false
  approveNotes.value = ''
}

const closeAppealModal = () => {
  showAppealModal.value = false
  appealContent.value = ''
  appealImages.value = []
}



const goBack = () => {
  router.back()
}

onMounted(() => {
  loadRefundDetail()
})
</script>

<style scoped>
@import url('@/static/css/user/退款沟通页.css');
</style>
