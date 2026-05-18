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

    <!-- 支付确认弹窗 -->
    <transition name="fade">
      <div v-if="showPaymentConfirm" class="result-modal">
        <div class="modal-content">
          <div class="modal-icon">💳</div>
          <h3 class="modal-title">支付确认</h3>
          <p class="modal-message">请确认您是否已完成支付？</p>
          <div class="modal-actions" style="margin-top: 20px;">
            <button class="modal-btn primary" @click="confirmPaymentComplete">
              已完成支付
            </button>
            <button class="modal-btn default" @click="cancelPaymentConfirm">
              未支付
            </button>
          </div>
        </div>
      </div>
    </transition>

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
  import { Toast, Dialog } from '@/utils/vant'
  import { authAPI } from '@/api/authAPI'

  // ==================== 类型定义 ====================
  type PaymentMethod = 'WECHAT' | 'ALIPAY'
  type PaymentStatus = 'PENDING' | 'SUCCESS' | 'FAILED'

  // ==================== 路由实例 ====================
  const router = useRouter()
  const route = useRoute()

  // ==================== 常量定义 ====================
  const PAYMENT_TIMEOUT = 900

  // ==================== 响应式数据 ====================
  const paying = ref<boolean>(false)
  const paymentStatus = ref<PaymentStatus>('PENDING')
  const selectedMethod = ref<PaymentMethod>('ALIPAY')
  const showSuccessModal = ref<boolean>(false)
  const showPaymentConfirm = ref<boolean>(false)

  const orderNumber = ref<string>(route.query.orderNumber as string || '')
  const amount = ref<number>(parseFloat(route.query.amount as string) || 0)
  const countdown = ref<number>(PAYMENT_TIMEOUT)

  let countdownTimer: ReturnType<typeof setInterval> | null = null
  let payWindow: Window | null = null

  // ==================== 计算属性 ====================
  const canPay = computed<boolean>(() => {
    return selectedMethod.value && paymentStatus.value === 'PENDING' && !paying.value
  })

  // ==================== 工具函数 ====================
  const formatPrice = (price: number): string => {
    if (typeof price !== 'number' || isNaN(price)) return '0.00'
    return price.toFixed(2)
  }

  const formatTime = (seconds: number): string => {
    if (seconds < 0) return '00:00'
    const minutes = Math.floor(seconds / 60)
    const remainingSeconds = seconds % 60
    return `${minutes.toString().padStart(2, '0')}:${remainingSeconds.toString().padStart(2, '0')}`
  }

  const clearTimer = (): void => {
    if (countdownTimer) {
      clearInterval(countdownTimer)
      countdownTimer = null
    }
  }

  // ==================== 查询订单状态 ====================
  const checkOrderStatus = async (): Promise<boolean> => {
    try {
      const response = await authAPI.queryPaymentResult(orderNumber.value)
      return !!(response.success && response.data.paid)
    } catch (error: any) {
      Toast.fail(error.message || '查询订单状态失败')
      return false
    }
  }

  // ==================== 支付成功处理 ====================
  const handlePaymentSuccess = (): void => {
    localStorage.removeItem('cartItems')
    clearTimer()
    paymentStatus.value = 'SUCCESS'
    showSuccessModal.value = true
  }

  // ==================== 页面跳转 ====================
  const goToOrderDetail = (): void => {
    showSuccessModal.value = false
    router.push({
      name: 'OrderDetail',
      query: { orderNumber: orderNumber.value }
    })
  }

  const backToHome = (): void => {
    showSuccessModal.value = false
    router.push({ name: 'UserDashboard' })
  }

  // ==================== 支付确认弹窗逻辑 ====================
  const confirmPaymentComplete = async (): Promise<void> => {
    showPaymentConfirm.value = false
    Toast.loading('正在确认支付结果...')
    
    const isPaid = await checkOrderStatus()
    Toast.close()
    
    if (isPaid) {
      handlePaymentSuccess()
    } else {
      Toast.fail('支付未完成，请继续支付')
      paymentStatus.value = 'PENDING'
      startCountdown()
    }
  }

  const cancelPaymentConfirm = (): void => {
    showPaymentConfirm.value = false
    paymentStatus.value = 'PENDING'
    startCountdown()
  }

  // ==================== 支付主流程 ====================
  const handlePayment = async (): Promise<void> => {
    if (!canPay.value) return
    
    paying.value = true
    Toast.loading('正在处理支付...')
    
    try {
      const response = await authAPI.paymentOrder({
        orderNumber: orderNumber.value,
        amount: amount.value,
        paymentMethod: selectedMethod.value
      })
      
      if (!response.success) {
        throw new Error(response.message || '创建支付订单失败')
      }

      Toast.close()
      
      // 新窗口打开支付页面
      payWindow = window.open('', '_blank')
      if (payWindow) {
        payWindow.document.write(response.data.paymentResult.pageHtml)
        payWindow.document.close()
        
        // 弹窗确认支付
        showPaymentConfirm.value = true
        clearTimer()
      } else {
        Toast.fail('弹窗被阻止，请允许弹窗后重试')
      }
      
    } catch (error: any) {
      Toast.fail(error.message || '支付处理失败，请重试')
    } finally {
      paying.value = false
    }
  }

  // ==================== 倒计时 ====================
  const startCountdown = (): void => {
    clearTimer()
    
    countdownTimer = setInterval(() => {
      if (countdown.value > 0) {
        countdown.value--
      } else {
        clearTimer()
        handlePaymentTimeout()
      }
    }, 1000)
  }

  const handlePaymentTimeout = async (): Promise<void> => {
    try {
      await authAPI.cancelOrder(orderNumber.value)
      Toast.info('支付超时，订单已取消')
      router.replace({ name: 'UserDashboard' })
    } catch (error: any) {
      Toast.fail(error.message || '取消订单失败')
      router.replace({ name: 'UserDashboard' })
    }
  }

  // ==================== 用户交互方法 ====================
  const copyOrderNumber = async (): Promise<void> => {
    try {
      if (navigator.clipboard && window.isSecureContext) {
        await navigator.clipboard.writeText(orderNumber.value)
        Toast.success('订单号已复制')
        return
      }
      throw new Error('Clipboard API not available')
    } catch {
      try {
        const textArea = document.createElement('textarea')
        textArea.value = orderNumber.value
        textArea.style.cssText = 'position:fixed;top:0;left:0;opacity:0;pointer-events:none;'
        document.body.appendChild(textArea)
        textArea.select()
        document.execCommand('copy')
        document.body.removeChild(textArea)
        Toast.success('订单号已复制')
      } catch {
        Toast.fail('复制失败，请手动复制')
      }
    }
  }

  const goBack = async (): Promise<void> => {
    if (paymentStatus.value === 'PENDING') {
      try {
        await Dialog.confirm('确定要取消支付吗？')
        await authAPI.cancelOrder(orderNumber.value)
        clearTimer()
        router.back()
      } catch {
        // 取消确认，继续支付
      }
    } else {
      router.back()
    }
  }

  const cancelPayment = async (): Promise<void> => {
    try {
      await Dialog.confirm('确定要取消支付吗？')
      
      Toast.loading('取消订单中...')
      
      try {
        await authAPI.cancelOrder(orderNumber.value)
        Toast.info('已取消支付')
      } catch (error: any) {
        Toast.fail(error.message || '取消支付失败')
      } finally {
        clearTimer()
        router.replace({ name: 'UserDashboard' })
      }
    } catch {
      // 取消确认
    }
  }

  // ==================== 生命周期 ====================
  onMounted(async () => {
    if (!orderNumber.value || !amount.value || amount.value <= 0) {
      Toast.fail('订单信息不存在')
      router.replace({ name: 'UserDashboard' })
      return
    }
    
    Toast.loading('加载订单信息...')
    
    const isPaid = await checkOrderStatus()
    
    Toast.close()
    
    if (isPaid) {
      handlePaymentSuccess()
    } else {
      startCountdown()
    }
  })

  onUnmounted(() => {
    clearTimer()
  })
</script>

<style scoped>
  @import url("@/static/css/user/支付页.css");
</style>