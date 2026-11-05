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
        <a class="nav-item" @click="goArchive">魔 枢 典 册</a>
        <a class="nav-item" @click="goCultivate">修 为</a>
      </nav>

      <div class="nav-user">
        <div class="user-meta">
          <span class="user-name">{{ user?.name || '无名修者' }}</span>
          <span class="user-realm" v-if="user?.cultivation">{{ user.cultivation }}境 · 第{{ user.layer || 1 }}重</span>
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

        <div class="hero-img" v-if="info.img" :style="{ backgroundImage: `url(${imgUrl(info.img)})` }">
          <span class="hero-img-veil"></span>
        </div>

        <div class="meta">
          <div class="meta-row" v-for="m in info.meta" :key="m.label">
            <span class="m-label">{{ m.label }}</span>
            <span class="m-value">{{ m.value }}</span>
          </div>
        </div>

        <div class="divider">
          <span>{{ type === 'trial' ? '秘 境 纪 事' : '秘 传 正 文' }}</span>
        </div>
        <div class="content">{{ info.content }}</div>

        <!-- v2：参悟此篇（每篇每日一次）/ 研读本篇 / 功法习得 -->
        <div class="act-row">
          <button
            v-if="type === 'art' || type === 'codex' || type === 'archive'"
            class="act-btn explore"
            type="button"
            :disabled="exploredToday || exploring"
            @click="onExploreThis"
          >{{ exploredToday ? '今 日 已 参 悟' : exploring ? '参 悟 中 ……' : '参 悟 此 篇' }}</button>
          <button
            class="act-btn"
            type="button"
            :disabled="readDone || reading"
            @click="onRead"
          >{{ readDone ? readDoneLabel : reading ? (type === 'trial' ? '勘 察 中 ……' : '研 读 中 ……') : readLabel }}</button>
          <template v-if="type === 'art' && !artInfo?.owned">
            <button class="act-btn art" type="button" :disabled="buying" @click="onBuy(0)">
              购 完 整 · {{ artInfo ? artInfo.priceFull + ' ' + artInfo.currencyName : '—' }}<i>可修至无上</i>
            </button>
            <button class="act-btn art frag" type="button" :disabled="buying" @click="onBuy(1)">
              购 残 片 · {{ artInfo ? artInfo.priceFrag + ' ' + artInfo.currencyName : '—' }}<i>三成五价 · 止步大成</i>
            </button>
          </template>
          <span v-if="actMsg" class="act-msg">{{ actMsg }}</span>
        </div>

        <!-- 功法：六重进度（入门→无上）+ 修炼 / 顿悟 / 破境 / 补全 -->
        <template v-if="type === 'art' && artInfo?.owned">
          <div class="divider"><span>{{ artInfo.formName === '残片' ? '残 篇 · 功 候' : '功 候 · 六 重' }}</span></div>
          <div class="art-prog">
            <div class="ap-head">
              <span class="ap-form" :class="{ frag: artInfo.form === 1 }">{{ artInfo.formName }}</span>
              <span class="ap-stage">{{ artInfo.stageName }}</span>
              <em>{{ artInfo.form === 1
                ? `残篇威能折损约四成五，最高止步${artInfo.fragMaxStageName}`
                : '完整卷 · 可修至无上' }}</em>
            </div>

            <div class="ap-dots">
              <span
                v-for="(s, i) in artInfo.stageNames"
                :key="s"
                class="ap-dot"
                :class="{ on: i + 1 <= artInfo.stage, cur: i + 1 === artInfo.stage }"
              >{{ s }}</span>
            </div>

            <div v-if="artInfo.stage < 4" class="ap-bar">
              <i :style="{ width: artInfo.profPercent + '%' }"></i>
              <span>熟练 {{ artInfo.prof }} / {{ artInfo.profNeed }}（{{ artInfo.profPercent }}%）</span>
            </div>
            <div class="ap-hint">
              {{ artInfo.nextHint }}
              <b v-if="artInfo.stage < 4">· 今日尚可修炼 {{ artInfo.trainLeft }} 课（每课耗 10 煞气）</b>
            </div>

            <div class="ap-acts">
              <button
                v-if="artInfo.canTrain"
                class="act-btn art"
                type="button"
                :disabled="artBusy"
                @click="onCultivate"
              >{{ artBusy ? '运 功 中 ……' : (artInfo.stage >= 5 ? '以 煞 养 心' : '修 炼 一 课') }}</button>
              <button
                v-if="artInfo.canInsight"
                class="act-btn art"
                type="button"
                :disabled="artBusy"
                @click="onInsight"
              >顿 悟 入 道</button>
              <button
                v-if="artInfo.canTranscend"
                class="act-btn art"
                type="button"
                :disabled="artBusy"
                @click="onTranscend"
              >死 战 破 境</button>
              <button
                v-if="artInfo.canComplete"
                class="act-btn art frag"
                type="button"
                :disabled="artBusy"
                @click="onComplete"
              >补 全 残 篇</button>
            </div>

            <div class="ap-stats">
              <span>攻伐 +{{ artInfo.atk }}</span>
              <span>护体 +{{ artInfo.def }}</span>
              <span>气血 +{{ artInfo.hp }}</span>
              <span>悟性 +{{ artInfo.insight }}</span>
              <span>身法 +{{ artInfo.agility }}</span>
              <span class="pw">战力 +{{ artInfo.power }}<i v-if="artInfo.factor < 1">（满值 +{{ artInfo.basePower }}）</i></span>
            </div>
          </div>
        </template>

        <!-- 域外秘境：此间非典籍，是可踏入的秘境——进入即结算 -->
        <template v-if="type === 'trial'">
          <div class="divider"><span>踏 入 秘 境</span></div>
          <div class="realm">
            <div class="realm-stat">
              <span>今日余 <b>{{ realmState ? realmState.left : 0 }} / {{ realmState?.dailyLimit || 0 }}</b></span>
              <span>入场 <b>{{ realmState?.costExp ?? 0 }}</b> 煞气</span>
              <span>产出 <b>{{ realmState?.rewardMin ?? 0 }} ~ {{ realmState?.rewardMax ?? 0 }}</b></span>
              <span>机缘 <b>{{ realmState?.treasureRate ?? 0 }}%</b></span>
              <span>凶险 <b>{{ realmState?.riskRate ?? 0 }}%</b></span>
            </div>

            <button
              class="act-btn realm-btn"
              type="button"
              :disabled="!realmState?.canEnter || entering"
              @click="onEnterRealm"
            >{{ entering ? '探 索 中 ……' : realmState?.canEnter ? '进 入 秘 境' : '不 可 入' }}</button>

            <div v-if="realmMsg" class="act-msg">{{ realmMsg }}</div>
            <div v-else-if="realmState && !realmState.canEnter" class="act-msg deny">{{ realmState.deny }}</div>

            <div v-if="realmLog.length" class="realm-log">
              <div class="rl-title">近 日 战 报</div>
              <div v-for="l in realmLog" :key="l.id" class="rl-row">
                <span class="rl-time">{{ l.createdAt.slice(5, 16) }}</span>
                <span class="rl-text">{{ l.detail }}</span>
                <span
                  class="rl-num"
                  :class="l.gain - l.cost >= 0 ? 'up' : 'down'"
                >{{ l.gain - l.cost >= 0 ? '+' : '' }}{{ l.gain - l.cost }}</span>
              </div>
            </div>
          </div>
        </template>

        <!-- 宗门论道 · 答题论道（答对 +10 煞气，答错 -5） -->
        <template v-if="type === 'debate' && quiz">
          <div class="divider"><span>论 道 问 答</span></div>
          <div class="quiz">
            <p class="quiz-lead">听罢 {{ quiz.speaker }} 之论，道友以为如何？</p>

            <button
              v-for="(opt, i) in quiz.options"
              :key="i"
              class="quiz-opt"
              type="button"
              :class="{
                picked: pick === letters[i],
                right: answered && letters[i] === quizResult?.answer,
                wrong: answered && pick === letters[i] && !quizResult?.correct
              }"
              :disabled="answered"
              @click="onAnswer(letters[i])"
            >
              <span class="opt-letter">{{ letters[i] }}</span>
              <span class="opt-text">{{ opt }}</span>
            </button>

            <div v-if="quizResult" class="quiz-result" :class="{ ok: quizResult.correct }">
              <div class="quiz-msg">{{ quizResult.msg }}</div>
              <div class="quiz-analysis">论者解析：{{ quizResult.analysis }}</div>
              <div v-if="quizResult.quizLimit" class="quiz-analysis">
                今日第 {{ quizResult.quizCount }} 论 ·
                {{ quizResult.quizFull ? '全额之赏' : '赏按' + quizResult.decayLabel + '计' }}
                · 全额 {{ quizResult.quizLimit }} 论，尚余 {{ quizResult.quizRemain }} 论
              </div>
              <div class="quiz-analysis">同一论题每日可辩一次，跨日再来；超额论道所得递减。</div>
            </div>
            <p v-else class="quiz-tip">同一论题每日可辩一次 · 前十论全额，其后递减 · 答对得煞与魔晶，答错反噬</p>
          </div>
        </template>

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
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import DemonBackground from '../components/DemonBackground.vue'
import { getSession, clearSession } from '../api/auth'
import { homeDetail, explore, debateQuiz, debateAnswer, imgUrl, readDoc, secretRealms, enterRealm, myRealmLog } from '../api/home'
import { buyArt, completeArt, cultivateArt, insightArt, transcendArt } from '../api/art'
// 境界位阶（引煞 0 → 无上 9）：来自 ../config/realm，由后端 demon_realm 表驱动
import { REALM_NAMES } from '../config/realm'

