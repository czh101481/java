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
      <button type="button" class="va-tab" :class="{ on: tab === 'trade' }" @click="switchTrade">
        鬼 市<em v-if="trades.length">{{ trades.length }}</em>
      </button>
      <button type="button" class="va-tab" :class="{ on: tab === 'rank' }" @click="switchRank(rankType)">
        富 甲 榜
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

            <div class="sc-price">
              <template v-if="it.discounting">
                <span class="disc-tag">{{ discText(it.discountPercent) }}</span>
                <s class="disc-old">{{ it.originalPrice }}</s>
                <b class="disc-new">{{ it.price }}</b>
                <span class="disc-cd">{{ countdown(it.discountEnd) }}</span>
              </template>
              <template v-else>
                <b class="disc-new">{{ it.price }}</b>
                <span class="disc-cur">{{ it.payCurrency === 'jade' ? '血玉' : '魔晶' }} / 件</span>
              </template>
            </div>

            <div class="sc-limit">
              <template v-if="it.dailyLimit > 0">
                今日已购 {{ it.boughtToday }} / {{ it.dailyLimit }}
                <i v-if="it.soldOut">· 已售罄</i>
              </template>
              <template v-else>不限购</template>
            </div>
            <div class="sc-limit" v-if="it.dailyUseLimit > 0">
              每日限用 {{ it.dailyUseLimit }} 件 · 今日已用 {{ it.usedToday }}
              <i v-if="it.useSoldOut">· 额度已尽</i>
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
              <div v-if="isKeyItem(it)" class="sc-key-note">破关之钥不可开囊 · 须在修为页耗用</div>
              <div class="sc-limit" v-if="it.dailyUseLimit > 0">
                今日已用 {{ it.usedToday }} / {{ it.dailyUseLimit }} 件
                <i v-if="it.useSoldOut">· 今日额度已尽</i>
              </div>
              <div class="sc-limit" v-else>不限日用 · 今日已用 {{ it.usedToday }} 件</div>
              <p class="sc-desc">{{ it.desc }}</p>
            </div>
            <div class="bag-acts">
              <div class="qty">
                <button type="button" :disabled="qtyOf(it) <= 1" @click="stepQty(it, -1)">−</button>
                <span>{{ qtyOf(it) }}</span>
                <button type="button" :disabled="qtyOf(it) >= useCap(it)" @click="stepQty(it, 1)">＋</button>
              </div>
              <button type="button" class="sell-btn" :disabled="busy" @click="startSell(it)">
                寄 售
              </button>
              <button
                v-if="isKeyItem(it)"
                type="button"
                class="use-btn key-btn"
                :disabled="busy"
                @click="goBreakthrough"
              >
                赴 修 为 页
              </button>
              <button
                v-else
                type="button"
                class="use-btn"
                :disabled="busy || it.useSoldOut"
                @click="doUse(it)"
              >
                {{ it.useSoldOut ? '今 日 已 尽' : '取 用 ×' + qtyOf(it) }}
              </button>
            </div>
          </article>
        </div>
      </template>

      <!-- ==================== 鬼市（玩家交易） ==================== -->
      <template v-else-if="tab === 'trade'">
        <section class="sell-panel">
          <div class="sp-head">
            寄 售 手 中 之 物
            <small>挂出即从乾坤袋预扣；成交后鬼市抽 5% 焚香钱，卖家实得 95%；撤单原物退回</small>
          </div>
          <div class="sp-form" v-if="bag.length">
            <select v-model.number="sell.itemId" class="sp-input">
              <option :value="0" disabled>选择要寄售的物品</option>
              <option v-for="b in bag" :key="b.id" :value="b.id">{{ b.name }}（持 {{ b.qty }}）</option>
            </select>
            <input
              v-model.number="sell.qty"
              class="sp-input num"
              type="number"
              min="1"
              :max="sellMaxQty"
              title="数量"
            />
            <input
              v-model.number="sell.price"
              class="sp-input num"
              type="number"
              min="1"
              max="99999"
              title="单价"
            />
            <select v-model="sell.currency" class="sp-input cur">
              <option value="coin">魔晶</option>
              <option value="jade">血玉</option>
            </select>
            <button
              class="sp-btn"
              type="button"
              :disabled="busy || !sell.itemId"
              @click="doSell"
            >挂 出</button>
          </div>
          <div class="sp-empty" v-else>乾坤袋中空无一物，先去藏宝阁购宝，再来寄售。</div>
          <p class="sp-err" v-if="sellErr">{{ sellErr }}</p>
        </section>

        <section class="trade-sec">
          <div class="ts-head">鬼 市 在 售 <em>{{ trades.length }}</em></div>
          <div class="va-empty" v-if="!trades.length">鬼 市 冷 清 ， 尚 无 挂 单 。</div>
          <div class="trade-grid" v-else>
            <article v-for="t in trades" :key="t.id" class="trade-card" :class="{ own: t.sellerId === myId }">
              <div class="tc-top">
                <div class="sc-face small" :class="catClass(t.itemCategory)">{{ t.itemIcon }}</div>
                <div class="sc-meta">
                  <div class="sc-name">{{ t.itemName }}<i class="tc-qty">×{{ t.qty }}</i></div>
                  <div class="sc-cat">{{ t.itemCategory }} · {{ t.effectText }}</div>
                </div>
              </div>
              <div class="tc-price">
                <b>{{ t.price }}</b>
                <span>{{ t.currencyName }} / 件</span>
                <em>合计 {{ t.total }}</em>
              </div>
              <div class="tc-seller">
                <span class="tc-av" :style="av(t.sellerAvatar, t.sellerName).style">
                  <img v-if="av(t.sellerAvatar, t.sellerName).img" :src="av(t.sellerAvatar, t.sellerName).img" alt="" />
                  <template v-else>{{ av(t.sellerAvatar, t.sellerName).char }}</template>
                </span>
                <span class="tc-sname">{{ t.sellerName }}</span>
                <span class="tc-time">{{ fmtTime(t.createdAt) }}</span>
              </div>
              <button
                v-if="t.sellerId === myId"
                class="tc-btn ghost"
                type="button"
                :disabled="busy"
                @click="doCancel(t)"
              >撤 回 挂 单</button>
              <button
                v-else
                class="tc-btn buy"
                type="button"
                :disabled="busy"
                @click="doBuyTrade(t)"
              >买 下 · {{ t.total }} {{ t.currencyName }}</button>
            </article>
          </div>
        </section>

        <section class="trade-sec" v-if="mine.length">
          <div class="ts-head">我 的 挂 单 <em>{{ mine.length }}</em></div>
          <div class="mine-list">
            <div v-for="t in mine" :key="t.id" class="mine-row" :class="'st' + t.status">
              <span class="mr-name">{{ t.itemName }} ×{{ t.qty }}</span>
              <span class="mr-price">{{ t.price }} {{ t.currencyName }} / 件 · 合计 {{ t.total }}</span>
              <span class="mr-st">
                {{ t.statusText }}<template v-if="t.status === 1 && t.buyerName"> · 买家 {{ t.buyerName }}</template>
                <template v-if="t.status === 1 && t.sellerNet"> · 实得 {{ t.sellerNet }}</template>
              </span>
              <button v-if="t.status === 0" class="mr-btn" type="button" @click="doCancel(t)">撤 单</button>
            </div>
          </div>
        </section>
      </template>

      <!-- ==================== 富甲榜 ==================== -->
      <template v-else-if="tab === 'rank'">
        <div class="rank-head">
          <div class="rank-switch">
            <button
              v-for="o in RANK_TYPES"
              :key="o.v"
              type="button"
              class="rk-sw"
              :class="{ on: rankType === o.v }"
              @click="switchRank(o.v)"
            >{{ o.t }}</button>
          </div>
          <div class="rank-me" v-if="rankMeta">
            我的名次 · 第 <b>{{ rankMeta.myRank || '—' }}</b> 位
            <span>（{{ rankLabel }} {{ rankMeta.myScore }}）</span>
          </div>
        </div>
        <div class="rank-note">总资产 = 魔晶 + 血玉 × {{ jadeRate }}（1 血玉折 {{ jadeRate }} 魔晶）</div>
        <div class="rank-list" v-if="ranks.length">
          <div class="rank-row head">
            <span class="rk-no">位</span>
            <span class="rk-av"></span>
            <span class="rk-name">修者</span>
            <span class="rk-coin">魔晶</span>
            <span class="rk-jade">血玉</span>
            <span class="rk-score">总资产</span>
          </div>
          <div v-for="r in ranks" :key="r.userId" class="rank-row" :class="{ me: r.me }">
            <span class="rk-no" :class="'top' + r.rank">{{ r.rank }}</span>
            <span class="rk-av" :style="av(r.avatar, r.name).style">
              <img v-if="av(r.avatar, r.name).img" :src="av(r.avatar, r.name).img" alt="" />
              <template v-else>{{ av(r.avatar, r.name).char }}</template>
            </span>
            <span class="rk-name">{{ r.name }}<i>{{ r.cultivation }}</i></span>
            <span class="rk-coin">{{ r.coin }}</span>
            <span class="rk-jade">{{ r.jade }}</span>
            <span class="rk-score">{{ r.score }}</span>
          </div>
        </div>
        <div class="va-empty" v-else>榜 上 尚 无 人 名 。</div>
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
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { getSession } from '../api/auth'
import { walletOverview, coinLogs, shopList, buyItem, bagList, useItem, coinRank } from '../api/coin'
import { tradeList, myTrades, sellItem, cancelTrade, buyTrade } from '../api/trade'
import { renderAvatar } from '../config/avatars'

