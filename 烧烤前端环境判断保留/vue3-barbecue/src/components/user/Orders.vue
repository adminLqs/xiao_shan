<template>
  <div class="order-page">
    <!-- 顶部导航 -->
    <div class="order-header">
      <h1 class="page-title">我的订单</h1>
    </div>

    <!-- 订单状态标签 -->
    <div class="order-tabs">
      <div 
        v-for="tab in tabs" 
        :key="tab.value"
        class="tab-item"
        :class="{ active: currentTab === tab.value }"
        @click="switchTab(tab.value)"
      >
        {{ tab.label }}
        <span v-if="tab.count > 0" class="tab-count">{{ tab.count }}</span>
      </div>
    </div>

    <!-- 订单列表 -->
    <div class="order-list">
      <div v-if="loading" class="loading-state">
        <div class="loading-spinner"></div>
        <span>加载中...</span>
      </div>
      
      <div v-else-if="filteredOrders.length === 0" class="empty-state">
        <div class="empty-icon">📦</div>
        <p class="empty-text">暂无订单</p>
        <button class="go-shop-btn" @click="goToShop">去购物</button>
      </div>
      
      <div v-else class="order-items">
        <div 
          v-for="order in filteredOrders" 
          :key="order.id"
          class="order-card"
          @click="viewOrderDetail(order.orderNumber)"
        >
          <!-- 订单头部 -->
          <div class="order-card-header">
            <div class="order-info">
              <span class="order-no">订单号：{{ order.orderNumber }}</span>
              <span class="order-time">{{ formatDate(order.createdAt) }}</span>
            </div>
            <div class="order-status" :class="getStatusClass(order.status)">
              {{ getStatusText(order.status) }}
            </div>
          </div>
          
          <!-- 订单底部 -->
          <div class="order-card-footer">
            <div class="order-amount">
              <span class="amount-label">实付</span>
              <span class="amount-value">¥{{ formatPrice(order.totalAmount) }}</span>
            </div>
            <div class="order-actions">
              <button 
                v-if="order.status === 'PENDING'"
                class="action-btn pay-btn"
                @click.stop="goToPay(order.orderNumber, order.totalAmount)"
              >
                去支付
              </button>
              <button 
                v-else
                class="action-btn detail-btn"
                @click.stop="viewOrderDetail(order.orderNumber)"
              >
                查看详情
              </button>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 加载更多 -->
      <div v-if="hasMore && !loading" class="load-more" @click="loadMore">
        加载更多
      </div>
      <div v-if="!hasMore && filteredOrders.length > 0" class="no-more">
        没有更多了~
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { authAPI } from '@/api/auth'

// ==================== 类型定义 ====================
interface Order {
  id: number
  orderNumber: string
  totalAmount: number
  status: 'PENDING' | 'PAID' | 'COMPLETED' | 'CANCELLED'
  createdAt: string
  paymentMethod?: string
}

// ==================== 路由 ====================
const router = useRouter()

// ==================== 响应式数据 ====================
const loading = ref(false)
const orders = ref<Order[]>([])
const currentTab = ref('all')
const page = ref(1)
const pageSize = ref(20)
const hasMore = ref(true)
const total = ref(0)

// 订单状态标签
const tabs = computed(() => [
  { label: '全部', value: 'all', count: total.value },
  { label: '待支付', value: 'PENDING', count: orders.value.filter(o => o.status === 'PENDING').length },
  { label: '已支付', value: 'PAID', count: orders.value.filter(o => o.status === 'PAID').length },
  { label: '已完成', value: 'COMPLETED', count: orders.value.filter(o => o.status === 'COMPLETED').length },
  { label: '已取消', value: 'CANCELLED', count: orders.value.filter(o => o.status === 'CANCELLED').length }
])

// 过滤后的订单
const filteredOrders = computed(() => {
  if (currentTab.value === 'all') return orders.value
  return orders.value.filter(order => order.status === currentTab.value)
})

