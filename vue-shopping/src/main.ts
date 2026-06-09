import { createApp } from 'vue'
import { createPinia } from 'pinia'
import router from './router/index'
import App from './App.vue'

// 全局引入骨架屏 CSS
import '@/static/css/common/骨架屏.css'

// 全局引入星环加载器 CSS
import '@/static/css/common/星环加载器.css'

// 全局引入普通加载器 CSS
import '@/static/css/common/加载中.css'

// 全局引入消息提示样式（覆盖 Element Plus 默认样式）
import '@/static/css/common/消息提示样式.css'

// 在创建应用前强制添加样式
const style = document.createElement('style')
style.textContent = `
  i.fas,
  i[class^="fa-"],
  i[class*=" fa-"] {
    font-family: 'Font Awesome 6 Free' !important;
    font-weight: 900 !important;
  }
`
document.head.appendChild(style)

// 创建组件实例
const app = createApp(App) 

// 创建pinia状态管理
const pinia = createPinia() 

app.use(router) // 使用路由
app.use(pinia) // 使用状态管理
app.mount('#app') // 挂载到#app
