<template>
  <div class="analytics-container">
    <!-- ========== 加载状态 ========== -->
    <div v-if="loading" class="skeleton-form" style="padding: 24px;">
      <div class="skeleton" style="height: 48px; margin-bottom: 24px; border-radius: 8px;"></div>
      <div class="skeleton-stat-grid">
        <div v-for="i in 4" :key="i" class="skeleton-stat-card">
          <div class="skeleton skeleton-stat-icon"></div>
          <div class="skeleton skeleton-stat-value"></div>
          <div class="skeleton skeleton-stat-label"></div>
        </div>
      </div>
      <div class="skeleton-section" style="margin-top: 24px;">
        <div class="skeleton skeleton-section-title" style="margin-bottom: 16px;"></div>
        <div style="display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px;">
          <div v-for="i in 4" :key="i" class="skeleton-stat-card">
            <div class="skeleton skeleton-stat-icon"></div>
            <div class="skeleton skeleton-stat-value"></div>
            <div class="skeleton skeleton-stat-label"></div>
          </div>
        </div>
      </div>
    </div>

    <!-- ========== 统计内容 ========== -->
    <div v-else class="analytics-content content-wrapper">
      <!-- 时间选择器 -->
      <div class="time-filter-bar">
        <div class="radio-group">
          <label class="radio-label" :class="{ active: timeRange === '7d' }">
            <input type="radio" name="timeRange" value="7d" v-model="timeRange" @change="handleTimeRangeChange" />
            <span>近7天</span>
          </label>
          <label class="radio-label" :class="{ active: timeRange === '30d' }">
            <input type="radio" name="timeRange" value="30d" v-model="timeRange" @change="handleTimeRangeChange" />
            <span>近30天</span>
          </label>
        </div>
      </div>
      <!-- 统计卡片行 -->
      <div class="stats-row">
        <div class="stat-col" v-for="stat in statsCards" :key="stat.label">
          <div class="card stat-card">
            <div class="stat-content">
              <div class="stat-icon" :style="{ background: stat.bgColor }">
                <i :class="stat.iconClass" :size="24"></i>
              </div>
              <div class="stat-info">
                <div class="stat-label">{{ stat.label }}</div>
                <div class="stat-value">{{ stat.value }}</div>
                <div class="stat-trend" :class="stat.trendType">
                  <i :class="stat.trendIconClass"></i>
                  <span>{{ stat.trendText }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 趋势图表行 -->
      <div class="chart-row">
        <!-- 销售额趋势图 -->
        <div class="chart-col">
          <div class="card chart-card">
            <div class="card-header">
              <div class="chart-header">
                <span class="chart-title">销售额趋势</span>
                <span class="tag tag-success">¥</span>
              </div>
            </div>
            <div class="card-body">
              <div class="chart-container" ref="salesChartRef"></div>
            </div>
          </div>
        </div>

        <!-- 订单量趋势图 -->
        <div class="chart-col">
          <div class="card chart-card">
            <div class="card-header">
              <div class="chart-header">
                <span class="chart-title">订单量趋势</span>
                <span class="tag tag-warning">笔</span>
              </div>
            </div>
            <div class="card-body">
              <div class="chart-container" ref="ordersChartRef"></div>
            </div>
          </div>
        </div>
      </div>

      <!-- 商品销量排行与订单状态 -->
      <div class="chart-row">
        <div class="chart-col">
          <div class="card chart-card">
            <div class="card-header">
              <div class="chart-header">
                <span class="chart-title">商品销量排行</span>
              </div>
            </div>
            <div class="card-body">
              <div class="chart-container" ref="productSalesChartRef"></div>
            </div>
          </div>
        </div>

        <div class="chart-col">
          <div class="card chart-card">
            <div class="card-header">
              <div class="chart-header">
                <span class="chart-title">订单状态分布</span>
              </div>
            </div>
            <div class="card-body">
              <div class="chart-container" ref="orderStatusChartRef"></div>
            </div>
          </div>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { authAPI } from '@/api/authAPI'
import Message from '@/utils/message'
import { useAuthStore } from '@/stores/auth'
import * as echarts from 'echarts'

const router = useRouter()

// ==================== 初始化 ====================
const authStore = useAuthStore()

// ==================== 类型定义 ====================
interface StatCard {
  label: string
  value: string | number
  iconClass: string
  bgColor: string
  trendType: 'up' | 'down'
  trendIconClass: string
  trendText: string
}

interface OrderCounts {
  PENDING: number
  PAID: number
  PROCESSING: number
  SHIPPED: number
  COMPLETED: number
  CANCELLED: number
  [key: string]: number
}

interface Order {
  id: number
  totalAmount: number
  status: string
  completedAt: string | null
  createdAt: string
}

interface OrderWithItems {
  order: Order
  orderItems: any[]
}

interface SellerOrdersResponse {
  records: OrderWithItems[]
  total: number
  page: number
  size: number
  totalPages: number
  counts: OrderCounts
}

interface ChartData {
  dates: string[]
  values: number[]
}

interface ProductSales {
  productName: string
  salesCount: number
  productId: number
}

// ==================== 响应式数据 ====================
const loading = ref(true)
const timeRange = ref('7d')
const orderCounts = ref<OrderCounts>({
  PENDING: 0,
  PAID: 0,
  PROCESSING: 0,
  SHIPPED: 0,
  COMPLETED: 0,
  CANCELLED: 0
})

const statsCards = ref<StatCard[]>([
  {
    label: '总销售额',
    value: 0,
    iconClass: 'fas fa-coins',
    bgColor: 'linear-gradient(135deg, #667eea, #764ba2)',
    trendType: 'up',
    trendIconClass: 'fas fa-arrow-up',
    trendText: '0%'
  },
  {
    label: '总订单数',
    value: 0,
    iconClass: 'fas fa-shopping-cart',
    bgColor: 'linear-gradient(135deg, #f093fb, #f5576c)',
    trendType: 'up',
    trendIconClass: 'fas fa-arrow-up',
    trendText: '0'
  },
  {
    label: '商品数量',
    value: 0,
    iconClass: 'fas fa-list',
    bgColor: 'linear-gradient(135deg, #4facfe, #00f2fe)',
    trendType: 'up',
    trendIconClass: 'fas fa-arrow-right',
    trendText: '0个'
  },
  {
    label: '有效订单',
    value: 0,
    iconClass: 'fas fa-check-circle',
    bgColor: 'linear-gradient(135deg, #43e97b, #38f9d7)',
    trendType: 'up',
    trendIconClass: 'fas fa-arrow-up',
    trendText: '0笔'
  }
])

// 图表数据
const salesTrend = ref<ChartData>({ dates: [], values: [] })
const ordersTrend = ref<ChartData>({ dates: [], values: [] })
const productSalesData = ref<ProductSales[]>([])

// ==================== 图表引用 ====================
const salesChartRef = ref<HTMLDivElement>()
const ordersChartRef = ref<HTMLDivElement>()
const productSalesChartRef = ref<HTMLDivElement>()
const orderStatusChartRef = ref<HTMLDivElement>()

// ==================== 图表实例 ====================
let salesChart: echarts.ECharts | null = null
let ordersChart: echarts.ECharts | null = null
let productSalesChart: echarts.ECharts | null = null
let orderStatusChart: echarts.ECharts | null = null

// ==================== 数据加载 ====================
const loadAnalyticsData = async () => {
  loading.value = true

  try {
    // 获取日期范围
    const days = timeRange.value === '7d' ? 7 : 30

    // 并行加载订单数据和商品数据
    const [ordersResponse, productsResponse] = await Promise.all([
      authAPI.getSellerOrders({ page: 1, pageSize: 500 }),
      authAPI.getSellerProducts({ page: 1, pageSize: 1 })
    ])

    // 处理订单数据
    if (ordersResponse.success && ordersResponse.data) {
      const data = ordersResponse.data as SellerOrdersResponse

      // 更新订单统计
      if (data.counts) {
        orderCounts.value = {
          PENDING: data.counts.PENDING || 0,
          PAID: data.counts.PAID || 0,
          PROCESSING: data.counts.PROCESSING || 0,
          SHIPPED: data.counts.SHIPPED || 0,
          COMPLETED: data.counts.COMPLETED || 0,
          CANCELLED: data.counts.CANCELLED || 0
        }
      }

      // 计算统计数据
      const allOrders = data.records.map((r: OrderWithItems) => r.order)

      // 筛选时间范围内的订单
      const cutoffDate = new Date()
      cutoffDate.setDate(cutoffDate.getDate() - days)

      const filteredOrders = allOrders.filter((order: Order) =>
        new Date(order.createdAt) >= cutoffDate
      )

      // 计算总销售额（统计有效订单）
      const validOrdersForSales = filteredOrders.filter((order: Order) =>
        order.status !== 'CANCELLED'
      )
      const totalSales = validOrdersForSales.reduce((sum: number, order: Order) =>
        sum + (order.totalAmount || 0), 0
      )

      // 计算总订单数
      const totalOrders = filteredOrders.length

      // 计算有效订单数（排除取消）
      const validOrders = filteredOrders.filter((order: Order) =>
        order.status !== 'CANCELLED'
      )

      // 计算趋势（对比上期）
      const prevCutoffDate = new Date()
      prevCutoffDate.setDate(prevCutoffDate.getDate() - days * 2)

      const prevOrders = allOrders.filter((order: Order) => {
        const orderDate = new Date(order.createdAt)
        return orderDate >= prevCutoffDate && orderDate < cutoffDate
      })

      const prevSales = prevOrders.reduce((sum: number, order: Order) =>
        sum + (order.totalAmount || 0), 0
      )
      const prevOrdersCount = prevOrders.length

      // 计算趋势百分比
      const salesTrendPercent = prevSales > 0
        ? Math.round(((totalSales - prevSales) / prevSales) * 100)
        : (totalSales > 0 ? 100 : 0)

      const ordersTrendPercent = prevOrdersCount > 0
        ? Math.round(((totalOrders - prevOrdersCount) / prevOrdersCount) * 100)
        : (totalOrders > 0 ? 100 : 0)

      // 更新统计卡片
      updateStatsCards({
        totalSales,
        totalOrders,
        productCount: productsResponse.data?.total || 0,
        validCount: validOrders.length,
        salesTrendPercent,
        ordersTrendPercent
      })

      // 计算趋势图数据
      calculateTrendData(allOrders, days)

      // 计算商品销量排行
      calculateProductSales(data.records)
    }

    // 初始化图表
    await nextTick()
    // 加延迟确保 DOM 渲染完成
    setTimeout(() => {
      initSalesChart()
      initOrdersChart()
      initProductSalesChart()
      initOrderStatusChart()
    }, 100)

  } catch (error) {
    Message.error('加载数据失败，请刷新页面重试')
  } finally {
    loading.value = false
  }
}

// ==================== 计算商品销量排行 ====================
const calculateProductSales = (orderRecords: OrderWithItems[]) => {
  const productSalesMap = new Map<number, { name: string; count: number }>()

  // 遍历所有订单记录
  orderRecords.forEach((item: OrderWithItems) => {
    // 只统计已完成的订单
    if (item.order.status === 'COMPLETED') {
      item.orderItems.forEach((orderItem: any) => {
        const productId = orderItem.productId
        const productName = orderItem.productName || `商品${productId}`
        const quantity = orderItem.quantity || 1

        if (productSalesMap.has(productId)) {
          const existing = productSalesMap.get(productId)!
          existing.count += quantity
        } else {
          productSalesMap.set(productId, { name: productName, count: quantity })
        }
      })
    }
  })

  // 转换为数组并按销量排序，取前10
  const sortedProducts: ProductSales[] = Array.from(productSalesMap.entries())
    .map(([productId, data]) => ({
      productId,
      productName: data.name.length > 10 ? data.name.substring(0, 10) + '...' : data.name,
      salesCount: data.count
    }))
    .sort((a, b) => b.salesCount - a.salesCount)
    .slice(0, 10)

  productSalesData.value = sortedProducts
}

// ==================== 计算趋势数据 ====================
const calculateTrendData = (allOrders: Order[], days: number) => {
  const dates: string[] = []
  const salesData: number[] = []
  const ordersData: number[] = []

  for (let i = days - 1; i >= 0; i--) {
    const date = new Date()
    date.setDate(date.getDate() - i)
    const dateStr = `${date.getMonth() + 1}/${date.getDate()}`
    dates.push(dateStr)

    // 筛选当天的订单
    const dayStart = new Date(date)
    dayStart.setHours(0, 0, 0, 0)
    const dayEnd = new Date(date)
    dayEnd.setHours(23, 59, 59, 999)

    const dayOrders = allOrders.filter((order: Order) => {
      const orderDate = new Date(order.createdAt)
      return orderDate >= dayStart && orderDate <= dayEnd
    })

    // 统计有效订单（排除取消）
    const dayValidOrders = dayOrders.filter(order =>
      order.status !== 'CANCELLED'
    )

    const daySales = dayValidOrders.reduce((sum, order) => sum + order.totalAmount, 0)

    salesData.push(daySales)
    ordersData.push(dayValidOrders.length)
  }

  salesTrend.value = { dates, values: salesData }
  ordersTrend.value = { dates, values: ordersData }
}

// ==================== 更新统计卡片 ====================
const updateStatsCards = (data: {
  totalSales: number
  totalOrders: number
  productCount: number
  validCount: number
  salesTrendPercent: number
  ordersTrendPercent: number
}) => {
  const existingCards = statsCards.value
  statsCards.value = [
    {
      ...(existingCards[0] || { label: '', iconClass: 'fas fa-coins', bgColor: '' }),
      value: `¥${formatNumber(data.totalSales)}`,
      trendType: data.salesTrendPercent >= 0 ? 'up' : 'down',
      trendIconClass: data.salesTrendPercent >= 0 ? 'fas fa-arrow-up' : 'fas fa-arrow-down',
      trendText: `${data.salesTrendPercent >= 0 ? '+' : ''}${data.salesTrendPercent}%`
    },
    {
      ...(existingCards[1] || { label: '', iconClass: 'fas fa-shopping-cart', bgColor: '' }),
      value: data.totalOrders,
      trendType: data.ordersTrendPercent >= 0 ? 'up' : 'down',
      trendIconClass: data.ordersTrendPercent >= 0 ? 'fas fa-arrow-up' : 'fas fa-arrow-down',
      trendText: `${data.ordersTrendPercent >= 0 ? '+' : ''}${data.ordersTrendPercent}%`
    },
    {
      ...(existingCards[2] || { label: '', iconClass: 'fas fa-list', bgColor: '' }),
      value: data.productCount,
      trendType: 'up',
      trendIconClass: 'fas fa-arrow-up',
      trendText: `${data.productCount}个`
    },
    {
      ...(existingCards[3] || { label: '', iconClass: 'fas fa-check-circle', bgColor: '' }),
      value: data.validCount,
      trendType: 'up',
      trendIconClass: 'fas fa-arrow-up',
      trendText: `${data.validCount}笔`
    }
  ]
}

// ==================== 初始化图表 ====================

// 销售额趋势图
const initSalesChart = () => {
  if (!salesChartRef.value) return

  if (salesChart) salesChart.dispose()
  salesChart = echarts.init(salesChartRef.value)

  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        const item = params[0]
        return `${item.axisValue}<br/>销售额: ¥${formatNumber(item.value)}`
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
      type: 'category',
      data: salesTrend.value.dates,
      boundaryGap: false,
      axisLine: { lineStyle: { color: '#e0e6f1' } },
      axisLabel: { color: '#606266', fontSize: 11, margin: 12 }
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        formatter: (value: number) => `¥${formatNumber(value)}`,
        color: '#606266',
        fontSize: 11,
        margin: 12
      },
      splitLine: { lineStyle: { color: '#f0f0f0', type: 'dashed' } }
    },
    series: [{
      name: '销售额',
      type: 'line',
      data: salesTrend.value.values,
      smooth: true,
      symbol: 'circle',
      symbolSize: 6,
      lineStyle: {
        width: 3,
        color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
          { offset: 0, color: '#667eea' },
          { offset: 1, color: '#764ba2' }
        ])
      },
      itemStyle: {
        color: '#667eea',
        borderColor: '#fff',
        borderWidth: 2
      },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(102, 126, 234, 0.3)' },
          { offset: 1, color: 'rgba(102, 126, 234, 0.05)' }
        ])
      }
    }]
  }

  salesChart.setOption(option)
}

