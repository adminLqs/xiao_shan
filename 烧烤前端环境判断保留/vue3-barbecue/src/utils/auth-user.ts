// utils/auth-user.ts
import { authAPI } from '@/api/auth'

export const useAuth = () => {
  // 判断是否在小程序环境
  const isMiniProgram = (): boolean => {
    const ua = window.navigator.userAgent.toLowerCase()
    return ua.indexOf('miniprogram') > -1 || !!window.__wxjs_environment
  }

  // 获取小程序code
  const getWxCode = (): Promise<string> => {
    return new Promise((resolve, reject) => {
      if (typeof wx === 'undefined' || !wx.miniProgram) {
        reject('不在小程序环境')
        return
      }
      
      wx.miniProgram.getEnv((res: any) => {
        if (res.miniprogram) {
          wx.login({
            success: (loginRes) => resolve(loginRes.code),
            fail: () => reject('获取微信code失败')
          })
        } else {
          reject('不在小程序环境')
        }
      })
    })
  }

  // 生成设备ID
  const generateDeviceId = (): string => {
    return 'DEV_' + Date.now() + '_' + Math.random().toString(36).substring(2, 10)
  }

  // 获取设备ID
  const getDeviceId = (): string | null => {
    return localStorage.getItem('deviceId')
  }


  // 获取用户ID并存储
  const getUserId = async (): Promise<number | null> => {
    try {
      let param = null;
      
      // 1. 如果是小程序环境，优先用code
      if (isMiniProgram()) {
        try {
          const code = await getWxCode()
          param = code
          console.log('获取到小程序code:', code)
        } catch (error) {
          console.log('获取code失败，降级使用deviceId')
        }
      }

      // 2. 如果没有code，用deviceId
      if (!param) {
        let deviceId = localStorage.getItem('deviceId')
        if (!deviceId) {
          deviceId = generateDeviceId()
          localStorage.setItem('deviceId', deviceId)
          console.log("生成新设备标识符:", deviceId)
        }
        param = deviceId
      }

      // 3. 调用统一登录接口（直接传字符串）
      const response = await authAPI.login(param)
      const data = response.data || response

      // 4. 从响应中获取userId并存储
      if (data.success && data.userId) {
        localStorage.setItem('userId', data.userId)
        console.log('用户ID存储成功:', data.userId)
        return data.userId
      }

      return null
    } catch (error) {
      console.error('获取用户ID失败:', error)
      return null
    }
  }

  return {
    getUserId,
    getDeviceId,
    isMiniProgram
  }
}