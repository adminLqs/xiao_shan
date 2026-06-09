<template>
  <div class="profile-page-container page-container">
    <div class="page-navbar">
      <button class="page-nav-back" @click="router.back()">
        <i class="fas fa-chevron-left"></i>
      </button>
      <div class="page-nav-title">
        <span>个人资料</span>
      </div>
      <div class="page-nav-right"></div>
    </div>

    <!-- 加载状态 - 骨架屏 -->
    <div v-if="loading" class="profile-content">
      <div class="skeleton-actions"></div>
      <div class="skeleton-card">
        <div class="skeleton-line short"></div>
        <div class="skeleton-avatar-row">
          <div class="skeleton-line"></div>
          <div class="skeleton-avatar"></div>
        </div>
        <div class="skeleton-line"></div>
        <div class="skeleton-line"></div>
        <div class="skeleton-line"></div>
      </div>
      <div class="skeleton-card">
        <div class="skeleton-line short"></div>
        <div class="skeleton-line"></div>
        <div class="skeleton-line"></div>
      </div>
    </div>

    <!-- 内容区域 -->
    <div v-else class="profile-content">
      <!-- 操作按钮区 -->
      <div class="profile-actions">
        <template v-if="!isEditing">
          <button class="btn primary-btn" @click="startEdit">
            <i class="fas fa-edit"></i>
            编辑资料
          </button>
          <button 
            v-if="authStore.hasRole('ROLE_SELLER') && authStore.activeRole !== 'ROLE_SELLER'" 
            class="btn secondary-btn" 
            @click="enterSellerMode"
          >
            <i class="fas fa-store"></i>
            进入商家后台
          </button>
        </template>
        <template v-else>
          <button class="btn outline-btn" @click="cancelEdit">
            <i class="fas fa-times"></i>
            取消编辑
          </button>
          <button class="btn primary-btn" @click="handleSave" :disabled="isSaving">
            <i v-if="isSaving" class="fas fa-spinner fa-spin"></i>
            <i v-else class="fas fa-save"></i>
            {{ isSaving ? '保存中...' : '保存更改' }}
          </button>
        </template>
      </div>

      <!-- 基本资料 -->
      <div class="profile-card">
        <div class="card-header">
          <h2 class="card-title">
            <i class="fas fa-id-card"></i>
            基本资料
          </h2>
        </div>
        <!-- 头像行 -->
        <div class="avatar-row" @click="triggerUpload">
          <span class="row-label">头像</span>
          <div class="avatar-right">
            <img :src="user.avatar || defaultAvatar" class="avatar-thumb"
              @click.stop="previewAvatar" alt="头像" />
          </div>
        </div>
        <!-- 隐藏的文件选择器 -->
        <input
          ref="fileInput"
          type="file"
          accept="image/*"
          style="display:none"
          @change="handleAvatarChange"
        />
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

      <!-- 底部选择弹窗 -->
      <div v-if="showAvatarSheet" class="action-sheet-overlay" @click="showAvatarSheet = false">
        <div class="action-sheet" @click.stop>
          <div class="sheet-title">更换头像</div>
          <div class="sheet-option" @click="takePhoto">
            <i class="fas fa-camera"></i>
            <span>拍照</span>
          </div>
          <div class="sheet-option" @click="selectFromAlbum">
            <i class="fas fa-image"></i>
            <span>从相册选择</span>
          </div>
          <div class="sheet-cancel" @click="showAvatarSheet = false">取消</div>
        </div>
      </div>

      <!-- 头像预览弹窗 -->
      <div v-if="showAvatarPreview" class="image-preview-overlay" @click="showAvatarPreview = false">
        <div class="image-preview-container" @click.stop>
          <img :src="user.avatar" class="preview-image" />
          <button class="preview-close" @click="showAvatarPreview = false">
            <i class="fas fa-times"></i>
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
import Message from '@/utils/message'
import { useAuthStore } from '@/stores/auth'
import { storeToRefs } from 'pinia'
import defaultAvatar from '@/static/images/user-avatar.jpg'


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
const loading = ref(true)
const userBackup = ref<Partial<User>>({})
const fileInput = ref<HTMLInputElement | null>(null)
const showAvatarSheet = ref(false)
const showAvatarPreview = ref(false)

// ==================== 头像上传 ====================

const triggerUpload = () => {
  showAvatarSheet.value = true
}

const selectFromAlbum = () => {
  showAvatarSheet.value = false
  fileInput.value?.click()
}

const takePhoto = () => {
  showAvatarSheet.value = false
  fileInput.value?.click()
}

// ==================== 头像预览 ====================

const previewAvatar = () => {
  if (user.avatar) {
    showAvatarPreview.value = true
  }
}

