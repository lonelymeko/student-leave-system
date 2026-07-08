import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

// H5 端 baseURL 用相对 /api，dev 时代理到本机 8080 后端
export default defineConfig({
  plugins: [uni()],
  server: {
    port: 5174,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
