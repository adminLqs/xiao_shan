<template>
  <div class="payment-container black-theme">
    <!-- 顶部导航 -->
    <div class="payment-header">
      <button class="back-btn" @click="goBack">
        <span class="back-icon">←</span>
      </button>
      <h1 class="header-title">订单支付</h1>
      <div class="header-placeholder"></div>
    </div>

    <div class="payment-content">
      <!-- 订单信息卡片 -->
      <div class="order-info-card">
        <div class="order-number">
          <span class="label">订单编号</span>
          <span class="value">{{ orderNumber }}</span>
          <button class="copy-btn" @click="copyOrderNumber">复制</button>
        </div>
        <div class="order-amount">
          <span class="label">支付金额</span>
          <span class="value">¥{{ formatPrice(amount) }}</span>
        </div>
        <div class="order-timer" v-if="paymentStatus === 'PENDING'">
          <span class="timer-icon">⏱️</span>
          <span class="timer-text">剩余支付时间</span>
          <span class="timer-count">{{ formatTime(countdown) }}</span>
        </div>
      </div>

      <!-- 支付方式选择 -->
      <div class="payment-methods">
        <h3 class="section-title">选择支付方式</h3>
        
        <div 
          class="payment-method"
          :class="{ active: selectedMethod === 'WECHAT' }"
          @click="selectedMethod = 'WECHAT'"
        >
          <div class="method-left">
            <span class="method-icon wechat">
              <svg class="icon-svg" viewBox="0 0 1024 1024" width="28" height="28">
                <path d="M864.32 465.28c-64.32-45.12-147.52-68.48-239.36-68.48-104.96 0-198.72 28.8-263.36 81.92-58.88 48-92.16 114.56-92.16 188.16 0 76.8 36.48 147.2 100.48 196.48 58.24 44.8 135.36 68.8 219.84 68.8 41.28 0 81.6-5.76 119.36-16.96l95.68 32.96-22.72-79.68c53.44-45.12 85.12-104.96 85.12-170.88 0-60.16-24-116.8-67.84-162.56z m-271.36 179.2c-19.2 0-34.56-15.36-34.56-34.56 0-19.2 15.36-34.56 34.56-34.56s34.56 15.36 34.56 34.56-15.36 34.56-34.56 34.56z m125.44 0c-19.2 0-34.56-15.36-34.56-34.56 0-19.2 15.36-34.56 34.56-34.56s34.56 15.36 34.56 34.56-15.36 34.56-34.56 34.56z m174.72-355.84c-49.92-40.96-117.76-63.36-190.4-63.36-86.4 0-164.48 27.52-221.44 76.8-63.36 54.4-98.56 128-98.56 208 0 83.2 39.04 158.72 107.52 212.48 59.52 46.72 136.96 71.68 220.16 71.68 32.64 0 64.64-4.48 94.72-13.44l91.2 30.08-20.48-74.88c42.88-43.52 67.84-99.84 67.84-157.44 0-69.12-28.16-134.4-78.72-184.32z m-177.92 114.56c-14.72 0-26.56-11.84-26.56-26.56s11.84-26.56 26.56-26.56 26.56 11.84 26.56 26.56-11.84 26.56-26.56 26.56z m124.8 0c-14.72 0-26.56-11.84-26.56-26.56s11.84-26.56 26.56-26.56 26.56 11.84 26.56 26.56-11.84 26.56-26.56 26.56z" fill="#07C160"/>
              </svg>
            </span>
            <div class="method-info">
              <span class="method-name">微信支付</span>
              <span class="method-desc">推荐使用微信支付</span>
            </div>
          </div>
          <div class="method-right">
            <span class="method-check" v-if="selectedMethod === 'WECHAT'">✓</span>
          </div>
        </div>

        <div 
          class="payment-method"
          :class="{ active: selectedMethod === 'ALIPAY' }"
          @click="selectedMethod = 'ALIPAY'"
        >
          <div class="method-left">
            <span class="method-icon alipay">
              <svg class="icon-svg" viewBox="0 0 1024 1024" width="28" height="28">
                <path d="M789.333333 298.666667H234.666667c-25.6 0-46.933333 21.333333-46.933334 46.933333v332.8c0 25.6 21.333333 46.933333 46.933334 46.933333h213.333333l-64 106.666667 128-106.666667h277.333334c25.6 0 46.933333-21.333333 46.933333-46.933333V345.6c0-25.6-21.333333-46.933333-46.933333-46.933333z m-345.6 256h-128v-42.666667h128v42.666667z m170.667334 0h-128v-42.666667h128v42.666667z m170.666666 0h-128v-42.666667h128v42.666667z" fill="#1677FF"/>
              </svg>
            </span>
            <div class="method-info">
              <span class="method-name">支付宝</span>
              <span class="method-desc">支付宝安全支付</span>
            </div>
          </div>
          <div class="method-right">
            <span class="method-check" v-if="selectedMethod === 'ALIPAY'">✓</span>
          </div>
        </div>
      </div>

      <!-- 支付按钮 -->
      <div class="payment-actions">
        <button 
          class="pay-btn" 
          @click="handlePayment"
          :disabled="!canPay || paying"
        >
          <span v-if="!paying">确认支付 ¥{{ formatPrice(amount) }}</span>
          <span v-else class="paying">
            <span class="spinner"></span>
            支付处理中...
          </span>
        </button>
        
        <button v-if="paymentStatus === 'PENDING'" class="cancel-btn" @click="cancelPayment">
          取消支付
        </button>
      </div>
    </div>

    <!-- 支付成功弹窗 -->
    <transition name="fade">
      <div v-if="showSuccessModal" class="result-modal">
        <div class="modal-content success">
          <div class="modal-icon">🎉</div>
          <h3 class="modal-title">支付成功</h3>
          <p class="modal-message">您的订单已支付成功</p>
          <div class="modal-actions">
            <button class="modal-btn primary" @click="goToOrderDetail">查看订单</button>
            <button class="modal-btn default" @click="backToHome">返回首页</button>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script lang="ts" setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showToast } from 'vant'
