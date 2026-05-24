<template>
  <div class="categories-page page-container">
    <div class="page-navbar">
      <div class="page-nav-right"></div>
      <div class="page-nav-title">
        <i class="fas fa-th-large"></i>
        <span>分类浏览</span>
      </div>
      <div class="page-nav-right"></div>
    </div>

    <!-- 一级分类横向滚动 -->
    <div class="category-tabs-scroll" ref="level1TabsRef">
      <div
        class="category-tab"
        :class="{ active: activeLevel1Id === null }"
        :ref="el => { if (el) level1TabRefs['all'] = el as HTMLElement }"
        @click="selectLevel1(null)"
      >
        <span>全部</span>
      </div>
      <div
        v-for="cat in level1Categories"
        :key="cat.id"
        class="category-tab"
        :class="{ active: activeLevel1Id === cat.id }"
        :ref="el => { if (el) level1TabRefs[cat.id] = el as HTMLElement }"
        @click="selectLevel1(cat.id)"
      >
        <span>{{ cat.name }}</span>
      </div>
      <div class="tab-underline" :style="level1UnderlineStyle"></div>
    </div>

    <!-- 二级分类横向滚动 -->
    <div class="category-tabs-scroll sub-tabs" ref="level2TabsRef" v-if="activeLevel1Id && level2Categories.length > 0">
      <div
        class="category-tab"
        :class="{ active: activeLevel2Id === null }"
        :ref="el => { if (el) level2TabRefs['all'] = el as HTMLElement }"
        @click="selectLevel2(null)"
      >
        <span>全部</span>
      </div>
      <div
        v-for="cat in level2Categories"
        :key="cat.id"
        class="category-tab"
        :class="{ active: activeLevel2Id === cat.id }"
        :ref="el => { if (el) level2TabRefs[cat.id] = el as HTMLElement }"
        @click="selectLevel2(cat.id)"
      >
        <span>{{ cat.name }}</span>
      </div>
      <div class="tab-underline" :style="level2UnderlineStyle"></div>
    </div>

    <!-- 商品列表 -->
    <div class="products-grid" v-if="products.length > 0 || loading">
      <template v-if="loading && products.length === 0">
        <div v-for="i in 6" :key="'skeleton-' + i" class="product-card skeleton-card">
          <div class="product-image">
            <div class="image-skeleton"></div>
          </div>
          <div class="product-info">
            <div class="skeleton-line skeleton-title"></div>
            <div class="skeleton-line skeleton-price"></div>
          </div>
        </div>
      </template>

      <div v-for="product in products" :key="product.id" class="product-card" @click="viewProduct(product.id)">
        <div class="product-image">
          <span v-if="product.badge" class="product-badge" :style="{ backgroundColor: product.badgeColor }">{{ product.badge }}</span>
          <div class="image-skeleton" v-show="!product.imageLoaded"></div>
          <img
            :src="product.productImages?.[0]?.image || product.images"
            :alt="product.name"
            :class="{ 'img-loaded': product.imageLoaded }"
            @load="product.imageLoaded = true"
            @error="product.imageLoaded = true"
            v-show="product.imageLoaded"
          >
        </div>
        <div class="product-info">
          <div class="product-title">{{ product.name }}</div>
          <div class="price-row">
            <span class="current-price">¥{{ formatPrice(product.price) }}</span>
            <span v-if="((product.originalPrice ?? product.original_price) !== undefined) && ((product.originalPrice ?? product.original_price) as number) > product.price"
              class="original-price">¥{{ formatPrice((product.originalPrice ?? product.original_price) as number) }}</span>
          </div>
          <div class="product-sales">已售 {{ product.salesCount || 0 }} 件</div>
        </div>
      </div>
    </div>

    <div v-else-if="!loading" class="empty-state">
      <i class="fas fa-box-open"></i>
      <p>该分类暂无商品</p>
    </div>

    <div class="back-to-top" :class="{ show: showBackTop }" @click="scrollToTop">
      <i class="fas fa-arrow-up"></i>
    </div>

    <div class="load-more-wrapper">
      <div v-if="loadingMore" class="loading-more-bar">
        <i class="fas fa-spinner fa-spin"></i>
        <span>加载中...</span>
      </div>
      <div v-else-if="!hasMore && products.length > 0" class="no-more-bar">
        — 已经到底了 —
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { authAPI } from '@/api/authAPI'
import Message from '@/utils/message'
import { useHomeStore } from '@/stores/home'

const router = useRouter()
const route = useRoute()
const homeStore = useHomeStore()

interface Category {
  id: number
  name: string
  parentId: number | null
  icon?: string
  isActive: boolean
}

interface Product {
  id: number
  name: string
  price: number
  originalPrice?: number
  original_price?: number
  images?: string
  productImages?: Array<{ image: string }>
  badge?: string
  badgeColor?: string
  imageLoaded?: boolean
  salesCount?: number
}

const level1Categories = ref<Category[]>([])
const level2Categories = ref<Category[]>([])
const activeLevel1Id = ref<number | null>(null)
const activeLevel2Id = ref<number | null>(null)

