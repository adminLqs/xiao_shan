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
      <div class="status-card" :class="orderStatus">
        <div class="status-icon">{{ statusIcon }}</div>
        <div class="status-info">
          <h3 class="status-title">{{ statusText }}</h3>
          <p class="status-time">{{ orderTime }}</p>
          <p v-if="paidAt" class="status-time">支付时间：{{ formatDateTime(paidAt) }}</p>
          <p v-if="refundTime" class="status-time">退款时间：{{ formatDateTime(refundTime) }}</p>
        </div>
      </div>

      <!-- 订单信息卡片 -->
      <div class="info-card">
        <div class="info-row">
          <span class="info-label">订单编号</span>
          <span class="info-value">{{ orderNumber }}</span>
          <button class="copy-btn" @click="copyOrderNumber">复制</button>
        </div>
        <div class="info-row">
          <span class="info-label">下单时间</span>
          <span class="info-value">{{ orderTime }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">支付方式</span>
          <span class="info-value">{{ paymentMethodText }}</span>
        </div>
        <div class="info-row" v-if="transactionId">
          <span class="info-label">交易单号</span>
          <span class="info-value">{{ transactionId }}</span>
        </div>
      </div>

      <!-- 退款信息卡片（已退款时显示） -->
      <div class="info-card refund-card" v-if="refundAmount > 0">
        <h3 class="card-title">退款信息</h3>
        <div class="info-row">
          <span class="info-label">退款金额</span>
          <span class="info-value">¥{{ formatPrice(refundAmount) }}</span>
        </div>
        <div class="info-row" v-if="refundStatus">
          <span class="info-label">退款状态</span>
          <span class="info-value">
            <span class="refund-status-badge" :class="refundStatus">
              {{ refundStatusText }}
            </span>
          </span>
        </div>
        <div class="info-row" v-if="refundTime">
          <span class="info-label">退款时间</span>
          <span class="info-value">{{ formatDateTime(refundTime) }}</span>
        </div>
        <div class="info-row" v-if="refundReason">
          <span class="info-label">退款原因</span>
          <span class="info-value">{{ refundReason }}</span>
        </div>
        <div class="info-row" v-if="refundTransactionId">
          <span class="info-label">退款交易号</span>
          <span class="info-value refund-tx">{{ refundTransactionId }}</span>
          <button class="copy-btn" @click="copyRefundTxId">复制</button>
        </div>
      </div>

      <!-- 收货信息卡片（根据配送类型显示） -->
      <div class="info-card" v-if="addressInfo">
        <h3 class="card-title">收货信息</h3>
        <div class="info-row">
          <span class="info-label">收货人</span>
          <span class="info-value">{{ addressInfo.name }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">联系电话</span>
          <span class="info-value">{{ addressInfo.phone }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">收货地址</span>
          <span class="info-value">{{ addressInfo.address }}</span>
        </div>
      </div>

      <!-- 用餐信息（到店用餐） -->
      <div class="info-card" v-if="diningInfo">
        <h3 class="card-title">用餐信息</h3>
        <div class="info-row">
          <span class="info-label">用餐人数</span>
          <span class="info-value">{{ diningInfo.peopleCount }}人</span>
        </div>
        <div class="info-row" v-if="diningInfo.tablePreference">
          <span class="info-label">桌号偏好</span>
          <span class="info-value">{{ diningInfo.tablePreference }}</span>
        </div>
      </div>

      <!-- 商品列表 -->
      <div class="product-card">
        <h3 class="card-title">商品清单</h3>
        <div class="product-list">
          <div 
            v-for="item in orderItems" 
            :key="item.id"
            class="product-item"
          >
            <div class="product-image">
              <img 
                v-if="item.image" 
                :src="item.image" 
                :alt="item.productName"
                @error="handleImageError"
              />
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
          <span>¥{{ formatPrice(totalAmount) }}</span>
        </div>
        <div class="fee-note" v-if="orderRemark">
          📝 备注：{{ orderRemark }}
        </div>
      </div>

      <!-- 底部按钮 -->
      <div class="detail-footer">
        <button 
          v-if="orderStatus === 'PENDING'"
          class="action-btn warning"
          @click="goToPay"
        >
          去支付
        </button>
        <button 
          v-if="orderStatus === 'PAID' && !isRefunded"
          class="action-btn refund"
          @click="openRefundModal"
        >
          申请退款
        </button>
        <button 
          class="action-btn secondary"
          @click="backToHome"
        >
          返回首页
        </button>
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
              <span class="value">{{ orderNumber }}</span>
            </div>
            <div class="info-item">
              <span class="label">订单金额：</span>
              <span class="value">¥{{ formatPrice(totalAmount) }}</span>
            </div>
            <div class="info-item" v-if="refundAmount > 0">
              <span class="label">已退款：</span>
              <span class="value">¥{{ formatPrice(refundAmount) }}</span>
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
              <input 
                type="number" 
                v-model="refundAmountInput" 
                :max="maxRefundAmount"
                :min="0.01"
                step="0.01"
                placeholder="请输入退款金额"
              />
            </div>
          </div>
          
          <div class="form-group">
            <label>退款原因 <span class="required">*</span></label>
            <textarea 
              v-model="refundReasonInput" 
              rows="3" 
              placeholder="请填写退款原因"
            ></textarea>
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
  import { showToast, showConfirmDialog } from 'vant'
  import { authAPI } from '@/api/authAPI'
  import 'vant/es/toast/style'
  import 'vant/es/dialog/style'

  // ==================== 类型定义 ====================
  interface OrderItem {
    id: number
    productId: number
    productName: string
    price: number
    quantity: number
    image?: string
  }

  interface AddressInfo {
    name: string
    phone: string
    address: string
  }

  interface DiningInfo {
    peopleCount: number
    tablePreference?: string
  }

  // ==================== 路由 ====================
  const router = useRouter()
  const route = useRoute()

  // ==================== 响应式数据 ====================
  const loading = ref(true)
  const submitting = ref(false)
  const showRefundModal = ref(false)

  // 订单基本信息
  const orderNumber = ref(route.query.orderNumber as string || '')
  const orderStatus = ref('PENDING')
  const orderTime = ref('')
  const paidAt = ref('')
  const paymentMethod = ref('')
  const transactionId = ref('')
  const orderRemark = ref('')
  const totalAmount = ref(0)

  // 退款信息
  const refundAmount = ref(0)
  const refundStatus = ref('NO_REFUND')
  const refundTime = ref('')
  const refundReason = ref('')
  const refundTransactionId = ref('')

  // 订单项
  const orderItems = ref<OrderItem[]>([])
  const subtotal = ref(0)

  // 配送/用餐信息
  const addressInfo = ref<AddressInfo | null>(null)
  const diningInfo = ref<DiningInfo | null>(null)

  // 退款弹窗输入
  const refundAmountInput = ref(0)
  const refundReasonInput = ref('')

  // ==================== 计算属性 ====================

  const statusIcon = computed(() => {
    const icons: Record<string, string> = {
      'PENDING': '⏳',
      'PAID': '✅',
      'SHIPPED': '📦',
      'COMPLETED': '🎉',
      'CANCELLED': '❌',
      'REFUNDING': '🔄',
      'REFUNDED': '💰'
    }
    return icons[orderStatus.value] || '📦'
  })

  const statusText = computed(() => {
    const texts: Record<string, string> = {
      'PENDING': '待支付',
      'PAID': '已支付',
      'SHIPPED': '已发货',
      'COMPLETED': '已完成',
      'CANCELLED': '已取消',
      'REFUNDING': '退款中',
      'REFUNDED': '已退款'
    }
    return texts[orderStatus.value] || '未知状态'
  })

  const paymentMethodText = computed(() => {
    const methods: Record<string, string> = {
      'WECHAT': '微信支付',
      'ALIPAY': '支付宝',

    }
    return methods[paymentMethod.value] || paymentMethod.value || '未支付'
  })

  const refundStatusText = computed(() => {
    const status: Record<string, string> = {
      'NO_REFUND': '未退款',
      'PARTIAL_REFUND': '部分退款',
      'FULL_REFUND': '全额退款'
    }
    return status[refundStatus.value] || refundStatus.value
  })

  const isRefunded = computed(() => {
    return refundStatus.value === 'FULL_REFUND' || orderStatus.value === 'REFUNDED'
  })

  const maxRefundAmount = computed(() => {
    return totalAmount.value - refundAmount.value
  })

  // ==================== 工具方法 ====================

  const formatPrice = (price: number): string => {
    if (price === undefined || price === null) return '0.00'
    return price.toFixed(2)
  }

  const formatDateTime = (dateStr: string): string => {
    if (!dateStr) return ''
    const date = new Date(dateStr)
    const year = date.getFullYear()
    const month = (date.getMonth() + 1).toString().padStart(2, '0')
    const day = date.getDate().toString().padStart(2, '0')
    const hours = date.getHours().toString().padStart(2, '0')
    const minutes = date.getMinutes().toString().padStart(2, '0')
    return `${year}-${month}-${day} ${hours}:${minutes}`
  }

  const copyOrderNumber = () => {
    navigator.clipboard.writeText(orderNumber.value)
    showToast({ message: '订单号已复制', type: 'success' })
  }

  const copyRefundTxId = () => {
    navigator.clipboard.writeText(refundTransactionId.value)
    showToast({ message: '退款交易号已复制', type: 'success' })
  }

  const goBack = () => {
    router.back()
  }

  const backToHome = () => {
    router.push({ name: 'UserDashboard' })
  }

  const goToPay = () => {
    router.push({
      name: 'UserPayment',
      query: {
        orderNumber: orderNumber.value,
        amount: totalAmount.value.toFixed(2)
      }
    })
  }

  const getProductEmoji = (name: string): string => {
    if (!name) return '🍖'
    if (name.includes('羊') || name.includes('牛') || name.includes('猪')) return '🥩'
    if (name.includes('鸡')) return '🍗'
    if (name.includes('鱼') || name.includes('虾')) return '🐟'
    if (name.includes('蔬菜')) return '🥬'
    if (name.includes('啤酒')) return '🍺'
    if (name.includes('主食') || name.includes('面')) return '🍚'
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
    if (refundAmountInput.value <= 0) {
      showToast({ message: '请输入退款金额', type: 'fail' })
      return
    }
    
    if (refundAmountInput.value > maxRefundAmount.value) {
      showToast({ message: '退款金额不能超过可退金额', type: 'fail' })
      return
    }
    
    if (!refundReasonInput.value.trim()) {
      showToast({ message: '请填写退款原因', type: 'fail' })
      return
    }
    
    try {
      await showConfirmDialog({
        title: '确认退款',
        message: `确定要申请退款 ¥${formatPrice(refundAmountInput.value)} 吗？`,
        confirmButtonText: '确认申请',
        cancelButtonText: '再想想'
      })
      
      submitting.value = true
      
      const response = await authAPI.refundOrder({
        orderNumber: orderNumber.value,
        refundAmount: refundAmountInput.value,
        refundReason: refundReasonInput.value
      })
      
      if (response.success) {
        showToast({ message: '退款申请已提交', type: 'success' })
        closeRefundModal()
        loadOrderDetail()
      } else {
        showToast({ message: response.message || '退款失败', type: 'fail' })
      }
    } catch (error) {
      // 用户取消操作
    } finally {
      submitting.value = false
    }
  }

  // ==================== 加载订单详情 ====================

  const loadOrderDetail = async () => {
    if (!orderNumber.value) {
      showToast({ message: '订单号不存在', type: 'fail' })
      router.back()
      return
    }
    
    loading.value = true
    
    try {
      // 响应拦截器已返回 response.data，直接使用 response
      const response = await authAPI.getOrderDetail(orderNumber.value)
      
      if (!response.success) {
        throw new Error(response.message || '获取订单详情失败')
      }
      
      const { order, orderItems: items } = response.data
      
      // 订单基本信息
      orderStatus.value = order.status || 'PENDING'
      orderTime.value = order.createdAt ? formatDateTime(order.createdAt) : ''
      paidAt.value = order.paidAt || ''
      paymentMethod.value = order.paymentMethod || ''
      transactionId.value = order.transactionId || ''
      orderRemark.value = order.remark || ''
      totalAmount.value = order.totalAmount || 0
      
      // 退款信息
      refundAmount.value = order.refundAmount || 0
      refundStatus.value = order.refundStatus || 'NO_REFUND'
      refundTime.value = order.refundTime || ''
      refundReason.value = order.refundReason || ''
      refundTransactionId.value = order.refundTransactionId || ''
      
      // 商品列表
      if (items && items.length > 0) {
        orderItems.value = items
        subtotal.value = items.reduce((sum, item) => sum + (item.price * item.quantity), 0)
      } else {
        subtotal.value = totalAmount.value
      }
      
      // 根据配送类型设置收货/用餐信息
      if (order.deliveryType === 'delivery') {
        addressInfo.value = {
          name: order.recipientName || '',
          phone: order.recipientPhone || '',
          address: order.detailAddress || ''
        }
      } else if (order.deliveryType === 'dinein') {
        diningInfo.value = {
          peopleCount: order.peopleCount || 1,
          tablePreference: order.tablePreference || ''
        }
      } else if (order.deliveryType === 'takeaway') {
        addressInfo.value = {
          name: order.recipientName || '',
          phone: order.recipientPhone || '',
          address: '到店自取'
        }
      }
      
    } catch (error) {
      console.error('加载订单详情失败:', error)
      showToast({ message: '加载失败，请重试', type: 'fail' })
      setTimeout(() => {
        router.push({ name: 'UserDashboard' })
      }, 2000)
    } finally {
      loading.value = false
    }
  }

  // ==================== 生命周期 ====================
  onMounted(() => {
    loadOrderDetail()
  })
</script>

<style scoped>
@import url('@/static/css/user/订单详情页.css');
</style>