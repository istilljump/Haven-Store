import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 3000,
    host: true,
    open: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src')
    }
  },
  build: {
    rollupOptions: {
      output: {
        // 大体积依赖单独拆包：echarts 只在管理看板用到、element-plus 全站共用，
        // 拆开后业务代码更新时用户无需重新下载整个依赖包
        manualChunks: {
          echarts: ['echarts'],
          'element-plus': ['element-plus'],
          vue: ['vue', 'vue-router', 'pinia', 'axios']
        }
      }
    }
  }
})
