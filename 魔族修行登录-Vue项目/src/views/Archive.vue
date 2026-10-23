<template>
  <!-- 魔枢典册 · 典籍库 -->
  <div class="archive">
    <DemonBackground />

    <!-- 导航栏 -->
    <header class="nav">
      <div class="brand">
        <span class="brand-mark">魔族修行</span>
        <span class="brand-sub">魔 域 枢 机</span>
      </div>

      <nav class="nav-links">
        <a class="nav-item" @click="goHome">首 页</a>
        <a class="nav-item" @click="goModule('art')">功 法 阁</a>
        <a class="nav-item" @click="goModule('trial')">历 练 录</a>
        <a class="nav-item" @click="goModule('debate')">宗 门 论 道</a>
        <a class="nav-item" @click="goModule('codex')">魔 典 秘 录</a>
        <a class="nav-item active">魔 枢 典 册</a>
      </nav>

      <div class="nav-user">
        <div class="user-meta">
          <span class="user-name">{{ user?.name || '无名修者' }}</span>
          <span class="user-realm" v-if="user?.cultivation">{{ user.cultivation }}境 · 第{{ realmIndex + 1 }}重</span>
          <span class="user-realm" v-else-if="user?.realm">魔种 · {{ user.realm }}</span>
        </div>
        <div class="avatar">{{ initial }}</div>
        <button class="logout" type="button" @click="onLogout">断魔契</button>
      </div>
    </header>

    <!-- 板块头部 -->
    <section class="lib-head">
      <h1 class="lib-title">魔 枢 典 册</h1>
      <p class="lib-sub">典 籍 库 · 万 法 之 源 · 凡 有 所 疑 ， 皆 可 于 此 查 证</p>
    </section>

    <!-- 四大分类卡片 -->
    <section class="cat-grid">
      <div
        v-for="c in categories"
        :key="c.key"
        class="cat-card"
        :class="{ on: c.key === activeCat }"
        @click="selectCat(c.key)"
      >
        <span class="cat-cover" :style="{ backgroundImage: `url(${imgUrl(c.cover)})` }">
          <span class="cat-cover-veil"></span>
        </span>
        <div class="cat-body">
          <div class="cat-head">
            <span class="cat-icon">{{ c.icon }}</span>
            <span class="cat-name">{{ c.name }}</span>
          </div>
          <p class="cat-desc">{{ c.desc }}</p>
          <div class="cat-foot">
            <span class="cat-count">共 {{ c.count }} {{ c.unit }}</span>
            <span class="cat-go">{{ c.key === activeCat ? '已 开 卷' : '开 卷 ⟶' }}</span>
          </div>
        </div>
      </div>
    </section>

    <!-- 检索 + 条目列表 -->
    <section class="lib-body">
      <div class="lib-tools">
        <div class="lib-now">
          <span class="now-mark">{{ activeMeta.icon }}</span>
          <span class="now-name">{{ activeMeta.name }}</span>
          <span class="now-count">共 {{ items.length }} {{ activeMeta.unit }}</span>
        </div>
        <form class="lib-search" @submit.prevent="load">
          <span class="search-icon">✶</span>
          <input v-model="kw" type="text" placeholder="于典册之中查证……" autocomplete="off" />
          <button type="submit">查 证</button>
          <span class="lib-clear" v-if="kw" @click="kw = ''; load()">清 除</span>
        </form>
      </div>

      <div v-if="loading" class="state">魔 渊 回 响 中 ……</div>

      <div v-else-if="deny" class="state deny">
        <span class="deny-lock">封</span>{{ deny }}
      </div>

      <div v-else-if="items.length === 0" class="state">
        此 卷 之 中 尚 无 记 载 。
      </div>

      <div v-else class="entries">
        <article
          v-for="it in items"
          :key="it.id"
          class="entry"
          :class="{ open: openId === it.id }"
        >
          <div class="entry-head" @click="toggle(it.id)">
            <span class="entry-idx">{{ String(it.sortOrder).padStart(2, '0') }}</span>
            <div class="entry-main">
              <div class="entry-title">{{ it.title }}</div>
              <div class="entry-sub">{{ it.subtitle }}</div>
            </div>
            <span class="entry-arrow">{{ openId === it.id ? '—' : '＋' }}</span>
          </div>

          <p class="entry-summary">{{ it.summary }}</p>

          <div class="entry-content" v-if="openId === it.id">{{ it.content }}</div>
        </article>
      </div>
    </section>

    <footer class="lib-foot">顺 逆 由 心 · 魔 非 魔 · 道 非 道</footer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import DemonBackground from '../components/DemonBackground.vue'
