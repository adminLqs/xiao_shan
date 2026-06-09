<template>
  <div class="refund-container page-container">
    <!-- 加载状态 - 骨架屏 -->
    <div v-if="loading" class="refund-skeleton">
      <!-- 商品卡片骨架 -->
      <div class="skeleton-card">
        <div class="skeleton-product">
          <div class="skeleton-product-image"></div>
          <div class="skeleton-product-detail">
            <div class="skeleton-line"></div>
            <div class="skeleton-line short"></div>
            <div class="skeleton-line short"></div>
          </div>
        </div>
      </div>

      <!-- 退款类型骨架 -->
      <div class="skeleton-card">
        <div class="skeleton-line short"></div>
        <div class="skeleton-type-options">
          <div class="skeleton-type-option"></div>
          <div class="skeleton-type-option"></div>
        </div>
      </div>

      <!-- 退款原因骨架 -->
      <div class="skeleton-card">
        <div class="skeleton-line short"></div>
        <div class="skeleton-line"></div>
      </div>

      <!-- 退款金额骨架 -->
      <div class="skeleton-card">
        <div class="skeleton-line short"></div>
        <div class="skeleton-line"></div>
      </div>
    </div>

    <!-- 步骤内容 -->
    <div v-if="!loading" class="refund-apply-step">
      <!-- 商品信息卡片（放在顶部） -->
      <div v-if="orderItem" class="product-card">
        <img :src="orderItem.productImage" class="product-image" :alt="orderItem.productName" />
        <div class="product-info">
          <div class="product-name">{{ orderItem.productName }}</div>
          <div class="tags-group">
            <span v-if="orderItem.skuName" class="tag-spec">{{ orderItem.skuName }}</span>
            <span class="tag-quantity">x{{ orderItem.quantity }}</span>
          </div>
        </div>
        <div class="product-price">
          <span class="refund-amount">¥{{ formatPrice(maxAmount) }}</span>
        </div>
      </div>

      <!-- 退款类型选择 -->
      <div class="apply-card">
        <div class="card-header">
          <span>退款类型</span>
        </div>
        <div class="type-options">
          <div
            class="type-option"
            :class="{ active: refundType === 'REFUND' }"
            @click="refundType = 'REFUND'"
          >
            <i class="fas fa-credit-card"></i>
            <span>仅退款</span>
          </div>
          <div
            class="type-option"
            :class="{ active: refundType === 'AFTER_SALE' }"
            @click="refundType = 'AFTER_SALE'"
          >
            <i class="fas fa-truck"></i>
            <span>退货退款</span>
          </div>
        </div>
      </div>

      <!-- 退款原因 -->
      <div class="apply-card">
        <div class="card-header">
          <span>退款原因 <span class="required">*</span></span>
        </div>
        <select v-model="refundReason" name="refundReason" class="reason-select">
          <option value="">请选择退款原因</option>
          <option value="不想要了">不想要了</option>
          <option value="质量问题">质量问题</option>
          <option value="发错货">发错货</option>
          <option value="少发货">少发货</option>
          <option value="未按约定发货">未按约定发货</option>
          <option value="其他">其他</option>
        </select>
      </div>

      <!-- 退款金额 -->
      <div class="apply-card">
        <div class="card-header">
          <span>退款金额 <span class="required">*</span></span>
        </div>
        <div class="amount-input-wrap">
          <span class="currency">¥</span>
          <input
            type="number"
            name="refundAmount"
            v-model="refundAmount"
            class="amount-input"
            placeholder="请输入退款金额"
            :max="maxAmount"
            step="0.01"
          />
          <span class="max-hint">最高 ¥{{ formatPrice(maxAmount) }}</span>
        </div>
      </div>

      <!-- 问题描述 -->
      <div class="apply-card">
        <div class="card-header">
          <span>问题描述</span>
        </div>
        <textarea
          v-model="description"
          name="description"
          class="description-textarea"
          rows="3"
          maxlength="500"
          placeholder="请描述问题（选填）"
        ></textarea>
        <div class="char-count">{{ description.length }}/500</div>
      </div>

      <!-- 上传凭证 -->
      <div class="apply-card">
        <div class="card-header">
          <span>上传凭证</span>
        </div>
        <div class="upload-area">
          <div
            v-for="(media, index) in evidenceMedia"
            :key="index"
            class="uploaded-media"
            :class="{ 'is-video': media.type === 'video', 'is-dragging': dragIndex === index }"
            @touchstart.passive="onTouchStart($event, index)"
            @touchmove="onTouchMove($event)"
            @touchend="onTouchEnd"
            @mousedown="onMouseDown($event, index)"
          >
            <img v-if="media.type === 'image'" :src="media.previewUrl" :alt="`凭证${index + 1}`" />
            <div v-else class="video-preview">
              <img :src="media.coverUrl || media.previewUrl" class="video-cover" />
              <div class="video-play-icon">
                <i class="fas fa-play"></i>
              </div>
            </div>
            <button class="remove-media" @click="removeMedia(index)">
              <i class="fas fa-times"></i>
            </button>
            <span v-if="media.type === 'video'" class="media-tag">视频</span>
          </div>

          <div
            v-if="evidenceMedia.length < 9"
            class="upload-btn"
            @click="selectImages"
          >
            <i class="fas fa-image"></i>
            <span>图片</span>
          </div>
          <div
            v-if="videoCount < 3"
            class="upload-btn video-upload-btn"
            @click="selectVideos"
          >
            <i class="fas fa-video"></i>
            <span>视频</span>
            <span v-if="videoCount > 0" class="upload-count">({{ videoCount }}/3)</span>
          </div>

          <input
            type="file"
            ref="imageInput"
            name="evidenceImage"
            class="media-input"
            accept="image/*"
            multiple
            @change="handleImageSelect"
          />
          <input
            type="file"
            ref="videoInput"
            name="evidenceVideo"
            class="media-input"
            accept="video/mp4,video/mov"
            multiple
            @change="handleVideoSelect"
          />
        </div>
        <p class="upload-hint">最多上传 9 个媒体文件，图片不超过 5MB，视频不超过 30MB（最多3个）</p>
      </div>
    </div>

    <!-- 提交按钮 -->
    <button
      class="submit-btn"
      @click="submitRefund"
      :disabled="submitting || submitCountdown > 0 || !canSubmit"
    >
      <i v-if="submitting" class="fas fa-spinner fa-spin"></i>
      {{ submitting ? '提交中...' : (submitCountdown > 0 ? `${submitCountdown}秒后重试` : '提交申请') }}
    </button>

    <!-- 底部留空 -->
    <div class="bottom-space"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, inject, type Ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { authAPI } from '@/api/authAPI'
