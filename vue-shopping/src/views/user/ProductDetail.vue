<template>
  <div class="product-detail-container">
    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>

    <!-- 商品详情内容 -->
    <div v-else-if="product" class="product-detail-content">
      <!-- ========== 顶部商家栏（放在主图上方） ========== -->
      <div class="seller-bar" v-if="sellerInfo">
        <div class="seller-info" @click="goToShop">
          <img :src="sellerInfo.storeAvatar || sellerDefaultAvatar" class="seller-avatar" />
          <div class="seller-detail">
            <div class="seller-name">{{ sellerInfo.storeName || sellerInfo.name || sellerInfo.nickname || '-' }}</div>
            <div class="seller-meta" v-if="sellerInfo.fansCount">
              <span>{{ sellerInfo.fansCount }}粉丝</span>
              <span v-if="sellerInfo.rating">| 评分 {{ sellerInfo.rating }}</span>
            </div>
          </div>
        </div>
        <button class="follow-btn" :class="{ followed: isFollowed }" @click="toggleFollow">
          {{ isFollowed ? '已关注' : '+ 关注' }}
        </button>
      </div>

      <!-- ========== 主图 Swiper（全宽，支持左右滑动 + 自动播放） ========== -->
      <div class="product-swiper" @touchstart="onTouchStart" @touchend="onTouchEnd">
        <div class="swiper-track" :style="{ transform: `translateX(-${currentIndex * 100}%)` }">
          <img v-for="(img, i) in productImages" :key="i" :src="img" class="swiper-image" />
        </div>
        <div class="swiper-dots">
          <span v-for="(img, i) in productImages" :key="i"
                class="dot" :class="{ active: i === currentIndex }"></span>
        </div>
      </div>

      <!-- ========== 价格区 ========== -->
      <div class="price-section">
        <span class="current-price">¥{{ formatPrice(product.price) }}</span>
        <span class="original-price" v-if="product.originalPrice">¥{{ formatPrice(product.originalPrice) }}</span>
        <span class="discount-tag" v-if="product.originalPrice">{{ discountPercent }}折</span>
        <span class="sales-count">已售 {{ product.salesCount || 0 }}件</span>
      </div>

      <!-- ========== 商品标题 ========== -->
      <div class="title-section">
        <div class="product-title" :class="{ expanded: titleExpanded }" @click="titleExpanded = !titleExpanded">
          {{ product.name }}
        </div>
      </div>

      <!-- ========== 优惠券/满减标签 ========== -->
      <div class="promo-tags" v-if="promoTags.length > 0">
        <span class="promo-tag" v-for="tag in promoTags" :key="tag">{{ tag }}</span>
      </div>

      <!-- ========== 灰色间隔 ========== -->
      <div class="gray-divider"></div>

      <!-- ========== 选择规格/数量行 ========== -->
      <div class="service-rows">
        <div class="service-row" @click="showSku = true">
          <span>📐 选择规格/数量</span>
          <i class="fas fa-chevron-right"></i>
        </div>
      </div>

      <!-- ========== 灰色间隔 ========== -->
      <div class="gray-divider"></div>

      <!-- ========== 商品详情（可折叠） ========== -->
      <div class="detail-section">
        <div class="section-title" @click="detailExpanded = !detailExpanded">
          商品详情
          <i class="fas" :class="detailExpanded ? 'fa-chevron-up' : 'fa-chevron-down'"></i>
        </div>
        <div class="detail-content" v-show="detailExpanded" v-html="product.detailHtml || product.description"></div>
      </div>

      <!-- ========== 灰色间隔 ========== -->
      <div class="gray-divider"></div>

      <!-- ========== 用户评价预览 ========== -->
      <div class="comment-preview" @click="openComments">
        <div class="section-title">
          用户评价 ({{ commentTotal }})
          <span class="view-all">全部评价 <i class="fas fa-chevron-right"></i></span>
        </div>
        <div class="comment-item" v-if="latestComment">
          <img :src="latestComment.avatar || defaultAvatar" class="comment-avatar" />
          <div class="comment-content">
            <div class="comment-user">{{ latestComment.userName || '匿名用户' }}</div>
            <div class="comment-stars">
              <i v-for="i in 5" :key="i" class="fas fa-star" :class="{ active: i <= (latestComment.rating || 5) }"></i>
            </div>
            <div class="comment-text">{{ latestComment.content }}</div>
          </div>
        </div>
      </div>

      <!-- 底部留空 -->
      <div class="bottom-space"></div>
    </div>

    <!-- 空状态 -->
    <div v-else class="empty-state">
      <i class="fas fa-box-open"></i>
      <p>商品不存在或已下架</p>
    </div>

    <!-- ========== 底部固定操作栏 ========== -->
    <div class="bottom-bar">
      <div class="bottom-icon" @click="toggleFavorite">
        <i :class="isFavorited ? 'fas fa-heart' : 'far fa-heart'" :style="{ color: isFavorited ? '#ff4757' : '#666' }"></i>
        <span>收藏</span>
      </div>
      <div class="bottom-icon">
        <i class="fas fa-headset"></i>
        <span>客服</span>
      </div>
      <button class="btn-cart" @click="handleAddToCart" :disabled="product?.status === 0 || product?.stock <= 0">加入购物车</button>
      <button class="btn-buy" @click="handleBuyNow" :disabled="product?.status === 0 || product?.stock <= 0">立即购买</button>
    </div>

    <!-- ========== SKU 选择弹窗 ========== -->
    <div class="sku-modal" v-if="showSku" @click.self="showSku = false">
      <div class="sku-content">
        <div class="sku-header">
          <img :src="currentImage || productImages[0]" class="sku-image" />
          <div class="sku-info">
            <div class="sku-name">{{ product?.name }}</div>
            <span class="sku-price">
              ¥{{ formatPrice(minPrice) }} - ¥{{ formatPrice(maxPrice) }}
            </span>
          </div>
          <button class="sku-close" @click="showSku = false">
            <i class="fas fa-times"></i>
          </button>
        </div>

        <div class="sku-body">
          <!-- 已选规格标签 -->
          <div v-if="selectedSkus.length > 0" class="selected-skus">
            <div class="selected-label">已选规格：</div>
            <div class="selected-tags">
              <span
                v-for="item in selectedSkus"
                :key="item.skuId"
                class="selected-tag"
              >
                {{ getSkuName(item.skuId) }}
                <i class="fas fa-times" @click="removeSku(item.skuId)"></i>
              </span>
            </div>
          </div>

          <!-- SKU 列表 -->
          <div v-if="skuList.length > 0" class="sku-list">
            <div class="sku-list-label">规格选择：</div>
            <div v-for="sku in skuList" :key="sku.id" class="sku-item">
              <img :src="sku.skuImage || productImages[0]" class="sku-item-image" />
              <div class="sku-item-info">
                <div class="sku-item-name">{{ sku.skuName }}</div>
                <div class="sku-item-price">¥{{ formatPrice(sku.price) }}</div>
                <div class="sku-item-stock">库存 {{ sku.stock }} 件</div>
              </div>
              <div class="sku-item-control">
                <div v-if="isMultiSelect" class="quantity-control">
                  <button
                    @click="decreaseSkuQuantity(sku.id)"
                    :disabled="getSkuQuantity(sku.id) <= 0"
                  >-</button>
                  <span>{{ getSkuQuantity(sku.id) }}</span>
                  <button
                    @click="increaseSkuQuantity(sku.id)"
                    :disabled="getSkuQuantity(sku.id) >= sku.stock"
                  >+</button>
                </div>
                <div v-else class="select-btn" @click="toggleSingleSku(sku.id)">
                  <span v-if="getSkuQuantity(sku.id) > 0">已选</span>
                  <span v-else>选择</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 多选开关 -->
          <div class="multi-select-switch">
            <label class="checkbox-label">
              <input type="checkbox" v-model="isMultiSelect" @change="onMultiSelectChange" />
              <span>多选购买（可同时选多个规格）</span>
            </label>
          </div>
        </div>

        <div class="sku-footer">
          <div v-if="selectedSkus.length > 0" class="total-info">
            合计：<span class="total-amount">¥{{ formatPrice(totalAmount) }}</span>
          </div>
          <div class="btn-group">
            <button
              class="btn-sku-cart"
              @click="addToCart"
              :disabled="selectedSkus.length === 0"
            >加入购物车</button>
            <button
              class="btn-sku-buy"
              @click="buyNow"
              :disabled="selectedSkus.length === 0"
            >立即购买</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { authAPI } from '@/api/authAPI'
