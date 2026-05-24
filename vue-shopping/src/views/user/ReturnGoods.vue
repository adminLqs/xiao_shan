<template>
  <div class="return-goods-wrapper">
    <div class="return-goods-container page-container">
      <div class="page-navbar">
        <button class="page-nav-back" @click="router.back()">
          <i class="fas fa-chevron-left"></i>
        </button>
        <div class="page-nav-title">
          <i class="fas fa-box-open"></i>
          <span>退货</span>
        </div>
        <div class="page-nav-right"></div>
      </div>

      <!-- 骨架屏 -->
      <div v-if="loading" class="skeleton-form">
        <div class="product-card skeleton-card">
          <div class="skeleton" style="width: 100px; height: 100px; border-radius: 8px;"></div>
          <div class="product-detail">
            <div class="skeleton-line long"></div>
            <div class="skeleton-line"></div>
            <div class="skeleton-line short"></div>
          </div>
        </div>
        <div class="return-section skeleton-card">
          <div class="skeleton-line short"></div>
          <div class="skeleton-line"></div>
        </div>
        <div class="return-section skeleton-card">
          <div class="skeleton-line short"></div>
          <div class="skeleton-line"></div>
        </div>
      </div>

      <!-- 商品信息卡片 -->
      <div v-else-if="orderItem" class="product-card">
      <img :src="orderItem.productImage" class="product-image" :alt="orderItem.productName" />
      <div class="product-detail">
        <div class="product-name">{{ orderItem.productName }}</div>
        <div class="product-spec" v-if="orderItem.skuName">{{ orderItem.skuName }}</div>
        <div class="product-price-row">
          <span class="product-price">¥{{ formatPrice(orderItem.price) }}</span>
          <span class="product-quantity">x{{ orderItem.quantity }}</span>
        </div>
      </div>
    </div>

    <!-- 取件信息卡片（上门取件已预约时显示） -->
    <div v-if="pickupInfo" class="pickup-info-card">
      <div class="pickup-header">
        <i class="fas fa-check-circle"></i>
        <span>取件订单已创建</span>
      </div>
      <div class="pickup-detail">
        <div class="pickup-row">
          <span>物流公司</span>
          <span>{{ pickupInfo.logisticsName }}</span>
        </div>
        <div class="pickup-row">
          <span>物流单号</span>
          <span>{{ pickupInfo.trackingNumber }}</span>
        </div>
        <div class="pickup-row" v-if="pickupInfo.pickupCode">
          <span>取件码</span>
          <span class="pickup-code">{{ pickupInfo.pickupCode }}</span>
        </div>
        <div class="pickup-row">
          <span>取件时间</span>
          <span>{{ pickupInfo.pickupTime }}</span>
        </div>
      </div>
      <button class="btn-cancel-pickup" @click="cancelPickup">取消预约</button>
    </div>

    <!-- 物流信息展示（退货已提交时显示） -->
    <div v-if="showLogisticsOnly && !pickupInfo" class="logistics-info-card">
      <div class="card-header">
        <i class="fas fa-truck"></i>
        <span class="card-title">退货物流信息</span>
      </div>
      <div class="card-body">
        <div class="logistics-item">
          <span class="logistics-label">物流公司</span>
          <span class="logistics-value">{{ refundData?.returnLogisticsName || '-' }}</span>
        </div>
        <div class="logistics-item">
          <span class="logistics-label">物流单号</span>
          <span class="logistics-value">{{ refundData?.returnTrackingNumber || '-' }}</span>
        </div>
        <div class="logistics-item">
          <span class="logistics-label">退货状态</span>
          <span class="logistics-value" :class="getReturnStatusClass(refundData?.returnStatus)">
            {{ getReturnStatusText(refundData?.returnStatus) }}
          </span>
        </div>
        <div class="logistics-item" v-if="refundData?.returnApplyTime">
          <span class="logistics-label">提交时间</span>
          <span class="logistics-value">{{ formatDateTime(refundData.returnApplyTime) }}</span>
        </div>
        <div class="logistics-item" v-if="refundData?.returnReceiveTime">
          <span class="logistics-label">商家确认时间</span>
          <span class="logistics-value">{{ formatDateTime(refundData.returnReceiveTime) }}</span>
        </div>
      </div>
      <button class="btn-primary" @click="router.back()">返回订单</button>
    </div>

    <!-- 退货方式选择 -->
    <div v-if="!showLogisticsOnly" class="return-section">
      <div class="section-title">退货方式 <span class="required">*</span></div>
      <div class="return-method-options">
        <div
          class="method-option"
          :class="{ active: returnMethod === 'PICKUP' }"
          @click="returnMethod = 'PICKUP'"
        >
          <div class="method-icon-wrapper">
            <i class="fas fa-home"></i>
          </div>
          <div class="method-info">
            <span>上门取件</span>
            <small>快递员上门取货，方便快捷</small>
          </div>
          <div class="method-check">
            <i v-if="returnMethod === 'PICKUP'" class="fas fa-check-circle"></i>
            <i v-else class="far fa-circle"></i>
          </div>
        </div>
        <div
          class="method-option"
          :class="{ active: returnMethod === 'SELF' }"
          @click="returnMethod = 'SELF'"
        >
          <div class="method-icon-wrapper">
            <i class="fas fa-truck"></i>
          </div>
          <div class="method-info">
            <span>自行寄回</span>
            <small>自行联系快递寄回商品</small>
          </div>
          <div class="method-check">
            <i v-if="returnMethod === 'SELF'" class="fas fa-check-circle"></i>
            <i v-else class="far fa-circle"></i>
          </div>
        </div>
      </div>
    </div>

    <!-- 商家收货地址（自寄时显示） -->
    <div v-if="!showLogisticsOnly && returnMethod === 'SELF'" class="return-section">
      <div class="seller-address-card">
        <div class="card-title">商家收货地址</div>
        <div class="address-detail">
          <i class="fas fa-map-marker-alt"></i>
          <span>{{ sellerAddress.address || '暂无' }}</span>
        </div>
        <div class="contact-phone">
          <i class="fas fa-phone"></i>
          <span>{{ sellerAddress.contactPhone || '暂无' }}</span>
        </div>
      </div>
    </div>

    <!-- 上门取件 - 地址选择 -->
    <div v-if="!showLogisticsOnly && returnMethod === 'PICKUP'" class="return-section">
      <div class="section-title">取件地址 <span class="required">*</span></div>
      <div class="address-selector" @click="selectPickupAddress">
        <div v-if="pickupAddress" class="selected-address">
          <div class="address-detail">
            <div class="address-recipient">
              <span>{{ pickupAddress.recipientName }}</span>
              <span>{{ pickupAddress.recipientPhone }}</span>
            </div>
            <div class="address-full">{{ formatPickupAddress(pickupAddress) }}</div>
          </div>
          <i class="fas fa-chevron-right"></i>
        </div>
        <div v-else class="address-placeholder">
          <i class="fas fa-map-marker-alt"></i>
          <span>请选择取件地址</span>
        </div>
      </div>
    </div>

    <!-- 上门取件 - 取件时间选择 -->
    <div v-if="!showLogisticsOnly && returnMethod === 'PICKUP'" class="return-section">
      <div class="section-title">取件时间 <span class="required">*</span></div>

      <!-- 日期选择 -->
      <div class="form-group">
        <label class="form-label">取件日期 <span class="required">*</span></label>
        <input
          type="date"
          v-model="pickupDate"
          class="form-input"
          :min="minPickupDate"
          @change="onPickupDateChange"
        />
      </div>

      <!-- 时间段选择 -->
      <div class="form-group" v-if="pickupDate">
        <label class="form-label">取件时间段 <span class="required">*</span></label>
        <div class="time-slots">
          <div
            v-for="slot in availableTimeSlots"
            :key="slot.value"
            class="time-slot"
            :class="{ active: pickupTime === slot.value }"
            @click="pickupTime = slot.value"
          >
            {{ slot.label }}
          </div>
        </div>
      </div>

      <!-- 取件码提示 -->
      <div class="pickup-tip">
        <i class="fas fa-info-circle"></i>
        <span>提交预约后，取件码将通过短信发送到您的手机，请留意查收</span>
      </div>
    </div>

    <!-- 退货物流信息（仅自寄需要填写） -->
    <div v-if="!showLogisticsOnly && returnMethod === 'SELF'" class="return-section">
      <div class="section-title">退货物流 <span class="required">*</span></div>
      <div class="logistics-form">
        <div class="form-group">
          <label class="form-label">物流公司 <span class="required">*</span></label>
          <select v-model="logisticsCompany" class="form-select">
            <option value="">请选择物流公司</option>
            <option value="顺丰速运">顺丰速运</option>
            <option value="中通快递">中通快递</option>
            <option value="圆通速递">圆通速递</option>
            <option value="韵达快递">韵达快递</option>
            <option value="申通快递">申通快递</option>
            <option value="极兔速递">极兔速递</option>
            <option value="京东物流">京东物流</option>
            <option value="邮政EMS">邮政EMS</option>
            <option value="其他">其他</option>
          </select>
        </div>
        <div class="form-group">
          <label class="form-label">物流单号 <span class="required">*</span></label>
          <input
            type="text"
            v-model="trackingNumber"
            class="form-input"
            placeholder="请输入物流单号"
            maxlength="30"
          />
        </div>
      </div>
    </div>



    <!-- 提交按钮 -->
    <button
      v-if="!showLogisticsOnly"
      class="submit-btn"
      :disabled="!canSubmit || submitting"
      @click="submitReturn"
    >
      <i v-if="submitting" class="fas fa-spinner fa-spin"></i>
      {{ submitting ? '提交中...' : '提交退货' }}
    </button>

    <!-- 提示信息 -->
    <div v-if="!showLogisticsOnly" class="tips-section">
      <div class="tips-title">
        <i class="fas fa-info-circle"></i>
        退货须知
      </div>
      <ul class="tips-list">
        <li>请在提交退货后 <strong>7天内</strong> 完成退货操作</li>
        <li>上门取件需保持商品完整，便于快递员检查</li>
        <li>自寄请保留物流凭证和单号</li>
        <li>商家确认收货后，退款将原路返回</li>
      </ul>
    </div>

    <!-- 底部留空 -->
    <div class="bottom-space"></div>

    <!-- 地址选择弹窗 -->
    <div v-if="showAddressPicker" class="modal-overlay" @click="showAddressPicker = false">
      <div class="modal-content address-modal" @click.stop>
        <div class="modal-header">
          <h3>选择取件地址</h3>
          <div class="header-right">
            <button class="btn-manage-address" @click="goToManageAddress">
              <i class="fas fa-cog"></i> 管理
            </button>
            <button class="modal-close" @click="showAddressPicker = false">
              <i class="fas fa-times"></i>
            </button>
          </div>
        </div>
        <div class="modal-body">
          <div v-if="userAddresses.length === 0" class="empty-state">
            <i class="fas fa-map-marker-alt"></i>
            <p>暂无收货地址</p>
          </div>
          <div v-else class="address-list">
            <div
              v-for="address in userAddresses"
              :key="address.id"
              class="address-item"
              :class="{ selected: pickupAddress?.id === address.id }"
              @click="confirmAddress(address)"
            >
              <div class="address-radio">
                <i v-if="pickupAddress?.id === address.id" class="fas fa-check-circle"></i>
                <i v-else class="far fa-circle"></i>
              </div>
              <div class="address-content">
                <div class="address-header">
                  <span class="address-name">{{ address.recipientName }}</span>
                  <span class="address-phone">{{ address.recipientPhone }}</span>
                  <span v-if="address.isDefault" class="default-tag">默认</span>
                </div>
                <div class="address-body">{{ formatPickupAddress(address) }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onActivated } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import Message from '@/utils/message'
