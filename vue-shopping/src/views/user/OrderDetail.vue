<template>
  <div class="order-detail-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-title">
        <i class="fas fa-clipboard-list"></i>
        订单详情
      </h1>
      <div class="breadcrumb">
        <RouterLink to="/">首页</RouterLink>
        <i class="fas fa-chevron-right"></i>
        <RouterLink :to="{name: 'UserOrders'}">我的订单</RouterLink>
        <i class="fas fa-chevron-right"></i>
        <span class="current">订单详情</span>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>

    <!-- 订单不存在 -->
    <div v-else-if="!orderData" class="empty-state">
      <i class="fas fa-search"></i>
      <p>订单不存在</p>
      <button class="btn-primary" @click="goBack">返回订单列表</button>
    </div>

    <!-- 订单详情内容 -->
    <div v-else class="order-detail-content">
      <!-- 订单状态栏 -->
      <div class="order-status-bar" :class="getStatusClass(orderData.status)">
        <div class="status-icon">
          <i :class="getStatusIcon(orderData.status)"></i>
        </div>
        <div class="status-info">
          <div class="status-text">{{ getStatusText(orderData.status) }}</div>
          <div class="status-desc">{{ getStatusDesc(orderData.status) }}</div>
        </div>
      </div>

      <!-- 订单基本信息 -->
      <div class="info-card">
        <div class="card-title">订单信息</div>
        <div class="info-grid">
          <div class="info-item">
            <span class="info-label">订单号：</span>
            <span class="info-value">{{ orderData.orderNumber }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">下单时间：</span>
            <span class="info-value">{{ formatDateTime(orderData.createdAt) }}</span>
          </div>
          <div v-if="orderData.paidAt" class="info-item">
            <span class="info-label">支付时间：</span>
            <span class="info-value">{{ formatDateTime(orderData.paidAt) }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">支付方式：</span>
            <span class="info-value">{{ getPaymentMethodText(orderData.paymentMethod) }}</span>
          </div>
        </div>
      </div>

      <!-- 收货地址 -->
      <div class="info-card">
        <div class="card-title">收货地址</div>
        <div class="address-info" v-if="addressData">
          <div class="address-recipient">
            <span>{{ addressData.recipientName }}</span>
            <span>{{ addressData.recipientPhone }}</span>
          </div>
          <div class="address-detail">
            {{ formatAddress(addressData) }}
          </div>
        </div>
        <div v-else class="no-address">暂无收货地址信息</div>
      </div>

      <!-- 商品列表 -->
      <div class="info-card">
        <div class="card-title">商品清单</div>
        <div class="product-list">
          <div v-for="item in orderItems" :key="item.id" class="product-item">
            <img :src="item.productImage" class="product-image" @click="viewProduct(item.productId)">
            <div class="product-info" @click="viewProduct(item.productId)">
              <div class="product-name">{{ item.productName }}</div>
              <div class="product-spec">数量：{{ item.quantity }}</div>
            </div>
            <div class="product-price">¥{{ formatPrice(item.price) }}</div>
            <!-- 商品操作按钮区域 -->
            <div class="product-actions">
              <!-- 评价按钮（已完成状态且未评价） -->
              <button
                v-if="orderData.status === 'COMPLETED'"
                class="btn-review"
                :class="{ 'btn-reviewed': item.isReviewed }"
                :disabled="item.isReviewed"
                @click.stop="reviewOrderItem(item.id)"
              >
                <i :class="item.isReviewed ? 'fas fa-check-circle' : 'fas fa-edit'"></i>
                {{ item.isReviewed ? '已评价' : '评价' }}
              </button>
              
              <!-- 申请退款按钮（待付款/已付款/处理中/已发货状态） -->
              <button
                v-if="['PENDING', 'PAID', 'PROCESSING', 'SHIPPED', 'DELIVERED'].includes(orderData.status)"
                class="btn-refund"
                @click.stop="goToRefund(item.id)"
              >
                <i class="fas fa-undo-alt"></i> 申请退款
              </button>
              
              <!-- 申请售后按钮（已完成状态） -->
              <button
                v-if="orderData.status === 'COMPLETED'"
                class="btn-after-sale"
                @click.stop="goToAfterSale(item.id)"
              >
                <i class="fas fa-tools"></i> 申请售后
              </button>
            </div>
          </div>
        </div>
        <div class="amount-summary">
          <div class="amount-row">
            <span>商品总价</span>
            <span>¥{{ formatPrice(orderData.totalAmount) }}</span>
          </div>
          <div class="amount-row total">
            <span>实付款</span>
            <span class="total-amount">¥{{ formatPrice(orderData.totalAmount) }}</span>
          </div>
        </div>
      </div>

      <!-- 底部操作按钮 -->
      <div class="action-buttons">
        <template v-if="orderData.status === 'PENDING'">
          <button class="btn-danger" @click="cancelOrder">
            <i class="fas fa-times"></i> 取消订单
          </button>
          <button class="btn-primary" @click="goToPay">
            <i class="fas fa-credit-card"></i> 去支付
          </button>
        </template>
        <template v-if="['SHIPPED', 'DELIVERED'].includes(orderData.status)">
          <button class="btn-success" @click="confirmReceive">
            <i class="fas fa-check"></i> 确认收货
          </button>
        </template>
        <template v-if="['SHIPPED', 'DELIVERED', 'COMPLETED'].includes(orderData.status) && orderData.trackingNumber">
          <button class="btn-outline" @click="viewLogistics(orderData.id)">
            <i class="fas fa-truck"></i> 查看物流
          </button>
        </template>
        <template v-if="['CANCELLED', 'REFUNDED'].includes(orderData.status)">
          <button class="btn-danger" @click="deleteOrder">
            <i class="fas fa-trash-alt"></i> 删除订单
          </button>
        </template>
        <button class="btn-outline" @click="goBack">
          <i class="fas fa-arrow-left"></i> 返回
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { ref, onMounted } from 'vue'
  import { useRoute, useRouter } from 'vue-router'
  import { authAPI } from '@/api/authAPI'
  import { showToast, showConfirmDialog } from 'vant'
  import 'vant/es/toast/style'
  import 'vant/es/dialog/style'
  import { useAuthStore } from '@/stores/auth'

  const authStore = useAuthStore()
  const route = useRoute()
  const router = useRouter()

  // ==================== 类型定义 ====================

  /** 订单状态枚举 */
  type OrderStatus = 'PENDING' | 'PAID' | 'PROCESSING' | 'SHIPPED' | 'DELIVERED' | 'COMPLETED' | 'CANCELLED' | 'REFUNDED'

  /** 支付方式枚举 */
  type PaymentMethod = 'ALIPAY' | 'WECHAT'

  /** 订单来源 */
  type OrderSource = 'cart' | 'product'

  /** 地址实体 */
  interface Address {
    id: number
    userId: number
    recipientName: string
    recipientPhone: string
    province: string
    city: string
    district: string
    detailAddress: string
    label: string
    isDefault: boolean
    createdAt: string
    updatedAt: string
  }

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
    reviewedAt: string | null
    createdAt: string
  }

  /** 订单实体 */
  interface Order {
    id: number
    orderNumber: string
    userId: number
    totalAmount: number
    status: OrderStatus
    source: OrderSource
    addressId: number
    paymentMethod: PaymentMethod | null
    transactionId: string | null
    paidAt: string | null
    trackingNumber: string | null
    logisticsCode: string | null
    logisticsName: string | null
    shippedAt: string | null
    deliveredAt: string | null
    completedAt: string | null
    createdAt: string
    updatedAt: string
    isDeleted: boolean
  }

  /** 订单详情响应数据（与后端 OrderDetailVO 对应） */
  interface OrderDetailData {
    order: Order
    orderItems: OrderItem[]
    address: Address
  }

  // ==================== 响应式数据 ====================

  const loading = ref(false)
  const orderData = ref<Order | null>(null)
  const orderItems = ref<OrderItem[]>([])
  const addressData = ref<Address | null>(null)

  // ==================== 静态配置 ====================

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

  /** 订单状态图标映射 */
  const statusIconMap: Record<OrderStatus, string> = {
    PENDING: 'fas fa-clock',
    PAID: 'fas fa-check-circle',
    PROCESSING: 'fas fa-spinner',
    SHIPPED: 'fas fa-truck',
    DELIVERED: 'fas fa-box-open',
    COMPLETED: 'fas fa-check-double',
    CANCELLED: 'fas fa-times-circle',
    REFUNDED: 'fas fa-undo-alt'
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

  /** 订单状态描述映射 */
  const statusDescMap: Record<OrderStatus, string> = {
    PENDING: '请尽快完成支付，超时订单将自动取消',
    PAID: '商家正在准备您的订单，请耐心等待',
    PROCESSING: '订单正在处理中',
    SHIPPED: '商品已发出，请留意物流信息',
    DELIVERED: '商品已送达，欢迎评价',
    COMPLETED: '订单已完成，感谢您的购买',
    CANCELLED: '订单已取消',
    REFUNDED: '订单已退款，款项将原路返回'
  }

  /** 支付方式映射 */
  const paymentMethodMap: Record<PaymentMethod, string> = {
    ALIPAY: '支付宝',
    WECHAT: '微信支付'
  }

  // ==================== 数据加载 ====================

  /**
   * 加载订单详情
   * @returns {Promise<void>}
   */
  const loadOrderDetail = async (): Promise<void> => {
    loading.value = true

    try {
      const orderId = Number(route.params.orderId)

      if (!orderId) {
        showToast({ message: '订单ID不存在', type: 'fail' })
        router.push({ name: 'UserOrders' })
        return
      }

      const response = await authAPI.getOrderDetail(orderId)

      if (response.success && response.data) {
        const data = response.data as OrderDetailData

        // 订单基本信息
        orderData.value = data.order

        // 订单商品列表（计算小计金额）
        orderItems.value = data.orderItems.map(item => ({
          ...item,
          totalPrice: item.price * item.quantity
        }))

        // 收货地址
        addressData.value = data.address
      } else {
        showToast({ message: response.message || '订单不存在', type: 'fail' })
        router.push({ name: 'UserOrders' })
      }
    } catch (error: any) {
      console.error('加载订单详情失败:', error)
      showToast({ message: error.message || '加载失败', type: 'fail' })
    } finally {
      loading.value = false
    }
  }

  // ==================== 订单操作 ====================

  /**
   * 取消订单
   * @returns {Promise<void>}
   */
  const cancelOrder = async (): Promise<void> => {
    try {
      await showConfirmDialog({ title: '取消订单', message: '确定要取消该订单吗？' })

      const response = await authAPI.cancelOrder(orderData.value!.id)
      if (response.success) {
        showToast({ message: '取消成功', type: 'success' })
        await loadOrderDetail()
      }
    } catch (error: any) {
      if (error !== 'cancel') {
        showToast({ message: error.message || '取消失败', type: 'fail' })
      }
    }
  }

  /**
   * 删除订单
   * @returns {Promise<void>}
   */
  const deleteOrder = async (): Promise<void> => {
    try {
      await showConfirmDialog({ title: '删除订单', message: '确定要删除该订单吗？' })

      const response = await authAPI.deleteOrder(orderData.value!.id)
      if (response.success) {
        showToast({ message: '删除成功', type: 'success' })
        router.push({ name: 'UserOrders' })
      }
    } catch (error: any) {
      if (error !== 'cancel') {
        showToast({ message: error.message || '删除失败', type: 'fail' })
      }
    }
  }

  /**
   * 发起支付
   * @returns {Promise<void>}
   */
  const goToPay = async (): Promise<void> => {
    loading.value = true

    try {
      const response = await authAPI.payOrder(orderData.value!.id)

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
   * @returns {Promise<void>}
   */
  const confirmReceive = async (): Promise<void> => {
    try {
      await showConfirmDialog({ title: '确认收货', message: '请确认已收到商品' })

      const response = await authAPI.confirmReceive(orderData.value!.id)
      if (response.success) {
        showToast({ message: '确认成功', type: 'success' })
        await loadOrderDetail()
      }
    } catch (error: any) {
      if (error !== 'cancel') {
        showToast({ message: error.message || '操作失败', type: 'fail' })
      }
    }
  }

  // ==================== 页面跳转 ====================

  /**
   * 查看物流信息
   * @param {number} orderId - 订单ID
   */
  const viewLogistics = (orderId: number): void => {
    router.push({ name: 'UserLogistics', query: { orderId: String(orderId) } })
  }

  /**
   * 评价商品
   * @param {number} orderItemId - 订单项ID
   */
  const reviewOrderItem = (orderItemId: number): void => {
    router.push({ name: 'Review', params: { orderItemId } })
  }

  /**
   * 申请退款
   * @param {number} orderItemId - 订单项ID
   */
  const goToRefund = (orderItemId: number): void => {
    showToast({
      message: '还未扩展',
      type: 'fail'
    })
    // router.push({ name: 'UserRefund', query: { orderItemId: String(orderItemId) } })
  }

  /**
   * 申请售后
   * @param {number} orderItemId - 订单项ID
   */
  const goToAfterSale = (orderItemId: number): void => {
    showToast({
      message: '还未扩展',
      type: 'fail'
    })
    // router.push({ name: 'UserAfterSale', query: { orderItemId: String(orderItemId) } })
  }

  /**
   * 联系客服
   */
  const contactService = (): void => {
    showToast({ message: '客服热线：400-888-6666', type: 'info' })
  }

  /**
   * 跳转商品详情
   * @param {number} productId - 商品ID
   */
  const viewProduct = (productId: number): void => {
    router.push({ name: 'ProductDetail', params: { productId } })
  }

  /**
   * 返回订单列表
   */
  const goBack = (): void => {
    router.push({ name: 'UserOrders' })
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
   * 获取订单状态图标
   * @param {OrderStatus} status - 订单状态
   * @returns {string} 图标类名
   */
  const getStatusIcon = (status: OrderStatus): string => {
    return statusIconMap[status] || 'fas fa-shopping-bag'
  }

  /**
   * 获取订单状态文字
   * @param {OrderStatus} status - 订单状态
   * @returns {string} 中文状态描述
   */
  const getStatusText = (status: OrderStatus): string => {
    return statusTextMap[status] || status
  }

  /**
   * 获取订单状态描述
   * @param {OrderStatus} status - 订单状态
   * @returns {string} 状态说明文案
   */
  const getStatusDesc = (status: OrderStatus): string => {
    return statusDescMap[status] || ''
  }

  /**
   * 获取支付方式文字
   * @param {PaymentMethod | null} method - 支付方式
   * @returns {string} 支付方式名称
   */
  const getPaymentMethodText = (method: PaymentMethod | null): string => {
    if (!method) return '未支付'
    return paymentMethodMap[method] || method
  }

  /**
   * 格式化价格
   * @param {number} price - 原始价格
   * @returns {string} 保留两位小数的价格字符串
   */
  const formatPrice = (price: number): string => {
    if (price == null || isNaN(price)) return '0.00'
    return price.toFixed(2)
  }

  /**
   * 格式化日期时间
   * @param {string | null} dateStr - ISO日期字符串
   * @returns {string} 格式化后的日期时间
   */
  const formatDateTime = (dateStr: string | null): string => {
    if (!dateStr) return '-'
    const date = new Date(dateStr)
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    const hours = String(date.getHours()).padStart(2, '0')
    const minutes = String(date.getMinutes()).padStart(2, '0')
    const seconds = String(date.getSeconds()).padStart(2, '0')
    return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
  }

  /**
   * 格式化完整地址
   * @param {Address} address - 地址对象
   * @returns {string} 完整地址字符串
   */
  const formatAddress = (address: Address): string => {
    const parts = [address.province, address.city, address.district, address.detailAddress]
    return parts.filter(p => p?.trim()).join(' ')
  }

  // ==================== 生命周期 ====================

  onMounted(() => {
    if (!authStore.validateUserPermission()) return
    
    loadOrderDetail()
  })
</script>

<style scoped>
@import url('@/static/css/user/订单详情.css');
</style>