import Message from '@/utils/message'

const router = useRouter()
const route = useRoute()

interface EvidenceMedia {
  type: 'image' | 'video'
  file: File
  previewUrl: string
  coverUrl?: string
}

const loading = ref(true)
const refundType = ref<'REFUND' | 'AFTER_SALE'>('REFUND')
const refundReason = ref('')
const refundAmount = ref<number | null>(null)
const description = ref('')
const evidenceMedia = ref<EvidenceMedia[]>([])
const submitting = ref(false)
const submitCountdown = ref(0)
const orderItem = ref<any>(null)

const dragIndex = ref(-1)
const dragStartY = ref(0)
const dragStartX = ref(0)
const dragTimer = ref<number | null>(null)
const maxAmount = ref(0)
const imageInput = ref<HTMLInputElement | null>(null)
const videoInput = ref<HTMLInputElement | null>(null)

const videoCount = computed(() => evidenceMedia.value.filter(m => m.type === 'video').length)

const orderItemId = computed(() => {
  const id = route.params.orderItemId as string
  return id ? Number(id) : null
})



const canSubmit = computed(() => {
  return refundReason.value &&
         refundAmount.value &&
         refundAmount.value > 0 &&
         refundAmount.value <= maxAmount.value &&
         orderItemId.value
})

const formatPrice = (price: number): string => {
  if (price == null || isNaN(price)) return '0.00'
  return price.toFixed(2)
}

const getRefundTypeText = (type: string): string => {
  return type === 'REFUND' ? '仅退款' : '退货退款'
}

