// router/index.ts
import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

// 导入模块路由
import userRoutes from './modules/user.ts'
import sellerRoutes from './modules/seller.ts'
import adminRoutes from './modules/admin.ts'

// 创建路由实例
const router = createRouter({
  // 工作模式
  history: createWebHistory(import.meta.env.BASE_URL),

  // 合并所有路由模块
  routes: [
    // 登录路由
    {
      path:"/login",
      name: "Login",
      component: () => import("@/views/user/Layout/Login.vue"),
    },
    ...userRoutes,    // 用户路由（动态添加）
    ...sellerRoutes,  // 商家路由（动态添加）
    ...adminRoutes,   // 管理员路由（动态添加）
    {
      path: '/',
      redirect: { name: 'UserDashboard' }
    },
  ],

  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    }
    if (from.name === 'ProductDetail' && to.name === 'UserDashboard') {
      return false
    }
    return { top: 0 }
  }
})

// 在路由切换开始前执行，用于权限验证
router.beforeEach(async () => {
  // 调用Pinia实例返回函数值
  const authStore = useAuthStore()

  // 每次跳转都重新检查（调用后端接口）
  await authStore.checkAndUpdate()
})

router.afterEach(() => {
  document.title = '云杉购'
})

export default router
