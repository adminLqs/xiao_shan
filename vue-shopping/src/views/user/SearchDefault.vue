<template>
  <div class="search-default">
    <div v-if="searchHistory.length > 0" class="search-history">
      <div class="history-header">
        <span>搜索历史</span>
        <button @click="clearHistory">清空</button>
      </div>
      <div class="history-tags">
        <span v-for="item in searchHistory" :key="item" class="history-tag" @click="handleSuggestClick(item)">
          {{ item }}
        </span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const searchHistory = ref<string[]>([])

const handleSuggestClick = (word: string) => {
  router.push({ name: 'SearchResult', query: { keyword: word } })
}

const clearHistory = () => {
  searchHistory.value = []
  localStorage.removeItem('searchHistory')
}

onMounted(() => {
  searchHistory.value = JSON.parse(localStorage.getItem('searchHistory') || '[]')
})
</script>

<style>
@import url('@/static/css/user/搜索页.css');

.search-default {
  padding: 0;
  padding-bottom: calc(40px + env(safe-area-inset-bottom));
}
</style>
