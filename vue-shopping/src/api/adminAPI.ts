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
   * 切换商家账号状态（封禁/解封）
   * @param userId - 用户ID
   * @param status - 状态 (1: 正常, 0: 封禁)
   */
  toggleUserStatus: (userId: number, status: number) => {
    return api.put(`/admin/users/${userId}/status`, { status })
  },

  /**
   * 重置商家密码
   * @param id - 商家ID
   */
  resetSellerPassword: (id: number) => {
    return api.put(`/admin/sellers/${id}/reset-password`)
  },

}
