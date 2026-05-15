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

      <!-- ✅ 新增：店铺标语 -->
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

      <!-- ✅ 新增：店铺地址 -->
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
    import { authAPI } from '@/api/auth'

    // ✅ 更新商家信息接口，添加 slogan 和 address
    interface SellerProfile {
        id?: number
        storeName: string
        storeDetail: string
        storeAvatar: string
        phone: string
        isOpen: boolean
        business: string
        slogan: string      // ✅ 新增：店铺标语
        address: string     // ✅ 新增：店铺地址
    }

    // 响应式数据
    const isEditing = ref(false)
    const avatarInput = ref<HTMLInputElement | null>(null)

    // ✅ 更新 profile 响应式对象，添加 slogan 和 address
    const profile = reactive<SellerProfile>({
        storeName: '',
        storeDetail: '',
        storeAvatar: '',
        phone: '',
        isOpen: true,
        business: '',
        slogan: '',      // ✅ 新增
        address: ''      // ✅ 新增
    })

    // ✅ 更新 editData 响应式对象，添加 slogan 和 address
    const editData = reactive<SellerProfile>({
        storeName: '',
        storeDetail: '',
        storeAvatar: '',
        phone: '',
        isOpen: true,
        business: '',
        slogan: '',      // ✅ 新增
        address: ''      // ✅ 新增
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
            console.error('加载商家信息失败:', error)
            showToast('加载失败')
        }
    }

    // ✅ 更新重置编辑数据，包含 slogan 和 address
    const resetEditData = () => {
        Object.assign(editData, {
            storeName: profile.storeName,
            storeDetail: profile.storeDetail,
            storeAvatar: profile.storeAvatar,
            phone: profile.phone,
            isOpen: profile.isOpen,
            business: profile.business,
            slogan: profile.slogan,      // ✅ 新增
            address: profile.address      // ✅ 新增
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

    // ✅ 更新保存信息，包含 slogan 和 address
    const saveProfile = async () => {
        try {
            const response = await authAPI.updateSellerProfile({
                storeName: editData.storeName,
                storeDetail: editData.storeDetail,
                phone: editData.phone,
                isOpen: editData.isOpen,
                business: editData.business,
                slogan: editData.slogan,      // ✅ 新增
                address: editData.address      // ✅ 新增
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
            console.error('保存失败:', error)
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
            console.error('上传失败:', error)
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
    .seller-profile {
        padding: 24px;
        background-color: #f5f5f5;
        min-height: 100%;
    }

    .profile-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 24px;
    }

    .profile-header h2 {
        font-size: 24px;
        color: #333;
        margin: 0;
    }

    .edit-btn, .save-btn, .cancel-btn {
        padding: 8px 20px;
        border-radius: 8px;
        font-size: 14px;
        cursor: pointer;
        transition: all 0.2s;
        border: none;
    }

    .edit-btn {
        background-color: #e65100;
        color: white;
        display: flex;
        align-items: center;
        gap: 6px;
    }

    .edit-btn:hover {
        background-color: #bf360c;
    }

    .save-btn {
        background-color: #4caf50;
        color: white;
        margin-right: 12px;
    }

    .save-btn:hover {
        background-color: #45a049;
    }

    .cancel-btn {
        background-color: #f5f5f5;
        color: #666;
        border: 1px solid #ddd;
    }

    .cancel-btn:hover {
        background-color: #eee;
    }

    /* 头像区域 */
    .avatar-section {
        display: flex;
        justify-content: center;
        margin-bottom: 32px;
    }

    .avatar-wrapper {
        position: relative;
        width: 120px;
        height: 120px;
        border-radius: 50%;
        overflow: hidden;
        cursor: pointer;
        border: 3px solid #e65100;
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    }

    .avatar {
        width: 100%;
        height: 100%;
        object-fit: cover;
    }

    .avatar-overlay {
        position: absolute;
        bottom: 0;
        left: 0;
        right: 0;
        background: rgba(0, 0, 0, 0.6);
        color: white;
        text-align: center;
        padding: 8px;
        font-size: 12px;
        transform: translateY(100%);
        transition: transform 0.2s;
    }

    .avatar-wrapper:hover .avatar-overlay {
        transform: translateY(0);
    }

    /* 表单 */
    .info-form {
        background-color: white;
        border-radius: 16px;
        padding: 24px;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
    }

    .form-item {
        margin-bottom: 20px;
        display: flex;
        align-items: flex-start;
    }

    .form-label {
        width: 100px;
        font-size: 14px;
        font-weight: 500;
        color: #666;
        padding-top: 10px;
        flex-shrink: 0;
    }

    .form-value {
        flex: 1;
        padding: 10px 0;
        font-size: 14px;
        color: #333;
        line-height: 1.5;
    }

    .form-desc {
        white-space: pre-wrap;
        word-break: break-word;
    }

    .form-input, .form-textarea {
        flex: 1;
        padding: 10px 12px;
        border: 1px solid #ddd;
        border-radius: 8px;
        font-size: 14px;
        transition: all 0.2s;
        font-family: inherit;
    }

    .form-input:focus, .form-textarea:focus {
        outline: none;
        border-color: #e65100;
        box-shadow: 0 0 0 2px rgba(230, 81, 0, 0.1);
    }

    .form-textarea {
        resize: vertical;
    }

    /* 状态开关 */
    .status-switch {
        display: flex;
        align-items: center;
        gap: 12px;
        flex: 1;
    }

    .switch {
        position: relative;
        display: inline-block;
        width: 50px;
        height: 24px;
    }

    .switch input {
        opacity: 0;
        width: 0;
        height: 0;
    }

    .slider {
        position: absolute;
        cursor: pointer;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        background-color: #ccc;
        transition: 0.3s;
        border-radius: 24px;
    }

    .slider:before {
        position: absolute;
        content: "";
        height: 18px;
        width: 18px;
        left: 3px;
        bottom: 3px;
        background-color: white;
        transition: 0.3s;
        border-radius: 50%;
    }

    input:checked + .slider {
        background-color: #4caf50;
    }

    input:checked + .slider:before {
        transform: translateX(26px);
    }

    .status-text {
        font-size: 14px;
        color: #333;
    }

    .status-badge {
        display: inline-block;
        padding: 4px 12px;
        border-radius: 20px;
        font-size: 12px;
        font-weight: 500;
    }

    .status-badge.open {
        background-color: #e8f5e9;
        color: #4caf50;
    }

    .status-badge.closed {
        background-color: #ffebee;
        color: #f44336;
    }

    /* 响应式 */
    @media (max-width: 768px) {
        .seller-profile {
            padding: 16px;
        }
        
        .form-item {
            flex-direction: column;
        }
        
        .form-label {
            width: 100%;
            padding-bottom: 8px;
        }
        
        .form-value, .form-input, .form-textarea, .status-switch {
            width: 100%;
        }
    }
</style>