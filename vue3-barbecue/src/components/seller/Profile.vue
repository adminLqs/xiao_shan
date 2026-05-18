<template>
  <div class="seller-profile">
    <div class="profile-header">
      <h2>商家信息</h2>
      <button class="edit-btn" @click="toggleEdit" v-if="!isEditing">
        <span class="edit-icon">✏️</span>
        编辑信息
      </button>
      <div class="action-buttons" v-else>
        <button class="save-btn" @click="saveProfile">保存</button>
        <button class="cancel-btn" @click="cancelEdit">取消</button>
      </div>
    </div>

    <!-- 商家头像 -->
    <div class="avatar-section">
      <div class="avatar-wrapper" @click="triggerAvatarUpload">
        <img :src="profile.storeAvatar || '/images/seller-avatar.jpg'" alt="商家头像" class="avatar" />
        <div class="avatar-overlay">
          <span>更换头像</span>
        </div>
        <input type="file" ref="avatarInput" style="display: none" accept="image/*" @change="handleAvatarChange" />
      </div>
    </div>

    <!-- 信息表单 -->
    <div class="info-form">
      <div class="form-item">
        <label class="form-label">店铺名称</label>
        <div v-if="!isEditing" class="form-value">{{ profile.storeName || '未设置' }}</div>
        <input v-else type="text" v-model="editData.storeName" class="form-input" placeholder="请输入店铺名称" />
      </div>

      <!-- 新增：店铺标语 -->
      <div class="form-item">
        <label class="form-label">店铺标语</label>
        <div v-if="!isEditing" class="form-value">{{ profile.slogan || '未设置' }}</div>
        <input v-else type="text" v-model="editData.slogan" class="form-input" placeholder="例如：炭火匠心 · 深夜烧烤" />
      </div>

      <div class="form-item">
        <label class="form-label">联系电话</label>
        <div v-if="!isEditing" class="form-value">{{ profile.phone || '未设置' }}</div>
        <input v-else type="tel" v-model="editData.phone" class="form-input" placeholder="请输入联系电话" />
      </div>

      <!-- 新增：店铺地址 -->
      <div class="form-item">
        <label class="form-label">店铺地址</label>
        <div v-if="!isEditing" class="form-value">{{ profile.address || '未设置' }}</div>
        <input v-else type="text" v-model="editData.address" class="form-input" placeholder="请输入店铺地址" />
      </div>

      <div class="form-item">
        <label class="form-label">营业状态</label>
        <div v-if="!isEditing" class="form-value">
          <span class="status-badge" :class="profile.isOpen ? 'open' : 'closed'">
            {{ profile.isOpen ? '营业中' : '休息中' }}
          </span>
        </div>
        <div v-else class="status-switch">
          <label class="switch">
            <input type="checkbox" v-model="editData.isOpen" />
            <span class="slider"></span>
          </label>
          <span class="status-text">{{ editData.isOpen ? '营业中' : '休息中' }}</span>
        </div>
      </div>

      <div class="form-item">
        <label class="form-label">营业时间</label>
        <div v-if="!isEditing" class="form-value">{{ profile.business || '未设置' }}</div>
        <input v-else type="text" v-model="editData.business" class="form-input" placeholder="例如：10:00-22:00" />
      </div>

      <div class="form-item">
        <label class="form-label">店铺详情</label>
        <div v-if="!isEditing" class="form-value form-desc">{{ profile.storeDetail || '未设置' }}</div>
        <textarea v-else v-model="editData.storeDetail" class="form-textarea" rows="4" placeholder="请输入店铺介绍"></textarea>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
  import { ref, reactive, onMounted } from 'vue'
  import { authAPI } from '@/api/authAPI'
  import Message from '@/utils/message'

  interface SellerProfile {
    id?: number
    storeName: string
    storeDetail: string
    storeAvatar: string
    phone: string
    isOpen: boolean
    business: string
    slogan: string
    address: string
  }

  const isEditing = ref(false)
  const avatarInput = ref<HTMLInputElement | null>(null)
  const submitting = ref(false)

  const profile = reactive<SellerProfile>({
    storeName: '',
    storeDetail: '',
    storeAvatar: '',
    phone: '',
    isOpen: true,
    business: '',
    slogan: '',
    address: ''
  })

  const editData = reactive<SellerProfile>({ ...profile })

  // ==================== 加载商家信息 ====================
  const loadProfile = async () => {
    const loading = Message.loading({ text: '加载中...' })
    
    try {
      const response = await authAPI.getSellerProfile()
      
      if (response.success) {
        Object.assign(profile, response.data.profile)
        resetEditData()
      } else {
        Message.error(response.message || '加载失败')
      }
    } catch (error: any) {
      Message.error(error.message || '加载失败')
    } finally {
      loading.close()
    }
  }

  // ==================== 编辑状态管理 ====================
  const resetEditData = () => {
    Object.assign(editData, { ...profile })
  }

  const toggleEdit = () => {
    resetEditData()
    isEditing.value = true
  }

  const cancelEdit = () => {
    isEditing.value = false
  }

  // ==================== 保存商家信息 ====================
  const saveProfile = async () => {
    if (!editData.storeName.trim()) {
      Message.warning('请输入店铺名称')
      return
    }
    
    if (!editData.phone.trim()) {
      Message.warning('请输入联系电话')
      return
    }
    
    const phoneReg = /^1[3-9]\d{9}$/
    if (!phoneReg.test(editData.phone)) {
      Message.warning('请输入正确的手机号码')
      return
    }
    
    submitting.value = true
    const loading = Message.loading({ text: '保存中...' })
    
    try {
      const response = await authAPI.updateSellerProfile({
        storeName: editData.storeName?.trim(),
        storeDetail: editData.storeDetail?.trim(),
        phone: editData.phone?.trim(),
        isOpen: editData.isOpen,
        business: editData.business?.trim(),
        slogan: editData.slogan?.trim(),
        address: editData.address?.trim()
      })
      
      if (response.success) {
        Object.assign(profile, editData)
        isEditing.value = false
        Message.success('保存成功')
      } else {
        Message.error(response.message || '保存失败')
      }
    } catch (error: any) {
      Message.error(error.message || '保存失败')
    } finally {
      loading.close()
      submitting.value = false
    }
  }

  // ==================== 头像上传 ====================
  const triggerAvatarUpload = () => {
    avatarInput.value?.click()
  }

  const handleAvatarChange = async (e: Event) => {
    const input = e.target as HTMLInputElement
    const file = input.files?.[0]
    
    if (!file) return
    
    const maxSize = 5 * 1024 * 1024
    const allowedTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp']
    
    if (file.size > maxSize) {
      Message.warning('文件大小不能超过5MB')
      return
    }
    
    if (!allowedTypes.includes(file.type)) {
      Message.warning('请选择JPEG、PNG、GIF或WebP格式的图片')
      return
    }
    
    const formData = new FormData()
    formData.append('avatar', file)
    
    const loading = Message.loading({ text: '上传中...' })
    
    try {
      const response = await authAPI.updateSellerAvatar(formData)
      
      if (response.success && response.data) {
        profile.storeAvatar = response.data.storeAvatar
        if (isEditing.value) {
          editData.storeAvatar = response.data
        }
        Message.success('头像更新成功')
      } else {
        Message.error(response.message || '上传失败')
      }
    } catch (error: any) {
      Message.error(error.message || '上传失败')
    } finally {
      loading.close()
      input.value = ''
    }
  }

  onMounted(() => {
    loadProfile()
  })
</script>

<style scoped>
  @import url('@/static/css/seller/商家信息页.css');
</style>