<template>
  <!-- 魔域枢机 · 首页 -->
  <div class="home">
    <!-- 暗黑魔神风背景：法相剪影 / 星盘 / 电光 / 骨刺符文 / 星尘 / 技能粒子爆发 -->
    <DemonBackground />

    <!-- 导航栏 -->
    <header class="nav">
      <div class="brand">
        <span class="brand-mark">魔族修行</span>
        <span class="brand-sub">魔 域 枢 机</span>
      </div>

      <nav class="nav-links">
        <a class="nav-item active">首 页</a>
        <a class="nav-item" @click="goModule('art')">功 法 阁</a>
        <a class="nav-item" @click="goModule('trial')">历 练 录</a>
        <a class="nav-item" @click="goModule('debate')">宗 门 论 道</a>
        <a class="nav-item" @click="goModule('codex')">魔 典 秘 录</a>
        <a class="nav-item" @click="goArchive()">魔 枢 典 册</a>
      </nav>

      <div class="nav-user">
        <div class="user-meta">
          <span class="user-name">{{ user?.name || '无名修者' }}</span>
          <span class="user-realm" v-if="user?.realm">所修 · {{ user.realm }}</span>
        </div>
        <div class="avatar">{{ initial }}</div>
        <button class="logout" type="button" @click="onLogout">断魔契</button>
      </div>
    </header>

    <!-- 魔门公告走马灯 -->
    <section class="ticker" v-if="announcements.length">
      <span class="ticker-label">{{ currentAnnouncement.level || '通告' }}</span>
      <div class="ticker-body">
        <span class="ticker-title">{{ currentAnnouncement.title }}</span>
        <span class="ticker-content">{{ currentAnnouncement.content }}</span>
      </div>
      <span class="ticker-dots">
        <i
          v-for="(a, i) in announcements"
          :key="a.id"
          :class="{ on: i === tickerIndex }"
          @click="tickerIndex = i"
        ></i>
      </span>
    </section>

    <!-- 主视觉 + 搜索栏 -->
    <section class="hero">
      <h1 class="hero-title">一 念 通 天 · 万 法 归 魔</h1>
      <p class="hero-sub">于 魔 渊 之 中 ， 搜 寻 功 法 、 历 练 与 宗 门 秘 辛</p>

      <form class="search" @submit.prevent="onSearch">
        <span class="search-icon">✶</span>
        <input
          v-model="keyword"
          type="text"
          class="search-input"
          placeholder="以真言叩问魔渊……"
          autocomplete="off"
        />
        <button type="submit" class="search-btn">搜 寻</button>
      </form>

      <!-- 检索结果 -->
      <div class="results" v-if="searchResult">
        <div class="results-head">
          <span>检 索 「 {{ searchResult.keyword }} 」 · 共 {{ searchResult.total }} 条</span>
          <span class="results-clear" @click="clearSearch">清 除</span>
        </div>

        <div class="result-group" v-for="g in searchResult.groups" :key="g.type">
          <div class="group-title">{{ g.typeName }}（{{ g.total }}）</div>
          <div class="result-item" v-for="it in g.items" :key="g.type + it.id" @click="openDetail(it)">
            <span
              class="item-thumb"
              v-if="it.img"
              :style="{ backgroundImage: `url(${imgUrl(it.img)})` }"
            ></span>
            <div class="item-main">
              <div class="item-title">{{ it.title }}</div>
              <div class="item-summary">{{ it.summary }}</div>
            </div>
            <span class="item-extra">{{ it.extra }}</span>
          </div>
        </div>

        <div class="results-empty" v-if="searchResult.groups.length === 0">
          魔 渊 深 处 未 寻 得 相 关 记 载 。
        </div>
      </div>
    </section>

    <!-- 热门修习（后端各模块最新收录混合推荐） -->
    <section class="hot" v-if="highlights.length">
      <div class="sec-title">
        <span>热 门 修 习</span>
        <em>各殿新近收录</em>
      </div>
      <div class="hot-scroll">
        <div class="hot-card" v-for="h in highlights" :key="h.type + h.id" @click="openDetail(h)">
          <span
            class="hot-cover"
            v-if="h.img"
            :style="{ backgroundImage: `url(${imgUrl(h.img)})` }"
          ></span>
          <span class="hot-type">{{ h.typeName }}</span>
          <span class="hot-title">{{ h.title }}</span>
          <span class="hot-extra">{{ h.extra }}</span>
          <span class="hot-summary">{{ h.summary }}</span>
        </div>
      </div>
    </section>

    <!-- 功能入口（展示后端数量与精选，详情页暂未开发） -->
    <section class="grid">
      <div
        v-for="m in modules"
        :key="m.key"
        class="card"
        @click="goModule(m.key)"
      >
        <div class="card-icon">{{ m.icon }}</div>
        <div class="card-title">{{ m.title }}</div>
        <div class="card-desc">{{ m.desc }}</div>
        <div class="card-count">共 {{ m.count }} {{ m.unit }}</div>
        <ul class="card-featured">
          <li v-for="f in m.featured" :key="f.id">
            <img v-if="f.img" class="feat-thumb" :src="imgUrl(f.img)" alt="" loading="lazy" />
            <span>{{ f.title }}</span>
          </li>
        </ul>
        <div class="card-state">入 殿 一 观 ⟶</div>
      </div>
    </section>

    <!-- 魔枢典册：典籍库入口，四大分类卡片 -->
    <section class="arc" v-if="archiveCats.length">
      <div class="sec-title">
        <span>魔 枢 典 册</span>
        <em>典籍库 · 万法之源</em>
      </div>
      <div class="arc-grid">
        <div class="arc-card" v-for="c in archiveCats" :key="c.key" @click="goArchive(c.key)">
          <span class="arc-cover" :style="{ backgroundImage: `url(${imgUrl(c.cover)})` }">
            <span class="arc-veil"></span>
          </span>
          <div class="arc-body">
            <div class="arc-head">
              <span class="arc-icon">{{ c.icon }}</span>
              <span class="arc-name">{{ c.name }}</span>
            </div>
            <p class="arc-desc">{{ c.desc }}</p>
            <div class="arc-foot">
              <span class="arc-count">共 {{ c.count }} {{ c.unit }}</span>
              <span class="arc-go">开 卷 ⟶</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 魔门翘楚（纯展示，静态数据） -->
    <section class="elite">
      <div class="sec-title">
        <span>魔 门 翘 楚</span>
        <em>威震群魔之辈</em>
      </div>
      <div class="elite-grid">
        <div class="elite-card" v-for="c in cultivators" :key="c.name">
          <div class="elite-avatar">{{ c.name.slice(0, 1) }}</div>
          <div class="elite-name">{{ c.name }}</div>
          <div class="elite-realm">{{ c.realm }}</div>
          <div class="elite-title">{{ c.title }}</div>
          <div class="elite-desc">{{ c.desc }}</div>
        </div>
      </div>
    </section>

    <footer class="home-foot">顺 逆 由 心 · 魔 非 魔 · 道 非 道</footer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import DemonBackground from '../components/DemonBackground.vue'
