
export default [

  // ============= 登录路由 =============
  {
    path:"/login",
    name: "Login",
    component: () => import("@/views/user/Layout/Login.vue"),
  },
  
  // ============ 用户端路由 ============
  {
    path: '/user/dashboard',
    name: "UserDashboard",
    component: () => import('@/views/user/Dashboard.vue'),
    meta: {
      title: '用户首页',
    },
  },
  
  // 个人中心布局页
  {
    path: '/user',
    name: 'CenterLayout',
    component: () => import('@/views/user/Layout/CenterLayout.vue'),
    redirect: {name: 'UserCenter'},
    children: [
      // 个人中心
      {
        path: 'center',
        name: 'UserCenter',
        component: () => import('@/components/user/Center.vue'),
        meta: {
          title: '个人中心',
        },
      },
      // 个人信息
      {
        path: 'profile',
        name: 'UserProfile',
        component: () => import('@/components/user/Profile.vue'),
        meta: {
          title: '个人资料',
        },
      },
      // 订单
      {
        path: 'orders',
        name: 'UserOrders',
        component: () => import('@/components/user/Orders.vue'),
        meta: {
          title: '我的订单',
        },
      },
      // 收藏路由
      {
        path: 'favorites',
        name: 'UserFavorites',
        component: () => import('@/components/user/Favorites.vue'),
        meta: {
          title: '我的收藏',
        }
      },
      // 收货地址
      {
        path: 'addresses',
        name: 'UserAddresses',
        component: () => import('@/components/user/Addresses.vue'),
        meta: {
          title: '收货地址',
        },
      },
      // 优惠券
      {
        path: 'coupons',
        name: 'UserCoupons',
        component: () => import('@/components/user/Coupons.vue'),
        meta: {
          title: '我的优惠券',
        },
      },
      // 账户设置
      {
        path: 'setting',
        name: 'UserSetting',
        component: () => import('@/components/user/Setting.vue'),
        meta: {
          title: '账户设置',
        },
      },
    ]
  },
  
  // ============ 商品相关路由 ============
  {
    path: '/products/:productId',
    name: 'ProductDetail',
    component: () => import('@/views/user/ProductDetail.vue'),
    props: true,  // 将路由参数作为 props 传递给组件
    meta: {
      title: '商品详情',
    },
  },
  
  // ============ 购物车路由 ============
  {
    path: '/cart',
    name: 'Cart',
    component: () => import('@/views/user/Cart.vue'),
    meta: {
      title: '购物车',   
    },
  },
  
  // ============ 结算路由 ============
  {
    path: '/checkout',
    name: 'Checkout',
    component: () => import('@/views/user/Checkout.vue'),
    meta: {
      title: '订单结算',
    },
  },
  
  // ============ 订单相关路由 ============
  {
    path: '/orders/:orderId',
    name: 'OrderDetail',
    component: () => import('@/views/user/OrderDetail.vue'),
    props: true,
    meta: {
      title: '订单详情',
    },
  },

  // ============= 评论路由 ==============
  {
      path: '/review/:orderItemId',
      name: 'Review',
      component: () => import('@/views/user/Review.vue'),
      meta: { 
        title: '发表评价' 
      }
  },

  // ============= 查询物流 ===============
  {
    path: '/user/logistics',
    name: 'UserLogistics',
    component: () => import('@/views/user/Logistics.vue'),
    meta: { 
      title: '物流详情',
    }
  },
  
  // ============ 商家入驻路由 ============
  {
    path: '/merchant/apply',
    name: 'MerchantApply',
    // component: () => import('@/views/merchant/Apply.vue'),
    meta: {
      title: '商家入驻',
    },
  },


]