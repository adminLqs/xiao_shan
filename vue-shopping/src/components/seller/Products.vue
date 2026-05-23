<template>
  <div class="product-management-container">
    <div class="action-bar">
      <div class="action-left">
        <RouterLink to="/seller/products/create" class="btn btn-primary">
          <i class="fas fa-plus-circle"></i> 发布商品
        </RouterLink>
        <button class="btn btn-outline" @click="handleBatchDelete" :disabled="selectedIds.length === 0">
          <i class="fas fa-trash-alt"></i> 批量删除
          <span v-if="selectedIds.length > 0" class="badge">{{ selectedIds.length }}</span>
        </button>
      </div>

      <div class="action-right">
        <div class="search-box">
          <i class="fas fa-search"></i>
          <input
            type="text"
            v-model="searchKeyword"
            placeholder="搜索商品名称、品牌..."
            @keyup.enter="handleSearch"
          />
          <button v-if="searchKeyword" class="clear-search" @click="clearSearch">
            <i class="fas fa-times"></i>
          </button>
        </div>
        <select v-model="filterStatus" class="filter-select" @change="loadProducts">
          <option value="">全部商品</option>
          <option value="1">上架中</option>
          <option value="0">已下架</option>
        </select>
      </div>
    </div>

    <div class="card">
      <div class="card-body">
        <div v-if="isLoading" class="loading-state">
          <div class="loading-spinner"></div>
          <p>加载中...</p>
        </div>

        <div v-else-if="products.length === 0" class="empty-state">
          <i class="fas fa-box-open"></i>
          <p>暂无商品</p>
          <RouterLink to="/seller/products/create" class="btn btn-primary btn-sm">
            立即发布商品
          </RouterLink>
        </div>

        <div v-else class="product-table-wrapper">
          <table class="product-table">
            <thead>
              <tr>
                <th class="checkbox-col">
                  <input type="checkbox" @change="toggleSelectAll" v-model="selectAll" />
                </th>
                <th class="image-col">商品图片</th>
                <th class="name-col">商品信息</th>
                <th class="price-col">价格</th>
                <th class="stock-col">库存</th>
                <th class="status-col">状态</th>
                <th class="action-col">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="product in products" :key="product.id">
                <td class="checkbox-col">
                  <input type="checkbox" v-model="selectedIds" :value="product.id" />
                </td>
                <td class="image-col">
                  <img
                    :src="product.images || '/images/default-product.jpg'"
                    :alt="product.name"
                    class="product-image"
                  />
                </td>
                <td class="name-col">
                  <div class="product-info">
                    <div class="product-name">{{ product.name }}</div>
                    <div class="product-brand">{{ product.brand }}</div>
                  </div>
                </td>
                <td class="price-col">
                  <div class="product-price">¥{{ formatPrice(product.price) }}</div>
                </td>
                <td class="stock-col">
                  <span :class="['stock-badge', getStockClass(product.stock)]">
                    {{ product.stock }}件
                  </span>
                </td>
                <td class="status-col">
                  <div class="status-switch" @click="toggleStatus(product, product.status === 1 ? 0 : 1)">
                    <div class="switch-slider" :class="{ active: product.status === 1 }">
                      <i v-if="product.status === 1" class="fas fa-eye"></i>
                      <i v-else class="fas fa-eye-slash"></i>
                    </div>
                    <span class="status-text" :class="{ active: product.status === 1 }">
                      {{ product.status === 1 ? '上架' : '下架' }}
                    </span>
                  </div>
                </td>
                <td class="action-col">
                  <div class="action-buttons">
                    <button class="action-btn edit-btn" @click="goToEdit(product.id)" title="编辑">
                      <i class="fas fa-edit"></i>
                    </button>
                    <button class="action-btn delete-btn" @click="deleteProduct(product)" title="删除">
                      <i class="fas fa-trash-alt"></i>
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="pagination" v-if="total > 0">
          <button class="page-btn" :disabled="currentPage === 1" @click="changePage(currentPage - 1)">
            <i class="fas fa-chevron-left"></i>
          </button>
          <span class="page-info">第 {{ currentPage }} / {{ totalPages }} 页</span>
          <button class="page-btn" :disabled="currentPage === totalPages" @click="changePage(currentPage + 1)">
            <i class="fas fa-chevron-right"></i>
          </button>
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
  categoryId: number
}

const isLoading = ref(false)
const products = ref<Product[]>([])
const selectedIds = ref<number[]>([])
const selectAll = ref(false)
const searchKeyword = ref('')
const filterStatus = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const totalPages = computed(() => Math.ceil(total.value / pageSize.value))

const loadProducts = async () => {
  isLoading.value = true

  try {
    const params = {
      page: currentPage.value,
      pageSize: pageSize.value,
      keyword: searchKeyword.value,
      status: filterStatus.value
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

const toggleSelectAll = () => {
  if (selectAll.value) {
    selectedIds.value = products.value.map(p => p.id)
  } else {
    selectedIds.value = []
  }
}

const handleBatchDelete = async () => {
  if (selectedIds.value.length === 0) return

  try {
    await Message.confirm(`确定要删除选中的 ${selectedIds.value.length} 个商品吗？删除后不可恢复。`, '批量删除确认')

    const response = await authAPI.batchDeleteProducts(selectedIds.value)

    if (response.success) {
      Message.success(`成功删除 ${selectedIds.value.length} 个商品`)
      selectedIds.value = []
      selectAll.value = false
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
  const action = status === 1 ? '上架' : '下架'

  try {
    await Message.confirm(`确定要${action}「${product.name}」吗？`, `${action}确认`)

    const response = await authAPI.updateProductStatus(product.id, status)

    if (response.success) {
      Message.success(`${action}成功`)
      loadProducts()
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      Message.error(error.message || `${action}失败`)
    }
  }
}

const deleteProduct = async (product: Product) => {
  try {
    await Message.confirm(`确定要删除商品「${product.name}」吗？删除后不可恢复。`, '删除确认')

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

const formatPrice = (price: number | null): string => {
  if (price == null || isNaN(price)) return '0.00'
  return price.toFixed(2)
}

const getStockClass = (stock: number | null): string => {
  if (stock == null || stock <= 0) return 'stock-out'
  if (stock < 10) return 'stock-low'
  return 'stock-normal'
}

onMounted(() => {
  if (!authStore.validateSellerPermission()) return

  loadProducts()
})
</script>

<style scoped>
@import url('@/static/css/seller/商品管理页.css');
</style>
