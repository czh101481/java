import { createRouter, createWebHashHistory } from 'vue-router'
import Login from '../views/Login.vue'
import Register from '../views/Register.vue'
import Home from '../views/Home.vue'
import Module from '../views/Module.vue'
import Detail from '../views/Detail.vue'
import Archive from '../views/Archive.vue'
import AdminLogin from '../views/AdminLogin.vue'
import AdminPanel from '../views/AdminPanel.vue'
import Social from '../views/Social.vue'
import Favorites from '../views/Favorites.vue'
import Account from '../views/Account.vue'

const routes = [
  { path: '/', redirect: '/login' },
  { path: '/login', name: 'login', component: Login },
  { path: '/register', name: 'register', component: Register },
  { path: '/home', name: 'home', component: Home },
  { path: '/module/:type', name: 'module', component: Module },
  { path: '/detail/:type/:id', name: 'detail', component: Detail },
  // 魔枢典册：cat 可选，缺省展示魔族谱系
  { path: '/archive/:cat?', name: 'archive', component: Archive },
  // 后台管理系统：登录（仅登录无注册）→ 统一数据管理页（art/trial/debate/codex/archive 五模块页签）
  { path: '/admin/login', name: 'adminLogin', component: AdminLogin },
  { path: '/admin/:module?', name: 'admin', component: AdminPanel },
  // 收藏阁：本人收藏的内容（点赞/收藏在详情页操作）
  { path: '/favorites', name: 'favorites', component: Favorites },
  // 账号阁：身份信息 / 换密令
  { path: '/account', name: 'account', component: Account },
  // 传音 · 魔友（好友 + 私信 + 分享修炼心得）
  { path: '/social', name: 'social', component: Social }
]

export default createRouter({
  // hash 模式：脱离服务器也能跳转（配合 vite base:'./'，file:// 双击打开 dist 同样可用）
  history: createWebHashHistory(),
  routes
})
