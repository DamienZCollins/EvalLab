import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      // 开发时把 /api 代理到后端，避免跨域配置散落在前端代码里
      '/api': {
        target: process.env.EVALLAB_SERVER_URL || 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
