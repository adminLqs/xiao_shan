<template>
  <div class="review-list-page page-container">
    <!-- 顶部导航栏 -->
    <div class="page-navbar">
      <button class="page-nav-back" @click="router.back()">
        <i class="fas fa-chevron-left"></i>
      </button>
      <div class="page-nav-title">
        <span>我的评价</span>
      </div>
      <div class="page-nav-right"></div>
    </div>

    <!-- Tab切换 -->
    <div class="review-tabs" ref="tabsRef">
      <div
        v-for="tab in tabs"
        :key="tab.value"
        class="tab-item"
        :class="{ active: activeTab === tab.value }"
        :ref="el => { if (el) tabRefs[tab.value] = el as HTMLElement }"
        @click="changeTab(tab.value as 'pending' | 'reviewed')"
      >
        {{ tab.label }}
      </div>
      <div class="tab-underline" :style="underlineStyle"></div>
    </div>

    <!-- 下拉刷新区域 -->
    <div class="refresh-indicator" :style="{ height: pullDistance + 'px' }">
      <div class="starlight-refresh" v-if="isRefreshing">
        <div class="loader-ring-sm">
          <i class="fas fa-sparkles brand-icon-sm"></i>
        </div>
        <span class="loader-text-sm">云杉购·刷新中</span>
      </div>
      <div class="pull-hint" v-else-if="pullDistance > 0">
        <span>{{ pullDistance > 60 ? '✨ 释放刷新' : '下拉刷新' }}</span>
      </div>
    </div>

    <!-- 列表区域 -->
    <div class="review-content tab-content" ref="listRef">
      <!-- 待评价列表 -->
      <div v-if="activeTab === 'pending'">
        <div v-if="loading" class="skeleton-list">
          <div v-for="i in 3" :key="i" class="skeleton-item"></div>
        </div>
        <div v-else-if="pendingItems.length === 0" class="empty-state">
          <i class="fas fa-check-circle"></i>
          <p>暂无待评价商品</p>
          <span>您已完成所有评价</span>
        </div>
        <div
          v-for="item in pendingItems"
          :key="item.orderItemId"
          class="pending-item"
        >
          <img :src="item.productImage" class="item-image" :alt="item.productName" />
          <div class="item-info">
            <div class="item-name">{{ item.productName }}</div>
            <div class="item-spec" v-if="item.skuName">{{ item.skuName }}</div>
          </div>
          <button class="review-btn" @click="goToReview(item.orderItemId)">
            <span>去评价</span>
          </button>
        </div>
      </div>

      <!-- 已评价列表 -->
      <div v-if="activeTab === 'reviewed'">
        <div v-if="loading" class="skeleton-list">
          <div v-for="i in 3" :key="i" class="skeleton-item"></div>
        </div>
        <div v-else-if="reviewedItems.length === 0" class="empty-state">
          <i class="fas fa-comment-dots"></i>
          <p>暂无评价记录</p>
          <span>去购买商品并评价吧</span>
        </div>
        <div
          v-for="item in reviewedItems"
          :key="item.orderItemId"
          class="reviewed-item"
        >
          <img :src="item.productImage" class="item-image" :alt="item.productName" />
          <div class="item-info">
            <div class="name-rating-row">
              <div class="item-name">{{ item.productName }}</div>
              <div class="review-rating">
                <i v-for="i in 5" :key="i" class="fas fa-star" :class="{ active: i <= item.rating }"></i>
              </div>
            </div>
            <div class="item-spec" v-if="item.skuName">{{ item.skuName }}</div>
            <div class="review-content-text">{{ item.comment || '暂无评价内容' }}</div>
            <span class="review-time">{{ formatTime(item.createdAt) }}</span>
          </div>
        </div>
      </div>

      <!-- 加载状态 -->
      <div v-if="loadingMore" class="loading-more">
        <i class="fas fa-spinner fa-spin"></i>
        <span>加载中...</span>
      </div>
      <div v-if="!loading && !hasMore && (activeTab === 'pending' ? pendingItems.length : reviewedItems.length) > 0" class="no-more">
        — 已经到底了 —
      </div>
    </div>

    <!-- 底部留空 -->
    <div class="bottom-space"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { authAPI } from '@/api/authAPI'
