import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authAPI } from '@/api/authAPI'
import Message from '@/utils/message'
import { useRouter } from 'vue-router'

export const useAuthStore = defineStore('auth', () => {
  // 路由实例
  const router = useRouter()

  // ========== State - 只存核心标识 ==========
  const isLoggedIn = ref(false) // 是否登录
  const role = ref('') // 当前活跃角色
  const activeRole = ref('') // 当前活跃角色（同role，冗余字段）
  const roles = ref<string[]>([]) // 用户拥有的所有角色
  const status = ref(1) // 账号状态: 0-禁用, 1-启用
  const userId = ref<number | null>(null) // 账号ID
  const token = ref('') // JWT token

  // ========== Getters - 权限判断 ==========
  const isUser = computed(() => role.value === 'ROLE_USER')
  const isSeller = computed(() => role.value === 'ROLE_SELLER')
  const isAdmin = computed(() => role.value === 'ROLE_ADMIN')
  const isActive = computed(() => status.value === 1)

  const hasRole = computed(() => (targetRole: string) => {
    if (!isLoggedIn.value) return false
    return roles.value.includes(targetRole)
  })

  // ========== Actions ==========

  /* 加载账号权限状态 */
  async function checkAndUpdate() {
    try {
      const response = await authAPI.getAccountProfile()

      if (response.success && response.data?.accountProfile) {
        const userRole = response.data.accountProfile.role
        const userIdValue = response.data.accountProfile.id
        const accountStatus = response.data.accountProfile.status
        const userRoles = response.data.accountProfile.roles || []

        if (['ROLE_USER', 'ROLE_SELLER', 'ROLE_ADMIN'].includes(userRole)) {
          isLoggedIn.value = true
          role.value = userRole
          activeRole.value = userRole
          roles.value = userRoles
          userId.value = userIdValue
        } else {
          isLoggedIn.value = false
          role.value = ''
          activeRole.value = ''
          roles.value = []
          userId.value = null
        }

        status.value = accountStatus
        return;
      }

      clear()
    } catch (error) {
      clear()
    }
  }

  // ========== 第一种：通用用户权限验证 ==========
  function validateUserPermission(): boolean {
    // 检查是否登录
    if (!isLoggedIn.value) {
      Message.error('请先登录', {
        duration: 1500,
        onClose: () => router!.replace({ name: 'Login' })
      })
      return false
    }

    // 检查角色权限
    if (!['ROLE_USER', 'ROLE_SELLER', 'ROLE_ADMIN'].includes(role.value)) {
      Message.error('您没有权限访问此功能', {
        duration: 2000,
        onClose: () => router!.replace({ name: 'UserDashboard' })
      })
      return false
    }

    // 检查账号状态
    if (status.value === 0) {
      Message.error('账号已被封禁，请联系客服', {
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
      Message.error('请先登录商家账号', {
        duration: 1500,
        onClose: () => router!.replace({ name: 'Login' })
      })
    return false
    }

    if (!['ROLE_SELLER', 'ROLE_ADMIN'].includes(role.value)) {
      Message.error('此功能仅限商家访问', {
        duration: 2000,
        onClose: () => router.replace({name: 'UserDashboard'})
      })
    return false
    }

    if (status.value === 0) {
      Message.error('商家账号已被封禁，请联系客服', {
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
      Message.error('请先登录管理员账号', {
        duration: 1500,
        onClose: () => router!.push({ name: 'Login' })
      })
      return false
    }

    if (role.value !== 'ROLE_ADMIN') {
      Message.error('此功能仅限管理员访问', {
        duration: 2000,
        onClose: () => router.replace({name: 'UserDashboard'})
      })
      return false
    }

    if (status.value === 0) {
      Message.error('管理员账号已被封禁，请联系超级管理员', {
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
      Message.error('账号已被封禁，请联系客服', {
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
    activeRole.value = ''
    roles.value = []
    status.value = 1
    userId.value = null
    token.value = ''
  }

  // 登出
  async function logout() {
    try {
      await authAPI.logout()
    } catch (error) {
      // 忽略登出API错误
    }
    clear()
  }

  // 初始化
  async function init() {
    await checkAndUpdate()
  }

  async function switchRole(newRole: string) {
    const res = await authAPI.switchRole(newRole)
    if (res.success && res.data) {
      role.value = newRole
      activeRole.value = newRole
      token.value = res.data.token || ''
      await checkAndUpdate()
      return true
    }
    return false
  }

  return {
    // state
    isLoggedIn,
    role,
    activeRole,
    roles,
    status,
    userId,
    token,

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
    clear,
    logout,
    switchRole
  }
})
