<template>
  <div class="shop-page page-container">
    <!-- 顶部导航栏 -->
    <div class="page-navbar">
      <button class="page-nav-back" @click="router.back()">
        <i class="fas fa-chevron-left"></i>
      </button>
      <div class="page-nav-title">
        <i class="fas fa-store"></i>
        <span>{{ loading ? '店铺' : (shopInfo.name || '店铺') }}</span>
      </div>
      <div class="page-nav-right"></div>
    </div>

    <!-- 加载状态 - 骨架屏 -->
    <div v-if="loading" class="shop-skeleton">
      <!-- 店铺头部骨架 -->
      <div class="skeleton-card">
        <div class="skeleton-shop-header">
          <div class="skeleton-avatar"></div>
          <div class="skeleton-shop-info">
            <div class="skeleton-line"></div>
            <div class="skeleton-line short" style="margin-top: 8px;"></div>
          </div>
          <div class="skeleton-follow-btn"></div>
        </div>
      </div>

      <!-- 店铺导航骨架 -->
      <div class="skeleton-shop-nav">
        <div class="skeleton-line" style="width: 60px;"></div>
        <div class="skeleton-line" style="width: 60px;"></div>
        <div class="skeleton-line" style="width: 60px;"></div>
      </div>

      <!-- 商品列表骨架 -->
      <div class="products-grid">
        <div v-for="i in 4" :key="i" class="product-card skeleton-card">
          <div class="product-image skeleton-image"></div>
          <div class="product-info">
            <div class="skeleton-line"></div>
            <div class="skeleton-line short"></div>
          </div>
        </div>
      </div>
    </div>

    <!-- 主要内容 -->
    <template v-if="!loading">
      <!-- 店铺头部信息 -->
      <div class="shop-header">
        <div class="shop-header-content">
          <div class="shop-info">
            <img :src="shopInfo.logo || defaultShopLogo" class="shop-logo" />
            <div class="shop-detail">
              <div class="shop-name">{{ shopInfo.name || '未命名店铺' }}</div>
              <div class="shop-meta">
                <span class="meta-item">{{ formatNumber(shopInfo.followerCount) }} 粉丝</span>
                <span class="meta-divider">|</span>
                <span class="meta-item">{{ formatNumber(shopInfo.productCount) }} 商品</span>
                <span class="meta-divider">|</span>
                <span class="meta-item">{{ shopInfo.score != null ? shopInfo.score.toFixed(1) : '暂无评分' }} 评分</span>
              </div>
            </div>
          </div>
          <div class="shop-actions-col">
            <button class="follow-btn" :class="{ followed: isFollowed }" @click="toggleFollow">
              <span>{{ isFollowed ? '已关注' : '+ 关注' }}</span>
            </button>
            <button class="service-btn" @click="contactShop">
              <i class="fas fa-headset"></i>
              <span>客服</span>
            </button>
          </div>
        </div>
      </div>

      <!-- 店铺导航 -->
      <div class="shop-nav" ref="tabsRef">
        <div
          v-for="tab in shopTabs"
          :key="tab.key"
          class="nav-item"
          :class="{ active: activeTab === tab.key }"
          :ref="el => { if (el) tabRefs[tab.key] = el as HTMLElement }"
          @click="handleTabClick(tab.key)"
        >
          {{ tab.label }}
        </div>
        <div class="tab-underline" :style="underlineStyle"></div>
      </div>

      <!-- 商品列表 -->
      <div v-if="activeTab === 'products'" class="products-section">
        <div class="products-grid">
          <div
            v-for="product in products"
            :key="product.id"
            class="product-card"
            @click="goToProduct(product.id)"
          >
            <div class="product-image-wrapper">
              <img :src="product.image" class="product-image" :alt="product.name" />
              <div v-if="product.discount" class="discount-tag">{{ product.discount }}</div>
            </div>
            <div class="product-info">
              <div class="product-name">{{ product.name }}</div>
              <div class="product-price">
                <span class="price-current">¥{{ formatPrice(product.price) }}</span>
                <span v-if="product.originalPrice" class="price-original">¥{{ formatPrice(product.originalPrice) }}</span>
              </div>
              <div class="product-sales">已售 {{ formatNumber(product.sales) }}</div>
            </div>
          </div>
        </div>

        <!-- 加载更多提示 -->
        <div class="load-more-wrapper">
          <div v-if="loadingMore" class="loading-more-bar">
            <i class="fas fa-spinner fa-spin"></i>
            <span>加载中...</span>
          </div>
          <div v-else-if="!hasMore && products.length > 0" class="no-more-bar">
            — 已经到底了 —
          </div>
        </div>
      </div>

      <!-- 店铺介绍 -->
      <div v-if="activeTab === 'about'" class="about-section">
        <div class="about-card">
          <div class="about-title">
            <i class="fas fa-info-circle"></i>
            <span>店铺简介</span>
          </div>
          <div class="about-content">{{ shopInfo.description || '暂无店铺介绍' }}</div>
        </div>
        <!-- 营业资质 -->
        <div class="about-card" v-if="shopInfo.qualification">
          <div class="about-title">
            <i class="fas fa-certificate"></i>
            <span>营业资质</span>
          </div>
          <div class="qualification-info">
            <!-- 文字信息 -->
            <div class="qual-row">
              <span>经营类型</span>
              <span>{{ formatBusinessType(shopInfo.qualification.businessType) }}</span>
            </div>
            <div class="qual-row">
              <span>主营类目</span>
              <span>{{ formatMainCategory(shopInfo.qualification.mainCategory) }}</span>
            </div>
            <div class="qual-row">
              <span>店铺地址</span>
              <span>{{ shopInfo.qualification.address || '-' }}</span>
            </div>

            <!-- 营业执照 -->
            <div class="qual-images" v-if="shopInfo.qualification.businessLicense">
              <div class="qual-image-title">营业执照</div>
              <div class="qual-image-list">
                <div class="qual-image-item" @click="previewQualificationImage(shopInfo.qualification.businessLicense)">
                  <img :src="shopInfo.qualification.businessLicense" />
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 评价列表 -->
      <div v-if="activeTab === 'reviews'" class="reviews-section">
        <div class="reviews-header">
          <div class="reviews-score">
            <span class="score-num">{{ shopInfo.score != null ? shopInfo.score.toFixed(1) : '暂无' }}</span>
            <div class="score-stars">
              <i v-for="i in 5" :key="i" class="fas fa-star" :class="{ active: shopInfo.score != null && i <= Math.round(shopInfo.score) }"></i>
            </div>
          </div>
          <div class="reviews-stats">
            <div class="stat-item">
              <span class="stat-value">{{ shopInfo.reviewCount }}</span>
              <span class="stat-label">评价</span>
            </div>
            <div class="stat-item">
              <span class="stat-value">{{ shopInfo.positiveRate }}%</span>
              <span class="stat-label">好评率</span>
            </div>
          </div>
        </div>
        <div class="review-list">
          <div
            v-for="review in reviews"
            :key="review.id"
            class="review-item"
          >
            <div class="review-header">
              <img :src="review.avatar || defaultAvatar" class="review-avatar" />
              <div class="review-user-info">
                <div class="review-user-name">{{ review.userName }}</div>
                <div class="review-rating">
                  <i v-for="i in 5" :key="i" class="fas fa-star" :class="{ active: i <= review.rating }"></i>
                </div>
              </div>
              <span class="review-time">{{ review.time }}</span>
            </div>
            <div class="review-content">{{ review.content }}</div>
            <!-- 媒体展示区域：图片和视频同一行 -->
            <div class="review-media" v-if="(review.images || []).length > 0 || (review.videos || []).length > 0">
              <div class="review-media-list">
                <!-- 视频 -->
                <div
                  v-for="video in review.videos"
                  :key="`video-${video.id}`"
                  class="media-item video-item"
                  @click="previewVideo(video.videoUrl)"
                >
                  <img :src="video.coverUrl || video.videoUrl || (review.images[0] ? review.images[0].image : '') || defaultAvatar" class="media-image" alt="视频封面" />
                  <div class="video-play-btn">
                    <i class="fas fa-play"></i>
                  </div>
                </div>
                <!-- 图片 -->
                <div
                  v-for="(img, idx) in review.images"
                  :key="`img-${idx}`"
                  class="media-item"
                  @click="previewImage(review.images, Number(idx))"
                >
                  <img :src="img.image" class="media-image" alt="评论图片" />
                </div>
              </div>
            </div>
          </div>
          <div v-if="reviews.length === 0" class="empty-reviews">
            <i class="fas fa-comment-dots"></i>
            <p>暂无评价</p>
          </div>
        </div>
      </div>

      <!-- 底部留空 -->
      <div class="bottom-space"></div>
    </template>

    <!-- 图片预览 -->
    <div v-if="showImagePreview" class="image-preview-overlay" @click="closeImagePreview">
      <button class="preview-close" @click.stop="closeImagePreview">
        <i class="fas fa-times"></i>
      </button>
      <div class="preview-swiper">
        <img :src="previewImages[previewImageIndex]" class="preview-image" />
      </div>
      <div class="preview-counter">{{ previewImageIndex + 1 }} / {{ previewImages.length }}</div>
      <button v-if="previewImageIndex > 0" class="preview-arrow left" @click.stop="previewImageIndex--">
        <i class="fas fa-chevron-left"></i>
      </button>
      <button v-if="previewImageIndex < previewImages.length - 1" class="preview-arrow right" @click.stop="previewImageIndex++">
        <i class="fas fa-chevron-right"></i>
      </button>
    </div>

    <!-- 视频预览 -->
    <div v-if="showVideoPreview" class="video-preview-overlay" @click="closeVideoPreview">
      <button class="preview-close" @click.stop="closeVideoPreview">
        <i class="fas fa-times"></i>
      </button>
      <video :src="previewVideoUrl" class="preview-video" controls autoplay playsinline @click.stop />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { authAPI } from '@/api/authAPI'
