<template>
  <!-- 魔窟：洞府挂机产出（聚煞阵 / 魔晶矿 / 血玉髓脉，12 小时封顶） -->
  <div class="dn">
    <div class="dn-aura" aria-hidden="true"></div>

    <header class="dn-head">
      <button class="dn-back" type="button" @click="goHome">⟵ 返 回 魔 域</button>
      <div class="dn-title">
        <span>魔 窟</span>
        <small>洞府挂机 · 煞气 / 魔晶 / 血玉 持续产出</small>
      </div>
      <div class="dn-wallet" v-if="st">
        <span class="w-coin"><i>晶</i>{{ fmt(st.coin) }}</span>
        <span class="w-jade"><i>玉</i>{{ fmt(st.jade) }}</span>
      </div>
    </header>

    <main class="dn-body">
      <div v-if="loading" class="dn-empty">魔 窟 气 机 凝 聚 中 ……</div>
      <div v-else-if="deny" class="dn-empty deny">{{ deny }}</div>

      <template v-else-if="st">
        <!-- ==================== 累积总览 · 一键收取 ==================== -->
        <section class="sum-card">
          <div class="sum-left">
            <div class="sum-label">洞 府 气 机</div>
            <div class="sum-progress">
              <div class="sp-bar">
                <div class="sp-fill" :style="{ width: progress + '%' }"></div>
              </div>
              <div class="sp-text">
                <template v-if="capped">已 积 满 {{ st.capHours }} 小 时 · 尽 快 收 取</template>
                <template v-else>再 过 {{ remainText }} 积 满 {{ st.capHours }} 小 时</template>
              </div>
            </div>
            <div class="sum-note" v-if="st.shaPenalty < 1">
              心魔缠身（{{ st.heartDemon }}），聚煞阵效率 ×{{ st.shaPenalty }}
            </div>
          </div>

          <div class="sum-right">
            <div class="sum-pendings">
              <div class="pd"><i class="pd-sha"></i><b>{{ fmt(tick.sha) }}</b><small>煞气</small></div>
              <div class="pd"><i class="pd-coin"></i><b>{{ fmt(tick.coin) }}</b><small>魔晶</small></div>
              <div class="pd"><i class="pd-jade"></i><b>{{ fmt(tick.jade) }}</b><small>血玉</small></div>
            </div>
            <button
              class="collect-btn"
              type="button"
              :disabled="busy || tick.total <= 0"
              @click="onCollect"
            >{{ tick.total > 0 ? '一 键 收 取' : '尚 无 可 取' }}</button>
          </div>
        </section>

        <!-- ==================== 三座设施 ==================== -->
        <section class="fac-grid">
          <article
            v-for="f in facList"
            :key="f.type"
            class="fac-card"
            :class="{ 'is-closed': f.level === 0 }"
          >
            <div class="fc-head">
              <span class="fc-seal" :class="'seal-' + f.type">{{ f.seal }}</span>
              <div class="fc-name">
                <b>{{ f.name }}</b>
                <small>{{ f.level > 0 ? `${f.level} / ${st.maxLevel} 级` : '未 开 辟' }}</small>
              </div>
            </div>

            <div class="fc-rate">
              <template v-if="f.level > 0">
                <div class="rate-line">
                  <span>每时产出</span>
                  <b>{{ rateText(f) }}</b>
                </div>
                <div class="rate-line sub" v-if="f.type === 'sha' && st.shaPenalty < 1">
                  <span>心魔折损</span>
                  <b>×{{ st.shaPenalty }}</b>
                </div>
              </template>
              <div class="rate-line sub" v-else>
                <span>开辟后</span>
                <b>{{ openRateText(f) }}</b>
              </div>
            </div>

            <div class="fc-pending">
              <span>洞中已积</span>
              <b>{{ pendingText(f) }}</b>
            </div>

            <div class="fc-desc">{{ f.desc }}</div>

            <button
              class="fc-btn"
              type="button"
              :class="{ gold: f.level === 0 }"
              :disabled="busy || !canUpgrade(f)"
              @click="onUpgrade(f)"
            >
              <template v-if="f.realmLocked">需 {{ f.realmRequiredName }} 境</template>
              <template v-else-if="f.level >= st.maxLevel">已 臻 圆 满</template>
              <template v-else>{{ f.level === 0 ? '开 辟' : '扩 建' }} · {{ f.costAmount }} {{ f.costCurrencyName }}</template>
            </button>
            <div class="fc-cost-note" v-if="!f.realmLocked && f.level < st.maxLevel && !affordable(f)">
              {{ f.costCurrencyName }}不足（囊中 {{ fmt(walletOf(f.costCurrency)) }}）
            </div>
          </article>
        </section>

        <p class="dn-foot">
          魔窟气机自行流转，离线亦不停歇；积存上限 {{ st.capHours }} 小时，逾时不增，收取归零重计。
          煞气直接计入累计修为；心魔 ≥ 60 聚煞阵降效两成，≥ 80 降四成。
          聚煞阵、魔晶矿以魔晶扩建；血玉髓脉开辟需凝煞境，以血玉扩建。
        </p>

        <div v-if="msg" class="dn-toast">{{ msg }}</div>
      </template>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { denState, denCollect, denUpgrade } from '../api/den'

