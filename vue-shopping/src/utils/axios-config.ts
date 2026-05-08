import axios from "axios"; // 主要HTTP库
import Cookies from "js-cookie"; // Cookie操作库

// 创建实例（克隆一个独立的axios）
const http = axios.create({
    // 本地开发	http://localhost:8088/api/v1/ 内网穿透	http://b9ef724d.natappfree.cc 访问
    baseURL: "/api/v1/",
    timeout: 10000,

    // 关键允许发送Cookie
    withCredentials: true
})

// 请求拦截器（发请求前处理）
http.interceptors.request.use(
    config => {
        // 从cookie中获取令牌
        const csrfToken = Cookies.get("XSRF-TOKEN")

        // 添加CSRF-TOKEN防止跨站请求伪造
        if (csrfToken) {
            config.headers["X-XSRF-TOKEN"] = csrfToken
        }

        return config
    },

    // 错误处理函数(请求失败时执行)
    error => {
        console.log("请求发送失败:",error)
        return Promise.reject(error)
    }
)

// 响应拦截器
http.interceptors.response.use(  
    response => {
        // 成功时返回业务数据
        return response.data
    },
    error => {
        // 返回统一的错误对象
        const {response} = error

        if (response) {
            const { status,data } = response

            const errorInfo = {
                status,
                message: data?.message || getDefaultMessage(status),
                data: data,
                originalError: error
            }

            return Promise.reject(errorInfo)
        } else {
            return Promise.reject({
                status: -1,
                message: "网络连接失败,请检查网络",
                originalError: error
            });
        }

    }

)

// 获取默认错误消息
function getDefaultMessage(status:number) {
    const messages = {
        400: "请求参数错误",
        401: "请先登录",
        403: "权限不足",
        409: "用户已存在",
        500: "服务器内部错误"
    } as Record<number,string>;

    return messages[status] || `请求失败 (${status})`;
}

// 默认导出实例
export default http