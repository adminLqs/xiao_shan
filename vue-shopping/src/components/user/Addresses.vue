<!-- views/user/Address.vue - 收货地址页面 -->
<template>
  <div class="address-container">
    <!-- 页面头部 -->
    <div class="address-header">
      <h3>收货地址</h3>
      <!-- 新增地址按钮 -->
      <button class="btn-primary" @click="openAddressModal()">
        <i class="fas fa-plus"></i> 新增地址
      </button>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>

    <!-- 地址列表 -->
    <div v-else class="address-list">
      <!-- 遍历地址列表 -->
      <div 
        v-for="address in addresses" 
        :key="address.id"
        class="address-card"
      >
        <!-- 地址信息区域 -->
        <div class="address-info">
          <!-- 收件人信息行 -->
          <div class="address-recipient">
            <span class="name">{{ address.recipientName }}</span>
            <span class="phone">{{ address.recipientPhone }}</span>
            <!-- 默认地址标签 -->
            <span v-if="address.isDefault" class="default-badge">默认</span>
          </div>
          <!-- 详细地址 -->
          <div class="address-detail">
            {{ address.province }} {{ address.city }} {{ address.district }} {{ address.detailAddress }}
          </div>
          <!-- 地址标签（家/公司/学校） -->
          <div class="address-label" v-if="address.label">
            <i class="fas fa-tag"></i>
            <span>{{ address.label }}</span>
          </div>
        </div>
        <!-- 操作按钮区域 -->
        <div class="address-actions">
          <button class="edit-btn" @click="openAddressModal(address)">编辑</button>
          <!-- 非默认地址才显示"设为默认"按钮 -->
          <button class="set-default-btn" v-if="!address.isDefault" @click="setDefaultAddress(address.id)">
            设为默认
          </button>
          <button class="delete-btn" @click="deleteAddress(address.id)">删除</button>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-if="addresses.length === 0" class="empty-state">
        <i class="fas fa-map-marker-alt"></i>
        <p>暂无收货地址</p>
        <button class="btn-primary" @click="openAddressModal()">立即添加</button>
      </div>
    </div>

    <!-- 新增/编辑地址弹窗 -->
    <div v-if="showAddressModal" class="modal-overlay" @click="closeAddressModal">
      <div class="modal-content" @click.stop>
        <!-- 弹窗头部 -->
        <div class="modal-header">
          <h3>{{ isEditing ? '编辑地址' : '新增地址' }}</h3>
          <button class="modal-close" @click="closeAddressModal">
            <i class="fas fa-times"></i>
          </button>
        </div>
        <!-- 弹窗主体 -->
        <div class="modal-body">
          <!-- 收件人姓名 -->
          <div class="form-group">
            <label>收件人姓名</label>
            <input type="text" v-model="addressForm.recipientName" placeholder="请输入收件人姓名">
          </div>
          <!-- 联系电话 -->
          <div class="form-group">
            <label>联系电话</label>
            <input type="tel" v-model="addressForm.recipientPhone" placeholder="请输入联系电话">
          </div>
          <!-- 地区选择（省/市/区） -->
          <div class="form-row">
            <div class="form-group">
              <label>省份</label>
              <input type="text" v-model="addressForm.province" placeholder="省份">
            </div>
            <div class="form-group">
              <label>城市</label>
              <input type="text" v-model="addressForm.city" placeholder="城市">
            </div>
            <div class="form-group">
              <label>区县</label>
              <input type="text" v-model="addressForm.district" placeholder="区县">
            </div>
          </div>
          <!-- 详细地址 -->
          <div class="form-group">
            <label>详细地址</label>
            <input type="text" v-model="addressForm.detailAddress" placeholder="街道、小区、门牌号">
          </div>
          <!-- 地址标签 -->
          <div class="form-group">
            <label>地址标签</label>
            <div class="label-options">
              <span class="label-option" :class="{ active: addressForm.label === '家' }" @click="addressForm.label = '家'">🏠 家</span>
              <span class="label-option" :class="{ active: addressForm.label === '公司' }" @click="addressForm.label = '公司'">💼 公司</span>
              <span class="label-option" :class="{ active: addressForm.label === '学校' }" @click="addressForm.label = '学校'">🎓 学校</span>
              <span class="label-option" :class="{ active: addressForm.label === '其他' }" @click="addressForm.label = '其他'">📍 其他</span>
            </div>
          </div>
          <!-- 设为默认地址复选框 -->
          <div class="form-group">
            <label class="checkbox-label">
              <span>设为默认地址</span>
              <input type="checkbox" v-model="addressForm.isDefault">
            </label>
          </div>
        </div>
        <!-- 弹窗底部按钮 -->
        <div class="modal-footer">
          <button class="btn-cancel" @click="closeAddressModal">取消</button>
          <button class="btn-confirm" @click="saveAddress">保存</button>
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
  import 'vant/es/dialog/style'
  import { useAuthStore } from '@/stores/auth'

  const authStore = useAuthStore()
  const router = useRouter()

  // ==================== 类型定义 ====================

  interface Address {
    id: number
    userId: number
    recipientName: string
    recipientPhone: string
    province: string
    city: string
    district: string
    detailAddress: string
    label: string
    isDefault: boolean
  }

  interface AddressForm {
    recipientName: string
    recipientPhone: string
    province: string
    city: string
    district: string
    detailAddress: string
    label: string
    isDefault: boolean
  }

  // ==================== 响应式数据 ====================

  const loading = ref(false)
  const addresses = ref<Address[]>([])

  const showAddressModal = ref(false)
  const isEditing = ref(false)
  const editingId = ref<number | null>(null)

  const addressForm = ref<AddressForm>({
    recipientName: '',
    recipientPhone: '',
    province: '',
    city: '',
    district: '',
    detailAddress: '',
    label: '',
    isDefault: false
  })

  // ==================== 数据加载 ====================

  /**
   * 加载地址列表
   * @returns {Promise<void>}
   */
  const loadAddresses = async () => {
    loading.value = true
    try {
      const response = await authAPI.getAddresses()
      if (response.success) {
        addresses.value = response.data || []
      }
    } catch (error) {
      showToast({ message: '加载失败', type: 'fail' })
    } finally {
      loading.value = false
    }
  }

  // ==================== 弹窗控制 ====================

  /**
   * 打开地址弹窗
   * @param {Address} [address] - 编辑时传入的地址对象
   */
  const openAddressModal = (address?: Address) => {
    if (address) {
      isEditing.value = true
      editingId.value = address.id
      addressForm.value = {
        recipientName: address.recipientName,
        recipientPhone: address.recipientPhone,
        province: address.province,
        city: address.city,
        district: address.district,
        detailAddress: address.detailAddress,
        label: address.label || '',
        isDefault: address.isDefault
      }
    } else {
      isEditing.value = false
      editingId.value = null
      addressForm.value = {
        recipientName: '',
        recipientPhone: '',
        province: '',
        city: '',
        district: '',
        detailAddress: '',
        label: '',
        isDefault: false
      }
    }
    showAddressModal.value = true
  }

  const closeAddressModal = () => {
    showAddressModal.value = false
    isEditing.value = false
    editingId.value = null
  }

  // ==================== 地址操作 ====================

  /**
   * 保存地址
   * @returns {Promise<void>}
   */
  const saveAddress = async () => {
    if (!addressForm.value.recipientName) {
      showToast({ message: '请输入收件人姓名', type: 'fail' })
      return
    }
    if (!addressForm.value.recipientPhone) {
      showToast({ message: '请输入联系电话', type: 'fail' })
      return
    }
    if (!addressForm.value.detailAddress) {
      showToast({ message: '请输入详细地址', type: 'fail' })
      return
    }

    try {
      let response
      if (isEditing.value) {
        response = await authAPI.updateAddress({
          id: editingId.value,
          ...addressForm.value
        })
      } else {
        response = await authAPI.addAddress(addressForm.value)
      }

      if (response.success) {
        showToast({ message: isEditing.value ? '修改成功' : '添加成功', type: 'success' })
        closeAddressModal()
        await loadAddresses()
      }
    } catch (error: any) {
      showToast({ message: error.message || '保存失败', type: 'fail' })
    }
  }

  /**
   * 删除地址
   * @param {number} addressId - 地址ID
   * @returns {Promise<void>}
   */
  const deleteAddress = async (addressId: number) => {
    try {
      await showConfirmDialog({
        title: '删除确认',
        message: '确定要删除该地址吗？'
      })
      const response = await authAPI.deleteAddress(addressId)
      if (response.success) {
        showToast({ message: '删除成功', type: 'success' })
        await loadAddresses()
      }
    } catch (error: any) {
      if (error !== 'cancel') {
        showToast({ message: error.message || '删除失败', type: 'fail' })
      }
    }
  }

  /**
   * 设置默认地址
   * @param {number} addressId - 地址ID
   * @returns {Promise<void>}
   */
  const setDefaultAddress = async (addressId: number) => {
    try {
      const response = await authAPI.setDefaultAddress(addressId)
      if (response.success) {
        showToast({ message: '设置成功', type: 'success' })
        await loadAddresses()
      }
    } catch (error: any) {
      showToast({ message: error.message || '设置失败', type: 'fail' })
    }
  }

  // ==================== 生命周期 ====================

  onMounted(() => {
    if (!authStore.validateUserPermission()) return
    
    loadAddresses()
  })
</script>

<style scoped>
  @import url('@/static/css/user/地址页.css');
</style>