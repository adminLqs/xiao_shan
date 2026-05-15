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
import { showToast, showSuccessToast, showFailToast } from 'vant'
import { authAPI } from '@/api/authAPI'

// 更新商家信息接口，添加 slogan 和 address
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

// 响应式数据
const isEditing = ref(false)
const avatarInput = ref<HTMLInputElement | null>(null)

// 更新 profile 响应式对象，添加 slogan 和 address
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

// 更新 editData 响应式对象，添加 slogan 和 address
const editData = reactive<SellerProfile>({
    storeName: '',
    storeDetail: '',
    storeAvatar: '',
    phone: '',
    isOpen: true,
    business: '',
    slogan: '',      
    address: ''      
})

// 获取商家信息
const loadProfile = async () => {
    try {
        const response = await authAPI.getSellerProfile()
        const data = response.data || response
        
        if (data.success) {
            Object.assign(profile, data.profile)
            resetEditData()
        }
    } catch (error) {
        showToast({
            message: '加载失败',
            type: 'fail'
        })
    }
}

// 更新重置编辑数据，包含 slogan 和 address
const resetEditData = () => {
    Object.assign(editData, {
        storeName: profile.storeName,
        storeDetail: profile.storeDetail,
        storeAvatar: profile.storeAvatar,
        phone: profile.phone,
        isOpen: profile.isOpen,
        business: profile.business,
        slogan: profile.slogan,      
        address: profile.address      
    })
}

// 切换编辑模式
const toggleEdit = () => {
    resetEditData()
    isEditing.value = true
}

// 取消编辑
const cancelEdit = () => {
    isEditing.value = false
}

// 更新保存信息，包含 slogan 和 address
const saveProfile = async () => {
    try {
        const response = await authAPI.updateSellerProfile({
            storeName: editData.storeName,
            storeDetail: editData.storeDetail,
            phone: editData.phone,
            isOpen: editData.isOpen,
            business: editData.business,
            slogan: editData.slogan,      
            address: editData.address      
        })
        const data = response.data || response
        
        if (data.success) {
            Object.assign(profile, editData)
            isEditing.value = false
            showSuccessToast('保存成功')
        } else {
            showFailToast(data.message || '保存失败')
        }
    } catch (error) {
        showFailToast('保存失败')
    }
}

// 触发头像上传
const triggerAvatarUpload = () => {
    avatarInput.value?.click()
}

// 处理头像上传
const handleAvatarChange = async (e: Event) => {
    const input = e.target as HTMLInputElement
    const file = input.files?.[0]
    
    if (!file) return
    
    // 验证文件
    const maxSize = 5 * 1024 * 1024
    const allowedTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp']
    
    if (file.size > maxSize) {
        showFailToast('文件大小不能超过5MB')
        return
    }
    
    if (!allowedTypes.includes(file.type)) {
        showFailToast('请选择JPEG、PNG、GIF或WebP格式的图片')
        return
    }
    
    const formData = new FormData()
    formData.append('avatar', file)
    
    const toast = showToast({
        type: 'loading',
        message: '上传中...',
        forbidClick: true,
        duration: 0
    })
    
    try {
        const response = await authAPI.updateSellerAvatar(formData)
        const data = response.data || response
        
        if (data.success && data.avatar) {
            profile.storeAvatar = data.avatar
            if (isEditing.value) {
                editData.storeAvatar = data.avatar
            }
            showSuccessToast('头像更新成功')
        } else {
            showFailToast(data.message || '上传失败')
        }
    } catch (error) {
        showFailToast('上传失败')
    } finally {
        toast.close()
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