const router = useRouter()

const loading = ref(true)
const deny = ref('')
const st = ref(null)
const msg = ref('')
const busy = ref(false)
const now = ref(Date.now())

// 本地按秒折算的待收数量（与后端公式一致）
const tick = reactive({ sha: 0, coin: 0, jade: 0, total: 0, effSec: 0 })

const FAC_META = {
  sha: { name: '聚 煞 阵', seal: '煞', desc: '纳地脉阴煞于阵眼，昼夜不息地凝出煞气，直接汇入修为。' },
  coin: { name: '魔 晶 矿', seal: '晶', desc: '魔窟深处的晶脉自行生长，按时析出魔晶，静待收取。' },
  jade: { name: '血 玉 髓 脉', seal: '玉', desc: '岩层深处血流成玉，凝得极慢，却最为贵重。' },
}

const facList = computed(() => {
  if (!st.value || !st.value.facilities) return []
  return ['sha', 'coin', 'jade'].map((t) => ({
    type: t,
    ...st.value.facilities[t],
    ...FAC_META[t],
  }))
})

const progress = computed(() => {
  if (!st.value) return 0
  const cap = st.value.capSec || 1
  return Math.min(100, (tick.effSec / cap) * 100)
})

const capped = computed(() => st.value && tick.effSec >= st.value.capSec)

const remainText = computed(() => {
  if (!st.value) return ''
  const left = Math.max(0, st.value.capSec - tick.effSec)
  const h = Math.floor(left / 3600)
  const m = Math.floor((left % 3600) / 60)
  const s = Math.floor(left % 60)
  return `${h} 时 ${m} 分 ${s} 秒`
})

function walletOf(currency) {
  if (!st.value) return 0
  return currency === 'jade' ? st.value.jade : st.value.coin
}

function affordable(f) {
  return walletOf(f.costCurrency) >= f.costAmount
}

function canUpgrade(f) {
  if (f.realmLocked) return false
  if (f.level >= st.value.maxLevel) return false
  return affordable(f)
}

function rateText(f) {
  if (f.type === 'sha') return `${st.value.shaPerHour} 煞气 / 时`
  if (f.type === 'coin') return `${st.value.coinPerHour} 魔晶 / 时`
  return `${(st.value.jadeMilliPerHour / 1000).toFixed(2)} 血玉 / 时`
}

function openRateText(f) {
  if (f.type === 'sha') return `${rate1('sha')} 煞气 / 时`
  if (f.type === 'coin') return `${rate1('coin')} 魔晶 / 时`
  return '0.04 血玉 / 时'
}

function rate1(type) {
  // 1 级产量（心魔不折损开辟预览）
  if (type === 'sha') return 3
  return 3
}

function pendingText(f) {
  if (f.type === 'sha') return `${fmt(tick.sha)} 煞气`
  if (f.type === 'coin') return `${fmt(tick.coin)} 魔晶`
  const milli = jadeMilliNow()
  return `${fmt(tick.jade)} 血玉（${(milli / 1000).toFixed(2)}）`
}