// 订单量趋势图
const initOrdersChart = () => {
  if (!ordersChartRef.value) return

  if (ordersChart) ordersChart.dispose()
  ordersChart = echarts.init(ordersChartRef.value)

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
      data: ordersTrend.value.dates,
      inverse: true,
      axisLine: { lineStyle: { color: '#e0e6f1' } },
      axisLabel: { color: '#606266', fontSize: 11 }
    },
    series: [{
      name: '订单量',
      type: 'bar',
      data: ordersTrend.value.values,
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

  ordersChart.setOption(option)
}

// 商品销量排行（横向柱状图）
const initProductSalesChart = () => {
  if (!productSalesChartRef.value) return

  if (productSalesChart) productSalesChart.dispose()
  productSalesChart = echarts.init(productSalesChartRef.value)

  const data = productSalesData.value.length > 0
    ? productSalesData.value
    : [{ productName: '暂无数据', salesCount: 1 }]

  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params: any) => {
        const item = params[0]
        return `${item.name}<br/>销量: ${item.value}件`
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
      data: data.map((item) => item.productName),
      axisLine: { lineStyle: { color: '#e0e6f1' } },
      axisLabel: {
        color: '#606266',
        fontSize: 11,
        interval: 0
      },
      inverse: true
    },
    series: [{
      name: '销量',
      type: 'bar',
      data: data.map((item) => item.salesCount),
      barWidth: '50%',
      itemStyle: {
        borderRadius: [0, 6, 6, 0],
        color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
          { offset: 0, color: '#4facfe' },
          { offset: 1, color: '#00f2fe' }
        ])
      },
      label: {
        show: true,
        position: 'right',
        formatter: '{c}',
        color: '#606266',
        fontSize: 11
      }
    }]
  }

  productSalesChart.setOption(option)
}

