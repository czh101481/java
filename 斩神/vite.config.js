import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 用相对 base，构建产物可直接以 file:// 双击打开
export default defineConfig({
  base: './',
  plugins: [vue()],
  server: { host: true, port: 5173 }
})
