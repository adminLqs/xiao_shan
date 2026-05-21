<template>
  <div class="user-layout-container">
    <!-- 顶部用户卡片 -->
    <div class="user-header-card">
      <div class="avatar-container" @click="triggerAvatarInput">
        <img :src="userProfile.avatar || defaultAvatar" class="user-avatar" alt="用户头像" />
        <div class="avatar-overlay">
          <i class="fas fa-camera"></i>
          <span>更换头像</span>
        </div>
        <input
          ref="avatarInput"
          type="file"
          accept="image/*"
          style="display: none"
          @change="handleAvatarChange"
        >
      </div>
      <div class="user-info">
        <h3 class="username">{{ userProfile.nickname || '用户' }}</h3>
        <p class="user-welcome">欢迎来到个人中心</p>
      </div>
    </div>

    <!-- 网格菜单入口 -->
    <div class="menu-grid">
      <RouterLink :to="{name:'UserOrders'}" class="menu-grid-item">
        <div class="menu-icon-wrapper">
          <i class="fas fa-clipboard-list"></i>
          <span v-if="orderCount > 0" class="menu-badge">{{ orderCount }}</span>
        </div>
        <span class="menu-label">我的订单</span>
      </RouterLink>

      <RouterLink :to="{name:'UserFavorites'}" class="menu-grid-item">
        <div class="menu-icon-wrapper">
          <i class="fas fa-heart"></i>
          <span v-if="favoriteCount > 0" class="menu-badge">{{ favoriteCount }}</span>
        </div>
        <span class="menu-label">我的收藏</span>
      </RouterLink>

      <RouterLink :to="{name:'UserAddresses'}" class="menu-grid-item">
        <div class="menu-icon-wrapper">
          <i class="fas fa-map-marker-alt"></i>
        </div>
        <span class="menu-label">收货地址</span>
      </RouterLink>

      <RouterLink :to="{name:'UserCoupons'}" class="menu-grid-item">
        <div class="menu-icon-wrapper">
          <i class="fas fa-tag"></i>
          <span v-if="couponCount > 0" class="menu-badge">{{ couponCount }}</span>
        </div>
        <span class="menu-label">优惠券</span>
      </RouterLink>

      <RouterLink :to="{ name: 'UserProfile' }" class="menu-grid-item">
        <div class="menu-icon-wrapper">
          <i class="fas fa-user-edit"></i>
        </div>
        <span class="menu-label">个人资料</span>
      </RouterLink>

      <RouterLink :to="{name:'UserSetting'}" class="menu-grid-item">
        <div class="menu-icon-wrapper">
          <i class="fas fa-cog"></i>
        </div>
        <span class="menu-label">账户设置</span>
      </RouterLink>
    </div>

    <!-- 功能按钮区 -->
    <div class="action-buttons">
      <RouterLink :to="{ name:'UserDashboard' }" class="action-btn return-btn">
        <i class="fas fa-store"></i>
        <span>返回商城</span>
      </RouterLink>
      <button class="action-btn logout-btn" @click="handleLogout">
        <i class="fas fa-sign-out-alt"></i>
        <span>退出登录</span>
      </button>
    </div>

    <!-- 主内容区 - RouterView 渲染当前激活的子页面 -->
    <main class="user-main-content">
      <RouterView />
    </main>

    <!-- 底部TabBar -->
    <div class="tab-bar">
      <RouterLink :to="{name: 'UserDashboard'}" class="tab-item">
        <i class="fas fa-home"></i>
        <span>首页</span>
      </RouterLink>
      <RouterLink to="/category" class="tab-item">
        <i class="fas fa-th-large"></i>
        <span>分类</span>
      </RouterLink>
      <RouterLink :to="{name: 'Cart'}" class="tab-item">
        <i class="fas fa-shopping-cart"></i>
        <span>购物车</span>
      </RouterLink>
      <RouterLink :to="{name: 'UserCenter'}" class="tab-item active">
        <i class="fas fa-user"></i>
        <span>我的</span>
      </RouterLink>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import defaultAvatar from '@/static/images/user-avatar.jpg'
import { authAPI } from '@/api/authAPI'
import Message from '@/utils/message'
import { useAuthStore } from '@/stores/auth'
import { storeToRefs } from 'pinia'

const authStore = useAuthStore()
const { isLoggedIn } = storeToRefs(authStore)

const router = useRouter()

const userProfile = ref({
    avatar: '',
    nickname: ''
})

const orderCount = ref<number>(0)
const favoriteCount = ref<number>(0)
const couponCount = ref<number>(0)

const avatarInput = ref<HTMLInputElement | null>(null)

const getUserProfile = async () => {
  try {
      const response = await authAPI.getUserProfile()
      userProfile.value = response.data?.profile || {}
  } catch(error) {
      Message.error('获取用户信息失败')
      router.push({name: 'UserDashboard'})
  }
}

const triggerAvatarInput = () => {
  avatarInput.value?.click()
}

const handleAvatarChange = (e: Event) => {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  if (!validateFile(file)) {
      input.value = ''
      return
  }
  uploadAvatar(file)
  input.value = ''
}

const validateFile = (file: File) => {
  const maxSize = 5 * 1024 * 1024
  const allowedTypes = ['image/jpeg', 'image/png', 'image/gif']

  if (file.size > maxSize) {
      Message.error('文件大小不能超过5MB')
      return false
  }
  if (!allowedTypes.includes(file.type)) {
      Message.error('请选择JPEG、PNG或GIF格式的图片')
      return false
  }
    return true
}

const uploadAvatar = async (file: File) => {
  const previewUrl = URL.createObjectURL(file)
  userProfile.value.avatar = previewUrl

  const loading = Message.loading({ message: '上传中...' })

  const formData = new FormData()
  formData.append('avatar', file)

  try {
      const response = await authAPI.updateAvatar(formData)
      if (response.success) {
      if (response.data?.avatar) {
          userProfile.value.avatar = response.data.avatar
      }
      loading.close()
      Message.success('头像更新成功')
      } else {
          loading.close()
          Message.error(response.message || '上传失败')
      if (userProfile.value.avatar === previewUrl) {
          userProfile.value.avatar = ''
      }
  }} catch (error: any) {
      loading.close()
      Message.error(error.message || '上传失败，请重试')
      if (userProfile.value.avatar === previewUrl) {
          userProfile.value.avatar = ''
      }
  } finally {
      URL.revokeObjectURL(previewUrl)
  }
}

const handleLogout = async () => {
  try {
    await Message.confirm('确定要退出登录吗？', '退出确认')

    const loading = Message.loading({ message: '退出中...' })

    try {
      const response = await authAPI.logout()
      loading.close()
      if (response.success) {
        authStore.clear()
        Message.success('退出成功')
        setTimeout(() => {
          router.push({ name: 'UserDashboard' })
        }, 1500)
      } else {
        Message.error('退出失败，请重试')
      }
    } catch (error) {
      loading.close()
      Message.error('退出失败，请重试')
    }
  } catch (error) {
  }
}

onMounted(() => {
  if (!authStore.validateUserPermission()) return;

  if (isLoggedIn.value){
    getUserProfile()
  }
})
</script>

<style scoped>
    @import url('@/static/css/user/个人中心布局页.css');
</style>