import Message from '@/utils/message'
import { useAuthStore } from '@/stores/auth'
import sellerDefaultAvatar from '@/static/images/seller-avatar.jpg'

const authStore = useAuthStore()
const router = useRouter()
const route = useRoute()

const productId = computed(() => Number(route.params.productId))

// ==================== 响应式数据 ====================
const loading = ref(true)
const product = ref<any>(null)
const isFavorited = ref(false)
const titleExpanded = ref(false)
const detailExpanded = ref(true)
const showSku = ref(false)

// Swiper相关
const currentIndex = ref(0)
const touchStartX = ref(0)
const touchEndX = ref(0)
let autoPlayTimer: ReturnType<typeof setInterval> | null = null

// 评价相关
const commentTotal = ref(0)
const latestComment = ref<any>(null)

// 店铺信息（真实接口数据）
const sellerInfo = ref<any>(null)
const isFollowed = ref(false)

// 优惠券标签（真实接口数据）
const promoTags = ref<string[]>([])

// SKU 相关
const skuList = ref<any[]>([])
const isMultiSelect = ref(false)
interface SelectedSku {
  skuId: number
  quantity: number
}
const selectedSkus = ref<SelectedSku[]>([])

// ==================== 计算属性 ====================
const productImages = computed(() => {
  if (!product.value?.images) return []
  if (typeof product.value.images === 'string') {
    return product.value.images.split(',').filter(img => img.trim())
  }
  return product.value.images
})