import { authAPI } from '@/api/authAPI'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const currentUserId = computed(() => authStore.userId)

const orderItemId = computed(() => {
  const id = route.query.orderItemId as string
  return id ? Number(id) : null
})

const refundId = computed(() => {
  return route.query.refundId as string
})

const loading = ref(true)
const orderItem = ref<any>(null)
const showLogisticsOnly = ref(false)
const refundData = ref<any>(null)
const pickupInfo = ref<any>(null)

const sellerAddress = ref({ address: '', contactPhone: '' })

const returnMethod = ref<'PICKUP' | 'SELF'>('PICKUP')
const logisticsCompany = ref('')
const trackingNumber = ref('')
const submitting = ref(false)

const userAddresses = ref<any[]>([])
const pickupAddress = ref<any>(null)
const showAddressPicker = ref(false)
const pickupDate = ref('')
const pickupTime = ref('')

const minPickupDate = computed(() => {
  const tomorrow = new Date()
  tomorrow.setDate(tomorrow.getDate() + 1)
  return tomorrow.toISOString().split('T')[0]
})

const availableTimeSlots = computed(() => {
  if (!pickupDate.value) return []
  const date = new Date(pickupDate.value)
  const day = date.getDay()
  const isWeekend = day === 0 || day === 6

  if (isWeekend) {
    return [
      { label: '9:00-12:00', value: 'WEEKEND_MORNING' },
      { label: '14:00-18:00', value: 'WEEKEND_AFTERNOON' }
    ]
  }
  return [
    { label: '9:00-12:00', value: 'WEEKDAY_MORNING' },
    { label: '14:00-18:00', value: 'WEEKDAY_AFTERNOON' }
  ]
})

