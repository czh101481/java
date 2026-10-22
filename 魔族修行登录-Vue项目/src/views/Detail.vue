<template>
  <div class="detail">
    <DemonBackground />

    <!-- 导航栏 -->
    <header class="nav">
      <div class="brand">
        <span class="brand-mark">魔族修行</span>
        <span class="brand-sub">魔 域 枢 机</span>
      </div>

      <nav class="nav-links">
        <a class="nav-item" @click="goHome">首 页</a>
        <a
          v-for="m in navModules"
          :key="m.key"
          class="nav-item"
          :class="{ active: m.key === type }"
          @click="goModule(m.key)"
        >{{ m.title }}</a>
      </nav>

      <div class="nav-user">
        <div class="user-meta">
          <span class="user-name">{{ user?.name || '无名修者' }}</span>
        </div>
        <div class="avatar">{{ initial }}</div>
        <button class="logout" type="button" @click="onLogout">断魔契</button>
      </div>
    </header>

    <!-- 详情面板 -->
    <section class="panel">
      <div v-if="loading" class="state">魔 渊 回 响 中 ……</div>

      <div v-else-if="!info" class="state">魔 渊 之 中 并 无 此 记 载 。</div>

      <template v-else>
        <div class="crumbs">
          <span class="crumb-link" @click="goHome">魔域枢机</span>
          <span class="sep">/</span>
          <span class="crumb-link" @click="goModule(type)">{{ info.typeName }}</span>
          <span class="sep">/</span>
          <span class="crumb-now">{{ info.title }}</span>
        </div>

        <span class="badge">{{ info.typeName }}</span>
        <h1 class="title">{{ info.title }}</h1>
        <p class="summary">{{ info.summary }}</p>

        <div class="meta">
          <div class="meta-row" v-for="m in info.meta" :key="m.label">
            <span class="m-label">{{ m.label }}</span>
            <span class="m-value">{{ m.value }}</span>
          </div>
        </div>

        <div class="divider">
          <span>秘 传 正 文</span>
        </div>
        <div class="content">{{ info.content }}</div>

        <div class="actions">
          <button class="btn ghost" type="button" @click="goModule(type)">⟵ 返 回 {{ info.typeName }}</button>
          <button class="btn" type="button" @click="goHome">回 魔 域 枢 机</button>
        </div>
      </template>
    </section>

    <footer class="det-foot">顺 逆 由 心 · 魔 非 魔 · 道 非 道</footer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import DemonBackground from '../components/DemonBackground.vue'
import { getSession, clearSession } from '../api/auth'
import { homeDetail } from '../api/home'

const route = useRoute()
const router = useRouter()
const session = getSession()
const user = ref(session.user)

const type = computed(() => String(route.params.type || 'art'))
const id = computed(() => Number(route.params.id || 0))
const initial = computed(() => (user.value?.name || '魔').slice(0, 1))

const navModules = [
  { key: 'art', title: '功 法 阁' },
  { key: 'trial', title: '历 练 录' },
  { key: 'debate', title: '宗 门 论 道' },
  { key: 'codex', title: '魔 典 秘 录' }
]

const info = ref(null)
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const res = await homeDetail(type.value, id.value)
    if (res.data.code === 200) info.value = res.data.data
    else info.value = null
  } catch (e) {
    info.value = null
  } finally {
    loading.value = false
  }
}

function goModule(key) { router.push('/module/' + key) }
function goHome() { router.push('/home') }
function onLogout() { clearSession(); router.replace('/login') }

onMounted(() => {
  if (!session.token) {
    router.replace('/login')
    return
  }
  load()
})
</script>

