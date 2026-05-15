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
    host: '0.0.0.0',      // 👈 加上这一行，允许外部访问
    port: 5173,
    allowedHosts: [
      'z9df432b.natappfree.cc',  // 👈 加上你的 natapp 域名
      '.natappfree.cc',           // 👈 允许所有 natapp 子域名
    ],
    proxy: {
      '/api': {                              // ✅ 添加这个！
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