function jadeMilliNow() {
  if (!st.value) return 0
  const carry = st.value.jadeCarry || 0
  const rate = st.value.jadeMilliPerHour || 0
  return Math.floor(carry + (rate * tick.effSec) / 3600)
}

function fmt(n) {
  const v = Math.max(0, Math.floor(Number(n) || 0))
  return v >= 10000 ? v.toLocaleString('zh-CN') : String(v)
}

// ==================== 本地秒级 tick ====================
function refreshTick() {
  if (!st.value) return
  const last = st.value.lastCollect ? new Date(st.value.lastCollect).getTime() : Date.now()
  const cap = st.value.capSec || 0
  let eff = Math.floor((now.value - last) / 1000)
  if (Number.isNaN(eff) || eff < 0) eff = 0
  eff = Math.min(eff, cap)
  tick.effSec = eff
  tick.sha = Math.floor(((st.value.shaPerHour || 0) * eff) / 3600)
  tick.coin = Math.floor(((st.value.coinPerHour || 0) * eff) / 3600)
  tick.jade = Math.floor(jadeMilliNow() / 1000)
  tick.total = tick.sha + tick.coin + tick.jade
}

let secTimer = null
let pollTimer = null

// ==================== 数据加载 ====================
async function load() {
  try {
    const res = await denState()
    if (res.data.code === 200) {
      st.value = res.data.data
      deny.value = ''
      refreshTick()
    } else {
      deny.value = res.data.msg || '魔窟之门紧闭，气机不显。'
    }
  } catch (e) {
    deny.value = e.message || '魔窟之门紧闭，气机不显。'
  } finally {
    loading.value = false
  }
}

async function onCollect() {
  if (busy.value) return
  busy.value = true
  try {
    const res = await denCollect()
    if (res.data.code === 200) {
      msg.value = res.data.data.msg || '气机入囊。'
      st.value = res.data.data
      refreshTick()
    } else {
      msg.value = res.data.msg || '魔窟无应。'
    }
  } catch (e) {
    msg.value = e.message || '魔窟无应。'
  } finally {
    busy.value = false
  }
}

async function onUpgrade(f) {
  if (busy.value || !canUpgrade(f)) return
  busy.value = true
  try {
    const res = await denUpgrade(f.type)
    if (res.data.code === 200) {
      msg.value = res.data.data.msg || '魔窟气机涌动。'
      st.value = res.data.data
      refreshTick()
    } else {
      msg.value = res.data.msg || '扩建未成。'
    }
  } catch (e) {
    msg.value = e.message || '扩建未成。'
  } finally {
    busy.value = false
  }
}

function goHome() {
  router.push('/home')
}

onMounted(() => {
  load()
  secTimer = setInterval(() => {
    now.value = Date.now()
    refreshTick()
  }, 1000)
  pollTimer = setInterval(load, 30000)
})

onBeforeUnmount(() => {
  if (secTimer) clearInterval(secTimer)
  if (pollTimer) clearInterval(pollTimer)
})
</script>

