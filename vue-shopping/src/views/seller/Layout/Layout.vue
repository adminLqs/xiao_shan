<template>
  <div v-if="isInitialLoading" class="layout-skeleton">
    <div class="skeleton-topbar">
      <div class="skeleton-avatar"></div>
      <div class="skeleton-info">
        <div class="skeleton-line short"></div>
        <div class="skeleton-line shorter"></div>
      </div>
    </div>
    <div class="skeleton-sidebar">
      <div v-for="i in 5" :key="i" class="skeleton-menu-item">
        <div class="skeleton-icon"></div>
        <div class="skeleton-line medium"></div>
      </div>
    </div>
    <div class="skeleton-content">
      <div class="skeleton-card"></div>
      <div class="skeleton-card short"></div>
      <div class="skeleton-card"></div>
    </div>
  </div>

  <div v-else>
  <div class="dashboard-container" v-if="isLoggedIn">
    <div class="sidebar" :class="{ collapsed: isSidebarCollapsed && !isMobile, 'mobile-open': isMobileMenuOpen }">
      <div class="sidebar-header">
        <h2 class="brand-logo">
          <i class="fas fa-sparkles"></i>
          <span v-if="!isSidebarCollapsed">云杉购</span>
        </h2>
        <button class="collapse-btn" @click="isMobile ? closeMobileMenu() : toggleSidebar()">
          <i :class="isSidebarCollapsed ? 'fas fa-chevron-right' : 'fas fa-chevron-left'"></i>
        </button>
      </div>

      <div class="sidebar-menu">
        <div class="menu-section">
          <div class="menu-title" v-if="!isSidebarCollapsed">核心功能</div>
          <RouterLink
            :to="{name: 'SellerDashboard'}"
            class="menu-item"
            :class="{ active: $route.name === 'SellerDashboard' }"
          >
            <i class="fas fa-tachometer-alt"></i>
            <span>控制台</span>
          </RouterLink>
          <RouterLink
            :to="{name: 'SellerProducts'}"
            class="menu-item"
            :class="{ active: $route.name === 'SellerProducts' || $route.name === 'SellerProductEdit' }"
          >
            <i class="fas fa-box"></i>
            <span>商品管理</span>
          </RouterLink>
          <RouterLink
            :to="{name: 'SellerAddProduct'}"
            class="menu-item"
            :class="{ active: $route.name === 'SellerAddProduct' }"
          >
            <i class="fas fa-plus-circle"></i>
            <span>发布商品</span>
          </RouterLink>
          <RouterLink
            :to="{name: 'SellerCoupons'}"
            class="menu-item"
            :class="{ active: $route.name === 'SellerCoupons' }"
          >
            <i class="fas fa-ticket-alt"></i>
            <span>优惠券管理</span>
          </RouterLink>
        </div>

        <div class="menu-section">
          <div class="menu-title" v-if="!isSidebarCollapsed">运营管理</div>
          <RouterLink
            :to="{name: 'SellerOrders'}"
            class="menu-item"
            :class="{ active: $route.name === 'SellerOrders' }"
          >
            <i class="fas fa-shopping-cart"></i>
            <span>订单管理</span>
          </RouterLink>
          <RouterLink
            :to="{name: 'SellerAnalytics'}"
            class="menu-item"
            :class="{ active: $route.name === 'SellerAnalytics' }"
          >
            <i class="fas fa-chart-line"></i>
            <span>数据分析</span>
          </RouterLink>
        </div>

        <div class="menu-section">
          <div class="menu-title" v-if="!isSidebarCollapsed">店铺管理</div>
          <RouterLink
            :to="{name:'SellerProfile'}"
            class="menu-item"
            :class="{ active: $route.name === 'SellerProfile' }"
          >
            <i class="fas fa-store-alt"></i>
            <span>店铺信息</span>
          </RouterLink>
          <RouterLink
            :to="{name: 'SellerMessages'}"
            class="menu-item"
            :class="{ active: $route.name === 'SellerMessages' }"
          >
            <i class="fas fa-bell"></i>
            <span>消息中心</span>
            <span v-if="unreadCount > 0" class="menu-badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
          </RouterLink>
          <RouterLink
            :to="{name: 'SellerPackage'}"
            class="menu-item"
            :class="{ active: $route.name === 'SellerPackage' }"
          >
            <i class="fas fa-crown"></i>
            <span>套餐购买</span>
          </RouterLink>
          <RouterLink
            :to="{name: 'SellerPackageRecords'}"
            class="menu-item"
            :class="{ active: $route.name === 'SellerPackageRecords' }"
          >
            <i class="fas fa-history"></i>
            <span>套餐记录</span>
          </RouterLink>
        </div>

        <!-- 套餐信息 -->
        <div class="sidebar-package" v-if="packageInfo && !isSidebarCollapsed">
          <div class="package-badge">
            <i class="fas fa-gem"></i>
            <span>{{ packageInfo.name }}</span>
            <span v-if="(packageInfo.remainingSeconds ?? 0) < 7 * 24 * 3600" class="expire-warning">
              <i class="fas fa-exclamation-circle"></i>
            </span>
          </div>
          <div class="package-meta">
            <span>{{ packageInfo.remainingProducts === -1 ? '不限商品位' : packageInfo.remainingProducts + ' 个商品位' }}</span>
            <span>·</span>
            <span :class="{ warning: (packageInfo.remainingSeconds ?? 0) < 7 * 24 * 3600 }">{{ packageInfo.remainingTimeText || '已到期' }}</span>
          </div>
          <RouterLink :to="{name: 'SellerPackage'}" class="package-link">
            <i class="fas fa-crown"></i> 升级套餐
          </RouterLink>
        </div>

        <div class="menu-section logout-section">
          <div class="menu-divider" v-if="!isSidebarCollapsed"></div>
          <a href="#" class="menu-item logout-item" @click.prevent="handleLogout">
            <i class="fas fa-sign-out-alt"></i>
            <span>退出登录</span>
          </a>
        </div>
      </div>
    </div>

    <div class="main-content" :class="{ expanded: isSidebarCollapsed && !isMobile }">
      <span class="top-package-badge" v-if="packageInfo">
        <i class="fas fa-gem"></i> {{ packageInfo.name }}
      </span>
      <div class="top-header">
        <div class="header-left">
          <button class="toggle-sidebar" @click="isMobile ? toggleMobileMenu() : toggleSidebar()">
            <i class="fas fa-bars"></i>
          </button>
        </div>

        <div class="header-right">
          <RouterLink to="/seller/messages" class="header-notify">
            <i class="fas fa-bell"></i>
            <span v-if="unreadCount > 0" class="notify-badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
          </RouterLink>
          <div class="user-info" @click="toggleUserDropdown">
            <img :src="userAvatar || defaultAvatar" class="user-avatar" alt="商家头像" />
            <div class="user-details">
              <div class="user-name">{{ userName || '商家用户' }}</div>
              <div class="user-role">商家用户</div>
            </div>
            <i class="fas fa-chevron-down" :class="{ rotated: showUserDropdown }"></i>

            <div v-show="showUserDropdown" class="user-dropdown" @click.stop>
              <div class="dropdown-header">
                <img :src="userAvatar || defaultAvatar" class="dropdown-avatar" alt="商家头像" />
                <div class="dropdown-info">
                  <div class="dropdown-name">{{ userName || '商家用户' }}</div>
                  <div class="dropdown-role">商家账号</div>
                </div>
              </div>
              <div class="dropdown-divider"></div>
              <a href="#" class="dropdown-item" @click.prevent="switchToUser">
                <i class="fas fa-exchange-alt"></i>
                <span>切换为用户</span>
              </a>
              <div class="dropdown-divider"></div>
              <a href="#" class="dropdown-item logout-item" @click.prevent="handleLogout">
                <i class="fas fa-sign-out-alt"></i>
                <span>退出登录</span>
              </a>
            </div>
          </div>
        </div>
      </div>

      <div class="breadcrumb">
        <i class="fas fa-home"></i>
        <span>商家中心</span>
        <i class="fas fa-chevron-right"></i>
        <span class="active">{{ currentPageTitle }}</span>
      </div>

      <div class="content">
        <RouterView v-slot="{ Component, route }">
          <component :is="Component" :key="route.fullPath" />
        </RouterView>
      </div>
    </div>

    <div
      v-if="isMobile && isMobileMenuOpen"
      class="mobile-overlay"
      @click="closeMobileMenu"
    ></div>
    </div>

    <div v-else class="loading-container starlight-loader">
      <div class="loader-ring">
        <i class="fas fa-sparkles brand-icon"></i>
      </div>
      <p class="loader-text">云杉购·星环加载中</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import defaultAvatar from '@/static/images/seller-avatar.jpg'
