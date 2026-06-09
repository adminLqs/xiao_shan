<template>
  <div class="app">
    <div v-if="showSplash" class="splash-screen">
      <button v-if="showSkip" class="splash-skip" @click="skipSplash">{{ countdown }} 跳过</button>

      <div class="splash-content">
        <div class="splash-logo-wrapper">
          <img src="@/static/images/云杉购图袋.png" class="splash-logo" />
        </div>

        <h1 class="splash-brand">云杉购</h1>
        <p class="splash-slogan">精品生活 · 从这里开始</p>

        <div class="splash-features">
          <span>品质保障</span>
          <span class="divider">|</span>
          <span>极速物流</span>
          <span class="divider">|</span>
          <span>正品保证</span>
        </div>

        <div class="splash-dots">
          <span class="dot active"></span>
          <span class="dot"></span>
          <span class="dot"></span>
        </div>

        <button class="splash-btn" @click="skipSplash">立即体验</button>
      </div>
    </div>

    <router-view v-else />
  </div>
</template>

<script setup lang="ts">
import { watch, onUnmounted, onMounted, ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useWebSocket } from '@/utils/websocket'

const authStore = useAuthStore()
const ws = useWebSocket()

const showSplash = ref(true)
const showSkip = ref(false)
const countdown = ref(3)

let connectTimer: ReturnType<typeof setTimeout> | null = null
let splashTimer: ReturnType<typeof setTimeout> | null = null
let skipTimer: ReturnType<typeof setTimeout> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

const initWebSocket = () => {
  if (connectTimer) {
    clearTimeout(connectTimer)
    connectTimer = null
  }

  ws.disconnect()

  connectTimer = setTimeout(() => {
    if (!authStore.isLoggedIn || !authStore.userId) {
      return
    }

    if (authStore.activeRole === 'ROLE_ADMIN') {
      ws.connectAdmin()
    } else if (authStore.activeRole === 'ROLE_SELLER') {
      ws.connectSeller(authStore.userId)
    } else if (authStore.activeRole === 'ROLE_USER') {
      ws.connectUser(authStore.userId)
    }
  }, 300)
}

watch(() => authStore.isLoggedIn, (val) => {
  if (!val) {
    ws.disconnect()
  }
})

watch(() => authStore.userId, (newId, oldId) => {
  if (newId && newId !== oldId) {
    initWebSocket()
  }
})

watch(() => authStore.activeRole, () => {
  if (authStore.userId) {
    initWebSocket()
  }
})

onMounted(async () => {
  skipTimer = setTimeout(() => {
    showSkip.value = true
    countdownTimer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(countdownTimer!)
        showSplash.value = false
      }
    }, 1000)
  }, 1000)

  await authStore.checkAndUpdate()

  setTimeout(() => {
    if (authStore.isLoggedIn && authStore.userId) {
      initWebSocket()
    }
  }, 100)
})

onUnmounted(() => {
  if (connectTimer) {
    clearTimeout(connectTimer)
  }
  if (splashTimer) {
    clearTimeout(splashTimer)
  }
  if (skipTimer) {
    clearTimeout(skipTimer)
  }
  if (countdownTimer) {
    clearInterval(countdownTimer)
  }
  ws.disconnect()
})

const skipSplash = () => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
  }
  showSplash.value = false
}
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

.business-message {
  border-radius: 6px;
  font-size: 14px;
  padding: 12px 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  border: 1px solid #e8ecf0;
}

.business-message .el-message__icon {
  font-size: 16px;
}

.splash-screen {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(180deg, #4a6491 0%, #2d4373 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}

.splash-skip {
  position: absolute;
  top: 16px;
  right: 16px;
  padding: 6px 16px;
  background: rgba(255,255,255,0.2);
  border: none;
  border-radius: 16px;
  color: rgba(255,255,255,0.8);
  font-size: 14px;
  cursor: pointer;
  z-index: 10;
  transition: all 0.2s;
}

.splash-skip:active {
  background: rgba(255,255,255,0.3);
  transform: scale(0.96);
}

.splash-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 24px;
  padding: 20px;
}

.splash-logo-wrapper {
  width: 120px;
  height: 120px;
  background: rgba(255,255,255,0.15);
  border-radius: 24px;
  padding: 20px;
  backdrop-filter: blur(10px);
  box-shadow: 0 8px 32px rgba(0,0,0,0.2);
}

.splash-logo {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.splash-brand {
  font-size: 36px;
  font-weight: 700;
  color: white;
  letter-spacing: 4px;
  margin: 0;
}

.splash-slogan {
  font-size: 14px;
  color: rgba(255,255,255,0.7);
  margin: 0;
}

.splash-features {
  display: flex;
  gap: 12px;
  color: rgba(255,255,255,0.6);
  font-size: 12px;
  margin-top: 8px;
}

.splash-features .divider {
  color: rgba(255,255,255,0.4);
}

.splash-dots {
  display: flex;
  justify-content: center;
  gap: 8px;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: rgba(255,255,255,0.3);
}

.dot.active {
  background: white;
  width: 24px;
  border-radius: 4px;
}

.splash-btn {
  padding: 14px 60px;
  background: white;
  color: #4a6491;
  border-radius: 30px;
  font-size: 16px;
  font-weight: 600;
  border: none;
  cursor: pointer;
  animation: breathe 2s ease-in-out infinite;
  box-shadow: 0 4px 16px rgba(0,0,0,0.2);
}

.splash-btn:active {
  transform: scale(0.96);
}

@keyframes breathe {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.03); }
}
</style>
