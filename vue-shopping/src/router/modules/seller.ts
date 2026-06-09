export default [
  {
    path: '/seller',
    name: 'SellerLayout',
    component: () => import('@/views/seller/Layout/Layout.vue'),
    meta: { title: '商家中心', requiresAuth: true },
    redirect: '/seller/dashboard',
    children: [
      // 商家首页
      {
        path: 'dashboard',
        name: 'SellerDashboard',
        component: () => import('@/views/seller/Dashboard.vue'),
        meta: { title: '控制台', keepAlive: true }
      },

      // 商家管理商品
      {
        path: 'products',
        name: 'SellerProducts',
        component: () => import('@/views/seller/Products.vue'),
        meta: { title: '商品管理', keepAlive: true }
      },

      // 商家发布商品
      {
        path: 'products/create',
        name: 'SellerAddProduct',
        component: () => import('@/views/seller/AddProduct.vue'),
         meta: { title: '发布商品', keepAlive: false }
      },

      // 商品编辑有页
      {
        path: 'products/:productId/edit',
        name: 'SellerProductEdit',
        component: () => import("@/views/seller/ProductEdit.vue"),
        meta: { title: '编辑商品', keepAlive: false }
      },

      // 优惠券管理
      {
        path: 'coupons',
        name: 'SellerCoupons',
        component: () => import('@/views/seller/Coupons.vue'),
        meta: { title: '优惠券管理', keepAlive: true }
      },

      // 订单管理
      {
        path: 'orders',
        name: 'SellerOrders',
        component: () => import('@/views/seller/orders/index.vue'),
        meta: {
          title: '订单管理',
          icon: 'fas fa-shopping-cart',
          breadcrumb: ['商家中心', '订单管理'],
          keepAlive: true
        }
      },
      {
        path: 'analytics',
        name: 'SellerAnalytics',
        component: () => import('@/views/seller/Analytics.vue'),
        meta: {
          title: '数据分析',
          keepAlive: true
        }
      },

      // 商家信息
      {
        path: 'profile',
        name: 'SellerProfile',
        component: () => import('@/views/seller/Profile.vue'),
        meta: { title: '商家信息' }
      },

      // 消息中心
      {
        path: 'messages',
        name: 'SellerMessages',
        component: () => import('@/views/seller/Messages.vue'),
        meta: { title: '消息中心' }
      },

      // 套餐购买
      {
        path: 'package',
        name: 'SellerPackage',
        component: () => import('@/views/seller/Package.vue'),
        meta: { title: '套餐购买' }
      },

      // 套餐记录
      {
        path: 'package-records',
        name: 'SellerPackageRecords',
        component: () => import('@/views/seller/PackageRecords.vue'),
        meta: { title: '套餐记录' }
      }

    ]
  },

  // 订单详情页
  {
    path: '/seller/orders/:id',
    name: 'SellerOrderDetail',
    component: () => import('@/views/seller/OrderDetail.vue'),
    meta: {
      title: '订单详情',
      keepAlive: false,
      requiresAuth: true
    }
  },

  // ============ 物流路由 ============

  // 商家物流详情页
  {
    path: '/seller/logistics',
    name: 'SellerLogistics',
    component: () => import('@/views/seller/Logistics.vue'),
    meta: {
      title: '物流详情',
      requiresAuth: true
    }
  },

  // 商家退款沟通页 - 共用用户端组件
  {
    path: '/seller/refund/chat/:refundId',
    name: 'SellerRefundChatStep',
    component: () => import('@/views/user/refund/components/RefundChatStep.vue'),
    meta: {
      title: '退款沟通',
      requiresAuth: true
    }
  },

  {
    path: '/seller/messages/list',
    name: 'SellerMessageList',
    component: () => import('@/components/NotificationList.vue'),
    meta: { title: '通知列表', requiresAuth: true }
  }

]
