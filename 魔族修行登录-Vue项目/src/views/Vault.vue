<template>
  <!-- 藏宝阁：魔晶 / 血玉 双币 · 商店 · 乾坤袋 · 收支簿 -->
  <div class="vault">
    <div class="va-aura" aria-hidden="true"></div>

    <header class="va-head">
      <button class="va-back" type="button" @click="goHome">⟵ 返 回 魔 域</button>
      <div class="va-title">
        <span>藏 宝 阁</span>
        <small>以魔晶易物 · 以血玉换珍</small>
      </div>
      <div class="va-wallet">
        <div class="wc wc-coin" title="魔晶：签到、参悟、论道、切磋、秘境皆可得">
          <span class="wc-icon">晶</span>
          <div class="wc-body">
            <b>{{ wallet.coin }}</b>
            <small>魔 晶</small>
          </div>
        </div>
        <div class="wc wc-jade" title="血玉：连签满七日、秘境机缘方得">
          <span class="wc-icon">玉</span>
          <div class="wc-body">
            <b>{{ wallet.jade }}</b>
            <small>血 玉</small>
          </div>
        </div>
      </div>
    </header>

    <nav class="va-tabs">
      <button type="button" class="va-tab" :class="{ on: tab === 'shop' }" @click="tab = 'shop'">
        藏 宝 阁<em v-if="shop.length">{{ shop.length }}</em>
      </button>
      <button type="button" class="va-tab" :class="{ on: tab === 'bag' }" @click="switchBag">
        乾 坤 袋<em v-if="bagTotal">{{ bagTotal }}</em>
      </button>
      <button type="button" class="va-tab" :class="{ on: tab === 'logs' }" @click="switchLogs">
        收 支 簿
      </button>
      <span class="va-today">
        今日：魔晶 +{{ wallet.todayCoinIn }} / −{{ wallet.todayCoinOut }}
        · 血玉 +{{ wallet.todayJadeIn }} / −{{ wallet.todayJadeOut }}
      </span>
    </nav>

    <main class="va-body">
      <div v-if="loading" class="va-empty">魔 气 凝 聚 中 ……</div>

      <!-- ==================== 商店 ==================== -->
      <template v-else-if="tab === 'shop'">
        <div v-if="!shop.length" class="va-empty">藏 宝 阁 暂 无 在 售 之 物 。</div>
        <div v-else class="shop-grid">
          <article
            v-for="it in shop"
            :key="it.id"
            class="shop-card"
            :class="{ sold: it.soldOut, poor: !it.affordable && !it.soldOut }"
          >
            <div class="sc-top">
              <div class="sc-face" :class="catClass(it.category)">{{ it.icon }}</div>
              <div class="sc-meta">
                <div class="sc-name">{{ it.name }}</div>
                <div class="sc-cat">{{ it.category }}</div>
              </div>
              <span v-if="it.held" class="sc-held">持 {{ it.held }}</span>
            </div>

            <p class="sc-desc">{{ it.desc }}</p>
            <div class="sc-effect">{{ it.effectText }}</div>

            <div class="sc-limit">
              <template v-if="it.dailyLimit > 0">
                今日已购 {{ it.boughtToday }} / {{ it.dailyLimit }}
                <i v-if="it.soldOut">· 已售罄</i>
              </template>
              <template v-else>不限购</template>
            </div>

            <div class="sc-buy">
              <div class="qty">
                <button type="button" :disabled="qtyOf(it) <= 1" @click="stepQty(it, -1)">−</button>
                <span>{{ qtyOf(it) }}</span>
                <button type="button" :disabled="it.soldOut" @click="stepQty(it, 1)">＋</button>
              </div>
              <button
                type="button"
                class="sc-btn"
                :class="it.payCurrency === 'jade' ? 'jade' : 'coin'"
                :disabled="busy || it.soldOut || !it.affordable"
                @click="doBuy(it)"
              >
                <template v-if="it.soldOut">已 售 罄</template>
                <template v-else-if="!it.affordable">不 足</template>
                <template v-else>
                  {{ it.price * qtyOf(it) }} <b>{{ it.payCurrency === 'jade' ? '血玉' : '魔晶' }}</b>
                </template>
              </button>
            </div>
          </article>
        </div>
      </template>

      <!-- ==================== 乾坤袋 ==================== -->
      <template v-else-if="tab === 'bag'">
        <div v-if="!bag.length" class="va-empty">乾 坤 袋 中 空 无 一 物 。 去 藏 宝 阁 看 看 。</div>
        <div v-else class="bag-grid">
          <article v-for="it in bag" :key="it.id" class="bag-card">
            <div class="sc-face small" :class="catClass(it.category)">{{ it.icon }}</div>
            <div class="bag-main">
              <div class="bag-name">{{ it.name }}<i>×{{ it.qty }}</i></div>
              <div class="sc-cat">{{ it.category }} · {{ it.effectText }}</div>
              <p class="sc-desc">{{ it.desc }}</p>
            </div>
            <div class="bag-acts">
              <div class="qty">
                <button type="button" :disabled="qtyOf(it) <= 1" @click="stepQty(it, -1)">−</button>
                <span>{{ qtyOf(it) }}</span>
                <button type="button" :disabled="qtyOf(it) >= it.qty" @click="stepQty(it, 1)">＋</button>
              </div>
              <button type="button" class="use-btn" :disabled="busy" @click="doUse(it)">
                取 用 ×{{ qtyOf(it) }}
              </button>
            </div>
          </article>
        </div>
      </template>

      <!-- ==================== 收支簿 ==================== -->
      <template v-else>
        <div v-if="!logs.length" class="va-empty">尚 无 一 笔 流 水 。</div>
        <div v-else class="log-list">
          <div v-for="l in logs" :key="l.id" class="log-row" :class="l.currency">
            <span class="lg-cur">{{ l.currency === 'jade' ? '玉' : '晶' }}</span>
            <span class="lg-reason">{{ l.reasonText }}</span>
            <span class="lg-amount" :class="{ plus: l.amount > 0, minus: l.amount < 0 }">
              {{ l.amount > 0 ? '+' : '' }}{{ l.amount }}
            </span>
            <span class="lg-balance">余 {{ l.balance }}</span>
            <span class="lg-time">{{ fmtTime(l.createdAt) }}</span>
          </div>
        </div>
      </template>
    </main>

    <transition name="vtoast">
      <div v-if="toast" class="va-toast" :class="{ bad: toastBad }">{{ toast }}</div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getSession } from '../api/auth'
