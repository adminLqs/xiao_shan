<template>
  <div v-if="loading" class="starlight-loader">
    <div class="loader-ring">
      <i class="fas fa-sparkles brand-icon"></i>
    </div>
    <p class="loader-text">加载中...</p>
  </div>

  <div v-else class="category-page">
    <div class="page-actions">
      <button class="add-btn" @click="showAddModal = true" :disabled="submitting">
        <i class="fas fa-plus"></i>
        <span>添加分类</span>
      </button>
    </div>

    <div class="category-tree">
      <div class="tree-header">
        <span>分类层级</span>
      </div>
      <div class="tree-content">
        <div v-if="categoryTree.length === 0" class="empty-state">
          <i class="fas fa-folder-open"></i>
          <p>暂无分类数据</p>
        </div>

        <div v-for="level1 in categoryTree" :key="level1.id" class="level1-item">
          <div class="level1-header">
            <span class="level1-name">{{ level1.name }}</span>
            <span class="status-tag" :class="level1.isActive ? 'active' : 'inactive'">
              {{ level1.isActive ? '启用' : '禁用' }}
            </span>
            <div class="level1-actions">
              <button class="action-btn edit" @click="handleEdit(level1)">
                <i class="fas fa-edit"></i>
              </button>
              <button class="action-btn add-child" @click="handleAddChild(level1)">
                <i class="fas fa-plus"></i>
              </button>
              <button class="action-btn toggle" @click="handleToggleStatus(level1)">
                <i class="fas" :class="level1.isActive ? 'fa-eye-slash' : 'fa-eye'"></i>
              </button>
              <button class="action-btn delete" @click="handleDelete(level1)">
                <i class="fas fa-trash"></i>
              </button>
            </div>
          </div>

          <div v-if="level1.children && level1.children.length > 0" class="level2-list">
            <div v-for="level2 in level1.children" :key="level2.id" class="level2-item">
              <span class="level2-name">├── {{ level2.name }}</span>
              <span class="status-tag" :class="level2.isActive ? 'active' : 'inactive'">
                {{ level2.isActive ? '启用' : '禁用' }}
              </span>
              <div class="level2-actions">
                <button class="action-btn edit" @click="handleEdit(level2)">
                  <i class="fas fa-edit"></i>
                </button>
                <button class="action-btn toggle" @click="handleToggleStatus(level2)">
                  <i class="fas" :class="level2.isActive ? 'fa-eye-slash' : 'fa-eye'"></i>
                </button>
                <button class="action-btn delete" @click="handleDelete(level2)">
                  <i class="fas fa-trash"></i>
                </button>
              </div>
            </div>
          </div>

          <div v-else class="level2-empty">
            <button class="add-child-btn" @click="handleAddChild(level1)">
              <i class="fas fa-plus"></i> 添加二级分类
            </button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="showAddModal" class="modal-overlay" @click="showAddModal = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>添加分类</h3>
          <button class="modal-close" @click="showAddModal = false"><i class="fas fa-times"></i></button>
        </div>
        <div v-if="submitting" class="modal-loading">
          <div class="loader-ring-sm"><i class="fas fa-sparkles"></i></div>
          <p class="loader-text-sm">处理中...</p>
        </div>
        <div v-else class="modal-body">
          <div class="form-group">
            <label class="form-label">分类类型</label>
            <div class="radio-group">
              <label class="radio-label">
                <input type="radio" name="type" v-model="formData.type" value="level1" @change="handleTypeChange" />
                <span>一级分类</span>
              </label>
              <label class="radio-label">
                <input type="radio" name="type" v-model="formData.type" value="level2" @change="handleTypeChange" />
                <span>二级分类</span>
              </label>
            </div>
          </div>
          <div class="form-group" v-if="formData.type === 'level2'">
            <label class="form-label">父分类</label>
            <select v-model="formData.parentId" class="form-select">
              <option :value="null">请选择父分类</option>
              <option v-for="cat in level1Options" :key="cat.id" :value="cat.id">{{ cat.name }}</option>
            </select>
          </div>
          <div class="form-group">
            <label class="form-label">分类名称</label>
            <input type="text" v-model="formData.name" class="form-input" placeholder="请输入分类名称" />
          </div>
          <div class="form-group">
            <label class="form-label">排序序号</label>
            <input type="number" v-model="formData.sortOrder" class="form-input" min="0" />
          </div>
          <div class="form-group">
            <label class="form-label">状态</label>
            <label class="switch-label">
              <input type="checkbox" v-model="formData.isActive" class="form-switch" />
              <span class="switch-slider"></span>
              <span class="switch-text">{{ formData.isActive ? '启用' : '禁用' }}</span>
            </label>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-cancel" @click="showAddModal = false">取消</button>
          <button class="btn-confirm" @click="handleSubmit">确定</button>
        </div>
      </div>
    </div>

    <div v-if="showEditModal" class="modal-overlay" @click="showEditModal = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>编辑分类</h3>
          <button class="modal-close" @click="showEditModal = false"><i class="fas fa-times"></i></button>
        </div>
        <div v-if="submitting" class="modal-loading">
          <div class="loader-ring-sm"><i class="fas fa-sparkles"></i></div>
          <p class="loader-text-sm">处理中...</p>
        </div>
        <div v-else class="modal-body">
          <div class="form-group">
            <label class="form-label">分类名称</label>
            <input type="text" v-model="editFormData.name" class="form-input" placeholder="请输入分类名称" />
          </div>
          <div class="form-group">
            <label class="form-label">排序序号</label>
            <input type="number" v-model="editFormData.sortOrder" class="form-input" min="0" />
          </div>
          <div class="form-group">
            <label class="form-label">状态</label>
            <label class="switch-label">
              <input type="checkbox" v-model="editFormData.isActive" class="form-switch" />
              <span class="switch-slider"></span>
              <span class="switch-text">{{ editFormData.isActive ? '启用' : '禁用' }}</span>
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

