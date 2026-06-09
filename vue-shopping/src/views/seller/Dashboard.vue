<template>
  <div class="dashboard-wrapper">
    <!-- 加载状态 -->
    <div v-if="loading" class="skeleton-form" style="padding: 24px;">
      <div class="skeleton" style="height: 80px; margin-bottom: 24px; border-radius: 12px;"></div>
      <div class="skeleton-stat-grid">
        <div v-for="i in 4" :key="i" class="skeleton-stat-card">
          <div class="skeleton skeleton-stat-icon"></div>
          <div class="skeleton skeleton-stat-value"></div>
          <div class="skeleton skeleton-stat-label"></div>
        </div>
      </div>
      <div class="skeleton-section" style="margin-top: 24px;">
        <div class="skeleton skeleton-section-title" style="margin-bottom: 16px;"></div>
        <div class="skeleton" style="height: 200px; border-radius: 12px;"></div>
      </div>
    </div>

    <div v-else class="seller-dashboard content-wrapper">
    <!-- ========== 欢迎区域 ========== -->
    <div class="welcome-section">
      <div class="welcome-text">
        <h2>欢迎回来，{{ storeName }}！</h2>
        <p>今天是 {{ currentDate }}，祝您生意兴隆！</p>
      </div>
      <div class="welcome-time">
        <span class="time">{{ currentTime }}</span>
      </div>
    </div>

    <!-- ========== 核心数据卡片 ========== -->
    <div class="stats-grid">
      <!-- 今日订单卡片 -->
      <div class="stat-card">
        <div class="stat-icon">
          <i class="fas fa-shopping-cart"></i>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ todayOrderCount }}</div>
          <div class="stat-label">今日订单</div>
        </div>
        <div class="stat-trend" :class="orderTrend > 0 ? 'up' : 'down'">
          <i :class="orderTrend > 0 ? 'fas fa-arrow-up' : 'fas fa-arrow-down'"></i>
          <span>{{ Math.abs(orderTrend) }}%</span>
        </div>
      </div>

      <!-- 今日收入卡片 -->
      <div class="stat-card">
        <div class="stat-icon">
          <i class="fas fa-yen-sign"></i>
        </div>
        <div class="stat-info">
          <div class="stat-value">¥{{ formatPrice(todayIncome) }}</div>
          <div class="stat-label">今日收入</div>
        </div>
        <div class="stat-trend" :class="incomeTrend > 0 ? 'up' : 'down'">
          <i :class="incomeTrend > 0 ? 'fas fa-arrow-up' : 'fas fa-arrow-down'"></i>
          <span>{{ Math.abs(incomeTrend) }}%</span>
        </div>
      </div>

    </div>

    <!-- ========== 待办事项区域 ========== -->
    <div class="todo-section">
      <div class="section-header">
        <h3><i class="fas fa-tasks"></i> 待办事项</h3>
        <span class="section-tip">需要您及时处理</span>
      </div>

      <div class="todo-list">
        <div v-if="pendingCount > 0" class="todo-item urgent" @click="goToOrders('PAID')">
          <div class="todo-icon">
            <i class="fas fa-receipt"></i>
          </div>
          <div class="todo-content">
            <div class="todo-title">您有 {{ pendingCount }} 笔订单待处理</div>
            <div class="todo-desc">买家已付款，请及时确认接单</div>
          </div>
          <div class="todo-action">
            <button class="btn-sm btn-primary">立即处理 →</button>
          </div>
        </div>

        <div v-if="shippingCount > 0" class="todo-item warning" @click="goToOrders('PROCESSING')">
          <div class="todo-icon">
            <i class="fas fa-box"></i>
          </div>
          <div class="todo-content">
            <div class="todo-title">您有 {{ shippingCount }} 笔订单待发货</div>
            <div class="todo-desc">订单已处理，请尽快安排发货</div>
          </div>
          <div class="todo-action">
            <button class="btn-sm btn-primary">立即发货 →</button>
          </div>
        </div>

        <div v-if="refundCount > 0" class="todo-item warning" @click="goToOrders('REFUNDING')">
          <div class="todo-icon">
            <i class="fas fa-undo-alt"></i>
          </div>
          <div class="todo-content">
            <div class="todo-title">您有 {{ refundCount }} 笔退款待处理</div>
            <div class="todo-desc">请及时处理退款申请</div>
          </div>
          <div class="todo-action">
            <button class="btn-sm btn-primary">立即处理 →</button>
          </div>
        </div>

        <div v-if="pendingCount === 0 && shippingCount === 0 && refundCount === 0" class="todo-empty">
          <i class="fas fa-check-circle"></i>
          <span>暂无待办事项，太棒了！</span>
        </div>
      </div>
    </div>

    <!-- ========== 快捷操作区域 ========== -->
    <div class="quick-actions">
      <div class="section-header">
        <h3><i class="fas fa-bolt"></i> 快捷操作</h3>
        <span class="section-tip">常用功能快速入口</span>
      </div>

      <div class="action-grid">
        <div class="action-item" @click="goToProducts">
          <i class="fas fa-box"></i>
          <span>商品管理</span>
        </div>
        <div class="action-item" @click="goToAddProduct">
          <i class="fas fa-plus-circle"></i>
          <span>发布商品</span>
        </div>
        <div class="action-item" @click="goToOrders('PROCESSING')">
          <i class="fas fa-truck"></i>
          <span>快速发货</span>
        </div>
        <div class="action-item" @click="goToProfile">
          <i class="fas fa-store"></i>
          <span>店铺信息</span>
        </div>
      </div>
    </div>

    <!-- ========== 近7天订单趋势 ========== -->
    <div class="trend-section">
      <div class="section-header">
        <h3><i class="fas fa-chart-line"></i> 订单趋势</h3>
        <span class="section-tip">近7天订单数量</span>
      </div>

      <div class="trend-chart">
        <div ref="trendChartRef" style="height:200px;"></div>
      </div>
    </div>
  </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { authAPI } from '@/api/authAPI'
