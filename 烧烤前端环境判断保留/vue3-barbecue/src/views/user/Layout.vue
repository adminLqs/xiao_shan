<template>
  <div class="app-layout">
    <!-- 主内容区域 - 不滚动，由子页面自己处理滚动 -->
    <div class="main-content">
        <RouterView />
    </div>

    <!-- 底部导航栏 -->
    <div class="bottom-nav-wrapper">
      <div class="bottom-nav-container">
        <div class="bottom-nav">
          <RouterLink 
            :to="{ name: 'UserDashboard' }" 
            class="nav-item" 
            :class="{ active: isActive('UserDashboard') }"
            @click="switchTab('home')"
          >
            <span class="nav-icon">🏠</span>
            <span class="nav-text">首页</span>
          </RouterLink>
          
          <RouterLink 
            :to="{ name: 'UserOrder' }" 
            class="nav-item" 
            :class="{ active: isActive('UserOrder') }"
            @click="switchTab('orders')"
          >
            <span class="nav-icon">📦</span>
            <span class="nav-text">订单</span>
          </RouterLink>
          
          <div 
            class="nav-item" 
            :class="{ active: currentTab === 'profile' }" 
            @click="switchTab('profile')"
          >
            <span class="nav-icon">👤</span>
            <span class="nav-text">我的</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const currentTab = ref<'home' | 'orders' | 'profile'>('home')

const isActive = (routeName: string): boolean => {
  return route.name === routeName
}

const switchTab = (tab: 'home' | 'orders' | 'profile') => {
  currentTab.value = tab
  if (tab === 'profile') {
    alert('我的页面开发中')
  }
}

watch(() => route.name, (newName) => {
  if (newName === 'UserDashboard') {
    currentTab.value = 'home'
  } else if (newName === 'UserOrder') {
    currentTab.value = 'orders'
  }
}, { immediate: true })
</script>

<style scoped>
    @import url("@/static/css/用户布局页.css")
</style>