<template>
  <div class="checkout-container">
    <!-- 页面头部区域：显示页面标题和面包屑导航 -->
    <div class="page-header">
      <h1 class="page-title">
        <i class="fas fa-credit-card"></i>
        订单结算
      </h1>
      <div class="breadcrumb">
        <RouterLink to="/">首页</RouterLink>
        <i class="fas fa-chevron-right"></i>
        <RouterLink to="/cart">购物车</RouterLink>
        <i class="fas fa-chevron-right"></i>
        <span class="current">订单结算</span>
      </div>
    </div>

    <!-- 加载状态：数据加载时显示旋转动画 -->
    <div v-if="loading" class="loading-container">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>

    <div v-else class="checkout-content">
      <!-- 左侧：订单信息区域 -->
      <div class="checkout-main">
        <!-- 收货地址区域 -->
        <div class="address-section">
          <div class="section-header">
            <h3><i class="fas fa-map-marker-alt"></i> 收货地址</h3>
            <button class="add-address-btn" @click="openAddressModal()">
              <i class="fas fa-plus"></i> 新增地址
            </button>
          </div>
          
          <!-- 地址列表：有地址时显示 -->
          <div class="address-list" v-if="addresses.length > 0">
            <div 
              v-for="address in addresses" 
              :key="address.id"
              class="address-card"
              :class="{ active: selectedAddressId === address.id }"
              @click="selectAddress(address.id)"
            >
              <!-- 地址信息区域 -->
              <div class="address-info">
                <div class="address-recipient">
                  <span class="name">{{ address.recipientName }}</span>
                  <span class="phone">{{ address.recipientPhone }}</span>
                  <!-- 默认地址标签：只有默认地址才显示 -->
                  <span v-if="address.isDefault" class="default-badge">默认</span>
                </div>
                <div class="address-detail">
                  {{ address.province }} {{ address.city }} {{ address.district }} {{ address.detailAddress }}
                </div>
                <!-- 地址标签（家/公司/学校） -->
                <div class="address-label" v-if="address.label">
                  <i class="fas fa-tag"></i>
                  <span>{{ address.label }}</span>
                </div>
              </div>
              <!-- 地址操作按钮 -->
              <div class="address-actions">
                <button class="edit-address" @click.stop="openAddressModal(address)">编辑</button>
                <button class="delete-address" @click.stop="deleteAddress(address.id)">删除</button>
              </div>
            </div>
          </div>
          
          <!-- 空地址状态：没有地址时显示 -->
          <div v-else class="empty-address">
            <i class="fas fa-map-marker-alt"></i>
            <p>暂无收货地址，请添加</p>
            <button class="btn-primary" @click="openAddressModal()">立即添加</button>
          </div>
        </div>

        <!-- 商品列表区域 -->
        <div class="product-section">
          <div class="section-header">
            <h3><i class="fas fa-box"></i> 商品信息</h3>
          </div>
          
          <div class="product-list">
            <!-- 遍历订单商品列表 -->
            <div v-for="item in orderItems" :key="item.productId" class="product-item">
              <!-- 商品图片 -->
              <div class="product-image">
                <img :src="item.productImage || '/images/default-product.jpg'" :alt="item.productName">
              </div>
              <!-- 商品信息 -->
              <div class="product-info">
                <div class="product-name">{{ item.productName }}</div>
                <div class="product-brand">{{ item.brand || '官方旗舰店' }}</div>
              </div>
              <!-- 商品单价 -->
              <div class="product-price">¥{{ formatPrice(item.price) }}</div>
              <!-- 商品数量 -->
              <div class="product-quantity">x{{ item.quantity }}</div>
              <!-- 商品小计 -->
              <div class="product-subtotal">¥{{ formatPrice(item.price * item.quantity) }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧：结算信息区域 -->
      <div class="checkout-sidebar">
        <div class="order-summary">
          <h3>订单信息</h3>
          
          <!-- 商品总价 -->
          <div class="summary-row">
            <span>商品总价</span>
            <span>¥{{ formatPrice(totalAmount) }}</span>
          </div>
          <!-- 运费 -->
          <div class="summary-row">
            <span>运费</span>
            <span>¥{{ formatPrice(shippingFee) }}</span>
          </div>
          <!-- 实付款 -->
          <div class="summary-row total">
            <span>实付款</span>
            <span class="total-amount">¥{{ formatPrice(payAmount) }}</span>
          </div>

          <!-- 支付方式选择区域 -->
          <div class="payment-section">
            <div class="payment-title">支付方式</div>
            <div class="payment-options">
              <!-- 支付宝支付选项 -->
              <div 
                class="payment-option"
                :class="{ active: paymentMethod === 'ALIPAY' }"
                @click="paymentMethod = 'ALIPAY'"
              >
                <svg class="payment-icon alipay-icon" viewBox="0 0 1024 1024" width="28" height="28">
                  <path fill="#1677FF" d="M873.6 313.6c-25.6-51.2-64-96-108.8-128-44.8-32-96-51.2-153.6-57.6-57.6-6.4-115.2 0-166.4 19.2-51.2 19.2-96 51.2-128 96-32 44.8-51.2 96-51.2 153.6 0 57.6 12.8 108.8 38.4 153.6 25.6 44.8 64 83.2 108.8 108.8 44.8 25.6 96 38.4 153.6 38.4 57.6 0 108.8-12.8 153.6-38.4 44.8-25.6 83.2-64 108.8-108.8 25.6-44.8 38.4-96 38.4-153.6 0-57.6-12.8-108.8-38.4-153.6z"/>
                  <path fill="#FFFFFF" d="M512 768c-140.8 0-256-115.2-256-256s115.2-256 256-256 256 115.2 256 256-115.2 256-256 256z"/>
                  <text x="512" y="520" text-anchor="middle" fill="#1677FF" font-size="40" font-weight="bold">支</text>
                </svg>
                <span>支付宝支付</span>
                <i v-if="paymentMethod === 'ALIPAY'" class="fas fa-check-circle check-icon"></i>
              </div>
              <!-- 微信支付选项 -->
              <div 
                class="payment-option"
                :class="{ active: paymentMethod === 'WECHAT' }"
                @click="paymentMethod = 'WECHAT'"
              >
                <svg class="payment-icon wechat-icon" viewBox="0 0 1024 1024" width="28" height="28">
                  <path fill="#07C160" d="M512 64C264.6 64 64 264.6 64 512s200.6 448 448 448 448-200.6 448-448S759.4 64 512 64z"/>
                  <path fill="#FFFFFF" d="M512 384c-70.4 0-128 57.6-128 128s57.6 128 128 128 128-57.6 128-128-57.6-128-128-128z"/>
                  <circle fill="#07C160" cx="384" cy="512" r="32"/>
                  <circle fill="#07C160" cx="640" cy="512" r="32"/>
                </svg>
                <span>微信支付</span>
                <i v-if="paymentMethod === 'WECHAT'" class="fas fa-check-circle check-icon"></i>
              </div>
            </div>
          </div>

          <!-- 提交订单按钮 -->
          <button 
            class="submit-btn" 
            @click="submitOrder"
            :disabled="submitting || !selectedAddressId"
          >
            <i v-if="submitting" class="fas fa-spinner fa-spin"></i>
            <i v-else class="fas fa-check"></i>
            {{ submitting ? '提交中...' : `提交订单 · ¥${formatPrice(payAmount)}` }}
          </button>
          <!-- 未选择地址的警告提示 -->
          <p v-if="!selectedAddressId" class="warning-tip">
            <i class="fas fa-exclamation-circle"></i>
            请选择收货地址
          </p>
        </div>
      </div>
    </div>

    <!-- 新增/编辑地址弹窗 -->
    <div v-if="showAddressModal" class="modal-overlay" @click="closeAddressModal">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>{{ isEditingAddress ? '编辑地址' : '新增地址' }}</h3>
          <button class="modal-close" @click="closeAddressModal">
            <i class="fas fa-times"></i>
          </button>
        </div>
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
              <span 
                class="label-option" 
                :class="{ active: addressForm.label === '家' }"
                @click="addressForm.label = '家'"
              >🏠 家</span>
              <span 
                class="label-option" 
                :class="{ active: addressForm.label === '公司' }"
                @click="addressForm.label = '公司'"
              >💼 公司</span>
              <span 
                class="label-option" 
                :class="{ active: addressForm.label === '学校' }"
                @click="addressForm.label = '学校'"
              >🎓 学校</span>
              <span 
                class="label-option" 
                :class="{ active: addressForm.label === '其他' }"
                @click="addressForm.label = '其他'"
              >📍 其他</span>
            </div>
          </div>
          <!-- 设为默认地址复选框 -->
          <div class="form-group">
            <label class="checkbox-label">
              <input type="checkbox" v-model="addressForm.isDefault">
              <span>设为默认地址</span>
            </label>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-cancel" @click="closeAddressModal">取消</button>
          <button class="btn-confirm" @click="saveAddress">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { ref, computed, onMounted } from 'vue'
  import { useRouter, useRoute } from 'vue-router'
  import { authAPI } from '@/api/authAPI'
  import { showToast, showConfirmDialog } from 'vant'
  import 'vant/es/toast/style'
  import { useAuthStore } from '@/stores/auth'
  import { storeToRefs } from 'pinia'

  const authStore = useAuthStore()
  const { isLoggedIn, role, status } = storeToRefs(authStore)

  const router = useRouter()
  const route = useRoute()

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
    createdAt?: string
    updatedAt?: string
  }

  interface OrderItem {
    productId: number
    productName: string
    brand: string
    price: number
    quantity: number
    productImage: string
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
  const submitting = ref(false)
  const orderItems = ref<OrderItem[]>([])
  const addresses = ref<Address[]>([])
  const selectedAddressId = ref<number | null>(null)
  const paymentMethod = ref('ALIPAY')

  const showAddressModal = ref(false)
  const isEditingAddress = ref(false)
  const editingAddressId = ref<number | null>(null)

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

  // ==================== 计算属性 ====================

  /**
   * 商品总价
   * @description 所有商品单价 × 数量的总和
   */
  const totalAmount = computed(() => {
    return orderItems.value.reduce((sum, item) => sum + (item.price * item.quantity), 0)
  })

  /**
   * 运费
   * @description 满99元包邮，否则10元
   */
  const shippingFee = computed(() => {
    return totalAmount.value >= 99 ? 0 : 10
  })

  /**
   * 实付款
   * @description 商品总价 + 运费
   */
  const payAmount = computed(() => {
    return totalAmount.value + shippingFee.value
  })

  // ==================== 数据加载 ====================

  /**
   * 加载订单数据
   * @description 根据来源（购物车/商品详情）调用不同接口获取订单商品信息
   * @returns {Promise<void>}
   */
  const loadOrderData = async () => {
    loading.value = true
    
    try {
      const source = route.query.source as string
      
      if (source === 'cart') {
        const idsParam = route.query.cartItemIds as string
        if (!idsParam) {
          showToast({ message: '请选择商品', type: 'fail' })
          router.push('/cart')
          return
        }
        
        const ids = idsParam.split(',')
        const response = await authAPI.getCheckoutItemsFromCart({ ids })
        
        if (response.success) {
          orderItems.value = response.data || []
        } else {
          throw new Error(response.message || '获取商品信息失败')
        }
        
      } else if (source === 'product') {
        const productId = route.query.productId as string
        const quantity = route.query.quantity as string
        
        if (!productId || !quantity) {
          showToast({ message: '商品信息错误', type: 'fail' })
          router.push('/')
          return
        }
        
        const response = await authAPI.getCheckoutItemsFromProduct({
          productId: Number(productId),
          quantity: Number(quantity)
        })
        
        if (response.success) {
          orderItems.value = response.data || []
        } else {
          throw new Error(response.message || '获取商品信息失败')
        }
        
      } else {
        showToast({ message: '请选择商品', type: 'fail' })
        router.push('/')
        return
      }
      
    } catch (error: any) {
      console.error('加载订单数据失败:', error)
      showToast({ message: error.message || '加载失败', type: 'fail' })
    } finally {
      loading.value = false
    }
  }

  /**
   * 加载地址列表
   * @description 获取用户地址列表，并自动选中默认地址或第一个地址
   * @returns {Promise<void>}
   */
  const loadAddresses = async () => {
    try {
      const response = await authAPI.getAddresses()
      if (response.success) {
        addresses.value = response.data || []
        
        const defaultAddress = addresses.value.find(element => element.isDefault)
        if (defaultAddress) {
          selectedAddressId.value = defaultAddress.id
        } else if (addresses.value.length > 0) {
          selectedAddressId.value = addresses.value[0].id
        }
      }
    } catch (error) {
      console.error('加载地址失败:', error)
    }
  }

  // ==================== 地址管理 ====================

  /**
   * 选择收货地址
   * @param {number} addressId - 地址ID
   */
  const selectAddress = (addressId: number) => {
    selectedAddressId.value = addressId
  }

  /**
   * 打开地址弹窗
   * @param {Address} [address] - 编辑时传入的地址对象，新增时不传
   */
  const openAddressModal = (address?: Address) => {
    if (address) {
      isEditingAddress.value = true
      editingAddressId.value = address.id
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
      isEditingAddress.value = false
      editingAddressId.value = null
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
    isEditingAddress.value = false
    editingAddressId.value = null
  }

  /**
   * 保存地址
   * @description 校验表单后调用新增或更新接口
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
    if (!addressForm.value.province || !addressForm.value.city || !addressForm.value.district) {
      showToast({ message: '请填写完整地区信息', type: 'fail' })
      return
    }
    if (!addressForm.value.detailAddress) {
      showToast({ message: '请输入详细地址', type: 'fail' })
      return
    }
    
    try {
      let response
      if (isEditingAddress.value) {
        response = await authAPI.updateAddress({
          id: editingAddressId.value,
          ...addressForm.value
        })
      } else {
        response = await authAPI.addAddress(addressForm.value)
      }
      
      if (response.success) {
        showToast({ 
          message: isEditingAddress.value ? '修改成功' : '添加成功', 
          type: 'success' 
        })
        closeAddressModal()
        await loadAddresses()
      } else {
        throw new Error(response.message || '保存失败')
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
        message: '确定要删除该地址吗？',
        confirmButtonText: '确定',
        cancelButtonText: '取消'
      })
      
      const response = await authAPI.deleteAddress(addressId)
      if (response.success) {
        showToast({ message: '删除成功', type: 'success' })
        await loadAddresses()
      }
    } catch (error: any) {
      // 用户取消删除时静默处理
      if (error !== 'cancel') {
        showToast({ message: error.message || '删除失败', type: 'fail' })
      }
    }
  }

  // ==================== 订单提交 ====================

  /**
   * 提交订单
   * @description 校验地址后创建订单，并跳转支付宝支付
   * @returns {Promise<void>}
   */
  const submitOrder = async () => {
    if (!selectedAddressId.value) {
      showToast({ message: '请选择收货地址', type: 'fail' })
      return
    }
    
    submitting.value = true
    
    try {
      const source = route.query.source as string

      const orderData = {
        addressId: selectedAddressId.value,
        source: source,
        paymentMethod: paymentMethod.value,
        orderItems: orderItems.value.map(item => ({
          productId: item.productId,
          quantity: item.quantity
        }))
      }
      
      const response = await authAPI.createOrder(orderData)
      
      if (response.success) {
        showToast({ message: '订单创建成功', type: 'success' })

        const paymentHtml = response.data.paymentHtml
        
        const div = document.createElement('div')
        div.innerHTML = paymentHtml
        document.body.appendChild(div)
        
        const form = document.querySelector('form')
        if (form) {
          form.submit()
        }
      } else {
        throw new Error(response.message || '创建订单失败')
      }
    } catch (error: any) {
      console.error('提交订单失败:', error)
      showToast({ message: error.message || '提交订单失败', type: 'fail' })
    } finally {
      submitting.value = false
    }
  }

  // ==================== 工具函数 ====================

  /**
   * 格式化价格
   * @param {number} price - 原始价格
   * @returns {string} 保留两位小数的价格字符串
   */
  const formatPrice = (price: number): string => {
    return price.toFixed(2)
  }

  // ==================== 生命周期 ====================

  onMounted(() => {
    if (!authStore.validateUserPermission()) return
    loadOrderData()
    loadAddresses()
  })
</script>

<style scoped>
  @import url('@/static/css/user/结算页.css');
</style>