import { getSession, clearSession, me } from '../api/auth'
import { homeOverview, homeSearch, homeAnnouncements, homeHighlights, imgUrl } from '../api/home'
import { archiveCategories } from '../api/archive'

const router = useRouter()
const session = getSession()
const user = ref(session.user)
const keyword = ref('')
const searchResult = ref(null)
const searching = ref(false)

const initial = computed(() => (user.value?.name || '魔').slice(0, 1))

// 进入模块列表页（导航项与功能卡共用）
function goModule(key) {
  router.push('/module/' + key)
}

// 打开内容详情（搜索结果 / 热门修习共用）
function openDetail(it) {
  router.push('/detail/' + it.type + '/' + it.id)
}

// 进入魔枢典册（指定分类；不传则默认谱系）
function goArchive(cat) {
  router.push(cat ? '/archive/' + cat : '/archive')
}

// 魔枢典册四大分类卡片
const archiveCats = ref([])

// 公告走马灯
const announcements = ref([])
const tickerIndex = ref(0)
const currentAnnouncement = computed(() => announcements.value[tickerIndex.value] || {})
let tickerTimer = null

// 热门修习（后端混合推荐）
const highlights = ref([])

// 魔门翘楚（纯前端静态展示）
const cultivators = [
  { name: '魔祖', realm: '血煞', title: '魔门之主', desc: '一念通天，万法归魔，统御群修。' },
  { name: '血煞尊者', realm: '攻伐', title: '血河一脉', desc: '血煞之术执牛耳者，煞气滔天。' },
  { name: '幽冥夫人', realm: '护体', title: '幽冥一脉', desc: '护魂之术冠绝魔域，万法难侵。' },
  { name: '噬魂大王', realm: '炼魂', title: '炼魂宗主', desc: '锁魄炼魂，威震群魔。' }
]

