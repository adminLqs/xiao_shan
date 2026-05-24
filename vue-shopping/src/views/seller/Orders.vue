<template>
  <div class="seller-orders-container">
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
      </div>
      <div class="tab-underline" ref="tabUnderlineRef"></div>
    </div>

    <!-- ========== 加载状态 ========== -->
    <div v-if="loading" class="skeleton-order-list">
      <div v-for="i in 5" :key="i" class="skeleton-order-card">
        <div class="skeleton-order-header">
          <div class="skeleton" style="width: 150px; height: 14px;"></div>
          <div class="skeleton" style="width: 80px; height: 14px;"></div>
        </div>
        <div class="skeleton-order-goods">
          <div class="skeleton" style="width: 80px; height: 80px; border-radius: 8px;"></div>
          <div style="flex: 1;">
            <div class="skeleton skeleton-line long" style="margin-bottom: 10px;"></div>
            <div class="skeleton skeleton-line medium"></div>
          </div>
        </div>
        <div class="skeleton-order-footer">
          <div class="skeleton" style="width: 80px; height: 32px; border-radius: 6px;"></div>
        </div>
      </div>
    </div>

    <!-- ========== 空状态 ========== -->
    <div v-else-if="orders.length === 0" class="empty-state">
      <i class="fas fa-shopping-bag"></i>
      <p>暂无订单</p>
    </div>

    <!-- ========== 订单列表 ========== -->
    <div v-else class="order-list content-wrapper">
      <div v-for="orderWrapper in orders" :key="orderWrapper.order.id" class="order-card">

        <!-- 订单头部 -->
        <div class="order-header">
          <div class="order-info">
            <span class="order-number">订单号：{{ orderWrapper.order.orderNumber }}</span>
          </div>
          <div class="order-status-group">
            <span class="order-status" :class="getStatusClass(orderWrapper.order.status)">
              {{ getStatusText(orderWrapper.order.status) }}
            </span>
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
              <div class="tags-group">
                <span v-if="item.skuName" class="tag-spec">{{ item.skuName }}</span>
                <span class="tag-quantity">x{{ item.quantity }}</span>
              </div>
            </div>
            <div class="item-price">¥{{ formatPrice(item.price) }}</div>
          </div>
        </div>

        <!-- 订单底部 -->
        <div class="order-footer">
          <div class="order-actions">
            <template v-if="orderWrapper.order.status === 'PAID'">
              <button class="btn-primary" @click="processOrder(orderWrapper.order.id)">处理订单</button>
            </template>
            <template v-if="orderWrapper.order.status === 'PROCESSING'">
              <button class="btn-primary" @click="openShipDialog(orderWrapper.order.id)">发货</button>
            </template>
            <template v-if="hasRefundingItem(orderWrapper)">
              <button class="btn-primary btn-refund" @click="handleRefund(orderWrapper)">处理退款</button>
            </template>
            <!-- 商家发货物流 -->
            <template v-if="orderWrapper.order.status !== 'PENDING' && orderWrapper.order.status !== 'CANCELLED' && orderWrapper.order.trackingNumber">
              <button class="btn-outline" @click="viewLogistics(orderWrapper.order.id)">查看物流</button>
            </template>
            <!-- 退货物流（买家已退货） -->
            <template v-if="hasReturningItem(orderWrapper)">
              <button class="btn-outline" @click="viewReturnLogistics(orderWrapper)">查看退货物流</button>
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

    <!-- ========== 退款选择弹窗 ========== -->
    <div v-if="showRefundSelector" class="modal-overlay refund-selector-overlay" @click="showRefundSelector = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>选择退款项</h3>
          <button class="modal-close" @click="showRefundSelector = false">
            <i class="fas fa-times"></i>
          </button>
        </div>
        <div class="modal-body">
          <div class="refund-item-list">
            <div
              v-for="(item, index) in refundSelectorItems"
              :key="index"
              class="refund-item-card"
              @click.stop="selectRefundItem(item)"
            >
              <div class="refund-item-info">
                <div class="refund-item-product">{{ item.productName || '商品' }}</div>
                <div class="refund-item-meta">
                  <span class="refund-type-tag" :class="getRefundTypeText(item) === '退货退款' ? 'type-after-sale' : 'type-refund'">
                    {{ getRefundTypeText(item) }}
                  </span>
                  <span class="refund-status-text">{{ getRefundStatusText(item) }}</span>
                </div>
                <div class="refund-item-amount">
                  ¥{{ item.price?.toFixed(2) || '0.00' }}
                  <span class="tag-quantity">× {{ item.quantity }}</span>
                </div>
              </div>
              <i class="fas fa-chevron-right refund-item-arrow"></i>
            </div>
          </div>
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
  import Message from '@/utils/message'
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
    refundStatus?: string
    refundType?: string
    refundId?: number
    skuName?: string
  }

  /** 订单信息 */
  interface Order {
    id: number
    orderNumber: string
    totalAmount: number
    status: string
    createdAt: string
    // 额外字段（可能存在）
    userId?: number
    addressId?: number
    paymentMethod?: string
    paidAt?: string
    shippedAt?: string
    deliveredAt?: string
    completedAt?: string
    trackingNumber?: string
    logisticsCode?: string
    logisticsName?: string
    remark?: string
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
    { label: '退款中', value: 'REFUNDING', count: 0 },
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
  const loading = ref(true)
  const orders = ref<OrderWithItems[]>([])
  const currentPage = ref(1)
  const pageSize = ref(10)
  const totalPages = ref(1)
  const total = ref(0)

  const showShipDialog = ref(false)
  const currentOrderId = ref<number | null>(null)
  const submitting = ref(false)
  const tabUnderlineRef = ref<HTMLElement | null>(null)

  // 退款选择弹窗相关
  const showRefundSelector = ref(false)
  const refundSelectorItems = ref<any[]>([])
  const currentOrderWrapper = ref<OrderWithItems | null>(null)

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
        pageSize: pageSize.value,
        status: activeTab.value === 'all' ? undefined  : activeTab.value
      }

      const response = await authAPI.getSellerOrders(params)

      if (response.success) {
        const data = response.data || {}
        orders.value = data.records || []
        totalPages.value = data.totalPages || 1
        total.value = data.total || 0
        updateTabCounts(data.counts)
      }
    } catch (error: any) {
      Message.error(error.message || '加载失败')
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
      Message.error('请输入物流单号')
      return
    }

    if (!shipForm.value.logisticsCode) {
      Message.error('请选择物流公司')
      return
    }

    submitting.value = true

    try {
      const orderId = currentOrderId.value
      if (!orderId) {
        Message.error('订单ID不存在')
        return
      }

      const data = {
        trackingNumber: shipForm.value.trackingNumber,
        logisticsCode: shipForm.value.logisticsCode,
        logisticsName: logisticsMap[shipForm.value.logisticsCode] || '未知物流'
      }

      const response = await authAPI.shipOrder(orderId, data)

      if (response.success) {
        Message.success('发货成功')
        closeShipDialog()
        await loadOrders()
      }
    } catch (error: any) {
      Message.error(error.message || '发货失败')
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
      await Message.confirm('确定要处理该订单吗？', '处理订单')

      const response = await authAPI.processOrder(orderId)

      if (response.success) {
        Message.success('订单已处理')
        await loadOrders()
      }
    } catch (error: any) {
      if (error !== 'cancel') {
        Message.error(error.message || '处理订单失败')
      }
    }
  }

  /**
   * 取消订单
   */
  const cancelOrder = async (orderId: number): Promise<void> => {
    try {
      await Message.confirm('确定要取消该订单吗？', '取消订单')

      const response = await authAPI.cancelOrder(orderId)

      if (response.success) {
        Message.success('取消成功')
        await loadOrders()
      }
    } catch (error: any) {
      if (error !== 'cancel') {
        Message.error(error.message || '操作失败')
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
    router.push({
      name: 'SellerOrderDetail',
      params: { id: orderId }
    })
  }

  /**
   * 判断订单是否有退款中的商品
   */
  const hasRefundingItem = (orderWrapper: OrderWithItems): boolean => {
    return orderWrapper.orderItems.some(item =>
      item.refundStatus === 'REFUNDING' ||
      item.refundStatus === 'AFTER_SALE' ||
      item.refundStatus === 'WAITING_RETURN' ||
      item.refundStatus === 'RETURNING' ||
      item.refundStatus === 'APPROVED'
    )
  }

  /**
   * 检查是否有退货中的订单项
   */
  const hasReturningItem = (orderWrapper: OrderWithItems): boolean => {
    return orderWrapper.orderItems.some(item => item.refundStatus === 'WAITING_RETURN')
  }

  /**
   * 查看退货物流
   */
  const viewReturnLogistics = (orderWrapper: OrderWithItems): void => {
    const item = orderWrapper.orderItems.find(item => item.refundStatus === 'RETURNING')
    if (item?.refundId) {
      router.push({ name: 'SellerLogistics', query: { refundId: String(item.refundId), type: 'return' } })
    }
  }

  /**
   * 处理退款（弹出退款项选择弹窗）
   */
  const handleRefund = (orderWrapper: OrderWithItems): void => {
    const refundingItems = orderWrapper.orderItems.filter(item =>
      item.refundStatus === 'REFUNDING' ||
      item.refundStatus === 'AFTER_SALE' ||
      item.refundStatus === 'WAITING_RETURN' ||
      item.refundStatus === 'RETURNING' ||
      item.refundStatus === 'APPROVED'
    )

    if (refundingItems.length === 0) {
      Message.error('没有待处理的退款')
      return
    }

    // 无论几个都弹窗
    refundSelectorItems.value = refundingItems
    showRefundSelector.value = true
  }

  /**
   * 选择处理某个退款项
   */
  const selectRefundItem = (item: OrderItem): void => {
    const refundId = item?.refundId
    if (!refundId) {
      Message.error('退款记录不存在')
      return
    }
    showRefundSelector.value = false

    if (item.refundStatus === 'RETURNING') {
      // 退货中，跳转查看退货物流
      router.push({
        name: 'SellerLogistics',
        query: { refundId: String(refundId), type: 'return' }
      })
    } else {
      // 其他状态，跳转 RefundChat 对峙页
      router.push({
        name: 'RefundChat',
        params: { refundId: String(refundId) }
      })
    }
  }

  // ==================== 分页 ====================

  const changePage = (page: number): void => {
    if (page < 1 || page > totalPages.value) return
    currentPage.value = page
    window.scrollTo({ top: 0, behavior: 'smooth' })
    loadOrders()
  }

  // ==================== Tab 下划线动画 ====================

  /**
   * 更新下划线位置
   */
  const updateUnderline = (): void => {
    const tabs = document.querySelectorAll('.order-tab')
    const activeTabEl = document.querySelector('.order-tab.active')
    const underline = tabUnderlineRef.value

    if (!activeTabEl || !underline) return

    const rect = activeTabEl.getBoundingClientRect()
    const containerRect = (activeTabEl.parentElement as HTMLElement)?.getBoundingClientRect()

    if (containerRect) {
      underline.style.left = `${rect.left - containerRect.left}px`
      underline.style.width = `${rect.width}px`
    }
  }

  // ==================== 监听器 ====================

  watch(activeTab, () => {
    currentPage.value = 1
    loadOrders()
    setTimeout(updateUnderline, 0)
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
   * 格式化日期时间（完整格式）
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
   * 格式化日期时间（精简格式 MM-DD HH:mm）
   */
  const formatDateTimeShort = (dateStr: string): string => {
    if (!dateStr) return '-'
    try {
      const date = new Date(dateStr)
      const now = new Date()
      const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
      const yesterday = new Date(today.getTime() - 24 * 60 * 60 * 1000)
      const orderDate = new Date(date.getFullYear(), date.getMonth(), date.getDate())

      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      const hours = String(date.getHours()).padStart(2, '0')
      const minutes = String(date.getMinutes()).padStart(2, '0')

      if (orderDate.getTime() === today.getTime()) {
        return `今天 ${hours}:${minutes}`
      } else if (orderDate.getTime() === yesterday.getTime()) {
        return `昨天 ${hours}:${minutes}`
      }

      return `${month}-${day} ${hours}:${minutes}`
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
   * 获取退款状态样式类
   */
  const getRefundStatusClass = (refundStatus: string): string => {
    const map: Record<string, string> = {
      REFUNDING: 'refund-pending',
      AFTER_SALE: 'refund-processing',
      WAITING_RETURN: 'refund-waiting',
      RETURNING: 'refund-waiting',
      APPROVED: 'refund-approved',
      COMPLETED: 'refund-completed',
      FAILED: 'refund-failed'
    }
    return map[refundStatus] || ''
  }

  /**
   * 获取退款状态文字
   */
  const getRefundStatusText = (item: OrderItem): string => {
    const refundType = getRefundTypeText(item)
    const refundStatus = item.refundStatus || ''

    if (refundType === '仅退款') {
      // 仅退款
      const map: Record<string, string> = {
        PROCESSING: '退款中',
        REFUNDING: '退款中',
        APPROVED: '已同意',
        SUCCESS: '已退款',
        COMPLETED: '已退款',
        FAILED: '已拒绝'
      }
      return map[refundStatus] || refundStatus
    } else {
      // 退货退款
      const map: Record<string, string> = {
        PROCESSING: '售后处理中',
        AFTER_SALE: '售后处理中',
        APPROVED: '待退货',
        WAITING_RETURN: '待退货',
        RETURNING: '退货中',
        RECEIVED: '已收货',
        SUCCESS: '已退款',
        COMPLETED: '已退款',
        FAILED: '已拒绝'
      }
      return map[refundStatus] || refundStatus
    }
  }

  /**
   * 获取退款类型文字
   */
  const getRefundTypeText = (item: OrderItem): string => {
    if (item.refundType === 'AFTER_SALE') return '退货退款'
    if (item.refundType === 'REFUND') return '仅退款'
    // 根据状态推断
    if (
      item.refundStatus === 'AFTER_SALE' ||
      item.refundStatus === 'WAITING_RETURN' ||
      item.refundStatus === 'RETURNING' ||
      item.refundStatus === 'APPROVED'
    ) {
      return '退货退款'
    }
    return '仅退款'
  }

  /**
   * 获取退款状态图标
   */
  const getRefundStatusIcon = (refundStatus: string): string => {
    const map: Record<string, string> = {
      REFUNDING: 'fa-clock',
      AFTER_SALE: 'fa-sync',
      WAITING_RETURN: 'fa-truck',
      RETURNING: 'fa-truck',
      APPROVED: 'fa-check-circle',
      COMPLETED: 'fa-check-double',
      FAILED: 'fa-times-circle'
    }
    return map[refundStatus] || 'fa-info-circle'
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

  /**
   * 获取订单整体退款状态
   * @param {OrderWithItems} orderWrapper - 订单包装类
   * @returns {string | null} 退款状态文本或null
   */
  const getOrderRefundStatus = (orderWrapper: OrderWithItems): string | null => {
    const orderItems = orderWrapper.orderItems || []
    if (orderItems.length === 0) return null

    const refundingCount = orderItems.filter(item =>
      item.refundStatus === 'REFUNDING' || item.refundStatus === 'AFTER_SALE' ||
      item.refundStatus === 'WAITING_RETURN' || item.refundStatus === 'RETURNING' ||
      item.refundStatus === 'APPROVED'
    ).length
    const refundedCount = orderItems.filter(item => item.refundStatus === 'COMPLETED').length

    if (refundingCount === orderItems.length) return '退款中'
    if (refundedCount === orderItems.length) return '已退款'
    if (refundingCount > 0 || refundedCount > 0) return '部分退款'
    return null
  }

  /**
   * 获取订单级别的退款状态样式类
   * @param {OrderWithItems} orderWrapper - 订单包装类
   * @returns {string} CSS类名
   */
  const getOrderRefundStatusClass = (orderWrapper: OrderWithItems): string => {
    const orderItems = orderWrapper.orderItems || []
    if (orderItems.length === 0) return ''

    const refundingCount = orderItems.filter(item =>
      item.refundStatus === 'REFUNDING' || item.refundStatus === 'AFTER_SALE' ||
      item.refundStatus === 'WAITING_RETURN' || item.refundStatus === 'RETURNING' ||
      item.refundStatus === 'APPROVED'
    ).length
    const refundedCount = orderItems.filter(item => item.refundStatus === 'COMPLETED').length

    if (refundingCount === orderItems.length) return 'status-refunding'
    if (refundedCount === orderItems.length) return 'status-refunded'
    return 'status-partial-refund'
  }

  // ==================== 生命周期 ====================

  onMounted(() => {
    if (!authStore.validateSellerPermission()) return

    // 检查套餐
    authAPI.checkActivePackage().then(res => {
      if (!res.data?.active) {
        Message.warning('套餐已过期，请续费')
        router.push({ name: 'SellerPackage' })
        return
      }
      loadOrders()
      setTimeout(updateUnderline, 0)
    }).catch(() => {
      Message.error('检查套餐状态失败')
      router.push({ name: 'SellerPackage' })
    })
  })
</script>

<style scoped>
@import url('@/static/css/seller/订单管理页.css');
</style>
