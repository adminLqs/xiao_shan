import http from '@/utils/axios-config'

// ==================== 类型定义 ====================
interface ApiResponse<T = any> {
  success: boolean
  data: T
  message?: string
}

// ==================== 内部请求包装 ====================
function request<T = any>(
  method: 'get' | 'post' | 'put' | 'delete' | 'patch',
  url: string,
  dataOrConfig?: any,
  config?: any
): Promise<ApiResponse<T>> {
  if (method === 'get' || method === 'delete') {
    return http[method](url, dataOrConfig) as unknown as Promise<ApiResponse<T>>
  } else {
    return http[method](url, dataOrConfig, config) as unknown as Promise<ApiResponse<T>>
  }
}

const api = {
  get: <T = any>(url: string, config?: any) => request<T>('get', url, config),
  post: <T = any>(url: string, data?: any, config?: any) => request<T>('post', url, data, config),
  put: <T = any>(url: string, data?: any, config?: any) => request<T>('put', url, data, config),
  delete: <T = any>(url: string, config?: any) => request<T>('delete', url, config),
  patch: <T = any>(url: string, data?: any, config?: any) => request<T>('patch', url, data, config),
}

export const adminAPI = {

  // ============ 商家入驻申请管理 ============

  /**
   * 获取商家入驻申请列表（分页）
   * @param params - 查询参数
   * @param params.page - 页码，默认1
   * @param params.size - 每页数量，默认10
   * @param params.status - 状态筛选（PENDING/APPROVED/REJECTED）
   */
  getApplications: (params: {
    page?: number
    size?: number
    status?: string
  }) => {
    return api.get('/admin/merchant/applications', { params })
  },

  /**
   * 获取申请详情
   * @param id - 申请ID
   */
  getApplicationDetail: (id: number) => {
    return api.get(`/admin/merchant/applications/${id}`)
  },

  /**
   * 审核通过申请
   * @param id - 申请ID
   */
  approveApplication: (id: number) => {
    return api.put(`/admin/merchant/applications/${id}/approve`)
  },

  /**
   * 审核驳回申请
   * @param id - 申请ID
   * @param data - 驳回原因
   */
  rejectApplication: (id: number, data: { reviewNotes: string }) => {
    return api.put(`/admin/merchant/applications/${id}/reject`, data)
  },

  /**
   * 获取申请统计
   */
  getApplicationStats: () => {
    return api.get('/admin/merchant/applications/stats')
  },

  // ============ 商家管理 ============

  /**
   * 获取商家列表（分页）
   * @param params - 查询参数
   * @param params.page - 页码，默认1
   * @param params.size - 每页数量，默认10
   * @param params.keyword - 搜索关键词（店铺名称/联系人）
   * @param params.status - 用户状态筛选（0封禁 1正常）
   */
  getSellers: (params: {
    page?: number
    size?: number
    keyword?: string
    status?: string | number
  }) => {
    return api.get('/admin/sellers', { params })
  },

  /**
   * 获取商家详情
   * @param id - 商家ID
   */
  getSellerDetail: (id: number) => {
    return api.get(`/admin/sellers/${id}`)
  },

  /**
   * 重置商家密码
   * @param id - 商家ID
   */
  resetSellerPassword: (id: number) => {
    return api.put(`/admin/sellers/${id}/reset-password`)
  },

  // ============ 分类管理 ============

  /**
   * 获取分类树
   */
  getCategoryTree: () => {
    return api.get('/admin/categories/tree')
  },

  /**
   * 获取所有分类列表
   */
  getAllCategories: () => {
    return api.get('/admin/categories')
  },

  /**
   * 获取一级分类列表
   */
  getLevel1Categories: () => {
    return api.get('/admin/categories/level1')
  },

  /**
   * 获取子分类列表
   * @param parentId - 父分类ID
   */
  getChildrenCategories: (parentId: number) => {
    return api.get(`/admin/categories/${parentId}/children`)
  },

  /**
   * 获取分类详情
   * @param id - 分类ID
   */
  getCategoryById: (id: number) => {
    return api.get(`/admin/categories/${id}`)
  },

  /**
   * 创建分类
   * @param data - 分类数据
   */
  createCategory: (data: {
    name: string
    parentId: number | null
    sortOrder?: number
    isActive?: boolean
  }) => {
    return api.post('/admin/categories', data)
  },

  /**
   * 更新分类
   * @param id - 分类ID
   * @param data - 更新数据
   */
  updateCategory: (id: number, data: {
    name?: string
    parentId?: number | null
    sortOrder?: number
    isActive?: boolean
  }) => {
    return api.put(`/admin/categories/${id}`, data)
  },

  /**
   * 删除分类
   * @param id - 分类ID
   */
  deleteCategory: (id: number) => {
    return api.delete(`/admin/categories/${id}`)
  },

  /**
   * 切换分类状态（启用/禁用）
   * @param id - 分类ID
   * @param isActive - 是否启用
   */
  toggleCategoryStatus: (id: number, isActive: boolean) => {
    return api.put(`/admin/categories/${id}/status`, { isActive })
  },

  /**
   * 更新分类排序
   * @param id - 分类ID
   * @param sortOrder - 排序序号
   */
  updateCategorySort: (id: number, sortOrder: number) => {
    return api.put(`/admin/categories/${id}/sort`, { sortOrder })
  },

  // ============ 管理员账号管理 ============

  /**
   * 获取管理员列表
   */
  getAdminList: () => {
    return api.get('/admin/admins')
  },

  /**
   * 创建管理员
   * @param data - 管理员数据
   */
  createAdmin: (data: { account: string; password: string }) => {
    return api.post('/admin/admins', data)
  },

  /**
   * 删除管理员
   * @param id - 管理员ID
   */
  deleteAdmin: (id: number) => {
    return api.delete(`/admin/admins/${id}`)
  },

  /**
   * 更新管理员信息
   * @param id - 管理员ID
   * @param data - 更新数据（password, status）
   */
  updateAdmin: (id: number, data: {
    password?: string
    status?: number
  }) => {
    return api.put(`/admin/admins/${id}`, data)
  },

  /**
   * 分配权限
   * @param id - 管理员ID
   * @param roles - 角色列表
   */
  assignRoles: (id: number, roles: string[]) => {
    return api.put(`/admin/admins/${id}/roles`, { roles })
  },

  // ============ Banner管理 ============

  /**
   * 获取Banner列表
   */
  getBannerList: () => {
    return api.get('/admin/banners')
  },

  /**
   * 获取Banner详情
   * @param id - Banner ID
   */
  getBannerById: (id: number) => {
    return api.get(`/admin/banners/${id}`)
  },

  /**
   * 创建Banner
   * @param data - Banner数据
   */
  createBanner: (data: {
    title: string
    imageUrl: string
    linkUrl?: string
    sortOrder?: number
    status?: number
    startTime?: string | null
    endTime?: string | null
    position?: string
  }) => {
    return api.post('/admin/banners', data)
  },

  /**
   * 更新Banner
   * @param id - Banner ID
   * @param data - 更新数据
   */
  updateBanner: (id: number, data: {
    title?: string
    imageUrl?: string
    linkUrl?: string
    sortOrder?: number
    status?: number
    startTime?: string | null
    endTime?: string | null
    position?: string
  }) => {
    return api.put(`/admin/banners/${id}`, data)
  },

  /**
   * 删除Banner
   * @param id - Banner ID
   */
  deleteBanner: (id: number) => {
    return api.delete(`/admin/banners/${id}`)
  },

  /**
   * 切换Banner状态
   * @param id - Banner ID
   * @param status - 状态（1启用，0禁用）
   */
  toggleBannerStatus: (id: number, status: number) => {
    return api.put(`/admin/banners/${id}/status`, { status })
  },

  /**
   * 更新Banner排序
   * @param id - Banner ID
   * @param sortOrder - 排序序号
   */
  updateBannerSort: (id: number, sortOrder: number) => {
    return api.put(`/admin/banners/${id}/sort`, { sortOrder })
  },

  // ============ 商品管理 ============

  /**
   * 获取商品列表（分页）
   * @param params - 查询参数
   * @param params.page - 页码，默认1
   * @param params.size - 每页数量，默认10
   * @param params.keyword - 搜索关键词（商品名称/品牌）
   * @param params.status - 状态筛选（0下架，1上架）
   * @param params.sellerId - 商家ID筛选
   */
  getProducts: (params: {
    page?: number
    size?: number
    keyword?: string
    status?: number
    sellerId?: number
  }) => {
    return api.get('/admin/products', { params })
  },

  /**
   * 获取商品详情
   * @param id - 商品ID
   */
  getProductById: (id: number) => {
    return api.get(`/admin/products/${id}`)
  },

  /**
   * 切换商品状态（上架/下架）
   * @param id - 商品ID
   * @param status - 状态（0下架，1上架）
   */
  toggleProductStatus: (id: number, status: number) => {
    return api.put(`/admin/products/${id}/status`, { status })
  },

  /**
   * 删除商品
   * @param id - 商品ID
   */
  deleteProduct: (id: number) => {
    return api.delete(`/admin/products/${id}`)
  },

  // ============ 用户管理 ============

  /**
   * 获取用户列表（分页）
   * @param params - 查询参数
   * @param params.page - 页码，默认1
   * @param params.size - 每页数量，默认20
   * @param params.keyword - 搜索关键词（账号/昵称）
   * @param params.status - 状态筛选（0禁用，1正常）
   */
  getUsers: (params: {
    page?: number
    pageSize?: number
    keyword?: string
    status?: number
  }) => {
    return api.get('/admin/users', { params })
  },

  /**
   * 获取用户详情
   * @param id - 用户ID
   */
  getUserById: (id: number) => {
    return api.get(`/admin/users/${id}`)
  },

  /**
   * 切换用户状态（启用/禁用）
   * @param id - 用户ID
   * @param status - 状态（0禁用，1正常）
   */
  toggleUserStatus: (id: number, status: number) => {
    return api.put(`/admin/users/${id}/status`, { status })
  },

  resetUserPassword: (id: number) => {
    return api.put(`/admin/users/${id}/reset-password`)
  },

}