// ==================== SKU 相关计算属性 ====================
const totalQuantity = computed(() => {
  return selectedSkus.value.reduce((sum, item) => sum + item.quantity, 0)
})

const totalAmount = computed(() => {
  return selectedSkus.value.reduce((sum, item) => {
    const sku = skuList.value.find(s => s.id === item.skuId)
    return sum + (sku?.price || 0) * item.quantity
  }, 0)
})

const currentImage = computed(() => productImages.value[currentIndex.value] || '')

const minPrice = computed(() => {
  if (skuList.value.length === 0) return 0
  return Math.min(...skuList.value.map(sku => sku.price || 0))
})

const maxPrice = computed(() => {
  if (skuList.value.length === 0) return 0
  return Math.max(...skuList.value.map(sku => sku.price || 0))
})

const discountPercent = computed(() => {
  if (!product.value?.originalPrice) return ''
  const percent = Math.round((product.value.price / product.value.originalPrice) * 10)
  return percent.toString()
})

// ==================== 商品详情加载 ====================
const loadProductDetail = async () => {
  loading.value = true
  try {
    const response = await authAPI.getProduct(productId.value)
    if (response.success && response.data?.product) {
      product.value = response.data.product
      commentTotal.value = product.value.commentCount || 0
      await loadSkuList()
    }
  } catch (error: any) {
    Message.error(error.message || '加载商品失败')
  } finally {
    loading.value = false
  }
}

// ==================== 加载 SKU 列表 ====================
const loadSkuList = async () => {
  try {
    const response = await authAPI.getProductSkus(productId.value)
    if (response.success && response.data?.skus) {
      skuList.value = response.data.skus
    }
  } catch {}
}

// ==================== SKU 选择逻辑 ====================
const getSkuName = (skuId: number): string => {
  const sku = skuList.value.find(s => s.id === skuId)
  return sku?.skuName || ''
}

const getSkuQuantity = (skuId: number): number => {
  const item = selectedSkus.value.find(s => s.skuId === skuId)
  return item?.quantity || 0
}

