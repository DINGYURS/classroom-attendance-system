import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'


// https://vite.dev/config/
export default defineConfig({
  plugins: [vue(),
    tailwindcss()
  ],
    server: {
    host: '0.0.0.0',
    cors: true,
    port: 8888,
    open: false, //自动打开
    proxy: {
      '/api': {
        // target: 'http://120.55.62.76:9000', // 真实接口地址, 后端给的基地址
        target: 'http://127.0.0.1:9100', // 真实接口地址, 后端给的基地址
        changeOrigin: true, // 允许跨域
        secure: false, //设置是否使用安全连接（https）
        // rewrite: path => path.replace(/^\/api/, "")
      }
    }
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  }
})
