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
          <span class="user-realm-badge" v-if="user?.cultivation" :style="badgeStyle">
            {{ user.cultivation }}境 · 第{{ myLevel + 1 }}重
          </span>
          <span class="user-realm" v-if="user?.realm">魔种 · {{ user.realm }}</span>
        </div>
        <div class="avatar" title="查看个人道牌" @click="openBadges">{{ initial }}</div>
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
          <span class="item-extra" :class="{ lock: isLockedType(it.type) }">
            {{ isLockedType(it.type) ? '🔒 需' + realmName(TYPE_REQ[it.type]) + '境' : it.extra }}
          </span>
          </div>
        </div>

        <div class="results-empty" v-if="searchResult.groups.length === 0">
          魔 渊 深 处 未 寻 得 相 关 记 载 。
        </div>
      </div>
    </section>

    <!-- 修行境界阶梯（引煞 → 魔祖，境界愈高可入之殿愈多） -->
    <section class="realm" v-if="ladder.length">
      <div class="sec-title">
        <span>修 行 境 界</span>
        <em>引煞起步 · 魔祖为尊 · 境界愈高，可入之殿愈多</em>
      </div>

      <div class="realm-ladder">
        <div
          v-for="r in ladder"
          :key="r.index"
          class="realm-node"
          :class="{ mine: r.index === myLevel, reached: r.index <= myLevel, locked: r.index > myLevel }"
        >
          <i v-if="r.index > 0" class="realm-line" :class="{ lit: r.index <= myLevel }"></i>
          <div class="realm-orb" :style="orbStyle(r)">
            <span v-if="r.index > myLevel" class="realm-lock">封</span>
            <span v-else class="realm-rank">{{ r.index + 1 }}</span>
          </div>
          <div class="realm-name">{{ r.name }}</div>
          <div class="realm-title">{{ r.title }}</div>
          <div class="realm-desc">{{ r.desc }}</div>
          <span class="realm-tag" v-if="r.index === myLevel">当 前</span>
        </div>
      </div>

      <!-- 煞气修为进度 + 冲击境界 -->
      <div class="realm-cultivate">
        <div class="realm-progress" :title="expTooltip">
          <i :style="{ width: expPercent + '%' }"></i>
          <span class="realm-progress-text">{{ expText }}</span>
        </div>
        <button
          class="realm-break"
          :class="{ ready: canBreak }"
          type="button"
          :disabled="breaking || isMaxRealm"
          @click="onBreakthrough"
        >{{ breakBtnText }}</button>
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
          <span class="hot-extra" :class="{ lock: isLockedType(h.type) }">
            {{ isLockedType(h.type) ? '🔒 需' + realmName(TYPE_REQ[h.type]) + '境' : h.extra }}
          </span>
          <span class="hot-summary">{{ h.summary }}</span>
        </div>
      </div>
    </section>

    <!-- 功能入口（境界不足的殿宇自动封禁） -->
    <section class="grid">
      <div
        v-for="m in modules"
        :key="m.key"
        class="card"
        :class="{ locked: isLockedType(m.key) }"
        @click="goModule(m.key)"
      >
        <div class="card-icon">{{ m.icon }}</div>
        <div class="card-title">{{ m.title }}</div>
        <div class="card-desc">{{ m.desc }}</div>
        <div class="card-count">共 {{ m.count }} {{ m.unit }}</div>
        <div class="card-req" :class="{ ok: !isLockedType(m.key) }">
          需 · {{ realmName(TYPE_REQ[m.key]) }}境
        </div>
        <ul class="card-featured">
          <li v-for="f in m.featured" :key="f.id">
            <img v-if="f.img" class="feat-thumb" :src="imgUrl(f.img)" alt="" loading="lazy" />
            <span>{{ f.title }}</span>
          </li>
        </ul>
        <div class="card-state">{{ isLockedType(m.key) ? '境 界 不 足 · 封' : '入 殿 一 观 ⟶' }}</div>
      </div>
    </section>

    <!-- 魔枢典册：典籍库入口，四大分类卡片 -->
    <section class="arc" v-if="archiveCats.length">
      <div class="sec-title">
        <span>魔 枢 典 册</span>
        <em>典籍库 · 万法之源</em>
      </div>
      <div class="arc-grid">
        <div
          class="arc-card"
          v-for="c in archiveCats"
          :key="c.key"
          :class="{ locked: archiveLocked }"
          @click="goArchive(c.key)"
        >
          <span class="arc-cover" :style="{ backgroundImage: `url(${imgUrl(c.cover)})` }">
            <span class="arc-veil"></span>
            <span class="arc-lock" v-if="archiveLocked">封 · 需 {{ realmName(TYPE_REQ.archive) }}境</span>
          </span>
          <div class="arc-body">
            <div class="arc-head">
              <span class="arc-icon">{{ c.icon }}</span>
              <span class="arc-name">{{ c.name }}</span>
            </div>
            <p class="arc-desc">{{ c.desc }}</p>
            <div class="arc-foot">
              <span class="arc-count">共 {{ c.count }} {{ c.unit }}</span>
              <span class="arc-go">{{ archiveLocked ? '未 达 境 界' : '开 卷 ⟶' }}</span>
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

    <!-- 魔门英杰榜（按境界位阶 + 煞气修为实时排行） -->
    <section class="rank" v-if="rankList.length">
      <div class="sec-title">
        <span>魔 门 英 杰 榜</span>
        <em>境界为尊 · 煞气为凭 · 前十列名</em>
      </div>

      <div class="rank-list">
        <div
          v-for="r in rankList"
          :key="r.rank"
          class="rank-row"
          :class="{ me: r.me, podium: r.rank <= 3 }"
        >
          <span class="rank-no">{{ rankMark(r.rank) }}</span>
          <span class="rank-avatar">{{ r.name.slice(0, 1) }}</span>
          <div class="rank-main">
            <div class="rank-name">
              {{ r.name }}
              <i v-if="r.me" class="rank-me">我</i>
            </div>
            <div class="rank-title">{{ r.title }}</div>
          </div>
          <span class="rank-realm" :style="realmTagStyle(r.index)">{{ r.cultivation }}境</span>
          <span class="rank-exp">煞气 {{ r.exp }}</span>
          <button
            v-if="!r.me"
            class="rank-spar"
            type="button"
            :disabled="sparing"
            @click.stop="onSpar(r.name)"
          >切 磋</button>
        </div>
      </div>
    </section>

    <footer class="home-foot">顺 逆 由 心 · 魔 非 魔 · 道 非 道</footer>

    <!-- 个人道牌（勋章墙弹窗） -->
    <div class="badge-mask" v-if="badgeVisible" @click.self="badgeVisible = false">
      <div class="badge-modal">
        <button class="badge-close" type="button" @click="badgeVisible = false">✕</button>

        <template v-if="badges">
          <div class="badge-head">
            <div class="badge-avatar" :style="badgeAvatarStyle">{{ badges.name.slice(0, 1) }}</div>
            <div class="badge-who">
              <div class="badge-name">{{ badges.name }}</div>
              <div class="badge-title">{{ badges.title }} · {{ badges.cultivation }}境（第 {{ badges.index + 1 }} 重）</div>
            </div>
          </div>

          <div class="badge-stats">
            <div class="stat">
              <span class="stat-num">{{ badges.exp }}</span>
              <span class="stat-label">煞气修为</span>
            </div>
            <div class="stat">
              <span class="stat-num">{{ badges.unlockCount }}/{{ badges.totalHalls }}</span>
              <span class="stat-label">可入殿宇</span>
            </div>
            <div class="stat">
              <span class="stat-num">{{ badges.exploreCount }}/{{ badges.exploreLimit }}</span>
              <span class="stat-label">今日参悟</span>
            </div>
          </div>

          <div class="medal-wall">
            <div
              v-for="m in badges.medallions"
              :key="m.index"
              class="medal"
              :class="{ earned: m.earned }"
            >
              <div class="medal-orb" :style="medalStyle(m)">
                <span v-if="m.earned">✦</span>
                <span v-else class="medal-lock">封</span>
              </div>
              <span class="medal-name">{{ m.name }}境</span>
              <span class="medal-title">{{ m.title }}</span>
            </div>
          </div>
        </template>
        <div v-else class="badge-loading">道 牌 显 影 中 ……</div>
      </div>
    </div>

    <!-- 切磋战报弹窗 -->
    <div class="badge-mask" v-if="sparReport" @click.self="sparReport = null">
      <div class="badge-modal spar-modal">
        <button class="badge-close" type="button" @click="sparReport = null">✕</button>
        <h2 class="spar-title" :class="{ win: sparReport.win, lose: !sparReport.win }">
          {{ sparReport.win ? '切 磋 得 胜' : '切 磋 落 败' }}
        </h2>
        <div class="spar-log">
          <p v-for="(line, i) in sparReport.report" :key="i">{{ line }}</p>
        </div>
        <div class="spar-delta" :class="{ win: sparReport.win }">
          我的煞气变化：{{ sparReport.myDelta >= 0 ? '+' : '' }}{{ sparReport.myDelta }}（当前 {{ sparReport.myExp }}）
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import DemonBackground from '../components/DemonBackground.vue'
import { getSession, clearSession, saveSession, me } from '../api/auth'
import { homeOverview, homeSearch, homeAnnouncements, homeHighlights, homeRealms, homeRealmStatus, explore, breakthrough, homeRank, homeBadges, sparring, imgUrl } from '../api/home'
import { archiveCategories } from '../api/archive'

