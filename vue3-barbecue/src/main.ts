import { createApp } from 'vue'
import { createPinia } from 'pinia'
import router from './router/index'
import App from './App.vue'

const app = createApp(App) // 创建组件实例
const pinia = createPinia()

app.use(router) // 使用路由
app.use(pinia) // 使用状态管理 

app.mount('#app') // 挂载到#app
