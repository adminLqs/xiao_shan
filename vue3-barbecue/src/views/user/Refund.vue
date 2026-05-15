<template>
  <div class="refund-page black-theme">
    <!-- 顶部导航 -->
    <div class="refund-header">
      <button class="back-btn" @click="goBack">
        <span class="back-icon">←</span>
      </button>
      <h1 class="header-title">申请退款</h1>
      <div class="header-placeholder"></div>
    </div>

    <div class="refund-content">
      <!-- 订单信息卡片 -->
      <div class="order-info-card">
        <div class="order-number">
          <span class="label">订单编号</span>
          <span class="value">{{ orderNumber }}</span>
          <button class="copy-btn" @click="copyOrderNumber">复制</button>
        </div>
        <div class="order-amount">
          <span class="label">订单金额</span>
          <span class="value">¥{{ formatPrice(orderAmount) }}</span>
        </div>
        <div class="refunded-amount" v-if="refundedAmount > 0">
          <span class="label">已退款</span>
          <span class="value">-¥{{ formatPrice(refundedAmount) }}</span>
        </div>
        <div class="available-amount">
          <span class="label">可退款金额</span>
          <span class="value highlight">¥{{ formatPrice(availableAmount) }}</span>
        </div>
      </div>

      <!-- 商品列表 -->
      <div class="product-card">
        <h3 class="card-title">商品清单</h3>
        <div 
          v-for="item in orderItems" 
          :key="item.id"
          class="order-item"
        >
          <div class="item-image">
            <img v-if="item.image" :src="item.image" :alt="item.name" />
            <span v-else class="item-placeholder">🥩</span>
          </div>
          <div class="item-info">
            <div class="item-name">{{ item.name }}</div>
            <div class="item-spec">{{ item.spec || '' }}</div>
          </div>
          <div class="item-price">
            <div class="price">¥{{ formatPrice(item.price) }}</div>
            <div class="quantity">x{{ item.quantity }}</div>
          </div>
        </div>
      </div>

      <!-- 退款表单 -->
      <div class="form-card">
        <h3 class="card-title">退款信息</h3>
        
        <!-- 退款金额 -->
        <div class="form-group">
          <label class="form-label">退款金额 <span class="required">*</span></label>
          <div class="amount-input-wrapper">
            <span class="currency">¥</span>
            <input 
              type="number" 
              v-model="refundAmount" 
              :max="availableAmount"
              :min="0.01"
              step="0.01"
              class="amount-input"
              placeholder="请输入退款金额"
              @input="validateAmount"
            />
          </div>
          <div class="amount-hint" v-if="refundAmount > availableAmount">
            <span class="error">退款金额不能超过可退金额</span>
          </div>
          <div class="amount-hint" v-else-if="refundAmount > 0">
            <span>将退还 ¥{{ formatPrice(refundAmount) }} 到您的支付账户</span>
          </div>
        </div>

        <!-- 退款原因 -->
        <div class="form-group">
          <label class="form-label">退款原因 <span class="required">*</span></label>
          <div class="reason-select">
            <div 
              v-for="reason in refundReasons" 
              :key="reason.value"
              class="reason-option"
              :class="{ active: refundReason === reason.value }"
              @click="refundReason = reason.value"
            >
              <span class="reason-icon">{{ reason.icon }}</span>
              <span class="reason-text">{{ reason.label }}</span>
              <span class="reason-check" v-if="refundReason === reason.value">✓</span>
            </div>
          </div>
          <textarea 
            v-model="customReason" 
            class="reason-textarea"
            rows="3"
            placeholder="或填写其他退款原因..."
            v-if="refundReason === 'other'"
          ></textarea>
        </div>

        <!-- 退款说明 -->
        <div class="form-group">
          <label class="form-label">退款说明</label>
          <div class="refund-tips">
            <p>💡 退款说明：</p>
            <ul>
              <li>退款金额将原路返回您的支付账户</li>
              <li>退款预计1-3个工作日到账</li>
              <li>如有疑问请联系客服</li>
            </ul>
          </div>
        </div>
      </div>

      <!-- 底部按钮 -->
      <div class="refund-footer">
        <button class="submit-btn" @click="submitRefund" :disabled="!canSubmit">
          <span v-if="!submitting">提交退款申请</span>
          <span v-else class="submitting">
            <span class="spinner"></span>
            提交中...
          </span>
        </button>
        <button class="cancel-btn" @click="goBack">取消</button>
      </div>
    </div>

    <!-- 提交结果弹窗 -->
    <transition name="fade">
      <div v-if="showResultModal" class="result-modal">
        <div class="result-content" :class="resultSuccess ? 'success' : 'fail'">
          <div class="result-icon">{{ resultSuccess ? '✅' : '❌' }}</div>
          <h3 class="result-title">{{ resultSuccess ? '提交成功' : '提交失败' }}</h3>
          <p class="result-text">{{ resultMessage }}</p>
          <button class="result-btn" @click="closeResultModal">确定</button>
        </div>
      </div>
    </transition>
  </div>
