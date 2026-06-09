import { defineStore } from 'pinia'
import { ref } from 'vue'

export interface OrderItem {
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
  refundStatus?: 'PROCESSING' | 'WAITING_RETURN' | 'RETURNING' | 'SUCCESS' | 'FAILED'
}

export type OrderStatus = 'PENDING' | 'PAID' | 'PROCESSING' | 'SHIPPED' | 'COMPLETED' | 'CANCELLED'

export interface Order {
  id: number
  orderNumber: string
  userId: number
  totalAmount: number
  status: OrderStatus
  source: string
  addressId: number
  paymentMethod?: string
  transactionId?: string
  paidAt?: string
  trackingNumber?: string
  logisticsCode?: string
  logisticsName?: string
  shippedAt?: string
  completedAt?: string
  createdAt: string
  updatedAt: string
  isDeleted: boolean
  orderItems: OrderItem[]
}

export interface OrderWithItems {
  order: Order
  orderItems: OrderItem[]
}

export interface PageInfo {
  page: number
  hasMore: boolean
}

export interface OrderCounts {
  all: number
  PENDING: number
  PAID: number
  SHIPPED: number
  COMPLETED: number
}

export const useOrdersStore = defineStore('orders', () => {
  const scrollPositions = ref<Record<string, number>>({})
  const ordersCache = ref<Record<string, OrderWithItems[]>>({})
  const pageCache = ref<Record<string, PageInfo>>({})
  const lastFetchTime = ref<Record<string, number>>({})

  const orderCounts = ref<OrderCounts>({
    all: 0,
    PENDING: 0,
    PAID: 0,
    SHIPPED: 0,
    COMPLETED: 0
  })

  const saveScrollPosition = (tabKey: string, top: number) => {
    console.log('[OrdersStore] 保存滚动位置 - tabKey:', tabKey, 'top:', top)
    scrollPositions.value[tabKey] = top
  }

  const getScrollPosition = (tabKey: string): number => {
    const position = scrollPositions.value[tabKey] || 0
    console.log('[OrdersStore] 获取滚动位置 - tabKey:', tabKey, 'position:', position)
    return position
  }

  const saveOrders = (tabKey: string, orders: OrderWithItems[], page: number, hasMore: boolean) => {
    console.log('[OrdersStore] 保存订单数据 - tabKey:', tabKey, '订单数量:', orders.length, 'page:', page)
    ordersCache.value[tabKey] = orders
    pageCache.value[tabKey] = { page, hasMore }
    lastFetchTime.value[tabKey] = Date.now()
  }

  const getOrders = (tabKey: string): OrderWithItems[] => {
    const orders = ordersCache.value[tabKey] || []
    console.log('[OrdersStore] 获取订单数据 - tabKey:', tabKey, '订单数量:', orders.length)
    return orders
  }

  const getPageInfo = (tabKey: string): PageInfo => {
    const pageInfo = pageCache.value[tabKey] || { page: 1, hasMore: true }
    console.log('[OrdersStore] 获取分页信息 - tabKey:', tabKey, 'pageInfo:', pageInfo)
    return pageInfo
  }

  const hasCachedOrders = (tabKey: string): boolean => {
    const cache = ordersCache.value[tabKey]
    const hasCache = cache !== undefined && cache.length > 0
    console.log('[OrdersStore] 检查是否有缓存 - tabKey:', tabKey, 'hasCache:', hasCache)
    return hasCache
  }

  const isDataFresh = (tabKey: string, maxAgeMinutes: number = 5): boolean => {
    const lastTime = lastFetchTime.value[tabKey]
    if (!lastTime) return false
    const ageMinutes = (Date.now() - lastTime) / (1000 * 60)
    const isFresh = ageMinutes <= maxAgeMinutes
    console.log('[OrdersStore] 检查数据新鲜度 - tabKey:', tabKey, 'ageMinutes:', ageMinutes.toFixed(2), 'isFresh:', isFresh)
    return isFresh
  }

  const clearCache = (tabKey: string) => {
    console.log('[OrdersStore] 清空缓存 - tabKey:', tabKey)
    delete scrollPositions.value[tabKey]
    delete ordersCache.value[tabKey]
    delete pageCache.value[tabKey]
    delete lastFetchTime.value[tabKey]
  }

  const clearAllCache = () => {
    scrollPositions.value = {}
    ordersCache.value = {}
    pageCache.value = {}
    lastFetchTime.value = {}
  }

  const updateOrderCounts = (counts: Partial<OrderCounts>) => {
    orderCounts.value = { ...orderCounts.value, ...counts }
  }

  const resetPage = (tabKey: string) => {
    if (pageCache.value[tabKey]) {
      pageCache.value[tabKey] = { page: 1, hasMore: true }
    }
  }

  return {
    scrollPositions,
    ordersCache,
    pageCache,
    lastFetchTime,
    orderCounts,
    saveScrollPosition,
    getScrollPosition,
    saveOrders,
    getOrders,
    getPageInfo,
    hasCachedOrders,
    isDataFresh,
    clearCache,
    clearAllCache,
    updateOrderCounts,
    resetPage
  }
})
