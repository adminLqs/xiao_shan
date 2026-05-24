<template>
  <div class="reviews-page page-container" @touchstart="onTouchStart" @touchmove="onTouchMove" @touchend="onTouchEnd">
    <!-- 顶部导航栏 -->
    <div class="page-navbar">
      <button class="page-nav-back" @click="router.back()">
        <i class="fas fa-chevron-left"></i>
      </button>
      <div class="page-nav-title">
        <i class="fas fa-comment-dots"></i>
        <span>商品评价</span>
      </div>
      <div class="page-nav-right"></div>
    </div>

    <!-- 下拉刷新区域 -->
    <div class="refresh-indicator" :style="{ height: pullDistance + 'px' }">
      <div class="starlight-refresh" v-if="isRefreshing">
        <div class="loader-ring-sm">
          <i class="fas fa-sparkles brand-icon-sm"></i>
        </div>
        <span class="loader-text-sm">云杉购·刷新中</span>
      </div>
      <div class="pull-hint" v-else-if="pullDistance > 0">
        <span>{{ pullDistance > 60 ? '✨ 释放刷新' : '下拉刷新' }}</span>
      </div>
    </div>

    <!-- 评价概览 -->
    <div class="review-stats" v-if="!loading && stats.total > 0">
      <div class="stats-left">
        <div class="score">{{ stats.averageRating }}</div>
        <div class="score-label">商品评分</div>
        <div class="positive-rate">{{ stats.positiveRate }}% 好评</div>
      </div>
      <div class="stats-right">
        <div
          v-for="rating in [5, 4, 3, 2, 1]"
          :key="rating"
          class="rating-bar"
          @click="filterByRating(rating)"
        >
          <span class="rating-label">{{ rating }}星</span>
          <div class="bar-bg">
            <div
              class="bar-fill"
              :style="{ width: getRatingPercent(rating) + '%' }"
            ></div>
          </div>
          <span class="rating-count">{{ stats.ratingDistribution[rating.toString()] || 0 }}</span>
        </div>
      </div>
    </div>

    <!-- 评价筛选Tabs -->
    <div class="review-tabs">
      <div
        v-for="tab in tabs"
        :key="tab.value"
        class="tab-item"
        :class="{ active: activeTab === tab.value }"
        @click="changeTab(tab.value)"
      >
        {{ tab.label }}
        <span v-if="tab.count !== undefined" class="tab-count">{{ tab.count }}</span>
      </div>
    </div>

    <!-- 评价列表 -->
    <div class="review-list" ref="listRef" @scroll="handleScroll">
      <div
        v-for="review in reviews"
        :key="review.id"
        class="review-item"
      >
        <!-- 用户信息行 -->
        <div class="review-header">
          <img :src="review.avatar || defaultAvatar" class="review-avatar" />
          <div class="review-user-info">
            <div class="review-user-name">{{ review.userName }}</div>
            <div class="review-meta-row">
              <div class="review-rating">
                <i v-for="i in 5" :key="i" class="fas fa-star" :class="{ active: i <= review.rating }"></i>
              </div>
              <span class="review-time">{{ review.time }}</span>
            </div>
          </div>
        </div>

        <!-- 评价内容 -->
        <div class="review-content" v-if="review.content">{{ review.content }}</div>

        <!-- 评价图片和视频并列 -->
        <div class="review-media" v-if="(review.images || []).length > 0 || (review.videos || []).length > 0">
          <!-- 视频 -->
          <div
            v-for="(video, vIdx) in (review.videos || [])"
            :key="'v-' + vIdx"
            class="media-item video-item"
            @click.stop="previewVideo(video.videoUrl)"
          >
            <div class="media-thumb-wrap">
              <img v-if="video.coverUrl" :src="video.coverUrl" class="media-thumb" />
              <div v-else class="media-placeholder">
                <i class="fas fa-video"></i>
              </div>
              <div class="video-play-icon">
                <i class="fas fa-play-circle"></i>
              </div>
              <span v-if="video.duration" class="video-duration">{{ formatDuration(video.duration) }}</span>
            </div>
          </div>
          <!-- 图片 -->
          <div
            v-for="(img, idx) in (review.images || [])"
            :key="'img-' + idx"
            class="media-item"
            @click="previewImage(review.images, Number(idx))"
          >
            <img :src="img" class="media-thumb" />
          </div>
        </div>

        <!-- 商品信息卡片（独立区块） -->
        <div class="review-product-card" v-if="review.productId" @click="goToProductWithSku(review)">
          <img :src="review.productImage || '/images/default-product.png'" class="review-product-image" />
          <div class="review-product-info">
            <div class="review-product-name">{{ review.productName || '已下架商品' }}</div>
            <div v-if="review.skuName" class="review-product-sku">{{ review.skuName }}</div>
          </div>
          <div class="review-product-action">
            <span>买同款</span>
            <i class="fas fa-chevron-right"></i>
          </div>
        </div>

        <!-- SKU信息（保留原有，兼容旧数据） -->
        <div class="review-sku" v-if="review.skuInfo && !review.skuName">
          {{ review.skuInfo }}
        </div>
      </div>

      <!-- 加载状态区域（独立于列表，固定高度） -->
      <div class="load-more-wrapper">
        <div v-if="loading" class="loading-more-bar">
          <i class="fas fa-spinner fa-spin"></i>
          <span>加载中...</span>
        </div>
        <div v-else-if="!hasMore && reviews.length > 0" class="no-more-bar">
          — 已经到底了 —
        </div>
      </div>
      <div v-if="reviews.length === 0 && !loading" class="empty-reviews">
        <i class="fas fa-comment-dots"></i>
        <p>暂无评价</p>
      </div>
    </div>

    <!-- 底部留空 -->
    <div class="bottom-space"></div>

    <!-- 图片预览弹窗 -->
    <div class="image-preview-overlay" v-if="showPreview" @click="closePreview">
      <button class="preview-close" @click.stop="closePreview">
        <i class="fas fa-times"></i>
      </button>
      <button class="preview-arrow left" @click.stop="prevImage" v-if="previewImages.length > 1">
        <i class="fas fa-chevron-left"></i>
      </button>
      <img :src="previewImages[previewIndex]" class="preview-image" @click.stop />
      <button class="preview-arrow right" @click.stop="nextImage" v-if="previewImages.length > 1">
        <i class="fas fa-chevron-right"></i>
      </button>
      <div class="preview-counter" v-if="previewImages.length > 1">
        {{ previewIndex + 1 }}/{{ previewImages.length }}
      </div>
    </div>

    <!-- 视频预览弹窗 -->
    <div class="video-preview-overlay" v-if="showVideoPreview" @click="showVideoPreview = false">
      <button class="preview-close" @click.stop="showVideoPreview = false">
        <i class="fas fa-times"></i>
      </button>
      <video
        :src="previewVideoUrl"
        class="preview-video"
        controls
        autoplay
        playsinline
        @click.stop
      ></video>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { authAPI } from '@/api/authAPI'
