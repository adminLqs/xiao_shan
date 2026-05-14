<template>
  <div class="user-layout-container">
    <!-- 用户侧边栏 - 固定左侧 -->
    <aside class="user-sidebar">
      <div class="user-profile-card">
        <!-- 头像 - 可点击上传 -->
        <div class="avatar-container" @click="triggerAvatarInput">
          <img 
            :src="userProfile.avatar || DEFAULT_AVATAR" 
            class="user-avatar"
            alt="用户头像"
          >
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
        <!-- 昵称 -->
        <h3 class="username">{{ userProfile.nickname || '用户' }}</h3>
      </div>

      <nav class="user-menu">
        <ul>
          <li class="menu-item" :class="{ active: $route.name === 'UserCenter' }">
            <RouterLink :to="{ name:'UserCenter' }">
              <i class="fas fa-home"></i>
              <span>个人中心</span>
            </RouterLink>
          </li>
          <li class="menu-item" :class="{ active: $route.name === 'UserProfile' }">
            <RouterLink :to="{ name: 'UserProfile' }">
              <i class="fas fa-user-edit"></i>
              <span>个人资料</span>
            </RouterLink>
          </li>
          <li class="menu-item" :class="{ active: $route.name === 'UserOrders' }">
            <RouterLink :to="{name: 'UserOrders'}">
              <i class="fas fa-clipboard-list"></i>
              <span>我的订单</span>
              <span class="badge" v-if="orderCount > 0">{{ orderCount }}</span>
            </RouterLink>
          </li>
          <li class="menu-item" :class="{ active: $route.name === 'UserFavorites' }">
            <RouterLink :to="{name: 'UserFavorites'}">
              <i class="fas fa-heart"></i>
              <span>我的收藏</span>
              <span class="badge" v-if="favoriteCount > 0">{{ favoriteCount }}</span>
            </RouterLink>
          </li>
          <li class="menu-item" :class="{ active: $route.name === 'UserAddresses' }">
            <RouterLink :to="{name: 'UserAddresses'}">
              <i class="fas fa-map-marker-alt"></i>
              <span>收货地址</span>
            </RouterLink>
          </li>
          <li class="menu-item" :class="{ active: $route.name === 'UserCoupons' }">
            <RouterLink :to="{name: 'UserCoupons'}">
              <i class="fas fa-tag"></i>
              <span>优惠券</span>
              <span class="badge" v-if="couponCount > 0">{{ couponCount }}</span>
            </RouterLink>
          </li>
          <li class="menu-item" :class="{ active: $route.name === 'UserSetting' }">
            <RouterLink :to="{name: 'UserSetting'}">
              <i class="fas fa-cog"></i>
              <span>账户设置</span>
            </RouterLink>
          </li>

          <!-- 分隔线和退出登录 -->
          <li class="menu-divider"></li>
          <li class="menu-item">
            <RouterLink :to="{ name:'UserDashboard' }" class="return-btn">
              <i class="fas fa-store"></i>
              <span>返回商城</span>
            </RouterLink>
          </li>
          <li class="menu-divider"></li>
          <li class="menu-item logout-item">
            <a href="#" class="login_button" @click.prevent="handleLogout">
              <i class="fas fa-sign-out-alt"></i>
              <span>退出登录</span>
            </a>
          </li>
        </ul>
      </nav>
    </aside>

    <!-- 右侧主内容区 - RouterView 渲染当前激活的子页面 -->
    <main class="user-main-content">
      <RouterView />
    </main>
  </div>
</template>

