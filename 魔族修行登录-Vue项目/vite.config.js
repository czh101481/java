import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  // 相对路径打包：dist/index.html 双击（file:// 协议）打开也能正常加载资源
  base: './',
  plugins: [vue()],
  server: {
    host: '0.0.0.0',
    port: 5173,
    open: true,
    // 头像等上传资源走同源相对路径，由这里转发到后端。
    // 换台电脑用局域网地址访问本机服务时，图片依旧能正确取到。
    proxy: {
      '/uploads': { target: 'http://localhost:8081', changeOrigin: true },
      '/api': { target: 'http://localhost:8081', changeOrigin: true }
    }
  }
})
