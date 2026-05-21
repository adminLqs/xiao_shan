<template>
  <div class="outer-container">
    <!-- ========== 顶部导航栏 - 移动端样式 ========== -->
    <div class="top-navigation">
      <div class="nav-container">
        <!-- 汉堡菜单按钮 -->
        <button class="hamburger-btn" @click="toggleDrawer">
          <span></span>
          <span></span>
          <span></span>
        </button>

        <!-- 品牌Logo居中 -->
        <RouterLink to="/" class="brand-logo">
          <i class="fas fa-shopping-bag"></i>
          <span>精品商城</span>
        </RouterLink>

        <!-- 右侧购物车图标 -->
        <RouterLink :to="{name: 'Cart'}" class="cart-btn">
          <i class="fas fa-shopping-cart"></i>
          <span v-if="cartCount > 0" class="cart-count">{{ cartCount }}</span>
        </RouterLink>
      </div>
    </div>

    <!-- ========== 移动端搜索栏 ========== -->
    <div class="mobile-search-section">
      <div class="search-box">
        <i class="fas fa-search search-icon"></i>
        <input
          type="text"
          class="search-input"
          placeholder="搜索商品..."
          v-model="searchKeyword"
          @keyup.enter="performSearch"
        >
      </div>
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
            <i class="fas fa-th-large"></i>
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
        <div class="section-header">
          <h2 class="section-title">
            <i class="fas fa-fire" style="color: #ff6b6b; margin-right: 8px;"></i>
            {{ currentCategoryName }}
          </h2>
        </div>

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

        <!-- 分页组件 -->
        <div class="pagination" v-if="totalPages > 1">
          <button class="page-btn" :disabled="currentPage === 1" @click="changePage(currentPage - 1)">
            <i class="fas fa-chevron-left"></i>
          </button>
          <button v-for="page in visiblePages" :key="page" class="page-btn" :class="{ active: currentPage === page }" @click="changePage(page)">
            {{ page }}
          </button>
          <button class="page-btn" :disabled="currentPage === totalPages" @click="changePage(currentPage + 1)">
            <i class="fas fa-chevron-right"></i>
          </button>
        </div>
      </div>
    </div>

    <!-- ========== 页脚 ========== -->
    <div class="footer">
      <div class="footer-container">
        <div class="footer-content">
          <div class="links-section">
            <div class="link-items">
              <a href="#" @click.prevent="goToMerchantApply">商家入驻</a>
              <a href="#">关于我们</a>
              <a href="#">帮助中心</a>
              <a href="#">联系我们</a>
            </div>
            <div class="service-info">
              <i class="fas fa-headset"></i>
              <span>客服热线：400-888-6666</span>
            </div>
          </div>
        </div>
      </div>
      <div class="copyright">
        <p>© 2026 精品购物商城 版权所有</p>
      </div>
    </div>

    <!-- ========== 底部导航栏 ========== -->
    <div class="tab-bar">
      <RouterLink :to="{name: 'UserDashboard'}" class="tab-bar-item active">
        <i class="fas fa-home"></i>
        <span>首页</span>
      </RouterLink>
      <RouterLink to="/category" class="tab-bar-item">
        <i class="fas fa-th-large"></i>
        <span>分类</span>
      </RouterLink>
      <RouterLink :to="{name: 'Cart'}" class="tab-bar-item">
        <i class="fas fa-shopping-cart"></i>
        <span>购物车</span>
        <span v-if="cartCount > 0" class="badge">{{ cartCount }}</span>
      </RouterLink>
      <RouterLink :to="{name: 'UserCenter'}" class="tab-bar-item">
        <i class="fas fa-user"></i>
        <span>我的</span>
      </RouterLink>
    </div>

    <!-- ========== 用户抽屉菜单 ========== -->
    <div class="user-dropdown" :class="{ show: showDrawer }" @click.self="closeDrawer">
      <div class="drawer-handle"></div>

      <!-- 未登录状态 -->
      <template v-if="!isLoggedIn">
        <div class="drawer-header">
          <RouterLink to="/login" class="dropdown-item login-register-btn" @click="closeDrawer">
            <i class="fas fa-user-circle"></i>
            <div class="btn-content">
              <span class="btn-title">登录/注册</span>
              <span class="btn-subtitle">登录后享受更多权益</span>
            </div>
          </RouterLink>
        </div>
      </template>

      <!-- 已登录状态 -->
      <template v-else>
        <div class="drawer-header">
          <img :src="userProfile.avatar || defaultAvatar" class="dropdown-avatar" alt="用户头像" />
          <div class="dropdown-info">
            <div class="dropdown-name">{{ userProfile.nickname || '用户' }}</div>
            <div class="dropdown-role" v-if="isSeller">商家账号</div>
            <div class="dropdown-role" v-if="isAdmin">管理员</div>
          </div>
        </div>
        <div class="dropdown-divider"></div>

        <RouterLink :to="{name:'UserCenter'}" class="dropdown-item" @click="closeDrawer">
          <i class="fas fa-user-circle"></i>
          <span>个人中心</span>
        </RouterLink>
        <RouterLink :to="{name:'UserOrders'}" class="dropdown-item" @click="closeDrawer">
          <i class="fas fa-clipboard-list"></i>
          <span>我的订单</span>
        </RouterLink>
        <RouterLink :to="{name: 'UserFavorites'}" class="dropdown-item" @click="closeDrawer">
          <i class="fas fa-heart"></i>
          <span>我的收藏</span>
        </RouterLink>

        <RouterLink v-if="isSeller || isAdmin" :to="{name: 'SellerDashboard'}" class="dropdown-item" @click="closeDrawer">
          <i class="fas fa-store"></i>
          <span>商家中心</span>
        </RouterLink>
        <RouterLink v-if="isAdmin" :to="{name: 'AdminDashboard'}" class="dropdown-item" @click="closeDrawer">
          <i class="fas fa-chart-line"></i>
          <span>管理后台</span>
        </RouterLink>

        <div class="dropdown-divider"></div>
        <a href="#" class="dropdown-item logout-btn" @click.prevent="handleLogout">
          <i class="fas fa-sign-out-alt"></i>
          <span>退出登录</span>
        </a>
      </template>
    </div>

    <!-- 抽屉遮罩层 -->
    <div v-if="showDrawer" class="drawer-overlay" @click="closeDrawer"></div>

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
  </div>
