<template>
  <div v-if="loading" class="starlight-loader">
    <div class="loader-ring">
      <i class="fas fa-sparkles brand-icon"></i>
    </div>
    <p class="loader-text">加载中...</p>
  </div>

  <div v-else class="sellers-page">
    <!-- 搜索筛选栏 -->
    <div class="filter-bar">
      <div class="filter-row">
        <div class="filter-left">
          <input
            v-model="keyword"
            type="text"
            placeholder="搜索店铺名称/联系人..."
            class="search-input"
            @keyup.enter="handleSearch"
          />
          <select v-model="filterStatus" class="filter-select" @change="handleSearch">
            <option value="">全部状态</option>
            <option value="1">营业中</option>
            <option value="0">已封禁</option>
          </select>
          <button class="btn btn-primary" @click="handleSearch">
            <i class="fas fa-search"></i>
            搜索
          </button>
        </div>
      </div>
    </div>

    <!-- 商家列表 -->
    <div class="card">
      <div class="card-body">
        <div v-if="sellers.length === 0" class="empty-state">
          <i class="fas fa-inbox"></i>
          <span>暂无商家记录</span>
        </div>

        <div v-else class="table-container">
          <table class="data-table">
            <thead>
              <tr>
                <th>店铺名称</th>
                <th>联系人</th>
                <th>联系电话</th>
                <th>经营类型</th>
                <th>主营类目</th>
                <th>入驻时间</th>
                <th>状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in sellers" :key="item.id">
                <td>
                  <div class="store-cell">
                    <img
                      v-if="item.logo"
                      :src="item.logo"
                      class="store-logo"
                      alt="logo"
                    />
                    <i v-else class="fas fa-store store-logo-placeholder"></i>
                    <span class="store-name">{{ item.storeName }}</span>
                  </div>
                </td>
                <td>{{ item.contactName }}</td>
                <td>{{ item.contactPhone }}</td>
                <td>{{ getBusinessTypeLabel(item.businessType) }}</td>
                <td>{{ getCategoryLabel(item.mainCategory) }}</td>
                <td>{{ formatDate(item.createdAt) }}</td>
                <td>
                  <span class="status-badge" :class="getStatusClass(item.status)">
                    {{ getStatusLabel(item.status) }}
                  </span>
                </td>
                <td>
                  <div class="action-btns">
                    <button class="btn-link" @click="handleViewDetail(item)">查看</button>
                    <button
                      v-if="item.status === 1"
                      class="btn-link ban"
                      @click="handleToggleStatus(item)"
                    >
                      封禁
                    </button>
                    <button
                      v-else
                      class="btn-link unban"
                      @click="handleToggleStatus(item)"
                    >
                      解封
                    </button>
                    <button class="btn-link reset" @click="handleResetPassword(item)">重置密码</button>
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
              class="page-number"
              :disabled="page <= 1"
              @click="handlePageChange(page - 1)"
            >
              <i class="fas fa-chevron-left"></i>
            </button>
            <span class="page-current">{{ page }} / {{ totalPages }}</span>
            <button
              class="page-number"
              :disabled="page >= totalPages"
              @click="handlePageChange(page + 1)"
            >
              <i class="fas fa-chevron-right"></i>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 详情弹窗 -->
    <div v-if="showDetailModal" class="modal-overlay" @click.self="closeDetailModal">
      <div class="modal-content detail-modal">
        <div class="modal-header">
          <h3>商家详情</h3>
          <button class="close-btn" @click="closeDetailModal">
            <i class="fas fa-times"></i>
          </button>
        </div>
        <div class="modal-body">
          <!-- 店铺信息 -->
          <div class="detail-section">
            <h4>店铺信息</h4>
            <div class="store-header">
              <img
                v-if="currentSeller?.logo"
                :src="currentSeller.logo"
                class="detail-logo"
                alt="logo"
              />
              <div v-else class="detail-logo-placeholder">
                <i class="fas fa-store"></i>
              </div>
              <div class="store-header-info">
                <div class="store-name">{{ currentSeller?.storeName }}</div>
                <span class="status-badge" :class="getStatusClass(currentSeller?.status)">
                  {{ getStatusLabel(currentSeller?.status) }}
                </span>
              </div>
            </div>
            <div class="detail-grid">
              <div class="detail-item">
                <label>经营类型</label>
                <span>{{ getBusinessTypeLabel(currentSeller?.businessType) }}</span>
              </div>
              <div class="detail-item">
                <label>主营类目</label>
                <span>{{ getCategoryLabel(currentSeller?.mainCategory) }}</span>
              </div>
              <div class="detail-item full-width">
                <label>店铺描述</label>
                <span>{{ currentSeller?.description || '-' }}</span>
              </div>
            </div>
          </div>

          <!-- 账号信息 -->
          <div class="detail-section">
            <h4>账号信息</h4>
            <div class="detail-grid">
              <div class="detail-item">
                <label>用户名</label>
                <span>{{ currentSeller?.username || '-' }}</span>
              </div>
              <div class="detail-item">
                <label>商家账号ID</label>
                <span>{{ currentSeller?.userId || currentSeller?.id }}</span>
              </div>
            </div>
          </div>

          <!-- 联系方式 -->
          <div class="detail-section">
            <h4>联系方式</h4>
            <div class="detail-grid">
              <div class="detail-item">
                <label>联系人</label>
                <span>{{ currentSeller?.contactName }}</span>
              </div>
              <div class="detail-item">
                <label>联系电话</label>
                <span>{{ currentSeller?.contactPhone }}</span>
              </div>
              <div class="detail-item full-width">
                <label>详细地址</label>
                <span>{{ currentSeller?.address || '-' }}</span>
              </div>
            </div>
          </div>

          <!-- 入驻申请原始信息 -->
          <div class="detail-section">
            <h4>入驻申请信息</h4>
            <div class="detail-grid">
              <div class="detail-item">
                <label>申请编号</label>
                <span>{{ currentSeller?.applicationId || '-' }}</span>
              </div>
              <div class="detail-item">
                <label>入驻时间</label>
                <span>{{ formatDate(currentSeller?.createdAt) }}</span>
              </div>
              <div class="detail-item">
                <label>审核时间</label>
                <span>{{ formatDate(currentSeller?.reviewedAt) || '-' }}</span>
              </div>
              <div class="detail-item">
                <label>审核备注</label>
                <span>{{ currentSeller?.reviewNotes || '-' }}</span>
              </div>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="modal-actions">
            <button
              class="btn"
              :class="currentSeller?.status === 1 ? 'btn-warning' : 'btn-success'"
              @click="handleToggleStatus(currentSeller)"
            >
              <i :class="currentSeller?.status === 1 ? 'fas fa-ban' : 'fas fa-unlock'"></i>
              {{ currentSeller?.status === 1 ? '封禁账号' : '解封账号' }}
            </button>
            <button class="btn btn-secondary" @click="handleResetPassword(currentSeller)">
              <i class="fas fa-key"></i>
              重置密码
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { adminAPI } from '@/api/adminAPI'
import Message from '@/utils/message'

