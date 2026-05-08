<template>
  <div class="seller-info-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-title">
        <i class="fas fa-store"></i>
        商家信息
      </h1>
      <div class="breadcrumb">
        <RouterLink to="/seller/dashboard">商家中心</RouterLink>
        <i class="fas fa-chevron-right"></i>
        <span class="current">商家信息</span>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>

    <!-- 商家信息表单 -->
    <div v-else class="info-card">
      <div class="card-header">
        <h3 class="card-title">
          <i class="fas fa-store-alt"></i>
          店铺资料
        </h3>
        <button class="edit-btn" @click="toggleEdit" v-if="!isEditing">
          <i class="fas fa-edit"></i>
          编辑资料
        </button>
      </div>

      <!-- 查看模式 -->
      <div v-if="!isEditing" class="view-mode">
        <!-- 店铺横幅 -->
        <div class="info-row" v-if="formData.storeBanner">
          <div class="info-label">店铺横幅</div>
          <div class="info-value">
            <img :src="formData.storeBanner" class="store-banner">
          </div>
        </div>

        <!-- 店铺头像 -->
        <div class="info-row">
          <div class="info-label">店铺头像</div>
          <div class="info-value">
            <img :src="formData.storeAvatar || '/images/seller-avatar.jpg'" class="store-avatar">
          </div>
        </div>

        <!-- 店铺名称 -->
        <div class="info-row">
          <div class="info-label">店铺名称</div>
          <div class="info-value">{{ formData.storeName || '未设置' }}</div>
        </div>

        <!-- 店铺简介 -->
        <div class="info-row">
          <div class="info-label">店铺简介</div>
          <div class="info-value">{{ formData.storeDetail || '未设置' }}</div>
        </div>

        <!-- 营业时间 -->
        <div class="info-row">
          <div class="info-label">营业时间</div>
          <div class="info-value">{{ formData.businessHours || '未设置' }}</div>
        </div>

        <!-- 联系电话 -->
        <div class="info-row">
          <div class="info-label">联系电话</div>
          <div class="info-value">{{ formData.contactPhone || '未设置' }}</div>
        </div>
      </div>

      <!-- 编辑模式 -->
      <div v-else class="edit-mode">
        <!-- 店铺横幅 -->
        <div class="form-group">
          <label class="form-label">店铺横幅</label>
          <div class="banner-upload">
            <div class="banner-preview" v-if="formData.storeBanner">
              <img :src="formData.storeBanner" class="preview-banner">
              <button class="remove-banner" @click="removeBanner">
                <i class="fas fa-times"></i>
              </button>
            </div>
            <div v-else class="banner-placeholder" @click="triggerBannerInput">
              <i class="fas fa-image"></i>
              <span>上传店铺横幅</span>
              <p>建议尺寸：1200 x 300px</p>
            </div>
            <input
              type="file"
              ref="bannerInput"
              accept="image/jpeg,image/png"
              style="display: none"
              @change="handleBannerUpload"
            >
          </div>
        </div>

        <!-- 店铺头像 -->
        <div class="form-group">
          <label class="form-label">店铺头像</label>
          <div class="avatar-upload">
            <img :src="formData.storeAvatar || '/images/seller-avatar.jpg'" class="preview-avatar">
            <button class="upload-btn" @click="triggerFileInput">
              <i class="fas fa-cloud-upload-alt"></i>
              更换头像
            </button>
            <input
              type="file"
              ref="fileInput"
              accept="image/jpeg,image/png"
              style="display: none"
              @change="handleAvatarUpload"
            >
          </div>
        </div>

        <div class="form-group">
          <label class="form-label required">店铺名称</label>
          <input type="text" class="form-control" v-model="formData.storeName" placeholder="请输入店铺名称">
        </div>

        <div class="form-group">
          <label class="form-label">店铺简介</label>
          <textarea class="form-control" v-model="formData.storeDetail" rows="3" placeholder="请输入店铺简介"></textarea>
        </div>

        <div class="form-row">
          <div class="form-group">
            <label class="form-label">营业时间</label>
            <input type="text" class="form-control" v-model="formData.businessHours" placeholder="如：09:00-21:00">
          </div>
          <div class="form-group">
            <label class="form-label">联系电话</label>
            <input type="tel" class="form-control" v-model="formData.contactPhone" placeholder="请输入联系电话">
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="form-actions">
          <button class="btn-cancel" @click="cancelEdit">取消</button>
          <button class="btn-save" @click="saveInfo" :disabled="submitting">
            <i v-if="submitting" class="fas fa-spinner fa-spin"></i>
            <i v-else class="fas fa-save"></i>
            {{ submitting ? '保存中...' : '保存信息' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { ref, onMounted } from 'vue'
  import { useRouter } from 'vue-router'
  import { authAPI } from '@/api/authAPI'
  import { showToast, showConfirmDialog } from 'vant'
  import 'vant/es/toast/style'
  import { useAuthStore } from '@/stores/auth'

  const authStore = useAuthStore()
  const router = useRouter()

  // ==================== 类型定义 ====================

  interface SellerFormData {
    storeName: string
    storeAvatar: string
    storeBanner: string
    storeDetail: string
    businessHours: string
    contactPhone: string
  }

  interface SellerProfileResponse {
    storeName: string
    storeAvatar?: string
    storeBanner?: string
    storeDetail?: string
    businessHours?: string
    contactPhone?: string
  }

  // ==================== 响应式数据 ====================

  const loading = ref(false)
  const isEditing = ref(false)
  const submitting = ref(false)

  const fileInput = ref<HTMLInputElement | null>(null)
  const bannerInput = ref<HTMLInputElement | null>(null)

  const pendingAvatarFile = ref<File | null>(null)
  const pendingBannerFile = ref<File | null>(null)

  const formData = ref<SellerFormData>({
    storeName: '',
    storeAvatar: '',
    storeBanner: '',
    storeDetail: '',
    businessHours: '',
    contactPhone: ''
  })

  const originalData = ref<SellerFormData>({
    storeName: '',
    storeAvatar: '',
    storeBanner: '',
    storeDetail: '',
    businessHours: '',
    contactPhone: ''
  })

  // ==================== 数据加载 ====================

  /**
   * 加载商家信息
   * @returns {Promise<void>}
   */
  const loadSellerInfo = async () => {
    loading.value = true

    try {
      const response = await authAPI.getSellerProfile()

      if (response.success && response.data) {
        const data = response.data as SellerProfileResponse
        formData.value = {
          storeName: data.storeName || '',
          storeAvatar: data.storeAvatar || '',
          storeBanner: data.storeBanner || '',
          storeDetail: data.storeDetail || '',
          businessHours: data.businessHours || '',
          contactPhone: data.contactPhone || ''
        }
        originalData.value = { ...formData.value }
      }
    } catch (error: any) {
      showToast({ message: error.message || '加载失败', type: 'fail' })
    } finally {
      loading.value = false
    }
  }

  // ==================== 编辑控制 ====================

  const toggleEdit = () => {
    originalData.value = { ...formData.value }
    isEditing.value = true
  }

  const cancelEdit = () => {
    formData.value = { ...originalData.value }
    pendingAvatarFile.value = null
    pendingBannerFile.value = null
    isEditing.value = false
  }

  // ==================== 文件上传 ====================

  const triggerFileInput = () => {
    fileInput.value?.click()
  }

  const triggerBannerInput = () => {
    bannerInput.value?.click()
  }

  /**
   * 处理头像上传（本地预览）
   * @param {Event} e - 文件上传事件
   */
  const handleAvatarUpload = async (e: Event) => {
    const input = e.target as HTMLInputElement
    const file = input.files?.[0]

    if (!file) return

    if (!file.type.match('image/jpeg') && !file.type.match('image/png')) {
      showToast({ message: '请上传 JPG 或 PNG 格式图片', type: 'fail' })
      return
    }

    if (file.size > 5 * 1024 * 1024) {
      showToast({ message: '图片大小不能超过 5MB', type: 'fail' })
      return
    }

    pendingAvatarFile.value = file

    const reader = new FileReader()
    reader.onload = (e) => {
      formData.value.storeAvatar = e.target?.result as string
    }
    reader.readAsDataURL(file)

    input.value = ''
  }

  /**
   * 处理横幅上传（本地预览）
   * @param {Event} e - 文件上传事件
   */
  const handleBannerUpload = async (e: Event) => {
    const input = e.target as HTMLInputElement
    const file = input.files?.[0]

    if (!file) return

    if (!file.type.match('image/jpeg') && !file.type.match('image/png')) {
      showToast({ message: '请上传 JPG 或 PNG 格式图片', type: 'fail' })
      return
    }

    if (file.size > 5 * 1024 * 1024) {
      showToast({ message: '横幅图片大小不能超过 5MB', type: 'fail' })
      return
    }

    pendingBannerFile.value = file

    const reader = new FileReader()
    reader.onload = (e) => {
      formData.value.storeBanner = e.target?.result as string
    }
    reader.readAsDataURL(file)

    input.value = ''
  }

  /**
   * 删除横幅
   * @returns {Promise<void>}
   */
  const removeBanner = async () => {
    try {
      await showConfirmDialog({
        title: '删除确认',
        message: '确定要删除店铺横幅吗？',
        confirmButtonText: '确定',
        cancelButtonText: '取消'
      })

      formData.value.storeBanner = ''
      pendingBannerFile.value = null
      showToast({ message: '横幅已删除', type: 'success' })
    } catch {
      // 用户取消，静默处理
    }
  }

  // ==================== 保存信息 ====================

  /**
   * 保存商家信息
   * @returns {Promise<void>}
   */
  const saveInfo = async () => {
    if (!formData.value.storeName.trim()) {
      showToast({ message: '请输入店铺名称', type: 'fail' })
      return
    }

    submitting.value = true

    try {
      const formDataObj = new FormData()

      const basicInfo = {
        storeName: formData.value.storeName,
        storeDetail: formData.value.storeDetail,
        businessHours: formData.value.businessHours,
        contactPhone: formData.value.contactPhone
      }

      formDataObj.append('storeInfo', new Blob([JSON.stringify(basicInfo)], {
        type: 'application/json'
      }))

      if (pendingAvatarFile.value) {
        formDataObj.append('avatar', pendingAvatarFile.value)
      }

      if (pendingBannerFile.value) {
        formDataObj.append('banner', pendingBannerFile.value)
      }

      const response = await authAPI.updateSellerProfile(formDataObj)

      if (response.success) {
        showToast({ message: '保存成功', type: 'success' })

        pendingAvatarFile.value = null
        pendingBannerFile.value = null

        await loadSellerInfo()
        isEditing.value = false
        originalData.value = { ...formData.value }
      } else {
        throw new Error(response.message || '保存失败')
      }
    } catch (error: any) {
      showToast({ message: error.message || '保存失败', type: 'fail' })
    } finally {
      submitting.value = false
    }
  }

  // ==================== 生命周期 ====================

  onMounted(() => {
    if (!authStore.validateSellerPermission()) return
    
    loadSellerInfo()
  })
</script>

<style scoped>
  @import url('@/static/css/seller/商家信息页.css');
</style>