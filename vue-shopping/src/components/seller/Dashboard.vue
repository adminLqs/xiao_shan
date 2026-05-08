<template>
  <div class="seller-dashboard">
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

      <!-- 待处理订单卡片（最核心） -->
      <div class="stat-card warning" @click="goToOrders('PAID')">
        <div class="stat-icon">
          <i class="fas fa-clock"></i>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ pendingCount }}</div>
          <div class="stat-label">待处理订单</div>
        </div>
        <div class="stat-action">
          <i class="fas fa-chevron-right"></i>
        </div>
      </div>

      <!-- 待发货订单卡片 -->
      <div class="stat-card primary" @click="goToOrders('PROCESSING')">
        <div class="stat-icon">
          <i class="fas fa-truck"></i>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ shippingCount }}</div>
          <div class="stat-label">待发货</div>
        </div>
        <div class="stat-action">
          <i class="fas fa-chevron-right"></i>
        </div>
      </div>
    </div>

    <!-- ========== 待办事项区域（最重要） ========== -->
    <div class="todo-section">
      <div class="section-header">
        <h3><i class="fas fa-tasks"></i> 待办事项</h3>
        <span class="section-tip">需要您及时处理</span>
      </div>
      
      <div class="todo-list">
        <!-- 待处理订单（已付款未接单） -->
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

        <!-- 待发货订单 -->
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

        <!-- 无待办事项 -->
        <div v-if="pendingCount === 0 && shippingCount === 0" class="todo-empty">
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
        <div 
          v-for="(item, index) in weeklyOrders" 
          :key="index"
          class="chart-bar"
          :style="{ height: getBarHeight(item.count) + '%' }"
        >
          <div class="bar-value">{{ item.count }}</div>
          <div class="bar-label">{{ item.day }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { ref, computed, onMounted, onUnmounted } from 'vue'
  import { useRouter } from 'vue-router'
  import { authAPI } from '@/api/authAPI'
  import { showToast } from 'vant'
  import 'vant/es/toast/style'
  import { useAuthStore } from '@/stores/auth'

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
  }

  // ==================== 响应式数据 ====================

  const storeName = ref('商家用户')
  const todayOrderCount = ref(0)
  const todayIncome = ref(0)
  const pendingCount = ref(0)
  const shippingCount = ref(0)
  const weeklyOrders = ref<WeeklyOrder[]>([])
  const orderTrend = ref(12)
  const incomeTrend = ref(8)

  const currentTime = ref('')
  let timer: number

  // ==================== 时间计算 ====================

  const currentDate = computed(() => {
    const date = new Date()
    const year = date.getFullYear()
    const month = date.getMonth() + 1
    const day = date.getDate()
    const weekdays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
    const weekday = weekdays[date.getDay()]
    return `${year}年${month}月${day}日 ${weekday}`
  })

  const updateTime = () => {
    const date = new Date()
    const hours = String(date.getHours()).padStart(2, '0')
    const minutes = String(date.getMinutes()).padStart(2, '0')
    const seconds = String(date.getSeconds()).padStart(2, '0')
    currentTime.value = `${hours}:${minutes}:${seconds}`
  }

  // ==================== 数据加载 ====================

  /**
   * 加载订单统计数据
   * @returns {Promise<void>}
   */
  const loadStatistics = async () => {
    try {
      const response = await authAPI.getSellerOrders({ page: 1, size: 1 })
      
      if (response.success && response.counts) {
        pendingCount.value = response.counts.PAID || 0
        shippingCount.value = response.counts.PROCESSING || 0
      }
    } catch (error) {
      console.error('加载统计数据失败:', error)
    }
  }

  /**
   * 加载今日订单数据
   * @returns {Promise<void>}
   */
  const loadTodayData = async () => {
    try {
      const response = await authAPI.getSellerOrders({ page: 1, size: 100 })
      
      if (response.success && response.data) {
        const today = new Date().toDateString()
        const todayOrders = response.data.filter((order: Order) => {
          return new Date(order.createdAt).toDateString() === today
        })
        
        todayOrderCount.value = todayOrders.length
        todayIncome.value = todayOrders.reduce((sum: number, order: Order) => {
          return sum + (order.totalAmount || 0)
        }, 0)
      }
    } catch (error) {
      console.error('加载今日数据失败:', error)
    }
  }

  /**
   * 加载近7天订单趋势
   * @returns {Promise<void>}
   */
  const loadWeeklyTrend = async () => {
    try {
      const days: WeeklyOrder[] = []
      for (let i = 6; i >= 0; i--) {
        const date = new Date()
        date.setDate(date.getDate() - i)
        const dayStr = `${date.getMonth() + 1}/${date.getDate()}`
        days.push({ day: dayStr, count: 0 })
      }
      
      const response = await authAPI.getSellerOrders({ page: 1, size: 500 })
      
      if (response.success && response.data) {
        response.data.forEach((order: Order) => {
          const orderDate = new Date(order.createdAt)
          const dayStr = `${orderDate.getMonth() + 1}/${orderDate.getDate()}`
          const target = days.find(d => d.day === dayStr)
          if (target) target.count++
        })
        weeklyOrders.value = days
      }
    } catch (error) {
      console.error('加载订单趋势失败:', error)
      weeklyOrders.value = [
        { day: '4/16', count: 3 }, { day: '4/17', count: 5 },
        { day: '4/18', count: 2 }, { day: '4/19', count: 8 },
        { day: '4/20', count: 6 }, { day: '4/21', count: 4 },
        { day: '4/22', count: 7 }
      ]
    }
  }

  /**
   * 加载店铺信息
   * @returns {Promise<void>}
   */
  const loadStoreInfo = async () => {
    try {
      const response = await authAPI.getSellerProfile()
      if (response.success && response.data) {
        storeName.value = response.data.storeName || '商家用户'
      }
    } catch (error) {
      console.error('加载店铺信息失败:', error)
    }
  }

  // ==================== 页面跳转 ====================

  /**
   * 跳转到订单列表
   * @param {string} [status] - 订单状态筛选
   */
  const goToOrders = (status?: string) => {
    router.push({
      name: 'SellerOrders',
      query: status ? { status } : {}
    })
  }

  const goToProducts = () => {
    router.push({ name: 'SellerProducts' })
  }

  const goToAddProduct = () => {
    router.push({ name: 'SellerAddProduct' })
  }

  const goToProfile = () => {
    router.push({ name: 'SellerProfile' })
  }

  // ==================== 工具函数 ====================

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
   * 获取柱状图高度百分比
   * @param {number} count - 订单数量
   * @returns {number} 高度百分比
   */
  const getBarHeight = (count: number): number => {
    const maxCount = Math.max(...weeklyOrders.value.map(w => w.count), 1)
    return (count / maxCount) * 100
  }

  // ==================== 生命周期 ====================

  onMounted(async () => {
    // 校验商家权限
    if (!authStore.validateSellerPermission()) return
    
     // 并行加载数据
    await Promise.all([
      loadStoreInfo(),
      loadStatistics(),
      loadTodayData(),
      loadWeeklyTrend()
    ])
    
    // 加载数据
    updateTime()
    timer = setInterval(updateTime, 1000)
  })

  // 组件卸载时清除定时器
  onUnmounted(() => {
    if (timer) clearInterval(timer)
  })
