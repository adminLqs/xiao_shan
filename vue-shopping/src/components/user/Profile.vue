<template>
  <div class="profile-page-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <h1 class="page-title">
          <i class="fas fa-user-edit"></i>
          个人资料
        </h1>
      </div>
      <div class="page-actions">
        <button v-if="!isEditing" class="btn primary-btn" @click="startEdit">
          <i class="fas fa-edit"></i>
          编辑资料
        </button>
        <div v-else style="display: flex; gap: 10px;">
          <button class="btn outline-btn" @click="cancelEdit">
            <i class="fas fa-times"></i>
            取消编辑
          </button>
          <button class="btn primary-btn" @click="handleSave" :disabled="isSaving">
            <i v-if="isSaving" class="fas fa-spinner fa-spin"></i>
            <i v-else class="fas fa-save"></i>
            {{ isSaving ? '保存中...' : '保存更改' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 基本资料 -->
    <div class="profile-card">
      <div class="card-header">
        <h2 class="card-title">
          <i class="fas fa-id-card"></i>
          基本资料
        </h2>
      </div>
      <form id="profileForm">
        <div class="form-group">
          <label class="form-label" for="nickname">
            昵称
            <span class="required" v-if="isEditing">*</span>
          </label>
          <input 
            type="text" 
            id="nickname" 
            class="form-control" 
            v-model="user.nickname" 
            :disabled="!isEditing"
            maxlength="16"
            placeholder="请输入昵称"
          >
          <small class="form-hint">最多16个字符</small>
        </div>

        <div class="form-row">
          <div class="form-group">
            <label class="form-label" for="gender">性别</label>
            <select id="gender" class="form-control form-select" v-model="user.gender" :disabled="!isEditing">
              <option value="">请选择</option>
              <option value="MALE">男</option>
              <option value="FEMALE">女</option>
              <option value="UNKNOWN">未知</option>
            </select>
          </div>
          <div class="form-group">
            <label class="form-label" for="birthday">生日</label>
            <input 
              type="date" 
              id="birthday" 
              class="form-control" 
              v-model="user.birthday"
              :disabled="!isEditing"
            >
          </div>
        </div>

        <div class="form-group">
          <label class="form-label" for="region">地区</label>
          <input 
            type="text" 
            id="region" 
            class="form-control" 
            v-model="user.region" 
            :disabled="!isEditing"
            placeholder="请输入您所在的地区"
          >
        </div>

        <div class="form-group">
          <label class="form-label" for="bio">个人简介</label>
          <textarea 
            id="bio" 
            class="form-control form-textarea" 
            v-model="user.bio" 
            :disabled="!isEditing"
            rows="3"
            maxlength="200"
            placeholder="介绍一下自己吧~"
          ></textarea>
          <small class="form-hint">最多200个字符</small>
        </div>
      </form>
    </div>

    <!-- 联系方式 -->
    <div class="profile-card">
      <div class="card-header">
        <h2 class="card-title">
          <i class="fas fa-phone-alt"></i>
          联系方式
        </h2>
      </div>
      <div class="form-group">
        <label class="form-label" for="phone">
          手机号码
          <span v-if="user.phone && user.phoneVerified" class="verification-status verified">
            <i class="fas fa-check-circle"></i>
            已验证
          </span>
          <span v-else-if="user.phone && !user.phoneVerified" class="verification-status unverified">
            <i class="fas fa-exclamation-circle"></i>
            未验证
          </span>
          <span v-else class="verification-status unverified">
            <i class="fas fa-exclamation-circle"></i>
            未设置
          </span>
        </label>
        <div class="input-group">
          <input 
            type="tel" 
            id="phone" 
            class="form-control" 
            v-model="user.phone" 
            :disabled="!isEditing"
            placeholder="请输入手机号码"
          >
          <button v-if="isEditing && user.phone" class="btn secondary-btn" @click="sendPhoneVerification">
            <i class="fas fa-sms"></i>
            发送验证码
          </button>
        </div>
      </div>

      <div class="form-group">
        <label class="form-label" for="email">
          电子邮箱
          <span v-if="user.email && user.emailVerified" class="verification-status verified">
            <i class="fas fa-check-circle"></i>
            已验证
          </span>
          <span v-else-if="user.email && !user.emailVerified" class="verification-status unverified">
            <i class="fas fa-exclamation-circle"></i>
            未验证
          </span>
          <span v-else class="verification-status unverified">
            <i class="fas fa-exclamation-circle"></i>
            未设置
          </span>
        </label>
        <div class="input-group">
          <input 
            type="email" 
            id="email" 
            class="form-control" 
            v-model="user.email" 
            :disabled="!isEditing"
            placeholder="请输入电子邮箱"
          >
          <button v-if="isEditing && user.email" class="btn secondary-btn" @click="sendEmailVerification">
            <i class="fas fa-paper-plane"></i>
            发送验证
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { ref, reactive, onMounted } from 'vue'
  import { useRouter } from 'vue-router'
  import { authAPI } from '@/api/authAPI'
  import { showToast } from 'vant'
  import 'vant/es/toast/style'
  import { useAuthStore } from '@/stores/auth'
  import { storeToRefs } from 'pinia'

  const authStore = useAuthStore()
  const { isLoggedIn, role, status } = storeToRefs(authStore)

  const router = useRouter()

  // ==================== 类型定义 ====================

  interface User {
    avatar: string
    nickname: string
    phone: string
    email: string
    gender: string
    birthday: string
    region: string
    bio: string
    phoneVerified: boolean
    emailVerified: boolean
  }

  // ==================== 响应式数据 ====================

  const user = reactive<User>({
    avatar: '',
    nickname: '',
    phone: '',
    email: '',
    gender: '',
    birthday: '',
    region: '',
    bio: '',
    phoneVerified: false,
    emailVerified: false
  })

  const isSaving = ref(false)
  const isEditing = ref(false)
  const userBackup = ref<Partial<User>>({})

  // ==================== 页面跳转 ====================

  const goBack = () => {
    router.back()
  }

  // ==================== 数据加载 ====================

  /**
  * 加载用户个人信息
  * @returns {Promise<void>}
  */
  const loadUserProfile = async () => {
    try {
      const response = await authAPI.getUserProfile()
      if (response.success && response.data) {
        Object.assign(user, response.data)
      }
    } catch (error: any) {
      console.error('加载用户信息失败:', error)
      
      const status = error.response?.status
      if (status === 401 || status === 403) {
        throw error
      }
      
      showToast({
        message: error.response?.data?.message || '加载用户信息失败',
        type: 'fail',
        duration: 2000
      })
    }
  }

  // ==================== 编辑模式 ====================

  /**
  * 进入编辑模式
  * @description 备份当前数据，用于取消时恢复
  */
  const startEdit = () => {
    if (!isLoggedIn.value) {
      showToast({
        message: '请先登录',
        type: 'fail',
        duration: 1500,
        onClose: () => router.push('/login')
      })
      return
    }
    
    isEditing.value = true
    userBackup.value = JSON.parse(JSON.stringify(user))
    showToast({ message: '进入编辑模式', type: 'info', duration: 1500 })
  }

  /**
  * 取消编辑
  * @description 恢复备份数据后退出编辑模式
  */
  const cancelEdit = () => {
    if (isSaving.value) {
      showToast({ message: '正在保存，请稍候...', type: 'warning', duration: 1500 })
      return
    }
    
    if (userBackup.value && Object.keys(userBackup.value).length > 0) {
      Object.assign(user, userBackup.value)
    }
    
    isEditing.value = false
    showToast({ message: '已取消编辑', type: 'info', duration: 1500 })
  }

  // ==================== 表单校验 ====================

  /**
  * 校验表单数据
  * @returns {string[]} 错误信息数组
  */
  const validateForm = (): string[] => {
    const errors: string[] = []

    if (user.nickname && user.nickname.length > 16) {
      errors.push('昵称长度不能超过16个字符')
    }

    if (user.gender && !['MALE', 'FEMALE', 'UNKNOWN', ''].includes(user.gender)) {
      errors.push('性别必须是男、女或未知')
    }

    if (user.birthday) {
      const birthday = new Date(user.birthday)
      const today = new Date()
      today.setHours(0, 0, 0, 0)

      if (birthday >= today) {
        errors.push('生日必须是过去的日期')
      }
    }

    if (user.phone && !/^1[3-9]\d{9}$/.test(user.phone)) {
      errors.push('手机号格式不正确')
    }

    if (user.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(user.email)) {
      errors.push('邮箱格式不正确')
    }

    if (user.bio && user.bio.length > 200) {
      errors.push('个人简介不能超过200个字符')
    }

    return errors
  }

  // ==================== 保存数据 ====================

  /**
  * 保存用户信息
  * @returns {Promise<void>}
  */
  const handleSave = async () => {
    if (!isLoggedIn.value) {
      showToast({
        message: '请先登录',
        type: 'fail',
        duration: 1500,
        onClose: () => router.push('/login')
      })
      return
    }
    
    const errors = validateForm()
    if (errors.length > 0) {
      showToast({ message: errors.join('，'), type: 'fail', duration: 2000 })
      return
    }

    isSaving.value = true
    
    const toast = showToast({
      message: '保存中...',
      type: 'loading',
      duration: 0,
      forbidClick: true
    })

    try {
      const updateData = {
        nickname: user.nickname,
        gender: user.gender,
        birthday: user.birthday,
        region: user.region,
        bio: user.bio,
        phone: user.phone,
        email: user.email
      }

      const response = await authAPI.updateUserProfile(updateData)
      
      if (response.success) {
        Object.assign(user, response.data || updateData)
        isEditing.value = false
        toast.close()
        showToast({ message: '个人资料更新成功', type: 'success', duration: 1500 })
      } else {
        toast.close()
        showToast({ message: response.message || '更新失败', type: 'fail', duration: 2000 })
      }
    } catch (error: any) {
      console.error('更新失败:', error)
      toast.close()
      
      const status = error.response?.status
      if (status === 401 || status === 403) {
        throw error
      }
      
      let errorMessage = '网络错误，请稍后重试'
      if (error.response) {
        errorMessage = error.response.data?.message || errorMessage
      }
      
      showToast({ message: errorMessage, type: 'fail', duration: 2000 })
    } finally {
      isSaving.value = false
    }
  }

  // ==================== 验证码发送 ====================

  /**
  * 发送手机验证码
  * @description 校验手机号格式后发送验证码
  */
  const sendPhoneVerification = () => {
    if (!isLoggedIn.value) {
      showToast({
        message: '请先登录',
        type: 'fail',
        duration: 1500,
        onClose: () => router.push('/login')
      })
      return
    }
    
    if (!user.phone) {
      showToast({ message: '请先填写手机号码', type: 'warning', duration: 1500 })
      return
    }
    
    if (!/^1[3-9]\d{9}$/.test(user.phone)) {
      showToast({ message: '手机号格式不正确', type: 'fail', duration: 1500 })
      return
    }
    
    showToast({ message: '验证码已发送到手机，请注意查收', type: 'success', duration: 2000 })
  }

  /**
  * 发送邮箱验证码
  * @description 校验邮箱格式后发送验证码
  */
  const sendEmailVerification = () => {
    if (!isLoggedIn.value) {
      showToast({
        message: '请先登录',
        type: 'fail',
        duration: 1500,
        onClose: () => router.push('/login')
      })
      return
    }
    
    if (!user.email) {
      showToast({ message: '请先填写邮箱地址', type: 'warning', duration: 1500 })
      return
    }
    
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(user.email)) {
      showToast({ message: '邮箱格式不正确', type: 'fail', duration: 1500 })
      return
    }
    
    showToast({ message: '验证邮件已发送，请查收邮箱', type: 'success', duration: 2000 })
  }

  // ==================== 生命周期 ====================

  onMounted(() => {
    if (!authStore.validateUserPermission()) return
    
    loadUserProfile()
  })
</script>

<style scoped>
  @import url('@/static/css/user/个人资料.css');
</style>