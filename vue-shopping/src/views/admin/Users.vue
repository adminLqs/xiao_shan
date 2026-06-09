<template>
  <div v-if="isLoading" class="starlight-loader">
    <div class="loader-ring">
      <i class="fas fa-sparkles brand-icon"></i>
    </div>
    <p class="loader-text">加载中...</p>
  </div>

  <div v-else class="users-page">
    <div class="search-bar">
      <div class="search-box">
        <i class="fas fa-search"></i>
        <input type="text" v-model="searchKeyword" placeholder="搜索账号、昵称..." @keyup.enter="handleSearch" />
        <button v-if="searchKeyword" class="clear-search" @click="clearSearch">
          <i class="fas fa-times"></i>
        </button>
      </div>
      <div class="filter-group">
        <select v-model="filterStatus" class="filter-select" @change="handleSearch">
          <option value="">全部状态</option>
          <option value="1">正常</option>
          <option value="0">禁用</option>
        </select>
        <button class="btn-search" @click="handleSearch">
          <i class="fas fa-search"></i> 搜索
        </button>
      </div>
    </div>

    <div class="users-table">
      <table v-if="users.length > 0" class="data-table">
        <thead>
          <tr>
            <th width="60">ID</th>
            <th width="70">头像</th>
            <th width="120">账号</th>
            <th width="100">昵称</th>
            <th width="90">角色</th>
            <th width="80">状态</th>
            <th width="150">注册时间</th>
            <th width="160" class="col-actions">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="user in users" :key="user.id">
            <td>{{ user.id }}</td>
            <td>
              <img v-if="user.avatar" :src="user.avatar" class="user-avatar" :alt="user.nickname" />
              <img v-else :src="defaultAvatar" class="user-avatar" />
            </td>
            <td class="col-account" :title="user.account">{{ user.account }}</td>
            <td class="col-nickname" :title="user.nickname">{{ user.nickname || '-' }}</td>
            <td>
              <span class="role-tag" :class="getRoleClass(user.role)">
                {{ getRoleLabel(user.role) }}
              </span>
            </td>
            <td>
              <span class="status-tag" :class="user.status === 1 ? 'active' : 'inactive'">
                {{ user.status === 1 ? '正常' : '禁用' }}
              </span>
            </td>
            <td>{{ formatDateTime(user.createdAt || user.createTime || user.create_time) }}</td>
            <td>
              <button class="action-btn" @click.stop="toggleDropdown(user.id)">
                <i class="fas fa-ellipsis-v"></i>
                <span>更多</span>
                <div v-if="dropdownOpen === user.id" class="dropdown-menu">
                  <button class="dropdown-item" @click.stop="handleViewDetail(user); dropdownOpen = null">
                    <i class="fas fa-eye"></i>
                    <span>查看详情</span>
                  </button>
                  <button 
                    class="dropdown-item" 
                    :class="user.status === 1 ? 'dropdown-disable' : 'dropdown-enable'"
                    @click.stop="handleToggleStatus(user); dropdownOpen = null"
                  >
                    <i class="fas" :class="user.status === 1 ? 'fa-ban' : 'fa-check-circle'"></i>
                    {{ user.status === 1 ? '禁用用户' : '启用用户' }}
                  </button>
                  <button class="dropdown-item dropdown-reset" @click.stop="handleResetPassword(user); dropdownOpen = null">
                    <i class="fas fa-key"></i>
                    <span>重置密码</span>
                  </button>
                </div>
              </button>
            </td>
          </tr>
        </tbody>
      </table>

      <div v-else class="empty-state">
        <i class="fas fa-users"></i>
        <p>暂无用户数据</p>
      </div>
    </div>

    <div class="pagination" v-if="total > 0">
      <div class="pagination-left">
        <button class="page-btn" :disabled="currentPage === 1" @click="changePage(currentPage - 1)">
          <i class="fas fa-chevron-left"></i>
        </button>
        <div class="page-numbers">
          <button v-if="currentPage > 3" class="page-number-btn" @click="changePage(1)">1</button>
          <span v-if="currentPage > 4" class="page-ellipsis">...</span>
          <button 
            v-for="page in visiblePages" 
            :key="page" 
            class="page-number-btn" 
            :class="{ active: page === currentPage }"
            @click="changePage(page)"
          >{{ page }}</button>
          <span v-if="currentPage < totalPages - 3" class="page-ellipsis">...</span>
          <button v-if="currentPage < totalPages - 2" class="page-number-btn" @click="changePage(totalPages)">{{ totalPages }}</button>
        </div>
        <button class="page-btn" :disabled="currentPage === totalPages" @click="changePage(currentPage + 1)">
          <i class="fas fa-chevron-right"></i>
        </button>
      </div>
      <div class="pagination-right">
        <span class="total-info">共 {{ total }} 条用户</span>
        <select v-model="pageSize" class="page-size-select" @change="handleSearch">
          <option :value="10">10条/页</option>
          <option :value="20">20条/页</option>
          <option :value="50">50条/页</option>
        </select>
      </div>
    </div>

    <div v-if="showDetailModal" class="modal-overlay" @click="showDetailModal = false">
      <div class="modal-content modal-content-large" @click.stop>
        <div class="modal-header">
          <h3>用户详情</h3>
          <button class="modal-close" @click="showDetailModal = false"><i class="fas fa-times"></i></button>
        </div>
        <div class="modal-body">
          <div class="detail-section">
            <div class="detail-avatar">
              <img :src="detailUser.avatar || defaultAvatar" class="detail-user-avatar" />
            </div>
            <div class="detail-info">
              <h4>{{ detailUser.nickname || detailUser.account }}</h4>
              <p class="detail-account">账号：{{ detailUser.account }}</p>
              <p class="detail-role">角色：{{ getRoleLabel(detailUser.role) }}</p>
              <p class="detail-status">状态：{{ detailUser.status === 1 ? '正常' : '禁用' }}</p>
            </div>
          </div>
          <div class="detail-field">
            <span class="detail-label">用户ID</span>
            <span class="detail-value">{{ detailUser.id }}</span>
          </div>
          <div class="detail-field">
            <span class="detail-label">注册时间</span>
            <span class="detail-value">{{ formatDateTime(detailUser.createdAt) }}</span>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-cancel" @click="showDetailModal = false">关闭</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import Message from '@/utils/message'
