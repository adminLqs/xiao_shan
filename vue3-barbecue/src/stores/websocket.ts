// stores/websocket.ts
import { ref } from 'vue'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { showToast } from 'vant'

export const useWebSocketStore = () => {
    const stompClient = ref<Client | null>(null)
    const isConnected = ref(false)

    /**
     * 商家连接 WebSocket（ID固定为1）
     */
    const connectSeller = () => {
        const client = new Client({
            webSocketFactory: () => new SockJS('/ws'),
            debug: (str) => console.log(str),
            reconnectDelay: 5000,
            onConnect: () => {
                console.log('WebSocket连接成功')
                isConnected.value = true

                // 订阅新订单通知
                client.subscribe('/user/1/queue/orders', (message) => {
                    const data = JSON.parse(message.body)
                    console.log('收到新订单通知:', data)
                    
                    // 播放提示音
                    playNotificationSound()
                    
                    // 显示提示
                    showToast({
                        message: `新订单！订单号: ${data.orderNumber}`,
                        type: 'success',
                        duration: 5000
                    })
                    
                    // 触发自定义事件，让订单页刷新
                    window.dispatchEvent(new CustomEvent('new-order', { detail: data }))
                })

                // 订阅客服消息（商家接收用户消息）
                client.subscribe('/user/1/queue/chat', (message) => {
                    const data = JSON.parse(message.body)
                    console.log('收到客服消息:', data)
                    
                    showToast({
                        message: `用户消息: ${data.content}`,
                        type: 'info',
                        duration: 3000
                    })
                    
                    // 触发自定义事件
                    window.dispatchEvent(new CustomEvent('new-chat-message', { detail: data }))
                })
            },
            onStompError: (frame) => {
                console.error('WebSocket错误:', frame)
            }
        })

        client.activate()
        stompClient.value = client
    }

    /**
     * 播放提示音
     */
    const playNotificationSound = () => {
        const audio = new Audio('/sounds/new-order.mp3')
        audio.play().catch(error => {
            console.log('音频播放失败:', error)
        })
    }

    /**
     * 断开连接
     */
    const disconnect = () => {
        if (stompClient.value) {
            stompClient.value.deactivate()
            stompClient.value = null
            isConnected.value = false
        }
    }

    return {
        stompClient,
        isConnected,
        connectSeller,
        disconnect
    }
}