import Message from '@/utils/message'
import defaultShopLogo from '@/static/images/seller-avatar.jpg'
import defaultAvatar from '@/static/images/user-avatar.jpg'

const router = useRouter()
const route = useRoute()
const shopId = computed(() => {
  const id = Number(route.params.sellerId)
  if (isNaN(id) || id <= 0) {
    Message.error('店铺ID无效')
    router.back()
    return 0
  }
  return id
})

const shopInfo = ref<any>({
  id: 0,
  name: '',
  logo: '',
  description: '',
  phone: '',
  followerCount: 0,
  productCount: 0,
  score: null as number | null,
  reviewCount: 0,
  positiveRate: 0,
  qualification: null as any
})

const isFollowed = ref(false)
const activeTab = ref('products')

// Tab下划线相关
const tabsRef = ref<HTMLElement>()
const tabRefs = ref<Record<string, HTMLElement>>({})
const underlineStyle = ref({ left: '0px', width: '0px' })

const shopTabs = [
  { key: 'products', label: '商品' },
  { key: 'reviews', label: '评价' },
  { key: 'about', label: '店铺' }
]

const products = ref<any[]>([])
const reviews = ref<any[]>([])
const loading = ref(true)
const reviewsLoading = ref(false)

// 分页相关
const page = ref(1)
const pageSize = ref(12)
const hasMore = ref(true)
const loadingMore = ref(false)