import { adminAPI } from '@/api/adminAPI'
import defaultAvatar from '@/static/images/user-avatar.jpg'

interface User {
  id: number
  account: string
  nickname: string
  avatar: string
  status: number
  role: string
  createdAt: string
  createTime?: string
  create_time?: string
}

const isLoading = ref(false)
const submitting = ref(false)
const users = ref<User[]>([])
const searchKeyword = ref('')
const filterStatus = ref('')
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const showDetailModal = ref(false)
const detailUser = ref<User>({} as User)
const dropdownOpen = ref<number | null>(null)

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))

const visiblePages = computed(() => {
  const pages: number[] = []
  const start = Math.max(1, currentPage.value - 2)
  const end = Math.min(totalPages.value, currentPage.value + 2)
  for (let i = start; i <= end; i++) {
    if (!pages.includes(i)) pages.push(i)
  }
  return pages
})

const loadUsers = async () => {
  isLoading.value = true
  try {
    const response = await adminAPI.getUsers({
      page: currentPage.value,
      pageSize: pageSize.value,
      keyword: searchKeyword.value.trim() || undefined,
      status: filterStatus.value !== '' ? Number(filterStatus.value) : undefined
    })
    if (response.success) {
      const data = response.data || {}
      users.value = (data.records || []).map((u: any) => ({
        ...u,
        status: (u.status === true || u.status === 1) ? 1 : 0,
        createdAt: u.createdAt || u.created_at || u.createTime || u.create_time || ''
      }))
      total.value = data.total || 0
    } else {
      throw new Error(response.message || '加载用户失败')
    }
  } catch (error: any) {
    Message.error(error.message || '加载用户失败，请重试')
  } finally {
    isLoading.value = false
  }
}

const getRoleLabel = (role: string) => {
  switch (role) {
    case 'ROLE_USER':
      return '普通用户'
    case 'ROLE_SELLER':
      return '商家'
    case 'ROLE_ADMIN':
      return '管理员'
    default:
      return role
  }
}

const getRoleClass = (role: string) => {
  switch (role) {
    case 'ROLE_USER':
      return 'role-user'
    case 'ROLE_SELLER':
      return 'role-seller'
    case 'ROLE_ADMIN':
      return 'role-admin'
    default:
      return ''
  }
}

const handleViewDetail = (user: User) => {
  detailUser.value = user
  showDetailModal.value = true
}

const handleResetPassword = async (user: User) => {
  try {
    await Message.confirm(`确定要重置用户「${user.account}」的密码吗？重置后密码将变为默认密码。`, '确认重置密码')
    
    submitting.value = true
    const response = await adminAPI.resetUserPassword(user.id)
    if (response.success) {
      Message.success(response.message || '密码重置成功')
    } else {
      Message.error(response.message || '密码重置失败')
    }
  } catch {
  } finally {
    submitting.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  loadUsers()
}

const clearSearch = () => {
  searchKeyword.value = ''
  handleSearch()
}

const changePage = (page: number) => {
  currentPage.value = page
  loadUsers()
}

const handleToggleStatus = async (user: User) => {
  const newStatus = user.status === 1 ? 0 : 1
  const action = newStatus === 1 ? '启用' : '禁用'

  try {
    await Message.confirm(`确定要${action}用户「${user.account}」吗？`, `${action}确认`)

    submitting.value = true
    const response = await adminAPI.toggleUserStatus(user.id, newStatus)
    if (response.success) {
      Message.success(`${action}成功`)
      loadUsers()
    } else {
      Message.error(response.message || `${action}失败`)
    }
  } catch {
  } finally {
    submitting.value = false
  }
}

const toggleDropdown = (userId: number) => {
  dropdownOpen.value = dropdownOpen.value === userId ? null : userId
}

const closeDropdown = (e: MouseEvent) => {
  const target = e.target as HTMLElement
  if (!target.closest('.action-btn')) {
    dropdownOpen.value = null
  }
}

const formatDateTime = (dateStr: string | undefined) => {
  if (!dateStr) return ''
  return dateStr.replace('T', ' ').substring(0, 16)
}

onMounted(() => {
  loadUsers()
  document.addEventListener('click', closeDropdown)
})

onUnmounted(() => {
  document.removeEventListener('click', closeDropdown)
})
</script>

<style scoped>
@import '@/static/css/admin/用户管理.css';
@import '@/static/css/common/骨架屏.css';
@import '@/static/css/common/星环加载器.css';
</style>