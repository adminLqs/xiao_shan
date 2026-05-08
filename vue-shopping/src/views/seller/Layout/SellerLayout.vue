<template>
  <!-- 商家中心主容器 - 仅在登录状态下显示 -->
  <div class="dashboard-container" v-if="isLoggedIn">
    <!-- ==================== 侧边栏区域 ==================== -->
    <!-- 侧边栏组件，根据折叠状态和手机端状态动态切换样式 -->
    <div class="sidebar" :class="{ collapsed: isSidebarCollapsed && !isMobile, 'mobile-open': isMobileMenuOpen }">
      <!-- 侧边栏头部区域 -->
      <div class="sidebar-header">
        <!-- 侧边栏标题 -->
        <h2>
          <!-- 店铺图标 -->
          <i class="fas fa-store"></i> 
          <!-- 标题文字，侧边栏折叠时隐藏 -->
          <span v-if="!isSidebarCollapsed">商家中心</span>
        </h2>
        <!-- 侧边栏折叠/展开按钮（桌面端使用） -->
        <button class="collapse-btn" @click="isMobile ? closeMobileMenu() : toggleSidebar()">
          <!-- 根据折叠状态显示不同的箭头图标 -->
          <i :class="isSidebarCollapsed ? 'fas fa-chevron-right' : 'fas fa-chevron-left'"></i>
        </button>
      </div>

      <!-- 侧边栏菜单区域 -->
      <div class="sidebar-menu">
        <!-- 第一组菜单：核心功能 -->
        <div class="menu-section">
          <!-- 菜单分组标题，折叠时隐藏 -->
          <div class="menu-title" v-if="!isSidebarCollapsed">核心功能</div>
          <!-- 控制台菜单项 -->
          <RouterLink
            :to="{name: 'SellerDashboard'}"
            class="menu-item"
            :class="{ active: $route.name === 'SellerDashboard' }"
          >
            <i class="fas fa-tachometer-alt"></i>
            <span>控制台</span>
          </RouterLink>
          <!-- 商品管理菜单项 -->
          <RouterLink
            :to="{name: 'SellerProducts'}"
            class="menu-item"
            :class="{ active: $route.name === 'SellerProducts' || $route.name === 'SellerProductEdit' }"
          >
            <i class="fas fa-box"></i>
            <span>商品管理</span>
          </RouterLink>
          <!-- 发布商品菜单项 -->
          <RouterLink
            :to="{name: 'SellerAddProduct'}"
            class="menu-item"
            :class="{ active: $route.name === 'SellerAddProduct' }" 
          >
            <i class="fas fa-plus-circle"></i>
            <span>发布商品</span>
          </RouterLink>
        </div>

        <!-- 第二组菜单：运营管理 -->
        <div class="menu-section">
          <!-- 菜单分组标题，折叠时隐藏 -->
          <div class="menu-title" v-if="!isSidebarCollapsed">运营管理</div>
          <!-- 订单管理菜单项 -->
          <RouterLink
            :to="{name: 'SellerOrders'}"
            class="menu-item"
            :class="{ active: $route.name === 'SellerOrders' }"
          >
            <i class="fas fa-shopping-cart"></i>
            <span>订单管理</span>
          </RouterLink>
          <!-- 数据分析菜单项 -->
          <RouterLink
            :to="{name: ''}"
            class="menu-item"
            :class="{ active: $route.name === '' }"
          >
            <i class="fas fa-chart-line"></i>
            <span>数据分析</span>
          </RouterLink>
        </div>

        <!-- 第三组菜单：店铺管理 -->
        <div class="menu-section">
          <!-- 菜单分组标题，折叠时隐藏 -->
          <div class="menu-title" v-if="!isSidebarCollapsed">店铺管理</div>
          <!-- 店铺信息菜单项 -->
          <RouterLink
            :to="{name:'SellerProfile'}"
            class="menu-item"
            :class="{ active: $route.name === 'SellerProfile' }"
          >
            <i class="fas fa-store-alt"></i>
            <span>店铺信息</span>
          </RouterLink>
        </div>

        <!-- 退出登录区域（固定在底部） -->
        <div class="menu-section logout-section">
          <!-- 菜单分割线，折叠时隐藏 -->
          <div class="menu-divider" v-if="!isSidebarCollapsed"></div>
          <!-- 退出登录按钮 -->
          <a href="#" class="menu-item logout-item" @click.prevent="handleLogout">
            <i class="fas fa-sign-out-alt"></i>
            <span>退出登录</span>
          </a>
        </div>
      </div>
    </div>

    <!-- ==================== 主内容区域 ==================== -->
    <!-- 主内容区，根据侧边栏折叠状态动态调整左边距 -->
    <div class="main-content" :class="{ expanded: isSidebarCollapsed && !isMobile }">
      <!-- 顶部导航栏 -->
      <div class="top-header">
        <!-- 顶部导航栏左侧区域 -->
        <div class="header-left">
          <!-- 菜单按钮：手机端打开侧边栏菜单，桌面端折叠侧边栏 -->
          <button class="toggle-sidebar" @click="isMobile ? toggleMobileMenu() : toggleSidebar()">
            <!-- 显示菜单图标 -->
            <i class="fas fa-bars"></i>
          </button>
        </div>

        <!-- 顶部导航栏右侧区域 -->
        <div class="header-right">
          <!-- 用户信息区域，点击切换下拉菜单 -->
          <div class="user-info" @click="toggleUserDropdown">
            <!-- 商家头像 -->
            <img 
              :src="userAvatar || SELLER_AVATAR" 
              alt="商家头像" 
              class="user-avatar"
            />
            <!-- 商家名称和角色信息 -->
            <div class="user-details">
              <!-- 商家名称 -->
              <div class="user-name">{{ userName || '商家用户' }}</div>
              <!-- 商家角色 -->
              <div class="user-role">商家用户</div>
            </div>
            <!-- 下拉箭头，根据下拉菜单显示状态旋转 -->
            <i class="fas fa-chevron-down" :class="{ rotated: showUserDropdown }"></i>

            <!-- 用户下拉菜单 -->
            <div v-show="showUserDropdown" class="user-dropdown" @click.stop>
              <!-- 下拉菜单头部 -->
              <div class="dropdown-header">
                <!-- 用户头像 -->
                <img :src="userAvatar || SELLER_AVATAR" alt="头像" class="dropdown-avatar">
                <!-- 用户信息区域 -->
                <div class="dropdown-info">
                  <!-- 用户名称 -->
                  <div class="dropdown-name">{{ userName || '商家用户' }}</div>
                  <!-- 用户角色 -->
                  <div class="dropdown-role">商家账号</div>
                </div>
              </div>
              <!-- 分割线 -->
              <div class="dropdown-divider"></div>
              <!-- 修改密码菜单项 -->
              <RouterLink :to="{name: ''}" class="dropdown-item">
                <i class="fas fa-key"></i>
                <span>修改密码</span>
              </RouterLink>
              <!-- 分割线 -->
              <div class="dropdown-divider"></div>
              <!-- 退出登录菜单项 -->
              <a href="#" class="dropdown-item logout-item" @click.prevent="handleLogout">
                <i class="fas fa-sign-out-alt"></i>
                <span>退出登录</span>
              </a>
            </div>
          </div>
        </div>
      </div>

      <!-- 面包屑导航区域 -->
      <div class="breadcrumb">
        <!-- 首页图标 -->
        <i class="fas fa-home"></i>
        <!-- 商家中心层级 -->
        <span>商家中心</span>
        <!-- 箭头分隔符 -->
        <i class="fas fa-chevron-right"></i>
        <!-- 当前页面标题 -->
        <span class="active">{{ currentPageTitle }}</span>
      </div>

      <!-- 动态内容区域，渲染子路由组件 -->
      <div class="content">
        <RouterView></RouterView>
      </div>
    </div>

    <!-- 手机端遮罩层：侧边栏打开时显示，点击关闭菜单 -->
    <div 
      v-if="isMobile && isMobileMenuOpen" 
      class="mobile-overlay"
      @click="closeMobileMenu"
    ></div>
  </div>
  
  <!-- 未登录状态：显示加载动画 -->
  <div v-else class="loading-container">
    <div class="loading-spinner"></div>
    <p>加载中...</p>
  </div>
