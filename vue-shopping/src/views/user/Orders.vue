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
    <div v-else ref="orderListRef" class="order-list">
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
              <div v-if="item.skuName" class="item-sku">规格：{{ item.skuName }}</div>
              <div class="item-quantity">数量：{{ item.quantity }}</div>
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

      <!-- ========== 加载更多提示 ========== -->
      <div v-if="loadingMore" class="loading-more">
        <i class="fas fa-spinner fa-spin"></i> 加载中...
      </div>
      <div v-else-if="!hasMore && orders.length > 0" class="no-more">
        没有更多了
      </div>
    </div>

    <!-- ========== 支付确认弹窗 ========== -->
    <div class="pay-confirm-overlay" v-if="showPayConfirm" @click.self="showPayConfirm = false">
      <div class="pay-confirm-dialog">
        <i class="fas fa-check-circle pay-icon"></i>
        <h3>请在支付页面完成付款</h3>
        <p class="pay-tip">支付完成后请点击下方按钮</p>
        <div class="pay-actions">
          <button class="btn-pay-done" @click="checkPayStatus" :disabled="checkingPay">
            {{ checkingPay ? '查询中...' : '已完成支付' }}
          </button>
          <button class="btn-pay-later" @click="handlePayLater">稍后支付</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { ref, onMounted, onUnmounted } from 'vue'
  import { useRouter, useRoute } from 'vue-router'
  import { authAPI } from '@/api/authAPI'
  import Message from '@/utils/message'
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
  const page = ref(1)
  const pageSize = ref(10)
  const hasMore = ref(true)
  const loadingMore = ref(false)
  const orderListRef = ref<HTMLDivElement | null>(null)

  // 支付相关状态
  const showPayConfirm = ref(false)
  const payingOrderId = ref<number | null>(null)
  const checkingPay = ref(false)

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
    if (loading.value) return

    loading.value = true

    try {
      const params: { page: number; pageSize: number; status?: string } = {
        page: page.value,
        pageSize: pageSize.value
      }

      // 只有非全部状态时才添加 status 参数
      if (activeTab.value !== 'all') {
        params.status = activeTab.value
      }

      const response = await authAPI.getOrders(params)

      if (response.success) {
        const data = response.data || {}
        const records = data.records || []
        
        if (page.value === 1) {
          orders.value = records
        } else {
          orders.value.push(...records)
        }
        
        hasMore.value = records.length >= pageSize.value
        updateTabCounts(data.counts)
      }
    } catch (error: any) {
      Message.error(error.message || '加载失败')
    } finally {
      loading.value = false
      loadingMore.value = false
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
    page.value = 1
    hasMore.value = true
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
      await Message.confirm('确定要取消该订单吗？', '取消订单')

      const response = await authAPI.cancelOrder(orderId)
      if (response.success) {
        Message.success('取消成功')
        await loadOrders()
      }
    } catch (error: any) {
      if (error !== 'cancel') {
        Message.error(error.message || '取消失败')
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
      await Message.confirm('确定要删除该订单吗？', '删除订单')

      const response = await authAPI.deleteOrder(orderId)
      if (response.success) {
        Message.success('删除成功')
        await loadOrders()
      }
    } catch (error: any) {
      if (error !== 'cancel') {
        Message.error(error.message || '删除失败')
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
        const paymentHtml = response.data?.paymentHtml
        
        // 用 window.open 打开支付页面
        const payWindow = window.open('', '_blank')
        if (payWindow) {
          payWindow.document.write(paymentHtml)
          payWindow.document.close()
        }

        // 保存当前支付的订单ID
        payingOrderId.value = orderId

        // 显示支付确认弹窗
        showPayConfirm.value = true
      } else {
        Message.error(response.message || '支付失败')
      }
    } catch (error: any) {
      Message.error(error.message || '支付失败')
    } finally {
      loading.value = false
    }
  }

  /**
   * 查询支付状态
   */
  const checkPayStatus = async () => {
    if (!payingOrderId.value || checkingPay.value) return
    
    checkingPay.value = true

    try {
      const response = await authAPI.getOrderDetail(payingOrderId.value)
      
      // 兼容多种返回格式
      const order = response.data?.order || response.data?.orderDetail?.order || response.data
      const status = order?.status
      
      if (status && (status === 'PAID' || status === 'PROCESSING' || status === 'SHIPPED')) {
        showPayConfirm.value = false
        Message.success('支付成功')
        page.value = 1
        hasMore.value = true
        await loadOrders()
      } else if (status === 'PENDING') {
        Message.warning('暂未收到支付通知，请确认是否已完成支付')
      } else if (status) {
        Message.warning(`订单状态：${status}`)
      } else {
        Message.error('查询订单失败：无法获取订单状态')
      }
    } catch (error: any) {
      Message.error(error.message || '查询失败，请重试')
    } finally {
      checkingPay.value = false
    }
  }

  /**
   * 稍后支付
   */
  const handlePayLater = () => {
    showPayConfirm.value = false
    // 不刷新列表，用户稍后可以继续支付
  }

  /**
   * 确认收货
   * @param {number} orderId - 订单ID
   * @returns {Promise<void>}
   */
  const confirmReceive = async (orderId: number): Promise<void> => {
    try {
      await Message.confirm('请确认已收到商品', '确认收货')

      const response = await authAPI.confirmReceive(orderId)
      if (response.success) {
        Message.success('确认成功')
        await loadOrders()
      }
    } catch (error: any) {
      if (error !== 'cancel') {
        Message.error(error.message || '操作失败')
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
   * 处理滚动事件
   */
  const handleScroll = () => {
    if (!orderListRef.value || loadingMore.value || !hasMore.value) return
    
    const { scrollTop, scrollHeight, clientHeight } = document.documentElement
    if (scrollTop + clientHeight >= scrollHeight - 100) {
      loadMore()
    }
  }

  /**
   * 加载更多数据
   */
  const loadMore = async () => {
    if (loadingMore.value || !hasMore.value) return
    loadingMore.value = true
    page.value++
    await loadOrders()
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
    
    window.addEventListener('scroll', handleScroll)
  })

  onUnmounted(() => {
    window.removeEventListener('scroll', handleScroll)
  })
</script>

<style scoped>
@import url('@/static/css/user/订单管理页.css');
</style>
