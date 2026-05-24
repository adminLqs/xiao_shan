<template>
  <div class="review-page page-container">
    <!-- 顶部导航栏 -->
    <div class="page-navbar">
      <button class="page-nav-back" @click="router.back()">
        <i class="fas fa-chevron-left"></i>
      </button>
      <div class="page-nav-title">
        <i class="fas fa-edit"></i>
        <span>发表评价</span>
      </div>
      <div class="page-nav-right"></div>
    </div>

    <!-- 骨架屏 -->
    <div v-if="loading" class="skeleton-form">
      <div class="product-card skeleton-card">
        <div class="skeleton" style="width: 100px; height: 100px; border-radius: 8px;"></div>
        <div class="product-detail">
          <div class="skeleton-line long"></div>
          <div class="skeleton-line"></div>
          <div class="skeleton-line short"></div>
        </div>
      </div>
      <div class="review-section skeleton-card">
        <div class="skeleton-line short"></div>
        <div class="skeleton" style="width: 180px; height: 32px;"></div>
      </div>
      <div class="review-section skeleton-card">
        <div class="skeleton-line short"></div>
        <div class="skeleton" style="width: 100%; height: 100px;"></div>
      </div>
    </div>

    <!-- 商品信息 -->
    <div v-else>
      <div class="product-card" v-if="orderItem">
      <img :src="orderItem.productImage" class="product-image" :alt="orderItem.productName" />
      <div class="product-detail">
        <div class="product-name">{{ orderItem.productName }}</div>
        <div class="product-spec" v-if="orderItem.skuName">{{ orderItem.skuName }}</div>
        <div class="product-price-row">
          <span class="product-price">¥{{ formatPrice(orderItem.price) }}</span>
          <span class="product-quantity">x{{ orderItem.quantity }}</span>
        </div>
      </div>
    </div>

    <!-- 评分选择 -->
    <div class="review-section">
      <div class="section-title">商品评分 <span class="required">*</span></div>
      <div class="rating-select">
        <i
          v-for="i in 5"
          :key="i"
          class="fas fa-star"
          :class="{ active: i <= rating }"
          @click="rating = i"
        ></i>
      </div>
      <div class="rating-label">{{ ratingLabels[rating] }}</div>
    </div>

    <!-- 评价内容 -->
    <div class="review-section">
      <div class="section-title">评价内容</div>
      <textarea
        v-model="content"
        class="content-textarea"
        rows="5"
        maxlength="500"
        placeholder="请分享您的使用体验和感受..."
      ></textarea>
      <div class="textarea-footer">
        <span class="char-count">{{ content.length }}/500</span>
      </div>
    </div>

    <!-- 媒体上传 -->
    <div class="review-section">
      <div class="section-title">晒单媒体</div>
      <div class="media-upload-area">
        <!-- 已选媒体预览 -->
        <div v-for="(file, index) in mediaFiles" :key="index" class="media-preview-item">
          <img v-if="file.type === 'image'" :src="file.previewUrl" @click="previewMedia(index)" />
          <div v-else class="video-preview-wrapper" @click="previewMedia(index)">
            <img v-if="file.coverUrl" :src="file.coverUrl" class="video-cover" alt="视频封面" />
            <video v-else :src="file.previewUrl"></video>
            <div class="video-play-icon">
              <i class="fas fa-play"></i>
            </div>
          </div>
          <button class="remove-media-btn" @click="removeMedia(index)">
            <i class="fas fa-times"></i>
          </button>
          <span v-if="file.type === 'video'" class="video-tag">视频</span>
        </div>

        <!-- 上传按钮 -->
        <div v-if="mediaFiles.length < 9" class="upload-actions">
          <button class="upload-option" @click="selectImage">
            <i class="fas fa-image"></i>
            <span>相册</span>
          </button>
          <button class="upload-option" @click="takePhoto">
            <i class="fas fa-camera"></i>
            <span>拍照</span>
          </button>
          <button v-if="videoCount < 3" class="upload-option" @click="selectVideo">
            <i class="fas fa-video"></i>
            <span>视频</span>
            <span v-if="videoCount > 0" class="upload-count">({{ videoCount }}/3)</span>
          </button>
        </div>
      </div>
    </div>

    <!-- 提交按钮 -->
    <button
      class="submit-btn"
      :disabled="!canSubmit || submitting"
      @click="submitReview"
    >
      <i v-if="submitting" class="fas fa-spinner fa-spin"></i>
      {{ submitting ? '提交中...' : '提交评价' }}
    </button>

    <!-- 提示信息 -->
    <div class="tips-section">
      <div class="tips-title">
        <i class="fas fa-info-circle"></i>
        温馨提示
      </div>
      <ul class="tips-list">
        <li>评价内容需符合法律法规和公序良俗</li>
        <li>上传真实图片有助于其他用户参考</li>
        <li>提交后将无法修改，请认真填写</li>
      </ul>
    </div>

    <!-- 底部留空 -->
    <div class="bottom-space"></div>
    </div>

    <!-- 媒体预览弹窗 -->
    <div v-if="previewVisible" class="media-preview-overlay" @click="previewVisible = false">
      <img v-if="previewMediaObj?.type === 'image'" :src="previewMediaObj?.previewUrl" />
      <video v-else :src="previewMediaObj?.previewUrl" controls autoplay></video>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { authAPI } from '@/api/authAPI'
