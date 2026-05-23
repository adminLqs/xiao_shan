<template>
  <div class="setting-page">
    <div class="page-header">
      <i class="fas fa-arrow-left" @click="$router.back()"></i>
      <span class="page-title">设置</span>
      <span class="placeholder"></span>
    </div>

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
    await authStore.logout()
    Message.success('已退出登录')
    router.push({ name: 'Login' })
  } catch {}
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

onMounted(async () => {
  if (!authStore.validateUserPermission()) return
  await loadUserProfile()
})
</script>

<style scoped>
  @import url('@/static/css/user/设置页.css');
</style>
