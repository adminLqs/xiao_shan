<template>
  <div class="product-management-container">
    <div class="top-bar">
      <div class="search-box">
        <i class="fas fa-search"></i>
        <input type="text" v-model="searchKeyword" placeholder="搜索商品名称、品牌..." @keyup.enter="handleSearch" />
        <button v-if="searchKeyword" class="clear-search" @click="clearSearch">
          <i class="fas fa-times"></i>
        </button>
      </div>
      <RouterLink to="/seller/products/create" class="btn btn-primary">
        <i class="fas fa-plus-circle"></i> 发布商品
      </RouterLink>
    </div>

    <div class="filter-bar">
      <select v-model="filterStatus" class="filter-select" @change="handleSearch">
        <option value="">全部状态</option>
        <option value="1">上架</option>
        <option value="0">下架</option>
        <option value="2">回收站</option>
      </select>
      <button class="btn btn-outline btn-sm" @click="toggleBatchMode" v-if="!isBatchMode && filterStatus !== '2'">
        <i class="fas fa-trash-alt"></i> 批量删除
      </button>
    </div>

    <div class="batch-mode-bar" v-if="isBatchMode">
      <span class="batch-tip">已选择 {{ selectedIds.length }} 个商品</span>
      <div class="batch-actions">
        <button class="btn btn-sm btn-outline" @click="cancelBatchMode">
          <i class="fas fa-times"></i> 取消
        </button>
        <button
          class="btn btn-sm btn-danger"
          @click="handleBatchDelete"
          :disabled="selectedIds.length === 0"
        >
          <i class="fas fa-trash"></i> 确认删除
        </button>
      </div>
    </div>

    <div class="product-grid-container">
      <div v-if="isLoading" class="loading-state">
        <i class="fas fa-spinner fa-spin"></i>
        <span>加载中...</span>
      </div>

      <div v-else-if="products.length === 0" class="empty-state">
        <i class="fas fa-box-open"></i>
        <p>暂无商品</p>
        <RouterLink to="/seller/products/create" class="btn btn-primary btn-sm">
          立即发布商品
        </RouterLink>
      </div>

      <div v-else class="product-grid content-wrapper">
        <div
          v-for="product in products"
          :key="product.id"
          class="product-card"
          :class="{ selected: selectedIds.includes(product.id), deleted: product.status === 2 }"
        >
          <div class="card-checkbox" v-if="isBatchMode && product.status !== 2" @click.stop>
            <input
              type="checkbox"
              v-model="selectedIds"
              :value="product.id"
              @change="updateSelectAll"
            />
          </div>

          <div class="card-image-wrapper" @click="goToEdit(product.id)">
            <img
              :src="product.productImages?.[0]?.image || product.images || '/placeholder.png'"
              :alt="product.name"
              class="card-image"
            />
            <div v-if="product.status === 2" class="deleted-overlay">
              <span class="recycle-bin-tag">回收站</span>
            </div>
            <div class="card-overlay" v-if="product.status !== 2">
              <button class="overlay-btn" @click="goToEdit(product.id)">
                <i class="fas fa-edit"></i> 编辑
              </button>
            </div>
          </div>

          <div class="card-content">
            <div class="card-title">{{ product.name }}</div>
            <div class="card-brand">{{ product.brand || '官方旗舰店' }}</div>

            <div class="price-row">
              <span class="current-price">¥{{ formatPrice(product.price) }}</span>
            </div>

            <div class="card-footer" v-if="product.status !== 2">
              <div class="card-stock">
                <span :class="['stock-badge', getStockClass(product.stock)]">
                  <i :class="getStockIcon(product.stock)"></i>
                  {{ getStockText(product.stock) }}
                </span>
              </div>

              <div
                class="status-switch"
                :class="{ disabled: statusLoading }"
                @click="!statusLoading && toggleStatus(product, product.status === 1 ? 0 : 1)"
              >
                <div class="switch-slider" :class="{ active: product.status === 1 }">
                  <i v-if="product.status === 1" class="fas fa-eye"></i>
                  <i v-else class="fas fa-eye-slash"></i>
                </div>
                <span class="status-text" :class="{ active: product.status === 1 }">
                  {{ product.status === 1 ? '上架' : '下架' }}
                </span>
              </div>
            </div>

            <div class="card-actions" v-if="product.status !== 2">
              <button class="action-btn edit-btn" @click="goToEdit(product.id)" title="编辑">
                <i class="fas fa-edit"></i>
              </button>
              <button class="action-btn delete-btn" @click="deleteProduct(product)" title="删除">
                <i class="fas fa-trash-alt"></i>
              </button>
            </div>

            <div class="card-actions" v-else>
              <button class="action-btn restore-btn" @click="restoreProduct(product)" title="恢复">
                <i class="fas fa-undo"></i> 恢复
              </button>
            </div>
          </div>
        </div>
      </div>

      <div class="pagination" v-if="total > 0">
        <div class="pagination-left">
          <button class="page-btn" :disabled="currentPage === 1" @click="changePage(currentPage - 1)">
            <i class="fas fa-chevron-left"></i>
          </button>

          <!-- 页码按钮 -->
          <div class="page-numbers">
            <!-- 首页 -->
            <button
              v-if="currentPage > 3"
              class="page-number-btn"
              @click="changePage(1)"
            >1</button>

            <!-- 省略号 -->
            <span v-if="currentPage > 4" class="page-ellipsis">...</span>

            <!-- 当前页前后各2页 -->
            <button
              v-for="page in visiblePages"
              :key="page"
              class="page-number-btn"
              :class="{ active: page === currentPage }"
              @click="changePage(page)"
            >{{ page }}</button>

            <!-- 省略号 -->
            <span v-if="currentPage < totalPages - 3" class="page-ellipsis">...</span>

            <!-- 尾页 -->
            <button
              v-if="currentPage < totalPages - 2"
              class="page-number-btn"
              @click="changePage(totalPages)"
            >{{ totalPages }}</button>
          </div>

          <button class="page-btn" :disabled="currentPage === totalPages" @click="changePage(currentPage + 1)">
            <i class="fas fa-chevron-right"></i>
          </button>
        </div>

        <div class="pagination-right">
          <!-- 跳转输入框 -->
          <div class="page-jump">
            <span>跳转到</span>
            <input
              type="number"
              v-model="jumpPage"
              class="jump-input"
              min="1"
              :max="totalPages"
              @keyup.enter="handleJump"
              placeholder="页码"
            />
            <span>页</span>
            <button class="jump-btn" @click="handleJump">确定</button>
          </div>

          <!-- 每页条数选择 -->
          <select v-model="pageSize" class="page-size-select" @change="handleSearch">
            <option :value="20">20条/页</option>
            <option :value="50">50条/页</option>
            <option :value="100">100条/页</option>
          </select>

          <span class="total-info">共 {{ total }} 条商品</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { authAPI } from '@/api/authAPI'