import Message from '@/utils/message'

interface MediaFile {
  type: 'image' | 'video'
  file: File
  previewUrl: string
  coverUrl?: string  // 视频封面
}

const router = useRouter()
const route = useRoute()

const orderItemId = computed(() => {
  const id = route.params.orderItemId as string
  return id ? Number(id) : null
})

const loading = ref(true)
const orderItem = ref<any>(null)
const rating = ref(5)
const content = ref('')
const mediaFiles = ref<MediaFile[]>([])
const submitting = ref(false)
const previewVisible = ref(false)
const previewMediaObj = ref<MediaFile | null>(null)

const hasVideo = computed(() => mediaFiles.value.some(f => f.type === 'video'))
const videoCount = computed(() => mediaFiles.value.filter(f => f.type === 'video').length)

const ratingLabels: Record<number, string> = {
  1: '非常差',
  2: '差',
  3: '一般',
  4: '好',
  5: '非常好'
}

const canSubmit = computed(() => {
  return rating.value > 0
})

const formatPrice = (price: number): string => {
  if (price == null || isNaN(price)) return '0.00'
  return price.toFixed(2)
}

// 生成视频封面
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
      
      // 转为 Blob URL 用于预览
      canvas.toBlob((blob) => {
        URL.revokeObjectURL(url)
        if (blob) {
          const coverUrl = URL.createObjectURL(blob)
          resolve(coverUrl)
        } else {
          // 如果生成失败，使用视频本身作为预览
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

const loadData = async () => {
  loading.value = true
  if (!orderItemId.value) {
    Message.error('参数错误')
    router.back()
    loading.value = false
    return
  }

  try {
    const response = await authAPI.getOrderItemDetail(orderItemId.value)
    if (response.success) {
      orderItem.value = response.data?.orderItem
    } else {
      throw new Error(response.message || '加载失败')
    }
  } catch (error: any) {
    Message.error(error.message || '加载失败')
  }

  loading.value = false
}

const imageInput = ref<HTMLInputElement | null>(null)
const videoInput = ref<HTMLInputElement | null>(null)

// 选择图片
const selectImage = () => {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = 'image/*'
  input.multiple = true
  input.onchange = selectImages
  input.style.display = 'none'
  document.body.appendChild(input)
  input.click()
  input.remove()
}

// 拍照
const takePhoto = () => {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = 'image/*'
  input.capture = 'environment'
  input.onchange = selectImages
  input.style.display = 'none'
  document.body.appendChild(input)
  input.click()
  input.remove()
}

// 选择视频
const selectVideo = () => {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = 'video/mp4,video/mov,video/avi,video/webm'
  input.multiple = true
  input.onchange = selectVideos
  input.style.display = 'none'
  document.body.appendChild(input)
  input.click()
  input.remove()
}

// 多选图片处理
const selectImages = async (e: Event) => {
  const files = (e.target as HTMLInputElement).files
  if (!files) return
  
  for (const file of Array.from(files)) {
    if (mediaFiles.value.length >= 9) {
      Message.warning('最多上传9张图片')
      break
    }
    if (file.size > 5 * 1024 * 1024) {
      Message.warning(`${file.name} 超过5MB`)
      continue
    }
    mediaFiles.value.push({ 
      type: 'image', 
      file, 
      previewUrl: URL.createObjectURL(file) 
    })
  }
}

// 多选视频处理
const selectVideos = async (e: Event) => {
  const files = (e.target as HTMLInputElement).files
  if (!files) return
  
  for (const file of Array.from(files)) {
    if (videoCount.value >= 3) {
      Message.warning('最多上传3个视频')
      break
    }
    if (file.size > 50 * 1024 * 1024) {
      Message.warning(`${file.name} 超过50MB`)
      continue
    }
    const allowedTypes = ['video/mp4', 'video/quicktime', 'video/x-msvideo', 'video/webm']
    if (!allowedTypes.includes(file.type)) {
      Message.warning(`${file.name} 格式不支持，仅支持 MP4、MOV、AVI、WEBM 格式`)
      continue
    }
    
    const coverUrl = await generateVideoCover(file, 1)
    mediaFiles.value.push({ 
      type: 'video', 
      file, 
      previewUrl: URL.createObjectURL(file),
      coverUrl: coverUrl 
    })
  }
}

// 删除媒体
const removeMedia = (index: number) => {
  const mediaFile = mediaFiles.value[index]
  if (mediaFile?.previewUrl) {  // 添加空值检查
    URL.revokeObjectURL(mediaFile.previewUrl)
  }
  mediaFiles.value.splice(index, 1)
}

// 预览
const previewMedia = (index: number) => {
    const mediaFile = mediaFiles.value[index]
  if (mediaFile) {  // 添加空值检查
    previewMediaObj.value = mediaFile
    previewVisible.value = true
  }
  previewVisible.value = true
}

// 提交
const submitReview = async () => {
  if (!canSubmit.value || submitting.value) return
  submitting.value = true
  try {
    const formData = new FormData()
    formData.append('orderItemId', String(orderItemId.value))
    formData.append('rating', String(rating.value))
    if (content.value) formData.append('comment', content.value)
    
    // 分离图片和视频
    const imageFiles = mediaFiles.value.filter(f => f.type === 'image')
    const videoFiles = mediaFiles.value.filter(f => f.type === 'video')
    
    // 添加图片（可多张）
    imageFiles.forEach(f => {
      formData.append('images', f.file)
    })
    
    // 添加视频和封面（可多个，最多3个）
    videoFiles.forEach(f => {
      formData.append('videos', f.file)
      // 如果有封面，也一起上传
      if (f.coverUrl && f.coverUrl.startsWith('blob:')) {
        // 将 blob URL 转换为 File
        fetch(f.coverUrl)
          .then(res => res.blob())
          .then(blob => {
            const coverFile = new File([blob], f.file.name.replace(/\.\w+$/, '_cover.jpg'), { type: 'image/jpeg' })
            formData.append('videoCovers', coverFile)
          })
      }
    })
    
    // 等待所有封面上传准备完成
    await new Promise(resolve => setTimeout(resolve, 100))
    
    const response = await authAPI.submitReviewFormData(formData)
    if (response.success) {
      Message.success('评价提交成功')
      // 清理预览URL
      mediaFiles.value.forEach(f => {
        if (f.previewUrl) URL.revokeObjectURL(f.previewUrl)
        if (f.coverUrl) URL.revokeObjectURL(f.coverUrl)
      })
      router.back()
    } else {
      throw new Error(response.message || '提交失败')
    }
  } catch (error: any) {
    Message.error(error.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
@import url('@/static/css/user/评论页.css');
</style>
