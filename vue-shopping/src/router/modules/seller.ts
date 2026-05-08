
export default [
  {
    path: '/seller',
    name: 'SellerLayout',
    component: () => import('@/views/seller/Layout/SellerLayout.vue'),
    meta: { title: '商家中心' },
    redirect: '/seller/dashboard',
    children: [
      // 商家首页
      {
        path: 'dashboard',
        name: 'SellerDashboard',
        component: () => import('@/components/seller/Dashboard.vue'),
        meta: { title: '控制台' }
      },

      // 商家管理商品
      {
        path: 'products',
        name: 'SellerProducts',
        component: () => import('@/components/seller/Products.vue'),
        meta: { title: '商品管理' }
      },

      // 商家发布商品
      {
        path: 'products/create',
        name: 'SellerAddProduct',
        component: () => import('@/components/seller/AddProduct.vue'),
         meta: { title: '发布商品' }
      },

      // 商品编辑有页
      {
        path: 'products/:productId/edit',
        name: 'SellerProductEdit',
        component: () => import("@/components/seller/ProductEdit.vue"),
        meta: { title: '编辑商品' }
      },

      // 订单管理
      {
        path: 'orders',
        name: 'SellerOrders',
        component: () => import('@/components/seller/Orders.vue'),
        meta: {
          title: '订单管理',
          icon: 'fas fa-shopping-cart',
          breadcrumb: ['商家中心', '订单管理']
        }
      },
      
      // 商家信息
      {
        path: 'profile',
        name: 'SellerProfile',
        component: () => import('@/components/seller/Profile.vue'),
        meta: { title: '商家信息' }
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

    }
  }

]