// 预览相关
const showImagePreview = ref(false)
const previewImages = ref<string[]>([])
const previewImageIndex = ref(0)
const showVideoPreview = ref(false)
const previewVideoUrl = ref('')

const formatPrice = (price: number): string => {
  if (price == null || isNaN(price)) return '0.00'
  return price.toFixed(2)
}

const formatNumber = (num: number): string => {
  if (num == null) return '0'
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + '万'
  }
  return num.toString()
}

const formatDate = (dateStr: string): string => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit'
  })
}

const formatBusinessType = (type: string): string => {
  const typeMap: Record<string, string> = {
    'INDIVIDUAL': '个体工商户',
    'COMPANY': '企业商家'
  }
  return typeMap[type] || type || '-'
}

const formatMainCategory = (category: string): string => {
  const categoryMap: Record<string, string> = {
    'ELECTRONICS': '手机数码',
    'COMPUTER': '电脑办公',
    'FOOD': '食品生鲜',
    'BEAUTY': '美妆个护',
    'CLOTHING': '服饰鞋包',
    'HOME': '家居家装',
    'SPORTS': '运动户外'
  }
  return categoryMap[category] || category || '-'
}

const goToProduct = (productId: number) => {
  router.push({ name: 'ProductDetail', params: { productId } })
}

const toggleFollow = async () => {
  try {
    if (isFollowed.value) {
      const response = await authAPI.unfollowSeller(shopId.value)
      if (response.success) {
        isFollowed.value = false
        shopInfo.value.followerCount = Math.max(0, shopInfo.value.followerCount - 1)
        Message.success('已取消关注')
      } else {
        throw new Error(response.message || '操作失败')
      }
    } else {
      const response = await authAPI.followSeller(shopId.value)
      if (response.success) {
        isFollowed.value = true
        shopInfo.value.followerCount++
        Message.success('关注成功')
      } else {
        throw new Error(response.message || '操作失败')
      }
    }
  } catch (error: any) {
    Message.error(error.message || '操作失败')
  }
}

const contactShop = () => {
  Message.info('客服功能开发中')
}

const loadShopInfo = async () => {
    try {
        const response = await authAPI.getSellerInfoById(shopId.value)
        if (response.success && response.data) {
            const data = response.data
            shopInfo.value = {
                id: data.id || 0,
                userId: data.userId || 0,
                name: data.storeName || data.name || '未命名店铺',
                logo: data.storeAvatar || data.avatar || '',
                description: data.storeDetail || data.description || '',
                phone: data.contactPhone || data.phone || '',
                followerCount: data.fansCount !== undefined && data.fansCount !== null ? data.fansCount : 0,
                productCount: data.productCount !== undefined && data.productCount !== null ? data.productCount : 0,
                score: data.rating !== undefined && data.rating !== null ? data.rating : null,
                reviewCount: 0,
                positiveRate: data.positiveRate !== undefined && data.positiveRate !== null ? data.positiveRate : 0,
                qualification: null
            }

            // 加载资质信息
            if (shopInfo.value.userId) {
                await loadQualification(shopInfo.value.userId)
            }
        } else {
            throw new Error(response.message || '加载失败')
        }

        const followResponse = await authAPI.checkFollowSeller(shopId.value)
        if (followResponse.success) {
            isFollowed.value = followResponse.data?.isFollowed || false
        }
    } catch (error: any) {
        Message.error(error.message || '加载失败')
    }
}