const increaseSkuQuantity = (skuId: number) => {
  const sku = skuList.value.find(s => s.id === skuId)
  if (!sku) return

  const existing = selectedSkus.value.find(s => s.skuId === skuId)
  if (existing) {
    if (existing.quantity < sku.stock) {
      existing.quantity++
    }
  } else {
    selectedSkus.value.push({ skuId, quantity: 1 })
  }
}

const decreaseSkuQuantity = (skuId: number) => {
  const existing = selectedSkus.value.find(s => s.skuId === skuId)
  if (existing) {
    if (existing.quantity <= 1) {
      removeSku(skuId)
    } else {
      existing.quantity--
    }
  }
}

const toggleSingleSku = (skuId: number) => {
  if (isMultiSelect.value) return

  const existing = selectedSkus.value.find(s => s.skuId === skuId)
  if (existing) {
    if (existing.quantity > 1) {
      existing.quantity = 1
    } else {
      selectedSkus.value = []
    }
  } else {
    selectedSkus.value = [{ skuId, quantity: 1 }]
  }
}

const removeSku = (skuId: number) => {
  const index = selectedSkus.value.findIndex(s => s.skuId === skuId)
  if (index > -1) {
    selectedSkus.value.splice(index, 1)
  }
}

const onMultiSelectChange = () => {
  if (!isMultiSelect.value) {
    if (selectedSkus.value.length > 1) {
      selectedSkus.value = selectedSkus.value.slice(0, 1)
    }
  }
}

const resetSpecs = () => {
  selectedSkus.value = []
  isMultiSelect.value = false
}

// ==================== 加载店铺信息 ====================
const loadSellerInfo = async () => {
  try {
    const response = await authAPI.getSellerByProduct(productId.value)
    if (response.success && response.data?.seller) {
      sellerInfo.value = response.data.seller
      await checkFollowStatus()
    }
  } catch (error: any) {
    if (error.status !== 403) {
      Message.error('店铺信息加载失败')
    }
  }
}

// ==================== 检查关注状态 ====================
const checkFollowStatus = async () => {
  if (!product.value?.sellerId) return
  try {
    const response = await authAPI.checkFollowSeller(product.value.sellerId)
    if (response.success) {
      isFollowed.value = response.data?.isFollowed || false
    }
  } catch {}
}

// ==================== 加载优惠券 ====================
const loadCoupons = async () => {
  try {
    const response = await authAPI.getProductCoupons(productId.value)
    if (response.success && response.data?.coupons) {
      promoTags.value = response.data.coupons.map((coupon: any) => coupon.name || coupon.title)
    }
  } catch (error: any) {
    promoTags.value = []
  }
}

// ==================== 加载评价 ====================
const loadLatestComment = async () => {
  try {
    const response = await authAPI.getProductReviews({
      productId: productId.value,
      page: 1,
      size: 1
    })
    if (response.success && response.data?.records?.length > 0) {
      const comment = response.data.records[0]
      latestComment.value = {
        avatar: comment.userProfile?.avatar || comment.avatar,
        userName: comment.userProfile?.nickname || comment.userName,
        rating: comment.review?.rating || comment.rating,
        content: comment.review?.comment || comment.content
      }
    }
  } catch (error) {
  }
}

// ==================== 收藏相关 ====================
const checkFavoriteStatus = async () => {
  try {
    const response = await authAPI.checkFavorite(productId.value)
    if (response.success) {
      isFavorited.value = response.data?.isFavorited || false
    }
  } catch (error) {
  }
}

const toggleFavorite = async () => {
  try {
    if (isFavorited.value) {
      const response = await authAPI.removeFavorite(productId.value)
      if (response.success) {
        isFavorited.value = false
        Message.success('已取消收藏')
      }
    } else {
      const response = await authAPI.addFavorite(productId.value)
      if (response.success) {
        isFavorited.value = true
        Message.success('收藏成功')
      }
    }
  } catch (error: any) {
    Message.error(error.message || '操作失败')
  }
}