const route = useRoute()
const router = useRouter()
const session = getSession()
const user = ref(session.user)

const type = computed(() => String(route.params.type || 'art'))
const id = computed(() => Number(route.params.id || 0))

const realmIndex = computed(() => {
  const i = REALM_NAMES.indexOf(user.value?.cultivation)
  return i >= 0 ? i : 0
})
const initial = computed(() => (user.value?.name || '魔').slice(0, 1))

const navModules = [
  { key: 'art', title: '功 法 阁' },
  { key: 'trial', title: '域 外 秘 境' },
  { key: 'debate', title: '宗 门 论 道' },
  { key: 'codex', title: '魔 典 秘 录' }
]

const info = ref(null)
const loading = ref(false)
const artInfo = computed(() => info.value?.art || null)

async function load() {
  loading.value = true
  try {
    const res = await homeDetail(type.value, id.value)
    if (res.data.code === 200) {
      info.value = res.data.data
      // 功法：后端回传是否已习得，直接决定按钮状态
      if (type.value === 'art') learnDone.value = !!info.value.owned
      // 论道页额外拉取问答题目（401/403 时静默，正文照常展示）
      if (type.value === 'debate') {
        try {
          const q = await debateQuiz(id.value)
          if (q.data.code === 200) quiz.value = q.data.data
        } catch (e) { /* 境界不足或接口异常时隐藏答题区 */ }
      }
    } else info.value = null
  } catch (e) {
    info.value = null
  } finally {
    loading.value = false
  }
  await loadRealm()
}

