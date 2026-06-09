export default [
  {
    path: '/admin',
    name: 'AdminLayout',
    component: () => import('@/views/admin/Layout.vue'),
    redirect: {name: 'AdminDashboard'},
    children: [
      {
        path: 'dashboard',
        name: 'AdminDashboard',
        component: () => import('@/views/admin/Dashboard.vue'),
      },
      {
        path: 'applications',
        name: 'AdminApplications',
        component: () => import('@/views/admin/Applications.vue'),
      },
      {
        path: 'applications/:id',
        name: 'AdminApplicationDetail',
        component: () => import('@/views/admin/Applications.vue'),
      },
      {
        path: 'sellers',
        name: 'AdminSellers',
        component: () => import('@/views/admin/Sellers.vue'),
      },
      {
        path: 'products',
        name: 'AdminProducts',
        component: () => import('@/views/admin/AdminProducts.vue'),
      },
      {
        path: 'categories',
        name: 'AdminCategories',
        component: () => import('@/views/admin/Category.vue'),
      },
      {
        path: 'banners',
        name: 'AdminBanners',
        component: () => import('@/views/admin/Banner.vue'),
      },
      {
        path: 'users',
        name: 'AdminUsers',
        component: () => import('@/views/admin/Users.vue'),
      },
      {
        path: 'admin-users',
        name: 'AdminAdminUsers',
        component: () => import('@/views/admin/AdminUsers.vue'),
      }
    ]
  }
]
