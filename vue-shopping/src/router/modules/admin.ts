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
        component: () => import('@/views/admin/Products.vue'),
      },
      {
        path: 'users',
        name: 'AdminUsers',
        component: () => import('@/views/admin/Users.vue'),
      }
    ]
  }
]
