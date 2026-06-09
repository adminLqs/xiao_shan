<template>
  <div class="login-page">
    <div class="brand-area">
      <div class="brand-icon">🛍️</div>
      <h1 class="brand-name">云杉购</h1>
      <p class="brand-slogan">精品生活 · 从这里开始</p>
    </div>

    <div class="form-area">
      <div v-if="checkingLogin" class="checking-overlay">
        <i class="fas fa-spinner fa-spin"></i>
        <span>正在检查登录状态...</span>
      </div>

      <div v-else-if="existingUser" class="existing-user-card">
        <img :src="existingUser.avatar || defaultAvatar" class="existing-avatar" />
        <h3>检测到已登录账号</h3>
        <p class="existing-username">{{ existingUser.nickname || existingUser.account }}</p>
        <p class="existing-tip">是否继续使用当前账号？</p>
        <div class="existing-actions">
          <button class="btn continue-btn" @click="continueWithAccount">
            <i class="fas fa-arrow-right"></i> 继续使用
          </button>
          <button class="btn switch-btn" @click="switchAccount">
            <i class="fas fa-exchange-alt"></i> 切换账号
          </button>
        </div>
      </div>

      <div v-else>
        <div class="tabs">
          <div
            class="tab"
            :class="{ active: activeTab === 'login' }"
            @click="switchTab('login')"
          >
            登录
          </div>
          <div
            class="tab"
            :class="{ active: activeTab === 'register' }"
            @click="switchTab('register')"
          >
            注册
          </div>
        </div>

        <form class="login-form" v-if="activeTab === 'login'">
          <div class="input-container">
            <i class="fas fa-user input-icon"></i>
            <input
              type="text"
              name="loginAccount"
              placeholder="请输入账号"
              v-model="loginData.account"
              required
              @focus="handleFocus"
              @blur="handleBlur"
            >
          </div>

          <div class="input-container password-container">
            <i class="fas fa-lock input-icon"></i>
            <input
              :type="showLoginPassword ? 'text' : 'password'"
              name="loginPassword"
              placeholder="请输入密码"
              v-model="loginData.password"
              required
              @focus="handleFocus"
              @blur="handleBlur"
            >
            <i
              class="fas toggle-password"
              :class="showLoginPassword ? 'fa-eye-slash' : 'fa-eye'"
              @click="togglePassword('login')"
            ></i>
          </div>

          <button
            type="button"
            class="btn login-btn"
            @click="handleLogin"
            :disabled="isLoggingIn"
          >
            <i v-if="isLoggingIn" class="fas fa-spinner fa-spin"></i>
            {{ isLoggingIn ? '登录中...' : '登 录' }}
          </button>

          <div class="register-link">
            没有账号？<a @click="switchTab('register')">立即注册</a>
          </div>

          <div class="divider">
            <span class="divider-line"></span>
            <span class="divider-text">或使用以下方式登录</span>
            <span class="divider-line"></span>
          </div>

          <div class="social-login">
            <button type="button" class="social-btn wechat-btn">
              <img src="@/static/images/icons/wechat.svg" class="social-icon" />
              <span>微信</span>
            </button>
            <button type="button" class="social-btn alipay-btn">
              <img src="@/static/images/icons/alipay.svg" class="social-icon" />
              <span>支付宝</span>
            </button>
            <button type="button" class="social-btn phone-btn">
              <i class="fas fa-mobile-alt"></i>
              <span>手机号</span>
            </button>
          </div>
        </form>

        <form class="register-form" v-if="activeTab === 'register'">
          <div class="input-container">
            <i class="fas fa-user input-icon"></i>
            <input
              type="text"
              name="registerAccount"
              placeholder="设置用户名"
              v-model="registerData.account"
              required
              @focus="handleFocus"
              @blur="handleBlur"
            >
          </div>

          <div class="input-container password-container">
            <i class="fas fa-lock input-icon"></i>
            <input
              :type="showRegisterPassword ? 'text' : 'password'"
              name="registerPassword"
              placeholder="设置密码"
              v-model="registerData.password"
              required
              @focus="handleFocus"
              @blur="handleBlur"
            >
            <i
              class="fas toggle-password"
              :class="showRegisterPassword ? 'fa-eye-slash' : 'fa-eye'"
              @click="togglePassword('register')"
            ></i>
          </div>

          <div class="input-container password-container">
            <i class="fas fa-lock input-icon"></i>
            <input
              :type="showRegisterConfirm ? 'text' : 'password'"
              name="registerConfirmPassword"
              placeholder="确认密码"
              v-model="registerData.confirmPassword"
              required
              @focus="handleFocus"
              @blur="handleBlur"
            >
            <i
              class="fas toggle-password"
              :class="showRegisterConfirm ? 'fa-eye-slash' : 'fa-eye'"
              @click="togglePassword('registerConfirm')"
            ></i>
          </div>

          <button
            type="button"
            class="btn login-btn"
            @click="handleRegister"
            :disabled="isRegistering"
          >
            <i v-if="isRegistering" class="fas fa-spinner fa-spin"></i>
            {{ isRegistering ? '注册中...' : '注 册' }}
          </button>

          <div class="register-link">
            已有账号？<a @click="switchTab('login')">立即登录</a>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { authAPI } from '@/api/authAPI'
import Message from '@/utils/message'
import { useAuthStore } from '@/stores/auth'
import defaultAvatar from '@/static/images/user-avatar.jpg'

const router = useRouter()
const authStore = useAuthStore()

const activeTab = ref('login')
const checkingLogin = ref(true)
const existingUser = ref<{ avatar: string; nickname: string; account: string } | null>(null)
const showLoginPassword = ref(false)
const showRegisterPassword = ref(false)
const showRegisterConfirm = ref(false)
const isLoggingIn = ref(false)
const isRegistering = ref(false)

