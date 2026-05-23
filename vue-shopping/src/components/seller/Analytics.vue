<template>
  <div class="analytics-container">
    <!-- ========== 页面标题 ========== -->
    <div class="page-header">
      <h2 class="page-title">
        <el-icon><DataAnalysis /></el-icon>
        数据分析
      </h2>
      <div class="header-actions">
        <el-radio-group v-model="timeRange" @change="handleTimeRangeChange" size="small">
          <el-radio-button value="7d">近7天</el-radio-button>
          <el-radio-button value="30d">近30天</el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <!-- ========== 加载状态 ========== -->
    <div v-if="loading" class="loading-state">
      <el-skeleton :rows="12" animated />
    </div>

    <!-- ========== 统计内容 ========== -->
    <div v-else class="analytics-content">
      <!-- 统计卡片行 -->
      <el-row :gutter="20" class="stats-row">
        <el-col :xs="12" :sm="6" v-for="stat in statsCards" :key="stat.label">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-content">
              <div class="stat-icon" :style="{ background: stat.bgColor }">
                <el-icon :size="24"><component :is="stat.icon" /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-label">{{ stat.label }}</div>
                <div class="stat-value">{{ stat.value }}</div>
                <div class="stat-trend" :class="stat.trendType">
                  <el-icon><component :is="stat.trendIcon" /></el-icon>
                  <span>{{ stat.trendText }}</span>
                </div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 趋势图表行 -->
      <el-row :gutter="20" class="chart-row">
        <!-- 销售额趋势图 -->
        <el-col :xs="24" :lg="12">
          <el-card shadow="hover" class="chart-card">
            <template #header>
              <div class="chart-header">
                <span class="chart-title">销售额趋势</span>
                <el-tag size="small" type="success">¥</el-tag>
              </div>
            </template>
            <div class="chart-container" ref="salesChartRef"></div>
          </el-card>
        </el-col>

        <!-- 订单量趋势图 -->
        <el-col :xs="24" :lg="12">
          <el-card shadow="hover" class="chart-card">
            <template #header>
              <div class="chart-header">
                <span class="chart-title">订单量趋势</span>
                <el-tag size="small" type="warning">笔</el-tag>
              </div>
            </template>
            <div class="chart-container" ref="ordersChartRef"></div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 商品销量排行 -->
      <el-row :gutter="20" class="chart-row">
        <el-col :xs="24" :lg="12">
          <el-card shadow="hover" class="chart-card">
            <template #header>
              <div class="chart-header">
                <span class="chart-title">订单状态分布</span>
              </div>
            </template>
            <div class="chart-container" ref="orderStatusChartRef"></div>
          </el-card>
        </el-col>

        <!-- 待办提示 -->
        <el-col :xs="24" :lg="12">
          <el-card shadow="hover" class="chart-card">
            <template #header>
              <div class="chart-header">
                <span class="chart-title">待办事项</span>
              </div>
            </template>
            <div class="todo-summary">
              <div class="todo-item warning">
                <span class="todo-label">待处理订单</span>
                <span class="todo-value">{{ orderCounts.PAID || 0 }} 笔</span>
              </div>
              <div class="todo-item info">
                <span class="todo-label">待发货订单</span>
                <span class="todo-value">{{ orderCounts.PROCESSING || 0 }} 笔</span>
              </div>
              <div class="todo-item success">
                <span class="todo-label">已完成订单</span>
                <span class="todo-value">{{ orderCounts.COMPLETED || 0 }} 笔</span>
              </div>
              <div class="todo-item danger">
                <span class="todo-label">已取消订单</span>
                <span class="todo-value">{{ orderCounts.CANCELLED || 0 }} 笔</span>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { authAPI } from '@/api/authAPI'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import * as echarts from 'echarts'
import {
  DataAnalysis,
  Coin,
  ShoppingCart,
  List,
  UserFilled,
  ArrowUp,
  ArrowRight
} from '@element-plus/icons-vue'

// ==================== 初始化 ====================
const authStore = useAuthStore()

// ==================== 类型定义 ====================
interface StatCard {
  label: string
  value: string | number
  icon: any
  bgColor: string
  trendType: 'up' | 'down'
  trendIcon: any
  trendText: string
}