import { walletOverview, coinLogs, shopList, buyItem, bagList, useItem } from '../api/coin'

const router = useRouter()
const session = getSession()

const tab = ref('shop')
const loading = ref(true)
const busy = ref(false)
const toast = ref('')
const toastBad = ref(false)
let toastTimer = null

const wallet = ref({
  coin: 0, jade: 0,
  todayCoinIn: 0, todayCoinOut: 0, todayJadeIn: 0, todayJadeOut: 0
})
const shop = ref([])
const bag = ref([])
const logs = ref([])

// 每个物品的下单/取用数量
const qtys = ref({})
const qtyOf = (it) => qtys.value[it.id] || 1
function stepQty(it, delta) {
  const cur = qtyOf(it)
  const max = it.dailyLimit > 0 && tab.value === 'shop'
    ? Math.max(1, it.remainToday === -1 ? 99 : it.remainToday)
    : (tab.value === 'bag' ? Math.max(1, it.qty) : 99)
  qtys.value = { ...qtys.value, [it.id]: Math.min(max, Math.max(1, cur + delta)) }
}

const CAT_CLASS = { '丹药': 'pill', '符箓': 'talisman', '奇物': 'relic', '珍品': 'treasure' }
const catClass = (c) => 'cat-' + (CAT_CLASS[c] || 'pill')

const bagTotal = computed(() => bag.value.reduce((s, b) => s + (b.qty || 0), 0))

