<template>
  <div class="user-center-container">
    
    <!-- ========== 欢迎横幅区域 ========== -->
    <div class="welcome-banner">
      <div class="banner-content">
        <h2>欢迎回来，{{ userProfile.nickname || '用户' }}！</h2>
        <p>您有{{ pendingOrdersCount }}个待处理订单</p>
      </div>
      <div class="banner-actions">
        <button class="btn primary-btn" @click="goToOrders">查看订单</button>
      </div>
    </div>

    <!-- ========== 核心数据卡片区域 ========== -->
    <div class="stats-grid">
      <div class="stat-card" @click="goToOrdersByStatus('PENDING')">
        <div class="stat-icon pending">
          <i class="fas fa-clock"></i>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ orderCounts.PENDING || 0 }}</div>
          <div class="stat-label">待付款</div>
        </div>
        <div class="stat-action">
          <i class="fas fa-chevron-right"></i>
        </div>
      </div>

      <div class="stat-card" @click="goToOrdersByStatus('PAID')">
        <div class="stat-icon paid">
          <i class="fas fa-check-circle"></i>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ orderCounts.PAID || 0 }}</div>
          <div class="stat-label">待发货</div>
        </div>
        <div class="stat-action">
          <i class="fas fa-chevron-right"></i>
        </div>
      </div>

      <div class="stat-card" @click="goToOrdersByStatus('SHIPPED')">
        <div class="stat-icon shipped">
          <i class="fas fa-truck"></i>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ orderCounts.SHIPPED || 0 }}</div>
          <div class="stat-label">待收货</div>
        </div>
        <div class="stat-action">
          <i class="fas fa-chevron-right"></i>
        </div>
      </div>

      <div class="stat-card" @click="goToOrdersByStatus('COMPLETED')">
        <div class="stat-icon completed">
          <i class="fas fa-check-double"></i>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ orderCounts.COMPLETED || 0 }}</div>
          <div class="stat-label">已完成</div>
        </div>
        <div class="stat-action">
          <i class="fas fa-chevron-right"></i>
        </div>
      </div>
    </div>

    <!-- ========== 快捷入口区域 ========== -->
    <div class="quick-actions">
      <div class="section-header">
        <h3><i class="fas fa-bolt"></i> 快捷入口</h3>
      </div>
      <div class="action-grid">
        <RouterLink :to="{name: 'UserOrders', query: {status: 'PENDING'}}" class="action-item">
          <div class="action-icon pending">
            <i class="fas fa-clock"></i>
          </div>
          <span>待付款</span>
          <span v-if="orderCounts.PENDING > 0" class="badge">{{ orderCounts.PENDING }}</span>
        </RouterLink>
        
        <RouterLink :to="{name: 'UserOrders', query: {status: 'SHIPPED'}}" class="action-item">
          <div class="action-icon shipped">
            <i class="fas fa-truck"></i>
          </div>
          <span>待收货</span>
          <span v-if="orderCounts.SHIPPED > 0" class="badge">{{ orderCounts.SHIPPED }}</span>
        </RouterLink>
        
        <RouterLink :to="{name: 'UserOrders', query: {status: 'COMPLETED'}}" class="action-item">
          <div class="action-icon completed">
            <i class="fas fa-check-circle"></i>
          </div>
          <span>已完成</span>
        </RouterLink>
        
        <RouterLink :to="{name: 'UserFavorites'}" class="action-item">
          <div class="action-icon wishlist">
            <i class="fas fa-heart"></i>
          </div>
          <span>我的收藏</span>
        </RouterLink>
        
        <RouterLink :to="{name: 'UserAddresses'}" class="action-item">
          <div class="action-icon address">
            <i class="fas fa-map-marker-alt"></i>
          </div>
          <span>收货地址</span>
        </RouterLink>
      </div>
    </div>

    <!-- ========== 最近订单区域 ========== -->
    <div class="recent-orders">
      <div class="section-header">
        <h3><i class="fas fa-clipboard-list"></i> 最近订单</h3>
        <RouterLink :to="{name: 'UserOrders'}" class="view-all">
          查看全部 <i class="fas fa-chevron-right"></i>
        </RouterLink>
      </div>
      
      <div class="orders-list">
        <div v-for="orderWrapper in recentOrders" :key="orderWrapper.order.id" class="order-card">
          <div class="order-header">
            <span class="order-id">订单号: {{ orderWrapper.order.orderNumber }}</span>
            <span class="order-date">{{ formatDate(orderWrapper.order.createdAt) }}</span>
            <span class="order-status" :class="getStatusClass(orderWrapper.order.status)">
              {{ getStatusText(orderWrapper.order.status) }}
            </span>
            <span class="order-amount">¥{{ formatPrice(orderWrapper.order.totalAmount) }}</span>
          </div>
          
          <div class="order-products">
            <div v-for="item in orderWrapper.orderItems" :key="item.id" class="product-item">
              <img :src="item.productImage" :alt="item.productName">
              <div class="product-info">
                <h4>{{ item.productName }}</h4>
                <p>¥{{ formatPrice(item.price) }} × {{ item.quantity }}</p>
              </div>
            </div>
          </div>
          
          <div class="order-footer">
            <template v-if="orderWrapper.order.status === 'PENDING'">
              <button class="btn-pay" @click="handlePay(orderWrapper.order.id)">
                <i class="fas fa-credit-card"></i> 立即付款
              </button>
              <button class="btn-cancel" @click="cancelOrder(orderWrapper.order.id)">
                <i class="fas fa-times"></i> 取消订单
              </button>
            </template>
            
            <template v-if="orderWrapper.order.status === 'SHIPPED'">
              <button class="btn-tracking" @click="viewLogistics(orderWrapper.order.id)">
                <i class="fas fa-truck"></i> 查看物流
              </button>
              <button class="btn-confirm" @click="confirmReceive(orderWrapper.order.id)">
                <i class="fas fa-check"></i> 确认收货
              </button>
            </template>
            
            <!-- 已发货状态且有物流单号时显示查看物流 -->
            <template v-if="orderWrapper.order.trackingNumber">
              <button class="btn-tracking" @click="viewLogistics(orderWrapper.order.id)">
                <i class="fas fa-map-marker-alt"></i> 查看物流
              </button>
            </template>
            
            <template v-if="orderWrapper.order.status === 'COMPLETED' || orderWrapper.order.status === 'CANCELLED'">
              <button class="btn-detail" @click="viewOrderDetail(orderWrapper.order.id)">
                <i class="fas fa-info-circle"></i> 查看详情
              </button>
            </template>
          </div>
        </div>
        
        <div v-if="recentOrders.length === 0" class="empty-orders">
          <i class="fas fa-shopping-bag"></i>
          <p>暂无订单</p>
          <RouterLink to="/" class="btn primary-btn">去逛逛</RouterLink>
        </div>
      </div>
    </div>

    <!-- ========== 收藏商品区域 ========== -->
    <div class="wishlist-section">
      <div class="section-header">
        <h3><i class="fas fa-heart"></i> 收藏商品</h3>
        <RouterLink :to="{name: 'UserFavorites'}" class="view-all">
          查看全部 <i class="fas fa-chevron-right"></i>
        </RouterLink>
      </div>
      
      <div class="wishlist-grid">
        <div v-for="item in wishlist" :key="item.id" class="wishlist-item" :class="{ 'out-of-stock': item.stock === 0 }">
          <img :src="item.productImage || '/images/default-product.png'" :alt="item.productName">
          <div class="product-info">
            <h4>{{ item.productName }}</h4>
            <p class="price">¥{{ formatPrice(item.price) }}</p>
            <button 
              class="btn small-btn" 
              :class="{ 'disabled-btn': item.stock === 0 }"
              :disabled="item.stock === 0"
              @click="addToCart(item.productId)"
            >
              {{ item.stock === 0 ? '已售罄' : '加入购物车' }}
            </button>
          </div>
          <button class="remove-btn" @click="removeFromWishlist(item.id)">
            <i class="fas fa-times"></i>
          </button>
          <div v-if="item.stock === 0" class="sold-out-mask">已售罄</div>
        </div>
        
        <div v-if="wishlist.length === 0" class="empty-wishlist">
          <i class="fas fa-heart-broken"></i>
          <p>暂无收藏商品</p>
          <RouterLink to="/" class="btn primary-btn">去逛逛</RouterLink>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { ref, onMounted } from 'vue'
  import { useRouter } from 'vue-router'
  import { authAPI } from '@/api/authAPI'
  import { showToast, showConfirmDialog, showLoadingToast } from 'vant'
  import 'vant/es/toast/style'
  import 'vant/es/dialog/style'
  import { useAuthStore } from '@/stores/auth'

  const authStore = useAuthStore()
  const router = useRouter()

  // ==================== 类型定义 ====================

  interface UserProfile {
    avatar: string
    nickname: string
  }

  interface OrderCounts {
    total: number
    PENDING: number
    PAID: number
    PROCESSING: number
    SHIPPED: number
    COMPLETED: number
    CANCELLED: number
    REFUNDED: number
  }

  interface OrderItem {
    id: number
    productId: number
    productName: string
    productImage: string
    quantity: number
    price: number
  }

  interface Order {
    id: number
    orderNumber: string
    totalAmount: number
    status: string
    createdAt: string
    trackingNumber: string | null
  }

  interface OrderWithItems {
    order: Order
    orderItems: OrderItem[]
  }

  interface WishlistItem {
    id: number
    productId: number
    productName: string
    productImage: string
    price: number
    stock: number
  }

  // ==================== 响应式数据 ====================

  const userProfile = ref<UserProfile>({
    avatar: '',
    nickname: ''
  })

  const orderCounts = ref<OrderCounts>({
    total: 0,
    PENDING: 0,
    PAID: 0,
    PROCESSING: 0,
    SHIPPED: 0,
    COMPLETED: 0,
    CANCELLED: 0,
    REFUNDED: 0
  })

  const pendingOrdersCount = ref(0)
  const recentOrders = ref<OrderWithItems[]>([])
  const wishlist = ref<WishlistItem[]>([])

  // ==================== 数据加载 ====================

  const loadUserProfile = async () => {
    try {
      const response = await authAPI.getUserProfile()
      if (response.success && response.data) {
        userProfile.value = {
          avatar: response.data.avatar || '',
          nickname: response.data.nickname || ''
        }
      }
    } catch (error: any) {
      console.error('加载用户信息失败:', error)
    }
  }

  const loadOrderCounts = async () => {
    try {
      const response = await authAPI.getOrders({ page: 1, size: 1 })
      
      if (response.success && response.counts) {
        orderCounts.value = {
          total: response.counts.total || 0,
          PENDING: response.counts.PENDING || 0,
          PAID: response.counts.PAID || 0,
          PROCESSING: response.counts.PROCESSING || 0,
          SHIPPED: response.counts.SHIPPED || 0,
          COMPLETED: response.counts.COMPLETED || 0,
          CANCELLED: response.counts.CANCELLED || 0,
          REFUNDED: response.counts.REFUNDED || 0
        }
        pendingOrdersCount.value = (orderCounts.value.PENDING || 0) + (orderCounts.value.SHIPPED || 0)
      }
    } catch (error: any) {
      console.error('加载订单统计失败:', error)
    }
  }

  const loadRecentOrders = async () => {
    try {
      const response = await authAPI.getOrders({ page: 1, size: 3 })
      if (response.success && response.data) {
        recentOrders.value = response.data
      }
    } catch (error: any) {
      console.error('加载最近订单失败:', error)
    }
  }

  const loadWishlist = async () => {
    try {
      const response = await authAPI.getFavorites({ page: 1, size: 4 })
      if (response.success && response.data) {
        wishlist.value = response.data
      }
    } catch (error: any) {
      console.error('加载收藏列表失败:', error)
    }
  }

  // ==================== 页面跳转 ====================

  const goToOrders = () => {
    router.push({ name: 'UserOrders' })
  }

  const goToOrdersByStatus = (status: string) => {
    router.push({ name: 'UserOrders', query: { status } })
  }

  const handlePay = async (orderId: number) => {
    try {
      const toast = showLoadingToast({
        message: '正在获取支付信息...',
        duration: 0,
        forbidClick: true
      })
      
      const response = await authAPI.payOrder(orderId)
      toast.close()
      
      if (response.success && response.data?.paymentHtml) {
        const payWindow = window.open()
        if (payWindow) {
          payWindow.document.write(response.data.paymentHtml)
          payWindow.document.close()
        }
      } else {
        showToast({ message: response.message || '获取支付信息失败', type: 'fail' })
      }
    } catch (error: any) {
      console.error('支付失败:', error)
      showToast({ message: error.message || '支付失败', type: 'fail' })
    }
  }

  const cancelOrder = async (orderId: number) => {
    try {
      await showConfirmDialog({
        title: '取消订单',
        message: '确定要取消该订单吗？',
        confirmButtonText: '确定',
        cancelButtonText: '取消'
      })
      
      const response = await authAPI.cancelOrder(orderId)
      if (response.success) {
        showToast({ message: '订单已取消', type: 'success' })
        await Promise.all([loadOrderCounts(), loadRecentOrders()])
      }
    } catch (error: any) {
      if (error !== 'cancel') {
        console.error('取消订单失败:', error)
        showToast({ message: error.message || '取消订单失败', type: 'fail' })
      }
    }
  }

  const confirmReceive = async (orderId: number) => {
    try {
      await showConfirmDialog({
        title: '确认收货',
        message: '请确认已收到商品，确认后将完成订单',
        confirmButtonText: '确认收货',
        cancelButtonText: '取消'
      })
      
      const response = await authAPI.confirmReceive(orderId)
      if (response.success) {
        showToast({ message: '确认收货成功', type: 'success' })
        await Promise.all([loadOrderCounts(), loadRecentOrders()])
      }
    } catch (error: any) {
      if (error !== 'cancel') {
        console.error('确认收货失败:', error)
        showToast({ message: error.message || '确认收货失败', type: 'fail' })
      }
    }
  }

  const viewOrderDetail = (orderId: number) => {
    router.push({
      name: 'OrderDetail',
      params: { orderId }
    })
  }

  const viewLogistics = (orderId: number) => {
    router.push({
      name: 'UserLogistics',
      query: { orderId: String(orderId) }
    })
  }

  // ==================== 收藏操作 ====================

  const addToCart = async (productId: number) => {
    try {
      const toast = showLoadingToast({
        message: '添加中...',
        duration: 0,
        forbidClick: true
      })
      
      const response = await authAPI.addToCart({ productId, quantity: 1 })
      toast.close()
      
      if (response.success) {
        showToast({ message: '已添加到购物车', type: 'success' })
      }
    } catch (error: any) {
      console.error('添加到购物车失败:', error)
      showToast({ message: error.message || '添加失败', type: 'fail' })
    }
  }

  const removeFromWishlist = async (favoriteId: number) => {
    try {
      await showConfirmDialog({
        title: '移除收藏',
        message: '确定要从收藏中移除该商品吗？',
        confirmButtonText: '确定',
        cancelButtonText: '取消'
      })
      
      const response = await authAPI.removeFavorite(favoriteId)
      if (response.success) {
        showToast({ message: '已从收藏中移除', type: 'success' })
        await loadWishlist()
      }
    } catch (error: any) {
      if (error !== 'cancel') {
        console.error('移除收藏失败:', error)
        showToast({ message: error.message || '移除失败', type: 'fail' })
      }
    }
  }

  // ==================== 工具函数 ====================

  const formatPrice = (price: number): string => {
    if (price == null) return '0.00'
    return price.toFixed(2)
  }

  const formatDate = (dateStr: string): string => {
    if (!dateStr) return ''
    try {
      const date = new Date(dateStr)
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      return `${year}-${month}-${day}`
    } catch {
      return ''
    }
  }

  const getStatusClass = (status: string): string => {
    const classes: Record<string, string> = {
      PENDING: 'status-pending',
      PAID: 'status-paid',
      PROCESSING: 'status-processing',
      SHIPPED: 'status-shipped',
      DELIVERED: 'status-delivered',
      COMPLETED: 'status-completed',
      CANCELLED: 'status-cancelled',
      REFUNDED: 'status-refunded'
    }
    return classes[status] || ''
  }

  const getStatusText = (status: string): string => {
    const texts: Record<string, string> = {
      PENDING: '待付款',
      PAID: '已付款',
      PROCESSING: '处理中',
      SHIPPED: '已发货',
      DELIVERED: '已送达',
      COMPLETED: '已完成',
      CANCELLED: '已取消',
      REFUNDED: '已退款'
    }
    return texts[status] || status
  }

  // ==================== 生命周期 ====================

  onMounted(async () => {
    if (!authStore.validateUserPermission()) return
    
    await Promise.all([
      loadUserProfile(),
      loadOrderCounts(),
      loadRecentOrders(),
      loadWishlist()
    ])
  })
</script>

<style scoped>
  @import url('@/static/css/user/个人中心.css');
</style>