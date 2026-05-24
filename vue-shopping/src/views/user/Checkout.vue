<template>
  <div class="checkout-container">
    <!-- 顶部导航栏 -->
    <div class="page-navbar">
      <button class="page-nav-back" @click="router.back()">
        <i class="fas fa-chevron-left"></i>
      </button>
      <div class="page-nav-title">
        <i class="fas fa-file-invoice-dollar"></i>
        <span>确认订单</span>
      </div>
      <div class="page-nav-right"></div>
    </div>

    <!-- 加载状态 - 骨架屏 -->
    <div v-if="loading" class="checkout-content">
      <div class="skeleton-card">
        <div class="skeleton-line short"></div>
        <div class="skeleton-line medium"></div>
      </div>
      <div class="skeleton-card" style="margin-top: 12px;">
        <div class="skeleton-line short"></div>
        <div class="skeleton-line medium" style="margin-top: 12px;"></div>
        <div class="skeleton-line" style="margin-top: 8px;"></div>
      </div>
      <div class="skeleton-card" style="margin-top: 12px;">
        <div class="skeleton-line short"></div>
        <div class="skeleton-line" style="margin-top: 12px;"></div>
      </div>
      <div class="skeleton-card" style="margin-top: 12px;">
        <div class="skeleton-line"></div>
        <div class="skeleton-line medium" style="margin-top: 8px;"></div>
        <div class="skeleton-line short" style="margin-top: 8px;"></div>
      </div>
    </div>

    <!-- 结算内容 -->
    <div v-else class="checkout-content">
      <!-- ========== 收货地址卡片 ========== -->
      <div class="address-card" @click="showAddressPanel = true">
        <div class="address-icon">📍</div>
        <div class="address-info" v-if="selectedAddress">
          <div class="address-recipient">
            <span class="name">{{ selectedAddress.recipientName }}</span>
            <span class="phone">{{ selectedAddress.recipientPhone }}</span>
            <span v-if="selectedAddress.isDefault" class="default-badge">默认</span>
          </div>
          <div class="address-detail">
            {{ selectedAddress.province }} {{ selectedAddress.city }} {{ selectedAddress.district }} {{ selectedAddress.detailAddress }}
          </div>
        </div>
        <div v-else class="address-empty">
          <p>请选择收货地址</p>
        </div>
        <i class="fas fa-chevron-right"></i>
      </div>

      <!-- 灰色间隔 -->
      <div class="gray-divider"></div>

      <!-- ========== 商品信息区域 ========== -->
      <div class="product-section">
        <div class="section-header">
          <h3><i class="fas fa-box"></i> 商品信息</h3>
        </div>

        <div class="product-list">
          <div v-for="item in orderItems" :key="`${item.productId}${item.skuId || ''}`" class="product-item">
            <img :src="item.productImage" class="product-image" :alt="item.productName">
            <div class="product-info">
              <div class="product-name">{{ item.productName }}</div>
              <div class="tags-group">
                <span v-if="item.skuName" class="tag-spec">{{ item.skuName }}</span>
                <span class="tag-quantity">x{{ item.quantity }}</span>
              </div>
            </div>
            <div class="product-price">¥{{ formatPrice(item.price) }}</div>
          </div>
        </div>
      </div>

      <!-- 灰色间隔 -->
      <div class="gray-divider"></div>

      <!-- ========== 支付方式选择 ========== -->
      <div class="payment-section">
        <div class="section-header">
          <h3><i class="fas fa-credit-card"></i> 支付方式</h3>
        </div>

        <div class="payment-options">
          <div
            class="payment-option"
            :class="{ active: paymentMethod === 'ALIPAY' }"
            @click="paymentMethod = 'ALIPAY'"
          >
              <svg class="icon-svg" viewBox="0 0 1024 1024" width="28" height="28">
                <path d="M789.333333 298.666667H234.666667c-25.6 0-46.933333 21.333333-46.933334 46.933333v332.8c0 25.6 21.333333 46.933333 46.933334 46.933333h213.333333l-64 106.666667 128-106.666667h277.333334c25.6 0 46.933333-21.333333 46.933333-46.933333V345.6c0-25.6-21.333333-46.933333-46.933333-46.933333z m-345.6 256h-128v-42.666667h128v42.666667z m170.667334 0h-128v-42.666667h128v42.666667z m170.666666 0h-128v-42.666667h128v42.666667z" fill="#1677FF"/>
              </svg>
            <span>支付宝</span>
            <i v-if="paymentMethod === 'ALIPAY'" class="fas fa-check-circle check-icon"></i>
          </div>
          <div
            class="payment-option"
            :class="{ active: paymentMethod === 'WECHAT' }"
            @click="paymentMethod = 'WECHAT'"
          >
              <svg class="icon-svg" viewBox="0 0 1024 1024" width="28" height="28">
                <path d="M864.32 465.28c-64.32-45.12-147.52-68.48-239.36-68.48-104.96 0-198.72 28.8-263.36 81.92-58.88 48-92.16 114.56-92.16 188.16 0 76.8 36.48 147.2 100.48 196.48 58.24 44.8 135.36 68.8 219.84 68.8 41.28 0 81.6-5.76 119.36-16.96l95.68 32.96-22.72-79.68c53.44-45.12 85.12-104.96 85.12-170.88 0-60.16-24-116.8-67.84-162.56z m-271.36 179.2c-19.2 0-34.56-15.36-34.56-34.56 0-19.2 15.36-34.56 34.56-34.56s34.56 15.36 34.56 34.56-15.36 34.56-34.56 34.56z m125.44 0c-19.2 0-34.56-15.36-34.56-34.56 0-19.2 15.36-34.56 34.56-34.56s34.56 15.36 34.56 34.56-15.36 34.56-34.56 34.56z m174.72-355.84c-49.92-40.96-117.76-63.36-190.4-63.36-86.4 0-164.48 27.52-221.44 76.8-63.36 54.4-98.56 128-98.56 208 0 83.2 39.04 158.72 107.52 212.48 59.52 46.72 136.96 71.68 220.16 71.68 32.64 0 64.64-4.48 94.72-13.44l91.2 30.08-20.48-74.88c42.88-43.52 67.84-99.84 67.84-157.44 0-69.12-28.16-134.4-78.72-184.32z m-177.92 114.56c-14.72 0-26.56-11.84-26.56-26.56s11.84-26.56 26.56-26.56 26.56 11.84 26.56 26.56-11.84 26.56-26.56 26.56z m124.8 0c-14.72 0-26.56-11.84-26.56-26.56s11.84-26.56 26.56-26.56 26.56 11.84 26.56 26.56-11.84 26.56-26.56 26.56z" fill="#07C160"/>
              </svg>
            <span>微信支付</span>
            <i v-if="paymentMethod === 'WECHAT'" class="fas fa-check-circle check-icon"></i>
          </div>
        </div>
      </div>

      <!-- 灰色间隔 -->
      <div class="gray-divider"></div>

      <!-- ========== 订单信息 ========== -->
      <div class="order-info-section">
        <div class="section-header">
          <h3><i class="fas fa-file-text"></i> 订单信息</h3>
        </div>

        <div class="order-info-list">
          <div class="info-row">
            <span>商品总价</span>
            <span>¥{{ formatPrice(totalAmount) }}</span>
          </div>
          <div class="info-row">
            <span>运费</span>
            <span>¥{{ formatPrice(shippingFee) }}</span>
          </div>
          <div class="info-row total">
            <span>合计</span>
            <span class="total-amount">¥{{ formatPrice(payAmount) }}</span>
          </div>
        </div>
      </div>

      <!-- 底部留空 -->
      <div class="bottom-space"></div>
    </div>

    <!-- ========== 底部固定提交按钮 ========== -->
    <div class="bottom-bar">
      <div class="total-info">
        <span class="total-label">合计:</span>
        <span class="total-price">¥{{ formatPrice(payAmount) }}</span>
      </div>
      <button
        class="submit-btn"
        @click="submitOrder"
        :disabled="submitting || !selectedAddressId"
        :class="{ 'is-loading': submitting }"
      >
        <i v-if="submitting" class="fas fa-spinner fa-spin"></i>
        {{ submitting ? '提交中...' : '提交订单' }}
      </button>
    </div>

    <!-- ========== 地址选择面板（底部弹出） ========== -->
    <div class="address-panel" :class="{ show: showAddressPanel }">
      <div class="panel-overlay" @click="showAddressPanel = false"></div>
      <div class="panel-content">
        <div class="panel-header">
          <span>选择收货地址</span>
          <i class="fas fa-times" @click="showAddressPanel = false"></i>
        </div>
        <div class="panel-body">
          <div v-if="addresses.length > 0">
            <div v-for="addr in addresses" :key="addr.id" class="addr-item"
              :class="{ active: selectedAddressId === addr.id }">
              <div class="addr-info" @click="selectAddress(addr.id); showAddressPanel = false">
                <div class="addr-recipient">
                  <span class="name">{{ addr.recipientName }}</span>
                  <span class="phone">{{ addr.recipientPhone }}</span>
                  <span v-if="addr.isDefault" class="default-tag">默认</span>
                </div>
                <div class="addr-detail">
                  {{ addr.province }} {{ addr.city }} {{ addr.district }} {{ addr.detailAddress }}
                </div>
              </div>
              <div class="addr-actions">
                <button class="addr-edit-btn" @click.stop="editAddress(addr)">
                  <i class="fas fa-pen"></i>
                </button>
                <div v-if="selectedAddressId === addr.id" class="addr-check">
                  <i class="fas fa-check"></i>
                </div>
              </div>
            </div>
          </div>
          <div v-else class="empty-addr">
            <p>暂无收货地址</p>
          </div>
        </div>
        <button class="add-addr-btn" @click="openAddressModal(); showAddressPanel = false">+ 新增地址</button>
      </div>
    </div>

    <!-- ========== 新增/编辑地址弹窗 ========== -->
    <div v-if="showAddressModal" class="modal-overlay" @click="closeAddressModal">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>{{ isEditingAddress ? '编辑地址' : '新增地址' }}</h3>
          <button class="modal-close" @click="closeAddressModal">
            <i class="fas fa-times"></i>
          </button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label>收件人姓名</label>
            <input type="text" v-model="addressForm.recipientName" placeholder="请输入收件人姓名">
          </div>
          <div class="form-group">
            <label>联系电话</label>
            <input type="tel" v-model="addressForm.recipientPhone" placeholder="请输入联系电话">
          </div>
          <div class="form-group">
            <label>所在地区</label>
            <AddressSelector
              v-model:province="addressForm.province"
              v-model:city="addressForm.city"
              v-model:district="addressForm.district"
            />
          </div>
          <div class="form-group">
            <label>详细地址</label>
            <input type="text" v-model="addressForm.detailAddress" placeholder="街道、小区、门牌号">
          </div>
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
          <div class="form-group switch-row">
            <span>设为默认地址</span>
            <label class="switch">
              <input type="checkbox" v-model="addressForm.isDefault">
              <span class="slider"></span>
            </label>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-cancel" @click="closeAddressModal">取消</button>
          <button class="btn-confirm" @click="saveAddress">保存</button>
        </div>
      </div>
    </div>

    <!-- ========== 支付状态确认弹窗 ========== -->
    <div v-if="showPayConfirm" class="pay-confirm-overlay">
      <div class="pay-confirm-dialog">
        <i class="fas fa-check-circle pay-icon"></i>
        <h3>订单已创建</h3>
        <p>订单号：{{ createdOrderNumber }}</p>
        <p class="pay-tip">请在新窗口中完成支付</p>
        <div class="pay-actions">
          <button class="btn-pay-done" @click="checkPayStatus" :disabled="checkingPay">
            {{ checkingPay ? '查询中...' : '已完成支付' }}
          </button>
          <button class="btn-pay-later" @click="handlePayLater">稍后支付</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute, onBeforeRouteLeave } from 'vue-router'
