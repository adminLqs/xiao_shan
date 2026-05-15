<!-- components/seller/Orders.vue -->
<template>
  <div class="seller-orders">
    <div class="page-header">
      <h2>订单管理</h2>
      <div class="header-actions">
        <select v-model="statusFilter" class="status-filter">
          <option value="">全部订单</option>
          <option value="PENDING">待支付</option>
          <option value="PAID">已支付</option>
          <option value="COMPLETED">已完成</option>
          <option value="CANCELLED">已取消</option>
        </select>
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
              <th>订单状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="order in orders" :key="order.id">
              <td class="order-no">{{ order.orderNumber }}</td>
              <td>{{ formatDate(order.createdAt) }}</td>
              <td>¥{{ order.totalAmount }}</td>
              <td>{{ getPaymentMethodText(order.paymentMethod) }}</td>
              <td>
                <span class="status-badge" :class="order.status">
                  {{ getStatusText(order.status) }}
                </span>
              </td>
              <td>
                <button class="detail-btn" @click="viewDetail(order.orderNumber)">查看详情</button>
                <button v-if="order.status === 'PENDING'" class="cancel-btn" @click="cancelOrder(order.orderNumber)">取消订单</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      
      <!-- 分页 -->
      <div class="pagination" v-if="total > pageSize">
        <button @click="prevPage" :disabled="page === 1">上一页</button>
        <span>{{ page }} / {{ totalPages }}</span>
        <button @click="nextPage" :disabled="page === totalPages">下一页</button>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { authAPI } from '@/api/auth'

const router = useRouter()
const loading = ref(false)
const orders = ref<any[]>([])
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const statusFilter = ref('')

const totalPages = computed(() => Math.ceil(total.value / pageSize.value))

const formatDate = (dateStr: string) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${date.getFullYear()}-${date.getMonth() + 1}-${date.getDate()} ${date.getHours()}:${date.getMinutes()}`
}

const getStatusText = (status: string) => {
  const map: Record<string, string> = {
    'PENDING': '待支付',
    'PAID': '已支付',
    'COMPLETED': '已完成',
    'CANCELLED': '已取消'
  }
  return map[status] || status
}

const getPaymentMethodText = (method: string) => {
  const map: Record<string, string> = {
    'WECHAT': '微信支付',
    'ALIPAY': '支付宝',
  }
  return map[method] || method || '未支付'
}

const loadOrders = async () => {
  loading.value = true
  try {
    const response = await authAPI.getSellerOrders(page.value, pageSize.value, statusFilter.value)
    const data = response.data || response
    if (data.success) {
      orders.value = data.orders
      total.value = data.total
    }
  } catch (error) {
    console.error('加载订单失败:', error)
    showToast('加载失败')
  } finally {
    loading.value = false
  }
}

const viewDetail = (orderNumber: string) => {
  router.push({ path: '/order/detail', query: { orderNumber } })
}

const cancelOrder = async (orderNumber: string) => {
  if (!confirm('确定要取消该订单吗？')) return
  try {
    const response = await authAPI.cancelOrder(orderNumber)
    const data = response.data || response
    if (data.success) {
      showToast('订单已取消')
      loadOrders()
    } else {
      showToast(data.message || '取消失败')
    }
  } catch (error) {
    showToast('取消失败')
  }
}

const prevPage = () => {
  if (page.value > 1) {
    page.value--
    loadOrders()
  }
}

const nextPage = () => {
  if (page.value < totalPages.value) {
    page.value++
    loadOrders()
  }
}

watch(statusFilter, () => {
  page.value = 1
  loadOrders()
})

onMounted(() => {
  loadOrders()
})
</script>

<style scoped>
  @import url('@/static/css/商家订单管理页.css');
</style>