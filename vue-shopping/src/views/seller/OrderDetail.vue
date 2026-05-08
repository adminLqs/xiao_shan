<template>
  <div class="order-detail-page">
    <!-- ========== 页面头部区域 ========== -->
    <div class="page-header">
      <button class="back-btn" @click="goBack">
        <i class="fas fa-arrow-left"></i>
        返回
      </button>
      <h1 class="page-title">
        <i class="fas fa-file-alt"></i>
        订单详情
      </h1>
    </div>

    <!-- ========== 加载状态 ========== -->
    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>

    <!-- ========== 订单详情内容 ========== -->
    <div v-else-if="orderData" class="detail-content">
      
      <!-- ========== 订单状态栏 ========== -->
      <div class="status-bar" :class="getStatusClass(orderData.status)">
        <div class="status-icon">
          <i :class="getStatusIcon(orderData.status)"></i>
        </div>
        <div class="status-info">
          <div class="status-text">{{ getStatusText(orderData.status) }}</div>
          <div class="status-desc">{{ getStatusDesc(orderData.status) }}</div>
        </div>
        <div class="status-time">
          <span v-if="orderData.paidAt">支付时间：{{ formatDateTime(orderData.paidAt) }}</span>
          <span v-if="orderData.shippedAt">发货时间：{{ formatDateTime(orderData.shippedAt) }}</span>
        </div>
      </div>

      <!-- ========== 订单信息卡片 ========== -->
      <div class="detail-card">
        <div class="card-header">
          <i class="fas fa-receipt"></i>
          <span class="card-title">订单信息</span>
        </div>
        <div class="card-body">
          <div class="info-grid">
            <div class="info-item">
              <span class="info-label">订单号：</span>
              <span class="info-value">{{ orderData.orderNumber }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">订单状态：</span>
              <span class="info-value status-text" :class="getStatusClass(orderData.status)">
                {{ getStatusText(orderData.status) }}
              </span>
            </div>
            <div class="info-item">
              <span class="info-label">创建时间：</span>
              <span class="info-value">{{ formatDateTime(orderData.createdAt) }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">支付方式：</span>
              <span class="info-value">{{ getPaymentMethodText(orderData.paymentMethod) }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">交易单号：</span>
              <span class="info-value">{{ orderData.transactionId || '-' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">订单来源：</span>
              <span class="info-value">{{ orderData.source === 'cart' ? '购物车' : '直接购买' }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- ========== 收货信息卡片 ========== -->
      <div class="detail-card">
        <div class="card-header">
          <i class="fas fa-map-marker-alt"></i>
          <span class="card-title">收货信息</span>
        </div>
        <div class="card-body">
          <div class="address-info" v-if="addressData">
            <div class="address-line">
              <span class="address-label">收件人：</span>
              <span>{{ addressData.recipientName }}</span>
            </div>
            <div class="address-line">
              <span class="address-label">联系电话：</span>
              <span>{{ addressData.recipientPhone }}</span>
            </div>
            <div class="address-line">
              <span class="address-label">收货地址：</span>
              <span>{{ formatAddress(addressData) }}</span>
            </div>
            <div class="address-line" v-if="addressData.label">
              <span class="address-label">地址标签：</span>
              <span class="address-tag">{{ addressData.label }}</span>
            </div>
          </div>
          <div v-else class="no-address">
            <i class="fas fa-exclamation-triangle"></i>
            <span>暂无收货地址信息</span>
          </div>
        </div>
      </div>

      <!-- ========== 物流信息卡片 ========== -->
      <div class="detail-card" v-if="orderData.status === 'SHIPPED' || orderData.status === 'DELIVERED' || orderData.status === 'COMPLETED'">
        <div class="card-header">
          <i class="fas fa-truck"></i>
          <span class="card-title">物流信息</span>
          <button class="btn-link" @click="viewLogistics">查看物流详情</button>
        </div>
        <div class="card-body">
          <div class="logistics-info">
            <div class="logistics-item">
              <span class="logistics-label">物流公司：</span>
              <span class="logistics-value">{{ orderData.logisticsName || '-' }}</span>
            </div>
            <div class="logistics-item">
              <span class="logistics-label">物流单号：</span>
              <span class="logistics-value">{{ orderData.trackingNumber || '-' }}</span>
            </div>
            <div class="logistics-item">
              <span class="logistics-label">发货时间：</span>
              <span class="logistics-value">{{ formatDateTime(orderData.shippedAt) }}</span>
            </div>
            <div class="logistics-item" v-if="orderData.deliveredAt">
              <span class="logistics-label">签收时间：</span>
              <span class="logistics-value">{{ formatDateTime(orderData.deliveredAt) }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- ========== 商品列表卡片 ========== -->
      <div class="detail-card">
        <div class="card-header">
          <i class="fas fa-boxes"></i>
          <span class="card-title">商品清单</span>
        </div>
        <div class="card-body no-padding">
          <div class="product-table">
            <div class="table-header">
              <div class="col-product">商品信息</div>
              <div class="col-price">单价</div>
              <div class="col-quantity">数量</div>
              <div class="col-total">小计</div>
            </div>
            
            <div v-for="item in orderItems" :key="item.id" class="table-row">
              <div class="col-product">
                <img :src="item.productImage || '/images/default-product.png'" class="product-image">
                <div class="product-info">
                  <div class="product-name">{{ item.productName }}</div>
                  <div class="product-id">商品ID：{{ item.productId }}</div>
                </div>
              </div>
              <div class="col-price">¥{{ formatPrice(item.price) }}</div>
              <div class="col-quantity">x{{ item.quantity }}</div>
              <div class="col-total">¥{{ formatPrice(item.totalPrice || item.price * item.quantity) }}</div>
            </div>
            
            <div class="table-footer">
              <div class="amount-row">
                <span class="amount-label">商品总额：</span>
                <span class="amount-value">¥{{ formatPrice(orderData.totalAmount) }}</span>
              </div>
              <div class="amount-row">
                <span class="amount-label">运费：</span>
                <span class="amount-value">¥0.00</span>
              </div>
              <div class="amount-row total-row">
                <span class="amount-label">实付款：</span>
                <span class="amount-value total-price">¥{{ formatPrice(orderData.totalAmount) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- ========== 操作按钮区 ========== -->
      <div class="action-bar">
        <button class="btn-outline" @click="goBack">返回</button>
        <button v-if="orderData.status === 'PENDING'" class="btn-danger" @click="cancelOrder">取消订单</button>
        <button v-if="orderData.status === 'PAID'" class="btn-primary" @click="processOrder">处理订单</button>
        <button v-if="orderData.status === 'PROCESSING'" class="btn-primary" @click="openShipDialog">发货</button>
        <button v-if="orderData.status === 'SHIPPED'" class="btn-outline" @click="viewLogistics">查看物流</button>
      </div>
    </div>

    <!-- ========== 订单不存在状态 ========== -->
    <div v-else class="empty-state">
      <i class="fas fa-box-open"></i>
      <p>订单不存在</p>
      <button class="btn-primary" @click="goBack">返回订单列表</button>
    </div>

    <!-- ========== 发货弹窗 ========== -->
    <div v-if="showShipDialog" class="modal-overlay" @click="closeShipDialog">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>发货</h3>
          <button class="modal-close" @click="closeShipDialog">
            <i class="fas fa-times"></i>
          </button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label class="form-label required">物流单号</label>
            <input 
              type="text" 
              v-model="shipForm.trackingNumber" 
              class="form-control"
              placeholder="请输入物流单号"
            >
          </div>
          <div class="form-group">
            <label class="form-label required">物流公司</label>
            <select v-model="shipForm.logisticsCode" class="form-control">
              <option value="">请选择物流公司</option>
              <option value="SF">顺丰速运</option>
              <option value="YTO">圆通速递</option>
              <option value="ZTO">中通快递</option>
              <option value="EMS">邮政EMS</option>
              <option value="YD">韵达快递</option>
              <option value="STO">申通快递</option>
              <option value="JT">极兔速递</option>
            </select>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-cancel" @click="closeShipDialog">取消</button>
          <button class="btn-confirm" @click="confirmShip" :disabled="submitting">
            {{ submitting ? '发货中...' : '确认发货' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { ref, onMounted } from 'vue'
  import { useRoute, useRouter } from 'vue-router'
  import { authAPI } from '@/api/authAPI'
  import { showToast, showConfirmDialog } from 'vant'
  import 'vant/es/toast/style'
  import 'vant/es/dialog/style'
  import { useAuthStore } from '@/stores/auth'

  const route = useRoute()
  const router = useRouter()
  const authStore = useAuthStore()

  // ==================== 类型定义 ====================

  type OrderStatus = 'PENDING' | 'PAID' | 'PROCESSING' | 'SHIPPED' | 'DELIVERED' | 'COMPLETED' | 'CANCELLED' | 'REFUNDED'
  type PaymentMethod = 'ALIPAY' | 'WECHAT'
  type OrderSource = 'cart' | 'product'

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
    createdAt: string
    updatedAt: string
  }

  interface OrderItem {
    id: number
    orderId: number
    productId: number
    sellerId: number
    productName: string
    productImage: string
    quantity: number
    price: number
    totalPrice: number
    isReviewed: boolean
    reviewedAt: string | null
    createdAt: string
  }

  interface Order {
    id: number
    orderNumber: string
    userId: number
    totalAmount: number
    status: OrderStatus
    source: OrderSource
    addressId: number
    paymentMethod: PaymentMethod | null
    transactionId: string | null
    paidAt: string | null
    trackingNumber: string | null
    logisticsCode: string | null
    logisticsName: string | null
    shippedAt: string | null
    deliveredAt: string | null
    completedAt: string | null
    createdAt: string
    updatedAt: string
    isDeleted: boolean
  }

  interface OrderDetailData {
    order: Order
    orderItems: OrderItem[]
    address: Address
  }

  interface ShipForm {
    trackingNumber: string
    logisticsCode: string
  }

  interface StatusConfig {
    text: string
    desc: string
    icon: string
    class: string
  }

  // ==================== 响应式数据 ====================

  const loading = ref(false)
  const submitting = ref(false)
  const showShipDialog = ref(false)

  const orderData = ref<Order | null>(null)
  const orderItems = ref<OrderItem[]>([])
  const addressData = ref<Address | null>(null)

  const shipForm = ref<ShipForm>({
    trackingNumber: '',
    logisticsCode: ''
  })

  // ==================== 静态配置 ====================

  const logisticsMap: Record<string, string> = {
    SF: '顺丰速运',
    YTO: '圆通速递',
    ZTO: '中通快递',
    EMS: '邮政EMS',
    YD: '韵达快递',
    STO: '申通快递',
    JT: '极兔速递'
  }

  const statusConfig: Record<OrderStatus, StatusConfig> = {
    PENDING: {
      text: '待付款',
      desc: '等待买家付款',
      icon: 'fas fa-clock',
      class: 'status-pending'
    },
    PAID: {
      text: '已付款',
      desc: '买家已付款，请及时处理',
      icon: 'fas fa-check-circle',
      class: 'status-paid'
    },
    PROCESSING: {
      text: '处理中',
      desc: '订单已确认，正在备货中',
      icon: 'fas fa-spinner',
      class: 'status-processing'
    },
    SHIPPED: {
      text: '已发货',
      desc: '商品已发出，等待买家收货',
      icon: 'fas fa-truck',
      class: 'status-shipped'
    },
    DELIVERED: {
      text: '已送达',
      desc: '商品已送达，请等待买家确认',
      icon: 'fas fa-home',
      class: 'status-delivered'
    },
    COMPLETED: {
      text: '已完成',
      desc: '订单已完成，感谢您的支持',
      icon: 'fas fa-check-double',
      class: 'status-completed'
    },
    CANCELLED: {
      text: '已取消',
      desc: '订单已取消',
      icon: 'fas fa-times-circle',
      class: 'status-cancelled'
    },
    REFUNDED: {
      text: '已退款',
      desc: '订单已退款',
      icon: 'fas fa-undo-alt',
      class: 'status-refunded'
    }
  }

  const paymentMethodMap: Record<PaymentMethod, string> = {
    ALIPAY: '支付宝',
    WECHAT: '微信支付'
  }

  // ==================== 数据加载 ====================

  /**
   * 加载订单详情
   * @returns {Promise<void>}
   */
  const loadOrderDetail = async (): Promise<void> => {
    loading.value = true

    try {
      const orderId = route.params.id

      if (!orderId) {
        showToast({ message: '订单ID不存在', type: 'fail' })
        router.push({ name: 'SellerOrders' })
        return
      }

      const response = await authAPI.getSellerOrderDetail(Number(orderId))

      if (response.success && response.data) {
        const data = response.data as OrderDetailData

        orderData.value = data.order
        orderItems.value = data.orderItems.map(item => ({
          ...item,
          totalPrice: item.price * item.quantity
        }))
        addressData.value = data.address
        console.log(orderData.value)
      } else {
        showToast({ message: response.message || '获取订单详情失败', type: 'fail' })
      }
    } catch (error: any) {
      console.error('加载订单详情失败:', error)
      showToast({ message: error.message || '加载失败', type: 'fail' })
    } finally {
      loading.value = false
    }
  }

  // ==================== 订单操作 ====================

  /**
   * 处理订单（PAID → PROCESSING）
   * @returns {Promise<void>}
   */
  const processOrder = async (): Promise<void> => {
    try {
      await showConfirmDialog({
        title: '处理订单',
        message: '确定要处理该订单吗？处理后将进入备货状态。',
        confirmButtonText: '确定',
        cancelButtonText: '取消'
      })

      const response = await authAPI.processOrder(orderData.value!.id)

      if (response.success) {
        showToast({ message: '订单已处理', type: 'success' })
        await loadOrderDetail()
      }
    } catch (error: any) {
      if (error !== 'cancel') {
        showToast({ message: error.message || '操作失败', type: 'fail' })
      }
    }
  }

  /**
   * 取消订单
   * @returns {Promise<void>}
   */
  const cancelOrder = async (): Promise<void> => {
    try {
      await showConfirmDialog({
        title: '取消订单',
        message: '确定要取消该订单吗？',
        confirmButtonText: '确定',
        cancelButtonText: '取消'
      })

      const response = await authAPI.cancelOrder(orderData.value!.id)

      if (response.success) {
        showToast({ message: '取消成功', type: 'success' })
        await loadOrderDetail()
      }
    } catch (error: any) {
      if (error !== 'cancel') {
        showToast({ message: error.message || '操作失败', type: 'fail' })
      }
    }
  }

  // ==================== 发货相关 ====================

  const openShipDialog = (): void => {
    shipForm.value = { trackingNumber: '', logisticsCode: '' }
    showShipDialog.value = true
  }

  const closeShipDialog = (): void => {
    showShipDialog.value = false
    shipForm.value = { trackingNumber: '', logisticsCode: '' }
  }

  /**
   * 确认发货
   * @returns {Promise<void>}
   */
  const confirmShip = async (): Promise<void> => {
    if (!shipForm.value.trackingNumber.trim()) {
      showToast({ message: '请输入物流单号', type: 'fail' })
      return
    }

    if (!shipForm.value.logisticsCode) {
      showToast({ message: '请选择物流公司', type: 'fail' })
      return
    }

    submitting.value = true

    try {
      const data = {
        trackingNumber: shipForm.value.trackingNumber,
        logisticsCode: shipForm.value.logisticsCode,
        logisticsName: logisticsMap[shipForm.value.logisticsCode]
      }

      const response = await authAPI.shipOrder(orderData.value?.id, data)

      if (response.success) {
        showToast({ message: '发货成功', type: 'success' })
        closeShipDialog()
        await loadOrderDetail()
      }
    } catch (error: any) {
      showToast({ message: error.message || '发货失败', type: 'fail' })
    } finally {
      submitting.value = false
    }
  }

  // ==================== 页面跳转 ====================

  const viewLogistics = (): void => {
    router.push({
      name: 'SellerLogistics',
      query: { orderId: orderData.value?.id }
    })
  }

  const goBack = (): void => {
    router.back()
  }

  // ==================== 工具函数 ====================

  const getStatusConfig = (status: OrderStatus): StatusConfig => {
    return statusConfig[status] || statusConfig.PENDING
  }

  const getStatusClass = (status: OrderStatus): string => {
    return getStatusConfig(status).class
  }

  const getStatusText = (status: OrderStatus): string => {
    return getStatusConfig(status).text
  }

  const getStatusDesc = (status: OrderStatus): string => {
    return getStatusConfig(status).desc
  }

  const getStatusIcon = (status: OrderStatus): string => {
    return getStatusConfig(status).icon
  }

  const getPaymentMethodText = (method: PaymentMethod | null): string => {
    if (!method) return '-'
    return paymentMethodMap[method] || method
  }

  const formatPrice = (price: number): string => {
    if (price == null) return '0.00'
    return price.toFixed(2)
  }

  const formatDateTime = (dateStr: string | null): string => {
    if (!dateStr) return '-'
    try {
      const date = new Date(dateStr)
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      const hours = String(date.getHours()).padStart(2, '0')
      const minutes = String(date.getMinutes()).padStart(2, '0')
      return `${year}-${month}-${day} ${hours}:${minutes}`
    } catch {
      return '-'
    }
  }

  const formatAddress = (address: Address | null): string => {
    if (!address) return '-'
    const parts = [address.province, address.city, address.district, address.detailAddress]
    return parts.filter(p => p?.trim()).join(' ')
  }

  // ==================== 生命周期 ====================

  onMounted(async () => {
    if (!authStore.validateSellerPermission()) return
    await loadOrderDetail()
  })
</script>

<style scoped>
/* ========== 页面容器 ========== */
/* 订单详情页面容器样式 */
.order-detail-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  min-height: 100vh;
  background: #f5f7fa;
}

/* ========== 页面头部 ========== */
/* 页面头部区域样式 */
.page-header {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 24px;
}

/* 返回按钮样式 */
.back-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  color: #64748b;
  cursor: pointer;
  transition: all 0.3s;
}

/* 返回按钮悬停效果 */
.back-btn:hover {
  background: #f8fafc;
  color: #4a6491;
}

/* 页面标题样式 */
.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #1e293b;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 10px;
}

/* 标题图标颜色 */
.page-title i {
  color: #4a6491;
}

/* ========== 加载状态 ========== */
/* 加载状态容器 */
.loading-state {
  text-align: center;
  padding: 80px;
}

/* 加载动画旋转图标 */
.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid #e2e8f0;
  border-top-color: #4a6491;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 16px;
}

/* 旋转动画关键帧 */
@keyframes spin {
  to { transform: rotate(360deg); }
}

/* ========== 空状态 ========== */
/* 空状态容器（订单不存在时） */
.empty-state {
  text-align: center;
  padding: 80px;
  background: white;
  border-radius: 12px;
}

/* 空状态图标 */
.empty-state i {
  font-size: 64px;
  color: #cbd5e1;
  margin-bottom: 16px;
}

/* 空状态文字 */
.empty-state p {
  color: #64748b;
  margin-bottom: 24px;
}

/* ========== 状态栏 ========== */
/* 订单状态栏样式 */
.status-bar {
  background: white;
  border-radius: 12px;
  padding: 20px 24px;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  gap: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  border-left: 4px solid;
}

/* 状态栏不同状态的颜色 */
.status-bar.status-pending { border-left-color: #f59e0b; }
.status-bar.status-paid { border-left-color: #3b82f6; }
.status-bar.status-processing { border-left-color: #8b5cf6; }
.status-bar.status-shipped { border-left-color: #06b6d4; }
.status-bar.status-delivered,
.status-bar.status-completed { border-left-color: #10b981; }
.status-bar.status-cancelled,
.status-bar.status-refunded { border-left-color: #ef4444; }

/* 状态图标 */
.status-icon i {
  font-size: 32px;
}

/* 状态图标不同状态的颜色 */
.status-bar.status-pending .status-icon i { color: #f59e0b; }
.status-bar.status-paid .status-icon i { color: #3b82f6; }
.status-bar.status-processing .status-icon i { color: #8b5cf6; }
.status-bar.status-shipped .status-icon i { color: #06b6d4; }
.status-bar.status-delivered .status-icon i,
.status-bar.status-completed .status-icon i { color: #10b981; }

/* 状态文字信息区域 */
.status-info {
  flex: 1;
}

/* 状态主文字 */
.status-text {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 4px;
}

/* 状态描述文字 */
.status-desc {
  font-size: 14px;
  color: #64748b;
}

/* 状态时间区域 */
.status-time {
  font-size: 14px;
  color: #64748b;
  display: flex;
  flex-direction: column;
  gap: 4px;
  text-align: right;
}

/* ========== 详情卡片 ========== */
/* 详情卡片容器 */
.detail-card {
  background: white;
  border-radius: 12px;
  margin-bottom: 20px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

/* 卡片头部样式 */
.card-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 20px;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
}

/* 卡片头部图标 */
.card-header i {
  font-size: 18px;
  color: #4a6491;
}

/* 卡片标题 */
.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
  flex: 1;
}

/* 链接按钮样式 */
.btn-link {
  background: none;
  border: none;
  color: #4a6491;
  cursor: pointer;
  font-size: 14px;
  padding: 4px 12px;
  border-radius: 6px;
  transition: all 0.3s;
}

/* 链接按钮悬停效果 */
.btn-link:hover {
  background: #e2e8f0;
}

/* 卡片主体样式 */
.card-body {
  padding: 20px;
}

/* 卡片主体无内边距（用于表格） */
.card-body.no-padding {
  padding: 0;
}

/* ========== 信息网格 ========== */
/* 信息网格布局（两列） */
.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

/* 信息项 */
.info-item {
  display: flex;
  align-items: baseline;
}

/* 信息标签 */
.info-label {
  width: 100px;
  color: #64748b;
  font-size: 14px;
}

/* 信息值 */
.info-value {
  color: #1e293b;
  font-size: 14px;
}

/* 状态文字内联样式 */
.info-value.status-text {
  font-size: 14px;
  font-weight: 500;
  padding: 2px 8px;
  border-radius: 12px;
  display: inline-block;
}

/* ========== 地址信息 ========== */
/* 地址信息区域 */
.address-info {
  line-height: 1.8;
}

/* 地址行 */
.address-line {
  margin-bottom: 8px;
}

/* 地址标签 */
.address-label {
  display: inline-block;
  width: 80px;
  color: #64748b;
}

/* 地址标签徽章 */
.address-tag {
  display: inline-block;
  padding: 2px 8px;
  background: #e2e8f0;
  border-radius: 12px;
  font-size: 12px;
  color: #64748b;
}

/* 无地址信息提示 */
.no-address {
  color: #94a3b8;
  text-align: center;
  padding: 20px;
}

/* ========== 物流信息 ========== */
/* 物流信息网格 */
.logistics-info {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

/* 物流信息项 */
.logistics-item {
  display: flex;
  align-items: baseline;
}

/* 物流标签 */
.logistics-label {
  width: 80px;
  color: #64748b;
}

/* 物流值 */
.logistics-value {
  color: #1e293b;
}

/* ========== 商品表格 ========== */
/* 商品表格容器（支持横向滚动） */
.product-table {
  overflow-x: auto;
}

/* 表格头部和行（使用网格布局） */
.table-header,
.table-row {
  display: grid;
  grid-template-columns: 3fr 1fr 1fr 1fr;
  padding: 16px 20px;
  border-bottom: 1px solid #e2e8f0;
}

/* 表格头部背景色 */
.table-header {
  background: #f8fafc;
  font-weight: 600;
  color: #1e293b;
}

/* 表格行悬停效果 */
.table-row:hover {
  background: #f8fafc;
}

/* 商品信息列 */
.col-product {
  display: flex;
  gap: 16px;
  align-items: center;
}

/* 商品图片 */
.product-image {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: 8px;
  background: #f1f5f9;
}

/* 商品名称 */
.product-name {
  font-weight: 500;
  margin-bottom: 4px;
}

/* 商品ID */
.product-id {
  font-size: 12px;
  color: #94a3b8;
}

/* 价格、数量、小计列（右对齐） */
.col-price,
.col-quantity,
.col-total {
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

/* 价格、数量、小计文字右对齐 */
.col-price,
.col-quantity,
.col-total {
  text-align: right;
}

/* 表格底部汇总区域 */
.table-footer {
  padding: 20px;
  border-top: 1px solid #e2e8f0;
  text-align: right;
}

/* 金额行 */
.amount-row {
  margin-bottom: 8px;
}

/* 金额标签 */
.amount-label {
  display: inline-block;
  width: 100px;
  color: #64748b;
}

/* 金额值 */
.amount-value {
  display: inline-block;
  width: 120px;
  text-align: right;
}

/* 总计行（带顶部分割线） */
.total-row {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #e2e8f0;
}

/* 总价格（红色加粗） */
.total-price {
  font-size: 20px;
  font-weight: 600;
  color: #ef4444;
}

/* ========== 操作按钮区 ========== */
/* 操作按钮容器 */
.action-bar {
  display: flex;
  justify-content: flex-end;
  gap: 16px;
  margin-top: 20px;
  padding-top: 20px;
}

/* 主要按钮样式 */
.btn-primary {
  padding: 10px 24px;
  background: #4a6491;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
}

/* 主要按钮悬停效果 */
.btn-primary:hover {
  background: #3a5479;
  transform: translateY(-2px);
}

/* 次要按钮样式 */
.btn-outline {
  padding: 10px 24px;
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
}

/* 次要按钮悬停效果 */
.btn-outline:hover {
  background: #f8fafc;
}

/* 危险按钮样式（取消订单） */
.btn-danger {
  padding: 10px 24px;
  background: #ef4444;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
}

/* 危险按钮悬停效果 */
.btn-danger:hover {
  background: #dc2626;
  transform: translateY(-2px);
}

/* ========== 状态徽章样式 ========== */
/* 不同状态的颜色样式 */
.status-pending { background: #fef3c7; color: #d97706; }
.status-paid { background: #dbeafe; color: #2563eb; }
.status-processing { background: #f3e8ff; color: #9333ea; }
.status-shipped { background: #cffafe; color: #0891b2; }
.status-delivered,
.status-completed { background: #dcfce7; color: #16a34a; }
.status-cancelled,
.status-refunded { background: #fee2e2; color: #dc2626; }

/* ========== 弹窗样式 ========== */
/* 弹窗遮罩层 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

/* 弹窗内容 */
.modal-content {
  background: white;
  border-radius: 12px;
  width: 90%;
  max-width: 500px;
  max-height: 90vh;
  overflow: auto;
}

/* 弹窗头部 */
.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #e2e8f0;
}

/* 弹窗标题 */
.modal-header h3 {
  margin: 0;
  font-size: 18px;
}

/* 弹窗关闭按钮 */
.modal-close {
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  color: #94a3b8;
}

/* 弹窗主体 */
.modal-body {
  padding: 20px;
}

/* 弹窗底部 */
.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 20px;
  border-top: 1px solid #e2e8f0;
}

/* ========== 表单样式 ========== */
/* 表单组 */
.form-group {
  margin-bottom: 16px;
}

/* 表单标签 */
.form-label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #1e293b;
}

/* 必填项红星 */
.form-label.required::after {
  content: '*';
  color: #ef4444;
  margin-left: 4px;
}

/* 表单控件 */
.form-control {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 14px;
  transition: all 0.3s;
}

/* 表单控件聚焦样式 */
.form-control:focus {
  outline: none;
  border-color: #4a6491;
  box-shadow: 0 0 0 3px rgba(74, 100, 145, 0.1);
}

/* 取消按钮 */
.btn-cancel {
  padding: 8px 20px;
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  cursor: pointer;
}

/* 确认按钮 */
.btn-confirm {
  padding: 8px 20px;
  background: #4a6491;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
}

/* 确认按钮禁用样式 */
.btn-confirm:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* ========== 响应式设计 ========== */
/* 移动端适配（宽度小于768px） */
@media (max-width: 768px) {
  /* 页面内边距减小 */
  .order-detail-page {
    padding: 12px;
  }
  
  /* 信息网格改为单列 */
  .info-grid {
    grid-template-columns: 1fr;
  }
  
  /* 物流信息网格改为单列 */
  .logistics-info {
    grid-template-columns: 1fr;
  }
  
  /* 表格列宽调整 */
  .table-header,
  .table-row {
    grid-template-columns: 2fr 1fr 0.8fr 1fr;
    padding: 12px;
    font-size: 12px;
  }
  
  /* 商品图片缩小 */
  .product-image {
    width: 50px;
    height: 50px;
  }
  
  /* 操作按钮垂直排列 */
  .action-bar {
    flex-direction: column;
  }
  
  /* 操作按钮占满宽度 */
  .action-bar button {
    width: 100%;
  }
  
  /* 状态栏垂直排列 */
  .status-bar {
    flex-direction: column;
    text-align: center;
  }
  
  /* 状态时间居中对齐 */
  .status-time {
    margin-left: 0;
    text-align: center;
  }
  
  /* 页面头部垂直排列 */
  .page-header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>