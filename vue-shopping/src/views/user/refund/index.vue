<template>
  <div class="refund-flow-page page-container">
    <!-- 顶部导航 -->
    <div class="page-navbar">
      <button class="page-nav-back" @click="router.back()">
        <i class="fas fa-chevron-left"></i>
      </button>
      <div class="page-nav-title">
        <span>{{ pageTitle }}</span>
      </div>
      <div class="page-nav-right"></div>
    </div>

    <!-- 步骤指示器 -->
    <div v-if="!loading || route.name === 'RefundApply'" class="step-indicator">
      <template v-for="(step, index) in steps" :key="index">
        <div
          class="step-item"
          :class="{ active: currentStep >= index + 1, completed: currentStep > index + 1 }"
        >
          <div class="step-dot">
            <i v-if="currentStep > index + 1" class="fas fa-check"></i>
            <span v-else>{{ index + 1 }}</span>
          </div>
          <div class="step-label">{{ step }}</div>
        </div>
        <!-- 连接线 - 在步骤之间（除了最后一个步骤） -->
        <div
          v-if="index < steps.length - 1"
          class="step-line"
          :class="{ completed: currentStep > index + 1 }"
        ></div>
      </template>
    </div>

    <!-- 骨架屏 -->
    <div v-if="loading && route.name !== 'RefundApply'" class="refund-skeleton">
      <div class="skeleton-card"></div>
      <div class="skeleton-card"></div>
      <div class="skeleton-card"></div>
      <div class="skeleton-card"></div>
    </div>

    <!-- 子路由渲染 -->
    <router-view v-show="!loading || route.name === 'RefundApply'" />
  </div>
</template>

<script setup lang="ts">
import { computed, provide, ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()

// 加载状态，由子组件通过 provide/inject 修改
const loading = ref(true)

// 当前步骤，由子组件通过 provide/inject 修改
const currentStep = ref(1)

// 退款类型，由子组件通过 provide/inject 修改
const refundType = ref<'REFUND' | 'AFTER_SALE'>('REFUND')

// 根据路由 name 计算默认步骤（页面首次加载时使用）
const getDefaultStep = (): number => {
  const name = route.name as string

  if (name === 'RefundApply') return 1
  if (name === 'ReturnGoods') return 3
  if (name === 'RefundDetail') return refundType.value === 'AFTER_SALE' ? 4 : 3
  return 1
}

// 路由切换时重置加载状态
watch(() => route.name, (newName) => {
  if (newName !== 'RefundApply') {
    loading.value = true
  }
}, { immediate: true })

// 根据退款类型动态计算步骤
const steps = computed(() => {
  if (refundType.value === 'AFTER_SALE') {
    return ['申请退款', '商家处理', '退货物流', '退款完成']
  }
  return ['申请退款', '商家处理', '退款完成']
})

const pageTitle = computed(() => {
  const name = route.name as string
  if (name === 'RefundApply') return '申请退款'
  if (name === 'RefundChatStep') return '退款沟通'
  if (name === 'ReturnGoods') return '退货'
  if (name === 'RefundDetail') return '退款详情'
  return '退款售后'
})

// 向子组件提供加载状态、退款类型和当前步骤（可修改）
provide('loading', loading)
provide('refundType', refundType)
provide('currentStep', currentStep)
</script>

<style scoped>
@import url('@/static/css/user/退款售后.css');
</style>