// ==================== 域外秘境：玩法状态 / 进入结算 / 战报 ====================
const realmState = ref(null)   // 本秘境的今日次数与可否进入
const realmLog = ref([])       // 本人于此境的近期战报
const entering = ref(false)
const realmMsg = ref('')

async function loadRealm() {
  realmState.value = null
  realmLog.value = []
  if (type.value !== 'trial') return
  try {
    const res = await secretRealms()
    if (res.data.code === 200) {
      realmState.value = (res.data.data?.list || []).find(r => r.id === id.value) || null
    }
    const lg = await myRealmLog(20)
    if (lg.data.code === 200) {
      realmLog.value = (lg.data.data?.list || []).filter(x => x.trialId === id.value).slice(0, 5)
    }
  } catch (e) { /* 静默 */ }
}

async function onEnterRealm() {
  if (!realmState.value?.canEnter || entering.value) return
  entering.value = true
  realmMsg.value = ''
  try {
    const res = await enterRealm(id.value)
    if (res.data.code === 200) {
      const d = res.data.data
      realmMsg.value = `${d.detail}（净得 ${d.net >= 0 ? '+' : ''}${d.net} 煞气）`
      await loadRealm()
    } else {
      realmMsg.value = res.data.msg || '秘境之门未启。'
    }
  } catch (e) {
    realmMsg.value = e.message || '魔门无应，连通失败'
  } finally {
    entering.value = false
  }
}