import { getSession, clearSession } from '../api/auth'
import { archiveCategories, archiveList } from '../api/archive'
import { imgUrl } from '../api/home'

const route = useRoute()
const router = useRouter()
const session = getSession()
const user = ref(session.user)
const initial = computed(() => (user.value?.name || '魔').slice(0, 1))

// 境界位阶（引煞 0 → 魔祖 6，与后端 Realm 一致）
const REALM_NAMES = ['引煞', '凝煞', '魔卒', '魔尉', '魔将', '血煞', '魔祖']
const realmIndex = computed(() => {
  const i = REALM_NAMES.indexOf(user.value?.cultivation)
  return i >= 0 ? i : 0
})

const categories = ref([])
const items = ref([])
const loading = ref(false)
const kw = ref('')
const openId = ref(null)
// 境界封禁提示（后端 403 时回显，需魔将境开卷）
const deny = ref('')

// 当前分类：取自路由参数，缺省为谱系
const activeCat = computed(() => String(route.params.cat || 'lineage'))

const activeMeta = computed(() => {
  const hit = categories.value.find((c) => c.key === activeCat.value)
  return hit || { name: '魔 枢 典 册', icon: '⛧', unit: '篇' }
})

async function loadCats() {
  try {
    const res = await archiveCategories()
    if (res.data.code === 200) categories.value = res.data.data || []
  } catch (e) { /* 静默 */ }
}

async function load() {
  loading.value = true
  openId.value = null
  deny.value = ''
  try {
    const res = await archiveList(activeCat.value, kw.value)
    if (res.data.code === 200) {
      items.value = res.data.data || []
    } else {
      items.value = []
      if (res.data.code === 403) deny.value = res.data.msg || '境界不足，典册封禁。'
    }
  } catch (e) {
    items.value = []
  } finally {
    loading.value = false
  }
}

function selectCat(key) {
  if (key === activeCat.value) return
  kw.value = ''
  router.push('/archive/' + key)
}

function toggle(id) {
  openId.value = openId.value === id ? null : id
}

function goHome() {
  router.push('/home')
}
function goModule(key) {
  router.push('/module/' + key)
}
function onLogout() {
  clearSession()
  router.replace('/login')
}

onMounted(() => {
  if (!session.token) {
    router.replace('/login')
    return
  }
  loadCats()
  load()
})

watch(activeCat, () => load())
</script>