import Message from '@/utils/message'
import defaultAvatar from '@/static/images/user-avatar.jpg'

const router = useRouter()
const route = useRoute()
const productId = computed(() => Number(route.params.productId))

// 响应式数据
const loading = ref(true)
const loadingMore = ref(false)
const isRefreshing = ref(false)
const reviews = ref<any[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const hasMore = ref(true)
const activeTab = ref('all')
const pullDistance = ref(0)
const stats = ref<any>({
  total: 0,
  averageRating: 0,
  positiveRate: 0,
  ratingDistribution: { '1': 0, '2': 0, '3': 0, '4': 0, '5': 0 },
  hasImages: 0
})

// 筛选条件
const filterRating = ref<number | undefined>(undefined)
const filterRatingRange = ref<[number, number] | null>(null)
const filterHasImages = ref(false)

// 下拉刷新相关
let startY = 0
let isTouching = false

// Tabs
const tabs = computed(() => [
  { label: '全部', value: 'all', count: stats.value.total },
  { label: '有图', value: 'hasImages', count: stats.value.hasImages },
  { label: '好评', value: 'good', count: (stats.value.ratingDistribution['4'] || 0) + (stats.value.ratingDistribution['5'] || 0) },
  { label: '中评', value: 'medium', count: stats.value.ratingDistribution['3'] },
  { label: '差评', value: 'bad', count: (stats.value.ratingDistribution['1'] || 0) + (stats.value.ratingDistribution['2'] || 0) }
])

// 计算评分百分比
const getRatingPercent = (rating: number) => {
  if (stats.value.total === 0) return 0
  const count = stats.value.ratingDistribution[rating.toString()] || 0
  return Math.round((count / stats.value.total) * 100)
}

// 返回上一页
const goBack = () => {
  router.back()
}

// 跳转到商品详情页
const goToProduct = (productId: number) => {
  if (productId) {
    router.push({ name: 'ProductDetail', params: { productId } })
  }
}

// 跳转到商品详情页并自动选中规格
const goToProductWithSku = (review: any) => {
  let skuSpec = ''
  if (review.skuName) {
    skuSpec = review.skuName.replace(/，/g, ',').replace(/、/g, ',')
  }
  router.push({
    name: 'ProductDetail',
    params: { productId: review.productId },
    query: { skuSpec }
  })
}

// 加载评价统计
const loadReviewStats = async () => {
  try {
    const response = await authAPI.getReviewStatistics(productId.value)
    if (response.success && response.data) {
      stats.value = response.data
    }
  } catch (error: any) {
    Message.error(error.message || '加载评价统计失败')
  }
}

// 加载评价列表
const loadReviews = async (reset = false) => {
  if (loadingMore.value) return
  if (reset) {
    currentPage.value = 1
    hasMore.value = true
  }
  if (!hasMore.value) return
  loadingMore.value = true
  try {
    const params: any = {
      page: currentPage.value,
      pageSize: pageSize.value
    }
    if (filterRatingRange.value) {
      params.minRating = filterRatingRange.value[0]
      params.maxRating = filterRatingRange.value[1]
    } else if (filterRating.value) {
      params.rating = filterRating.value
    }
    if (filterHasImages.value) {
      params.hasImages = true
    }
    const response = await authAPI.getProductReviews({ productId: productId.value, ...params })
    if (response.success && response.data) {
      const newReviews = response.data.records || []
      // 处理评论数据
      const processedReviews = newReviews.map((comment: any) => {
        const userReviewVO = comment.review ? comment : { review: comment, userProfile: null, reviewImages: [], reviewVideos: [] }
        const review = userReviewVO.review || {}
        const userProfile = userReviewVO.userProfile || {}
        const reviewImages = userReviewVO.reviewImages || []
        const reviewVideos = userReviewVO.reviewVideos || []
        return {
          id: review.id,
          avatar: userProfile.avatar || defaultAvatar,
          userName: userProfile.nickname || '匿名用户',
          rating: review.rating || 5,
          content: review.comment || '',
          time: formatTime(review.createdAt),
          images: reviewImages.map((img: any) => img.image),
          videos: reviewVideos.map((v: any) => ({
            id: v.id,
            videoUrl: v.videoUrl || v.video_url,
            coverUrl: v.coverUrl || v.cover_url,
            duration: v.duration || 0
          })),
          skuInfo: review.skuInfo || '',
          // 商品信息
          productId: review.productId,
          productName: review.productName || userReviewVO.productName,
          productImage: review.productImage || review.product_image || userReviewVO.productImage || userReviewVO.product_image || '/images/default-product.png',
          skuName: review.skuName || userReviewVO.skuName
        }
      })
      if (reset) {
        reviews.value = processedReviews
      } else {
        reviews.value.push(...processedReviews)
      }
      hasMore.value = response.data.hasMore
      currentPage.value++
    } else {
      hasMore.value = false
    }
  } catch (error: any) {
    Message.error(error.message || '加载评价失败')
    hasMore.value = false
  } finally {
    loading.value = false
    loadingMore.value = false
    isRefreshing.value = false
    pullDistance.value = 0
  }
}

// 下拉刷新 - 触摸开始
const onTouchStart = (e: TouchEvent) => {
  const scrollTop = document.documentElement.scrollTop
  if (scrollTop === 0 && e.touches?.[0]) {
    startY = e.touches[0].clientY
    isTouching = true
  }
}

// 下拉刷新 - 触摸移动
const onTouchMove = (e: TouchEvent) => {
  if (!isTouching) return
  const currentY = e.touches?.[0]?.clientY
  if (currentY === undefined) return
  const deltaY = currentY - startY
  if (deltaY > 0) {
    pullDistance.value = Math.min(deltaY * 0.5, 80)
  }
}

// 下拉刷新 - 触摸结束
const onTouchEnd = () => {
  if (!isTouching) return
  isTouching = false
  if (pullDistance.value >= 60) {
    isRefreshing.value = true
    handleRefresh()
  } else {
    pullDistance.value = 0
  }
}

// 执行刷新
const handleRefresh = async () => {
  await loadReviews(true)
  await loadReviewStats()
}

// 滚动加载（带防抖）
let scrollTimer: ReturnType<typeof setTimeout> | null = null
const handleScroll = () => {
  if (loading.value || !hasMore.value || isRefreshing.value) return
  if (scrollTimer) {
    clearTimeout(scrollTimer)
  }
  scrollTimer = setTimeout(() => {
    const scrollTop = document.documentElement.scrollTop
    const scrollHeight = document.documentElement.scrollHeight
    const clientHeight = document.documentElement.clientHeight
    // 距离底部小于50px时加载更多
    if (scrollTop + clientHeight >= scrollHeight - 50) {
      loadReviews()
    }
  }, 100)
}

// 切换Tab
const changeTab = async (tab: string) => {
  activeTab.value = tab
  // 重置筛选条件
  filterRatingRange.value = null
  if (tab === 'all') {
    filterRating.value = undefined
    filterHasImages.value = false
  } else if (tab === 'hasImages') {
    filterRating.value = undefined
    filterHasImages.value = true
  } else if (tab === 'good') {
    // 好评：4星 + 5星
    filterRatingRange.value = [4, 5]
    filterRating.value = undefined
    filterHasImages.value = false
  } else if (tab === 'medium') {
    // 中评：3星
    filterRatingRange.value = [3, 3]
    filterRating.value = undefined
    filterHasImages.value = false
  } else if (tab === 'bad') {
    // 差评：1星 + 2星
    filterRatingRange.value = [1, 2]
    filterRating.value = undefined
    filterHasImages.value = false
  } else {
    // 具体星级
    filterRating.value = Number(tab)
    filterHasImages.value = false
  }
  // 滚动到顶部
  window.scrollTo({ top: 0, behavior: 'smooth' })
  await loadReviews(true)
}

// 按评分筛选
const filterByRating = (rating: number) => {
  changeTab(rating.toString())
}

// 预览图片
const showPreview = ref(false)
const previewImages = ref<string[]>([])
const previewIndex = ref(0)

// 视频预览
const previewVideoUrl = ref('')
const showVideoPreview = ref(false)

const previewImage = (images: string[], index: number) => {
  previewImages.value = images
  previewIndex.value = index
  showPreview.value = true
}

const previewVideo = (url: string) => {
  previewVideoUrl.value = url
  showVideoPreview.value = true
}

const formatDuration = (seconds: number): string => {
  const min = Math.floor(seconds / 60)
  const sec = seconds % 60
  return `${min}:${String(sec).padStart(2, '0')}`
}

const closePreview = () => {
  showPreview.value = false
  previewImages.value = []
  previewIndex.value = 0
}

const prevImage = () => {
  if (previewIndex.value > 0) {
    previewIndex.value--
  } else {
    previewIndex.value = previewImages.value.length - 1
  }
}

const nextImage = () => {
  if (previewIndex.value < previewImages.value.length - 1) {
    previewIndex.value++
  } else {
    previewIndex.value = 0
  }
}

// 格式化时间
const formatTime = (dateStr: string) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minute = 60 * 1000
  const hour = 60 * minute
  const day = 24 * hour
  if (diff < minute) return '刚刚'
  if (diff < hour) return Math.floor(diff / minute) + '分钟前'
  if (diff < day) return Math.floor(diff / hour) + '小时前'
  if (diff < 30 * day) return Math.floor(diff / day) + '天前'
  return date.toLocaleDateString('zh-CN', { year: 'numeric', month: 'short', day: 'numeric' })
}

onMounted(async () => {
  await loadReviewStats()
  await loadReviews(true)
  window.addEventListener('scroll', handleScroll)
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
  if (scrollTimer) {
    clearTimeout(scrollTimer)
  }
})
</script>

<style scoped>
@import url('@/static/css/user/评论列表.css');
</style>