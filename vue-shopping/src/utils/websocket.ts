import { ref } from 'vue'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import Message from '@/utils/message'
import { ElNotification } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import router from '@/router'
import { authAPI } from '@/api/authAPI'
import orderSound from '@/static/audio/订单创建播报.mp3'
import paymentSound from '@/static/audio/订单支付播报.mp3'
import refundSound from '@/static/audio/订单退款播报.mp3'
import chatSound from '@/static/audio/苹果短信消息.mp3'
import shipSound from '@/static/audio/订单发货音频.mp3'
import package7DaySound from '@/static/audio/套餐7天到期.mp3'
import package1DaySound from '@/static/audio/套餐一天到期.mp3'
import packageExpireSound from '@/static/audio/套餐已到期.mp3'
import packageRenewSound from '@/static/audio/套餐续费恢复.mp3'

export const useWebSocket = () => {
    const stompClient = ref<Client | null>(null)
    const isConnected = ref(false)
    const unreadCount = ref(0)
    const authStore = useAuthStore()

    // 请求通知权限
    const requestNotificationPermission = () => {
        if ('Notification' in window && Notification.permission === 'default') {
            Notification.requestPermission()
        }
    }

    // 发送推送通知
    const sendNotification = (title: string, body: string) => {
        if ('Notification' in window && Notification.permission === 'granted') {
            new Notification(title, { body, icon: '/favicon.ico' })
        }
    }

    // 播放音频的通用函数
    const playSound = (src: string) => {
        const audio = new Audio(src)
        audio.volume = 1
        audio.play().catch(() => {})
    }

    /**
     * 预加载音频文件（静默加载，提前缓存）
     * @param audioPaths - 音频文件路径数组
     */
    const preloadAudios = (audioPaths: string[]): void => {
        audioPaths.forEach((src: string) => {
            const audio = new Audio(src)
            audio.preload = 'auto'
            audio.load()
        })
    }

    // 通用订阅处理函数
    const subscribeToTopic = (client: Client, topic: string, handler: (data: any) => void) => {
        client.subscribe(topic, (msg) => {
            try {
                handler(JSON.parse(msg.body))
            } catch (e) {
                Message.error(`解析${topic}消息失败: ${e}`)
            }
        })
    }


    // 公共订阅 - 踢下线通知（商家端和用户端都需要）
    const setupCommonSubscriptions = (client: Client, userId: number) => {
        subscribeToTopic(client, `/exchange/amq.topic/kickout.user.${userId}`, (data: any) => {
            Message.warning(data.content || '您的账号在其他设备登录')
            // 清除登录状态
            authAPI.logout()
            authStore.clear()

            // 断开 WebSocket
            disconnect()
            // 跳转登录页
            setTimeout(() => {
                router.push({name: 'Login'})
            }, 1000)
        })
    }

    // 用户端连接
    const connectUser = (userId: number) => {
        if (!userId) return

        requestNotificationPermission()
        preloadAudios([orderSound, paymentSound, refundSound, chatSound, shipSound])

        const client = new Client({
            webSocketFactory: () => new SockJS('/ws'),
            reconnectDelay: 5000,
            onConnect: () => {
                isConnected.value = true
                console.log('已连接用户端订阅')

                // 公共订阅 - 踢下线通知
                setupCommonSubscriptions(client, userId)

                // 订单支付成功通知
                subscribeToTopic(client, `/exchange/amq.topic/user.payment.${userId}`, (data: any) => {
                    playSound(paymentSound)
                    Message.success(`订单 ${data.orderNumber} 支付成功！`)
                    window.dispatchEvent(new CustomEvent('user-payment-success', { detail: data }))
                })

                // 订阅发货通知
                subscribeToTopic(client, `/exchange/amq.topic/user.shipment.${userId}`, (data: any) => {
                    playSound(shipSound)
                    sendNotification('发货通知', `订单 ${data.orderNumber} 已发货`)
                    ElNotification({
                        title: '发货通知',
                        message: `订单 <strong>${data.orderNumber}</strong> 已发货<br>物流单号: ${data.content}`,
                        type: 'success',
                        dangerouslyUseHTMLString: true,
                        duration: 5000
                    })
                    window.dispatchEvent(new CustomEvent('shipment-notification', { detail: data }))
                })

                // 退款处理结果通知
                subscribeToTopic(client, `/exchange/amq.topic/user.refund.${userId}`, (data: any) => {
                    playSound(refundSound)
                    sendNotification('退款处理通知', data.content)
                    Message.info(`退款状态更新: ${data.content}`)
                    window.dispatchEvent(new CustomEvent('refund-update', { detail: data }))
                })

                // 退款沟通消息
                subscribeToTopic(client, `/exchange/amq.topic/user.refund.chat.${userId}`, (data: any) => {
                    playSound(chatSound)
                    sendNotification('退款沟通', data.content)
                    Message.info(`退款沟通: ${data.content}`)
                    window.dispatchEvent(new CustomEvent('refund-chat-message', { detail: data }))
                })

                // 订阅客服消息
                subscribeToTopic(client, `/exchange/amq.topic/user.chat.${userId}`, (data: any) => {
                    playSound(chatSound)
                    sendNotification('商家回复', `${data.content}`)
                    Message.info(`商家: ${data.content}`)
                    window.dispatchEvent(new CustomEvent('new-chat-message', { detail: data }))
                })
            },
            onStompError: (frame: any) => Message.error('WebSocket错误:', frame)
        })

        client.activate()
        stompClient.value = client
    }


    // 商家端连接
    const connectSeller = (userId: number) => {
        requestNotificationPermission()
        preloadAudios([orderSound, paymentSound, refundSound, chatSound, shipSound,
             package7DaySound, package1DaySound,  packageExpireSound,  packageRenewSound
        ])

        const client = new Client({
            webSocketFactory: () => new SockJS('/ws'),
            reconnectDelay: 5000,
            onConnect: () => {
                isConnected.value = true
                console.log('已连接商家端订阅')

                // 公共订阅 - 踢下线通知
                setupCommonSubscriptions(client, userId)

                // ===== 商家专属订阅 =====

                // 新订单
                subscribeToTopic(client, '/exchange/amq.topic/seller.new-order', (data: any) => {
                    playSound(orderSound)
                    sendNotification('新订单通知', `订单号: ${data.orderNumber} | 金额: ¥${data.amount}`)
                    Message.success(`新订单！订单号: ${data.orderNumber}`)
                    window.dispatchEvent(new CustomEvent('new-order', { detail: data }))
                })

                // 支付成功
                subscribeToTopic(client, '/exchange/amq.topic/seller.payment-success', (data: any) => {
                    playSound(paymentSound)
                    sendNotification('支付成功通知', `订单 ${data.orderNumber} 支付成功`)
                    Message.success(`订单 ${data.orderNumber} 支付成功！`)
                    window.dispatchEvent(new CustomEvent('payment-success', { detail: data }))
                })

                // 退款申请
                subscribeToTopic(client, `/exchange/amq.topic/seller.refund.${userId}`, (data: any) => {
                    playSound(refundSound)
                    sendNotification('退款申请通知', `订单号: ${data.orderNumber} | 退款金额: ¥${data.amount}`)
                    Message.warning(`用户申请退款，订单号: ${data.orderNumber}`)
                    window.dispatchEvent(new CustomEvent('refund-application', { detail: data }))
                })

                // 退款沟通消息
                subscribeToTopic(client, `/exchange/amq.topic/seller.refund.chat.${userId}`, (data: any) => {
                    playSound(chatSound)
                    sendNotification('退款沟通', data.content)
                    Message.info(`退款沟通: ${data.content}`)
                    window.dispatchEvent(new CustomEvent('refund-chat-message', { detail: data }))
                })

                // 套餐到期提醒
                subscribeToTopic(client, `/exchange/amq.topic/seller.package.${userId}`, (data: any) => {
                    // 根据剩余天数选择不同音频
                    if (data.days === 7) {
                        playSound(package7DaySound)
                    } else if (data.days === 1) {
                        playSound(package1DaySound)
                    } else if (data.type === 'expired') {
                        playSound(packageExpireSound)
                    } else if (data.type === 'renew') {
                        playSound(packageRenewSound)
                    }

                    sendNotification('套餐通知', data.content)
                    Message.warning(data.content)
                    window.dispatchEvent(new CustomEvent('package-notification', { detail: data }))
                })

                // 用户消息
                subscribeToTopic(client, `/exchange/amq.topic/seller.chat.${userId}`, (data: any) => {
                    playSound(chatSound)
                    sendNotification('新用户消息', `${data.content}`)
                    Message.info(`收到新用户消息`)
                    window.dispatchEvent(new CustomEvent('new-chat-message', { detail: data }))
                })


                // ===== 用户端基础订阅（商家也会在用户端购物） =====

                // // 发货通知
                // subscribeToTopic(client, `/exchange/amq.topic/user.shipment.${userId}`, (data: any) => {
                //     playSound(shipSound)
                //     Message.success(`订单 ${data.orderNumber} 已发货`)
                //     window.dispatchEvent(new CustomEvent('shipment-notification', { detail: data }))
                // })

                // // 退款处理结果通知
                // subscribeToTopic(client, `/exchange/amq.topic/user.refund.${userId}`, (data: any) => {
                //     playSound(refundSound)
                //     Message.info(`退款状态更新: ${data.content}`)
                //     window.dispatchEvent(new CustomEvent('refund-update', { detail: data }))
                // })

                // // 客服回复
                // subscribeToTopic(client, `/exchange/amq.topic/user.chat.${userId}`, (data: any) => {
                //     playSound(chatSound)
                //     Message.info(`商家: ${data.content}`)
                //     window.dispatchEvent(new CustomEvent('new-chat-message', { detail: data }))
                // })


            },
            onStompError: (frame: any) => Message.error('WebSocket错误:', frame)
        })

        client.activate()
        stompClient.value = client
    }

    // 管理员订阅
    const connectAdmin = () => {

    }


    const disconnect = () => {
        if (stompClient.value) {
            stompClient.value.deactivate()
            stompClient.value = null
            isConnected.value = false
        }
    }

    return {
        isConnected,
        unreadCount,
        connectAdmin,
        connectSeller,
        connectUser,
        disconnect
    }
}