<style scoped>
.archive {
  position: fixed;
  inset: 0;
  z-index: 2;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  padding-bottom: 64px;
  background: radial-gradient(ellipse at 50% 20%, #1c0510 0%, #0b0207 55%, #050104 100%);
}
.nav, .lib-head, .cat-grid, .lib-body, .lib-foot { position: relative; z-index: 1; }

/* ===== 导航（与首页一致） ===== */
.nav {
  position: sticky; top: 0; z-index: 5;
  display: flex; align-items: center; gap: 24px;
  padding: 14px 30px;
  background: linear-gradient(180deg, rgba(10, 4, 16, 0.92), rgba(10, 4, 16, 0.55));
  border-bottom: 1px solid rgba(255, 59, 107, 0.22);
  backdrop-filter: blur(12px);
  box-shadow: 0 0 30px rgba(255, 59, 107, 0.15);
}
.brand { display: flex; flex-direction: column; line-height: 1.1; }
.brand-mark { font-size: 22px; letter-spacing: 6px; color: var(--bone); text-shadow: 0 0 12px var(--crimson-glow); }
.brand-sub { font-size: 11px; letter-spacing: 4px; color: var(--gold); opacity: 0.8; margin-top: 3px; }
.nav-links { display: flex; gap: 4px; flex: 1; flex-wrap: wrap; }
.nav-item {
  padding: 8px 12px; font-size: 14px; letter-spacing: 3px;
  color: rgba(232, 220, 200, 0.7); cursor: pointer; border-radius: 8px;
  transition: all 0.25s ease; user-select: none;
}
.nav-item:hover { color: var(--bone); background: rgba(255, 59, 107, 0.12); }
.nav-item.active {
  color: #fff;
  background: linear-gradient(135deg, rgba(179, 24, 58, 0.35), rgba(106, 44, 156, 0.35));
  box-shadow: 0 0 16px rgba(255, 59, 107, 0.3);
}
.nav-user { display: flex; align-items: center; gap: 12px; }
.user-meta { display: flex; flex-direction: column; align-items: flex-end; line-height: 1.2; }
.user-name { font-size: 14px; color: var(--gold); letter-spacing: 2px; }
.user-realm { font-size: 11px; color: rgba(232, 220, 200, 0.55); letter-spacing: 1px; }
.avatar {
  width: 38px; height: 38px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 18px; color: #fff;
  background: linear-gradient(135deg, var(--crimson), #4a0d24);
  box-shadow: 0 0 14px rgba(255, 59, 107, 0.5);
  border: 1px solid rgba(217, 176, 102, 0.4);
}
.logout {
  padding: 7px 14px; font-size: 12px; letter-spacing: 2px;
  color: rgba(232, 220, 200, 0.8); background: transparent;
  border: 1px solid rgba(255, 59, 107, 0.35); border-radius: 8px;
  cursor: pointer; font-family: inherit; transition: all 0.25s ease;
}
.logout:hover { color: #fff; border-color: var(--crimson-glow); box-shadow: 0 0 14px rgba(255, 59, 107, 0.4); }

/* ===== 板块头部 ===== */
.lib-head { text-align: center; padding: 56px 20px 10px; }
.lib-title {
  font-size: 34px; letter-spacing: 12px; color: var(--bone);
  text-shadow: 0 0 16px var(--crimson-glow), 0 0 34px rgba(255, 59, 107, 0.6);
}
.lib-sub { margin-top: 14px; font-size: 13px; letter-spacing: 3px; color: rgba(217, 176, 102, 0.8); }

/* ===== 四大分类卡片 ===== */
.cat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 18px;
  width: min(1180px, 94%);
  margin: 32px auto 0;
}
.cat-card {
  position: relative;
  display: flex; flex-direction: column;
  border-radius: 16px; overflow: hidden;
  background: linear-gradient(160deg, rgba(36, 14, 56, 0.75), rgba(10, 4, 16, 0.85));
  border: 1px solid rgba(255, 59, 107, 0.22);
  box-shadow: 0 0 26px rgba(255, 59, 107, 0.12);
  cursor: pointer;
  transition: all 0.3s ease;
}
.cat-card:hover { transform: translateY(-4px); border-color: var(--crimson-glow); box-shadow: 0 0 40px rgba(255, 59, 107, 0.32); }
.cat-card.on {
  border-color: var(--crimson-glow);
  box-shadow: 0 0 44px rgba(255, 59, 107, 0.42), inset 0 0 30px rgba(179, 24, 58, 0.15);
}
.cat-cover {
  position: relative;
  display: block;
  height: 118px;
  background-size: cover;
  background-position: center 25%;
}
.cat-cover-veil {
  position: absolute; inset: 0;
  background: linear-gradient(180deg, rgba(5, 1, 4, 0.1) 0%, rgba(10, 4, 16, 0.55) 65%, rgba(10, 4, 16, 0.95) 100%);
}
.cat-body { padding: 14px 16px 16px; }
.cat-head { display: flex; align-items: center; gap: 8px; }
.cat-icon { font-size: 18px; color: var(--crimson-glow); text-shadow: 0 0 12px var(--crimson-glow); }
.cat-name { font-size: 18px; letter-spacing: 3px; color: var(--bone); }
.cat-desc {
  margin: 10px 0 12px;
  font-size: 12px; line-height: 1.7; letter-spacing: 1px;
  color: rgba(232, 220, 200, 0.6);
  min-height: 40px;
}
.cat-foot { display: flex; align-items: center; justify-content: space-between; }
.cat-count { font-size: 12px; letter-spacing: 1px; color: var(--gold); }
.cat-go {
  font-size: 11px; letter-spacing: 2px; color: var(--crimson-glow);
  border: 1px solid rgba(255, 59, 107, 0.45); border-radius: 12px; padding: 2px 10px;
}

/* ===== 正文区 ===== */
.lib-body { width: min(1180px, 94%); margin: 34px auto 0; }
.lib-tools {
  display: flex; align-items: center; justify-content: space-between; gap: 18px;
  padding: 12px 18px; margin-bottom: 16px;
  border-radius: 12px;
  background: rgba(6, 2, 10, 0.55);
  border: 1px solid rgba(217, 176, 102, 0.18);
}
.lib-now { display: flex; align-items: center; gap: 10px; }
.now-mark { font-size: 16px; color: var(--crimson-glow); }
.now-name { font-size: 15px; letter-spacing: 3px; color: var(--bone); }
.now-count { font-size: 12px; letter-spacing: 1px; color: rgba(217, 176, 102, 0.8); }
.lib-search {
  display: flex; align-items: center; gap: 8px;
  padding: 5px 6px 5px 14px;
  border-radius: 30px;
  background: linear-gradient(160deg, rgba(36, 14, 56, 0.8), rgba(10, 4, 16, 0.88));
  border: 1px solid rgba(255, 59, 107, 0.3);
}
.lib-search .search-icon { font-size: 15px; color: var(--crimson-glow); }
.lib-search input {
  width: 220px; border: none; outline: none; background: transparent;
  color: var(--bone); font-size: 14px; font-family: inherit; letter-spacing: 2px;
}
.lib-search input::placeholder { color: rgba(232, 220, 200, 0.3); }
.lib-search button {
  padding: 8px 16px; border: none; border-radius: 24px;
  font-family: inherit; font-size: 13px; letter-spacing: 3px; color: #fff;
  background: linear-gradient(135deg, var(--crimson), #4a0d24);
  cursor: pointer;
}
.lib-clear { font-size: 12px; color: var(--crimson-glow); cursor: pointer; padding-right: 8px; }

.state {
  text-align: center; padding: 48px 0;
  font-size: 13px; letter-spacing: 3px; color: rgba(255, 59, 107, 0.7);
}
.state.deny {
  color: rgba(255, 80, 110, 0.95);
  border: 1px dashed rgba(255, 59, 107, 0.4);
  border-radius: 14px;
  background: rgba(40, 4, 18, 0.4);
  display: flex; align-items: center; justify-content: center; gap: 14px;
}
.deny-lock {
  flex-shrink: 0;
  width: 40px; height: 40px;
  border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 18px; letter-spacing: 0;
  color: rgba(255, 80, 110, 0.95);
  border: 1px solid rgba(255, 59, 107, 0.5);
  box-shadow: 0 0 14px rgba(255, 59, 107, 0.35);
}

.entries { display: flex; flex-direction: column; gap: 12px; }
.entry {
  border-radius: 14px;
  background: linear-gradient(160deg, rgba(36, 14, 56, 0.6), rgba(10, 4, 16, 0.78));
  border: 1px solid rgba(255, 59, 107, 0.18);
  transition: all 0.3s ease;
  overflow: hidden;
}
.entry:hover { border-color: rgba(255, 59, 107, 0.45); }
.entry.open { border-color: rgba(255, 59, 107, 0.55); box-shadow: 0 0 30px rgba(179, 24, 58, 0.25); }
.entry-head {
  display: flex; align-items: center; gap: 16px;
  padding: 16px 20px 10px; cursor: pointer;
}
.entry-idx {
  flex: 0 0 auto; width: 34px; height: 34px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 12px; color: var(--gold);
  border: 1px solid rgba(217, 176, 102, 0.4);
}
.entry-main { flex: 1; min-width: 0; }
.entry-title { font-size: 17px; letter-spacing: 2px; color: var(--bone); text-shadow: 0 0 10px rgba(255, 59, 107, 0.35); }
.entry-sub { font-size: 12px; letter-spacing: 1px; color: rgba(217, 176, 102, 0.75); margin-top: 5px; }
.entry-arrow { font-size: 16px; color: var(--crimson-glow); }
.entry-summary {
  padding: 0 20px 14px 70px;
  font-size: 13px; line-height: 1.8; letter-spacing: 1px;
  color: rgba(232, 220, 200, 0.65);
}
.entry-content {
  margin: 0 20px 18px;
  padding: 16px 18px;
  border-radius: 10px;
  background: rgba(4, 1, 6, 0.6);
  border-left: 3px solid var(--crimson-glow);
  font-size: 14px; line-height: 2.1; letter-spacing: 1.5px;
  color: rgba(232, 220, 200, 0.85);
  white-space: pre-line; /* 保留正文换行 */
}

.lib-foot {
  text-align: center; margin-top: 50px;
  font-size: 12px; letter-spacing: 5px; color: rgba(255, 59, 107, 0.5);
}

/* 窄屏适配 */
@media (max-width: 980px) {
  .cat-grid { grid-template-columns: repeat(2, 1fr); }
  .lib-tools { flex-direction: column; align-items: stretch; }
  .lib-search input { width: auto; flex: 1; }
}
@media (max-width: 720px) {
  .nav { flex-wrap: wrap; gap: 12px; padding: 12px 16px; }
  .nav-links { order: 3; width: 100%; overflow-x: auto; }
  .lib-title { font-size: 26px; letter-spacing: 6px; }
  .cat-grid { grid-template-columns: 1fr; }
  .entry-summary { padding-left: 20px; }
}
</style>
