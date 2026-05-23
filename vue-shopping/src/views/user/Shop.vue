<template>
  <div class="shop-container">
    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>

    <!-- 店铺内容 -->
    <div v-else class="shop-content">
      <!-- 店铺信息卡片 -->
      <div class="shop-info-card" v-if="sellerInfo">
        <img :src="sellerInfo.storeAvatar || sellerDefaultAvatar" class="shop-logo" />
        <div class="shop-info">
          <div class="shop-name">{{ sellerInfo.storeName || sellerInfo.name || sellerInfo.nickname || '-' }}</div>
          <div class="shop-desc" v-if="sellerInfo.storeDetail">{{ sellerInfo.storeDetail }}</div>
          <div class="shop-stats">
            <span class="stat-item">
              <i class="fas fa-star"></i>
              <span>{{ avgRating }}</span>
            </span>
            <span class="stat-divider">|</span>
            <span class="stat-item">
              <i class="fas fa-shopping-bag"></i>
              <span>{{ products.length }}件在售</span>
            </span>
          </div>
        </div>
      </div>

      <!-- Tab 切换 -->
      <div class="shop-tabs" v-if="sellerInfo">
        <div class="tab" :class="{ active: activeTab === 'products' }" @click="activeTab = 'products'">
          全部商品
        </div>
        <div class="tab" :class="{ active: activeTab === 'reviews' }" @click="activeTab = 'reviews'">
          评价
        </div>
      </div>

      <!-- 商品列表区域 -->
      <div v-show="activeTab === 'products'" class="products-section">
        <div class="product-grid">
          <div v-for="product in products" :key="product.id" class="product-card">
            <img :src="product.images?.split(',')[0]" class="product-image" @click="goToProduct(product.id)" />
            <div class="product-info">
              <div class="product-name">{{ product.name }}</div>
              <div class="product-price">¥{{ formatPrice(product.price) }}</div>
            </div>
          </div>
        </div>

        <!-- 加载更多提示 -->
        <div v-if="loadingMore" class="loading-more">
          <i class="fas fa-spinner fa-spin"></i> 加载中...
        </div>
        <div v-else-if="!hasMore && products.length > 0" class="no-more">
          没有更多了
        </div>
      </div>

      <!-- 评价列表区域 -->
      <div v-show="activeTab === 'reviews'" class="reviews-section">
        <div v-if="reviewsLoading" class="loading-more">
          <i class="fas fa-spinner fa-spin"></i> 加载中...
        </div>
        <div v-else-if="reviews.length === 0" class="no-review">
          暂无评价
        </div>
        <div v-else>
          <div v-for="review in reviews" :key="review.review.id" class="review-card">
            <img :src="review.userProfile?.avatar || userDefaultAvatar" class="review-avatar" />
            <div class="review-content">
              <div class="review-user">{{ review.userProfile?.nickname || '匿名用户' }}</div>
              <div class="review-stars">
                <i v-for="i in 5" :key="i" class="fas fa-star" :class="{ active: i <= (review.review.rating || 5) }"></i>
              </div>
              <div class="review-text">{{ review.review.comment }}</div>
              <div class="review-product">商品：{{ review.productName }}</div>
              <div class="review-time">{{ formatDate(review.review.createdAt) }}</div>
            </div>
          </div>
          <div v-if="!reviewsHasMore && reviews.length > 0" class="no-more">没有更多了</div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-if="!loading && !sellerInfo" class="empty-state">
      <i class="fas fa-store"></i>
      <p>店铺不存在</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { authAPI } from '@/api/authAPI'
import Message from '@/utils/message'
import { useAuthStore } from '@/stores/auth'
import sellerDefaultAvatar from '@/static/images/seller-avatar.jpg'
import userDefaultAvatar from '@/static/images/user-avatar.jpg'

const authStore = useAuthStore()
const router = useRouter()
const route = useRoute()

const sellerId = computed(() => Number(route.params.sellerId))

// ==================== 响应式数据 ====================
const loading = ref(true)
const activeTab = ref('products')

