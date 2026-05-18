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
  import { Toast, Dialog } from '@/utils/vant'
  import { useUserStore } from '@/stores/auth'
  import { authAPI } from '@/api/authAPI'

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
        Toast.success('用户ID已复制')
      } catch {
        Toast.fail('复制失败')
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
      Toast.fail('用户未登录')
      return
    }
    userId.value = uid
    
    try {
      const res = await authAPI.getUserInfo(uid)
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
      Toast.fail('请输入正确的手机号')
      return
    }

    const uid = userStore.userId
    if (!uid) {
      Toast.fail('用户未登录')
      return
    }

    try {
      await Dialog.confirm(`确认绑定手机号 ${bindPhoneInput.value}？`)
    } catch {
      return
    }

    submitting.value = true
    Toast.loading('绑定中...')

    try {
      const res = await authAPI.bindPhone(uid, bindPhoneInput.value)
      
      if (res.success) {
        Toast.success(res.message || '绑定成功')
        phone.value = bindPhoneInput.value
        closeBindModal()
      } else {
        Toast.fail(res.message || '绑定失败')
      }
    } catch (error) {
      console.error('绑定失败:', error)
      Toast.fail('网络错误，请重试')
    } finally {
      submitting.value = false
    }
  }

  /**
   * 提交找回账号
   */
  const submitFind = async () => {
    if (!isValidFindPhone.value) {
      Toast.fail('请输入正确的手机号')
      return
    }

    finding.value = true
    foundUserId.value = null
    Toast.loading('查询中...')

    try {
      const res = await authAPI.findUserIdByPhone(findPhoneInput.value)
      
      if (res.success && res.data && res.data.userId) {
        const newUserId = res.data.userId
        
        // 更新本地存储
        localStorage.setItem('userId', String(newUserId))
        
        Toast.success('账号找回成功，即将刷新页面')
        closeFindModal()
        
        // 延迟刷新，让用户看到成功提示
        setTimeout(() => {
          window.location.reload()
        }, 500)
      } else {
        Toast.fail(res.message || '未找到该手机号绑定的账号')
      }
    } catch (error) {
      console.error('找回失败:', error)
      Toast.fail('网络错误，请重试')
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
 @import url('@/static/css/user/账号页.css');
</style>  