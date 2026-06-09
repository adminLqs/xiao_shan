<template>
  <div v-if="loading" class="starlight-loader">
    <div class="loader-ring">
      <i class="fas fa-sparkles brand-icon"></i>
    </div>
    <p class="loader-text">加载中...</p>
  </div>

  <div v-else class="admin-users-page">
    <div class="page-actions">
      <button class="add-btn" @click="showAddModal = true" :disabled="submitting">
        <i class="fas fa-plus"></i>
        <span>新增管理员</span>
      </button>
    </div>

    <div class="admin-table">
      <table v-if="adminList.length > 0" class="data-table">
        <thead>
          <tr>
            <th width="60">ID</th>
            <th width="70">头像</th>
            <th width="120" class="col-account">账号</th>
            <th width="100" class="col-nickname">昵称</th>
            <th width="80">状态</th>
            <th width="150">创建时间</th>
            <th width="220" class="col-actions">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="admin in adminList" :key="admin.id">
            <td>{{ admin.id }}</td>
            <td>
              <img
                :src="getAvatarUrl(admin.avatar)"
                :alt="admin.nickname"
                class="admin-avatar"
                @error="handleAvatarError"
              />
            </td>
            <td class="col-account" :title="admin.account">{{ admin.account }}</td>
            <td class="col-nickname" :title="admin.nickname">{{ admin.nickname || '-' }}</td>
            <td>
              <span class="status-tag" :class="admin.status ? 'active' : 'inactive'">
                {{ admin.status ? '正常' : '禁用' }}
              </span>
            </td>
            <td>{{ formatDateTime(admin.createdAt) }}</td>
            <td class="actions-cell">
              <button class="action-btn edit" @click="handleEdit(admin)">
                <i class="fas fa-edit"></i>
                <span>编辑</span>
              </button>
              <button
                class="action-btn"
                :class="admin.status ? 'btn-disable' : 'btn-enable'"
                @click="handleToggleStatus(admin)"
              >
                <i class="fas" :class="admin.status ? 'fa-ban' : 'fa-check-circle'"></i>
                {{ admin.status ? '禁用' : '启用' }}
              </button>
              <button class="action-btn delete" @click="handleDelete(admin)">
                <i class="fas fa-trash"></i>
                <span>删除</span>
              </button>
            </td>
          </tr>
        </tbody>
      </table>

      <div v-else class="empty-state">
        <i class="fas fa-users"></i>
        <p>暂无管理员数据</p>
      </div>
    </div>

    <div v-if="showAddModal" class="modal-overlay" @click="showAddModal = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>新增管理员</h3>
          <button class="modal-close" @click="showAddModal = false"><i class="fas fa-times"></i></button>
        </div>
        <div v-if="submitting" class="modal-loading">
          <div class="loader-ring-sm"><i class="fas fa-sparkles"></i></div>
          <p class="loader-text-sm">处理中...</p>
        </div>
        <div v-else class="modal-body">
          <div class="form-group">
            <label class="form-label">账号</label>
            <input type="text" v-model="addForm.account" class="form-input" placeholder="请输入账号" />
          </div>
          <div class="form-group">
            <label class="form-label">密码</label>
            <input type="password" v-model="addForm.password" class="form-input" placeholder="请输入密码（至少6位）" />
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-cancel" @click="showAddModal = false">取消</button>
          <button class="btn-confirm" @click="handleAdd">确定</button>
        </div>
      </div>
    </div>

    <div v-if="showEditModal" class="modal-overlay" @click="showEditModal = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>编辑管理员</h3>
          <button class="modal-close" @click="showEditModal = false"><i class="fas fa-times"></i></button>
        </div>
        <div v-if="submitting" class="modal-loading">
          <div class="loader-ring-sm"><i class="fas fa-sparkles"></i></div>
          <p class="loader-text-sm">处理中...</p>
        </div>
        <div v-else class="modal-body">
          <div class="form-group">
            <label class="form-label">账号</label>
            <input type="text" :value="editForm.account" disabled class="form-input form-input-disabled" />
          </div>
          <div class="form-group">
            <label class="form-label">昵称</label>
            <input type="text" v-model="editForm.nickname" class="form-input" placeholder="请输入昵称" />
          </div>
          <div class="form-group">
            <label class="form-label">新密码</label>
            <input type="password" v-model="editForm.password" class="form-input" placeholder="不填则不修改密码（至少6位）" />
          </div>
          <div class="form-group">
            <label class="form-label">状态</label>
            <label class="switch-label">
              <input type="checkbox" v-model="editForm.status" class="form-switch" />
              <span class="switch-slider"></span>
              <span class="switch-text">{{ editForm.status ? '正常' : '禁用' }}</span>
            </label>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-cancel" @click="showEditModal = false">取消</button>
          <button class="btn-confirm" @click="handleEditSubmit">确定</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import Message from '@/utils/message'
