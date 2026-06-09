<template>
  <div class="after-sale-list page-container">
    <div class="page-navbar">
      <button class="page-nav-back" @click="router.back()">
        <i class="fas fa-chevron-left"></i>
      </button>
      <div class="page-nav-title">
        <span>退款/售后</span>
      </div>
      <div class="page-nav-right"></div>
    </div>

    <!-- 筛选条 -->
    <div v-if="!loading" class="order-tabs-scroll" ref="tabsRef">
      <div class="order-tab" :class="{ active: filterType === 'all' }"
        :ref="el => { if (el) tabRefs['all'] = el as HTMLElement }"
        @click="changeTab('all')">全部</div>
      <div class="order-tab" :class="{ active: filterType === 'REFUND' }"
        :ref="el => { if (el) tabRefs['REFUND'] = el as HTMLElement }"
        @click="changeTab('REFUND')">仅退款</div>
      <div class="order-tab" :class="{ active: filterType === 'AFTER_SALE' }"
        :ref="el => { if (el) tabRefs['AFTER_SALE'] = el as HTMLElement }"
        @click="changeTab('AFTER_SALE')">退货退款</div>
      <div class="tab-underline" :style="underlineStyle"></div>
    </div>

    <!-- 骨架屏 -->
    <div v-if="loading" class="skeleton-list">
      <div v-for="i in 3" :key="i" class="skeleton-item">
        <div class="skeleton-image"></div>
        <div class="skeleton-content">
          <div class="skeleton-line medium"></div>
          <div class="skeleton-line short"></div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else-if="filteredList.length === 0" class="empty-state">
      <div class="empty-icon">
        <i class="fas fa-inbox"></i>
      </div>
      <p>暂无退款/售后记录</p>
      <button class="btn-primary" @click="router.back()">去购物</button>
    </div>

    <!-- 退款列表 -->
    <div v-else class="refund-list tab-content">
      <div
        v-for="refund in filteredList"
        :key="refund.id"
        class="refund-card"
        @click="handleRefundClick(refund)"
      >
        <!-- 顶部：时间 + 状态 -->
        <div class="card-header">
          <span class="apply-time">{{ formatDate(refund.applyTime) }}</span>
          <span :class="['tag-refund', getStatusClass(refund.refundStatus, refund.returnStatus)]">
            {{ getStatusText(refund.refundStatus, refund.returnStatus) }}
          </span>
        </div>

        <!-- 中间：商品信息 + 价格 -->
        <div class="card-body">
          <img :src="refund.productImage" class="product-image" :alt="refund.productName" />
          <div class="product-info">
            <div class="product-name">{{ refund.productName }}</div>
            <div class="tags-group">
              <span v-if="refund.skuName" class="tag-spec">{{ refund.skuName }}</span>
              <span class="tag-quantity">x{{ refund.quantity }}</span>
            </div>
          </div>
          <div class="product-price">
            <span class="refund-amount">¥{{ formatPrice(refund.refundAmount) }}</span>
          </div>
        </div>

        <!-- 底部：操作按钮 -->
        <div class="card-footer">
          <button class="action-btn" @click.stop="handleRefundClick(refund)">
            {{ getActionText() }}
          </button>
        </div>
      </div>
    </div>

    <!-- 加载更多提示 -->
    <div v-if="!loading && refundList.length > 0" class="load-more-wrapper">
      <div v-if="loadingMore" class="loading-more-bar">
        <i class="fas fa-spinner fa-spin"></i>
        <span>加载中...</span>
      </div>
      <div v-else-if="!hasMore" class="no-more-bar">
        — 已经到底了 —
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { authAPI } from '@/api/authAPI'
import Message from '@/utils/message'

const router = useRouter()

interface RefundRecord {
  id: number
  orderItemId: number
  orderNumber: string
  productName: string
  productImage: string
  skuName: string
  quantity: number
  refundAmount: number
  refundStatus: string
  refundType: string
  returnStatus: string
  returnTrackingNumber?: string
  returnLogisticsName?: string
  applyTime: string
}

