import http from '@/utils/axios-config'

interface ApiResponse<T = any> {
  success: boolean
  data: T
  message?: string
}

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
}

export interface PackageInfo {
  id: number
  name: string
  price: number
  durationDays: number
  productLimit: number
  features: string[]
  description: string
}

export interface CurrentPackage {
  packageId: number
  packageName: string
  startDate: string
  endDate: string
  status: string
}

export const sellerAPI = {
  /**
   * 获取套餐列表
   */
  getPackages: () => {
    return api.get<PackageInfo[]>('/seller/packages')
  },

  /**
   * 获取当前生效套餐
   */
  getCurrentPackage: () => {
    return api.get<CurrentPackage>('/seller/packages/current')
  },

  /**
   * 购买套餐
   * @param packageId - 套餐ID
   */
  buyPackage: (packageId: number) => {
    return api.post<string>(`/seller/packages/${packageId}/buy`)
  },
}
