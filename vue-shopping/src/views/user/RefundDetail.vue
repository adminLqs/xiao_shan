<template>
  <div class="refund-detail-page page-container">
    <div v-if="loading" class="refund-skeleton">
      <div class="skeleton-status-card"></div>
      <div class="skeleton-card">
        <div class="skeleton-line short"></div>
        <div class="skeleton-line"></div>
        <div class="skeleton-line"></div>
        <div class="skeleton-line"></div>
      </div>
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
    </div>

    <div v-else-if="refundData" class="refund-content">
      <div class="status-card" :class="getStatusClass(refundData.refundStatus)">
        <div class="status-dot"></div>
        <div class="status-info">
          <div class="status-text">{{ getStatusText(refundData.refundStatus) }}</div>
          <div class="status-desc">{{ getStatusDesc(refundData.refundStatus) }}</div>
        </div>
      </div>

      <div class="detail-card" v-if="refundData.productName">
        <div class="card-header">
          <i class="fas fa-box"></i>
          <span class="card-title">商品信息</span>
        </div>
        <div class="card-body">
          <div class="product-item">
            <img :src="refundData.productImage || defaultProductImage" class="product-image">
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
            <span class="info-value">{{ refundData.refundType === 'REFUND' ? '仅退款' : '退货退款' }}</span>
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
        </div>
      </div>

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

      <div v-if="refundData.returnTrackingNumber || refundData.returnLogisticsName" class="detail-card">
        <div class="card-header">
          <i class="fas fa-truck"></i>
          <span class="card-title">退货物流</span>
        </div>
        <div class="card-body">
          <div class="info-row">
            <span class="info-label">物流公司</span>
            <span class="info-value">{{ refundData.returnLogisticsName || '-' }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">物流单号</span>
            <span class="info-value">{{ refundData.returnTrackingNumber || '-' }}</span>
          </div>
        </div>
      </div>
    </div>

    <div v-else class="empty-state">
      <i class="fas fa-search"></i>
      <p>退款记录不存在</p>
      <button class="btn-primary" @click="router.back()">返回</button>
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
import { ref, computed, onMounted } from 'vue'
import ImagePreview from '@/components/ImagePreview.vue'
import { useRoute, useRouter } from 'vue-router'
import { authAPI } from '@/api/authAPI'
import Message from '@/utils/message'
import defaultProductImage from '@/static/images/云杉购图标.jpg'

const route = useRoute()
const router = useRouter()

interface RefundVideo {
  id: number
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

const loading = ref(true)
const refundData = ref<RefundData | null>(null)
const showMediaPreview = ref(false)
const mediaPreviewIndex = ref(0)

const allMediaList = computed(() => {
  const list: { type: 'image' | 'video'; url: string; cover?: string }[] = []
  if (refundData.value?.evidenceImages) {
    try {
      const images = JSON.parse(refundData.value.evidenceImages)
      if (Array.isArray(images)) {
        images.forEach(url => {
          list.push({ type: 'image', url })
        })
      }
    } catch {
      refundData.value.evidenceImages.split(',').filter(img => img.trim()).forEach(url => {
        list.push({ type: 'image', url })
      })
    }
  }
  refundData.value?.videos?.forEach(v => {
    list.push({ type: 'video', url: v.videoUrl, cover: v.coverUrl })
  })
  return list
})

const statusConfig: Record<string, { text: string; desc: string; class: string }> = {
  PROCESSING: {
    text: '处理中',
    desc: '商家正在审核您的退款申请',
    class: 'processing'
  },
  WAITING_RETURN: {
    text: '待退货',
    desc: '等待您退货',
    class: 'processing'
  },
  RETURNING: {
    text: '退货中',
    desc: '商品正在退回途中',
    class: 'processing'
  },
  SUCCESS: {
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
    } else {
      Message.error(response.message || '获取退款详情失败')
    }
  } catch (error: any) {
    Message.error(error.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const previewImage = (url: string) => {
  mediaPreviewIndex.value = allMediaList.value.findIndex(item => item.url === url)
  showMediaPreview.value = true
}

const previewVideo = (url: string) => {
  mediaPreviewIndex.value = allMediaList.value.findIndex(item => item.url === url)
  showMediaPreview.value = true
}

onMounted(() => {
  loadRefundDetail()
})
</script>

<style scoped>
@import url('@/static/css/user/退款售后.css');
</style>
