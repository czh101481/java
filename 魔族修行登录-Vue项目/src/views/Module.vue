<template>
  <div class="module">
    <DemonBackground />

    <!-- 导航栏 -->
    <header class="nav">
      <div class="brand">
        <span class="brand-mark">魔族修行</span>
        <span class="brand-sub">魔 域 枢 机</span>
      </div>

      <nav class="nav-links">
        <a class="nav-item" :class="{ active: isHome }" @click="goHome">首 页</a>
        <a
          v-for="m in navModules"
          :key="m.key"
          class="nav-item"
          :class="{ active: m.key === type }"
          @click="goModule(m.key)"
        >{{ m.title }}</a>
        <a class="nav-item" @click="goArchive">魔 枢 典 册</a>
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

    <!-- 模块头部 -->
    <section class="mod-head">
      <div class="mod-icon">{{ meta.icon }}</div>
      <div class="mod-text">
        <h1 class="mod-title">{{ meta.title }}</h1>
        <p class="mod-desc">{{ meta.desc }}</p>
        <p class="mod-count">共 {{ items.length }} {{ meta.unit }}</p>
      </div>

      <form class="mod-search" @submit.prevent="load">
        <span class="search-icon">✶</span>
        <input v-model="kw" type="text" placeholder="于此殿中搜寻……" autocomplete="off" />
        <button type="submit">搜 寻</button>
        <span class="mod-clear" v-if="kw" @click="kw = ''; load()">清 除</span>
      </form>
    </section>

    <!-- 条目列表 -->
    <section class="list">
      <div v-if="loading" class="state">魔 渊 回 响 中 ……</div>

      <div v-else-if="deny" class="state deny">
        <span class="deny-lock">封</span>{{ deny }}
      </div>

      <div v-else-if="items.length === 0" class="state">
        此 殿 之 中 尚 无 记 载 。
      </div>

      <div
        v-else
        v-for="it in items"
        :key="it.id"
        class="row"
        @click="openDetail(it.id)"
      >
        <img v-if="it.img" class="row-thumb" :src="imgUrl(it.img)" alt="" loading="lazy" />
        <span class="row-id">{{ it.id }}</span>
        <div class="row-main">
          <div class="row-title">{{ it.title }}</div>
          <div class="row-summary">{{ it.summary }}</div>
        </div>
        <span class="row-extra">{{ it.extra }}</span>
        <span class="row-go">入 内 ⟶</span>
      </div>
    </section>

    <footer class="mod-foot">顺 逆 由 心 · 魔 非 魔 · 道 非 道</footer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import DemonBackground from '../components/DemonBackground.vue'
import { getSession, clearSession } from '../api/auth'
import { homeList, imgUrl } from '../api/home'

const route = useRoute()
const router = useRouter()
const session = getSession()
const user = ref(session.user)

const type = computed(() => String(route.params.type || 'art'))
const initial = computed(() => (user.value?.name || '魔').slice(0, 1))
const isHome = ref(false)

// 境界位阶（引煞 0 → 魔祖 6，与后端 Realm 一致）
const REALM_NAMES = ['引煞', '凝煞', '魔卒', '魔尉', '魔将', '血煞', '魔祖']
const realmIndex = computed(() => {
  const i = REALM_NAMES.indexOf(user.value?.cultivation)
  return i >= 0 ? i : 0
})

const META = {
  art:    { icon: '☥', title: '功 法 阁', desc: '修魔之术，万千法门', unit: '部' },
  trial:  { icon: '⚔', title: '历 练 录', desc: '入世历劫，淬炼真我', unit: '程' },
  debate: { icon: '⚛', title: '宗 门 论 道', desc: '群魔聚议，论道证心', unit: '题' },
  codex:  { icon: '✠', title: '魔 典 秘 录', desc: '上古残卷，禁忌之知', unit: '卷' }
}
const meta = computed(() => META[type.value] || META.art)
const navModules = [
  { key: 'art', title: '功 法 阁' },
  { key: 'trial', title: '历 练 录' },
  { key: 'debate', title: '宗 门 论 道' },
  { key: 'codex', title: '魔 典 秘 录' }
]

