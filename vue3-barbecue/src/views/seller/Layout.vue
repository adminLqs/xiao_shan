<template>
  <div class="bbq-layout">
    <aside class="sidebar">
      <div class="merchant-card">
        <div class="avatar-wrapper">
          <img
            :src="merchantInfo.storeAvatar || DEFAULT_AVATAR"
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
          <p class="shop-address" v-if="merchantInfo.address">📍 {{ merchantInfo.address }}</p>
        </div>
      </div>

      <nav class="nav-menu">
        <ul>
          <li>
            <RouterLink :to="{name: 'SellerProducts'}" class="nav-link" active-class="active">
              <span class="icon">📦</span><span>商品管理</span>
            </RouterLink>
          </li>
          <li>
            <RouterLink :to="{name: 'SellerAddProduct'}" class="nav-link" active-class="active">
              <span class="icon">✨</span><span>商品发布</span>
            </RouterLink>
          </li>
          <li>
            <RouterLink :to="{name: 'SellerOrders'}" class="nav-link" active-class="active">
              <span class="icon">📋</span><span>订单管理</span>
            </RouterLink>
          </li>
          <li>
            <RouterLink :to="{name: 'SellerProfile'}" class="nav-link" active-class="active">
              <span class="icon">⚙️</span><span>商家信息</span>
            </RouterLink>
          </li>
          <li>
            <RouterLink :to="{name: 'SellerChat'}" class="nav-link" active-class="active">
              <span class="icon">💬</span><span>客服消息</span>
              <span v-if="webSocketStore.unreadCount.value > 0" class="unread-badge">
                {{ webSocketStore.unreadCount.value }}
              </span>
            </RouterLink>
          </li>
        </ul>
      </nav>

      <div class="sidebar-footer">
        <p class="theme-signature">🔥 烧烤江湖 🔥</p>
      </div>
    </aside>

    <main class="main-content">
      <RouterView />
    </main>
  </div>
</template>

<script setup lang="ts">
  import { reactive, onMounted, onUnmounted } from 'vue'
  import { useWebSocketStore } from '@/stores/websocket'
  import { authAPI } from '@/api/authAPI'

  const webSocketStore = useWebSocketStore()

  const merchantInfo = reactive({
    storeAvatar: '',
    storeName: '',
    slogan: '',
    isOpen: true,
    address: ''
  })

  const DEFAULT_AVATAR = '/images/seller-avatar.jpg'

  const loadMerchantInfo = async () => {
    try {
      const response = await authAPI.getSellerProfile()
 
      if (response.success) {
        const profile = response.data.profile
        merchantInfo.storeAvatar = profile.storeAvatar
        merchantInfo.storeName = profile.storeName
        merchantInfo.slogan = profile.slogan
        merchantInfo.isOpen = profile.isOpen
        merchantInfo.address = profile.address
      }
    } catch (error) {
      console.error('加载商家信息失败:', error)
    }
  }

  onMounted(() => {
    loadMerchantInfo()
    webSocketStore.connectSeller()
  })

  onUnmounted(() => {
    webSocketStore.disconnect()
  })
</script>

<style scoped>
  @import url('@/static/css/seller/商家布局页.css');
</style>