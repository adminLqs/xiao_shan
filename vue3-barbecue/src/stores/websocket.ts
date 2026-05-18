// stores/websocket.ts
import { ref } from 'vue'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import Message from '@/utils/message'
import orderSound from '@/static/audio/订单创建播报.mp3'
import paymentSound from '@/static/audio/订单支付播报.mp3'
import refundSound from '@/static/audio/订单退款播报.mp3'
import chatSound from '@/static/audio/苹果短信消息.mp3'

export const useWebSocketStore = () => {
    const stompClient = ref<Client | null>(null)
    const isConnected = ref(false)
    const unreadCount = ref(0)

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
                console.error(`解析${topic}消息失败:`, e)
            }
        })
    }

    const connectSeller = () => {
        // 请求通知权限
        requestNotificationPermission()

        // 预加载所有音频文件
        preloadAudios([orderSound, paymentSound, refundSound, chatSound])

        const client = new Client({
            webSocketFactory: () => new SockJS('/ws'),
            reconnectDelay: 5000,
            onConnect: () => {
                isConnected.value = true

                // 新订单
                subscribeToTopic(client, '/topic/new-order', (data: any) => {
                    playSound(orderSound)
                    sendNotification('新订单通知', `订单号: ${data.orderNumber} | 金额: ¥${data.amount}`)

                    Message.notifyWarning(`订单号: ${data.orderNumber} | 金额: ¥${data.amount}`, '新订单通知')
                    Message.success(`新订单！订单号: ${data.orderNumber}`)

                    window.dispatchEvent(new CustomEvent('new-order', { detail: data }))
                })

                // 支付成功
                subscribeToTopic(client, '/topic/payment-success', (data: any) => {
                    playSound(paymentSound)
                    sendNotification('支付成功通知', `订单 ${data.orderNumber} 支付成功！金额: ¥${data.amount}`)

                    Message.notifyWarning(`订单号: ${data.orderNumber} | 金额: ¥${data.amount}`, '支付成功通知')
                    Message.success(`订单 ${data.orderNumber} 支付成功！`)
                    window.dispatchEvent(new CustomEvent('payment-success', { detail: data }))
                })

                // 退款申请
                subscribeToTopic(client, '/topic/refund-application', (data: any) => {
                    playSound(refundSound)
                    sendNotification('退款申请通知', `订单号: ${data.orderNumber} | 退款金额: ¥${data.amount}`)

                    Message.notifyWarning(`订单号: ${data.orderNumber} | 退款金额: ¥${data.amount}`, '退款申请通知')
                    Message.warning(`用户申请退款，订单号: ${data.orderNumber}`)

                    window.dispatchEvent(new CustomEvent('refund-application', { detail: data }))
                })

                // 用户消息
                subscribeToTopic(client, '/topic/chat', (data: any) => {
                    playSound(chatSound)
                    sendNotification('新用户消息', `用户 ${data.fromUserId}: ${data.content}`)

                    Message.notifyInfo(`用户 ${data.fromUserId}: ${data.content}`, '新用户消息')
                    Message.info(`收到新用户消息`)

                    window.dispatchEvent(new CustomEvent('new-chat-message', { detail: data }))
                })
            },
            onStompError: (frame: any) => console.error('WebSocket错误:', frame)
        })

        client.activate()
        stompClient.value = client
    }

    const connectUser = (userId: number) => {
        if (!userId) {
            return
        }

        requestNotificationPermission()
        preloadAudios([chatSound])

        const client = new Client({
            webSocketFactory: () => new SockJS('/ws'),
            reconnectDelay: 5000,
            onConnect: () => {
                isConnected.value = true

                // 订阅客服消息（点对点）
                subscribeToTopic(client, `/topic/chat/user/${userId}`, (data: any) => {
                    playSound(chatSound)
                    sendNotification('商家回复', `${data.content}`)
                    Message.info(`商家: ${data.content}`)
                    window.dispatchEvent(new CustomEvent('new-chat-message', { detail: data }))
                })

                // 订阅发货通知
                subscribeToTopic(client, `/topic/shipment/user/${userId}`, (data: any) => {
                    playSound(chatSound)
                    sendNotification('发货通知', `订单 ${data.orderNumber} 已发货`)
                    Message.success(`订单 ${data.orderNumber} 已发货，物流单号: ${data.content}`)
                    window.dispatchEvent(new CustomEvent('shipment-notification', { detail: data }))
                })

            },
            onStompError: (frame: any) => console.error('WebSocket错误:', frame)
        })

        client.activate()
        stompClient.value = client
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
        connectSeller,
        connectUser,
        disconnect
    }
}

