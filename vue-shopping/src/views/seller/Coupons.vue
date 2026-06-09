<template>
  <div class="coupon-management-container">
    <div class="action-bar">
      <div class="action-left">
        <button class="btn btn-primary" @click="openCreateDialog">
          <i class="fas fa-plus-circle"></i> 新建优惠券
        </button>
      </div>
      <div class="action-right">
        <select v-model="filterStatus" class="filter-select" @change="loadCoupons">
          <option value="">全部</option>
          <option value="1">启用</option>
          <option value="0">禁用</option>
        </select>
      </div>
    </div>

    <div v-if="isLoading" class="loading-state">
        <i class="fas fa-spinner fa-spin"></i>
        <span>加载中...</span>
      </div>

      <div v-else-if="coupons.length === 0" class="empty-state">
        <i class="fas fa-ticket-alt"></i>
        <p>暂无优惠券</p>
      </div>

      <div v-else class="coupon-list">
        <div v-for="coupon in coupons" :key="coupon.id" class="coupon-card" :class="{ disabled: coupon.status === 0 }">
          <div class="coupon-left">
            <div class="coupon-amount">
              <template v-if="coupon.type === 1">
                <span class="amount-num">{{ coupon.discountAmount }}</span>
                <span class="amount-unit">元</span>
              </template>
              <template v-else-if="coupon.type === 2">
                <span class="amount-num">{{ coupon.discountRate }}</span>
                <span class="amount-unit">折</span>
              </template>
              <template v-else>
                <span class="amount-num">{{ coupon.discountAmount }}</span>
                <span class="amount-unit">元</span>
              </template>
            </div>
            <div class="coupon-condition">{{ getConditionText(coupon) }}</div>
          </div>
          <div class="coupon-right">
            <div class="coupon-info">
              <div class="coupon-name">{{ coupon.name }}</div>
              <div class="coupon-meta">
                <span>{{ coupon.totalCount === -1 ? '不限量' : '共' + coupon.totalCount + '张' }}</span>
                <span class="meta-divider">|</span>
                <span>已领 {{ coupon.receivedCount || 0 }}</span>
                <span class="meta-divider">|</span>
                <span>已用 {{ coupon.usedCount || 0 }}</span>
              </div>
              <div class="coupon-date">{{ formatDate(coupon.startTime) }} ~ {{ formatDate(coupon.endTime) }}</div>
            </div>
            <div class="coupon-actions">
              <button class="action-btn edit-btn" @click="openEditDialog(coupon)" title="编辑"><i class="fas fa-edit"></i></button>
              <button class="action-btn" :class="coupon.status === 1 ? 'disable-btn' : 'enable-btn'" @click="toggleStatus(coupon)" :title="coupon.status === 1 ? '禁用' : '启用'">
                <i :class="coupon.status === 1 ? 'fas fa-ban' : 'fas fa-check'"></i>
              </button>
              <button class="action-btn delete-btn" @click="deleteCoupon(coupon)" title="删除"><i class="fas fa-trash"></i></button>
            </div>
          </div>
          <div class="coupon-status" :class="coupon.status === 1 ? 'active' : 'inactive'">
            {{ coupon.status === 1 ? '启用' : '禁用' }}
          </div>
        </div>
      </div>

    <div class="pagination" v-if="!isLoading && total > 0">
      <div class="pagination-left">
        <button class="page-btn" :disabled="currentPage === 1" @click="changePage(currentPage - 1)">
          <i class="fas fa-chevron-left"></i>
        </button>

        <div class="page-numbers">
          <button
            v-if="currentPage > 3"
            class="page-number-btn"
            @click="changePage(1)"
          >1</button>

          <span v-if="currentPage > 4" class="page-ellipsis">...</span>

          <button
            v-for="page in visiblePages"
            :key="page"
            class="page-number-btn"
            :class="{ active: page === currentPage }"
            @click="changePage(page)"
          >{{ page }}</button>

          <span v-if="currentPage < totalPages - 3" class="page-ellipsis">...</span>

          <button
            v-if="currentPage < totalPages - 2"
            class="page-number-btn"
            @click="changePage(totalPages)"
          >{{ totalPages }}</button>
        </div>

        <button class="page-btn" :disabled="currentPage === totalPages" @click="changePage(currentPage + 1)">
          <i class="fas fa-chevron-right"></i>
        </button>
      </div>

      <div class="pagination-right">
        <select v-model="pageSize" class="page-size-select" @change="loadCoupons">
          <option :value="10">10条/页</option>
          <option :value="20">20条/页</option>
          <option :value="50">50条/页</option>
        </select>
        <span class="total-info">共 {{ total }} 条优惠券</span>
      </div>
    </div>

    <div v-if="dialogVisible" class="modal-overlay" @click.self="dialogVisible = false">
      <div class="modal-content">
        <div class="modal-header">
          <h3>{{ isEdit ? '编辑优惠券' : '新建优惠券' }}</h3>
          <button class="modal-close" @click="dialogVisible = false">✕</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label>类型</label>
            <div class="radio-group">
              <label><input type="radio" v-model="formData.type" :value="1" /> 满减</label>
              <label><input type="radio" v-model="formData.type" :value="2" /> 折扣</label>
              <label><input type="radio" v-model="formData.type" :value="3" /> 无门槛</label>
            </div>
          </div>
          <template v-if="formData.type === 1">
            <div class="form-group">
              <label>选择方案</label>
              <div class="preset-options">
                <button
                  v-for="opt in fullReductionPresets"
                  :key="opt.key"
                  @click="selectPreset(opt)"
                  :class="{ active: isPresetActive(opt) }"
                  class="preset-btn"
                >
                  {{ opt.label }}
                </button>
              </div>
            </div>
            <div v-if="isCustom" class="custom-inputs">
              <div class="form-group">
                <label>满多少元</label>
                <input type="number" v-model="formData.minAmount" class="form-input" min="0" step="0.01" placeholder="请输入最低消费金额" />
              </div>
              <div class="form-group">
                <label>减多少元</label>
                <input type="number" v-model="formData.discountAmount" class="form-input" min="0" step="0.01" placeholder="请输入减免金额" />
              </div>
            </div>
          </template>
          <template v-if="formData.type === 2">
            <div class="form-group">
              <label>选择方案</label>
              <div class="preset-options">
                <button
                  v-for="opt in discountPresets"
                  :key="opt.key"
                  @click="selectPreset(opt)"
                  :class="{ active: isPresetActive(opt) }"
                  class="preset-btn"
                >
                  {{ opt.label }}
                </button>
              </div>
            </div>
            <div v-if="isCustom" class="custom-inputs">
              <div class="form-group">
                <label>折扣</label>
                <input type="number" v-model="formData.discountRate" class="form-input" min="0.1" max="9.9" step="0.1" placeholder="请输入折扣（如 8.5 表示 8.5 折）" />
              </div>
            </div>
          </template>
          <template v-if="formData.type === 3">
            <div class="form-group">
              <label>选择方案</label>
              <div class="preset-options">
                <button
                  v-for="opt in noThresholdPresets"
                  :key="opt.key"
                  @click="selectPreset(opt)"
                  :class="{ active: isPresetActive(opt) }"
                  class="preset-btn"
                >
                  {{ opt.label }}
                </button>
              </div>
            </div>
            <div v-if="isCustom" class="custom-inputs">
              <div class="form-group">
                <label>减多少元</label>
                <input type="number" v-model="formData.discountAmount" class="form-input" min="0" step="0.01" placeholder="请输入减免金额" />
              </div>
            </div>
          </template>
          <div class="form-group">
            <label>发行总量（-1 不限）</label>
            <input type="number" v-model="formData.totalCount" class="form-input" min="-1" placeholder="-1表示不限量" />
          </div>
          <div class="form-group">
            <label>每人限领（张）</label>
            <input type="number" v-model="formData.perUserLimit" class="form-input" min="1" placeholder="每人最多可领取的数量" />
          </div>
          <div class="form-group">
            <label>生效时间</label>
            <input type="datetime-local" v-model="formData.startTime" class="form-input" />
          </div>
          <div class="form-group">
            <label>结束时间</label>
            <input type="datetime-local" v-model="formData.endTime" class="form-input" />
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-cancel" @click="dialogVisible = false">取消</button>
          <button class="btn-confirm" :disabled="submitLoading" @click="handleSubmit">
            <i v-if="submitLoading" class="fas fa-spinner fa-spin"></i>
            {{ submitLoading ? '保存中...' : '保存' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import Message from '@/utils/message'
import { authAPI } from '@/api/authAPI'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()

interface Coupon {
  id: number
  name: string
  type: number
  minAmount: number
  discountAmount: number
  discountRate: number
  totalCount: number
  receivedCount: number
  usedCount: number
  perUserLimit: number
  startTime: string
  endTime: string
  status: number
}

const isLoading = ref(false)
const coupons = ref<Coupon[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const filterStatus = ref('')

const dialogVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)

const formData = ref({
  id: 0,
  name: '',
  type: 1,
  minAmount: 0,
  discountAmount: 0,
  discountRate: 0,
  totalCount: -1,
  perUserLimit: 1,
  startTime: '',
  endTime: ''
})

const selectedPreset = ref('')

const fullReductionPresets = [
  { key: '50-5', label: '满50减5', minAmount: 50, discountAmount: 5 },
  { key: '100-10', label: '满100减10', minAmount: 100, discountAmount: 10 },
  { key: '200-30', label: '满200减30', minAmount: 200, discountAmount: 30 },
  { key: '500-80', label: '满500减80', minAmount: 500, discountAmount: 80 },
  { key: 'custom', label: '自定义', custom: true }
]

const discountPresets = [
  { key: '9', label: '9折', discountRate: 9 },
  { key: '8.5', label: '8.5折', discountRate: 8.5 },
  { key: '8', label: '8折', discountRate: 8 },
  { key: '7', label: '7折', discountRate: 7 },
  { key: 'custom', label: '自定义', custom: true }
]

const noThresholdPresets = [
  { key: '3', label: '减3元', discountAmount: 3 },
  { key: '5', label: '减5元', discountAmount: 5 },
  { key: '10', label: '减10元', discountAmount: 10 },
  { key: '20', label: '减20元', discountAmount: 20 },
  { key: 'custom', label: '自定义', custom: true }
]

const currentPresets = computed(() => {
  switch (formData.value.type) {
    case 1:
      return fullReductionPresets
    case 2:
      return discountPresets
    case 3:
      return noThresholdPresets
    default:
      return []
  }
})

const isCustom = computed(() => selectedPreset.value === 'custom')

watch(() => formData.value.type, () => {
  selectedPreset.value = ''
})

const totalPages = computed(() => Math.ceil(total.value / pageSize.value))

const visiblePages = computed(() => {
  const pages: number[] = []
  const start = Math.max(1, currentPage.value - 2)
  const end = Math.min(totalPages.value, currentPage.value + 2)

  for (let i = start; i <= end; i++) {
    if (!pages.includes(i)) pages.push(i)
  }
  return pages
})

const loadCoupons = async () => {
  isLoading.value = true

  try {
    const params: any = {
      page: currentPage.value,
      pageSize: pageSize.value
    }

    if (filterStatus.value) {
      params.status = filterStatus.value
    }

    const response = await authAPI.getSellerCoupons(params)

    if (response.success) {
      const data = response.data || {}
      const rawCoupons = Array.isArray(data) ? data : (data.records || data.list || [])
      console.log('优惠券数据:', rawCoupons)

      coupons.value = rawCoupons.map((coupon: any) => {
        let typeNum = 1
        switch (coupon.type) {
          case 'FULL_REDUCTION':
            typeNum = 1
            break
          case 'DISCOUNT':
            typeNum = 2
            break
          case 'NO_THRESHOLD':
            typeNum = 3
            break
          default:
            typeNum = typeof coupon.type === 'number' ? coupon.type : 1
        }
        return {
          ...coupon,
          type: typeNum
        }
      })

      total.value = Array.isArray(data) ? rawCoupons.length : (data.total || rawCoupons.length || 0)
    } else {
      Message.error(response.message || '加载优惠券失败')
    }
  } catch (error: any) {
    Message.error(error.message || '加载优惠券失败，请重试')
  } finally {
    isLoading.value = false
  }
}

const changePage = (page: number) => {
  currentPage.value = page
  loadCoupons()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const getTypeText = (type: number): string => {
  switch (type) {
    case 1:
      return '满减'
    case 2:
      return '折扣'
    case 3:
      return '无门槛'
    default:
      return '未知'
  }
}

const getTypeClass = (type: number): string => {
  switch (type) {
    case 1:
      return 'type-full-reduction'
    case 2:
      return 'type-discount'
    case 3:
      return 'type-no-threshold'
    default:
      return ''
  }
}

const getConditionText = (coupon: Coupon): string => {
  switch (coupon.type) {
    case 1:
      return `满${coupon.minAmount}元减${coupon.discountAmount}元`
    case 2:
      return `${coupon.discountRate}折`
    case 3:
      return `减${coupon.discountAmount}元`
    default:
      return '-'
  }
}

const getStatusText = (status: number): string => {
  switch (status) {
    case 1:
      return '启用中'
    case 0:
      return '已禁用'
    default:
      return '未知'
  }
}

const getStatusClass = (status: number): string => {
  return status === 1 ? 'status-active' : 'status-disabled'
}

const formatDate = (dateStr: string): string => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  if (isNaN(date.getTime())) return dateStr
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const formatDateTimeForInput = (dateStr: string): string => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  if (isNaN(date.getTime())) return dateStr
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day}T${hours}:${minutes}`
}

const selectPreset = (opt: any) => {
  selectedPreset.value = opt.key
  if (!opt.custom) {
    if (opt.minAmount !== undefined) {
      formData.value.minAmount = opt.minAmount
    }
    if (opt.discountAmount !== undefined) {
      formData.value.discountAmount = opt.discountAmount
    }
    if (opt.discountRate !== undefined) {
      formData.value.discountRate = opt.discountRate
    }
  }
}

const isPresetActive = (opt: any): boolean => {
  if (selectedPreset.value === opt.key) return true
  if (selectedPreset.value === '') {
    if (opt.minAmount !== undefined && opt.discountAmount !== undefined) {
      return formData.value.minAmount === opt.minAmount &&
             formData.value.discountAmount === opt.discountAmount
    }
    if (opt.discountRate !== undefined) {
      return formData.value.discountRate === opt.discountRate
    }
    if (opt.discountAmount !== undefined && opt.minAmount === undefined) {
      return formData.value.discountAmount === opt.discountAmount
    }
  }
  return false
}

const resetForm = () => {
  formData.value = {
    id: 0,
    name: '',
    type: 1,
    minAmount: 0,
    discountAmount: 0,
    discountRate: 0,
    totalCount: -1,
    perUserLimit: 1,
    startTime: '',
    endTime: ''
  }
  selectedPreset.value = ''
}

const openCreateDialog = () => {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

const openEditDialog = (coupon: Coupon) => {
  isEdit.value = true

  formData.value = {
    id: coupon.id,
    name: coupon.name,
    type: coupon.type,
    minAmount: coupon.minAmount || 0,
    discountAmount: coupon.discountAmount || 0,
    discountRate: coupon.discountRate || 0,
    totalCount: coupon.totalCount,
    perUserLimit: coupon.perUserLimit || 1,
    startTime: formatDateTimeForInput(coupon.startTime || ''),
    endTime: formatDateTimeForInput(coupon.endTime || '')
  }
  dialogVisible.value = true
}

const validateForm = (): boolean => {
  if (!formData.value.totalCount && formData.value.totalCount !== 0 && formData.value.totalCount !== -1) {
    Message.error('请输入发行总量')
    return false
  }
  if (!formData.value.perUserLimit || formData.value.perUserLimit < 1) {
    Message.error('请输入每人限领数量')
    return false
  }
  if (!formData.value.startTime) {
    Message.error('请选择生效开始时间')
    return false
  }
  if (!formData.value.endTime) {
    Message.error('请选择生效结束时间')
    return false
  }
  if (formData.value.endTime <= formData.value.startTime) {
    Message.error('结束时间必须大于开始时间')
    return false
  }
  if (formData.value.type === 1) {
    if (!formData.value.minAmount && formData.value.minAmount !== 0) {
      Message.error('请输入最低消费金额')
      return false
    }
    if (!formData.value.discountAmount && formData.value.discountAmount !== 0) {
      Message.error('请输入减免金额')
      return false
    }
  } else if (formData.value.type === 2) {
    if (!formData.value.discountRate) {
      Message.error('请输入折扣')
      return false
    }
    if (formData.value.discountRate < 0.1 || formData.value.discountRate > 9.9) {
      Message.error('折扣范围必须在0.1~9.9之间')
      return false
    }
  } else if (formData.value.type === 3) {
    if (!formData.value.discountAmount && formData.value.discountAmount !== 0) {
      Message.error('请输入减免金额')
      return false
    }
  }
  return true
}

const formatDateTimeForSubmit = (dateStr: string): string => {
  if (!dateStr) return ''
  return dateStr + ':00'
}

const generateCouponName = () => {
  switch (formData.value.type) {
    case 1:
      return `满${formData.value.minAmount}减${formData.value.discountAmount}`
    case 2:
      return `${formData.value.discountRate}折优惠券`
    case 3:
      return `无门槛减${formData.value.discountAmount}元`
    default:
      return '优惠券'
  }
}

const handleSubmit = async () => {
  if (!validateForm()) return

  formData.value.name = generateCouponName()
  submitLoading.value = true

  let typeStr = 'FULL_REDUCTION'
  switch (formData.value.type) {
    case 1:
      typeStr = 'FULL_REDUCTION'
      break
    case 2:
      typeStr = 'DISCOUNT'
      break
    case 3:
      typeStr = 'NO_THRESHOLD'
      break
  }

  const submitData: any = {
    ...formData.value,
    type: typeStr,
    startTime: formatDateTimeForSubmit(formData.value.startTime),
    endTime: formatDateTimeForSubmit(formData.value.endTime)
  }

  try {
    let response

    if (isEdit.value) {
      response = await authAPI.updateSellerCoupon(submitData.id, submitData)
    } else {
      response = await authAPI.createSellerCoupon(submitData)
    }

    if (response.success) {
      Message.success(isEdit.value ? '编辑成功' : '创建成功')
      dialogVisible.value = false
      loadCoupons()
    } else {
      Message.error(response.message || '保存失败')
    }
  } catch (error: any) {
    Message.error(error.message || '保存失败，请重试')
  } finally {
    submitLoading.value = false
  }
}

const toggleStatus = async (coupon: Coupon) => {
  const newStatus = coupon.status === 1 ? 0 : 1
  const action = newStatus === 1 ? '启用' : '禁用'

  try {
    await Message.confirm(`确定要${action}「${coupon.name}」吗？`, `${action}确认`)

    const response = await authAPI.updateSellerCouponStatus(coupon.id, newStatus)

    if (response.success) {
      Message.success(`${action}成功`)
      loadCoupons()
    } else {
      Message.warning(response.message || `${action}失败`)
    }
  } catch {
  }
}

const deleteCoupon = async (coupon: Coupon) => {
  try {
    await Message.confirm('确定要删除优惠券「' + coupon.name + '」吗？删除后不可恢复。', '删除确认')

    const response = await authAPI.deleteSellerCoupon(coupon.id)

    if (response.success) {
      Message.success('删除成功')
      loadCoupons()
    } else {
      Message.error(response.message || '删除失败')
    }
  } catch {
  }
}

onMounted(() => {
  if (!authStore.validateSellerPermission()) return
  loadCoupons()
})
</script>

<style scoped>
@import url('@/static/css/seller/优惠券管理页.css');
</style>
