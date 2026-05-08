<!-- views/user/Favorites.vue - 我的收藏页面 -->
<template>
  <div class="favorites-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <h3 class="page-title">
        <i class="fas fa-heart"></i>
        我的收藏
      </h3>
      <button 
        v-if="total > 0" 
        class="batch-delete-btn" 
        @click="batchDelete"
        :disabled="selectedIds.length === 0"
      >
        批量删除
        <span v-if="selectedIds.length > 0" class="badge">{{ selectedIds.length }}</span>
      </button>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>

    <!-- 空状态 -->
    <div v-else-if="favorites.length === 0" class="empty-state">
      <i class="far fa-heart"></i>
      <p>暂无收藏商品</p>
      <button class="btn-primary" @click="goShopping">去逛逛</button>
    </div>

    <!-- 收藏列表 -->
    <div v-else class="favorites-list">
      <div v-for="item in favorites" :key="item.id" class="favorite-card">
        <!-- 复选框（批量删除用） -->
        <div class="item-checkbox">
          <input type="checkbox" v-model="selectedIds" :value="item.id" />
        </div>

        <!-- 商品图片 -->
        <div class="item-image" @click="viewProduct(item.productId)">
          <img :src="item.productImage || '/images/default-product.jpg'" :alt="item.productName">
        </div>

        <!-- 商品信息 -->
        <div class="item-info" @click="viewProduct(item.productId)">
          <div class="item-name">{{ item.productName }}</div>
          <div class="item-brand">{{ item.brand || '官方旗舰店' }}</div>
          <div class="item-time">收藏时间：{{ formatDate(item.createdAt) }}</div>
        </div>

        <!-- 商品价格 -->
        <div class="item-price">
          <div class="current-price">¥{{ formatPrice(item.price) }}</div>
          <div v-if="item.originalPrice" class="original-price">¥{{ formatPrice(item.originalPrice) }}</div>
        </div>

        <!-- 库存状态 -->
        <div class="item-stock">
          <span :class="item.stock > 0 ? 'in-stock' : 'out-stock'">
            {{ item.stock > 0 ? '有货' : '缺货' }}
          </span>
        </div>

        <!-- 操作按钮 -->
        <div class="item-actions">
          <button class="action-btn cart-btn" @click="addToCart(item)" :disabled="item.stock === 0">
            <i class="fas fa-shopping-cart"></i>
            加入购物车
          </button>
          <button class="action-btn delete-btn" @click="removeFavorite(item.id)">
            <i class="fas fa-trash-alt"></i>
            删除
          </button>
        </div>
      </div>
    </div>

    <!-- ========== 分页组件 ========== -->
    <div class="pagination" v-if="totalPages > 1">
      <!-- 上一页按钮 -->
      <button 
        class="page-btn prev" 
        :disabled="currentPage === 1" 
        @click="goToPage(currentPage - 1)"
      >
        <i class="fas fa-chevron-left"></i> 上一页
      </button>
      
      <!-- 页码按钮（动态生成） -->
      <div class="page-numbers">
        <button 
          v-for="page in visiblePages" 
          :key="page"
          class="page-num"
          :class="{ active: currentPage === page, dots: page === '...' }"
          :disabled="page === '...'"
          @click="page !== '...' && goToPage(page)"
        >
          {{ page }}
        </button>
      </div>
      
      <!-- 下一页按钮 -->
      <button 
        class="page-btn next" 
        :disabled="currentPage === totalPages" 
        @click="goToPage(currentPage + 1)"
      >
        下一页 <i class="fas fa-chevron-right"></i>
      </button>
    </div>

    <!-- 简单分页（移动端） -->
    <div class="simple-pagination" v-if="totalPages > 1">
      <button class="page-btn" :disabled="currentPage === 1" @click="goToPage(currentPage - 1)">
        <i class="fas fa-chevron-left"></i>
      </button>
      <span class="page-info">{{ currentPage }} / {{ totalPages }}</span>
      <button class="page-btn" :disabled="currentPage === totalPages" @click="goToPage(currentPage + 1)">
        <i class="fas fa-chevron-right"></i>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { ref, computed, onMounted } from 'vue'
  import { useRouter } from 'vue-router'
  import { authAPI } from '@/api/authAPI'
  import { showToast, showConfirmDialog } from 'vant'
  import 'vant/es/toast/style'
  import 'vant/es/dialog/style'
  import { useAuthStore } from '@/stores/auth'
  import { storeToRefs } from 'pinia'

  const authStore = useAuthStore()
  const { isLoggedIn, role, status } = storeToRefs(authStore)

  const router = useRouter()

  // ==================== 类型定义 ====================

  interface ApiResponse<T = any> {
    success: boolean
    data: T
    total?: number
    totalPages?: number
    page?: number
    size?: number
    message?: string
  }

  interface FavoriteItem {
    id: number
    productId: number
    productName: string
    brand: string
    price: number
    originalPrice?: number
    stock: number
    productImage: string
    createdAt: string
  }

  // ==================== 响应式数据 ====================

  const loading = ref(false)
  const favorites = ref<FavoriteItem[]>([])
  const selectedIds = ref<number[]>([])
  const currentPage = ref(1)
  const pageSize = ref(10)
  const total = ref(0)
  const totalPages = ref(1)

  // ==================== 分页页码 ====================

  /**
   * 带省略号的分页页码数组
   * @description 当前页前后显示2页，首尾固定显示
   */
  const visiblePages = computed(() => {
    const delta = 2
    const range: number[] = []
    const rangeWithDots: (number | string)[] = []
    let l: number

    for (let i = 1; i <= totalPages.value; i++) {
      if (i === 1 || i === totalPages.value || (i >= currentPage.value - delta && i <= currentPage.value + delta)) {
        range.push(i)
      }
    }

    for (const i of range) {
      if (l) {
        if (i - l === 2) {
          rangeWithDots.push(l + 1)
        } else if (i - l !== 1) {
          rangeWithDots.push('...')
        }
      }
      rangeWithDots.push(i)
      l = i
    }
    return rangeWithDots
  })

  // ==================== 数据加载 ====================

  /**
   * 加载收藏列表
   * @returns {Promise<void>}
   */
  const loadFavorites = async () => {
    loading.value = true
    try {
      const response = await authAPI.getFavorites({
        page: currentPage.value,
        size: pageSize.value
      }) as unknown as ApiResponse<FavoriteItem[]>
      
      if (response.success) {
        favorites.value = response.data || []
        total.value = response.total || 0
        totalPages.value = response.totalPages || 1
      }
    } catch (error: any) {
      showToast({ message: error.message || '加载失败', type: 'fail' })
    } finally {
      loading.value = false
    }
  }

  /**
   * 跳转到指定页
   * @param {number} page - 目标页码
   */
  const goToPage = (page: number) => {
    if (page < 1 || page > totalPages.value) return
    currentPage.value = page
    loadFavorites()
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  // ==================== 收藏操作 ====================

  /**
   * 删除单个收藏
   * @param {number} favoriteId - 收藏ID
   * @returns {Promise<void>}
   */
  const removeFavorite = async (favoriteId: number) => {
    try {
      await showConfirmDialog({
        title: '删除确认',
        message: '确定要删除该收藏吗？'
      })
      
      const response = await authAPI.removeFavorite(favoriteId)
      if (response.success) {
        showToast({ message: '删除成功', type: 'success' })
        // 当前页无数据时回退到上一页
        if (favorites.value.length === 1 && currentPage.value > 1) {
          currentPage.value--
        }
        loadFavorites()
      }
    } catch (error: any) {
      if (error !== 'cancel') {
        showToast({ message: error.message || '删除失败', type: 'fail' })
      }
    }
  }

  /**
   * 批量删除收藏
   * @returns {Promise<void>}
   */
  const batchDelete = async () => {
    if (selectedIds.value.length === 0) return
    
    try {
      await showConfirmDialog({
        title: '批量删除确认',
        message: `确定要删除选中的 ${selectedIds.value.length} 个收藏吗？`
      })
      
      const response = await authAPI.batchRemoveFavorites(selectedIds.value)
      if (response.success) {
        showToast({ message: '删除成功', type: 'success' })
        selectedIds.value = []
        loadFavorites()
      }
    } catch (error: any) {
      if (error !== 'cancel') {
        showToast({ message: error.message || '删除失败', type: 'fail' })
      }
    }
  }

  // ==================== 购物车操作 ====================

  /**
   * 加入购物车
   * @param {FavoriteItem} item - 收藏商品对象
   * @returns {Promise<void>}
   */
  const addToCart = async (item: FavoriteItem) => {
    try {
      const response = await authAPI.addToCart({
        productId: item.productId,
        quantity: 1
      })
      if (response.success) {
        showToast({ message: '已添加到购物车', type: 'success' })
      }
    } catch (error: any) {
      showToast({ message: error.message || '添加失败', type: 'fail' })
    }
  }

  // ==================== 页面跳转 ====================

  /**
   * 查看商品详情
   * @param {number} productId - 商品ID
   */
  const viewProduct = (productId: number) => {
    router.push({
      name: 'ProductDetail',
      params: { productId }
    })
  }

  const goShopping = () => {
    router.push('/')
  }

  // ==================== 工具函数 ====================

  /**
   * 格式化价格
   * @param {number} price - 原始价格
   * @returns {string} 保留两位小数的价格字符串
   */
  const formatPrice = (price: number): string => {
    return price.toFixed(2)
  }

  /**
   * 格式化日期
   * @param {string} dateStr - ISO日期字符串
   * @returns {string} YYYY-MM-DD 格式
   */
  const formatDate = (dateStr: string): string => {
    if (!dateStr) return '-'
    const date = new Date(dateStr)
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}`
  }

  // ==================== 生命周期 ====================

  onMounted(() => {
    if (!authStore.validateUserPermission()) return
    
    loadFavorites()
  })
</script>

<style scoped>
@import url('@/static/css/user/收藏页.css');

</style>