import { adminAPI } from '@/api/adminAPI'
import defaultAvatar from '@/static/images/user-avatar.jpg'

interface Admin {
  id: number
  account: string
  nickname: string
  avatar: string
  role: string
  status: boolean
  createdAt: string
}

const adminList = ref<Admin[]>([])
const loading = ref(false)
const submitting = ref(false)
const showAddModal = ref(false)
const showEditModal = ref(false)

const addForm = ref({
  account: '',
  password: ''
})

const editForm = ref({
  id: 0,
  account: '',
  nickname: '',
  password: '',
  status: true
})

const loadAdminList = async () => {
  loading.value = true
  try {
    const res = await adminAPI.getAdminList()
    if (res.success) {
      const data = res.data || []
      adminList.value = data.map((row: any) => ({
        ...row,
        status: !!row.status,
        createdAt: row.createdAt || row.created_at || row.createTime || row.create_time || ''
      }))
    } else {
      throw new Error(res.message || '加载管理员列表失败')
    }
  } catch (error: any) {
    Message.error(error.message || '加载管理员列表失败，请重试')
  } finally {
    loading.value = false
  }
}

const formatDateTime = (dateStr: string) => {
  if (!dateStr) return ''
  return dateStr.replace('T', ' ').substring(0, 16)
}

const getAvatarUrl = (avatar: string) => {
  if (!avatar || avatar.trim() === '') {
    return defaultAvatar
  }
  return avatar.trim()
}

const handleAvatarError = (event: Event) => {
  const target = event.target as HTMLImageElement
  target.src = defaultAvatar
}

const handleAdd = async () => {
  if (!addForm.value.account.trim()) {
    Message.error('账号不能为空')
    return
  }
  if (!addForm.value.password || addForm.value.password.length < 6) {
    Message.error('密码长度不能少于6位')
    return
  }

  submitting.value = true
  try {
    const res = await adminAPI.createAdmin({
      account: addForm.value.account.trim(),
      password: addForm.value.password
    })

    if (res.success) {
      Message.success('创建成功')
      showAddModal.value = false
      addForm.value = { account: '', password: '' }
      loadAdminList()
    } else {
      Message.error(res.message || '创建失败')
    }
  } finally {
    submitting.value = false
  }
}

const handleEdit = (admin: Admin) => {
  editForm.value = {
    id: admin.id,
    account: admin.account,
    nickname: admin.nickname || '',
    password: '',
    status: admin.status
  }
  showEditModal.value = true
}

const handleEditSubmit = async () => {
  if (editForm.value.password && editForm.value.password.length < 6) {
    Message.error('密码长度不能少于6位')
    return
  }

  submitting.value = true
  try {
    const data: {
      password?: string
      nickname?: string
      status?: number
    } = {}

    if (editForm.value.password && editForm.value.password.trim()) {
      data.password = editForm.value.password.trim()
    }
    if (editForm.value.nickname !== undefined) {
      data.nickname = editForm.value.nickname.trim()
    }
    data.status = editForm.value.status ? 1 : 0

    const res = await adminAPI.updateAdmin(editForm.value.id, data)

    if (res.success) {
      Message.success('更新成功')
      showEditModal.value = false
      loadAdminList()
    } else {
      Message.error(res.message || '更新失败')
    }
  } finally {
    submitting.value = false
  }
}

const handleToggleStatus = async (admin: Admin) => {
  const newStatus = !admin.status
  const action = newStatus ? '启用' : '禁用'

  try {
    await Message.confirm(`确定要${action}管理员「${admin.account}」吗？`, `${action}确认`)

    submitting.value = true
    const res = await adminAPI.updateAdmin(admin.id, { status: newStatus ? 1 : 0 })
    if (res.success) {
      Message.success(`${action}成功`)
      loadAdminList()
    } else {
      Message.error(res.message || `${action}失败`)
    }
  } catch {
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (admin: Admin) => {
  try {
    await Message.confirm(`确定要删除管理员「${admin.account}」吗？`, '确认删除')

    submitting.value = true
    const res = await adminAPI.deleteAdmin(admin.id)
    if (res.success) {
      Message.success('删除成功')
      loadAdminList()
    } else {
      Message.error(res.message || '删除失败')
    }
  } catch {
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadAdminList()
})
</script>

<style scoped>
@import '@/static/css/admin/管理员管理.css';
@import '@/static/css/common/骨架屏.css';
@import '@/static/css/common/星环加载器.css';
</style>