const loadQualification = async (userId: number) => {
    try {
        const res = await authAPI.getSellerQualification(userId)
        if (res.success && res.data) {
            shopInfo.value.qualification = res.data
        }
    } catch (error) {
        // 忽略错误，资质信息不是必须的
    }
}

const loadProducts = async (reset = false) => {
  if (loadingMore.value) return
  if (reset) {
    page.value = 1
    hasMore.value = true
  }
  if (!hasMore.value) return

  loadingMore.value = true
  try {
    const response = await authAPI.getProducts({
      sellerId: shopId.value,
      page: page.value,
      pageSize: pageSize.value
    })
    if (response.success && response.data) {
      const newProducts = response.data.records?.map((item: any) => ({
        id: item.id,
        name: item.name,
        image: item.images ? item.images.split(',')[0] : '',
        price: item.price,
        originalPrice: item.originalPrice,
        discount: item.discount,
        sales: item.salesCount || item.sales || 0
      })) || []
      if (reset) {
        products.value = newProducts
      } else {
        products.value.push(...newProducts)
      }
      hasMore.value = newProducts.length >= pageSize.value
      page.value++
    }
  } catch (error: any) {
    Message.error(error.message || '加载失败')
  } finally {
    loadingMore.value = false
  }
}

// 滚动加载
const handleScroll = () => {
  if (activeTab.value !== 'products' || loadingMore.value || !hasMore.value) return
  const { scrollTop, scrollHeight, clientHeight } = document.documentElement
  if (scrollTop + clientHeight >= scrollHeight - 100) {
    loadProducts()
  }
}

// 更新下划线位置
const updateUnderline = (key: string) => {
  const tab = tabRefs.value[key]
  const container = tabsRef.value
  if (!tab || !container) return

  const tabRect = tab.getBoundingClientRect()
  const containerRect = container.getBoundingClientRect()

  underlineStyle.value = {
    left: (tabRect.left - containerRect.left) + 'px',
    width: tabRect.width + 'px'
  }
}

const handleTabClick = async (key: string) => {
  activeTab.value = key
  nextTick(() => updateUnderline(key))
  if (key === 'reviews' && reviews.value.length === 0) {
    await loadReviews()
  }
}

const loadReviews = async () => {
    reviewsLoading.value = true
    try {
        const response = await authAPI.getSellerReviews(shopId.value, { page: 1, pageSize: 10 })
        if (response.success && response.data) {
            reviews.value = response.data.records?.map((item: any) => {
                const userProfile = item.userProfile || item
                const review = item.review || item
                const reviewImages = item.reviewImages || []
                const reviewVideos = item.reviewVideos || []
                return {
                    id: review.id || item.id,
                    avatar: userProfile?.avatar || defaultAvatar,
                    userName: userProfile?.nickname || '匿名用户',
                    rating: review.rating || 5,
                    content: review.comment || '',
                    time: formatTime(review.createdAt || item.createdAt),
                    images: reviewImages,
                    videos: reviewVideos
                }
            }) || []
            shopInfo.value.reviewCount = response.data.total || reviews.value.length
        } else {
            throw new Error(response.message || '加载失败')
        }
    } catch (error: any) {
        Message.error(error.message || '加载失败')
    } finally {
        reviewsLoading.value = false
    }
}

const formatTime = (dateStr: string) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

const previewImage = (images: any[], index: number) => {
  previewImages.value = images.map(img => img.image)
  previewImageIndex.value = index
  showImagePreview.value = true
}

// 资质图片预览
const previewQualificationImage = (url: string) => {
  previewImages.value = [url]
  previewImageIndex.value = 0
  showImagePreview.value = true
}

const closeImagePreview = () => {
  showImagePreview.value = false
  previewImages.value = []
  previewImageIndex.value = 0
}

const previewVideo = (url: string) => {
  previewVideoUrl.value = url
  showVideoPreview.value = true
}

const closeVideoPreview = () => {
  showVideoPreview.value = false
  previewVideoUrl.value = ''
}

onMounted(async () => {
  loading.value = true
  try {
    await Promise.all([loadShopInfo(), loadProducts()])
  } finally {
    loading.value = false
  }
  nextTick(() => updateUnderline(activeTab.value))
  window.addEventListener('scroll', handleScroll)
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})
</script>

<style scoped>
@import url('@/static/css/user/店铺页.css');
</style>