import Message from '@/utils/message'
import { useAuthStore } from '@/stores/auth'
import * as echarts from 'echarts'

const router = useRouter()
const authStore = useAuthStore()

// ==================== 类型定义 ====================

interface WeeklyOrder {
  day: string
  count: number
}

interface Order {
  id: number
  createdAt: string
  totalAmount: number
  status: string
  completedAt: string | null
}

interface OrderWithItemsVO {
  order: Order
  orderItems: any[]
}

interface OrderCounts {
  [key: string]: number
}

interface SellerOrdersResponse {
  records: OrderWithItemsVO[]
  total: number
  page: number
  size: number
  totalPages: number
  counts: OrderCounts
}

interface SellerProfile {
  storeName: string
  [key: string]: any
}

interface SellerProfileResponse {
  profile: SellerProfile
}

// ==================== 响应式数据 ====================

const loading = ref(true)
const storeName = ref('商家用户')
const todayOrderCount = ref(0)
const todayIncome = ref(0)
const pendingCount = ref(0)
const shippingCount = ref(0)
const refundCount = ref(0)
const weeklyOrders = ref<WeeklyOrder[]>([])
const orderTrend = ref(0)
const incomeTrend = ref(0)

const currentTime = ref('')
let timer: ReturnType<typeof setInterval> | null = null

// 图表引用
const trendChartRef = ref<HTMLDivElement | null>(null)
let trendChart: echarts.ECharts | null = null

// ==================== 计算属性 ====================

const currentDate = computed(() => {
  const date = new Date()
  const year = date.getFullYear()
  const month = date.getMonth() + 1
  const day = date.getDate()
  const weekdays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
  const weekday = weekdays[date.getDay()]
  return `${year}年${month}月${day}日 ${weekday}`
})

const chartLabels = computed(() => weeklyOrders.value.map(item => item.day))
const chartData = computed(() => weeklyOrders.value.map(item => item.count))

// ==================== 时间更新 ====================

const updateTime = (): void => {
  const date = new Date()
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')
  currentTime.value = `${hours}:${minutes}:${seconds}`
}

// ==================== ECharts 图表初始化 ====================

const initTrendChart = () => {
  if (!trendChartRef.value) return

  if (trendChart) trendChart.dispose()
  trendChart = echarts.init(trendChartRef.value)

  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        const item = params[0]
        return `${item.axisValue}<br/>订单量: ${item.value}笔`
      }
    },
    grid: {
      left: '12%',
      right: '8%',
      bottom: '20%',
      top: '15%',
      containLabel: true
    },
    xAxis: {
      type: 'value',
      axisLabel: { color: '#606266', fontSize: 11 },
      splitLine: { lineStyle: { color: '#f0f0f0', type: 'dashed' } }
    },
    yAxis: {
      type: 'category',
      data: chartLabels.value,
      inverse: true,
      axisLine: { lineStyle: { color: '#e0e6f1' } },
      axisLabel: { color: '#606266', fontSize: 11 }
    },
    series: [{
      name: '订单量',
      type: 'bar',
      data: chartData.value,
      barWidth: '50%',
      itemStyle: {
        borderRadius: [0, 6, 6, 0],
        color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
          { offset: 0, color: '#f093fb' },
          { offset: 1, color: '#f5576c' }
        ])
      }
    }]
  }

  trendChart.setOption(option)
}

// ==================== 数据加载 ====================

