<template>
  <div class="coupons-page page-container">
    <!-- 顶部导航栏 -->
    <div class="page-navbar">
      <button class="page-nav-back" @click="router.back()">
        <i class="fas fa-chevron-left"></i>
      </button>
      <div class="page-nav-title">
        <i class="fas fa-ticket-alt"></i>
        <span>优惠券</span>
      </div>
      <div class="page-nav-right"></div>
    </div>

    <!-- 骨架屏加载 -->
    <div v-if="loading" class="skeleton-order-list">
      <div v-for="i in 3" :key="i" class="skeleton-card">
        <div class="skeleton-order-header">
          <div class="skeleton skeleton-line short"></div>
        </div>
        <div class="skeleton-order-goods">
          <div class="skeleton" style="width: 60px; height: 60px; border-radius: 8px;"></div>
          <div style="flex: 1;">
            <div class="skeleton skeleton-line long"></div>
            <div class="skeleton skeleton-line medium" style="margin-top: 8px;"></div>
          </div>
        </div>
      </div>
    </div>

    <div v-else>
      <!-- 可用优惠券 -->
      <div class="coupon-section">
        <div class="section-header">
          <div class="section-title">
            <i class="fas fa-gift"></i>
            <span>可使用</span>
          </div>
          <span class="section-count">{{ availableCoupons.length }}</span>
        </div>
        <div class="coupon-list">
          <div
            v-for="coupon in availableCoupons"
            :key="coupon.id"
            class="coupon-card available"
            @click="showCouponDetail(coupon)"
          >
            <div class="coupon-left">
              <div class="coupon-amount">¥{{ coupon.amount }}</div>
              <div class="coupon-condition">满{{ coupon.minAmount }}可用</div>
            </div>
            <div class="coupon-right">
              <div class="coupon-name">{{ coupon.name }}</div>
              <div class="coupon-time">{{ coupon.validPeriod }}</div>
              <div class="coupon-shop" v-if="coupon.shopName">{{ coupon.shopName }}</div>
            </div>
            <div class="coupon-corner"></div>
          </div>
          <div v-if="availableCoupons.length === 0" class="empty-section">
            <i class="fas fa-ticket-alt"></i>
            <p>暂无可用优惠券</p>
          </div>
        </div>
      </div>

      <!-- 已使用/已过期 -->
      <div class="coupon-section">
        <div class="section-header">
          <div class="section-title">
            <i class="fas fa-history"></i>
            <span>已使用/已过期</span>
          </div>
          <span class="section-count">{{ usedExpiredCoupons.length }}</span>
        </div>
        <div class="coupon-list">
          <div
            v-for="coupon in usedExpiredCoupons"
            :key="coupon.id"
            class="coupon-card disabled"
          >
            <div class="coupon-left">
              <div class="coupon-amount">¥{{ coupon.amount }}</div>
              <div class="coupon-condition">满{{ coupon.minAmount }}可用</div>
            </div>
            <div class="coupon-right">
              <div class="coupon-name">{{ coupon.name }}</div>
              <div class="coupon-time">{{ coupon.validPeriod }}</div>
              <div class="coupon-status">{{ coupon.status === 'USED' ? '已使用' : '已过期' }}</div>
            </div>
            <div class="coupon-corner"></div>
          </div>
          <div v-if="usedExpiredCoupons.length === 0" class="empty-section">
            <i class="fas fa-history"></i>
            <p>暂无历史记录</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 领取优惠券入口 -->
    <div class="get-coupons-entry">
      <div class="entry-card">
        <div class="entry-icon">
          <i class="fas fa-plus-circle"></i>
        </div>
        <div class="entry-content">
          <div class="entry-title">领取更多优惠券</div>
          <div class="entry-desc">领取店铺专属优惠券，享受更多优惠</div>
        </div>
        <button class="entry-btn" @click="goToCouponCenter">
          <span>去领取</span>
          <i class="fas fa-arrow-right"></i>
        </button>
      </div>
    </div>

    <!-- 优惠券详情弹窗 -->
    <div class="coupon-detail-overlay" v-if="showDetail" @click="closeDetail">
      <div class="coupon-detail-card" @click.stop>
        <button class="detail-close" @click="closeDetail">
          <i class="fas fa-times"></i>
        </button>
        <div class="detail-header">
          <div class="detail-amount">¥{{ selectedCoupon?.amount }}</div>
          <div class="detail-condition">满{{ selectedCoupon?.minAmount }}元可用</div>
        </div>
        <div class="detail-body">
          <div class="detail-item">
            <i class="fas fa-ticket-alt"></i>
            <span>{{ selectedCoupon?.name }}</span>
          </div>
          <div class="detail-item">
            <i class="fas fa-store"></i>
            <span>{{ selectedCoupon?.shopName || '全店通用' }}</span>
          </div>
          <div class="detail-item">
            <i class="fas fa-calendar"></i>
            <span>{{ selectedCoupon?.validPeriod }}</span>
          </div>
          <div class="detail-item">
            <i class="fas fa-info-circle"></i>
            <span>{{ selectedCoupon?.description || '无使用说明' }}</span>
          </div>
        </div>
        <button class="detail-use-btn" @click="useCoupon">立即使用</button>
      </div>
    </div>

    <!-- 底部留空 -->
    <div class="bottom-space"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { authAPI } from '@/api/authAPI'
import Message from '@/utils/message'

const router = useRouter()

const loading = ref(true)
const coupons = ref<any[]>([])

const availableCoupons = computed(() => {
  return coupons.value.filter(c => c.status === 'AVAILABLE')
})

const usedExpiredCoupons = computed(() => {
  return coupons.value.filter(c => c.status === 'USED' || c.status === 'EXPIRED')
})

const showDetail = ref(false)
const selectedCoupon = ref<any>(null)

const loadCoupons = async () => {
  loading.value = true
  try {
    const response = await authAPI.getUserCoupons()
    if (response.success && response.data) {
      coupons.value = response.data.map((item: any) => ({
        id: item.id,
        name: item.couponName,
        amount: item.amount,
        minAmount: item.minAmount,
        validPeriod: formatPeriod(item.startTime, item.endTime),
        shopName: item.shopName,
        status: item.status,
        description: item.description
      }))
    } else {
      throw new Error(response.message || '加载失败')
    }
  } catch (error: any) {
    Message.error(error.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const formatPeriod = (start: string, end: string) => {
  if (!start || !end) return ''
  const startDate = new Date(start)
  const endDate = new Date(end)
  return `${startDate.getMonth() + 1}/${startDate.getDate()} - ${endDate.getMonth() + 1}/${endDate.getDate()}`
}

const showCouponDetail = (coupon: any) => {
  selectedCoupon.value = coupon
  showDetail.value = true
}

const closeDetail = () => {
  showDetail.value = false
  selectedCoupon.value = null
}

const useCoupon = () => {
  if (selectedCoupon.value?.shopId) {
    router.push({ name: 'Shop', params: { shopId: selectedCoupon.value.shopId } })
  } else {
    router.push({ name: 'Categories' })
  }
  closeDetail()
}

const goToCouponCenter = () => {
  Message.info('优惠券中心开发中')
}

onMounted(async () => {
  await loadCoupons()
})
</script>

<style scoped>
@import url('@/static/css/user/优惠券.css');
</style>