const router = useRouter()
const session = getSession()
const user = ref(session.user)
const keyword = ref('')
const searchResult = ref(null)
const searching = ref(false)

const initial = computed(() => (user.value?.name || '魔').slice(0, 1))

// ==================== 境界体系（引煞 → 魔祖，共七境） ====================
// 位阶 0 起；demon_user.cultivation 存当前境界名（与魔种 realm 相互独立）
const REALM_NAMES = ['引煞', '凝煞', '魔卒', '魔尉', '魔将', '血煞', '魔祖']
const REALM_COLORS = ['#6b7a8f', '#8a5fb0', '#b3552e', '#c2944a', '#a02c48', '#ff3b6b', '#ffd76a']
// 各功能模块所需最低境界（与后端 Realm.FEATURE_REQUIREMENT 一致）
const TYPE_REQ = { art: 0, trial: 1, debate: 2, codex: 3, archive: 4 }

// 境界阶梯（先用本地兜底数据渲染，成功拉取后端后覆盖）
const ladder = ref([
  { index: 0, name: '引煞', title: '初入魔门', desc: '引煞入体，初踏魔途' },
  { index: 1, name: '凝煞', title: '煞气凝形', desc: '煞气凝形，根基渐固' },
  { index: 2, name: '魔卒', title: '魔门行伍', desc: '位列行伍，可入历练' },
  { index: 3, name: '魔尉', title: '统御百夫', desc: '百夫之长，可窥魔典' },
  { index: 4, name: '魔将', title: '坐镇一方', desc: '坐镇一方，执掌典册' },
  { index: 5, name: '血煞', title: '威震魔域', desc: '煞名远播，威震群魔' },
  { index: 6, name: '魔祖', title: '万法归魔', desc: '一念通天，统御群修' }
])
// 修者境界状态（后端返回：境界名/位阶/进度百分比/下一境）
const realmStatus = ref(null)

