<template>
  <div class="logistics-container">
    <!-- ========== 页面头部 ========== -->
    <div class="page-navbar">
      <button class="page-nav-back" @click="goBack">
        <i class="fas fa-chevron-left"></i>
      </button>
      <div class="page-nav-title">
        <span>物流详情</span>
      </div>
      <div class="page-nav-right"></div>
    </div>

    <!-- ========== 加载状态 ========== -->
    <div v-if="loading" class="logistics-content">
      <div class="skeleton-card">
        <div class="skeleton-line"></div>
      </div>
      <div class="skeleton-card">
        <div class="skeleton-company">
          <div class="skeleton-avatar"></div>
          <div class="skeleton-company-info">
            <div class="skeleton-line"></div>
            <div class="skeleton-line short"></div>
          </div>
        </div>
      </div>
      <div class="skeleton-card">
        <div v-for="i in 3" :key="i" class="skeleton-timeline-item">
          <div class="skeleton-timeline-dot"></div>
          <div class="skeleton-timeline-content">
            <div class="skeleton-line"></div>
            <div class="skeleton-line short"></div>
          </div>
        </div>
      </div>
    </div>

    <!-- ========== 物流信息内容 ========== -->
    <div v-else-if="orderInfo || logisticsInfo.trackingNumber" class="logistics-content">

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

      <!-- ========== 收货地址卡片 ========== -->
      <div class="address-card" v-if="addressData">
        <div class="address-icon">
          <i class="fas fa-map-marker-alt"></i>
        </div>
        <div class="address-content">
          <div class="address-detail">{{ formatAddress(addressData) }}</div>
          <div class="phone-row">
            <span>{{ addressData.recipientName }}</span>
            <span class="phone">{{ showFullPhone ? addressData.recipientPhone : formatPhone(addressData.recipientPhone) }}</span>
            <button class="eye-btn" @click="showFullPhone = !showFullPhone">
              <i :class="showFullPhone ? 'fas fa-eye' : 'fas fa-eye-slash'"></i>
            </button>
            <span class="privacy-tip">保护手机号码</span>
          </div>
        </div>
      </div>

      <!-- ========== 物流公司卡片 ========== -->
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

      <!-- ========== 物流轨迹时间线 ========== -->
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
          <button class="btn-outline" @click="contactBuyer">
            <i class="fas fa-headset"></i>
            <span>联系买家</span>
          </button>
        </div>
      </div>
    </div>

    <!-- ========== 空状态 ========== -->
    <div v-else class="empty-state">
      <i class="fas fa-box-open"></i>
      <p>订单不存在</p>
      <p class="tips">请从订单列表进入</p>
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

interface OrderInfo {
  id: number | null
  orderNumber: string
  status: string
  totalAmount: number
  createdAt: string | null
  paidAt: string | null
  processingAt: string | null
  shippedAt: string | null
  completedAt: string | null
  returnApplyTime: string | null
  trackingNumber: string | null
  logisticsCode: string
  logisticsName: string
}

interface LogisticsTrace {
  time: string
  description: string
  status?: string
  location?: string
}

interface LogisticsInfo {
  trackingNumber: string
  logisticsCode: string
  logisticsName: string
  traces: LogisticsTrace[]
}

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
}

interface ProductInfo {
  productName: string
  productImage: string
  skuName?: string
  quantity: number
}

// ==================== 响应式数据 ====================

const loading = ref(true)
const refreshing = ref(false)
const confirming = ref(false)

const productItems = ref<ProductInfo[]>([])

const refundId = ref<string | null>(null)
const isReturnLogistics = ref(false)
const buyerId = ref<number | null>(null)

const orderInfo = ref<OrderInfo>({
  id: null,
  orderNumber: '',
  status: '',
  totalAmount: 0,
  createdAt: null,
  paidAt: null,
  processingAt: null,
  shippedAt: null,
  completedAt: null,
  returnApplyTime: null,
  trackingNumber: null,
  logisticsCode: '',
  logisticsName: ''
})

const logisticsInfo = ref<LogisticsInfo>({
  trackingNumber: '',
  logisticsCode: '',
  logisticsName: '',
  traces: []
})

