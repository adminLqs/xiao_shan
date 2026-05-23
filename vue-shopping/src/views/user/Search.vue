<template>
  <div class="search-page">
    <div class="search-bar">
      <i class="fas fa-arrow-left" @click="$router.back()"></i>
      <input v-model="keyword" placeholder="搜索商品" @keyup.enter="doSearch" ref="searchInput" />
      <span @click="doSearch">搜索</span>
    </div>

    <div class="filter-bar">
      <div class="sort-tags">
        <span
          v-for="item in sortOptions"
          :key="item.value"
          :class="{ active: currentSort === item.value }"
          @click="changeSort(item.value)"
        >{{ item.label }}</span>
      </div>
      <button class="filter-btn" @click="showFilter = true">
        <i class="fas fa-filter"></i> 筛选
      </button>
    </div>

    <div class="search-history" v-if="history.length > 0 && !keyword">
      <div class="history-header">
        <span>搜索历史</span>
        <span @click="clearHistory">清除</span>
      </div>
      <span v-for="item in history" :key="item.keyword" class="history-tag" @click="doSearchWithTag(item.keyword)">{{ item.keyword }}</span>
    </div>

    <div class="products-section" v-if="keyword">
      <div v-if="loading" class="loading-container">
        <i class="fas fa-spinner fa-spin"></i>
        <span>加载中...</span>
      </div>
      <div v-else-if="products.length === 0" class="empty-result">
        <i class="fas fa-search"></i>
        <p>未找到相关商品</p>
      </div>
      <div v-else class="products-grid">
        <div v-for="product in products" :key="product.id" class="product-card" @click="viewProduct(product.id)">
          <img :src="product.images" :alt="product.name" />
          <div class="product-info">
            <div class="product-name">{{ product.name }}</div>
            <div class="product-price">¥{{ formatPrice(product.price) }}</div>
            <div class="product-sales">已售 {{ product.salesCount || 0 }}</div>
          </div>
        </div>
      </div>
      <div v-if="loadingMore" class="loading-more">
        <i class="fas fa-spinner fa-spin"></i> 加载中...
      </div>
      <div v-else-if="!hasMore && products.length > 0" class="no-more">没有更多了</div>
    </div>

    <div v-if="!keyword && history.length === 0" class="hot-search-section">
      <h4>热门搜索</h4>
      <span v-for="tag in hotTags" :key="tag" class="hot-tag" @click="doSearchWithTag(tag)">{{ tag }}</span>
    </div>

    <div v-if="showFilter" class="filter-overlay" @click="showFilter = false"></div>
    <div class="filter-panel" :class="{ active: showFilter }">
      <div class="filter-header">
        <span>筛选</span>
        <span @click="resetFilter">重置</span>
      </div>
      <div class="filter-content">
        <div class="filter-section">
          <h5>分类</h5>
          <div class="filter-options">
            <span
              :class="{ active: !selectedCategory }"
              @click="selectedCategory = null"
            >全部</span>
            <span
              v-for="cat in level1Categories"
              :key="cat.id"
              :class="{ active: selectedCategory === cat.id }"
              @click="selectedCategory = cat.id"
            >{{ cat.name }}</span>
          </div>
        </div>
        <div class="filter-section">
          <h5>价格区间</h5>
          <div class="filter-options">
            <span
              :class="{ active: !priceRange[0] && !priceRange[1] }"
              @click="priceRange = [null, null]"
            >不限</span>
            <span
              v-for="range in priceRanges"
              :key="range.label"
              :class="{ active: priceRange[0] === range.min && priceRange[1] === range.max }"
              @click="priceRange = [range.min, range.max]"
            >{{ range.label }}</span>
          </div>
        </div>
      </div>
      <div class="filter-footer">
        <button @click="showFilter = false">取消</button>
        <button class="confirm" @click="confirmFilter">确定</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { authAPI } from '@/api/authAPI'

const router = useRouter()
const route = useRoute()

interface Product {
  id: number
  name: string
  price: number
  images?: string
  salesCount?: number
}

interface Category {
  id: number
  name: string
  parentId: number | null
  icon?: string
  isActive: boolean
}

interface SearchHistory {
  keyword: string
  timestamp: number
}

const keyword = ref('')
const searchInput = ref<HTMLInputElement | null>(null)
const showFilter = ref(false)
const loading = ref(false)
const loadingMore = ref(false)
const hasMore = ref(true)

const products = ref<Product[]>([])
const page = ref(1)
const pageSize = ref(20)

