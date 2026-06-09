<template>
  <div class="admin-layout">
    <!-- 侧边栏 -->
    <aside class="sidebar" :class="{ collapsed: sidebarCollapsed }">
      <div class="sidebar-header">
        <div class="logo">
          <i class="fas fa-cog"></i>
          <span v-if="!sidebarCollapsed">后台管理</span>
        </div>
        <button class="collapse-btn" @click="toggleSidebar">
          <i class="fas" :class="sidebarCollapsed ? 'fa-angle-right' : 'fa-angle-left'"></i>
        </button>
      </div>

      <nav class="sidebar-nav">
        <router-link :to="{name: 'AdminDashboard'}" class="nav-item" active-class="active">
          <i class="fas fa-tachometer-alt"></i>
          <span v-if="!sidebarCollapsed">控制台</span>
        </router-link>
        <router-link :to="{name: 'AdminApplications'}" class="nav-item" active-class="active">
          <i class="fas fa-store"></i>
          <span v-if="!sidebarCollapsed">商家审核</span>
        </router-link>
        <router-link :to="{name: 'AdminSellers'}" class="nav-item" active-class="active">
          <i class="fas fa-store-alt"></i>
          <span v-if="!sidebarCollapsed">商家管理</span>
        </router-link>
        <router-link :to="{name: 'AdminProducts'}" class="nav-item" active-class="active">
          <i class="fas fa-box"></i>
          <span v-if="!sidebarCollapsed">商品管理</span>
        </router-link>
        <router-link :to="{name: 'AdminCategories'}" class="nav-item" active-class="active">
          <i class="fas fa-tags"></i>
          <span v-if="!sidebarCollapsed">分类管理</span>
        </router-link>
        <router-link :to="{name: 'AdminBanners'}" class="nav-item" active-class="active">
          <i class="fas fa-images"></i>
          <span v-if="!sidebarCollapsed">Banner管理</span>
        </router-link>
        <router-link :to="{name: 'AdminUsers'}" class="nav-item" active-class="active">
          <i class="fas fa-users"></i>
          <span v-if="!sidebarCollapsed">用户管理</span>
        </router-link>
        <router-link :to="{name: 'AdminAdminUsers'}" class="nav-item" active-class="active">
          <i class="fas fa-user-shield"></i>
          <span v-if="!sidebarCollapsed">管理员管理</span>
        </router-link>
      </nav>
    </aside>

    <!-- 主内容区 -->
    <div class="main-content">
      <!-- 顶部栏 -->
      <header class="topbar">
        <div class="topbar-left">
          <h1 class="page-title">{{ pageTitle }}</h1>
        </div>
        <div class="topbar-right">
          <div class="admin-info">
            <span class="admin-name">{{ adminName || '管理员' }}</span>
          </div>
          <button class="logout-btn" @click="handleLogout">
            <i class="fas fa-sign-out-alt"></i>
            <span>退出</span>
          </button>
        </div>
      </header>

      <!-- 页面内容 -->
      <div class="content-wrapper">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const sidebarCollapsed = ref(false)

const adminName = computed(() => {
  // 从 authStore 获取用户信息
  const profile = authStore.role || '管理员'
  return profile === 'ROLE_ADMIN' ? '管理员' : profile
})

const pageTitle = computed(() => {
  const routeMap: Record<string, string> = {
    'AdminDashboard': '控制台',
    'AdminApplications': '商家入驻审核',
    'AdminSellers': '商家管理',
    'AdminProducts': '商品管理',
    'AdminCategories': '分类管理',
    'AdminBanners': 'Banner管理',
    'AdminUsers': '用户管理',
    'AdminAdminUsers': '管理员管理',
  }
  return routeMap[route.name as string] || '管理后台'
})

const toggleSidebar = () => {
  sidebarCollapsed.value = !sidebarCollapsed.value
}

const handleLogout = async () => {
  await authStore.logout()
  router.replace({ name: 'Login' })
}

onMounted(() => {
  authStore.validateAdminPermission()
})
</script>

<style scoped>
@import url('@/static/css/admin/后台布局.css');
</style>
