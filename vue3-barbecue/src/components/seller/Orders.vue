<template>
  <div class="seller-orders">
    <!-- 页面头部 -->
    <div class="page-header">
      <h2>订单管理</h2>
      <div class="header-actions">
        <select v-model="statusFilter" class="status-filter">
          <option value="">全部订单</option>
          <option value="PENDING">待支付</option>
          <option value="PAID">已支付</option>
          <option value="SHIPPED">已发货</option>
          <option value="COMPLETED">已完成</option>
          <option value="CANCELLED">已取消</option>
          <option value="REFUNDING">退款中</option>
          <option value="REFUNDED">已退款</option>
          <option value="REFUNDFAILED">退款失败</option>
        </select>
        <button class="refresh-btn" @click="refreshOrders">刷新</button>
      </div>
    </div>

    <!-- 订单列表 -->
    <div class="orders-list">
      <div v-if="loading" class="loading-state">
        <div class="loading-spinner"></div>
        <span>加载中...</span>
      </div>
      
      <div v-else-if="orders.length === 0" class="empty-state">
        <div class="empty-icon">📦</div>
        <p>暂无订单</p>
      </div>
      
      <div v-else class="orders-table">
        <table>
          <thead>
            <tr>
              <th>订单号</th>
              <th>下单时间</th>
              <th>订单金额</th>
              <th>支付方式</th>
              <th>配送类型</th>
              <th>订单状态</th>
              <th>退款信息</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="vo in orders" :key="vo.order.id">
              <td class="order-no">{{ vo.order.orderNumber }}</td>
              <td>{{ formatDate(vo.order.createdAt) }}</td>
              <td class="amount">¥{{ formatPrice(vo.order.totalAmount) }}</td>
              <td>{{ getPaymentMethodText(vo.order.paymentMethod) }}</td>
              <td>{{ getDeliveryTypeText(vo.order.deliveryType) }}</td>
              <td>
                <span class="status-badge" :class="getStatusClass(vo.order.status)">
                  {{ getStatusText(vo.order.status) }}
                </span>
              </td>
              <td class="refund-info">
                <span v-if="vo.order.status === 'REFUNDING'" class="refund-tag refunding">退款中</span>
                <span v-else-if="vo.order.status === 'REFUNDED'" class="refund-tag refunded">
                  已退款 ¥{{ formatPrice(vo.refundRecord?.refundAmount || 0) }}
                </span>
                <span v-else-if="vo.order.status === 'REFUNDFAILED'" class="refund-tag refund-failed">退款失败</span>
                <span v-else>-</span>
              </td>
              <td class="actions">
                <button class="detail-btn" @click="viewDetail(vo.order.orderNumber)">查看详情</button>
                
                <button 
                  v-if="vo.order.status === 'PAID' && vo.order.deliveryType === 'delivery'"
                  class="ship-btn"
                  @click="shipOrder(vo.order.orderNumber)"
                >发货</button>
                
                <button 
                  v-if="vo.order.status === 'PAID' && (vo.order.deliveryType === 'dinein' || vo.order.deliveryType === 'takeaway')"
                  class="complete-btn"
                  @click="completeOrder(vo.order.orderNumber)"
                >核销</button>
                
                <button 
                  v-if="vo.order.status === 'REFUNDING'"
                  class="handle-refund-btn"
                  @click="openHandleRefundModal(vo)"
                >处理退款</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      
      <div class="pagination" v-if="total > pageSize">
        <button @click="prevPage" :disabled="page === 1">上一页</button>
        
        <!-- 页码按钮 -->
        <button 
          v-for="p in visiblePages" 
          :key="p"
          class="page-num"
          :class="{ active: p === page }"
          @click="goToPage(p)"
        >{{ p }}</button>
        
        <button @click="nextPage" :disabled="page === totalPages">下一页</button>
        
        <!-- 跳转 -->
        <span class="page-jump">
          跳至
          <input 
            type="number" 
            v-model="jumpPage" 
            :max="totalPages" 
            :min="1"
            @keyup.enter="goToPage(jumpPage)"
          />
          页
        </span>
      </div>
    </div>

    <!-- 处理退款弹窗 -->
    <div v-if="showRefundModal" class="refund-modal-mask" @click.self="closeRefundModal">
      <div class="refund-modal">
        <div class="refund-modal-header">
          <h3>处理退款</h3>
          <button class="close-btn" @click="closeRefundModal">✕</button>
        </div>
        <div class="refund-modal-body">
          <div class="refund-info">
            <div class="info-item">
              <span class="label">订单号：</span>
              <span class="value">{{ handleOrder?.order?.orderNumber }}</span>
            </div>
            <div class="info-item">
              <span class="label">退款金额：</span>
              <span class="value highlight">¥{{ formatPrice(handleOrder?.refundRecord?.refundAmount || 0) }}</span>
            </div>
          </div>
          
          <div class="form-group">
            <label>处理结果 <span class="required">*</span></label>
            <div class="result-options">
              <button 
                class="result-btn success"
                :class="{ active: refundResult === 'SUCCESS' }"
                @click="refundResult = 'SUCCESS'"
              >✅ 退款成功</button>
              <button 
                class="result-btn fail"
                :class="{ active: refundResult === 'FAIL' }"
                @click="refundResult = 'FAIL'"
              >❌ 退款失败</button>
            </div>
          </div>
          
          <div class="form-group" v-if="refundResult === 'FAIL'">
            <label>失败原因 <span class="required">*</span></label>
            <textarea 
              v-model="refundFailReason" 
              rows="3" 
              placeholder="请填写退款失败的原因"
            ></textarea>
          </div>
        </div>
        <div class="refund-modal-footer">
          <button class="cancel-btn" @click="closeRefundModal">取消</button>
          <button class="confirm-btn" @click="submitHandleRefund" :disabled="!canSubmitHandle">
            {{ submitting ? '提交中...' : '确认处理' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
  import { ref, computed, onMounted, watch } from 'vue'
  import { useRouter } from 'vue-router'
  import Message from '@/utils/message'
  import { authAPI } from '@/api/authAPI'

  // ==================== 类型定义 ====================
  interface Order {
    id: number
    orderNumber: string
    totalAmount: number
    deliveryType: string
    paymentMethod: string
    status: string
    createdAt: string
  }

  interface RefundRecord {
    id: number
    orderNumber: string
    refundAmount: number
    refundReason: string
    status: string
    failReason: string
    createdAt: string
  }

  interface OrderVO {
    order: Order
    refundRecord: RefundRecord | null
  }

  // ==================== 常量 ====================
  const ORDER_STATUS_TEXT: Record<string, string> = {
    PENDING: '待支付', PAID: '已支付', SHIPPED: '已发货',
    COMPLETED: '已完成', CANCELLED: '已取消', REFUNDING: '退款中',
    REFUNDED: '已退款', REFUNDFAILED: '退款失败'
  }

  const ORDER_STATUS_CLASS: Record<string, string> = {
    PENDING: 'pending', PAID: 'paid', SHIPPED: 'shipped',
    COMPLETED: 'completed', CANCELLED: 'cancelled', REFUNDING: 'refunding',
    REFUNDED: 'refunded', REFUNDFAILED: 'refund-failed'
  }

  const PAYMENT_METHOD_TEXT: Record<string, string> = {
    WECHAT: '微信支付', ALIPAY: '支付宝'
  }

  const DELIVERY_TYPE_TEXT: Record<string, string> = {
    dinein: '到店用餐', takeaway: '打包自取', delivery: '外卖配送'
  }

  // ==================== 路由 ====================
  const router = useRouter()

  // ==================== 响应式数据 ====================
  const loading = ref(false)
  const submitting = ref(false)
  const orders = ref<OrderVO[]>([])
  const page = ref(1)
  const pageSize = ref(20)
  const total = ref(0)
  const statusFilter = ref('')

  const showRefundModal = ref(false)
  const handleOrder = ref<OrderVO | null>(null)
  const refundResult = ref<'SUCCESS' | 'FAIL' | ''>('')
  const refundFailReason = ref('')

  // 响应式数据新增
  const jumpPage = ref(1)

  // 计算可见页码
  const visiblePages = computed(() => {
    const pages: number[] = []
    const max = totalPages.value
    const current = page.value
    
    let start = Math.max(1, current - 2)
    let end = Math.min(max, current + 2)
    
    if (end - start < 4) {
      if (start === 1) end = Math.min(max, start + 4)
      else start = Math.max(1, end - 4)
    }
    
    for (let i = start; i <= end; i++) pages.push(i)
    return pages
  })

  // 跳转到指定页
  const goToPage = (p: number) => {
    const target = Math.max(1, Math.min(p, totalPages.value))
    if (target !== page.value) {
      page.value = target
      loadOrders()
    }
  }
  // ==================== 计算属性 ====================
  const totalPages = computed(() => Math.ceil(total.value / pageSize.value))

  const canSubmitHandle = computed(() => {
    // 确保 refundResult 必须是 'SUCCESS' 或 'FAIL'
    if (refundResult.value !== 'SUCCESS' && refundResult.value !== 'FAIL') {
      return false
    }
    
    if (refundResult.value === 'FAIL' && !refundFailReason.value.trim()) {
      return false
    }
    
    if (submitting.value) {
      return false
    }
    
    return true
  })

  // ==================== 工具方法 ====================
  const formatDate = (dateStr: string): string => {
    if (!dateStr) return ''
    const d = new Date(dateStr)
    return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
  }

  const formatPrice = (price: number): string => {
    if (price == null) return '0.00'
    return Number(price).toFixed(2)
  }

  const getStatusText = (s: string) => ORDER_STATUS_TEXT[s] || s
  const getStatusClass = (s: string) => ORDER_STATUS_CLASS[s] || 'pending'
  const getPaymentMethodText = (m: string) => m ? (PAYMENT_METHOD_TEXT[m] || m) : '未支付'
  const getDeliveryTypeText = (t: string) => DELIVERY_TYPE_TEXT[t] || t

  // ==================== 业务方法 ====================
  const loadOrders = async () => {
    loading.value = true
    try {
      const response = await authAPI.getSellerOrders(page.value, pageSize.value, statusFilter.value)
      if (response.success) {
        orders.value = response.data.orders || []
        total.value = response.data.total || 0
      } else {
        Message.error(response.message || '加载失败')
      }
    } catch {
      Message.error('加载失败')
    } finally {
      loading.value = false
    }
  }

  const refreshOrders = () => { page.value = 1; loadOrders() }

  const viewDetail = (orderNumber: string) => {
    router.push({ name: 'SellerOrderDetail', query: { orderNumber } })
  }

  const shipOrder = async (orderNumber: string) => {
    try {
      await Message.confirm('确定要将该订单标记为已发货吗？', '确认发货')
      const response = await authAPI.shipOrder(orderNumber)
      response.success ? Message.success('发货成功') : Message.error(response.message || '发货失败')
      loadOrders()
    } catch {}
  }

  const completeOrder = async (orderNumber: string) => {
    try {
      await Message.confirm('确定要将该订单标记为已完成吗？', '确认核销')
      const response = await authAPI.completeOrder(orderNumber)
      response.success ? Message.success('核销成功') : Message.error(response.message || '核销失败')
      loadOrders()
    } catch {}
  }

  const openHandleRefundModal = (vo: OrderVO) => {
    handleOrder.value = vo
    refundResult.value = ''
    refundFailReason.value = ''
    showRefundModal.value = true
  }

  const closeRefundModal = () => {
    showRefundModal.value = false
    handleOrder.value = null
    refundResult.value = ''
    refundFailReason.value = ''
  }

  const submitHandleRefund = async () => {
    if (!canSubmitHandle.value) return
    submitting.value = true
    
    try {
      const response = await authAPI.handleRefund({
        orderNumber: handleOrder.value!.order.orderNumber,
        result: refundResult.value as 'SUCCESS' | 'FAIL',
        failReason: refundResult.value === 'FAIL' ? refundFailReason.value : ''
      })
      if (response.success) {
        Message.success(refundResult.value === 'SUCCESS' ? '退款成功' : '已标记退款失败')
        closeRefundModal()
        loadOrders()
      } else {
        Message.error(response.message || '处理失败')
      }
    } catch {
      Message.error('处理失败，请重试')
    } finally {
      submitting.value = false
    }
  }

  const prevPage = () => { if (page.value > 1) { page.value--; loadOrders() } }
  const nextPage = () => { if (page.value < totalPages.value) { page.value++; loadOrders() } }

  // ==================== 监听器 ====================
  watch(statusFilter, () => { page.value = 1; loadOrders() })

  // ==================== 生命周期 ====================
  onMounted(
    () => loadOrders()
  )
</script>

<style scoped>
@import url('@/static/css/seller/订单管理页.css');
</style>