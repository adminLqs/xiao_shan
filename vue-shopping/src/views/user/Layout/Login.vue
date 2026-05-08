<template>
  <div id="main_background"></div>

  <!-- 购物主题装饰元素 -->
  <div class="shopping-elements" id="shopping-elements"></div>

  <div class="main-container">
    <div class="main_section">
      <!-- 左侧欢迎区域 -->
      <div class="welcome-section">
        <h2 class="welcome-title">欢迎来到购物商城</h2>
        <p class="welcome-text">登录您的账户，发现更多精彩商品和专属优惠</p>

        <ul class="shopping-benefits">
          <li><i class="fas fa-check-circle"></i> 获取独家折扣和促销信息</li>
          <li><i class="fas fa-check-circle"></i> 查看您的订单历史和收藏夹</li>
          <li><i class="fas fa-check-circle"></i> 享受更快捷的结账体验</li>
          <li><i class="fas fa-check-circle"></i> 获取个性化商品推荐</li>
        </ul>
      </div>

      <!-- 右侧表单区域 -->
      <div class="form-section">
        <div class="logo">
          <h2><i class="fas fa-shopping-bag"></i> 购物商城</h2>
          <p>登录并开始您的购物之旅</p>
        </div>

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

        <!-- 登录表单 -->
        <form class="login-form" v-if="activeTab === 'login'">
          <div class="input-container">
            <i class="fas fa-user input-icon"></i>
            <input 
              type="text" 
              placeholder="用户名/手机号/邮箱" 
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
              placeholder="密码" 
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
            class="btn login_button" 
            @click="handleLogin"
            :disabled="isLoggingIn"
          >
            <i v-if="isLoggingIn" class="fas fa-spinner fa-spin"></i>
            {{ isLoggingIn ? '登录中...' : '登 录' }}
          </button>

          <div class="helper-links">
            <a href="#">忘记密码?</a>
            <a href="#">手机快速登录</a>
          </div>
        </form>

        <!-- 注册表单 -->
        <form class="register-form" v-if="activeTab === 'register'">
          <div class="input-container">
            <i class="fas fa-user input-icon"></i>
            <input 
              type="text" 
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
            class="btn register_button" 
            @click="handleRegister"
            :disabled="isRegistering"
          >
            <i v-if="isRegistering" class="fas fa-spinner fa-spin"></i>
            {{ isRegistering ? '注册中...' : '注 册' }}
          </button>
        </form>
      </div>
    </div>
  </div>
  
</template>

