<template>
  <div class="pre-checkout-container">
    <!-- 顶部导航栏 -->
    <div class="pre-header">
      <button class="back-btn" @click="goBack">
        <span class="back-icon">←</span>
      </button>
      <h1 class="header-title">确认订单</h1>
      <div class="header-placeholder"></div>
    </div>

    <div class="content-wrapper">
      <!-- 配送方式选择卡片 -->
      <div class="delivery-section">
        <div class="section-title">
          <span class="title-icon">🚚</span>
          <span class="title-text">选择配送方式</span>
        </div>
        
        <div class="delivery-options">
          <!-- 到店用餐 -->
          <div 
            class="delivery-option"
            :class="{ active: deliveryType === 'dinein' }"
            @click="selectDeliveryType('dinein')"
          >
            <div class="option-left">
              <span class="option-icon dinein">🍽️</span>
              <div class="option-info">
                <span class="option-name">到店用餐</span>
                <span class="option-desc">前往店铺享用美食</span>
              </div>
            </div>
            <div class="option-right">
              <span class="option-check" v-if="deliveryType === 'dinein'">✓</span>
              <span class="option-uncheck" v-else></span>
            </div>
          </div>

          <!-- 打包自取 -->
          <div 
            class="delivery-option"
            :class="{ active: deliveryType === 'takeaway' }"
            @click="selectDeliveryType('takeaway')"
          >
            <div class="option-left">
              <span class="option-icon takeaway">📦</span>
              <div class="option-info">
                <span class="option-name">打包自取</span>
                <span class="option-desc">到店自提，免配送费</span>
              </div>
            </div>
            <div class="option-right">
              <span class="option-check" v-if="deliveryType === 'takeaway'">✓</span>
              <span class="option-uncheck" v-else></span>
            </div>
          </div>

          <!-- 外卖配送 -->
          <div 
            class="delivery-option"
            :class="{ active: deliveryType === 'delivery' }"
            @click="selectDeliveryType('delivery')"
          >
            <div class="option-left">
              <span class="option-icon delivery">🛵</span>
              <div class="option-info">
                <span class="option-name">外卖配送</span>
                <span class="option-desc">配送到家，需填写地址</span>
              </div>
            </div>
            <div class="option-right">
              <span class="option-check" v-if="deliveryType === 'delivery'">✓</span>
              <span class="option-uncheck" v-else></span>
            </div>
          </div>
        </div>
      </div>

      <!-- 外卖配送信息填写 -->
      <transition name="slide">
        <div v-if="deliveryType === 'delivery'" class="form-section">
          <div class="section-title">
            <span class="title-icon">📍</span>
            <span class="title-text">收货信息</span>
          </div>
          
          <div class="form-card">
            <div class="form-group">
              <label class="form-label">收货人</label>
              <input 
                type="text" 
                class="form-input" 
                placeholder="请输入收货人姓名"
                v-model="recipientName"
              />
            </div>
            
            <div class="form-group">
              <label class="form-label">联系电话</label>
              <input 
                type="tel" 
                class="form-input" 
                :class="{ 'input-error': recipientPhone && !isValidPhone(recipientPhone) }"
                placeholder="请输入手机号码"
                v-model="recipientPhone"
              />
              <span v-if="recipientPhone && !isValidPhone(recipientPhone)" class="error-tip">
                请输入正确的手机号
              </span>
            </div>
            
            <div class="form-group">
              <label class="form-label">详细地址</label>
              <input 
                type="text" 
                class="form-input" 
                placeholder="请输入详细地址"
                v-model="detailAddress"
              />
            </div>
          </div>
        </div>
      </transition>

      <!-- 到店用餐信息填写 -->
      <transition name="slide">
        <div v-if="deliveryType === 'dinein'" class="form-section">
          <div class="section-title">
            <span class="title-icon">🍽️</span>
            <span class="title-text">用餐信息</span>
          </div>
          
          <div class="form-card">
            <div class="form-group">
              <label class="form-label">用餐人数</label>
              <div class="people-counter">
                <button class="counter-btn" @click="updatePeopleCount(-1)" :disabled="peopleCount <= 1">−</button>
                <span class="people-count">{{ peopleCount }}人</span>
                <button class="counter-btn" @click="updatePeopleCount(1)" :disabled="peopleCount >= 10">+</button>
              </div>
            </div>
            
            <div class="form-group">
              <label class="form-label">桌号偏好</label>
              <input 
                type="text" 
                class="form-input" 
                placeholder="如有特殊需求请备注"
                v-model="tablePreference"
              />
            </div>
          </div>
        </div>
      </transition>

      <!-- 打包自取信息填写 -->
      <transition name="slide">
        <div v-if="deliveryType === 'takeaway'" class="form-section">
          <div class="section-title">
            <span class="title-icon">📦</span>
            <span class="title-text">取餐信息</span>
          </div>
          
          <div class="form-card">
            <div class="form-group">
              <label class="form-label">取餐人</label>
              <input 
                type="text" 
                class="form-input" 
                placeholder="请输入取餐人姓名"
                v-model="takeawayName"
              />
            </div>
            
            <div class="form-group">
              <label class="form-label">联系电话</label>
              <input 
                type="tel" 
                class="form-input" 
                :class="{ 'input-error': takeawayPhone && !isValidPhone(takeawayPhone) }"
                placeholder="请输入手机号码"
                v-model="takeawayPhone"
              />
              <span v-if="takeawayPhone && !isValidPhone(takeawayPhone)" class="error-tip">
                请输入正确的手机号
              </span>
            </div>
          </div>
        </div>
      </transition>

      <!-- 订单商品列表 -->
      <div class="order-items-section">
        <div class="section-title">
          <span class="title-icon">🛒</span>
          <span class="title-text">商品清单</span>
          <span class="item-count">{{ cartItems.length }}件商品</span>
        </div>
        
        <div class="order-items">
          <div v-for="item in cartItems" :key="item.id" class="order-item">
            <div class="item-image">
              <img v-if="item.image" :src="item.image" :alt="item.name" />
              <span v-else class="item-img-placeholder">{{ getCategoryEmoji(item!.category) }}</span>
            </div>
            <div class="item-content">
              <div class="item-name">{{ item.name }}</div>
              <div class="item-price-info">
                <span class="item-price">¥{{ formatPrice(item.price) }}</span>
                <span class="item-quantity">x{{ item.quantity }}</span>
              </div>
            </div>
            <div class="item-total">
              ¥{{ formatPrice(item.price * item.quantity) }}
            </div>
          </div>
        </div>
      </div>

      <!-- 备注信息 -->
      <div class="remark-section">
        <div class="remark-left">
          <span class="remark-icon">📝</span>
          <span class="remark-label">订单备注</span>
        </div>
        <input 
          type="text" 
          class="remark-input" 
          placeholder="口味、偏好等要求（选填）"
          v-model="orderRemark"
        />
      </div>

      <!-- 费用明细 -->
      <div class="fee-section">
        <div class="fee-row">
          <span class="fee-label">商品合计</span>
          <span class="fee-value">¥{{ formatPrice(subtotal) }}</span>
        </div>
        
        <!-- 外卖配送：显示配送费 + 打包费 -->
        <template v-if="deliveryType === 'delivery'">
          <div class="fee-row">
            <span class="fee-label">配送费</span>
            <span class="fee-value">¥{{ formatPrice(DELIVERY_FEE) }}</span>
          </div>
          <div class="fee-row">
            <span class="fee-label">打包费</span>
            <span class="fee-value">¥{{ formatPrice(PACKAGING_FEE) }}</span>
          </div>
        </template>

        <!-- 打包自取：只显示打包费 -->
        <div v-if="deliveryType === 'takeaway'" class="fee-row">
          <span class="fee-label">打包费</span>
          <span class="fee-value">¥{{ formatPrice(PACKAGING_FEE) }}</span>
        </div>
        
        <div class="fee-row total">
          <span class="fee-label">实付</span>
          <span class="fee-value">¥{{ formatPrice(totalAmount) }}</span>
        </div>
      </div>
    </div>

    <!-- 底部提交栏 -->
    <div class="pre-footer">
      <div class="footer-left">
        <span class="total-label">实付</span>
        <span class="total-price">¥{{ formatPrice(totalAmount) }}</span>
      </div>
      <button 
        class="submit-btn" 
        @click="submitOrder"
        :disabled="!canSubmit || submitting"
      >
        <span v-if="!submitting">提交订单</span>
        <span v-else class="submitting">
          <span class="spinner"></span>
          提交中...
        </span>
      </button>
    </div>
  </div>
