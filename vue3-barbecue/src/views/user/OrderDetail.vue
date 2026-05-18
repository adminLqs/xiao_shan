<template>
  <div class="order-detail-container">
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
    <div v-else>
      <!-- 订单状态卡片 -->
      <div class="status-card" :class="detail.order.status">
        <div class="status-icon">{{ statusIcon }}</div>
        <div class="status-info">
          <h3 class="status-title">{{ statusText }}</h3>
          <p class="status-time">{{ formatDateTime(detail.order.createdAt) }}</p>
          <p v-if="detail.order.paidAt" class="status-time">支付时间：{{ formatDateTime(detail.order.paidAt) }}</p>
          <p v-if="refundRecord?.completedAt" class="status-time">退款时间：{{ formatDateTime(refundRecord.completedAt) }}</p>
        </div>
      </div>

      <!-- 订单信息卡片 -->
      <div class="info-card">
        <h3 class="card-title">订单详情</h3>
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
        <div class="info-row" v-if="detail.order.remark">
          <span class="info-label">备注</span>
          <span class="info-value">{{ detail.order.remark }}</span>
        </div>
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

      <!-- 收货信息卡片 -->
      <div class="info-card" v-if="detail.order.deliveryType === 'delivery' || detail.order.deliveryType === 'takeaway'">
        <h3 class="card-title">{{ detail.order.deliveryType === 'takeaway' ? '取餐信息' : '收货信息' }}</h3>
        <div class="info-row">
          <span class="info-label">{{ detail.order.deliveryType === 'takeaway' ? '取餐人' : '收货人' }}</span>
          <span class="info-value">{{ detail.order.recipientName }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">联系电话</span>
          <span class="info-value">{{ detail.order.recipientPhone }}</span>
        </div>
        <div class="info-row" v-if="detail.order.deliveryType === 'delivery'">
          <span class="info-label">收货地址</span>
          <span class="info-value">{{ detail.order.detailAddress }}</span>
        </div>
      </div>

      <!-- 用餐信息 -->
      <div class="info-card" v-if="detail.order.deliveryType === 'dinein'">
        <h3 class="card-title">用餐信息</h3>
        <div class="info-row">
          <span class="info-label">用餐人数</span>
          <span class="info-value">{{ detail.order.peopleCount || 1 }}人</span>
        </div>
        <div class="info-row" v-if="detail.order.tablePreference">
          <span class="info-label">桌号偏好</span>
          <span class="info-value">{{ detail.order.tablePreference }}</span>
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
        <div class="fee-row">
          <span>商品合计</span>
          <span>¥{{ formatPrice(subtotal) }}</span>
        </div>
        <div class="fee-row total">
          <span>实付金额</span>
          <span>¥{{ formatPrice(detail.order.totalAmount) }}</span>
        </div>
      </div>

      <!-- 底部按钮 -->
      <div class="detail-footer">
        <button v-if="detail.order.status === 'PENDING'" class="action-btn warning" @click="goToPay">去支付</button>
        <button v-if="detail.order.status === 'PAID'" class="action-btn refund" @click="openRefundModal">申请退款</button>
        <button class="action-btn secondary" @click="backToHome">返回首页</button>
      </div>
    </div>

    <!-- 退款弹窗 -->
    <div v-if="showRefundModal" class="refund-modal-mask" @click.self="closeRefundModal">
      <div class="refund-modal">
        <div class="refund-modal-header">
          <h3>申请退款</h3>
          <button class="close-btn" @click="closeRefundModal">✕</button>
        </div>
        <div class="refund-modal-body">
          <div class="refund-info">
            <div class="info-item">
              <span class="label">订单号：</span>
              <span class="value">{{ detail.order.orderNumber }}</span>
            </div>
            <div class="info-item">
              <span class="label">订单金额：</span>
              <span class="value">¥{{ formatPrice(detail.order.totalAmount) }}</span>
            </div>
            <div class="info-item">
              <span class="label">可退款：</span>
              <span class="value highlight">¥{{ formatPrice(maxRefundAmount) }}</span>
            </div>
          </div>
          
          <div class="form-group">
            <label>退款金额 <span class="required">*</span></label>
            <div class="amount-input">
              <span class="currency">¥</span>
              <input type="number" v-model="refundAmountInput" :max="maxRefundAmount" :min="0.01" step="0.01" placeholder="请输入退款金额" />
            </div>
          </div>
          
          <div class="form-group">
            <label>退款原因 <span class="required">*</span></label>
            <textarea v-model="refundReasonInput" rows="3" placeholder="请填写退款原因"></textarea>
          </div>
          
          <div class="refund-tips">
            <p>💡 退款说明：</p>
            <ul>
              <li>退款金额将原路返回您的支付账户</li>
              <li>退款预计1-3个工作日到账</li>
              <li>如有疑问请联系客服</li>
            </ul>
          </div>
        </div>
        <div class="refund-modal-footer">
          <button class="cancel-btn" @click="closeRefundModal">取消</button>
          <button class="confirm-btn" @click="submitRefund" :disabled="submitting">
            {{ submitting ? '提交中...' : '提交申请' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Toast, Dialog } from '@/utils/vant'
import { authAPI } from '@/api/authAPI'

// ==================== 类型定义 ====================
interface Order {
  id: number
  orderNumber: string
  totalAmount: number
  status: string
  paymentMethod: string
  transactionId: string
  deliveryType: string
  recipientName: string
  recipientPhone: string
  detailAddress: string
  peopleCount: number
  tablePreference: string
  remark: string
  paidAt: string
  createdAt: string
}

interface OrderItem {
  id: number
  productName: string
  price: number
  quantity: number
  image?: string
}

interface RefundRecord {
  id: number
  refundAmount: number
  refundReason: string
  refundTransactionId: string
  status: string
  failReason: string
  createdAt: string
  completedAt: string
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
const submitting = ref(false)
const showRefundModal = ref(false)

const detail = ref<OrderDetailVO>({
  order: {} as Order,
  orderItems: [],
  refundRecords: null
})

const refundAmountInput = ref(0)
const refundReasonInput = ref('')

// ==================== 计算属性 ====================
const order = computed(() => detail.value.order)
const refundRecord = computed(() => detail.value.refundRecords)
const orderItems = computed(() => detail.value.orderItems)

const statusIcon = computed(() => {
  const icons: Record<string, string> = {
    PENDING: '⏳', PAID: '✅', SHIPPED: '📦', COMPLETED: '🎉',
    CANCELLED: '❌', REFUNDING: '🔄', REFUNDED: '💰', REFUNDFAILED: '❌'
  }
  return icons[order.value.status] || '📦'
})

const statusText = computed(() => {
  const texts: Record<string, string> = {
    PENDING: '待支付', PAID: '已支付', SHIPPED: '已发货', COMPLETED: '已完成',
    CANCELLED: '已取消', REFUNDING: '退款中', REFUNDED: '已退款', REFUNDFAILED: '退款失败'
  }
  return texts[order.value.status] || '未知状态'
})

const paymentMethodText = computed(() => {
  const m: Record<string, string> = { WECHAT: '微信支付', ALIPAY: '支付宝' }
  return m[order.value.paymentMethod] || order.value.paymentMethod || '未支付'
})

const refundStatusText = computed(() => {
  const s: Record<string, string> = { PROCESSING: '处理中', SUCCESS: '已退款', FAILED: '退款失败' }
  return refundRecord.value ? (s[refundRecord.value.status] || refundRecord.value.status) : ''
})

const subtotal = computed(() => {
  return orderItems.value.reduce((sum, item) => sum + (item.price * item.quantity), 0)
})

const maxRefundAmount = computed(() => {
  if (!refundRecord.value || refundRecord.value.status !== 'SUCCESS') {
    return order.value.totalAmount
  }
  return order.value.totalAmount - refundRecord.value.refundAmount
})

// ==================== 工具方法 ====================
const formatPrice = (p: number) => (p ?? 0).toFixed(2)
const formatDateTime = (d: string) => {
  if (!d) return ''
  const dt = new Date(d)
  return `${dt.getFullYear()}-${String(dt.getMonth()+1).padStart(2,'0')}-${String(dt.getDate()).padStart(2,'0')} ${String(dt.getHours()).padStart(2,'0')}:${String(dt.getMinutes()).padStart(2,'0')}`
}

const copyText = async (text: string, label: string) => {
  try {
    await navigator.clipboard.writeText(text)
    Toast.success(`${label}已复制`)
  } catch { Toast.fail('复制失败') }
}

const getProductEmoji = (name: string): string => {
  if (!name) return '🍖'
  if (name.includes('羊') || name.includes('牛') || name.includes('猪')) return '🥩'
  if (name.includes('鸡')) return '🍗'
  if (name.includes('鱼') || name.includes('虾')) return '🐟'
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
const backToHome = () => router.push({ name: 'UserDashboard' })
const goToPay = () => {
  router.push({ name: 'UserPayment', query: { orderNumber: order.value.orderNumber, amount: order.value.totalAmount.toFixed(2) } })
}

// ==================== 退款方法 ====================
const openRefundModal = () => {
  refundAmountInput.value = maxRefundAmount.value
  refundReasonInput.value = ''
  showRefundModal.value = true
}

const closeRefundModal = () => {
  showRefundModal.value = false
  refundAmountInput.value = 0
  refundReasonInput.value = ''
}

const submitRefund = async () => {
  if (refundAmountInput.value <= 0) { Toast.fail('请输入退款金额'); return }
  if (refundAmountInput.value > maxRefundAmount.value) { Toast.fail('退款金额不能超过可退金额'); return }
  if (!refundReasonInput.value.trim()) { Toast.fail('请填写退款原因'); return }
  
  try {
    await Dialog.confirm(`确定要申请退款 ¥${formatPrice(refundAmountInput.value)} 吗？`, {
      confirmButtonText: '确认申请', cancelButtonText: '再想想'
    })
    
    submitting.value = true
    Toast.loading('提交退款申请...')
    
    const response = await authAPI.refundOrder({
      orderNumber: order.value.orderNumber,
      refundAmount: refundAmountInput.value,
      refundReason: refundReasonInput.value
    })
    
    if (response.success) {
      Toast.success('退款申请已提交')
      closeRefundModal()
      loadOrderDetail()
    } else {
      Toast.fail(response.message || '退款失败')
    }
  } catch { /* 取消 */ }
  finally { submitting.value = false }
}

// ==================== 加载订单详情 ====================
const loadOrderDetail = async () => {
  const orderNumber = route.query.orderNumber as string
  if (!orderNumber) { Toast.fail('订单号不存在'); router.back(); return }
  
  loading.value = true
  try {
    const response = await authAPI.getOrderDetail(orderNumber)
    if (!response.success) throw new Error(response.message || '获取订单详情失败')
    
    detail.value = response.data.orderDetail
  } catch (error: any) {
    Toast.fail(error.message || '加载失败')
    setTimeout(() => router.push({ name: 'UserDashboard' }), 2000)
  } finally { loading.value = false }
}

onMounted(() => loadOrderDetail())
</script>

<style scoped>
@import url('@/static/css/user/订单详情页.css');
</style>