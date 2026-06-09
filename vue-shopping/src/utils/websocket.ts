import { ref } from 'vue'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import Message from '@/utils/message'
import { useAuthStore } from '@/stores/auth'
import router from '@/router'
import { authAPI } from '@/api/authAPI'
import orderSound from '@/static/audio/订单创建播报.mp3'
import paymentSuccessSound  from '@/static/audio/订单支付播报.mp3'
import refundSound from '@/static/audio/订单退款播报.mp3'
import chatSound from '@/static/audio/苹果短信消息.mp3'
import shipSound from '@/static/audio/订单发货音频.mp3'
import afterSaleProcessedSound from '@/static/audio/订单售后已处理音频.mp3'
import orderPaidSound from '@/static/audio/订单支付成功.mp3'
import package7DaySound from '@/static/audio/套餐7天到期.mp3'
import package1DaySound from '@/static/audio/套餐一天到期.mp3'
import packageExpireSound from '@/static/audio/套餐已到期.mp3'
import packageRenewSound from '@/static/audio/套餐购买成功.mp3'

export const useWebSocket = () => {
    const stompClient = ref<Client | null>(null)
    const isConnected = ref(false)
    const unreadCount = ref(0)
    const authStore = useAuthStore()
    let disconnectTimer: ReturnType<typeof setTimeout> | null = null
    let heartbeatInterval: ReturnType<typeof setInterval> | null = null

    // 启动心跳，每 60 秒发送一次
    const startHeartbeat = () => {
        stopHeartbeat()
        heartbeatInterval = setInterval(() => {
            if (stompClient.value?.connected) {
                stompClient.value.publish({
                    destination: '/app/heartbeat',
                    body: ''
                })
                console.log('心跳已发送')
            }
        }, 60000)
    }

    // 停止心跳
    const stopHeartbeat = () => {
        if (heartbeatInterval) {
            clearInterval(heartbeatInterval)
            heartbeatInterval = null
        }
    }

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
                console.error(`解析${topic}消息失败: ${e}`)
            }
        })
    }


    // 公共订阅 - 踢下线通知（商家端和用户端都需要）
    const setupCommonSubscriptions = (client: Client, userId: number) => {
        subscribeToTopic(client, `/exchange/amq.topic/kickout.user.${userId}`, (data: any) => {
            Message.websocketKickout(data.content)
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

        preloadAudios([orderSound, chatSound, shipSound, orderPaidSound, afterSaleProcessedSound])

        const client = new Client({
            webSocketFactory: () => new SockJS('/ws'),
            connectHeaders: { userId: String(userId) },
            reconnectDelay: 5000,
            onConnect: () => {
                isConnected.value = true
                console.log('已连接用户端订阅')
                startHeartbeat()

                // 公共订阅 - 踢下线通知
                setupCommonSubscriptions(client, userId)

                // 订单支付成功通知
                subscribeToTopic(client, `/exchange/amq.topic/user.payment.${userId}`, (data: any) => {
                    playSound(orderPaidSound)
                    Message.success(`订单 ${data.orderNumber} 支付成功！`)
                    window.dispatchEvent(new CustomEvent('user-payment-success', { detail: data }))
                })

                // 订阅发货通知
                subscribeToTopic(client, `/exchange/amq.topic/user.shipment.${userId}`, (data: any) => {
                    playSound(shipSound)
                    Message.websocketShip(data.orderNumber, data.content)
                    window.dispatchEvent(new CustomEvent('shipment-notification', { detail: data }))
                })

                // 退款处理结果通知
                subscribeToTopic(client, `/exchange/amq.topic/user.refund.${userId}`, (data: any) => {
                    playSound(afterSaleProcessedSound)
                    Message.websocketRefundResult(data.content)
                    window.dispatchEvent(new CustomEvent('refund-update', { detail: data }))
                })

                // 退款沟通消息
                subscribeToTopic(client, `/exchange/amq.topic/user.refund.chat.${userId}`, (data: any) => {
                    playSound(chatSound)
                    const prefix = data.senderType === 'SELLER' ? '商家' : '买家'
                    Message.info(`${prefix}: ${data.content}`)
                    window.dispatchEvent(new CustomEvent('refund-chat-message', { detail: data }))

                    // 如果当前在退款沟通页面，自动刷新聊天记录
                    if (data.refundId) {
                        window.dispatchEvent(new CustomEvent('refresh-refund-chat', { detail: { refundId: data.refundId } }))
                    }
                })

                // 订阅客服消息
                subscribeToTopic(client, `/exchange/amq.topic/user.chat.${userId}`, (data: any) => {
                    playSound(chatSound)
                    Message.info(`${data.senderName || '商家'}: ${data.text || '[媒体消息]'}`)
                    window.dispatchEvent(new CustomEvent('new-chat-message', { detail: data }))
                })
            },
            onStompError: (frame: any) => console.error('WebSocket错误:', frame)
        })

        client.activate()
        stompClient.value = client
    }


    // 商家端连接
    const connectSeller = (userId: number) => {
        preloadAudios([orderSound, paymentSuccessSound , refundSound, chatSound, shipSound,
             package7DaySound, package1DaySound,  packageExpireSound,  packageRenewSound
        ])

        const client = new Client({
            webSocketFactory: () => new SockJS('/ws'),
            connectHeaders: { userId: String(userId) },
            reconnectDelay: 5000,
            onConnect: () => {
                isConnected.value = true
                console.log('已连接商家端订阅')
                startHeartbeat()

                // 公共订阅 - 踢下线通知
                setupCommonSubscriptions(client, userId)

                // ===== 商家专属订阅 =====

                // 新订单
                subscribeToTopic(client, '/exchange/amq.topic/seller.new-order', (data: any) => {
                    playSound(orderSound)
                    Message.websocketNewOrder(data.orderNumber, data.amount)
                    window.dispatchEvent(new CustomEvent('new-order', { detail: data }))
                })

                // 支付成功
                subscribeToTopic(client, '/exchange/amq.topic/seller.payment-success', (data: any) => {
                    playSound(paymentSuccessSound )
                    Message.websocketPaymentSuccess(data.orderNumber)
                    window.dispatchEvent(new CustomEvent('payment-success', { detail: data }))
                })

                // 退款申请
                subscribeToTopic(client, `/exchange/amq.topic/seller.refund.${userId}`, (data: any) => {
                    playSound(refundSound)
                    Message.websocketRefundApply(data.orderNumber, data.amount)
                    window.dispatchEvent(new CustomEvent('refund-application', { detail: data }))
                })

                // 买家退货提交
                subscribeToTopic(client, `/exchange/amq.topic/seller.return.${userId}`, (data: any) => {
                    playSound(chatSound)
                    Message.info(`买家已提交退货，物流单号: ${data.trackingNumber}`)
                    window.dispatchEvent(new CustomEvent('return-submitted', { detail: data }))
                    window.dispatchEvent(new CustomEvent('new-notification'))
                })

                // 退款沟通消息
                subscribeToTopic(client, `/exchange/amq.topic/seller.refund.chat.${userId}`, (data: any) => {
                    playSound(chatSound)
                    const prefix = data.senderType === 'SELLER' ? '商家' : '买家'
                    Message.info(`${prefix}: ${data.content}`)
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

                    Message.websocketPackage(data.content)
                    window.dispatchEvent(new CustomEvent('package-notification', { detail: data }))
                })

                // 用户消息
                subscribeToTopic(client, `/exchange/amq.topic/seller.chat.${userId}`, (data: any) => {
                    playSound(chatSound)
                    Message.info(`${data.senderName || '用户'}: ${data.text || '[媒体消息]'}`)
                    window.dispatchEvent(new CustomEvent('new-chat-message', { detail: data }))
                })

            },
            onStompError: (frame: any) => console.error('WebSocket错误:', frame)
        })

        client.activate()
        stompClient.value = client
    }

    // 管理员订阅
    const connectAdmin = () => {
        const client = new Client({
            webSocketFactory: () => new SockJS('/ws'),
            connectHeaders: { userId: String(authStore.userId) },
            reconnectDelay: 5000,
            onConnect: () => {
                isConnected.value = true
                console.log('已连接管理员端订阅')
                startHeartbeat()
                setupCommonSubscriptions(client, authStore.userId!)
            },
            onStompError: (frame: any) => console.error('WebSocket错误:', frame)
        })
        client.activate()
        stompClient.value = client
    }


    const disconnect = () => {
        if (disconnectTimer) clearTimeout(disconnectTimer)
        stopHeartbeat()
        disconnectTimer = setTimeout(() => {
            if (stompClient.value) {
                // 只有在连接已建立或正在连接时才断开
                if (stompClient.value.connected || stompClient.value.active) {
                    stompClient.value.deactivate()
                }
                stompClient.value = null
                isConnected.value = false
            }
            disconnectTimer = null
        }, 100)
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
