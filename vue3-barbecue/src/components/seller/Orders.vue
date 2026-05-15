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
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="order in orders" :key="order.id">
              <td class="order-no">{{ order.orderNumber }}</td>
              <td>{{ formatDate(order.createdAt) }}</td>
              <td class="amount">¥{{ formatPrice(order.totalAmount) }}</td>
              <td>{{ getPaymentMethodText(order.paymentMethod) }}</td>
              <td>{{ getDeliveryTypeText(order.deliveryType) }}</td>
              <td>
                <span class="status-badge" :class="getStatusClass(order.status)">
                  {{ getStatusText(order.status) }}
                </span>
              </td>
              <td class="actions">
                <button class="detail-btn" @click="viewDetail(order.orderNumber)">
                  查看详情
                </button>
                <!-- 已支付且为外卖配送：显示发货按钮 -->
                <button 
                  v-if="order.status === 'PAID' && order.deliveryType === 'delivery'"
                  class="ship-btn"
                  @click="shipOrder(order.orderNumber)"
                >
                  发货
                </button>
                <!-- 已支付且为到店用餐/打包自取：显示核销按钮 -->
                <button 
                  v-if="order.status === 'PAID' && (order.deliveryType === 'dinein' || order.deliveryType === 'takeaway')"
                  class="complete-btn"
                  @click="completeOrder(order.orderNumber)"
                >
                  核销
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      
      <!-- 分页 -->
      <div class="pagination" v-if="total > pageSize">
        <button @click="prevPage" :disabled="page === 1">上一页</button>
        <span class="page-info">{{ page }} / {{ totalPages }}</span>
        <button @click="nextPage" :disabled="page === totalPages">下一页</button>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
  import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
  import { useRouter } from 'vue-router'
  import { showToast, showConfirmDialog } from 'vant'
  import { authAPI } from '@/api/authAPI'
  import 'vant/es/dialog/style'
  'vant/es/toast/style'
  
  // ==================== 类型定义 ====================
  interface Order {
    id: number
    orderNumber: string
    totalAmount: number
    refundedAmount: number
    status: string
    paymentMethod: string
    deliveryType: string
    createdAt: string
  }

  // ==================== 订单状态常量 ====================
  const ORDER_STATUS = {
    PENDING: 'PENDING',
    PAID: 'PAID',
    SHIPPED: 'SHIPPED',
    COMPLETED: 'COMPLETED',
    CANCELLED: 'CANCELLED',
    REFUNDING: 'REFUNDING',
    REFUNDED: 'REFUNDED'
  }

  const ORDER_STATUS_TEXT: Record<string, string> = {
    [ORDER_STATUS.PENDING]: '待支付',
    [ORDER_STATUS.PAID]: '已支付',
    [ORDER_STATUS.SHIPPED]: '已发货',
    [ORDER_STATUS.COMPLETED]: '已完成',
    [ORDER_STATUS.CANCELLED]: '已取消',
    [ORDER_STATUS.REFUNDING]: '退款中',
    [ORDER_STATUS.REFUNDED]: '已退款'
  }

  const ORDER_STATUS_CLASS: Record<string, string> = {
    [ORDER_STATUS.PENDING]: 'pending',
    [ORDER_STATUS.PAID]: 'paid',
    [ORDER_STATUS.SHIPPED]: 'shipped',
    [ORDER_STATUS.COMPLETED]: 'completed',
    [ORDER_STATUS.CANCELLED]: 'cancelled',
    [ORDER_STATUS.REFUNDING]: 'refunding',
    [ORDER_STATUS.REFUNDED]: 'refunded'
  }

  const PAYMENT_METHOD_TEXT: Record<string, string> = {
    WECHAT: '微信支付',
    ALIPAY: '支付宝'
  }

  const DELIVERY_TYPE_TEXT: Record<string, string> = {
    dinein: '到店用餐',
    takeaway: '打包自取',
    delivery: '外卖配送'
  }

  // ==================== 路由 ====================
  const router = useRouter()

  // ==================== 响应式数据 ====================
  const loading = ref(false)
  const orders = ref<Order[]>([])
  const page = ref(1)
  const pageSize = ref(20)
  const total = ref(0)
  const statusFilter = ref('')

  // ==================== WebSocket 相关 ====================
  let stompClient: any = null
  let wsConnected = ref(false)

  // 音频对象（使用你提供的路径）
  const notificationAudio = new Audio('/src/static/radio/订单音频.mp3')

  /**
   * 播放提示音
   */
  const playNotificationSound = () => {
    notificationAudio.play().catch(error => {
      console.log('音频播放失败:', error)
    })
  }

  /**
   * 初始化 WebSocket 连接
   */
  const initWebSocket = () => {
    // 动态导入 SockJS 和 Stomp
    import('sockjs-client').then(({ default: SockJS }) => {
      import('@stomp/stompjs').then(({ Client }) => {
        const client = new Client({
          webSocketFactory: () => new SockJS('/ws'),
          debug: (str) => console.log(str),
          reconnectDelay: 5000,
          onConnect: () => {
            console.log('WebSocket连接成功')
            wsConnected.value = true

            // 订阅新订单通知（商家ID固定为1）
            client.subscribe('/user/1/queue/orders', (message: any) => {
              const data = JSON.parse(message.body)
              console.log('收到新订单通知:', data)
              
              // 播放提示音
              playNotificationSound()
              
              // 显示提示
              showToast({
                message: `新订单！订单号: ${data.orderNumber}`,
                type: 'success',
                duration: 5000
              })
              
              // 刷新订单列表
              refreshOrders()
            })
          },
          onStompError: (frame: any) => {
            console.error('WebSocket错误:', frame)
          }
        })

        client.activate()
        stompClient = client
      })
    }).catch(error => {
      console.error('WebSocket模块加载失败:', error)
    })
  }

  /**
   * 断开 WebSocket 连接
   */
  const disconnectWebSocket = () => {
    if (stompClient) {
      stompClient.deactivate()
      stompClient = null
      wsConnected.value = false
    }
  }

  // ==================== 计算属性 ====================
  const totalPages = computed(() => Math.ceil(total.value / pageSize.value))

  // ==================== 工具方法 ====================

  /**
   * 格式化日期
   */
  const formatDate = (dateStr: string): string => {
    if (!dateStr) return ''
    const date = new Date(dateStr)
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    const hours = String(date.getHours()).padStart(2, '0')
    const minutes = String(date.getMinutes()).padStart(2, '0')
    return `${year}-${month}-${day} ${hours}:${minutes}`
  }

  /**
   * 格式化价格
   */
  const formatPrice = (price: number): string => {
    if (price === undefined || price === null) return '0.00'
    return price.toFixed(2)
  }

  /**
   * 获取订单状态文本
   */
  const getStatusText = (status: string): string => {
    return ORDER_STATUS_TEXT[status] || status
  }

  /**
   * 获取订单状态样式类
   */
  const getStatusClass = (status: string): string => {
    return ORDER_STATUS_CLASS[status] || 'pending'
  }

  /**
   * 获取支付方式文本
   */
  const getPaymentMethodText = (method: string): string => {
    if (!method) return '未支付'
    return PAYMENT_METHOD_TEXT[method] || method
  }

  /**
   * 获取配送类型文本
   */
  const getDeliveryTypeText = (type: string): string => {
    return DELIVERY_TYPE_TEXT[type] || type
  }

  // ==================== 业务方法 ====================

  /**
   * 加载订单列表
   */
  const loadOrders = async () => {
    loading.value = true
    try {
      const response = await authAPI.getSellerOrders(page.value, pageSize.value, statusFilter.value)
      
      if (response.success) {
        orders.value = response.data || []
        total.value = response.total || 0
      } else {
        showToast({
          message: response.message || '加载失败',
          type: 'fail'
        })
      }
    } catch (error) {
      showToast({
        message: '加载失败',
        type: 'fail'
      })
    } finally {
      loading.value = false
    }
  }

  /**
   * 刷新订单列表
   */
  const refreshOrders = () => {
    page.value = 1
    loadOrders()
  }

  /**
   * 查看订单详情
   */
  const viewDetail = (orderNumber: string) => {
    router.push({name: 'OrderDetail', query: { orderNumber } })
  }

  /**
   * 发货（外卖配送）
   */
  const shipOrder = async (orderNumber: string) => {
    try {
      await showConfirmDialog({
        title: '确认发货',
        message: '确定要将该订单标记为已发货吗？',
        confirmButtonText: '确认发货',
        cancelButtonText: '再想想'
      })
      
      const response = await authAPI.shipOrder(orderNumber)
      const data = response.data || response
      
      if (data.success) {
        showToast({ message: '发货成功', type: 'success' })
        loadOrders()
      } else {
        showToast({ message: data.message || '发货失败', type: 'fail' })
      }
    } catch (error) {
      // 用户取消操作，不做任何处理
    }
  }

  /**
   * 核销（到店用餐/打包自取）
   */
  const completeOrder = async (orderNumber: string) => {
    try {
      await showConfirmDialog({
        title: '确认核销',
        message: '确定要将该订单标记为已完成吗？',
        confirmButtonText: '确认核销',
        cancelButtonText: '再想想'
      })
      
      const response = await authAPI.completeOrder(orderNumber)
      const data = response.data || response
      
      if (data.success) {
        showToast({ message: '核销成功', type: 'success' })
        loadOrders()
      } else {
        showToast({ message: data.message || '核销失败', type: 'fail' })
      }
    } catch (error) {
      // 用户取消操作，不做任何处理
    }
  }

  /**
   * 上一页
   */
  const prevPage = () => {
    if (page.value > 1) {
      page.value--
      loadOrders()
    }
  }

  /**
   * 下一页
   */
  const nextPage = () => {
    if (page.value < totalPages.value) {
      page.value++
      loadOrders()
    }
  }

  // ==================== 监听器 ====================
  watch(statusFilter, () => {
    page.value = 1
    loadOrders()
  })

  // ==================== 生命周期 ====================
  onMounted(() => {
    loadOrders()
    // 初始化 WebSocket 连接
    initWebSocket()
  })

  onUnmounted(() => {
    // 组件卸载时断开 WebSocket
    disconnectWebSocket()
  })
</script>

<style scoped>
@import url('@/static/css/seller/订单管理页.css');
</style>