// ==================== v2：研读 / 习得 ====================
// 秘境不是典籍：同一接口在秘境页改称「勘察」（仍计入图鉴收集度）
const readLabel = computed(() => (type.value === 'trial' ? '勘 察 此 境' : '研 读 本 篇'))
const readDoneLabel = computed(() => (type.value === 'trial' ? '今 日 已 勘 察' : '今 日 已 读'))
const reading = ref(false)
const learning = ref(false)
const readDone = ref(false)
const learnDone = ref(false)
const actMsg = ref('')
const exploring = ref(false)
const exploredToday = ref(false)

async function onRead() {
  if (reading.value || readDone.value) return
  reading.value = true
  try {
    const res = await readDoc(type.value, id.value)
    if (res.data.code === 200) {
      readDone.value = true
      const d = res.data.data
      actMsg.value = d.msg
        + (d.newToCollection ? ' · 首次录入图鉴' : '')
        + (d.collection ? ` · 收集度 ${d.collection.percent}%` : '')
    } else {
      // 仅「今日已读」算完成态；其余错误允许重试
      const m = res.data.msg || ''
      if (m.includes('今日已')) readDone.value = true
      actMsg.value = m
    }
  } catch (e) {
    actMsg.value = e.message || '研读无应。'
  } finally {
    reading.value = false
  }
}

// ==================== 功法：完整 / 残片 购得 + 六重修炼 ====================
const buying = ref(false)
const artBusy = ref(false)

async function afterArt(res, okText) {
  if (res.data.code === 200) {
    actMsg.value = okText || res.data.data.msg
    await load()
    return true
  }
  actMsg.value = res.data.msg || '功法无应。'
  return false
}

// 购得：form 0 完整 / 1 残片
async function onBuy(form) {
  if (buying.value) return
  buying.value = true
  try {
    const res = await buyArt(id.value, form)
    await afterArt(res)
  } catch (e) {
    actMsg.value = e.message || '购得无应。'
  } finally {
    buying.value = false
  }
}

// 修炼一课
async function onCultivate() {
  if (artBusy.value) return
  artBusy.value = true
  try {
    await afterArt(await cultivateArt(id.value))
  } catch (e) {
    actMsg.value = e.message || '运功无应。'
  } finally {
    artBusy.value = false
  }
}

// 圆满 → 入道（耗「悟道机缘」）
async function onInsight() {
  if (artBusy.value) return
  artBusy.value = true
  try {
    await afterArt(await insightArt(id.value))
  } catch (e) {
    actMsg.value = e.message || '顿悟无应。'
  } finally {
    artBusy.value = false
  }
}

// 入道 → 无上（耗「死战悟道」）
async function onTranscend() {
  if (artBusy.value) return
  artBusy.value = true
  try {
    await afterArt(await transcendArt(id.value))
  } catch (e) {
    actMsg.value = e.message || '破境无应。'
  } finally {
    artBusy.value = false
  }
}

// 残片补全为完整功法
async function onComplete() {
  if (artBusy.value) return
  artBusy.value = true
  try {
    await afterArt(await completeArt(id.value))
  } catch (e) {
    actMsg.value = e.message || '补全无应。'
  } finally {
    artBusy.value = false
  }
}

