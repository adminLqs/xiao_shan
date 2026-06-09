<template>
  <div class="my-coupons-page">
    <div class="page-navbar">
      <button class="page-nav-back" @click="router.back()">
        <i class="fas fa-chevron-left"></i>
      </button>
      <div class="page-nav-title">
        <span>我的优惠券</span>
      </div>
      <div class="page-nav-right"></div>
    </div>

    <div class="tab-bar" ref="tabsRef">
      <div
        v-for="tab in tabs"
        :key="tab.value"
        class="tab-item"
        :class="{ active: activeTab === tab.value }"
        :ref="el => { if (el) tabRefs[tab.value] = el as HTMLElement }"
        @click="switchTab(tab.value)"
      >
        {{ tab.label }}
      </div>
      <div class="tab-underline" :style="underlineStyle"></div>
    </div>

    <div class="tab-content">
      <div v-if="loading" class="skeleton-coupon-list">
        <div v-for="i in 4" :key="i" class="skeleton-card">
          <div class="skeleton skeleton-left"></div>
          <div class="skeleton-right">
            <div class="skeleton skeleton-line short"></div>
            <div class="skeleton skeleton-line medium"></div>
            <div class="skeleton skeleton-line short"></div>
          </div>
        </div>
      </div>

      <div v-else class="coupon-list">
      <div
        v-for="coupon in coupons"
        :key="coupon.id"
        class="coupon-card"
        :class="activeTab === 'USED' ? 'used' : activeTab === 'EXPIRED' ? 'expired' : ''"
      >
        <div class="coupon-circle-left"></div>
        <div class="coupon-left">
          <template v-if="coupon.type === 'DISCOUNT'">
            <div class="coupon-discount">{{ coupon.discount }}折</div>
          </template>
          <template v-else>
            <div class="coupon-amount">
              <span class="symbol">¥</span>{{ coupon.amount }}
            </div>
          </template>
          <div class="coupon-condition">
            {{ coupon.minAmount > 0 ? `满${coupon.minAmount}可用` : '无门槛' }}
          </div>
        </div>
        <div class="coupon-right">
          <div class="coupon-info">
            <div class="coupon-name">{{ coupon.name }}</div>
            <div class="coupon-desc">{{ coupon.description || '全场通用' }}</div>
            <div class="coupon-validity">
              <i class="far fa-clock"></i>
              {{ formatValidity(coupon.startTime, coupon.endTime) }}
            </div>
          </div>
          <div class="coupon-bottom">
            <template v-if="activeTab === 'UNUSED'">
              <button class="use-btn" @click="handleUse(coupon)">
                去使用
              </button>
            </template>
            <template v-else-if="activeTab === 'USED'">
              <span class="status-tag used">已使用</span>
            </template>
            <template v-else>
              <span class="status-tag expired">已过期</span>
            </template>
          </div>
        </div>
        <div class="coupon-circle-right"></div>
      </div>

      <div v-if="coupons.length === 0 && !loading" class="empty-section">
        <i class="fas fa-ticket-alt"></i>
        <p>{{ emptyText }}</p>
      </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { authAPI } from '@/api/authAPI'
import { ElMessage } from 'element-plus'

const router = useRouter()

const tabs: { label: string; value: 'UNUSED' | 'USED' | 'EXPIRED' }[] = [
  { label: '未使用', value: 'UNUSED' },
  { label: '已使用', value: 'USED' },
  { label: '已过期', value: 'EXPIRED' }
]

const activeTab = ref<'UNUSED' | 'USED' | 'EXPIRED'>('UNUSED')
const loading = ref(true)
const coupons = ref<any[]>([])
const tabsRef = ref<HTMLElement>()
const tabRefs = ref<Record<string, HTMLElement>>({})
const underlineStyle = ref({ left: '0px', width: '0px' })

const emptyText = computed(() => {
  const map: Record<string, string> = {
    UNUSED: '暂无可用优惠券',
    USED: '暂无已使用优惠券',
    EXPIRED: '暂无已过期优惠券'
  }
  return map[activeTab.value]
})

const formatValidity = (start: string, end: string) => {
  if (!start || !end) return '长期有效'
  const startDate = new Date(start)
  const endDate = new Date(end)
  return `${startDate.getFullYear()}.${String(startDate.getMonth() + 1).padStart(2, '0')}.${String(startDate.getDate()).padStart(2, '0')} - ${endDate.getFullYear()}.${String(endDate.getMonth() + 1).padStart(2, '0')}.${String(endDate.getDate()).padStart(2, '0')}`
}

const updateUnderline = (value: string) => {
  const tab = tabRefs.value[value]
  const container = tabsRef.value
  if (!tab || !container) return

  const tabRect = tab.getBoundingClientRect()
  const containerRect = container.getBoundingClientRect()

  underlineStyle.value = {
    left: (tabRect.left - containerRect.left) + 'px',
    width: tabRect.width + 'px'
  }
}

const loadCoupons = async () => {
  loading.value = true
  try {
    const response = await authAPI.getUserCoupons(activeTab.value)
    if (response.success && response.data) {
      const data = response.data
      const list = Array.isArray(data) ? data : (data.records || data.list || [])
      coupons.value = list.map((item: any) => ({
        id: item.id,
        name: item.name || item.couponName,
        type: item.type || 'CASH',
        amount: item.amount || 0,
        discount: item.discount || 0,
        minAmount: item.minAmount || 0,
        description: item.description,
        startTime: item.startTime,
        endTime: item.endTime,
        status: item.status
      }))
    }
  } catch (error: any) {
    ElMessage.error(error.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const switchTab = (tab: 'UNUSED' | 'USED' | 'EXPIRED') => {
  if (activeTab.value === tab) return
  activeTab.value = tab
  nextTick(() => updateUnderline(tab))
}

const handleUse = (coupon: any) => {
  ElMessage.info('请在结算时选择使用优惠券')
  router.push({ name: 'UserDashboard' })
}

watch(activeTab, () => {
  loadCoupons()
})

onMounted(() => {
  loadCoupons()
  nextTick(() => updateUnderline(activeTab.value))
})
</script>

<style scoped>
@import url('@/static/css/user/我的优惠券.css');
</style>
