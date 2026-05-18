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
  import { useWebSocketStore } from '@/stores/websocket'
  import { onMounted, onUnmounted, ref } from 'vue'
  import { useUserStore } from '@/stores/auth'
  import Message from '@/utils/message'
  import router from '@/router'

  // ==================== 响应式数据 ====================
  const webSocketStore = useWebSocketStore()
  const userStore = useUserStore()
  const route = useRoute()

  // 当前用户ID
  const currentUserId = ref<number | null>(null)

  const isActive = (routeName: string): boolean => {
    return route.name === routeName
  }

  // ==================== 初始化 ====================
  const init = async (): Promise<void> => {
    const userId = userStore.userId
    if (!userId) {
      Message.error('请先登录')
      router.back()
      return
    }

    currentUserId.value = Number(userId)
    if (isNaN(currentUserId.value) || currentUserId.value <= 0) {
      Message.error('用户信息错误')
      router.back()
      return
    }

  }


  // ==================== 生命周期 ====================
  onMounted(() => {
    init()
    webSocketStore.connectUser(currentUserId.value!)
  })

  onUnmounted(() => {
    webSocketStore.disconnect()
  })
</script>

<style scoped>
  @import url("@/static/css/user/用户布局页.css")
</style>