// 参悟此篇：每篇每日仅可参悟一次，每日总量受上限约束
async function onExploreThis() {
  if (exploring.value || exploredToday.value) return
  exploring.value = true
  try {
    const res = await explore(type.value, id.value)
    if (res.data.code === 200) {
      const d = res.data.data
      exploredToday.value = true
      actMsg.value = `参悟《${d.title}》得 ${d.gain} 煞气` + (d.insight ? '（顿悟·双倍）' : '')
    } else {
      // 今日已参悟此篇 → 置为已参悟态，避免反复点击
      if (res.data.code === 400 && res.data.msg && res.data.msg.indexOf('已参悟') >= 0) {
        exploredToday.value = true
      }
      actMsg.value = res.data.msg || '参悟无应。'
    }
  } catch (e) {
    actMsg.value = e.message || '参悟无应。'
  } finally {
    exploring.value = false
  }
}

// ==================== 论道问答 ====================
const letters = ['A', 'B', 'C']
const quiz = ref(null)
const pick = ref('')
const answered = ref(false)
const quizResult = ref(null)

async function onAnswer(letter) {
  if (answered.value) return
  pick.value = letter
  try {
    const res = await debateAnswer(id.value, letter)
    if (res.data.code === 200) {
      quizResult.value = res.data.data
      answered.value = true
    } else {
      alert(res.data.msg || '论道无应。')
    }
  } catch (e) {
    alert(e.message || '魔门无应，连通失败')
  }
}

function goModule(key) { router.push('/module/' + key) }
function goHome() { router.push('/home') }
function goArchive() { router.push('/archive') }
function goCultivate() { router.push('/cultivate') }
function onLogout() { clearSession(); router.replace('/login') }

onMounted(() => {
  if (!session.token) {
    router.replace('/login')
    return
  }
  load()
})