interface OrderCounts {
  PENDING: number
  PAID: number
  PROCESSING: number
  SHIPPED: number
  DELIVERED: number
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

interface ProductResponse {
  records: any[]
  total: number
  page: number
  size: number
  totalPages: number
}

interface ChartData {
  dates: string[]
  values: number[]
}

// ==================== 响应式数据 ====================
const loading = ref(true)
const timeRange = ref('7d')
const orderCounts = ref<OrderCounts>({
  PENDING: 0,
  PAID: 0,
  PROCESSING: 0,
  SHIPPED: 0,
  DELIVERED: 0,
  COMPLETED: 0,
  CANCELLED: 0
})

const statsCards = ref<StatCard[]>([
  {
    label: '总销售额',
    value: 0,
    icon: Coin,
    bgColor: 'linear-gradient(135deg, #667eea, #764ba2)',
    trendType: 'up',
    trendIcon: ArrowUp,
    trendText: '0%'
  },
  {
    label: '总订单数',
    value: 0,
    icon: ShoppingCart,
    bgColor: 'linear-gradient(135deg, #f093fb, #f5576c)',
    trendType: 'up',
    trendIcon: ArrowUp,
    trendText: '0'
  },
  {
    label: '商品数量',
    value: 0,
    icon: List,
    bgColor: 'linear-gradient(135deg, #4facfe, #00f2fe)',
    trendType: 'up',
    trendIcon: ArrowRight,
    trendText: '0个'
  },
  {
    label: '已完成订单',
    value: 0,
    icon: UserFilled,
    bgColor: 'linear-gradient(135deg, #43e97b, #38f9d7)',
    trendType: 'up',
    trendIcon: ArrowUp,
    trendText: '0笔'
  }
])

// 图表数据
const salesTrend = ref<ChartData>({ dates: [], values: [] })
const ordersTrend = ref<ChartData>({ dates: [], values: [] })

// ==================== 图表引用 ====================
const salesChartRef = ref<HTMLDivElement>()
const ordersChartRef = ref<HTMLDivElement>()
const orderStatusChartRef = ref<HTMLDivElement>()

// ==================== 图表实例 ====================
let salesChart: echarts.ECharts | null = null
let ordersChart: echarts.ECharts | null = null
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
          DELIVERED: data.counts.DELIVERED || 0,
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

      // 计算总销售额
      const totalSales = filteredOrders.reduce((sum: number, order: Order) => 
        sum + (order.totalAmount || 0), 0
      )

      // 计算总订单数
      const totalOrders = filteredOrders.length

      // 计算已完成订单数
      const completedOrders = filteredOrders.filter((order: Order) => 
        order.status === 'COMPLETED'
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
        completedCount: completedOrders.length,
        salesTrendPercent,
        ordersTrendPercent
      })

      // 计算趋势图数据
      calculateTrendData(allOrders, days)
    }

    // 初始化图表
    await nextTick()
    initSalesChart()
    initOrdersChart()
    initOrderStatusChart()
    
  } catch (error) {
    ElMessage.error('加载数据失败，请刷新页面重试')
  } finally {
    loading.value = false
  }
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
    
    // 计算当天的销售额和订单数（已完成订单）
    const dayCompletedOrders = dayOrders.filter((order: Order) => order.status === 'COMPLETED')
    
    const daySales = dayCompletedOrders.reduce((sum: number, order: Order) => 
      sum + (order.totalAmount || 0), 0
    )
    
    salesData.push(daySales)
    ordersData.push(dayCompletedOrders.length)
  }
  
  salesTrend.value = { dates, values: salesData }
  ordersTrend.value = { dates, values: ordersData }
}