interface Category {
  id: number
  name: string
  parentId: number | null
  sortOrder: number
  isActive: boolean
  createdAt?: string
  children?: Category[]
}

const categoryTree = ref<Category[]>([])
const level1Options = ref<{ id: number; name: string }[]>([])
const loading = ref(false)
const submitting = ref(false)

const showAddModal = ref(false)
const showEditModal = ref(false)

const formData = ref({
  type: 'level1' as 'level1' | 'level2',
  parentId: null as number | null,
  name: '',
  sortOrder: 0,
  isActive: true
})

const editFormData = ref({
  id: 0,
  name: '',
  sortOrder: 0,
  isActive: true
})

const loadCategoryTree = async () => {
  loading.value = true
  try {
    const res = await adminAPI.getCategoryTree()
    if (res.success) {
      categoryTree.value = res.data || []
      level1Options.value = categoryTree.value.map(c => ({
        id: c.id,
        name: c.name
      }))
    }
  } finally {
    loading.value = false
  }
}

const handleTypeChange = () => {
  formData.value.parentId = null
}

const handleSubmit = async () => {
  if (!formData.value.name.trim()) {
    Message.error('分类名称不能为空')
    return
  }

  submitting.value = true
  try {
    const data = {
      name: formData.value.name.trim(),
      parentId: formData.value.type === 'level2' ? formData.value.parentId : null,
      sortOrder: formData.value.sortOrder,
      isActive: formData.value.isActive
    }

    const res = await adminAPI.createCategory(data)
    if (res.success) {
      Message.success('创建成功')
      showAddModal.value = false
      formData.value = {
        type: 'level1',
        parentId: null,
        name: '',
        sortOrder: 0,
        isActive: true
      }
      loadCategoryTree()
    } else {
      Message.error(res.message || '创建失败')
    }
  } finally {
    submitting.value = false
  }
}

const handleEdit = (category: Category) => {
  editFormData.value = {
    id: category.id,
    name: category.name,
    sortOrder: category.sortOrder,
    isActive: category.isActive
  }
  showEditModal.value = true
}

const handleEditSubmit = async () => {
  if (!editFormData.value.name.trim()) {
    Message.error('分类名称不能为空')
    return
  }

  submitting.value = true
  try {
    const res = await adminAPI.updateCategory(editFormData.value.id, {
      name: editFormData.value.name.trim(),
      sortOrder: editFormData.value.sortOrder,
      isActive: editFormData.value.isActive
    })

    if (res.success) {
      Message.success('更新成功')
      showEditModal.value = false
      loadCategoryTree()
    } else {
      Message.error(res.message || '更新失败')
    }
  } finally {
    submitting.value = false
  }
}

const handleAddChild = (level1: Category) => {
  formData.value = {
    type: 'level2',
    parentId: level1.id,
    name: '',
    sortOrder: 0,
    isActive: true
  }
  showAddModal.value = true
}

const handleToggleStatus = async (category: Category) => {
  submitting.value = true
  try {
    const newStatus = !category.isActive
    const res = await adminAPI.toggleCategoryStatus(category.id, newStatus)
    if (res.success) {
      Message.success(res.message || '操作成功')
      loadCategoryTree()
    } else {
      Message.error(res.message || '操作失败')
    }
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (category: Category) => {
  try {
    await Message.confirm(`确定要删除分类「${category.name}」吗？`, '确认删除')

    submitting.value = true
    const res = await adminAPI.deleteCategory(category.id)
    if (res.success) {
      Message.success('删除成功')
      loadCategoryTree()
    } else {
      Message.error(res.message || '删除失败')
    }
  } catch {
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadCategoryTree()
})
</script>

<style scoped>
@import '@/static/css/admin/分类管理.css';
@import '@/static/css/common/骨架屏.css';
@import '@/static/css/common/星环加载器.css';
</style>
