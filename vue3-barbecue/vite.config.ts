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
    host: '0.0.0.0', // 允许外部访问
    port: 5173,
    allowedHosts: [
      'http://g54eacdf.natappfree.cc',  // natapp 域名
      '.natappfree.cc', // natapp 子域名
    ],
    proxy: {
      // 代理Api请求路径
      '/api': {                    
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      // 代理SpringBoot的静态资源
      '/images': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      // 代理上传文件
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/ws': {
        target: 'http://localhost:8080',
        ws: true,
        changeOrigin: true
      }
    }
  }

})
