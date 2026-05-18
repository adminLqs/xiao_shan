<template>
  <div class="seller-order-detail">
    <!-- 顶部导航 -->
    <div class="detail-header">
      <button class="back-btn" @click="goBack">
        <span class="back-icon">←</span>
      </button>
      <h1 class="header-title">订单详情</h1>
      <div class="header-placeholder"></div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>

    <!-- 订单内容 -->
    <div v-else class="detail-content">
      <!-- 订单状态卡片 -->
      <div class="status-card" :class="detail.order.status">
        <div class="status-icon">{{ statusIcon }}</div>
        <div class="status-info">
          <h3 class="status-title">{{ statusText }}</h3>
          <p class="status-time">下单时间：{{ formatDateTime(detail.order.createdAt) }}</p>
          <p v-if="detail.order.paidAt" class="status-time">支付时间：{{ formatDateTime(detail.order.paidAt) }}</p>
          <p v-if="refundRecord?.completedAt" class="status-time">退款时间：{{ formatDateTime(refundRecord.completedAt) }}</p>
        </div>
      </div>

      <!-- 订单信息卡片 -->
      <div class="info-card">
        <h3 class="card-title">订单信息</h3>
        <div class="info-row">
          <span class="info-label">订单编号</span>
          <span class="info-value">{{ detail.order.orderNumber }}</span>
          <button class="copy-btn" @click="copyText(detail.order.orderNumber, '订单号')">复制</button>
        </div>
        <div class="info-row">
          <span class="info-label">下单时间</span>
          <span class="info-value">{{ formatDateTime(detail.order.createdAt) }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">支付方式</span>
          <span class="info-value">{{ paymentMethodText }}</span>
        </div>
        <div class="info-row" v-if="detail.order.transactionId">
          <span class="info-label">交易单号</span>
          <span class="info-value">{{ detail.order.transactionId }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">配送方式</span>
          <span class="info-value">{{ deliveryTypeText }}</span>
        </div>
        <div class="info-row" v-if="detail.order.remark">
          <span class="info-label">订单备注</span>
          <span class="info-value remark">{{ detail.order.remark }}</span>
        </div>
      </div>

      <!-- 收货/用餐信息卡片 -->
      <div class="info-card" v-if="detail.order.deliveryType === 'delivery' || detail.order.deliveryType === 'takeaway' || detail.order.deliveryType === 'dinein'">
        <h3 class="card-title">{{ detail.order.deliveryType === 'dinein' ? '用餐信息' : detail.order.deliveryType === 'takeaway' ? '取餐信息' : '收货信息' }}</h3>
        <template v-if="detail.order.deliveryType === 'delivery'">
          <div class="info-row">
            <span class="info-label">收货人</span>
            <span class="info-value">{{ detail.order.recipientName }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">联系电话</span>
            <span class="info-value">{{ detail.order.recipientPhone }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">收货地址</span>
            <span class="info-value">{{ detail.order.detailAddress }}</span>
          </div>
        </template>
        <template v-else-if="detail.order.deliveryType === 'takeaway'">
          <div class="info-row">
            <span class="info-label">取餐人</span>
            <span class="info-value">{{ detail.order.recipientName }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">联系电话</span>
            <span class="info-value">{{ detail.order.recipientPhone }}</span>
          </div>
        </template>
        <template v-else-if="detail.order.deliveryType === 'dinein'">
          <div class="info-row">
            <span class="info-label">用餐人数</span>
            <span class="info-value">{{ detail.order.peopleCount || 1 }}人</span>
          </div>
          <div class="info-row" v-if="detail.order.tablePreference">
            <span class="info-label">桌号偏好</span>
            <span class="info-value">{{ detail.order.tablePreference }}</span>
          </div>
        </template>
      </div>

      <!-- 退款信息卡片 -->
      <div class="info-card refund-card" v-if="refundRecord">
        <h3 class="card-title">退款信息</h3>
        <div class="info-row">
          <span class="info-label">退款金额</span>
          <span class="info-value">¥{{ formatPrice(refundRecord.refundAmount) }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">退款状态</span>
          <span class="info-value">
            <span class="refund-status-badge" :class="refundRecord.status.toLowerCase()">
              {{ refundStatusText }}
            </span>
          </span>
        </div>
        <div class="info-row" v-if="refundRecord.createdAt">
          <span class="info-label">申请时间</span>
          <span class="info-value">{{ formatDateTime(refundRecord.createdAt) }}</span>
        </div>
        <div class="info-row" v-if="refundRecord.completedAt">
          <span class="info-label">完成时间</span>
          <span class="info-value">{{ formatDateTime(refundRecord.completedAt) }}</span>
        </div>
        <div class="info-row" v-if="refundRecord.refundReason">
          <span class="info-label">退款原因</span>
          <span class="info-value">{{ refundRecord.refundReason }}</span>
        </div>
        <div class="info-row" v-if="refundRecord.status === 'FAILED' && refundRecord.failReason">
          <span class="info-label">失败原因</span>
          <span class="info-value" style="color: #f44336">{{ refundRecord.failReason }}</span>
        </div>
        <div class="info-row" v-if="refundRecord.refundTransactionId">
          <span class="info-label">退款交易号</span>
          <span class="info-value refund-tx">{{ refundRecord.refundTransactionId }}</span>
          <button class="copy-btn" @click="copyText(refundRecord.refundTransactionId, '退款交易号')">复制</button>
        </div>
      </div>

      <!-- 商品列表 -->
      <div class="product-card">
        <h3 class="card-title">商品清单</h3>
        <div class="product-list">
          <div v-for="item in detail.orderItems" :key="item.id" class="product-item">
            <div class="product-image">
              <img v-if="item.image" :src="item.image" :alt="item.productName" @error="handleImageError" />
              <div v-else class="image-placeholder">
                <span class="placeholder-emoji">{{ getProductEmoji(item.productName) }}</span>
              </div>
            </div>
            <div class="product-info">
              <div class="product-name">{{ item.productName }}</div>
            </div>
            <div class="product-price">
              <div class="price">¥{{ formatPrice(item.price) }}</div>
              <div class="quantity">x{{ item.quantity }}</div>
              <div class="subtotal">¥{{ formatPrice(item.price * item.quantity) }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 费用明细 -->
      <div class="fee-card">
        <h3 class="card-title">费用明细</h3>
        <div class="fee-row">
          <span>商品合计</span>
          <span>¥{{ formatPrice(subtotal) }}</span>
        </div>
        <div class="fee-row total">
          <span>实付金额</span>
          <span>¥{{ formatPrice(detail.order.totalAmount) }}</span>
        </div>
      </div>

      <!-- 底部操作栏 -->
      <div class="action-bar">
        <button class="action-btn back-btn-bottom" @click="goBack">返回列表</button>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
  import { ref, computed, onMounted } from 'vue'
  import { useRouter, useRoute } from 'vue-router'
  import Message from '@/utils/message'
  import { authAPI } from '../../api/authAPI'

  // ==================== 类型定义 ====================
  interface Order {
    id: number; orderNumber: string; totalAmount: number; status: string
    paymentMethod: string; transactionId: string; deliveryType: string
    recipientName: string; recipientPhone: string; detailAddress: string
    peopleCount: number; tablePreference: string; remark: string
    paidAt: string; createdAt: string
  }

  interface OrderItem {
    id: number; productName: string; price: number; quantity: number; image?: string
  }

  interface RefundRecord {
    id: number; refundAmount: number; refundReason: string
    refundTransactionId: string; status: string; failReason: string
    createdAt: string; completedAt: string
  }

  interface OrderDetailVO {
    order: Order
    orderItems: OrderItem[]
    refundRecords: RefundRecord | null
  }

  // ==================== 路由 ====================
  const router = useRouter()
  const route = useRoute()

  // ==================== 响应式数据 ====================
  const loading = ref(true)
  const detail = ref<OrderDetailVO>({ order: {} as Order, orderItems: [], refundRecords: null })

  // ==================== 计算属性 ====================
  const refundRecord = computed(() => detail.value.refundRecords)

  const statusIcon = computed(() => {
    const icons: Record<string, string> = {
      PENDING: '⏳', PAID: '✅', SHIPPED: '📦', COMPLETED: '🎉',
      CANCELLED: '❌', REFUNDING: '🔄', REFUNDED: '💰', REFUNDFAILED: '❌'
    }
    return icons[detail.value.order.status] || '📦'
  })

  const statusText = computed(() => {
    const texts: Record<string, string> = {
      PENDING: '待支付', PAID: '已支付', SHIPPED: '已发货', COMPLETED: '已完成',
      CANCELLED: '已取消', REFUNDING: '退款中', REFUNDED: '已退款', REFUNDFAILED: '退款失败'
    }
    return texts[detail.value.order.status] || '未知状态'
  })

  const paymentMethodText = computed(() => {
    const m: Record<string, string> = { WECHAT: '微信支付', ALIPAY: '支付宝' }
    return m[detail.value.order.paymentMethod] || detail.value.order.paymentMethod || '未支付'
  })

  const deliveryTypeText = computed(() => {
    const t: Record<string, string> = { dinein: '到店用餐', takeaway: '打包自取', delivery: '外卖配送' }
    return t[detail.value.order.deliveryType] || detail.value.order.deliveryType
  })

  const refundStatusText = computed(() => {
    if (!refundRecord.value) return ''
    const s: Record<string, string> = { PROCESSING: '处理中', SUCCESS: '退款成功', FAILED: '退款失败' }
    return s[refundRecord.value.status] || refundRecord.value.status
  })

  const subtotal = computed(() => {
    return detail.value.orderItems.reduce((sum, item) => sum + (item.price * item.quantity), 0)
  })

  // ==================== 工具方法 ====================
  const formatPrice = (p: number) => (p ?? 0).toFixed(2)
  const formatDateTime = (d: string) => {
    if (!d) return ''
    const dt = new Date(d)
    return `${dt.getFullYear()}-${String(dt.getMonth()+1).padStart(2,'0')}-${String(dt.getDate()).padStart(2,'0')} ${String(dt.getHours()).padStart(2,'0')}:${String(dt.getMinutes()).padStart(2,'0')}`
  }

  const copyText = async (text: string, label: string) => {
    try { await navigator.clipboard.writeText(text); Message.success(`${label}已复制`) }
    catch { Message.error('复制失败') }
  }

  const getProductEmoji = (name: string): string => {
    if (!name) return '🍖'
    if (name.includes('羊')||name.includes('牛')||name.includes('猪')) return '🥩'
    if (name.includes('鸡')) return '🍗'
    if (name.includes('鱼')||name.includes('虾')) return '🐟'
    if (name.includes('蔬菜')) return '🥬'
    if (name.includes('啤酒')) return '🍺'
    return '🍢'
  }

  const handleImageError = (event: Event) => {
    const img = event.target as HTMLImageElement
    img.style.display = 'none'
    const placeholder = document.createElement('div')
    placeholder.className = 'image-placeholder'
    placeholder.innerHTML = '<span class="placeholder-emoji">🍖</span>'
    img.parentNode?.appendChild(placeholder)
  }

  const goBack = () => router.back()

  // ==================== 加载订单详情 ====================
  const loadOrderDetail = async () => {
    const orderNumber = route.query.orderNumber as string
    if (!orderNumber) { Message.error('订单号不存在'); router.back(); return }
    
    loading.value = true
    try {
      const response = await authAPI.getOrderDetail(orderNumber)
      if (!response.success) throw new Error(response.message || '获取订单详情失败')
      detail.value = response.data.orderDetail
    } catch (error: any) {
      Message.error(error.message || '加载失败')
      setTimeout(() => router.back(), 2000)
    } finally { loading.value = false }
  }

  onMounted(() => loadOrderDetail())
</script>

<style scoped>
@import url('@/static/css/seller/订单详情页.css');
</style>