// 订单状态分布
const initOrderStatusChart = () => {
  if (!orderStatusChartRef.value) return

  if (orderStatusChart) orderStatusChart.dispose()
  orderStatusChart = echarts.init(orderStatusChartRef.value)

  const statusData = [
    { name: '待付款', value: orderCounts.value.PENDING || 0 },
    { name: '待发货', value: orderCounts.value.PAID || 0 },
    { name: '处理中', value: orderCounts.value.PROCESSING || 0 },
    { name: '待收货', value: orderCounts.value.SHIPPED || 0 },
    { name: '已完成', value: orderCounts.value.COMPLETED || 0 },
    { name: '已取消', value: orderCounts.value.CANCELLED || 0 }
  ].filter(item => item.value > 0)

  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c}笔 ({d}%)'
    },
    legend: {
      orient: 'vertical',
      right: '5%',
      top: 'center',
      textStyle: { color: '#606266', fontSize: 12 }
    },
    series: [{
      name: '订单状态',
      type: 'pie',
      radius: ['50%', '75%'],
      center: ['40%', '50%'],
      avoidLabelOverlap: false,
      itemStyle: {
        borderRadius: 8,
        borderColor: '#fff',
        borderWidth: 2
      },
      label: {
        show: false
      },
      emphasis: {
        label: {
          show: true,
          fontSize: 14,
          fontWeight: 'bold'
        }
      },
      data: statusData.length > 0 ? statusData : [{ name: '暂无数据', value: 1 }],
      color: ['#e6a23c', '#409eff', '#f56c6c', '#67c23a', '#909399', '#f7ba2a']
    }]
  }

  orderStatusChart.setOption(option)
}

// ==================== 工具函数 ====================
const formatNumber = (num: number): string => {
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + 'w'
  }
  return num.toLocaleString()
}

const handleTimeRangeChange = () => {
  loadAnalyticsData()
}

// ==================== 窗口大小调整 ====================
const handleChartResize = () => {
  salesChart?.resize()
  ordersChart?.resize()
  productSalesChart?.resize()
  orderStatusChart?.resize()
}

// ==================== 生命周期 ====================
onMounted(async () => {
  // 先检查套餐权限
  try {
    const packageStatus = await authAPI.checkActivePackage()

    if (!packageStatus.data?.active) {
      Message.warning('套餐已过期，请续费')
      router.push({ name: 'SellerPackage' })
      return
    }
  } catch (error) {
    Message.error('检查套餐状态失败')
    router.push({ name: 'SellerPackage' })
    return
  }

  loadAnalyticsData()
  window.addEventListener('resize', handleChartResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleChartResize)
  salesChart?.dispose()
  ordersChart?.dispose()
  productSalesChart?.dispose()
  orderStatusChart?.dispose()
})
</script>

<style scoped>
@import url('@/static/css/seller/数据分析.css');
</style>