// ==================== 工具方法 ====================

const formatPrice = (price: number): string => {
  return price.toFixed(2)
}

const formatDate = (dateStr: string): string => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const month = (date.getMonth() + 1).toString().padStart(2, '0')
  const day = date.getDate().toString().padStart(2, '0')
  const hours = date.getHours().toString().padStart(2, '0')
  const minutes = date.getMinutes().toString().padStart(2, '0')
  return `${month}-${day} ${hours}:${minutes}`
}

const getStatusText = (status: string): string => {
  const map: Record<string, string> = {
    'PENDING': '待支付',
    'PAID': '已支付',
    'COMPLETED': '已完成',
    'CANCELLED': '已取消'
  }
  return map[status] || status
}

const getStatusClass = (status: string): string => {
  const map: Record<string, string> = {
    'PENDING': 'status-pending',
    'PAID': 'status-paid',
    'COMPLETED': 'status-completed',
    'CANCELLED': 'status-cancelled'
  }
  return map[status] || ''
}

// ==================== 页面跳转 ====================

const goToShop = () => {
  router.push({ name: 'UserDashboard' })
}

const goToPay = (orderNumber: string, amount: number) => {
  router.push({
    name: 'UserPayment',
    query: {
      orderNumber: orderNumber,
      amount: amount.toFixed(2)
    }
  })
}

const viewOrderDetail = (orderNumber: string) => {
  router.push({
    path: '/order/detail',
    query: { orderNumber: orderNumber }
  })
}

const switchTab = (tab: string) => {
  currentTab.value = tab
  // 切换标签时重新加载
  page.value = 1
  orders.value = []
  loadOrders()
}

// ==================== 加载订单数据 ====================

const loadOrders = async () => {
  if (loading.value) return
  
  loading.value = true
  
  try {
    const userId = localStorage.getItem('userId')
    if (!userId) {
      showToast('请先登录')
      router.push('/')
      return
    }
    
    const response = await authAPI.getUserOrders(userId, page.value, pageSize.value)
    const data = response.data || response
    
    if (data.success) {
      const newOrders = data.orders || []
      if (page.value === 1) {
        orders.value = newOrders
      } else {
        orders.value = [...orders.value, ...newOrders]
      }
      total.value = data.total
      hasMore.value = data.hasMore
    } else {
      showToast(data.message || '加载失败')
    }
    
  } catch (error) {
    console.error('加载订单失败:', error)
    showToast('加载失败，请重试')
  } finally {
    loading.value = false
  }
}

const loadMore = () => {
  if (hasMore.value && !loading.value) {
    page.value++
    loadOrders()
  }
}

// ==================== 生命周期 ====================
onMounted(() => {
  loadOrders()
})
</script>

<style scoped>
/* 样式保持不变，与之前相同 */
.order-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  background-color: #fef7e9;
  overflow: hidden;
}

.order-header {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px 20px;
  background: linear-gradient(145deg, #e65100, #bf360c);
  color: white;
  flex-shrink: 0;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  margin: 0;
}

.order-tabs {
  display: flex;
  gap: 8px;
  padding: 12px 16px;
  background-color: white;
  border-bottom: 1px solid #f0e0d0;
  flex-shrink: 0;
  overflow-x: auto;
  scrollbar-width: none;
}

.order-tabs::-webkit-scrollbar {
  display: none;
}

.tab-item {
  flex-shrink: 0;
  padding: 8px 20px;
  background-color: #f5e6d5;
  border-radius: 30px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
}

.tab-item.active {
  background: linear-gradient(145deg, #e65100, #bf360c);
  color: white;
  box-shadow: 0 2px 8px rgba(230, 81, 0, 0.3);
}

.tab-count {
  display: inline-block;
  margin-left: 6px;
  padding: 0 6px;
  background-color: rgba(0, 0, 0, 0.1);
  border-radius: 20px;
  font-size: 11px;
}

.tab-item.active .tab-count {
  background-color: rgba(255, 255, 255, 0.2);
}

.order-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  -webkit-overflow-scrolling: touch;
}

.order-list::-webkit-scrollbar {
  width: 4px;
}

.order-list::-webkit-scrollbar-track {
  background: #f0e0d0;
}

.order-list::-webkit-scrollbar-thumb {
  background: #e65100;
  border-radius: 4px;
}

.order-card {
  background-color: white;
  border-radius: 16px;
  margin-bottom: 16px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  border: 1px solid #f0e0d0;
  cursor: pointer;
  transition: all 0.2s;
}

.order-card:active {
  transform: scale(0.98);
}

.order-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0e0d0;
  margin-bottom: 12px;
}