<script setup lang="ts">
  import { ref } from 'vue'
  import { useRouter } from 'vue-router'
  import { authAPI } from '@/api/authAPI'
  import { showToast, showLoadingToast, closeToast } from 'vant'
  import 'vant/es/toast/style'

  const router = useRouter()

  // ========== 响应式数据 ==========
  const activeTab = ref('login') // 当前激活的选项卡
  const showLoginPassword = ref(false) // 登录密码是否可见
  const showRegisterPassword = ref(false) // 注册密码是否可见
  const showRegisterConfirm = ref(false) // 确认密码是否可见
  const isLoggingIn = ref(false) // 登录加载状态
  const isRegistering = ref(false) // 注册加载状态

  // 表单数据
  const loginData = ref({
      account: '',
      password: ''
  })

  const registerData = ref({
      account: '',
      password: '',
      confirmPassword: ''
  })

  // ========== 方法函数 ==========

  /**
   * 切换登录/注册选项卡
   */
  const switchTab = (tab: string) => {
      activeTab.value = tab
  }

  /**
   * 切换密码显示/隐藏
   */
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

  /**
   * 登录处理
   */
  const handleLogin = async () => {
      const { account, password } = loginData.value

      // 表单验证
      if (!account || !password) {
          showToast({
              message: '用户名和密码不能为空',
              type: 'fail',
              duration: 2000
          })
          return
      }
      if (account.length < 6) {
          showToast({
              message: '用户名不能低于6位长度',
              type: 'fail',
              duration: 2000
          })
          return
      }
      if (password.length < 6) {
          showToast({
              message: '密码不能低于6位长度',
              type: 'fail',
              duration: 2000
          })
          return
      }

      // 设置加载状态
      isLoggingIn.value = true

      // 显示加载提示
      const loadingToast = showLoadingToast({
          message: '登录中...',
          duration: 0,
          forbidClick: true
      })

      try {
          // 调用登录接口
          const response = await authAPI.login({
              account,
              password
          })

          loadingToast.close()

          if (response.success) {
              showToast({
                  message: '登录成功！正在跳转...',
                  type: 'success',
                  duration: 1500
              })
              
              // 延迟跳转
              setTimeout(() => {
                  switch(response.data?.role) {
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
              showToast({
                  message: response.message || '登录失败',
                  type: 'fail',
                  duration: 2000
              })
          }
      } catch (error: any) {
          loadingToast.close()
          
          // 错误处理
          let errorMsg = '登录失败'
          if (error.response?.status === 401) {
              errorMsg = '账号或密码错误'
          } else if (error.response?.data?.message) {
              errorMsg = error.response.data.message
          } else if (error.message) {
              errorMsg = error.message
          }
          
          showToast({
              message: errorMsg,
              type: 'fail',
              duration: 2000
          })
      } finally {
          isLoggingIn.value = false
      }
  }

  /**
   * 注册处理
   */
  const handleRegister = async () => {
      const { account, password, confirmPassword } = registerData.value

      // 表单验证
      if (!account || !password) {
          showToast({
              message: '用户名和密码不能为空',
              type: 'fail',
              duration: 2000
          })
          return
      }
      if (account.length < 6) {
          showToast({
              message: '用户名不能低于6位长度',
              type: 'fail',
              duration: 2000
          })
          return
      }
      if (password.length < 6) {
          showToast({
              message: '密码不能低于6位长度',
              type: 'fail',
              duration: 2000
          })
          return
      }
      if (password !== confirmPassword) {
          showToast({
              message: '两次密码输入不一致，请重新输入！',
              type: 'fail',
              duration: 2000
          })
          return
      }

      // 设置加载状态
      isRegistering.value = true

      // 显示加载提示
      const loadingToast = showLoadingToast({
          message: '注册中...',
          duration: 0,
          forbidClick: true
      })

      try {
          // 调用注册接口
          const response = await authAPI.register({
              account,
              password,
              confirmPassword
          })

          loadingToast.close()

          if (response.success) {
              showToast({
                  message: '注册成功！正在跳转登录...',
                  type: 'success',
                  duration: 2000
              })
              
              // 自动切换到登录界面
              setTimeout(() => {
                  activeTab.value = 'login'
                  loginData.value.account = account
                  loginData.value.password = ''
              }, 2000)
          } else {
              showToast({
                  message: response.message || '注册失败',
                  type: 'fail',
                  duration: 2000
              })
          }
      } catch (error: any) {
          loadingToast.close()
          
          // 错误处理
          let errorMsg = '注册失败'
          if (error.response?.data?.message) {
              errorMsg = error.response.data.message
          } else if (error.response?.data?.error) {
              errorMsg = error.response.data.error
          } else if (error.message) {
              errorMsg = error.message
          }
          
          showToast({
              message: errorMsg,
              type: 'fail',
              duration: 2000
          })
      } finally {
          isRegistering.value = false
      }
  }

  /**
   * 输入框焦点效果
   */
  const handleFocus = (event: FocusEvent) => {
      const target = event.target as HTMLInputElement
      target.style.borderColor = "#64ffda"
  }

  const handleBlur = (event: FocusEvent) => {
      const target = event.target as HTMLInputElement
      target.style.borderColor = "#ddd"
  }
</script>

<style scoped>
  @import url("@/static/css/user/登录页面.css");
</style>