const handleAvatarChange = async (e: Event) => {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  // 文件格式校验
  const validTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp']
  const isValidType = validTypes.includes(file.type.toLowerCase()) ||
                      /\.(jpg|jpeg|png|webp)$/i.test(file.name)

  if (!isValidType) {
    Message.error('请选择 JPG、PNG 或 WebP 格式的图片')
    input.value = ''
    return
  }

  // 文件大小校验（5MB）
  const maxSize = 5 * 1024 * 1024 // 5MB
  if (file.size > maxSize) {
    Message.error('图片大小不能超过 5MB')
    input.value = ''
    return
  }

  // 立即显示本地预览（使用 FileReader）
  const reader = new FileReader()
  reader.onload = (e) => {
    user.avatar = e.target?.result as string
  }
  reader.readAsDataURL(file)

  // 上传到服务器
  const formData = new FormData()
  formData.append('avatar', file)

  try {
    const response = await authAPI.updateAvatar(formData)
    if (response.success) {
      // 上传成功后替换为服务器返回的真实URL
      user.avatar = response.data?.avatarUrl || response.data?.avatar || ''
      Message.success('头像更新成功')
    } else {
      // 上传失败恢复原头像
      await loadUserProfile()
      Message.error(response.message || '上传失败')
    }
  } catch (error: any) {
    await loadUserProfile()
    Message.error(error.message || '上传失败')
  } finally {
    input.value = ''
  }
}

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
    if (response.success && response.data?.profile) {
      Object.assign(user, response.data.profile)
    }
  } catch (error: any) {
    const status = error.response?.status
    if (status === 401 || status === 403) {
      throw error
    }

    Message.error(error.response?.data?.message || '加载用户信息失败')
  } finally {
    loading.value = false
  }
}

// ==================== 编辑模式 ====================

/**
* 进入编辑模式
* @description 备份当前数据，用于取消时恢复
*/
const startEdit = () => {
  if (!isLoggedIn.value) {
    Message.error('请先登录')
    setTimeout(() => router.push({ name: 'Login' }), 1500)
    return
  }

  isEditing.value = true
  userBackup.value = JSON.parse(JSON.stringify(user))
  Message.info('进入编辑模式')
}

/**
* 取消编辑
* @description 恢复备份数据后退出编辑模式
*/
const cancelEdit = () => {
  if (isSaving.value) {
    Message.warning('正在保存，请稍候...')
    return
  }

  if (userBackup.value && Object.keys(userBackup.value).length > 0) {
    Object.assign(user, userBackup.value)
  }

  isEditing.value = false
  Message.info('已取消编辑')
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
    Message.error('请先登录')
    setTimeout(() => router.push({ name: 'Login' }), 1500)
    return
  }

  const errors = validateForm()
  if (errors.length > 0) {
    Message.error(errors.join('，'))
    return
  }

  isSaving.value = true

  const loading = Message.loading({ text: '保存中...' })

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
      Object.assign(user, response.data?.profile || updateData)
      isEditing.value = false
      loading.close()
      Message.success('个人资料更新成功')
    } else {
      loading.close()
      Message.error(response.message || '更新失败')
    }
  } catch (error: any) {
    loading.close()

    const status = error.response?.status
    if (status === 401 || status === 403) {
      throw error
    }

    let errorMessage = '网络错误，请稍后重试'
    if (error.response) {
      errorMessage = error.response.data?.message || errorMessage
    }

    Message.error(errorMessage)
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
    Message.error('请先登录')
    setTimeout(() => router.push({ name: 'Login' }), 1500)
    return
  }

  if (!user.email) {
    Message.warning('请先填写手机号码')
    return
  }

  if (!/^1[3-9]\d{9}$/.test(user.phone)) {
    Message.error('手机号格式不正确')
    return
  }

  Message.success('验证码已发送到手机，请注意查收')
}

/**
* 进入商家后台
* @description 切换角色为商家并跳转商家端
*/
const enterSellerMode = async () => {
  const success = await authStore.switchRole('ROLE_SELLER')
  if (success) {
    router.push({ name: 'SellerDashboard' })
  } else {
    Message.error('切换角色失败')
  }
}

/**
* 发送邮箱验证码
* @description 校验邮箱格式后发送验证码
*/
const sendEmailVerification = () => {
  if (!isLoggedIn.value) {
    Message.error('请先登录')
    setTimeout(() => router.push({ name: 'Login' }), 1500)
    return
  }

  if (!user.email) {
    Message.warning('请先填写邮箱地址')
    return
  }

  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(user.email)) {
    Message.error('邮箱格式不正确')
    return
  }

  Message.success('验证邮件已发送，请查收邮箱')
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
