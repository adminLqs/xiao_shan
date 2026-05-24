<template>
  <div class="app">
    <router-view v-slot="{ Component }">
      <transition name="page-slide" mode="out-in">
        <component :is="Component" />
      </transition>
    </router-view>
  </div>
</template>

<script setup lang="ts">
import { watch } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useWebSocket } from '@/utils/websocket'

const authStore = useAuthStore()
const ws = useWebSocket()

watch(() => authStore.isLoggedIn, (loggedIn) => {
  if (!loggedIn || !authStore.userId) {
    ws.disconnect()
    return
  }

  if (authStore.isAdmin) {
    ws.connectAdmin()  
  } else if (authStore.isSeller) {
    ws.connectSeller(authStore.userId)   
  } else {
    ws.connectUser(authStore.userId)    
  }
}, { immediate: true })
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
  -webkit-tap-highlight-color: transparent;
  min-height: 100vh;
}

.app {
  width: 100%;
  min-height: 100vh;
}

.page-slide-enter-active,
.page-slide-leave-active {
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.page-slide-enter-from {
  opacity: 0;
  transform: translateX(20px);
}

.page-slide-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}
</style>