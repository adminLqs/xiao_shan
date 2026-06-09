<template>
  <div class="coupon-center-page">
    <div class="page-navbar">
      <button class="page-nav-back" @click="router.back()">
        <i class="fas fa-chevron-left"></i>
      </button>
      <div class="page-nav-title">
        <span>领券中心</span>
      </div>
      <div class="page-nav-right"></div>
    </div>

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
        :class="{ received: coupon.received }"
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
            <template v-if="coupon.received">
              <span class="received-tag">已领取</span>
            </template>
            <template v-else>
              <button
                class="receive-btn"
                :disabled="receivingId === coupon.id"
                @click="handleReceive(coupon.id)"
              >
                {{ receivingId === coupon.id ? '领取中...' : '立即领取' }}
              </button>
            </template>
          </div>
        </div>
        <div class="coupon-circle-right"></div>
      </div>

      <div v-if="coupons.length === 0" class="empty-section">
        <i class="fas fa-ticket-alt"></i>
        <p>暂无可用优惠券</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { authAPI } from '@/api/authAPI'
import { ElMessage } from 'element-plus'

const router = useRouter()

const loading = ref(true)
const coupons = ref<any[]>([])
const receivingId = ref<number | null>(null)

const formatValidity = (start: string, end: string) => {
  if (!start || !end) return '长期有效'
  const startDate = new Date(start)
  const endDate = new Date(end)
  return `${startDate.getFullYear()}.${String(startDate.getMonth() + 1).padStart(2, '0')}.${String(startDate.getDate()).padStart(2, '0')} - ${endDate.getFullYear()}.${String(endDate.getMonth() + 1).padStart(2, '0')}.${String(endDate.getDate()).padStart(2, '0')}`
}

const loadCoupons = async () => {
  loading.value = true
  try {
    const response = await authAPI.getAvailableCoupons()
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
        received: item.received || false
      }))
    }
  } catch (error: any) {
    ElMessage.error(error.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const handleReceive = async (couponId: number) => {
  if (receivingId.value) return
  receivingId.value = couponId
  try {
    const response = await authAPI.receiveCoupon(couponId)
    if (response.success) {
      ElMessage.success('领取成功')
      const coupon = coupons.value.find(c => c.id === couponId)
      if (coupon) {
        coupon.received = true
      }
    } else {
      ElMessage.error(response.message || '领取失败')
    }
  } catch (error: any) {
    ElMessage.error(error.message || '领取失败')
  } finally {
    receivingId.value = null
  }
}

onMounted(() => {
  loadCoupons()
})
</script>

<style scoped>
@import url('@/static/css/user/领券中心.css');
</style>
