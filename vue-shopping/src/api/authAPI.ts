import http from '@/utils/axios-config'

export const authAPI = {

    // ============ 游客 ============

    /**
     * 用户登录
     * @param data - 登录参数
     * @param data.account - 账号
     * @param data.password - 密码
     */
    login: (data: any) => {
        return http.post('/auth/login', data);
    },

    /**
     * 用户注册
     * @param data - 注册参数
     * @param data.account - 账号
     * @param data.password - 密码
     * @param data.confirmPassword - 确认密码
     */
    register: (data: any) => {
        return http.post('/auth/register', data);
    },

    /**
     * 用户登出
     */
    logout: async () => {
        return http.get('/auth/logout')
    },

    // ============ 用户 ============

    /**
     * 获取商品列表（首页/搜索/分类筛选）
     * @param params - 查询参数
     * @param params.page - 页码，默认1
     * @param params.pageSize - 每页数量，默认20
     * @param params.keyword - 搜索关键词
     * @param params.categoryId - 分类ID，不传表示全部
     */
    getProducts: (params: {
        page?: number;
        pageSize?: number;
        keyword?: string;
        categoryId?: number;
    }) => {
        return http.get('/products', { params })
    },

    /**
     * 获取用户个人信息
     */
    getUserProfile: () => {
        return http.get('/user/profile')
    },

    /**
     * 修改用户头像
     * @param data - FormData对象，包含avatar文件
     */
    updateAvatar: (data: any) => {
        return http.post('/user/avatar', data);
    },

    /**
     * 修改用户信息
     * @param data - 用户信息参数
     * @param data.nickname - 昵称
     * @param data.gender - 性别
     * @param data.birthday - 生日
     */
    updateUserProfile: (data: any) => {
        return http.put('/user/profile', data);
    },

    // ============ 购物车 ============

    /**
     * 添加商品到购物车
     * @param data - 添加参数
     * @param data.productId - 商品ID
     * @param data.quantity - 购买数量
     */
    addToCart: (data: { productId: number; quantity: number }) => {
        return http.post('/cart/items', data);
    },

    /**
     * 获取购物车列表
     */
    getCartList: () => {
        return http.get('/cart/items');
    },

    /**
     * 修改购物车商品数量
     * @param data - 修改参数
     * @param data.cartItemId - 购物车项ID
     * @param data.quantity - 新数量
     */
    updateCartItem: (data: { cartItemId: number; quantity: number }) => {
        return http.put('/cart/items', data);
    },

    /**
     * 删除购物车项
     * @param cartItemId - 购物车项ID
     */
    deleteCartItem: (cartItemId: number) => {
        return http.delete(`/cart/items/${cartItemId}`);
    },

    /**
     * 批量删除购物车项
     * @param ids - 购物车项ID数组
     */
    batchDeleteCartItems: (ids: number[]) => {
        return http.delete('/cart/items/batch', { data: { ids } });
    },

    /**
     * 清空购物车
     */
    clearCart: () => {
        return http.delete('/cart/items');
    },

    /**
     * 获取购物车商品数量
     */
    getCartCount: () => {
        return http.get('/cart/count');
    },

    // ============ 收藏 ============

    /**
     * 添加收藏
     * @param productId - 商品ID
     */
    addFavorite: (productId: number) => {
        return http.post('/favorites', { productId })
    },

    /**
     * 取消收藏
     * @param favoriteId - 收藏ID
     */
    removeFavorite: (favoriteId: number) => {
        return http.delete(`/favorites/${favoriteId}`)
    },

    /**
     * 批量取消收藏
     * @param favoriteIds - 收藏ID数组
     */
    batchRemoveFavorites: (favoriteIds: number[]) => {
        return http.delete('/favorites/batch', { data: { ids: favoriteIds } })
    },

    /**
     * 检查是否已收藏
     * @param productId - 商品ID
     */
    checkFavorite: (productId: number) => {
        return http.get('/favorites/check', { params: { productId } })
    },

    /**
     * 获取收藏列表（分页）
     * @param params - 查询参数
     * @param params.page - 页码，默认1
     * @param params.pageSize - 每页数量，默认10
     */
    getFavorites: (params?: { page?: number; pageSize?: number }) => {
        return http.get('/favorites', { params })
    },

    /**
     * 获取收藏数量
     */
    getFavoriteCount: () => {
        return http.get('/favorites/count')
    },

    // ============ 地址管理 ============

    /**
     * 获取用户所有地址
     */
    getAddresses: () => {
        return http.get('/addresses')
    },

    /**
     * 获取地址详情
     * @param addressId - 地址ID
     */
    getAddress: (addressId: number) => {
        return http.get(`/addresses/${addressId}`)
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
        return http.post('/addresses', data)
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
        return http.put(`/addresses/${data.id}`, data)
    },

    /**
     * 删除地址
     * @param addressId - 地址ID
     */
    deleteAddress: (addressId: number) => {
        return http.delete(`/addresses/${addressId}`)
    },

    /**
     * 设置默认地址
     * @param addressId - 地址ID
     */
    setDefaultAddress: (addressId: number) => {
        return http.put(`/addresses/${addressId}/default`)
    },

    // ============ 订单 ============

    /**
     * 创建订单
     * @param data - 订单参数
     * @param data.addressId - 收货地址ID
     * @param data.paymentMethod - 支付方式
     * @param data.items - 商品列表
     * @param data.items[].productId - 商品ID
     * @param data.items[].quantity - 购买数量
     */
    createOrder: (data: {
        addressId: number
        paymentMethod: string
        items: Array<{ productId: number; quantity: number }>
    }) => {
        return http.post('/orders', data)
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
        return http.get('/orders', { params })
    },

    /**
     * 获取订单详情
     * @param orderId - 订单ID
     */
    getOrder: (orderId: number) => {
        return http.get(`/orders/${orderId}`)
    },

    /**
     * 获取订单详情（完整版：含地址和商品列表）
     * @param orderId - 订单ID
     */
    getOrderDetail: (orderId: number) => {
        return http.get(`/orders/${orderId}/detail`)
    },

    /**
     * 取消订单
     * @param orderId - 订单ID
     */
    cancelOrder: (orderId: number) => {
        return http.put(`/orders/${orderId}/cancel`)
    },

    /**
     * 确认收货
     * @param orderId - 订单ID
     */
    confirmReceive: (orderId: number) => {
        return http.put(`/orders/${orderId}/confirm`)
    },

    /**
     * 删除订单（软删除）
     * @param orderId - 订单ID
     */
    deleteOrder: (orderId: number) => {
        return http.delete(`/orders/${orderId}`)
    },

    /**
     * 获取订单状态统计（各状态数量）
     */
    getOrderCounts: () => {
        return http.get('/orders/counts')
    },

    /**
     * 支付订单
     * @param orderId - 订单ID
     */
    payOrder: (orderId: number) => {
        return http.post(`/orders/${orderId}/pay`)
    },

    // ============ 订单项接口 =============
    /**
     * 获取订单项详情
     * @param orderItemId - 订单项ID
     */
    getOrderItemDetail: (orderItemId: number) => {
        return http.get(`/orderItems/${orderItemId}`)
    },
    
    // ============ 评论接口 ============
    /**
     * 提交商品评论
     * @param data - FormData对象
     * @param data.reviewData - JSON字符串（包含orderItemId、rating、comment、isAnonymous）
     * @param data.images - 图片文件（可选，多张）
     */
    submitReview: (data: FormData) => {
        return http.post('/reviews', data)
    },

    /**
     * 获取订单项评价信息
     * @param orderItemId - 订单项ID
     */
    getReviewByOrderItemId: (orderItemId: number) => {
        return http.get(`/reviews/orderItem/${orderItemId}`)
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
        return http.get(`/products/${params.productId}/reviews`, { params })
    },

    /**
     * 获取商品评价统计
     * @param productId - 商品ID
     */
    getReviewStatistics: (productId: number) => {
        return http.get(`/products/${productId}/reviews/statistics`)
    },

    /**
     * 获取用户评价列表
     * @param params - 查询参数
     * @param params.page - 页码
     * @param params.pageSize - 每页数量
     */
    getUserReviews: (params?: { page?: number; pageSize?: number }) => {
        return http.get('/reviews/user', { params })
    },

    /**
     * 获取待评价订单项列表
     * @param params - 查询参数
     * @param params.page - 页码
     * @param params.pageSize - 每页数量
     */
    getPendingReviews: (params?: { page?: number; pageSize?: number }) => {
        return http.get('/reviews/pending', { params })
    },

    /**
     * 获取评论图片列表
     * @param reviewId - 评论ID
     */
    getReviewImages: (reviewId: number) => {
        return http.get(`/reviews/${reviewId}/images`)
    },

    /**
     * 删除评论图片
     * @param imageId - 图片ID
     */
    deleteReviewImage: (imageId: number) => {
        return http.delete(`/reviews/images/${imageId}`)
    },

    /**
     * 更新评论
     * @param data - FormData对象
     * @param data.reviewData - JSON字符串（包含reviewId、comment、rating、deleteImageIds）
     * @param data.images - 新增图片文件（可选）
     */
    updateReview: (data: FormData) => {
        return http.put('/reviews', data)
    },

    /**
     * 商家回复评论
     * @param reviewId - 评论ID
     * @param data - 回复内容
     * @param data.reply - 回复内容
     */
    sellerReplyReview: (reviewId: number, data: { reply: string }) => {
        return http.post(`/reviews/${reviewId}/reply`, data)
    },

    /**
     * 删除商家回复
     * @param reviewId - 评论ID
     */
    deleteSellerReply: (reviewId: number) => {
        return http.delete(`/reviews/${reviewId}/reply`)
    },

    /**
     * 获取待回复评论列表（商家端）
     * @param params - 查询参数
     * @param params.page - 页码
     * @param params.pageSize - 每页数量
     */
    getPendingReplyReviews: (params?: { page?: number; pageSize?: number }) => {
        return http.get('/seller/reviews/pending', { params })
    },

    /**
     * 获取商品评价标签
     * @param productId - 商品ID
     */
    getReviewTags: (productId: number) => {
        return http.get(`/reviews/product/${productId}/tags`)
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
        return http.get(`/orders/${orderId}/logistics`)
    },

    /**
     * 用户端刷新物流信息
     * POST /api/v1/orders/{orderId}/logistics/refresh
     *
     * @param orderId - 订单ID
     * @returns Promise
     */
    refreshUserLogistics: (orderId: number) => {
        return http.post(`/orders/${orderId}/logistics/refresh`)
    },


    // ============ 结算页 ============

    /**
     * 购物车结算：根据购物车项ID列表获取商品信息
     * @param data - 请求参数
     * @param data.ids - 购物车项ID数组
     */
    getCheckoutItemsFromCart: (data: { ids: string[] }) => {
        return http.post('/checkout/cart', data)
    },

    /**
     * 立即购买：根据商品ID和数量获取商品信息
     * @param data - 请求参数
     * @param data.productId - 商品ID
     * @param data.quantity - 购买数量
     */
    getCheckoutItemsFromProduct: (data: { productId: number; quantity: number }) => {
        return http.post('/checkout/product', data)
    },

    /**
     * 根据购物车项ID列表获取商品信息
     * @param ids - 购物车项ID数组
     */
    getCartItemsByIds: (ids: string[]) => {
        return http.post('/cart/items/batch', { ids })
    },

    // ============ 商家入驻 ============

    /**
     * 获取商品详情
     * @param productId - 商品ID
     */
    getProduct: (productId: number) => {
        return http.get(`/products/${productId}`)
    },

    /**
     * 提交商家入驻申请
     * @param data - FormData对象，包含入驻申请信息
     */
    submitMerchantApplication: (data: FormData) => {
        return http.post('/merchant/applications', data);
    },

    /**
     * 查询入驻申请状态
     */
    getMerchantApplicationStatus: () => {
        return http.get('/merchant/applications/status');
    },

    // ============ 商家商品管理 ============

    /**
     * 添加商品
     * @param data - FormData对象，包含商品信息和图片
     */
    addProduct: (data: any) => {
        return http.post('/seller/products', data);
    },

    /**
     * 删除商品
     * @param productId - 商品ID
     */
    deleteProduct: (productId: number) => {
        return http.delete(`/seller/products/${productId}`)
    },

    /**
     * 批量删除商品
     * @param productIds - 商品ID数组
     */
    batchDeleteProducts: (productIds: number[]) => {
        return http.delete('/seller/products/batch', { data: { productIds } })
    },

    /**
     * 更新商品
     * @param productId - 商品ID
     * @param data - 商品数据
     */
    updateProduct: (productId: number, data: any) => {
        return http.put(`/seller/products/${productId}`, data)
    },

    /**
     * 获取商家商品列表（带状态筛选）
     * @param params - 查询参数
     * @param params.page - 页码
     * @param params.pagepageSize - 每页数量
     * @param params.keyword - 搜索关键词
     * @param params.status - 商品状态
     */
    getSellerProducts: (params: {
        page?: number;
        pagepageSize?: number;
        keyword?: string;
        status?: number
    }) => {
        return http.get('/seller/products', { params })
    },

    /**
     * 修改商品状态
     * @param productId - 商品ID
     * @param status - 状态值（0-下架，1-上架）
     */
    updateProductStatus: (productId: number, status: number) => {
        return http.patch(`/seller/products/${productId}`, { status });
    },

    // ============ 商家信息管理 ============

    /**
     * 获取商家个人资料
     */
    getSellerProfile: () => {
        return http.get('/seller/profile')
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
        return http.put('/seller/profile', data)
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
        return http.get('/seller/orders', { params })
    },

    /**
     * 获取商家订单详情（含地址和商品列表）
     * 
     * @param orderId - 订单ID
     * @returns Promise
     */
    getSellerOrderDetail: (orderId: number) => {
        return http.get(`/seller/orders/${orderId}`)
    },

    /**
     * 商家处理订单（PAID → PROCESSING）
     * 
     * @param orderId - 订单ID
     * @returns Promise
     */
    processOrder: (orderId: number) => {
        return http.put(`/seller/orders/${orderId}/process`)
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
        return http.put(`/seller/orders/${orderId}/ship`, data)
    },

    /**
     * 商家取消订单
     * 
     * @param orderId - 订单ID
     * @param reason - 取消原因（可选）
     * @returns Promise
     */
    sellerCancelOrder: (orderId: number, reason?: string) => {
        return http.put(`/seller/orders/${orderId}/cancel`, { reason })
    },

    /**
     * 获取物流信息
     *
     * @param orderId - 订单ID
     * @returns Promise
     */
    getLogisticsInfo: (orderId: number) => {
        return http.get(`/seller/orders/${orderId}/logistics`)
    },

    /**
     * 刷新物流信息
     *
     * @param orderId - 订单ID
     * @returns Promise
     */
    refreshLogistics: (orderId: number) => {
        return http.post(`/seller/orders/${orderId}/logistics/refresh`)
    },

    // ============ 公共接口 ============

    /**
     * 刷新token
     * @param refreshToken - 刷新令牌
     */
    refreshToken: (refreshToken: string) => {
        return http.post('/auth/refresh', { refreshToken });
    },

    /**
     * 获取账号信息（角色、状态）
     */
    getAccountProfile: () => {
        return http.get('/account/profile')
    },

    /**
     * 获取所有分类
     */
    getAllCategories: () => {
        return http.get('/categories')
    },

}