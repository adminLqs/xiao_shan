<template>
  <div class="user-logistics-container page-container">
    <div class="page-navbar">
      <button class="page-nav-back" @click="router.back()">
        <i class="fas fa-chevron-left"></i>
      </button>
      <div class="page-nav-title">
        <span>物流信息</span>
      </div>
      <div class="page-nav-right"></div>
    </div>

    <!-- ========== 加载状态 - 骨架屏 ========== -->
    <div v-if="loading" class="logistics-content">
      <!-- 订单号骨架 -->
      <div class="skeleton-card">
        <div class="skeleton-line"></div>
      </div>

      <!-- 物流公司卡片骨架 -->
      <div class="skeleton-card">
        <div class="skeleton-company">
          <div class="skeleton-avatar"></div>
          <div class="skeleton-company-info">
            <div class="skeleton-line"></div>
            <div class="skeleton-line short"></div>
          </div>
          <div class="skeleton-copy-btn"></div>
        </div>
      </div>

      <!-- 物流轨迹卡片骨架 -->
      <div class="skeleton-card">
        <div v-for="i in 3" :key="i" class="skeleton-timeline-item">
          <div class="skeleton-timeline-dot"></div>
          <div class="skeleton-timeline-content">
            <div class="skeleton-line"></div>
            <div class="skeleton-line short"></div>
          </div>
        </div>
      </div>

      <!-- 地址卡片骨架 -->
      <div class="skeleton-card">
        <div class="skeleton-line short"></div>
        <div class="skeleton-line"></div>
        <div class="skeleton-line"></div>
      </div>
    </div>

    <!-- ========== 物流信息内容 ========== -->
    <div v-else-if="orderInfo || (logisticsInfo.trackingNumber)" class="logistics-content">

      <!-- ========== 商品信息卡片 ========== -->
      <div class="product-list-wrapper">
        <div class="product-info-card" v-for="(item, idx) in productItems" :key="idx"
          :style="idx > 0 ? 'border-top:1px solid #f0f2f5;' : ''">
          <img :src="item.productImage" class="product-image" />
          <div class="product-detail">
            <div class="product-name">{{ item.productName }}</div>
            <div class="tags-group">
              <span v-if="item.skuName" class="tag-spec">{{ item.skuName }}</span>
              <span class="tag-quantity">x{{ item.quantity }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- ========== 物流公司卡片（紧凑型） ========== -->
      <div v-if="logisticsInfo.trackingNumber || orderInfo.trackingNumber" class="logistics-company-card">
        <div class="company-logo">
          <i class="fas fa-truck"></i>
        </div>
        <div class="company-info">
          <div class="company-name">{{ logisticsInfo.logisticsName || '-' }}</div>
          <div class="tracking-number">{{ logisticsInfo.trackingNumber || orderInfo.trackingNumber || '-' }}</div>
        </div>
        <button class="copy-btn" @click="copyTrackingNumber">
          <i class="fas fa-copy"></i>
        </button>
      </div>

      <!-- ========== 收件信息卡片 ========== -->
      <div class="address-card" v-if="orderInfo && orderInfo.receiverName">
        <div class="address-icon">
          <i class="fas fa-map-marker-alt"></i>
        </div>
        <div class="address-content">
          <div class="address-detail">{{ orderInfo.receiverAddress || '-' }}</div>
          <div class="phone-row">
            <span>{{ orderInfo.receiverName || '-' }}</span>
            <span class="phone">{{ orderInfo.receiverPhone || '-' }}</span>
          </div>
        </div>
      </div>

      <!-- ========== 物流轨迹时间线（核心区域） ========== -->
      <div v-if="orderInfo" class="logistics-card">
        <div class="logistics-timeline">
          <div
            v-for="(trace, index) in logisticsTraces"
            :key="index"
            class="timeline-item"
            :class="{ 'is-first': index === 0 }"
          >
            <div class="timeline-line">
              <div class="timeline-dot"></div>
            </div>
            <div class="timeline-content">
              <div class="timeline-status">{{ trace.status || trace.description }}</div>
              <div class="timeline-time">{{ formatDateTime(trace.time) }}</div>
              <div class="timeline-location" v-if="trace.location">
                <i class="fas fa-map-marker-alt"></i>
                {{ trace.location }}
              </div>
            </div>
          </div>

          <div v-if="logisticsTraces.length === 0" class="no-trace">
            <i class="fas fa-box-open"></i>
            <p>暂无物流信息</p>
            <p class="tips">请等待快递员揽件</p>
          </div>
        </div>
      </div>



      <!-- ========== 底部操作按钮 ========== -->
      <div class="order-footer">
        <div class="order-actions">
          <button class="btn-contact" @click="contactSeller">
            <i class="fas fa-headset"></i>
            <span>联系客服</span>
          </button>
        </div>
      </div>
    </div>

    <!-- ========== 订单不存在状态 ========== -->
    <div v-else class="empty-state">
      <i class="fas fa-box-open"></i>
      <p>订单不存在</p>
      <p class="tips">请从订单列表或售后列表进入</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { authAPI } from '@/api/authAPI'
import Message from '@/utils/message'
import { useAuthStore } from '@/stores/auth'
import { storeToRefs } from 'pinia'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const { isLoggedIn } = storeToRefs(authStore)

const loading = ref(true)
const confirming = ref(false)
const sellerId = ref<number | null>(null)

interface OrderInfo {
  id: number | null
  orderNumber: string
  status: string
  totalAmount: number
  shippedAt: string | null
  trackingNumber: string | null
  logisticsCode: string
  logisticsName: string
  receiverName: string
  receiverPhone: string
  receiverAddress: string
}

interface LogisticsInfo {
  trackingNumber: string
  logisticsCode: string
  logisticsName: string
  traces: any[]
}

interface LogisticsTrace {
  time: string
  description: string
  status: string
  location: string
}

interface ProductInfo {
  productName: string
  productImage: string
  skuName?: string
  quantity: number
}

const productItems = ref<ProductInfo[]>([])

const orderInfo = ref<OrderInfo>({
  id: null,
  orderNumber: '',
  status: '',
  totalAmount: 0,
  shippedAt: null,
  trackingNumber: null,
  logisticsCode: '',
  logisticsName: '',
  receiverName: '',
  receiverPhone: '',
  receiverAddress: ''
})

const logisticsInfo = ref<LogisticsInfo>({
  trackingNumber: '',
  logisticsCode: '',
  logisticsName: '',
  traces: []
})

const logisticsTraces = ref<LogisticsTrace[]>([])

const loadLogistics = async () => {
  loading.value = true

  try {
    const orderId = route.query.orderId
    const trackingNumber = route.query.trackingNumber as string
    const logisticsName = route.query.logisticsName as string
    const refundId = route.query.refundId as string

    // 如果直接传入了物流单号，显示退货物流信息
    if (trackingNumber) {
      orderInfo.value = {
        id: null,
        orderNumber: '',
        status: 'SHIPPED',
        totalAmount: 0,
        shippedAt: null,
        trackingNumber: trackingNumber,
        logisticsCode: '',
        logisticsName: logisticsName || '',
        receiverName: '',
        receiverPhone: '',
        receiverAddress: ''
      }

      logisticsInfo.value = {
        trackingNumber: trackingNumber,
        logisticsCode: '',
        logisticsName: logisticsName || '',
        traces: []
      }

      const response = await authAPI.getLogisticsByTrackingNumber(trackingNumber, logisticsName, refundId)
      if (response.success && response.data) {
        logisticsTraces.value = response.data.traces || []
      } else {
        logisticsTraces.value = []
      }

      // ===== 退货物流：通过 refundId 获取商品信息 =====
      if (route.query.refundId) {
        const refundRes = await authAPI.getRefundDetail(Number(route.query.refundId))
        if (refundRes.success && refundRes.data) {
          sellerId.value = refundRes.data.sellerId || null
          productItems.value = [{
            productName: refundRes.data.productName || '',
            productImage: refundRes.data.productImage || '',
            skuName: refundRes.data.skuName || '',
            quantity: refundRes.data.quantity || 1
          }]
        }
      }

      loading.value = false
      return
    }

    // ===== 退货物流：有 refundId 但没有 trackingNumber（上门取件场景） =====
    if (refundId && route.query.type === 'return') {
      const refundRes = await authAPI.getRefundDetail(Number(refundId))
      if (refundRes.success && refundRes.data) {
        const refundData = refundRes.data
        sellerId.value = refundData.sellerId || null

        orderInfo.value = {
          id: refundData.orderId || null,
          orderNumber: refundData.orderNumber || '',
          status: 'SHIPPED',
          totalAmount: refundData.amount || 0,
          shippedAt: null,
          trackingNumber: refundData.returnTrackingNumber || '',
          logisticsCode: '',
          logisticsName: refundData.returnLogisticsName || '',
          receiverName: '',
          receiverPhone: '',
          receiverAddress: ''
        }

        logisticsInfo.value = {
          trackingNumber: refundData.returnTrackingNumber || '',
          logisticsCode: '',
          logisticsName: refundData.returnLogisticsName || '',
          traces: []
        }

        productItems.value = [{
          productName: refundData.productName || '',
          productImage: refundData.productImage || '',
          skuName: refundData.skuName || '',
          quantity: refundData.quantity || 1
        }]

        // 如果已有运单号，获取物流轨迹
        if (refundData.returnTrackingNumber) {
          const response = await authAPI.getLogisticsByTrackingNumber(refundData.returnTrackingNumber, refundData.returnLogisticsName, refundId)
          if (response.success && response.data) {
            logisticsTraces.value = response.data.traces || []
          }
        }
      }

      loading.value = false
      return
    }

    if (!orderId) {
      Message.error('订单ID不存在')
      router.push({ name: 'UserOrders' })
      return
    }

    const response = await authAPI.getUserLogisticsInfo(Number(orderId))

    if (response.success && response.data) {
      // 支持两种返回结构
      const resultData = response.data.result || response.data
      const order = resultData.order

      if (order) {
        // 兼容 recipientName 和 receiverName 字段
        const receiverName = order.recipientName || order.receiverName || ''
        const receiverPhone = order.recipientPhone || order.receiverPhone || ''
        const receiverAddress = order.recipientAddress || order.receiverAddress || ''

        orderInfo.value = {
          id: order.id,
          orderNumber: order.orderNumber,
          status: order.status,
          totalAmount: order.totalAmount,
          shippedAt: order.shippedAt,
          trackingNumber: order.trackingNumber,
          logisticsCode: order.logisticsCode || '',
          logisticsName: order.logisticsName || '',
          receiverName: receiverName,
          receiverPhone: receiverPhone,
          receiverAddress: receiverAddress
        }
      }

      const logistics = resultData.logistics
      const traces = logistics?.traces || []

      logisticsInfo.value = {
        trackingNumber: logistics?.trackingNumber || order?.trackingNumber || '',
        logisticsCode: logistics?.logisticsCode || order?.logisticsCode || '',
        logisticsName: logistics?.logisticsName || order?.logisticsName || '',
        traces: traces
      }

      // 后端已合并默认轨迹和真实轨迹，直接使用
      logisticsTraces.value = traces

      // ===== 订单物流：通过 orderId 获取订单项 =====
      const orderRes = await authAPI.getOrderDetail(Number(orderId))
      if (orderRes.success) {
        const items = orderRes.data?.orderDetail?.orderItems || orderRes.data?.orderItems || []
        productItems.value = items.map((item: any) => ({
          productName: item.productName || '',
          productImage: item.productImage || '',
          skuName: item.skuName || '',
          quantity: item.quantity || 1
        }))
      }
    } else {
      Message.error(response.message || '获取物流信息失败')
    }
  } catch (error: any) {
    Message.error(error.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const confirmReceive = async () => {
  try {
    await Message.confirm('请确认已收到商品，确认后将完成订单', '确认收货')

    confirming.value = true

    const loading = Message.loading({ text: '确认中...' })

    const orderId = orderInfo.value?.id
    if (!orderId) {
      throw new Error('订单信息不存在')
    }
    const response = await authAPI.confirmReceive(orderId)

    loading.close()

    if (response.success) {
      Message.success('确认收货成功')
      orderInfo.value.status = 'COMPLETED'

      setTimeout(() => {
        router.push({ name: 'UserOrders' })
      }, 1500)
    } else {
      Message.error(response.message || '确认收货失败')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      Message.error(error.message || '确认收货失败')
    }
  } finally {
    confirming.value = false
  }
}

const copyTrackingNumber = async () => {
  const trackingNumber = logisticsInfo.value.trackingNumber
  if (!trackingNumber) return

  try {
    await navigator.clipboard.writeText(trackingNumber)
    Message.success('已复制')
  } catch (error) {
    Message.error('复制失败')
  }
}

const copyOrderNumber = async () => {
  const orderNumber = orderInfo.value.orderNumber
  if (!orderNumber) return

  try {
    await navigator.clipboard.writeText(orderNumber)
    Message.success('已复制')
  } catch (error) {
    Message.error('复制失败')
  }
}

const contactSeller = () => {
  const targetSellerId = route.query.sellerId || sellerId.value
  if (!targetSellerId) {
    Message.error('无法获取商家信息')
    return
  }
  router.push({ name: 'Chat', params: { targetId: Number(targetSellerId) } })
}

const formatPrice = (price: number): string => {
  if (price == null) return '0.00'
  return price.toFixed(2)
}

const formatDateTime = (dateStr: string | null): string => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  const h = String(date.getHours()).padStart(2, '0')
  const min = String(date.getMinutes()).padStart(2, '0')
  return `${y}-${m}-${d} ${h}:${min}`
}

onMounted(() => {
  if (!authStore.validateUserPermission()) return
  loadLogistics()
})
</script>

<style scoped>
@import url('@/static/css/user/物流页.css');
</style>