</template>

<script setup lang="ts">
  import { ref, computed, onMounted, onUnmounted } from 'vue'
  import { useRouter } from 'vue-router'
  import defaultAvatar from '@/static/images/user-avatar.jpg'
  import { authAPI } from '@/api/authAPI'
  import Message from '@/utils/message'
  import { useAuthStore } from '@/stores/auth'
  import { storeToRefs } from 'pinia'

  const router = useRouter()
  const authStore = useAuthStore()
  const { isLoggedIn, role, status, isUser, isSeller, isAdmin } = storeToRefs(authStore)

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

  const userProfile = ref({
    avatar: '',
    nickname: ''
  })

  const cartCount = ref(0)
  const favoriteCount = ref(0)
  const notificationCount = ref(0)

  // ==================== 分类相关状态 ====================

  const allCategories = ref<Category[]>([])
  const level1Categories = ref<Category[]>([])
  const level2Categories = ref<Category[]>([])
  const activeLevel1Id = ref<number | null>(null)
  const activeLevel2Id = ref<number | null>(null)

  // ==================== 商品相关状态 ====================

  const products = ref<Product[]>([])
  const loading = ref(false)
  const searchKeyword = ref('')
  const currentPage = ref(1)
  const pageSize = ref(20)
  const totalPages = ref(1)

  // ==================== UI 状态 ====================

  const showDrawer = ref(false)
  const showQuickView = ref(false)
  const selectedProduct = ref<Product | null>(null)

  // ==================== 轮播图数据 ====================

  const banners = ref<Banner[]>([
    { id: 1, image: 'https://picsum.photos/id/20/800/400', title: '限时秒杀', subtitle: '全场商品低至5折', link: '/products?promotion=spring' },
    { id: 2, image: 'https://picsum.photos/id/26/800/400', title: '新品上市', subtitle: '潮流新品抢先购', link: '/products?isNew=true' },
    { id: 3, image: 'https://picsum.photos/id/0/800/400', title: '品牌特卖', subtitle: '大牌好物限时抢购', link: '/products?brandSale=true' },
    { id: 4, image: 'https://picsum.photos/id/1/800/400', title: '开学季大促', subtitle: '学生专享优惠券免费领', link: '/products?promotion=backToSchool' },
    { id: 5, image: 'https://picsum.photos/id/15/800/400', title: '数码狂欢节', subtitle: '爆款数码产品直降1000元', link: '/products?promotion=digital' }
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

  const visiblePages = computed(() => {
    const delta = 2
    const range: number[] = []
    const rangeWithDots: (number | string)[] = []
    let l: number

    for (let i = 1; i <= totalPages.value; i++) {
      if (i === 1 || i === totalPages.value || (i >= currentPage.value - delta && i <= currentPage.value + delta)) {
        range.push(i)
      }
    }

    for (const i of range) {
      if (l) {
        if (i - l === 2) {
          rangeWithDots.push(l + 1)
        } else if (i - l !== 1) {
          rangeWithDots.push('...')
        }
      }
      rangeWithDots.push(i)
      l = i
    }

    return rangeWithDots
  })

  // ==================== 用户相关函数 ====================

  const loadUserProfile = async () => {
    if (!isLoggedIn.value) return

    try {
      const response = await authAPI.getUserProfile()
      if (response.success && response.data?.profile) {
        userProfile.value.avatar = response.data.profile.avatar || ''
        userProfile.value.nickname = response.data.profile.nickname || response.data.profile.username || '用户'
      }
    } catch (error) {
      console.error('加载用户信息失败:', error)
    }
  }

  const loadCartCount = async () => {
    if (!isLoggedIn.value) return

    try {
      const response = await authAPI.getCartCount()
      if (response.success) {
        cartCount.value = response.data || 0
      }
    } catch (error) {
      console.error('加载购物车数量失败:', error)
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
      console.error('加载分类失败:', error)
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
    loadProducts()
  }

  const selectLevel2Category = (categoryId: number | null) => {
    activeLevel2Id.value = categoryId
    currentPage.value = 1
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
        products.value = response.data?.records || []
        totalPages.value = response.data?.totalPages || 1
      } else {
        throw new Error(response.message || '加载商品失败')
      }
    } catch (error: any) {
      console.error('加载商品失败:', error)
      products.value = []
    } finally {
      loading.value = false
    }
  }

  const changePage = (page: number) => {
    if (page < 1 || page > totalPages.value) return
    currentPage.value = page
    loadProducts()
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  const performSearch = () => {
    if (searchKeyword.value.trim()) {
      currentPage.value = 1
      loadProducts()
    }
  }

  // ==================== UI 交互函数 ====================

  const toggleDrawer = () => {
    showDrawer.value = !showDrawer.value
  }

  const closeDrawer = () => {
    showDrawer.value = false
  }

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

  const handleLogout = async () => {
    try {
      await authAPI.logout()
      authStore.clear()
      Message.success('退出成功')
      closeDrawer()
      router.replace({ name: 'UserDashboard' })
    } catch (error) {
      Message.error('退出失败')
    }
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
    if (banner.link) {
      router.push(banner.link)
    }
  }

  // ==================== 工具函数 ====================

  const formatPrice = (price: number): string => {
    return price.toFixed(2)
  }

  // ==================== 生命周期 ====================

  onMounted(async () => {
    if (!authStore.validateAccountStatus()) {
      return
    }

    if (isLoggedIn.value) {
      await loadUserProfile()
      await loadCartCount()
    }

    await loadCategories()
    await loadProducts()

    startCarousel()
  })

  onUnmounted(() => {
    if (carouselTimer) clearInterval(carouselTimer)
  })
</script>

<style scoped>
  @import url('https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css');
  @import url('@/static/css/user/用户首页.css');
</style>
