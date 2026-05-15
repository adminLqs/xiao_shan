<template>
  <div class="bbq-user-home">
    <!-- 顶部导航栏 -->
    <div class="top-nav">
      <div class="location">
        <span class="icon">📍</span>
        <span class="text">{{ shopInfo.name }}</span>
        <span class="arrow">▼</span>
      </div>
      <div class="actions">
        <span class="action-icon" @click="handleSearch">🔍</span>
        <span class="action-icon cart-icon" @click="toggleCartPanel">🛒</span>
      </div>
    </div>

    <!-- 商家卡片 -->
    <div class="merchant-card">
      <div class="avatar-wrapper" @click="previewShop">
        <img
          :src="shopInfo.avatar || DEFAULT_AVATAR"
          class="avatar"
          alt="店铺头像"
        />
        <div class="avatar-overlay">
          <span>查看店铺</span>
        </div>
      </div>
      <div class="merchant-details">
        <h2 class="shop-name">{{ shopInfo.name }}</h2>
        <p class="shop-slogan">{{ shopInfo.slogan }}</p>
        <div class="shop-status">
          <span class="status-dot" :class="{ active: shopInfo.isOpen }"></span>
          <span>{{ shopInfo.isOpen ? '营业中' : '休息中' }}</span>
          <span class="business-hours" v-if="shopInfo.businessHours">
            · {{ shopInfo.businessHours }}
          </span>
        </div>
        <p class="shop-address" v-if="shopInfo.address">
          📍 {{ shopInfo.address }}
        </p>
      </div>
    </div>

    <!-- 分类标签 -->
    <div class="category-tabs">
      <div class="tabs-container">
        <div 
          v-for="(category, index) in categoryList" 
          :key="category.id"
          class="tab-item"
          :class="{ active: currentCategoryIndex === index }"
          @click="currentCategoryIndex = index"
        >
          <span class="tab-name">{{ category.name }}</span>
          <span class="tab-count">{{ category.count }}</span>
        </div>
      </div>
    </div>

    <!-- 商品列表 -->
    <div class="product-list" @scroll="handleScroll">
      <div v-if="loading" class="loading-state">
        <span>加载中...</span>
      </div>
      
      <div v-else class="product-grid">
        <div 
          v-for="product in filteredProducts" 
          :key="product.id"
          class="product-card"
        >
          <!-- 商品内容保持不变 -->
          <div class="product-image">
            <img 
              v-if="product.image" 
              :src="product.image" 
              :alt="product.name"
            />
            <span v-else class="product-img-placeholder">🥩</span>
            
            <span v-if="product.isPopular" class="product-tag popular">🔥 招牌</span>
            <span v-else-if="product.isNew" class="product-tag new">✨ 新品</span>
            <span v-else-if="product.isSoldOut" class="product-tag soldout">售罄</span>
            
            <span v-if="getCartQuantity(product.id) > 0" class="cart-badge">
              {{ getCartQuantity(product.id) }}
            </span>
          </div>
          
          <div class="product-info">
            <h3 class="product-name">{{ product.name }}</h3>
            <p class="product-desc" v-if="product.description">{{ product.description }}</p>
            
            <div class="product-footer">
              <div class="price-info">
                <span class="current-price">¥{{ formatPrice(product.price) }}</span>
                <span v-if="product.original_price > product.price" class="original-price">
                  ¥{{ formatPrice(product.original_price) }}
                </span>
              </div>
              
              <div class="cart-controls">
                <div v-if="getCartQuantity(product.id) > 0" class="quantity-control">
                  <button 
                    class="quantity-btn minus" 
                    @click.stop="decreaseCart(product)"
                  >
                    <span class="btn-icon">−</span>
                  </button>
                  <span class="quantity-num">{{ getCartQuantity(product.id) }}</span>
                  <button 
                    class="quantity-btn plus" 
                    @click.stop="increaseCart(product)"
                  >
                    <span class="btn-icon">+</span>
                  </button>
                </div>
                
                <button 
                  v-else
                  class="add-cart-btn" 
                  :class="{ disabled: product.isSoldOut }"
                  @click.stop="addToCart(product)"
                  :disabled="product.isSoldOut"
                >
                  <span class="btn-icon">+</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="empty-state" v-if="!loading && products.length === 0">
        <span>🍢 暂无商品</span>
      </div>
    </div>

    <!-- 购物车悬浮球 -->
    <div v-if="cartTotal > 0" class="cart-float" @click="toggleCartPanel">
      <div class="float-content">
        <span class="float-icon">🛒</span>
        <span class="float-count">{{ cartTotal }}</span>
        <span class="float-price">¥{{ cartTotalAmount.toFixed(2) }}</span>
      </div>
      <div class="float-arrow" :class="{ 'arrow-up': showCartPanel }">▼</div>
    </div>

    <!-- 购物车面板 -->
    <transition name="slide">
      <div v-if="showCartPanel" class="cart-panel">
        <!-- 面板头部 -->
        <div class="panel-header">
          <div class="header-left">
            <span class="header-icon">🛒</span>
            <span class="header-title">购物车</span>
            <span class="header-count">({{ cartTotal }}件)</span>
          </div>
          <div class="header-right">
            <button class="clear-btn" @click="clearCart" v-if="cartItems.length > 0">
              <span class="clear-icon">🗑️</span>
              <span>清空</span>
            </button>
            <button class="close-btn" @click="toggleCartPanel">✕</button>
          </div>
        </div>

        <div v-if="cartItems.length === 0" class="panel-empty">
          <div class="empty-animation">🛒</div>
          <p class="empty-text">购物车还是空的</p>
          <p class="empty-subtext">快去添加喜欢的烧烤吧~</p>
        </div>

        <div v-else class="panel-content">
          <div class="cart-list">
            <div 
              v-for="item in cartItems" 
              :key="item.id"
              class="cart-item"
            >
              <div class="item-image">
                <img 
                  v-if="item.image" 
                  :src="item.image" 
                  :alt="item.name"
                />
                <span v-else class="item-img-placeholder">{{ getCategoryEmoji(item.category) }}</span>
              </div>
              
              <div class="item-info">
                <div class="item-name">{{ item.name }}</div>
                <div class="item-price">¥{{ formatPrice(item.price) }}</div>
              </div>
              
              <div class="item-controls">
                <button class="item-btn minus" @click="decreaseCart(item)">
                  <span>−</span>
                </button>
                <span class="item-quantity">{{ item.quantity }}</span>
                <button class="item-btn plus" @click="increaseCart(item)">
                  <span>+</span>
                </button>
              </div>
            </div>
          </div>
          
          <div class="cart-summary">
            <div class="summary-row">
              <span class="summary-label">商品合计</span>
              <span class="summary-value">¥{{ cartTotalAmount.toFixed(2) }}</span>
            </div>
            <div class="summary-row">
              <span class="summary-label">包装费</span>
              <span class="summary-value">¥2.00</span>
            </div>
            <div class="summary-row total">
              <span class="summary-label">实付</span>
              <span class="summary-value">¥{{ (cartTotalAmount + 2).toFixed(2) }}</span>
            </div>
            <div class="summary-tip" v-if="cartItems.length > 0">
              ⚡ 实际费用以订单确认为准
            </div>
          </div>
          
          <button class="checkout-btn" @click="handleCheckout">
            <span>去结算</span>
            <span class="checkout-arrow">›</span>
          </button>
        </div>

        <div class="panel-footer"></div>
      </div>
    </transition>

    <transition name="fade">
      <div v-if="showCartPanel" class="cart-mask" @click="toggleCartPanel"></div>
    </transition>
  </div>
