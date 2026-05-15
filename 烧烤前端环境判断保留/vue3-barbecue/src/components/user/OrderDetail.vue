<template>
  <div class="order-detail-container black-theme">
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
        </div>
      </div>

      <!-- 订单信息卡片 -->
      <div class="info-card">
        <div class="info-row">
          <span class="info-label">订单编号</span>
          <span class="info-value">{{ orderNumber }}</span>
          <button class="copy-btn" @click="copyorderNumber">复制</button>
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
        <div 
          v-for="item in orderItems" 
          :key="item.id"
          class="order-item"
        >
          <div class="item-image">
            <img v-if="item.image" :src="item.image" :alt="item.name" />
            <span v-else class="item-placeholder">🥩</span>
          </div>
          <div class="item-info">
            <div class="item-name">{{ item.name }}</div>
            <div class="item-spec">{{ item.spec || '' }}</div>
          </div>
          <div class="item-price">
            <div class="price">¥{{ formatPrice(item.price) }}</div>
            <div class="quantity">x{{ item.quantity }}</div>
          </div>
        </div>
      </div>

      <!-- 费用明细 -->
      <div class="fee-card">
        <div class="fee-row">
          <span>商品合计</span>
          <span>¥{{ formatPrice(subtotal) }}</span>
        </div>
        <div class="fee-row" v-if="packagingFee > 0">
          <span>包装费</span>
          <span>¥{{ formatPrice(packagingFee) }}</span>
        </div>
        <div class="fee-row" v-if="deliveryFee > 0">
          <span>配送费</span>
          <span>¥{{ formatPrice(deliveryFee) }}</span>
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
          class="action-btn primary"
          @click="contactShop"
        >
          联系商家
        </button>
        <button 
          class="action-btn secondary"
          @click="backToHome"
        >
          返回首页
        </button>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
  import { ref, computed, onMounted } from 'vue'
  import { useRouter, useRoute } from 'vue-router'
  import { showToast } from 'vant'
  import 'vant/es/toast/style'
  import { authAPI } from '@/api/auth'

  // ==================== 类型定义 ====================
  interface OrderItem {
    id: number
    name: string
    price: number
    quantity: number
    image?: string
    spec?: string
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
  const orderNumber = ref(route.query.orderNumber as string || '')
  const orderStatus = ref('PENDING')
  const orderTime = ref('')
  const paymentMethod = ref('')
  const transactionId = ref('')
  const orderItems = ref<OrderItem[]>([])
  const addressInfo = ref<AddressInfo | null>(null)
  const diningInfo = ref<DiningInfo | null>(null)
  const orderRemark = ref('')

  // 费用相关
  const subtotal = ref(0)
  const packagingFee = ref(2.00)
  const deliveryFee = ref(0)
  const totalAmount = ref(0)

  // ==================== 计算属性 ====================

  const statusIcon = computed(() => {
    const icons: Record<string, string> = {
      'PAID': '✅',
      'PENDING': '⏳',
      'CANCELLED': '❌'
    }
    return icons[orderStatus.value] || '📦'
  })

  const statusText = computed(() => {
    const texts: Record<string, string> = {
      'PAID': '支付成功',
      'PENDING': '待支付',
      'CANCELLED': '已取消'
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

  // ==================== 工具方法 ====================

  const formatPrice = (price: number): string => {
    return price.toFixed(2)
  }

  const copyorderNumber = (): void => {
    navigator.clipboard.writeText(orderNumber.value).then(() => {
      showToast('订单号已复制')
    }).catch(() => {
      showToast('复制失败')
    })
  }

  const goBack = (): void => {
    router.back()
  }

  const backToHome = (): void => {
    router.push({ name: 'UserDashboard' })
  }

  const goToPay = (): void => {
    router.push({
      name: 'UserPayment',
      query: {
        orderNumber: orderNumber.value,
        amount: totalAmount.value.toFixed(2)
      }
    })
  }

  const contactShop = (): void => {
    showToast('商家电话: 400-888-8888')
  }

  // ==================== 获取订单详情 ====================

  /**
   * 加载订单详情 - 从后端获取真实数据
   */
  const loadOrderDetail = async (): Promise<void> => {
    if (!orderNumber.value) {
      showToast('订单号不存在')
      router.back()
      return
    }
    
    loading.value = true
    
    try {
      // 调用后端接口获取订单详情
      const response = await authAPI.getOrderDetail(orderNumber.value)
      const data = response.data || response
      
      if (!data.success) {
        throw new Error(data.message || '获取订单详情失败')
      }
      
      // 解析订单数据
      const order = data.order
      
      orderStatus.value = order.status || 'PENDING'
      orderTime.value = order.createdAt ? new Date(order.createdAt).toLocaleString() : ''
      paymentMethod.value = order.paymentMethod || ''
      transactionId.value = order.transactionId || ''
      totalAmount.value = order.totalAmount || 0
      orderRemark.value = order.remark || ''
      
      // 计算商品合计
      if (data.orderItems && data.orderItems.length > 0) {
        orderItems.value = data.orderItems.map((item: any) => ({
          id: item.productId,
          name: item.productName,
          price: item.price,
          quantity: item.quantity,
          image: item.productImage,
          spec: ''
        }))
        
        subtotal.value = orderItems.value.reduce((sum, item) => sum + (item.price * item.quantity), 0)
      } else {
        // 如果没有订单项，用总金额减去费用
        subtotal.value = totalAmount.value - packagingFee.value - deliveryFee.value
      }
      
      // 根据配送类型设置收货信息或用餐信息
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
      
      console.log('订单详情加载成功:', order)
      
    } catch (error) {
      console.error('加载订单详情失败:', error)
      showToast('加载失败，请重试')
      // 3秒后返回首页
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
  .order-detail-container {
    min-height: 100vh;
    background-color: #121212;
    color: #fff;
    padding-bottom: 80px;
  }

  .detail-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 16px 20px;
    background-color: #1e1e1e;
    border-bottom: 1px solid #333;
    position: sticky;
    top: 0;
    z-index: 10;
  }

  .back-btn {
    background: none;
    border: none;
    color: #fff;
    font-size: 24px;
    cursor: pointer;
    padding: 0 8px;
  }

  .header-title {
    font-size: 18px;
    font-weight: 500;
    margin: 0;
  }

  .header-placeholder {
    width: 40px;
  }

  .loading-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    min-height: 300px;
    gap: 16px;
  }

  .loading-spinner {
    width: 40px;
    height: 40px;
    border: 3px solid #333;
    border-top-color: #ff6b6b;
    border-radius: 50%;
    animation: spin 1s linear infinite;
  }

  .status-card {
    display: flex;
    align-items: center;
    gap: 16px;
    margin: 16px;
    padding: 20px;
    background-color: #1e1e1e;
    border-radius: 12px;
    border-left: 4px solid;
  }

  .status-card.PAID { border-left-color: #4caf50; }
  .status-card.PENDING { border-left-color: #ff9800; }
  .status-card.CANCELLED { border-left-color: #f44336; }

  .status-icon { font-size: 48px; }
  .status-info { flex: 1; }
  .status-title { font-size: 18px; font-weight: 600; margin: 0 0 4px 0; }
  .status-time { font-size: 12px; color: #999; margin: 0; }

  .info-card, .product-card, .fee-card {
    background-color: #1e1e1e;
    border-radius: 12px;
    margin: 16px;
    padding: 16px;
    border: 1px solid #333;
  }

  .card-title {
    font-size: 16px;
    font-weight: 600;
    margin: 0 0 12px 0;
    padding-bottom: 8px;
    border-bottom: 1px solid #333;
  }

  .info-row {
    display: flex;
    align-items: center;
    padding: 8px 0;
    font-size: 14px;
  }

  .info-label {
    width: 80px;
    color: #999;
  }

  .info-value {
    flex: 1;
    color: #fff;
  }

  .copy-btn {
    background: none;
    border: 1px solid #ff6b6b;
    color: #ff6b6b;
    font-size: 12px;
    padding: 4px 8px;
    border-radius: 4px;
    cursor: pointer;
  }

  .order-item {
    display: flex;
    align-items: center;
    padding: 12px 0;
    border-bottom: 1px solid #333;
  }

  .order-item:last-child { border-bottom: none; }

  .item-image {
    width: 60px;
    height: 60px;
    background-color: #2a2a2a;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: 12px;
  }

  .item-image img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    border-radius: 8px;
  }

  .item-placeholder { font-size: 30px; }
  .item-info { flex: 1; }
  .item-name { font-size: 14px; font-weight: 500; margin-bottom: 4px; }
  .item-spec { font-size: 12px; color: #999; }
  .item-price { text-align: right; }
  .price { font-size: 14px; font-weight: 500; color: #ff6b6b; }
  .quantity { font-size: 12px; color: #999; margin-top: 4px; }

  .fee-row {
    display: flex;
    justify-content: space-between;
    padding: 8px 0;
    font-size: 14px;
    color: #999;
  }

  .fee-row.total {
    margin-top: 8px;
    padding-top: 12px;
    border-top: 1px solid #333;
    font-size: 16px;
    font-weight: 600;
    color: #ff6b6b;
  }

  .fee-note {
    margin-top: 12px;
    padding-top: 8px;
    font-size: 12px;
    color: #999;
    border-top: 1px solid #333;
  }

  .detail-footer {
    position: fixed;
    bottom: 0;
    left: 0;
    right: 0;
    display: flex;
    gap: 12px;
    padding: 16px 20px;
    background-color: #1e1e1e;
    border-top: 1px solid #333;
  }

  .action-btn {
    flex: 1;
    padding: 12px;
    border-radius: 8px;
    font-size: 16px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.3s;
    border: none;
  }

  .action-btn.primary { background: #ff6b6b; color: #fff; }
  .action-btn.warning { background: #ff9800; color: #fff; }
  .action-btn.secondary { background: none; border: 1px solid #333; color: #fff; }
  .action-btn:hover { transform: translateY(-2px); }

  @keyframes spin {
    to { transform: rotate(360deg); }
  }
</style>