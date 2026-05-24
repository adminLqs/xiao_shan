<template>
  <div class="order-list">
    <div v-if="loading && orders.length === 0">
      <div v-for="i in 3" :key="i" class="skeleton-card">
        <div class="skeleton-line short"></div>
        <div class="skeleton-line medium" style="margin-top: 12px;"></div>
        <div class="skeleton-line" style="margin-top: 8px;"></div>
        <div class="skeleton-line short" style="margin-top: 12px;"></div>
      </div>
    </div>
    <div v-else-if="orders.length === 0" class="empty-cart">
      <i class="fas fa-shopping-bag"></i>
      <p>暂无订单</p>
      <button class="btn-primary" @click="goShopping">去逛逛</button>
    </div>
    <div v-else>
      <div v-for="orderWrapper in orders" :key="orderWrapper.order.id" class="order-card">
        <div class="order-header">
          <div class="order-header-top">
            <span class="order-number">订单号：{{ orderWrapper.order.orderNumber }}</span>
            <div class="order-status-group">
              <span v-if="!getOrderRefundStatus(orderWrapper)" class="order-status status-completed">已完成</span>
              <span v-else class="order-status refund-status" :class="getOrderRefundStatusClass(orderWrapper)">{{ getOrderRefundStatus(orderWrapper) }}</span>
            </div>
          </div>
        </div>
        <div class="order-items">
          <div v-for="item in orderWrapper.orderItems" :key="item.id" class="order-item" @click="viewProduct(item.productId)">
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
        <div class="order-footer">
          <div class="order-actions">
            <template v-if="orderWrapper.orderItems.length === 1">
              <button v-if="orderWrapper.orderItems[0]!.isReviewed" class="btn-outline" disabled>已评价</button>
              <button v-else class="btn-outline" @click="reviewOrder(orderWrapper.orderItems[0]!.id)">评价</button>
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
                <button class="dropdown-item" @click="goToAfterSale(orderWrapper); toggleMoreActions(orderWrapper.order.id)">
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
          </div>
        </div>
      </div>
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
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onActivated, onDeactivated, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { authAPI } from '@/api/authAPI'
import Message from '@/utils/message'
import { useAuthStore } from '@/stores/auth'

const emit = defineEmits(['update-tab-counts'])

const router = useRouter()
const authStore = useAuthStore()

let savedScrollTop = 0

interface OrderItem {
  id: number
  orderId: number
  productId: number
  productName: string
  productImage: string
  skuName?: string
  quantity: number
  price: number
  isReviewed: boolean
  refundStatus?: string
}

interface Order {
  id: number
  orderNumber: string
  totalAmount: number
  completedAt?: string
  trackingNumber?: string
  status: string
  orderItems: OrderItem[]
}

interface OrderWithItems {
  order: Order
  orderItems: OrderItem[]
}

const orders = ref<OrderWithItems[]>([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(10)
const hasMore = ref(true)
const loadingMore = ref(false)
const showMoreActions = ref<Record<number, boolean>>({})

const loadOrders = async (): Promise<void> => {
  if (loading.value) return
  loading.value = true
  try {
    const params = {
      page: page.value,
      pageSize: pageSize.value,
      status: 'COMPLETED'
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
      emit('update-tab-counts', {
        all: data.counts?.total || 0,
        PENDING: data.counts?.PENDING || 0,
        PAID: data.counts?.PAID || 0,
        SHIPPED: data.counts?.SHIPPED || 0,
        COMPLETED: data.counts?.COMPLETED || 0
      })
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

const canAfterSale = (order: Order): boolean => {
  if (!order.completedAt) return false
  const completedTime = new Date(order.completedAt).getTime()
  const now = Date.now()
  const days = (now - completedTime) / (1000 * 60 * 60 * 24)
  return days <= 7
}

const goToAfterSale = async (orderWrapper: OrderWithItems) => {
  const orderItem = orderWrapper.orderItems[0]
  if (!orderItem) return
  if (orderItem.refundStatus) {
    try {
      const response = await authAPI.getRefundByOrderItemId(orderItem.id)
      if (response.success && response.data) {
        router.push({ name: 'RefundChat', params: { refundId: response.data.id } })
      } else {
        router.push({ name: 'Refund', query: { orderItemId: orderItem.id, orderId: orderWrapper.order.id, type: 'AFTER_SALE' } })
      }
    } catch {
      router.push({ name: 'Refund', query: { orderItemId: orderItem.id, orderId: orderWrapper.order.id, type: 'AFTER_SALE' } })
    }
  } else {
    router.push({ name: 'Refund', query: { orderItemId: orderItem.id, orderId: orderWrapper.order.id, type: 'AFTER_SALE' } })
  }
}

const reviewOrder = (orderItemId: number) => {
  router.push({ name: 'Review', params: { orderItemId } })
}

const viewLogistics = (orderId: number) => {
  router.push({ name: 'UserLogistics', query: { orderId: String(orderId) } })
}

const viewOrderDetail = (orderId: number) => {
  router.push({ name: 'OrderDetail', params: { orderId } })
}

const viewProduct = (productId: number) => {
  router.push({ name: 'ProductDetail', params: { productId } })
}

const goShopping = () => {
  router.push({ name: 'UserDashboard' })
}

const getOrderRefundStatus = (orderWrapper: OrderWithItems) => {
  const items = orderWrapper.orderItems || []
  const refunding = items.filter(item =>
    item.refundStatus === 'REFUNDING' || item.refundStatus === 'AFTER_SALE' || item.refundStatus === 'WAITING_RETURN' || item.refundStatus === 'RETURNING' || item.refundStatus === 'APPROVED'
  ).length
  const refunded = items.filter(item => item.refundStatus === 'COMPLETED').length
  if (refunding === items.length) return '退款中'
  if (refunded === items.length) return '已退款'
  if (refunding > 0 || refunded > 0) return '部分退款'
  return null
}

const getOrderRefundStatusClass = (orderWrapper: OrderWithItems) => {
  const items = orderWrapper.orderItems || []
  const refunding = items.filter(item =>
    item.refundStatus === 'REFUNDING' || item.refundStatus === 'AFTER_SALE' || item.refundStatus === 'WAITING_RETURN' || item.refundStatus === 'RETURNING' || item.refundStatus === 'APPROVED'
  ).length
  const refunded = items.filter(item => item.refundStatus === 'COMPLETED').length
  if (refunding === items.length) return 'status-refunding'
  if (refunded === items.length) return 'status-refunded'
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

  const map: Record<string, string> = {
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
  return map[status] || status
}

const formatPrice = (price: number) => (price || 0).toFixed(2)

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

onActivated(() => {
  if (savedScrollTop > 0) {
    setTimeout(() => {
      window.scrollTo({ top: savedScrollTop, behavior: 'instant' })
    }, 50)
  }
})

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
