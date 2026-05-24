<template>
  <div class="logistics-container">
    <!-- ========== 页面头部 - 与订单详情页呼应 ========== -->
    <div class="page-header">
      <button class="back-btn" @click="goBack">
        <i class="fas fa-arrow-left"></i>
        <span>返回</span>
      </button>
      <h1 class="page-title">
        <i class="fas fa-truck"></i>
        <span>物流详情</span>
      </h1>
      <div class="header-placeholder"></div>
    </div>

    <!-- ========== 加载状态 - 骨架屏 ========== -->
    <div v-if="loading" class="skeleton-detail">
      <div class="skeleton-section">
        <div class="skeleton skeleton-section-title"></div>
        <div class="skeleton-section-content">
          <div class="skeleton skeleton-line long"></div>
          <div class="skeleton skeleton-line medium"></div>
        </div>
      </div>
      <div class="skeleton-section" style="margin-top: 16px;">
        <div class="skeleton skeleton-section-title"></div>
        <div class="skeleton" style="height: 120px; margin-top: 12px;"></div>
      </div>
      <div class="skeleton-section" style="margin-top: 16px;">
        <div class="skeleton skeleton-section-title"></div>
        <div class="skeleton-order-list">
          <div v-for="i in 3" :key="i" class="skeleton-order-card">
            <div class="skeleton-order-goods">
              <div class="skeleton" style="width: 64px; height: 64px; border-radius: 8px;"></div>
              <div style="flex: 1;">
                <div class="skeleton skeleton-line long" style="margin-bottom: 10px;"></div>
                <div class="skeleton skeleton-line medium"></div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ========== 物流信息 ========== -->
    <div v-else class="logistics-content">
      <div v-if="orderInfo.orderNumber" class="order-number-row">
        <span class="order-label">订单号</span>
        <span class="order-value">{{ orderInfo.orderNumber }}</span>
      </div>

      <!-- ========== 商品信息卡片 ========== -->
      <div class="product-info-card" v-for="(item, idx) in productItems" :key="idx"
        :style="idx > 0 ? 'border-top:1px solid #f0f2f5;' : ''">
        <img :src="item.productImage" class="product-image" />
        <div class="product-detail">
          <div class="product-name">{{ item.productName }}</div>
          <div class="tags-group">
            <span v-if="item.skuName" class="tag-spec">{{ item.skuName }}</span>
            <span class="tag-quantity">x{{ item.quantity }}</span>
          </div>
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
          <div class="info-row" v-if="orderInfo.shippedAt || isReturnLogistics">
            <span class="info-label">{{ isReturnLogistics ? '退货时间：' : '发货时间：' }}</span>
            <span class="info-value">{{ formatDateTime(orderInfo.shippedAt || orderInfo.returnApplyTime || '') }}</span>
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
            <p class="tips">请等待快递员揽件</p>
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
        <button class="btn-outline" @click="goBack">
          <i class="fas fa-arrow-left"></i>
          返回
        </button>
        <button
          v-if="orderInfo.trackingNumber"
          class="btn-primary"
          @click="refreshLogistics"
          :disabled="refreshing"
        >
          <i v-if="refreshing" class="fas fa-spinner fa-spin"></i>
          {{ refreshing ? '刷新中...' : '刷新物流' }}
        </button>
        <button
          v-if="showConfirmButton"
          class="btn-success"
          @click="confirmReturnReceive"
          :disabled="confirming"
        >
          <i v-if="confirming" class="fas fa-spinner fa-spin"></i>
          {{ confirming ? '确认中...' : '确认收货' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { ref, computed, onMounted } from 'vue'
  import { useRoute, useRouter } from 'vue-router'
  import { authAPI } from '@/api/authAPI'
  import Message from '@/utils/message'
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
    returnApplyTime: string | null
    trackingNumber: string | null
    logisticsCode: string
    logisticsName: string
  }

  interface LogisticsTrace {
    time: string
    description: string
    status?: string
    location?: string
  }

  interface LogisticsInfo {
  trackingNumber: string
  logisticsCode: string
  logisticsName: string
  traces: LogisticsTrace[]
}

interface ProductInfo {
  productName: string
  productImage: string
  skuName?: string
  quantity: number
}

// ==================== 响应式数据 ====================

  const loading = ref(true)
  const refreshing = ref(false)
  const confirming = ref(false)

  const productItems = ref<ProductInfo[]>([])

  const refundId = ref<string | null>(null)
  const isReturnLogistics = ref(false)

  const orderInfo = ref<OrderInfo>({
    id: null,
    orderNumber: '',
    status: '',
    totalAmount: 0,
    shippedAt: null,
    returnApplyTime: null,
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

  const showConfirmButton = computed(() => {
    return isReturnLogistics.value && logisticsInfo.value.trackingNumber
  })

  // ==================== 数据加载 ====================

  /**
   * 加载物流信息
   * @description 根据订单ID获取订单和物流详情，或直接根据物流单号查询
   * @returns {Promise<void>}
   */
  const loadLogistics = async () => {
    loading.value = true

    try {
      const orderId = route.query.orderId
      const trackingNumber = route.query.trackingNumber as string
      const logisticsName = route.query.logisticsName as string
      const refundParam = route.query.refundId as string

      // === 场景 1：退货物流（有 refundId） ===
      if (route.query.refundId) {
        // 通过 refundId 获取退货物流信息
        const refundRes = await authAPI.getRefundDetail(Number(route.query.refundId))
        if (refundRes.success && refundRes.data) {
          const refund = refundRes.data
          isReturnLogistics.value = true
          refundId.value = route.query.refundId as string

          orderInfo.value = {
            id: null,
            orderNumber: refund.orderNumber || '',
            status: 'RETURNING',
            totalAmount: 0,
            shippedAt: refund.returnApplyTime || refund.applyTime || '',
            returnApplyTime: refund.returnApplyTime || refund.applyTime || '',
            trackingNumber: refund.returnTrackingNumber || '',
            logisticsCode: '',
            logisticsName: refund.returnLogisticsName || ''
          }

          logisticsInfo.value = {
            trackingNumber: refund.returnTrackingNumber || '',
            logisticsCode: '',
            logisticsName: refund.returnLogisticsName || '',
            traces: []
          }

          // ===== 退货物流：通过 refundId 获取商品信息 =====
          productItems.value = [{
            productName: refund.productName || '',
            productImage: refund.productImage || '',
            skuName: refund.skuName || '',
            quantity: refund.quantity || 1
          }]

          // 查询物流轨迹
          if (refund.returnTrackingNumber) {
            const res = await authAPI.getLogisticsByTrackingNumber(
              refund.returnTrackingNumber,
              refund.returnLogisticsName
            )
            if (res.success && res.data) {
              logisticsTraces.value = res.data.traces || []
            } else {
              logisticsTraces.value = []
            }
          } else {
            logisticsTraces.value = []
          }

          loading.value = false
          return
        }
      }

      // === 场景 2：只有 trackingNumber（可能是退货物流） ===
      if (trackingNumber) {
        isReturnLogistics.value = true
        refundId.value = refundParam || null

        orderInfo.value = {
          id: null,
          orderNumber: '',
          status: 'SHIPPED',
          totalAmount: 0,
          shippedAt: null,
          returnApplyTime: null,
          trackingNumber: trackingNumber,
          logisticsCode: '',
          logisticsName: logisticsName || ''
        }

        logisticsInfo.value = {
          trackingNumber: trackingNumber,
          logisticsCode: '',
          logisticsName: logisticsName || '',
          traces: []
        }

        if (refundParam) {
          const refundRes = await authAPI.getRefundDetail(Number(refundParam))
          if (refundRes.success && refundRes.data) {
            orderInfo.value.orderNumber = refundRes.data.orderNumber || ''
            orderInfo.value.totalAmount = refundRes.data.refundAmount || 0
            orderInfo.value.shippedAt = refundRes.data.returnApplyTime || refundRes.data.applyTime || ''
            orderInfo.value.returnApplyTime = refundRes.data.returnApplyTime || refundRes.data.applyTime || ''
            // ===== 退货物流：通过 refundId 获取商品信息 =====
            productItems.value = [{
              productName: refundRes.data.productName || '',
              productImage: refundRes.data.productImage || '',
              skuName: refundRes.data.skuName || '',
              quantity: refundRes.data.quantity || 1
            }]
          }
        }

        const response = await authAPI.getLogisticsByTrackingNumber(trackingNumber, logisticsName)
        if (response.success && response.data) {
          logisticsTraces.value = response.data.traces || []
        } else {
          logisticsTraces.value = []
        }
        loading.value = false
        return
      }

      // === 场景 3：订单物流（有 orderId） ===
      if (!orderId) {
        Message.error('订单ID不存在')
        router.push({ name: 'SellerOrders' })
        return
      }

      const response = await authAPI.getLogisticsInfo(Number(orderId))

      if (response.success && response.data) {
        if (response.data.order) {
          const order = response.data.order
          orderInfo.value = {
            id: order.id,
            orderNumber: order.orderNumber,
            status: order.status,
            totalAmount: order.totalAmount,
            shippedAt: order.shippedAt,
            returnApplyTime: order.returnApplyTime || null,
            trackingNumber: order.trackingNumber,
            logisticsCode: order.logisticsCode || '',
            logisticsName: order.logisticsName || ''
          }
        }

        if (response.data.logistics) {
          const logistics = response.data.logistics
          logisticsInfo.value = {
            trackingNumber: logistics.trackingNumber || '',
            logisticsCode: logistics.logisticsCode || '',
            logisticsName: logistics.logisticsName || '',
            traces: logistics.traces || []
          }
          logisticsTraces.value = logistics.traces || []
        }

        // ===== 订单物流：通过 orderId 获取订单项 =====
        const orderRes = await authAPI.getSellerOrderDetail(Number(orderId))
        if (orderRes.success) {
          const items = orderRes.data?.orderDetail?.orderItems || []
          productItems.value = items.map((item: any) => ({
            productName: item.productName || '',
            productImage: item.productImage || '',
            skuName: item.skuName || '',
            quantity: item.quantity || 1
          }))
        }
      } else {
        Message.error(response.message || '获取物流信息失败')
        setTimeout(() => {
          router.push({ name: 'SellerOrders' })
        }, 2000)
      }
    } catch (error) {
      Message.error('加载失败，请稍后重试')
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
      const trackingNumber = logisticsInfo.value.trackingNumber

      if (isReturnLogistics.value && trackingNumber) {
        const response = await authAPI.getLogisticsByTrackingNumber(trackingNumber, logisticsInfo.value.logisticsName)
        if (response.success && response.data) {
          logisticsTraces.value = response.data.traces || []
          Message.success('刷新成功')
        } else {
          Message.error(response.message || '刷新失败')
        }
      } else {
        const orderId = orderInfo.value.id
        const response = await authAPI.refreshLogistics(Number(orderId))

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
      }
    } catch (error: any) {
      Message.error(error.message || '刷新物流失败')
    } finally {
      refreshing.value = false
    }
  }

  /**
   * 商家确认退货收货
   * @description 商家确认收到退货商品，触发退款流程
   * @returns {Promise<void>}
   */
  const confirmReturnReceive = async () => {
    if (!refundId.value) {
      Message.error('退款记录不存在')
      return
    }

    try {
      await Message.confirm('确认已收到退货商品？确认后将自动处理退款。', '确认收货')

      confirming.value = true

      const response = await authAPI.sellerConfirmReturn(Number(refundId.value))

      if (response.success) {
        Message.success('确认收货成功，退款已处理')
        setTimeout(() => {
          router.back()
        }, 1500)
      } else {
        Message.error(response.message || '操作失败')
      }
    } catch (error: any) {
      if (error !== 'cancel') {
        Message.error(error.message || '操作失败')
      }
    } finally {
      confirming.value = false
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

  // ==================== 生命周期 ====================

  onMounted(() => {
    if (!authStore.validateUserPermission()) return
    loadLogistics()
  })
</script>

<style scoped>
@import url('@/static/css/seller/商家物流.css');
</style>
