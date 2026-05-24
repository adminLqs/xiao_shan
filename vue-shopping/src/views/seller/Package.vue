<template>
  <div class="package-container">
    <!-- ========== 当前套餐卡片 ========== -->
    <div v-if="currentPackage && !loading" class="current-package-card">
      <div class="package-header">
        <div class="package-icon">
          <i class="fas fa-crown"></i>
        </div>
        <div class="package-title">
          <span class="title-text">当前套餐</span>
          <span class="package-status" :class="currentPackage.status">
            {{ currentPackage.status === 'ACTIVE' ? '生效中' : '已到期' }}
          </span>
        </div>
      </div>
      <div class="package-info">
        <div class="package-name">{{ currentPackage.packageName }}</div>
        <div class="package-meta">
          <span>到期时间：{{ formatDate(currentPackage.endDate) }}</span>
          <span class="remaining-days" :class="{ warning: currentPackage.remainingDays < 7 }">
            剩余 {{ currentPackage.remainingDays }} 天
          </span>
        </div>
        <div class="package-features">
          <span v-for="(f, index) in currentPackage.features" :key="index" class="feature-tag">{{ f }}</span>
        </div>
      </div>
      <button class="btn-renew" @click="scrollToPackages">
        <i class="fas fa-refresh"></i>
        <span>续费 / 升级</span>
      </button>
    </div>

    <!-- ========== 未购买套餐状态 ========== -->
    <div v-else-if="!loading && !currentPackage" class="no-package-card">
      <div class="no-package-icon">
        <i class="fas fa-store-slash"></i>
      </div>
      <p class="no-package-text">暂未购买套餐</p>
      <button class="btn-buy" @click="scrollToPackages">
        <i class="fas fa-shopping-cart"></i>
        <span>立即购买</span>
      </button>
    </div>

    <div class="page-header">
      <h2>选择套餐</h2>
      <p>升级您的店铺功能，获得更多曝光机会</p>
    </div>

    <!-- ========== 加载状态 - 星环流光骨架屏 ========== -->
    <div v-if="loading" class="skeleton-form" style="padding: 0;">
      <div class="skeleton" style="height: 120px; margin-bottom: 24px; border-radius: 12px;"></div>
      <div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px;">
        <div v-for="i in 3" :key="i" class="skeleton-order-card" style="padding: 24px;">
          <div class="skeleton" style="width: 80px; height: 24px; margin-bottom: 16px;"></div>
          <div class="skeleton" style="width: 60%; height: 32px; margin-bottom: 20px;"></div>
          <div class="skeleton skeleton-line long" style="margin-bottom: 12px;"></div>
          <div class="skeleton skeleton-line medium" style="margin-bottom: 12px;"></div>
          <div class="skeleton skeleton-line short"></div>
          <div class="skeleton" style="height: 40px; margin-top: 24px; border-radius: 8px;"></div>
        </div>
      </div>
    </div>

    <!-- ========== 套餐内容 ========== -->
    <div v-else>

      <div class="packages-grid" id="packages-grid">
        <div
          v-for="pkg in packages"
        :key="pkg.id"
        class="package-card"
        :class="{ active: isCurrentPackage(pkg.id) }"
      >
        <div class="package-badge" v-if="isCurrentPackage(pkg.id)">
          <i class="fas fa-check"></i>
          <span>当前套餐</span>
        </div>

        <div class="package-header">
          <h3>{{ pkg.name }}</h3>
          <div class="price">
            <span class="currency">¥</span>
            <span class="amount">{{ pkg.price }}</span>
            <span class="period">/{{ pkg.durationDays }}天</span>
          </div>
        </div>

        <div class="package-features">
          <div class="feature-item" v-for="(feature, index) in parseFeatures(pkg.features, pkg.productLimit, pkg.durationDays)" :key="index">
            <i class="fas fa-check-circle"></i>
            <span>{{ feature }}</span>
          </div>
        </div>

        <button
          class="buy-btn"
          :class="{ disabled: isCurrentPackage(pkg.id) }"
          @click="handleBuy(pkg)"
          :disabled="isCurrentPackage(pkg.id)"
        >
          <i class="fas fa-shopping-cart"></i>
          <span>{{ isCurrentPackage(pkg.id) ? '已开通' : '立即购买' }}</span>
        </button>
      </div>
    </div>

    <!-- 支付确认弹窗 -->
    <div v-if="showPayConfirm" class="pay-confirm-overlay">
      <div class="pay-confirm-dialog">
        <i class="fas fa-check-circle pay-icon"></i>
        <h3>套餐订单已创建</h3>
        <p class="pay-tip">请在新窗口中完成支付</p>
        <div class="pay-actions">
          <button class="btn-pay-done" @click="checkPayStatus" :disabled="checkingPay">
            {{ checkingPay ? '查询中...' : '已完成支付' }}
          </button>
          <button class="btn-pay-later" @click="showPayConfirm = false">稍后支付</button>
        </div>
      </div>
    </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
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
  packageId: number
  packageName: string
  startDate: string
  endDate: string
  status: string
  remainingDays: number
  features: string[]
}