const loading = ref(true)
const loadingMore = ref(false)
const hasMore = ref(true)
const refundList = ref<RefundRecord[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const filterType = ref<string>('all')

// Tab下划线
const tabsRef = ref<HTMLElement>()
const tabRefs = ref<Record<string, HTMLElement>>({})
const underlineStyle = ref({ left: '0px', width: '0px' })

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

const changeTab = (value: string) => {
  filterType.value = value
  nextTick(() => updateUnderline(value))
}

const totalPages = computed(() => Math.ceil(total.value / pageSize.value))

const filteredList = computed(() => {
  if (filterType.value === 'all') {
    return refundList.value
  }
  return refundList.value.filter(item => item.refundType === filterType.value)
})

const formatPrice = (price: number | undefined): string => {
  if (price == null || isNaN(price)) return '0.00'
  return price.toFixed(2)
}

const formatDate = (dateStr: string | undefined): string => {
  if (!dateStr) return '-'
  try {
    const date = new Date(dateStr)
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    const hours = String(date.getHours()).padStart(2, '0')
    const minutes = String(date.getMinutes()).padStart(2, '0')
    return `${month}-${day} ${hours}:${minutes}`
  } catch {
    return '-'
  }
}

const getRefundTypeText = (type: string): string => {
  return type === 'AFTER_SALE' ? '退货退款' : '仅退款'
}

const getStatusText = (refundStatus: string, returnStatus: string): string => {
  if (refundStatus === 'FAILED') return '已拒绝'
  if (refundStatus === 'SUCCESS') return '已退款'

  if (returnStatus === 'RETURNING') return '退货中'
  if (returnStatus === 'RECEIVED') return '已退款'

  const statusMap: Record<string, string> = {
    'PROCESSING': '处理中',
    'WAITING_RETURN': '待退货',
    'RETURNING': '退货中',
    'SUCCESS': '已退款',
    'FAILED': '已拒绝'
  }
  return statusMap[refundStatus] || refundStatus
}

const getStatusClass = (refundStatus: string, returnStatus: string): string => {
  if (refundStatus === 'FAILED') return 'status-failed'
  if (refundStatus === 'SUCCESS') return 'status-success'

  if (returnStatus === 'RETURNING') return 'status-returning'
  if (returnStatus === 'RECEIVED') return 'status-success'

  const classMap: Record<string, string> = {
    'PROCESSING': 'status-processing',
    'WAITING_RETURN': 'status-waiting-return',
    'RETURNING': 'status-returning',
    'SUCCESS': 'status-success',
    'FAILED': 'status-failed'
  }
  return classMap[refundStatus] || 'status-default'
}

const getActionText = (): string => {
  return '查看售后'
}

const handleRefundClick = (refund: RefundRecord) => {
  // 退货退款 + 待退货 → 直接跳转到退货页
  if (refund.refundType === 'AFTER_SALE' && refund.refundStatus === 'WAITING_RETURN' && !refund.returnStatus) {
    router.push({
      name: 'ReturnGoods',
      params: { refundId: String(refund.id), orderItemId: String(refund.orderItemId) }
    })
  } else {
    router.push({
      name: 'RefundChatStep',
      params: { refundId: String(refund.id) }
    })
  }
}

const loadRefundList = async (page: number = 1) => {
  const isFirstLoad = page === 1

  if (!isFirstLoad) {
    loadingMore.value = true
  } else {
    loading.value = true
  }

  try {
    const response = await authAPI.getUserRefunds(page, pageSize.value)
    if (response.success && response.data) {
      const records = response.data.records || []
      total.value = response.data.total || 0
      currentPage.value = page

      if (isFirstLoad) {
        refundList.value = records
      } else {
        refundList.value = [...refundList.value, ...records]
      }

      hasMore.value = records.length >= pageSize.value && currentPage.value < totalPages.value
    } else {
      if (isFirstLoad) {
        refundList.value = []
        total.value = 0
      }
      hasMore.value = false
    }
  } catch (error: any) {
    Message.error(error.message || '加载失败')
    if (!isFirstLoad) {
      currentPage.value--
    }
  } finally {
    loading.value = false
    loadingMore.value = false
    nextTick(() => updateUnderline(filterType.value))
  }
}

const handleScroll = () => {
  if (loadingMore.value || !hasMore.value || loading.value) return
  const { scrollTop, scrollHeight, clientHeight } = document.documentElement
  if (scrollTop + clientHeight >= scrollHeight - 150) {
    loadMore()
  }
}

const loadMore = async () => {
  if (loadingMore.value || !hasMore.value) return
  loadingMore.value = true
  currentPage.value++
  await loadRefundList(currentPage.value)
  loadingMore.value = false
}

onMounted(() => {
  loadRefundList()
  window.addEventListener('scroll', handleScroll)
  window.addEventListener('refund-update', handleRefundUpdate)
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
  window.removeEventListener('refund-update', handleRefundUpdate)
})

const handleRefundUpdate = () => {
  currentPage.value = 1
  hasMore.value = true
  loadRefundList()
}
</script>

<style scoped>
@import url('@/static/css/user/售后列表.css');
</style>