</script>

<style scoped>
/* 控制台容器 */
.seller-dashboard {
  padding: 0;
}

/* ========== 欢迎区域 ========== */
.welcome-section {
  background: linear-gradient(135deg, #4a6491 0%, #2d3a5e 100%);
  border-radius: 16px;
  padding: 24px 32px;
  margin-bottom: 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: white;
}

.welcome-text h2 {
  font-size: 24px;
  margin: 0 0 8px 0;
  font-weight: 600;
}

.welcome-text p {
  margin: 0;
  opacity: 0.9;
  font-size: 14px;
}

.welcome-time .time {
  font-size: 32px;
  font-weight: 600;
  font-family: monospace;
}

/* ========== 统计卡片 ========== */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  background: white;
  border-radius: 16px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  transition: all 0.3s;
  cursor: pointer;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1);
}

.stat-card.warning {
  border-left: 4px solid #f59e0b;
}

.stat-card.primary {
  border-left: 4px solid #4a6491;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: #f1f5f9;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: #4a6491;
}

.stat-card.warning .stat-icon {
  background: #fef3c7;
  color: #f59e0b;
}

.stat-card.primary .stat-icon {
  background: #dbeafe;
  color: #4a6491;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #1e293b;
}

.stat-label {
  font-size: 14px;
  color: #64748b;
  margin-top: 4px;
}

