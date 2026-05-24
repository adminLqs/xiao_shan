<template>
  <div class="order-list">
    <!-- 骨架屏 -->
    <div v-if="loading && orders.length === 0">
      <div v-for="i in 3" :key="i" class="skeleton-card">
        <div class="skeleton-line short"></div>
        <div class="skeleton-line medium" style="margin-top: 12px;"></div>
        <div class="skeleton-line" style="margin-top: 8px;"></div>
        <div class="skeleton-line short" style="margin-top: 12px;"></div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else-if="orders.length === 0" class="empty-cart">
      <i class="fas fa-shopping-bag"></i>
      <p>暂无订单</p>
      <button class="btn-primary" @click="goShopping">去逛逛</button>
    </div>

    <!-- 订单列表 -->
    <div v-else>
      <div v-for="orderWrapper in orders" :key="orderWrapper.order.id" class="order-card">
        <!-- 订单头部 -->
        <div class="order-header">
          <div class="order-header-top">
            <span class="order-number">订单号：{{ orderWrapper.order.orderNumber }}</span>
            <div class="order-status-group">
              <span v-if="!getOrderRefundStatus(orderWrapper)" class="order-status" :class="getStatusClass(orderWrapper.order.status)">
                {{ getStatusText(orderWrapper.order.status) }}
              </span>
              <span v-if="getOrderRefundStatus(orderWrapper)" class="order-status refund-status" :class="getOrderRefundStatusClass(orderWrapper)">
                {{ getOrderRefundStatus(orderWrapper) }}
              </span>
            </div>
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
              <div class="tags-group">
                <span v-if="item.skuName" class="tag-spec">{{ item.skuName }}</span>
                <span class="tag-quantity">x{{ item.quantity }}</span>
              </div>
            </div>
            <div class="item-price">¥{{ formatPrice(item.price) }}</div>
          </div>
        </div>

        <!-- 订单底部 -->
        <div class="order-footer">
          <div class="order-actions">
            <!-- 退款状态（包括部分退款）- 优先处理 -->
            <template v-if="getOrderRefundStatus(orderWrapper)">
              <button 
                v-if="orderWrapper.order.trackingNumber" 
                class="btn-outline" 
                @click="viewLogistics(orderWrapper.order.id)" 
              >查看物流</button>
              <button class="btn-outline" @click="viewOrderDetail(orderWrapper.order.id)">查看详情</button>
            </template>

            <!-- 待付款状态：去支付 + 取消订单 + 查看详情 -->
            <template v-else-if="orderWrapper.order.status === 'PENDING'">
              <button class="btn-pay" @click="goToPay(orderWrapper.order.id)">去支付</button>
              <button class="btn-outline btn-danger" @click="cancelOrder(orderWrapper.order.id)">取消订单</button>
              <button class="btn-outline" @click="viewOrderDetail(orderWrapper.order.id)">查看详情</button>
            </template>

            <!-- 已付款状态：申请退款 + 查看详情 -->
            <template v-else-if="orderWrapper.order.status === 'PAID'">
              <template v-if="orderWrapper.orderItems.length === 1">
                <button
                  v-if="orderWrapper.orderItems[0]!.refundStatus && orderWrapper.orderItems[0]!.refundStatus !== 'COMPLETED'"
                  class="btn-outline"
                  @click="goToRefund(orderWrapper)"
                >{{ getRefundStatusText(orderWrapper.orderItems[0]!.refundStatus) }}</button>
                <button
                  v-else
                  :class="canRefund(orderWrapper.order) ? 'btn-outline' : 'btn-outline btn-expired'"
                  :disabled="!canRefund(orderWrapper.order)"
                  @click="canRefund(orderWrapper.order) && goToRefund(orderWrapper)"
                >{{ canRefund(orderWrapper.order) ? '申请退款' : '已过权益期' }}</button>
                <button class="btn-outline" @click="viewOrderDetail(orderWrapper.order.id)">查看详情</button>
              </template>
              <template v-else>
                <button class="btn-outline" @click="viewOrderDetail(orderWrapper.order.id)">查看详情</button>
              </template>
            </template>

            <!-- 处理中状态：查看详情 -->
            <template v-else-if="orderWrapper.order.status === 'PROCESSING'">
              <button class="btn-outline" @click="viewOrderDetail(orderWrapper.order.id)">查看详情</button>
            </template>

            <!-- 已发货状态：退货物流 + 确认收货 + 查看物流 + 查看详情 + 更多 -->
            <template v-else-if="orderWrapper.order.status === 'SHIPPED'">
              <template v-if="orderWrapper.orderItems.length === 1">
                <button
                  v-if="orderWrapper.orderItems[0]!.refundStatus === 'RETURNING'"
                  class="btn-outline"
                  @click="viewReturnLogistics(orderWrapper.orderItems[0]!)"
                >查看退货物流</button>
                <button class="btn-success" @click="confirmReceive(orderWrapper.order.id)">确认收货</button>
                <button
                  v-if="orderWrapper.order.trackingNumber"
                  class="btn-outline"
                  @click="viewLogistics(orderWrapper.order.id)"
                >查看物流</button>
                <button class="btn-outline" @click="viewOrderDetail(orderWrapper.order.id)">查看详情</button>
                <button class="btn-more" @click="toggleMoreActions(orderWrapper.order.id)">
                  <i class="fas fa-ellipsis-h"></i>
                </button>
                <div class="more-actions-dropdown" v-if="showMoreActions[orderWrapper.order.id]">
                  <button class="dropdown-item" @click="handleAfterSaleAction(orderWrapper)">
                    {{ orderWrapper.orderItems[0]!.refundStatus && orderWrapper.orderItems[0]!.refundStatus !== 'COMPLETED'
                      ? getRefundStatusText(orderWrapper.orderItems[0]!.refundStatus)
                      : (canAfterSale(orderWrapper.order) ? '申请售后' : '已过权益期') }}
                  </button>
                </div>
              </template>
              <template v-else>
                <button class="btn-success" @click="confirmReceive(orderWrapper.order.id)">确认收货</button>
                <button
                  v-if="orderWrapper.order.trackingNumber"
                  class="btn-outline"
                  @click="viewLogistics(orderWrapper.order.id)"
                >查看物流</button>
                <button class="btn-outline" @click="viewOrderDetail(orderWrapper.order.id)">查看详情</button>
              </template>
            </template>

            <!-- 已送达状态：退货物流 + 确认收货 + 查看物流 + 查看详情 + 更多 -->
            <template v-else-if="orderWrapper.order.status === 'DELIVERED'">
              <template v-if="orderWrapper.orderItems.length === 1">
                <button
                  v-if="orderWrapper.orderItems[0]!.refundStatus === 'RETURNING'"
                  class="btn-outline"
                  @click="viewReturnLogistics(orderWrapper.orderItems[0]!)"
                >查看退货物流</button>
                <button class="btn-success" @click="confirmReceive(orderWrapper.order.id)">确认收货</button>
                <button
                  v-if="orderWrapper.order.trackingNumber"
                  class="btn-outline"
                  @click="viewLogistics(orderWrapper.order.id)"
                >查看物流</button>
                <button class="btn-outline" @click="viewOrderDetail(orderWrapper.order.id)">查看详情</button>
                <button class="btn-more" @click="toggleMoreActions(orderWrapper.order.id)">
                  <i class="fas fa-ellipsis-h"></i>
                </button>
                <div class="more-actions-dropdown" v-if="showMoreActions[orderWrapper.order.id]">
                  <button class="dropdown-item" @click="handleAfterSaleAction(orderWrapper)">
                    {{ orderWrapper.orderItems[0]!.refundStatus && orderWrapper.orderItems[0]!.refundStatus !== 'COMPLETED'
                      ? getRefundStatusText(orderWrapper.orderItems[0]!.refundStatus)
                      : (canAfterSale(orderWrapper.order) ? '申请售后' : '已过权益期') }}
                  </button>
                </div>
              </template>
              <template v-else>
                <button class="btn-success" @click="confirmReceive(orderWrapper.order.id)">确认收货</button>
                <button
                  v-if="orderWrapper.order.trackingNumber"
                  class="btn-outline"
                  @click="viewLogistics(orderWrapper.order.id)"
                >查看物流</button>
                <button class="btn-outline" @click="viewOrderDetail(orderWrapper.order.id)">查看详情</button>
              </template>
            </template>

            <!-- 已完成状态：评价 + 查看物流 + 查看详情 + 更多 -->
            <template v-else-if="orderWrapper.order.status === 'COMPLETED'">
              <template v-if="orderWrapper.orderItems.length === 1">
                <button
                  v-if="orderWrapper.orderItems[0]!.isReviewed"
                  class="btn-outline"
                  disabled
                >已评价</button>
                <button
                  v-else
                  class="btn-outline"
                  @click="reviewOrder(orderWrapper.orderItems[0]!.id)"
                >评价</button>
                <button
                  v-if="orderWrapper.order.trackingNumber"
                  class="btn-outline"
                  @click="viewLogistics(orderWrapper.order.id)"
                >查看物流</button>
                <button class="btn-outline" @click="viewOrderDetail(orderWrapper.order.id)">查看详情</button>
                <button class="btn-more" @click="toggleMoreActions(orderWrapper.order.id)">
                  <i class="fas fa-ellipsis-h"></i>
                </button>
                <div class="more-actions-dropdown" v-if="showMoreActions[orderWrapper.order.id]">
                  <button class="dropdown-item" @click="handleAfterSaleAction(orderWrapper)">
                    {{ orderWrapper.orderItems[0]!.refundStatus && orderWrapper.orderItems[0]!.refundStatus !== 'COMPLETED'
                      ? getRefundStatusText(orderWrapper.orderItems[0]!.refundStatus)
                      : (canAfterSale(orderWrapper.order) ? '申请售后' : '已过权益期') }}
                  </button>
                </div>
              </template>
              <template v-else>
                <button
                  v-if="orderWrapper.order.trackingNumber"
                  class="btn-outline"
                  @click="viewLogistics(orderWrapper.order.id)"
                >查看物流</button>
                <button class="btn-outline" @click="viewOrderDetail(orderWrapper.order.id)">查看详情</button>
              </template>
            </template>

            <!-- 已取消状态 -->
            <template v-else-if="orderWrapper.order.status === 'CANCELLED'">
              <button class="btn-outline btn-danger" @click="deleteOrder(orderWrapper.order.id)">删除订单</button>
            </template>
          </div>
        </div>
      </div>

      <!-- 加载状态区域 -->
      <div class="load-more-wrapper">
        <div v-if="loadingMore" class="loading-more-bar">
          <i class="fas fa-spinner fa-spin"></i>
          <span>加载中...</span>
        </div>
        <div v-else-if="!hasMore && orders.length > 0" class="no-more-bar">
          — 已经到底了 —
        </div>
      </div>
    </div>

    <!-- 支付确认弹窗 -->
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
import { ref, onMounted, onActivated, onDeactivated, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { authAPI } from '@/api/authAPI'
import Message from '@/utils/message'
import { useAuthStore } from '@/stores/auth'

// 事件定义
const emit = defineEmits(['update-tab-counts'])

const router = useRouter()
const authStore = useAuthStore()

// 滚动位置保存
let savedScrollTop = 0

// ==================== 类型定义 ====================

interface OrderItem {
  id: number
  orderId: number
  productId: number
  sellerId: number
  productName: string
  productImage: string
  skuId?: number
  skuName?: string
  quantity: number
  price: number
  totalPrice: number
  isReviewed: boolean
  reviewedAt: string
  createdAt: string
  refundStatus?: string
  refundId?: number
}

type OrderStatus = 'PENDING' | 'PAID' | 'PROCESSING' | 'SHIPPED' | 'DELIVERED' | 'COMPLETED' | 'CANCELLED' | 'REFUNDED'

type PaymentMethod = 'ALIPAY' | 'WECHAT'

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

interface OrderWithItems {
  order: Order
  orderItems: OrderItem[]
}

// ==================== 常量定义 ====================

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

const orders = ref<OrderWithItems[]>([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(10)
const hasMore = ref(true)
const loadingMore = ref(false)
const showPayConfirm = ref(false)
const payingOrderId = ref<number | null>(null)
const checkingPay = ref(false)
const showMoreActions = ref<Record<number, boolean>>({})

// ==================== 数据加载 ====================

const loadOrders = async (): Promise<void> => {
  if (loading.value) return

  loading.value = true

  try {
    const params: { page: number; pageSize: number } = {
      page: page.value,
      pageSize: pageSize.value
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

      // 发送标签页数量更新事件
      const counts = {
        all: data.counts?.total || 0,
        PENDING: data.counts?.PENDING || 0,
        PAID: data.counts?.PAID || 0,
        SHIPPED: data.counts?.SHIPPED || 0,
        COMPLETED: data.counts?.COMPLETED || 0
      }
      emit('update-tab-counts', counts)
    }
  } catch (error: any) {
    Message.error(error.message || '加载失败')
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

const toggleMoreActions = (orderId: number) => {
  showMoreActions.value[orderId] = !showMoreActions.value[orderId]
}

// ==================== 订单操作 ====================

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

const goToPay = async (orderId: number): Promise<void> => {
  loading.value = true

  try {
    const response = await authAPI.payOrder(orderId)

    if (response.success) {
      const paymentHtml = response.data?.paymentHtml

      const payWindow = window.open('', '_blank')
      if (payWindow) {
        payWindow.document.write(paymentHtml)
        payWindow.document.close()
      }

      payingOrderId.value = orderId
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

const checkPayStatus = async () => {
  if (!payingOrderId.value || checkingPay.value) return

  checkingPay.value = true

  try {
    const response = await authAPI.getOrderDetail(payingOrderId.value)

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

const handlePayLater = () => {
  showPayConfirm.value = false
}

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

const viewLogistics = (orderId: number): void => {
  router.push({
    name: 'UserLogistics',
    query: { orderId: String(orderId) }
  })
}

const reviewOrder = (orderItemId: number): void => {
  router.push({
    name: 'Review',
    params: { orderItemId }
  })
}

const viewOrderDetail = (orderId: number): void => {
  router.push({
    name: 'OrderDetail',
    params: { orderId }
  })
}

const viewProduct = (productId: number): void => {
  router.push({
    name: 'ProductDetail',
    params: { productId }
  })
}

const goShopping = (): void => {
  router.push({ name: 'UserDashboard' })
}

// ==================== 工具函数 ====================

const canRefund = (order: Order): boolean => {
  if (!order.paidAt) return false
  const paidTime = new Date(order.paidAt).getTime()
  const now = Date.now()
  const days = (now - paidTime) / (1000 * 60 * 60 * 24)
  return days <= 7
}

const canAfterSale = (order: Order): boolean => {
  if (order.status === 'COMPLETED') {
    if (!order.completedAt) return false
    const completedTime = new Date(order.completedAt).getTime()
    const now = Date.now()
    const days = (now - completedTime) / (1000 * 60 * 60 * 24)
    return days <= 7
  }
  const baseTime = order.shippedAt
  if (!baseTime) return false
  const baseTimestamp = new Date(baseTime).getTime()
  const now = Date.now()
  const days = (now - baseTimestamp) / (1000 * 60 * 60 * 24)
  return days <= 15
}

const goToRefund = async (orderWrapper: OrderWithItems): Promise<void> => {
  const orderItem = orderWrapper.orderItems[0]
  if (!orderItem) return

  if (orderItem.refundStatus) {
    try {
      const response = await authAPI.getRefundByOrderItemId(orderItem.id)
      if (response.success && response.data) {
        router.push({
          name: 'RefundChat',
          params: { refundId: response.data.id }
        })
      } else {
        router.push({
          name: 'Refund',
          query: {
            orderItemId: orderItem.id,
            orderId: orderWrapper.order.id,
            type: 'REFUND'
          }
        })
      }
    } catch {
      router.push({
        name: 'Refund',
        query: {
          orderItemId: orderItem.id,
          orderId: orderWrapper.order.id,
          type: 'REFUND'
        }
      })
    }
    return
  }

  router.push({
    name: 'Refund',
    query: {
      orderItemId: orderItem.id,
      orderId: orderWrapper.order.id,
      type: 'REFUND'
    }
  })
}

const goToAfterSale = async (orderWrapper: OrderWithItems): Promise<void> => {
  const orderItem = orderWrapper.orderItems[0]
  if (!orderItem) return

  if (orderItem.refundStatus) {
    try {
      const response = await authAPI.getRefundByOrderItemId(orderItem.id)
      if (response.success && response.data) {
        router.push({
          name: 'RefundChat',
          params: { refundId: response.data.id }
        })
      } else {
        router.push({
          name: 'Refund',
          query: {
            orderItemId: orderItem.id,
            orderId: orderWrapper.order.id,
            type: 'AFTER_SALE'
          }
        })
      }
    } catch {
      router.push({
        name: 'Refund',
        query: {
          orderItemId: orderItem.id,
          orderId: orderWrapper.order.id,
          type: 'AFTER_SALE'
        }
      })
    }
    return
  }

  router.push({
    name: 'Refund',
    query: {
      orderItemId: orderItem.id,
      orderId: orderWrapper.order.id,
      type: 'AFTER_SALE'
    }
  })
}

const viewReturnLogistics = (orderItem: OrderItem): void => {
  if (!orderItem.refundId) return
  router.push({
    name: 'UserLogistics',
    query: {
      refundId: String(orderItem.refundId),
      type: 'return'
    }
  })
}

const handleAfterSaleAction = (orderWrapper: OrderWithItems): void => {
  const orderItem = orderWrapper.orderItems[0]
  if (!orderItem) return

  if (orderItem.refundStatus === 'RETURNING') {
    viewReturnLogistics(orderItem)
  } else {
    goToAfterSale(orderWrapper)
  }
  toggleMoreActions(orderWrapper.order.id)
}

const getStatusClass = (status: OrderStatus): string => {
  return statusClassMap[status] || ''
}

const getStatusText = (status: OrderStatus): string => {
  return statusTextMap[status] || status
}

const getOrderRefundStatus = (orderWrapper: OrderWithItems): string | null => {
  const orderItems = orderWrapper.orderItems || []
  if (orderItems.length === 0) return null

  const refundingCount = orderItems.filter(item =>
    item.refundStatus === 'REFUNDING' || item.refundStatus === 'AFTER_SALE' ||
    item.refundStatus === 'WAITING_RETURN' || item.refundStatus === 'RETURNING' ||
    item.refundStatus === 'APPROVED'
  ).length
  const refundedCount = orderItems.filter(item => item.refundStatus === 'COMPLETED').length

  if (refundingCount === orderItems.length) return '退款中'
  if (refundedCount === orderItems.length) return '已退款'
  if (refundingCount > 0 || refundedCount > 0) return '部分退款'
  return null
}

const getOrderRefundStatusClass = (orderWrapper: OrderWithItems): string => {
  const orderItems = orderWrapper.orderItems || []
  if (orderItems.length === 0) return ''

  const refundingCount = orderItems.filter(item =>
    item.refundStatus === 'REFUNDING' || item.refundStatus === 'AFTER_SALE' ||
    item.refundStatus === 'WAITING_RETURN' || item.refundStatus === 'RETURNING' ||
    item.refundStatus === 'APPROVED'
  ).length
  const refundedCount = orderItems.filter(item => item.refundStatus === 'COMPLETED').length

  if (refundingCount === orderItems.length) return 'status-refunding'
  if (refundedCount === orderItems.length) return 'status-refunded'
  return 'status-partial-refund'
}

const getRefundStatusText = (status: string, refundType?: string, returnStatus?: string): string => {
  if (refundType === 'AFTER_SALE' && status === 'PROCESSING') {
    return '售后处理中'
  }
  if (refundType === 'AFTER_SALE' && status === 'APPROVED') {
    return '请退货'
  }
  if (returnStatus === 'RETURNING') {
    return '退货中'
  }
  if (returnStatus === 'RECEIVED') {
    return '已收货，退款中'
  }

  const statusMap: Record<string, string> = {
    'REFUNDING': '退款中',
    'AFTER_SALE': '售后处理中',
    'WAITING_RETURN': '待退货',
    'RETURNING': '退货中',
    'RECEIVED': '已完成',
    'APPROVED': '已同意',
    'COMPLETED': '已完成',
    'FAILED': '已拒绝',
    'SUCCESS': '已完成'
  }
  return statusMap[status] || status
}

const formatPrice = (price: number): string => {
  if (price == null || isNaN(price)) {
    return '0.00'
  }
  return price.toFixed(2)
}

// ==================== 滚动加载 ====================

const handleScroll = () => {
  if (loadingMore.value || !hasMore.value) return

  const { scrollTop, scrollHeight, clientHeight } = document.documentElement
  if (scrollTop + clientHeight >= scrollHeight - 100) {
    loadMore()
  }
}

const loadMore = async () => {
  if (loadingMore.value || !hasMore.value) return
  loadingMore.value = true
  page.value++
  await loadOrders()
}

// ==================== 生命周期 ====================

// 缓存激活时恢复滚动位置
onActivated(() => {
  if (savedScrollTop > 0) {
    setTimeout(() => {
      window.scrollTo({ top: savedScrollTop, behavior: 'instant' })
    }, 50)
  }
})

// 缓存失活时保存滚动位置
onDeactivated(() => {
  savedScrollTop = window.scrollY
})

onMounted(() => {
  if (!authStore.validateUserPermission()) return
  loadOrders()
  window.addEventListener('scroll', handleScroll)
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})
</script>

<style scoped>
@import url('@/static/css/user/订单列表.css');
</style>