const onPickupDateChange = () => {
  pickupTime.value = ''
}

const canSubmit = computed(() => {
  if (returnMethod.value === 'PICKUP') {
    return pickupAddress.value?.id && pickupDate.value && pickupTime.value
  }
  if (returnMethod.value === 'SELF') {
    return logisticsCompany.value && trackingNumber.value
  }
  return false
})

const formatPrice = (price: number): string => {
  if (price == null || isNaN(price)) return '0.00'
  return price.toFixed(2)
}

const formatPickupAddress = (address: any): string => {
  if (!address) return ''
  const parts = []
  if (address.province) parts.push(address.province)
  if (address.city) parts.push(address.city)
  if (address.district) parts.push(address.district)
  if (address.detail) parts.push(address.detail)
  return parts.join(' ')
}



const loadSellerAddress = async () => {
  try {
    const sellerId = orderItem.value?.sellerId
    if (!sellerId) return
    const res = await authAPI.getSellerAddress(sellerId)
    if (res.success && res.data) {
      sellerAddress.value = res.data
    }
  } catch {}
}

const selectPickupAddress = async () => {
  if (userAddresses.value.length === 0) {
    Message.warning('请先添加收货地址')
    router.push({ name: 'AddressList', query: { from: 'ReturnGoods' } })
    return
  }
  showAddressPicker.value = true
}