</template>

<script lang="ts" setup>
  // ==================== 导入模块 ====================
  import { ref, computed, onMounted } from 'vue'
  import { useRouter, useRoute } from 'vue-router'
  import { showToast } from 'vant'
  import 'vant/es/toast/style'
  import { authAPI } from '@/api/authAPI'
  import { useUserStore } from '@/stores/auth'

  // ==================== 路由与状态 ====================
  const router = useRouter()
  const route = useRoute()
  const userStore = useUserStore()

  // ==================== 类型定义 ====================

  /** 购物车商品项 */
  interface CartItem {
    id: number
    name: string
    price: number
    quantity: number
    image?: string
    category: string
  }

  /** 配送方式类型 */
  type DeliveryType = 'dinein' | 'takeaway' | 'delivery'

  // ==================== 常量定义 ====================

  /** 配送费 */
  const DELIVERY_FEE = 3.00

  /** 打包费 */
  const PACKAGING_FEE = 2.00

  /** 手机号正则表达式 */
  const PHONE_REGEX = /^1[3-9]\d{9}$/

  // ==================== 响应式数据 ====================

  /** 提交状态 */
  const submitting = ref(false)

  /** 配送方式 */
  const deliveryType = ref<DeliveryType>('dinein')

  /** 购物车商品列表 */
  const cartItems = ref<CartItem[]>([])

  // 配送信息（外卖）
  const recipientName = ref('')
  const recipientPhone = ref('')
  const detailAddress = ref('')

  // 用餐信息（到店）
  const peopleCount = ref(1)
  const tablePreference = ref('')

  // 取餐信息（自取）
  const takeawayName = ref('')
  const takeawayPhone = ref('')

  /** 订单备注 */
  const orderRemark = ref('')

  // ==================== 校验函数 ====================

  /**
   * 校验手机号格式
   * @param phone - 手机号
   * @returns 是否有效
   */
  const isValidPhone = (phone: string): boolean => {
    if (!phone) return false
    return PHONE_REGEX.test(phone)
  }

  // ==================== 计算属性 ====================

  /**
   * 商品小计金额
   */
  const subtotal = computed(() => {
    return cartItems.value.reduce((sum, item) => sum + (item.price * item.quantity), 0)
  })

  /**
   * 订单实付总金额
   * 费用规则：
   * - 外卖配送：商品小计 + 配送费(3元) + 打包费(2元)
   * - 打包自取：商品小计 + 打包费(2元)
   * - 到店用餐：仅商品小计
   */
  const totalAmount = computed(() => {
    let total = subtotal.value
    
    switch (deliveryType.value) {
      case 'delivery':
        total += DELIVERY_FEE      // 配送费 3元
        total += PACKAGING_FEE     // 打包费 2元
        break
      case 'takeaway':
        total += PACKAGING_FEE     // 打包费 2元
        break
      case 'dinein':
        // 到店用餐无附加费用
        break
    }
    
    return total
  })

  /**
   * 是否可提交订单
   */
  const canSubmit = computed(() => {
    if (cartItems.value.length === 0) return false

    if (deliveryType.value === 'delivery') {
      if (!recipientName.value) return false
      if (!recipientPhone.value || !isValidPhone(recipientPhone.value)) return false
      if (!detailAddress.value) return false
      return true
    }
    
    if (deliveryType.value === 'takeaway') {
      if (!takeawayName.value) return false
      if (!takeawayPhone.value || !isValidPhone(takeawayPhone.value)) return false
      return true
    }
    
    return true
  })

  // ==================== 工具方法 ====================

  /**
   * 格式化价格
   * @param price - 价格数值
   * @returns 保留两位小数的价格字符串
   */
  const formatPrice = (price: number): string => {
    if (price === undefined || price === null || isNaN(price)) {
      return '0.00'
    }
    return price.toFixed(2)
  }

  /**
   * 获取商品分类对应的 Emoji
   * @param category - 商品分类
   * @returns 对应的 Emoji 图标
   */
  const getCategoryEmoji = (category: string): string => {
    const map: Record<string, string> = {
      meat: '🥩', seafood: '🦐', vegetable: '🥬',
      staple: '🍚', drink: '🍺', snack: '🍡',
      skewer: '🍢', cold: '🥗', default: '🍖'
    }
    return map[category] || map.default
  }

  /**
   * 返回上一页
   */
  const goBack = () => router.push({ path: '/' })

  /**
   * 选择配送方式
   * @param type - 配送类型
   */
  const selectDeliveryType = (type: DeliveryType) => {
    deliveryType.value = type
  }

  /**
   * 更新用餐人数
   * @param delta - 变化值（+1 或 -1）
   */
  const updatePeopleCount = (delta: number) => {
    const newCount = peopleCount.value + delta
    if (newCount >= 1 && newCount <= 10) {
      peopleCount.value = newCount
    }
  }

  /**
   * 生成订单号
   * @returns 格式：ORD + 年月日 + 4位随机数
   */
  const generateOrderNumber = (): string => {
    const date = new Date()
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    const random = Math.floor(Math.random() * 10000).toString().padStart(4, '0')
    return `ORD${year}${month}${day}${random}`
  }

  // ==================== 数据加载 ====================

  /**
   * 从本地存储加载购物车数据
   */
  const loadCartData = () => {
    try {
      const saved = localStorage.getItem('cartItems')
      if (saved) {
        cartItems.value = JSON.parse(saved)
      }
    } catch (error) {
      showToast({ message: '加载购物车失败', type: 'fail' })
    }

    if (cartItems.value.length === 0) {
      showToast({ message: '购物车是空的', type: 'fail' })
      router.back()
    }
  }

  // ==================== 订单提交 ====================

  /**
   * 提交订单
   * 验证表单 -> 构建订单数据 -> 调用API -> 跳转支付页面
   */
  const submitOrder = async () => {
    if (!canSubmit.value) {
      showToast({ message: '请填写完整信息', type: 'fail' })
      return
    }

    const userId = userStore.userId
    if (!userId) {
      showToast({ message: '用户未登录', type: 'fail' })
      return
    }

    submitting.value = true

    try {
      const orderData = {
        orderNumber: generateOrderNumber(),
        totalAmount: totalAmount.value,
        deliveryType: deliveryType.value,
        deliveryFee: deliveryType.value === 'delivery' ? DELIVERY_FEE : 0,
        packagingFee: deliveryType.value !== 'dinein' ? PACKAGING_FEE : 0,
        recipientName: deliveryType.value === 'delivery' ? recipientName.value
          : deliveryType.value === 'takeaway' ? takeawayName.value : '到店用餐',
        recipientPhone: deliveryType.value === 'delivery' ? recipientPhone.value
          : deliveryType.value === 'takeaway' ? takeawayPhone.value : '',
        detailAddress: deliveryType.value === 'delivery' ? detailAddress.value
          : deliveryType.value === 'dinein' ? '到店用餐' : '打包自取',
        peopleCount: deliveryType.value === 'dinein' ? peopleCount.value : null,
        tablePreference: deliveryType.value === 'dinein' ? tablePreference.value : null,
        remark: orderRemark.value,
        orderItems: cartItems.value.map(item => ({
          productId: item.id,
          productName: item.name,
          image: item.image,
          quantity: item.quantity,
          price: item.price
        }))
      }

      const response = await authAPI.createOrder(userId, orderData)
      const data = response.data || response

      if (data?.success) {
        showToast({ message: '订单创建成功', type: 'success' })
        router.push({
          name: 'UserPayment',
          query: {
            orderNumber: data.orderNumber,
            amount: totalAmount.value.toFixed(2)
          }
        })
      } else {
        showToast({ message: data?.message || '订单创建失败', type: 'fail' })
      }
    } catch (error: any) {
      showToast({ message: error.message || '提交失败，请重试', type: 'fail' })
    } finally {
      submitting.value = false
    }
  }

  // ==================== 生命周期 ====================

  onMounted(() => {
    loadCartData()
  })
</script>

<style scoped>
@import url("@/static/css/user/预支付页.css");
</style>