import { authAPI } from '@/api/authAPI'
import Message from '@/utils/message'
import { useAuthStore } from '@/stores/auth'
import { storeToRefs } from 'pinia'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const { isLoggedIn, role, status } = storeToRefs(authStore)

interface SellerProfile {
  storeName: string
  storeAvatar?: string
  avatar?: string
}

const isInitialLoading = ref(true)
const isSidebarCollapsed = ref(false)
const isMobileMenuOpen = ref(false)
const isMobile = ref(window.innerWidth <= 1024)
const userAvatar = ref('')
const userName = ref('')
const showUserDropdown = ref(false)
const packageInfo = ref<any>(null)
const unreadCount = ref(0)
const layoutTransitionName = ref('')

const pageTitleMap: Record<string, string> = {
  SellerDashboard: '控制台',
  SellerProducts: '商品管理',
  SellerAddProduct: '发布商品',
  SellerCoupons: '优惠券管理',
  SellerOrders: '订单管理',
  SellerProductEdit: '编辑商品',
  SellerAnalytics: '数据分析',
  SellerProfile: '商家信息',
  SellerMessages: '消息中心',
  SellerPackage: '套餐购买',
  SellerPackageRecords: '套餐记录'
}

const currentPageTitle = computed(() => {
  const path = route.name as string
  return pageTitleMap[path] || '商家中心'
})

