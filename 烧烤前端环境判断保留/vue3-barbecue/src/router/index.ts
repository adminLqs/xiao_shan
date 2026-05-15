import { createRouter, createWebHistory } from 'vue-router'

// 创建路由实例
export default createRouter({
  // 工作模式
  history: createWebHistory(import.meta.env.BASE_URL),

  // 合并所有路由模块
  routes: [
    // 用户页
    {
      path: '/',
      name: 'UserLayout',
      component: () => import("@/views/user/Layout.vue"),
      redirect: '/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'UserDashboard',
          component: () => import('@/components/user/Dashboard.vue')
        },
        {
          path: 'order',
          name: 'UserOrder',
          component: () => import('@/components/user/Orders.vue')
        },
        // 订单详情页
        {
          path: '/order/detail',
          name: 'OrderDetail',
          component: () => import('@/components/user/OrderDetail.vue'),
          meta: {
            requiresAuth: true,
            title: '订单详情'
          }
        },
      ]
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
        requiresAuth: true,
        title: "订单支付"
      }
    },

    // 商家页
    {
      path: '/seller',
      name: 'SellerLayout',
      component: () => import("@/views/seller/Layout.vue"),
      redirect: '/seller/products/add',
      children: [
        {
          path: 'products',
          name: 'SellerProducts',
          component: () => import("@/components/seller/Product.vue")
        },
        {
          path: 'products/add',
          name: 'SellerAddProduct',
          component: () => import("@/components/seller/AddProduct.vue"),
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
          component: () => import('@/components/seller/Profile.vue')
        }
      ]
    },
    // 默认导航
    {
      path: '/',
      redirect: '/',
    },
  ],
})