const loadAllData = async (): Promise<void> => {
  loading.value = true
  try {
    await Promise.all([
      loadStoreInfo(),
      loadSellerOrders()
    ])
  } catch (error) {
    Message.error('加载仪表盘数据失败')
  } finally {
    loading.value = false
    await nextTick()
    setTimeout(() => {
      initTrendChart()
    }, 150)
  }
}

const loadStoreInfo = async (): Promise<void> => {
  try {
    const response = await authAPI.getSellerProfile()
    if (response.success && response.data?.profile) {
      storeName.value = response.data.profile.storeName || '商家用户'
    }
  } catch (error) {
    Message.error('加载商家信息失败')
  }
}

const loadSellerOrders = async (): Promise<void> => {
  try {
    const response = await authAPI.getSellerOrders({ page: 1, pageSize: 500 })

    if (response.success && response.data) {
      const data = response.data as SellerOrdersResponse

      // 处理统计数据
      if (data.counts) {
        pendingCount.value = data.counts.PAID || 0
        shippingCount.value = data.counts.PROCESSING || 0
        refundCount.value = data.counts.REFUNDING || 0
      }

      // 处理今日订单和收入
      if (data.records && data.records.length > 0) {
        const allOrders = data.records.map(wrapper => wrapper.order)

        const formatDateStr = (date: Date): string => {
          return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
        }

        const todayStr = formatDateStr(new Date())

        const todayCompletedOrders = allOrders.filter(order => {
          if (order.status !== 'COMPLETED' || !order.completedAt) return false
          return formatDateStr(new Date(order.completedAt)) === todayStr
        })

        todayOrderCount.value = todayCompletedOrders.length
        todayIncome.value = todayCompletedOrders.reduce((sum, order) => sum + (order.totalAmount || 0), 0)

        const yesterdayStr = formatDateStr(new Date(new Date().getTime() - 86400000))

        const yesterdayCompleted = allOrders.filter(order => {
          if (order.status !== 'COMPLETED' || !order.completedAt) return false
          return formatDateStr(new Date(order.completedAt)) === yesterdayStr
        })

        const calcTrend = (todayCount: number, yesterdayCount: number): number => {
          if (yesterdayCount === 0) return todayCount > 0 ? 100 : 0
          return Math.round(((todayCount - yesterdayCount) / yesterdayCount) * 100)
        }

        orderTrend.value = calcTrend(todayCompletedOrders.length, yesterdayCompleted.length)

        const yesterdayIncomeVal = yesterdayCompleted.reduce((sum, o) => sum + (o.totalAmount || 0), 0)
        incomeTrend.value = calcTrend(todayIncome.value, yesterdayIncomeVal)

        // 处理周趋势数据
        const days: WeeklyOrder[] = []
        for (let i = 6; i >= 0; i--) {
          const date = new Date()
          date.setDate(date.getDate() - i)
          const dayStr = `${date.getMonth() + 1}/${date.getDate()}`
          days.push({ day: dayStr, count: 0 })
        }

        const completedOrders = allOrders.filter(order =>
          order.status === 'COMPLETED' && order.completedAt
        )

        completedOrders.forEach(order => {
          const orderDate = new Date(order.completedAt!)
          const dayStr = `${orderDate.getMonth() + 1}/${orderDate.getDate()}`
          const target = days.find(d => d.day === dayStr)
          if (target) {
            target.count++
          }
        })

        weeklyOrders.value = days
      }
    }
  } catch (error) {
    Message.error(`加载商家订单数据失败:${error}`)
  }
}

// ==================== 页面跳转 ====================

const goToOrders = (status?: string): void => {
  router.push({
    name: 'SellerOrders',
    query: status ? { status } : {}
  })
}

const goToProducts = (): void => {
  router.push({ name: 'SellerProducts' })
}

const goToAddProduct = (): void => {
  router.push({ name: 'SellerAddProduct' })
}

const goToProfile = (): void => {
  router.push({ name: 'SellerProfile' })
}

// ==================== 工具函数 ====================

const formatPrice = (price: number): string => {
  if (price == null) return '0.00'
  return price.toFixed(2)
}

// ==================== 生命周期 ====================

const handleChartResize = () => {
  trendChart?.resize()
}

const handleReturnSubmitted = () => {
  loadAllData()
}

onMounted(() => {
  if (!authStore.validateSellerPermission()) return

  loadAllData()

  updateTime()
  timer = setInterval(updateTime, 1000)
  window.addEventListener('resize', handleChartResize)
  window.addEventListener('return-submitted', handleReturnSubmitted)
})

onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
  window.removeEventListener('resize', handleChartResize)
  window.removeEventListener('return-submitted', handleReturnSubmitted)
  trendChart?.dispose()
})
</script>

<style scoped>
@import url('@/static/css/seller/控制台.css');
</style>
