<template>
  <!-- 用户物流详情页面容器 -->
  <div class="user-logistics-container">

    <!-- ========== 页面头部区域 ========== -->
    <div class="page-header">
      <!-- 返回按钮 -->
      <button class="back-btn" @click="goBack">
        <i class="fas fa-arrow-left"></i>
        返回
      </button>

      <!-- 页面标题 -->
      <h1 class="page-title">
        <i class="fas fa-truck"></i>
        物流详情
      </h1>
    </div>

    <!-- ========== 加载状态 ========== -->
    <!-- 数据加载时显示加载动画 -->
    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>

    <!-- ========== 物流信息内容 ========== -->
    <div v-else-if="orderInfo" class="logistics-content">

      <!-- ========== 订单信息卡片 ========== -->
      <div class="order-info-card">
        <!-- 订单头部：订单号 + 订单状态 -->
        <div class="order-header">
          <span class="order-number">订单号：{{ orderInfo.orderNumber }}</span>
          <span class="order-status" :class="getStatusClass(orderInfo.status)">
            {{ getStatusText(orderInfo.status) }}
          </span>
        </div>
        <!-- 订单金额 -->
        <div class="order-amount">
          实付款：¥{{ formatPrice(orderInfo.totalAmount) }}
        </div>
      </div>

      <!-- ========== 物流进度卡片（已发货状态显示） ========== -->
      <!-- 只有当订单状态为已发货、已送达或已完成时才显示物流信息 -->
      <div class="logistics-card" v-if="orderInfo.trackingNumber">
        <!-- 物流卡片头部 -->
        <div class="logistics-header">
          <i class="fas fa-truck"></i>
          <span class="logistics-title">物流信息</span>
        </div>

        <!-- 物流公司信息区域 -->
        <div class="logistics-info">
          <!-- 物流公司名称 -->
          <div class="info-row">
            <span class="info-label">物流公司：</span>
            <span class="info-value">{{ logisticsInfo.logisticsName || '-' }}</span>
          </div>
          <!-- 物流单号 -->
          <div class="info-row">
            <span class="info-label">物流单号：</span>
            <span class="info-value">{{ logisticsInfo.trackingNumber || '-' }}</span>
          </div>
          <!-- 发货时间 -->
          <div class="info-row">
            <span class="info-label">发货时间：</span>
            <span class="info-value">{{ formatDateTime(orderInfo.shippedAt) }}</span>
          </div>
        </div>

        <!-- 物流轨迹时间线 -->
        <div class="logistics-timeline">
          <!-- 遍历物流轨迹列表，显示每个节点 -->
          <div
            v-for="(trace, index) in logisticsTraces"
            :key="index"
            class="timeline-item"
            :class="{ 'is-first': index === 0 }"
          >
            <!-- 时间线圆点 -->
            <div class="timeline-dot"></div>
            <!-- 轨迹内容 -->
            <div class="timeline-content">
              <!-- 轨迹时间 -->
              <div class="timeline-time">{{ formatDateTime(trace.time) }}</div>
              <!-- 轨迹描述 -->
              <div class="timeline-status">{{ trace.status }}</div>
              <!-- 轨迹位置（如果有） -->
              <div class="timeline-location" v-if="trace.location">
                <i class="fas fa-map-marker-alt"></i>
                {{ trace.location }}
              </div>
            </div>
          </div>

          <!-- 暂无轨迹：没有物流信息时显示 -->
          <div v-if="logisticsTraces.length === 0" class="no-trace">
            <i class="fas fa-box-open"></i>
            <p>暂无物流信息</p>
          </div>
        </div>
      </div>

      <!-- 未发货提示卡片 -->
      <div v-if="!orderInfo.trackingNumber" class="not-shipped-card">
        <i class="fas fa-box"></i>
        <p>该订单尚未发货</p>
        <p class="tips">卖家发货后将显示物流信息</p>
      </div>

      <!-- 操作按钮组 -->
      <div class="action-buttons">
        <!-- 返回按钮 -->
        <button class="btn-outline" @click="goBack">返回</button>

        <!-- 确认收货按钮（仅已发货订单显示） -->
        <button
          v-if="orderInfo.status === 'SHIPPED'"
          class="btn-success"
          @click="confirmReceive"
          :disabled="confirming"
        >
          <i v-if="confirming" class="fas fa-spinner fa-spin"></i>
          {{ confirming ? '确认中...' : '确认收货' }}
        </button>

        <!-- 刷新物流按钮（仅已发货订单显示） -->
        <button
          v-if="orderInfo.trackingNumber"
          class="btn-primary"
          @click="refreshLogistics"
          :disabled="refreshing"
        >
          <!-- 刷新中显示旋转图标 -->
          <i v-if="refreshing" class="fas fa-spinner fa-spin"></i>
          {{ refreshing ? '刷新中...' : '刷新物流' }}
        </button>
      </div>
    </div>

    <!-- ========== 订单不存在状态 ========== -->
    <div v-else class="empty-state">
      <i class="fas fa-box-open"></i>
      <p>订单不存在</p>
      <button class="btn-primary" @click="goBack">返回订单列表</button>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { ref, onMounted } from 'vue'
  import { useRoute, useRouter } from 'vue-router'
  import { authAPI } from '@/api/authAPI'
  import Message from '@/utils/message'
  import { useAuthStore } from '@/stores/auth'
  import { storeToRefs } from 'pinia'

  const route = useRoute()
  const router = useRouter()
  const authStore = useAuthStore()
  const { isLoggedIn, role, status } = storeToRefs(authStore)

  const loading = ref(false)
  const refreshing = ref(false)
  const confirming = ref(false)

  interface OrderInfo {
    id: number | null
    orderNumber: string
    status: string
    totalAmount: number
    shippedAt: string | null
    trackingNumber: string | null
    logisticsCode: string
    logisticsName: string
  }

  interface LogisticsInfo {
    trackingNumber: string
    logisticsCode: string
    logisticsName: string
    traces: any[]
  }

  interface LogisticsTrace {
    time: string
    description: string
  }

  const orderInfo = ref<OrderInfo>({
    id: null,
    orderNumber: '',
    status: '',
    totalAmount: 0,
    shippedAt: null,
    trackingNumber: null,
    logisticsCode: '',
    logisticsName: ''
  })

  const logisticsInfo = ref<LogisticsInfo>({
    trackingNumber: '',
    logisticsCode: '',
    logisticsName: '',
    traces: []
  })

  const logisticsTraces = ref<LogisticsTrace[]>([])

  /**
   * 加载订单和物流信息
   * @description 根据订单ID获取订单详情及物流轨迹，失败时返回订单列表页
   * @returns {Promise<void>}
   */
  const loadLogistics = async () => {
    loading.value = true

    try {
      const orderId = route.query.orderId

      if (!orderId) {
        Message.error('订单ID不存在')
        router.push({ name: 'UserOrders' })
        return
      }

      const response = await authAPI.getUserLogisticsInfo(Number(orderId))

      if (response.success && response.data) {
        const order = response.data.order

        if (order) {
          orderInfo.value = {
            id: order.id,
            orderNumber: order.orderNumber,
            status: order.status,
            totalAmount: order.totalAmount,
            shippedAt: order.shippedAt,
            trackingNumber: order.trackingNumber,
            logisticsCode: order.logisticsCode || '',
            logisticsName: order.logisticsName || ''
          }
        }

        const logistics = response.data.logistics

        if (logistics) {
          logisticsInfo.value = {
            trackingNumber: logistics.trackingNumber || '',
            logisticsCode: logistics.logisticsCode || '',
            logisticsName: logistics.logisticsName || '',
            traces: logistics.traces || []
          }
          logisticsTraces.value = logistics.traces || []
        }
      } else {
        Message.error(response.message || '获取物流信息失败')
      }
    } catch (error: any) {
      console.error('加载物流信息失败:', error)
      Message.error(error.message || '加载失败')
    } finally {
      loading.value = false
    }
  }

  /**
   * 手动刷新物流轨迹
   * @description 调用快递鸟API获取最新物流信息，刷新后更新轨迹列表
   * @returns {Promise<void>}
   */
  const refreshLogistics = async () => {
    refreshing.value = true

    try {
      const orderId = orderInfo.value.id

      const response = await authAPI.refreshUserLogistics(orderId)

      if (response.success && response.data?.logistics) {
        const logistics = response.data.logistics

        logisticsInfo.value = {
          trackingNumber: logistics.trackingNumber || '',
          logisticsCode: logistics.logisticsCode || '',
          logisticsName: logistics.logisticsName || '',
          traces: logistics.traces || []
        }
        logisticsTraces.value = logistics.traces || []

        Message.success('刷新成功')
      } else {
        Message.error(response.message || '刷新失败')
      }
    } catch (error: any) {
      console.error('刷新物流失败:', error)
      Message.error(error.message || '刷新失败')
    } finally {
      refreshing.value = false
    }
  }

  /**
   * 确认收货
   * @description 用户收到商品后确认收货，订单状态变更为已完成，延迟后跳转订单列表
   * @returns {Promise<void>}
   */
  const confirmReceive = async () => {
    try {
      await Message.confirm('请确认已收到商品，确认后将完成订单', '确认收货')

      confirming.value = true

      const loading = Message.loading({ message: '确认中...' })

      const response = await authAPI.confirmReceive(orderInfo.value.id)

      loading.close()

      if (response.success) {
        Message.success('确认收货成功')
        orderInfo.value.status = 'COMPLETED'

        // 延迟跳转，确保提示对用户可见
        setTimeout(() => {
          router.push('/user/orders')
        }, 1500)
      } else {
        Message.error(response.message || '确认收货失败')
      }
    } catch (error: any) {
      // 用户主动取消确认弹窗时静默处理
      if (error !== 'cancel') {
        console.error('确认收货失败:', error)
        Message.error(error.message || '确认收货失败')
      }
    } finally {
      confirming.value = false
    }
  }

  /**
   * 返回上一页
   */
  const goBack = () => {
    router.back()
  }

  /**
   * 格式化价格
   * @param {number} price - 原始价格
   * @returns {string} 保留两位小数的价格字符串
   */
  const formatPrice = (price: number): string => {
    if (price == null) return '0.00'
    return price.toFixed(2)
  }

  /**
   * 格式化日期时间
   * @param {string} dateStr - ISO格式日期字符串
   * @returns {string} 格式化后的日期时间 "YYYY-MM-DD HH:mm:ss"
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
      const seconds = String(date.getSeconds()).padStart(2, '0')
      return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
    } catch {
      return '-'
    }
  }

  /**
   * 获取订单状态对应的CSS类名
   * @param {string} status - 订单状态枚举值
   * @returns {string} CSS类名
   */
  const getStatusClass = (status: string): string => {
    const classes: Record<string, string> = {
      PENDING: 'status-pending',
      PAID: 'status-paid',
      PROCESSING: 'status-processing',
      SHIPPED: 'status-shipped',
      DELIVERED: 'status-delivered',
      COMPLETED: 'status-completed',
      CANCELLED: 'status-cancelled',
      REFUNDED: 'status-refunded'
    }
    return classes[status] || ''
  }

  /**
   * 获取订单状态显示文字
   * @param {string} status - 订单状态枚举值
   * @returns {string} 中文状态描述
   */
  const getStatusText = (status: string): string => {
    const texts: Record<string, string> = {
      PENDING: '待付款',
      PAID: '已付款',
      PROCESSING: '处理中',
      SHIPPED: '已发货',
      DELIVERED: '已送达',
      COMPLETED: '已完成',
      CANCELLED: '已取消',
      REFUNDED: '已退款'
    }
    return texts[status] || status
  }

  /**
   * 页面初始化
   * @description 校验用户权限后加载物流信息
   */
  onMounted(async () => {
    if (!authStore.validateUserPermission()) return
    await loadLogistics()
  })
</script>

<style scoped>
@import url('@/static/css/user/物流页.css');
</style>
