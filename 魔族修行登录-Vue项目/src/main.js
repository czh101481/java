import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { initRealmConfig } from './config/realm'
import './styles.css'

// 拉取后端 demon_realm 境界配置（失败则用内置兜底值，不阻塞渲染）
initRealmConfig()

createApp(App).use(router).mount('#app')