const loadOrderItem = async () => {
  loading.value = true
  if (!orderItemId.value) {
    Message.error('请选择要退款的商品')
    router.back()
    loading.value = false
    return
  }

  try {
    const response = await authAPI.getOrderItemDetail(orderItemId.value)

    if (response.success) {
      orderItem.value = response.data?.orderItem
      if (orderItem.value) {
          maxAmount.value = orderItem.value.price * orderItem.value.quantity
          refundAmount.value = maxAmount.value

          // if (orderItem.value.refundStatus) {
          //   if (orderItem.value.refundStatus !== 'FAILED') {
          //     Message.warning('该商品已有退款申请，正在跳转到退款详情页')
          //     setTimeout(() => {
          //       router.push({
          //         name: 'RefundChatStep',
          //         params: { refundId: String(orderItem.value.refundId), orderItemId: String(orderItemId.value) }
          //       })
          //     }, 1500)
          //     return
          //   }
          // }

        const orderStatus = orderItem.value.orderStatus
        const refundableStatuses = ['PAID', 'PROCESSING', 'SHIPPED', 'COMPLETED']
        if (!refundableStatuses.includes(orderStatus)) {
          Message.error('当前订单状态不允许申请退款')
          setTimeout(() => {
            router.back()
          }, 1500)
          return
        }
      } else {
        throw new Error('获取商品信息失败')
      }
    } else {
      throw new Error(response.message || '获取商品信息失败')
    }
  } catch (error: any) {
    Message.error(error.message || '加载失败')
    setTimeout(() => {
      router.back()
    }, 1500)
  } finally {
    loading.value = false
  }
}

const selectImages = () => {
  imageInput.value?.click()
}

const selectVideos = () => {
  videoInput.value?.click()
}

const generateVideoCover = (file: File, seconds: number = 1): Promise<string> => {
  return new Promise((resolve) => {
    const video = document.createElement('video')
    video.preload = 'metadata'
    video.muted = true
    video.playsInline = true

    const url = URL.createObjectURL(file)
    video.src = url

    video.onloadeddata = () => {
      video.currentTime = seconds
    }

    video.onseeked = () => {
      const canvas = document.createElement('canvas')
      canvas.width = video.videoWidth
      canvas.height = video.videoHeight
      const ctx = canvas.getContext('2d')
      if (ctx) {
        ctx.drawImage(video, 0, 0, canvas.width, canvas.height)
      }

      canvas.toBlob((blob) => {
        URL.revokeObjectURL(url)
        if (blob) {
          const coverUrl = URL.createObjectURL(blob)
          resolve(coverUrl)
        } else {
          resolve(url)
        }
      }, 'image/jpeg', 0.8)
    }

    video.onerror = () => {
      URL.revokeObjectURL(url)
      resolve('')
    }
  })
}

const handleImageSelect = (event: Event) => {
  const target = event.target as HTMLInputElement
  const files = target.files
  if (!files) return

  const remainingSlots = 9 - evidenceMedia.value.length
  const imageSlots = 9 - videoCount.value
  const actualSlots = Math.min(remainingSlots, imageSlots)
  const filesToProcess = Array.from(files).slice(0, actualSlots)

  for (const file of filesToProcess) {
    if (file.size > 5 * 1024 * 1024) {
      Message.error(`图片"${file.name}"超过 5MB 限制`)
      continue
    }
    if (!file.type.startsWith('image/')) {
      Message.error(`"${file.name}"不是图片文件`)
      continue
    }
    const previewUrl = URL.createObjectURL(file)
    evidenceMedia.value.push({ type: 'image', file, previewUrl })
  }
  target.value = ''
}

const handleVideoSelect = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const files = target.files
  if (!files) return

  const remainingVideoSlots = 3 - videoCount.value
  const filesToProcess = Array.from(files).slice(0, remainingVideoSlots)

  for (const file of filesToProcess) {
    if (file.size > 30 * 1024 * 1024) {
      Message.error(`视频"${file.name}"超过 30MB 限制`)
      continue
    }
    const allowedTypes = ['video/mp4', 'video/quicktime']
    if (!allowedTypes.includes(file.type)) {
      Message.error(`"${file.name}"格式不支持，仅支持 MP4、MOV 格式`)
      continue
    }

    const coverUrl = await generateVideoCover(file, 1)
    evidenceMedia.value.push({
      type: 'video',
      file,
      previewUrl: URL.createObjectURL(file),
      coverUrl
    })
  }
  target.value = ''
}