const confirmAddress = (address: any) => {
  if (!address) return
  pickupAddress.value = address
  showAddressPicker.value = false
  Message.success('取件地址已选择')
}

const goBack = () => {
  router.back()
}

const goToManageAddress = () => {
  showAddressPicker.value = false
  router.push({ name: 'UserAddresses' })
}

const getReturnStatusText = (status: string | undefined): string => {
  const map: Record<string, string> = {
    'RETURNING': '退货中',
    'RECEIVED': '已确认收货',
    'SUCCESS': '退款完成'
  }
  return map[status || ''] || '-'
}

const getReturnStatusClass = (status: string | undefined): string => {
  const map: Record<string, string> = {
    'RETURNING': 'status-returning',
    'RECEIVED': 'status-received',
    'SUCCESS': 'status-success'
  }
  return map[status || ''] || ''
}

const getPickupTimeLabel = (timeValue: string): string => {
  const map: Record<string, string> = {
    'WEEKDAY_MORNING': '9:00-12:00',
    'WEEKDAY_AFTERNOON': '14:00-18:00',
    'WEEKEND_MORNING': '9:00-12:00',
    'WEEKEND_AFTERNOON': '14:00-18:00'
  }
  return map[timeValue] || timeValue
}

const cancelPickup = async () => {
  const confirmed = await Message.confirm('确定要取消预约取件吗？')
  if (!confirmed) return

  try {
    const res = await authAPI.cancelPickupOrder({
      refundId: refundId.value,
      logisticCode: pickupInfo.value?.trackingNumber,
      shipperCode: pickupInfo.value?.shipperCode || 'SF',
      orderCode: 'REFUND_' + refundId.value
    })

    if (res.success) {
      Message.success('已取消预约')
      pickupInfo.value = null
      // 重置表单状态，允许用户重新提交
      pickupDate.value = ''
      pickupTime.value = ''
    } else {
      throw new Error(res.message || '取消失败')
    }
  } catch (error: any) {
    Message.error(error.message || '取消失败')
  }
}

const formatDateTime = (dateStr: string | undefined): string => {
  if (!dateStr) return '-'
  try {
    const date = new Date(dateStr)
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    const hours = String(date.getHours()).padStart(2, '0')
    const minutes = String(date.getMinutes()).padStart(2, '0')
    return `${year}-${month}-${day} ${hours}:${minutes}`
  } catch {
    return '-'
  }
}