// 同一路由组件内切换详情（如从一部功法跳另一部）：重置页内状态后重载
watch([() => route.params.type, () => route.params.id], () => {
  if (!session.token) return
  readDone.value = false
  exploredToday.value = false
  actMsg.value = ''
  quiz.value = null
  pick.value = ''
  answered.value = false
  quizResult.value = null
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

/* 详情主图（后端 image 字段） */
.hero-img {
  position: relative;
  height: 300px;
  margin-top: 24px;
  border-radius: 14px;
  background-size: cover;
  background-position: center 22%;
  border: 1px solid rgba(255, 59, 107, 0.35);
  box-shadow: 0 0 34px rgba(255, 59, 107, 0.28);
  overflow: hidden;
}
/* 底部渐隐：让图与暗底融合 */
.hero-img-veil {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(180deg, rgba(5, 1, 4, 0.15) 0%, rgba(5, 1, 4, 0) 40%, rgba(5, 1, 4, 0.75) 100%);
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

/* ===== 宗门论道 · 答题区 ===== */
.quiz { display: flex; flex-direction: column; gap: 10px; }
.quiz-lead {
  font-size: 13px; letter-spacing: 2px;
  color: rgba(217, 176, 102, 0.9);
  margin-bottom: 4px;
}
.quiz-opt {
  display: flex; align-items: center; gap: 12px;
  padding: 13px 16px;
  font-family: inherit; text-align: left;
  border-radius: 11px; cursor: pointer;
  color: rgba(232, 220, 200, 0.85);
  background: linear-gradient(150deg, rgba(30, 5, 18, 0.6), rgba(8, 2, 10, 0.75));
  border: 1px solid rgba(255, 59, 107, 0.3);
  transition: all 0.25s ease;
}
.quiz-opt:hover:not(:disabled) {
  border-color: rgba(255, 59, 107, 0.7);
  box-shadow: 0 0 18px rgba(255, 59, 107, 0.3);
  transform: translateX(4px);
}
.quiz-opt:disabled { cursor: default; opacity: 0.75; }
.quiz-opt.picked { border-color: rgba(255, 59, 107, 0.8); }
.quiz-opt.right {
  border-color: rgba(102, 204, 102, 0.75);
  background: rgba(20, 60, 20, 0.35);
  opacity: 1;
  box-shadow: 0 0 16px rgba(102, 204, 102, 0.25);
}
.quiz-opt.wrong {
  border-color: rgba(255, 59, 107, 0.9);
  background: rgba(80, 6, 24, 0.5);
  opacity: 1;
  box-shadow: 0 0 16px rgba(255, 59, 107, 0.4);
}
.opt-letter {
  flex-shrink: 0;
  width: 26px; height: 26px;
  border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 12px; color: var(--det-gold, #d9b066);
  border: 1px solid rgba(217, 176, 102, 0.5);
}
.opt-text { font-size: 14px; letter-spacing: 1px; }
.quiz-tip {
  font-size: 11px; letter-spacing: 2px;
  color: rgba(232, 220, 200, 0.35);
  text-align: center;
}
.quiz-result {
  padding: 14px 18px;
  border-radius: 11px;
  border: 1px solid rgba(255, 59, 107, 0.5);
  background: rgba(50, 6, 22, 0.5);
}
.quiz-result.ok {
  border-color: rgba(102, 204, 102, 0.6);
  background: rgba(18, 50, 22, 0.45);
}
.quiz-msg { font-size: 14px; letter-spacing: 2px; color: #fff; }
.quiz-analysis {
  font-size: 12px; line-height: 1.8; letter-spacing: 1px;
  color: rgba(232, 220, 200, 0.7);
  margin-top: 8px;
}

@media (max-width: 720px) {
  .nav { flex-wrap: wrap; gap: 14px; padding: 12px 16px; }
  .nav-links { order: 3; width: 100%; overflow-x: auto; gap: 16px; }
  .panel { padding: 24px 20px 28px; }
  .title { font-size: 24px; letter-spacing: 4px; }
}
/* ==================== v2：研读 / 习得 ==================== */
.act-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 22px;
  flex-wrap: wrap;
}
.act-btn {
  padding: 9px 20px;
  cursor: pointer;
  border: 1px solid rgba(255, 59, 107, 0.45);
  border-radius: 8px;
  background: rgba(255, 59, 107, 0.1);
  color: #ffd9e2;
  font-family: inherit;
  font-size: 13px;
  letter-spacing: 3px;
  transition: 0.2s;
}
.act-btn:hover:not(:disabled) { background: rgba(255, 59, 107, 0.25); }
.act-btn.art { border-color: rgba(217, 176, 102, 0.5); background: rgba(217, 176, 102, 0.1); color: var(--gold); }
.act-btn.art:hover:not(:disabled) { background: rgba(217, 176, 102, 0.22); }
.act-btn.art i {
  display: block; font-style: normal; font-size: 10px; letter-spacing: 1px;
  color: rgba(232, 220, 200, 0.45); margin-top: 3px;
}
.act-btn.art.frag { border-color: rgba(167, 139, 250, 0.5); background: rgba(90, 60, 160, 0.12); color: #d6c8ff; }
.act-btn.art.frag:hover:not(:disabled) { background: rgba(110, 75, 190, 0.24); }

/* ===== 功法 · 六重进度 ===== */
.art-prog {
  padding: 18px 22px;
  border-radius: 12px;
  background: linear-gradient(150deg, rgba(34, 6, 20, 0.7), rgba(8, 2, 10, 0.85));
  border: 1px solid rgba(217, 176, 102, 0.26);
}
.ap-head { display: flex; align-items: center; gap: 12px; }
.ap-form {
  padding: 2px 10px; border-radius: 10px; font-size: 11px; letter-spacing: 2px;
  color: #ffe3a8; border: 1px solid rgba(217, 176, 102, 0.5); background: rgba(120, 85, 10, 0.22);
}
.ap-form.frag { color: #d6c8ff; border-color: rgba(167, 139, 250, 0.5); background: rgba(70, 40, 130, 0.26); }
.ap-stage { font-size: 16px; letter-spacing: 4px; color: #ffd9a0; }
.ap-head em { font-style: normal; font-size: 11px; letter-spacing: 1px; color: rgba(232, 220, 200, 0.4); }

.ap-dots { display: flex; gap: 8px; margin-top: 14px; }
.ap-dot {
  flex: 1; text-align: center; padding: 6px 0;
  font-size: 11px; letter-spacing: 1px;
  color: rgba(232, 220, 200, 0.3);
  border: 1px solid rgba(255, 59, 107, 0.18);
  border-radius: 6px; background: rgba(10, 2, 8, 0.55);
}
.ap-dot.on { color: #ffd9e2; border-color: rgba(255, 59, 107, 0.5); background: rgba(120, 12, 40, 0.45); }
.ap-dot.cur { color: #fff; border-color: #ff8aa6; box-shadow: 0 0 12px rgba(255, 59, 107, 0.45); }

.ap-bar {
  position: relative; height: 22px; margin-top: 12px;
  border-radius: 11px; overflow: hidden;
  background: rgba(10, 2, 8, 0.75); border: 1px solid rgba(255, 59, 107, 0.2);
}
.ap-bar i {
  position: absolute; inset: 0 auto 0 0;
  background: linear-gradient(90deg, rgba(179, 24, 58, 0.85), rgba(255, 120, 150, 0.9));
  transition: width 0.5s ease;
}
.ap-bar span {
  position: relative; z-index: 1; display: block; text-align: center; line-height: 22px;
  font-size: 11px; color: #ffe6ec; text-shadow: 0 1px 4px rgba(0, 0, 0, 0.85);
}
.ap-hint { margin-top: 10px; font-size: 12px; letter-spacing: 1px; color: rgba(232, 220, 200, 0.62); }
.ap-hint b { font-weight: normal; color: rgba(255, 159, 180, 0.9); }
.ap-acts { display: flex; flex-wrap: wrap; gap: 10px; margin-top: 14px; }
.ap-stats {
  display: flex; flex-wrap: wrap; gap: 14px; margin-top: 14px;
  font-size: 11px; letter-spacing: 1px; color: rgba(196, 181, 253, 0.85);
}
.ap-stats .pw { color: #ffd9a0; }
.ap-stats .pw i { font-style: normal; font-size: 10px; color: rgba(232, 220, 200, 0.4); }
.act-btn.explore { border-color: rgba(167, 139, 250, 0.55); background: rgba(167, 139, 250, 0.12); color: #c4b5fd; }
.act-btn.explore:hover:not(:disabled) { background: rgba(167, 139, 250, 0.26); }
.act-btn:disabled { opacity: 0.45; cursor: not-allowed; }
.act-msg { font-size: 12px; letter-spacing: 1px; color: rgba(232, 220, 200, 0.6); }
.act-msg.deny { color: rgba(255, 107, 143, 0.85); }

/* ===== 域外秘境 ===== */
.realm { margin-top: 14px; }
.realm-stat {
  display: flex; flex-wrap: wrap; gap: 16px; margin-bottom: 14px;
  font-size: 12px; letter-spacing: 1px; color: rgba(232, 220, 200, 0.55);
}
.realm-stat b { font-weight: normal; color: #e8dcc8; }
.realm-btn {
  border-color: rgba(255, 59, 107, 0.6);
  background: linear-gradient(135deg, rgba(179, 24, 58, 0.55), rgba(74, 13, 36, 0.5));
  color: #ffd9e2;
  letter-spacing: 4px;
}
.realm-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #b3183a, #4a0d24);
  box-shadow: 0 0 22px rgba(255, 59, 107, 0.5);
}
.realm-log { margin-top: 18px; }
.rl-title { font-size: 12px; letter-spacing: 4px; color: var(--gold); margin-bottom: 8px; }
.rl-row {
  display: flex; align-items: baseline; gap: 12px;
  padding: 8px 0; border-top: 1px dashed rgba(255, 59, 107, 0.16);
  font-size: 12px; line-height: 1.7;
}
.rl-time { flex: 0 0 auto; color: rgba(217, 176, 102, 0.7); font-size: 11px; }
.rl-text { flex: 1; color: rgba(232, 220, 200, 0.68); }
.rl-num { flex: 0 0 auto; font-size: 12px; }
.rl-num.up { color: #ff6b8a; }
.rl-num.down { color: #7ee0a0; }
</style>