const currentSort = ref('')
const selectedCategory = ref<number | null>(null)
const priceRange = ref<[number | null, number | null]>([null, null])

const level1Categories = ref<Category[]>([])

const sortOptions = [
  { label: '综合', value: '' },
  { label: '价格↑', value: 'price_asc' },
  { label: '价格↓', value: 'price_desc' },
  { label: '销量', value: 'salesCount_desc' },
  { label: '新品', value: 'createdAt_desc' },
]

const priceRanges = [
  { label: '0-50', min: 0, max: 50 },
  { label: '50-100', min: 50, max: 100 },
  { label: '100-200', min: 100, max: 200 },
  { label: '200-500', min: 200, max: 500 },
  { label: '500+', min: 500, max: null },
]

const hotTags = ['春季促销', '数码', '新品', '水果', '服装', '手机']

const history = ref<SearchHistory[]>([])
const STORAGE_KEY = 'searchHistory'
const MAX_HISTORY = 10

const loadHistory = () => {
  try {
    const data = localStorage.getItem(STORAGE_KEY)
    if (data) {
      history.value = JSON.parse(data)
    }
  } catch {}
}

const saveHistory = (kw: string) => {
  const newItem: SearchHistory = { keyword: kw, timestamp: Date.now() }
  history.value = history.value.filter(item => item.keyword !== kw)
  history.value.unshift(newItem)
  if (history.value.length > MAX_HISTORY) {
    history.value = history.value.slice(0, MAX_HISTORY)
  }
  localStorage.setItem(STORAGE_KEY, JSON.stringify(history.value))
}

const clearHistory = () => {
  history.value = []
  localStorage.removeItem(STORAGE_KEY)
}

const loadCategories = async () => {
  try {
    const response = await authAPI.getAllCategories()
    if (response.success && response.data?.categories) {
      level1Categories.value = response.data.categories.filter(
        (cat: Category) => cat.parentId === null && cat.isActive
      )
    }
  } catch {}
}

const loadProducts = async (reset = false) => {
  if (reset) {
    page.value = 1
    products.value = []
    hasMore.value = true
  }

  if (!hasMore.value && !reset) return
  if (loading.value) return

  loading.value = true
  try {
    const params: any = {
      page: page.value,
      pageSize: pageSize.value
    }
    if (keyword.value) params.keyword = keyword.value
    if (selectedCategory.value) params.level1CategoryId = selectedCategory.value
    if (priceRange.value[0] !== null) params.minPrice = priceRange.value[0]
    if (priceRange.value[1] !== null) params.maxPrice = priceRange.value[1]
    if (currentSort.value) params.sort = currentSort.value

    const response = await authAPI.getProducts(params)
    if (response.success && response.data?.records) {
      const records = response.data.records
      if (reset) {
        products.value = records
      } else {
        products.value.push(...records)
      }
      hasMore.value = records.length >= pageSize.value
      page.value++
    }
  } catch {} finally {
    loading.value = false
  }
}

const doSearch = () => {
  if (!keyword.value.trim()) return
  saveHistory(keyword.value)
  loadProducts(true)
}

const doSearchWithTag = (tag: string) => {
  keyword.value = tag
  saveHistory(tag)
  loadProducts(true)
}

const changeSort = (sort: string) => {
  currentSort.value = sort
  if (keyword.value) {
    loadProducts(true)
  }
}

const resetFilter = () => {
  selectedCategory.value = null
  priceRange.value = [null, null]
}

const confirmFilter = () => {
  showFilter.value = false
  if (keyword.value) {
    loadProducts(true)
  }
}

const viewProduct = (productId: number) => {
  router.push({ name: 'ProductDetail', params: { productId } })
}

const formatPrice = (price: number | null | undefined): string => {
  if (price == null || isNaN(price)) return '0.00'
  return price.toFixed(2)
}

const handleScroll = () => {
  if (loadingMore.value || !hasMore.value || loading.value || !keyword.value) return
  const { scrollTop, scrollHeight, clientHeight } = document.documentElement
  if (scrollTop + clientHeight >= scrollHeight - 100) {
    loadingMore.value = true
    loadProducts().then(() => {
      loadingMore.value = false
    })
  }
}

onMounted(() => {
  const queryKeyword = route.query.keyword as string
  if (queryKeyword) {
    keyword.value = queryKeyword
  }

  loadCategories()
  loadHistory()
  searchInput.value?.focus()

  if (keyword.value) {
    loadProducts()
  }

  window.addEventListener('scroll', handleScroll)
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})
</script>

<style scoped>
  @import url('@/static/css/user/搜索页.css');
</style>