const toggleSidebar = () => {
  isSidebarCollapsed.value = !isSidebarCollapsed.value
  localStorage.setItem('sidebarCollapsed', String(isSidebarCollapsed.value))
}

const toggleMobileMenu = () => {
  isMobileMenuOpen.value = !isMobileMenuOpen.value
}

const closeMobileMenu = () => {
  isMobileMenuOpen.value = false
}

const handleResize = () => {
  isMobile.value = window.innerWidth <= 1024
  if (!isMobile.value) {
    isMobileMenuOpen.value = false
  }
}

const toggleUserDropdown = () => {
  showUserDropdown.value = !showUserDropdown.value
}

const handleClickOutside = (event: MouseEvent) => {
  const userInfo = document.querySelector('.user-info')
  if (userInfo && !userInfo.contains(event.target as Node)) {
    showUserDropdown.value = false
  }
}

const loadUserInfo = async () => {
  try {
    const response = await authAPI.getSellerProfile()

    if (response.success && response.data?.profile) {
      const data = response.data.profile as SellerProfile
      userName.value = data.storeName
      userAvatar.value = data.storeAvatar || data.avatar || ''
    }
  } catch (error: any) {
    Message.error('加载用户信息失败')

    const statusCode = error.response?.status
    if (statusCode === 401 || statusCode === 403) {
      throw error
    }
  }
}

const loadPackageInfo = async () => {
  try {
    const response = await authAPI.getCurrentPackage()
    if (response.success && response.data?.hasPackage) {
      packageInfo.value = {
        name: response.data.currentPackage?.name || '免费版',
        daysRemaining: response.data.currentPackage?.daysRemaining || 0,
        remainingSeconds: response.data.currentPackage?.remainingSeconds || 0,
        remainingTimeText: response.data.currentPackage?.remainingTimeText || '已到期',
        remainingProducts: response.data.remainingProducts || 0,
      }
    }
  } catch {}
}

const loadUnreadCount = async () => {
  const res = await authAPI.getSellerUnreadCount()
  if (res.success) unreadCount.value = res.data || 0
}

const handleNewNotification = () => {
  loadUnreadCount()  // 直接请求数据库真实未读数
}

const handleNotificationRead = (event: Event) => {
  const data = (event as CustomEvent).detail
  unreadCount.value = Math.max(0, unreadCount.value - (data?.count || 1))
}

const handlePackageRenew = (event: Event) => {
  const data = (event as CustomEvent).detail
  if (data?.type === 'renew') {
    setTimeout(() => {
      window.location.reload()
    }, 5000)
  }
}

const handleLogout = async () => {
  try {
    await Message.confirm('确定要退出登录吗？', '退出确认')

    await authAPI.logout()
    authStore.clear()

    localStorage.removeItem('sidebarCollapsed')

    Message.success('退出成功')
    setTimeout(() => router.push({name: 'Login'}), 1500)
  } catch {
    // 用户取消退出，静默处理
  }
}

const switchToUser = async () => {
  showUserDropdown.value = false
  const success = await authStore.switchRole('ROLE_USER')
  if (success) {
    router.push({ name: 'UserDashboard' })
  } else {
    Message.error('切换角色失败')
  }
}

watch(() => route.path, (newPath, oldPath) => {
  if (isMobile.value) {
    isMobileMenuOpen.value = false
  }

  // 只在同父路由下切换才加动画
  if (oldPath) {
    const newParent = newPath.split('/').slice(0, 3).join('/')
    const oldParent = oldPath.split('/').slice(0, 3).join('/')

    if (newParent === oldParent && newPath !== oldPath) {
      layoutTransitionName.value = 'slide-child'
      setTimeout(() => {
        layoutTransitionName.value = ''
      }, 300)
    }
  }
})

onMounted(() => {
  if (authStore.role !== 'ROLE_SELLER' && authStore.role !== 'ROLE_ADMIN') {
    router.replace({ name: 'UserDashboard' })
    return
  }

  const savedState = localStorage.getItem('sidebarCollapsed')
  if (savedState !== null) {
    isSidebarCollapsed.value = savedState === 'true'
  }

  Promise.all([loadUserInfo(), loadPackageInfo(), loadUnreadCount()]).finally(() => {
    setTimeout(() => {
      isInitialLoading.value = false
    }, 300)
  })

  document.addEventListener('click', handleClickOutside)
  window.addEventListener('resize', handleResize)
  window.addEventListener('new-notification', handleNewNotification)
  window.addEventListener('notification-read', handleNotificationRead)
  window.addEventListener('refresh-unread-count', loadUnreadCount)
  window.addEventListener('package-notification', handlePackageRenew)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
  window.removeEventListener('resize', handleResize)
  window.removeEventListener('new-notification', handleNewNotification)
  window.removeEventListener('notification-read', handleNotificationRead)
  window.removeEventListener('refresh-unread-count', loadUnreadCount)
  window.removeEventListener('package-notification', handlePackageRenew)
})
</script>

<style scoped>
@import url('@/static/css/seller/商家布局页.css');
@import url('https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css');
</style>
