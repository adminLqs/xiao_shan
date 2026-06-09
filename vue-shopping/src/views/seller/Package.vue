<template>
  <div class="package-container">
    <!-- ========== 当前套餐信息卡片（保留在顶部） ========== -->
    <div v-if="currentPackage?.hasPackage && !loading" class="current-package-info">
      <div class="current-info">
        <i class="fas fa-crown"></i>
        <span>{{ currentPackage.currentPackage?.name }}</span>
        <span class="dot">·</span>
        <span :class="{ warning: (currentPackage.currentPackage?.remainingSeconds ?? 0) < 7 * 24 * 3600 }">
          剩余{{ currentPackage.currentPackage?.remainingTimeText || '已到期' }}
          <span v-if="currentPackage.currentPackage?.status === 'PAUSED'">（暂停中）</span>
        </span>
        <span class="dot">·</span>
        <span>{{ currentPackage.isUnlimited ? '商品数量不限' : '可上架' + currentPackage.productLimit + '个商品' }}</span>
      </div>
      <button class="btn-action" @click="scrollToPackages">续费/升级</button>
    </div>

    <!-- ========== 未购买套餐状态 ========== -->
    <div v-else-if="!loading && !currentPackage?.hasPackage" class="no-package-card">
      <div class="no-package-icon">
        <i class="fas fa-store-slash"></i>
      </div>
      <p class="no-package-text">暂未购买套餐</p>
      <button class="btn-buy" @click="scrollToPackages">
        <i class="fas fa-shopping-cart"></i>
        <span>立即购买</span>
      </button>
    </div>

    <!-- ========== 套餐横向对比列表 ========== -->
    <div v-if="!loading" class="packages-container" id="packages-grid">
      <div class="section-title">选择套餐</div>
      <div v-if="packages.length === 0" class="empty-packages">
        暂无可用套餐
      </div>
      <div class="package-list">
        <div
          v-for="pkg in packages"
          :key="pkg.id"
          class="package-card"
          :class="{
            active: isCurrentPackage(pkg.id),
            upgrade: isUpgradePackage(pkg.id),
            downgrade: isDowngradePackage(pkg.id)
          }"
        >
          <div class="card-info">
            <div class="card-name">
              {{ pkg.name }}
              <span v-if="isCurrentPackage(pkg.id)" class="current-tag">✓当前</span>
            </div>
            <div class="card-features">
              <span>可上架{{ getProductLimitText(pkg) }}</span>
              <span v-if="checkFeature(pkg, 'analytics')">数据分析</span>
              <span v-if="checkFeature(pkg, 'ads')">广告投放</span>
              <span v-if="checkFeature(pkg, 'support')">专属客服</span>
              <span v-if="checkFeature(pkg, 'api')">API接口</span>
            </div>
          </div>
          <div class="card-price">
            <div class="price-row">
              <span class="amount">¥{{ pkg.price }}</span>
              <span class="period">/{{ pkg.durationDays }}天</span>
            </div>
            <button
              class="buy-btn"
              :class="{
                'btn-current': isCurrentPackage(pkg.id),
                'btn-upgrade': isUpgradePackage(pkg.id),
                'btn-downgrade': isDowngradePackage(pkg.id)
              }"
              @click="handleBuy(pkg)"
            >
              {{ getButtonText(pkg.id) }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { authAPI } from '@/api/authAPI'
import Message from '@/utils/message'

interface PackageInfo {
  id: number
  name: string
  price: number
  durationDays: number
  productLimit: number
  features: string
  description: string
}

interface FeatureMap {
  [key: string]: any
}

interface CurrentPackage {
  hasPackage: boolean
  currentPackage?: {
    packageId: number
    id: number
    name: string
    price: number
    startDate: string
    endDate: string
    daysRemaining: number
    remainingSeconds: number
    remainingTimeText: string
    status: string
  }
  productLimit: number
  isUnlimited: boolean
  currentProductCount: number
  remainingProducts: number
  isExceeded: boolean
}

const packages = ref<PackageInfo[]>([])
const currentPackage = ref<CurrentPackage | null>(null)
const loading = ref(true)

// 所有功能项
const allFeatures = computed(() => [
  { key: 'productLimit', label: '商品数量' },
  { key: 'analytics', label: '数据分析' },
  { key: 'ads', label: '广告投放' },
  { key: 'support', label: '专属客服' },
  { key: 'api', label: 'API接口' }
])

const loadPackages = async () => {
  try {
    loading.value = true
    const response = await authAPI.getPackages()
    if (response.success && response.data) {
      if (Array.isArray(response.data)) {
        packages.value = response.data
      } else if (response.data.packages && Array.isArray(response.data.packages)) {
        packages.value = response.data.packages
      } else if (response.data.records && Array.isArray(response.data.records)) {
        packages.value = response.data.records
      } else {
        packages.value = []
      }
    }
  } catch (error: any) {
    Message.error('加载套餐列表失败: ' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

const loadCurrentPackage = async () => {
  try {
    const response = await authAPI.getCurrentPackage()
    if (response.success && response.data) {
      currentPackage.value = response.data
    }
  } catch (error) {
    // 静默处理
  }
}

const scrollToPackages = () => {
  const element = document.getElementById('packages-grid')
  if (element) {
    element.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
}

const isCurrentPackage = (packageId: number): boolean => {
  return currentPackage.value?.currentPackage?.packageId === packageId
}

// 获取套餐的产品限制（-1 视为最高等级 Infinity）
const getPkgLimit = (pkg: PackageInfo): number => {
  return pkg.productLimit === -1 ? Infinity : pkg.productLimit
}

// 获取当前套餐的产品限制（-1 视为最高等级 Infinity）
const getCurrentPackageLimit = (): number => {
  const limit = currentPackage.value?.productLimit ?? 0
  return limit === -1 ? Infinity : limit
}

// 判断是否是升级套餐：目标等级 > 当前等级
const isUpgradePackage = (packageId: number): boolean => {
  if (!currentPackage.value?.hasPackage) return false
  if (isCurrentPackage(packageId)) return false

  const pkg = packages.value.find(p => p.id === packageId)
  if (!pkg) return false

  return getPkgLimit(pkg) > getCurrentPackageLimit()
}

// 判断是否是降级套餐：目标等级 < 当前等级
const isDowngradePackage = (packageId: number): boolean => {
  if (!currentPackage.value?.hasPackage) return false
  if (isCurrentPackage(packageId)) return false

  const pkg = packages.value.find(p => p.id === packageId)
  if (!pkg) return false

  return getPkgLimit(pkg) < getCurrentPackageLimit()
}

// 获取按钮文案
const getButtonText = (packageId: number): string => {
  if (isCurrentPackage(packageId)) {
    return '续费'
  }
  if (isUpgradePackage(packageId)) {
    return '升级'
  }
  return '购买'
}

// 获取商品数量文本
const getProductLimitText = (pkg: PackageInfo): string => {
  if (pkg.productLimit === -1) return '不限'
  return pkg.productLimit + ' 个商品'
}

// 检查套餐是否包含某个功能
const checkFeature = (pkg: PackageInfo, featureKey: string): boolean => {
  try {
    const features: FeatureMap = JSON.parse(pkg.features)
    return features[featureKey] === true
  } catch {
    return false
  }
}

// 获取功能状态样式类
const getFeatureStatus = (pkg: PackageInfo, featureKey: string): string => {
  return checkFeature(pkg, featureKey) ? 'feature-yes' : 'feature-no'
}

const getStatusClass = (status: string | undefined | null): string => {
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

const getStatusText = (status: string | undefined | null): string => {
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

const formatDate = (dateStr: string | undefined): string => {
  if (!dateStr || dateStr.trim() === '' || dateStr === 'null' || dateStr === 'undefined') {
    return '永久有效'
  }

  const date = new Date(dateStr)
  if (isNaN(date.getTime())) {
    return '永久有效'
  }

  const year = date.getFullYear()
  if (year < 2000 || year > 2100) {
    return '永久有效'
  }

  return `${year}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

const parseFeatures = (featuresStr: string, productLimit: number, durationDays: number): string[] => {
  const result: string[] = []

  if (productLimit === -1) {
    result.push('商品数量不限')
  } else {
    result.push(`最多发布${productLimit}个商品`)
  }

  result.push(`有效期${durationDays}天`)

  try {
    const features: FeatureMap = JSON.parse(featuresStr)

    if (features.analytics === true) {
      result.push('数据分析')
    }

    if (features.ads === true) {
      result.push('广告投放')
    }

    if (features.support === true) {
      result.push('专属客服')
    }

    if (features.api === true) {
      result.push('API接口')
    }

    if (features.priority && typeof features.priority === 'number') {
      const priorityLevels = ['', '普通', '较低', '中等', '较高', '高', '很高', 'VIP', 'SVIP', '至尊', '顶级']
      const level = Math.min(features.priority, priorityLevels.length - 1)
      result.push(`优先级: ${priorityLevels[level]}`)
    }
  } catch (error) {
    // 解析features失败，使用空数组
  }

  return result
}

const handleBuy = async (pkg: PackageInfo) => {
  try {
    Message.info('正在创建订单...')
    const response = await authAPI.purchasePackage(pkg.id)

    if (response.success) {
      // 用 window.open 打开支付页面
      const paymentHtml = response.data?.paymentHtml
      if (paymentHtml) {
        const payWindow = window.open('', '_blank')
        if (payWindow) {
          payWindow.document.write(paymentHtml)
          payWindow.document.close()
        }
      }
    }
  } catch (error: any) {
    Message.error(error.message || '购买失败')
  }
}

// ==================== WebSocket 自动处理 ====================
// 支付成功刷新页面
const onPaymentSuccessEvent = () => {
  Message.success('支付成功')
  setTimeout(() => window.location.reload(), 1000)
}

// 套餐购买成功刷新页面
const onPackageRenewEvent = (event: Event) => {
  const data = (event as CustomEvent).detail
  if (data?.type === 'renew') {

  }
}

onMounted(() => {
  loadPackages()
  loadCurrentPackage()

  // 监听支付成功事件
  window.addEventListener('payment-success', onPaymentSuccessEvent)
  // 监听套餐购买成功事件
  window.addEventListener('package-notification', onPackageRenewEvent)
})

onUnmounted(() => {
  // 移除监听
  window.removeEventListener('payment-success', onPaymentSuccessEvent)
  window.removeEventListener('package-notification', onPackageRenewEvent)
})
</script>

<style scoped>
@import url('@/static/css/seller/套餐页.css');
</style>
