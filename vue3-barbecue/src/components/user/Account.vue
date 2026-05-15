<template>
  <div class="account-page">
    <div class="page-header">
      <h2>账号管理</h2>
    </div>

    <div class="account-info">
      <!-- 用户ID显示 -->
      <div class="info-card">
        <div class="info-label">用户ID</div>
        <div class="info-value">{{ userId || '未获取到' }}</div>
      </div>

      <!-- 手机号绑定/更换 -->
      <div class="info-card" @click="openBindModal">
        <div class="info-label">手机号</div>
        <div class="info-value">
          <span v-if="phone">{{ maskPhone(phone) }}</span>
          <span v-else class="unbind">未绑定</span>
          <span class="arrow">›</span>
        </div>
      </div>

      <!-- 找回账号 -->
      <div class="info-card" @click="openFindModal">
        <div class="info-label">找回账号</div>
        <div class="info-value">
          <span class="find-text">通过手机号找回用户ID</span>
          <span class="arrow">›</span>
        </div>
      </div>

      <!-- 提示说明 -->
      <div class="info-tip">
        <span>💡 绑定手机号后，可通过手机号找回账号</span>
      </div>
    </div>

    <!-- 手机号绑定弹窗 -->
    <div v-if="showBindModal" class="modal-mask" @click.self="closeBindModal">
      <div class="modal-container">
        <div class="modal-header">
          <h3>{{ phone ? '更换手机号' : '绑定手机号' }}</h3>
          <button class="close-btn" @click="closeBindModal">✕</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label>手机号 <span class="required">*</span></label>
            <input 
              type="tel" 
              v-model="bindPhoneInput" 
              placeholder="请输入手机号"
              maxlength="11"
              @input="validatePhoneInput"
            />
            <span v-if="phoneError" class="error-tip">{{ phoneError }}</span>
          </div>
        </div>
        <div class="modal-footer">
          <button class="cancel-btn" @click="closeBindModal">取消</button>
          <button class="confirm-btn" @click="submitBind" :disabled="submitting || !isValidPhone">
            {{ submitting ? '绑定中...' : '确认绑定' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 找回账号弹窗 -->
    <div v-if="showFindModal" class="modal-mask" @click.self="closeFindModal">
      <div class="modal-container">
        <div class="modal-header">
          <h3>找回账号</h3>
          <button class="close-btn" @click="closeFindModal">✕</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label>手机号 <span class="required">*</span></label>
            <input 
              type="tel" 
              v-model="findPhoneInput" 
              placeholder="请输入绑定的手机号"
              maxlength="11"
              @input="validateFindPhoneInput"
            />
            <span v-if="findPhoneError" class="error-tip">{{ findPhoneError }}</span>
          </div>
          <div class="find-result" v-if="foundUserId">
            <div class="result-card">
              <span class="result-label">您的用户ID：{{ foundUserId }}</span>
              <button class="copy-id-btn" @click="copyUserId">复制</button>
            </div>
            <p class="result-tip">请妥善保管用户ID，用于账号找回</p>
          </div>
        </div>
        <div class="modal-footer">
          <button class="cancel-btn" @click="closeFindModal">取消</button>
          <button class="confirm-btn" @click="submitFind" :disabled="finding || !isValidFindPhone">
            {{ finding ? '查询中...' : '找回账号' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, computed, onMounted } from 'vue'
import { showToast } from 'vant'
import { useUserStore } from '@/stores/auth'
import { authAPI } from '@/api/authAPI'
import 'vant/es/toast/style'

const userStore = useUserStore()

// ==================== 状态 ====================
const userId = ref<number | null>(null)
const phone = ref('')

// 绑定弹窗
const showBindModal = ref(false)
const submitting = ref(false)
const bindPhoneInput = ref('')
const phoneError = ref('')

// 找回弹窗
const showFindModal = ref(false)
const finding = ref(false)
const findPhoneInput = ref('')
const findPhoneError = ref('')
const foundUserId = ref<number | null>(null)

// ==================== 计算属性 ====================

/** 手机号正则表达式 */
const PHONE_REGEX = /^1[3-9]\d{9}$/

/** 验证手机号是否有效 */
const isValidPhone = computed(() => PHONE_REGEX.test(bindPhoneInput.value))

/** 找回手机号是否有效 */
const isValidFindPhone = computed(() => PHONE_REGEX.test(findPhoneInput.value))

// ==================== 工具方法 ====================

/**
 * 格式化手机号（脱敏）
 */
const maskPhone = (phoneValue: string): string => {
  if (!phoneValue || phoneValue.length < 11) return phoneValue
  return phoneValue.substring(0, 3) + '****' + phoneValue.substring(7)
}

/**
 * 验证手机号输入
 */
const validatePhoneInput = () => {
  if (bindPhoneInput.value && !PHONE_REGEX.test(bindPhoneInput.value)) {
    phoneError.value = '请输入正确的手机号'
  } else {
    phoneError.value = ''
  }
}

/**
 * 验证找回手机号输入
 */
const validateFindPhoneInput = () => {
  if (findPhoneInput.value && !PHONE_REGEX.test(findPhoneInput.value)) {
    findPhoneError.value = '请输入正确的手机号'
  } else {
    findPhoneError.value = ''
  }
}

/**
 * 复制用户ID
 */
const copyUserId = async () => {
  if (foundUserId.value) {
    try {
      await navigator.clipboard.writeText(String(foundUserId.value))
      showToast({ message: '用户ID已复制', type: 'success' })
    } catch {
      showToast({ message: '复制失败', type: 'fail' })
    }
  }
}

// ==================== API 调用 ====================

/**
 * 加载用户信息
 */
const loadUserInfo = async () => {
  const uid = userStore.userId
  if (!uid) {
    showToast({ message: '用户未登录', type: 'fail' })
    return
  }
  userId.value = uid
  
  try {
    const res = await authAPI.getUserInfo(uid)
    // 响应拦截器已返回 response.data，所以 res 直接是后端返回的数据
    if (res.success && res.data) {
      phone.value = res.data.phone || ''
    }
  } catch (error) {
    console.error('获取用户信息失败:', error)
  }
}

/**
 * 提交绑定手机号
 */
const submitBind = async () => {
  if (!isValidPhone.value) {
    showToast({ message: '请输入正确的手机号', type: 'fail' })
    return
  }

  const uid = userStore.userId
  if (!uid) {
    showToast({ message: '用户未登录', type: 'fail' })
    return
  }

  submitting.value = true

  try {
    const res = await authAPI.bindPhone(uid, bindPhoneInput.value)
    
    if (res.success) {
      showToast({ message: res.message || '绑定成功', type: 'success' })
      phone.value = bindPhoneInput.value
      closeBindModal()
    } else {
      showToast({ message: res.message || '绑定失败', type: 'fail' })
    }
  } catch (error) {
    console.error('绑定失败:', error)
    showToast({ message: '网络错误，请重试', type: 'fail' })
  } finally {
    submitting.value = false
  }
}

/**
 * 提交找回账号
 */
const submitFind = async () => {
  if (!isValidFindPhone.value) {
    showToast({ message: '请输入正确的手机号', type: 'fail' })
    return
  }

  finding.value = true
  foundUserId.value = null

  try {
    const res = await authAPI.findUserIdByPhone(findPhoneInput.value)
    
    if (res.success && res.data) {
      foundUserId.value = res.data
      showToast({ message: '查询成功', type: 'success' })
    } else {
      showToast({ message: res.message || '未找到该手机号绑定的账号', type: 'fail' })
    }
  } catch (error) {
    console.error('找回失败:', error)
    showToast({ message: '网络错误，请重试', type: 'fail' })
  } finally {
    finding.value = false
  }
}

// ==================== 弹窗控制 ====================

const openBindModal = () => {
  bindPhoneInput.value = phone.value || ''
  phoneError.value = ''
  showBindModal.value = true
}

const closeBindModal = () => {
  showBindModal.value = false
  bindPhoneInput.value = ''
  phoneError.value = ''
}

const openFindModal = () => {
  findPhoneInput.value = ''
  findPhoneError.value = ''
  foundUserId.value = null
  showFindModal.value = true
}

const closeFindModal = () => {
  showFindModal.value = false
  findPhoneInput.value = ''
  findPhoneError.value = ''
  foundUserId.value = null
}

// ==================== 生命周期 ====================
onMounted(() => {
  loadUserInfo()
})
</script>

<style scoped>
.account-page {
  min-height: 100%;
  background-color: #fffaf2;
  padding: 20px;
}

.page-header h2 {
  font-size: 24px;
  font-weight: 600;
  color: #1a1a2e;
  margin: 0 0 20px 0;
  position: relative;
  padding-left: 12px;
}

.page-header h2::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 24px;
  background: linear-gradient(135deg, #e65100, #ff8a3c);
  border-radius: 2px;
}

.info-card {
  background: white;
  border-radius: 16px;
  padding: 16px 20px;
  margin-bottom: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid #f0e4d4;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.info-card:hover {
  background-color: #fff5e6;
  border-color: #e65100;
  transform: translateX(2px);
}

.info-label {
  font-size: 14px;
  font-weight: 500;
  color: #b87c4b;
}

.info-value {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #3d2a1a;
}

.info-value .unbind {
  color: #ff9800;
}

.info-value .find-text {
  color: #e65100;
}

.arrow {
  color: #c9a87c;
  font-size: 16px;
}

.info-tip {
  margin-top: 20px;
  padding: 12px 16px;
  background: linear-gradient(135deg, #fff5e6, #fffaf2);
  border-radius: 12px;
  font-size: 12px;
  color: #e65100;
  text-align: center;
  border: 1px solid #f0e4d4;
}

/* 找回结果样式 */
.find-result {
  margin-top: 16px;
  padding: 12px;
  background: #e8f5e9;
  border-radius: 12px;
  border: 1px solid #c8e6c9;
}

.result-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
}

.result-label {
  font-size: 13px;
  color: #2e7d32;
}

.result-value {
  font-size: 16px;
  font-weight: bold;
  color: #e65100;
  font-family: monospace;
}

.copy-id-btn {
  padding: 4px 12px;
  background: white;
  border: 1px solid #c8e6c9;
  border-radius: 6px;
  font-size: 12px;
  color: #2e7d32;
  cursor: pointer;
  transition: all 0.2s;
}

.copy-id-btn:hover {
  background: #e8f5e9;
}

.result-tip {
  font-size: 11px;
  color: #666;
  margin: 8px 0 0 0;
}

/* 弹窗样式 */
.modal-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-container {
  background: white;
  border-radius: 20px;
  width: 90%;
  max-width: 400px;
  overflow: hidden;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.2);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #f0e4d4;
  background: linear-gradient(135deg, #fffaf2, #fef5e7);
}

.modal-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #1a1a2e;
}

.close-btn {
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  color: #b87c4b;
  width: 32px;
  height: 32px;
  border-radius: 8px;
}

.close-btn:hover {
  background: #fff5e6;
  color: #e65100;
}

.modal-body {
  padding: 20px;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #b87c4b;
}

.form-group .required {
  color: #f44336;
}

.form-group input {
  width: 100%;
  padding: 12px;
  border: 1px solid #f0e4d4;
  border-radius: 12px;
  font-size: 14px;
  box-sizing: border-box;
  transition: all 0.2s;
}

.form-group input:focus {
  outline: none;
  border-color: #e65100;
  box-shadow: 0 0 0 3px rgba(230, 81, 0, 0.1);
}

.error-tip {
  display: block;
  margin-top: 6px;
  font-size: 12px;
  color: #f44336;
}

.modal-footer {
  display: flex;
  gap: 12px;
  padding: 16px 20px;
  border-top: 1px solid #f0e4d4;
  background: #fef7e9;
}

.cancel-btn,
.confirm-btn {
  flex: 1;
  padding: 12px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  border: none;
  transition: all 0.2s;
}

.cancel-btn {
  background: white;
  color: #b87c4b;
  border: 1px solid #f0e4d4;
}

.cancel-btn:hover {
  background: #fff5e6;
  border-color: #e65100;
  color: #e65100;
}

.confirm-btn {
  background: linear-gradient(135deg, #e65100, #ff8a3c);
  color: white;
  box-shadow: 0 2px 4px rgba(230, 81, 0, 0.2);
}

.confirm-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(230, 81, 0, 0.25);
}

.confirm-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>