.order-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.order-no {
  font-size: 13px;
  color: #b87c4b;
  font-family: monospace;
}

.order-time {
  font-size: 11px;
  color: #c0a080;
}

.order-status {
  font-size: 12px;
  font-weight: 600;
  padding: 4px 12px;
  border-radius: 20px;
}

.status-pending {
  background-color: #fff3e0;
  color: #ff9800;
}

.status-paid {
  background-color: #e8f5e9;
  color: #4caf50;
}

.status-completed {
  background-color: #e3f2fd;
  color: #2196f3;
}

.status-cancelled {
  background-color: #ffebee;
  color: #f44336;
}

.order-card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.order-amount {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.amount-label {
  font-size: 12px;
  color: #b87c4b;
}

.amount-value {
  font-size: 18px;
  font-weight: 700;
  color: #e65100;
}

.order-actions {
  display: flex;
  gap: 8px;
}

.action-btn {
  padding: 6px 16px;
  border-radius: 24px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
}

.pay-btn {
  background: linear-gradient(135deg, #e65100, #bf360c);
  color: white;
  box-shadow: 0 2px 4px rgba(230, 81, 0, 0.3);
}

.pay-btn:active {
  transform: scale(0.95);
}

.detail-btn {
  background-color: #f5e6d5;
  color: #5a3e2b;
}

.detail-btn:active {
  background-color: #e8d5c0;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  gap: 12px;
  color: #b87c4b;
}

.loading-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid #f0e0d0;
  border-top-color: #e65100;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  text-align: center;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
  opacity: 0.6;
}

.empty-text {
  font-size: 16px;
  font-weight: 500;
  color: #5a3e2b;
  margin-bottom: 24px;
}

.go-shop-btn {
  padding: 10px 32px;
  background: linear-gradient(135deg, #e65100, #bf360c);
  color: white;
  border: none;
  border-radius: 30px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
}

.go-shop-btn:active {
  transform: scale(0.95);
}

.load-more {
  text-align: center;
  padding: 16px;
  color: #e65100;
  font-size: 14px;
  cursor: pointer;
}

.no-more {
  text-align: center;
  padding: 16px;
  color: #c0a080;
  font-size: 12px;
}

/* 深色模式 */
@media (prefers-color-scheme: dark) {
  .order-page {
    background-color: #2a2a2a;
  }
  
  .order-header {
    background: linear-gradient(145deg, #bf360c, #8b2c0c);
  }
  
  .order-tabs {
    background-color: #333;
    border-bottom-color: #555;
  }
  
  .tab-item {
    background-color: #444;
    color: #ddd;
  }
  
  .tab-item.active {
    background: linear-gradient(145deg, #e65100, #bf360c);
  }
  
  .order-card {
    background-color: #333;
    border-color: #555;
  }
  
  .order-card-header,
  .order-card-footer {
    border-color: #555;
  }
  
  .order-no {
    color: #c0a080;
  }
  
  .order-time {
    color: #a56336;
  }
  
  .detail-btn {
    background-color: #444;
    color: #ffb87c;
  }
  
  .empty-text {
    color: #ffb87c;
  }
}
</style>