// 当前修者位阶（0 = 引煞；未知境界视为引煞）
const myLevel = computed(() => {
  const i = REALM_NAMES.indexOf(user.value?.cultivation)
  return i >= 0 ? i : 0
})
const myRealmName = computed(() => REALM_NAMES[myLevel.value])
const myPercent = computed(() =>
  realmStatus.value?.percent ?? Math.round((myLevel.value * 100) / (REALM_NAMES.length - 1))
)

// ==================== 煞气修为与冲境 ====================
// 冲击各境界所需煞气（与后端 Realm.THRESHOLDS 一致；索引即目标位阶）
const REALM_THRESHOLDS = [0, 50, 150, 350, 650, 1000, 1400]

const isMaxRealm = computed(() => myLevel.value >= REALM_NAMES.length - 1)
const myExp = computed(() => realmStatus.value?.exp ?? user.value?.exp ?? 0)
const nextThreshold = computed(() =>
  isMaxRealm.value ? 0 : REALM_THRESHOLDS[myLevel.value + 1]
)
const canBreak = computed(() => realmStatus.value?.canBreakthrough ?? myExp.value >= nextThreshold.value)
const breaking = ref(false)
// 每日参悟进度（后端每日上限 10 篇，跨日自动清零）
const exploreCount = computed(() => realmStatus.value?.exploreCount ?? 0)
const exploreLimit = computed(() => realmStatus.value?.exploreLimit ?? 10)
const dailyCapped = computed(() => exploreCount.value >= exploreLimit.value)
const expPercent = computed(() => {
  if (isMaxRealm.value) return 100
  if (!nextThreshold.value) return 0
  return Math.min(100, Math.round((myExp.value * 100) / nextThreshold.value))
})
const expText = computed(() => {
  const base = isMaxRealm.value
    ? `${myRealmName}境 · 魔域之巅 · 煞气 ${myExp.value}`
    : `煞气 ${myExp.value} / ${nextThreshold.value} · 冲击「${REALM_NAMES[myLevel.value + 1]}」境`
  return `${base} · 今日参悟 ${exploreCount.value}/${exploreLimit.value}${dailyCapped.value ? '（已满）' : ''}`
})
const expTooltip = computed(() =>
  isMaxRealm.value
    ? '已至魔祖之境'
    : `参悟魔域内容可积累煞气（每篇 +5），每日上限 ${exploreLimit.value} 篇`
)
const breakBtnText = computed(() => {
  if (isMaxRealm.value) return '已 至 巅 峰'
  if (breaking.value) return '冲 境 中 ……'
  if (canBreak.value) return '冲 击 境 界 ⟶'
  if (dailyCapped.value) return '今 日 已 满'
  return `还差 ${nextThreshold.value - myExp.value} 点煞气`
})

