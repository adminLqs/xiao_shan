<template>
  <div class="bbq-layout">
    <!-- 左侧导航栏 -->
    <aside class="sidebar">
      <div class="merchant-card">
        <div class="avatar-wrapper">
          <img
            :src="merchantInfo.storeAvatar || '/images/seller-avatar.jpg'"
            alt="商家头像"
            class="avatar"
          />
        </div>
        <div class="merchant-details">
          <h2 class="shop-name">{{ merchantInfo.storeName || '杉杉烤肉坊' }}</h2>
          <p class="shop-slogan">{{ merchantInfo.slogan || '炭火匠心 · 深夜烧烤' }}</p>
          <div class="shop-status">
            <span class="status-dot" :class="merchantInfo.isOpen ? 'open' : 'closed'"></span>
            <span class="status-text">{{ merchantInfo.isOpen ? '营业中' : '休息中' }}</span>
          </div>
          <p class="shop-address" v-if="merchantInfo.address">
            📍 {{ merchantInfo.address }}
          </p>
        </div>
      </div>

      <nav class="nav-menu">
        <ul>
          <li>
            <RouterLink :to="{name: 'SellerProducts'}" class="nav-link" active-class="active">
              <span class="icon">📦</span>
              <span>商品管理</span>
            </RouterLink>
          </li>
          <li>
            <RouterLink :to="{name: 'SellerAddProduct'}" class="nav-link" active-class="active">
              <span class="icon">✨</span>
              <span>商品发布</span>
            </RouterLink>
          </li>
          <li>
            <RouterLink :to="{name: 'SellerOrders'}" class="nav-link" active-class="active">
              <span class="icon">📋</span>
              <span>订单管理</span>
            </RouterLink>
          </li>
          <li>
            <RouterLink :to="{name: 'SellerProfile'}" class="nav-link" active-class="active">
              <span class="icon">⚙️</span>
              <span>商家信息</span>
            </RouterLink>
          </li>
        </ul>
      </nav>

      <div class="sidebar-footer">
        <p class="theme-signature">🔥 烧烤江湖 🔥</p>
      </div>
    </aside>

    <!-- 右侧主内容区 -->
    <main class="main-content">
      <!-- ✅ 调试信息 -->
      <div class="debug-info" v-if="!$route.name">
        ⚠️ 当前路由: {{ $route.path }}<br>
        路由名称: {{ $route.name }}<br>
        请检查路由配置
      </div>
      <RouterView />
    </main>
  </div>
</template>

<script setup lang="ts">
  import { ref, reactive, onMounted } from 'vue'
  import { useRoute } from 'vue-router'
  import { authAPI } from '@/api/auth'

  const route = useRoute()

  const merchantInfo = reactive({
    storeAvatar: '',
    storeName: '',
    slogan: '',
    isOpen: true,
    address: ''
  })

  const loadMerchantInfo = async () => {
    try {
      const response = await authAPI.getSellerProfile()
      const data = response.data || response
      if (data.success && data.profile) {
        merchantInfo.storeAvatar = data.profile.storeAvatar
        merchantInfo.storeName = data.profile.storeName
        merchantInfo.slogan = data.profile.slogan
        merchantInfo.isOpen = data.profile.isOpen
        merchantInfo.address = data.profile.address
      }
    } catch (error) {
      console.error('加载商家信息失败:', error)
    }
  }

  onMounted(() => {
    loadMerchantInfo()
  })
</script>

<style scoped>
    @import url('@/static/css/商家布局.css');
</style>