const removeMedia = (index: number) => {
  const media = evidenceMedia.value[index]
  if (media?.previewUrl) {
    URL.revokeObjectURL(media.previewUrl)
  }
  if (media?.coverUrl) {
    URL.revokeObjectURL(media.coverUrl)
  }
  evidenceMedia.value.splice(index, 1)
}

const submitRefund = async () => {
  if (!canSubmit.value) return
  submitting.value = true

  try {
    const currentOrderItemId = orderItemId.value
    if (!currentOrderItemId) {
      throw new Error('订单项 ID 不存在')
    }

    const orderId = orderItem.value?.orderId
    if (!orderId) {
      throw new Error('无法获取订单信息')
    }

    const formData = new FormData()
    formData.append('orderItemId', String(currentOrderItemId))
    formData.append('refundType', refundType.value)
    formData.append('refundReason', refundReason.value)
    formData.append('refundAmount', String(refundAmount.value ?? 0))
    formData.append('description', description.value)

    const imageFiles = evidenceMedia.value.filter(m => m.type === 'image')
    const videoFiles = evidenceMedia.value.filter(m => m.type === 'video')

    imageFiles.forEach(img => {
      formData.append('images', img.file)
    })

    videoFiles.forEach(video => {
      formData.append('videos', video.file)
      if (video.coverUrl && video.coverUrl.startsWith('blob:')) {
        fetch(video.coverUrl)
          .then(res => res.blob())
          .then(blob => {
            const coverFile = new File([blob], video.file.name.replace(/\.\w+$/, '_cover.jpg'), { type: 'image/jpeg' })
            formData.append('videoCovers', coverFile)
          })
      }
    })

    await new Promise(resolve => setTimeout(resolve, 100))

    const response = await authAPI.submitRefundFormData(currentOrderItemId, formData)

    if (response.success) {
      evidenceMedia.value.forEach(m => {
        if (m.previewUrl) URL.revokeObjectURL(m.previewUrl)
        if (m.coverUrl) URL.revokeObjectURL(m.coverUrl)
      })
      Message.success('退款申请提交成功')
      const refundId = response.data?.refundId || response.data?.id || response.data?.data?.refundId
      if (!refundId) {
        Message.error('退款记录创建失败')
        return
      }
      router.replace({
        name: 'RefundChatStep',
        params: { refundId: String(refundId) }
      })
    } else {
      const errorMessage = response.message || '提交失败'

      if (errorMessage.includes('该商品已有退款申请')) {
        if (orderItem.value?.refundId) {
          Message.warning('该商品已有退款申请，正在跳转到沟通页')
          setTimeout(() => {
            router.replace({
              name: 'RefundChatStep',
              params: { refundId: String(orderItem.value?.refundId) }
            })
          }, 1500)
        } else {
          Message.error('无法获取退款记录信息')
        }
      } else if (errorMessage.includes('已达最大申请次数')) {
        Message.error('已达最大申请次数(3次)，无法再次申请')
      } else {
        throw new Error(errorMessage)
      }
    }
  } catch (error: any) {
    Message.error(error.message || '提交失败')
  } finally {
    submitting.value = false
    if (submitCountdown.value === 0) {
      submitCountdown.value = 2
      const timer = setInterval(() => {
        submitCountdown.value--
        if (submitCountdown.value <= 0) {
          clearInterval(timer)
        }
      }, 1000)
    }
  }
}

const onTouchStart = (e: TouchEvent, index: number) => {
  const touch = e.touches[0]
  if (!touch) return
  dragStartX.value = touch.clientX
  dragStartY.value = touch.clientY
  dragTimer.value = window.setTimeout(() => {
    dragIndex.value = index
    Message.info('长按可拖动排序')
  }, 500)
}