const loginData = ref({
    account: '',
    password: ''
})

const registerData = ref({
    account: '',
    password: '',
    confirmPassword: ''
})

const switchTab = (tab: string) => {
    activeTab.value = tab
}

const togglePassword = (type: string) => {
    switch(type) {
        case 'login':
            showLoginPassword.value = !showLoginPassword.value
            break
        case 'register':
            showRegisterPassword.value = !showRegisterPassword.value
            break
        case 'registerConfirm':
            showRegisterConfirm.value = !showRegisterConfirm.value
            break
    }
}

const handleLogin = async () => {
    const { account, password } = loginData.value

    if (!account || !password) {
        Message.error('用户名和密码不能为空')
        return
    }
    if (account.length < 6) {
        Message.error('用户名不能低于6位长度')
        return
    }
    if (password.length < 6) {
        Message.error('密码不能低于6位长度')
        return
    }

    isLoggingIn.value = true

    try {
        const response = await authAPI.login({
            account,
            password
        })

        if (response.success) {
            Message.success('登录成功！正在跳转...')

            setTimeout(async () => {
                const role = response.data?.role
                const userId = response.data?.id
                const roles = response.data?.roles || []

                if (role && ['ROLE_USER', 'ROLE_SELLER', 'ROLE_ADMIN'].includes(role)) {
                    authStore.isLoggedIn = true
                    authStore.role = role
                    authStore.activeRole = role
                    authStore.roles = roles
                    authStore.userId = userId
                    authStore.status = 1
                } else {
                    await authStore.checkAndUpdate()
                }

                switch(authStore.activeRole) {
                    case "ROLE_USER":
                        router.replace({ name: "UserDashboard" })
                        break
                    case "ROLE_SELLER":
                        router.replace({ name: "SellerDashboard" })
                        break
                    case "ROLE_ADMIN":
                        router.replace({ name: "AdminDashboard" })
                        break
                    default:
                        router.replace("/")
                }
            }, 1500)
        } else {
            Message.error(response.message || '登录失败')
        }
    } catch (error: any) {
        let errorMsg = '登录失败'
        if (error.response?.status === 401) {
            errorMsg = '账号或密码错误'
        } else if (error.response?.data?.message) {
            errorMsg = error.response.data.message
        } else if (error.message) {
            errorMsg = error.message
        }

        Message.error(errorMsg)
    } finally {
        isLoggingIn.value = false
    }
}

const handleRegister = async () => {
    const { account, password, confirmPassword } = registerData.value

    if (!account || !password) {
        Message.error('用户名和密码不能为空')
        return
    }
    if (account.length < 6) {
        Message.error('用户名不能低于6位长度')
        return
    }
    if (password.length < 6) {
        Message.error('密码不能低于6位长度')
        return
    }
    if (password !== confirmPassword) {
        Message.error('两次密码输入不一致，请重新输入！')
        return
    }

    isRegistering.value = true

    try {
        const response = await authAPI.register({
            account,
            password,
            confirmPassword
        })

        if (response.success) {
            Message.success('注册成功！正在跳转登录...')

            setTimeout(() => {
                activeTab.value = 'login'
                loginData.value.account = account
                loginData.value.password = ''
            }, 2000)
        } else {
            Message.error(response.message || '注册失败')
        }
    } catch (error: any) {
        let errorMsg = '注册失败'
        if (error.response?.data?.message) {
            errorMsg = error.response.data.message
        } else if (error.response?.data?.error) {
            errorMsg = error.response.data.error
        } else if (error.message) {
            errorMsg = error.message
        }

        Message.error(errorMsg)
    } finally {
        isRegistering.value = false
    }
}

const checkLoginStatus = async () => {
  checkingLogin.value = true
  await new Promise(resolve => setTimeout(resolve, 500))

  if (authStore.isLoggedIn) {
    try {
      const response = await authAPI.getAccountProfile()
      if (response.success && response.data?.accountProfile) {
        const profile = response.data.accountProfile
        const role = profile.role

        let nickname = profile.nickname || profile.username || '用户'

        if (role === 'ROLE_SELLER') {
          nickname = `${nickname}（商家账号）`
        } else if (role === 'ROLE_ADMIN') {
          nickname = `${nickname}（管理员账号）`
        }

        existingUser.value = {
          avatar: profile.avatar || '',
          nickname: nickname,
          account: profile.username || ''
        }
      }
    } catch {
      existingUser.value = null
    }
  } else {
    existingUser.value = null
  }
  checkingLogin.value = false
}

const continueWithAccount = async () => {
  try {
    const response = await authAPI.getAccountProfile()
    if (response.success && response.data?.accountProfile) {
      const role = response.data.accountProfile.role
      switch(role) {
        case 'ROLE_USER': router.replace({ name: 'UserDashboard' }); break
        case 'ROLE_SELLER': router.replace({ name: 'SellerDashboard' }); break
        case 'ROLE_ADMIN': router.replace({ name: 'AdminDashboard' }); break
        default: router.replace('/')
      }
    }
  } catch {
    Message.error('获取用户信息失败')
  }
}

const switchAccount = async () => {
  try {
    await authAPI.logout()
    authStore.clear()
  } catch {}
  existingUser.value = null
  loginData.value = { account: '', password: '' }
  activeTab.value = 'login'
}

const handleFocus = (event: FocusEvent) => {
    const target = event.target as HTMLInputElement
    target.style.borderColor = "#4a6491"
}

const handleBlur = (event: FocusEvent) => {
    const target = event.target as HTMLInputElement
    target.style.borderColor = "#e2e8f0"
}

onMounted(() => {
  checkLoginStatus()
})
</script>

<style scoped>
@import url("@/static/css/user/登录页面.css");
</style>
