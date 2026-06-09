<template>
  <div v-if="loading" class="starlight-loader">
    <div class="loader-ring">
      <i class="fas fa-sparkles brand-icon"></i>
    </div>
    <p class="loader-text">加载中...</p>
  </div>

  <div v-else class="banner-page">
    <div class="page-actions">
      <button class="add-btn" @click="showAddModal = true" :disabled="submitting">
        <i class="fas fa-plus"></i>
        <span>添加Banner</span>
      </button>
    </div>

    <div class="banner-list">
      <div v-if="bannerList.length === 0" class="empty-state">
        <i class="fas fa-images"></i>
        <p>暂无Banner数据</p>
      </div>

      <div v-for="banner in bannerList" :key="banner.id" class="banner-item">
        <div class="banner-image">
          <img :src="banner.imageUrl" :alt="banner.title" />
        </div>
        <div class="banner-info">
          <h3 class="banner-title">{{ banner.title }}</h3>
          <p class="banner-link">{{ banner.linkUrl || '无跳转链接' }}</p>
          <div class="banner-meta">
            <span class="status-tag" :class="banner.status ? 'active' : 'inactive'">
              {{ banner.status ? '启用' : '禁用' }}
            </span>
            <span class="sort-tag">排序: {{ banner.sortOrder }}</span>
          </div>
          <div class="banner-time">
            <span v-if="banner.startTime">{{ formatDate(banner.startTime) }} 至 {{ formatDate(banner.endTime) }}</span>
            <span v-else>永久有效</span>
          </div>
        </div>
        <div class="banner-actions">
          <button class="action-btn edit" @click="handleEdit(banner)">
            <i class="fas fa-edit"></i>
          </button>
          <button class="action-btn toggle" @click="handleToggleStatus(banner)">
            <i class="fas" :class="banner.status ? 'fa-eye-slash' : 'fa-eye'"></i>
          </button>
          <button class="action-btn delete" @click="handleDelete(banner)">
            <i class="fas fa-trash"></i>
          </button>
        </div>
      </div>
    </div>

    <div v-if="showAddModal" class="modal-overlay" @click="closeModal">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>{{ editingBanner ? '编辑Banner' : '添加Banner' }}</h3>
          <button class="modal-close" @click="closeModal"><i class="fas fa-times"></i></button>
        </div>
        <div v-if="submitting" class="modal-loading">
          <div class="loader-ring-sm"><i class="fas fa-sparkles"></i></div>
          <p class="loader-text-sm">处理中...</p>
        </div>
        <div v-else class="modal-body">
          <div class="form-group">
            <label class="form-label required">标题</label>
            <input type="text" v-model="formData.title" class="form-input" placeholder="请输入Banner标题" />
          </div>
          <div class="form-group">
            <label class="form-label required">图片地址</label>
            <input type="text" v-model="formData.imageUrl" class="form-input" placeholder="请输入图片URL" />
          </div>
          <div class="form-group">
            <label class="form-label">跳转链接</label>
            <input type="text" v-model="formData.linkUrl" class="form-input" placeholder="请输入跳转链接（可选）" />
          </div>
          <div class="form-group">
            <label class="form-label">排序序号</label>
            <input type="number" v-model="formData.sortOrder" class="form-input" min="0" />
          </div>
          <div class="form-group">
            <label class="form-label">状态</label>
            <label class="switch-label">
              <input type="checkbox" v-model="formData.status" class="form-switch" />
              <span class="switch-slider"></span>
              <span class="switch-text">{{ formData.status ? '启用' : '禁用' }}</span>
            </label>
          </div>
          <div class="form-group">
            <label class="form-label">生效时间</label>
            <div class="time-picker-group">
              <input type="datetime-local" v-model="formData.startTime" class="form-input time-input" />
              <span class="time-separator">至</span>
              <input type="datetime-local" v-model="formData.endTime" class="form-input time-input" />
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-cancel" @click="closeModal">取消</button>
          <button class="btn-confirm" @click="handleSubmit">确定</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import Message from '@/utils/message'
