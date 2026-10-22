import { createRouter, createWebHashHistory } from 'vue-router'
import Login from '../views/Login.vue'
import Register from '../views/Register.vue'
import Home from '../views/Home.vue'
import Module from '../views/Module.vue'
import Detail from '../views/Detail.vue'
import Archive from '../views/Archive.vue'

const routes = [
  { path: '/', redirect: '/login' },
  { path: '/login', name: 'login', component: Login },
  { path: '/register', name: 'register', component: Register },
  { path: '/home', name: 'home', component: Home },
  { path: '/module/:type', name: 'module', component: Module },
  { path: '/detail/:type/:id', name: 'detail', component: Detail },
  // 魔枢典册：cat 可选，缺省展示魔族谱系
  { path: '/archive/:cat?', name: 'archive', component: Archive }
]

export default createRouter({
  // hash 模式：脱离服务器也能跳转（配合 vite base:'./'，file:// 双击打开 dist 同样可用）
  history: createWebHashHistory(),
  routes
})