// 冲击境界：成功则刷新会话与状态，失败提示还差多少煞气
async function onBreakthrough() {
  if (breaking.value || isMaxRealm.value) return
  breaking.value = true
  try {
    const res = await breakthrough()
    if (res.data.code === 200) {
      alert(res.data.data || '突破成功！')
      // 刷新修者信息与境界状态
      try {
        const meRes = await me()
        if (meRes.data.code === 200) {
          user.value = meRes.data.data
          saveSession(session.token, user.value)
        }
      } catch (e) { /* 静默 */ }
      try {
        const stRes = await homeRealmStatus()
        if (stRes.data.code === 200) realmStatus.value = stRes.data.data
      } catch (e) { /* 静默 */ }
      // 境界变化后刷新榜单
      await loadRank()
    } else {
      alert(res.data.msg || '冲击失败。')
    }
  } catch (e) {
    alert(e.message || '魔门无应，连通失败')
  } finally {
    breaking.value = false
  }
}
// 导航栏境界徽章配色（随境界变化）
const badgeStyle = computed(() => {
  const c = REALM_COLORS[myLevel.value]
  return {
    color: c,
    borderColor: c + '66',
    textShadow: `0 0 8px ${c}99`
  }
})

function realmName(i) {
  return REALM_NAMES[i] || REALM_NAMES[0]
}

// 某模块对当前修者是否封禁
function isLockedType(t) {
  const req = TYPE_REQ[t]
  return req != null && myLevel.value < req
}
const archiveLocked = computed(() => myLevel.value < TYPE_REQ.archive)

// 境界不足的统一提示
function denyMsg(reqIndex) {
  return `境界不足，此殿封禁。需修至「${realmName(reqIndex)}」境，方可入内。`
}

// ==================== 魔门英杰榜 ====================
const rankList = ref([])

// 名次标记：前三用金/银/铜符，其余用数字
function rankMark(n) {
  return ['❶', '❷', '❸'][n - 1] || String(n)
}
// 榜单境界标签配色（随境界变化）
function realmTagStyle(i) {
  const c = REALM_COLORS[i] || REALM_COLORS[0]
  return { color: c, borderColor: c + '66', textShadow: `0 0 8px ${c}66` }
}
async function loadRank() {
  try {
    const res = await homeRank()
    if (res.data.code === 200) rankList.value = res.data.data || []
  } catch (e) { /* 静默 */ }
}

// ==================== 个人道牌（勋章墙） ====================
const badgeVisible = ref(false)
const badges = ref(null)

async function openBadges() {
  badgeVisible.value = true
  badges.value = null
  try {
    const res = await homeBadges()
    if (res.data.code === 200) badges.value = res.data.data
  } catch (e) { /* 静默，弹窗保持显影中 */ }
}
const badgeAvatarStyle = computed(() => {
  const c = REALM_COLORS[badges.value?.index ?? myLevel.value]
  return {
    background: `radial-gradient(circle at 35% 30%, ${c}, #1a030a 80%)`,
    boxShadow: `0 0 20px ${c}99`,
    borderColor: c + 'aa'
  }
})
function medalStyle(m) {
  const c = REALM_COLORS[m.index]
  return m.earned
    ? {
        background: `radial-gradient(circle at 35% 30%, ${c}, #1a030a 80%)`,
        boxShadow: `0 0 14px ${c}88`,
        borderColor: c + 'aa'
      }
    : { background: 'rgba(20, 8, 18, 0.8)' }
}

// ==================== 修者切磋 ====================
const sparing = ref(false)
const sparReport = ref(null)