// 商家列表
const sellers = ref<any[]>([])
const loading = ref(true)
const keyword = ref('')
const filterStatus = ref('')
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 详情弹窗
const showDetailModal = ref(false)
const currentSeller = ref<any>(null)

// 计算属性
const totalPages = computed(() => Math.ceil(total.value / pageSize.value))

// 状态映射
const statusMap: Record<string, string> = {
  'ACTIVE': '营业中',
  'INACTIVE': '休息中',
  'SUSPENDED': '已封禁',
  'PENDING': '待审核'
}

const businessTypeMap: Record<string, string> = {
  'RETAIL': '零售',
  'WHOLESALE': '批发',
  'O2O': 'O2O',
  'CUSTOM': '定制'
}

const categoryMap: Record<string, string> = {
  'ELECTRONICS': '数码电子',
  'CLOTHING': '服装鞋帽',
  'FOOD': '食品生鲜',
  'COSMETICS': '美妆护肤',
  'HOME': '家居用品',
  'BOOKS': '图书音像',
  'SPORTS': '运动户外',
  'OTHER': '其他'
}

// 方法
const getStatusLabel = (status: string | undefined) => status ? (statusMap[status] || status) : '-'
const getStatusClass = (status: string | undefined) => {
  if (!status) return ''
  const classMap: Record<string, string> = {
    'ACTIVE': 'status-approved',
    'INACTIVE': 'status-pending',
    'SUSPENDED': 'status-rejected',
    'PENDING': 'status-pending'
  }
  return classMap[status] || ''
}
const getBusinessTypeLabel = (type: string | undefined) => type ? (businessTypeMap[type] || type) : '-'
const getCategoryLabel = (category: string | undefined) => category ? (categoryMap[category] || category) : '-'

const formatDate = (dateStr: string | undefined) => {
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

// 加载商家列表
const loadSellers = async () => {
  loading.value = true
  try {
    const requestParams = {
      page: page.value,
      size: pageSize.value,
      keyword: keyword.value || undefined,
      status: filterStatus.value || undefined
    }

    const res = await adminAPI.getSellers(requestParams)

    if (res.success && res.data) {
      sellers.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch (error: any) {
    Message.error(error.message || '加载商家列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  page.value = 1
  loadSellers()
}

// 分页
const handlePageChange = (newPage: number) => {
  page.value = newPage
  loadSellers()
}

// 查看详情
const handleViewDetail = (item: any) => {
  currentSeller.value = { ...item }
  showDetailModal.value = true
}

// 关闭详情弹窗
const closeDetailModal = () => {
  showDetailModal.value = false
  currentSeller.value = null
}

// 封禁/解封
const handleToggleStatus = async (item: any) => {
  if (!item) return

  const isBan = item.status === 1
  const action = isBan ? '封禁' : '解封'
  const confirmMsg = isBan
    ? `确定要封禁"${item.storeName}"的账号吗？封禁后该商家将无法登录。`
    : `确定要解封"${item.storeName}"的账号吗？解封后商家可正常登录。`

  try {
    await Message.confirm(confirmMsg, `${action}确认`)
    const newStatus = isBan ? 0 : 1
    const res = await adminAPI.toggleUserStatus(item.userId || item.id, newStatus)
    if (res.success) {
      Message.success(`${action}成功`)
      closeDetailModal()
      loadSellers()
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      Message.error(error.message || `${action}失败`)
    }
  }
}

// 重置密码
const handleResetPassword = async (item: any) => {
  if (!item) return

  try {
    await Message.confirm(
      `确定要重置"${item.storeName}"的密码吗？重置后密码将恢复为默认密码。`,
      '重置密码确认'
    )
    const res = await adminAPI.resetSellerPassword(item.id)
    if (res.success) {
      Message.success('密码重置成功')
      closeDetailModal()
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      Message.error(error.message || '密码重置失败')
    }
  }
}

onMounted(() => {
  loadSellers()
})
</script>

<style scoped>
@import url('@/static/css/admin/商家管理.css');
@import '@/static/css/common/星环加载器.css';
</style>
