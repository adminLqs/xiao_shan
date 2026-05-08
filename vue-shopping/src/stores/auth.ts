import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authAPI } from '@/api/authAPI'
import { showToast, showConfirmDialog } from 'vant'     // Vant组件
import { useRouter } from 'vue-router'

export const useAuthStore = defineStore('auth', () => {
  // 路由实例
  const router = useRouter()

  // ========== State - 只存核心标识 ==========
  const isLoggedIn = ref(false) // 是否登录
  const role = ref('') // 角色: ROLE_USER / ROLE_SELLER / ROLE_ADMIN
  const status = ref(1) // 账号状态: 0-禁用, 1-启用
  
  // ========== Getters - 权限判断 ==========
  const isUser = computed(() => role.value === 'ROLE_USER')
  const isSeller = computed(() => role.value === 'ROLE_SELLER')
  const isAdmin = computed(() => role.value === 'ROLE_ADMIN')
  const isActive = computed(() => status.value === 1)  // 账号是否启用
  
  // 是否有任一角色
  const hasRole = computed(() => (roles: string[]) => {
    if (!isLoggedIn.value) return false
    return roles.includes(role.value)
  })
  
  // ========== Actions ==========

  /* 加载账号权限状态 */
  async function checkAndUpdate() {
    try {
      // 请求账号信息
      const response = await authAPI.getAccountProfile()
      
      if (response.success && response.data) {
        // 角色权限
        const userRole = response.data.role
        // 账号状态
        const accountStatus = response.data.status
        
        // 角色有效才设置登录状态
        if (['ROLE_USER', 'ROLE_SELLER', 'ROLE_ADMIN'].includes(userRole)) {
          isLoggedIn.value = true // 是否登录
          role.value = userRole // 角色权限
        } else {
          // 角色无效，保持未登录
          isLoggedIn.value = false
          role.value = ''
        }

        // 无论角色是否有效，都记录状态（供组件判断封禁）
        status.value = accountStatus

        return;
      }

      // 未登录或无效，清空状态
      clear()
    } catch (error) {
      clear()
    }
  }

  // ========== 第一种：通用用户权限验证 ==========
  function validateUserPermission(): boolean {
    // 检查是否登录
    if (!isLoggedIn.value) {
      showToast({
        message: '请先登录',
        type: 'fail',
        duration: 1500,
        onClose: () => router!.replace({ name: 'Login' })
      })
      return false
    }
    
    // 检查角色权限
    if (!['ROLE_USER', 'ROLE_SELLER', 'ROLE_ADMIN'].includes(role.value)) {
      showToast({
        message: '您没有权限访问此功能',
        type: 'fail',
        duration: 2000,
        onClose: () => router!.replace({ name: 'UserDashboard' })
      })
      return false
    }
    
    // 检查账号状态
    if (status.value === 0) {
      showToast({
        message: '账号已被封禁，请联系客服',
        type: 'fail',
        duration: 1500,
        onClose: () => router!.replace({ name: 'UserDashboard' })
      })
      return false
    }
    
    return true
  }
  
  // ========== 第二种：商家权限验证 ==========
  function validateSellerPermission(): boolean {
    if (!isLoggedIn.value) {
      showToast({
        message: '请先登录商家账号',
        type: 'fail',
        duration: 1500,
        onClose: () => router!.replace({ name: 'Login' })
      })
    return false
    }
    
    if (!['ROLE_SELLER', 'ROLE_ADMIN'].includes(role.value)) {
      showToast({
        message: '此功能仅限商家访问',
        type: 'fail',
        duration: 2000,
        onClose: () => router.replace({name: 'UserDashboard'})
      })
    return false
    }
    
    if (status.value === 0) {
      showToast({
        message: '商家账号已被封禁，请联系客服',
        type: 'fail',
        duration: 1500,
        onClose: () => router!.replace({ name: 'UserDashboard' })
      })
      return false
    }
    
    return true
  }
  
  // ========== 第三种：管理员权限验证 ==========
  function validateAdminPermission(): boolean {
    if (!isLoggedIn.value) {
      showToast({
        message: '请先登录管理员账号',
        type: 'fail',
        duration: 1500,
        onClose: () => router!.push({ name: 'Login' })
      })
      return false
    }
    
    if (role.value !== 'ROLE_ADMIN') {
      showToast({
        message: '此功能仅限管理员访问',
        type: 'fail',
        duration: 2000,
        onClose: () => router.replace({name: 'UserDashboard'})
      })
      return false
    }
    
    if (status.value === 0) {
      showToast({
        message: '管理员账号已被封禁，请联系超级管理员',
        type: 'fail',
        duration: 1500,
        onClose: () => router!.push({ name: 'UserDashboard' })
      })
      return false
    }
    
    return true
  }
  
  // ========== 第四种：纯账号状态验证 ==========
  function validateAccountStatus(): boolean {
    // 只验证账号是否被封禁
    if (status.value === 0) {
      showToast({
        message: '账号已被封禁，请联系客服',
        type: 'fail',
        duration: 1500,
        onClose: () => router!.push({ name: 'UserDashboard' })
      })
      return false
    }
    
    return true
  }
  
  
  // 清理权限状态
  function clear() {
    isLoggedIn.value = false
    role.value = ''
    status.value = 1
  }
  
  // 初始化
  async function init() {
    await checkAndUpdate()
  }
  
  return {
    // state
    isLoggedIn,
    role,
    status,
    
    // getters
    isUser,
    isSeller,
    isAdmin,
    hasRole,
    isActive,

    // actions - 四种验证方法
    validateUserPermission,    // 方法1：用户权限（所有角色）
    validateSellerPermission,  // 方法2：商家权限（商家+管理员）
    validateAdminPermission,   // 方法3：管理员权限（仅管理员）
    validateAccountStatus,     // 方法4：账号状态（不检查角色）
    
    // actions
    checkAndUpdate,
    clear
  }
})