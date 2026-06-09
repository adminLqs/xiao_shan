<template>
  <div v-if="isLoading" class="starlight-loader">
    <div class="loader-ring">
      <i class="fas fa-sparkles brand-icon"></i>
    </div>
    <p class="loader-text">加载中...</p>
  </div>

  <div v-else class="products-page">
    <div class="search-bar">
      <div class="search-box">
        <i class="fas fa-search"></i>
        <input type="text" v-model="searchKeyword" placeholder="搜索商品名称、品牌..." @keyup.enter="handleSearch" />
        <button v-if="searchKeyword" class="clear-search" @click="clearSearch">
          <i class="fas fa-times"></i>
        </button>
      </div>
      <div class="filter-group">
        <select v-model="filterStatus" class="filter-select" @change="handleSearch">
          <option value="">全部状态</option>
          <option value="1">上架</option>
          <option value="0">下架</option>
        </select>
        <button class="btn-search" @click="handleSearch">
          <i class="fas fa-search"></i> 搜索
        </button>
      </div>
    </div>

    <div class="product-table">
      <table v-if="products.length > 0" class="data-table">
        <thead>
          <tr>
            <th width="60">ID</th>
            <th width="100">图片</th>
            <th>名称</th>
            <th width="100">品牌</th>
            <th width="100">价格</th>
            <th width="80">库存</th>
            <th width="80">销量</th>
            <th width="80">状态</th>
            <th width="120">商家</th>
            <th width="150">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="product in products" :key="product.id">
            <td>{{ product.id }}</td>
            <td>
              <img 
                :src="product.productImages?.[0]?.image || product.images || '/placeholder.png'" 
                :alt="product.name" 
                class="product-thumb"
              />
            </td>
            <td class="product-name">{{ product.name }}</td>
            <td>{{ product.brand || '-' }}</td>
            <td class="price">¥{{ formatPrice(product.price) }}</td>
            <td>
              <span :class="['stock-badge', getStockClass(product.stock)]">
                {{ product.stock || 0 }}
              </span>
            </td>
            <td>{{ product.sales || 0 }}</td>
            <td>
              <span class="status-tag" :class="product.status === 1 ? 'active' : 'inactive'">
                {{ product.status === 1 ? '上架' : '下架' }}
              </span>
            </td>
            <td>{{ product.sellerName || '-' }}</td>
            <td>
              <button 
                class="action-btn" 
                :class="product.status === 1 ? 'btn-off' : 'btn-on'"
                @click="handleToggleStatus(product)"
                :disabled="submitting"
              >
                <i class="fas" :class="product.status === 1 ? 'fa-eye-slash' : 'fa-eye'"></i>
                {{ product.status === 1 ? '下架' : '上架' }}
              </button>
            </td>
          </tr>
        </tbody>
      </table>

      <div v-else class="empty-state">
        <i class="fas fa-box-open"></i>
        <p>暂无商品数据</p>
      </div>
    </div>

    <div class="pagination" v-if="total > 0">
      <div class="pagination-left">
        <button class="page-btn" :disabled="currentPage === 1" @click="changePage(currentPage - 1)">
          <i class="fas fa-chevron-left"></i>
        </button>
        <div class="page-numbers">
          <button v-if="currentPage > 3" class="page-number-btn" @click="changePage(1)">1</button>
          <span v-if="currentPage > 4" class="page-ellipsis">...</span>
          <button 
            v-for="page in visiblePages" 
            :key="page" 
            class="page-number-btn" 
            :class="{ active: page === currentPage }"
            @click="changePage(page)"
          >{{ page }}</button>
          <span v-if="currentPage < totalPages - 3" class="page-ellipsis">...</span>
          <button v-if="currentPage < totalPages - 2" class="page-number-btn" @click="changePage(totalPages)">{{ totalPages }}</button>
        </div>
        <button class="page-btn" :disabled="currentPage === totalPages" @click="changePage(currentPage + 1)">
          <i class="fas fa-chevron-right"></i>
        </button>
      </div>
      <div class="pagination-right">
        <span class="total-info">共 {{ total }} 条商品</span>
        <select v-model="pageSize" class="page-size-select" @change="handleSearch">
          <option :value="10">10条/页</option>
          <option :value="20">20条/页</option>
          <option :value="50">50条/页</option>
        </select>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import Message from '@/utils/message'
import { adminAPI } from '@/api/adminAPI'

interface Product {
  id: number
  name: string
  brand: string
  price: number | null
  stock: number | null
  sales: number | null
  status: number
  images?: string
  productImages?: Array<{ image: string }>
  sellerName?: string
}

const isLoading = ref(false)
const submitting = ref(false)
const products = ref<Product[]>([])
const searchKeyword = ref('')
const filterStatus = ref('')
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))

const visiblePages = computed(() => {
  const pages: number[] = []
  const start = Math.max(1, currentPage.value - 2)
  const end = Math.min(totalPages.value, currentPage.value + 2)
  for (let i = start; i <= end; i++) {
    if (!pages.includes(i)) pages.push(i)
  }
  return pages
})

const loadProducts = async () => {
  isLoading.value = true
  try {
    const params: {
      page: number
      size: number
      keyword?: string
      status?: number
    } = {
      page: currentPage.value,
      size: pageSize.value
    }
    if (searchKeyword.value.trim()) {
      params.keyword = searchKeyword.value.trim()
    }
    if (filterStatus.value !== '') {
      params.status = Number(filterStatus.value)
    }

    const response = await adminAPI.getProducts(params)
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
}

const handleToggleStatus = async (product: Product) => {
  const newStatus = product.status === 1 ? 0 : 1
  const action = newStatus === 1 ? '上架' : '下架'

  try {
    await Message.confirm(`确定要${action}商品「${product.name}」吗？`, `${action}确认`)

    submitting.value = true
    const response = await adminAPI.toggleProductStatus(product.id, newStatus)
    if (response.success) {
      Message.success(`${action}成功`)
      loadProducts()
    } else {
      Message.error(response.message || `${action}失败`)
    }
  } catch {
  } finally {
    submitting.value = false
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
  loadProducts()
})
</script>

<style scoped>
@import '@/static/css/admin/商品管理.css';
@import '@/static/css/common/骨架屏.css';
@import '@/static/css/common/星环加载器.css';
</style>