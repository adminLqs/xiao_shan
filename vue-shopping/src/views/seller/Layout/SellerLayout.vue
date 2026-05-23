<template>
  <div class="dashboard-container" v-if="isLoggedIn">
    <div class="sidebar" :class="{ collapsed: isSidebarCollapsed && !isMobile, 'mobile-open': isMobileMenuOpen }">
      <div class="sidebar-header">
        <h2>
          <i class="fas fa-store"></i>
          <span v-if="!isSidebarCollapsed">商家中心</span>
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
      <div class="top-header">
        <div class="header-left">
          <button class="toggle-sidebar" @click="isMobile ? toggleMobileMenu() : toggleSidebar()">
            <i class="fas fa-bars"></i>
          </button>
        </div>

        <div class="header-right">
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
              <RouterLink :to="{name: ''}" class="dropdown-item">
                <i class="fas fa-key"></i>
                <span>修改密码</span>
              </RouterLink>
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
        <RouterView></RouterView>
      </div>
    </div>

    <div
      v-if="isMobile && isMobileMenuOpen"
      class="mobile-overlay"
      @click="closeMobileMenu"
    ></div>
  </div>

  <div v-else class="loading-container">
    <div class="loading-spinner"></div>
    <p>加载中...</p>
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

const isSidebarCollapsed = ref(false)
const isMobileMenuOpen = ref(false)
const isMobile = ref(window.innerWidth <= 1024)
const userAvatar = ref('')
const userName = ref('')
const showUserDropdown = ref(false)

const pageTitleMap: Record<string, string> = {
  SellerDashboard: '控制台',
  SellerProducts: '商品管理',
  SellerAddProduct: '发布商品',
  SellerOrders: '订单管理',
  SellerAnalytics: '数据分析',
  SellerProfile: '商家信息'
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

const handleLogout = async () => {
  try {
    await Message.confirm('确定要退出登录吗？', '退出确认')

    Message.info('退出中...')

    await authAPI.logout()
    authStore.clear()

    localStorage.removeItem('sidebarCollapsed')

    Message.success('退出成功')
    setTimeout(() => router.push({name: 'SellerDashboard'}), 1500)
  } catch {
    // 用户取消退出，静默处理
  }
}

watch(() => route.path, () => {
  if (isMobile.value) {
    isMobileMenuOpen.value = false
  }
})

onMounted(async () => {
  if (!authStore.validateSellerPermission()) return

  const savedState = localStorage.getItem('sidebarCollapsed')
  if (savedState !== null) {
    isSidebarCollapsed.value = savedState === 'true'
  }

  await loadUserInfo()

  document.addEventListener('click', handleClickOutside)
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
@import url('@/static/css/seller/商家布局页.css');
@import url('https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css');
</style>
