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
      <div class="status-card" :class="orderStatus">
        <div class="status-icon">{{ statusIcon }}</div>
        <div class="status-info">
          <h3 class="status-title">{{ statusText }}</h3>
          <p class="status-time">下单时间：{{ orderTime }}</p>
          <p v-if="paidAt" class="status-time">支付时间：{{ formatDateTime(paidAt) }}</p>
          <p v-if="refundTime" class="status-time">退款时间：{{ formatDateTime(refundTime) }}</p>
        </div>
      </div>

      <!-- 订单信息卡片 -->
      <div class="info-card">
        <h3 class="card-title">订单信息</h3>
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
        <div class="info-row">
          <span class="info-label">配送方式</span>
          <span class="info-value">{{ deliveryTypeText }}</span>
        </div>
        <div class="info-row" v-if="orderRemark">
          <span class="info-label">订单备注</span>
          <span class="info-value remark">{{ orderRemark }}</span>
        </div>
      </div>

      <!-- 收货/用餐信息卡片 -->
      <div class="info-card" v-if="addressInfo || diningInfo">
        <h3 class="card-title">{{ addressInfo ? '收货信息' : '用餐信息' }}</h3>
        <template v-if="addressInfo">
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
        </template>
        <template v-else-if="diningInfo">
          <div class="info-row">
            <span class="info-label">用餐人数</span>
            <span class="info-value">{{ diningInfo.peopleCount }}人</span>
          </div>
          <div class="info-row" v-if="diningInfo.tablePreference">
            <span class="info-label">桌号偏好</span>
            <span class="info-value">{{ diningInfo.tablePreference }}</span>
          </div>
        </template>
      </div>

      <!-- 退款信息卡片 -->
      <div class="info-card refund-card" v-if="refundAmount > 0">
        <h3 class="card-title">退款信息</h3>
        <div class="info-row">
          <span class="info-label">退款金额</span>
          <span class="info-value">¥{{ formatPrice(refundAmount) }}</span>
        </div>
        <div class="info-row">
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
        <h3 class="card-title">费用明细</h3>
        <div class="fee-row">
          <span>商品合计</span>
          <span>¥{{ formatPrice(subtotal) }}</span>
        </div>
        <div class="fee-row total">
          <span>实付金额</span>
          <span>¥{{ formatPrice(totalAmount) }}</span>
        </div>
      </div>

      <!-- 底部操作栏 -->
      <div class="action-bar">
        <button class="action-btn back-btn" @click="goBack">
          返回列表
        </button>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
  import { ref, computed, onMounted } from 'vue'
  import { useRouter, useRoute } from 'vue-router'
  import { showToast } from 'vant'
  import { authAPI } from '@/api/authAPI'
  import 'vant/es/toast/style'

  // ==================== 类型定义 ====================
  interface OrderItem {
    id: number
    productId: number
    productName: string
    price: number
    quantity: number
    image?: string
  }

  interface OrderDetail {
    id: number
    orderNumber: string
    userId: number
    totalAmount: number
    status: string
    paymentMethod: string
    transactionId: string
    paidAt: string
    deliveryType: string
    remark: string
    createdAt: string
    refundAmount: number
    refundStatus: string
    refundTime: string
    refundReason: string
    refundTransactionId: string
    recipientName: string
    recipientPhone: string
    detailAddress: string
    peopleCount: number
    tablePreference: string
  }

  // ==================== 路由 ====================
  const router = useRouter()
  const route = useRoute()

  // ==================== 响应式数据 ====================
  const loading = ref(false)
  const orderNumber = ref(route.query.orderNumber as string || '')
  const orderData = ref<OrderDetail | null>(null)
  const orderItems = ref<OrderItem[]>([])

  // ==================== 计算属性 ====================

  const orderStatus = computed(() => orderData.value?.status || 'PENDING')
  const orderTime = computed(() => orderData.value?.createdAt ? formatDateTime(orderData.value.createdAt) : '')
  const paidAt = computed(() => orderData.value?.paidAt || '')
  const paymentMethod = computed(() => orderData.value?.paymentMethod || '')
  const transactionId = computed(() => orderData.value?.transactionId || '')
  const deliveryType = computed(() => orderData.value?.deliveryType || '')
  const orderRemark = computed(() => orderData.value?.remark || '')
  const totalAmount = computed(() => orderData.value?.totalAmount || 0)

  const refundAmount = computed(() => orderData.value?.refundAmount || 0)
  const refundStatus = computed(() => orderData.value?.refundStatus || 'NO_REFUND')
  const refundTime = computed(() => orderData.value?.refundTime || '')
  const refundReason = computed(() => orderData.value?.refundReason || '')
  const refundTransactionId = computed(() => orderData.value?.refundTransactionId || '')

  const subtotal = computed(() => {
    return orderItems.value.reduce((sum, item) => sum + (item.price * item.quantity), 0)
  })

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
      'CASH': '现金支付'
    }
    return methods[paymentMethod.value] || paymentMethod.value || '未支付'
  })

  const deliveryTypeText = computed(() => {
    const types: Record<string, string> = {
      'dinein': '到店用餐',
      'takeaway': '打包自取',
      'delivery': '外卖配送'
    }
    return types[deliveryType.value] || deliveryType.value
  })

  const refundStatusText = computed(() => {
    const status: Record<string, string> = {
      'NO_REFUND': '未退款',
      'PARTIAL_REFUND': '部分退款',
      'FULL_REFUND': '全额退款'
    }
    return status[refundStatus.value] || refundStatus.value
  })

  const addressInfo = computed(() => {
    if (deliveryType.value === 'delivery' && orderData.value) {
      return {
        name: orderData.value.recipientName || '',
        phone: orderData.value.recipientPhone || '',
        address: orderData.value.detailAddress || ''
      }
    }
    if (deliveryType.value === 'takeaway' && orderData.value) {
      return {
        name: orderData.value.recipientName || '',
        phone: orderData.value.recipientPhone || '',
        address: '到店自取'
      }
    }
    return null
  })

  const diningInfo = computed(() => {
    if (deliveryType.value === 'dinein' && orderData.value) {
      return {
        peopleCount: orderData.value.peopleCount || 1,
        tablePreference: orderData.value.tablePreference || ''
      }
    }
    return null
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

  /**
   * 根据商品名称获取默认表情
   */
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

  /**
   * 图片加载失败处理
   */
  const handleImageError = (event: Event) => {
    const img = event.target as HTMLImageElement
    img.style.display = 'none'
    const placeholder = document.createElement('div')
    placeholder.className = 'image-placeholder'
    placeholder.innerHTML = '<span class="placeholder-emoji">🍖</span>'
    img.parentNode?.appendChild(placeholder)
  }

  // ==================== 业务方法 ====================

  /**
   * 加载订单详情
   * 注意：响应拦截器已返回 response.data，所以直接使用 data
   */
  const loadOrderDetail = async () => {
    if (!orderNumber.value) {
      showToast({ message: '订单号不存在', type: 'fail' })
      router.back()
      return
    }
    
    loading.value = true
    
    try {
      const response = await authAPI.getOrderDetail(orderNumber.value)
      
      // 检查业务状态
      if (!response.success) {
        throw new Error(response.message || '获取订单详情失败')
      }
      
      // 获取订单数据和订单项
      const { order, orderItems: items } = response.data
      
      orderData.value = order
      orderItems.value = items || []
      
    } catch (error) {
      console.error('加载订单详情失败:', error)
      showToast({ message: '加载失败，请重试', type: 'fail' })
      setTimeout(() => {
        router.back()
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
@import url('@/static/css/seller/订单详情页.css');
</style>