<style scoped>
.dn {
  position: fixed;
  inset: 0;
  z-index: 3;
  overflow-y: auto;
  padding-bottom: 96px;
  background: radial-gradient(ellipse at 50% 18%, #150611 0%, #0a0308 55%, #040103 100%);
  font-family: inherit;
}
.dn-aura {
  position: fixed; inset: 0; pointer-events: none;
  background:
    radial-gradient(circle at 18% 10%, rgba(160, 22, 58, 0.20), transparent 46%),
    radial-gradient(circle at 82% 80%, rgba(70, 130, 190, 0.10), transparent 48%);
}

/* ===== 头部 ===== */
.dn-head {
  position: sticky; top: 0; z-index: 6;
  display: flex; align-items: center; gap: 20px;
  padding: 16px 34px;
  background: rgba(8, 2, 12, 0.78);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(255, 59, 107, 0.18);
}
.dn-back {
  padding: 7px 15px; border-radius: 8px; cursor: pointer;
  font-family: inherit; font-size: 12px; letter-spacing: 2px;
  color: rgba(232, 220, 200, 0.85);
  background: rgba(20, 4, 14, 0.7);
  border: 1px solid rgba(255, 59, 107, 0.25);
}
.dn-back:hover { color: #ffe9d2; border-color: rgba(255, 59, 107, 0.5); }
.dn-title { flex: 1; display: flex; flex-direction: column; gap: 2px; }
.dn-title span {
  font-size: 22px; letter-spacing: 8px;
  color: #e8dcc8; text-shadow: 0 0 18px rgba(179, 24, 58, 0.55);
}
.dn-title small { font-size: 11px; letter-spacing: 2px; color: rgba(217, 176, 102, 0.75); }
.dn-wallet { display: flex; gap: 10px; }
.dn-wallet span {
  display: flex; align-items: center; gap: 6px;
  padding: 5px 12px; border-radius: 999px;
  font-size: 13px; color: #e8dcc8;
  background: rgba(16, 3, 10, 0.7);
  border: 1px solid rgba(255, 59, 107, 0.2);
}
.dn-wallet i {
  font-style: normal; font-size: 11px;
  width: 18px; height: 18px; line-height: 18px; text-align: center;
  border-radius: 50%;
}
.w-coin i { background: rgba(217, 176, 102, 0.18); color: #d9b066; }
.w-jade i { background: rgba(120, 200, 190, 0.16); color: #8fd9ce; }

/* ===== 主体 ===== */
.dn-body { max-width: 1060px; margin: 0 auto; padding: 26px 30px 10px; }
.dn-empty {
  padding: 80px 0; text-align: center;
  letter-spacing: 6px; color: rgba(232, 220, 200, 0.5);
}
.dn-empty.deny { color: #ff8fa8; letter-spacing: 2px; }

/* ===== 累积总览 ===== */
.sum-card {
  display: flex; align-items: center; gap: 28px;
  padding: 22px 26px; margin-bottom: 22px;
  border-radius: 14px;
  background: linear-gradient(135deg, rgba(30, 6, 18, 0.85), rgba(12, 3, 10, 0.9));
  border: 1px solid rgba(255, 59, 107, 0.22);
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.45);
}
.sum-left { flex: 1; min-width: 0; }
.sum-label { font-size: 13px; letter-spacing: 4px; color: #d9b066; margin-bottom: 12px; }
.sum-progress { display: flex; align-items: center; gap: 14px; }
.sp-bar {
  flex: 1; height: 10px; border-radius: 999px;
  background: rgba(255, 255, 255, 0.07);
  overflow: hidden;
}
.sp-fill {
  height: 100%; border-radius: 999px;
  background: linear-gradient(90deg, #7a1030, #b3183a, #d9b066);
  transition: width 1s linear;
}
.sp-text { font-size: 12px; letter-spacing: 1px; color: rgba(232, 220, 200, 0.75); white-space: nowrap; }
.sum-note { margin-top: 10px; font-size: 12px; color: #ff8fa8; letter-spacing: 1px; }

.sum-right { display: flex; align-items: center; gap: 22px; }
.sum-pendings { display: flex; gap: 14px; }
.pd {
  display: flex; flex-direction: column; align-items: center; gap: 2px;
  min-width: 76px; padding: 10px 12px; border-radius: 10px;
  background: rgba(8, 2, 6, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.06);
}
.pd i {
  width: 10px; height: 10px; border-radius: 50%; margin-bottom: 4px;
}
.pd-sha { background: #b3183a; box-shadow: 0 0 10px rgba(179, 24, 58, 0.8); }
.pd-coin { background: #d9b066; box-shadow: 0 0 10px rgba(217, 176, 102, 0.8); }
.pd-jade { background: #7fded0; box-shadow: 0 0 10px rgba(127, 222, 208, 0.8); }
.pd b { font-size: 20px; color: #ffe9d2; }
.pd small { font-size: 11px; letter-spacing: 2px; color: rgba(232, 220, 200, 0.6); }

.collect-btn {
  padding: 14px 26px; border-radius: 10px; cursor: pointer;
  font-family: inherit; font-size: 15px; letter-spacing: 4px;
  color: #ffe9d2;
  background: linear-gradient(135deg, #7a1030, #b3183a);
  border: 1px solid rgba(255, 59, 107, 0.55);
  box-shadow: 0 6px 24px rgba(179, 24, 58, 0.35);
}
.collect-btn:hover:not(:disabled) { filter: brightness(1.15); }
.collect-btn:disabled { opacity: 0.45; cursor: not-allowed; }

/* ===== 设施 ===== */
.fac-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 18px; }
.fac-card {
  display: flex; flex-direction: column; gap: 12px;
  padding: 20px 20px 18px; border-radius: 14px;
  background: linear-gradient(160deg, rgba(24, 5, 14, 0.85), rgba(10, 3, 8, 0.92));
  border: 1px solid rgba(255, 59, 107, 0.18);
}
.fac-card.is-closed { opacity: 0.9; border-style: dashed; }
.fc-head { display: flex; align-items: center; gap: 12px; }
.fc-seal {
  width: 44px; height: 44px; line-height: 44px; text-align: center;
  border-radius: 10px; font-size: 20px; font-weight: 700;
  color: #0a0208;
}
.seal-sha { background: linear-gradient(135deg, #b3183a, #ff5e6b); }
.seal-coin { background: linear-gradient(135deg, #b8860b, #e8c37a); }
.seal-jade { background: linear-gradient(135deg, #2a9d8f, #7fded0); }
.fc-name { display: flex; flex-direction: column; gap: 2px; }
.fc-name b { font-size: 16px; letter-spacing: 3px; color: #e8dcc8; }
.fc-name small { font-size: 11px; letter-spacing: 1px; color: rgba(217, 176, 102, 0.8); }

.fc-rate {
  padding: 10px 12px; border-radius: 10px;
  background: rgba(8, 2, 6, 0.55);
  border: 1px solid rgba(255, 255, 255, 0.05);
}
.rate-line { display: flex; justify-content: space-between; align-items: baseline; }
.rate-line span { font-size: 11px; letter-spacing: 2px; color: rgba(232, 220, 200, 0.55); }
.rate-line b { font-size: 14px; color: #ffe9d2; }
.rate-line.sub b { color: #ff8fa8; font-size: 12px; }

.fc-pending {
  display: flex; justify-content: space-between; align-items: baseline;
  padding: 0 2px;
}
.fc-pending span { font-size: 11px; letter-spacing: 2px; color: rgba(232, 220, 200, 0.55); }
.fc-pending b { font-size: 15px; color: #d9b066; }

.fc-desc { font-size: 12px; line-height: 1.7; color: rgba(232, 220, 200, 0.55); min-height: 40px; }

.fc-btn {
  margin-top: auto; padding: 10px 0; border-radius: 8px; cursor: pointer;
  font-family: inherit; font-size: 13px; letter-spacing: 2px;
  color: #e8dcc8;
  background: rgba(30, 8, 20, 0.8);
  border: 1px solid rgba(255, 59, 107, 0.35);
}
.fc-btn.gold {
  color: #0a0208;
  background: linear-gradient(135deg, #b8860b, #e8c37a);
  border-color: rgba(232, 195, 122, 0.6);
}
.fc-btn:hover:not(:disabled) { filter: brightness(1.2); }
.fc-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.fc-cost-note { font-size: 11px; color: #ff8fa8; text-align: center; }

.dn-foot {
  margin-top: 22px; padding: 14px 18px;
  font-size: 12px; line-height: 1.9; letter-spacing: 1px;
  color: rgba(232, 220, 200, 0.45);
  border-top: 1px dashed rgba(217, 176, 102, 0.25);
}

.dn-toast {
  position: sticky; bottom: 18px;
  margin-top: 18px; padding: 12px 18px; border-radius: 10px;
  font-size: 13px; letter-spacing: 1px; color: #ffe9d2;
  background: rgba(30, 6, 18, 0.92);
  border: 1px solid rgba(255, 59, 107, 0.4);
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.5);
}

@media (max-width: 960px) {
  .fac-grid { grid-template-columns: 1fr; }
  .sum-card { flex-direction: column; align-items: stretch; }
  .sum-right { justify-content: space-between; }
}
</style>