</template>

<script lang="ts" setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showToast } from 'vant'
import 'vant/es/toast/style'
import { authAPI } from '@/api/authAPI'

// ==================== 路由 ====================
const router = useRouter()
const route = useRoute()

// ==================== 响应式数据 ====================
const loading = ref(false)
const submitting = ref(false)
const showResultModal = ref(false)
const resultSuccess = ref(false)
const resultMessage = ref('')

// 订单信息
const orderNumber = ref(route.query.orderNumber as string || '')
const orderAmount = ref(0)
const refundedAmount = ref(0)
const orderItems = ref<any[]>([])
const orderStatus = ref('')

// 退款信息
const refundAmount = ref(0)
const refundReason = ref('')
const customReason = ref('')

// 退款原因选项
const refundReasons = [
  { value: 'not_wanted', label: '不想要了', icon: '😕' },
  { value: 'wrong_product', label: '商品买错了', icon: '🤔' },
  { value: 'quality_issue', label: '商品质量问题', icon: '😤' },
  { value: 'delivery_issue', label: '配送问题', icon: '🚚' },
  { value: 'other', label: '其他原因', icon: '📝' }
]

// ==================== 计算属性 ====================
const availableAmount = computed(() => {
  return orderAmount.value - refundedAmount.value
})

const canSubmit = computed(() => {
  if (refundAmount.value <= 0) return false
  if (refundAmount.value > availableAmount.value) return false
  if (!refundReason.value) return false
  if (refundReason.value === 'other' && !customReason.value.trim()) return false
  return true
})

const finalReason = computed(() => {
  if (refundReason.value === 'other') {
    return customReason.value
  }
  const reason = refundReasons.find(r => r.value === refundReason.value)
  return reason ? reason.label : ''
})

// ==================== 工具方法 ====================
const formatPrice = (price: number): string => {
  return price.toFixed(2)
}

const validateAmount = () => {
  if (refundAmount.value > availableAmount.value) {
    refundAmount.value = availableAmount.value
  }
  if (refundAmount.value < 0) {
    refundAmount.value = 0
  }
}

const copyOrderNumber = () => {
  navigator.clipboard.writeText(orderNumber.value).then(() => {
    showToast('订单号已复制')
  }).catch(() => {
    showToast('复制失败')
  })
}

const goBack = () => {
  router.back()
}

// ==================== 提交退款 ====================
const submitRefund = async () => {
  if (!canSubmit.value) return
  
  submitting.value = true
  
  try {
    const response = await authAPI.refundOrder({
      orderNumber: orderNumber.value,
      refundAmount: refundAmount.value,
      refundReason: finalReason.value
    })
    const data = response.data || response
    
    if (data.success) {
      resultSuccess.value = true
      resultMessage.value = '退款申请已提交，预计1-3个工作日到账'
      showResultModal.value = true
    } else {
      throw new Error(data.message || '退款失败')
    }
  } catch (error: any) {
    console.error('退款失败:', error)
    resultSuccess.value = false
    resultMessage.value = error.message || '退款失败，请重试'
    showResultModal.value = true
  } finally {
    submitting.value = false
  }
}

// 关闭结果弹窗
const closeResultModal = () => {
  showResultModal.value = false
  if (resultSuccess.value) {
    // 跳转到订单详情页
    router.push({
      path: '/order/detail',
      query: { orderNumber: orderNumber.value }
    })
  }
}

// ==================== 加载订单信息 ====================
const loadOrderInfo = async () => {
  if (!orderNumber.value) {
    showToast('订单号不存在')
    router.back()
    return
  }
  
  loading.value = true
  
  try {
    const response = await authAPI.getOrderDetail(orderNumber.value)
    const data = response.data || response
    
    if (!data.success) {
      throw new Error(data.message || '获取订单信息失败')
    }
    
    const order = data.order
    
    // 检查订单状态
    if (order.status !== 'PAID') {
      showToast('该订单无法退款')
      router.back()
      return
    }
    
    orderAmount.value = order.totalAmount || 0
    refundedAmount.value = order.refundAmount || 0
    
    // 检查是否已全额退款
    if (refundedAmount.value >= orderAmount.value) {
      showToast('该订单已全额退款')
      router.back()
      return
    }
    
    // 设置默认退款金额
    refundAmount.value = availableAmount.value
    
    // 商品列表
    if (data.orderItems && data.orderItems.length > 0) {
      orderItems.value = data.orderItems.map((item: any) => ({
        id: item.productId,
        name: item.productName,
        price: item.price,
        quantity: item.quantity,
        image: item.productImage,
        spec: ''
      }))
    }
    
  } catch (error) {
    console.error('加载订单信息失败:', error)
    showToast('加载失败，请重试')
    router.back()
  } finally {
    loading.value = false
  }
}

// ==================== 生命周期 ====================
onMounted(() => {
  loadOrderInfo()
})
</script>

<style scoped>
@import url('@/static/css/用户退款页.css');
</style>