// ==================== 更新统计卡片 ====================
const updateStatsCards = (data: {
  totalSales: number
  totalOrders: number
  productCount: number
  completedCount: number
  salesTrendPercent: number
  ordersTrendPercent: number
}) => {
  statsCards.value = [
    {
      ...statsCards.value[0],
      value: `¥${formatNumber(data.totalSales)}`,
      trendType: data.salesTrendPercent >= 0 ? 'up' : 'down',
      trendText: `${data.salesTrendPercent >= 0 ? '+' : ''}${data.salesTrendPercent}%`
    },
    {
      ...statsCards.value[1],
      value: data.totalOrders,
      trendType: data.ordersTrendPercent >= 0 ? 'up' : 'down',
      trendText: `${data.ordersTrendPercent >= 0 ? '+' : ''}${data.ordersTrendPercent}%`
    },
    {
      ...statsCards.value[2],
      value: data.productCount,
      trendType: 'up',
      trendText: `${data.productCount}个`
    },
    {
      ...statsCards.value[3],
      value: data.completedCount,
      trendType: 'up',
      trendText: `${data.completedCount}笔`
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
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: salesTrend.value.dates,
      boundaryGap: false,
      axisLine: { lineStyle: { color: '#e0e6f1' } },
      axisLabel: { color: '#606266', fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        formatter: (value: number) => `¥${formatNumber(value)}`,
        color: '#606266',
        fontSize: 11
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
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: ordersTrend.value.dates,
      boundaryGap: false,
      axisLine: { lineStyle: { color: '#e0e6f1' } },
      axisLabel: { color: '#606266', fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: '#606266', fontSize: 11 },
      splitLine: { lineStyle: { color: '#f0f0f0', type: 'dashed' } }
    },
    series: [{
      name: '订单量',
      type: 'bar',
      data: ordersTrend.value.values,
      barWidth: '40%',
      itemStyle: {
        borderRadius: [6, 6, 0, 0],
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#f093fb' },
          { offset: 1, color: '#f5576c' }
        ])
      }
    }]
  }
  
  ordersChart.setOption(option)
}

// 订单状态分布
const initOrderStatusChart = () => {
  if (!orderStatusChartRef.value) return
  
  if (orderStatusChart) orderStatusChart.dispose()
  orderStatusChart = echarts.init(orderStatusChartRef.value)
  
  const statusData = [
    { name: '待付款', value: orderCounts.value.PENDING || 0 },
    { name: '已付款', value: orderCounts.value.PAID || 0 },
    { name: '处理中', value: orderCounts.value.PROCESSING || 0 },
    { name: '已发货', value: orderCounts.value.SHIPPED || 0 },
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
  orderStatusChart?.resize()
}

// ==================== 生命周期 ====================
onMounted(() => {
  loadAnalyticsData()
  window.addEventListener('resize', handleChartResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleChartResize)
  salesChart?.dispose()
  ordersChart?.dispose()
  orderStatusChart?.dispose()
})
</script>

<style scoped>
/* ========== 页面容器 ========== */
.analytics-container {
  padding: 24px;
}

/* ========== 页面标题 ========== */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 16px;
}

.page-title {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 22px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

/* ========== 统计卡片 ========== */
.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  margin-bottom: 20px;
  transition: transform 0.3s;
  cursor: pointer;
}

.stat-card:hover {
  transform: translateY(-4px);
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}

.stat-info {
  flex: 1;
  min-width: 0;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 22px;
  font-weight: 700;
  color: #303133;
  margin-bottom: 4px;
}

.stat-trend {
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.stat-trend.up {
  color: #67c23a;
}

.stat-trend.down {
  color: #f56c6c;
}

/* ========== 图表区域 ========== */
.chart-row {
  margin-bottom: 20px;
}

.chart-card {
  margin-bottom: 20px;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chart-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.chart-container {
  width: 100%;
  height: 350px;
}

/* ========== 待办事项摘要 ========== */
.todo-summary {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 10px 0;
}

.todo-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-radius: 8px;
  font-size: 14px;
}

.todo-item.warning {
  background: #fef3cd;
  border-left: 4px solid #f59e0b;
}

.todo-item.info {
  background: #d1ecf1;
  border-left: 4px solid #17a2b8;
}

.todo-item.success {
  background: #d4edda;
  border-left: 4px solid #28a745;
}

.todo-item.danger {
  background: #f8d7da;
  border-left: 4px solid #dc3545;
}

.todo-label {
  font-weight: 500;
  color: #333;
}

.todo-value {
  font-weight: 700;
  color: #333;
}

/* ========== Element Plus 覆盖 ========== */
:deep(.el-card__body) {
  padding: 20px;
}

@media (max-width: 768px) {
  .analytics-container {
    padding: 16px;
  }
  
  .chart-container {
    height: 280px;
  }
  
  .stat-value {
    font-size: 18px;
  }
}
</style>
