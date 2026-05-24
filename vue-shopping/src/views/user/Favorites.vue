<template>
  <div class="favorites-page">
    <!-- 导航栏 -->
    <div class="cart-navbar">
      <button class="cart-nav-back" @click="router.back()">
        <i class="fas fa-chevron-left"></i>
      </button>
      <div class="cart-nav-title">
        <i class="fas fa-heart"></i>
        <span>我的收藏</span>
      </div>
    </div>

    <!-- 操作栏 -->
    <div class="cart-actions-bar" v-if="favorites.length > 0">
      <div class="select-all" @click="toggleSelectAll">
        <i :class="isAllSelected ? 'fas fa-check-circle' : 'far fa-circle'"></i>
        <span>全选</span>
      </div>
      <span class="cart-items-count">共{{ favorites.length }}件</span>
      <button class="delete-btn" @click="batchDelete" :disabled="selectedIds.length === 0">删除</button>
    </div>

    <!-- 骨架屏 -->
    <div v-if="loading" class="favorites-content">
      <div class="cart-list">
        <div v-for="n in 4" :key="n" class="skeleton-card">
          <div class="cart-item-top">
            <div class="skeleton-avatar" style="width:70px;height:70px;border-radius:8px;"></div>
            <div class="item-info">
              <div class="skeleton-line medium"></div>
              <div class="skeleton-line short" style="width:40%"></div>
              <div class="skeleton-line short" style="width:30%"></div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else-if="favorites.length === 0" class="empty-cart">
      <i class="fas fa-heart"></i>
      <p>暂无收藏商品</p>
      <RouterLink :to="{name: 'UserDashboard'}" class="btn btn-primary">去逛逛</RouterLink>
    </div>

    <!-- 收藏列表 -->
    <div v-else class="favorites-content">
      <div class="cart-list">
        <div
          v-for="item in favorites"
          :key="item.id"
          class="cart-item"
          :class="{
            'out-of-stock': item.stock <= 0,
            'selected': selectedIds.includes(item.id)
          }"
          @click="toggleSelectItem(item)"
        >
          <div class="cart-item-top">
            <div class="item-image" @click.stop="viewProduct(item.productId)">
              <img :src="item.productImage || '/images/default-product.jpg'" :alt="item.productName" />
            </div>

            <div class="item-info">
              <div class="item-name text-ellipsis" @click.stop="viewProduct(item.productId)">{{ item.productName }}</div>
              <div class="tags-group">
                <span v-if="item.skuName" class="item-sku text-ellipsis">{{ item.skuName }}</span>
                <span class="item-time text-ellipsis">{{ formatDate(item.createdAt) }}</span>
                <span v-if="item.stock <= 0" class="out-of-stock-tag">缺货</span>
              </div>
            </div>

            <div class="item-price-right">
              <span class="current-price">¥{{ formatPrice(item.price) }}</span>
              <span v-if="item.originalPrice && item.originalPrice > item.price" class="original-price text-ellipsis">¥{{ formatPrice(item.originalPrice) }}</span>
            </div>
          </div>

          <div class="item-bottom">
            <div class="item-status">
              <span v-if="item.stock <= 0" class="stock-text">暂时缺货</span>
              <template v-else>
                <span v-if="selectedIds.includes(item.id)" class="selected-text">
                  <i class="fas fa-check"></i> 已选
                </span>
                <span v-else class="unselected-text">点此选择</span>
              </template>
            </div>

            <div class="item-right-actions">
              <button class="cart-btn" @click.stop="addToCart(item)" :disabled="item.stock === 0">加入购物车</button>
              <span class="delete-text-btn" @click.stop="removeFavorite(item.id)">删除</span>
            </div>
          </div>
        </div>
      </div>

      <div v-if="loadingMore" class="loading-more-bar">
        <i class="fas fa-spinner fa-spin"></i> 加载更多...
      </div>
      <div v-else-if="!hasMore && favorites.length > 0" class="no-more-bar">— 已经到底了 —</div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { ref, computed, onMounted, onUnmounted } from 'vue'
  import { useRouter } from 'vue-router'
  import { authAPI } from '@/api/authAPI'
  import Message from '@/utils/message'
  import { useAuthStore } from '@/stores/auth'
  import { storeToRefs } from 'pinia'


  const authStore = useAuthStore()
  const { isLoggedIn, isSeller, isAdmin } = storeToRefs(authStore)

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
    skuId?: number
    productName: string
    skuName?: string
    brand: string
    price: number
    originalPrice?: number
    stock: number
    productImage: string
    createdAt: string
  }

  // ==================== 响应式数据 ====================

  const loading = ref(true)
  const loadingMore = ref(false)
  const hasMore = ref(true)
  const favorites = ref<FavoriteItem[]>([])
  const selectedIds = ref<number[]>([])
  const currentPage = ref(1)
  const pageSize = ref(20)
  const total = ref(0)
  const isEditing = ref(false)

  // ==================== 计算属性 ====================

  const selectableItems = computed(() =>
    favorites.value.filter(item => item.stock > 0)
  )

  const isAllSelected = computed(() => {
    if (selectableItems.value.length === 0) return false
    const selectedSelectableIds = selectedIds.value.filter(id =>
      selectableItems.value.some(item => item.id === id)
    )
    return selectedSelectableIds.length === selectableItems.value.length
  })

  // ==================== 数据加载 ====================

  const loadFavorites = async () => {
    if (loadingMore.value) return

    if (currentPage.value === 1) {
      loading.value = true
    } else {
      loadingMore.value = true
    }

    try {
      const response = await authAPI.getFavorites({
        page: currentPage.value,
        pageSize: pageSize.value
      })

      if (response.success) {
        const data = response.data || {}
        const items = Array.isArray(data) ? data : (data.records || [])

        if (currentPage.value === 1) {
          favorites.value = items
          selectedIds.value = selectedIds.value.filter(id => {
            const item = favorites.value.find(i => i.id === id)
            return item && item.stock > 0
          })
        } else {
          favorites.value.push(...items)
        }

        total.value = data.total || items.length
        hasMore.value = items.length >= pageSize.value
      }
    } catch (error: unknown) {
      const err = error as { message?: string }
      Message.error(err?.message || '加载失败')
    } finally {
      loading.value = false
      loadingMore.value = false
    }
  }

  const loadMore = async () => {
    if (loadingMore.value || !hasMore.value) return
    currentPage.value++
    await loadFavorites()
  }

  const handleScroll = () => {
    if (loadingMore.value || !hasMore.value || loading.value) return

    const { scrollTop, scrollHeight, clientHeight } = document.documentElement
    if (scrollTop + clientHeight >= scrollHeight - 150) {
      loadMore()
    }
  }

  // ==================== 选择操作 ====================

  const toggleSelectItem = (item: FavoriteItem) => {
    if (item.stock <= 0) return

    const index = selectedIds.value.indexOf(item.id)
    if (index > -1) {
      selectedIds.value.splice(index, 1)
    } else {
      selectedIds.value.push(item.id)
    }
  }

  const toggleSelectAll = () => {
    if (isAllSelected.value) {
      selectedIds.value = []
    } else {
      const allSelectableIds = selectableItems.value.map(item => item.id)
      selectedIds.value = [...allSelectableIds]
    }
  }

  // ==================== 收藏操作 ====================

  const removeFavorite = async (favoriteId: number) => {
    try {
      await Message.confirm('确定要删除该收藏吗？', '删除确认')

      const response = await authAPI.removeFavorite(favoriteId)
      if (response.success) {
        Message.success('删除成功')
        selectedIds.value = selectedIds.value.filter(id => id !== favoriteId)
        currentPage.value = 1
        hasMore.value = true
        await loadFavorites()
      }
    } catch (error: any) {
      if (error !== 'cancel') {
        Message.error(error.message || '删除失败')
      }
    }
  }

  const batchDelete = async () => {
    if (selectedIds.value.length === 0) return

    try {
      await Message.confirm(`确定要删除选中的 ${selectedIds.value.length} 个收藏吗？`, '批量删除确认')

      const response = await authAPI.batchRemoveFavorites(selectedIds.value)
      if (response.success) {
        Message.success('删除成功')
        selectedIds.value = []
        currentPage.value = 1
        hasMore.value = true
        await loadFavorites()
      }
    } catch (error: any) {
      if (error !== 'cancel') {
        Message.error(error.message || '删除失败')
      }
    }
  }

  // ==================== 购物车操作 ====================

  const addToCart = async (item: FavoriteItem) => {
    if (!isLoggedIn.value) {
      Message.error('请先登录后再添加')
      router.push({ name: 'Login' })
      return
    }

    try {
      const response = await authAPI.addToCart({
        productId: item.productId,
        quantity: 1
      })
      if (response.success) {
        Message.success('已添加到购物车')
      }
    } catch (error: any) {
      Message.error(error.message || '添加失败')
    }
  }

  // ==================== 页面跳转 ====================

  const viewProduct = (productId: number) => {
    router.push({
      name: 'ProductDetail',
      params: { productId }
    })
  }

  // ==================== 工具函数 ====================

  const formatPrice = (price: number): string => {
    if (price == null || isNaN(price)) return '0.00'
    return price.toFixed(2)
  }

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
    window.addEventListener('scroll', handleScroll)
  })

  onUnmounted(() => {
    window.removeEventListener('scroll', handleScroll)
  })
</script>

<style scoped>
@import url('@/static/css/user/收藏页.css');
</style>
