import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
  server: {
    host: '0.0.0.0',  // 监听所有网络接口，允许外部访问
    port: 5173,       // Vite 服务端口
    allowedHosts: [
      '62.234.79.156', // 云服务IP
      'http://daf24653.natappfree.cc',  // 允许这个域名访问
      '.natappfree.cc',           // 允许所有 natapp 子域名
    ],
    proxy: {
      // 代理所有 /api 请求
      '/api': {
        target: 'http://localhost:8088',
        changeOrigin: true,
      },
      // 代理SpringBoot的静态资源
      '/images': {
        target: 'http://localhost:8088',
        changeOrigin: true
      },
      // 代理上传文件
      '/uploads': {
        target: 'http://localhost:8088',
        changeOrigin: true
      }
    }
  }

})