.stat-trend {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  padding: 4px 8px;
  border-radius: 20px;
}

.stat-trend.up {
  color: #10b981;
  background: #dcfce7;
}

.stat-trend.down {
  color: #ef4444;
  background: #fee2e2;
}

.stat-action {
  color: #cbd5e1;
  transition: all 0.3s;
}

.stat-card:hover .stat-action {
  color: #4a6491;
  transform: translateX(4px);
}

/* ========== 待办事项区域 ========== */
.todo-section {
  background: white;
  border-radius: 16px;
  padding: 20px;
  margin-bottom: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e2e8f0;
}

.section-header h3 {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.section-tip {
  font-size: 12px;
  color: #94a3b8;
}

.todo-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.todo-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s;
}

.todo-item.urgent {
  background: #fef2f2;
  border-left: 4px solid #ef4444;
}

.todo-item.warning {
  background: #fffbeb;
  border-left: 4px solid #f59e0b;
}

.todo-item:hover {
  transform: translateX(4px);
}

.todo-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

.todo-item.urgent .todo-icon {
  background: #fee2e2;
  color: #ef4444;
}

.todo-item.warning .todo-icon {
  background: #fef3c7;
  color: #f59e0b;
}

.todo-content {
  flex: 1;
}

.todo-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 4px;
}

.todo-item.urgent .todo-title {
  color: #dc2626;
}

.todo-item.warning .todo-title {
  color: #d97706;
}

.todo-desc {
  font-size: 13px;
  color: #64748b;
}

.todo-empty {
  text-align: center;
  padding: 40px;
  color: #10b981;
}

.todo-empty i {
  font-size: 48px;
  margin-bottom: 12px;
  display: block;
}

.btn-sm {
  padding: 8px 16px;
  font-size: 13px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-primary {
  background: #4a6491;
  color: white;
}

.btn-primary:hover {
  background: #3a5479;
  transform: translateY(-2px);
}

/* ========== 快捷操作 ========== */
.quick-actions {
  background: white;
  border-radius: 16px;
  padding: 20px;
  margin-bottom: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.action-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 20px;
  border-radius: 12px;
  background: #f8fafc;
  cursor: pointer;
  transition: all 0.3s;
}

.action-item:hover {
  background: #4a6491;
  color: white;
  transform: translateY(-4px);
}

.action-item i {
  font-size: 28px;
  color: #4a6491;
}

.action-item:hover i {
  color: white;
}

.action-item span {
  font-size: 14px;
  font-weight: 500;
}

/* ========== 订单趋势 ========== */
.trend-section {
  background: white;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.trend-chart {
  display: flex;
  align-items: flex-end;
  justify-content: space-around;
  height: 200px;
  padding: 20px 0;
}

.chart-bar {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  height: 100%;
  justify-content: flex-end;
}

.bar-value {
  font-size: 12px;
  font-weight: 600;
  color: #4a6491;
}

.chart-bar::before {
  content: '';
  width: 40px;
  background: linear-gradient(180deg, #4a6491 0%, #7c9bc0 100%);
  border-radius: 8px 8px 4px 4px;
  transition: height 0.3s;
  height: v-bind(height);
}

.bar-label {
  font-size: 12px;
  color: #64748b;
  margin-top: 8px;
}

/* ========== 响应式 ========== */
@media (max-width: 1024px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .action-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .welcome-section {
    flex-direction: column;
    text-align: center;
    gap: 12px;
  }
  
  .stats-grid {
    grid-template-columns: 1fr;
  }
  
  .todo-item {
    flex-direction: column;
    text-align: center;
  }
  
  .action-grid {
    grid-template-columns: 1fr;
  }
  
  .trend-chart {
    height: 150px;
  }
  
  .chart-bar::before {
    width: 24px;
  }
}
</style>