<script setup lang="ts">
    import { ref, watch, onMounted } from 'vue' 
    import { useRouter, useRoute } from 'vue-router'
    import { authAPI } from '@/api/authAPI' 
    import { showToast } from 'vant' 
    import 'vant/es/toast/style' 
    import { useAuthStore } from '@/stores/auth'  
    import { storeToRefs } from 'pinia' 

    // ============ Pinia 权限状态 ============
    const authStore = useAuthStore()
    const { isLoggedIn, role, status } = storeToRefs(authStore)

    const router = useRouter() // 获取路由实例
    const route = useRoute() // 获取当前路由信息

    // 用户个人信息
    const userProfile = ref({
        avatar: '',
        nickname: ''
    })

    // 默认头像
    const DEFAULT_AVATAR = "/images/user-avatar.jpg"

    // 统计数据（用于显示徽章）
    const orderCount = ref<number>(0)
    const favoriteCount = ref<number>(0)
    const couponCount = ref<number>(0)

    // 文件输入引用
    const avatarInput = ref<HTMLInputElement | null>(null)

    // 获取用户信息
    const getUserProfile = async () => {
      try {
          const response = await authAPI.getUserProfile()
          userProfile.value = response.data
      } catch(error) {
          showToast({
            message: '获取用户信息失败',
            type: 'fail',
            duration: 2000
          })
          router.push({name: 'UserDashboard'})
      }
    }

    // 触发头像上传
    const triggerAvatarInput = () => {
      avatarInput.value?.click()
    }

    // 处理头像文件选择
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

    // 验证文件
    const validateFile = (file: File) => {
      const maxSize = 5 * 1024 * 1024
      const allowedTypes = ['image/jpeg', 'image/png', 'image/gif']

      if (file.size > maxSize) {
          showToast({
          message: '文件大小不能超过5MB',
          type: 'fail',
          duration: 2000
          })
          return false
      }
      if (!allowedTypes.includes(file.type)) {
          showToast({
              message: '请选择JPEG、PNG或GIF格式的图片',
              type: 'fail',
              duration: 2000
          })
          return false
      }
        return true
    }

    // 上传头像
    const uploadAvatar = async (file: File) => {
        // 创建图片预览URL
        const previewUrl = URL.createObjectURL(file)
        // 先显示预览图片
        userProfile.value.avatar = previewUrl
        
        // 显示加载提示
        const toast = showToast({
            message: '上传中...',
            type: 'loading',
            duration: 0,
            forbidClick: true
        })

        const formData = new FormData()
        formData.append('avatar', file)

        try {
            const response = await authAPI.updateAvatar(formData)
            if (response.success) {
            if (response.data) {
                // 更新成功，使用服务器返回的真实头像地址
                userProfile.value.avatar = response.data
            }
            toast.close()
            showToast({
                message: '头像更新成功',
                type: 'success',
                duration: 1500
            })
            } else {
                toast.close()
                showToast({
                    message: response.message || '上传失败',
                    type: 'fail',
                    duration: 2000
            })
            // 上传失败，恢复原头像（如果有原头像的话）
            if (userProfile.value.avatar === previewUrl) {
                userProfile.value.avatar = ''
            }
        }} catch (error: any) {
            toast.close()
            showToast({
              message: error.message || '上传失败，请重试',
              type: 'fail',
              duration: 2000
            })
            // 上传异常，恢复原头像
            if (userProfile.value.avatar === previewUrl) {
                userProfile.value.avatar = ''
            }
        } finally {
            // 清理预览URL，释放内存
            URL.revokeObjectURL(previewUrl)
        }
    }

    // 退出登录
    const handleLogout = async () => {
        // 显示加载提示
        const toast = showToast({
            message: '退出中...',
            type: 'loading',
            duration: 0,
            forbidClick: true
        })

        try {
            const response = await authAPI.logout()
            toast.close()
            if (response.success) {
              showToast({
                  message: '退出成功',
                  type: 'success',
                  duration: 1500
              })
              setTimeout(() => {
                  router.push({name: 'UserDashboard'})
              }, 1500)
            } else {
              toast.close()
              showToast({
                message: '退出失败，请重试',
                type: 'fail',
                duration: 2000
              })
            }
        } catch (error) {
            toast.close()
            console.error('退出失败:', error)
            showToast({
                message: '退出失败，请重试',
                type: 'fail',
                duration: 2000
            })
        }
    }

  onMounted(() => {
    // 在加载之前检查登录状态
    if (!authStore.validateUserPermission()) return; // 如果未登录，会跳转到登录页
    

    // 已登录加载信息
    if (isLoggedIn.value){
      getUserProfile()
    }

    // 这里可以加载统计数据
  })
</script>

<style scoped>
    @import url('@/static/css/user/个人中心布局页.css');
</style>