const router = useRouter()
const session = getSession()
// 当前修者 id：用于判定「自己的挂单」并高亮自己的榜位
const myId = session.user && session.user.id ? session.user.id : null

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

// ==================== 鬼市 ====================
const trades = ref([])
const mine = ref([])
const sellErr = ref('')
const sell = ref({ itemId: 0, qty: 1, price: 1, currency: 'coin' })

// 所选寄售物的持有上限
const sellMaxQty = computed(() => {
  const b = bag.value.find((x) => x.id === sell.value.itemId)
  return b ? Math.max(1, b.qty) : 1
})

// ==================== 富甲榜 ====================
const RANK_TYPES = [
  { v: 'rich', t: '富 甲' },
  { v: 'coin', t: '魔 晶' },
  { v: 'jade', t: '血 玉' }
]
const ranks = ref([])
const rankType = ref('rich')
const rankMeta = ref(null)
const jadeRate = ref(100)
const rankLabel = computed(() => {
  const o = RANK_TYPES.find((x) => x.v === rankType.value)
  return o ? o.t.replace(/\s/g, '') : '总资产'
})

// 折扣倒计时：每秒推进一次
const nowTs = ref(Date.now())
let clock = null

const av = (avatar, name) => renderAvatar(avatar, name)

function discText(p) {
  const n = Number(p) / 10
  return (Number.isInteger(n) ? n : n.toFixed(1)) + ' 折'
}

