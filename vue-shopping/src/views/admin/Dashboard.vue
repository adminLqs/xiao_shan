<template>
  <div v-if="loading" class="starlight-loader">
    <div class="loader-ring">
      <i class="fas fa-sparkles brand-icon"></i>
    </div>
    <p class="loader-text">加载中...</p>
  </div>

  <div v-else class="dashboard-page">
    <!-- 统计卡片 -->
    <div class="stats-grid">
      <div class="stat-card pending-card">
        <div class="stat-icon">
          <i class="fas fa-clock"></i>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ stats.pending || 0 }}</span>
          <span class="stat-label">待审核申请</span>
        </div>
        <div class="stat-arrow">
          <router-link :to="{ name: 'AdminApplications', query: { status: 'PENDING' } }">
            <i class="fas fa-arrow-right"></i>
          </router-link>
        </div>
      </div>

      <div class="stat-card seller-card">
        <div class="stat-icon">
          <i class="fas fa-store"></i>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ stats.sellers || 0 }}</span>
          <span class="stat-label">商家总数</span>
        </div>
        <div class="stat-arrow">
          <router-link :to="{ name: 'AdminSellers' }">
            <i class="fas fa-arrow-right"></i>
          </router-link>
        </div>
      </div>

      <div class="stat-card user-card">
        <div class="stat-icon">
          <i class="fas fa-users"></i>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ stats.users || 0 }}</span>
          <span class="stat-label">用户总数</span>
        </div>
        <div class="stat-arrow">
          <router-link :to="{ name: 'AdminUsers' }">
            <i class="fas fa-arrow-right"></i>
          </router-link>
        </div>
      </div>

      <div class="stat-card product-card">
        <div class="stat-icon">
          <i class="fas fa-box"></i>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ stats.products || 0 }}</span>
          <span class="stat-label">商品总数</span>
        </div>
        <div class="stat-arrow">
          <router-link :to="{ name: 'AdminProducts' }">
            <i class="fas fa-arrow-right"></i>
          </router-link>
        </div>
      </div>
    </div>

    <!-- 快捷入口 -->
    <div class="quick-actions">
      <h3 class="section-title">快捷操作</h3>
      <div class="actions-grid">
        <div class="action-card" @click="goTo('AdminApplications')">
          <div class="action-icon applications">
            <i class="fas fa-file-invoice"></i>
          </div>
          <span class="action-title">商家审核</span>
          <span class="action-desc">审核商家入驻申请</span>
          <span v-if="stats.pending > 0" class="action-badge">{{ stats.pending }}</span>
        </div>

        <div class="action-card" @click="goTo('/admin/products')">
          <div class="action-icon products">
            <i class="fas fa-boxes"></i>
          </div>
          <span class="action-title">商品管理</span>
          <span class="action-desc">管理平台商品</span>
        </div>

        <div class="action-card" @click="goTo('/admin/users')">
          <div class="action-icon users">
            <i class="fas fa-user-circle"></i>
          </div>
          <span class="action-title">用户管理</span>
          <span class="action-desc">管理平台用户</span>
        </div>

        <div class="action-card" @click="goTo('/admin/sellers')">
          <div class="action-icon sellers">
            <i class="fas fa-store-alt"></i>
          </div>
          <span class="action-title">商家管理</span>
          <span class="action-desc">管理平台商家</span>
        </div>
      </div>
    </div>

    <!-- 待审核申请列表 -->
    <div class="recent-applications">
      <div class="section-header">
        <h3 class="section-title">待审核申请</h3>
        <router-link :to="{ name: 'AdminApplications', query: { status: 'PENDING' } }" class="view-all">
          查看全部 <i class="fas fa-chevron-right"></i>
        </router-link>
      </div>

      <div v-if="recentApplications.length === 0" class="empty-state">
        <i class="fas fa-inbox"></i>
        <span>暂无待审核申请</span>
      </div>

      <div v-else class="applications-list">
        <div v-for="item in recentApplications" :key="item.id" class="application-item">
          <div class="application-info">
            <h4 class="store-name">{{ item.storeName }}</h4>
            <p class="contact-info">
              {{ item.contactName }} | {{ item.contactPhone }}
            </p>
            <span class="apply-time">申请时间: {{ formatDate(item.createdAt) }}</span>
          </div>
          <div class="application-actions">
            <router-link :to="'/admin/applications/' + item.id" class="btn btn-view">
              查看详情
            </router-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { adminAPI } from '@/api/adminAPI'
import Message from '@/utils/message'

const router = useRouter()

const loading = ref(true)
const stats = reactive({
  pending: 0,
  sellers: 0,
  users: 0,
  products: 0
})

const recentApplications = ref<any[]>([])

const goTo = (name: string) => {
  router.push({ name })
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const loadStats = async () => {
  try {
    const appRes = await adminAPI.getApplicationStats()

    if (appRes.success && appRes.data) {
      stats.pending = appRes.data.pending || 0
      stats.sellers = appRes.data.approved || 0
    }
  } catch (error: any) {
    Message.error(error.message || '加载统计数据失败')
  }
}

const loadRecentApplications = async () => {
  try {
    const res = await adminAPI.getApplications({
      page: 1,
      size: 5,
      status: 'PENDING'
    })
    if (res.success && res.data) {
      recentApplications.value = res.data.data || []
    }
  } catch (error: any) {
    Message.error(error.message || '加载待审核申请失败')
  }
}

const loadAll = async () => {
  loading.value = true
  try {
    await Promise.all([loadStats(), loadRecentApplications()])
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadAll()
})
</script>

<style scoped>
@import url('@/static/css/admin/控制台.css');
@import '@/static/css/common/星环加载器.css';
</style>