import 'vant/es/toast/style'
import { authAPI } from '@/api/authAPI'

// ==================== 路由实例 ====================
const router = useRouter()
const route = useRoute()

// ==================== 响应式数据 ====================
const paying = ref(false)
const paymentStatus = ref<'PENDING' | 'SUCCESS' | 'FAILED'>('PENDING')
const selectedMethod = ref<'WECHAT' | 'ALIPAY'>('ALIPAY')
const showSuccessModal = ref(false)

const orderNumber = ref(route.query.orderNumber as string || '')
const amount = ref(parseFloat(route.query.amount as string) || 0)

const countdown = ref(900)

// 定时器变量
let countdownTimer: number | null = null

// ==================== 计算属性 ====================
const canPay = computed(() => {
  return selectedMethod.value && paymentStatus.value === 'PENDING' && !paying.value
})

// ==================== 工具函数 ====================
const formatPrice = (price: number): string => price.toFixed(2)

const formatTime = (seconds: number): string => {
  const minutes = Math.floor(seconds / 60)
  const remainingSeconds = seconds % 60
  return `${minutes.toString().padStart(2, '0')}:${remainingSeconds.toString().padStart(2, '0')}`
}

// ==================== 查询订单状态 ====================
/**
 * 查询订单支付状态
 * @returns 是否已支付
 */
const checkOrderStatus = async (): Promise<boolean> => {
  try {
    const response = await authAPI.queryPaymentResult(orderNumber.value)
    const data = response.data || response
    
    if (data.success && data.paid) {
      return true
    }
    return false
  } catch (error) {
    console.error('查询订单状态失败:', error)
    return false
  }
}

// ==================== 支付成功处理 ====================
const handlePaymentSuccess = (): void => {
  // 清空购物车
  localStorage.removeItem('cartItems')
  // 停止倒计时
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
  // 显示成功弹窗
  showSuccessModal.value = true
}

/**
 * 跳转到订单详情
 */
const goToOrderDetail = (): void => {
  showSuccessModal.value = false
  router.push({
    path: '/order/detail',
    query: { orderNumber: orderNumber.value }
  })
}