function countdown(end) {
  if (!end) return '限时折扣'
  const diff = new Date(String(end).replace(' ', 'T')).getTime() - nowTs.value
  if (!isFinite(diff) || diff <= 0) return '已结束'
  const s = Math.floor(diff / 1000)
  const d = Math.floor(s / 86400)
  const h = Math.floor((s % 86400) / 3600)
  const m = Math.floor((s % 3600) / 60)
  const sec = s % 60
  if (d > 0) return `剩 ${d}天${h}时`
  if (h > 0) return `剩 ${h}:${String(m).padStart(2, '0')}:${String(sec).padStart(2, '0')}`
  return `剩 ${m}:${String(sec).padStart(2, '0')}`
}

// 每个物品的下单/取用数量
const qtys = ref({})
const qtyOf = (it) => qtys.value[it.id] || 1

// v1.1：乾坤袋单次可取用上限 = min(持有量, 今日剩余使用额度)
function useCap(it) {
  const held = Math.max(1, Number(it.qty) || 0)
  if (!(it.dailyUseLimit > 0)) return held
  const left = Math.max(0, Number(it.dailyUseLimit) - (Number(it.usedToday) || 0))
  return Math.max(1, Math.min(held, left))
}
function stepQty(it, delta) {
  const cur = qtyOf(it)
  const max = it.dailyLimit > 0 && tab.value === 'shop'
    ? Math.max(1, it.remainToday === -1 ? 99 : it.remainToday)
    : (tab.value === 'bag' ? useCap(it) : 99)
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

// ==================== 鬼市 ====================
async function loadTrades() {
  try {
    const res = await tradeList()
    if (res.data.code === 200) trades.value = res.data.data.list || []
  } catch (e) { /* 静默 */ }
}

async function loadMine() {
  try {
    const res = await myTrades()
    if (res.data.code === 200) mine.value = res.data.data.list || []
  } catch (e) { /* 静默 */ }
}

async function switchTrade() {
  tab.value = 'trade'
  sellErr.value = ''
  await Promise.all([loadTrades(), loadMine(), loadBag()])
  if (!sell.value.itemId && bag.value.length) {
    sell.value.itemId = bag.value[0].id
  }
}

// 从乾坤袋跳来寄售：预填物品、数量与建议价
async function startSell(it) {
  tab.value = 'trade'
  sellErr.value = ''
  sell.value = {
    itemId: it.id,
    qty: 1,
    price: it.priceCoin > 0 ? it.priceCoin : (it.priceJade > 0 ? it.priceJade : 1),
    currency: it.priceCoin > 0 ? 'coin' : 'jade'
  }
  await Promise.all([loadTrades(), loadMine()])
}

async function doSell() {
  if (busy.value) return
  const itemId = Number(sell.value.itemId) || 0
  if (!itemId) {
    sellErr.value = '请先选择要寄售的物品。'
    return
  }
  const qty = Math.max(1, Math.min(Number(sell.value.qty) || 1, sellMaxQty.value))
  const price = Math.max(1, Math.min(Number(sell.value.price) || 1, 99999))
  busy.value = true
  sellErr.value = ''
  try {
    const res = await sellItem(itemId, qty, price, sell.value.currency)
    if (res.data.code === 200) {
      showToast(res.data.data.msg || '已挂入鬼市')
      await Promise.all([loadWallet(), loadBag(), loadTrades(), loadMine()])
      sell.value.itemId = 0
    } else {
      sellErr.value = res.data.msg || '挂单失败'
    }
  } catch (e) {
    sellErr.value = e.message || '挂单失败'
  } finally {
    busy.value = false
  }
}

async function doCancel(t) {
  if (busy.value) return
  if (!confirm(`撤回「${t.itemName}」×${t.qty} 的挂单？原物将退回乾坤袋。`)) return
  busy.value = true
  try {
    const res = await cancelTrade(t.id)
    if (res.data.code === 200) {
      showToast(res.data.data.msg || '已撤回')
      await Promise.all([loadBag(), loadTrades(), loadMine()])
    } else {
      showToast(res.data.msg || '撤回失败', true)
      await Promise.all([loadTrades(), loadMine()])
    }
  } catch (e) {
    showToast(e.message || '撤回失败', true)
  } finally {
    busy.value = false
  }
}

async function doBuyTrade(t) {
  if (busy.value) return
  if (!confirm(`以 ${t.total} ${t.currencyName} 买下「${t.itemName}」×${t.qty}？`)) return
  busy.value = true
  try {
    const res = await buyTrade(t.id)
    if (res.data.code === 200) {
      showToast(res.data.data.msg || '已入乾坤袋')
      await Promise.all([loadWallet(), loadBag(), loadTrades(), loadMine()])
    } else {
      showToast(res.data.msg || '买入失败', true)
      await Promise.all([loadTrades(), loadMine()])
    }
  } catch (e) {
    showToast(e.message || '买入失败', true)
  } finally {
    busy.value = false
  }
}

// ==================== 富甲榜 ====================
async function switchRank(type) {
  tab.value = 'rank'
  rankType.value = type || 'rich'
  try {
    const res = await coinRank(rankType.value, 20)
    if (res.data.code === 200) {
      ranks.value = res.data.data.list || []
      rankMeta.value = res.data.data
      jadeRate.value = res.data.data.jadeRate || 100
    }
  } catch (e) {
    showToast(e.message || '榜单读取失败', true)
  }
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

// 破关之钥（悟道机缘 / 死战悟道）不可开囊，只能在修为页的功法破关处耗用
const isKeyItem = (it) => !!it && ['insight', 'transcend'].includes(it.effectType)
function goBreakthrough() {
  router.push('/cultivate')
}

async function doUse(it) {
  if (busy.value) return
  const left = it.dailyUseLimit > 0
    ? Math.max(0, Number(it.dailyUseLimit) - (Number(it.usedToday) || 0))
    : 99
  const q = Math.min(qtyOf(it), it.qty, left)
  if (q <= 0) {
    showToast('今日取用额度已尽，明日再来。', true)
    return
  }
  busy.value = true
  try {
    const res = await useItem(it.id, q)
    if (res.data.code === 200) {
      showToast(res.data.data.msg || '已生效')
      qtys.value = { ...qtys.value, [it.id]: 1 }
      await Promise.all([loadWallet(), loadShop(), loadBag()])
    } else {
      showToast(res.data.msg || '取用失败', true)
      // 后端拒绝（如并发下额度被占）后同步一次额度，避免界面停在旧数字
      await Promise.all([loadShop(), loadBag()])
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
  clock = setInterval(() => { nowTs.value = Date.now() }, 1000)
  await Promise.all([loadWallet(), loadShop(), loadBag()])
  loading.value = false
})

onUnmounted(() => {
  if (clock) clearInterval(clock)
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
.use-btn.key-btn {
  border-color: rgba(217, 176, 102, 0.5);
  background: rgba(217, 176, 102, 0.14);
  color: var(--gold);
}
.use-btn.key-btn:hover:not(:disabled) { background: rgba(217, 176, 102, 0.28); color: #fff6dd; }
.sc-key-note { margin-top: 4px; font-size: 11px; letter-spacing: 1px; color: var(--gold); opacity: 0.85; }

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

/* ===== 价格 / 限时折扣 ===== */
.sc-price { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.disc-new { font-size: 16px; color: var(--gold); letter-spacing: 1px; }
.disc-cur { font-size: 11px; color: rgba(232, 220, 200, 0.45); }
.disc-old { font-size: 12px; color: rgba(232, 220, 200, 0.35); }
.disc-tag {
  padding: 1px 7px;
  border-radius: 6px;
  font-size: 11px;
  background: linear-gradient(135deg, #ff3b6b, #7a0f24);
  color: #fff2f5;
  box-shadow: 0 0 12px rgba(255, 59, 107, 0.45);
}
.disc-cd { margin-left: auto; font-size: 11px; color: #ffb3c4; letter-spacing: 1px; }

/* 乾坤袋「寄售」按钮 */
.sell-btn {
  padding: 6px 14px;
  border-radius: 8px;
  border: 1px solid rgba(217, 176, 102, 0.45);
  background: rgba(217, 176, 102, 0.12);
  color: #ffe6a3;
  font-family: inherit;
  font-size: 12px;
  letter-spacing: 1px;
  cursor: pointer;
  transition: 0.2s;
  white-space: nowrap;
}
.sell-btn:hover:not(:disabled) { background: rgba(217, 176, 102, 0.26); color: #fff6dd; }
.sell-btn:disabled { opacity: 0.4; cursor: not-allowed; }

/* ===== 鬼市 ===== */
.sell-panel {
  margin-bottom: 22px;
  padding: 16px 18px;
  border-radius: 14px;
  border: 1px solid rgba(217, 176, 102, 0.28);
  background: linear-gradient(160deg, rgba(34, 12, 26, 0.9), rgba(12, 4, 10, 0.92));
}
.sp-head {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 10px;
  font-size: 15px;
  letter-spacing: 3px;
  color: var(--gold);
}
.sp-head small { font-size: 11px; letter-spacing: 1px; color: rgba(232, 220, 200, 0.42); }
.sp-form { display: flex; flex-wrap: wrap; gap: 10px; margin-top: 14px; }
.sp-input {
  flex: 1 1 160px;
  min-width: 0;
  padding: 9px 12px;
  border-radius: 8px;
  border: 1px solid rgba(176, 102, 255, 0.28);
  background: rgba(255, 255, 255, 0.05);
  color: var(--bone);
  font-family: inherit;
  font-size: 13px;
  outline: none;
}
.sp-input:focus { border-color: rgba(217, 176, 102, 0.6); }
.sp-input.num { flex: 0 0 96px; }
.sp-input.cur { flex: 0 0 100px; cursor: pointer; }
.sp-input option { background: #160a24; color: #e8dcc8; }
.sp-btn {
  flex: 0 0 auto;
  padding: 9px 26px;
  border-radius: 8px;
  border: none;
  background: linear-gradient(135deg, #d9b066, #8a6420);
  color: #1c1203;
  font-family: inherit;
  font-size: 13px;
  letter-spacing: 3px;
  font-weight: 700;
  cursor: pointer;
  transition: 0.2s;
}
.sp-btn:hover:not(:disabled) { filter: brightness(1.12); }
.sp-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.sp-empty { margin-top: 12px; font-size: 12px; color: rgba(232, 220, 200, 0.42); }
.sp-err { margin: 10px 0 0; font-size: 12px; color: #ff8fa8; }

.trade-sec { margin-bottom: 24px; }
.ts-head {
  margin-bottom: 12px;
  font-size: 14px;
  letter-spacing: 4px;
  color: rgba(232, 220, 200, 0.8);
}
.ts-head em {
  margin-left: 8px;
  padding: 0 7px;
  border-radius: 9px;
  background: rgba(255, 59, 107, 0.25);
  color: #ffd7e0;
  font-size: 11px;
  font-style: normal;
  letter-spacing: 0;
}
.trade-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: 14px; }
.trade-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 15px;
  border-radius: 13px;
  border: 1px solid rgba(176, 102, 255, 0.2);
  background: linear-gradient(160deg, rgba(28, 10, 22, 0.92), rgba(11, 3, 9, 0.94));
  transition: 0.22s;
}
.trade-card:hover { border-color: rgba(217, 176, 102, 0.42); box-shadow: 0 0 22px rgba(217, 176, 102, 0.14); }
.trade-card.own { border-style: dashed; border-color: rgba(217, 176, 102, 0.4); }
.tc-top { display: flex; align-items: center; gap: 10px; }
.tc-qty { margin-left: 6px; font-style: normal; font-size: 12px; color: var(--gold); }
.tc-price { display: flex; align-items: baseline; gap: 6px; }
.tc-price b { font-size: 18px; color: var(--gold); }
.tc-price span { font-size: 11px; color: rgba(232, 220, 200, 0.45); }
.tc-price em { margin-left: auto; font-style: normal; font-size: 11px; color: rgba(232, 220, 200, 0.5); }
.tc-seller { display: flex; align-items: center; gap: 8px; font-size: 12px; color: rgba(232, 220, 200, 0.6); }
.tc-av {
  width: 24px; height: 24px;
  flex: none;
  display: flex; align-items: center; justify-content: center;
  border-radius: 50%;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.18);
  background: linear-gradient(135deg, #b3183a, #3a0a16);
  font-size: 11px;
  color: #fff;
}
.tc-av img { width: 100%; height: 100%; object-fit: cover; }
.tc-time { margin-left: auto; font-size: 11px; color: rgba(232, 220, 200, 0.3); }
.tc-btn {
  height: 32px;
  border-radius: 8px;
  border: 1px solid transparent;
  font-family: inherit;
  font-size: 12px;
  letter-spacing: 1px;
  cursor: pointer;
  transition: 0.2s;
}
.tc-btn.buy { background: linear-gradient(135deg, #ff5e85, #8a0f22); color: #fff3f5; }
.tc-btn.buy:hover:not(:disabled) { filter: brightness(1.15); box-shadow: 0 0 16px rgba(255, 59, 107, 0.4); }
.tc-btn.ghost { background: transparent; border-color: rgba(217, 176, 102, 0.45); color: #ffe6a3; }
.tc-btn.ghost:hover:not(:disabled) { background: rgba(217, 176, 102, 0.16); }
.tc-btn:disabled { opacity: 0.5; cursor: not-allowed; }

.mine-list { border: 1px solid rgba(176, 102, 255, 0.16); border-radius: 12px; overflow: hidden; }
.mine-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 10px 16px;
  font-size: 12px;
  border-bottom: 1px solid rgba(176, 102, 255, 0.08);
}
.mine-row:last-child { border-bottom: none; }
.mine-row:nth-child(odd) { background: rgba(255, 255, 255, 0.015); }
.mr-name { min-width: 150px; color: var(--bone); letter-spacing: 1px; }
.mr-price { color: rgba(217, 176, 102, 0.85); }
.mr-st { margin-left: auto; color: rgba(232, 220, 200, 0.5); }
.mine-row.st1 .mr-st { color: #7ce6b0; }
.mine-row.st2 .mr-st { color: rgba(232, 220, 200, 0.35); }
.mr-btn {
  padding: 4px 12px;
  border-radius: 6px;
  border: 1px solid rgba(160, 44, 72, 0.6);
  background: transparent;
  color: #ff8fa8;
  font-family: inherit;
  font-size: 11px;
  cursor: pointer;
  transition: 0.2s;
}
.mr-btn:hover { background: rgba(160, 44, 72, 0.16); }

/* ===== 富甲榜 ===== */
.rank-head { display: flex; align-items: center; gap: 16px; flex-wrap: wrap; margin-bottom: 10px; }
.rank-switch { display: flex; gap: 8px; }
.rk-sw {
  padding: 7px 18px;
  border-radius: 18px;
  border: 1px solid rgba(176, 102, 255, 0.28);
  background: transparent;
  color: rgba(232, 220, 200, 0.62);
  font-family: inherit;
  font-size: 12px;
  letter-spacing: 3px;
  cursor: pointer;
  transition: 0.2s;
}
.rk-sw:hover { color: var(--bone); border-color: rgba(217, 176, 102, 0.5); }
.rk-sw.on {
  background: linear-gradient(135deg, #7a5c14, #c9a227);
  border-color: transparent;
  color: #1a1206;
  font-weight: 700;
}
.rank-me { margin-left: auto; font-size: 12px; color: rgba(232, 220, 200, 0.6); }
.rank-me b { color: var(--gold); font-size: 15px; }
.rank-note { margin-bottom: 14px; font-size: 11px; color: rgba(232, 220, 200, 0.35); }
.rank-list {
  max-width: 880px;
  margin: 0 auto;
  border: 1px solid rgba(176, 102, 255, 0.16);
  border-radius: 12px;
  overflow: hidden;
}
.rank-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 9px 16px;
  font-size: 13px;
  border-bottom: 1px solid rgba(176, 102, 255, 0.08);
}
.rank-row:last-child { border-bottom: none; }
.rank-row.head {
  font-size: 11px;
  letter-spacing: 2px;
  color: rgba(232, 220, 200, 0.4);
  background: rgba(255, 255, 255, 0.03);
}
.rank-row.me { background: rgba(217, 176, 102, 0.08); box-shadow: inset 2px 0 0 var(--gold); }
.rk-no { width: 34px; text-align: center; font-size: 12px; color: rgba(232, 220, 200, 0.5); }
.rk-no.top1 { color: #ffe6a3; font-size: 16px; font-weight: 700; text-shadow: 0 0 10px rgba(217, 176, 102, 0.7); }
.rk-no.top2 { color: #e3e3ee; font-size: 15px; font-weight: 700; }
.rk-no.top3 { color: #e8a06a; font-size: 15px; font-weight: 700; }
.rk-av {
  width: 30px; height: 30px;
  flex: none;
  display: flex; align-items: center; justify-content: center;
  border-radius: 50%;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.16);
  background: linear-gradient(135deg, #b3183a, #3a0a16);
  font-size: 12px;
  color: #fff;
}
.rk-av img { width: 100%; height: 100%; object-fit: cover; }
.rk-name { flex: 1; min-width: 0; color: var(--bone); letter-spacing: 1px; }
.rk-name i { margin-left: 8px; font-style: normal; font-size: 11px; color: rgba(232, 220, 200, 0.4); }
.rk-coin, .rk-jade, .rk-score { width: 88px; text-align: right; font-size: 12px; }
.rk-coin { color: #ffe6a3; }
.rk-jade { color: #ff9db8; }
.rk-score { color: var(--gold); font-size: 13px; font-weight: 600; }
.rank-row.head .rk-coin,
.rank-row.head .rk-jade,
.rank-row.head .rk-score {
  font-size: 11px;
  font-weight: 400;
  color: rgba(232, 220, 200, 0.4);
}

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
  .sp-input.num, .sp-input.cur { flex: 1 1 80px; }
  .trade-grid { grid-template-columns: 1fr; }
  .rank-head { gap: 10px; }
  .rank-me { width: 100%; margin-left: 0; }
  .rank-row { gap: 8px; padding: 8px 10px; }
  .rk-no { width: 24px; }
  .rk-coin, .rk-jade, .rk-score { width: 60px; }
  .mine-row { flex-wrap: wrap; gap: 6px 10px; }
  .mr-st { margin-left: 0; width: 100%; }
}
</style>
