<template>
  <div class="search-page page-container">
    <!-- 搜索栏 -->
    <div class="search-navbar">
      <button class="search-back" @click="router.back()">
        <i class="fas fa-chevron-left"></i>
      </button>
      <div class="search-input-wrap">
        <i class="fas fa-search search-input-icon"></i>
        <input v-model="keyword" @input="onKeywordChange" placeholder="搜索商品" ref="searchInput" />
        <button v-if="keyword" class="search-clear" @click="clearSearch">
          <i class="fas fa-times-circle"></i>
        </button>
      </div>
    </div>

    <!-- 筛选条 -->
    <div v-if="keyword" class="filter-bar">
      <div class="filter-item" :class="{ active: filter.sort === 'default' }" @click="setSort('default')">综合</div>
      <div class="filter-item" :class="{ active: filter.sort === 'sales_desc' }" @click="setSort('sales_desc')">销量</div>
      <div class="filter-item" :class="{ active: filter.sort === 'price_asc' || filter.sort === 'price_desc' }" @click="setSort('price_asc')">
        价格 <i class="fas" :class="priceArrow"></i>
      </div>
      <div class="filter-item" :class="{ active: filter.sort === 'newest' }" @click="setSort('newest')">新品</div>
      <div class="filter-btn" @click="showFilter = true">
        <i class="fas fa-sliders-h"></i>
        <span>筛选</span>
      </div>
    </div>

    <!-- 搜索历史 -->
    <div v-if="!keyword && products.length === 0 && searchHistory.length > 0" class="search-history">
      <div class="history-header">
        <span>搜索历史</span>
        <button @click="clearHistory">清空</button>
      </div>
      <div class="history-tags">
        <span v-for="item in searchHistory" :key="item" class="history-tag" @click="keyword = item; doSearch()">{{ item }}</span>
      </div>
    </div>

    <!-- 热门搜索 -->
    <div v-if="!keyword && products.length === 0 && searchHistory.length === 0" class="hot-search-section">
      <h4>热门搜索</h4>
      <span v-for="tag in hotTags" :key="tag" class="hot-tag" @click="keyword = tag; doSearch()">{{ tag }}</span>
    </div>

    <!-- 骨架屏 -->
    <div v-if="loading" class="products-grid">
      <div v-for="i in 4" :key="i" class="product-card">
        <div class="product-image">
          <div class="image-skeleton"></div>
        </div>
        <div class="product-info">
          <div class="skeleton-line"></div>
          <div class="skeleton-line short"></div>
        </div>
      </div>
    </div>

    <!-- 搜索结果 -->
    <div v-else-if="products.length > 0" class="products-grid">
      <div v-for="product in products" :key="product.id" class="product-card" @click="goToProduct(product.id)">
        <div class="product-image">
          <img :src="product.productImages?.[0]?.image || product.images" :alt="product.name" />
        </div>
        <div class="product-info">
          <div class="product-name">{{ product.name }}</div>
          <div class="product-price-container">
            <div class="price-row">
              <span class="current-price">¥{{ formatPrice(product.price) }}</span>
              <span v-if="((product.originalPrice ?? product.original_price) !== undefined) && ((product.originalPrice ?? product.original_price) as number) > product.price"
                class="original-price">¥{{ formatPrice((product.originalPrice ?? product.original_price) as number) }}</span>
            </div>
            <div class="product-sales">已售 {{ product.salesCount || 0 }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else-if="keyword && !loading" class="empty-state">
      <i class="fas fa-search"></i>
      <p>未找到"{{ keyword }}"相关商品</p>
    </div>

    <!-- 筛选面板遮罩 -->
    <div class="filter-overlay" v-if="showFilter" @click="showFilter = false">
      <div class="filter-panel" @click.stop>
        <div class="filter-header">
          <span>筛选</span>
          <button @click="resetFilter">重置</button>
        </div>

        <div class="filter-section">
          <div class="filter-label">价格区间</div>
          <div class="price-range">
            <input v-model.number="filter.minPrice" type="number" placeholder="最低价" />
            <span>至</span>
            <input v-model.number="filter.maxPrice" type="number" placeholder="最高价" />
          </div>
        </div>

        <div class="filter-section">
          <div class="filter-label">排序方式</div>
          <div class="filter-tags">
            <span v-for="item in sortOptions" :key="item.value"
              class="filter-tag" :class="{ active: filter.sort === item.value }"
              @click="filter.sort = item.value">{{ item.label }}</span>
          </div>
        </div>

        <div class="filter-section">
          <div class="filter-label">商品分类</div>
          <div class="filter-tags">
            <span class="filter-tag" :class="{ active: !filter.categoryId }" @click="filter.categoryId = null">全部</span>
            <span v-for="cat in categories" :key="cat.id"
              class="filter-tag" :class="{ active: filter.categoryId === cat.id }"
              @click="filter.categoryId = cat.id">{{ cat.name }}</span>
          </div>
        </div>

        <button class="filter-confirm" @click="applyFilter">确认筛选</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { authAPI } from '@/api/authAPI'
import Message from '@/utils/message'

const router = useRouter()
const route = useRoute()

interface Product {
  id: number
  name: string
  price: number
  originalPrice?: number
  original_price?: number
  images?: string
  productImages?: Array<{ image: string }>
  salesCount?: number
}

const keyword = ref('')
const products = ref<Product[]>([])
const loading = ref(false)
const searchInput = ref<HTMLInputElement | null>(null)
let debounceTimer: ReturnType<typeof setTimeout> | null = null

// 搜索历史（存储字符串数组，最多5条）
const searchHistory = ref<string[]>(JSON.parse(localStorage.getItem('searchHistory') || '[]'))

const hotTags = ['春季促销', '数码', '新品', '水果', '服装', '手机']

// 筛选面板
const showFilter = ref(false)
const categories = ref<any[]>([])

const filter = reactive({
  minPrice: null as number | null,
  maxPrice: null as number | null,
  sort: 'default',
  categoryId: null as number | null
})

const sortOptions = [
  { label: '综合', value: 'default' },
  { label: '销量优先', value: 'sales_desc' },
  { label: '价格↑', value: 'price_asc' },
  { label: '价格↓', value: 'price_desc' },
  { label: '新品', value: 'newest' }
]

const resetFilter = () => {
  filter.minPrice = null
  filter.maxPrice = null
  filter.sort = 'default'
  filter.categoryId = null
}

const applyFilter = () => {
  showFilter.value = false
  if (!keyword.value.trim()) return
  products.value = []
  doSearch()
}

// 加载分类列表
const loadCategories = async () => {
  try {
    const res = await authAPI.getAllCategories()
    if (res.success) {
      categories.value = res.data?.categories?.filter((c: any) => !c.parentId) || []
    }
  } catch (e: any) {
    Message.error('加载分类失败', e)
  }
}

const priceArrow = computed(() => {
  if (filter.sort === 'price_asc') return 'fa-arrow-up'
  if (filter.sort === 'price_desc') return 'fa-arrow-down'
  return 'fa-arrow-up'
})

const setSort = (type: string) => {
  if (type === 'price_asc' && filter.sort === 'price_asc') {
    filter.sort = 'price_desc'
  } else if (type === 'price_asc' && filter.sort === 'price_desc') {
    filter.sort = 'price_asc'
  } else {
    filter.sort = type
  }
  if (!keyword.value.trim()) return
  products.value = []
  doSearch()
}

const onKeywordChange = () => {
  if (debounceTimer) clearTimeout(debounceTimer)
  if (!keyword.value.trim()) {
    products.value = []
    return
  }
  debounceTimer = setTimeout(() => doSearch(), 300)
}

const doSearch = async () => {
  if (!keyword.value.trim()) return
  loading.value = true
  try {
    const params: any = {
      keyword: keyword.value.trim(),
      page: 1,
      pageSize: 20
    }

    // 排序
    if (filter.sort && filter.sort !== 'default') {
      params.sort = filter.sort
    }

    // 价格区间
    if (filter.minPrice !== null && filter.minPrice !== undefined && filter.minPrice !== 0) {
      params.minPrice = Number(filter.minPrice)
    }
    if (filter.maxPrice !== null && filter.maxPrice !== undefined && filter.maxPrice !== 0) {
      params.maxPrice = Number(filter.maxPrice)
    }

    // 分类
    if (filter.categoryId) {
      params.categoryId = Number(filter.categoryId)
    }

    const res = await authAPI.getProducts(params)

    if (res.success) {
      products.value = res.data?.records || []
      saveHistory(keyword.value.trim())
    } else {
      Message.error(res.message || '搜索失败')
    }
  } catch (e: any) {
    Message.error(e.message || '搜索失败')
  } finally {
    loading.value = false
  }
}

const clearSearch = () => {
  keyword.value = ''
  products.value = []
  searchInput.value?.focus()
}

const saveHistory = (word: string) => {
  const history = searchHistory.value.filter(h => h !== word)
  history.unshift(word)
  searchHistory.value = history.slice(0, 5)
  localStorage.setItem('searchHistory', JSON.stringify(searchHistory.value))
}

const clearHistory = () => {
  searchHistory.value = []
  localStorage.removeItem('searchHistory')
}

const goToProduct = (id: number) => {
  router.push({ name: 'ProductDetail', params: { productId: id } })
}

const formatPrice = (price: number | null | undefined): string => {
  if (!price && price !== 0) return '0.00'
  return Number(price).toFixed(2)
}

onMounted(() => {
  loadCategories()
  const queryKeyword = route.query.keyword as string
  if (queryKeyword) {
    keyword.value = queryKeyword
    doSearch()
  }
  searchInput.value?.focus()
})

onUnmounted(() => {
  if (debounceTimer) clearTimeout(debounceTimer)
})
</script>

<style scoped>
@import url('@/static/css/user/搜索页.css');
</style>
