<template>
  <div class="applications-page">
    <!-- 统计卡片 -->
    <div class="stats-cards">
      <div class="stat-card">
        <div class="stat-icon pending">
          <i class="fas fa-clock"></i>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ stats.pending }}</span>
          <span class="stat-label">待审核</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon approved">
          <i class="fas fa-check-circle"></i>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ stats.approved }}</span>
          <span class="stat-label">已通过</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon rejected">
          <i class="fas fa-times-circle"></i>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ stats.rejected }}</span>
          <span class="stat-label">已驳回</span>
        </div>
      </div>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <div class="filter-tabs">
        <button
          v-for="tab in statusTabs"
          :key="tab.value"
          class="filter-tab"
          :class="{ active: filterStatus === tab.value }"
          @click="handleStatusChange(tab.value)"
        >
          {{ tab.label }}
          <span class="tab-count">{{ tab.count }}</span>
        </button>
      </div>
    </div>

    <!-- 申请列表 -->
    <div class="applications-list">
      <div v-if="loading" class="loading-state">
        <i class="fas fa-spinner fa-spin"></i>
        <span>加载中...</span>
      </div>

      <div v-else-if="applications.length === 0" class="empty-state">
        <i class="fas fa-inbox"></i>
        <span>暂无申请记录</span>
      </div>

      <div v-else class="list-content">
        <table class="data-table">
          <thead>
            <tr>
              <th>申请人</th>
              <th>店铺信息</th>
              <th>申请时间</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in applications" :key="item.id">
              <td>
                <div class="user-info">
                  <span class="name">{{ item.contactName }}</span>
                  <span class="phone">{{ item.contactPhone }}</span>
                </div>
              </td>
              <td>
                <div class="store-info">
                  <span class="store-name">{{ item.storeName }}</span>
                  <span class="business-type">{{ getBusinessTypeLabel(item.businessType) }}</span>
                </div>
              </td>
              <td>
                <span class="date">{{ formatDate(item.createdAt) }}</span>
              </td>
              <td>
                <span class="status-badge" :class="item.status.toLowerCase()">
                  {{ getStatusLabel(item.status) }}
                </span>
              </td>
              <td>
                <div class="action-btns">
                  <button class="btn-link" @click="handleViewDetail(item)">查看</button>
                  <button
                    v-if="item.status === 'PENDING'"
                    class="btn-link approve"
                    @click="handleApprove(item)"
                  >通过</button>
                  <button
                    v-if="item.status === 'PENDING'"
                    class="btn-link reject"
                    @click="handleReject(item)"
                  >驳回</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 分页 -->
      <div v-if="total > 0" class="pagination">
        <span class="pagination-info">共 {{ total }} 条</span>
        <div class="pagination-controls">
          <button
            class="page-btn"
            :disabled="page <= 1"
            @click="handlePageChange(page - 1)"
          >
            <i class="fas fa-chevron-left"></i>
          </button>
          <span class="page-current">{{ page }} / {{ totalPages }}</span>
          <button
            class="page-btn"
            :disabled="page >= totalPages"
            @click="handlePageChange(page + 1)"
          >
            <i class="fas fa-chevron-right"></i>
          </button>
        </div>
      </div>
    </div>

    <!-- 详情弹窗 -->
    <div v-if="showDetailModal" class="modal-overlay" @click.self="closeDetailModal">
      <div class="modal-content detail-modal">
        <div class="modal-header">
          <h3>申请详情</h3>
          <button class="close-btn" @click="closeDetailModal">
            <i class="fas fa-times"></i>
          </button>
        </div>
        <div class="modal-body">
          <div class="detail-section">
            <h4>基本信息</h4>
            <div class="detail-grid">
              <div class="detail-item">
                <label>联系人</label>
                <span>{{ currentApplication?.contactName }}</span>
              </div>
              <div class="detail-item">
                <label>联系电话</label>
                <span>{{ currentApplication?.contactPhone }}</span>
              </div>
              <div class="detail-item">
                <label>联系邮箱</label>
                <span>{{ currentApplication?.contactEmail }}</span>
              </div>
            </div>
          </div>
          <div class="detail-section">
            <h4>店铺信息</h4>
            <div class="detail-grid">
              <div class="detail-item">
                <label>店铺名称</label>
                <span>{{ currentApplication?.storeName }}</span>
              </div>
              <div class="detail-item">
                <label>经营类型</label>
                <span>{{ getBusinessTypeLabel(currentApplication?.businessType) }}</span>
              </div>
              <div class="detail-item">
                <label>主营类目</label>
                <span>{{ getCategoryLabel(currentApplication?.mainCategory) }}</span>
              </div>
              <div class="detail-item full">
                <label>店铺描述</label>
                <span>{{ currentApplication?.storeDetail || '无' }}</span>
              </div>
            </div>
          </div>
          <div class="detail-section">
            <h4>资质文件</h4>
            <div class="images-preview">
              <div class="image-item">
                <span class="image-label">营业执照</span>
                <img
                  v-if="currentApplication?.businessLicense"
                  :src="currentApplication.businessLicense"
                  alt="营业执照"
                  @click="previewImage(currentApplication.businessLicense)"
                />
                <span v-else class="no-image">未上传</span>
              </div>
              <div class="image-item">
                <span class="image-label">身份证正面</span>
                <img
                  v-if="currentApplication?.idCardFront"
                  :src="currentApplication.idCardFront"
                  alt="身份证正面"
                  @click="previewImage(currentApplication.idCardFront)"
                />
                <span v-else class="no-image">未上传</span>
              </div>
              <div class="image-item">
                <span class="image-label">身份证反面</span>
                <img
                  v-if="currentApplication?.idCardBack"
                  :src="currentApplication.idCardBack"
                  alt="身份证反面"
                  @click="previewImage(currentApplication.idCardBack)"
                />
                <span v-else class="no-image">未上传</span>
              </div>
            </div>
          </div>
          <div v-if="currentApplication?.reviewNotes" class="detail-section">
            <h4>审核备注</h4>
            <div class="review-notes">
              <span class="status-badge" :class="currentApplication.status.toLowerCase()">
                {{ getStatusLabel(currentApplication.status) }}
              </span>
              <p>{{ currentApplication.reviewNotes }}</p>
            </div>
          </div>
        </div>
        <div v-if="currentApplication?.status === 'PENDING'" class="modal-footer">
          <button class="btn btn-reject" @click="handleReject(currentApplication)">驳回</button>
          <button class="btn btn-approve" @click="handleApprove(currentApplication)">审核通过</button>
        </div>
      </div>
    </div>

    <!-- 驳回弹窗 -->
    <div v-if="showRejectModal" class="modal-overlay" @click.self="closeRejectModal">
      <div class="modal-content reject-modal">
        <div class="modal-header">
          <h3>驳回申请</h3>
          <button class="close-btn" @click="closeRejectModal">
            <i class="fas fa-times"></i>
          </button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label>驳回原因 <span class="required">*</span></label>
            <textarea
              v-model="rejectReason"
              class="form-textarea"
              placeholder="请输入驳回原因"
              rows="4"
            ></textarea>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-cancel" @click="closeRejectModal">取消</button>
          <button class="btn btn-confirm-reject" @click="confirmReject" :disabled="!rejectReason.trim()">
            确认驳回
          </button>
        </div>
      </div>
    </div>

    <!-- 图片预览弹窗 -->
    <div v-if="previewUrl" class="modal-overlay" @click.self="previewUrl = ''">
      <div class="image-preview-modal">
        <button class="close-btn" @click="previewUrl = ''">
          <i class="fas fa-times"></i>
        </button>
        <img :src="previewUrl" alt="预览图片" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { adminAPI } from '@/api/adminAPI'
