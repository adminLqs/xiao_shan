<template>
  <!-- 用户物流详情页面容器 -->
  <div class="user-logistics-container">

    <!-- ========== 加载状态 ========== -->
    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>

    <!-- ========== 物流信息内容 ========== -->
    <div v-else-if="orderInfo" class="logistics-content">

      <!-- ========== 订单号显示 ========== -->
      <div class="order-number-row">
        <span class="order-label">订单号</span>
        <span class="order-value">{{ orderInfo.orderNumber }}</span>
      </div>

      <!-- ========== 物流公司卡片（紧凑型） ========== -->
      <div v-if="orderInfo.trackingNumber" class="logistics-company-card">
        <div class="company-logo">
          <i class="fas fa-truck"></i>
        </div>
        <div class="company-info">
          <div class="company-name">{{ logisticsInfo.logisticsName || '-' }}</div>
          <div class="tracking-number">{{ logisticsInfo.trackingNumber || '-' }}</div>
        </div>
        <button class="copy-btn" @click="copyTrackingNumber">
          <i class="fas fa-copy"></i>
        </button>
      </div>

      <!-- ========== 物流轨迹时间线（核心区域） ========== -->
      <div v-if="orderInfo.trackingNumber" class="logistics-card">
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

      <!-- ========== 未发货状态 ========== -->
      <div v-if="!orderInfo.trackingNumber" class="not-shipped-card">
        <i class="fas fa-box"></i>
        <p>等待卖家发货</p>
        <p class="tips">卖家发货后将显示物流信息</p>
      </div>

      <!-- ========== 订单信息卡片（收件信息，默认展开） ========== -->
      <div class="address-card" v-if="orderInfo">
        <div class="card-header">
          <span class="card-title">收件信息</span>
        </div>
        <div class="card-content">
          <div class="address-info">
            <div class="receiver">
              <span class="receiver-name">{{ orderInfo.receiverName || '-' }}</span>
              <span class="receiver-phone">{{ orderInfo.receiverPhone || '-' }}</span>
            </div>
            <div class="receiver-address">{{ orderInfo.receiverAddress || '-' }}</div>
          </div>
        </div>
      </div>

      <!-- ========== 底部操作按钮 ========== -->
      <div class="action-buttons">
        <!-- 确认收货按钮（仅已发货订单显示） -->
        <button
          v-if="orderInfo.status === 'SHIPPED'"
          class="btn-receive"
          @click="confirmReceive"
          :disabled="confirming"
        >
          <i v-if="confirming" class="fas fa-spinner fa-spin"></i>
          {{ confirming ? '确认中...' : '确认收货' }}
        </button>

        <!-- 联系卖家按钮 -->
        <button class="btn-contact" @click="contactSeller">
          <i class="fas fa-headset"></i>
          <span>联系卖家</span>
        </button>
      </div>
    </div>

    <!-- ========== 订单不存在状态 ========== -->
    <div v-else class="empty-state">
      <i class="fas fa-box-open"></i>
      <p>订单不存在</p>
      <p class="tips">请从订单列表进入</p>
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

const loading = ref(false)
const confirming = ref(false)

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

      if (logistics) {
        logisticsInfo.value = {
          trackingNumber: logistics.trackingNumber || '',
          logisticsCode: logistics.logisticsCode || '',
          logisticsName: logistics.logisticsName || '',
          traces: logistics.traces || []
        }
        logisticsTraces.value = logistics.traces || []
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

    const loading = Message.loading({ message: '确认中...' })

    const response = await authAPI.confirmReceive(orderInfo.value.id)

    loading.close()

    if (response.success) {
      Message.success('确认收货成功')
      orderInfo.value.status = 'COMPLETED'

      setTimeout(() => {
        router.push('/user/orders')
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

const contactSeller = () => {
  Message.info('联系卖家功能开发中')
}

const formatPrice = (price: number): string => {
  if (price == null) return '0.00'
  return price.toFixed(2)
}

const formatDateTime = (dateStr: string): string => {
  if (!dateStr) return '-'

  try {
    const date = new Date(dateStr)
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    const hours = String(date.getHours()).padStart(2, '0')
    const minutes = String(date.getMinutes()).padStart(2, '0')
    return `${month}-${day} ${hours}:${minutes}`
  } catch {
    return '-'
  }
}

onMounted(() => {
  loadLogistics()
})
</script>

<style scoped>
@import url('@/static/css/user/物流页.css');
</style>
