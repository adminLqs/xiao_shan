<template>
  <div class="seller-orders-container">
    <!-- ========== 页面头部 ========== -->
    <div class="page-header">
      <h1 class="page-title">
        <i class="fas fa-shopping-cart"></i>
        订单管理
      </h1>
      <div class="breadcrumb">
        <RouterLink to="/seller/dashboard">商家中心</RouterLink>
        <i class="fas fa-chevron-right"></i>
        <span class="current">订单管理</span>
      </div>
    </div>

    <!-- ========== 订单状态标签页 ========== -->
    <div class="order-tabs">
      <div 
        v-for="tab in orderTabs" 
        :key="tab.value"
        class="order-tab"
        :class="{ active: activeTab === tab.value }"
        @click="activeTab = tab.value"
      >
        {{ tab.label }}
        <span v-if="tab.count > 0" class="tab-count">{{ tab.count }}</span>
      </div>
    </div>

    <!-- ========== 加载状态 ========== -->
    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>

    <!-- ========== 空状态 ========== -->
    <div v-else-if="orders.length === 0" class="empty-state">
      <i class="fas fa-shopping-bag"></i>
      <p>暂无订单</p>
    </div>

    <!-- ========== 订单列表 ========== -->
    <div v-else class="order-list">
      <div v-for="orderWrapper in orders" :key="orderWrapper.order.id" class="order-card">
        
        <!-- 订单头部 -->
        <div class="order-header">
          <div class="order-info">
            <span class="order-number">订单号：{{ orderWrapper.order.orderNumber }}</span>
            <span class="order-time">{{ formatDateTime(orderWrapper.order.createdAt) }}</span>
          </div>
          <div class="order-status" :class="getStatusClass(orderWrapper.order.status)">
            {{ getStatusText(orderWrapper.order.status) }}
          </div>
        </div>

        <!-- 订单商品列表 -->
        <div class="order-items">
          <div 
            v-for="item in orderWrapper.orderItems" 
            :key="item.id" 
            class="order-item"
          >
            <img :src="item.productImage" class="item-image">
            <div class="item-info">
              <div class="item-name">{{ item.productName }}</div>
              <div class="item-spec">数量：{{ item.quantity }}</div>
            </div>
            <div class="item-price">¥{{ formatPrice(item.price) }}</div>
          </div>
        </div>

        <!-- 订单底部 -->
        <div class="order-footer">
          <div class="order-total">
            实付款：<span class="total-amount">¥{{ formatPrice(orderWrapper.order.totalAmount) }}</span>
          </div>
          <div class="order-actions">
            <template v-if="orderWrapper.order.status === 'PENDING'">
              <button class="btn-outline" @click="cancelOrder(orderWrapper.order.id)">取消订单</button>
            </template>
            <template v-if="orderWrapper.order.status === 'PAID'">
              <button class="btn-primary" @click="processOrder(orderWrapper.order.id)">处理订单</button>
            </template>
            <template v-if="orderWrapper.order.status === 'PROCESSING'">
              <button class="btn-primary" @click="openShipDialog(orderWrapper.order.id)">发货</button>
            </template>
            <template v-if="orderWrapper.order.status !== 'PENDING' && orderWrapper.order.status !== 'CANCELLED'">
              <button class="btn-outline" @click="viewLogistics(orderWrapper.order.id)">查看物流</button>
            </template>
            <button class="btn-outline" @click="viewOrderDetail(orderWrapper.order.id)">查看详情</button>
          </div>
        </div>
      </div>
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

    <!-- ========== 分页组件 ========== -->
    <div class="pagination" v-if="totalPages > 1">
      <button class="page-btn" :disabled="currentPage === 1" @click="changePage(currentPage - 1)">
        <i class="fas fa-chevron-left"></i>
      </button>
      <span class="page-info">{{ currentPage }} / {{ totalPages }}</span>
      <button class="page-btn" :disabled="currentPage === totalPages" @click="changePage(currentPage + 1)">
        <i class="fas fa-chevron-right"></i>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { ref, onMounted, watch } from 'vue'
  import { useRouter } from 'vue-router'
  import { authAPI } from '@/api/authAPI'
  import { showToast, showConfirmDialog } from 'vant'
  import 'vant/es/toast/style'
  import 'vant/es/dialog/style'
  import { useAuthStore } from '@/stores/auth'

  const authStore = useAuthStore()
  const router = useRouter()

  // ==================== 类型定义 ====================

  /** 订单商品项 */
  interface OrderItem {
    id: number
    productId: number
    productName: string
    productImage: string
    quantity: number
    price: number
  }

  /** 订单信息 */
  interface Order {
    id: number
    orderNumber: string
    totalAmount: number
    status: string
    createdAt: string
  }

  /** 订单包装类（新架构） */
  interface OrderWithItems {
    order: Order
    orderItems: OrderItem[]
  }

  /** 标签页配置 */
  interface OrderTab {
    label: string
    value: string
    count: number
  }

  /** 发货表单 */
  interface ShipForm {
    trackingNumber: string
    logisticsCode: string
  }

  // ==================== 常量配置 ====================

  /** 订单标签页 */
  const orderTabs = ref<OrderTab[]>([
    { label: '全部', value: 'all', count: 0 },
    { label: '待付款', value: 'PENDING', count: 0 },
    { label: '已付款', value: 'PAID', count: 0 },
    { label: '处理中', value: 'PROCESSING', count: 0 },
    { label: '已发货', value: 'SHIPPED', count: 0 },
    { label: '已完成', value: 'COMPLETED', count: 0 },
    { label: '已取消', value: 'CANCELLED', count: 0 },
    { label: '已退款', value: 'REFUNDED', count: 0 }
  ])

  /** 物流公司映射 */
  const logisticsMap: Record<string, string> = {
    SF: '顺丰速运',
    YTO: '圆通速递',
    ZTO: '中通快递',
    EMS: '邮政EMS',
    YD: '韵达快递',
    STO: '申通快递',
    JT: '极兔速递'
  }

  // ==================== 响应式数据 ====================

  const activeTab = ref('all')
  const loading = ref(false)
  const orders = ref<OrderWithItems[]>([])
  const currentPage = ref(1)
  const pageSize = ref(10)
  const totalPages = ref(1)
  const total = ref(0)

  const showShipDialog = ref(false)
  const currentOrderId = ref<number | null>(null)
  const submitting = ref(false)

  const shipForm = ref<ShipForm>({
    trackingNumber: '',
    logisticsCode: ''
  })

  // ==================== 数据加载 ====================

  /**
   * 加载订单列表
   */
  const loadOrders = async (): Promise<void> => {
    loading.value = true

    try {
      const params = {
        page: currentPage.value,
        size: pageSize.value,
        status: activeTab.value === 'all' ? undefined  : activeTab.value
      }

      const response = await authAPI.getSellerOrders(params)

      if (response.success) {
        orders.value = response.data || []
        totalPages.value = response.totalPages || 1
        total.value = response.total || 0
        updateTabCounts(response.counts)
      }
    } catch (error: any) {
      console.error('加载订单失败:', error)
      showToast({ message: error.message || '加载失败', type: 'fail' })
    } finally {
      loading.value = false
    }
  }

  /**
   * 更新标签页数量角标
   */
  const updateTabCounts = (counts: any): void => {
    if (!counts) return

    orderTabs.value.forEach(tab => {
      if (tab.value === 'all') {
        tab.count = counts.total || 0
      } else {
        tab.count = counts[tab.value] || 0
      }
    })
  }

  // ==================== 发货相关 ====================

  const openShipDialog = (orderId: number): void => {
    currentOrderId.value = orderId
    shipForm.value = { trackingNumber: '', logisticsCode: '' }
    showShipDialog.value = true
  }

  const closeShipDialog = (): void => {
    showShipDialog.value = false
    currentOrderId.value = null
    shipForm.value = { trackingNumber: '', logisticsCode: '' }
  }

  /**
   * 确认发货
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
      const orderId = currentOrderId.value
      if (!orderId) {
        showToast({ message: '订单ID不存在', type: 'fail' })
        return
      }

      const data = {
        trackingNumber: shipForm.value.trackingNumber,
        logisticsCode: shipForm.value.logisticsCode,
        logisticsName: logisticsMap[shipForm.value.logisticsCode]
      }

      const response = await authAPI.shipOrder(orderId, data)

      if (response.success) {
        showToast({ message: '发货成功', type: 'success' })
        closeShipDialog()
        await loadOrders()
      }
    } catch (error: any) {
      console.error('发货失败:', error)
      showToast({ message: error.message || '发货失败', type: 'fail' })
    } finally {
      submitting.value = false
    }
  }

  // ==================== 订单操作 ====================

  /**
   * 处理订单（PAID → PROCESSING）
   */
  const processOrder = async (orderId: number): Promise<void> => {
    try {
      await showConfirmDialog({
        title: '处理订单',
        message: '确定要处理该订单吗？',
        confirmButtonText: '确定',
        cancelButtonText: '取消'
      })

      const response = await authAPI.processOrder(orderId)

      if (response.success) {
        showToast({ message: '订单已处理', type: 'success' })
        await loadOrders()
      }
    } catch (error: any) {
      if (error !== 'cancel') {
        console.error('处理订单失败:', error)
        showToast({ message: error.message || '操作失败', type: 'fail' })
      }
    }
  }

  /**
   * 取消订单
   */
  const cancelOrder = async (orderId: number): Promise<void> => {
    try {
      await showConfirmDialog({
        title: '取消订单',
        message: '确定要取消该订单吗？',
        confirmButtonText: '确定',
        cancelButtonText: '取消'
      })

      const response = await authAPI.cancelOrder(orderId)

      if (response.success) {
        showToast({ message: '取消成功', type: 'success' })
        await loadOrders()
      }
    } catch (error: any) {
      if (error !== 'cancel') {
        console.error('取消订单失败:', error)
        showToast({ message: error.message || '操作失败', type: 'fail' })
      }
    }
  }

  /**
   * 查看物流
   */
  const viewLogistics = (orderId: number): void => {
    router.push({
      name: 'SellerLogistics',
      query: { orderId: String(orderId) }
    })
  }

  /**
   * 查看订单详情
   */
  const viewOrderDetail = (orderId: number): void => {
    router.push(`/seller/orders/${orderId}`)
  }

  // ==================== 分页 ====================

  const changePage = (page: number): void => {
    if (page < 1 || page > totalPages.value) return
    currentPage.value = page
    window.scrollTo({ top: 0, behavior: 'smooth' })
    loadOrders()
  }

  // ==================== 监听器 ====================

  watch(activeTab, () => {
    currentPage.value = 1
    loadOrders()
  })

  // ==================== 工具函数 ====================

  /**
   * 格式化价格
   */
  const formatPrice = (price: number): string => {
    if (price == null) return '0.00'
    return price.toFixed(2)
  }

  /**
   * 格式化日期时间
   */
  const formatDateTime = (dateStr: string): string => {
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

  /**
   * 获取状态样式类
   */
  const getStatusClass = (status: string): string => {
    const map: Record<string, string> = {
      PENDING: 'status-pending',
      PAID: 'status-paid',
      PROCESSING: 'status-processing',
      SHIPPED: 'status-shipped',
      COMPLETED: 'status-completed',
      CANCELLED: 'status-cancelled',
      REFUNDED: 'status-refunded'
    }
    return map[status] || ''
  }

  /**
   * 获取状态文字
   */
  const getStatusText = (status: string): string => {
    const map: Record<string, string> = {
      PENDING: '待付款',
      PAID: '已付款',
      PROCESSING: '处理中',
      SHIPPED: '已发货',
      COMPLETED: '已完成',
      CANCELLED: '已取消',
      REFUNDED: '已退款'
    }
    return map[status] || status
  }

  // ==================== 生命周期 ====================

  onMounted(() => {
    if (!authStore.validateSellerPermission()) return
    loadOrders()
  })
</script>

<style scoped>
@import url('@/static/css/seller/商家订单.css');
</style>