const items = ref([])
const loading = ref(false)
const kw = ref('')
// 境界封禁提示（后端 403 时回显）
const deny = ref('')

async function load() {
  loading.value = true
  deny.value = ''
  try {
    const res = await homeList(type.value, kw.value)
    if (res.data.code === 200) {
      items.value = res.data.data || []
    } else {
      items.value = []
      if (res.data.code === 403) deny.value = res.data.msg || '境界不足，此殿封禁。'
    }
  } catch (e) {
    items.value = []
  } finally {
    loading.value = false
  }
}

function goModule(key) {
  router.push('/module/' + key)
}
function goHome() {
  router.push('/home')
}
function goArchive() {
  router.push('/archive')
}
function openDetail(id) {
  router.push(`/detail/${type.value}/${id}`)
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
  load()
})

// 切换模块（导航跳转）时重新拉取
watch(type, () => {
  kw.value = ''
  load()
})
</script>

<style scoped>
.module {
  position: fixed;
  inset: 0;
  z-index: 2;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  padding-bottom: 60px;
  background: radial-gradient(ellipse at 50% 22%, #1c0510 0%, #0b0207 55%, #050104 100%);
}

/* ===== 导航 ===== */
.nav {
  position: sticky;
  top: 0;
  z-index: 5;
  display: flex;
  align-items: center;
  gap: 26px;
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
  font-size: 14px; letter-spacing: 2px; cursor: pointer;
  color: rgba(232, 220, 200, 0.6); transition: all 0.25s; padding-bottom: 3px;
  border-bottom: 1px solid transparent;
}
.nav-item:hover { color: #ff6b8f; }
.nav-item.active {
  color: #ff3b6b; border-bottom-color: #ff3b6b;
  text-shadow: 0 0 10px rgba(255, 59, 107, 0.6);
}
.nav-user { display: flex; align-items: center; gap: 12px; }
.user-meta { display: flex; flex-direction: column; align-items: flex-end; }
.user-name { font-size: 13px; color: #e8dcc8; letter-spacing: 2px; }
.user-realm { font-size: 11px; color: rgba(179, 24, 58, 0.95); margin-top: 2px; }
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
.logout:hover { color: #fff; border-color: #ff3b6b; box-shadow: 0 0 14px rgba(255, 59, 107, 0.45); }

/* ===== 模块头部 ===== */
.mod-head {
  position: relative; z-index: 1;
  display: flex; align-items: center; gap: 22px;
  margin: 34px auto 8px; padding: 22px 30px;
  width: 92%; max-width: 1080px;
  border-radius: 14px;
  background: linear-gradient(150deg, rgba(38, 6, 20, 0.72), rgba(8, 2, 10, 0.82));
  border: 1px solid rgba(255, 59, 107, 0.22);
  box-shadow: 0 0 40px rgba(179, 24, 58, 0.18), inset 0 0 30px rgba(179, 24, 58, 0.1);
}
.mod-icon {
  font-size: 38px; color: #ff3b6b;
  text-shadow: 0 0 18px rgba(255, 59, 107, 0.8);
}
.mod-text { flex: 1; }
.mod-title {
  font-size: 26px; font-weight: normal; letter-spacing: 8px; color: #e8dcc8;
  text-shadow: 0 0 16px rgba(255, 59, 107, 0.55);
}
.mod-desc { font-size: 13px; letter-spacing: 3px; color: rgba(232, 220, 200, 0.6); margin-top: 8px; }
.mod-count { font-size: 12px; letter-spacing: 2px; color: #d9b066; margin-top: 6px; }

.mod-search { position: relative; display: flex; align-items: center; gap: 8px; }
.mod-search input {
  width: 190px; padding: 9px 12px 9px 30px;
  background: rgba(6, 2, 10, 0.75);
  border: 1px solid rgba(255, 59, 107, 0.3);
  border-radius: 9px; color: #e8dcc8; font-family: inherit; font-size: 13px; outline: none;
}
.mod-search input:focus { border-color: #ff3b6b; box-shadow: 0 0 14px rgba(255, 59, 107, 0.35); }
.search-icon {
  position: absolute; left: 10px; font-size: 13px; color: #ff3b6b; pointer-events: none;
}
.mod-search button {
  padding: 9px 15px; border-radius: 9px; cursor: pointer;
  font-family: inherit; font-size: 13px; letter-spacing: 2px; color: #fff;
  border: none; background: linear-gradient(135deg, #b3183a, #4a0d24);
  box-shadow: 0 0 16px rgba(179, 24, 58, 0.5);
}
.mod-clear { font-size: 12px; color: #d9b066; cursor: pointer; letter-spacing: 1px; }

/* ===== 列表 ===== */
.list {
  position: relative; z-index: 1;
  width: 92%; max-width: 1080px; margin: 22px auto 0;
  display: flex; flex-direction: column; gap: 12px;
}
.state {
  padding: 40px; text-align: center; font-size: 13px; letter-spacing: 4px;
  color: rgba(232, 220, 200, 0.45);
}
.state.deny {
  color: rgba(255, 80, 110, 0.95);
  border: 1px dashed rgba(255, 59, 107, 0.4);
  border-radius: 14px;
  background: rgba(40, 4, 18, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
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
.row {
  display: flex; align-items: center; gap: 16px;
  padding: 16px 20px; border-radius: 12px; cursor: pointer;
  background: linear-gradient(150deg, rgba(30, 5, 18, 0.6), rgba(8, 2, 10, 0.75));
  border: 1px solid rgba(255, 59, 107, 0.16);
  transition: all 0.28s;
}
.row:hover {
  border-color: rgba(255, 59, 107, 0.55);
  box-shadow: 0 0 26px rgba(179, 24, 58, 0.28);
  transform: translateX(5px);
}
/* 列表行缩略图 */
.row-thumb {
  flex: 0 0 52px;
  width: 52px; height: 52px;
  border-radius: 10px;
  object-fit: cover;
  border: 1px solid rgba(255, 59, 107, 0.4);
  box-shadow: 0 0 12px rgba(255, 59, 107, 0.22);
}
.row-id {
  flex: 0 0 auto; width: 32px; height: 32px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 12px; color: #d9b066;
  border: 1px solid rgba(217, 176, 102, 0.4);
}
.row-main { flex: 1; min-width: 0; }
.row-title {
  font-size: 16px; letter-spacing: 2px; color: #e8dcc8;
  text-shadow: 0 0 10px rgba(255, 59, 107, 0.35);
}
.row-summary {
  font-size: 12px; color: rgba(232, 220, 200, 0.55); margin-top: 5px;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.row-extra {
  flex: 0 0 auto; padding: 4px 12px; border-radius: 20px;
  font-size: 12px; letter-spacing: 1px; color: #ff6b8f;
  border: 1px solid rgba(255, 59, 107, 0.45);
}
.row-go { flex: 0 0 auto; font-size: 12px; color: rgba(217, 176, 102, 0.75); letter-spacing: 2px; }

.mod-foot {
  text-align: center; margin-top: 44px; font-size: 12px;
  letter-spacing: 5px; color: rgba(255, 59, 107, 0.4);
}

@media (max-width: 720px) {
  .nav { flex-wrap: wrap; gap: 14px; padding: 12px 16px; }
  .nav-links { order: 3; width: 100%; overflow-x: auto; gap: 16px; }
  .mod-head { flex-wrap: wrap; }
  .mod-title { font-size: 21px; letter-spacing: 5px; }
  .row-summary { white-space: normal; }
}
</style>