</template>

<script lang="ts" setup>
  import { ref, reactive, computed, onMounted, watch } from 'vue'
  import { useRouter } from 'vue-router'
  import { showToast, showConfirmDialog } from 'vant'
  import { authAPI } from '@/api/authAPI'
  import { useUserStore } from '@/stores/auth.ts'
  import 'vant/es/toast/style'
  import 'vant/lib/index.css'

  // ==================== 路由和Store ====================
  const router = useRouter()
  const userStore = useUserStore()

  // ==================== 类型定义 ====================
  interface ShopInfo {
    name: string
    avatar: string
    slogan: string
    isOpen: boolean
    address: string
    businessHours: string
    storeDetail: string
  }

  interface Product {
    id: number
    name: string
    description: string
    price: number
    original_price: number
    image: string
    category: string
    isPopular?: boolean
    isNew?: boolean
    isSoldOut?: boolean
  }

  interface CartItem extends Product {
    quantity: number
    addedAt: number
  }

  interface Category {
    id: string
    name: string
    count: number
  }

  // ==================== 响应式数据 ====================
  const currentTab = ref<'home' | 'orders' | 'profile'>('home')
  const currentCategoryIndex = ref(0)
  const loading = ref(false)
  const DEFAULT_AVATAR = '/images/seller-avatar.jpg'

  // 购物车相关
  const cartItems = ref<CartItem[]>([])
  const showCartPanel = ref(false)

  // 店铺信息
  const shopInfo = reactive<ShopInfo>({
    name: '杉杉烤肉坊',
    avatar: '',
    slogan: '炭火匠心 · 深夜烧烤',
    isOpen: true,
    address: '',
    businessHours: '',
    storeDetail: ''
  })

  // 商品列表
  const products = ref<Product[]>([])

  // ==================== 常量配置 ====================
  const TOAST_DURATION = {
    SHORT: 1500,
    NORMAL: 2000,
    LONG: 3000
  } as const

  const categoryNames: Record<string, string> = {
    'meat': '🔥 招牌烤肉',
    'seafood': '🦐 海鲜烧烤',
    'vegetable': '🥬 烤蔬菜',
    'staple': '🍚 主食类',
    'drink': '🍺 酒水饮料',
    'snack': '🍡 特色小吃',
    'skewer': '🍢 经典串烧',
    'cold': '🥗 爽口凉菜'
  }

  const categoryEmoji: Record<string, string> = {
    'meat': '🥩',
    'seafood': '🦐',
    'vegetable': '🥬',
    'staple': '🍚',
    'drink': '🍺',
    'snack': '🍡',
    'skewer': '🍢',
    'cold': '🥗',
    'default': '🍖'
  }

  // ==================== 计算属性 ====================
  const cartTotal = computed(() => {
    return cartItems.value.reduce((sum, item) => sum + item.quantity, 0)
  })

  const cartTotalAmount = computed(() => {
    return cartItems.value.reduce((sum, item) => sum + (item.price * item.quantity), 0)
  })

  const categoryList = computed<Category[]>(() => {
    const map = new Map<string, number>()
    products.value.forEach(p => map.set(p.category, (map.get(p.category) || 0) + 1))
    
    const categories = Array.from(map.entries()).map(([id, count]) => ({
      id,
      name: categoryNames[id] || id,
      count
    }))
    
    return categories.sort((a, b) => {
      if (a.id === 'meat') return -1
      if (b.id === 'meat') return 1
      return a.name.localeCompare(b.name, 'zh-CN')
    })
  })

  const filteredProducts = computed(() => {
    const current = categoryList.value[currentCategoryIndex.value]
    return current 
      ? products.value.filter(p => p.category === current.id)
      : products.value
  })

  // ==================== 商家信息加载 ====================
  const loadSellerProfile = async () => {
    try {
      const response = await authAPI.getSellerProfile()
      const data = response.data || response
      
      if (data.success && data.profile) {
        const profile = data.profile
        shopInfo.name = profile.storeName || shopInfo.name
        shopInfo.avatar = profile.storeAvatar || ''
        shopInfo.slogan = profile.slogan || shopInfo.slogan
        shopInfo.isOpen = profile.isOpen !== undefined ? profile.isOpen : true
        shopInfo.address = profile.address || ''
        shopInfo.businessHours = profile.business || ''
        shopInfo.storeDetail = profile.storeDetail || ''
      }
    } catch (error) {
      showToast({
        message: '加载商家信息失败，请刷新重试',
        type: 'fail',
        duration: TOAST_DURATION.NORMAL
      })
    }
  }

  // ==================== 购物车方法 ====================
  const getCartQuantity = (productId: number): number => {
    const item = cartItems.value.find(item => item.id === productId)
    return item?.quantity || 0
  }

  const getCategoryEmoji = (category: string): string => {
    return categoryEmoji[category] || categoryEmoji.default
  }

  const addToCart = (product: Product) => {
    if (!product || product.isSoldOut) {
      showToast({
        message: '商品已售罄',
        type: 'fail',
        duration: TOAST_DURATION.SHORT
      })
      return
    }
    
    const existingItem = cartItems.value.find(item => item.id === product.id)
    
    if (existingItem) {
      existingItem.quantity += 1
    } else {
      const newItem: CartItem = {
        ...product,
        quantity: 1,
        addedAt: Date.now()
      }
      cartItems.value.push(newItem)
    }
    
    saveCartToStorage()
  }

  const increaseCart = (product: Product | CartItem) => {
    const existingItem = cartItems.value.find(item => item.id === product.id)
    if (existingItem) {
      existingItem.quantity += 1
      saveCartToStorage()
    }
  }

  const decreaseCart = (product: Product | CartItem) => {
    const existingItem = cartItems.value.find(item => item.id === product.id)
    if (existingItem) {
      if (existingItem.quantity <= 1) {
        cartItems.value = cartItems.value.filter(item => item.id !== product.id)
      } else {
        existingItem.quantity -= 1
      }
      saveCartToStorage()
    }
  }
  
  const clearCart = () => {
    if (cartItems.value.length === 0) {
      showToast({
        message: '购物车已是空的',
        type: 'info',
        duration: TOAST_DURATION.SHORT
      })
      return
    }
    
    showConfirmDialog({
      title: '提示',
      message: '确定清空购物车吗？',
      confirmButtonColor: '#ee0a24'
    }).then(() => {
      cartItems.value = []
      localStorage.removeItem('cartItems')
      showToast({
        message: '清空成功',
        type: 'success',
        duration: TOAST_DURATION.SHORT
      })
    }).catch(() => {
      // 用户取消，无需提示
    })
  }

  const saveCartToStorage = () => {
    try {
      localStorage.setItem('cartItems', JSON.stringify(cartItems.value))
    } catch (error) {
      // 静默失败，不影响用户体验
    }
  }

  const loadCartData = () => {
    try {
      const saved = localStorage.getItem('cartItems')
      if (saved) {
        cartItems.value = JSON.parse(saved)
      }
    } catch (error) {
      // 静默失败，使用空购物车
    }
  }

  const toggleCartPanel = () => {
    showCartPanel.value = !showCartPanel.value
  }

  const handleCheckout = () => {
    if (cartItems.value.length === 0) {
      showToast({
        message: '购物车是空的，请先添加商品',
        type: 'fail',
        duration: TOAST_DURATION.NORMAL
      })
      return
    }
    
    router.replace({
      name: "UserPreCheckout"
    })
    
    showCartPanel.value = false
  }

  // ==================== UI 方法 ====================
  const formatPrice = (price: number): string => {
    return price.toFixed(2)
  }

  const handleSearch = () => {
    showToast({
      message: '搜索功能开发中，敬请期待',
      type: 'info',
      duration: TOAST_DURATION.SHORT
    })
  }

  const previewShop = () => {
    // 店铺预览功能
    showToast({
      message: '店铺详情加载中',
      type: 'loading',
      duration: TOAST_DURATION.SHORT
    })
  }

  // ==================== 商品数据加载 ====================
  const getAllProducts = async () => {
    loading.value = true
    try {
      const response = await authAPI.getAllProducts()
      
      let productData = null
      
      if (response && response.data) {
        if (Array.isArray(response.data)) {
          productData = response.data
        } else if (response.data.success && Array.isArray(response.data.data)) {
          productData = response.data.data
        }
      }
      
      if (productData && productData.length > 0) {
        products.value = productData.map((item: any) => ({
          id: item.id,
          name: item.name,
          description: item.description || '暂无描述',
          price: Number(item.price),
          original_price: Number(item.originalPrice) || Number(item.price),
          image: item.image || '',
          category: item.category || 'skewer',
          isPopular: item.isPopular || false,
          isNew: checkIfNew(item.createdAt),
          isSoldOut: item.isSoldOut || false
        }))
      } else {
        showToast({
          message: '暂无商品数据',
          type: 'info',
          duration: TOAST_DURATION.NORMAL
        })
      }
      
    } catch (error) {
      showToast({
        message: '加载商品失败，请检查网络',
        type: 'fail',
        duration: TOAST_DURATION.LONG
      })
    } finally {
      loading.value = false
    }
  }

  const checkIfNew = (createdAt: string): boolean => {
    if (!createdAt) return false
    
    try {
      const createTime = new Date(createdAt).getTime()
      const now = new Date().getTime()
      const sevenDays = 7 * 24 * 60 * 60 * 1000
      return (now - createTime) <= sevenDays
    } catch {
      return false
    }
  }

  // ==================== 监听器 ====================
  watch(cartItems, () => {
    saveCartToStorage()
  }, { deep: true })

  // ==================== 生命周期 ====================
  onMounted(async () => {
    try {
      await Promise.all([
        loadSellerProfile(),
        loadCartData(),
        getAllProducts()
      ])
    } catch (error) {
      // 静默处理初始化失败，不影响页面渲染
    }
  })
</script>

<style scoped>
@import url("@/static/css/user/用户首页.css");
</style>