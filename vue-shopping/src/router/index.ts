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
    ...userRoutes,    // 用户路由（动态添加）
    ...sellerRoutes,  // 商家路由（动态添加）
    ...adminRoutes,   // 管理员路由（动态添加）
    {
      path: '/',
      redirect: { name: 'UserDashboard' }
    },
  ],
})

// ========== 前置守卫：权限校验 ==========
// 在路由切换开始前执行，用于权限验证
router.beforeEach(async () => {
  // 调用Pinia实例返回函数值
  const authStore = useAuthStore()
  
  // 每次跳转都重新检查（调用后端接口）
  await authStore.checkAndUpdate()
})


// ========== 后置守卫：页面标题 ==========
// 在路由切换完成后执行，用于设置页面标题
router.afterEach((to) => {
  // 设置页面标题
  const defaultTitle = '精品商城'
  const title = to.meta.title ? `${to.meta.title} - ${defaultTitle}` : defaultTitle
  document.title = title
})

export default router