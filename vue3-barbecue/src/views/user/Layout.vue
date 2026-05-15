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
          >
            <span class="nav-icon">🏠</span>
            <span class="nav-text">首页</span>
          </RouterLink>
          
          <RouterLink 
            :to="{ name: 'UserOrder' }" 
            class="nav-item" 
            :class="{ active: isActive('UserOrder') }"
          >
            <span class="nav-icon">📦</span>
            <span class="nav-text">订单</span>
          </RouterLink>

          <!-- 客服入口 -->
          <RouterLink :to="{ name: 'CustomerService' }" class="nav-item">
            <span class="nav-icon">💬</span>
            <span class="nav-text">客服</span>
          </RouterLink>

          <RouterLink 
            :to="{ name: 'UserAccount' }" 
            class="nav-item" 
            :class="{ active: isActive('UserAccount') }"
          >
            <span class="nav-icon">👤</span>
            <span class="nav-text">账号</span>
          </RouterLink>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
  import { useRoute } from 'vue-router'

  const route = useRoute()

  const isActive = (routeName: string): boolean => {
    return route.name === routeName
  }


</script>

<style scoped>
    @import url("@/static/css/user/用户布局页.css")
</style>