<template>
  <div class="order-page">
    <div class="order-header">
      <h1 class="page-title">我的订单</h1>
    </div>

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

    <div class="order-list" ref="scrollContainer">
      <!-- 加载状态 -->
      <div v-if="loading && orders.length === 0" class="state-wrapper">
        <div class="loading-state">
          <div class="loading-spinner"></div>
          <span>加载中...</span>
        </div>
      </div>
      
      <!-- 空状态 -->
      <div v-else-if="!loading && orders.length === 0" class="state-wrapper">
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
          v-for="vo in orders" 
          :key="vo.order.id"
          class="order-card"
        >
          <div class="order-card-header">
            <div class="order-info">
              <span class="order-no">订单号：{{ vo.order.orderNumber }}</span>
              <span class="order-time">{{ formatDate(vo.order.createdAt) }}</span>
            </div>
            <div class="order-status" :class="getStatusClass(vo.order.status)">
              {{ getStatusText(vo.order.status) }}
            </div>
          </div>
          
          <div class="refund-info-tag" v-if="vo.order.status === 'REFUNDED'">
            <span class="refund-icon">💰</span>
            <span class="refund-text">已退款 ¥{{ formatPrice(vo.refundRecord?.refundAmount || 0) }}</span>
          </div>
          <div class="refund-info-tag fail" v-else-if="vo.order.status === 'REFUNDFAILED'">
            <span class="refund-icon">❌</span>
            <span class="refund-text">退款失败</span>
          </div>
          
          <div class="order-card-footer">
            <div class="order-amount">
              <span class="amount-label">实付</span>
              <span class="amount-value">¥{{ formatPrice(vo.order.totalAmount) }}</span>
            </div>
            <div class="order-actions">
              <button v-if="vo.order.status === 'PENDING'" class="action-btn pay-btn" @click.stop="goToPay(vo.order.orderNumber, vo.order.totalAmount)">去支付</button>
              <button v-else-if="vo.order.status === 'PAID'" class="action-btn refund-btn" @click.stop="openRefundModal(vo)">申请退款</button>
              <button v-else-if="vo.order.status === 'REFUNDING'" class="action-btn refunding-btn" disabled>🔄 退款中</button>
              <button v-else-if="vo.order.status === 'REFUNDED'" class="action-btn refunded-btn" disabled>✅ 已退款</button>
              <button v-else-if="vo.order.status === 'REFUNDFAILED'" class="action-btn refund-failed-btn" disabled>❌ 退款失败</button>
              <button v-else-if="vo.order.status === 'SHIPPED'" class="action-btn confirm-receipt-btn" @click.stop="confirmReceipt(vo.order.orderNumber)">确认收货</button>
              <button v-else-if="vo.order.status === 'CANCELLED'" class="action-btn cancelled-btn" disabled>已取消</button>
              <button class="action-btn detail-btn" @click.stop="viewOrderDetail(vo.order.orderNumber)">查看详情</button>
            </div>
          </div>
        </div>
        
        <!-- 底部加载状态 -->
        <div v-if="loading && orders.length > 0" class="loading-more">
          <div class="loading-spinner-small"></div>
          <span>加载中...</span>
        </div>
        <div v-if="!hasMore && orders.length > 0" class="no-more">没有更多了~</div>
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
              <span class="value">{{ refundOrder?.order.orderNumber }}</span>
            </div>
            <div class="info-item">
              <span class="label">订单金额：</span>
              <span class="value">¥{{ formatPrice(refundOrder?.order.totalAmount || 0) }}</span>
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
          <button class="confirm-btn" @click="submitRefund" :disabled="!canSubmitRefund">
            {{ submitting ? '提交中...' : '提交申请' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
  import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
  import { useRouter } from 'vue-router'
  import { Toast, Dialog } from '@/utils/vant'
  import { authAPI } from '@/api/authAPI'
  import { useUserStore } from '@/stores/auth'

  const userStore = useUserStore()

  // ==================== 类型定义 ====================
  interface Order { id: number; orderNumber: string; totalAmount: number; status: string; createdAt: string }
  interface RefundRecord { id: number; refundAmount: number; refundReason: string; status: string; failReason: string }
  interface OrderVO { order: Order; refundRecord: RefundRecord | null }

  // ==================== 路由 ====================
  const router = useRouter()

  // ==================== 响应式数据 ====================
  const loading = ref(false)
  const submitting = ref(false)
  const orders = ref<OrderVO[]>([])
  const currentTab = ref('all')
  const page = ref(1)
  const pageSize = ref(20)
  const hasMore = ref(true)
  const total = ref(0)
  const scrollContainer = ref<HTMLElement | null>(null)

  const showRefundModal = ref(false)
  const refundOrder = ref<OrderVO | null>(null)
  const refundAmountInput = ref(0)
  const refundReasonInput = ref('')

  // ==================== 计算属性 ====================
  const maxRefundAmount = computed(() => refundOrder.value?.order.totalAmount || 0)

  const canSubmitRefund = computed(() => {
    if (refundAmountInput.value <= 0) return false
    if (refundAmountInput.value > maxRefundAmount.value) return false
    if (!refundReasonInput.value.trim()) return false
    if (submitting.value) return false
    return true
  })

  const tabs = computed(() => [
    { label: '全部', value: 'all', count: total.value },
    { label: '待支付', value: 'PENDING', count: orders.value.filter(o => o.order.status === 'PENDING').length },
    { label: '已支付', value: 'PAID', count: orders.value.filter(o => o.order.status === 'PAID').length },
    { label: '已发货', value: 'SHIPPED', count: orders.value.filter(o => o.order.status === 'SHIPPED').length },
    { label: '已完成', value: 'COMPLETED', count: orders.value.filter(o => o.order.status === 'COMPLETED').length },
    { label: '已取消', value: 'CANCELLED', count: orders.value.filter(o => o.order.status === 'CANCELLED').length }
  ])

  // ==================== 工具方法 ====================
  const formatPrice = (p: number) => (p ?? 0).toFixed(2)
  const formatDate = (d: string) => {
    if (!d) return ''
    const dt = new Date(d)
    return `${dt.getMonth() + 1}/${dt.getDate()} ${String(dt.getHours()).padStart(2, '0')}:${String(dt.getMinutes()).padStart(2, '0')}`
  }

  const STATUS_MAP: Record<string, string> = {
    PENDING: '待支付', PAID: '已支付', SHIPPED: '已发货', COMPLETED: '已完成',
    CANCELLED: '已取消', REFUNDING: '退款中', REFUNDED: '已退款', REFUNDFAILED: '退款失败'
  }
  const getStatusText = (s: string) => STATUS_MAP[s] || s
  const getStatusClass = (s: string) => `status-${s.toLowerCase()}`

  // ==================== 页面跳转 ====================
  const goToShop = () => router.push({ name: 'UserDashboard' })
  const goToPay = (orderNumber: string, amount: number) => {
    router.push({ name: 'UserPayment', query: { orderNumber, amount: amount.toFixed(2) } })
  }
  const viewOrderDetail = (orderNumber: string) => {
    router.push({ name: 'OrderDetail', query: { orderNumber } })
  }

  // ==================== 退款相关 ====================
  const openRefundModal = (vo: OrderVO) => {
    refundOrder.value = vo
    refundAmountInput.value = vo.order.totalAmount
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
    try { await Dialog.confirm(`确认退款 ¥${formatPrice(refundAmountInput.value)}？`) } catch { return }
    
    submitting.value = true
    Toast.loading('提交退款申请...')
    try {
      const response = await authAPI.refundOrder({
        orderNumber: refundOrder.value!.order.orderNumber,
        refundAmount: refundAmountInput.value,
        refundReason: refundReasonInput.value
      })
      if (response.success) {
        Toast.success('退款申请已提交')
        closeRefundModal()
        resetAndReload()
      } else {
        Toast.fail(response.message || '退款失败')
      }
    } catch (error: any) {
      Toast.fail(error.message || '退款失败，请重试')
    } finally {
      submitting.value = false
    }
  }

  // ==================== 确认收货 ====================
  const confirmReceipt = async (orderNumber: string) => {
    try {
      await Dialog.confirm('请确认已收到商品，确认后订单将变为已完成状态。', {
        confirmButtonText: '确认收货', cancelButtonText: '再想想'
      })
      const response = await authAPI.confirmReceipt(orderNumber)
      if (response.success) { Toast.success('收货成功'); resetAndReload() }
      else { Toast.fail(response.message || '操作失败') }
    } catch {}
  }

  // ==================== 切换标签 ====================
  const switchTab = (tab: string) => {
    if (currentTab.value === tab) return
    currentTab.value = tab
    resetAndReload()
  }

  const resetAndReload = () => {
    page.value = 1
    orders.value = []
    hasMore.value = true
    nextTick(() => scrollContainer.value?.scrollTo(0, 0))
    loadOrders()
  }

  // ==================== 滚动加载 ====================
  const loadOrders = async () => {
    if (loading.value || (!hasMore.value && page.value > 1)) return
    loading.value = true
    
    try {
      const userId = userStore.userId
      if (!userId) { Toast.fail('未登录'); return }
      
      const status = currentTab.value === 'all' ? '' : currentTab.value
      const response = await authAPI.getUserOrders(userId, page.value, pageSize.value, status)
      
      if (response.success) {
        const list = response.data.orders || []
        orders.value = page.value === 1 ? list : [...orders.value, ...list]
        total.value = response.data.total
        hasMore.value = response.data.hasMore
      } else {
        Toast.fail(response.message || '加载失败')
      }
    } catch {
      Toast.fail('加载失败')
    } finally {
      loading.value = false
    }
  }

  const handleScroll = () => {
    const el = scrollContainer.value
    if (!el || loading.value || !hasMore.value) return
    if (el.scrollHeight - el.scrollTop - el.clientHeight < 60) {
      page.value++
      loadOrders()
    }
  }

  // ==================== 生命周期 ====================
  onMounted(async () => {
    await nextTick()
    scrollContainer.value = document.querySelector('.order-list')
    scrollContainer.value?.addEventListener('scroll', handleScroll)
    loadOrders()
  })

  onUnmounted(() => {
    scrollContainer.value?.removeEventListener('scroll', handleScroll)
  })
</script>

<style scoped>
@import url('@/static/css/user/订单管理页.css');
</style>