function showToast(msg, bad = false) {
  toast.value = msg
  toastBad.value = bad
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => { toast.value = '' }, 2600)
}

async function loadWallet() {
  try {
    const res = await walletOverview()
    if (res.data.code === 200) wallet.value = res.data.data
  } catch (e) { /* 静默 */ }
}

async function loadShop() {
  try {
    const res = await shopList()
    if (res.data.code === 200) shop.value = res.data.data.list || []
  } catch (e) { /* 静默 */ }
}

async function loadBag() {
  try {
    const res = await bagList()
    if (res.data.code === 200) bag.value = res.data.data.list || []
  } catch (e) { /* 静默 */ }
}

async function loadLogs() {
  try {
    const res = await coinLogs(50)
    if (res.data.code === 200) logs.value = res.data.data.list || []
  } catch (e) { /* 静默 */ }
}

async function switchBag() {
  tab.value = 'bag'
  qtys.value = {}
  await loadBag()
}

async function switchLogs() {
  tab.value = 'logs'
  await loadLogs()
}

async function doBuy(it) {
  if (busy.value || it.soldOut) return
  const q = qtyOf(it)
  busy.value = true
  try {
    const res = await buyItem(it.id, q)
    if (res.data.code === 200) {
      showToast(res.data.data.msg || '已入乾坤袋')
      qtys.value = { ...qtys.value, [it.id]: 1 }
      await Promise.all([loadWallet(), loadShop(), loadBag()])
    } else {
      showToast(res.data.msg || '购宝失败', true)
    }
  } catch (e) {
    showToast(e.message || '购宝失败', true)
  } finally {
    busy.value = false
  }
}

async function doUse(it) {
  if (busy.value) return
  const q = Math.min(qtyOf(it), it.qty)
  busy.value = true
  try {
    const res = await useItem(it.id, q)
    if (res.data.code === 200) {
      showToast(res.data.data.msg || '已生效')
      qtys.value = { ...qtys.value, [it.id]: 1 }
      await Promise.all([loadWallet(), loadBag()])
    } else {
      showToast(res.data.msg || '取用失败', true)
    }
  } catch (e) {
    showToast(e.message || '取用失败', true)
  } finally {
    busy.value = false
  }
}

function fmtTime(t) {
  if (!t) return ''
  const s = String(t).replace('T', ' ')
  return s.length >= 16 ? s.slice(5, 16) : s
}

function goHome() {
  router.push('/home')
}

onMounted(async () => {
  if (!session.token) {
    router.replace('/login')
    return
  }
  await Promise.all([loadWallet(), loadShop(), loadBag()])
  loading.value = false
})
</script>

