<template>
  <div class="orders-container page-container">
    <div class="page-navbar">
      <button class="page-nav-back" @click="router.back()">
        <i class="fas fa-chevron-left"></i>
      </button>
      <div class="page-nav-title">
        <span>我的订单</span>
      </div>
      <div class="page-nav-right"></div>
    </div>

    <!-- Tab 标签栏 -->
    <div class="order-tabs-scroll" ref="tabsRef">
      <div
        v-for="tab in tabs"
        :key="tab.value"
        class="order-tab"
        :class="{ active: activeTab === tab.value }"
        :ref="el => { if (el) tabRefs[tab.value] = el as HTMLElement }"
        @click="handleTabClick(tab.value)"
      >
        {{ tab.label }}
      </div>
      <div class="tab-underline" :style="underlineStyle"></div>
    </div>

    <!-- 缓存组件区域 -->
    <div class="tab-content">
      <keep-alive>
        <component :is="currentComponent" :key="activeTab" />
      </keep-alive>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, markRaw, onMounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { defineAsyncComponent } from 'vue'

// 动态导入子组件并使用 markRaw 包装
const AllOrders = markRaw(defineAsyncComponent(() => import('./components/AllOrders.vue')))
const PendingOrders = markRaw(defineAsyncComponent(() => import('./components/PendingOrders.vue')))
const PaidOrders = markRaw(defineAsyncComponent(() => import('./components/PaidOrders.vue')))
const ShippedOrders = markRaw(defineAsyncComponent(() => import('./components/ShippedOrders.vue')))
const CancelledOrders = markRaw(defineAsyncComponent(() => import('./components/CancelledOrders.vue')))

const router = useRouter()
const route = useRoute()

/** 有效的订单状态列表 */
const VALID_STATUSES = ['PENDING', 'PAID', 'PROCESSING', 'SHIPPED', 'COMPLETED', 'CANCELLED']

/** 标签页配置 */
interface TabConfig {
  label: string
  value: string
  component: any
}

const tabs = ref<TabConfig[]>([
  { label: '全部', value: 'all', component: AllOrders },
  { label: '待付款', value: 'PENDING', component: PendingOrders },
  { label: '待发货', value: 'PAID', component: PaidOrders },
  { label: '待收货', value: 'SHIPPED', component: ShippedOrders },
  { label: '已取消', value: 'CANCELLED', component: CancelledOrders }
])

const activeTab = ref('all')
const tabsRef = ref<HTMLElement>()
const tabRefs = ref<Record<string, HTMLElement>>({})
const underlineStyle = ref({ left: '0px', width: '0px' })

const currentComponent = computed(() => {
  const tab = tabs.value.find(t => t.value === activeTab.value)
  return tab?.component || tabs.value[0]!.component
})

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

/** 同步订单状态到 URL */
const updateQueryStatus = (status: string): void => {
  const currentQuery = { ...route.query }
  if (status === 'all') {
    delete currentQuery.status
  } else {
    currentQuery.status = status
  }
  router.replace({ path: route.path, query: currentQuery })
}

/** 处理标签页点击 */
const handleTabClick = (status: string): void => {
  if (activeTab.value === status) return
  activeTab.value = status
  updateQueryStatus(status)
  nextTick(() => updateUnderline(status))
}

// 监听路由参数变化
watch(() => route.query.status, (newStatus) => {
  if (newStatus && VALID_STATUSES.includes(newStatus as string)) {
    activeTab.value = newStatus as string
  } else {
    activeTab.value = 'all'
  }
  nextTick(() => updateUnderline(activeTab.value))
}, { immediate: true })

onMounted(() => {
  nextTick(() => updateUnderline(activeTab.value))
})
</script>

<style scoped>
@import url('@/static/css/user/订单列表.css');
</style>
