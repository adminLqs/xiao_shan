<template>
  <div class="search-result">
    <div class="filter-bar">
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

    <div v-else-if="!loading" class="empty-state">
      <i class="fas fa-search"></i>
      <p>未找到"{{ keyword }}"相关商品</p>
    </div>

    <div v-if="products.length > 0" class="load-more-wrapper">
      <div v-if="loadingMore" class="loading-more-bar">
        <i class="fas fa-spinner fa-spin"></i>
        <span>加载中...</span>
      </div>
      <div v-else-if="!hasMore" class="no-more-bar">
        — 已经到底了 —
      </div>
    </div>

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
import { ref, computed, reactive, onMounted, onUnmounted, watch } from 'vue'
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
const loadingMore = ref(false)
const page = ref(1)
const pageSize = ref(20)
const hasMore = ref(true)
const debounceTimer: ReturnType<typeof setTimeout> | null = null

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
  page.value = 1
  hasMore.value = true
  products.value = []
  doSearch()
}

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
  page.value = 1
  hasMore.value = true
  products.value = []
  doSearch()
}

const doSearch = async () => {
  if (!keyword.value.trim()) return
  loading.value = true
  try {
    const params: any = {
      keyword: keyword.value.trim(),
      page: page.value,
      pageSize: pageSize.value
    }

    if (filter.sort && filter.sort !== 'default') {
      params.sort = filter.sort
    }

    if (filter.minPrice !== null && filter.minPrice !== undefined && filter.minPrice !== 0) {
      params.minPrice = Number(filter.minPrice)
    }
    if (filter.maxPrice !== null && filter.maxPrice !== undefined && filter.maxPrice !== 0) {
      params.maxPrice = Number(filter.maxPrice)
    }

    if (filter.categoryId) {
      params.categoryId = Number(filter.categoryId)
    }

    const res = await authAPI.getProducts(params)

    if (res.success) {
      const records = res.data?.records || []
      if (page.value === 1) {
        products.value = records
      } else {
        products.value.push(...records)
      }
      hasMore.value = records.length >= pageSize.value
      saveHistory(keyword.value.trim())
    } else {
      Message.error(res.message || '搜索失败')
    }
  } catch (e: any) {
    Message.error(e.message || '搜索失败')
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

const handleScroll = () => {
  if (loadingMore.value || !hasMore.value || loading.value) return

  const { scrollTop, scrollHeight, clientHeight } = document.documentElement
  if (scrollTop + clientHeight >= scrollHeight - 100) {
    loadMore()
  }
}

const loadMore = async () => {
  if (loadingMore.value || !hasMore.value) return
  loadingMore.value = true
  page.value++
  await doSearch()
}

const saveHistory = (word: string) => {
  const history = JSON.parse(localStorage.getItem('searchHistory') || '[]').filter((h: string) => h !== word)
  history.unshift(word)
  const newHistory = history.slice(0, 5)
  localStorage.setItem('searchHistory', JSON.stringify(newHistory))
}

const goToProduct = (id: number) => {
  router.push({ name: 'ProductDetail', params: { productId: id } })
}

const formatPrice = (price: number | null | undefined): string => {
  if (!price && price !== 0) return '0.00'
  return Number(price).toFixed(2)
}

watch(() => route.query.keyword, (newKeyword) => {
  if (newKeyword) {
    keyword.value = newKeyword as string
    resetFilter()
    doSearch()
  }
})

watch(showFilter, (val) => {
  document.body.style.overflow = val ? 'hidden' : ''
})

onMounted(() => {
  loadCategories()
  const queryKeyword = route.query.keyword as string
  if (queryKeyword) {
    keyword.value = queryKeyword
    doSearch()
  }
  window.addEventListener('scroll', handleScroll)
})

onUnmounted(() => {
  if (debounceTimer) clearTimeout(debounceTimer)
  document.body.style.overflow = ''
  window.removeEventListener('scroll', handleScroll)
})
</script>

<style scoped>
@import url('@/static/css/user/搜索页.css');

.search-result {
  padding-bottom: calc(40px + env(safe-area-inset-bottom));
}
</style>