</template>

<script setup lang="ts">
  import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
  import { useRoute, useRouter } from 'vue-router'
  import { authAPI } from '@/api/authAPI'
  import { showToast, showConfirmDialog } from 'vant'
  import 'vant/es/toast/style'
  import 'vant/es/dialog/style'
  import { useAuthStore } from '@/stores/auth'
  import { storeToRefs } from 'pinia'

  const route = useRoute()
  const router = useRouter()
  const authStore = useAuthStore()
  const { isLoggedIn, role, status } = storeToRefs(authStore)

  // ==================== 类型定义 ====================

  interface SellerProfile {
    storeName: string
    storeAvatar?: string
    avatar?: string
  }

  // ==================== 响应式数据 ====================

  const isSidebarCollapsed = ref(false)
  const isMobileMenuOpen = ref(false)
  const isMobile = ref(window.innerWidth <= 1024)
  const userAvatar = ref('')
  const userName = ref('')
  const showUserDropdown = ref(false)

  const SELLER_AVATAR = '/images/seller-avatar.jpg'

  // ==================== 页面标题映射 ====================

  const pageTitleMap: Record<string, string> = {
    SellerDashboard: '控制台',
    SellerProducts: '商品管理',
    SellerAddProduct: '发布商品',
    SellerOrders: '订单管理',
    SellerAnalytics: '数据分析',
    SellerProfile: '商家信息'
  }

  // ==================== 计算属性 ====================

  /**
   * 当前页面标题
   * @description 根据路由名称动态获取对应的页面标题
   */
  const currentPageTitle = computed(() => {
    const path = route.name as string
    return pageTitleMap[path] || '商家中心'
  })

  // ==================== 布局控制 ====================

  /**
   * 切换侧边栏折叠状态
   * @description 桌面端使用，状态保存到本地存储
   */
  const toggleSidebar = () => {
    isSidebarCollapsed.value = !isSidebarCollapsed.value
    localStorage.setItem('sidebarCollapsed', String(isSidebarCollapsed.value))
  }

  /**
   * 切换手机端菜单状态
   */
  const toggleMobileMenu = () => {
    isMobileMenuOpen.value = !isMobileMenuOpen.value
  }

  /**
   * 关闭手机端菜单
   */
  const closeMobileMenu = () => {
    isMobileMenuOpen.value = false
  }

  /**
   * 监听窗口大小变化
   * @description 切换桌面端/手机端模式
   */
  const handleResize = () => {
    isMobile.value = window.innerWidth <= 1024
    if (!isMobile.value) {
      isMobileMenuOpen.value = false
    }
  }

  /**
   * 切换用户下拉菜单显示状态
   */
  const toggleUserDropdown = () => {
    showUserDropdown.value = !showUserDropdown.value
  }

  /**
   * 点击外部关闭下拉菜单
   * @param {MouseEvent} event - 鼠标点击事件
   */
  const handleClickOutside = (event: MouseEvent) => {
    const userInfo = document.querySelector('.user-info')
    if (userInfo && !userInfo.contains(event.target as Node)) {
      showUserDropdown.value = false
    }
  }

  // ==================== 用户信息 ====================

  /**
   * 加载商家信息
   * @returns {Promise<void>}
   */
  const loadUserInfo = async () => {
    try {
      const response = await authAPI.getSellerProfile()

      if (response.success && response.data) {
        const data = response.data as SellerProfile
        userName.value = data.storeName
        userAvatar.value = data.storeAvatar || data.avatar || ''
      }
    } catch (error: any) {
      console.error('加载用户信息失败:', error)

      const statusCode = error.response?.status
      if (statusCode === 401 || statusCode === 403) {
        throw error
      }
    }
  }

  /**
   * 退出登录
   * @returns {Promise<void>}
   */
  const handleLogout = async () => {
    try {
      await showConfirmDialog({
        title: '退出确认',
        message: '确定要退出登录吗？',
        confirmButtonText: '确定',
        cancelButtonText: '取消'
      })

      const toast = showToast({
        message: '退出中...',
        type: 'loading',
        duration: 0,
        forbidClick: true
      })

      await authAPI.logout()
      authStore.clear()
      toast.close()

      localStorage.removeItem('sidebarCollapsed')

      showToast({ message: '退出成功', type: 'success', duration: 1500 })
      setTimeout(() => router.push('/'), 1500)
    } catch {
      // 用户取消退出，静默处理
    }
  }

  // ==================== 生命周期 ====================

  watch(() => route.path, () => {
    if (isMobile.value) {
      isMobileMenuOpen.value = false
    }
  })

  onMounted(async () => {
    // 验证商家权限，如果没有权限则自动跳转
    if (!authStore.validateSellerPermission()) return

     // 从本地存储恢复侧边栏折叠状态
    const savedState = localStorage.getItem('sidebarCollapsed')
    if (savedState !== null) {
      isSidebarCollapsed.value = savedState === 'true'
    }

    await loadUserInfo()

    // 添加点击页面其他区域关闭下拉菜单的监听器
    document.addEventListener('click', handleClickOutside)

    // 移除窗口大小变化监听器，防止内存泄漏
    window.addEventListener('resize', handleResize)
  })

  onUnmounted(() => {
    // 移除点击外部关闭下拉菜单的监听器，防止内存泄漏
    document.removeEventListener('click', handleClickOutside)

    // 移除窗口大小变化监听器，防止内存泄漏
    window.removeEventListener('resize', handleResize)
  })
</script>

<style scoped>
  /* 引入商家布局的独立样式文件 */
  @import url('@/static/css/seller/商家布局页.css');
  /* 引入 Font Awesome 6.4.0 图标库 */
  @import url('https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css');
</style>