<template>
  <div class="search-layout">
    <div class="search-navbar">
      <button class="search-back" @click="router.back()">
        <i class="fas fa-chevron-left"></i>
      </button>
      <div class="search-input-wrap">
        <i class="fas fa-search search-input-icon"></i>
        <input
          v-model="keyword"
          @keyup.enter="doSearch"
          @focus="onFocus"
          @input="handleInput"
          ref="searchInput"
        />
        <button v-if="keyword" class="search-clear" @click.stop="clearSearch">
          <i class="fas fa-times-circle"></i>
        </button>
        <div class="search-placeholder-wrapper" v-show="!keyword">
          <span class="search-placeholder-text">{{ placeholderText }}</span>
        </div>
      </div>
    </div>

    <div v-if="showDropdown" ref="dropdownRef" class="search-dropdown">
      <div v-if="keyword" class="dropdown-section">
        <div class="section-header">
          <span>搜索建议</span>
        </div>
        <div v-if="loadingSuggest" class="loading-text">加载中...</div>
        <div v-else-if="suggestions.length > 0" class="suggest-list">
          <div v-for="item in suggestions" :key="item" class="suggest-item" @click.stop="handleSuggestClick(item)">
            <i class="fas fa-search"></i>
            <span>{{ item }}</span>
          </div>
        </div>
        <div v-else class="empty-text">暂无搜索建议</div>
      </div>
      <div v-else class="empty-text">输入关键词搜索商品</div>
    </div>

    <router-view v-slot="{ Component }" v-show="!showDropdown">
    <transition name="slide-up" mode="out-in">
      <component :is="Component" :key="route.path" />
    </transition>
  </router-view>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { authAPI } from '@/api/authAPI'

const router = useRouter()
const route = useRoute()

const keyword = ref('')
const searchInput = ref<HTMLInputElement | null>(null)
const dropdownRef = ref<HTMLDivElement | null>(null)
const placeholderText = ref('')
const showDropdown = ref(false)
const suggestions = ref<string[]>([])
const loadingSuggest = ref(false)
let suggestTimer: ReturnType<typeof setTimeout> | null = null

const onFocus = () => {
  showDropdown.value = true
  if (keyword.value) {
    handleInput()
  }
}

const doSearch = () => {
  const searchWord = keyword.value.trim() || placeholderText.value.trim()
  if (!searchWord) return
  saveSearchHistory(searchWord)
  showDropdown.value = false
  router.push({ name: 'SearchResult', query: { keyword: searchWord } })
}

const saveSearchHistory = (word: string) => {
  const history = JSON.parse(localStorage.getItem('searchHistory') || '[]')
  const filtered = history.filter((h: string) => h !== word)
  filtered.unshift(word)
  localStorage.setItem('searchHistory', JSON.stringify(filtered.slice(0, 10)))
}

const handleSuggestClick = (word: string) => {
  keyword.value = word
  saveSearchHistory(word)
  showDropdown.value = false
  router.push({ name: 'SearchResult', query: { keyword: word } })
}

const handleInput = () => {
  if (suggestTimer) clearTimeout(suggestTimer)
  if (!keyword.value.trim()) {
    suggestions.value = []
    showDropdown.value = false
    return
  }
  showDropdown.value = true
  suggestTimer = setTimeout(async () => {
    loadingSuggest.value = true
    try {
      const res = await authAPI.getSearchSuggest(keyword.value)
      if (res.success && res.data) {
        suggestions.value = Array.isArray(res.data) ? res.data : []
      }
    } catch {}
    loadingSuggest.value = false
  }, 300)
}

const clearSearch = () => {
  keyword.value = ''
  suggestions.value = []
  showDropdown.value = false
  router.push({ name: 'SearchDefault' })
}

const onClickOutside = (e: MouseEvent) => {
  const target = e.target as Node
  if (!searchInput.value?.contains(target) && !dropdownRef.value?.contains(target)) {
    showDropdown.value = false
  }
}

watch(() => route.query.keyword, (val) => {
  if (val) {
    keyword.value = val as string
  }
})

watch(() => route.query.placeholder, (val) => {
  if (val) {
    placeholderText.value = val as string
  }
})

onMounted(() => {
  document.addEventListener('click', onClickOutside)
  const queryKeyword = route.query.keyword as string
  if (queryKeyword) {
    keyword.value = queryKeyword
  }
  if (route.query.placeholder) {
    placeholderText.value = route.query.placeholder as string
  }
  if (route.query.autofocus === 'true') {
    nextTick(() => {
      searchInput.value?.focus()
    })
  }
})

onUnmounted(() => {
  if (suggestTimer) clearTimeout(suggestTimer)
  document.removeEventListener('click', onClickOutside)
})
</script>

<style scoped>
@import url('@/static/css/user/搜索页.css');

.search-placeholder-wrapper {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  overflow: hidden;
  pointer-events: none;
}

.search-placeholder-text {
  font-size: 14px;
  color: #94a3b8;
  white-space: nowrap;
}
</style>
