<template>
  <div class="mobile-layout">
    <!-- 主内容区 -->
    <main class="main-content">
      <RouterView />
    </main>

    <!-- 底部导航栏 -->
    <div class="tab-bar">
      <RouterLink to="/user/dashboard" class="tab-bar-item" active-class="active">
        <i class="fas fa-home"></i>
        <span>首页</span>
      </RouterLink>
      <RouterLink to="/user/categories" class="tab-bar-item" active-class="active">
        <i class="fas fa-th-large"></i>
        <span>分类</span>
      </RouterLink>
      <RouterLink to="/user/messages" class="tab-bar-item" active-class="active">
        <i class="fas fa-bell"></i>
        <span>消息</span>
        <span v-if="msgCount > 0" class="badge">{{ msgCount > 99 ? '99+' : msgCount }}</span>
      </RouterLink>
      <RouterLink to="/user/center" class="tab-bar-item" active-class="active">
        <i class="fas fa-user"></i>
        <span>我的</span>
      </RouterLink>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { RouterLink, RouterView } from 'vue-router'
import { authAPI } from '@/api/authAPI'
import Message from '@/utils/message'

const msgCount = ref<number>(0)

const loadMessageCount = async () => {
  try {
    // const response = await authAPI.getUnreadMessageCount()
    // if (response.success) {
    //   msgCount.value = response.data || 0
    // }
  } catch (error) {
    Message.error('加载消息数量失败:'+ error)
    msgCount.value = 0
  }
}

onMounted(async () => {
  await loadMessageCount()
})
</script>

<style scoped>
@import url('@/static/css/user/移动端布局');
</style>