const logisticsTraces = ref<LogisticsTrace[]>([])
const addressData = ref<Address | null>(null)
const showFullPhone = ref(false)

const showConfirmButton = computed(() => {
  return isReturnLogistics.value && logisticsInfo.value.trackingNumber
})

// ==================== 数据加载 ====================

const loadLogistics = async () => {
  loading.value = true

  try {
    const orderId = route.query.orderId
    const trackingNumber = route.query.trackingNumber as string
    const logisticsName = route.query.logisticsName as string
    const refundParam = route.query.refundId as string

    // === 场景 1：退货物流（有 refundId） ===
    if (route.query.refundId) {
      const refundRes = await authAPI.getRefundDetail(Number(route.query.refundId))
      if (refundRes.success && refundRes.data) {
        const refund = refundRes.data
        isReturnLogistics.value = true
        refundId.value = route.query.refundId as string
        buyerId.value = refund.buyerId || null

        orderInfo.value = {
          id: null,
          orderNumber: refund.orderNumber || '',
          status: 'RETURNING',
          totalAmount: 0,
          createdAt: null,
          paidAt: null,
          processingAt: null,
          shippedAt: refund.returnApplyTime || refund.applyTime || '',
          completedAt: null,
          returnApplyTime: refund.returnApplyTime || refund.applyTime || '',
          trackingNumber: refund.returnTrackingNumber || '',
          logisticsCode: '',
          logisticsName: refund.returnLogisticsName || ''
        }

        logisticsInfo.value = {
          trackingNumber: refund.returnTrackingNumber || '',
          logisticsCode: '',
          logisticsName: refund.returnLogisticsName || '',
          traces: []
        }

        productItems.value = [{
          productName: refund.productName || '',
          productImage: refund.productImage || '',
          skuName: refund.skuName || '',
          quantity: refund.quantity || 1
        }]

        if (refund.returnTrackingNumber) {
          const res = await authAPI.getLogisticsByTrackingNumber(
            refund.returnTrackingNumber,
            refund.returnLogisticsName
          )
          if (res.success && res.data) {
            logisticsTraces.value = res.data.traces || []
          }
        }

        loading.value = false
        return
      }
    }

    // === 场景 2：只有 trackingNumber ===
    if (trackingNumber) {
      isReturnLogistics.value = true
      refundId.value = refundParam || null

      orderInfo.value = {
        id: null,
        orderNumber: '',
        status: 'SHIPPED',
        totalAmount: 0,
        createdAt: null,
        paidAt: null,
        processingAt: null,
        shippedAt: null,
        completedAt: null,
        returnApplyTime: null,
        trackingNumber: trackingNumber,
        logisticsCode: '',
        logisticsName: logisticsName || ''
      }

      logisticsInfo.value = {
        trackingNumber: trackingNumber,
        logisticsCode: '',
        logisticsName: logisticsName || '',
        traces: []
      }

      if (refundParam) {
        const refundRes = await authAPI.getRefundDetail(Number(refundParam))
        if (refundRes.success && refundRes.data) {
          orderInfo.value.orderNumber = refundRes.data.orderNumber || ''
          productItems.value = [{
            productName: refundRes.data.productName || '',
            productImage: refundRes.data.productImage || '',
            skuName: refundRes.data.skuName || '',
            quantity: refundRes.data.quantity || 1
          }]
        }
      }

      const response = await authAPI.getLogisticsByTrackingNumber(trackingNumber, logisticsName)
      if (response.success && response.data) {
        logisticsTraces.value = response.data.traces || []
      }

      loading.value = false
      return
    }

    // === 场景 3：订单物流（有 orderId） ===
    if (!orderId) {
      Message.error('订单ID不存在')
      router.push({ name: 'SellerOrders' })
      return
    }

    const response = await authAPI.getLogisticsInfo(Number(orderId))

    if (response.success && response.data) {
      let order = null
      if (response.data.order) {
        order = response.data.order
        orderInfo.value = {
          id: order.id,
          orderNumber: order.orderNumber,
          status: order.status,
          totalAmount: order.totalAmount,
          createdAt: order.createdAt || null,
          paidAt: order.paidAt || null,
          processingAt: order.processingAt || null,
          shippedAt: order.shippedAt || null,
          completedAt: order.completedAt || null,
          returnApplyTime: order.returnApplyTime || null,
          trackingNumber: order.trackingNumber,
          logisticsCode: order.logisticsCode || '',
          logisticsName: order.logisticsName || ''
        }
      }

      const logistics = response.data.logistics
      const traces = logistics?.traces || []

      logisticsInfo.value = {
        trackingNumber: logistics?.trackingNumber || order?.trackingNumber || '',
        logisticsCode: logistics?.logisticsCode || order?.logisticsCode || '',
        logisticsName: logistics?.logisticsName || order?.logisticsName || '',
        traces: traces
      }

      // 后端已合并默认轨迹和真实轨迹，直接使用
      logisticsTraces.value = traces

      // 获取订单项
      const orderRes = await authAPI.getSellerOrderDetail(Number(orderId))
      if (orderRes.success) {
        const items = orderRes.data?.orderDetail?.orderItems || []
        productItems.value = items.map((item: any) => ({
          productName: item.productName || '',
          productImage: item.productImage || '',
          skuName: item.skuName || '',
          quantity: item.quantity || 1
        }))
      }

      // 获取收货地址
      if (response.data.address) {
        addressData.value = response.data.address
      }
    } else {
      Message.error(response.message || '获取物流信息失败')
    }
  } catch (error) {
    Message.error('加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// ==================== 刷新物流 ====================

const refreshLogistics = async () => {
  refreshing.value = true

  try {
    const trackingNumber = logisticsInfo.value.trackingNumber

    if (isReturnLogistics.value && trackingNumber) {
      const response = await authAPI.getLogisticsByTrackingNumber(trackingNumber, logisticsInfo.value.logisticsName)
      if (response.success && response.data) {
        logisticsTraces.value = response.data.traces || []
        Message.success('刷新成功')
      } else {
        Message.error(response.message || '刷新失败')
      }
    } else {
      const orderId = orderInfo.value.id
      const response = await authAPI.refreshLogistics(Number(orderId))

      if (response.success && response.data?.logistics) {
        const logistics = response.data.logistics
        const traces = logistics.traces || []

        logisticsInfo.value = {
          trackingNumber: logistics.trackingNumber || '',
          logisticsCode: logistics.logisticsCode || '',
          logisticsName: logistics.logisticsName || '',
          traces: traces
        }

        // 后端已合并默认轨迹和真实轨迹，直接使用
        logisticsTraces.value = traces
        Message.success('刷新成功')
      } else {
        Message.error(response.message || '刷新失败')
      }
    }
  } catch (error: any) {
    Message.error(error.message || '刷新物流失败')
  } finally {
    refreshing.value = false
  }
}

// ==================== 确认退货收货 ====================

const confirmReturnReceive = async () => {
  if (!refundId.value) {
    Message.error('退款记录不存在')
    return
  }

  try {
    await Message.confirm('确认已收到退货商品？确认后将自动处理退款。', '确认收货')

    confirming.value = true

    const response = await authAPI.sellerConfirmReturn(Number(refundId.value))

    if (response.success) {
      Message.success('确认收货成功，退款已处理')
      setTimeout(() => {
        router.back()
      }, 1500)
    } else {
      Message.error(response.message || '操作失败')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      Message.error(error.message || '操作失败')
    }
  } finally {
    confirming.value = false
  }
}

const contactBuyer = () => {
  const targetUserId = route.query.userId || buyerId.value
  if (!targetUserId) {
    Message.error('无法获取买家信息')
    return
  }
  router.push({ name: 'Chat', params: { targetId: Number(targetUserId) } })
}

// ==================== 工具函数 ====================

const goBack = () => {
  router.back()
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

const formatAddress = (address: Address): string => {
  const parts = [address.province, address.city, address.district, address.detailAddress]
  return parts.filter(p => p?.trim()).join(' ')
}

const formatPhone = (phone: string): string => {
  if (!phone) return ''
  if (phone.length === 11) {
    return phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2')
  }
  return phone
}

// ==================== 生命周期 ====================

onMounted(() => {
  if (!authStore.validateSellerPermission()) return
  loadLogistics()
})
</script>

<style scoped>
@import url('@/static/css/seller/商家物流.css');
</style>
