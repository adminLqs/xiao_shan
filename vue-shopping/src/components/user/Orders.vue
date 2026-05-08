<template>
  <div class="orders-container">
    
    <!-- ========== 订单状态标签页 ========== -->
    <div class="order-tabs">
      <div 
        v-for="tab in orderTabs" 
        :key="tab.value"
        class="order-tab"
        :class="{ active: activeTab === tab.value }"
        @click="handleTabClick(tab.value)"
      >
        {{ tab.label }}
        <span v-if="tab.count > 0" class="tab-count">{{ tab.count }}</span>
      </div>
    </div>

    <!-- ========== 加载状态 ========== -->
    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>

    <!-- ========== 空状态 ========== -->
    <div v-else-if="orders.length === 0" class="empty-state">
      <i class="fas fa-shopping-bag"></i>
      <p>暂无订单</p>
      <button class="btn-primary" @click="goShopping">去逛逛</button>
    </div>

    <!-- ========== 订单列表 ========== -->
    <div v-else class="order-list">
      <div v-for="orderWrapper in orders" :key="orderWrapper.order.id" class="order-card">
        
        <!-- 订单头部 -->
        <div class="order-header">
          <div class="order-info">
            <span class="order-number">订单号：{{ orderWrapper.order.orderNumber }}</span>
            <span class="order-time">{{ formatDate(orderWrapper.order.createdAt) }}</span>
          </div>
          <div class="order-status" :class="getStatusClass(orderWrapper.order.status)">
            {{ getStatusText(orderWrapper.order.status) }}
          </div>
        </div>

        <!-- 订单商品列表 -->
        <div class="order-items">
          <div 
            v-for="item in orderWrapper.orderItems" 
            :key="item.id" 
            class="order-item"
            @click="viewProduct(item.productId)"
          >
            <img :src="item.productImage" class="item-image">
            <div class="item-info">
              <div class="item-name">{{ item.productName }}</div>
              <div class="item-spec">数量：{{ item.quantity }}</div>
            </div>
            <div class="item-price">¥{{ formatPrice(item.price) }}</div>
          </div>
        </div>

        <!-- 订单底部 -->
        <div class="order-footer">
          <div class="order-total">
            实付款：
            <span class="total-amount">¥{{ formatPrice(orderWrapper.order.totalAmount) }}</span>
          </div>
          <div class="order-actions">
            
            <!-- 待付款状态 -->
            <template v-if="orderWrapper.order.status === 'PENDING'">              
              <button class="btn-primary" @click="goToPay(orderWrapper.order.id)">
                <i class="fas fa-credit-card"></i> 去支付
              </button>
              <button class="btn-outline btn-danger" @click="cancelOrder(orderWrapper.order.id)">
                <i class="fas fa-times"></i> 取消订单
              </button>
            </template>
            
            <!-- 已付款状态 -->
            <template v-if="orderWrapper.order.status === 'PAID'">
              <button class="btn-outline" @click="viewLogistics(orderWrapper.order.id)">
                <i class="fas fa-truck"></i> 查看物流
              </button>
            </template>
            
            <!-- 已发货状态 -->
            <template v-if="orderWrapper.order.status === 'SHIPPED'">
              <button class="btn-outline" @click="viewLogistics(orderWrapper.order.id)">
                <i class="fas fa-truck"></i> 查看物流
              </button>
              <button class="btn-success" @click="confirmReceive(orderWrapper.order.id)">
                <i class="fas fa-check"></i> 确认收货
              </button>
            </template>

            <!-- 已完成状态：单个商品且未评价时显示评价按钮 -->
            <template v-if="orderWrapper.order.status === 'COMPLETED' && orderWrapper.orderItems.length === 1">
              <!-- 已评论：显示已评价（禁用样式） -->
              <button 
                v-if="orderWrapper.orderItems[0]!.isReviewed" 
                class="btn-commented" 
                disabled
              >
                <i class="fas fa-check-circle"></i> 已评价
              </button>
              <!-- 未评论：显示评价按钮 -->
              <button 
                v-else 
                class="btn-outline btn-review" 
                @click="reviewOrder(orderWrapper.orderItems[0]!.id)"
              >
                <i class="fas fa-star"></i> 评价
              </button>
            </template>

            <!-- 已完成状态：显示查看物流按钮 -->
            <template v-if="orderWrapper.order.status === 'COMPLETED'">
              <button class="btn-outline" @click="viewLogistics(orderWrapper.order.id)">
                <i class="fas fa-truck"></i> 查看物流
              </button>
            </template>
            
            <!-- 已取消状态 -->
            <template v-if="orderWrapper.order.status === 'CANCELLED'">
              <button class="btn-outline btn-danger" @click="deleteOrder(orderWrapper.order.id)">
                <i class="fas fa-trash-alt"></i> 删除订单
              </button>
            </template>
            
            <!-- 通用按钮：查看详情 -->
            <button class="btn-outline" @click="viewOrderDetail(orderWrapper.order.id)">
              <i class="fas fa-info-circle"></i> 查看详情
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- ========== 分页组件 ========== -->
    <div class="pagination" v-if="totalPages > 1">
      <button 
        class="page-btn" 
        :disabled="currentPage === 1" 
        @click="changePage(currentPage - 1)"
      >
        <i class="fas fa-chevron-left"></i>
      </button>
      <span class="page-info">{{ currentPage }} / {{ totalPages }}</span>
      <button 
        class="page-btn" 
        :disabled="currentPage === totalPages" 
        @click="changePage(currentPage + 1)"
      >
        <i class="fas fa-chevron-right"></i>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { ref, onMounted } from 'vue'
  import { useRouter, useRoute } from 'vue-router'
  import { authAPI } from '@/api/authAPI'
  import { showToast, showConfirmDialog } from 'vant'
  import 'vant/es/toast/style'
  import 'vant/es/dialog/style'
  import { useAuthStore } from '@/stores/auth'

  const router = useRouter()
  const route = useRoute()
  const authStore = useAuthStore()

  // ==================== 类型定义 ====================

  /** 订单商品项 */
  interface OrderItem {
    id: number
    orderId: number
    productId: number
    sellerId: number
    productName: string
    productImage: string
    quantity: number
    price: number
    totalPrice: number
    isReviewed: boolean
    reviewedAt: string
    createdAt: string
  }

  /** 订单状态枚举 */
  type OrderStatus = 'PENDING' | 'PAID' | 'PROCESSING' | 'SHIPPED' | 'DELIVERED' | 'COMPLETED' | 'CANCELLED' | 'REFUNDED'

  /** 支付方式枚举 */
  type PaymentMethod = 'ALIPAY' | 'WECHAT'

  /** 订单实体 */
  interface Order {
    id: number
    orderNumber: string
    userId: number
    totalAmount: number
    status: OrderStatus
    source: string
    addressId: number
    paymentMethod?: PaymentMethod
    transactionId?: string
    paidAt?: string
    trackingNumber?: string
    logisticsCode?: string
    logisticsName?: string
    shippedAt?: string
    deliveredAt?: string
    completedAt?: string
    createdAt: string
    updatedAt: string
    isDeleted: boolean
    orderItems: OrderItem[]
  }

  /** 订单包装类（新架构） */
  interface OrderWithItems {
    order: Order
    orderItems: OrderItem[]
  }

  /** 标签页配置 */
  interface OrderTab {
    label: string
    value: string
    count: number
  }

  // ==================== 常量定义 ====================

  /** 有效的订单状态列表 */
  const VALID_STATUSES = ['PENDING', 'PAID', 'SHIPPED', 'COMPLETED', 'CANCELLED', 'REFUNDED']

  /** 订单标签页配置 */
  const orderTabs = ref<OrderTab[]>([
    { label: '全部', value: 'all', count: 0 },
    { label: '待付款', value: 'PENDING', count: 0 },
    { label: '已付款', value: 'PAID', count: 0 },
    { label: '已发货', value: 'SHIPPED', count: 0 },
    { label: '已完成', value: 'COMPLETED', count: 0 }
  ])

  /** 订单状态样式映射 */
  const statusClassMap: Record<OrderStatus, string> = {
    PENDING: 'status-pending',
    PAID: 'status-paid',
    PROCESSING: 'status-processing',
    SHIPPED: 'status-shipped',
    DELIVERED: 'status-delivered',
    COMPLETED: 'status-completed',
    CANCELLED: 'status-cancelled',
    REFUNDED: 'status-refunded'
  }

  /** 订单状态文字映射 */
  const statusTextMap: Record<OrderStatus, string> = {
    PENDING: '待付款',
    PAID: '已付款',
    PROCESSING: '处理中',
    SHIPPED: '已发货',
    DELIVERED: '已送达',
    COMPLETED: '已完成',
    CANCELLED: '已取消',
    REFUNDED: '已退款'
  }

  // ==================== 响应式数据 ====================

  const activeTab = ref('all')
  const loading = ref(false)
  const orders = ref<OrderWithItems[]>([])
  const currentPage = ref(1)
  const pageSize = ref(10)
  const totalPages = ref(1)
  let isLoading = false

  // ==================== URL 参数处理 ====================

  /**
   * 从 URL query 参数获取订单状态
   * @returns {string} 状态值，无效时返回 'all'
   */
  const getStatusFromQuery = (): string => {
    const statusParam = route.query.status as string
    if (statusParam && VALID_STATUSES.includes(statusParam)) {
      return statusParam
    }
    return 'all'
  }

  /**
   * 同步订单状态到 URL
   * @param {string} status - 订单状态
   */
  const updateQueryStatus = (status: string): void => {
    const currentQuery = { ...route.query }
    if (status === 'all') {
      delete currentQuery.status
    } else {
      currentQuery.status = status
    }
    router.replace({ path: route.path, query: currentQuery })
  }

  // ==================== 数据加载 ====================

  /**
   * 加载订单列表
   * @returns {Promise<void>}
   */
  const loadOrders = async (): Promise<void> => {
    if (isLoading) return
    
    isLoading = true
    loading.value = true
    
    try {
      const params: { page: number; size: number; status?: string } = {
        page: currentPage.value,
        size: pageSize.value
      }
      
      // 只有非全部状态时才添加 status 参数
      if (activeTab.value !== 'all') {
        params.status = activeTab.value
      }
      
      const response = await authAPI.getOrders(params)
      
      if (response.success) {
        // 新架构：response.data 直接是 OrderWithItems[] 数组
        orders.value = response.data || []
        totalPages.value = response.totalPages || 1
        updateTabCounts(response.counts)
      }
    } catch (error: any) {
      console.error('加载订单失败:', error)
      showToast({ message: error.message || '加载失败', type: 'fail' })
    } finally {
      loading.value = false
      isLoading = false
    }
  }

  /**
   * 更新标签页订单数量角标
   * @param {any} counts - 各状态订单数量
   */
  const updateTabCounts = (counts: any): void => {
    if (!counts) return

    orderTabs.value.forEach(tab => {
      if (tab.value === 'all') {
        tab.count = counts.total || 0
      } else {
        tab.count = counts[tab.value] || 0
      }
    })
  }

  /**
   * 切换订单状态标签页
   * @param {string} status - 目标状态
   * @returns {Promise<void>}
   */
  const switchTab = async (status: string): Promise<void> => {
    if (activeTab.value === status) return
    
    activeTab.value = status
    currentPage.value = 1
    updateQueryStatus(status)
    await loadOrders()
  }

  // ==================== 订单操作 ====================

  /**
   * 取消订单
   * @param {number} orderId - 订单ID
   * @returns {Promise<void>}
   */
  const cancelOrder = async (orderId: number): Promise<void> => {
    try {
      await showConfirmDialog({
        title: '取消订单',
        message: '确定要取消该订单吗？'
      })
      
      const response = await authAPI.cancelOrder(orderId)
      if (response.success) {
        showToast({ message: '取消成功', type: 'success' })
        await loadOrders()
      }
    } catch (error: any) {
      if (error !== 'cancel') {
        console.error('取消订单失败:', error)
        showToast({ message: error.message || '取消失败', type: 'fail' })
      }
    }
  }

  /**
   * 删除订单
   * @param {number} orderId - 订单ID
   * @returns {Promise<void>}
   */
  const deleteOrder = async (orderId: number): Promise<void> => {
    try {
      await showConfirmDialog({
        title: '删除订单',
        message: '确定要删除该订单吗？'
      })
      
      const response = await authAPI.deleteOrder(orderId)
      if (response.success) {
        showToast({ message: '删除成功', type: 'success' })
        await loadOrders()
      }
    } catch (error: any) {
      if (error !== 'cancel') {
        console.error('删除订单失败:', error)
        showToast({ message: error.message || '删除失败', type: 'fail' })
      }
    }
  }

  /**
   * 发起支付
   * @param {number} orderId - 订单ID
   * @returns {Promise<void>}
   */
  const goToPay = async (orderId: number): Promise<void> => {
    loading.value = true
    
    try {
      const response = await authAPI.payOrder(orderId)
      
      if (response.success) {
        const paymentHtml = response.data.paymentHtml
        const div = document.createElement('div')
        div.style.display = 'none'
        div.innerHTML = paymentHtml
        document.body.appendChild(div)
        
        const form = div.querySelector('form')
        if (form) {
          form.submit()
        } else {
          showToast({ message: '支付页面加载失败', type: 'fail' })
        }
      } else {
        showToast({ message: response.message || '支付失败', type: 'fail' })
      }
    } catch (error: any) {
      console.error('支付失败:', error)
      showToast({ message: error.message || '支付失败', type: 'fail' })
    } finally {
      loading.value = false
    }
  }

  /**
   * 确认收货
   * @param {number} orderId - 订单ID
   * @returns {Promise<void>}
   */
  const confirmReceive = async (orderId: number): Promise<void> => {
    try {
      await showConfirmDialog({
        title: '确认收货',
        message: '请确认已收到商品'
      })
      
      const response = await authAPI.confirmReceive(orderId)
      if (response.success) {
        showToast({ message: '确认成功', type: 'success' })
        await loadOrders()
      }
    } catch (error: any) {
      if (error !== 'cancel') {
        console.error('确认收货失败:', error)
        showToast({ message: error.message || '操作失败', type: 'fail' })
      }
    }
  }

  // ==================== 页面跳转 ====================

  /**
   * 查看物流
   * @param {number} orderId - 订单ID
   */
  const viewLogistics = (orderId: number): void => {
    router.push({
      name: 'UserLogistics',
      query: { orderId: String(orderId) }
    })
  }

  /**
   * 跳转评价页
   * @param {number} orderItemId - 订单项ID
   */
  const reviewOrder = (orderItemId: number): void => {
    router.push({ 
      name: 'Review',
      params: { orderItemId } 
    })
  }

  /**
   * 查看订单详情
   * @param {number} orderId - 订单ID
   */
  const viewOrderDetail = (orderId: number): void => {
    router.push({
      name: 'OrderDetail',
      params: {orderId: orderId}
    })
  }

  /**
   * 查看商品详情
   * @param {number} productId - 商品ID
   */
  const viewProduct = (productId: number): void => {
    router.push({
      name: 'ProductDetail',
      params: {productId: productId}
    })
  }

  /**
   * 去逛逛（跳转首页）
   */
  const goShopping = (): void => {
    router.push('/')
  }

  /**
   * 切换分页
   * @param {number} page - 目标页码
   */
  const changePage = (page: number): void => {
    if (page < 1 || page > totalPages.value) return
    currentPage.value = page
    loadOrders()
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  /**
   * 处理标签页点击
   * @param {string} status - 状态值
   */
  const handleTabClick = (status: string): void => {
    switchTab(status)
  }

  // ==================== 工具函数 ====================

  /**
   * 获取订单状态样式类
   * @param {OrderStatus} status - 订单状态
   * @returns {string} CSS类名
   */
  const getStatusClass = (status: OrderStatus): string => {
    return statusClassMap[status] || ''
  }

  /**
   * 获取订单状态显示文字
   * @param {OrderStatus} status - 订单状态
   * @returns {string} 中文状态描述
   */
  const getStatusText = (status: OrderStatus): string => {
    return statusTextMap[status] || status
  }

  /**
   * 格式化价格
   * @param {number} price - 原始价格
   * @returns {string} 保留两位小数的价格字符串
   */
  const formatPrice = (price: number): string => {
    if (price == null || isNaN(price)) {
      return '0.00'
    }
    return price.toFixed(2)
  }

  /**
   * 格式化日期
   * @param {string} dateStr - ISO日期字符串
   * @returns {string} YYYY-MM-DD 格式
   */
  const formatDate = (dateStr: string): string => {
    if (!dateStr) return '-'
    const date = new Date(dateStr)
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}`
  }


  // ==================== 生命周期 ====================

  onMounted(async () => {
    if (!authStore.validateUserPermission()) return
    
    const statusFromQuery = getStatusFromQuery()
    activeTab.value = statusFromQuery

    await loadOrders()
  })
</script>

<style scoped>
@import url('@/static/css/user/订单页.css');
</style>