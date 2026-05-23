export default [

  // ============= 登录路由 =============
  {
    path:"/login",
    name: "Login",
    component: () => import("@/views/user/Login.vue"),
  },
  
  // ============ 用户端路由 - 移动端布局 ============
  {
    path: '/user',
    name: 'MobileLayout',
    component: () => import('@/views/user/Layout/MobileLayout.vue'),
    redirect: '/user/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'UserDashboard',
        component: () => import('@/views/user/Dashboard.vue'),
      },
      {
        path: 'categories',
        name: 'UserCategories',
        component: () => import('@/views/user/Categories.vue'),
      },
      {
        path: 'messages',
        name: 'UserMessages',
        component: () => import('@/views/user/Messages.vue'),
      },
      {
        path: '/user/center',
        name: 'UserCenter',
        component: () => import('@/views/user/Center.vue'),
      },
    ]
  },

  // ============ 用户端其他页面（独立路由） ============

  // 搜索页
  {
    path: '/user/search',
    name: 'UserSearch',
    component: () => import('@/views/user/Search.vue'),
  },

  // 个人资料
  {
    path: '/user/profile',
    name: 'UserProfile',
    component: () => import('@/views/user/Profile.vue'),
  },

  // 订单列表
  {
    path: '/user/orders',
    name: 'UserOrders',
    component: () => import('@/views/user/Orders.vue'),
  },

  // 收藏路由
  {
    path: '/user/favorites',
    name: 'UserFavorites',
    component: () => import('@/views/user/Favorites.vue'),
  },

  // 收货地址
  {
    path: '/user/addresses',
    name: 'UserAddresses',
    component: () => import('@/views/user/Addresses.vue'),
  },

  // 优惠券
  {
    path: '/user/coupons',
    name: 'UserCoupons',
    component: () => import('@/views/user/Coupons.vue'),
  },

  // 账户设置
  {
    path: '/user/setting',
    name: 'UserSetting',
    component: () => import('@/views/user/Setting.vue'),
  },

  {
    path: '/merchant/apply',
    name: 'MerchantApply',
    component: () => import('@/views/user/MerchantApply.vue'),
  },
  
  // ============ 商品相关路由 ============
  {
    path: '/products/:productId',
    name: 'ProductDetail',
    component: () => import('@/views/user/ProductDetail.vue'),
    props: true,
  },

  // ============ 商家店铺路由 ============
  {
    path: '/shop/:sellerId',
    name: 'Shop',
    component: () => import('@/views/user/Shop.vue'),
  },
  
  // ============ 购物车路由 ============
  {
    path: '/cart',
    name: 'Cart',
    component: () => import('@/views/user/Cart.vue'),
  },
  
  // ============ 结算路由 ============
  {
    path: '/checkout',
    name: 'Checkout',
    component: () => import('@/views/user/Checkout.vue'),
  },
  
  // ============ 订单相关路由 ============
  {
    path: '/orders/:orderId',
    name: 'OrderDetail',
    component: () => import('@/views/user/OrderDetail.vue'),
    props: true,
  },

  // ============= 评论路由 ==============
  {
    path: '/review/:orderItemId',
    name: 'Review',
    component: () => import('@/views/user/Review.vue'),
    props: true,
  },

  // ============= 查询物流 ===============
  {
    path: '/user/logistics',
    name: 'UserLogistics',
    component: () => import('@/views/user/Logistics.vue'),
  },

]