<style scoped>
.detail {
  position: fixed;
  inset: 0;
  z-index: 2;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  padding-bottom: 60px;
  background: radial-gradient(ellipse at 50% 18%, #1c0510 0%, #0b0207 55%, #050104 100%);
}

/* ===== 导航 ===== */
.nav {
  position: sticky; top: 0; z-index: 5;
  display: flex; align-items: center; gap: 26px;
  padding: 16px 34px;
  background: rgba(8, 2, 12, 0.72);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(255, 59, 107, 0.18);
}
.brand { display: flex; flex-direction: column; }
.brand-mark {
  font-size: 20px; letter-spacing: 5px; color: #e8dcc8;
  text-shadow: 0 0 14px rgba(255, 59, 107, 0.75);
}
.brand-sub { font-size: 11px; letter-spacing: 4px; color: rgba(179, 24, 58, 0.9); margin-top: 3px; }
.nav-links { display: flex; gap: 26px; flex: 1; }
.nav-item {
  font-size: 14px; letter-spacing: 2px; cursor: pointer; color: rgba(232, 220, 200, 0.6);
  transition: all 0.25s; padding-bottom: 3px; border-bottom: 1px solid transparent;
}
.nav-item:hover { color: #ff6b8f; }
.nav-item.active { color: #ff3b6b; border-bottom-color: #ff3b6b; }
.nav-user { display: flex; align-items: center; gap: 12px; }
.user-meta { display: flex; flex-direction: column; align-items: flex-end; }
.user-name { font-size: 13px; color: #e8dcc8; letter-spacing: 2px; }
.avatar {
  width: 34px; height: 34px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 15px; color: #fff;
  background: linear-gradient(135deg, #b3183a, #4a0d24);
  box-shadow: 0 0 14px rgba(255, 59, 107, 0.5);
}
.logout {
  padding: 6px 14px; border-radius: 8px; cursor: pointer;
  font-family: inherit; font-size: 12px; letter-spacing: 2px;
  color: rgba(232, 220, 200, 0.85);
  background: rgba(20, 4, 14, 0.7);
  border: 1px solid rgba(255, 59, 107, 0.4);
  transition: all 0.25s;
}
.logout:hover { color: #fff; border-color: #ff3b6b; }

/* ===== 面板 ===== */
.panel {
  position: relative; z-index: 1;
  width: 92%; max-width: 860px;
  margin: 34px auto 0;
  padding: 34px 40px 40px;
  border-radius: 16px;
  background: linear-gradient(160deg, rgba(40, 6, 22, 0.76), rgba(8, 2, 10, 0.86));
  border: 1px solid rgba(255, 59, 107, 0.24);
  box-shadow: 0 0 50px rgba(179, 24, 58, 0.2), inset 0 0 40px rgba(179, 24, 58, 0.1);
  animation: rise 0.7s cubic-bezier(0.2, 0.8, 0.2, 1) both;
}
@keyframes rise {
  from { opacity: 0; transform: translateY(26px); }
  to   { opacity: 1; transform: translateY(0); }
}

.state { padding: 44px; text-align: center; font-size: 13px; letter-spacing: 4px; color: rgba(232, 220, 200, 0.45); }

.crumbs { font-size: 12px; letter-spacing: 2px; color: rgba(232, 220, 200, 0.45); margin-bottom: 18px; }
.crumb-link { cursor: pointer; color: rgba(217, 176, 102, 0.8); }
.crumb-link:hover { color: #d9b066; text-shadow: 0 0 8px rgba(217, 176, 102, 0.6); }
.sep { margin: 0 8px; }
.crumb-now { color: rgba(232, 220, 200, 0.6); }

.badge {
  display: inline-block; padding: 4px 14px; border-radius: 20px;
  font-size: 12px; letter-spacing: 3px; color: #ff6b8f;
  border: 1px solid rgba(255, 59, 107, 0.5);
  box-shadow: 0 0 12px rgba(255, 59, 107, 0.3);
}
.title {
  font-weight: normal; font-size: 32px; letter-spacing: 6px;
  color: #e8dcc8; margin: 16px 0 10px;
  text-shadow: 0 0 18px rgba(255, 59, 107, 0.6);
}
.summary {
  font-size: 14px; line-height: 1.9; letter-spacing: 2px;
  color: rgba(232, 220, 200, 0.72);
}

.meta {
  display: flex; flex-wrap: wrap; gap: 12px;
  margin: 24px 0 4px; padding: 18px 20px;
  border-radius: 12px;
  background: rgba(6, 2, 10, 0.55);
  border: 1px solid rgba(217, 176, 102, 0.18);
}
.meta-row { display: flex; align-items: center; gap: 10px; min-width: 150px; }
.m-label { font-size: 12px; letter-spacing: 2px; color: rgba(217, 176, 102, 0.85); }
.m-value {
  font-size: 13px; letter-spacing: 1px; color: #e8dcc8;
  padding: 3px 12px; border-radius: 6px;
  background: rgba(179, 24, 58, 0.2);
  border: 1px solid rgba(255, 59, 107, 0.28);
}

.divider {
  display: flex; align-items: center; gap: 14px;
  margin: 28px 0 16px; font-size: 12px; letter-spacing: 5px; color: rgba(255, 59, 107, 0.7);
}
.divider::before, .divider::after {
  content: ''; flex: 1; height: 1px;
  background: linear-gradient(90deg, transparent, rgba(255, 59, 107, 0.45), transparent);
}

.content {
  font-size: 15px; line-height: 2.3; letter-spacing: 2px;
  color: rgba(232, 220, 200, 0.88);
  padding: 20px 22px; border-radius: 12px;
  background: rgba(6, 2, 10, 0.45);
  border-left: 2px solid rgba(255, 59, 107, 0.55);
}

.actions { display: flex; gap: 14px; margin-top: 30px; flex-wrap: wrap; }
.btn {
  padding: 11px 22px; border-radius: 10px; cursor: pointer;
  font-family: inherit; font-size: 13px; letter-spacing: 3px; color: #fff;
  border: none; background: linear-gradient(135deg, #b3183a, #4a0d24);
  box-shadow: 0 0 18px rgba(179, 24, 58, 0.5);
  transition: all 0.28s;
}
.btn:hover { transform: translateY(-2px); box-shadow: 0 0 28px rgba(255, 59, 107, 0.65); }
.btn.ghost {
  background: rgba(20, 4, 14, 0.7);
  border: 1px solid rgba(255, 59, 107, 0.4);
  color: rgba(232, 220, 200, 0.9);
  box-shadow: none;
}
.btn.ghost:hover { border-color: #ff3b6b; box-shadow: 0 0 16px rgba(255, 59, 107, 0.35); }

.det-foot {
  text-align: center; margin-top: 40px; font-size: 12px;
  letter-spacing: 5px; color: rgba(255, 59, 107, 0.4);
}

@media (max-width: 720px) {
  .nav { flex-wrap: wrap; gap: 14px; padding: 12px 16px; }
  .nav-links { order: 3; width: 100%; overflow-x: auto; gap: 16px; }
  .panel { padding: 24px 20px 28px; }
  .title { font-size: 24px; letter-spacing: 4px; }
}
</style>
