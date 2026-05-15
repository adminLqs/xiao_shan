<!-- PreCheckout.vue - 黑色主题预支付页面 -->
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
                placeholder="请输入手机号码"
                v-model="recipientPhone"
              />
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
                placeholder="请输入手机号码"
                v-model="takeawayPhone"
              />
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
              <span v-else class="item-img-placeholder">{{ getCategoryEmoji(item.category) }}</span>
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
        
        <div v-if="deliveryType === 'delivery'" class="fee-row">
          <span class="fee-label">配送费</span>
          <span class="fee-value">¥{{ formatPrice(deliveryFee) }}</span>
        </div>
        
        <div v-if="deliveryType === 'takeaway'" class="fee-row">
          <span class="fee-label">打包费</span>
          <span class="fee-value">¥{{ formatPrice(packagingFee) }}</span>
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
  import { ref, computed, onMounted } from 'vue'        // Vue 核心响应式API
  import { useRouter, useRoute } from 'vue-router'     // Vue Router 路由相关
  import { showToast } from 'vant'                     // Vant UI 轻量级提示组件
  import 'vant/es/toast/style'                         // 引入 Toast 样式文件
  import { authAPI } from '@/api/auth'                 // 认证相关API接口

  // ==================== 类型定义 ====================
  /**
   * 购物车商品项接口
   * 定义商品在购物车中的数据结构
   */
  interface CartItem {
    id: number           // 商品ID
    name: string         // 商品名称
    price: number        // 商品单价
    quantity: number     // 商品数量
    image?: string       // 商品图片URL（可选）
    category: string     // 商品类别（用于显示对应emoji）
  }

  // ==================== 路由实例 ====================
  const router = useRouter()    // 路由实例，用于页面跳转
  const route = useRoute()      // 当前路由信息

  // ==================== 响应式数据 ====================
  const submitting = ref(false)  // 提交状态标志，防止重复提交
  
  /**
   * 配送类型
   * dinein: 到店用餐
   * takeaway: 打包自取
   * delivery: 外卖配送
   */
  const deliveryType = ref<'dinein' | 'takeaway' | 'delivery'>('dinein')
  
  const cartItems = ref<CartItem[]>([])  // 购物车商品列表

  // ==================== 订单信息 ====================
  // 外卖配送相关字段（对应 Order 实体类）
  const recipientName = ref('')    // 收货人姓名
  const recipientPhone = ref('')   // 收货人联系电话
  const detailAddress = ref('')    // 详细收货地址
  const orderRemark = ref('')      // 订单备注信息

  // 到店用餐相关字段
  const peopleCount = ref(1)       // 用餐人数，默认为1人
  const tablePreference = ref('')  // 桌号偏好/特殊需求

  // 打包自取相关字段
  const takeawayName = ref('')     // 取餐人姓名
  const takeawayPhone = ref('')    // 取餐人联系电话

  // ==================== 费用常量 ====================
  const deliveryFee = 5.00      // 外卖配送费（元）
  const packagingFee = 2.00     // 打包费（元）

  // ==================== 计算属性 ====================
  /**
   * 商品小计金额
   * 计算所有商品单价*数量的总和
   */
  const subtotal = computed(() => {
    return cartItems.value.reduce((sum, item) => sum + (item.price * item.quantity), 0)
  })

  /**
   * 订单实付总金额
   * 根据配送类型添加对应的附加费用
   * - 外卖配送：商品合计 + 配送费
   * - 打包自取：商品合计 + 打包费
   * - 到店用餐：仅商品合计
   */
  const totalAmount = computed(() => {
    let total = subtotal.value  
    
    if (deliveryType.value === 'delivery') {
      total += deliveryFee        // 添加配送费
    } else if (deliveryType.value === 'takeaway') {
      total += packagingFee       // 添加打包费
    }
    
    return total
  })

  /**
   * 判断是否可以提交订单
   * 验证条件：
   * 1. 购物车不能为空
   * 2. 根据不同的配送类型验证必填字段
   */
  const canSubmit = computed(() => {
    // 购物车为空则不可提交
    if (cartItems.value.length === 0) return false
    
    // 外卖配送：需要填写收货人、电话、地址
    if (deliveryType.value === 'delivery') {
      return recipientName.value && recipientPhone.value && detailAddress.value
    } 
    // 打包自取：需要填写取餐人、电话
    else if (deliveryType.value === 'takeaway') {
      return takeawayName.value && takeawayPhone.value
    }
    
    // 到店用餐：无额外必填字段
    return true
  })

  // ==================== 工具方法 ====================
  /**
   * 格式化价格显示
   * @param price - 价格数值
   * @returns 保留两位小数的价格字符串
   */
  const formatPrice = (price: number): string => {
    return price.toFixed(2)
  }

  /**
   * 根据商品类别获取对应的emoji图标
   * @param category - 商品类别
   * @returns 对应的emoji字符串
   */
  const getCategoryEmoji = (category: string): string => {
    const emojiMap: Record<string, string> = {
      'meat': '🥩',        // 肉类
      'seafood': '🦐',     // 海鲜类
      'vegetable': '🥬',   // 蔬菜类
      'staple': '🍚',      // 主食类
      'drink': '🍺',       // 饮品类
      'snack': '🍡',       // 小吃类
      'skewer': '🍢',      // 烤串类
      'cold': '🥗',        // 凉菜类
      'default': '🍖'      // 默认图标
    }
    return emojiMap[category] || emojiMap.default as string
  }

  /**
   * 返回上一页
   * 跳转到首页
   */
  const goBack = () => {
    router.push({
      path: "/"
    })
  }

  /**
   * 选择配送方式
   * @param type - 配送类型（dinein/takeaway/delivery）
   */
  const selectDeliveryType = (type: 'dinein' | 'takeaway' | 'delivery') => {
    deliveryType.value = type
  }

  /**
   * 更新用餐人数
   * @param delta - 变化值（+1 或 -1）
   */
  const updatePeopleCount = (delta: number) => {
    peopleCount.value += delta
  }

  /**
   * 生成订单号
   * 格式：ORD + 年月日 + 4位随机数
   * 示例：ORD202401011234
   */
  const generateOrderNumber = (): string => {
    const date = new Date()
    const year = date.getFullYear()                      // 年份
    const month = String(date.getMonth() + 1).padStart(2, '0')  // 月份，补零
    const day = String(date.getDate()).padStart(2, '0')        // 日期，补零
    const random = Math.floor(Math.random() * 10000).toString().padStart(4, '0')  // 4位随机数
    return `ORD${year}${month}${day}${random}`
  }

  /**
   * 提交订单
   * 验证表单 -> 构建订单数据 -> 调用API -> 跳转支付页面
   */
  const submitOrder = async () => {
    // 表单验证：检查必填字段是否完整
    if (!canSubmit.value) {
      showToast('请填写完整信息')
      return
    }
    
    submitting.value = true  // 设置提交状态，防止重复提交
    
    try {
      // 获取当前登录用户ID
      const userId = localStorage.getItem('userId')
      if (!userId) {
        showToast('请先登录')
        router.push('/')
        return
      }
      
      // ==================== 构建订单数据 ====================
      const orderData = {
        // 订单编号（唯一标识）
        orderNumber: generateOrderNumber(),
        
        // 订单总金额
        totalAmount: totalAmount.value,
        
        // 配送方式类型
        deliveryType: deliveryType.value,

        // ==================== 收货人信息 ====================
        // 根据配送类型获取对应的联系人姓名
        recipientName: deliveryType.value === 'delivery' ? recipientName.value : 
                      deliveryType.value === 'takeaway' ? takeawayName.value : '到店用餐',

        // 根据配送类型获取对应的联系电话
        recipientPhone: deliveryType.value === 'delivery' ? recipientPhone.value :
                        deliveryType.value === 'takeaway' ? takeawayPhone.value : '',

        // 详细地址（外卖配送需填写，其他场景使用默认值）
        detailAddress: deliveryType.value === 'delivery' ? detailAddress.value : 
                      deliveryType.value === 'dinein' ? '到店用餐' : '打包自取',
        
        // ==================== 到店用餐专属字段 ====================
        // 用餐人数（仅到店用餐时有效）
        peopleCount: deliveryType.value === 'dinein' ? peopleCount.value : null,
        
        // 桌号偏好/特殊需求（仅到店用餐时有效）
        tablePreference: deliveryType.value === 'dinein' ? tablePreference.value : null,
        
        // 订单备注（所有类型通用）
        remark: orderRemark.value,
        
        // ==================== 订单商品明细 ====================
        orderItems: cartItems.value.map(item => ({
          productId: item.id,           // 商品ID
          productName: item.name,       // 商品名称
          image: item.image,     // 商品图片
          quantity: item.quantity,      // 购买数量
          price: item.price             // 商品单价
        }))
      }
      
      console.log('提交订单:', orderData)  // 开发调试日志
      
      // ==================== 调用API创建订单 ====================
      const response = await authAPI.createOrder(userId, orderData);
      const data = response.data || response  // 兼容不同的响应格式
      
      console.log('订单创建响应:', response)
      
      // ==================== 处理响应结果 ====================
      // 检查订单是否创建成功
      if (data?.success === true) {
        const orderNumber = data.orderNumber  // 获取后端返回的订单号
        
        showToast('订单创建成功')
        
        // 跳转到支付页面，传递订单号和金额参数
        router.push({
          name: 'UserPayment',
          query: {
            orderNumber: orderNumber,                      // 订单号
            amount: totalAmount.value.toFixed(2)       // 支付金额（保留两位小数）
          }
        })
      } else {
        // 订单创建失败，显示错误信息
        const errorMsg = data?.message || '订单创建失败'
        showToast(errorMsg)
      }
      
    } catch (error: any) {
      // ==================== 异常处理 ====================
      console.error('提交订单失败:', error)
      
      // 根据错误类型显示相应的提示信息
      if (error.status) {
        // HTTP状态码错误（如404、500等）
        showToast(error.message || `服务器错误: ${error.status}`)
      } else if (error.message) {
        // 业务逻辑错误或网络错误
        showToast(error.message)
      } else {
        // 未知错误
        showToast('提交失败，请重试')
      }
    } finally {
      // 无论成功或失败，都要重置提交状态
      submitting.value = false
    }
  }

  /**
   * 从本地存储加载购物车数据
   * 如果购物车为空，则提示并返回上一页
   */
  const loadCartData = () => {
    try {
      const saved = localStorage.getItem('cartItems')  // 从localStorage读取购物车数据
      if (saved) {
        cartItems.value = JSON.parse(saved)            // 解析JSON并赋值
      }
    } catch (error) {
      console.error('加载购物车失败:', error)          // 错误日志
    }
    
    // 购物车为空时提示并返回
    if (cartItems.value.length === 0) {
      showToast('购物车是空的')
      router.back()  // 返回上一页
    }
  }

  // ==================== 生命周期钩子 ====================
  /**
   * 组件挂载时执行
   * 加载购物车数据
   */
  onMounted(() => {
    loadCartData()
  })
</script>

<style scoped>
  @import url("@/static/css/预支付页.css")
</style>

