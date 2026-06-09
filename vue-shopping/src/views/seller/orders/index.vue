<template>
  <div class="seller-orders-page">
    <div class="order-tabs">
      <div
        v-for="tab in tabs"
        :key="tab.value"
        class="order-tab"
        :class="{ active: activeTab === tab.value }"
        @click="handleTabClick(tab.value)"
      >
        {{ tab.label }}
        <span v-if="tab.count > 0" class="tab-count">{{ tab.count }}</span>
      </div>
      <div class="tab-underline" ref="underlineRef"></div>
    </div>

    <component :is="currentComponent" :key="activeTab" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SellerAllOrders from './SellerAllOrders.vue'
import SellerPendingOrders from './SellerPendingOrders.vue'
import SellerPaidOrders from './SellerPaidOrders.vue'
import SellerShippedOrders from './SellerShippedOrders.vue'
import SellerCompletedOrders from './SellerCompletedOrders.vue'
import SellerCancelledOrders from './SellerCancelledOrders.vue'
import SellerRefundingOrders from './SellerRefundingOrders.vue'
import SellerRefundedOrders from './SellerRefundedOrders.vue'

const route = useRoute()
const router = useRouter()
const activeTab = ref('all')
const underlineRef = ref<HTMLElement | null>(null)

const tabs = ref([
  { label: '全部', value: 'all', count: 0 },
  { label: '待付款', value: 'PENDING', count: 0 },
  { label: '待发货', value: 'PAID,PROCESSING', count: 0 },
  { label: '待收货', value: 'SHIPPED', count: 0 },
  { label: '已完成', value: 'COMPLETED', count: 0 },
  { label: '已取消', value: 'CANCELLED', count: 0 },
  { label: '退款中', value: 'REFUNDING', count: 0 },
  { label: '已退款', value: 'REFUNDED', count: 0 }
])

const currentComponent = computed(() => {
  const components: Record<string, any> = {
    all: SellerAllOrders,
    PENDING: SellerPendingOrders,
    'PAID,PROCESSING': SellerPaidOrders,
    SHIPPED: SellerShippedOrders,
    COMPLETED: SellerCompletedOrders,
    CANCELLED: SellerCancelledOrders,
    REFUNDING: SellerRefundingOrders,
    REFUNDED: SellerRefundedOrders
  }
  return components[activeTab.value] || SellerAllOrders
})

const updateUnderline = (): void => {
  const activeTabEl = document.querySelector('.order-tab.active') as HTMLElement
  const underline = underlineRef.value

  if (!activeTabEl || !underline) return

  const tabsContainer = activeTabEl.parentElement as HTMLElement
  const activeRect = activeTabEl.getBoundingClientRect()
  const containerRect = tabsContainer?.getBoundingClientRect()

  if (!containerRect) return

  const left = activeRect.left - containerRect.left + (tabsContainer.scrollLeft || 0)
  const width = activeRect.width

  underline.style.left = `${left}px`
  underline.style.width = `${width}px`
}

const handleTabClick = (value: string): void => {
  activeTab.value = value
  router.push({ query: value === 'all' ? {} : { tab: value } })
}

watch(activeTab, () => {
  requestAnimationFrame(() => {
    updateUnderline()
  })
})

const updateActiveTabFromRoute = () => {
  const tab = route.query.tab as string
  if (tab) {
    activeTab.value = tab
  } else {
    const status = route.query.status as string
    if (status) {
      const statusMap: Record<string, string> = {
        PAID: 'PAID,PROCESSING',
        PROCESSING: 'PAID,PROCESSING',
        SHIPPED: 'SHIPPED',
        COMPLETED: 'COMPLETED',
        CANCELLED: 'CANCELLED',
        REFUNDING: 'REFUNDING',
        REFUNDED: 'REFUNDED',
        PENDING: 'PENDING'
      }
      activeTab.value = statusMap[status] || 'all'
    } else {
      activeTab.value = 'all'
    }
  }
}

watch(() => route.query.tab, () => {
  updateActiveTabFromRoute()
})

watch(() => route.query.status, () => {
  updateActiveTabFromRoute()
})

onMounted(() => {
  updateActiveTabFromRoute()
  requestAnimationFrame(() => {
    updateUnderline()
  })
  window.addEventListener('resize', updateUnderline)
})
</script>

<style scoped>
  @import url('@/static/css/seller/订单管理页.css');
</style>
