import { fileURLToPath } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 多页入口：注册页（index.html）+ 登录页（login-anime.html）
// 两页是各自独立的视觉主题，因此不共用主题 CSS，也不引入 vue-router。
const entry = (path) => fileURLToPath(new URL(path, import.meta.url))

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173
  },
  build: {
    rollupOptions: {
      input: {
        register: entry('./index.html'),
        login: entry('./login-anime.html')
      }
    }
  }
})