import { authAPI } from '@/api/authAPI'
import Message from '@/utils/message'
import { useAuthStore } from '@/stores/auth'
import AddressSelector from '@/components/user/AddressSelector.vue'

const authStore = useAuthStore()
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
}

interface OrderItem {
  cartItemId?: number
  productId: number
  skuId?: number
  productName: string
  brand: string
  skuName?: string
  price: number
  originalPrice?: number
  quantity: number
  productImage: string
  stock: number
  isFreeShipping?: boolean
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
const submitting = ref(false)
const orderItems = ref<OrderItem[]>([])
const addresses = ref<Address[]>([])
const selectedAddressId = ref<number | null>(null)
const paymentMethod = ref('ALIPAY')

const showAddressModal = ref(false)
const showAddressPanel = ref(false)
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

// 支付相关状态
const showPayConfirm = ref(false)
const createdOrderId = ref<number | null>(null)
const createdOrderNumber = ref('')
const checkingPay = ref(false)



// ==================== 计算属性 ====================
const totalAmount = computed(() => {
  return orderItems.value.reduce((sum, item) => sum + (item.price * item.quantity), 0)
})

const shippingFee = computed(() => {
  // 检查所有商品是否都包邮
  const allFreeShipping = orderItems.value.every(item => item.isFreeShipping !== false)
  if (allFreeShipping) return 0

  // 满99包邮
  return totalAmount.value >= 99 ? 0 : 10
})

const payAmount = computed(() => {
  return totalAmount.value + shippingFee.value
})

const selectedAddress = computed(() => {
  return addresses.value.find(addr => addr.id === selectedAddressId.value)
})

// ==================== 数据加载 ====================
const loadOrderData = async () => {
  loading.value = true
  try {
    const source = route.query.source as string

    if (source === 'cart') {
      const idsParam = route.query.cartItemIds as string
      if (!idsParam) {
        Message.error('请选择商品')
        router.push({ name: 'Cart' })
        return
      }

      const ids = idsParam.split(',')
      const response = await authAPI.getCheckoutItemsFromCart({ ids })

      if (response.success) {
        orderItems.value = response.data?.items || []
      } else {
        throw new Error(response.message || '获取商品信息失败')
      }

    } else if (source === 'product') {
      const productId = route.query.productId as string
      const skuIds = route.query.skuIds as string
      const quantity = route.query.quantity as string
      const skuId = route.query.skuId as string

      if (!productId) {
        Message.error('商品信息错误')
        router.push({ name: 'UserDashboard' })
        return
      }

      // 处理 skuIds 格式（多个 SKU）- 循环调用接口并累积结果
      if (skuIds) {
        const skuList = skuIds.split(',').map(item => {
          const [id, qty] = item.split(':')
          return { skuId: Number(id), quantity: Number(qty) || 1 }
        })

        orderItems.value = [] // 先清空
        for (const item of skuList) {
          const response = await authAPI.getCheckoutItemsFromProduct({
            productId: Number(productId),
            quantity: item.quantity,
            skuId: item.skuId
          })
          if (response.success && response.data?.items) {
            // 追加到数组，不是覆盖
            orderItems.value.push(...response.data.items)
          }
        }
        if (orderItems.value.length === 0) {
          throw new Error('获取商品信息失败')
        }
      } else if (skuId && quantity) {
        // 兼容旧的单个 skuId 格式
        const response = await authAPI.getCheckoutItemsFromProduct({
          productId: Number(productId),
          quantity: Number(quantity),
          skuId: Number(skuId)
        })
        if (response.success) {
          orderItems.value = response.data?.items || []
        } else {
          throw new Error(response.message || '获取商品信息失败')
        }
      } else if (quantity) {
        // 没有 SKU 的商品
        const response = await authAPI.getCheckoutItemsFromProduct({
          productId: Number(productId),
          quantity: Number(quantity)
        })
        if (response.success) {
          orderItems.value = response.data?.items || []
        } else {
          throw new Error(response.message || '获取商品信息失败')
        }
      } else {
        Message.error('请选择规格')
        router.push({ name: 'ProductDetail', params: { productId } })
        return
      }

    } else {
      Message.error('请选择商品')
      router.push({ name: 'UserDashboard' })
      return
    }

  } catch (error: any) {
    Message.error(error.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const loadAddresses = async () => {
  try {
    const response = await authAPI.getAddresses()
    if (response.success) {
      addresses.value = response.data.addresses || []

      const defaultAddress = addresses.value.find(element => element.isDefault)
      if (defaultAddress) {
        selectedAddressId.value = defaultAddress.id
      } else {
        const firstAddress = addresses.value[0]
        if (firstAddress) {
          selectedAddressId.value = firstAddress.id
        }
      }
    }
  } catch (error) {
    Message.error('加载地址失败')
  }
}

// ==================== 地址管理 ====================
const selectAddress = (addressId: number) => {
  selectedAddressId.value = addressId
}

const editAddress = (addr: Address) => {
  showAddressPanel.value = false
  openAddressModal(addr)
}

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
    Message.error('请填写完整地区信息')
    return
  }
  if (!addressForm.value.detailAddress) {
    Message.error('请输入详细地址')
    return
  }

  try {
    let response
    if (isEditingAddress.value && editingAddressId.value !== null) {
      response = await authAPI.updateAddress({
        id: editingAddressId.value,
        ...addressForm.value
      })
    } else {
      response = await authAPI.addAddress(addressForm.value)
    }

    if (response.success) {
      Message.success(isEditingAddress.value ? '修改成功' : '添加成功')
      closeAddressModal()
      await loadAddresses()
    } else {
      throw new Error(response.message || '保存失败')
    }
  } catch (error: any) {
    Message.error(error.message || '保存失败')
  }
}

// ==================== 订单提交 ====================
const submitOrder = async () => {
  if (!selectedAddressId.value) {
    Message.error('请选择收货地址')
    return
  }

  // 防重复提交
  if (submitting.value) return

  // 二次确认
  try {
    await Message.confirm('确认提交订单？', '确认订单')
  } catch {
    return  // 用户取消
  }

  submitting.value = true

  try {
    const source = route.query.source as string

    const orderData = {
      addressId: selectedAddressId.value,
      paymentMethod: paymentMethod.value,
      source: source || 'product',
      orderItems: orderItems.value.map(item => ({
        productId: item.productId,
        quantity: item.quantity,
        skuId: item.skuId
      }))
    }

    const response = await authAPI.createOrder(orderData)

    if (response.success) {
      createdOrderId.value = response.data?.orderId
      createdOrderNumber.value = response.data?.orderNumber || ''

      // 使用 window.open 打开支付页面
      const paymentHtml = response.data.paymentHtml
      const payWindow = window.open('', '_blank')
      if (payWindow) {
        payWindow.document.write(paymentHtml)
        payWindow.document.close()
      }

      // 显示支付确认弹窗
      showPayConfirm.value = true
    } else {
      throw new Error(response.message || '创建订单失败')
    }
  } catch (error: any) {
    Message.error(error.message || '提交订单失败')
  } finally {
    submitting.value = false
  }
}

// 查询支付状态
const checkPayStatus = async () => {
  if (!createdOrderId.value || checkingPay.value) return
  checkingPay.value = true

  try {
    const response = await authAPI.getOrderDetail(createdOrderId.value)

    // 兼容两种返回格式
    const order = response.data?.order || response.data?.orderDetail?.order
    const status = order?.status

    if (status === 'PAID' || status === 'PROCESSING' || status === 'SHIPPED') {
      showPayConfirm.value = false
      Message.success('支付成功')
      router.back()
    } else if (status === 'PENDING') {
      Message.warning('暂未收到支付通知，请确认是否已完成支付')
    } else {
      Message.error(`订单状态异常：${status || '未知'}`)
    }
  } catch (error: any) {
    Message.error(error.message || '查询失败')
  } finally {
    checkingPay.value = false
  }
}

// 稍后支付
const handlePayLater = () => {
  showPayConfirm.value = false
}

// ==================== 工具函数 ====================
const formatPrice = (price: number): string => {
  if (price == null || isNaN(price)) return '0.00'
  return price.toFixed(2)
}

// ==================== 生命周期 ====================
onMounted(() => {
  if (!authStore.validateUserPermission()) return
  loadOrderData()
  loadAddresses()
})

// 离开页面时的拦截
onBeforeRouteLeave((to, from, next) => {
  if (submitting.value) {
    Message.warning('订单正在提交中，请稍候')
    next(false)  // 阻止离开
  } else {
    next()  // 允许离开
  }
})
</script>

<style scoped>
@import url('@/static/css/user/结算页.css');
</style>
