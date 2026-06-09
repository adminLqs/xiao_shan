<template>
  <div class="address-container page-container">
    <div class="page-navbar">
      <button class="page-nav-back" @click="router.back()">
        <i class="fas fa-chevron-left"></i>
      </button>
      <div class="page-nav-title">
        <span>收货地址</span>
      </div>
      <div class="page-nav-right"></div>
    </div>

    <!-- 骨架屏加载 -->
    <div v-if="loading" class="address-list">
      <div v-for="n in 3" :key="n" class="address-card skeleton-card">
        <div class="address-info">
          <div class="skeleton-line medium"></div>
          <div class="skeleton-line short" style="margin-top: 8px;"></div>
        </div>
      </div>
    </div>

    <!-- 地址列表 -->
    <div v-else class="address-list">
      <!-- 遍历地址列表 -->
      <div
        v-for="address in addresses"
        :key="address.id"
        class="address-card"
        :class="{ 'default-address': address.isDefault }"
      >
        <!-- 地址信息区域 -->
        <div class="address-info">
          <!-- 收件人信息行 -->
          <div class="address-recipient">
            <span class="name">{{ address.recipientName }}</span>
            <span class="phone">{{ address.recipientPhone }}</span>
            <span v-if="address.isDefault" class="default-badge">默认</span>
          </div>
          <!-- 详细地址 -->
          <div class="address-detail text-ellipsis">
            {{ address.province }}{{ address.city }}{{ address.district }}{{ address.detailAddress }}
          </div>
          <!-- 地址标签 -->
          <span class="address-tag" v-if="address.label">{{ address.label }}</span>
        </div>
        <!-- 操作按钮区域 -->
        <div class="address-actions">
          <button class="action-icon edit-btn" @click="openAddressModal(address)" title="编辑">
            <i class="fas fa-pen"></i>
          </button>
          <button class="action-icon delete-btn" @click="deleteAddress(address.id)" title="删除">
            <i class="fas fa-trash"></i>
          </button>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-if="addresses.length === 0" class="empty-cart" style="margin-top: 40px;">
        <i class="fas fa-map-marker-alt"></i>
        <p>暂无收货地址</p>
      </div>
    </div>

    <!-- 底部固定新增按钮 -->
    <div class="bottom-fixed-bar">
      <button class="btn-primary" @click="openAddressModal()">
        <i class="fas fa-plus"></i> 新增收货地址
      </button>
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
          <!-- 地区选择 -->
          <div class="form-group">
            <label>所在地区</label>
            <AddressSelector v-model="addressRegion" @change="onRegionChange" />
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
          <!-- 设为默认地址 -->
          <div class="form-group switch-row">
            <span>设为默认地址</span>
            <label class="switch">
              <input type="checkbox" v-model="addressForm.isDefault">
              <span class="slider"></span>
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
  import Message from '@/utils/message'
  import { useAuthStore } from '@/stores/auth'
  import AddressSelector from '@/components/user/AddressSelector.vue'


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

  const loading = ref(true)
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

  const addressRegion = ref({
    province: '',
    city: '',
    district: ''
  })

  const onRegionChange = () => {
    addressForm.value.province = addressRegion.value.province
    addressForm.value.city = addressRegion.value.city
    addressForm.value.district = addressRegion.value.district
  }

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
        addresses.value = response.data?.addresses || []
      }
    } catch (error) {
      Message.error('加载失败')
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
      addressRegion.value = {
        province: address.province,
        city: address.city,
        district: address.district
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
      addressRegion.value = {
        province: '',
        city: '',
        district: ''
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
      Message.error('请输入收件人姓名')
      return
    }
    if (!addressForm.value.recipientPhone) {
      Message.error('请输入联系电话')
      return
    }
    if (!addressForm.value.province || !addressForm.value.city || !addressForm.value.district) {
      Message.error('请选择完整的省市区信息')
      return
    }
    if (!addressForm.value.detailAddress) {
      Message.error('请输入详细地址')
      return
    }

    try {
      let response
      if (isEditing.value && editingId.value !== null) {
        response = await authAPI.updateAddress({
          id: editingId.value,
          ...addressForm.value
        })
      } else {
        response = await authAPI.addAddress(addressForm.value)
      }

      if (response.success) {
        Message.success(isEditing.value ? '修改成功' : '添加成功')
        closeAddressModal()
        await loadAddresses()
      }
    } catch (error: any) {
      Message.error(error.message || '保存失败')
    }
  }

  /**
   * 删除地址
   * @param {number} addressId - 地址ID
   * @returns {Promise<void>}
   */
  const deleteAddress = async (addressId: number) => {
    try {
      await Message.confirm('确定要删除该地址吗？', '删除确认')
      const response = await authAPI.deleteAddress(addressId)
      if (response.success) {
        Message.success('删除成功')
        await loadAddresses()
      }
    } catch (error: any) {
      if (error !== 'cancel') {
        Message.error(error.message || '删除失败')
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
        Message.success('设置成功')
        await loadAddresses()
      }
    } catch (error: any) {
      Message.error(error.message || '设置失败')
    }
  }

  // ==================== 生命周期 ====================

  onMounted(() => {
    if (!authStore.validateUserPermission()) return

    loadAddresses()
  })
</script>

<style scoped>
@import url('@/static/css/user/收货地址.css');
</style>
