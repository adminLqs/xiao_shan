<template>
  <div class="favorites-page">
    <!-- 导航栏 -->
    <div class="page-navbar">
      <button class="page-nav-back" @click="router.back()">
        <i class="fas fa-chevron-left"></i>
      </button>
      <div class="page-nav-title">
        <span>我的收藏</span>
      </div>
      <div class="page-nav-count" v-if="favorites.length > 0">{{ favorites.length }}件</div>
    </div>

    <!-- 骨架屏 -->
    <div v-if="loading" class="favorites-content">
      <div class="cart-list">
        <div v-for="n in 4" :key="n" class="skeleton-card">
          <div class="cart-item-top">
            <div class="skeleton-avatar" style="width:70px;height:70px;border-radius:8px;"></div>
            <div class="item-info">
              <div class="skeleton-line medium"></div>
              <div class="skeleton-line short" style="width:40%"></div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else-if="favorites.length === 0" class="empty-cart">
      <i class="fas fa-heart"></i>
      <p>暂无收藏商品</p>
      <RouterLink :to="{name: 'UserDashboard'}" class="btn btn-primary">去逛逛</RouterLink>
    </div>

    <!-- 收藏列表 -->
    <div v-else class="favorites-content">
      <div
        v-for="[date, items] in groupedFavorites"
        :key="date"
      >
        <div class="date-divider">{{ date }}</div>
        <div class="cart-list">
          <div
            v-for="item in items"
            :key="item.id"
            class="favorite-item"
            :class="{ 'out-of-stock': item.stock <= 0 || item.productStatus === 2 }"
            @click="handleViewProduct(item)"
          >
            <div class="order-header">
              <div class="order-header-top">
                <div class="order-header-left">
                  <img :src="item.sellerAvatar || sellerDefaultAvatar" class="seller-avatar" :alt="item.sellerName" />
                  <span class="seller-name">{{ item.sellerName }}</span>
                </div>
              </div>
            </div>
            <div class="order-items">
              <div class="order-item">
                <div class="item-image">
                  <img :src="item.productImage || defaultProductImage" :alt="item.productName" />
                  <div v-if="item.productStatus === 2" class="image-overlay">
                    <span class="deleted-tag">已下架</span>
                  </div>
                </div>

                <div class="item-info">
                  <div class="item-name">{{ item.productName }}</div>
                  <div class="tags-group">
                    <span v-if="item.skuName" class="tag-spec">{{ item.skuName }}</span>
                  </div>
                </div>

                <div class="price-col">
                  <div class="item-price">¥{{ formatPrice(item.price) }}</div>
                  <div v-if="item.originalPrice && item.originalPrice > item.price && item.productStatus !== 2" class="original-price">¥{{ formatPrice(item.originalPrice) }}</div>
                </div>
                <button class="delete-btn" @click.stop="removeFavorite(item.id)">
                  <i class="fas fa-trash-alt"></i>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-if="loadingMore" class="loading-more-bar">
        <i class="fas fa-spinner fa-spin"></i> 加载更多...
      </div>
      <div v-else-if="!hasMore && favorites.length > 0" class="no-more-bar">— 已经到底了 —</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { authAPI } from '@/api/authAPI'
import Message from '@/utils/message'
import { useAuthStore } from '@/stores/auth'
import sellerDefaultAvatar from '@/static/images/seller-avatar.jpg'
import defaultProductImage from '@/static/images/云杉购图标.jpg'

const authStore = useAuthStore()
const router = useRouter()

// ==================== 类型定义 ====================

interface ApiResponse<T = any> {
  success: boolean
  data: T
  total?: number
  totalPages?: number
  page?: number
  size?: number
  message?: string
}

interface FavoriteItem {
  id: number
  productId: number
  skuId?: number
  productName: string
  skuName?: string
  brand: string
  price: number
  originalPrice?: number
  stock: number
  productImage: string
  createdAt: string
  productStatus?: number // 商品状态（0-下架，1-上架，2-已删除）
}

// ==================== 响应式数据 ====================

const loading = ref(true)
const loadingMore = ref(false)
const hasMore = ref(true)
const favorites = ref<FavoriteItem[]>([])
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)

// 按日期分组
const groupedFavorites = computed(() => {
  const groups: Record<string, any[]> = {}
  favorites.value.forEach(item => {
    const date = formatDateGroup(item.createdAt)
    if (!groups[date]) groups[date] = []
    groups[date].push(item)
  })
  return Object.entries(groups)
})

// ==================== 数据加载 ====================

const loadFavorites = async () => {
  if (loadingMore.value) return

  if (currentPage.value === 1) {
    loading.value = true
  } else {
    loadingMore.value = true
  }

  try {
    const response = await authAPI.getFavorites({
      page: currentPage.value,
      pageSize: pageSize.value
    })

    if (response.success) {
      const data = response.data || {}
      const items = Array.isArray(data) ? data : (data.records || [])

      if (currentPage.value === 1) {
        favorites.value = items
      } else {
        favorites.value.push(...items)
      }

      total.value = data.total || items.length
      hasMore.value = items.length >= pageSize.value
    }
  } catch (error: unknown) {
    const err = error as { message?: string }
    Message.error(err?.message || '加载失败')
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

const loadMore = async () => {
  if (loadingMore.value || !hasMore.value) return
  currentPage.value++
  await loadFavorites()
}

const handleScroll = () => {
  if (loadingMore.value || !hasMore.value || loading.value) return

  const { scrollTop, scrollHeight, clientHeight } = document.documentElement
  if (scrollTop + clientHeight >= scrollHeight - 150) {
    loadMore()
  }
}

// ==================== 查看商品 ====================

const handleViewProduct = (item: FavoriteItem) => {
    if (item.productStatus === 2) {
      Message.confirm('商品已下架，是否删除收藏？').then(() => removeFavorite(item.id))
      return
    }
    if (item.stock <= 0) {
      Message.confirm('商品已售罄，是否删除收藏？').then(() => removeFavorite(item.id))
      return
    }
    viewProduct(item.productId)
  }

// ==================== 收藏操作 ====================

const removeFavorite = async (favoriteId: number) => {
  try {
    await Message.confirm('确定要删除该收藏吗？', '删除确认')

    const response = await authAPI.removeFavorite(favoriteId)
    if (response.success) {
      Message.success('删除成功')
      currentPage.value = 1
      hasMore.value = true
      await loadFavorites()
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      Message.error(error.message || '删除失败')
    }
  }
}

// ==================== 页面跳转 ====================

const viewProduct = (productId: number) => {
  router.push({
    name: 'ProductDetail',
    params: { productId }
  })
}

// ==================== 工具函数 ====================

const formatPrice = (price: number): string => {
  if (price == null || isNaN(price)) return '0.00'
  return price.toFixed(2)
}

const formatDate = (dateStr: string): string => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

// 用于分组的日期格式化（今天/昨天/YYYY-MM-DD）
const formatDateGroup = (dateStr: string): string => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  const today = new Date()
  const yesterday = new Date(today.getTime() - 86400000)

  if (date.toDateString() === today.toDateString()) return '今天'
  if (date.toDateString() === yesterday.toDateString()) return '昨天'

  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

// ==================== 生命周期 ====================

onMounted(() => {
  if (!authStore.validateUserPermission()) return

  loadFavorites()
  window.addEventListener('scroll', handleScroll)
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})
</script>

<style scoped>
@import url('@/static/css/user/收藏页.css');
</style>
