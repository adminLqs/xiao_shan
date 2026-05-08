<template>
  <!-- 物流详情页面容器 -->
  <div class="logistics-container">
    <!-- ========== 页面头部区域 ========== -->
    <div class="page-header">
      <h1 class="page-title">
        <i class="fas fa-truck"></i>
        物流详情
      </h1>
      <div class="breadcrumb">
        <RouterLink to="/seller/dashboard">商家中心</RouterLink>
        <i class="fas fa-chevron-right"></i>
        <RouterLink to="/seller/orders">订单管理</RouterLink>
        <i class="fas fa-chevron-right"></i>
        <span class="current">物流详情</span>
      </div>
    </div>

    <!-- ========== 加载状态 ========== -->
    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>

    <!-- ========== 物流信息 ========== -->
    <div v-else class="logistics-content">
      <!-- 订单信息卡片 -->
      <div class="order-info-card">
        <div class="order-header">
          <span class="order-number">订单号：{{ orderInfo.orderNumber }}</span>
          <span class="order-status" :class="getStatusClass(orderInfo.status)">
            {{ getStatusText(orderInfo.status) }}
          </span>
        </div>
        <div class="order-amount">
          实付款：¥{{ formatPrice(orderInfo.totalAmount) }}
        </div>
      </div>

      <!-- 物流进度卡片 - 仅在已发货时显示 -->
      <div v-if="orderInfo.trackingNumber" class="logistics-card">
        <div class="logistics-header">
          <i class="fas fa-truck"></i>
          <span class="logistics-title">物流信息</span>
        </div>
        
        <!-- 物流公司信息 -->
        <div class="logistics-info">
          <div class="info-row">
            <span class="info-label">物流公司：</span>
            <span class="info-value">{{ logisticsInfo.logisticsName || '-' }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">物流单号：</span>
            <span class="info-value">{{ logisticsInfo.trackingNumber || '-' }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">发货时间：</span>
            <span class="info-value">{{ formatDateTime(orderInfo.shippedAt) }}</span>
          </div>
        </div>

        <!-- 物流轨迹时间线 -->
        <div class="logistics-timeline">
          <div 
            v-for="(trace, index) in logisticsTraces" 
            :key="index"
            class="timeline-item"
            :class="{ 'is-first': index === 0 }"
          >
            <div class="timeline-dot"></div>
            <div class="timeline-content">
              <div class="timeline-time">{{ formatDateTime(trace.time) }}</div>
              <div class="timeline-status">{{ trace.status }}</div>
              <div class="timeline-location" v-if="trace.location">
                <i class="fas fa-map-marker-alt"></i>
                {{ trace.location }}
              </div>
            </div>
          </div>
          
          <div v-if="logisticsTraces.length === 0" class="no-trace">
            <i class="fas fa-box-open"></i>
            <p>暂无物流信息</p>
          </div>
        </div>
      </div>

      <!-- 未发货提示 -->
      <div v-else class="not-shipped-card">
        <i class="fas fa-box"></i>
        <p>该订单尚未发货</p>
        <p class="tips">请先发货后再查看物流信息</p>
      </div>

      <!-- 操作按钮组 -->
      <div class="action-buttons">
        <button class="btn-outline" @click="goBack">返回</button>
        <button 
          v-if="orderInfo.trackingNumber" 
          class="btn-primary" 
          @click="refreshLogistics"
          :disabled="refreshing"
        >
          <i v-if="refreshing" class="fas fa-spinner fa-spin"></i>
          {{ refreshing ? '刷新中...' : '刷新物流' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { ref, onMounted } from 'vue'
  import { useRoute, useRouter } from 'vue-router'
  import { authAPI } from '@/api/authAPI'
  import { showToast } from 'vant'
  import 'vant/es/toast/style'
  import { useAuthStore } from '@/stores/auth'

  const authStore = useAuthStore()
  const route = useRoute()
  const router = useRouter()

  // ==================== 类型定义 ====================

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

  interface LogisticsTrace {
    time: string
    description: string
  }

  interface LogisticsInfo {
    trackingNumber: string
    logisticsCode: string
    logisticsName: string
    traces: LogisticsTrace[]
  }

  // ==================== 响应式数据 ====================

  const loading = ref(false)
  const refreshing = ref(false)

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

  // ==================== 数据加载 ====================

  /**
   * 加载物流信息
   * @description 根据订单ID获取订单和物流详情
   * @returns {Promise<void>}
   */
  const loadLogistics = async () => {
    loading.value = true
    
    try {
      const orderId = route.query.orderId
      
      if (!orderId) {
        showToast({ message: '订单ID不存在', type: 'fail' })
        router.push('/seller/orders')
        return
      }
      
      const response = await authAPI.getLogisticsInfo(Number(orderId))
      
      if (response.success) {
        if (response.data?.order) {
          const order = response.data.order
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
        
        if (response.data?.logistics) {
          const logistics = response.data.logistics
          logisticsInfo.value = {
            trackingNumber: logistics.trackingNumber || '',
            logisticsCode: logistics.logisticsCode || '',
            logisticsName: logistics.logisticsName || '',
            traces: logistics.traces || []
          }
          logisticsTraces.value = logistics.traces || []
        }
      } else {
        showToast({ message: response.message || '获取物流信息失败', type: 'fail' })
        setTimeout(() => {
          router.push({ name: 'SellerOrders' })
        }, 2000)
      }
    } catch (error) {
      console.error('加载物流信息失败:', error)
      showToast({ message: '加载失败，请稍后重试', type: 'fail' })
    } finally {
      loading.value = false
    }
  }

  /**
   * 刷新物流信息
   * @description 手动刷新最新的物流轨迹数据
   * @returns {Promise<void>}
   */
  const refreshLogistics = async () => {
    refreshing.value = true
    
    try {
      const orderId = orderInfo.value.id
      
      const response = await authAPI.refreshLogistics(Number(orderId))
      
      if (response.success && response.data) {
        const logistics = response.data
        logisticsInfo.value = {
          trackingNumber: logistics.trackingNumber || '',
          logisticsCode: logistics.logisticsCode || '',
          logisticsName: logistics.logisticsName || '',
          traces: logistics.traces || []
        }
        logisticsTraces.value = logistics.traces || []
        showToast({ message: '刷新成功', type: 'success' })
      } else {
        showToast({ message: response.message || '刷新失败', type: 'fail' })
      }
    } catch (error) {
      console.error('刷新物流失败:', error)
      showToast({ message: '刷新失败，请稍后重试', type: 'fail' })
    } finally {
      refreshing.value = false
    }
  }

  // ==================== 页面跳转 ====================

  const goBack = () => {
    router.back()
  }

  // ==================== 工具函数 ====================

  /**
   * 格式化价格
   * @param {number | null} price - 原始价格数值
   * @returns {string} 格式化后的价格字符串
   */
  const formatPrice = (price: number | null): string => {
    if (price == null) return '0.00'
    return price.toFixed(2)
  }

  /**
   * 格式化日期时间
   * @param {string} dateStr - 原始日期字符串
   * @returns {string} 格式化后的日期时间字符串
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
   * 获取订单状态样式类
   * @param {string} status - 订单状态代码
   * @returns {string} CSS类名字符串
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
   * 获取订单状态文字
   * @param {string} status - 订单状态代码
   * @returns {string} 订单状态中文描述
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

  // ==================== 生命周期 ====================

  onMounted(() => {
    if (!authStore.validateUserPermission()) return
    loadLogistics()
  })
</script>

<style scoped>
 @import url('@/static/css/seller/商家物流.css');
</style>