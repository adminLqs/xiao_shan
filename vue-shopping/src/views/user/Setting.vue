<template>
  <div class="setting-container">
    <!-- 退出时的星环加载器（优先级最高） -->
    <div v-if="isExiting" class="starlight-loader">
      <div class="loader-ring">
        <i class="fas fa-sparkles brand-icon"></i>
      </div>
      <p class="loader-text">云杉购·期待下次再见</p>
    </div>

    <!-- 正常内容 -->
    <div v-else class="setting-page page-container">
    <div class="page-navbar">
      <button class="page-nav-back" @click="router.back()">
        <i class="fas fa-chevron-left"></i>
      </button>
      <div class="page-nav-title">
        <i class="fas fa-cog"></i>
        <span>账户设置</span>
      </div>
      <div class="page-nav-right"></div>
    </div>

    <!-- 骨架屏 -->
    <div v-if="loading" class="skeleton-form">
      <div class="user-info-row skeleton-card">
        <div class="skeleton" style="width: 60px; height: 60px; border-radius: 50%;"></div>
        <div class="skeleton-line medium" style="width: 100px; margin-left: 16px;"></div>
      </div>
      <div class="setting-list skeleton-card">
        <div v-for="n in 5" :key="n" class="setting-item">
          <div class="skeleton" style="width: 24px; height: 24px; border-radius: 50%;"></div>
          <div class="skeleton-line short" style="width: 80px; margin-left: 12px;"></div>
          <div class="skeleton" style="width: 16px; height: 16px;"></div>
        </div>
      </div>
    </div>

    <div v-else>
      <div class="user-info-row">
        <img :src="userProfile.avatar || defaultAvatar" class="user-avatar" />
        <span class="user-name">{{ userProfile.nickname || '用户' }}</span>
      </div>

      <div class="setting-list">
        <div class="setting-item" @click="$router.push({ name: 'UserProfile' })">
          <i class="fas fa-user"></i>
          <span>个人资料</span>
          <i class="fas fa-chevron-right arrow"></i>
        </div>
        <div class="setting-item" @click="showTip">
          <i class="fas fa-shield-alt"></i>
          <span>账号安全</span>
          <i class="fas fa-chevron-right arrow"></i>
        </div>
        <div class="setting-item" @click="showTip">
          <i class="fas fa-bell"></i>
          <span>消息通知</span>
          <i class="fas fa-chevron-right arrow"></i>
        </div>
        <div class="setting-item" @click="showTip">
          <i class="fas fa-lock"></i>
          <span>隐私设置</span>
          <i class="fas fa-chevron-right arrow"></i>
        </div>
        <div class="setting-item" @click="showTip">
          <i class="fas fa-info-circle"></i>
          <span>关于我们</span>
          <i class="fas fa-chevron-right arrow"></i>
        </div>
      </div>

      <button class="logout-btn" @click="handleLogout">退出登录</button>
    </div>
  </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { authAPI } from '@/api/authAPI'
import Message from '@/utils/message'
import { useAuthStore } from '@/stores/auth'
import defaultAvatar from '@/static/images/user-avatar.jpg'

const authStore = useAuthStore()
const router = useRouter()

const loading = ref(true)
const isExiting = ref(false)
const userProfile = ref<any>({
  avatar: '',
  nickname: ''
})

const showTip = () => {
  Message.info('功能正在扩展中...')
}

const handleLogout = async () => {
  try {
    await Message.confirm('确定要退出登录吗？', '退出确认')
    await authAPI.logout()

    // 显示星环加载器
    isExiting.value = true

    // 延迟 1.5 秒展示品牌动画
    setTimeout(() => {
      router.push({ name: 'Login' })
    }, 1500)
  } catch {
    // 用户取消
  }
}

const loadUserProfile = async () => {
  try {
    const response = await authAPI.getUserProfile()
    if (response.success && response.data?.profile) {
      userProfile.value = {
        avatar: response.data.profile.avatar || '',
        nickname: response.data.profile.nickname || ''
      }
    }
  } catch {
    // 静默处理
  }
}

onMounted(() => {
  if (!authStore.validateUserPermission()) return
  loadUserProfile().finally(() => {
    loading.value = false
  })
})
</script>

<style scoped>
@import url('@/static/css/user/账户设置.css');
</style>