const loadData = async () => {
  loading.value = true
  if (!orderItemId.value || !refundId.value) {
    Message.error('参数错误')
    router.back()
    loading.value = false
    return
  }

  try {
    const refundDetailResponse = await authAPI.getRefundDetail(Number(refundId.value))
    if (!refundDetailResponse.success || !refundDetailResponse.data) {
      Message.error('退款记录不存在')
      router.back()
      loading.value = false
      return
    }

    refundData.value = refundDetailResponse.data
    const { refundType, refundStatus, returnStatus } = refundDetailResponse.data

    if (refundType !== 'AFTER_SALE') {
      Message.error('非售后订单，无需退货')
      router.back()
      loading.value = false
      return
    }

    if (refundStatus === 'PROCESSING') {
      Message.error('商家尚未同意退货')
      router.back()
      loading.value = false
      return
    }

    if (refundStatus === 'FAILED') {
      Message.error('退款已被拒绝')
      router.back()
      loading.value = false
      return
    }

    if (returnStatus === 'RETURNING' || returnStatus === 'RECEIVED') {
      router.replace({
        name: 'UserLogistics',
        query: { refundId: String(refundId.value), type: 'return' }
      })
      loading.value = false
      return
    }

    if (refundStatus === 'SUCCESS') {
      showLogisticsOnly.value = true
    } else if (refundStatus !== 'APPROVED' && refundStatus !== 'WAITING_RETURN') {
      router.replace({
        name: 'UserLogistics',
        query: { refundId: String(refundId.value), type: 'return' }
      })
      loading.value = false
      return
    }

    const returnInfoResponse = await authAPI.getReturnInfo(Number(refundId.value))

    if (!returnInfoResponse.success || !returnInfoResponse.data) {
      throw new Error(returnInfoResponse.message || '获取退货信息失败')
    }

    const returnInfo = returnInfoResponse.data

    if (returnInfo.sellerAddress) {
      sellerAddress.value = returnInfo.sellerAddress
    }

    if (returnInfo.userAddresses) {
      userAddresses.value = returnInfo.userAddresses
    }

    if (returnInfo.returnMethod) {
      returnMethod.value = returnInfo.returnMethod
    }

    const itemResponse = await authAPI.getOrderItemDetail(orderItemId.value)
    if (itemResponse.success && itemResponse.data?.orderItem) {
      orderItem.value = itemResponse.data.orderItem
      loadSellerAddress()
    }

  } catch (error: any) {
    Message.error(error.message || '加载失败')
    router.back()
  } finally {
    loading.value = false
  }
}

const submitReturn = async () => {
  if (submitting.value) return
  if (!canSubmit.value) return
  if (!refundId.value) {
    Message.error('参数错误')
    return
  }

  submitting.value = true

  try {
    const submitData: any = {
      returnMethod: returnMethod.value
    }

    if (returnMethod.value === 'SELF') {
      submitData.returnLogisticsName = logisticsCompany.value
      submitData.returnTrackingNumber = trackingNumber.value
    }

    if (returnMethod.value === 'PICKUP') {
      if (!pickupAddress.value || !pickupAddress.value.id) {
        Message.error('请选择取件地址')
        submitting.value = false
        return
      }
      if (!pickupDate.value) {
        Message.error('请选择取件日期')
        submitting.value = false
        return
      }
      if (!pickupTime.value) {
        Message.error('请选择取件时间段')
        submitting.value = false
        return
      }
      submitData.addressId = pickupAddress.value.id
      submitData.pickupDate = pickupDate.value
      submitData.pickupTime = pickupTime.value
    }

    const response = await authAPI.submitReturn(Number(refundId.value), submitData)

    if (response.success && response.data) {
      Message.success('退货信息已提交')

      // 提取取件信息
      const { trackingNumber, logisticsName, pickupCode } = response.data

      // 保存取件信息到本地
      pickupInfo.value = {
        trackingNumber,
        logisticsName,
        pickupCode,
        pickupTime: pickupDate.value + ' ' + getPickupTimeLabel(pickupTime.value),
        shipperCode: 'SF'
      }

      // 提交成功后跳转到物流页 - 用户端
      router.replace({
        name: 'UserLogistics',
        query: { refundId: String(refundId.value), type: 'return' }
      })
    } else {
      throw new Error(response.message || '提交失败')
    }
  } catch (error: any) {
    Message.error(error.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadData()
})

onActivated(() => {
  loadData()
})
</script>

<style scoped>
@import url('@/static/css/user/退货页.css');
</style>
