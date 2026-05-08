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
import { showToast, showConfirmDialog } from 'vant'
import 'vant/es/toast/style'
import 'vant/es/dialog/style'
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
    if (response.success && response.data) {
      cartItems.value = response.data
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
    showToast({ message: error.message || '加载购物车失败', type: 'fail', duration: 2000 })
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
      showToast({ 
        message: `已选中${allSelectableIds.length}件商品，缺货商品不可选`, 
        type: 'info',
        duration: 2000
      })
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
    showToast({ message: `库存不足，最多可购买${item.stock}件`, type: 'fail' })
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
    showToast({ message: error.message || '更新失败', type: 'fail' })
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
    showToast({ message: `库存不足，最多可购买${item.stock}件`, type: 'fail' })
  }
}

/**
 * 删除单个购物车商品（带二次确认）
 * @param cartItemId - 购物车项ID
 */
const removeItem = async (cartItemId: number) => {
  try {
    await showConfirmDialog({
      title: '删除确认',
      message: '确定要删除该商品吗？',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
    const response = await authAPI.deleteCartItem(cartItemId)
    if (response.success) {
      showToast({ message: '删除成功', type: 'success' })
      // 从已选列表中移除
      selectedIds.value = selectedIds.value.filter(id => id !== cartItemId)
      await loadCartList()
    } else {
      throw new Error(response.message || '删除失败')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      showToast({ message: error.message || '删除失败', type: 'fail' })
    }
  }
}

/**
 * 批量删除选中的商品（带二次确认）
 */
const batchDelete = async () => {
  if (selectedIds.value.length === 0) return

  try {
    await showConfirmDialog({
      title: '批量删除确认',
      message: `确定要删除选中的 ${selectedIds.value.length} 个商品吗？`,
      confirmButtonText: '确定删除',
      cancelButtonText: '取消'
    })
    const response = await authAPI.batchDeleteCartItems(selectedIds.value)
    if (response.success) {
      showToast({ message: `成功删除${selectedIds.value.length}个商品`, type: 'success' })
      selectedIds.value = []
      await loadCartList()
    } else {
      throw new Error(response.message || '批量删除失败')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      showToast({ message: error.message || '批量删除失败', type: 'fail' })
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
    showToast({ message: '请选择要结算的商品', type: 'fail' })
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
/* ========== 购物车容器 ========== */
.cart-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
  min-height: 100vh;
  background: #f5f7fa;
}

/* ========== 页面头部 ========== */
.page-header {
  margin-bottom: 24px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.page-title i {
  color: #4a6491;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #64748b;
}

.breadcrumb a {
  color: #4a6491;
  text-decoration: none;
}

.breadcrumb .current {
  color: #1e293b;
  font-weight: 500;
}

/* ========== 加载状态 ========== */
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid #e2e8f0;
  border-top-color: #4a6491;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* ========== 空购物车 ========== */
.empty-cart {
  text-align: center;
  padding: 80px 20px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.empty-cart i {
  font-size: 80px;
  color: #cbd5e1;
  margin-bottom: 20px;
}

.empty-cart p {
  font-size: 16px;
  color: #64748b;
  margin-bottom: 24px;
}

/* ========== 购物车内容 ========== */
.cart-content {
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
}

.cart-main {
  flex: 1;
  min-width: 0;
}

.cart-summary {
  width: 320px;
  flex-shrink: 0;
}

/* ========== 操作栏 ========== */
.cart-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: white;
  padding: 16px 20px;
  border-radius: 12px;
  margin-bottom: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.select-all {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-size: 14px;
  color: #475569;
}

.select-all input {
  width: 18px;
  height: 18px;
  cursor: pointer;
}

.stock-tip {
  font-size: 12px;
  color: #ff4757;
  font-weight: normal;
  margin-left: 4px;
}

.delete-btn {
  background: none;
  border: none;
  color: #ef4444;
  font-size: 14px;
  cursor: pointer;
  padding: 6px 12px;
  border-radius: 6px;
  transition: all 0.3s;
}

.delete-btn:hover:not(:disabled) {
  background: #fef2f2;
}

.delete-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* ========== 购物车列表 ========== */
.cart-list {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.cart-item {
  display: flex;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #e2e8f0;
  transition: background 0.3s;
}

.cart-item:hover {
  background: #fafbfc;
}

/* 缺货商品样式 */
.cart-item.out-of-stock {
  opacity: 0.7;
  background: #fafbfc;
}

.item-checkbox {
  width: 40px;
  flex-shrink: 0;
}

.item-checkbox input {
  width: 18px;
  height: 18px;
  cursor: pointer;
}

/* 禁用复选框样式 */
.item-checkbox input:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.item-image {
  width: 80px;
  height: 80px;
  flex-shrink: 0;
  cursor: pointer;
}

.item-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
}

.item-info {
  flex: 1;
  padding: 0 16px;
  cursor: pointer;
}

.item-name {
  font-size: 15px;
  font-weight: 500;
  color: #1e293b;
  margin-bottom: 6px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.item-brand {
  font-size: 12px;
  color: #94a3b8;
}

/* 缺货标签 */
.out-of-stock-tag {
  display: inline-block;
  margin-top: 6px;
  padding: 2px 8px;
  background: #ff4757;
  color: white;
  font-size: 10px;
  border-radius: 4px;
}

.item-price {
  width: 100px;
  flex-shrink: 0;
  text-align: center;
}

.current-price {
  font-size: 16px;
  font-weight: 600;
  color: #ef4444;
}

.original-price {
  font-size: 12px;
  color: #94a3b8;
  text-decoration: line-through;
  margin-top: 2px;
}

.item-quantity {
  width: 120px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.quantity-btn {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  border: 1px solid #e2e8f0;
  background: white;
  cursor: pointer;
  transition: all 0.3s;
}

.quantity-btn:hover:not(:disabled) {
  background: #f1f5f9;
  border-color: #cbd5e1;
}

.quantity-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.quantity-input {
  width: 50px;
  height: 28px;
  text-align: center;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 14px;
}

.quantity-input:focus {
  outline: none;
  border-color: #4a6491;
}

.quantity-input:disabled {
  background: #f1f5f9;
  cursor: not-allowed;
}

.item-subtotal {
  width: 100px;
  flex-shrink: 0;
  text-align: center;
  font-size: 16px;
  font-weight: 600;
  color: #ef4444;
}

.item-actions {
  width: 40px;
  flex-shrink: 0;
  text-align: center;
}

.action-btn {
  background: none;
  border: none;
  color: #94a3b8;
  cursor: pointer;
  padding: 6px;
  border-radius: 6px;
  transition: all 0.3s;
}

.action-btn:hover {
  color: #ef4444;
  background: #fef2f2;
}

/* ========== 结算栏 ========== */
.cart-summary {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  position: sticky;
  top: 80px;
  height: fit-content;
}

.summary-info {
  margin-bottom: 20px;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  font-size: 14px;
  color: #64748b;
}

.summary-row.total {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #e2e8f0;
  font-size: 18px;
  font-weight: 600;
}

.summary-row.total .total-amount {
  font-size: 24px;
  color: #ef4444;
}

.selected-count {
  font-weight: 500;
  color: #4a6491;
}

.summary-tip {
  font-size: 12px;
  color: #94a3b8;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 12px 0;
  border-top: 1px solid #e2e8f0;
}

.checkout-btn {
  width: 100%;
  padding: 14px;
  background: #ef4444;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
}

.checkout-btn:hover:not(:disabled) {
  background: #dc2626;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(239, 68, 68, 0.3);
}

.checkout-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* ========== 按钮样式 ========== */
.btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
  border: none;
  text-decoration: none;
}

.btn-primary {
  background: #4a6491;
  color: white;
}

.btn-primary:hover {
  background: #3a5479;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(74, 100, 145, 0.3);
}

/* ========== 响应式 ========== */
@media (max-width: 968px) {
  .cart-content {
    flex-direction: column;
  }
  
  .cart-summary {
    width: 100%;
    position: static;
  }
}

@media (max-width: 768px) {
  .cart-item {
    flex-wrap: wrap;
    gap: 12px;
  }
  
  .item-checkbox {
    order: 1;
  }
  
  .item-image {
    order: 2;
  }
  
  .item-info {
    order: 3;
    width: calc(100% - 140px);
  }
  
  .item-price {
    order: 4;
    width: auto;
    margin-left: 40px;
  }
  
  .item-quantity {
    order: 5;
  }
  
  .item-subtotal {
    order: 6;
    width: auto;
    margin-left: auto;
  }
  
  .item-actions {
    order: 7;
  }
  
  .cart-actions {
    flex-wrap: wrap;
    gap: 12px;
  }
  
  .page-header {
    margin-bottom: 16px;
  }
  
  .page-title {
    font-size: 20px;
  }
}
</style>