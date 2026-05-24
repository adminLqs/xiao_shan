<template>
  <div class="order-detail-page">
    <!-- ========== 页面头部区域 ========== -->
    <div class="page-header">
      <button class="back-btn" @click="goBack">
        <i class="fas fa-arrow-left"></i>
        <span>返回</span>
      </button>
      <h1 class="page-title">
        <i class="fas fa-file-alt"></i>
        <span>订单详情</span>
      </h1>
      <div class="header-placeholder"></div>
    </div>

    <!-- ========== 加载状态 ========== -->
    <div v-if="loading" class="skeleton-detail">
      <div class="skeleton-section">
        <div class="skeleton skeleton-section-title"></div>
        <div class="skeleton-section-content">
          <div class="skeleton skeleton-line long"></div>
          <div class="skeleton skeleton-line medium"></div>
          <div class="skeleton skeleton-line short"></div>
        </div>
      </div>
      <div class="skeleton-section">
        <div class="skeleton skeleton-section-title"></div>
        <div class="skeleton-section-content">
          <div class="skeleton" style="height: 80px; margin-bottom: 10px;"></div>
        </div>
      </div>
      <div class="skeleton-section">
        <div class="skeleton skeleton-section-title"></div>
        <div class="skeleton-order-list">
          <div v-for="i in 2" :key="i" class="skeleton-order-card">
            <div class="skeleton-order-goods">
              <div class="skeleton" style="width: 80px; height: 80px; border-radius: 8px;"></div>
              <div style="flex: 1;">
                <div class="skeleton skeleton-line long" style="margin-bottom: 10px;"></div>
                <div class="skeleton skeleton-line medium"></div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ========== 订单详情内容 ========== -->
    <div v-else-if="orderData" class="detail-content content-wrapper">

      <!-- ========== 订单状态栏 ========== -->
      <div class="status-bar" :class="[getStatusClass(orderData.status)]">
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
        <div class="status-time">
          <span v-if="orderData.paidAt">支付时间：{{ formatDateTime(orderData.paidAt) }}</span>
          <span v-if="orderData.shippedAt">发货时间：{{ formatDateTime(orderData.shippedAt) }}</span>
        </div>
      </div>

      <!-- ========== 订单信息卡片 ========== -->
      <div class="detail-card">
        <div class="card-header">
          <i class="fas fa-receipt"></i>
          <span class="card-title">订单信息</span>
        </div>
        <div class="card-body">
          <div class="info-grid">
            <div class="info-item">
              <span class="info-label">订单号：</span>
              <span class="info-value">{{ orderData.orderNumber }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">创建时间：</span>
              <span class="info-value">{{ formatDateTime(orderData.createdAt) }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">支付方式：</span>
              <span class="info-value">{{ getPaymentMethodText(orderData.paymentMethod) }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">交易单号：</span>
              <span class="info-value">{{ orderData.transactionId || '-' }}</span>
            </div>
            </div>
        </div>
      </div>

      <!-- ========== 收货信息卡片 ========== -->
      <div class="detail-card">
        <div class="card-header">
          <i class="fas fa-map-marker-alt"></i>
          <span class="card-title">收货信息</span>
        </div>
        <div class="card-body">
          <div class="address-info" v-if="addressData">
            <div class="address-line">
              <span class="address-label">收件人：</span>
              <span>{{ addressData.recipientName }}</span>
            </div>
            <div class="address-line">
              <span class="address-label">联系电话：</span>
              <span>{{ addressData.recipientPhone }}</span>
            </div>
            <div class="address-line">
              <span class="address-label">收货地址：</span>
              <span>{{ formatAddress(addressData) }}</span>
            </div>
            <div class="address-line" v-if="addressData.label">
              <span class="address-label">地址标签：</span>
              <span class="address-tag">{{ addressData.label }}</span>
            </div>
          </div>
          <div v-else class="no-address">
            <i class="fas fa-exclamation-triangle"></i>
            <span>暂无收货地址信息</span>
          </div>
        </div>
      </div>

      <!-- ========== 物流信息卡片 ========== -->
      <div class="detail-card" v-if="orderData.status === 'SHIPPED' || orderData.status === 'DELIVERED' || orderData.status === 'COMPLETED'">
        <div class="card-header">
          <i class="fas fa-truck"></i>
          <span class="card-title">物流信息</span>
          <button class="btn-link" @click="viewLogistics">查看物流详情</button>
        </div>
        <div class="card-body">
          <div class="logistics-info">
            <div class="logistics-item">
              <span class="logistics-label">物流公司：</span>
              <span class="logistics-value">{{ orderData.logisticsName || '-' }}</span>
            </div>
            <div class="logistics-item">
              <span class="logistics-label">物流单号：</span>
              <span class="logistics-value">{{ orderData.trackingNumber || '-' }}</span>
            </div>
            <div class="logistics-item">
              <span class="logistics-label">发货时间：</span>
              <span class="logistics-value">{{ formatDateTime(orderData.shippedAt) }}</span>
            </div>
            <div class="logistics-item" v-if="orderData.deliveredAt">
              <span class="logistics-label">签收时间：</span>
              <span class="logistics-value">{{ formatDateTime(orderData.deliveredAt) }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- ========== 商品列表卡片 ========== -->
      <div class="detail-card">
        <div class="card-header">
          <i class="fas fa-boxes"></i>
          <span class="card-title">商品清单</span>
        </div>
        <div class="card-body no-padding">
          <div class="product-list">
            <div v-for="item in orderItems" :key="item.id" class="product-item">
              <img :src="item.productImage || '/images/default-product.png'" class="product-image"
                @click="viewProduct(item.productId)">
              <div class="product-info">
                <div class="product-name">{{ item.productName }}</div>
                <div class="tags-group">
                  <span v-if="item.skuName" class="tag-spec">{{ item.skuName }}</span>
                  <span class="tag-quantity">x{{ item.quantity }}</span>
                </div>
              </div>
              <div class="product-right">
                <div class="product-price">¥{{ formatPrice(item.price) }}</div>
                <div v-if="item.refundStatus" class="refund-status-container">
                  <span
                    v-if="isAfterSaleApproved(item)"
                    class="refund-status-tag tag-waiting-return"
                  >等待买家退货</span>
                  <span
                    v-else-if="item.returnStatus === 'RETURNING'"
                    class="refund-status-tag tag-returning"
                  >退货中</span>
                  <span
                    v-else-if="item.returnStatus === 'RECEIVED'"
                    class="refund-status-tag tag-completed"
                  >已完成</span>
                  <button
                    v-else
                    class="btn-refund-status"
                    :class="getRefundBtnClass(item.refundStatus)"
                    @click="viewRefundProgress(item)"
                  >
                    {{ getRefundBtnText(item.refundStatus) }}
                  </button>
                  <button
                    v-if="item.returnStatus === 'RETURNING'"
                    class="btn-refund-status btn-view-logistics"
                    @click="viewReturnLogistics(item)"
                  >
                    <i class="fas fa-truck"></i>
                    查看退货物流
                  </button>
                </div>
              </div>
            </div>
            <div class="product-list-footer">
              <div class="amount-row">
                <span class="amount-label">商品总额：</span>
                <span class="amount-value">¥{{ formatPrice(orderData.totalAmount) }}</span>
              </div>
              <div class="amount-row total-row">
                <span class="amount-label">实付款：</span>
                <span class="amount-value total-price">¥{{ formatPrice(orderData.totalAmount) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 底部留空防止被固定操作栏遮挡 -->
      <div class="bottom-space"></div>
    </div>

    <!-- ========== 底部固定操作栏 ========== -->
    <div v-if="showBottomBar" class="bottom-action-bar">
      <button v-if="orderData?.status === 'PENDING'" class="btn-danger" @click="cancelOrder">取消订单</button>
      <button v-if="orderData?.status === 'PAID'" class="btn-primary" @click="processOrder">处理订单</button>
      <button v-if="orderData?.status === 'PROCESSING'" class="btn-primary" @click="openShipDialog">发货</button>
      <button v-if="orderData?.status === 'SHIPPED'" class="btn-outline" @click="viewLogistics">查看物流</button>
    </div>

    <!-- ========== 发货弹窗 ========== -->
    <div v-if="showShipDialog" class="modal-overlay" @click="closeShipDialog">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>发货</h3>
          <button class="modal-close" @click="closeShipDialog">
            <i class="fas fa-times"></i>
          </button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label class="form-label required">物流单号</label>
            <input
              type="text"
              v-model="shipForm.trackingNumber"
              class="form-control"
              placeholder="请输入物流单号"
            >
          </div>
          <div class="form-group">
            <label class="form-label required">物流公司</label>
            <select v-model="shipForm.logisticsCode" class="form-control">
              <option value="">请选择物流公司</option>
              <option value="SF">顺丰速运</option>
              <option value="YTO">圆通速递</option>
              <option value="ZTO">中通快递</option>
              <option value="EMS">邮政EMS</option>
              <option value="YD">韵达快递</option>
              <option value="STO">申通快递</option>
              <option value="JT">极兔速递</option>
            </select>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-cancel" @click="closeShipDialog">取消</button>
          <button class="btn-confirm" @click="confirmShip" :disabled="submitting">
            {{ submitting ? '发货中...' : '确认发货' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { ref, computed, onMounted } from 'vue'
  import { useRoute, useRouter } from 'vue-router'
  import { authAPI } from '@/api/authAPI'
  import Message from '@/utils/message'
  import { useAuthStore } from '@/stores/auth'

  const route = useRoute()
  const router = useRouter()
  const authStore = useAuthStore()

  // ==================== 类型定义 ====================

  type OrderStatus = 'PENDING' | 'PAID' | 'PROCESSING' | 'SHIPPED' | 'DELIVERED' | 'COMPLETED' | 'CANCELLED' | 'REFUNDED'
  type PaymentMethod = 'ALIPAY' | 'WECHAT'
  type OrderSource = 'cart' | 'product'

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
    refundStatus?: string
    refundId?: number
    skuName?: string
    refundType?: string
    returnStatus?: string
  }

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

  interface OrderDetailData {
    order: Order
    orderItems: OrderItem[]
    address: Address
  }

  interface ShipForm {
    trackingNumber: string
    logisticsCode: string
  }

  interface StatusConfig {
    text: string
    desc: string
    icon: string
    class: string
  }

  // ==================== 响应式数据 ====================

  const loading = ref(true)
  const submitting = ref(false)
  const showShipDialog = ref(false)

  const orderData = ref<Order | null>(null)
  const orderItems = ref<OrderItem[]>([])
  const addressData = ref<Address | null>(null)

  const shipForm = ref<ShipForm>({
    trackingNumber: '',
    logisticsCode: ''
  })

  // ==================== 计算属性 ====================

  /**
   * 判断是否显示底部操作栏
   * PENDING - 取消订单
   * PAID - 处理订单
   * PROCESSING - 发货
   * SHIPPED - 查看物流
   * 其他状态 - 不显示
   */
  const showBottomBar = computed(() => {
    if (!orderData.value) return false
    const status = orderData.value.status
    return status === 'PENDING' || status === 'PAID' || status === 'PROCESSING' || status === 'SHIPPED'
  })

  // ==================== 静态配置 ====================

  const logisticsMap: Record<string, string> = {
    SF: '顺丰速运',
    YTO: '圆通速递',
    ZTO: '中通快递',
    EMS: '邮政EMS',
    YD: '韵达快递',
    STO: '申通快递',
    JT: '极兔速递'
  }

  const statusConfig: Record<OrderStatus, StatusConfig> = {
    PENDING: {
      text: '待付款',
      desc: '等待买家付款',
      icon: 'fas fa-clock',
      class: 'status-pending'
    },
    PAID: {
      text: '已付款',
      desc: '买家已付款，请及时处理',
      icon: 'fas fa-check-circle',
      class: 'status-paid'
    },
    PROCESSING: {
      text: '处理中',
      desc: '订单已确认，正在备货中',
      icon: 'fas fa-spinner',
      class: 'status-processing'
    },
    SHIPPED: {
      text: '已发货',
      desc: '商品已发出，等待买家收货',
      icon: 'fas fa-truck',
      class: 'status-shipped'
    },
    DELIVERED: {
      text: '已送达',
      desc: '商品已送达，请等待买家确认',
      icon: 'fas fa-home',
      class: 'status-delivered'
    },
    COMPLETED: {
      text: '已完成',
      desc: '订单已完成，感谢您的支持',
      icon: 'fas fa-check-double',
      class: 'status-completed'
    },
    CANCELLED: {
      text: '已取消',
      desc: '订单已取消',
      icon: 'fas fa-times-circle',
      class: 'status-cancelled'
    },
    REFUNDED: {
      text: '已退款',
      desc: '订单已退款',
      icon: 'fas fa-undo-alt',
      class: 'status-refunded'
    }
  }

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
      const orderId = route.params.id

      if (!orderId) {
        Message.error('订单ID不存在')
        router.push({ name: 'SellerOrders' })
        return
      }

      const response = await authAPI.getSellerOrderDetail(Number(orderId))

      if (response.success && response.data?.orderDetail) {
        const data = response.data.orderDetail as OrderDetailData

        orderData.value = data.order
        orderItems.value = data.orderItems.map(item => ({
          ...item,
          totalPrice: item.price * item.quantity
        }))
        addressData.value = data.address
      } else {
        Message.error(response.message || '获取订单详情失败')
      }
    } catch (error: any) {
      Message.error(error.message || '加载失败')
    } finally {
      loading.value = false
    }
  }



  // ==================== 订单操作 ====================

  /**
   * 处理订单（PAID → PROCESSING）
   * @returns {Promise<void>}
   */
  const processOrder = async (): Promise<void> => {
    try {
      await Message.confirm('确定要处理该订单吗？处理后将进入备货状态。', '处理订单')

      const response = await authAPI.processOrder(orderData.value!.id)

      if (response.success) {
        Message.success('订单已处理')
        await loadOrderDetail()
      }
    } catch (error: any) {
      if (error !== 'cancel') {
        Message.error(error.message || '操作失败')
      }
    }
  }

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
        Message.error(error.message || '操作失败')
      }
    }
  }

  // ==================== 发货相关 ====================

  const openShipDialog = (): void => {
    shipForm.value = { trackingNumber: '', logisticsCode: '' }
    showShipDialog.value = true
  }

  const closeShipDialog = (): void => {
    showShipDialog.value = false
    shipForm.value = { trackingNumber: '', logisticsCode: '' }
  }

  /**
   * 确认发货
   * @returns {Promise<void>}
   */
  const confirmShip = async (): Promise<void> => {
    if (!shipForm.value.trackingNumber.trim()) {
      Message.error('请输入物流单号')
      return
    }

    if (!shipForm.value.logisticsCode) {
      Message.error('请选择物流公司')
      return
    }

    submitting.value = true

    try {
      const data = {
        trackingNumber: shipForm.value.trackingNumber,
        logisticsCode: shipForm.value.logisticsCode,
        logisticsName: logisticsMap[shipForm.value.logisticsCode] || ''
      }

      const orderId = orderData.value?.id
      if (!orderId) {
        Message.error('订单信息不存在')
        return
      }
      const response = await authAPI.shipOrder(orderId, data)

      if (response.success) {
        Message.success('发货成功')
        closeShipDialog()
        await loadOrderDetail()
      }
    } catch (error: any) {
      Message.error(error.message || '发货失败')
    } finally {
      submitting.value = false
    }
  }

  // ==================== 页面跳转 ====================

  const viewLogistics = (): void => {
    router.push({
      name: 'SellerLogistics',
      query: { orderId: orderData.value?.id }
    })
  }

  /**
   * 查看退款进度
   * @param item - 订单商品项
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

        if (refundData.refundType === 'AFTER_SALE' &&
            refundData.refundStatus === 'APPROVED' &&
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

        if (refundData.returnStatus === 'RETURNING' && refundData.returnTrackingNumber) {
          router.push({
            name: 'SellerLogistics',
            query: {
              trackingNumber: refundData.returnTrackingNumber,
              logisticsName: refundData.returnLogisticsName,
              refundId: String(item.refundId)
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

  const viewProduct = (productId: number): void => {
    router.push({ name: 'ProductDetail', params: { productId } })
  }

  const goBack = (): void => {
    router.back()
  }

  // ==================== 工具函数 ====================

  const getStatusConfig = (status: OrderStatus): StatusConfig => {
    return statusConfig[status] || statusConfig.PENDING
  }

  const getStatusClass = (status: OrderStatus): string => {
    return getStatusConfig(status).class
  }

  /**
   * 判断是否为售后已同意状态
   */
  const isAfterSaleApproved = (item: OrderItem): boolean => {
    return item.refundType === 'AFTER_SALE' &&
           (item.refundStatus === 'APPROVED' || item.refundStatus === 'WAITING_RETURN') &&
           (!item.returnStatus || item.returnStatus === 'NULL' || item.returnStatus === 'NONE')
  }

  /**
   * 查看退货物流
   */
  const viewReturnLogistics = (item: OrderItem): void => {
    if (!item.refundId) {
      Message.error('退款记录不存在')
      return
    }
    router.push({
      name: 'SellerLogistics',
      query: {
        refundId: String(item.refundId),
        type: 'return'
      }
    })
  }

  /**
   * 获取订单整体退款状态
   * @returns {string | null} 退款状态文本或null
   */
  const getOrderRefundStatus = (): string | null => {
    const items = orderItems.value || []
    if (items.length === 0) return null

    const hasAfterSaleApproved = items.some(item => isAfterSaleApproved(item))
    const hasReturning = items.some(item => item.returnStatus === 'RETURNING')
    const hasReceived = items.some(item => item.returnStatus === 'RECEIVED')

    const refundingCount = items.filter(item =>
      (item.refundStatus as string) === 'REFUNDING' || (item.refundStatus as string) === 'AFTER_SALE' ||
      (item.refundStatus as string) === 'WAITING_RETURN' || (item.refundStatus as string) === 'RETURNING' ||
      (item.refundStatus as string) === 'APPROVED'
    ).length
    const refundedCount = items.filter(item => (item.refundStatus as string) === 'COMPLETED').length

    if (hasAfterSaleApproved) return '等待买家退货'
    if (hasReturning) return '退货中'
    if (hasReceived) return '已完成'
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
   * 获取退款状态样式类
   */
  const getRefundStatusClass = (refundStatus: string): string => {
    const map: Record<string, string> = {
      REFUNDING: 'refund-pending',
      AFTER_SALE: 'refund-processing',
      WAITING_RETURN: 'refund-waiting',
      RETURNING: 'refund-waiting',
      APPROVED: 'refund-approved',
      COMPLETED: 'refund-completed',
      FAILED: 'refund-failed'
    }
    return map[refundStatus] || ''
  }

  /**
   * 获取退款状态文本（商家端）
   */
  const getRefundStatusText = (refundStatus: string, refundType?: string, returnStatus?: string): string => {
    if (refundType === 'AFTER_SALE' && refundStatus === 'PROCESSING') {
      return '售后处理中'
    }
    if (refundType === 'AFTER_SALE' && refundStatus === 'APPROVED') {
      return '等待买家退货'
    }
    if (returnStatus === 'RETURNING') {
      return '退货中'
    }
    if (returnStatus === 'RECEIVED') {
      return '已收货，退款中'
    }

    const map: Record<string, string> = {
      REFUNDING: '退款中',
      AFTER_SALE: '售后处理中',
      WAITING_RETURN: '待退货',
      RETURNING: '退货中',
      RECEIVED: '已完成',
      APPROVED: '已同意',
      COMPLETED: '已完成',
      FAILED: '已拒绝',
      SUCCESS: '已完成'
    }
    return map[refundStatus] || refundStatus
  }

  const getRefundBtnClass = (status: string): string => {
    const map: Record<string, string> = {
      REFUNDING: 'btn-refunding',
      AFTER_SALE: 'btn-after-sale',
      WAITING_RETURN: 'btn-after-sale',
      RETURNING: 'btn-receive-goods',
      RECEIVED: 'btn-refund-done',
      APPROVED: 'btn-after-sale',
      COMPLETED: 'btn-refund-done',
      FAILED: 'btn-refund-done'
    }
    return map[status] || ''
  }

  const getRefundBtnText = (status: string): string => {
    const map: Record<string, string> = {
      REFUNDING: '处理退款',
      AFTER_SALE: '处理售后',
      WAITING_RETURN: '等待买家退货',
      RETURNING: '查看退货物流',
      RECEIVED: '确认收货并退款',
      APPROVED: '等待买家退货',
      COMPLETED: '已退款',
      FAILED: '查看退款'
    }
    return map[status] || ''
  }

  const getStatusText = (status: OrderStatus): string => {
    return getStatusConfig(status).text
  }

  const getStatusDesc = (status: OrderStatus): string => {
    return getStatusConfig(status).desc
  }

  const getStatusIcon = (status: OrderStatus): string => {
    return getStatusConfig(status).icon
  }

  const getPaymentMethodText = (method: PaymentMethod | null): string => {
    if (!method) return '-'
    return paymentMethodMap[method] || method
  }

  const formatPrice = (price: number): string => {
    if (price == null) return '0.00'
    return price.toFixed(2)
  }

  const formatDateTime = (dateStr: string | null): string => {
    if (!dateStr) return '-'
    try {
      const date = new Date(dateStr)
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      const hours = String(date.getHours()).padStart(2, '0')
      const minutes = String(date.getMinutes()).padStart(2, '0')
      return `${year}-${month}-${day} ${hours}:${minutes}`
    } catch {
      return '-'
    }
  }

  const formatAddress = (address: Address | null): string => {
    if (!address) return '-'
    const parts = [address.province, address.city, address.district, address.detailAddress]
    return parts.filter(p => p?.trim()).join(' ')
  }

  // ==================== 生命周期 ====================

  onMounted(() => {
  if (!authStore.validateSellerPermission()) return
  loadOrderDetail()
})
</script>

<style scoped>
@import url('@/static/css/seller/订单详情页.css');
</style>
