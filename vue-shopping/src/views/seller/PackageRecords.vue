<template>
  <div class="package-records-page">

    <!-- 当前套餐详情卡片 -->
    <div v-if="currentPackage?.hasPackage && currentPackage.currentPackage" class="current-card">
      <div class="card-header">
        <div class="package-icon">
          <i class="fas fa-crown"></i>
        </div>
        <span class="package-name">{{ currentPackage.currentPackage?.name }}</span>
        <span class="package-status" :class="getCurrentPackageStatusClass(currentPackage.currentPackage?.status)">
          {{ getCurrentPackageStatusText(currentPackage.currentPackage?.status) }}
        </span>
      </div>
      <div class="card-body">
        <span class="info-item">剩余时间: {{ currentPackage.currentPackage?.remainingTimeText || '已到期' }}</span>
        <span class="info-item">套餐价格: ¥{{ currentPackage.currentPackage?.price }}</span>
        <span class="info-item">开通时间: {{ formatDate(currentPackage.currentPackage?.startDate) }}</span>
        <span class="info-item">商品配额: {{ currentPackage.currentProductCount }} / {{ currentPackage.isUnlimited ? '∞' : currentPackage.productLimit }}</span>
      </div>
      <div class="card-footer">
        <button class="btn-renew" @click="router.push('/seller/package')">续费/升级</button>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else class="empty-card">
      <i class="fas fa-gem"></i>
      <p>暂未购买套餐</p>
      <button @click="router.push('/seller/package')">去购买</button>
    </div>

    <!-- 购买历史 -->
    <div class="history-section">
      <h3 class="section-title">购买记录</h3>

      <div v-if="loading" class="skeleton-list">
        <div v-for="i in 3" :key="i" class="skeleton-item">
          <div class="skeleton" style="height: 60px; border-radius: 8px;"></div>
        </div>
      </div>

      <div v-else-if="history.length === 0" class="empty-history">
        <i class="fas fa-receipt"></i>
        <p>暂无购买记录</p>
      </div>

      <div v-else class="history-list">
        <div v-for="item in history" :key="item.id" class="history-item" :class="getStatusClass(item.status)">
          <div class="history-row history-top">
            <span class="history-name">{{ item.packageName }}</span>
            <span class="history-price">¥{{ item.price }}</span>
          </div>
          <div class="history-row history-bottom">
            <span class="history-date">{{ formatDate(item.startDate) }} ~ {{ formatDate(item.endDate) }}</span>
            <span class="status-tag" :class="getStatusClass(item.status)">
              {{ getStatusText(item.status) }}
            </span>
          </div>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { authAPI } from '@/api/authAPI'
import Message from '@/utils/message'

const router = useRouter()

interface CurrentPackage {
  hasPackage: boolean
  currentPackage?: {
    name: string
    price: number
    startDate: string
    endDate: string
    daysRemaining: number
    remainingSeconds: number
    remainingTimeText: string
    status: string
  }
  currentProductCount?: number
  productLimit?: number
  isUnlimited?: boolean
  isExceeded?: boolean
}

interface HistoryItem {
  id: number
  packageName: string
  price: number
  startDate: string
  endDate: string
  createdAt: string
  status: string
}

const currentPackage = ref<CurrentPackage | null>(null)
const history = ref<HistoryItem[]>([])
const loading = ref(true)

const loadCurrentPackage = async () => {
  try {
    const response = await authAPI.getCurrentPackage()
    if (response.success && response.data) {
      const data = response.data.hasPackage !== undefined ? response.data : response.data.data
      currentPackage.value = data
    }
  } catch (error) {
    // 静默处理
  }
}

const loadHistory = async () => {
  try {
    const response = await authAPI.getPackageHistory()
    if (response.success && response.data) {
      history.value = response.data
    }
  } catch (error) {
    Message.error('加载购买记录失败')
  } finally {
    loading.value = false
  }
}

const formatDate = (dateStr: string | undefined | null): string => {
  if (!dateStr || dateStr.trim() === '' || dateStr === 'null' || dateStr === 'undefined') {
    return '-'
  }

  const date = new Date(dateStr)
  if (isNaN(date.getTime())) {
    return '-'
  }

  const year = date.getFullYear()
  if (year < 2000 || year > 2100) {
    return '-'
  }

  return `${year}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

const getCurrentPackageStatusClass = (status: string | undefined | null): string => {
  if (!status) return 'expired'
  switch (status) {
    case 'ACTIVE':
      return 'active'
    case 'PAUSED':
      return 'paused'
    case 'EXPIRED':
      return 'expired'
    default:
      return 'expired'
  }
}

const getCurrentPackageStatusText = (status: string | undefined | null): string => {
  if (!status) return '已到期'
  switch (status) {
    case 'ACTIVE':
      return '生效中'
    case 'PAUSED':
      return '暂停中'
    case 'EXPIRED':
      return '已到期'
    default:
      return '已到期'
  }
}

const getStatusClass = (status: string | undefined | null): string => {
  if (!status) return ''
  switch (status) {
    case 'ACTIVE':
      return 'status-active'
    case 'PAUSED':
      return 'status-paused'
    case 'EXPIRED':
      return 'status-expired'
    case 'CANCELLED':
      return 'status-cancelled'
    default:
      return ''
  }
}

const getStatusText = (status: string | undefined | null): string => {
  if (!status) return '未知'
  switch (status) {
    case 'ACTIVE':
      return '生效中'
    case 'PAUSED':
      return '暂停中'
    case 'EXPIRED':
      return '已到期'
    case 'CANCELLED':
      return '已取消'
    default:
      return status
  }
}

onMounted(() => {
  loadCurrentPackage()
  loadHistory()
})
</script>

<style scoped>
@import url('@/static/css/seller/套餐记录.css');
</style>
