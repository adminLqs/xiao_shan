<template>
  <div class="user-center">
    <div class="center-header">
      <div class="header-actions">
        <i class="fas fa-cog" @click="goToSetting"></i>
        <i class="fas fa-bell"></i>
      </div>
    </div>

    <div class="user-info-card" @click="goToProfile">
      <img :src="userProfile.avatar || defaultAvatar" class="user-avatar" />
      <div class="user-text">
        <span class="user-name">{{ userProfile.nickname || '用户' }}</span>
        <span class="user-tip">查看个人资料 ></span>
      </div>
    </div>

    <div class="order-status-bar">
      <div v-for="item in orderStatusItems" :key="item.status" class="status-item" @click="goToOrders(item.status)">
        <i :class="item.icon"></i>
        <span>{{ item.label }}</span>
        <span class="badge" v-if="item.count > 0">{{ item.count }}</span>
      </div>
    </div>

    <div class="menu-list">
      <div v-for="item in menuItems" :key="item.name" class="menu-item" @click="goTo(item.name)">
        <i :class="item.icon"></i>
        <span>{{ item.label }}</span>
        <span class="badge" v-if="item.count > 0">{{ item.count }}</span>
        <i class="fas fa-chevron-right arrow"></i>
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
import defaultAvatar from '@/static/images/user-avatar.jpg'

const authStore = useAuthStore()
const router = useRouter()

const userProfile = ref<any>({
  avatar: '',
  nickname: ''
})

const orderCount = ref(0)
const favoriteCount = ref(0)
const couponCount = ref(0)

const orderStatusItems = ref([
  { status: 'PENDING', label: '待付款', icon: 'fas fa-wallet', count: 0 },
  { status: 'PAID', label: '待发货', icon: 'fas fa-box', count: 0 },
  { status: 'SHIPPED', label: '待收货', icon: 'fas fa-truck', count: 0 },
  { status: 'COMPLETED', label: '待评价', icon: 'fas fa-star', count: 0 },
])

const menuItems = computed(() => [
  { name: 'UserOrders', label: '我的订单', icon: 'fas fa-clipboard-list', count: orderCount.value },
  { name: 'UserFavorites', label: '我的收藏', icon: 'fas fa-heart', count: favoriteCount.value },
  { name: 'UserAddresses', label: '收货地址', icon: 'fas fa-map-marker-alt', count: 0 },
  { name: '', label: '优惠券', icon: 'fas fa-tag', count: couponCount.value },
  { name: 'UserProfile', label: '个人资料', icon: 'fas fa-user-edit', count: 0 },
  { name: 'UserSetting', label: '账户设置', icon: 'fas fa-cog', count: 0 },
])

const goTo = (name: string) => {
  if (!name) {
    Message.info('优惠券功能正在扩展中，敬请期待...')
    return
  }
  router.push({ name })
}

const goToOrders = (status: string) => router.push({ name: 'UserOrders', query: { status } })
const goToProfile = () => router.push({ name: 'UserProfile' })
const goToSetting = () => router.push({ name: 'UserSetting' })

const loadUserProfile = async () => {
  try {
    const response = await authAPI.getUserProfile()
    if (response.success && response.data?.profile) {
      userProfile.value = {
        avatar: response.data.profile.avatar || '',
        nickname: response.data.profile.nickname || ''
      }
    }
  } catch {
    // 静默处理
  }
}

const loadStats = async () => {
  try {
    const response = await authAPI.getOrderCounts()
    if (response.success && response.data?.counts) {
      const counts = response.data.counts
      orderStatusItems.value = [
        { status: 'PENDING', label: '待付款', icon: 'fas fa-wallet', count: counts.PENDING || 0 },
        { status: 'PAID', label: '待发货', icon: 'fas fa-box', count: counts.PAID || 0 },
        { status: 'SHIPPED', label: '待收货', icon: 'fas fa-truck', count: counts.SHIPPED || 0 },
        { status: 'COMPLETED', label: '待评价', icon: 'fas fa-star', count: counts.COMPLETED || 0 },
      ]
      orderCount.value = Object.values(counts).reduce((sum: number, val: any) => sum + (val || 0), 0)
    }
  } catch {
    // 静默处理
  }

  try {
    const favResponse = await authAPI.getFavorites({ page: 1, pageSize: 1 })
    if (favResponse.success) {
      favoriteCount.value = favResponse.data?.total || 0
    }
  } catch {
    // 静默处理
  }

  // TODO: 后期扩展 - 优惠券功能
}

onMounted(async () => {
  if (!authStore.validateUserPermission()) return

  await Promise.all([
    loadUserProfile(),
    loadStats()
  ])
})
</script>

<style scoped>
  @import url('@/static/css/user/个人中心.css');
</style>