import Message from '@/utils/message'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const router = useRouter()

interface Product {
  id: number
  name: string
  brand: string
  description?: string
  price: number | null
  stock: number | null
  status: number
  images?: string
  productImages?: Array<{ image: string }>
  categoryId: number
}

const isLoading = ref(false)
const products = ref<Product[]>([])
const selectedIds = ref<number[]>([])
const selectAll = ref(false)
const searchKeyword = ref('')
const filterStatus = ref('')
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const isBatchMode = ref(false)
const jumpPage = ref('')
const statusLoading = ref(false)

const totalPages = computed(() => Math.ceil(total.value / pageSize.value))

const visiblePages = computed(() => {
  const pages: number[] = []
  const start = Math.max(1, currentPage.value - 2)
  const end = Math.min(totalPages.value, currentPage.value + 2)

  for (let i = start; i <= end; i++) {
    if (!pages.includes(i)) pages.push(i)
  }
  return pages
})

const handleJump = () => {
  const page = parseInt(jumpPage.value)
  if (!isNaN(page) && page >= 1 && page <= totalPages.value) {
    changePage(page)
  } else {
    Message.error('请输入有效的页码')
  }
  jumpPage.value = ''
}

const loadProducts = async () => {
  isLoading.value = true

  try {
    const params: {
      page: number
      pageSize: number
      keyword: string
      status?: number
    } = {
      page: currentPage.value,
      pageSize: pageSize.value,
      keyword: searchKeyword.value
    }

    if (filterStatus.value !== '') {
      params.status = Number(filterStatus.value)
    }

    const response = await authAPI.getSellerProducts(params)

    if (response.success) {
      const data = response.data || {}
      products.value = data.records || []
      total.value = data.total || 0
    } else {
      throw new Error(response.message || '加载商品失败')
    }
  } catch (error: any) {
    Message.error(error.message || '加载商品失败，请重试')
  } finally {
    isLoading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  loadProducts()
}

const clearSearch = () => {
  searchKeyword.value = ''
  handleSearch()
}

const changePage = (page: number) => {
  currentPage.value = page
  loadProducts()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const toggleBatchMode = () => {
  if (isBatchMode.value) {
    cancelBatchMode()
  } else {
    isBatchMode.value = true
  }
}

const cancelBatchMode = () => {
  isBatchMode.value = false
  selectedIds.value = []
  selectAll.value = false
}

const updateSelectAll = () => {
  selectAll.value = selectedIds.value.length === products.value.length
}

const handleBatchDelete = async () => {
  if (selectedIds.value.length === 0) return

  try {
    await Message.confirm(`确定要删除选中的 ${selectedIds.value.length} 个商品吗？删除后不可恢复。`, '批量删除确认')

    const response = await authAPI.batchDeleteProducts(selectedIds.value)

    if (response.success) {
      Message.success(`成功删除 ${selectedIds.value.length} 个商品`)
      cancelBatchMode()
      loadProducts()
    } else {
      throw new Error(response.message || '批量删除失败')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      Message.error(error.message || '批量删除失败')
    }
  }
}

const goToEdit = (productId: number) => {
  router.push({
    name: 'SellerProductEdit',
    params: { productId }
  })
}

const toggleStatus = async (product: Product, status: number) => {
  if (statusLoading.value) return
  const action = status === 1 ? '上架' : '下架'

  try {
    await Message.confirm(`确定要${action}「${product.name}」吗？`, `${action}确认`)

    statusLoading.value = true
    const response = await authAPI.updateProductStatus(product.id, status)

    if (response.success) {
      Message.success(`${action}成功`)
      loadProducts()
    } else {
      Message.warning(response.message || `${action}失败`)
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      Message.error(error.message || `${action}失败`)
    }
  } finally {
    statusLoading.value = false
  }
}

const deleteProduct = async (product: Product) => {
  try {
    await Message.confirm(`确定要删除商品「${product.name}」吗？删除后可在回收站恢复。`, '删除确认')

    const response = await authAPI.deleteProduct(product.id)

    if (response.success) {
      Message.success('删除成功')
      loadProducts()
    } else {
      throw new Error(response.message || '删除失败')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      Message.error(error.message || '删除失败')
    }
  }
}

const restoreProduct = async (product: Product) => {
  try {
    await Message.confirm(`确定要恢复商品「${product.name}」吗？恢复后商品将变为下架状态。`, '恢复确认')

    const response = await authAPI.restoreProduct(product.id)

    if (response.success) {
      Message.success(response.message || '恢复成功')
      loadProducts()
    } else {
      throw new Error(response.message || '恢复失败')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      Message.error(error.message || '恢复失败')
    }
  }
}

const formatPrice = (price: number | null): string => {
  if (price == null || isNaN(price)) return '0.00'
  return price.toFixed(2)
}

const getStockClass = (stock: number | null): string => {
  if (stock == null || stock <= 0) return 'stock-out'
  if (stock < 10) return 'stock-low'
  return 'stock-normal'
}

const getStockIcon = (stock: number | null): string => {
  if (stock == null || stock <= 0) return 'fas fa-times-circle'
  if (stock < 10) return 'fas fa-exclamation-circle'
  return 'fas fa-check-circle'
}

const getStockText = (stock: number | null): string => {
  if (stock == null || stock <= 0) return '缺货'
  if (stock < 10) return `仅剩${stock}`
  return '有货'
}

onMounted(() => {
  if (!authStore.validateSellerPermission()) return
  loadProducts()
})
</script>

<style scoped>
@import url('@/static/css/seller/商品管理页.css');
</style>