// 背景符文锁链节点
const runes = ['✦', '⟡', '✧', '⚝', '✶', '✷', '⟐', '✦', '⟡', '✧']

// 背景星尘：一次性随机（位置/大小/延迟/时长），避免每次渲染重排跳动
const dust = Array.from({ length: 48 }, () => ({
  x: (Math.random() * 100).toFixed(2),
  y: (Math.random() * 100).toFixed(2),
  s: (1 + Math.random() * 2.4).toFixed(1),
  o: (0.25 + Math.random() * 0.5).toFixed(2),
  d: (Math.random() * 12).toFixed(2),
  dur: (9 + Math.random() * 12).toFixed(1)
}))

// 模块静态元信息（key 对应后端的 type）
const moduleMeta = {
  art:    { icon: '☥', title: '功 法 阁', desc: '修魔之术，万千法门', unit: '部' },
  trial:  { icon: '⚔', title: '历 练 录', desc: '入世历劫，淬炼真我', unit: '程' },
  debate: { icon: '⚛', title: '宗 门 论 道', desc: '群魔聚议，论道证心', unit: '题' },
  codex:  { icon: '✠', title: '魔 典 秘 录', desc: '上古残卷，禁忌之知', unit: '卷' }
}
const modules = ref([])

function applyOverview(o) {
  modules.value = [
    { ...moduleMeta.art,    key: 'art',    count: o.artCount,    featured: o.artFeatured },
    { ...moduleMeta.trial,  key: 'trial',  count: o.trialCount,  featured: o.trialFeatured },
    { ...moduleMeta.debate, key: 'debate', count: o.debateCount, featured: o.debateFeatured },
    { ...moduleMeta.codex,  key: 'codex',  count: o.codexCount,  featured: o.codexFeatured }
  ]
}

onMounted(async () => {
  // 无令牌则遣返魔门
  if (!session.token) {
    router.replace('/login')
    return
  }
  // 有令牌但本地无用户信息时，向保留的后端接口问询修者信息
  if (!user.value) {
    try {
      const res = await me()
      if (res.data.code === 200) user.value = res.data.data
    } catch (e) { /* 静默 */ }
  }
  // 拉取首页概览（各模块数量 + 精选）
  try {
    const res = await homeOverview()
    if (res.data.code === 200) applyOverview(res.data.data)
  } catch (e) { /* 后端未起时静默，卡片显示 0 */ }

  // 拉取魔门公告（走马灯）
  try {
    const res = await homeAnnouncements()
    if (res.data.code === 200) {
      announcements.value = res.data.data || []
      if (announcements.value.length > 1) {
        tickerTimer = setInterval(() => {
          tickerIndex.value = (tickerIndex.value + 1) % announcements.value.length
        }, 4000)
      }
    }
  } catch (e) { /* 静默 */ }

  // 拉取热门修习（各模块最新收录混合推荐）
  try {
    const res = await homeHighlights()
    if (res.data.code === 200) highlights.value = res.data.data || []
  } catch (e) { /* 静默 */ }

  // 拉取魔枢典册分类（典籍库入口卡片）
  try {
    const res = await archiveCategories()
    if (res.data.code === 200) archiveCats.value = res.data.data || []
  } catch (e) { /* 静默 */ }
})

onBeforeUnmount(() => {
  if (tickerTimer) clearInterval(tickerTimer)
})

async function onSearch() {
  const k = keyword.value.trim()
  if (!k) { searchResult.value = null; return }
  searching.value = true
  try {
    const res = await homeSearch(k)
    if (res.data.code === 200) searchResult.value = res.data.data
  } catch (e) {
    alert(e.message)
  } finally {
    searching.value = false
  }
}

function clearSearch() {
  keyword.value = ''
  searchResult.value = null
}

function onLocked(name) {
  alert(`「${name}」此殿尚未开启，静待魔主谕令。`)
}

function onLogout() {
  clearSession()
  router.replace('/login')
}
</script>

