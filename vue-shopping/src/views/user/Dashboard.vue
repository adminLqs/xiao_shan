<template>
  <div class="outer-container">
    <!-- ========== 顶部搜索栏（正常流，滚动时渐隐） ========== -->
    <div class="mobile-search-section" :style="{ opacity: showSearchBar ? 0 : 1 }">
      <div class="search-box">
        <i class="fas fa-search search-icon" @click="router.push({name:'UserSearch'})"></i>
        <input
          type="text"
          class="search-input"
          placeholder="搜索商品..."
          @focus="router.push({name:'UserSearch'})"
          readonly
        >
      </div>
      <i class="fas fa-sync-alt refresh-icon" @click="handleRefresh" :class="{ rotating: loading }"></i>
    </div>

    <!-- ========== 固定搜索栏（滚动时渐隐出现） ========== -->
    <div class="mobile-search-fixed" :style="{ opacity: showSearchBar ? 1 : 0, pointerEvents: showSearchBar ? 'auto' : 'none' }">
      <div class="search-box">
        <i class="fas fa-search search-icon" @click="router.push({name:'UserSearch'})"></i>
        <input
          type="text"
          class="search-input"
          placeholder="搜索商品..."
          @focus="router.push({name:'UserSearch'})"
          readonly
        >
      </div>
      <i class="fas fa-sync-alt refresh-icon" @click="handleRefresh" :class="{ rotating: loading }"></i>
    </div>

    <!-- ========== 一级分类导航 ========== -->
    <div class="level1-category-section">
      <div class="category-container">
        <div class="category-scroll">
          <div
            class="level1-category-item"
            :class="{ active: activeLevel1Id === null }"
            @click="selectLevel1Category(null)"
          >
            <i class="fas fa-fire"></i>
            <span>全部</span>
          </div>
          <div
            v-for="category in level1Categories"
            :key="category.id"
            class="level1-category-item"
            :class="{ active: activeLevel1Id === category.id }"
            @click="selectLevel1Category(category.id)"
          >
            <i :class="category.icon || 'fas fa-tag'"></i>
            <span>{{ category.name }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- ========== 二级分类导航 ========== -->
    <div class="level2-category-section" v-if="activeLevel1Id && level2Categories.length > 0">
      <div class="category-scroll">
        <div
          class="level2-category-item"
          :class="{ active: activeLevel2Id === null }"
          @click="selectLevel2Category(null)"
        >
          <span>全部</span>
        </div>
        <div
          v-for="category in level2Categories"
          :key="category.id"
          class="level2-category-item"
          :class="{ active: activeLevel2Id === category.id }"
          @click="selectLevel2Category(category.id)"
        >
          <span>{{ category.name }}</span>
        </div>
      </div>
    </div>

    <!-- ========== 商家入驻横幅 ========== -->
    <div class="merchant-banner" v-if="!isSeller && !isAdmin">
      <div class="merchant-container">
        <div class="merchant-content">
          <div class="merchant-info">
            <h3 class="merchant-title">加入精品商城，开启电商之旅</h3>
            <p class="merchant-desc">0元入驻 · 海量流量 · 专业扶持</p>
          </div>
          <button class="merchant-btn" @click="goToMerchantApply">
            <i class="fas fa-store"></i>
            <span>立即入驻</span>
            <i class="fas fa-arrow-right"></i>
          </button>
        </div>
      </div>
    </div>

    <!-- ========== 商品轮播图 ========== -->
    <div class="carousel-section">
      <div class="carousel-container">
        <div class="carousel-wrapper">
          <div class="carousel-slides" :style="{ transform: `translateX(-${currentSlide * 100}%)` }">
            <div v-for="(banner, index) in banners" :key="index" class="carousel-slide" @click="handleBannerClick(banner)">
              <img :src="banner.image" :alt="banner.title">
              <div class="carousel-caption" v-if="banner.title">
                <h3>{{ banner.title }}</h3>
                <p>{{ banner.subtitle }}</p>
              </div>
            </div>
          </div>
          <div class="carousel-dots">
            <span v-for="(banner, index) in banners" :key="index" class="dot" :class="{ active: currentSlide === index }" @click="goToSlide(index)"></span>
          </div>
          <button class="carousel-arrow prev" @click="prevSlide"><i class="fas fa-chevron-left"></i></button>
          <button class="carousel-arrow next" @click="nextSlide"><i class="fas fa-chevron-right"></i></button>
        </div>
      </div>
    </div>

    <!-- ========== 商品展示区 ========== -->
    <div class="products-section">
      <div class="products-container">
        <!-- 加载状态 -->
        <div v-if="loading" class="loading-container">
          <i class="fas fa-spinner fa-spin"></i>
          <span>加载中...</span>
        </div>

        <!-- 商品网格 - 2列布局 -->
        <div v-else class="products-grid">
          <div v-for="product in products" :key="product.id" class="product-card" @click="viewProductDetail(product.id)">
            <div class="product-image">
              <span v-if="product.badge" class="product-badge" :style="{ backgroundColor: product.badgeColor }">{{ product.badge }}</span>
              <span v-if="product.stock <= 0" class="out-of-stock-tag">缺货</span>
              <img :src="product.images" :alt="product.name">
              <div class="product-actions">
                <button class="quick-view" @click.stop="quickView(product)"><i class="fas fa-eye"></i></button>
                <button class="add-to-cart" @click.stop="addToCart(product)" :disabled="product.stock <= 0">
                  <i class="fas fa-shopping-cart"></i>
                </button>
              </div>
            </div>
            <div class="product-info">
              <div class="product-title">{{ product.name }}</div>
              <div class="product-price-container">
                <div class="price-row">
                  <span class="product-price">¥{{ formatPrice(product.price) }}</span>
                  <span v-if="product.originalPrice" class="product-original-price">¥{{ formatPrice(product.originalPrice) }}</span>
                </div>
                <div class="product-sales">已售 {{ product.salesCount || 0 }} 件</div>
              </div>
            </div>
          </div>
        </div>

        <div v-if="loadingMore" class="loading-more">
          <i class="fas fa-spinner fa-spin"></i> 加载中...
        </div>
        <div v-else-if="!hasMore && products.length > 0" class="no-more">
          没有更多了
        </div>
      </div>
    </div>

    <!-- ========== 快速查看模态框 ========== -->
    <div v-if="showQuickView" class="modal-overlay" @click="closeQuickView">
      <div class="modal-content" @click.stop>
        <button class="modal-close" @click="closeQuickView"><i class="fas fa-times"></i></button>
        <div class="quick-view-content" v-if="selectedProduct">
          <div class="quick-view-image">
            <img :src="selectedProduct.images" :alt="selectedProduct.name">
          </div>
          <div class="quick-view-info">
            <h3>{{ selectedProduct.name }}</h3>
            <div class="quick-view-price">
              <span class="current-price">¥{{ formatPrice(selectedProduct.price) }}</span>
              <span v-if="selectedProduct.originalPrice" class="original-price">¥{{ formatPrice(selectedProduct.originalPrice) }}</span>
            </div>
            <div class="quick-view-stats">
              <span>销量: {{ selectedProduct.salesCount || 0 }}件</span>
              <span>库存: {{ selectedProduct.stock || 0 }}件</span>
            </div>
            <p class="quick-view-desc">{{ selectedProduct.description || '暂无商品描述' }}</p>
            <button class="buy-now-btn" @click="buyNow(selectedProduct)">立即购买</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 悬浮购物车按钮 -->
    <RouterLink :to="{name: 'Cart'}" class="float-cart-btn">
      <i class="fas fa-shopping-cart"></i>
      <span v-if="cartCount > 0" class="float-cart-badge">{{ cartCount }}</span>
    </RouterLink>

    <!-- 回到顶部按钮 -->
    <div class="back-to-top" :class="{ show: showBackTop }" @click="scrollToTop">
      <i class="fas fa-arrow-up"></i>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { authAPI } from '@/api/authAPI'
import Message from '@/utils/message'
import { useAuthStore } from '@/stores/auth'
import { storeToRefs } from 'pinia'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const { isLoggedIn, isSeller, isAdmin } = storeToRefs(authStore)

// ==================== 类型定义 ====================

interface Category {
  id: number
  name: string
  parentId: number | null
  icon?: string
  isActive: boolean
}

interface Product {
  id: number
  name: string
  brand: string
  price: number
  originalPrice?: number
  stock: number
  images?: string
  salesCount?: number
  description?: string
  badge?: string
  badgeColor?: string
}

interface Banner {
  id: number
  image: string
  title: string
  subtitle: string
  link: string
}

// ==================== 用户相关状态 ====================

const cartCount = ref(0)

// ==================== 分类相关状态 ====================

const allCategories = ref<Category[]>([])
const level1Categories = ref<Category[]>([])
const level2Categories = ref<Category[]>([])
const activeLevel1Id = ref<number | null>(null)
const activeLevel2Id = ref<number | null>(null)

// ==================== 商品相关状态 ====================

const products = ref<Product[]>([])
const loading = ref(false)
const loadingMore = ref(false)
const hasMore = ref(true)
const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = ref(20)

// ==================== UI 状态 ====================

const showQuickView = ref(false)
const selectedProduct = ref<Product | null>(null)
const showBackTop = ref(false)
const showSearchBar = ref(false)

// ==================== 轮播图数据 ====================

const banners = ref<Banner[]>([
  { id: 1, image: 'https://picsum.photos/id/20/800/400', title: '限时秒杀', subtitle: '全场商品低至5折', link: '/user/dashboard?promotion=spring' },
  { id: 2, image: 'https://picsum.photos/id/26/800/400', title: '新品上市', subtitle: '潮流新品抢先购', link: '/user/dashboard?isNew=true' },
  { id: 3, image: 'https://picsum.photos/id/0/800/400', title: '品牌特卖', subtitle: '大牌好物限时抢购', link: '/user/dashboard?brandSale=true' },
  { id: 4, image: 'https://picsum.photos/id/1/800/400', title: '开学季大促', subtitle: '学生专享优惠券免费领', link: '/user/dashboard?promotion=backToSchool' },
  { id: 5, image: 'https://picsum.photos/id/15/800/400', title: '数码狂欢节', subtitle: '爆款数码产品直降1000元', link: '/user/dashboard?promotion=digital' }
])

const currentSlide = ref(0)
let carouselTimer: ReturnType<typeof setInterval> | null = null

// ==================== 计算属性 ====================

const currentCategoryName = computed(() => {
  if (activeLevel2Id.value) {
    const category = level2Categories.value.find(c => c.id === activeLevel2Id.value)
    return category ? category.name : '全部商品'
  }
  if (activeLevel1Id.value) {
    const category = level1Categories.value.find(c => c.id === activeLevel1Id.value)
    return category ? category.name : '全部商品'
  }
  return '全部商品'
})



// ==================== 用户相关函数 ====================

const loadCartCount = async () => {
  if (!isLoggedIn.value) return

  try {
    const response = await authAPI.getCartCount()
    if (response.success) {
      cartCount.value = response.data || 0
    }
  } catch (error) {
    Message.error('加载购物车数量失败:' + error)
    cartCount.value = 0
  }
}

// ==================== 分类相关函数 ====================

const loadCategories = async () => {
  try {
    const response = await authAPI.getAllCategories()
    if (response.success && response.data?.categories) {
      allCategories.value = response.data.categories
      level1Categories.value = allCategories.value.filter(
        cat => cat.parentId === null && cat.isActive
      )
    }
  } catch (error) {
    Message.error('加载分类失败:' + error)
  }
}

const loadLevel2Categories = () => {
  if (activeLevel1Id.value) {
    level2Categories.value = allCategories.value.filter(
      cat => cat.parentId === activeLevel1Id.value && cat.isActive
    )
  } else {
    level2Categories.value = []
  }
}

const selectLevel1Category = (categoryId: number | null) => {
  activeLevel1Id.value = categoryId
  activeLevel2Id.value = null
  loadLevel2Categories()
  currentPage.value = 1
  hasMore.value = true
  loadProducts()
}

const selectLevel2Category = (categoryId: number | null) => {
  activeLevel2Id.value = categoryId
  currentPage.value = 1
  hasMore.value = true
  loadProducts()
}

// ==================== 商品相关函数 ====================

const loadProducts = async () => {
  loading.value = true

  try {
    const params: any = {
      page: currentPage.value,
      pageSize: pageSize.value
    }

    if (searchKeyword.value) {
      params.keyword = searchKeyword.value
    }

    if (activeLevel2Id.value) {
      params.level2CategoryId = activeLevel2Id.value
    } else if (activeLevel1Id.value) {
      params.level1CategoryId = activeLevel1Id.value
    }

    const response = await authAPI.getProducts(params)

    if (response.success) {
      if (currentPage.value === 1) {
        products.value = response.data?.records || []
      } else {
        products.value.push(...(response.data?.records || []))
      }
      hasMore.value = (response.data?.records || []).length >= pageSize.value
    } else {
      throw new Error(response.message || '加载商品失败')
    }
  } catch (error: any) {
    Message.error('加载商品失败:', error)
    if (currentPage.value === 1) {
      products.value = []
    }
  } finally {
    loading.value = false
  }
}

const handleScroll = () => {
  showBackTop.value = window.scrollY > 300
  showSearchBar.value = window.scrollY > 150

  if (loadingMore.value || !hasMore.value || loading.value) return
  const { scrollTop, scrollHeight, clientHeight } = document.documentElement
  if (scrollTop + clientHeight >= scrollHeight - 150) {
    loadMore()
  }
}

const scrollToTop = () => {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const loadMore = async () => {
  if (loadingMore.value || !hasMore.value) return
  loadingMore.value = true
  currentPage.value++
  await loadProducts()
  loadingMore.value = false
}

const performSearch = () => {
  if (searchKeyword.value.trim()) {
    currentPage.value = 1
    loadProducts()
  }
}

// ==================== UI 交互函数 ====================

const viewProductDetail = (productId: number) => {
  router.push({
    name: 'ProductDetail',
    params: { productId }
  })
}

const quickView = (product: Product) => {
  selectedProduct.value = product
  showQuickView.value = true
}

const closeQuickView = () => {
  showQuickView.value = false
  selectedProduct.value = null
}

const addToCart = async (product: Product) => {
  if (!isLoggedIn.value) {
    Message.error('请先登录后再添加')
    router.push('/login')
    return
  }

  try {
    const response = await authAPI.addToCart({ productId: product.id, quantity: 1 })
    if (response.success) {
      Message.success('已添加到购物车')
      await loadCartCount()
    }
  } catch (error) {
    Message.error('添加失败')
  }
}

const buyNow = (product: Product) => {
  router.push({
    name: 'Checkout',
    query: {
      'productId': `${product.id}`,
      'quantity': 1
    }
  })
}

const goToMerchantApply = () => {
  router.push('/merchant/apply')
}

// ==================== 刷新函数 ====================

const handleRefresh = async () => {
  currentPage.value = 1
  hasMore.value = true
  await loadProducts()
  Message.success('已刷新')
}

// ==================== 轮播图控制函数 ====================

const nextSlide = () => {
  currentSlide.value = (currentSlide.value + 1) % banners.value.length
}

const prevSlide = () => {
  currentSlide.value = currentSlide.value === 0 ? banners.value.length - 1 : currentSlide.value - 1
}

const goToSlide = (index: number) => {
  currentSlide.value = index
}

const startCarousel = () => {
  if (carouselTimer) clearInterval(carouselTimer)
  carouselTimer = setInterval(nextSlide, 5000)
}

const pauseCarousel = () => {
  if (carouselTimer) clearInterval(carouselTimer)
}

const handleBannerClick = (banner: Banner) => {
  if (!banner.link) return

  // 解析链接中的活动参数
  const url = new URL(banner.link, window.location.origin)
  const params = url.searchParams

  // 清空分类选择，确保只按关键词搜索
  activeLevel1Id.value = null
  activeLevel2Id.value = null
  level2Categories.value = []

  // 根据活动类型设置搜索关键词
  if (params.get('promotion') === 'spring') {
    // 春季促销
    searchKeyword.value = '春季促销'
  } else if (params.get('isNew') === 'true') {
    // 新品上市
    searchKeyword.value = '新品'
  } else if (params.get('brandSale') === 'true') {
    // 品牌特卖
    searchKeyword.value = '品牌'
  } else if (params.get('promotion') === 'backToSchool') {
    // 开学季大促
    searchKeyword.value = '开学季'
  } else if (params.get('promotion') === 'digital') {
    // 数码狂欢节
    searchKeyword.value = '数码'
  } else {
    // 默认行为：跳转到链接
    router.push(banner.link)
    return
  }

  // 重置分页并刷新商品列表
  currentPage.value = 1
  hasMore.value = true
  loadProducts()
}

// ==================== 工具函数 ====================

const formatPrice = (price: number): string => {
  if (price == null || isNaN(price)) return '0.00'
  return price.toFixed(2)
}

// ==================== 生命周期 ====================

onMounted(async () => {
  if (!authStore.validateAccountStatus()) {
    return
  }

  if (isLoggedIn.value) {
    await loadCartCount()
  }

  await loadCategories()

  const queryKeyword = route.query.keyword as string
  if (queryKeyword) {
    searchKeyword.value = queryKeyword
  }

  await loadProducts()

  startCarousel()

  window.addEventListener('scroll', handleScroll)
})

onUnmounted(() => {
  if (carouselTimer) clearInterval(carouselTimer)
  window.removeEventListener('scroll', handleScroll)
})
</script>

<style scoped>
  @import url('https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css');
  @import url('@/static/css/user/用户首页.css');
</style>
