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
  import { showToast, showConfirmDialog, showLoadingToast } from 'vant'
  import 'vant/es/toast/style'
  import 'vant/es/dialog/style'
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
        showToast({ message: '订单ID不存在', type: 'fail' })
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
        showToast({ message: response.message || '获取物流信息失败', type: 'fail' })
      }
    } catch (error: any) {
      console.error('加载物流信息失败:', error)
      showToast({ message: error.message || '加载失败', type: 'fail' })
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
    } catch (error: any) {
      console.error('刷新物流失败:', error)
      showToast({ message: error.message || '刷新失败', type: 'fail' })
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
      await showConfirmDialog({
        title: '确认收货',
        message: '请确认已收到商品，确认后将完成订单',
        confirmButtonText: '确认收货',
        cancelButtonText: '取消'
      })
      
      confirming.value = true
      
      const toast = showLoadingToast({
        message: '确认中...',
        duration: 0,
        forbidClick: true
      })
      
      const response = await authAPI.confirmReceive(orderInfo.value.id)
      
      toast.close()
      
      if (response.success) {
        showToast({ message: '确认收货成功', type: 'success' })
        orderInfo.value.status = 'COMPLETED'
        
        // 延迟跳转，确保Toast对用户可见
        setTimeout(() => {
          router.push('/user/orders')
        }, 1500)
      } else {
        showToast({ message: response.message || '确认收货失败', type: 'fail' })
      }
    } catch (error: any) {
      // 用户主动取消确认弹窗时静默处理
      if (error !== 'cancel') {
        console.error('确认收货失败:', error)
        showToast({ message: error.message || '确认收货失败', type: 'fail' })
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
/* ========== 页面容器 ========== */
/* 限制最大宽度，水平居中，设置背景色 */
.user-logistics-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
  min-height: 100vh;
  background: #f5f7fa;
}

/* ========== 页面头部 ========== */
/* 页面头部区域 */
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

/* 页面主标题 */
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

/* 旋转动画 */
@keyframes spin {
  to { transform: rotate(360deg); }
}

/* ========== 空状态 ========== */
/* 空状态容器 */
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

/* ========== 订单信息卡片 ========== */
/* 订单信息卡片 */
.order-info-card {
  background: white;
  border-radius: 12px;
  padding: 16px 20px;
  margin-bottom: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

/* 订单头部 */
.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

/* 订单号 */
.order-number {
  font-size: 14px;
  font-weight: 500;
  color: #1e293b;
}

/* 订单状态徽章 */
.order-status {
  font-size: 12px;
  font-weight: 500;
  padding: 2px 8px;
  border-radius: 12px;
}

/* 待付款状态 */
.status-pending { background: #fef3c7; color: #d97706; }
/* 已付款状态 */
.status-paid { background: #dbeafe; color: #2563eb; }
/* 处理中状态 */
.status-processing { background: #f3e8ff; color: #9333ea; }
/* 已发货状态 */
.status-shipped { background: #cffafe; color: #0891b2; }
/* 已送达状态 */
.status-delivered { background: #dcfce7; color: #16a34a; }
/* 已完成状态 */
.status-completed { background: #dcfce7; color: #16a34a; }
/* 已取消状态 */
.status-cancelled { background: #fee2e2; color: #dc2626; }
/* 已退款状态 */
.status-refunded { background: #fef3c7; color: #d97706; }

/* 订单金额 */
.order-amount {
  font-size: 14px;
  color: #ef4444;
  font-weight: 500;
}

/* ========== 物流卡片 ========== */
/* 物流信息卡片 */
.logistics-card {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

/* 物流卡片头部 */
.logistics-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px 20px;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
}

/* 物流图标 */
.logistics-header i {
  font-size: 18px;
  color: #4a6491;
}

/* 物流标题 */
.logistics-title {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
}

/* ========== 物流公司信息 ========== */
/* 物流信息区域 */
.logistics-info {
  padding: 16px 20px;
  border-bottom: 1px solid #e2e8f0;
  background: #fafbfc;
}

/* 信息行 */
.info-row {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
  font-size: 14px;
}

/* 最后一行无底部间距 */
.info-row:last-child {
  margin-bottom: 0;
}

/* 信息标签 */
.info-label {
  width: 80px;
  color: #64748b;
}

/* 信息值 */
.info-value {
  color: #1e293b;
  font-weight: 500;
}

/* ========== 物流轨迹时间线 ========== */
/* 时间线容器 */
.logistics-timeline {
  padding: 20px;
}

/* 时间线节点 */
.timeline-item {
  display: flex;
  position: relative;
  padding-bottom: 24px;
}

/* 最后一个节点无底部间距 */
.timeline-item:last-child {
  padding-bottom: 0;
}

/* 时间线圆点 */
.timeline-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #cbd5e1;
  margin-top: 4px;
  margin-right: 16px;
  flex-shrink: 0;
  position: relative;
  z-index: 1;
}

/* 第一个节点圆点（绿色高亮） */
.timeline-item.is-first .timeline-dot {
  background: #10b981;
  width: 12px;
  height: 12px;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.2);
}

/* 时间线连接线 */
.timeline-item::before {
  content: '';
  position: absolute;
  left: 4px;
  top: 14px;
  bottom: 0;
  width: 2px;
  background: #e2e8f0;
}

/* 最后一个节点无连接线 */
.timeline-item:last-child::before {
  display: none;
}

/* 时间线内容 */
.timeline-content {
  flex: 1;
}

/* 轨迹时间 */
.timeline-time {
  font-size: 12px;
  color: #94a3b8;
  margin-bottom: 4px;
}

/* 轨迹描述 */
.timeline-status {
  font-size: 14px;
  color: #1e293b;
  font-weight: 500;
  margin-bottom: 4px;
}

/* 轨迹位置 */
.timeline-location {
  font-size: 12px;
  color: #64748b;
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 位置图标 */
.timeline-location i {
  font-size: 10px;
}

/* 暂无轨迹 */
.no-trace {
  text-align: center;
  padding: 40px;
}

/* 暂无轨迹图标 */
.no-trace i {
  font-size: 48px;
  color: #cbd5e1;
  margin-bottom: 16px;
}

/* 暂无轨迹文字 */
.no-trace p {
  color: #64748b;
}

/* ========== 未发货卡片 ========== */
/* 未发货提示卡片 */
.not-shipped-card {
  background: white;
  border-radius: 12px;
  padding: 60px 20px;
  text-align: center;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

/* 未发货图标 */
.not-shipped-card i {
  font-size: 64px;
  color: #ffc107;
  margin-bottom: 16px;
}

/* 未发货主文字 */
.not-shipped-card p {
  margin: 8px 0;
  color: #666;
  font-size: 14px;
}

/* 未发货提示文字 */
.not-shipped-card .tips {
  color: #999;
  font-size: 12px;
}

/* ========== 操作按钮 ========== */
/* 按钮组容器 */
.action-buttons {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-top: 24px;
}

/* 主要按钮（刷新物流） */
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
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 主要按钮悬停效果 */
.btn-primary:hover:not(:disabled) {
  background: #3a5479;
  transform: translateY(-2px);
}

/* 主要按钮禁用样式 */
.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 成功按钮（确认收货） */
.btn-success {
  padding: 10px 24px;
  background: #10b981;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 成功按钮悬停效果 */
.btn-success:hover:not(:disabled) {
  background: #059669;
  transform: translateY(-2px);
}

/* 成功按钮禁用样式 */
.btn-success:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 次要按钮（返回） */
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

/* 旋转动画 */
.fa-spinner {
  animation: spin 1s linear infinite;
}

/* ========== 响应式 ========== */
/* 移动端适配 */
@media (max-width: 640px) {
  /* 容器内边距减小 */
  .user-logistics-container {
    padding: 16px;
  }
  
  /* 页面头部垂直排列 */
  .page-header {
    flex-direction: column;
    align-items: flex-start;
  }
  
  /* 订单头部垂直排列 */
  .order-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
  
  /* 信息行垂直排列 */
  .info-row {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }
  
  /* 标签宽度自动 */
  .info-label {
    width: auto;
  }
  
  /* 按钮垂直排列 */
  .action-buttons {
    flex-direction: column;
  }
  
  /* 按钮占满宽度 */
  .action-buttons button {
    width: 100%;
    justify-content: center;
  }
}
</style>