<style scoped>
.home {
  position: fixed;
  inset: 0;
  z-index: 2;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  padding-bottom: 64px;
  /* 暗黑魔神风：以红黑紫覆盖全局的紫粉底色 */
  background: radial-gradient(ellipse at 50% 22%, #1c0510 0%, #0b0207 55%, #050104 100%);
}

/* ===== 背景特效层：中心星盘 / 法阵 / 电光 / 符文锁链 / 星尘 ===== */
.fx {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  overflow: hidden;
}

/* 中心星盘 */
.fx-astrolabe {
  position: absolute;
  left: 50%;
  top: 40%;
  width: min(940px, 120vw);
  height: min(940px, 120vw);
  transform: translate(-50%, -50%);
  opacity: 0.55;
}
.fx-astrolabe .ring { fill: none; stroke: rgba(255, 59, 107, 0.3); stroke-width: 1.2; }
.fx-astrolabe .ring.dash { stroke: rgba(255, 59, 107, 0.34); stroke-dasharray: 10 16; }
.fx-astrolabe .ring.inner { stroke: rgba(217, 176, 102, 0.26); }
.fx-astrolabe .poly { fill: none; stroke: rgba(255, 59, 107, 0.24); stroke-width: 1; }
.fx-astrolabe .poly.thin { stroke: rgba(255, 182, 216, 0.2); }
.fx-astrolabe .spoke { stroke: rgba(255, 255, 255, 0.14); stroke-width: 1; }
.fx-core {
  fill: rgba(255, 182, 216, 0.55);
  filter: drop-shadow(0 0 20px rgba(255, 59, 107, 0.85));
  animation: fxPulse 4.6s ease-in-out infinite;
}
.fx-spin-slow { transform-origin: 300px 300px; animation: fxSpin 72s linear infinite; }
.fx-spin-rev { transform-origin: 300px 300px; animation: fxSpinRev 48s linear infinite; }
@keyframes fxSpin { to { transform: rotate(360deg); } }
@keyframes fxSpinRev { to { transform: rotate(-360deg); } }
@keyframes fxPulse { 0%, 100% { opacity: 0.4; } 50% { opacity: 1; } }

/* 电光（左右各一道，间歇闪现） */
.fx-arc {
  position: absolute;
  top: -5%;
  height: 110%;
  width: 32vw;
  opacity: 0.6;
}
.fx-arc.a1 { left: 1vw; }
.fx-arc.a2 { right: 1vw; transform: scaleX(-1); }
.fx-arc .bolt {
  fill: none;
  stroke: rgba(255, 59, 107, 0.55);
  stroke-width: 1.6;
  stroke-linecap: round;
  filter: drop-shadow(0 0 10px rgba(255, 59, 107, 0.75));
  stroke-dasharray: 150 620;
  stroke-dashoffset: 770;
  animation: fxBolt 6s linear infinite;
}
.fx-arc .bolt.b2 {
  stroke: rgba(255, 59, 107, 0.5);
  filter: drop-shadow(0 0 10px rgba(255, 59, 107, 0.7));
  animation-delay: 2.6s;
}
@keyframes fxBolt {
  0%, 84% { opacity: 0; stroke-dashoffset: 770; }
  87% { opacity: 1; }
  93% { opacity: 0.55; stroke-dashoffset: 0; }
  100% { opacity: 0; stroke-dashoffset: 0; }
}

/* 符文锁链（左右垂落） */
.fx-chain {
  position: absolute;
  top: 0;
  bottom: 0;
  width: 44px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
}
.fx-chain.left { left: 14px; }
.fx-chain.right { right: 14px; }
.fx-chain span {
  font-size: 15px;
  color: rgba(217, 176, 102, 0.5);
  text-shadow: 0 0 12px rgba(255, 59, 107, 0.7);
  animation: fxRune 5.6s ease-in-out infinite;
}
@keyframes fxRune {
  0%, 100% { transform: translateY(0); opacity: 0.3; }
  50% { transform: translateY(-8px); opacity: 0.85; }
}

/* 星尘（多层上浮） */
.fx-dust { position: absolute; inset: 0; }
.fx-dust i {
  position: absolute;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 0 7px rgba(255, 182, 216, 0.9);
  animation-name: fxRise;
  animation-timing-function: linear;
  animation-iteration-count: infinite;
}
@keyframes fxRise {
  0% { transform: translateY(0) scale(0.6); opacity: 0; }
  15% { opacity: 0.9; }
  100% { transform: translateY(-115vh) scale(1.1); opacity: 0; }
}

/* 内容层级：保证卡片与文字压在特效层之上（nav 本身是 sticky z-index:5） */
.ticker, .hero, .hot, .grid, .arc, .elite, .home-foot { position: relative; z-index: 1; }

/* ===== 导航栏 ===== */
.nav {
  position: sticky;
  top: 0;
  z-index: 5;
  display: flex;
  align-items: center;
  gap: 28px;
  padding: 14px 30px;
  background: linear-gradient(180deg, rgba(10, 4, 16, 0.92), rgba(10, 4, 16, 0.55));
  border-bottom: 1px solid rgba(255, 59, 107, 0.22);
  backdrop-filter: blur(12px);
  box-shadow: 0 0 30px rgba(255, 59, 107, 0.15);
}
.brand { display: flex; flex-direction: column; line-height: 1.1; }
.brand-mark {
  font-size: 22px;
  letter-spacing: 6px;
  color: var(--bone);
  text-shadow: 0 0 12px var(--crimson-glow);
}
.brand-sub {
  font-size: 11px;
  letter-spacing: 4px;
  color: var(--gold);
  opacity: 0.8;
  margin-top: 3px;
}

.nav-links { display: flex; gap: 6px; flex: 1; }
.nav-item {
  padding: 8px 14px;
  font-size: 14px;
  letter-spacing: 3px;
  color: rgba(232, 220, 200, 0.7);
  cursor: pointer;
  border-radius: 8px;
  transition: all 0.25s ease;
  user-select: none;
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
  width: 38px; height: 38px;
  border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 18px; color: #fff;
  background: linear-gradient(135deg, var(--crimson), #4a0d24);
  box-shadow: 0 0 14px rgba(255, 59, 107, 0.5);
  border: 1px solid rgba(217, 176, 102, 0.4);
}
.logout {
  padding: 7px 14px;
  font-size: 12px;
  letter-spacing: 2px;
  color: rgba(232, 220, 200, 0.8);
  background: transparent;
  border: 1px solid rgba(255, 59, 107, 0.35);
  border-radius: 8px;
  cursor: pointer;
  font-family: inherit;
  transition: all 0.25s ease;
}
.logout:hover { color: #fff; border-color: var(--crimson-glow); box-shadow: 0 0 14px rgba(255, 59, 107, 0.4); }

/* ===== 主视觉 + 搜索栏 ===== */
.hero { text-align: center; padding: 70px 20px 40px; }
.hero-title {
  font-size: 40px;
  letter-spacing: 12px;
  color: var(--bone);
  text-shadow: 0 0 16px var(--crimson-glow), 0 0 34px rgba(255, 59, 107, 0.6);
  margin-bottom: 18px;
}
.hero-sub {
  font-size: 14px;
  letter-spacing: 4px;
  color: rgba(217, 176, 102, 0.85);
  margin-bottom: 36px;
}

.search {
  display: flex;
  align-items: center;
  width: min(640px, 90%);
  margin: 0 auto;
  padding: 6px 6px 6px 18px;
  background: linear-gradient(160deg, rgba(36, 14, 56, 0.8), rgba(10, 4, 16, 0.88));
  border: 1px solid rgba(255, 59, 107, 0.35);
  border-radius: 50px;
  box-shadow: 0 0 40px rgba(255, 59, 107, 0.2), inset 0 0 24px rgba(179, 24, 58, 0.1);
  backdrop-filter: blur(10px);
  transition: all 0.3s ease;
}
.search:focus-within {
  border-color: var(--crimson-glow);
  box-shadow: 0 0 50px rgba(255, 59, 107, 0.4), inset 0 0 24px rgba(179, 24, 58, 0.14);
}
.search-icon { font-size: 18px; color: var(--crimson-glow); margin-right: 10px; }
.search-input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  color: var(--bone);
  font-size: 16px;
  font-family: inherit;
  letter-spacing: 2px;
}
.search-input::placeholder { color: rgba(232, 220, 200, 0.32); }
.search-btn {
  padding: 12px 28px;
  border: none;
  border-radius: 50px;
  font-family: inherit;
  font-size: 15px;
  letter-spacing: 4px;
  color: #fff;
  cursor: pointer;
  background: linear-gradient(135deg, var(--crimson), #4a0d24);
  box-shadow: 0 0 20px rgba(179, 24, 58, 0.5);
  transition: all 0.3s ease;
}
.search-btn:hover { transform: translateY(-1px); box-shadow: 0 0 30px rgba(255, 59, 107, 0.7); }

/* ===== 检索结果 ===== */
.results {
  width: min(720px, 92%);
  margin: 30px auto 0;
  text-align: left;
  padding: 22px 24px;
  border-radius: 16px;
  background: linear-gradient(160deg, rgba(36, 14, 56, 0.72), rgba(10, 4, 16, 0.82));
  border: 1px solid rgba(255, 59, 107, 0.24);
  box-shadow: 0 0 30px rgba(255, 59, 107, 0.14);
  backdrop-filter: blur(8px);
}
.results-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  letter-spacing: 2px;
  color: var(--gold);
  margin-bottom: 14px;
}
.results-clear {
  font-size: 12px;
  letter-spacing: 1px;
  color: rgba(255, 59, 107, 0.85);
  cursor: pointer;
  border: 1px solid rgba(255, 59, 107, 0.4);
  border-radius: 14px;
  padding: 3px 12px;
  transition: all 0.2s;
}
.results-clear:hover { color: #fff; box-shadow: 0 0 12px rgba(255, 59, 107, 0.4); }
.group-title {
  font-size: 13px;
  letter-spacing: 3px;
  color: var(--crimson-glow);
  margin: 16px 0 8px;
  padding-left: 8px;
  border-left: 3px solid var(--crimson-glow);
}
.result-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 10px 8px;
  border-bottom: 1px dashed rgba(255, 59, 107, 0.15);
}
.result-item:last-child { border-bottom: none; }
.item-main { flex: 1; min-width: 0; }
/* 检索结果缩略图 */
.item-thumb {
  flex-shrink: 0;
  width: 46px;
  height: 46px;
  border-radius: 9px;
  background-size: cover;
  background-position: center;
  border: 1px solid rgba(255, 59, 107, 0.4);
  box-shadow: 0 0 12px rgba(255, 59, 107, 0.25);
}
.item-title { font-size: 15px; color: var(--bone); letter-spacing: 1px; }
.item-summary {
  font-size: 12px;
  color: rgba(232, 220, 200, 0.6);
  margin-top: 3px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.item-extra {
  flex-shrink: 0;
  font-size: 11px;
  letter-spacing: 1px;
  color: var(--gold);
  border: 1px solid rgba(217, 176, 102, 0.4);
  border-radius: 12px;
  padding: 2px 10px;
}
.results-empty {
  text-align: center;
  font-size: 13px;
  letter-spacing: 3px;
  color: rgba(255, 59, 107, 0.7);
  padding: 18px 0;
}

/* ===== 功能入口 ===== */
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 22px;
  width: min(1080px, 92%);
  margin: 30px auto 0;
}
.card {
  padding: 28px 24px;
  border-radius: 16px;
  background: linear-gradient(160deg, rgba(36, 14, 56, 0.72), rgba(10, 4, 16, 0.82));
  border: 1px solid rgba(255, 59, 107, 0.24);
  box-shadow: 0 0 30px rgba(255, 59, 107, 0.14), inset 0 0 30px rgba(179, 24, 58, 0.08);
  backdrop-filter: blur(8px);
  cursor: pointer;
  text-align: center;
  transition: all 0.3s ease;
}
.card:hover {
  transform: translateY(-4px);
  border-color: var(--crimson-glow);
  box-shadow: 0 0 44px rgba(255, 59, 107, 0.35);
}
.card-icon {
  font-size: 34px;
  color: var(--crimson-glow);
  text-shadow: 0 0 14px var(--crimson-glow);
  margin-bottom: 12px;
}
.card-title { font-size: 19px; letter-spacing: 4px; color: var(--bone); margin-bottom: 6px; }
.card-desc { font-size: 13px; letter-spacing: 1px; color: rgba(232, 220, 200, 0.6); margin-bottom: 12px; }
.card-count {
  font-size: 13px;
  letter-spacing: 2px;
  color: var(--gold);
  margin-bottom: 10px;
}
.card-featured {
  list-style: none;
  text-align: left;
  margin: 0 auto 14px;
  max-width: 200px;
}
.card-featured li {
  font-size: 12px;
  letter-spacing: 1px;
  color: rgba(232, 220, 200, 0.75);
  padding: 4px 0;
  position: relative;
  display: flex;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
  overflow: hidden;
}
/* 功能卡精选缩略图 */
.feat-thumb {
  width: 26px;
  height: 26px;
  flex-shrink: 0;
  border-radius: 6px;
  object-fit: cover;
  border: 1px solid rgba(255, 59, 107, 0.4);
  box-shadow: 0 0 8px rgba(255, 59, 107, 0.25);
}
.card-featured li span {
  overflow: hidden;
  text-overflow: ellipsis;
}
.card-state {
  display: inline-block;
  padding: 3px 14px;
  border-radius: 20px;
  font-size: 12px;
  letter-spacing: 3px;
  color: var(--gold);
  border: 1px solid rgba(217, 176, 102, 0.5);
}

.home-foot {
  text-align: center;
  margin-top: 50px;
  font-size: 12px;
  letter-spacing: 5px;
  color: rgba(255, 59, 107, 0.5);
}

/* ===== 公告走马灯 ===== */
.ticker {
  display: flex;
  align-items: center;
  gap: 14px;
  margin: 16px auto 0;
  width: min(1080px, 92%);
  padding: 10px 18px;
  background: linear-gradient(160deg, rgba(36, 14, 56, 0.6), rgba(10, 4, 16, 0.7));
  border: 1px solid rgba(217, 176, 102, 0.25);
  border-radius: 12px;
  box-shadow: 0 0 20px rgba(255, 59, 107, 0.1);
  overflow: hidden;
}
.ticker-label {
  flex-shrink: 0;
  font-size: 12px;
  letter-spacing: 2px;
  color: #fff;
  padding: 3px 10px;
  border-radius: 10px;
  background: linear-gradient(135deg, var(--crimson), #4a0d24);
  box-shadow: 0 0 12px rgba(255, 59, 107, 0.4);
}
.ticker-body {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: baseline;
  gap: 12px;
  overflow: hidden;
}
.ticker-title { font-size: 14px; letter-spacing: 2px; color: var(--gold); white-space: nowrap; flex-shrink: 0; }
.ticker-content {
  font-size: 13px;
  color: rgba(232, 220, 200, 0.7);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.ticker-dots { flex-shrink: 0; display: flex; gap: 6px; }
.ticker-dots i {
  width: 7px; height: 7px;
  border-radius: 50%;
  background: rgba(255, 59, 107, 0.3);
  cursor: pointer;
  transition: all 0.2s;
}
.ticker-dots i.on { background: var(--crimson-glow); box-shadow: 0 0 8px var(--crimson-glow); }

/* ===== 区块标题（热门 / 翘楚 共用） ===== */
.sec-title {
  display: flex;
  align-items: baseline;
  gap: 12px;
  width: min(1080px, 92%);
  margin: 48px auto 18px;
}
.sec-title span {
  font-size: 20px;
  letter-spacing: 6px;
  color: var(--bone);
  text-shadow: 0 0 12px var(--crimson-glow);
}
.sec-title em {
  font-size: 12px;
  letter-spacing: 3px;
  color: rgba(217, 176, 102, 0.7);
  font-style: normal;
}

/* ===== 热门修习 ===== */
.hot-scroll {
  display: flex;
  gap: 18px;
  width: min(1080px, 92%);
  margin: 0 auto;
  overflow-x: auto;
  padding-bottom: 8px;
}
.hot-card {
  flex: 0 0 220px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 18px 16px;
  border-radius: 14px;
  background: linear-gradient(160deg, rgba(36, 14, 56, 0.72), rgba(10, 4, 16, 0.82));
  border: 1px solid rgba(255, 59, 107, 0.22);
  box-shadow: 0 0 24px rgba(255, 59, 107, 0.12);
  transition: all 0.3s ease;
  overflow: hidden;
}
/* 热门修习封面图 */
.hot-cover {
  height: 120px;
  margin: -6px -4px 2px;
  border-radius: 10px;
  background-size: cover;
  background-position: center;
  border: 1px solid rgba(255, 59, 107, 0.3);
  box-shadow: 0 0 16px rgba(255, 59, 107, 0.2);
}
.hot-card:hover {
  transform: translateY(-3px);
  border-color: var(--crimson-glow);
  box-shadow: 0 0 34px rgba(255, 59, 107, 0.3);
}
.hot-type {
  font-size: 11px;
  letter-spacing: 2px;
  color: var(--gold);
  border: 1px solid rgba(217, 176, 102, 0.4);
  border-radius: 10px;
  padding: 2px 8px;
  align-self: flex-start;
}
.hot-title { font-size: 16px; letter-spacing: 1px; color: var(--bone); }
.hot-extra { font-size: 11px; color: var(--crimson-glow); letter-spacing: 1px; }
.hot-summary {
  font-size: 12px;
  color: rgba(232, 220, 200, 0.6);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ===== 魔枢典册（典籍库入口） ===== */
.arc-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 18px;
  width: min(1080px, 92%);
  margin: 0 auto;
}
.arc-card {
  display: flex;
  flex-direction: column;
  border-radius: 14px;
  overflow: hidden;
  background: linear-gradient(160deg, rgba(36, 14, 56, 0.72), rgba(10, 4, 16, 0.82));
  border: 1px solid rgba(255, 59, 107, 0.22);
  box-shadow: 0 0 24px rgba(255, 59, 107, 0.12);
  cursor: pointer;
  transition: all 0.3s ease;
}
.arc-card:hover {
  transform: translateY(-4px);
  border-color: var(--crimson-glow);
  box-shadow: 0 0 38px rgba(255, 59, 107, 0.34);
}
.arc-cover {
  position: relative;
  display: block;
  height: 104px;
  background-size: cover;
  background-position: center 25%;
}
.arc-veil {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(5, 1, 4, 0.12) 0%, rgba(10, 4, 16, 0.6) 62%, rgba(10, 4, 16, 0.96) 100%);
}
.arc-body { padding: 12px 14px 14px; }
.arc-head { display: flex; align-items: center; gap: 7px; }
.arc-icon { font-size: 16px; color: var(--crimson-glow); text-shadow: 0 0 10px var(--crimson-glow); }
.arc-name { font-size: 16px; letter-spacing: 2px; color: var(--bone); }
.arc-desc {
  margin: 9px 0 12px;
  font-size: 12px;
  line-height: 1.7;
  letter-spacing: 1px;
  color: rgba(232, 220, 200, 0.6);
  min-height: 40px;
}
.arc-foot { display: flex; align-items: center; justify-content: space-between; }
.arc-count { font-size: 11px; letter-spacing: 1px; color: var(--gold); }
.arc-go {
  font-size: 11px;
  letter-spacing: 2px;
  color: var(--crimson-glow);
  border: 1px solid rgba(255, 59, 107, 0.45);
  border-radius: 12px;
  padding: 2px 9px;
}

/* ===== 魔门翘楚 ===== */
.elite-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
  width: min(1080px, 92%);
  margin: 0 auto;
}
.elite-card {
  text-align: center;
  padding: 24px 18px;
  border-radius: 16px;
  background: linear-gradient(160deg, rgba(36, 14, 56, 0.72), rgba(10, 4, 16, 0.82));
  border: 1px solid rgba(255, 59, 107, 0.22);
  box-shadow: 0 0 26px rgba(255, 59, 107, 0.12);
  transition: all 0.3s ease;
}
.elite-card:hover {
  transform: translateY(-4px);
  border-color: var(--crimson-glow);
  box-shadow: 0 0 38px rgba(255, 59, 107, 0.32);
}
.elite-avatar {
  width: 54px; height: 54px;
  margin: 0 auto 12px;
  border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 24px; color: #fff;
  background: linear-gradient(135deg, var(--crimson), #4a0d24);
  box-shadow: 0 0 16px rgba(255, 59, 107, 0.5);
  border: 1px solid rgba(217, 176, 102, 0.4);
}
.elite-name { font-size: 18px; letter-spacing: 3px; color: var(--gold); }
.elite-realm { font-size: 12px; letter-spacing: 2px; color: var(--crimson-glow); margin: 4px 0 8px; }
.elite-title { font-size: 13px; color: rgba(232, 220, 200, 0.8); letter-spacing: 1px; }
.elite-desc { font-size: 12px; color: rgba(232, 220, 200, 0.55); margin-top: 8px; line-height: 1.6; }

/* 窄屏适配 */
@media (max-width: 720px) {
  .nav { flex-wrap: wrap; gap: 14px; padding: 12px 16px; }
  .nav-links { order: 3; width: 100%; overflow-x: auto; }
  .hero-title { font-size: 28px; letter-spacing: 6px; }
  .ticker, .sec-title, .hot-scroll, .elite-grid { width: 92%; }
  .arc-grid { grid-template-columns: repeat(2, 1fr); width: 92%; }
}

/* 可点击提示：搜索结果 / 热门修习 / 功能卡 均可进入详情 */
.home .result-item,
.home .hot-card,
.home .card { cursor: pointer; }
.home .result-item:hover {
  border-color: rgba(255, 59, 107, 0.5);
  background: rgba(255, 59, 107, 0.06);
}
.home .hot-card:hover {
  border-color: rgba(255, 59, 107, 0.55);
  transform: translateY(-4px);
}
</style>