import Message from '@/utils/message'

const router = useRouter()

const activeTab = ref('pending')
const loading = ref(true)
const loadingMore = ref(false)
const hasMore = ref(true)
const currentPage = ref(1)
const pendingItems = ref<any[]>([])
const reviewedItems = ref<any[]>([])

// Tab下划线
const tabsRef = ref<HTMLElement>()
const tabRefs = ref<Record<string, HTMLElement>>({})
const underlineStyle = ref({ left: '0px', width: '0px' })

// 下拉刷新
const pullDistance = ref(0)
const isRefreshing = ref(false)
let startY = 0
let isTouching = false

const tabs = [
  { label: '待评价', value: 'pending' },
  { label: '我的评价', value: 'reviewed' },
]

/** 更新下划线位置 */
const updateUnderline = (value: string) => {
  const tab = tabRefs.value[value]
  const container = tabsRef.value
  if (!tab || !container) return

  const tabRect = tab.getBoundingClientRect()
  const containerRect = container.getBoundingClientRect()

  underlineStyle.value = {
    left: (tabRect.left - containerRect.left) + 'px',
    width: tabRect.width + 'px'
  }
}

const changeTab = async (tab: 'pending' | 'reviewed') => {
  activeTab.value = tab
  nextTick(() => updateUnderline(tab))
  currentPage.value = 1
  hasMore.value = true
  if (tab === 'pending') {
    pendingItems.value = []
  } else {
    reviewedItems.value = []
  }
  await loadReviewItems(tab)
}

const loadReviewItems = async (type: string, reset = false) => {
  if (loadingMore.value) return
  if (reset) {
    currentPage.value = 1
    hasMore.value = true
  }
  if (!hasMore.value) return

  loading.value = true
  loadingMore.value = true

  try {
    const type: 'pending' | 'reviewed' = activeTab.value === 'reviewed' ? 'reviewed' : 'pending'
    const response = await authAPI.getReviewItems({ type, page: currentPage.value, pageSize: 10 })
    if (response.success && response.data) {
      const items = response.data.records || []
      if (type === 'pending') {
        if (reset) {
          pendingItems.value = items
        } else {
          pendingItems.value.push(...items)
        }
      } else {
        if (reset) {
          reviewedItems.value = items
        } else {
          reviewedItems.value.push(...items)
        }
      }
      hasMore.value = response.data.hasMore || false
      currentPage.value++
    } else {
      hasMore.value = false
    }
  } catch (error: any) {
    Message.error(error.message || '加载失败')
    hasMore.value = false
  } finally {
    loading.value = false
    loadingMore.value = false
    isRefreshing.value = false
    pullDistance.value = 0
  }
}

const goToReview = (orderItemId: number) => {
  router.push({ name: 'Review', params: { orderItemId } })
}

const formatTime = (dateStr: string) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

// 下拉刷新
const onTouchStart = (e: TouchEvent) => {
  const scrollTop = document.documentElement.scrollTop
  if (scrollTop === 0 && e.touches?.[0]) {
    startY = e.touches[0].clientY
    isTouching = true
  }
}

const onTouchMove = (e: TouchEvent) => {
  if (!isTouching) return
  const currentY = e.touches?.[0]?.clientY
  if (currentY === undefined) return
  const deltaY = currentY - startY
  if (deltaY > 0) {
    pullDistance.value = Math.min(deltaY * 0.5, 80)
  }
}

const onTouchEnd = () => {
  if (!isTouching) return
  isTouching = false
  if (pullDistance.value >= 60) {
    isRefreshing.value = true
    loadReviewItems(activeTab.value, true)
  } else {
    pullDistance.value = 0
  }
}

onMounted(async () => {
  await loadReviewItems('pending')
  window.addEventListener('touchstart', onTouchStart)
  window.addEventListener('touchmove', onTouchMove)
  window.addEventListener('touchend', onTouchEnd)
  nextTick(() => updateUnderline(activeTab.value))
})

onUnmounted(() => {
  window.removeEventListener('touchstart', onTouchStart)
  window.removeEventListener('touchmove', onTouchMove)
  window.removeEventListener('touchend', onTouchEnd)
})
</script>

<style scoped>
@import url('@/static/css/user/评价列表页.css');
</style>
