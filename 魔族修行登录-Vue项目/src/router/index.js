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
import Vault from '../views/Vault.vue'
import Cultivate from '../views/Cultivate.vue'
import Den from '../views/Den.vue'
import Achievement from '../views/Achievement.vue'

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
  // 传音 · 魔友（好友 + 私信 + 分享修炼心得）
  { path: '/social', name: 'social', component: Social },
  // 藏宝阁（魔晶 / 血玉 · 商店 · 乾坤袋 · 收支簿）
  { path: '/vault', name: 'vault', component: Vault },
  // 修为（战力 / 五维数值 / 九重进度 / 已习功法）
  { path: '/cultivate', name: 'cultivate', component: Cultivate },
  // 魔窟（洞府挂机：聚煞阵 / 魔晶矿 / 血玉髓脉，12 小时封顶）
  { path: '/den', name: 'den', component: Den },
  // 道途成就（纯荣誉徽记 + 六期进度；不发任何资源）
  { path: '/achievement', name: 'achievement', component: Achievement }
]

export default createRouter({
  // hash 模式：脱离服务器也能跳转（配合 vite base:'./'，file:// 双击打开 dist 同样可用）
  history: createWebHashHistory(),
  routes
})