async function onSpar(targetName) {
  if (sparing.value) return
  if (!confirm(`确定向「${targetName}」发起切磋？胜者 +15 煞气，败者 -10。`)) return
  sparing.value = true
  try {
    const res = await sparring(targetName)
    if (res.data.code === 200) {
      sparReport.value = res.data.data
      // 战斗改变双方煞气与排名，刷新榜单与自身状态
      await loadRank()
      try {
        const stRes = await homeRealmStatus()
        if (stRes.data.code === 200) realmStatus.value = stRes.data.data
      } catch (e) { /* 静默 */ }
    } else {
      alert(res.data.msg || '切磋无应。')
    }
  } catch (e) {
    alert(e.message || '魔门无应，连通失败')
  } finally {
    sparing.value = false
  }
}

// 阶梯节点球体配色：已达成的境界点亮，未达成的灰暗上锁
function orbStyle(r) {
  const c = REALM_COLORS[r.index]
  if (r.index <= myLevel.value) {
    return {
      background: `radial-gradient(circle at 35% 30%, ${c}, #1a030a 80%)`,
      boxShadow: `0 0 16px ${c}88`,
      borderColor: c + 'aa'
    }
  }
  return { background: 'rgba(20, 8, 18, 0.8)' }
}

// 进入模块列表页（导航项与功能卡共用；境界不足则封禁）
function goModule(key) {
  if (isLockedType(key)) {
    alert(denyMsg(TYPE_REQ[key]))
    return
  }
  router.push('/module/' + key)
}

// 打开内容详情（搜索结果 / 热门修习共用；境界不足则封禁）
function openDetail(it) {
  if (isLockedType(it.type)) {
    alert(denyMsg(TYPE_REQ[it.type]))
    return
  }
  router.push('/detail/' + it.type + '/' + it.id)
}

