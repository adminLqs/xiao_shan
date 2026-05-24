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

export const authAPI = {

    // ============ 游客 ============

    /**
     * 用户登录
     * @param data - 登录参数
     * @param data.account - 账号
     * @param data.password - 密码
     */
    login: (data: any) => {
        return api.post('/auth/login', data);
    },

    /**
     * 用户注册
     * @param data - 注册参数
     * @param data.account - 账号
     * @param data.password - 密码
     * @param data.confirmPassword - 确认密码
     */
    register: (data: any) => {
        return api.post('/auth/register', data);
    },

    /**
     * 用户登出
     */
    logout: async () => {
        return api.get('/auth/logout')
    },

    // ============ 用户 ============

    /**
     * 获取商品列表（首页/搜索/分类筛选）
     * @param params - 查询参数
     * @param params.page - 页码，默认1
     * @param params.pageSize - 每页数量，默认20
     * @param params.keyword - 搜索关键词
     * @param params.categoryId - 分类ID，不传表示全部
     * @param params.sellerId - 商家ID，不传表示全部
     */
    getProducts: (params: {
        page?: number;
        pageSize?: number;
        keyword?: string;
        categoryId?: number;
        sellerId?: number;
    }) => {
        return api.get('/products', { params })
    },

    /**
     * 获取用户个人信息
     */
    getUserProfile: () => {
        return api.get('/user/profile')
    },

    /**
     * 修改用户头像
     * @param data - FormData对象，包含avatar文件
     */
    updateAvatar: (data: any) => {
        return api.post('/user/avatar', data);
    },

    /**
     * 修改用户信息
     * @param data - 用户信息参数
     * @param data.nickname - 昵称
     * @param data.gender - 性别
     * @param data.birthday - 生日
     */
    updateUserProfile: (data: any) => {
        return api.put('/user/profile', data);
    },

    // ============ 购物车 ============

    /**
     * 添加商品到购物车
     * @param data - 添加参数
     * @param data.productId - 商品 ID
     * @param data.skuId - SKU ID（可选，有规格的商品需要传）
     * @param data.quantity - 购买数量
     */
    addToCart: (data: { productId: number; skuId?: number; quantity: number }) => {
        return api.post('/cart/items', data);
    },

    /**
     * 获取购物车列表
     */
    getCartList: () => {
        return api.get('/cart/items');
    },

    /**
     * 修改购物车商品数量
     * @param data - 修改参数
     * @param data.cartItemId - 购物车项ID
     * @param data.quantity - 新数量
     */
    updateCartItem: (data: { cartItemId: number; quantity: number }) => {
        return api.put('/cart/items', data);
    },

    /**
     * 删除购物车项
     * @param cartItemId - 购物车项ID
     */
    deleteCartItem: (cartItemId: number) => {
        return api.delete(`/cart/items/${cartItemId}`);
    },

    /**
     * 批量删除购物车项
     * @param ids - 购物车项ID数组
     */
    batchDeleteCartItems: (ids: number[]) => {
        return api.delete('/cart/items/batch', { data: { ids } });
    },

    /**
     * 清空购物车
     */
    clearCart: () => {
        return api.delete('/cart/items');
    },

    /**
     * 获取购物车商品数量
     */
    getCartCount: () => {
        return api.get('/cart/count');
    },

    // ============ 收藏 ============

    /**
     * 添加收藏
     * @param productId - 商品ID
     */
    addFavorite: (productId: number) => {
        return api.post('/favorites', { productId })
    },

    /**
     * 取消收藏（根据商品ID）
     * @param productId - 商品ID
     */
    removeFavorite: (productId: number) => {
        return api.delete(`/favorites/product/${productId}`)
    },

    /**
     * 批量取消收藏
     * @param favoriteIds - 收藏ID数组
     */
    batchRemoveFavorites: (favoriteIds: number[]) => {
        return api.delete('/favorites/batch', { data: { ids: favoriteIds } })
    },

    /**
     * 检查是否已收藏
     * @param productId - 商品ID
     */
    checkFavorite: (productId: number) => {
        return api.get('/favorites/check', { params: { productId } })
    },

    /**
     * 获取收藏列表（分页）
     * @param params - 查询参数
     * @param params.page - 页码，默认1
     * @param params.pageSize - 每页数量，默认10
     */
    getFavorites: (params?: { page?: number; pageSize?: number }) => {
        return api.get('/favorites', { params })
    },

    /**
     * 获取收藏数量
     */
    getFavoriteCount: () => {
        return api.get('/favorites/count')
    },

    // ============ 地址管理 ============

    /**
     * 获取用户所有地址
     */
    getAddresses: () => {
        return api.get('/addresses')
    },

    /**
     * 获取地址详情
     * @param addressId - 地址ID
     */
    getAddress: (addressId: number) => {
        return api.get(`/addresses/${addressId}`)
    },

    /**
     * 新增地址
     * @param data - 地址参数
     * @param data.recipientName - 收件人姓名
     * @param data.recipientPhone - 收件人电话
     * @param data.province - 省份
     * @param data.city - 城市
     * @param data.district - 区县
     * @param data.detailAddress - 详细地址
     * @param data.isDefault - 是否默认地址
     */
    addAddress: (data: {
        recipientName: string
        recipientPhone: string
        province: string
        city: string
        district: string
        detailAddress: string
        isDefault: boolean
    }) => {
        return api.post('/addresses', data)
    },

    /**
     * 更新地址
     * @param data - 地址参数
     * @param data.id - 地址ID
     * @param data.recipientName - 收件人姓名
     * @param data.recipientPhone - 收件人电话
     * @param data.province - 省份
     * @param data.city - 城市
     * @param data.district - 区县
     * @param data.detailAddress - 详细地址
     * @param data.isDefault - 是否默认地址
     */
    updateAddress: (data: {
        id: number
        recipientName: string
        recipientPhone: string
        province: string
        city: string
        district: string
        detailAddress: string
        isDefault: boolean
    }) => {
        return api.put(`/addresses/${data.id}`, data)
    },

    /**
     * 删除地址
     * @param addressId - 地址ID
     */
    deleteAddress: (addressId: number) => {
        return api.delete(`/addresses/${addressId}`)
    },

    /**
     * 设置默认地址
     * @param addressId - 地址ID
     */
    setDefaultAddress: (addressId: number) => {
        return api.put(`/addresses/${addressId}/default`)
    },

    // ============ 订单 ============

    /**
     * 创建订单
     * @param data - 订单参数
     * @param data.addressId - 收货地址ID
     * @param data.paymentMethod - 支付方式
     * @param data.orderItems - 商品列表
     * @param data.orderItems[].productId - 商品ID
     * @param data.orderItems[].quantity - 购买数量
     * @param data.orderItems[].skuId - SKU ID
     */
    createOrder: (data: {
        addressId: number
        paymentMethod: string
        orderItems: Array<{ productId: number; quantity: number; skuId?: number }>
        source?: string
    }) => {
        return api.post('/orders', data)
    },

    /**
     * 获取订单列表（分页）
     * @param params - 查询参数
     * @param params.page - 页码，默认1
     * @param params.pageSize - 每页数量，默认10
     * @param params.status - 订单状态筛选
     */
    getOrders: (params: {
        page?: number
        pageSize?: number
        status?: string
    }) => {
        return api.get('/orders', { params })
    },

    /**
     * 获取订单详情
     * @param orderId - 订单ID
     */
    getOrder: (orderId: number) => {
        return api.get(`/orders/${orderId}`)
    },

    /**
     * 获取订单详情（完整版：含地址和商品列表）
     * @param orderId - 订单ID
     */
    getOrderDetail: (orderId: number) => {
        return api.get(`/orders/${orderId}/detail`)
    },

    /**
     * 取消订单
     * @param orderId - 订单ID
     */
    cancelOrder: (orderId: number) => {
        return api.put(`/orders/${orderId}/cancel`)
    },

    /**
     * 确认收货
     * @param orderId - 订单ID
     */
    confirmReceive: (orderId: number) => {
        return api.put(`/orders/${orderId}/confirm`)
    },

    /**
     * 删除订单（软删除）
     * @param orderId - 订单ID
     */
    deleteOrder: (orderId: number) => {
        return api.delete(`/orders/${orderId}`)
    },

    // ============ 退款接口 ============

    /**
     * 申请退款（旧版本）
     * @param orderId - 订单ID
     * @param refundReason - 退款原因
     */
    applyRefund: (orderId: number, refundReason: string) => {
        return api.post(`/orders/${orderId}/refund`, { refundReason })
    },

    /**
     * 提交退款申请（新版本，支持订单项级别）
     * @param orderId - 订单ID
     * @param data - 退款参数
     * @param data.orderItemId - 订单项ID
     * @param data.refundType - 退款类型（REFUND/AFTER_SALE）
     * @param data.refundReason - 退款原因
     * @param data.refundAmount - 退款金额
     * @param data.description - 退款描述
     * @param data.evidenceImages - 证据图片（逗号分隔）
     */
    submitRefund: (orderId: number, data: {
        orderItemId: number
        refundType: 'REFUND' | 'AFTER_SALE'
        refundReason: string
        refundAmount: number
        description?: string
        evidenceImages?: string
    }) => {
        return api.post(`/orders/${orderId}/refund`, data)
    },

    /**
     * 提交退款申请（FormData格式，支持图片和视频）
     * POST /api/v1/refunds/submit/{orderItemId}
     * @param orderItemId - 订单项ID
     * @param formData - FormData对象，包含：refundType、refundReason、refundAmount、description、images[]、videos[]
     */
    submitRefundFormData: (orderItemId: number, formData: FormData) => {
        return api.post(`/refunds/submit/${orderItemId}`, formData, {
            headers: {
                'Content-Type': 'multipart/form-data'
            }
        })
    },

    /**
     * 获取用户的退款记录列表
     */
    getUserRefunds: (page: number = 1, pageSize: number = 10) => {
        return api.get('/user/refunds', { params: { page, pageSize } })
    },

    /**
     * 获取用户进行中的退款数量
     */
    getPendingRefundCount: () => {
        return api.get('/user/refunds/pending-count')
    },

    /**
     * 获取订单的退款记录
     * @param orderId - 订单ID
     */
    getOrderRefunds: (orderId: number) => {
        return api.get(`/orders/${orderId}/refunds`)
    },

    /**
     * 获取用户收货地址列表
     * GET /api/v1/user/addresses
     */
    getAddressList: () => {
        return api.get('/user/addresses')
    },

    /**
     * 查询退款状态
     * @param orderId - 订单ID
     */
    getRefundStatus: (orderId: number) => {
        return api.get(`/orders/${orderId}/refund/status`)
    },

    /**
     * 获取退款详情
     * @param refundId - 退款ID
     */
    getRefundDetail: (refundId: number) => {
        return api.get(`/refunds/${refundId}`)
    },

    // ============ 商家退款处理接口 ============

    /**
     * 获取商家待处理的退款列表
     */
    getSellerPendingRefunds: () => {
        return api.get('/seller/refunds')
    },

    /**
     * 商家同意退款
     * POST /api/v1/seller/refunds/{refundId}/approve
     * @param refundId - 退款ID
     * @param notes - 审核备注
     */
    approveRefund: (refundId: number, notes?: string) => {
        return api.post(`/seller/refunds/${refundId}/approve`, { notes: notes || '' })
    },

    /**
     * 商家拒绝退款
     * @param orderId - 订单ID
     * @param refundId - 退款ID
     * @param reviewNotes - 拒绝原因
     */
    rejectRefund: (orderId: number, refundId: number, reviewNotes: string) => {
        return api.put(`/seller/orders/${orderId}/refund/${refundId}/reject`, { reviewNotes })
    },

    /**
     * 获取退货信息
     * GET /api/v1/refunds/{refundId}/return-info
     * @param refundId - 退款ID
     */
    getReturnInfo: (refundId: number) => {
        return api.get(`/refunds/${refundId}/return-info`)
    },

    /**
     * 提交退货信息
     * POST /api/v1/refunds/{refundId}/return-submit
     * @param refundId - 退款ID
     * @param data.returnMethod - 退货方式（PICKUP/SELF）
     * @param data.addressId - 用户地址ID（上门取件时必填）
     * @param data.pickupDate - 取件日期（上门取件时必填）
     * @param data.pickupTime - 取件时间段（上门取件时必填）
     * @param data.returnTrackingNumber - 退货单号（自寄时必填）
     * @param data.returnLogisticsName - 退货物流公司（自寄时必填）
     */
    submitReturn: (refundId: number, data: {
        returnMethod: 'PICKUP' | 'SELF'
        addressId?: number
        pickupDate?: string
        pickupTime?: string
        returnTrackingNumber?: string
        returnLogisticsName?: string
    }) => {
        return api.post(`/refunds/${refundId}/return-submit`, data)
    },

    /**
     * 取消取件预约
     * POST /api/v1/refunds/cancel-pickup
     * @param data - 取消参数
     * @param data.refundId - 退款ID
     * @param data.logisticCode - 物流单号
     * @param data.shipperCode - 快递公司编码
     * @param data.orderCode - 订单编号
     */
    cancelPickupOrder: (data: {
        refundId: string
        logisticCode: string
        shipperCode: string
        orderCode: string
    }) => {
        return api.post('/refunds/cancel-pickup', data)
    },

    // ============ 商家套餐接口 ============

    /**
     * 获取所有可用套餐
     */
    getPackages: () => {
        return api.get('/seller/packages')
    },

    /**
     * 获取套餐详情
     * @param packageId - 套餐ID
     */
    getPackageDetail: (packageId: number) => {
        return api.get(`/seller/packages/${packageId}`)
    },

    /**
     * 获取当前套餐信息和使用情况
     */
    getCurrentPackage: () => {
        return api.get('/seller/packages/current')
    },

    /**
     * 购买套餐
     * @param packageId - 套餐ID
     */
    purchasePackage: (packageId: number) => {
        return api.post(`/seller/packages/${packageId}/buy`)
    },

    /**
     * 检查发布商品权限
     */
    checkPublishPermission: () => {
        return api.get('/seller/packages/check-publish')
    },

    /**
     * 获取套餐购买历史
     */
    getPackageHistory: () => {
        return api.get('/seller/packages/history')
    },

    /**
     * 获取套餐订单状态
     * @param orderId - 订单ID
     */
    getPackageOrderStatus: (orderId: number) => {
        return api.get(`/seller/packages/orders/${orderId}`)
    },

    /**
     * 检查是否有有效套餐
     */
    checkActivePackage: () => {
        return api.get('/seller/packages/status')
    },

    /**
     * 获取订单状态统计（各状态数量）
     */
    getOrderCounts: () => {
        return api.get('/orders/counts')
    },

    /**
     * 支付订单
     * @param orderId - 订单ID
     */
    payOrder: (orderId: number) => {
        return api.post(`/orders/${orderId}/pay`)
    },

    // ============ 订单项接口 =============
    /**
     * 获取订单项详情
     * @param orderItemId - 订单项ID
     */
    getOrderItemDetail: (orderItemId: number) => {
        return api.get(`/orderItems/${orderItemId}`)
    },

    // ============ 退款/售后接口 ============
    /**
     * 根据订单项ID获取退款记录
     * @param orderItemId - 订单项ID
     */
    getRefundByOrderItemId: (orderItemId: number) => {
        return api.get(`/refunds/order-item/${orderItemId}`)
    },

    /**
     * 提交申诉
     * @param refundId - 退款ID
     * @param appealContent - 申诉内容
     * @param evidenceImages - 申诉凭证图片（逗号分隔）
     */
    submitAppeal: (refundId: number, appealContent: string, evidenceImages: string) => {
        return api.put(`/refunds/${refundId}/appeal`, {
            appealContent,
            evidenceImages
        })
    },

    // ============ 退款聊天接口 ============

    /**
     * 获取退款聊天记录
     * @param refundId - 退款ID
     */
    getRefundChatHistory: (refundId: number) => {
        return api.get(`/refunds/${refundId}/chat`)
    },

    /**
     * 发送退款聊天消息
     * @param refundId - 退款ID
     * @param data - 消息数据
     * @param data.message - 消息内容
     * @param data.images - 图片（逗号分隔）
     */
    sendRefundMessage: (refundId: number, data: { message: string; images?: string }) => {
        return api.post(`/refunds/${refundId}/chat/send`, data)
    },

    /**
     * 获取退款详情（含聊天记录）
     * @param refundId - 退款ID
     */
    getRefundDetailWithChat: (refundId: number) => {
        return api.get(`/refunds/${refundId}/detail-with-chat`)
    },

    /**
     * 商家回复退款申请
     * @param refundId - 退款ID
     * @param response - 回复内容
     * @param images - 图片证据（逗号分隔）
     */
    sellerRespondRefund: (refundId: number, response: string, images?: string) => {
        return api.put(`/seller/refunds/${refundId}/respond`, { response, images })
    },

    /**
     * 商家确认收货
     * POST /api/v1/seller/refunds/{refundId}/confirm-receive
     * @param refundId - 退款ID
     */
    sellerConfirmReturn: (refundId: number) => {
        return api.post(`/seller/refunds/${refundId}/confirm-receive`)
    },

    /**
     * 商家查询退货物流信息
     * GET /api/v1/seller/refunds/{refundId}/logistics
     * @param refundId - 退款ID
     */
    sellerQueryReturnLogistics: (refundId: number) => {
        return api.get(`/seller/refunds/${refundId}/logistics`)
    },

    // ============ 评论接口 ============
    /**
     * 提交商品评论（旧版本）
     * @param data - 评论数据
     * @param data.orderItemId - 订单项ID
     * @param data.rating - 评分（1-5）
     * @param data.comment - 评论内容
     * @param images - 评论图片列表（可选，最多9张）
     */
    submitReview: (data: { orderItemId: number; rating: number; comment: string }, images?: File[]) => {
        const formData = new FormData();
        formData.append('review', new Blob([JSON.stringify(data)], { type: 'application/json' }));
        if (images && images.length > 0) {
            images.forEach(img => formData.append('images', img));
        }
        return api.post('/reviews', formData);
    },

    /**
     * 提交商品评论（FormData格式，支持图片和视频）
     * POST /api/v1/reviews (multipart/form-data)
     * @param formData - FormData对象，包含：orderItemId、rating、comment、images[]、video
     */
    submitReviewFormData: (formData: FormData) => {
        return api.post('/reviews', formData, {
            headers: {
                'Content-Type': 'multipart/form-data'
            }
        });
    },

    /**
     * 获取订单项评价信息
     * @param orderItemId - 订单项ID
     */
    getReviewByOrderItemId: (orderItemId: number) => {
        return api.get(`/reviews/orderItem/${orderItemId}`)
    },

    /**
     * 获取商品评价列表
     * @param params - 查询参数
     * @param params.productId - 商品ID
     * @param params.page - 页码
     * @param params.pageSize - 每页数量
     * @param params.rating - 评分筛选
     */
    getProductReviews: (params: {
        productId: number;
        page?: number;
        pageSize?: number;
        rating?: number;
    }) => {
        return api.get(`/products/${params.productId}/reviews`, { params })
    },

    /**
     * 获取商品评价统计
     * @param productId - 商品ID
     */
    getReviewStatistics: (productId: number) => {
        return api.get(`/products/${productId}/reviews/statistics`)
    },

    /**
     * 获取用户评价列表
     * @param params - 查询参数
     * @param params.page - 页码
     * @param params.pageSize - 每页数量
     */
    getUserReviews: (params?: { page?: number; pageSize?: number }) => {
        return api.get('/reviews/user', { params })
    },

    /**
     * 获取待评价订单项列表
     * @param params - 查询参数
     * @param params.page - 页码
     * @param params.pageSize - 每页数量
     */
    getPendingReviews: (params?: { page?: number; pageSize?: number }) => {
        return api.get('/reviews/pending', { params })
    },

    /**
     * 获取评价相关订单项列表
     * GET /api/v1/orders/review-items?type=pending|reviewed
     * @param params - 查询参数
     * @param params.type - 类型（pending: 待评价, reviewed: 已评价）
     * @param params.page - 页码
     * @param params.pageSize - 每页数量
     */
    getReviewItems: (params: { type: 'pending' | 'reviewed'; page?: number; pageSize?: number }) => {
        return api.get('/orders/review-items', { params })
    },

    /**
     * 获取评论图片列表
     * @param reviewId - 评论ID
     */
    getReviewImages: (reviewId: number) => {
        return api.get(`/reviews/${reviewId}/images`)
    },

    /**
     * 删除评论图片
     * @param imageId - 图片ID
     */
    deleteReviewImage: (imageId: number) => {
        return api.delete(`/reviews/images/${imageId}`)
    },

    /**
     * 更新评论
     * @param data - FormData对象
     * @param data.reviewData - JSON字符串（包含reviewId、comment、rating、deleteImageIds）
     * @param data.images - 新增图片文件（可选）
     */
    updateReview: (data: FormData) => {
        return api.put('/reviews', data)
    },

    /**
     * 商家回复评论
     * @param reviewId - 评论ID
     * @param data - 回复内容
     * @param data.reply - 回复内容
     */
    sellerReplyReview: (reviewId: number, data: { reply: string }) => {
        return api.post(`/reviews/${reviewId}/reply`, data)
    },

    /**
     * 删除商家回复
     * @param reviewId - 评论ID
     */
    deleteSellerReply: (reviewId: number) => {
        return api.delete(`/reviews/${reviewId}/reply`)
    },

    /**
     * 获取待回复评论列表（商家端）
     * @param params - 查询参数
     * @param params.page - 页码
     * @param params.pageSize - 每页数量
     */
    getPendingReplyReviews: (params?: { page?: number; pageSize?: number }) => {
        return api.get('/seller/reviews/pending', { params })
    },

    /**
     * 获取商品评价标签
     * @param productId - 商品ID
     */
    getReviewTags: (productId: number) => {
        return api.get(`/reviews/product/${productId}/tags`)
    },


    // ============ 用户物流 ============
    /**
     * 用户端获取物流信息
     * GET /api/v1/orders/{orderId}/logistics
     *
     * @param orderId - 订单ID
     * @returns Promise
     */
    getUserLogisticsInfo: (orderId: number) => {
        return api.get(`/orders/${orderId}/logistics`)
    },

    /**
     * 用户端刷新物流信息
     * POST /api/v1/orders/{orderId}/logistics/refresh
     *
     * @param orderId - 订单ID
     * @returns Promise
     */
    refreshUserLogistics: (orderId: number) => {
        return api.post(`/orders/${orderId}/logistics/refresh`)
    },

    /**
     * 根据物流单号查询物流信息
     * GET /api/v1/logistics/query?trackingNumber=xxx&logisticsName=xxx&refundId=xxx
     *
     * @param trackingNumber - 物流单号
     * @param logisticsName - 物流公司名称（可选）
     * @param refundId - 退款ID（可选，用于退货物流）
     * @returns Promise
     */
    getLogisticsByTrackingNumber: (trackingNumber: string, logisticsName?: string, refundId?: string) => {
        return api.get('/logistics/query', {
            params: {
                trackingNumber,
                logisticsName: logisticsName || '',
                refundId: refundId || ''
            }
        })
    },


    // ============ 结算页 ============

    /**
     * 购物车结算：根据购物车项ID列表获取商品信息
     * @param data - 请求参数
     * @param data.ids - 购物车项ID数组
     */
    getCheckoutItemsFromCart: (data: { ids: string[] }) => {
        return api.post('/checkout/cart', data)
    },

    /**
     * 立即购买：根据商品ID和数量获取商品信息
     * @param data - 请求参数
     * @param data.productId - 商品ID
     * @param data.quantity - 购买数量
     * @param data.skuId - SKU ID（可选）
     */
    getCheckoutItemsFromProduct: (data: { productId: number; quantity: number; skuId?: number }) => {
        return api.post('/checkout/product', data)
    },

    /**
     * 根据购物车项ID列表获取商品信息
     * @param ids - 购物车项ID数组
     */
    getCartItemsByIds: (ids: string[]) => {
        return api.post('/cart/items/batch', { ids })
    },

    // ============ 商家入驻 ============

    /**
     * 获取商品详情
     * @param productId - 商品ID
     */
    getProduct: (productId: number) => {
        return api.get(`/products/${productId}`)
    },

    /**
     * 获取商品SKU列表（用户端）
     * @param productId - 商品ID
     * @param params - 可选的查询参数
     */
    getProductSkus: (productId: number, params?: Record<string, any>) => {
        return api.get(`/products/${productId}/skus`, { params })
    },

    /**
     * 获取商品SKU列表（商家端，从数据库读取）
     * @param productId - 商品ID
     */
    getSellerProductSkus: (productId: number) => {
        return api.get(`/seller/products/${productId}/skus`)
    },

    /**
     * 获取商品参数列表
     * @param productId - 商品ID
     */
    getProductParams: (productId: number) => {
        return api.get(`/products/${productId}/params`)
    },

    /**
     * 获取商品质量数据统计
     * @param productId - 商品ID
     */
    getProductQualityStats: (productId: number) => {
        return api.get(`/products/${productId}/quality-stats`)
    },

    /**
     * 提交商家入驻申请
     * @param data - FormData对象，包含入驻申请信息
     */
    submitMerchantApplication: (data: FormData) => {
        return api.post('/merchant/applications', data);
    },

    /**
     * 查询入驻申请状态
     */
    getMerchantApplicationStatus: () => {
        return api.get('/merchant/apply/status');
    },

    // ============ 商家商品管理 ============

    /**
     * 添加商品
     * @param data - FormData对象，包含商品信息和图片
     */
    addProduct: (data: any) => {
        return api.post('/seller/products', data);
    },

    /**
     * 删除商品
     * @param productId - 商品ID
     */
    deleteProduct: (productId: number) => {
        return api.delete(`/seller/products/${productId}`)
    },

    /**
     * 批量删除商品
     * @param productIds - 商品ID数组
     */
    batchDeleteProducts: (productIds: number[]) => {
        return api.delete('/seller/products/batch', { data: { productIds } })
    },

    /**
     * 更新商品
     * @param productId - 商品ID
     * @param data - 商品数据
     */
    updateProduct: (productId: number, data: any) => {
        return api.put(`/seller/products/${productId}`, data)
    },

    /**
     * 获取商家商品列表（带状态筛选）
     * @param params - 查询参数
     * @param params.page - 页码
     * @param params.pageSize - 每页数量
     * @param params.keyword - 搜索关键词
     * @param params.status - 商品状态
     */
    getSellerProducts: (params: {
        page?: number;
        pageSize?: number;
        keyword?: string;
        status?: number
    }) => {
        return api.get('/seller/products', { params })
    },

    /**
     * 修改商品状态
     * @param productId - 商品ID
     * @param status - 状态值（0-下架，1-上架）
     */
    updateProductStatus: (productId: number, status: number) => {
        return api.patch(`/seller/products/${productId}`, { status });
    },

    // ============ 商家数据分析 ===========
    /**
     * 获取商家数据分析
     * @param params - 查询参数
     * @param params.days - 统计天数 (7/30/90)
     */
    getSellerAnalytics: (params: { days: number }) => {
        return api.get('/seller/analytics', { params })
    },


    // ============ 商家信息管理 ============

    /**
     * 获取商家个人资料
     */
    getSellerProfile: () => {
        return api.get('/seller/profile')
    },

    /**
     * 根据商品ID查询商家信息
     * GET /api/v1/products/{productId}/seller
     * @param productId - 商品ID
     */
    getSellerByProduct: (productId: number) => {
        return api.get(`/products/${productId}/seller`)
    },

    /**
     * 获取商品优惠券列表
     * GET /api/v1/products/{productId}/coupons
     * @param productId - 商品ID
     */
    getProductCoupons: (productId: number) => {
        return api.get(`/products/${productId}/coupons`)
    },

    /**
     * 获取用户优惠券列表
     * GET /api/v1/user/coupons
     */
    getUserCoupons: () => {
        return api.get('/user/coupons')
    },

    /**
     * 关注商家
     * POST /api/v1/seller/{sellerId}/follow
     * @param sellerId - 商家ID
     */
    followSeller: (sellerId: number) => {
        return api.post(`/seller/${sellerId}/follow`)
    },

    /**
     * 取消关注商家
     * DELETE /api/v1/seller/{sellerId}/follow
     * @param sellerId - 商家ID
     */
    unfollowSeller: (sellerId: number) => {
        return api.delete(`/seller/${sellerId}/follow`)
    },

    /**
     * 检查是否已关注商家
     * GET /api/v1/seller/{sellerId}/follow/check
     * @param sellerId - 商家ID
     */
    checkFollowSeller: (sellerId: number) => {
        return api.get(`/seller/${sellerId}/follow/check`)
    },

    /**
     * 根据商家ID获取商家信息（公开访问）
     * GET /api/v1/seller/info/{sellerId}
     * @param sellerId - 商家ID
     */
    getSellerInfoById: (sellerId: number) => {
        return api.get(`/seller/info/${sellerId}`)
    },

    /**
     * 获取商家收货地址
     * GET /api/v1/seller/{sellerId}/address
     * @param sellerId - 商家ID
     * @returns Promise<{ address: string; contactPhone: string }>
     */
    getSellerAddress: (sellerId: number) => {
        return api.get(`/seller/${sellerId}/address`)
    },

    /**
     * 获取商家资质信息
     * GET /api/v1/seller/{userId}/qualification
     * @param userId - 商家用户ID
     */
    getSellerQualification: (userId: number) => {
        return api.get(`/seller/${userId}/qualification`)
    },

    /**
     * 获取商家评价列表（公开访问）
     * GET /api/v1/seller/{sellerId}/reviews?page=1&pageSize=10
     * @param sellerId - 商家ID
     * @param params - 查询参数
     * @param params.page - 页码
     * @param params.pageSize - 每页数量
     */
    getSellerReviews: (sellerId: number, params?: { page?: number; pageSize?: number }) => {
        return api.get(`/seller/${sellerId}/reviews`, { params })
    },

    /**
     * 更新商家资料（统一接口：支持基本信息 + 头像 + 横幅）
     * @param data - FormData对象
     * @param data.storeInfo - 基本信息JSON字符串（包含storeName、storeDetail、businessHours、contactPhone）
     * @param data.avatar - 店铺头像文件（可选）
     * @param data.banner - 店铺横幅文件（可选）
     * @returns Promise
     */
    updateSellerProfile: (data: FormData) => {
        return api.put('/seller/profile', data)
    },

    // ============ 商家订单管理 ============

    /**
     * 获取商家订单列表（分页）
     *
     * @param params - 查询参数
     * @param params.page - 页码，默认1
     * @param params.pageSize - 每页数量，默认10
     * @param params.status - 订单状态筛选（PENDING/PAID/PROCESSING/SHIPPED/COMPLETED/CANCELLED/REFUNDED）
     * @returns Promise
     */
    getSellerOrders: (params: {
        page?: number;
        pageSize?: number;
        status?: string;
    }) => {
        return api.get('/seller/orders', { params })
    },

    /**
     * 获取商家订单详情（含地址和商品列表）
     *
     * @param orderId - 订单ID
     * @returns Promise
     */
    getSellerOrderDetail: (orderId: number) => {
        return api.get(`/seller/orders/${orderId}`)
    },

    /**
     * 商家处理订单（PAID → PROCESSING）
     *
     * @param orderId - 订单ID
     * @returns Promise
     */
    processOrder: (orderId: number) => {
        return api.put(`/seller/orders/${orderId}/process`)
    },

    /**
     * 商家发货（PROCESSING → SHIPPED）
     *
     * @param orderId - 订单ID
     * @param data - 发货参数
     * @param data.trackingNumber - 物流单号
     * @param data.logisticsCode - 物流公司代码
     * @param data.logisticsName - 物流公司名称
     * @returns Promise
     */
    shipOrder: (orderId: number, data: {
        trackingNumber: string;
        logisticsCode: string;
        logisticsName: string;
    }) => {
        return api.put(`/seller/orders/${orderId}/ship`, data)
    },

    /**
     * 商家取消订单
     *
     * @param orderId - 订单ID
     * @param reason - 取消原因（可选）
     * @returns Promise
     */
    sellerCancelOrder: (orderId: number, reason?: string) => {
        return api.put(`/seller/orders/${orderId}/cancel`, { reason })
    },

    /**
     * 获取物流信息
     *
     * @param orderId - 订单ID
     * @returns Promise
     */
    getLogisticsInfo: (orderId: number) => {
        return api.get(`/seller/orders/${orderId}/logistics`)
    },

    /**
     * 刷新物流信息
     *
     * @param orderId - 订单ID
     * @returns Promise
     */
    refreshLogistics: (orderId: number) => {
        return api.post(`/seller/orders/${orderId}/logistics/refresh`)
    },

    // ============ 消息中心 ============

    /**
     * 获取消息列表
     * @returns Promise<ApiResponse<{ messages: MessageItem[] }>>
     */
    getMessages: () => {
        return api.get<{ messages: any }>('/messages')
    },

    /**
     * 清空所有消息
     * @returns Promise<ApiResponse<void>>
     */
    clearMessages: () => {
        return api.delete('/messages')
    },

    // ============ 公共接口 ============

    /**
     * 刷新token
     * @param refreshToken - 刷新令牌
     */
    refreshToken: (refreshToken: string) => {
        return api.post('/auth/refresh', { refreshToken });
    },

    /**
     * 获取账号信息（角色、状态）
     */
    getAccountProfile: () => {
        return api.get('/account/profile')
    },

    /**
     * 获取所有分类
     */
    getAllCategories: () => {
        return api.get('/categories')
    },

    /**
     * 发送退款消息（带文件）
     * @param refundId - 退款ID
     * @param formData - 包含消息和图片的FormData
     */
    sendRefundMessageWithFiles: (refundId: number, formData: FormData) => {
        return api.post(`/refunds/${refundId}/messages`, formData, {
            headers: {
                'Content-Type': 'multipart/form-data'
            }
        })
    },

    /**
     * 上传文件
     * @param formData - FormData对象，包含文件
     */
    uploadFile: (formData: FormData) => {
        return api.post('/upload', formData, {
            headers: {
                'Content-Type': 'multipart/form-data'
            }
        })
    },

}
