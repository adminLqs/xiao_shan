<template>
  <div class="mobile-layout">
    <!-- 主内容区 -->
    <main class="main-content">
      <router-view v-slot="{ Component }">
        <keep-alive :include="['UserDashboard']">
          <component :is="Component" />
        </keep-alive>
      </router-view>
    </main>

    <!-- 导航栏 -->
    <div class="magic-tab-bar">
      <!-- 首页 -->
      <RouterLink :to="{name: 'UserDashboard'}" class="magic-tab-item" active-class="magic-active">
        <div class="magic-icon-wrap">
          <i class="fas fa-compass"></i>
        </div>
        <span class="magic-label">探索</span>
      </RouterLink>

      <!-- 分类浏览 -->
      <RouterLink :to="{name: 'Categories'}" class="magic-tab-item magic-center-left" active-class="magic-active">
        <div class="magic-icon-wrap magic-elevated">
          <i class="fas fa-th-large"></i>
        </div>
        <span class="magic-label">分类</span>
      </RouterLink>

      <!-- 购物车（凸起C位） -->
      <RouterLink :to="{name: 'Cart'}" class="magic-tab-item magic-center" active-class="magic-active">
        <div class="magic-icon-wrap magic-hero">
          <i class="fas fa-shopping-bag"></i>
          <span v-if="cartCount > 0" class="magic-badge">{{ cartCount > 99 ? '99+' : cartCount }}</span>
        </div>
        <span class="magic-label">购物袋</span>
      </RouterLink>

      <!-- 消息 -->
      <RouterLink :to="{name: 'UserMessages'}" class="magic-tab-item" active-class="magic-active">
        <div class="magic-icon-wrap">
          <i class="fas fa-bolt"></i>
        </div>
        <span class="magic-label">消息</span>
        <span v-if="msgCount > 0" class="magic-dot"></span>
      </RouterLink>

      <!-- 我的 -->
      <RouterLink :to="{name: 'UserCenter'}" class="magic-tab-item" active-class="magic-active">
        <div class="magic-icon-wrap">
          <i class="fas fa-user-astronaut"></i>
        </div>
        <span class="magic-label">我的</span>
      </RouterLink>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { RouterLink, RouterView } from 'vue-router'
import { authAPI } from '@/api/authAPI'
import Message from '@/utils/message'

const msgCount = ref<number>(0)
const cartCount = ref(0)

const loadMessageCount = async () => {
  try {
    // const response = await authAPI.getUnreadMessageCount()
    // if (response.success) {
    //   msgCount.value = response.data || 0
    // }
  } catch (error) {
    Message.error('加载消息数量失败')
    msgCount.value = 0
  }
}

// 获取购物车数量
const loadCartCount = async () => {
  try {
    const res = await authAPI.getCartCount()
    if (res.success) cartCount.value = res.data || 0
  } catch {}
}

onMounted(() => {
  loadMessageCount()
  loadCartCount()
})
</script>

<style scoped>
@import url('@/static/css/user/移动端布局');
</style>
