<template>
  <div class="categories-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-title">商品分类</h1>
    </div>

    <!-- 分类列表 -->
    <div class="categories-container">
      <div 
        v-for="category in level1Categories" 
        :key="category.id" 
        class="category-group"
      >
        <div class="category-header" @click="toggleCategory(category.id)">
          <div class="category-icon">
            <i :class="category.icon || 'fas fa-tag'"></i>
          </div>
          <span class="category-name">{{ category.name }}</span>
          <i class="fas fa-chevron-down arrow" :class="{ expanded: expandedCategories.includes(category.id) }"></i>
        </div>
        
        <!-- 二级分类 -->
        <div class="subcategories" v-show="expandedCategories.includes(category.id)">
          <div 
            v-for="sub in getSubcategories(category.id)" 
            :key="sub.id" 
            class="subcategory-item"
            @click="selectCategory(sub.id)"
          >
            <span>{{ sub.name }}</span>
            <i class="fas fa-chevron-right"></i>
          </div>
        </div>
      </div>
    </div>

    <!-- 热门搜索 -->
    <div class="hot-search-section">
      <div class="section-header">
        <h2 class="section-title">
          <i class="fas fa-search"></i>
          热门搜索
        </h2>
      </div>
      <div class="hot-tags">
        <span 
          v-for="tag in hotTags" 
          :key="tag" 
          class="hot-tag"
          @click="searchTag(tag)"
        >
          {{ tag }}
        </span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { authAPI } from '@/api/authAPI'

const router = useRouter()

interface Category {
  id: number
  name: string
  parentId: number | null
  icon?: string
  isActive: boolean
}

const level1Categories = ref<Category[]>([])
const allCategories = ref<Category[]>([])
const expandedCategories = ref<number[]>([1])

const hotTags = [
  '手机', '电脑', '服装', '家电', '美妆', '食品', '运动', '母婴'
]

const loadCategories = async () => {
  try {
    const response = await authAPI.getAllCategories()
    if (response.success && response.data?.categories) {
      allCategories.value = response.data.categories
      level1Categories.value = allCategories.value.filter(
        cat => cat.parentId === null && cat.isActive
      )
    }
  } catch (error) {
    Message.error('加载分类失败')
}

const getSubcategories = (parentId: number): Category[] => {
  return allCategories.value.filter(
    cat => cat.parentId === parentId && cat.isActive
  )
}

const toggleCategory = (categoryId: number) => {
  const index = expandedCategories.value.indexOf(categoryId)
  if (index > -1) {
    expandedCategories.value.splice(index, 1)
  } else {
    expandedCategories.value.push(categoryId)
  }
}

const selectCategory = (categoryId: number) => {
  router.push({
    name: 'UserDashboard',
    query: { categoryId }
  })
}

const searchTag = (tag: string) => {
  router.push({
    name: 'UserDashboard',
    query: { keyword: tag }
  })
}

onMounted(async () => {
  await loadCategories()
})
</script>

<style scoped>
.categories-page {
  min-height: 100vh;
  background-color: #f5f7fa;
}

/* 页面头部 */
.page-header {
  padding: 16px;
  background: white;
  border-bottom: 1px solid #f0f0f0;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

/* 分类列表 */
.categories-container {
  margin: 12px;
  background: white;
  border-radius: 12px;
  overflow: hidden;
}

.category-group {
  border-bottom: 1px solid #f0f0f0;
}

.category-group:last-child {
  border-bottom: none;
}

.category-header {
  display: flex;
  align-items: center;
  padding: 14px 16px;
  cursor: pointer;
  min-height: 48px;
}

.category-header:active {
  background: #f8fafc;
}

.category-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: linear-gradient(135deg, #f1f5f9, #e2e8f0);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
}

.category-icon i {
  font-size: 16px;
  color: #4a6491;
}

.category-name {
  flex: 1;
  font-size: 15px;
  color: #333;
}

.arrow {
  font-size: 14px;
  color: #94a3b8;
  transition: transform 0.3s;
}

.arrow.expanded {
  transform: rotate(180deg);
}

/* 二级分类 */
.subcategories {
  background: #fafafa;
}

.subcategory-item {
  display: flex;
  align-items: center;
  padding: 12px 16px 12px 60px;
  cursor: pointer;
  min-height: 44px;
}

.subcategory-item:active {
  background: #f0f0f0;
}

.subcategory-item span {
  flex: 1;
  font-size: 14px;
  color: #64748b;
}

.subcategory-item i {
  font-size: 14px;
  color: #cbd5e1;
}

/* 热门搜索 */
.hot-search-section {
  margin: 12px;
  padding: 16px;
  background: white;
  border-radius: 12px;
}

.section-header {
  margin-bottom: 12px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  margin: 0;
  display: flex;
  align-items: center;
}

.section-title i {
  margin-right: 8px;
  color: #4a6491;
}

.hot-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.hot-tag {
  padding: 6px 14px;
  background: #f1f5f9;
  border-radius: 20px;
  font-size: 13px;
  color: #64748b;
  cursor: pointer;
}

.hot-tag:active {
  background: #e2e8f0;
}
</style>
