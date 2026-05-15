import { createApp } from 'vue'
import { createPinia } from 'pinia'
import router from './router/index'
import App from './App.vue'

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

const app = createApp(App) // 创建组件实例

app.use(router) // 使用路由
app.mount('#app') // 挂载到#app