// 商品相关
const loadingMore = ref(false)
const products = ref<any[]>([])
const page = ref(1)
const pageSize = ref(10)
const hasMore = ref(true)

// 评价相关
const sellerInfo = ref<any>(null)
const reviews = ref<any[]>([])
const reviewsPage = ref(1)
const reviewsHasMore = ref(true)
const reviewsLoading = ref(false)
const avgRating = ref<string | number>('-')

// ==================== 数据加载 ====================
const loadSellerInfo = async () => {
  try {
    const response = await authAPI.getSellerInfoById(sellerId.value)
    if (response.success && response.data) {
      sellerInfo.value = response.data
    }
  } catch (error) {
    Message.error('加载店铺信息失败')
  }
}

const loadProducts = async () => {
  if (loadingMore.value) return

  loadingMore.value = true

  try {
    const response = await authAPI.getProducts({
      page: page.value,
      pageSize: pageSize.value,
      sellerId: sellerId.value
    })

    if (response.success && response.data?.records) {
      const records = response.data.records

      if (page.value === 1) {
        products.value = records
      } else {
        products.value.push(...records)
      }

      hasMore.value = records.length >= pageSize.value
      page.value++
    }
  } catch (error) {
    Message.error('加载商品失败')
  } finally {
    loadingMore.value = false
  }
}

const loadReviews = async () => {
  if (reviewsLoading.value || !reviewsHasMore.value) return
  reviewsLoading.value = true
  try {
    const response = await authAPI.getSellerReviews({
      sellerId: sellerId.value,
      page: reviewsPage.value,
      pageSize: 10
    })
    if (response.success && response.data?.records) {
      if (reviewsPage.value === 1) {
        reviews.value = response.data.records
      } else {
        reviews.value.push(...response.data.records)
      }
      reviewsHasMore.value = response.data.records.length >= 10
      reviewsPage.value++

      // 计算平均评分
      if (reviews.value.length > 0) {
        const total = reviews.value.reduce((sum, r) => sum + (r.review?.rating || 0), 0)
        avgRating.value = (total / reviews.value.length).toFixed(1)
      }
    }
  } catch {
    Message.error('加载评价失败')
  } finally {
    reviewsLoading.value = false
  }
}

// ==================== 页面跳转 ====================
const goToProduct = (productId: number) => {
  router.push({ name: 'ProductDetail', params: { productId } })
}

// ==================== 滚动监听 ====================
const handleScroll = () => {
  if (activeTab.value === 'products') {
    if (loadingMore.value || !hasMore.value) return

    const { scrollTop, scrollHeight, clientHeight } = document.documentElement
    if (scrollTop + clientHeight >= scrollHeight - 100) {
      loadProducts()
    }
  } else if (activeTab.value === 'reviews') {
    if (reviewsLoading.value || !reviewsHasMore.value) return

    const { scrollTop, scrollHeight, clientHeight } = document.documentElement
    if (scrollTop + clientHeight >= scrollHeight - 100) {
      loadReviews()
    }
  }
}

// ==================== 工具函数 ====================
const formatPrice = (price: number): string => {
  if (price == null || isNaN(price)) return '0.00'
  return price.toFixed(2)
}

const formatDate = (dateStr: string): string => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

// ==================== 监听 Tab 切换 ====================
watch(activeTab, (newTab) => {
  if (newTab === 'reviews' && reviews.value.length === 0) {
    reviewsPage.value = 1
    reviewsHasMore.value = true
    loadReviews()
  }
})

// ==================== 生命周期 ====================
onMounted(async () => {
  if (!authStore.validateUserPermission()) return

  // 并行加载：商家信息 + 商品 + 评论
  await Promise.all([
    loadSellerInfo(),
    loadProducts(),
    loadReviews()
  ])

  loading.value = false

  loading.value = false

  window.addEventListener('scroll', handleScroll)
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})
</script>

<style scoped>
  @import url('@/static/css/user/店铺页.css');
</style>