// 进入魔枢典册（指定分类；不传则默认谱系；需魔将境）
function goArchive(cat) {
  if (archiveLocked.value) {
    alert(denyMsg(TYPE_REQ.archive))
    return
  }
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
  // 持令则向后端问询修者信息（刷新境界字段，保持本地会话最新）
  try {
    const res = await me()
    if (res.data.code === 200) {
      user.value = res.data.data
      saveSession(session.token, user.value)
    }
  } catch (e) { /* 静默 */ }

  // 拉取境界阶梯（七境全量 + 各功能门槛）
  try {
    const res = await homeRealms()
    if (res.data.code === 200 && res.data.data?.ladder?.length) {
      ladder.value = res.data.data.ladder
    }
  } catch (e) { /* 静默，沿用本地兜底阶梯 */ }

  // 拉取修者境界状态（进度条）
  try {
    const res = await homeRealmStatus()
    if (res.data.code === 200) realmStatus.value = res.data.data
  } catch (e) { /* 静默 */ }

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

  // 拉取魔门英杰榜（境界 + 煞气排行）
  await loadRank()
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
.ticker, .hero, .realm, .hot, .grid, .arc, .elite, .rank, .home-foot { position: relative; z-index: 1; }

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
.user-realm-badge {
  margin-top: 2px;
  font-size: 11px;
  letter-spacing: 1px;
  border: 1px solid rgba(217, 176, 102, 0.5);
  border-radius: 12px;
  padding: 1px 10px;
  background: rgba(10, 4, 16, 0.6);
}
.user-realm { font-size: 11px; color: rgba(232, 220, 200, 0.45); letter-spacing: 1px; }
.avatar {
  width: 38px; height: 38px;
  border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 18px; color: #fff;
  background: linear-gradient(135deg, var(--crimson), #4a0d24);
  box-shadow: 0 0 14px rgba(255, 59, 107, 0.5);
  border: 1px solid rgba(217, 176, 102, 0.4);
  cursor: pointer;
  transition: all 0.25s ease;
}
.avatar:hover { transform: scale(1.1); box-shadow: 0 0 22px rgba(255, 59, 107, 0.8); }
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

/* ===== 修行境界阶梯 ===== */
.realm-ladder {
  display: flex;
  align-items: flex-start;
  justify-content: center;
  width: min(1080px, 96%);
  margin: 0 auto;
}
.realm-node {
  position: relative;
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 0 4px;
}
/* 节点间连接线 */
.realm-line {
  position: absolute;
  top: 24px;
  right: 50%;
  width: 100%;
  height: 2px;
  background: rgba(255, 59, 107, 0.16);
}
.realm-line.lit { background: linear-gradient(90deg, rgba(255, 59, 107, 0.6), rgba(255, 59, 107, 0.85)); }
.realm-orb {
  position: relative;
  z-index: 1;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(255, 59, 107, 0.3);
  transition: all 0.3s ease;
}
.realm-node.mine .realm-orb { animation: fxPulse 2.8s ease-in-out infinite; }
.realm-rank { font-size: 17px; color: #fff; text-shadow: 0 0 8px rgba(0, 0, 0, 0.8); }
.realm-lock { font-size: 15px; color: rgba(232, 220, 200, 0.35); letter-spacing: 0; }
.realm-name { margin-top: 10px; font-size: 15px; letter-spacing: 3px; color: var(--bone); }
.realm-node.locked .realm-name { color: rgba(232, 220, 200, 0.35); }
.realm-node.mine .realm-name { color: var(--gold); text-shadow: 0 0 10px rgba(217, 176, 102, 0.6); }
.realm-title { margin-top: 4px; font-size: 11px; letter-spacing: 2px; color: rgba(232, 220, 200, 0.55); }
.realm-desc {
  margin-top: 5px;
  font-size: 10px;
  line-height: 1.5;
  letter-spacing: 1px;
  color: rgba(232, 220, 200, 0.35);
  min-height: 30px;
}
.realm-tag {
  margin-top: 6px;
  font-size: 10px;
  letter-spacing: 2px;
  color: #fff;
  padding: 1px 10px;
  border-radius: 10px;
  background: linear-gradient(135deg, var(--crimson), #4a0d24);
  box-shadow: 0 0 10px rgba(255, 59, 107, 0.5);
}
.realm-cultivate {
  display: flex;
  align-items: center;
  gap: 16px;
  width: min(880px, 92%);
  margin: 26px auto 0;
}
.realm-progress {
  position: relative;
  flex: 1;
  height: 8px;
  border-radius: 6px;
  background: rgba(255, 59, 107, 0.12);
  border: 1px solid rgba(255, 59, 107, 0.25);
  overflow: hidden;
}
.realm-progress i {
  display: block;
  height: 100%;
  border-radius: 6px;
  background: linear-gradient(90deg, #6b7a8f, #b3552e, var(--crimson), var(--gold));
  box-shadow: 0 0 12px rgba(255, 59, 107, 0.7);
  transition: width 0.8s ease;
}
.realm-progress-text {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  letter-spacing: 2px;
  color: rgba(255, 255, 255, 0.75);
  text-shadow: 0 0 6px rgba(0, 0, 0, 0.9);
}
/* 冲击境界按钮 */
.realm-break {
  flex-shrink: 0;
  padding: 9px 20px;
  font-family: inherit;
  font-size: 12px;
  letter-spacing: 3px;
  color: rgba(232, 220, 200, 0.6);
  background: transparent;
  border: 1px solid rgba(255, 59, 107, 0.35);
  border-radius: 20px;
  cursor: not-allowed;
  transition: all 0.3s ease;
}
.realm-break.ready {
  color: #fff;
  cursor: pointer;
  background: linear-gradient(135deg, var(--crimson), #4a0d24);
  border-color: var(--crimson-glow);
  box-shadow: 0 0 18px rgba(255, 59, 107, 0.55);
  animation: fxPulse 2.2s ease-in-out infinite;
}
.realm-break.ready:hover { transform: translateY(-1px); box-shadow: 0 0 28px rgba(255, 59, 107, 0.8); }
.realm-break:disabled { opacity: 0.6; }

/* ===== 功能卡境界门槛 ===== */
.card-req {
  margin: -4px 0 12px;
  font-size: 11px;
  letter-spacing: 2px;
  color: rgba(255, 59, 107, 0.85);
  border: 1px dashed rgba(255, 59, 107, 0.45);
  border-radius: 12px;
  display: inline-block;
  padding: 2px 12px;
}
.card-req.ok { color: var(--gold); border-color: rgba(217, 176, 102, 0.45); }
/* 封禁态功能卡：整体压暗、去光泽 */
.card.locked { opacity: 0.55; filter: grayscale(0.6); }
.card.locked:hover { transform: none; border-color: rgba(255, 59, 107, 0.24); box-shadow: 0 0 30px rgba(255, 59, 107, 0.14); }
.card.locked .card-state { color: rgba(255, 59, 107, 0.8); border-color: rgba(255, 59, 107, 0.5); }

/* ===== 典册卡封禁 ===== */
.arc-card.locked { opacity: 0.55; filter: grayscale(0.6); }
.arc-card.locked:hover { transform: none; border-color: rgba(255, 59, 107, 0.22); box-shadow: 0 0 24px rgba(255, 59, 107, 0.12); }
.arc-lock {
  position: absolute;
  inset: auto 0 8px;
  text-align: center;
  font-size: 11px;
  letter-spacing: 2px;
  color: rgba(255, 80, 110, 0.95);
  text-shadow: 0 0 8px rgba(0, 0, 0, 0.9);
}
.arc-card.locked .arc-go { color: rgba(232, 220, 200, 0.5); border-color: rgba(232, 220, 200, 0.3); }

/* ===== 检索/热门锁定标注 ===== */
.item-extra.lock, .hot-extra.lock {
  color: rgba(255, 59, 107, 0.85);
  border-color: rgba(255, 59, 107, 0.45);
}

/* ===== 魔门英杰榜 ===== */
.rank-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: min(720px, 92%);
  margin: 0 auto;
}
.rank-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 18px;
  border-radius: 12px;
  background: linear-gradient(160deg, rgba(36, 14, 56, 0.6), rgba(10, 4, 16, 0.75));
  border: 1px solid rgba(255, 59, 107, 0.2);
  box-shadow: 0 0 18px rgba(255, 59, 107, 0.08);
  transition: all 0.25s ease;
}
.rank-row:hover { border-color: rgba(255, 59, 107, 0.5); transform: translateX(4px); }
/* 前三名：金边高亮 */
.rank-row.podium {
  border-color: rgba(217, 176, 102, 0.5);
  box-shadow: 0 0 24px rgba(217, 176, 102, 0.18);
  background: linear-gradient(160deg, rgba(60, 44, 12, 0.35), rgba(10, 4, 16, 0.8));
}
/* 本人行：红框高亮 */
.rank-row.me {
  border-color: var(--crimson-glow);
  box-shadow: 0 0 26px rgba(255, 59, 107, 0.4);
  background: linear-gradient(160deg, rgba(80, 10, 34, 0.5), rgba(10, 4, 16, 0.8));
}
.rank-no {
  flex-shrink: 0;
  width: 30px;
  text-align: center;
  font-size: 17px;
  color: var(--gold);
  text-shadow: 0 0 10px rgba(217, 176, 102, 0.6);
}
.rank-avatar {
  flex-shrink: 0;
  width: 38px;
  height: 38px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  color: #fff;
  background: linear-gradient(135deg, var(--crimson), #4a0d24);
  border: 1px solid rgba(217, 176, 102, 0.4);
  box-shadow: 0 0 12px rgba(255, 59, 107, 0.4);
}
.rank-main { flex: 1; min-width: 0; }
.rank-name { font-size: 15px; letter-spacing: 2px; color: var(--bone); display: flex; align-items: center; gap: 8px; }
.rank-me {
  font-style: normal;
  font-size: 10px;
  letter-spacing: 1px;
  color: #fff;
  padding: 1px 8px;
  border-radius: 8px;
  background: linear-gradient(135deg, var(--crimson), #4a0d24);
  box-shadow: 0 0 8px rgba(255, 59, 107, 0.5);
}
.rank-title { font-size: 11px; letter-spacing: 1px; color: rgba(232, 220, 200, 0.45); margin-top: 3px; }
.rank-realm {
  flex-shrink: 0;
  font-size: 12px;
  letter-spacing: 1px;
  border: 1px solid rgba(255, 59, 107, 0.4);
  border-radius: 12px;
  padding: 2px 12px;
  background: rgba(10, 4, 16, 0.6);
}
.rank-exp {
  flex-shrink: 0;
  width: 86px;
  text-align: right;
  font-size: 12px;
  letter-spacing: 1px;
  color: var(--gold);
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

/* ===== 个人道牌（勋章墙弹窗） ===== */
.badge-mask {
  position: fixed;
  inset: 0;
  z-index: 50;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(3, 0, 4, 0.8);
  backdrop-filter: blur(6px);
}
.badge-modal {
  position: relative;
  width: min(560px, 92%);
  max-height: 86vh;
  overflow-y: auto;
  padding: 30px 28px;
  border-radius: 18px;
  background: linear-gradient(160deg, rgba(36, 14, 56, 0.95), rgba(10, 4, 16, 0.98));
  border: 1px solid rgba(255, 59, 107, 0.45);
  box-shadow: 0 0 60px rgba(255, 59, 107, 0.35), inset 0 0 40px rgba(179, 24, 58, 0.12);
}
.badge-close {
  position: absolute;
  top: 14px;
  right: 16px;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  border: 1px solid rgba(255, 59, 107, 0.4);
  background: transparent;
  color: rgba(232, 220, 200, 0.7);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}
.badge-close:hover { color: #fff; border-color: var(--crimson-glow); box-shadow: 0 0 12px rgba(255, 59, 107, 0.5); }
.badge-head { display: flex; align-items: center; gap: 16px; margin-bottom: 22px; }
.badge-avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: #fff;
  border: 1px solid rgba(255, 59, 107, 0.5);
}
.badge-name { font-size: 20px; letter-spacing: 4px; color: var(--bone); text-shadow: 0 0 12px var(--crimson-glow); }
.badge-title { font-size: 12px; letter-spacing: 2px; color: var(--gold); margin-top: 6px; }
.badge-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 24px;
}
.stat {
  text-align: center;
  padding: 12px 6px;
  border-radius: 12px;
  background: rgba(255, 59, 107, 0.06);
  border: 1px solid rgba(255, 59, 107, 0.2);
}
.stat-num { display: block; font-size: 20px; color: var(--gold); text-shadow: 0 0 10px rgba(217, 176, 102, 0.5); }
.stat-label { display: block; font-size: 11px; letter-spacing: 2px; color: rgba(232, 220, 200, 0.5); margin-top: 5px; }
.medal-wall {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
}
.medal { text-align: center; opacity: 0.4; }
.medal.earned { opacity: 1; }
.medal-orb {
  width: 52px;
  height: 52px;
  margin: 0 auto;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: #fff;
  border: 1px solid rgba(255, 59, 107, 0.3);
  transition: all 0.3s ease;
}
.medal.earned .medal-orb:hover { transform: scale(1.08); }
.medal-lock { font-size: 14px; color: rgba(232, 220, 200, 0.35); letter-spacing: 0; }
.medal-name { display: block; margin-top: 8px; font-size: 13px; letter-spacing: 2px; color: var(--bone); }
.medal-title { display: block; font-size: 10px; letter-spacing: 1px; color: rgba(232, 220, 200, 0.4); margin-top: 3px; }
.badge-loading { text-align: center; padding: 40px 0; font-size: 13px; letter-spacing: 4px; color: rgba(232, 220, 200, 0.5); }

/* ===== 修者切磋 ===== */
.rank-spar {
  flex-shrink: 0;
  padding: 5px 12px;
  font-family: inherit;
  font-size: 11px;
  letter-spacing: 2px;
  color: rgba(255, 59, 107, 0.9);
  background: transparent;
  border: 1px solid rgba(255, 59, 107, 0.45);
  border-radius: 14px;
  cursor: pointer;
  transition: all 0.25s ease;
}
.rank-spar:hover:not(:disabled) {
  color: #fff;
  background: linear-gradient(135deg, var(--crimson), #4a0d24);
  border-color: var(--crimson-glow);
  box-shadow: 0 0 14px rgba(255, 59, 107, 0.5);
}
.rank-spar:disabled { opacity: 0.4; cursor: wait; }
.spar-modal { width: min(480px, 92%); }
.spar-title {
  margin: 0 0 18px;
  text-align: center;
  font-size: 24px;
  font-weight: normal;
  letter-spacing: 8px;
}
.spar-title.win { color: var(--gold); text-shadow: 0 0 16px rgba(217, 176, 102, 0.8); }
.spar-title.lose { color: var(--crimson-glow); text-shadow: 0 0 16px rgba(255, 59, 107, 0.8); }
.spar-log {
  padding: 16px 18px;
  border-radius: 12px;
  background: rgba(6, 2, 10, 0.5);
  border-left: 2px solid rgba(255, 59, 107, 0.55);
}
.spar-log p {
  margin: 0 0 10px;
  font-size: 13px;
  line-height: 1.9;
  letter-spacing: 1px;
  color: rgba(232, 220, 200, 0.85);
}
.spar-log p:last-child { margin-bottom: 0; color: var(--bone); }
.spar-delta {
  margin-top: 16px;
  text-align: center;
  font-size: 13px;
  letter-spacing: 2px;
  color: rgba(255, 59, 107, 0.9);
}
.spar-delta.win { color: var(--gold); }

/* 窄屏适配 */
@media (max-width: 720px) {
  .nav { flex-wrap: wrap; gap: 14px; padding: 12px 16px; }
  .nav-links { order: 3; width: 100%; overflow-x: auto; }
  .hero-title { font-size: 28px; letter-spacing: 6px; }
  .ticker, .sec-title, .hot-scroll, .elite-grid, .rank-list { width: 92%; }
  .medal-wall { grid-template-columns: repeat(3, 1fr); }
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
