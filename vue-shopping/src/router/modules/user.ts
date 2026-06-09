export default [

  // ============ 用户端路由 - 移动端布局 ============
  {
    path: '/user',
    name: 'MobileLayout',
    component: () => import('@/views/user/Layout/MobileLayout.vue'),
    redirect: '/user/dashboard',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'UserDashboard',
        component: () => import('@/views/user/Dashboard.vue'),
        meta: { keepAlive: true }
      },
      {
        path: 'messages',
        name: 'UserMessages',
        component: () => import('@/views/user/Messages.vue'),
        meta: { keepAlive: true }
      },
      {
        path: 'center',
        name: 'UserCenter',
        component: () => import('@/views/user/Center.vue'),
        meta: { keepAlive: true }
      },
    ]
  },
  // 购物车
  {
    path: '/user/cart',
    name: 'Cart',
    component: () => import('@/views/user/Cart.vue'),
    meta: { keepAlive: true, requiresAuth: true }
  },

  // ============ 用户端其他页面（独立路由） ============

  // 搜索页
  {
    path: '/user/search',
    component: () => import('@/views/user/SearchLayout.vue'),
    redirect: '/user/search/default',
    children: [
      {
        path: 'default',
        name: 'SearchDefault',
        component: () => import('@/views/user/SearchDefault.vue')
      },
      {
        path: 'result',
        name: 'SearchResult',
        component: () => import('@/views/user/SearchResult.vue')
      }
    ]
  },

  // 个人资料
  {
    path: '/user/profile',
    name: 'UserProfile',
    component: () => import('@/views/user/Profile.vue'),
    meta: { requiresAuth: true }
  },

  // 订单列表
  {
    path: '/user/orders',
    name: 'UserOrders',
    component: () => import('@/views/user/orders/index.vue'),
    meta: { requiresAuth: true }
  },

  // 收藏路由
  {
    path: '/user/favorites',
    name: 'UserFavorites',
    component: () => import('@/views/user/Favorites.vue'),
    meta: { requiresAuth: true }
  },

  // 收货地址
  {
    path: '/user/addresses',
    name: 'UserAddresses',
    component: () => import('@/views/user/Addresses.vue'),
    meta: { requiresAuth: true }
  },

  // 领券中心
  {
    path: '/user/coupons',
    name: 'UserCoupons',
    component: () => import('@/views/user/Coupons.vue'),
    meta: { requiresAuth: true }
  },

  // 我的优惠券
  {
    path: '/user/my-coupons',
    name: 'MyCoupons',
    component: () => import('@/views/user/MyCoupons.vue'),
    meta: { requiresAuth: true }
  },

  // 账户设置
  {
    path: '/user/setting',
    name: 'UserSetting',
    component: () => import('@/views/user/Setting.vue'),
    meta: { requiresAuth: true }
  },

  {
    path: '/user/merchant/apply',
    name: 'MerchantApply',
    component: () => import('@/views/user/MerchantApply.vue'),
    meta: { requiresAuth: true }
  },

  // ============ 商品相关路由 ============
  {
    path: '/user/products/:productId',
    name: 'ProductDetail',
    component: () => import('@/views/user/ProductDetail.vue'),
    props: true,
    meta: { keepAlive: false, requiresAuth: true }
  },

  // 商品评论列表
  {
    path: '/user/products/:productId/reviews',
    name: 'ProductReviews',
    component: () => import('@/views/user/Reviews.vue'),
    props: true,
    meta: { requiresAuth: true }
  },

  // ============ 商家店铺路由 ============
  {
    path: '/user/shop/:sellerId',
    name: 'Shop',
    component: () => import('@/views/user/Shop.vue'),
    meta: { requiresAuth: true }
  },

  // ============ 结算路由 ============
  {
    path: '/user/checkout',
    name: 'Checkout',
    component: () => import('@/views/user/Checkout.vue'),
    meta: { requiresAuth: true }
  },

  // ============ 订单相关路由 ============
  {
    path: '/user/orders/:orderId',
    name: 'OrderDetail',
    component: () => import('@/views/user/OrderDetail.vue'),
    props: true,
    meta: { requiresAuth: true }
  },

  // ============= 评论路由 ==============
  {
    path: '/user/review/:orderItemId',
    name: 'Review',
    component: () => import('@/views/user/Review.vue'),
    props: true,
    meta: { requiresAuth: true }
  },
  {
    path: '/user/reviews',
    name: 'ReviewList',
    component: () => import('@/views/user/ReviewList.vue'),
    meta: { requiresAuth: true }
  },

  // ============= 查询物流 ===============
  {
    path: '/user/logistics',
    name: 'UserLogistics',
    component: () => import('@/views/user/Logistics.vue'),
    meta: { requiresAuth: true }
  },

  // ============= 退款售后 ===============
  {
    path: '/user/refund',
    name: 'RefundFlow',
    component: () => import('@/views/user/refund/index.vue'),
    redirect: '/user/refund/apply',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'apply/:orderItemId',
        name: 'RefundApply',
        component: () => import('@/views/user/refund/components/RefundApply.vue'),
        props: true,
      },
      {
        path: 'chat/:refundId',
        name: 'RefundChatStep',
        component: () => import('@/views/user/refund/components/RefundChatStep.vue'),
        props: true,
      },
      {
        path: 'return/:refundId/:orderItemId',
        name: 'ReturnGoods',
        component: () => import('@/views/user/refund/components/ReturnGoods.vue'),
        props: true,
      },
      {
        path: 'detail/:refundId',
        name: 'RefundDetail',
        component: () => import('@/views/user/RefundDetail.vue'),
        props: true,
      },
    ]
  },

  {
    path: '/user/after-sale',
    name: 'AfterSaleList',
    component: () => import('@/views/user/AfterSaleList.vue'),
    meta: { requiresAuth: true }
  },

  {
    path: '/user/messages/list',
    name: 'MessageList',
    component: () => import('@/components/NotificationList.vue'),
    meta: { title: '通知列表', requiresAuth: true }
  },

]
