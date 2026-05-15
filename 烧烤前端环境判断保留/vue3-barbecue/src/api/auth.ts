import http from "@/utils/axios-config"

export const authAPI = {

    // 统一认证用户接口
    login: async (param: string) => {
        const response = await http.post(`/auth/${param}`)
        return response
    },

    // 创建订单 返回支付信息
    createOrder: async (deviceId:string, data:any) => {
        const response = await http.post(`/orders/${deviceId}`, data);
        return response
    },

    // 创建支付订单
    createPayment: (params: any) => {
        return http.post('/payments/create', params)
    },

    // 查询支付结果
    queryPaymentResult: (orderNumber: string) => {
        return http.get(`/payments/result/${orderNumber}`)
    },
    
    // 获取支付订单详情
    getOrderDetail: (orderNumber: string) => {
        return http.get(`/orders/detail/${orderNumber}`)
    },

    // 获取该用户所有订单
    getUserOrders: async (userId: string, page: number = 1, pageSize: number = 20) => {
        const response = await http.get(`/orders/user/${userId}`, {
            params: { page, pageSize }
        })
        return response
    },

    // 取消订单
    cancelOrder: async (orderNumber: string) => {
        const response = await http.put(`/orders/cancel/${orderNumber}`)
        return response
    },
    

    // ========== 商家 ===========

    // 获取商家信息
    getSellerProfile: async () => {
        const response = await http.get('/seller/profile')
        return response
    },

    // 更新商家信息
    updateSellerProfile: async (data: any) => {
        const response = await http.put('/seller/profile', data)
        return response
    },

    // 获取商家订单列表
    getSellerOrders: async (page: number = 1, pageSize: number = 20, status?: string) => {
        const params: any = { page, pageSize }
        if (status) params.status = status
        const response = await http.get('/seller/orders', { params })
        return response
    },

    // 商家更改头像
    updateSellerAvatar: async (data:any) => {
        const response = await http.post("/seller/avatar", data);
        return response;
    },

    // 添加商品
    addProduct: async (data:any) => {
        const response = await http.post("/seller/products", data);
        return response;
    },

    // 删除商品
    deleteProduct: async (productId:string | number) => {
        const response = await http.delete(`/seller/products/${productId}`);
        return response;
    },

    // 修改商品
    updateProduct: async (productId: number | string, data:any) => {
        const response = await http.put(`/seller/products/${productId}`, data);
        return response;
    },
    
    // 获取所有商品
    getAllProducts: async () => {
        const response = await http.get("/seller/products");
        return response;
    },

    
}