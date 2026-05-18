import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/auth.ts'

// 创建路由实例
const router = createRouter({
  // 工作模式
  history: createWebHistory(import.meta.env.BASE_URL),

  // 合并所有路由模块
  routes: [
    // 用户页
    {
      path: '/user',
      name: 'UserLayout',
      component: () => import("@/views/user/Layout.vue"),
      redirect: { name: 'UserDashboard'},
      children: [
        {
          path: 'dashboard',
          name: 'UserDashboard',
          component: () => import('@/components/user/Dashboard.vue'),
          meta: {
            title: '烧烤选购'
          }
        },
        {
          path: 'order',
          name: 'UserOrder',
          component: () => import('@/components/user/Orders.vue'),
          meta: {
            title: '用户订单'
          }
        },
        {
          path: 'account',
          name: 'UserAccount',
          component: () => import('@/components/user/Account.vue'),
          meta: {
            title: '账号管理'
          }
        }
      ]
    },   
    // 用户订单详情页
    {
      path: '/order/detail',
      name: 'OrderDetail',
      component: () => import('@/views/user/OrderDetail.vue'),
      meta: {
        title: '订单详情'
      }
    },
    // 预结算
    {
      path: '/precheckout',
      name: 'UserPreCheckout',
      component: () => import("@/views/user/PreCheckout.vue")
    },
    // 结算页
    {
      path: '/payment',
      name: 'UserPayment',
      component: () => import("@/views/user/Payment.vue"),
      meta: {
        title: "订单支付"
      }
    },
    // 客服页
    {
      path: '/customer-service',
      name: 'CustomerService',
      component: () => import('@/views/user/CustomerService.vue'),
      meta: {
        title: '在线客服'
      }
    },
 
    

    // 商家页
    {
      path: '/seller',
      name: 'SellerLayout',
      component: () => import("@/views/seller/Layout.vue"),
      redirect: {name: 'SellerProducts'},
      children: [
        {
          path: 'products',
          name: 'SellerProducts',
          component: () => import("@/components/seller/Product.vue"),
          meta: {
            title: '商品管理'
          }
        },
        {
          path: 'products/add',
          name: 'SellerAddProduct',
          component: () => import("@/components/seller/AddProduct.vue"),
          meta: {
            title: '商品添加'
          }
        },
        {
          path: 'orders',
          name: 'SellerOrders',
          component: () => import("@/components/seller/Orders.vue"),
          meta: {
            title: '订单管理'
          }
        },
        {
          path: 'profile',
          name: 'SellerProfile',
          component: () => import('@/components/seller/Profile.vue'),
          meta: {
            title: '商家信息'
          }
        },
        {
          path: 'chat',
          name: 'SellerChat',
          component: () => import('@/components/seller/ChatManagement.vue'),
          meta: {
            title: '客服消息'
          }
        }
        
      ]
    },
    // 客服回复
    {
      path: '/chat/detail',
      name: 'SellerChatDetail',
      component: () => import('@/views/seller/ChatDetail.vue'),
      meta: {
        title: '对话详情'
      }
    },
    // 订单详情
    {
      path: '/seller/order/detail',
      name: 'SellerOrderDetail',
      component: () => import('@/views/seller/OrderDetail.vue'),
      meta: {
        title: '订单详情'
      },
    },

    // 默认导航
    {
      path: '/',
      redirect: {name: 'UserDashboard'},
    },
  ],
})

// ==================== 全局前置守卫 ====================
router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()
  
  // 确保用户已初始化
  if (!userStore.userId) {
    await userStore.initUser()
  }

  next()
})

// ==================== 全局后置守卫 ====================
router.afterEach((to) => {
  // 设置页面标题
  if (to.meta.title) {
    document.title = `${to.meta.title} | 杉杉烧烤`
  } else {
    document.title = '杉杉烧烤'
  }
  
  // 滚动到顶部
  window.scrollTo({ top: 0, behavior: 'smooth' })
})

export default router