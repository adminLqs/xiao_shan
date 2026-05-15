<template>
  <div class="order-page">
    <!-- 顶部导航 -->
    <div class="order-header">
      <h1 class="page-title">我的订单</h1>
    </div>

    <!-- 订单状态标签 -->
    <div class="order-tabs">
      <div 
        v-for="tab in tabs" 
        :key="tab.value"
        class="tab-item"
        :class="{ active: currentTab === tab.value }"
        @click="switchTab(tab.value)"
      >
        {{ tab.label }}
        <span v-if="tab.count > 0" class="tab-count">{{ tab.count }}</span>
      </div>
    </div>

    <!-- 订单列表 -->
    <div class="order-list">
      <!-- 加载状态 -->
      <div v-if="loading" class="state-wrapper">
        <div class="loading-state">
          <div class="loading-spinner"></div>
          <span>加载中...</span>
        </div>
      </div>
      
      <!-- 空状态 -->
      <div v-else-if="filteredOrders.length === 0" class="state-wrapper">
        <div class="empty-state">
          <div class="empty-icon">📦</div>
          <p class="empty-text">暂无订单</p>
          <p class="empty-subtext">去逛逛，发现更多美食</p>
          <button class="go-shop-btn" @click="goToShop">去购物</button>
        </div>
      </div>
      
      <!-- 订单列表 -->
      <div v-else class="order-items">
        <div 
          v-for="order in filteredOrders" 
          :key="order.id"
          class="order-card"
        >
          <!-- 订单头部 -->
          <div class="order-card-header">
            <div class="order-info">
              <span class="order-no">订单号：{{ order.orderNumber }}</span>
              <span class="order-time">{{ formatDate(order.createdAt) }}</span>
            </div>
            <div class="order-status" :class="getStatusClass(order.status)">
              {{ getStatusText(order.status) }}
            </div>
          </div>
          
          <!-- 退款信息提示（如果已退款） -->
          <div class="refund-info-tag" v-if="order.refundStatus === 'FULL_REFUND'">
            <span class="refund-icon">💰</span>
            <span class="refund-text">已全额退款</span>
          </div>
          <div class="refund-info-tag partial" v-else-if="order.refundStatus === 'PARTIAL_REFUND'">
            <span class="refund-icon">💸</span>
            <span class="refund-text">已退款 ¥{{ formatPrice(order.refundAmount || 0) }}</span>
          </div>
          
          <!-- 订单底部 -->
          <div class="order-card-footer">
            <div class="order-amount">
              <span class="amount-label">实付</span>
              <span class="amount-value">¥{{ formatPrice(order.totalAmount) }}</span>
            </div>
            <div class="order-actions">
              <!-- 待支付：显示去支付 -->
              <button 
                v-if="order.status === 'PENDING'"
                class="action-btn pay-btn"
                @click.stop="goToPay(order.orderNumber, order.totalAmount)"
              >
                去支付
              </button>
              
              <!-- 已支付 - 根据退款状态显示不同按钮 -->
              <template v-else-if="order.status === 'PAID'">
                <button 
                  v-if="order.refundStatus === 'FULL_REFUND'"
                  class="action-btn refunded-btn"
                  disabled
                >
                  ✅ 已退款
                </button>
                <button 
                  v-else-if="order.refundStatus === 'PARTIAL_REFUND'"
                  class="action-btn refund-partial-btn"
                  @click.stop="openRefundModal(order)"
                >
                  💰 继续退款
                  <span class="refund-tip">(已退¥{{ formatPrice(order.refundAmount || 0) }})</span>
                </button>
                <button 
                  v-else
                  class="action-btn refund-btn"
                  @click.stop="openRefundModal(order)"
                >
                  申请退款
                </button>
              </template>
              
              <!-- 已发货：显示确认收货按钮 -->
              <button 
                v-else-if="order.status === 'SHIPPED'"
                class="action-btn confirm-receipt-btn"
                @click.stop="confirmReceipt(order.orderNumber)"
              >
                确认收货
              </button>
              
              <!-- 已取消：显示已取消 -->
              <button 
                v-else-if="order.status === 'CANCELLED'"
                class="action-btn cancelled-btn"
                disabled
              >
                已取消
              </button>
              
              <!-- 查看详情按钮（始终显示） -->
              <button 
                class="action-btn detail-btn"
                @click.stop="viewOrderDetail(order.orderNumber)"
              >
                查看详情
              </button>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 加载更多 -->
      <div v-if="hasMore && !loading && filteredOrders.length > 0" class="load-more" @click="loadMore">
        加载更多
      </div>
      <div v-if="!hasMore && filteredOrders.length > 0" class="no-more">
        没有更多了~
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
              <span class="value">{{ refundOrder?.orderNumber }}</span>
            </div>
            <div class="info-item">
              <span class="label">订单金额：</span>
              <span class="value">¥{{ formatPrice(refundOrder?.totalAmount || 0) }}</span>
            </div>
            <div class="info-item" v-if="refundOrder?.refundAmount && refundOrder.refundAmount > 0">
              <span class="label">已退款：</span>
              <span class="value">¥{{ formatPrice(refundOrder.refundAmount) }}</span>
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
                @input="validateRefundAmount"
              />
            </div>
            <div class="amount-hint" v-if="refundAmountInput > maxRefundAmount">
              <span class="error">退款金额不能超过可退金额</span>
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
          <button class="confirm-btn" @click="submitRefund" :disabled="!canSubmitRefund">
            {{ submitting ? '提交中...' : '提交申请' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
  import { ref, computed, onMounted } from 'vue'
  import { useRouter } from 'vue-router'
  import { showConfirmDialog, showToast } from 'vant'
  import { authAPI } from '@/api/authAPI'
  import { useUserStore } from '@/stores/auth'
  import 'vant/es/dialog/style'  
  import 'vant/es/toast/style'  

  const userStore = useUserStore()

  // ==================== 类型定义 ====================
  interface Order {
    id: number
    orderNumber: string
    totalAmount: number
    status: 'PENDING' | 'PAID' | 'SHIPPED' | 'COMPLETED' | 'CANCELLED' | 'REFUNDING' | 'REFUNDED'
    createdAt: string
    paymentMethod?: string
    refundAmount?: number
    refundStatus?: string
  }

  // ==================== 路由 ====================
  const router = useRouter()


  // ==================== 响应式数据 ====================
  const loading = ref(false)
  const submitting = ref(false)
  const orders = ref<Order[]>([])
  const currentTab = ref('all')
  const page = ref(1)
  const pageSize = ref(20)
  const hasMore = ref(true)
  const total = ref(0)

  // 退款弹窗
  const showRefundModal = ref(false)
  const refundOrder = ref<Order | null>(null)
  const refundAmountInput = ref(0)
  const refundReasonInput = ref('')

  // ==================== 计算属性 ====================

  const maxRefundAmount = computed(() => {
    if (!refundOrder.value) return 0
    return refundOrder.value.totalAmount - (refundOrder.value.refundAmount || 0)
  })

  const canSubmitRefund = computed(() => {
    if (refundAmountInput.value <= 0) return false
    if (refundAmountInput.value > maxRefundAmount.value) return false
    if (!refundReasonInput.value.trim()) return false
    if (submitting.value) return false
    return true
  })

  const tabs = computed(() => [
    { label: '全部', value: 'all', count: total.value },
    { label: '待支付', value: 'PENDING', count: orders.value.filter(o => o.status === 'PENDING').length },
    { label: '已支付', value: 'PAID', count: orders.value.filter(o => o.status === 'PAID').length },
    { label: '已发货', value: 'SHIPPED', count: orders.value.filter(o => o.status === 'SHIPPED').length },
    { label: '已完成', value: 'COMPLETED', count: orders.value.filter(o => o.status === 'COMPLETED').length },
    { label: '已取消', value: 'CANCELLED', count: orders.value.filter(o => o.status === 'CANCELLED').length }
  ])

  const filteredOrders = computed(() => {
    if (currentTab.value === 'all') return orders.value
    return orders.value.filter(order => order.status === currentTab.value)
  })

  // ==================== 工具方法 ====================

  const formatPrice = (price: number): string => {
    return price.toFixed(2)
  }

  const formatDate = (dateStr: string): string => {
    if (!dateStr) return ''
    const date = new Date(dateStr)
    const month = (date.getMonth() + 1).toString().padStart(2, '0')
    const day = date.getDate().toString().padStart(2, '0')
    const hours = date.getHours().toString().padStart(2, '0')
    const minutes = date.getMinutes().toString().padStart(2, '0')
    return `${month}-${day} ${hours}:${minutes}`
  }

  const getStatusText = (status: string): string => {
    const map: Record<string, string> = {
      'PENDING': '待支付',
      'PAID': '已支付',
      'SHIPPED': '已发货',
      'COMPLETED': '已完成',
      'CANCELLED': '已取消',
      'REFUNDING': '退款中',
      'REFUNDED': '已退款'
    }
    return map[status] || status
  }

  const getStatusClass = (status: string): string => {
    const map: Record<string, string> = {
      'PENDING': 'status-pending',
      'PAID': 'status-paid',
      'SHIPPED': 'status-shipped',
      'COMPLETED': 'status-completed',
      'CANCELLED': 'status-cancelled',
      'REFUNDING': 'status-refunding',
      'REFUNDED': 'status-refunded'
    }
    return map[status] || ''
  }

  const validateRefundAmount = () => {
    if (refundAmountInput.value > maxRefundAmount.value) {
      refundAmountInput.value = maxRefundAmount.value
    }
    if (refundAmountInput.value < 0) {
      refundAmountInput.value = 0
    }
  }

  // ==================== 页面跳转 ====================

  const goToShop = () => {
    router.push({ name: 'UserDashboard' })
  }

  const goToPay = (orderNumber: string, amount: number) => {
    router.push({
      name: 'UserPayment',
      query: {
        orderNumber: orderNumber,
        amount: amount.toFixed(2)
      }
    })
  }

  const viewOrderDetail = (orderNumber: string) => {
    router.push({
      name: 'OrderDetail',
      query: { orderNumber: orderNumber }
    })
  }


  // ==================== 退款相关 ====================

  const openRefundModal = (order: Order) => {
    refundOrder.value = order
    refundAmountInput.value = order.totalAmount - (order.refundAmount || 0)
    refundReasonInput.value = ''
    showRefundModal.value = true
  }

  const closeRefundModal = () => {
    showRefundModal.value = false
    refundOrder.value = null
    refundAmountInput.value = 0
    refundReasonInput.value = ''
  }

  const submitRefund = async () => {
    if (!canSubmitRefund.value) return
    
    submitting.value = true
    
    try {
      const response = await authAPI.refundOrder({
        orderNumber: refundOrder.value!.orderNumber,
        refundAmount: refundAmountInput.value,
        refundReason: refundReasonInput.value
      })
      const data = response.data || response
      
      if (data.success) {
        showToast({ message: '退款申请已提交', type: 'success' })
        closeRefundModal()
        page.value = 1
        orders.value = []
        loadOrders()
      } else {
        showToast({ message: data.message || '退款失败', type: 'fail' })
      }
    } catch (error: any) {
      console.error('退款失败:', error)
      showToast({ message: error.message || '退款失败，请重试', type: 'fail' })
    } finally {
      submitting.value = false
    }
  }

  // ==================== 确认收货 ====================

  /**
   * 确认收货
   */
  const confirmReceipt = async (orderNumber: string) => {
    try {
      await showConfirmDialog({
        title: '确认收货',
        message: '请确认已收到商品，确认后订单将变为已完成状态。',
        confirmButtonText: '确认收货',
        cancelButtonText: '再想想'
      })
      
      const response = await authAPI.confirmReceipt(orderNumber)
      const data = response.data || response
      
      if (data.success) {
        showToast({ message: '收货成功', type: 'success' })
        // 刷新订单列表
        page.value = 1
        orders.value = []
        loadOrders()
      } else {
        showToast({ message: data.message || '操作失败', type: 'fail' })
      }
    } catch (error) {
      // 用户取消操作，不做任何处理
    }
  }

  // ==================== 切换标签 ====================

  const switchTab = (tab: string) => {
    currentTab.value = tab
    page.value = 1
    orders.value = []
    loadOrders()
  }

  // ==================== 加载订单数据 ====================

  const loadOrders = async () => {
    if (loading.value) return
    
    loading.value = true
    
    try {
      const userId = userStore.userId
      if (!userId){
        showToast({ message: '未注册，请稍后再试', type: 'fail' })
        return
      }
      
      const response = await authAPI.getUserOrders(userId, page.value, pageSize.value)
      const data = response.data || response
      
      if (data.success) {
        const newOrders = data.orders || []
        if (page.value === 1) {
          orders.value = newOrders
        } else {
          orders.value = [...orders.value, ...newOrders]
        }
        total.value = data.total
        hasMore.value = data.hasMore
      } else {
        showToast({ message: data.message || '加载失败', type: 'fail' })
      }
      
    } catch (error) {
      showToast({ message: '加载失败，请重试', type: 'fail' })
    } finally {
      loading.value = false
    }
  }

  const loadMore = () => {
    if (hasMore.value && !loading.value) {
      page.value++
      loadOrders()
    }
  }

  // ==================== 生命周期 ====================
  onMounted(() => {
    loadOrders()
  })
</script>

<style scoped>
@import url('@/static/css/user/订单页.css');
</style>