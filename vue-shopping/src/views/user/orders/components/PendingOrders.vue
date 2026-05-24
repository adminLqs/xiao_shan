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
              <span class="order-status status-pending">待付款</span>
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
            <button class="btn-pay" @click="goToPay(orderWrapper.order.id)">去支付</button>
            <button class="btn-outline btn-danger" @click="cancelOrder(orderWrapper.order.id)">取消订单</button>
            <button class="btn-outline" @click="viewOrderDetail(orderWrapper.order.id)">查看详情</button>
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

defineOptions({ name: 'PendingOrders' })

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
}

interface Order {
  id: number
  orderNumber: string
  totalAmount: number
  paidAt?: string
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
const showPayConfirm = ref(false)
const payingOrderId = ref<number | null>(null)
const checkingPay = ref(false)

const loadOrders = async (): Promise<void> => {
  if (loading.value) return
  loading.value = true
  try {
    const params = {
      page: page.value,
      pageSize: pageSize.value,
      status: 'PENDING'
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
    } else {
      Message.error('查询订单失败')
    }
  } catch (error: any) {
    Message.error(error.message || '查询失败')
  } finally {
    checkingPay.value = false
  }
}

const handlePayLater = () => {
  showPayConfirm.value = false
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

const formatPrice = (price: number) => {
  return (price || 0).toFixed(2)
}

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