/**
 * 返回首页
 */
const backToHome = (): void => {
  showSuccessModal.value = false
  router.push({ name: 'UserDashboard' })
}

// ==================== 支付主流程 ====================
const handlePayment = async (): Promise<void> => {
  if (!canPay.value) return
  
  paying.value = true
  
  try {
    const response = await authAPI.paymentOrder({
      orderNumber: orderNumber.value,
      amount: amount.value,
      paymentMethod: selectedMethod.value
    })
    
    const data = response.data || response
    
    if (!data.success) {
      throw new Error(data.message || '创建支付订单失败')
    }

    // 支付宝支付：跳转到支付页面
    if (data.pageHtml) {
      // 将当前页面跳转到支付宝支付页面
      document.write(data.pageHtml)
      document.close()
    }
    
  } catch (error: any) {
    console.error('支付失败:', error)
    showToast(error.message || '支付处理失败，请重试')
  } finally {
    paying.value = false
  }
}

// ==================== 倒计时 ====================
const startCountdown = (): void => {
  if (countdownTimer) clearInterval(countdownTimer)
  
  countdownTimer = setInterval(() => {
    if (countdown.value > 0) {
      countdown.value--
    } else {
      clearInterval(countdownTimer!)
      countdownTimer = null
      // 超时取消订单
      authAPI.cancelOrder(orderNumber.value)
      showToast('支付超时，订单已取消')
      router.push({ name: 'UserDashboard' })
    }
  }, 1000)
}

// ==================== 用户交互方法 ====================
const copyOrderNumber = (): void => {
  navigator.clipboard.writeText(orderNumber.value).then(() => {
    showToast('订单号已复制')
  }).catch(() => {
    showToast('复制失败')
  })
}

const goBack = (): void => {
  if (paymentStatus.value === 'PENDING') {
    authAPI.cancelOrder(orderNumber.value)
    if (countdownTimer) clearInterval(countdownTimer)
  }
  router.back()
}

const cancelPayment = (): void => {
  authAPI.cancelOrder(orderNumber.value)
  if (countdownTimer) clearInterval(countdownTimer)
  router.push({ name: 'UserDashboard' })
}

// ==================== 生命周期 ====================
onMounted(async () => {
  if (!orderNumber.value || !amount.value || amount.value <= 0) {
    showToast('订单信息不存在')
    router.back()
    return
  }
  
  console.log('支付页面加载，订单号:', orderNumber.value, '金额:', amount.value)
  
  // 先查询订单是否已支付
  const isPaid = await checkOrderStatus()
  
  if (isPaid) {
    // 已支付，直接显示成功弹窗
    handlePaymentSuccess()
  } else {
    // 未支付，开始倒计时
    startCountdown()
  }
})

onUnmounted(() => {
  if (countdownTimer) clearInterval(countdownTimer)
})
</script>

<style scoped>
@import url("@/static/css/user/支付页.css");

/* 支付成功弹窗样式 */
.result-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 24px;
  padding: 32px 24px;
  text-align: center;
  width: 85%;
  max-width: 320px;
}

.modal-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.modal-title {
  font-size: 20px;
  font-weight: 600;
  margin: 0 0 8px 0;
  color: #333;
}

.modal-message {
  font-size: 14px;
  color: #666;
  margin: 0 0 24px 0;
}

.modal-actions {
  display: flex;
  gap: 12px;
}

.modal-btn {
  flex: 1;
  padding: 12px;
  border-radius: 12px;
  font-size: 14px;
  cursor: pointer;
  border: none;
  transition: all 0.2s;
}

.modal-btn.primary {
  background: #e65100;
  color: white;
}

.modal-btn.primary:hover {
  background: #bf360c;
}

.modal-btn.default {
  background: #f5f5f5;
  color: #666;
  border: 1px solid #ddd;
}

.modal-btn.default:hover {
  background: #eee;
}

/* 图标样式 */
.method-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: #f5f5f5;
}

.method-icon.wechat {
  background: #e8f5e9;
}

.method-icon.alipay {
  background: #e8f0fe;
}

.icon-svg {
  display: block;
}

/* 渐隐动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>