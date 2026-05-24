<template>
  <div class="seller-package-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-title">
        <i class="fas fa-gem"></i>
        商家套餐
      </h1>
    </div>

    <!-- 骨架屏 -->
    <div v-if="loading" class="skeleton-form" style="padding: 24px;">
      <div class="skeleton" style="height: 120px; margin-bottom: 24px; border-radius: 12px;"></div>
      <div class="skeleton" style="height: 32px; margin-bottom: 24px;"></div>
      <div class="packages-grid skeleton-grid">
        <div v-for="i in 3" :key="i" class="package-card skeleton-card">
          <div class="skeleton" style="width: 60%; height: 24px; margin-bottom: 12px;"></div>
          <div class="skeleton" style="width: 40%; height: 32px; margin-bottom: 16px;"></div>
          <div class="skeleton skeleton-line long" style="margin-bottom: 8px;"></div>
          <div class="skeleton skeleton-line medium" style="margin-bottom: 8px;"></div>
          <div class="skeleton skeleton-line short"></div>
          <div class="skeleton" style="height: 40px; margin-top: 20px; border-radius: 8px;"></div>
        </div>
      </div>
      <div class="skeleton" style="height: 32px; margin-top: 24px;"></div>
      <div class="skeleton" style="height: 60px; margin-top: 12px;"></div>
    </div>

    <!-- 套餐到期提示 -->
    <div v-if="currentPackage && !loading && currentPackage.currentPackage?.daysRemaining && currentPackage.currentPackage.daysRemaining < 7" class="expire-warning-bar">
      <i class="fas fa-exclamation-triangle"></i>
      <span>您的套餐即将到期，请及时续费</span>
    </div>

    <!-- 当前套餐信息 -->
    <div v-else-if="currentPackage" class="current-package-card">
      <div class="package-header">
        <div class="package-name">{{ currentPackage.currentPackage?.name }}</div>
        <div class="package-status active">生效中</div>
      </div>
      <div class="package-info">
        <div class="info-item">
          <span class="label">剩余天数：</span>
          <span class="value" :class="{ warning: currentPackage.currentPackage?.daysRemaining && currentPackage.currentPackage.daysRemaining < 7 }">
            {{ currentPackage.currentPackage?.daysRemaining }} 天
          </span>
        </div>
        <div class="info-item">
          <span class="label">商品数量：</span>
          <span class="value">
            {{ currentPackage.currentProductCount }} / {{ currentPackage.isUnlimited ? '无限' : currentPackage.productLimit }}
          </span>
        </div>
        <div class="info-item" v-if="!currentPackage.isUnlimited">
          <span class="label">剩余发布：</span>
          <span class="value">{{ currentPackage.remainingProducts }} 个</span>
        </div>
      </div>
      <div class="package-progress">
        <div class="progress-bar" :style="{ width: progressPercent + '%' }"></div>
      </div>
      <div class="card-actions">
        <button class="btn-renew" @click="scrollToPackages">
          <i class="fas fa-refresh"></i> 续费 / 升级
        </button>
        <button class="btn-view-detail" @click="showPackageDetail = !showPackageDetail">
          <i class="fas fa-info-circle"></i>
          {{ showPackageDetail ? '收起详情' : '查看我的套餐' }}
        </button>
      </div>
    </div>

    <!-- 套餐详情展开区域 -->
    <div v-if="showPackageDetail && currentPackage" class="package-detail-panel">
      <div class="detail-row">
        <span class="detail-label">套餐名称</span>
        <span class="detail-value">{{ currentPackage.currentPackage?.name }}</span>
      </div>
      <div class="detail-row">
        <span class="detail-label">状态</span>
        <span class="detail-value status" :class="currentPackage.currentPackage?.daysRemaining && currentPackage.currentPackage.daysRemaining > 0 ? 'ACTIVE' : 'EXPIRED'">
          {{ currentPackage.currentPackage?.daysRemaining && currentPackage.currentPackage.daysRemaining > 0 ? '生效中' : '已到期' }}
        </span>
      </div>
      <div class="detail-row">
        <span class="detail-label">开通时间</span>
        <span class="detail-value">{{ formatDate(currentPackage.currentPackage?.startDate || '') }}</span>
      </div>
      <div class="detail-row">
        <span class="detail-label">到期时间</span>
        <span class="detail-value">{{ formatDate(currentPackage.currentPackage?.endDate || '') }}</span>
      </div>
      <div class="detail-row">
        <span class="detail-label">剩余天数</span>
        <span class="detail-value" :class="{ warning: currentPackage.currentPackage?.daysRemaining && currentPackage.currentPackage.daysRemaining < 7 }">
          {{ currentPackage.currentPackage?.daysRemaining }} 天
        </span>
      </div>
      <div class="detail-row">
        <span class="detail-label">套餐权益</span>
        <span class="detail-value">
          <span class="feature-tag">{{ currentPackage.isUnlimited ? '商品数量无限制' : `最多发布 ${currentPackage.productLimit} 个商品` }}</span>
        </span>
      </div>
    </div>

    <!-- 未购买套餐提示 -->
    <div v-else class="no-package-card">
      <div class="no-package-icon">
        <i class="fas fa-gem"></i>
      </div>
      <h3>您还未购买套餐</h3>
      <p>购买套餐后即可发布商品，享受更多功能</p>
      <button class="btn-purchase-now" @click="scrollToPackages">
        <i class="fas fa-shopping-cart"></i>
        立即购买
      </button>
    </div>

    <!-- 套餐列表 -->
    <div class="packages-section">
      <h2 class="section-title">选择套餐</h2>
      <div class="packages-grid">
        <div
          v-for="pkg in packages"
          :key="pkg.id"
          class="package-card"
          :class="{ selected: selectedPackage?.id === pkg.id }"
          @click="selectPackage(pkg)"
        >
          <div class="card-header">
            <h3 class="package-name">{{ pkg.name }}</h3>
            <div class="package-price">
              <span class="currency">¥</span>
              <span class="amount">{{ pkg.price }}</span>
              <span class="period">/{{ pkg.durationDays }}天</span>
            </div>
          </div>
          <div class="card-body">
            <p class="package-desc">{{ pkg.description }}</p>
            <ul class="package-features">
              <li v-if="pkg.productLimit === -1">
                <i class="fas fa-check"></i> 商品数量无限制
              </li>
              <li v-else>
                <i class="fas fa-check"></i> 最多发布 {{ pkg.productLimit }} 个商品
              </li>
              <li v-if="pkg.durationDays">
                <i class="fas fa-clock"></i> 有效期 {{ pkg.durationDays }} 天
              </li>
            </ul>
          </div>
          <button class="btn-select" :class="{ active: selectedPackage?.id === pkg.id }">
            {{ selectedPackage?.id === pkg.id ? '已选择' : '选择套餐' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 购买按钮 -->
    <div class="purchase-section" v-if="selectedPackage">
      <button class="btn-purchase" @click="handlePurchase">
        <i class="fas fa-credit-card"></i>
        立即购买 ¥{{ selectedPackage.price }}
      </button>
    </div>

    <!-- 购买历史 -->
    <div class="history-section">
      <h2 class="section-title">购买历史</h2>
      <div v-if="history.length === 0" class="empty-history">
        暂无购买记录
      </div>
      <div v-else class="history-list">
        <div v-for="item in history" :key="item.id" class="history-item">
          <div class="history-info">
            <div class="history-name">{{ item.packageName }}</div>
            <div class="history-date">购买时间：{{ formatDate(item.createdAt) }}</div>
          </div>
          <div class="history-status" :class="getStatusClass(item.status)">
            {{ getStatusText(item.status) }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { authAPI } from '@/api/authAPI'
import Message from '@/utils/message'
import { useAuthStore } from '@/stores/auth'

interface Package {
  id: number
  name: string
  description: string
  price: number
  durationDays: number
  productLimit: number
  features: string
  isActive: boolean
  sortOrder: number
}

interface CurrentPackageInfo {
  hasPackage: boolean
  currentPackage?: {
    id: number
    name: string
    price: number
    startDate: string
    endDate: string
    daysRemaining: number
  }
  productLimit: number
  isUnlimited: boolean
  currentProductCount: number
  remainingProducts: number
  isExceeded: boolean
}

interface PackageOrder {
  id: number
  sellerId: number
  packageId: number
  packageName: string
  price: number
  startDate: string
  endDate: string
  status: string
  createdAt: string
}

const authStore = useAuthStore()
const router = useRouter()

const loading = ref(true)
const packages = ref<Package[]>([])
const currentPackage = ref<CurrentPackageInfo | null>(null)
const selectedPackage = ref<Package | null>(null)
const history = ref<PackageOrder[]>([])
const showPackageDetail = ref(false)

const progressPercent = computed(() => {
  if (!currentPackage.value || currentPackage.value.isUnlimited) return 0
  const used = currentPackage.value.currentProductCount
  const limit = currentPackage.value.productLimit
  return Math.min((used / limit) * 100, 100)
})

const loadPackages = async () => {
  try {
    const response = await authAPI.getPackages()
    if (response.success) {
      packages.value = response.data
    }
  } catch (error) {
    Message.error('加载套餐失败')
  }
}

const loadCurrentPackage = async () => {
  try {
    const response = await authAPI.getCurrentPackage()
    if (response.success) {
      currentPackage.value = response.data
    }
  } catch (error) {
    Message.error('加载当前套餐失败')
  }
}

const loadHistory = async () => {
  try {
    const response = await authAPI.getPackageHistory()
    if (response.success) {
      history.value = response.data
    }
  } catch (error) {
    Message.error('加载历史失败')
  }
}

const selectPackage = (pkg: Package) => {
  selectedPackage.value = pkg
}

const scrollToPackages = () => {
  const element = document.querySelector('.packages-section')
  if (element) {
    element.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
}

const handlePurchase = async () => {
  if (!selectedPackage.value) {
    Message.error('请先选择套餐')
    return
  }

  try {
    const response = await authAPI.purchasePackage(selectedPackage.value.id)
    if (response.success) {
      Message.success('套餐购买成功')
      await authStore.checkAndUpdate()
      router.go(0)
    }
  } catch (error: any) {
    Message.error(error.message || '购买失败')
  }
}

const formatDate = (dateStr: string): string => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN')
}

const getStatusClass = (status: string): string => {
  const map: Record<string, string> = {
    ACTIVE: 'status-active',
    EXPIRED: 'status-expired',
    CANCELLED: 'status-cancelled'
  }
  return map[status] || ''
}

const getStatusText = (status: string): string => {
  const map: Record<string, string> = {
    ACTIVE: '生效中',
    EXPIRED: '已到期',
    CANCELLED: '已取消'
  }
  return map[status] || status
}

onMounted(async () => {
  await Promise.all([
    loadPackages(),
    loadCurrentPackage(),
    loadHistory()
  ])
  loading.value = false
})
</script>

<style scoped>
@import url('@/static/css/seller/商家套餐.css');
</style>