const packages = ref<PackageInfo[]>([])
const currentPackage = ref<CurrentPackage | null>(null)
const loading = ref(true)
const showPayConfirm = ref(false)
const packageOrderId = ref<number | null>(null)
const checkingPay = ref(false)

const loadPackages = async () => {
  try {
    loading.value = true
    const response = await authAPI.getPackages()
    if (response.success && response.data) {
      packages.value = response.data
    }
  } catch (error) {
    Message.error('加载套餐列表失败')
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
    // 如果没有当前套餐，静默处理
  }
}

const scrollToPackages = () => {
  const element = document.getElementById('packages-grid')
  if (element) {
    element.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
}

const isCurrentPackage = (packageId: number): boolean => {
  return currentPackage.value?.packageId === packageId
}

const formatDate = (dateStr: string | undefined): string => {
  // 检查日期字符串是否有效
  if (!dateStr || dateStr.trim() === '' || dateStr === 'null' || dateStr === 'undefined') {
    return '永久有效'
  }

  const date = new Date(dateStr)

  // 检查日期是否有效（Invalid Date 的时间戳是 NaN）
  if (isNaN(date.getTime())) {
    return '永久有效'
  }

  // 检查是否是无效的日期（如 1970年之前或其他异常日期）
  const year = date.getFullYear()
  if (year < 2000 || year > 2100) {
    return '永久有效'
  }

  return `${year}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

const parseFeatures = (featuresStr: string, productLimit: number, durationDays: number): string[] => {
  const result: string[] = []

  // 添加商品数量限制
  if (productLimit === -1) {
    result.push('商品数量不限')
  } else {
    result.push(`最多发布${productLimit}个商品`)
  }

  // 添加有效期
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
      // 1. 用 window.open 打开支付页面
      const paymentHtml = response.data?.paymentHtml
      if (paymentHtml) {
        const payWindow = window.open('', '_blank')
        if (payWindow) {
          payWindow.document.write(paymentHtml)
          payWindow.document.close()
        }
      }

      // 2. 保存订单 ID
      packageOrderId.value = response.data?.orderId

      // 3. 显示支付确认弹窗
      showPayConfirm.value = true
    }
  } catch (error: any) {
    Message.error(error.message || '购买失败')
  }
}

const checkPayStatus = async () => {
  if (!packageOrderId.value) return
  checkingPay.value = true

  try {
    const response = await authAPI.getPackageOrderStatus(packageOrderId.value)
    if (response.success && response.data?.status === 'ACTIVE') {
      showPayConfirm.value = false
      Message.success('支付成功，套餐已生效')
      await loadCurrentPackage()
      // 延迟刷新，让用户看到成功提示
      setTimeout(() => {
        window.location.reload()
      }, 1000)
    } else {
      Message.warning('暂未收到支付通知，请确认是否已完成支付')
    }
  } catch (error: any) {
    Message.error(error.message || '查询失败')
  } finally {
    checkingPay.value = false
  }
}

onMounted(() => {
  loadPackages()
  loadCurrentPackage()
})
</script>

<style scoped>
@import url('@/static/css/seller/套餐页.css');
</style>
