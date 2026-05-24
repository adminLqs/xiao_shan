<template>
  <div class="package-detail-page">
    <div class="page-header">
      <button class="btn-back" @click="router.back()">
        <i class="fas fa-chevron-left"></i>
      </button>
      <h2>我的套餐</h2>
    </div>

    <div v-if="loading" class="skeleton-container">
      <div class="skeleton skeleton-card" style="height: 160px; margin-bottom: 16px;"></div>
      <div class="skeleton skeleton-card" style="height: 200px;"></div>
    </div>

    <div v-else-if="packageInfo" class="detail-content">
      <div class="status-card" :class="packageInfo.currentPackage?.daysRemaining && packageInfo.currentPackage.daysRemaining > 0 ? 'ACTIVE' : 'EXPIRED'">
        <div class="status-icon">
          <i :class="packageInfo.currentPackage?.daysRemaining && packageInfo.currentPackage.daysRemaining > 0 ? 'fas fa-crown' : 'fas fa-exclamation-circle'"></i>
        </div>
        <div class="status-info">
          <div class="status-text">{{ packageInfo.currentPackage?.daysRemaining && packageInfo.currentPackage.daysRemaining > 0 ? '生效中' : '已到期' }}</div>
          <div class="package-name">{{ packageInfo.currentPackage?.name }}</div>
        </div>
        <div class="status-days" v-if="packageInfo.currentPackage?.daysRemaining && packageInfo.currentPackage.daysRemaining > 0">
          <span class="days-num">{{ packageInfo.currentPackage?.daysRemaining }}</span>
          <span class="days-label">天</span>
        </div>
      </div>

      <div class="info-card">
        <div class="card-title">套餐详情</div>
        <div class="info-row">
          <span class="label">套餐名称</span>
          <span class="value">{{ packageInfo.currentPackage?.name }}</span>
        </div>
        <div class="info-row">
          <span class="label">购买价格</span>
          <span class="value">¥{{ packageInfo.currentPackage?.price }}</span>
        </div>
        <div class="info-row">
          <span class="label">开通时间</span>
          <span class="value">{{ formatDate(packageInfo.currentPackage?.startDate || '') }}</span>
        </div>
        <div class="info-row">
          <span class="label">到期时间</span>
          <span class="value">{{ formatDate(packageInfo.currentPackage?.endDate || '') }}</span>
        </div>
      </div>

      <div class="info-card">
        <div class="card-title">套餐权益</div>
        <div class="feature-list">
          <div class="feature-item">
            <i class="fas fa-check-circle"></i>
            <span>{{ packageInfo.isUnlimited ? '商品数量无限制' : `最多发布 ${packageInfo.productLimit} 个商品` }}</span>
          </div>
        </div>
      </div>

      <div class="action-buttons">
        <button class="btn-renew" @click="router.push('/seller/package')">
          续费 / 升级
        </button>
      </div>
    </div>

    <div v-else class="empty-state">
      <i class="fas fa-store-slash"></i>
      <p>暂未购买套餐</p>
      <button @click="router.push('/seller/package')">去购买</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { authAPI } from '@/api/authAPI'
import Message from '@/utils/message'

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

const router = useRouter()
const loading = ref(true)
const packageInfo = ref<CurrentPackageInfo | null>(null)

const loadPackage = async () => {
  try {
    const res = await authAPI.getCurrentPackage()
    if (res.success && res.data) {
      packageInfo.value = res.data as CurrentPackageInfo
    }
  } catch {
    Message.error('加载失败')
  } finally {
    loading.value = false
  }
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`
}

onMounted(() => loadPackage())
</script>