import { adminAPI } from '@/api/adminAPI'

interface Banner {
  id: number
  title: string
  imageUrl: string
  linkUrl: string
  sortOrder: number
  status: number
  startTime: string
  endTime: string
  position: string
  createdAt: string
}

const bannerList = ref<Banner[]>([])
const showAddModal = ref(false)
const editingBanner = ref<Banner | null>(null)
const loading = ref(false)
const submitting = ref(false)

const formData = ref({
  title: '',
  imageUrl: '',
  linkUrl: '',
  sortOrder: 0,
  status: 1,
  startTime: '',
  endTime: '',
  position: 'HOME'
})

const loadBannerList = async () => {
  loading.value = true
  try {
    const res = await adminAPI.getBannerList()
    if (res.success) {
      bannerList.value = res.data || []
    }
  } finally {
    loading.value = false
  }
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return '-'
  return dateStr.replace('T', ' ').substring(0, 16)
}

const closeModal = () => {
  showAddModal.value = false
  editingBanner.value = null
  formData.value = {
    title: '',
    imageUrl: '',
    linkUrl: '',
    sortOrder: 0,
    status: 1,
    startTime: '',
    endTime: '',
    position: 'HOME'
  }
}

const handleSubmit = async () => {
  if (!formData.value.title.trim()) {
    Message.error('标题不能为空')
    return
  }
  if (!formData.value.imageUrl.trim()) {
    Message.error('图片地址不能为空')
    return
  }

  submitting.value = true
  try {
    const data = {
      title: formData.value.title.trim(),
      imageUrl: formData.value.imageUrl.trim(),
      linkUrl: formData.value.linkUrl,
      sortOrder: formData.value.sortOrder,
      status: formData.value.status,
      startTime: formData.value.startTime || null,
      endTime: formData.value.endTime || null,
      position: formData.value.position
    }

    let res
    if (editingBanner.value) {
      res = await adminAPI.updateBanner(editingBanner.value.id, data)
    } else {
      res = await adminAPI.createBanner(data)
    }

    if (res.success) {
      Message.success(editingBanner.value ? '更新成功' : '创建成功')
      closeModal()
      loadBannerList()
    } else {
      Message.error(res.message || '操作失败')
    }
  } finally {
    submitting.value = false
  }
}

const handleEdit = (banner: Banner) => {
  editingBanner.value = banner
  formData.value = {
    title: banner.title,
    imageUrl: banner.imageUrl,
    linkUrl: banner.linkUrl || '',
    sortOrder: banner.sortOrder,
    status: banner.status,
    startTime: banner.startTime ? banner.startTime.replace('T', '').substring(0, 16) : '',
    endTime: banner.endTime ? banner.endTime.replace('T', '').substring(0, 16) : '',
    position: banner.position
  }
  showAddModal.value = true
}

const handleToggleStatus = async (banner: Banner) => {
  submitting.value = true
  try {
    const newStatus = banner.status === 1 ? 0 : 1
    const res = await adminAPI.toggleBannerStatus(banner.id, newStatus)
    if (res.success) {
      Message.success(res.message || '操作成功')
      loadBannerList()
    } else {
      Message.error(res.message || '操作失败')
    }
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (banner: Banner) => {
  try {
    await Message.confirm(`确定要删除Banner「${banner.title}」吗？`, '确认删除')

    submitting.value = true
    const res = await adminAPI.deleteBanner(banner.id)
    if (res.success) {
      Message.success('删除成功')
      loadBannerList()
    } else {
      Message.error(res.message || '删除失败')
    }
  } catch {
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadBannerList()
})
</script>

<style scoped>
@import '@/static/css/admin/轮播图管理.css';
@import '@/static/css/common/星环加载器.css';
</style>
