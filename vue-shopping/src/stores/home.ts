import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useHomeStore = defineStore('home', () => {
  const products = ref<any[]>([])
  const allCategories = ref<any[]>([])
  const scrollTop = ref(0)
  const currentPage = ref(1)
  const hasMore = ref(true)

  const saveScrollPosition = () => {
    scrollTop.value = window.scrollY
  }

  const restoreScrollPosition = () => {
    window.scrollTo({ top: scrollTop.value, behavior: 'instant' })
  }

  const resetHomeData = () => {
    products.value = []
    allCategories.value = []
    scrollTop.value = 0
    currentPage.value = 1
    hasMore.value = true
  }

  return {
    products,
    allCategories,
    scrollTop,
    currentPage,
    hasMore,
    saveScrollPosition,
    restoreScrollPosition,
    resetHomeData
  }
})