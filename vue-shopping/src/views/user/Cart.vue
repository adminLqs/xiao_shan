<template>
  <div class="cart-container">
    <!-- 页面标题区域 -->
    <div class="page-header">
      <h1 class="page-title">
        <i class="fas fa-shopping-cart"></i>
        购物车
      </h1>
      <div class="breadcrumb">
        <RouterLink to="/">首页</RouterLink>
        <i class="fas fa-chevron-right"></i>
        <span class="current">购物车</span>
      </div>
    </div>

    <!-- 加载状态，数据加载时显示 -->
    <div v-if="loading" class="loading-container">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>

    <!-- 空购物车状态，购物车为空时显示 -->
    <div v-else-if="cartItems.length === 0" class="empty-cart">
      <i class="fas fa-shopping-cart"></i>
      <p>购物车还是空的</p>
      <RouterLink to="/" class="btn btn-primary">去逛逛</RouterLink>
    </div>

    <!-- 购物车内容，有商品时显示 -->
    <div v-else class="cart-content">
      <div class="cart-main">
        <!-- 全选操作栏 -->
        <div class="cart-actions">
          <label class="select-all">
            <input
              type="checkbox"
              :checked="isAllSelected"
              @change="toggleSelectAll"
            />
            <span>全选</span>
            <!-- 显示缺货商品数量提示 -->
            <span v-if="outOfStockCount > 0" class="stock-tip">
              （缺货{{ outOfStockCount }}件商品不可选）
            </span>
          </label>
          <button class="delete-btn" @click="batchDelete" :disabled="selectedIds.length === 0">
            <i class="fas fa-trash-alt"></i>
            删除选中
          </button>
        </div>

        <!-- 购物车商品列表 -->
        <div class="cart-list">
          <div v-for="item in cartItems" :key="item.id" class="cart-item" :class="{ 'out-of-stock': item.stock <= 0 }">
            <!-- 商品复选框，缺货时禁用勾选 -->
            <div class="item-checkbox">
              <input
                type="checkbox"
                :value="item.id"
                v-model="selectedIds"
                :disabled="item.stock <= 0"
              />
            </div>

            <!-- 商品图片，点击跳转商品详情 -->
            <div class="item-image" @click="goToProduct(item.productId)">
              <img :src="item.productImage" :alt="item.productName" />
            </div>

            <!-- 商品信息区域 -->
            <div class="item-info" @click="goToProduct(item.productId)">
              <div class="item-name">{{ item.productName }}</div>
              <div class="item-brand">{{ item.brand || '官方旗舰店' }}</div>
              <!-- 缺货标签 -->
              <span v-if="item.stock <= 0" class="out-of-stock-tag">缺货</span>
            </div>

            <!-- 商品价格区域 -->
            <div class="item-price">
              <div class="current-price">¥{{ formatPrice(item.price) }}</div>
              <div v-if="item.originalPrice" class="original-price">¥{{ formatPrice(item.originalPrice) }}</div>
            </div>

            <!-- 数量控制器，缺货时禁用 -->
            <div class="item-quantity">
              <button class="quantity-btn" @click="decreaseQuantity(item)" :disabled="item.quantity <= 1 || item.stock <= 0">
                <i class="fas fa-minus"></i>
              </button>
              <input
                type="number"
                class="quantity-input"
                v-model.number="item.quantity"
                @change="updateQuantity(item)"
                min="1"
                :max="item.stock"
                :disabled="item.stock <= 0"
              />
              <button class="quantity-btn" @click="increaseQuantity(item)" :disabled="item.quantity >= item.stock || item.stock <= 0">
                <i class="fas fa-plus"></i>
              </button>
            </div>

            <!-- 商品小计金额 -->
            <div class="item-subtotal">
              ¥{{ formatPrice(item.price * item.quantity) }}
            </div>

            <!-- 操作按钮 -->
            <div class="item-actions">
              <button class="action-btn" @click="removeItem(item.id)" title="删除">
                <i class="fas fa-trash-alt"></i>
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- 结算栏 -->
      <div class="cart-summary">
        <div class="summary-info">
          <div class="summary-row">
            <span>已选商品</span>
            <span class="selected-count">{{ selectedCount }} 件</span>
          </div>
          <div class="summary-row total">
            <span>合计</span>
            <span class="total-amount">¥{{ formatPrice(totalAmount) }}</span>
          </div>
          <div class="summary-tip">
            <i class="fas fa-info-circle"></i>
            不含运费，运费将在下单时计算
          </div>
        </div>
        <!-- 去结算按钮，无选中商品时禁用 -->
        <button class="checkout-btn" @click="goToCheckout" :disabled="selectedCount === 0">
          去结算 ({{ selectedCount }})
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  // ========== 依赖导入 ==========
  import { ref, computed, onMounted } from 'vue'
  import { useRouter } from 'vue-router'
  import { authAPI } from '@/api/authAPI'
  import Message from '@/utils/message'
  import { useAuthStore } from '@/stores/auth'

  // 路由实例
  const router = useRouter()

  // ========== 全局状态 ==========
  const authStore = useAuthStore()

  // ========== 类型定义 ==========
  /** 购物车商品项 */
  interface CartItem {
    id: number           // 购物车项ID
    userId: number       // 用户ID
    productId: number    // 商品ID
    quantity: number     // 购买数量
    productName: string  // 商品名称
    brand: string        // 品牌
    price: number        // 单价
    originalPrice?: number  // 原价
    productImage: string // 商品图片
    stock: number        // 库存数量
  }

  // ========== 响应式数据 ==========
  /** 加载状态 */
  const loading = ref(false)
  /** 购物车列表数据 */
  const cartItems = ref<CartItem[]>([])
  /** 已选中的购物车项ID列表 */
  const selectedIds = ref<number[]>([])

  // ========== 计算属性 ==========

  /**
   * 可选的购物车项（库存大于0的商品）
   */
  const selectableItems = computed(() =>
    cartItems.value.filter(item => item.stock > 0)
  )

  /**
   * 缺货商品数量（库存为0的商品）
   */
  const outOfStockCount = computed(() =>
    cartItems.value.filter(item => item.stock <= 0).length
  )

  /**
   * 是否全选（只针对可选商品）
   */
  const isAllSelected = computed(() => {
    // 如果没有可选商品，返回false
    if (selectableItems.value.length === 0) return false
    // 获取已选中的可选商品ID集合
    const selectedSelectableIds = selectedIds.value.filter(id =>
      selectableItems.value.some(item => item.id === id)
    )
    // 判断是否全选
    return selectedSelectableIds.length === selectableItems.value.length
  })

  /** 选中商品总件数（只统计可选商品） */
  const selectedCount = computed(() =>
    cartItems.value
      .filter(item => selectedIds.value.includes(item.id) && item.stock > 0)
      .reduce((sum, item) => sum + item.quantity, 0)
  )

  /** 选中商品总金额（只统计可选商品） */
  const totalAmount = computed(() =>
    cartItems.value
      .filter(item => selectedIds.value.includes(item.id) && item.stock > 0)
      .reduce((sum, item) => sum + item.price * item.quantity, 0)
  )

  // ========== 业务方法 ==========

  /**
   * 获取购物车列表
   */
  const loadCartList = async () => {
    loading.value = true
    try {
      const response = await authAPI.getCartList()
      if (response.success && response.data?.records) {
        cartItems.value = response.data.records
        // 加载完成后，清空已选中的缺货商品ID
        selectedIds.value = selectedIds.value.filter(id => {
          const item = cartItems.value.find(i => i.id === id)
          return item && item.stock > 0
        })
      } else {
        throw new Error(response.message || '加载购物车失败')
      }
    } catch (error: any) {
      console.error('[Cart] 加载购物车失败:', error)
      Message.error(error.message || '加载购物车失败')
    } finally {
      loading.value = false
    }
  }

  /**
   * 全选/取消全选
   * 只操作可选商品（库存大于0的商品），缺货商品不会被选中
   */
  const toggleSelectAll = () => {
    if (isAllSelected.value) {
      // 取消全选：清空所有选中
      selectedIds.value = []
    } else {
      // 全选：只选中可选商品的ID（缺货商品不选中）
      const allSelectableIds = selectableItems.value.map(item => item.id)
      selectedIds.value = [...allSelectableIds]

      // 如果有缺货商品，给出提示
      if (outOfStockCount.value > 0) {
        Message.info(`已选中${allSelectableIds.length}件商品，缺货商品不可选`)
      }
    }
  }

  /**
   * 更新商品数量（调用API）
   * @param item - 目标购物车项
   */
  const updateQuantity = async (item: CartItem) => {
    // 边界校验
    if (item.quantity < 1) {
      item.quantity = 1
      return
    }
    if (item.quantity > item.stock) {
      item.quantity = item.stock
      Message.error(`库存不足，最多可购买${item.stock}件`)
      return
    }

    try {
      const response = await authAPI.updateCartItem({
        cartItemId: item.id,
        quantity: item.quantity
      })
      if (!response.success) {
        throw new Error(response.message || '更新失败')
      }
    } catch (error: any) {
      console.error('[Cart] 更新数量失败:', error)
      Message.error(error.message || '更新失败')
      await loadCartList() // 回滚数据
    }
  }

  /** 减少商品数量 */
  const decreaseQuantity = (item: CartItem) => {
    if (item.quantity > 1) {
      item.quantity--
      updateQuantity(item)
    }
  }

  /** 增加商品数量 */
  const increaseQuantity = (item: CartItem) => {
    if (item.quantity < item.stock) {
      item.quantity++
      updateQuantity(item)
    } else {
      Message.error(`库存不足，最多可购买${item.stock}件`)
    }
  }

  /**
   * 删除单个购物车商品（带二次确认）
   * @param cartItemId - 购物车项ID
   */
  const removeItem = async (cartItemId: number) => {
    try {
      await Message.confirm('确定要删除该商品吗？', '删除确认')
      const response = await authAPI.deleteCartItem(cartItemId)
      if (response.success) {
        Message.success('删除成功')
        // 从已选列表中移除
        selectedIds.value = selectedIds.value.filter(id => id !== cartItemId)
        await loadCartList()
      } else {
        throw new Error(response.message || '删除失败')
      }
    } catch (error: any) {
      if (error !== 'cancel') {
        Message.error(error.message || '删除失败')
      }
    }
  }

  /**
   * 批量删除选中的商品（带二次确认）
   */
  const batchDelete = async () => {
    if (selectedIds.value.length === 0) return

    try {
      await Message.confirm(`确定要删除选中的 ${selectedIds.value.length} 个商品吗？`, '批量删除确认')
      const response = await authAPI.batchDeleteCartItems(selectedIds.value)
      if (response.success) {
        Message.success(`成功删除${selectedIds.value.length}个商品`)
        selectedIds.value = []
        await loadCartList()
      } else {
        throw new Error(response.message || '批量删除失败')
      }
    } catch (error: any) {
      if (error !== 'cancel') {
        Message.error(error.message || '批量删除失败')
      }
    }
  }

  /**
   * 跳转至商品详情页
   * @param productId - 商品ID
   */
  const goToProduct = (productId: number) => {
    router.push({ name: 'ProductDetail', params: { productId } })
  }

  /**
   * 去结算：校验选中商品并跳转至订单确认页
   */
  const goToCheckout = () => {
    if (selectedCount.value === 0) {
      Message.error('请选择要结算的商品')
      return
    }
    router.push({
      path: '/checkout',
      query: {
        source: 'cart',
        cartItemIds: selectedIds.value.join(',')
      }
    })
  }

  // ========== 工具函数 ==========
  /** 格式化金额（保留两位小数） */
  const formatPrice = (price: number) => price.toFixed(2)

  // ========== 生命周期 ==========
  onMounted(async () => {
    // 未登录则跳转登录页（方法内部处理）
    if (!authStore.validateUserPermission()) return
    await loadCartList()
  })
</script>

<style scoped>
  @import url('@/static/css/user/购物车.css');
</style>