// ==================== 关注店铺 ====================
const toggleFollow = async () => {
  if (!product.value?.sellerId) return
  try {
    if (isFollowed.value) {
      await authAPI.unfollowSeller(product.value.sellerId)
      isFollowed.value = false
      Message.success('已取消关注')
    } else {
      await authAPI.followSeller(product.value.sellerId)
      isFollowed.value = true
      Message.success('关注成功')
    }
  } catch (error: any) {
    Message.error(error.message || '操作失败')
  }
}

// ==================== 底部栏操作 ====================
const handleAddToCart = () => {
  if (skuList.value.length > 0) {
    resetSpecs()
    showSku.value = true
  } else {
    addToCart()
  }
}

const handleBuyNow = () => {
  if (skuList.value.length > 0) {
    resetSpecs()
    showSku.value = true
  } else {
    buyNow()
  }
}

// ==================== 购物车和购买 ====================
const addToCart = async () => {
  if (skuList.value.length === 0) {
    try {
      const response = await authAPI.addToCart({
        productId: product.value.id,
        quantity: 1
      })
      if (response.success) {
        Message.success('已添加到购物车')
      }
    } catch (error: any) {
      Message.error(error.message || '添加失败')
    }
    return
  }

  if (selectedSkus.value.length === 0) {
    Message.error('请选择规格')
    return
  }

  try {
    for (const item of selectedSkus.value) {
      await authAPI.addToCart({
        productId: product.value.id,
        quantity: item.quantity,
        skuId: item.skuId
      })
    }
    Message.success(`已添加 ${totalQuantity.value} 件商品到购物车`)
    showSku.value = false
    resetSpecs()
  } catch (error: any) {
    Message.error(error.message || '添加失败')
  }
}

const buyNow = () => {
  if (skuList.value.length === 0) {
    router.push({
      name: 'Checkout',
      query: {
        source: 'product',
        productId: product.value.id,
        quantity: 1
      }
    })
    return
  }

  if (selectedSkus.value.length === 0) {
    Message.error('请选择规格')
    return
  }

  const skuIds = selectedSkus.value.map(item => `${item.skuId}:${item.quantity}`).join(',')
  router.push({
    name: 'Checkout',
    query: {
      source: 'product',
      productId: product.value.id,
      skuIds
    }
  })
  showSku.value = false
  resetSpecs()
}

// ==================== Swiper滑动 + 自动播放 ====================
const onTouchStart = (e: TouchEvent) => {
  stopAutoPlay()
  touchStartX.value = e.touches[0].clientX
}

const onTouchEnd = (e: TouchEvent) => {
  touchEndX.value = e.changedTouches[0].clientX
  const diff = touchStartX.value - touchEndX.value
  if (Math.abs(diff) > 50) {
    if (diff > 0 && currentIndex.value < productImages.value.length - 1) {
      currentIndex.value++
    } else if (diff < 0 && currentIndex.value > 0) {
      currentIndex.value--
    }
  }
  startAutoPlay()
}

const startAutoPlay = () => {
  stopAutoPlay()
  autoPlayTimer = setInterval(() => {
    currentIndex.value = (currentIndex.value + 1) % productImages.value.length
  }, 3000)
}

const stopAutoPlay = () => {
  if (autoPlayTimer) {
    clearInterval(autoPlayTimer)
    autoPlayTimer = null
  }
}

// ==================== 页面跳转 ====================
const goToShop = () => {
  if (product.value?.sellerId) {
    router.push({ name: 'Shop', params: { sellerId: product.value.sellerId } })
  }
}

const openComments = () => {
  Message.info('评价页面开发中')
}

// ==================== 工具函数 ====================
const formatPrice = (price: number) => {
  if (price == null || isNaN(price)) return '0.00'
  return price.toFixed(2)
}

// ==================== 生命周期 ====================
onMounted(async () => {
  if (!authStore.validateUserPermission()) return

  await loadProductDetail()
  await checkFavoriteStatus()
  await loadLatestComment()
  await loadSellerInfo()
  await loadCoupons()
  startAutoPlay()
})

onUnmounted(() => {
  stopAutoPlay()
})
</script>

<style scoped>
  @import url("@/static/css/user/商品详情.css");
</style>
