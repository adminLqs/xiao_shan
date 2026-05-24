<template>
  <div class="order-detail-container page-container">
    <div class="page-navbar">
      <button class="page-nav-back" @click="router.back()">
        <i class="fas fa-chevron-left"></i>
      </button>
      <div class="page-nav-title">
        <i class="fas fa-file-invoice"></i>
        <span>订单详情</span>
      </div>
      <div class="page-nav-right"></div>
    </div>

    <!-- 加载状态 - 骨架屏 -->
    <div v-if="loading" class="order-detail-content">
      <div class="skeleton-card skeleton-status-bar"></div>
      <div class="skeleton-card">
        <div class="skeleton-line short"></div>
        <div class="skeleton-line"></div>
        <div class="skeleton-line medium"></div>
      </div>
      <div class="skeleton-card">
        <div class="skeleton-line short"></div>
        <div class="skeleton-line"></div>
        <div class="skeleton-line medium"></div>
      </div>
      <div class="skeleton-card">
        <div class="skeleton-line short"></div>
        <div class="skeleton-item">
          <div class="skeleton-avatar"></div>
          <div class="skeleton-item-content">
            <div class="skeleton-line medium"></div>
            <div class="skeleton-line short"></div>
          </div>
        </div>
        <div class="skeleton-line"></div>
      </div>
    </div>

    <!-- 订单不存在 -->
    <div v-else-if="!orderData" class="empty-cart">
      <i class="fas fa-search"></i>
      <p>订单不存在</p>
      <button class="btn btn-primary" @click="goBack">返回订单列表</button>
    </div>

    <!-- 订单详情内容 -->
    <div v-else class="order-detail-content">
      <!-- 订单状态栏 -->
      <div class="order-status-bar" :class="[getStatusClass(orderData.status), getRefundStatusClass(orderData.status)]">
        <div class="status-icon">
          <i :class="getStatusIcon(orderData.status)"></i>
        </div>
        <div class="status-info">
          <div class="status-text">
            {{ getStatusText(orderData.status) }}
            <span v-if="getOrderRefundStatus()" class="refund-status-badge" :class="getRefundBadgeClass()">
              {{ getOrderRefundStatus() }}
            </span>
          </div>
          <div class="status-desc">{{ getStatusDesc(orderData.status) }}</div>
        </div>
      </div>

      <!-- 订单基本信息 -->
      <div class="info-card">
        <div class="card-title"><i class="fas fa-receipt"></i>订单信息</div>
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
        <div class="card-title"><i class="fas fa-map-marker-alt"></i>收货地址</div>
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
        <div class="card-title"><i class="fas fa-boxes"></i>商品清单</div>
        <div class="product-list">
          <div v-for="item in orderItems" :key="item.id" class="product-card">
            <!-- 商品信息 -->
            <div class="product-item">
              <img :src="item.productImage || '/images/default-product.png'" class="product-image" @click="viewProduct(item.productId)">
              <div class="product-info">
                <div class="product-name">{{ item.productName }}</div>
                <div class="tags-group">
                  <span v-if="item.skuName" class="tag-spec">{{ item.skuName }}</span>
                  <span class="tag-quantity">x{{ item.quantity }}</span>
                </div>
              </div>
              <div class="product-price">¥{{ formatPrice(item.price) }}</div>
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

        <!-- 底部统一操作区 -->
        <div class="order-actions-bar">
          <button
            v-if="orderData?.status === 'PAID' || orderData?.status === 'PROCESSING'"
            @click="handleRefund"
            class="action-btn refund"
          >申请退款</button>
          <template v-if="orderData?.status === 'COMPLETED'">
            <button
              v-if="hasUnreviewedItems"
              @click="goToReview"
              class="action-btn review"
            >评价</button>
            <button
              v-else
              class="action-btn review disabled"
              disabled
            >已评价</button>
          </template>
          <button
            v-if="showAfterSaleBtn"
            @click="handleAfterSale"
            class="action-btn after-sale"
          >申请售后</button>
        </div>
      </div>

      <!-- 底部留空防止被固定操作栏遮挡 -->
      <div class="bottom-space"></div>
    </div>

    <!-- 商品选择弹窗 -->
    <div v-if="showItemSelector" class="item-selector-overlay" @click.self="showItemSelector = false">
      <div class="item-selector-dialog">
        <div class="dialog-header">
          <span>{{ selectorTitle }}</span>
          <button class="dialog-close" @click="showItemSelector = false">
            <i class="fas fa-times"></i>
          </button>
        </div>
        <div class="dialog-content">
          <div
            v-for="item in selectableItems"
            :key="item.id"
            class="selector-item"
            :class="{ selected: selectedItemId === item.id, disabled: !isItemSelectable(item) }"
            @click="isItemSelectable(item) && selectItem(item.id)"
          >
            <img :src="item.productImage" class="selector-image">
            <div class="selector-info">
              <div class="selector-name">{{ item.productName }}</div>
              <div v-if="item.skuName" class="selector-spec">{{ item.skuName }}</div>
              <div class="selector-tags">
                <span v-if="selectorType === 'REVIEW' && item.isReviewed" class="tag reviewed">已评价</span>
                <span v-else-if="selectorType !== 'REVIEW' && item.refundStatus === 'COMPLETED'" class="tag refund-done">已退款</span>
                <span v-else-if="selectorType !== 'REVIEW' && item.refundStatus" class="tag refunding">{{ getRefundStatusText(item.refundStatus, item.refundType, item.returnStatus) }}</span>
              </div>
            </div>
            <div class="selector-radio">
              <i v-if="selectedItemId === item.id" class="fas fa-check-circle"></i>
              <i v-else-if="isItemSelectable(item)" class="far fa-circle"></i>
            </div>
          </div>
        </div>
        <div class="dialog-footer">
          <button class="btn-cancel" @click="showItemSelector = false">取消</button>
          <button class="btn-confirm" @click="confirmSelection" :disabled="!selectedItemId">确定</button>
        </div>
      </div>
    </div>

    <!-- 底部固定操作栏 -->
    <div v-if="showBottomBar" class="bottom-bar-fixed">
      <template v-if="orderData?.status === 'PENDING'">
        <button class="btn-primary" @click="goToPay">去支付</button>
        <button class="btn-danger" @click="cancelOrder">取消订单</button>
      </template>
      <template v-if="['SHIPPED', 'DELIVERED'].includes(orderData?.status || '')">
        <button class="btn-success" @click="confirmReceive">确认收货</button>
      </template>
      <template v-if="['SHIPPED', 'DELIVERED', 'COMPLETED'].includes(orderData?.status || '') && orderData?.trackingNumber">
        <button class="btn-outline" @click="viewLogistics(orderData?.id)">查看物流</button>
      </template>
      <template v-if="['CANCELLED', 'REFUNDED'].includes(orderData?.status || '')">
        <button class="btn-danger" @click="deleteOrder">删除订单</button>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { ref, computed, onMounted } from 'vue'
  import { useRoute, useRouter } from 'vue-router'
  import { authAPI } from '@/api/authAPI'
  import Message from '@/utils/message'
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
    skuName?: string
    isReviewed: boolean
    reviewedAt: string | null
    createdAt: string
    refundStatus?: string
    refundId?: number
    refundType?: string
    returnStatus?: string
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

  const loading = ref(true)
  const orderData = ref<Order | null>(null)
  const orderItems = ref<OrderItem[]>([])
  const addressData = ref<Address | null>(null)

  // 商品选择弹窗相关
  const showItemSelector = ref(false)
  const selectorTitle = ref('')
  const selectedItemId = ref<number | null>(null)
  const selectorType = ref<'REFUND' | 'AFTER_SALE' | 'REVIEW'>('REFUND')
  const selectableItems = ref<OrderItem[]>([])

  // ==================== 计算属性 ====================

  const showBottomBar = computed(() => {
    if (!orderData.value) return false
    const status = orderData.value.status
    return ['PENDING', 'SHIPPED', 'DELIVERED', 'COMPLETED', 'CANCELLED', 'REFUNDED'].includes(status)
  })

  // 是否有未评价的商品
  const hasUnreviewedItems = computed(() => {
    return orderItems.value.some(item => item.isReviewed === false)
  })

  // 是否显示申请售后按钮
  const showAfterSaleBtn = computed(() => {
    if (!orderData.value) return false
    const status = orderData.value.status
    return ['SHIPPED', 'DELIVERED', 'COMPLETED'].includes(status)
  })

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
        Message.error('订单ID不存在')
        router.push({ name: 'UserOrders' })
        return
      }

      const response = await authAPI.getOrderDetail(orderId)

      if (response.success && response.data?.orderDetail) {
        const data = response.data.orderDetail as OrderDetailData

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
        Message.error(response.message || '订单不存在')
        router.push({ name: 'UserOrders' })
      }
    } catch (error: any) {
      Message.error(error.message || '加载失败')
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
      await Message.confirm('确定要取消该订单吗？', '取消订单')

      const response = await authAPI.cancelOrder(orderData.value!.id)
      if (response.success) {
        Message.success('取消成功')
        await loadOrderDetail()
      }
    } catch (error: any) {
      if (error !== 'cancel') {
        Message.error(error.message || '取消失败')
      }
    }
  }

  /**
   * 删除订单
   * @returns {Promise<void>}
   */
  const deleteOrder = async (): Promise<void> => {
    try {
      await Message.confirm('确定要删除该订单吗？', '删除订单')

      const response = await authAPI.deleteOrder(orderData.value!.id)
      if (response.success) {
        Message.success('删除成功')
        router.push({ name: 'UserOrders' })
      }
    } catch (error: any) {
      if (error !== 'cancel') {
        Message.error(error.message || '删除失败')
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
        const paymentHtml = response.data?.paymentHtml

        const payWindow = window.open('', '_blank')
        if (payWindow) {
          payWindow.document.write(paymentHtml)
          payWindow.document.close()
        }
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
   * 确认收货
   * @returns {Promise<void>}
   */
  const confirmReceive = async (): Promise<void> => {
    try {
      await Message.confirm('请确认已收到商品', '确认收货')

      const response = await authAPI.confirmReceive(orderData.value!.id)
      if (response.success) {
        Message.success('确认成功')
        await loadOrderDetail()
      }
    } catch (error: any) {
      if (error !== 'cancel') {
        Message.error(error.message || '操作失败')
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
   * 处理申请退款
   */
  const handleRefund = (): void => {
    showItemSelectorModal('申请退款', 'REFUND', orderItems.value)
  }

  /**
   * 处理申请售后
   */
  const handleAfterSale = (): void => {
    showItemSelectorModal('申请售后', 'AFTER_SALE', orderItems.value)
  }

  /**
   * 处理评价
   */
  const goToReview = (): void => {
    // 传入所有商品，弹窗内根据 isReviewed 状态显示标签和禁用
    showItemSelectorModal('选择评价商品', 'REVIEW', orderItems.value)
  }

  /**
   * 显示商品选择弹窗
   * @param {string} title - 弹窗标题
   * @param {'REFUND' | 'AFTER_SALE' | 'REVIEW'} type - 操作类型
   * @param {OrderItem[]} items - 可选商品列表
   */
  const showItemSelectorModal = (title: string, type: 'REFUND' | 'AFTER_SALE' | 'REVIEW', items: OrderItem[]): void => {
    selectorTitle.value = title
    selectorType.value = type
    selectableItems.value = items
    selectedItemId.value = null
    showItemSelector.value = true
  }

  /**
   * 选择商品
   * @param {number} itemId - 商品ID
   */
  const selectItem = (itemId: number): void => {
    selectedItemId.value = itemId
  }

  /**
   * 确认选择
   */
  const confirmSelection = (): void => {
    if (!selectedItemId.value) return

    switch (selectorType.value) {
      case 'REFUND':
        goToRefund(selectedItemId.value)
        break
      case 'AFTER_SALE':
        goToAfterSale(selectedItemId.value)
        break
      case 'REVIEW':
        router.push({ name: 'Review', params: { orderItemId: selectedItemId.value } })
        break
    }

    showItemSelector.value = false
    selectedItemId.value = null
  }

  /**
   * 判断商品是否可选
   * @param {OrderItem} item - 订单项
   * @returns {boolean} 是否可选
   */
  const isItemSelectable = (item: OrderItem): boolean => {
    if (selectorType.value === 'REVIEW') {
      return item.isReviewed === false
    }
    if (selectorType.value === 'REFUND' || selectorType.value === 'AFTER_SALE') {
      // 无退款记录 → 可选
      if (!item.refundStatus) return true
      // 已退款或退款失败 → 不可选
      if (item.refundStatus === 'COMPLETED' || item.refundStatus === 'FAILED') return false
      // 其他退款状态（退款中/售后中/待退货） → 不可选
      return false
    }
    return true
  }

  /**
   * 申请退款
   * @param {number} orderItemId - 订单项ID
   */
  const goToRefund = (orderItemId: number): void => {
    router.push({ name: 'Refund', query: { orderItemId: String(orderItemId), type: 'REFUND' } })
  }

  /**
   * 申请售后
   * @param {number} orderItemId - 订单项ID
   */
  const goToAfterSale = (orderItemId: number): void => {
    router.push({ name: 'Refund', query: { orderItemId: String(orderItemId), type: 'AFTER_SALE' } })
  }

  /**
   * 查看退款/售后进度
   * @param {OrderItem} item - 订单项
   */
  const viewRefundProgress = async (item: OrderItem): Promise<void> => {
    if (!item.refundId) {
      Message.error('退款记录不存在')
      return
    }

    try {
      const response = await authAPI.getRefundDetail(item.refundId)

      if (response.success && response.data) {
        const refundData = response.data

        if (refundData.returnStatus === 'RETURNING') {
          router.push({
            name: 'UserLogistics',
            query: {
              refundId: String(item.refundId),
              trackingNumber: refundData.returnTrackingNumber,
              logisticsName: refundData.returnLogisticsName
            }
          })
          return
        }

        if (refundData.refundType === 'AFTER_SALE' &&
            (refundData.refundStatus === 'APPROVED' || refundData.refundStatus === 'WAITING_RETURN') &&
            (!refundData.returnStatus || refundData.returnStatus === 'NULL' || refundData.returnStatus === null)) {
          router.push({
            name: 'ReturnGoods',
            query: {
              refundId: String(item.refundId),
              orderItemId: String(item.id)
            }
          })
          return
        }
      }
    } catch (error) {
      console.error('获取退款详情失败', error)
    }

    router.push({ name: 'RefundChat', params: { refundId: String(item.refundId) } })
  }

  /**
   * 获取退款状态显示文字（用户端）
   * @param {string} status - 退款状态
   * @param {string} refundType - 退款类型
   * @param {string} returnStatus - 退货状态
   * @returns {string} 显示文字
   */
  const getRefundStatusText = (status: string, refundType?: string, returnStatus?: string): string => {
    if (refundType === 'AFTER_SALE' && status === 'PROCESSING') {
      return '售后处理中'
    }
    if (refundType === 'AFTER_SALE' && status === 'APPROVED') {
      return '商家已同意，请退货'
    }
    if (returnStatus === 'RETURNING') {
      return '等待商家收货'
    }
    if (returnStatus === 'RECEIVED') {
      return '退款成功'
    }

    const map: Record<string, string> = {
      'REFUNDING': '退款中',
      'AFTER_SALE': '售后处理中',
      'WAITING_RETURN': '待退货',
      'RETURNING': '等待商家收货',
      'RECEIVED': '退款成功',
      'APPROVED': '去退货',
      'COMPLETED': '退款成功',
      'FAILED': '已拒绝',
      'SUCCESS': '退款成功'
    }
    return map[status] || status
  }

  /**
   * 获取退款状态样式类
   * @param {string} status - 退款状态
   * @returns {string} CSS类名
   */
  const getRefundStatusClass = (status: string): string => {
    const map: Record<string, string> = {
      'REFUNDING': 'status-refunding',
      'AFTER_SALE': 'status-after-sale',
      'WAITING_RETURN': 'status-waiting-return',
      'RETURNING': 'status-returning',
      'APPROVED': 'status-approved',
      'COMPLETED': 'status-completed',
      'FAILED': 'status-failed'
    }
    return map[status] || ''
  }

  /**
   * 获取退款状态图标
   * @param {string} status - 退款状态
   * @returns {string} FontAwesome图标类名
   */
  const getRefundStatusIcon = (status: string): string => {
    const map: Record<string, string> = {
      'REFUNDING': 'fa-clock',
      'AFTER_SALE': 'fa-sync',
      'WAITING_RETURN': 'fa-truck',
      'RETURNING': 'fa-truck',
      'APPROVED': 'fa-check-circle',
      'COMPLETED': 'fa-check-double',
      'FAILED': 'fa-times-circle'
    }
    return map[status] || 'fa-info-circle'
  }

  /**
   * 联系客服
   */
  const contactService = (): void => {
    Message.info('客服热线：400-888-6666')
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

  /**
   * 跳转退货页面
   */
  const goToReturn = (item: OrderItem): void => {
    if (item.refundId) {
      router.push({
        name: 'ReturnGoods',
        query: {
          refundId: String(item.refundId),
          orderItemId: String(item.id)
        }
      })
    }
  }

  /**
   * 查看退货物流
   */
  const viewReturnLogistics = (item: OrderItem): void => {
    if (item.refundId) {
      router.push({
        name: 'UserLogistics',
        query: {
          refundId: String(item.refundId),
          trackingNumber: '',
          logisticsName: ''
        }
      })
    }
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
   * 获取订单整体退款状态
   * @returns {string | null} 退款状态文本或null
   */
  const getOrderRefundStatus = (): string | null => {
    const items = orderItems.value || []
    if (items.length === 0) return null

    const refundingCount = items.filter(item =>
      (item.refundStatus as string) === 'REFUNDING' || (item.refundStatus as string) === 'AFTER_SALE' ||
      (item.refundStatus as string) === 'WAITING_RETURN' || (item.refundStatus as string) === 'RETURNING' ||
      (item.refundStatus as string) === 'APPROVED'
    ).length
    const refundedCount = items.filter(item => (item.refundStatus as string) === 'COMPLETED').length

    if (refundingCount === items.length) return '退款中'
    if (refundedCount === items.length) return '已退款'
    if (refundingCount > 0 || refundedCount > 0) return '部分退款'
    return null
  }

  /**
   * 获取退款状态徽章样式类
   * @returns {string} CSS类名
   */
  const getRefundBadgeClass = (): string => {
    const items = orderItems.value || []
    if (items.length === 0) return ''

    const refundingCount = items.filter(item =>
      (item.refundStatus as string) === 'REFUNDING' || (item.refundStatus as string) === 'AFTER_SALE' ||
      (item.refundStatus as string) === 'WAITING_RETURN' || (item.refundStatus as string) === 'RETURNING' ||
      (item.refundStatus as string) === 'APPROVED'
    ).length
    const refundedCount = items.filter(item => (item.refundStatus as string) === 'COMPLETED').length

    if (refundingCount === items.length) return 'badge-refunding'
    if (refundedCount === items.length) return 'badge-refunded'
    return 'badge-partial-refund'
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