import Message from '@/utils/message'

interface Application {
  id: number
  userId: number
  contactName: string
  contactPhone: string
  contactEmail: string
  storeName: string
  storeDetail: string
  businessType: string
  mainCategory: string
  businessLicense: string
  idCardFront: string
  idCardBack: string
  status: string
  reviewNotes: string
  reviewedBy: number
  createdAt: string
  updatedAt: string
  reviewedAt: string
}

const loading = ref(true)
const applications = ref<Application[]>([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const filterStatus = ref('')

const stats = reactive({
  total: 0,
  pending: 0,
  approved: 0,
  rejected: 0
})

const statusTabs = computed(() => [
  { label: '全部', value: '', count: stats.total },
  { label: '待审核', value: 'PENDING', count: stats.pending },
  { label: '已通过', value: 'APPROVED', count: stats.approved },
  { label: '已驳回', value: 'REJECTED', count: stats.rejected }
])

const totalPages = computed(() => Math.ceil(total.value / pageSize.value) || 1)

const showDetailModal = ref(false)
const showRejectModal = ref(false)
const currentApplication = ref<Application | null>(null)
const rejectReason = ref('')
const previewUrl = ref('')

const businessTypeMap: Record<string, string> = {
  'INDIVIDUAL': '个人',
  'SOLE_PROPRIETOR': '个体工商户',
  'COMPANY': '企业'
}

const categoryMap: Record<string, string> = {
  'CLOTHING': '服装鞋帽',
  'DIGITAL': '数码家电',
  'FOOD': '食品饮料',
  'BEAUTY': '美妆护肤',
  'HOME': '家居生活',
  'SPORTS': '运动户外',
  'BOOKS': '图书文具',
  'BABY': '母婴用品',
  'OTHER': '其他'
}

const statusMap: Record<string, string> = {
  'PENDING': '待审核',
  'APPROVED': '已通过',
  'REJECTED': '已驳回'
}

const getStatusLabel = (status: string) => statusMap[status] || status
const getBusinessTypeLabel = (type: string | undefined) => type ? (businessTypeMap[type] || type) : '-'
const getCategoryLabel = (category: string | undefined) => category ? (categoryMap[category] || category) : '-'

const formatDate = (dateStr: string) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const loadStats = async () => {
  try {
    const res = await adminAPI.getApplicationStats()
    if (res.success && res.data) {
      stats.total = res.data.total || 0
      stats.pending = res.data.pending || 0
      stats.approved = res.data.approved || 0
      stats.rejected = res.data.rejected || 0
    }
  } catch (error: any) {
    Message.error(error.message || '加载统计数据失败')
  }
}

const loadApplications = async () => {
  loading.value = true
  try {
    const res = await adminAPI.getApplications({
      page: page.value,
      size: pageSize.value,
      status: filterStatus.value || undefined
    })
    if (res.success && res.data) {
      applications.value = res.data.data || []
      total.value = res.data.total || 0
    }
  } catch (error) {
    Message.error('加载申请列表失败')
  } finally {
    loading.value = false
  }
}

const handleStatusChange = (status: string) => {
  filterStatus.value = status
  page.value = 1
  loadApplications()
}

const handlePageChange = (newPage: number) => {
  page.value = newPage
  loadApplications()
}

const handleViewDetail = async (item: Application) => {
  try {
    const res = await adminAPI.getApplicationDetail(item.id)
    if (res.success && res.data) {
      currentApplication.value = res.data
      showDetailModal.value = true
    }
  } catch (error) {
    Message.error('加载详情失败')
  }
}

const closeDetailModal = () => {
  showDetailModal.value = false
  currentApplication.value = null
}

const handleApprove = async (item: Application) => {
  try {
    await Message.confirm(`确认通过"${item.storeName}"的入驻申请？`, '审核通过')
    const res = await adminAPI.approveApplication(item.id)
    if (res.success) {
      Message.success('审核通过')
      closeDetailModal()
      loadApplications()
      loadStats()
    }
  } catch {
    // 用户取消
  }
}

const handleReject = (item: Application) => {
  currentApplication.value = item
  rejectReason.value = ''
  showRejectModal.value = true
  showDetailModal.value = false
}

const closeRejectModal = () => {
  showRejectModal.value = false
  rejectReason.value = ''
}

const confirmReject = async () => {
  if (!currentApplication.value || !rejectReason.value.trim()) return

  try {
    const res = await adminAPI.rejectApplication(currentApplication.value.id, {
      reviewNotes: rejectReason.value.trim()
    })
    if (res.success) {
      Message.success('已驳回申请')
      closeRejectModal()
      loadApplications()
      loadStats()
    }
  } catch (error) {
    Message.error('操作失败')
  }
}

const previewImage = (url: string) => {
  previewUrl.value = url
}

onMounted(() => {
  loadStats()
  loadApplications()
})
</script>

<style scoped>
@import url('@/static/css/admin/商家审核.css');
</style>