// Tab下划线
const level1TabsRef = ref<HTMLElement>()
const level2TabsRef = ref<HTMLElement>()
const level1TabRefs = ref<Record<string | number, HTMLElement>>({})
const level2TabRefs = ref<Record<string | number, HTMLElement>>({})
const level1UnderlineStyle = ref({ left: '0px', width: '0px' })
const level2UnderlineStyle = ref({ left: '0px', width: '0px' })

// 防抖定时器
let loadTimer: ReturnType<typeof setTimeout> | null = null

const products = ref<Product[]>([])
const loading = ref(false)
const loadingMore = ref(false)
const hasMore = ref(true)
const currentPage = ref(1)
const pageSize = ref(20)

const allCategories = ref<Category[]>([])
const showBackTop = ref(false)

const selectLevel1 = (id: number | null) => {
  if (activeLevel1Id.value === id && activeLevel2Id.value === null) {
    return
  }
  activeLevel1Id.value = id
  activeLevel2Id.value = null
  level2Categories.value = []
  level2UnderlineStyle.value = { left: '0px', width: '0px' }
  
  nextTick(() => updateLevel1Underline(id === null ? 'all' : id))
  
  if (id) {
    level2Categories.value = allCategories.value.filter(
      cat => cat.parentId === id && cat.isActive
    )
    nextTick(() => updateLevel2Underline('all'))
  }
  
  currentPage.value = 1
  hasMore.value = true
  products.value = []
  loading.value = true
  
  if (loadTimer) clearTimeout(loadTimer)
  loadTimer = setTimeout(() => {
    loadProducts()
  }, 150)
}

const selectLevel2 = (id: number | null) => {
  if (activeLevel2Id.value === id) {
    return
  }
  activeLevel2Id.value = id
  nextTick(() => updateLevel2Underline(id === null ? 'all' : id))
  currentPage.value = 1
  hasMore.value = true
  
  // 防抖：延迟加载，避免快速切换闪烁
  if (loadTimer) clearTimeout(loadTimer)
  loadTimer = setTimeout(() => {
    loadProducts()
  }, 150)
}

const updateLevel1Underline = (key: string | number) => {
  const tab = level1TabRefs.value[key]
  const container = level1TabsRef.value
  if (!tab || !container) return
  
  const tabRect = tab.getBoundingClientRect()
  const containerRect = container.getBoundingClientRect()
  
  level1UnderlineStyle.value = {
    left: (tabRect.left - containerRect.left) + 'px',
    width: tabRect.width + 'px'
  }
}

const updateLevel2Underline = (key: string | number) => {
  const tab = level2TabRefs.value[key]
  const container = level2TabsRef.value
  if (!tab || !container) return
  
  const tabRect = tab.getBoundingClientRect()
  const containerRect = container.getBoundingClientRect()
  
  level2UnderlineStyle.value = {
    left: (tabRect.left - containerRect.left) + 'px',
    width: tabRect.width + 'px'
  }
}

const loadCategories = async () => {
  try {
    const response = await authAPI.getAllCategories()
    if (response.success && response.data?.categories) {
      allCategories.value = response.data.categories
      level1Categories.value = allCategories.value.filter(c => c.parentId === null && c.isActive)
    }
  } catch (error) {
    Message.error('加载分类失败')
  }
}

const loadProducts = async () => {
  loading.value = true
  try {
    const params: any = { page: currentPage.value, pageSize: pageSize.value }
    if (activeLevel2Id.value) {
      params.level2CategoryId = activeLevel2Id.value
    } else if (activeLevel1Id.value) {
      params.level1CategoryId = activeLevel1Id.value
    }

    const response = await authAPI.getProducts(params)
    if (response.success) {
      const newProducts = response.data?.records || []
      const productsWithState = newProducts.map((p: Product) => ({ ...p, imageLoaded: false }))
      if (currentPage.value === 1) {
        products.value = productsWithState
      } else {
        products.value.push(...productsWithState)
      }
      hasMore.value = newProducts.length >= pageSize.value
    }
  } catch {
    if (currentPage.value === 1) products.value = []
  } finally {
    loading.value = false
  }
}

const viewProduct = (productId: number) => {
  router.push({ name: 'ProductDetail', params: { productId } })
}

const formatPrice = (price: number | null | undefined): string => {
  if (!price && price !== 0) return '0.00'
  return Number(price).toFixed(2)
}

const handleScroll = () => {
  if (loadingMore.value || !hasMore.value || loading.value) return
  const { scrollTop, scrollHeight, clientHeight } = document.documentElement
  if (scrollTop + clientHeight >= scrollHeight - 200) {
    loadingMore.value = true
    currentPage.value++
    loadProducts().then(() => { loadingMore.value = false })
  }
}

const handleBackTopScroll = () => {
  showBackTop.value = window.scrollY > 300
}

const scrollToTop = () => {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

onMounted(async () => {
  homeStore.resetHomeData()

  await loadCategories()

  if (route.query.categoryId) {
    const catId = Number(route.query.categoryId)
    selectLevel1(catId)
  } else {
    await loadProducts()
    nextTick(() => updateLevel1Underline('all'))
  }

  window.addEventListener('scroll', handleScroll)
  window.addEventListener('scroll', handleBackTopScroll)
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
  window.removeEventListener('scroll', handleBackTopScroll)
})
</script>

<style scoped>
@import url('@/static/css/user/分类浏览.css');
</style>
