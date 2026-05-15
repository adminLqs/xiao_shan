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
          :src="shopInfo.avatar || '/images/seller-avatar.jpg'"
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
  import { authAPI } from '@/api/auth'
  import { ref, reactive, computed, onMounted, watch } from 'vue'
  import { useRouter } from 'vue-router'
  import { useAuth } from '@/utils/auth-user' // 导入用户认证

  // 在 setup 中初始化
  const auth = useAuth()

  // 实例化路由
  const router = useRouter()

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

  // 购物车商品接口
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
  const hasMore = ref(false)
  const loading = ref(false)
  
  // 购物车相关
  const cartItems = ref<CartItem[]>([])
  const showCartPanel = ref(false)  // 控制购物车面板显示

  // ✅ 更新店铺信息结构（从后端映射）
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

  // ==================== 计算属性 ====================
  // 购物车总数量
  const cartTotal = computed(() => {
    return cartItems.value.reduce((sum, item) => sum + item.quantity, 0)
  })

  // 购物车总金额
  const cartTotalAmount = computed(() => {
    return cartItems.value.reduce((sum, item) => sum + (item.price * item.quantity), 0)
  })

  // 分类名称映射
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

  // 分类表情映射（用于购物车空状态）
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

  // 生成分类列表
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

  // 当前分类的商品
  const filteredProducts = computed(() => {
    const current = categoryList.value[currentCategoryIndex.value]
    return current 
      ? products.value.filter(p => p.category === current.id)
      : products.value
  })

  // ==================== 加载商家信息 ====================
  const loadSellerProfile = async () => {
    try {
      const response = await authAPI.getSellerProfile()
      const data = response.data || response
      
      if (data.success && data.profile) {
        const profile = data.profile
        // 映射商家信息到 shopInfo
        shopInfo.name = profile.storeName || shopInfo.name
        shopInfo.avatar = profile.storeAvatar || ''
        shopInfo.slogan = profile.slogan || shopInfo.slogan
        shopInfo.isOpen = profile.isOpen !== undefined ? profile.isOpen : true
        shopInfo.address = profile.address || ''
        shopInfo.businessHours = profile.business || ''
        shopInfo.storeDetail = profile.storeDetail || ''
        
        console.log('商家信息加载成功:', shopInfo)
      }
    } catch (error) {
      console.error('加载商家信息失败:', error)
      // 使用默认值，静默失败
    }
  }

  // ==================== 购物车方法 ====================
  
  // 获取商品在购物车中的数量
  const getCartQuantity = (productId: number): number => {
    const item = cartItems.value.find(item => item.id === productId)
    return item?.quantity || 0
  }

  // 获取分类表情
  const getCategoryEmoji = (category: string): string => {
    return categoryEmoji[category] || categoryEmoji.default as string
  }

  // 添加商品到购物车
  const addToCart = (product: Product) => {
    if (!product || product.isSoldOut) return
    
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
    
    // 保存到本地存储
    saveCartToStorage()
    
    // 显示轻提示（可选）
    showAddToast(product.name)
  }

  // 增加商品数量
  const increaseCart = (product: Product | CartItem) => {
    const existingItem = cartItems.value.find(item => item.id === product.id)
    if (existingItem) {
      existingItem.quantity += 1
      saveCartToStorage()
    }
  }

  // 减少商品数量
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

  // 清空购物车
  const clearCart = () => {
    if (cartItems.value.length === 0) return
    
    if (confirm('确定清空购物车吗？')) {
      cartItems.value = []
      localStorage.removeItem('cartItems')
    }
  }

  // 保存购物车到本地存储
  const saveCartToStorage = () => {
    try {
      localStorage.setItem('cartItems', JSON.stringify(cartItems.value))
    } catch (error) {
      console.error('保存购物车失败:', error)
    }
  }

  // 加载购物车从本地存储
  const loadCartData = () => {
    try {
      const saved = localStorage.getItem('cartItems')
      if (saved) {
        cartItems.value = JSON.parse(saved)
      }
    } catch (error) {
      console.error('加载购物车失败:', error)
    }
  }

  // 切换购物车面板
  const toggleCartPanel = () => {
    showCartPanel.value = !showCartPanel.value
  }

  // 显示添加提示
  const showAddToast = (productName: string) => {
    const toast = document.createElement('div')
    toast.className = 'cart-toast'
    toast.textContent = `✅ 已加入购物车: ${productName}`
    document.body.appendChild(toast)
    
    setTimeout(() => {
      toast.classList.add('show')
    }, 10)
    
    setTimeout(() => {
      toast.classList.remove('show')
      setTimeout(() => {
        document.body.removeChild(toast)
      }, 300)
    }, 2000)
  }

  // 结算
  const handleCheckout = () => {
    if (cartItems.value.length === 0) return
    
    router.replace({
      name: "UserPreCheckout"
    })
    
    showCartPanel.value = false
  }

  // ==================== 原有方法 ====================
  const formatPrice = (price: number): string => {
    return price.toFixed(2)
  }

  const switchTab = (tab: 'home' | 'orders' | 'profile') => {
    currentTab.value = tab
    alert(`切换到${tab === 'home' ? '首页' : tab === 'orders' ? '订单' : '我的'}`)
  }

  const handleSearch = () => {
    console.log('打开搜索')
  }

  const previewShop = () => {
    console.log('查看店铺详情')
  }

  const handleScroll = () => {
    // 处理滚动加载
  }

  // ==================== 获取商品数据 ====================
  const getAllProducts = async () => {
    loading.value = true
    try {
      const response = await authAPI.getAllProducts()
      
      let productData = null
      
      if (response && response.data) {
        if (Array.isArray(response.data)) {
          productData = response.data
        }
        else if (response.data.success && Array.isArray(response.data.data)) {
          productData = response.data.data
        }
      }
      
      if (productData) {
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
      }
      
    } catch (error) {
      console.error('获取商品失败:', error)
    } finally {
      loading.value = false
    }
  }

  // 检查是否为新
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

  // 初始化用户 - 只调用一次
  const initUser = async () => {
    let userId = Number(localStorage.getItem('userId'));
    
    if (!userId) {
      userId = Number(await auth.getUserId())
    }

    if (userId) {
      console.log('用户初始化成功,userId:', userId)
    } else {
      console.error('用户初始化失败')
    }
  }

  // 监听购物车变化，自动保存
  watch(cartItems, () => {
    saveCartToStorage()
  }, { deep: true })

  // ==================== 生命周期 ====================
  onMounted(async () => {
    await initUser()           // 初始化用户
    await loadSellerProfile()  // 加载商家信息
    loadCartData()             // 加载购物车
    getAllProducts()           // 加载所有商品
  })
</script>

<style scoped>
/* 导入CSS */
@import url("@/static/css/用户首页.css");
</style>