// stores/user.ts
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authAPI } from '../api/authAPI'

export const useUserStore = defineStore('user', () => {
  // ========== State ==========
  const userId = ref<number | null>(null)
  const deviceId = ref<string | null>(null)
  const loading = ref(false)

  // ========== Getters ==========
  const isLoggedIn = computed(() => userId.value !== null)

  // ========== 私有方法 ==========

  /**
   * 生成设备标识符
   */
  const generateDeviceId = (): string => {
    return 'DEV_' + Date.now() + '_' + Math.random().toString(36).substring(2, 10)
  }

  // ========== Actions ==========

  /**
   * 初始化用户信息
   * 优先从本地存储读取用户ID，若不存在则生成设备标识并调用后端接口获取。
   *
   * @returns 用户ID，初始化失败返回 null
   */
  const initUser = async (): Promise<number | null> => {
    if (userId.value !== null) {
      return userId.value
    }

    loading.value = true

    try {
      const storedUserId = localStorage.getItem('userId')
      if (storedUserId) {
        userId.value = Number(storedUserId)
        return userId.value
      }

      let deviceIdValue = localStorage.getItem('deviceId')
      if (!deviceIdValue) {
        deviceIdValue = generateDeviceId()
        localStorage.setItem('deviceId', deviceIdValue)
      }
      deviceId.value = deviceIdValue

      const response = await authAPI.getUserId(deviceIdValue)
      
      if (response.success && response.data && response.data.userId) {
        userId.value = response.data.userId
        localStorage.setItem('userId', String(response.data.userId))
        return userId.value
      }

      return null
    } catch (error) {
      console.error('初始化用户失败:', error)
      return null
    } finally {
      loading.value = false
    }
  }

  /**
   * 清除用户状态
   */
  const clearUser = () => {
    userId.value = null
    deviceId.value = null
    localStorage.removeItem('userId')
  }

  /**
   * 获取当前用户ID
   *
   * @returns 用户ID，不存在返回 null
   */
  const getUserId = async (): Promise<number | null> => {
    if (userId.value !== null) {
      return userId.value
    }
    return await initUser()
  }

  return {
    // state
    userId,
    deviceId,
    loading,
    // getters
    isLoggedIn,
    // actions
    initUser,
    clearUser,
    getUserId
  }
})