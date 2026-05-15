import http from "@/utils/axios-config"

export const authAPI = {

    // ==================== 用户相关 ====================

    /**
     * 获取用户ID（登录/注册）
     * @param deviceId 设备标识符
     */
    getUserId:  (deviceId: string) => {
        return  http.post(`/auth/${deviceId}`)
    },

    /**
     * 绑定手机号
     * @param userId 用户ID
     * @param phone 手机号
     */
    bindPhone: (userId: number, phone: string) => {
        return http.put(`/user/${userId}/phone`, { phone })
    },

    /**
     * 通过手机号查询用户ID（找回账号）
     * @param phone 手机号
     */
    findUserIdByPhone: (phone: string) => {
        return http.get(`/user/findByPhone`, { params: { phone } })
    },

    /**
     * 获取用户信息
     * @param userId 用户ID
     */
    getUserInfo: (userId: number) => {
        return http.get(`/user/${userId}`)
    },

    // ==================== 订单相关 ====================

    /**
     * 创建订单
     * @param userId 用户ID
     * @param data 订单数据
     */
    createOrder:  (userId: number, data: any) => {
        return  http.post(`/orders/${userId}`, data);
    },

    /**
     * 获取订单详情
     * @param orderNumber 订单号
     */
    getOrderDetail:  (orderNumber: string) => {
        return http.get(`/orders/detail/${orderNumber}`)
    },

    /**
     * 获取用户订单列表
     * @param userId 用户ID
     * @param page 页码
     * @param pageSize 每页数量
     */
    getUserOrders: (userId: number, page: number = 1, pageSize: number = 20) => {
        return http.get(`/orders/user/${userId}`, {
            params: { page, pageSize }
        })

    },

    /**
     * 取消订单
     * @param orderNumber 订单号
     */
    cancelOrder: (orderNumber: string) => {
        return http.put(`/orders/cancel/${orderNumber}`)
    },

    /**
     * 申请退款
     * @param params 退款参数
     */
    refundOrder:  (params: { orderNumber: string; refundAmount: number; refundReason: string }) => {
        return http.post('/orders/refund', params)
    },

    /**
     * 确认收货（用户用）
     * @param orderNumber 订单号
     */
    confirmReceipt: (orderNumber: string) => {
        return http.put(`/orders/confirm/${orderNumber}`)
    },

    /**
     * 发货（商家用）- 外卖配送
     * @param orderNumber 订单号
     */
    shipOrder: (orderNumber: string) => {
        return http.put(`/orders/ship/${orderNumber}`)
    },

    /**
     * 核销/完成订单（商家用）- 到店用餐/打包自取
     * @param orderNumber 订单号
     */
    completeOrder: (orderNumber: string) => {
        return http.put(`/orders/complete/${orderNumber}`)
    },

    // ==================== 支付相关 ====================

    /**
     * 支付订单
     * @param params 支付参数
     */
    paymentOrder: (params: { orderNumber: string; amount: number; paymentMethod: string }) => {
        return http.post('/order/payment', params)
    },

    /**
     * 查询支付结果（轮询用）
     * @param orderNumber 订单号
     */
    queryPaymentResult: (orderNumber: string) => {
        return http.get(`/payments/result/${orderNumber}`)
    },

    // ==================== 商家相关 ====================

    /**
     * 获取商家信息
     */
    getSellerProfile: () => {
        return  http.get('/seller/profile')
    },

    /**
     * 更新商家信息
     * @param data 商家信息
     */
    updateSellerProfile:  (data: any) => {
        return http.put('/seller/profile', data)
    },

    /**
     * 更新订单状态（商家用）
     * @param orderNumber 订单号
     * @param status 订单状态
     */
    updateOrderStatus: (orderNumber: string, status: string) => {
        return  http.put(`/orders/status/${orderNumber}`, { status })
    },

    /**
     * 获取商家订单列表
     * @param page 页码
     * @param pageSize 每页数量
     * @param status 订单状态筛选
     */
    getSellerOrders:  (page: number = 1, pageSize: number = 20, status?: string) => {
        const params: any = { page, pageSize }
        if (status) params.status = status
        return  http.get('/seller/orders', { params })
    },

    /**
     * 更新商家头像
     * @param data FormData 包含头像文件
     */
    updateSellerAvatar:  (data: FormData) => {
        return  http.post("/seller/avatar", data);
    },

    // ==================== 商品相关 ====================

    /**
     * 添加商品
     * @param data FormData 包含商品信息和图片
     */
    addProduct:  (data: FormData) => {
        return  http.post("/seller/products", data);
    },

    /**
     * 删除商品
     * @param productId 商品ID
     */
    deleteProduct:  (productId: string | number) => {
        return  http.delete(`/seller/products/${productId}`);
    },

    /**
     * 修改商品
     * @param productId 商品ID
     * @param data 商品数据
     */
    updateProduct:  (productId: number | string, data: any) => {
        return  http.put(`/seller/products/${productId}`, data);
    },
    
    /**
     * 获取所有商品
     */
    getAllProducts:  () => {
        return  http.get("/seller/products");
    },
}