<style scoped>
.vault {
  position: fixed;
  inset: 0;
  z-index: 2;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background:
    radial-gradient(ellipse at 50% 8%, #1d0a14 0%, #0b0207 55%, #050104 100%);
  color: var(--bone);
}
.va-aura {
  position: absolute;
  inset: -20% -10% auto -10%;
  height: 60%;
  pointer-events: none;
  background:
    radial-gradient(circle at 30% 40%, rgba(255, 59, 107, 0.18), transparent 60%),
    radial-gradient(circle at 72% 30%, rgba(176, 102, 255, 0.16), transparent 60%);
  filter: blur(10px);
}

/* ===== 顶部 ===== */
.va-head {
  position: relative;
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 18px 26px 14px;
  border-bottom: 1px solid rgba(176, 102, 255, 0.18);
}
.va-back {
  padding: 8px 14px;
  border: 1px solid rgba(217, 176, 102, 0.35);
  border-radius: 8px;
  background: rgba(20, 8, 14, 0.7);
  color: rgba(232, 220, 200, 0.75);
  font-family: inherit;
  font-size: 12px;
  letter-spacing: 2px;
  cursor: pointer;
  transition: 0.2s;
}
.va-back:hover { color: #fff; border-color: var(--gold); box-shadow: 0 0 14px rgba(217, 176, 102, 0.35); }
.va-title { display: flex; flex-direction: column; gap: 4px; }
.va-title span {
  font-size: 24px;
  letter-spacing: 8px;
  color: var(--bone);
  text-shadow: 0 0 16px var(--crimson-glow);
}
.va-title small { font-size: 11px; letter-spacing: 2px; color: rgba(232, 220, 200, 0.45); }

.va-wallet { margin-left: auto; display: flex; gap: 12px; }
.wc {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 132px;
  padding: 8px 16px 8px 10px;
  border-radius: 12px;
  border: 1px solid rgba(217, 176, 102, 0.25);
  background: linear-gradient(135deg, rgba(40, 12, 24, 0.9), rgba(14, 5, 10, 0.9));
}
.wc-icon {
  width: 32px; height: 32px;
  flex: none;
  display: flex; align-items: center; justify-content: center;
  border-radius: 50%;
  font-size: 15px;
  color: #1a030a;
  font-weight: 700;
}
.wc-coin .wc-icon { background: linear-gradient(135deg, #ffe6a3, #d9b066); box-shadow: 0 0 14px rgba(217, 176, 102, 0.6); }
.wc-jade .wc-icon { background: linear-gradient(135deg, #ff9db8, #b3183a); color: #fff1f4; box-shadow: 0 0 14px rgba(255, 59, 107, 0.6); }
.wc-body { display: flex; flex-direction: column; line-height: 1.15; }
.wc-body b { font-size: 19px; color: #fff; letter-spacing: 1px; }
.wc-body small { font-size: 10px; letter-spacing: 3px; color: rgba(232, 220, 200, 0.5); }

/* ===== 页签 ===== */
.va-tabs {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 26px 0;
}
.va-tab {
  position: relative;
  padding: 9px 22px;
  border: 1px solid rgba(176, 102, 255, 0.2);
  border-bottom: none;
  border-radius: 10px 10px 0 0;
  background: transparent;
  color: rgba(232, 220, 200, 0.55);
  font-family: inherit;
  font-size: 14px;
  letter-spacing: 4px;
  cursor: pointer;
  transition: 0.22s;
}
.va-tab:hover { color: var(--bone); }
.va-tab.on {
  color: #fff;
  background: linear-gradient(180deg, rgba(179, 24, 58, 0.35), rgba(30, 8, 16, 0.2));
  border-color: rgba(255, 59, 107, 0.45);
  box-shadow: 0 -2px 18px rgba(255, 59, 107, 0.25);
}
.va-tab em {
  margin-left: 6px;
  padding: 0 6px;
  border-radius: 9px;
  background: var(--crimson-glow);
  color: #1a030a;
  font-size: 11px;
  font-style: normal;
  letter-spacing: 0;
}
.va-today {
  margin-left: auto;
  font-size: 11px;
  letter-spacing: 1px;
  color: rgba(232, 220, 200, 0.4);
}

/* ===== 主体 ===== */
.va-body {
  position: relative;
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 22px 26px 40px;
  border-top: 1px solid rgba(176, 102, 255, 0.18);
}
.va-empty {
  padding: 60px 0;
  text-align: center;
  font-size: 13px;
  letter-spacing: 4px;
  color: rgba(232, 220, 200, 0.38);
}

/* ===== 商品卡 ===== */
.shop-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 16px;
}
.shop-card {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 16px;
  border-radius: 14px;
  border: 1px solid rgba(176, 102, 255, 0.2);
  background: linear-gradient(160deg, rgba(28, 10, 22, 0.92), rgba(11, 3, 9, 0.94));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.04);
  transition: 0.25s;
}
.shop-card:hover {
  border-color: rgba(255, 59, 107, 0.4);
  box-shadow: 0 0 24px rgba(255, 59, 107, 0.16);
  transform: translateY(-2px);
}
.shop-card.sold { opacity: 0.55; }
.shop-card.poor { border-color: rgba(120, 100, 110, 0.25); }

.sc-top { display: flex; align-items: center; gap: 12px; }
.sc-face {
  width: 46px; height: 46px;
  flex: none;
  display: flex; align-items: center; justify-content: center;
  border-radius: 12px;
  font-size: 22px;
  color: #fff;
  border: 1px solid rgba(255, 255, 255, 0.14);
  background: linear-gradient(135deg, #b3183a, #3a0a16);
  box-shadow: 0 0 16px rgba(255, 59, 107, 0.35);
}
.sc-face.small { width: 40px; height: 40px; font-size: 19px; border-radius: 10px; }
.sc-face.cat-pill { background: linear-gradient(135deg, #ff5e5e, #4a0d1c); }
.sc-face.cat-talisman { background: linear-gradient(135deg, #b06bff, #2a0b45); box-shadow: 0 0 16px rgba(176, 102, 255, 0.45); }
.sc-face.cat-relic { background: linear-gradient(135deg, #3ad6c9, #062a2a); box-shadow: 0 0 16px rgba(58, 214, 201, 0.4); }
.sc-face.cat-treasure { background: linear-gradient(135deg, #d9b066, #3a2a08); color: #fff6dd; box-shadow: 0 0 18px rgba(217, 176, 102, 0.55); }

.sc-meta { min-width: 0; }
.sc-name { font-size: 16px; letter-spacing: 2px; color: var(--bone); }
.sc-cat { font-size: 11px; letter-spacing: 2px; color: rgba(217, 176, 102, 0.7); margin-top: 3px; }
.sc-held {
  margin-left: auto;
  padding: 2px 8px;
  border-radius: 8px;
  border: 1px solid rgba(176, 102, 255, 0.35);
  font-size: 11px;
  color: rgba(232, 220, 200, 0.65);
}

.sc-desc { font-size: 12px; line-height: 1.6; color: rgba(232, 220, 200, 0.55); min-height: 38px; }
.sc-effect {
  padding: 4px 10px;
  border-radius: 7px;
  background: rgba(255, 59, 107, 0.08);
  border: 1px solid rgba(255, 59, 107, 0.16);
  font-size: 11px;
  letter-spacing: 1px;
  color: #ffb3c4;
}
.sc-limit { font-size: 11px; color: rgba(232, 220, 200, 0.42); }
.sc-limit i { color: #ff8fa8; font-style: normal; }

.sc-buy { display: flex; align-items: center; gap: 8px; margin-top: auto; }
.qty {
  display: flex;
  align-items: center;
  gap: 2px;
  border: 1px solid rgba(176, 102, 255, 0.25);
  border-radius: 8px;
  overflow: hidden;
}
.qty button {
  width: 26px; height: 28px;
  border: none;
  background: rgba(176, 102, 255, 0.12);
  color: var(--bone);
  font-size: 14px;
  cursor: pointer;
  transition: 0.15s;
}
.qty button:hover:not(:disabled) { background: rgba(255, 59, 107, 0.28); color: #fff; }
.qty button:disabled { opacity: 0.3; cursor: not-allowed; }
.qty span { min-width: 26px; text-align: center; font-size: 13px; color: #fff; }

.sc-btn {
  flex: 1;
  height: 30px;
  border-radius: 8px;
  border: 1px solid transparent;
  font-family: inherit;
  font-size: 12px;
  letter-spacing: 1px;
  cursor: pointer;
  transition: 0.2s;
}
.sc-btn b { font-weight: 400; opacity: 0.75; font-size: 11px; }
.sc-btn.coin { background: linear-gradient(135deg, #d9b066, #8a6420); color: #1c1203; }
.sc-btn.jade { background: linear-gradient(135deg, #ff5e85, #8a0f22); color: #fff3f5; }
.sc-btn:hover:not(:disabled) { filter: brightness(1.15); box-shadow: 0 0 16px rgba(255, 59, 107, 0.35); }
.sc-btn:disabled {
  background: rgba(255, 255, 255, 0.06);
  border-color: rgba(255, 255, 255, 0.08);
  color: rgba(232, 220, 200, 0.35);
  cursor: not-allowed;
}

/* ===== 乾坤袋 ===== */
.bag-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 14px; }
.bag-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px;
  border-radius: 12px;
  border: 1px solid rgba(176, 102, 255, 0.18);
  background: linear-gradient(160deg, rgba(26, 9, 20, 0.9), rgba(10, 3, 8, 0.92));
}
.bag-main { flex: 1; min-width: 0; }
.bag-name { font-size: 15px; letter-spacing: 2px; color: var(--bone); }
.bag-name i { margin-left: 6px; font-style: normal; font-size: 12px; color: var(--gold); }
.bag-acts { display: flex; flex-direction: column; align-items: flex-end; gap: 8px; }
.use-btn {
  padding: 6px 14px;
  border-radius: 8px;
  border: 1px solid rgba(255, 59, 107, 0.45);
  background: rgba(255, 59, 107, 0.16);
  color: #ffd7e0;
  font-family: inherit;
  font-size: 12px;
  letter-spacing: 1px;
  cursor: pointer;
  transition: 0.2s;
  white-space: nowrap;
}
.use-btn:hover:not(:disabled) { background: rgba(255, 59, 107, 0.3); color: #fff; }
.use-btn:disabled { opacity: 0.4; cursor: not-allowed; }

/* ===== 收支簿 ===== */
.log-list {
  max-width: 880px;
  margin: 0 auto;
  border: 1px solid rgba(176, 102, 255, 0.16);
  border-radius: 12px;
  overflow: hidden;
}
.log-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 11px 16px;
  border-bottom: 1px solid rgba(176, 102, 255, 0.08);
  font-size: 13px;
}
.log-row:last-child { border-bottom: none; }
.log-row:nth-child(odd) { background: rgba(255, 255, 255, 0.015); }
.lg-cur {
  width: 24px; height: 24px;
  flex: none;
  display: flex; align-items: center; justify-content: center;
  border-radius: 50%;
  font-size: 11px;
  color: #1a030a;
  background: linear-gradient(135deg, #ffe6a3, #d9b066);
}
.log-row.jade .lg-cur { background: linear-gradient(135deg, #ff9db8, #b3183a); color: #fff1f4; }
.lg-reason { min-width: 110px; color: rgba(232, 220, 200, 0.8); letter-spacing: 1px; }
.lg-amount { min-width: 74px; text-align: right; font-size: 14px; }
.lg-amount.plus { color: #7ce6b0; }
.lg-amount.minus { color: #ff8fa8; }
.lg-balance { min-width: 70px; text-align: right; font-size: 12px; color: rgba(232, 220, 200, 0.45); }
.lg-time { margin-left: auto; font-size: 11px; color: rgba(232, 220, 200, 0.3); }

/* ===== 提示 ===== */
.va-toast {
  position: fixed;
  left: 50%;
  bottom: 46px;
  transform: translateX(-50%);
  padding: 10px 22px;
  border-radius: 10px;
  border: 1px solid rgba(217, 176, 102, 0.45);
  background: rgba(24, 8, 14, 0.95);
  color: var(--bone);
  font-size: 13px;
  letter-spacing: 1px;
  box-shadow: 0 0 24px rgba(217, 176, 102, 0.25);
  z-index: 20;
}
.va-toast.bad { border-color: rgba(255, 59, 107, 0.6); color: #ffb3c4; box-shadow: 0 0 24px rgba(255, 59, 107, 0.35); }
.vtoast-enter-active, .vtoast-leave-active { transition: 0.25s; }
.vtoast-enter-from, .vtoast-leave-to { opacity: 0; transform: translate(-50%, 8px); }

@media (max-width: 720px) {
  .va-head { flex-wrap: wrap; gap: 10px; padding: 12px 14px; }
  .va-wallet { width: 100%; margin-left: 0; }
  .wc { flex: 1; min-width: 0; }
  .va-tabs { padding: 10px 14px 0; overflow-x: auto; }
  .va-today { display: none; }
  .va-body { padding: 14px 14px 40px; }
  .shop-grid { grid-template-columns: 1fr; }
  .bag-grid { grid-template-columns: 1fr; }
}
</style>
