<!-- Payment.vue - 支付页面（只保留微信和支付宝） -->
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
          <button class="copy-btn" @click="copyorderNumber">复制</button>
        </div>
        <div class="order-amount">
          <span class="label">支付金额</span>
          <span class="value">¥{{ formatPrice(amount) }}</span>
        </div>
        <div class="order-timer" v-if="paymentStatus === 'pending'">
          <span class="timer-icon">⏱️</span>
          <span class="timer-text">剩余支付时间</span>
          <span class="timer-count">{{ formatTime(countdown) }}</span>
        </div>
      </div>

      <!-- 支付方式选择（只保留微信和支付宝） -->
      <div class="payment-methods">
        <h3 class="section-title">选择支付方式</h3>
        
        <div 
          class="payment-method"
          :class="{ active: selectedMethod === 'wechat' }"
          @click="selectedMethod = 'wechat'"
        >
          <div class="method-left">
            <span class="method-icon wechat">💚</span>
            <div class="method-info">
              <span class="method-name">微信支付</span>
              <span class="method-desc">推荐使用微信支付</span>
            </div>
          </div>
          <div class="method-right">
            <span class="method-check" v-if="selectedMethod === 'wechat'">✓</span>
          </div>
        </div>

        <div 
          class="payment-method"
          :class="{ active: selectedMethod === 'alipay' }"
          @click="selectedMethod = 'alipay'"
        >
          <div class="method-left">
            <span class="method-icon alipay">💙</span>
            <div class="method-info">
              <span class="method-name">支付宝</span>
              <span class="method-desc">支付宝安全支付</span>
            </div>
          </div>
          <div class="method-right">
            <span class="method-check" v-if="selectedMethod === 'alipay'">✓</span>
          </div>
        </div>
      </div>

      <!-- 支付状态显示 -->
      <transition name="fade">
        <div v-if="paymentStatus !== 'pending'" class="payment-status-card" :class="paymentStatus">
          <div class="status-icon">{{ paymentStatus === 'success' ? '✅' : '❌' }}</div>
          <div class="status-info">
            <h3 class="status-title">{{ paymentStatus === 'success' ? '支付成功' : '支付失败' }}</h3>
            <p class="status-message">{{ paymentMessage }}</p>
          </div>
        </div>
      </transition>

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
        
        <button v-if="paymentStatus === 'pending'" class="cancel-btn" @click="cancelPayment">
          取消支付
        </button>
        
        <div v-if="paymentStatus === 'success'" class="success-actions">
          <button class="view-order-btn" @click="viewOrder">查看订单</button>
          <button class="back-home-btn" @click="backToHome">返回首页</button>
        </div>
        
        <div v-if="paymentStatus === 'failed'" class="failed-actions">
          <button class="retry-btn" @click="retryPayment">重新支付</button>
          <button class="contact-btn" @click="contactService">联系客服</button>
        </div>
      </div>
    </div>

    <!-- 支付结果弹窗 -->
    <transition name="slide-up">
      <div v-if="showResultModal" class="result-modal">
        <div class="modal-content" :class="paymentStatus">
          <div class="modal-icon">{{ paymentStatus === 'success' ? '🎉' : '😢' }}</div>
          <h3 class="modal-title">{{ paymentStatus === 'success' ? '支付成功' : '支付失败' }}</h3>
          <p class="modal-message">{{ paymentMessage }}</p>
          <button class="modal-btn" @click="closeResultModal">确定</button>
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
  import { authAPI } from '@/api/auth'

  // ==================== 路由实例 ====================
  const router = useRouter()
  const route = useRoute()

  // ==================== 响应式数据 ====================
  // 支付状态
  const paying = ref(false)                                    // 是否正在支付中
  const paymentStatus = ref<'pending' | 'success' | 'failed'>('pending')  // 支付状态：待支付/成功/失败
  const paymentMessage = ref('')                               // 支付结果消息
  const selectedMethod = ref<'wechat' | 'alipay'>('wechat')    // 选中的支付方式
  const showResultModal = ref(false)                           // 是否显示结果弹窗

  // 订单信息（从路由参数获取）
  const orderNumber = ref(route.query.orderNumber as string || '')      // 订单号
  const amount = ref(parseFloat(route.query.amount as string) || 0)  // 支付金额

  // 倒计时（15分钟 = 900秒）
  const countdown = ref(900)

  // 定时器
  let countdownTimer: number | null = null  // 倒计时定时器
  let pollTimer: number | null = null       // 轮询支付结果定时器

  // ==================== 环境判断函数 ====================

  /**
   * 判断是否在小程序环境
   */
  const isMiniProgram = (): boolean => {
    const ua = navigator.userAgent.toLowerCase()
    return ua.indexOf('miniprogram') > -1 || !!(window as any).__wxjs_environment
  }

  /**
   * 判断是否在微信浏览器环境（非小程序）
   */
  const isWeChatBrowser = (): boolean => {
    const ua = navigator.userAgent.toLowerCase()
    return ua.indexOf('micromessenger') > -1 && !isMiniProgram()
  }

  /**
   * 判断是否在支付宝浏览器环境
   */
  const isAlipayBrowser = (): boolean => {
    const ua = navigator.userAgent.toLowerCase()
    return ua.indexOf('alipay') > -1
  }

  // ==================== 计算属性 ====================

  /**
   * 是否可以支付（支付方式已选 && 状态为待支付 && 不在支付中）
   */
  const canPay = computed(() => {
    return selectedMethod.value && paymentStatus.value === 'pending' && !paying.value
  })

  // ==================== 工具函数 ====================

  /**
   * 格式化价格（保留两位小数）
   */
  const formatPrice = (price: number): string => {
    return price.toFixed(2)
  }

  /**
   * 格式化倒计时（秒转 mm:ss）
   */
  const formatTime = (seconds: number): string => {
    const minutes = Math.floor(seconds / 60)
    const remainingSeconds = seconds % 60
    return `${minutes.toString().padStart(2, '0')}:${remainingSeconds.toString().padStart(2, '0')}`
  }

  // ==================== 微信支付调用 ====================

  /**
   * 调起微信支付（小程序环境 或 微信浏览器环境）
   * @param payParams 微信支付参数（timeStamp, nonceStr, package, signType, paySign）
   */
  const invokeWeChatPay = (payParams: any): Promise<void> => {
    return new Promise((resolve, reject) => {
      // 小程序环境
      if (isMiniProgram()) {
        // @ts-ignore
        wx.requestPayment({
          timeStamp: payParams.timeStamp,
          nonceStr: payParams.nonceStr,
          package: payParams.package,
          signType: payParams.signType || 'MD5',
          paySign: payParams.paySign,
          success: () => {
            console.log('微信支付成功')
            resolve()
          },
          fail: (err: any) => {
            console.error('微信支付失败', err)
            reject(new Error(err.errMsg || '支付失败'))
          }
        })
      } 
      // 微信浏览器环境（JSAPI支付）
      else if (isWeChatBrowser()) {
        // @ts-ignore
        WeixinJSBridge.invoke('getBrandWCPayRequest', {
          appId: payParams.appId,
          timeStamp: payParams.timeStamp,
          nonceStr: payParams.nonceStr,
          package: payParams.package,
          signType: payParams.signType || 'MD5',
          paySign: payParams.paySign
        }, (res: any) => {
          if (res.err_msg === 'get_brand_wcpay_request:ok') {
            resolve()
          } else {
            reject(new Error(res.err_msg))
          }
        })
      } else {
        reject(new Error('不在微信环境'))
      }
    })
  }

  /**
   * 调起支付宝支付（支付宝浏览器环境）
   * @param payParams 支付宝支付参数（tradeNo）
   */
  const invokeAlipayPay = (payParams: any): Promise<void> => {
    return new Promise((resolve, reject) => {
      if (isAlipayBrowser()) {
        // @ts-ignore
        window.AlipayJSBridge.call('tradePay', {
          tradeNO: payParams.tradeNo
        }, (result: any) => {
          if (result.resultCode === '9000') {
            resolve()
          } else {
            reject(new Error('支付失败'))
          }
        })
      } else {
        reject(new Error('不在支付宝环境'))
      }
    })
  }

  /**
   * 打开支付宝网页支付（PC端）
   * @param pageHtml 支付宝返回的支付页面HTML
   */
  const openAlipayPagePay = (pageHtml: string): void => {
    // 在新窗口中打开支付页面
    const payWindow = window.open()
    if (payWindow) {
      payWindow.document.write(pageHtml)
      payWindow.document.close()
    } else {
      // 如果被拦截，提示用户
      showToast('请允许弹出窗口，或复制链接在浏览器中打开')
    }
  }

  // ==================== 清空购物车并显示成功 ====================

  /**
   * 支付成功后清空购物车并显示成功弹窗
   */
  const handlePaymentSuccess = (): void => {
    // 清空购物车
    localStorage.removeItem('cartItems')
    console.log('购物车已清空')
    
    // 更新状态
    paymentStatus.value = 'success'
    paymentMessage.value = '支付成功'
    showResultModal.value = true
  }

  // ==================== 支付主流程 ====================

  /**
   * 处理支付（核心方法）
   * 流程：
   * 1. 调用后端创建支付订单
   * 2. 根据环境和支付方式决定：
   *    - 微信环境：直接调起微信支付
   *    - 支付宝环境：直接调起支付宝支付
   *    - 普通浏览器：打开支付宝网页支付页面
   */
  const handlePayment = async (): Promise<void> => {
    if (!canPay.value) return
    
    paying.value = true
    
    try {
      // 步骤1：调用后端创建支付订单
      const response = await authAPI.createPayment({
        orderNumber: orderNumber.value,
        amount: amount.value,
        paymentMethod: selectedMethod.value
      })
      
      // 处理响应（兼容 Axios 格式）
      const data = response.data || response
      
      console.log('支付响应:', data)
      
      // 创建失败处理
      if (!data.success) {
        throw new Error(data.message || '创建支付订单失败')
      }
      
      // 步骤2：微信支付流程
      if (selectedMethod.value === 'wechat') {
        // 微信环境（小程序或微信浏览器）：直接调起支付
        if (isMiniProgram() || isWeChatBrowser()) {
          console.log('微信环境，调起支付')
          await invokeWeChatPay(data.payParams || {})
          // 支付成功处理
          handlePaymentSuccess()
          return
        } else {
          // 非微信环境：提示用户在微信中打开
          showToast('请在微信浏览器中打开进行支付')
          return
        }
      }
      
      // 步骤3：支付宝支付流程
      if (selectedMethod.value === 'alipay') {
        // 支付宝浏览器环境：直接调起支付
        if (isAlipayBrowser()) {
          console.log('支付宝环境，调起支付')
          await invokeAlipayPay(data.payParams || {})
          handlePaymentSuccess()
          return
        }
        
        // 普通浏览器：打开支付宝网页支付页面
        if (data.pageHtml) {
          console.log('普通浏览器，打开支付宝网页支付')
          openAlipayPagePay(data.pageHtml)
          // 开始轮询支付结果
          // startPollingPaymentResult()
          return
        }
      }
      
      // 其他情况：提示错误
      throw new Error('暂不支持该支付方式')
      
    } catch (error: any) {
      console.error('支付失败:', error)
      paymentStatus.value = 'failed'
      paymentMessage.value = error.message || '支付处理失败，请重试'
      showResultModal.value = true
    } finally {
      paying.value = false
    }
  }

  // ==================== 轮询支付结果 ====================

  /**
   * 开始轮询支付结果
   * 每2秒查询一次，最多60次（2分钟）
   * 支付成功后停止轮询并跳转
   */
  // const startPollingPaymentResult = (): void => {
  //   let pollCount = 0
  //   const maxPolls = 60  // 最多轮询60次
    
  //   // 清除之前的轮询
  //   if (pollTimer) {
  //     clearInterval(pollTimer)
  //     pollTimer = null
  //   }
    
  //   pollTimer = setInterval(async () => {
  //     try {
  //       pollCount++
        
  //       // 调用后端查询支付结果
  //       const response = await authAPI.queryPaymentResult(orderNumber.value)
  //       const data = response.data || response
        
  //       console.log('查询支付结果:', data)
        
  //       // 支付成功
  //       if (data.success && data.paid) {
  //         clearInterval(pollTimer!)
  //         pollTimer = null
  //         handlePaymentSuccess()
          
  //         // 清除倒计时
  //         if (countdownTimer) {
  //           clearInterval(countdownTimer)
  //           countdownTimer = null
  //         }
  //       } 
  //       // 超时处理
  //       else if (pollCount >= maxPolls) {
  //         clearInterval(pollTimer!)
  //         pollTimer = null
  //         paymentStatus.value = 'failed'
  //         paymentMessage.value = '支付超时，请重试'
  //         showResultModal.value = true
          
  //         if (countdownTimer) {
  //           clearInterval(countdownTimer)
  //           countdownTimer = null
  //         }
  //       }
        
  //     } catch (error) {
  //       console.error('查询支付结果失败:', error)
  //     }
  //   }, 2000)  // 每2秒查询一次
  // }

  // ==================== 倒计时 ====================

  /**
   * 开始倒计时（15分钟）
   * 倒计时结束自动取消订单
   */
  const startCountdown = (): void => {
    if (countdownTimer) {
      clearInterval(countdownTimer)
      countdownTimer = null
    }
    
    countdownTimer = setInterval(() => {
      if (countdown.value > 0) {
        countdown.value--
      } else {
        // 倒计时结束
        clearInterval(countdownTimer!)
        countdownTimer = null
        
        if (paymentStatus.value === 'pending') {
          paymentStatus.value = 'failed'
          paymentMessage.value = '支付超时，请重新下单'
          showResultModal.value = true
          
          // 清除轮询
          if (pollTimer) {
            clearInterval(pollTimer)
            pollTimer = null
          }
        }
      }
    }, 1000)
  }

  // ==================== 用户交互方法 ====================

  /**
   * 复制订单号到剪贴板
   */
  const copyorderNumber = (): void => {
    navigator.clipboard.writeText(orderNumber.value).then(() => {
      showToast('订单号已复制')
    }).catch(() => {
      showToast('复制失败')
    })
  }

  /**
   * 返回上一页
   */
  const goBack = (): void => {
    if (paymentStatus.value === 'pending') {
      if (confirm('确定放弃支付吗？')) {
        clearTimers()
        router.back()
      }
    } else {
      router.back()
    }
  }

  /**
   * 取消支付
   */
  const cancelPayment = (): void => {
    if (confirm('确定取消支付吗？')) {
      clearTimers()
      router.push({ name: 'UserDashboard' })
    }
  }

  /**
   * 查看订单详情
   */
  const viewOrder = (): void => {
    router.push({
      path: '/order/detail',
      query: { orderNumber: orderNumber.value }
    })
  }

  /**
   * 返回首页
   */
  const backToHome = (): void => {
    router.push({ name: 'UserDashboard' })
  }

  /**
   * 重新支付
   */
  const retryPayment = (): void => {
    paymentStatus.value = 'pending'
    paymentMessage.value = ''
    showResultModal.value = false
    selectedMethod.value = 'wechat'
    countdown.value = 900
    startCountdown()
  }

  /**
   * 联系客服
   */
  const contactService = (): void => {
    showToast('商家电话: 400-888-8888')
  }

  /**
   * 关闭结果弹窗
   */
  const closeResultModal = (): void => {
    showResultModal.value = false
    
    if (paymentStatus.value === 'success') {
      // 支付成功，跳转到订单详情
      router.push({
        path: '/order/detail',
        query: { orderNumber: orderNumber.value }
      })
    }
  }

  /**
   * 清理所有定时器（防止内存泄漏）
   */
  const clearTimers = (): void => {
    if (countdownTimer) {
      clearInterval(countdownTimer)
      countdownTimer = null
    }
    if (pollTimer) {
      clearInterval(pollTimer)
      pollTimer = null
    }
  }

  // ==================== 生命周期钩子 ====================

  /**
   * 组件挂载时执行
   * 1. 验证订单信息
   * 2. 开始倒计时
   */
  onMounted(() => {
    // 验证订单信息是否完整
    if (!orderNumber.value || !amount.value || amount.value <= 0) {
      showToast('订单信息不存在')
      router.back()
      return
    }
    
    console.log('支付页面加载，订单号:', orderNumber.value, '金额:', amount.value)
    console.log('环境判断 - 小程序:', isMiniProgram(), '微信浏览器:', isWeChatBrowser(), '支付宝浏览器:', isAlipayBrowser())
    
    // 开始倒计时
    startCountdown()
  })

  /**
   * 组件卸载时清理定时器
   */
  onUnmounted(() => {
    clearTimers()
  })
</script>

<style scoped>
  @import url("@/static/css/支付页.css");
</style>