const onTouchMove = (e: TouchEvent) => {
  const touch = e.touches[0]
  if (!touch) return

  if (dragTimer.value !== null) {
    const deltaX = Math.abs(touch.clientX - dragStartX.value)
    const deltaY = Math.abs(touch.clientY - dragStartY.value)
    if (deltaX > 10 || deltaY > 10) {
      clearTimeout(dragTimer.value)
      dragTimer.value = null
    }
  }
  if (dragIndex.value === -1) return

  const target = e.target as HTMLElement
  const item = target.closest('.uploaded-media') as HTMLElement
  if (!item) return

  const items = Array.from(document.querySelectorAll('.uploaded-media'))
  const currentIndex = items.indexOf(item)
  if (currentIndex === -1) return

  const clientY = touch.clientY
  let closestIndex = currentIndex

  for (let i = 0; i < items.length; i++) {
    const el = items[i]
    if (!el) continue
    const rect = el.getBoundingClientRect()
    const centerY = rect.top + rect.height / 2
    const closestEl = items[closestIndex]
    if (!closestEl) continue
    const closestRect = closestEl.getBoundingClientRect()
    const closestCenterY = closestRect.top + closestRect.height / 2
    if (Math.abs(clientY - centerY) < Math.abs(clientY - closestCenterY)) {
      closestIndex = i
    }
  }

  if (closestIndex !== currentIndex && closestIndex !== dragIndex.value) {
    const mediaList = [...evidenceMedia.value]
    const temp = mediaList[currentIndex]
    if (temp) {
      mediaList.splice(currentIndex, 1)
      mediaList.splice(closestIndex, 0, temp)
      evidenceMedia.value = mediaList
      dragIndex.value = closestIndex
    }
  }
}

const onTouchEnd = () => {
  if (dragTimer.value !== null) {
    clearTimeout(dragTimer.value)
    dragTimer.value = null
  }
  dragIndex.value = -1
}

const onMouseDown = (e: MouseEvent, index: number) => {
  dragStartX.value = e.clientX
  dragStartY.value = e.clientY
  dragTimer.value = window.setTimeout(() => {
    dragIndex.value = index
    Message.info('按住可拖动排序')
  }, 500)

  const onMouseMove = (moveEvent: MouseEvent) => {
    if (dragTimer.value !== null) {
      const deltaX = Math.abs(moveEvent.clientX - dragStartX.value)
      const deltaY = Math.abs(moveEvent.clientY - dragStartY.value)
      if (deltaX > 10 || deltaY > 10) {
        clearTimeout(dragTimer.value)
        dragTimer.value = null
      }
    }
    if (dragIndex.value === -1) return

    const items = Array.from(document.querySelectorAll('.uploaded-media'))
    const clientY = moveEvent.clientY
    let closestIndex = dragIndex.value

    for (let i = 0; i < items.length; i++) {
      const el = items[i]
      if (!el) continue
      const rect = el.getBoundingClientRect()
      const centerY = rect.top + rect.height / 2
      const closestEl = items[closestIndex]
      if (!closestEl) continue
      const closestRect = closestEl.getBoundingClientRect()
      const closestCenterY = closestRect.top + closestRect.height / 2
      if (Math.abs(clientY - centerY) < Math.abs(clientY - closestCenterY)) {
        closestIndex = i
      }
    }

    if (closestIndex !== dragIndex.value) {
      const mediaList = [...evidenceMedia.value]
      const temp = mediaList[dragIndex.value]
      if (temp) {
        mediaList.splice(dragIndex.value, 1)
        mediaList.splice(closestIndex, 0, temp)
        evidenceMedia.value = mediaList
        dragIndex.value = closestIndex
      }
    }
  }

  const onMouseUp = () => {
    onTouchEnd()
    document.removeEventListener('mousemove', onMouseMove)
    document.removeEventListener('mouseup', onMouseUp)
  }

  document.addEventListener('mousemove', onMouseMove)
  document.addEventListener('mouseup', onMouseUp)
}

onMounted(() => {
  const currentStep = inject<Ref<number>>('currentStep', ref(1))
  currentStep.value = 1
  loadOrderItem()
})

onUnmounted(() => {
  evidenceMedia.value.forEach(m => {
    if (m.previewUrl) URL.revokeObjectURL(m.previewUrl)
    if (m.coverUrl) URL.revokeObjectURL(m.coverUrl)
  })
})
</script>

<style scoped>
@import url('@/static/css/user/退款售后.css');
</style>
