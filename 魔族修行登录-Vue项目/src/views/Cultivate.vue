<template>
  <!-- 修为：数值面板（战力 / 五维 / 九重进度 / 已习功法） -->
  <div class="cu">
    <div class="cu-aura" aria-hidden="true"></div>

    <header class="cu-head">
      <button class="cu-back" type="button" @click="goHome">⟵ 返 回 魔 域</button>
      <div class="cu-title">
        <span>修 为</span>
        <small>境界分重 · 功法加成 · 数值总览</small>
      </div>
      <div class="cu-power" v-if="panel">
        <small>战 力</small>
        <b>{{ fmt(panel.power) }}</b>
      </div>
    </header>

    <main class="cu-body">
      <div v-if="loading" class="cu-empty">魔 气 凝 聚 中 ……</div>
      <div v-else-if="deny" class="cu-empty deny">{{ deny }}</div>

      <template v-else-if="panel">
        <!-- ==================== 境界 · 重数 ==================== -->
        <section class="realm-card">
          <div class="rc-left">
            <div class="rc-realm">{{ panel.cultivation }}<i>境</i></div>
            <div class="rc-title">{{ panel.realmTitle }}</div>
            <div class="rc-layer">
              第 <b>{{ panel.layerName }}</b> 重
              <em v-if="panel.layerFull">· 九重圆满</em>
            </div>
          </div>

          <div class="rc-mid">
            <div class="rc-dots">
              <span
                v-for="n in panel.layerMax"
                :key="n"
                class="dot"
                :class="{ on: n <= panel.layer, full: n === panel.layer }"
              >{{ n }}</span>
            </div>
            <div class="rc-progress">
              <i :style="{ width: panel.layerPercent + '%' }"></i>
              <span>
                本重 {{ fmt(panel.exp) }} / {{ fmt(panel.layerNextExp) }} 煞气
                · {{ panel.layerPercent }}%
              </span>
            </div>
            <div class="rc-next">
              <template v-if="panel.nextRealm">
                下一境「{{ panel.nextRealm }}」需 {{ fmt(panel.nextRealmExp) }} 煞气
                <i v-if="!panel.layerFull">（须先修至九重圆满）</i>
                <i v-else-if="panel.exp < panel.nextRealmExp">
                  （还差 {{ fmt(panel.nextRealmExp - panel.exp) }}）
                </i>
                <i v-else class="ready">（可冲击）</i>
              </template>
              <template v-else>已至无上之境，九重之外再无桎梏。</template>
            </div>
          </div>

          <div class="rc-right">
            <div class="rc-item"><small>累计重数</small><b>{{ panel.totalLayer }}</b></div>
            <div class="rc-item"><small>已习功法</small><b>{{ panel.learnedCount }} / {{ panel.artCap }}</b></div>
            <div class="rc-item"><small>魔种</small><b class="sm">{{ panel.originTitle || '—' }}</b></div>
          </div>
        </section>

        <!-- ==================== 五维 ==================== -->
        <section class="attr-wrap">
          <div class="attr-title"><span>五 维 数 值</span><em>境界成长 + 功法加成</em></div>
          <div class="attr-grid">
            <article v-for="a in panel.attrs" :key="a.key" class="attr-card" :class="'a-' + a.key">
              <div class="ac-name">{{ a.name }}</div>
              <div class="ac-value">{{ fmt(a.value) }}</div>
              <div class="ac-split">
                <span>境界 {{ fmt(a.fromRealm) }}</span>
                <span class="art">功法 +{{ fmt(a.fromArt) }}</span>
              </div>
              <p class="ac-desc">{{ a.desc }}</p>
            </article>
          </div>
        </section>

        <!-- ==================== 战力拆解 ==================== -->
        <section class="power-card">
          <div class="pc-head">
            <span>战 力 拆 解</span>
            <em>切磋、秘境与英杰榜同源</em>
          </div>
          <div class="pc-rows">
            <div class="pc-row">
              <span>境界折算<i>境阶含重 × 8000</i></span>
              <b>{{ fmt(panel.powerRealm) }}</b>
            </div>
            <div class="pc-row">
              <span>累计煞气</span>
              <b>{{ fmt(panel.powerExp) }}</b>
            </div>
            <div class="pc-row">
              <span>功法战力<i>攻伐×8 + 护体×6 + 气血×1 + 悟性×4 + 身法×3</i></span>
              <b>{{ fmt(panel.powerArt) }}</b>
            </div>
            <div class="pc-row" v-if="panel.originPowerMod > 0">
              <span>魔种天赋<i>{{ panel.originTitle }}</i></span>
              <b>×{{ (1 + panel.originPowerMod).toFixed(2) }}</b>
            </div>
            <div class="pc-row total">
              <span>合计战力</span>
              <b>{{ fmt(panel.power) }}</b>
            </div>
          </div>
        </section>

        <!-- ==================== 已习功法 ==================== -->
        <section class="art-wrap">
          <div class="art-title">
            <span>已 习 功 法</span>
            <em>
              {{ panel.learnedCount }} / {{ panel.artCap }} 部
              <a @click="goModule('art')">前 往 功 法 阁 ⟶</a>
            </em>
          </div>

          <div v-if="!panel.arts.length" class="art-empty">
            尚 未 习 得 任 何 功 法 —— 可 以 魔 晶 于 功 法 阁 购 得 黄 级 入 门 功 法 。
          </div>

          <div v-else class="art-list">
            <article v-for="a in panel.arts" :key="a.id" class="art-row">
              <div class="ar-grade" :class="gradeClass(a.grade)">{{ a.grade }}</div>
              <div class="ar-main">
                <div class="ar-name">
                  {{ a.name }}
                  <span class="ar-layer">{{ a.layer }} 重</span>
                  <span class="ar-cat">{{ a.category }}</span>
                </div>
                <div class="ar-bonus">
                  <span>攻伐 +{{ a.atk }}</span>
                  <span>护体 +{{ a.def }}</span>
                  <span>气血 +{{ a.hp }}</span>
                  <span>悟性 +{{ a.insight }}</span>
                  <span>身法 +{{ a.agility }}</span>
                </div>
              </div>
              <div class="ar-power">
                <small>战力</small>
                <b>+{{ fmt(a.power) }}</b>
              </div>
            </article>
          </div>
        </section>

        <p class="cu-foot">
          境界每境九重，重数随煞气增长自动推进，只升不降；九重圆满方可冲境。
          功法分重：黄 1-4 · 玄 1-6 · 地 1-7 · 天 1-10 · 仙 1-10，同系列须按重递进。
        </p>
      </template>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getSession } from '../api/auth'
import { cultivatePanel } from '../api/cultivate'

const router = useRouter()
const session = getSession()

const loading = ref(true)
const deny = ref('')
const panel = ref(null)

function fmt(n) {
  const v = Number(n) || 0
  return v.toLocaleString('zh-CN')
}

function gradeClass(g) {
  return 'g-' + ({ 黄: 'huang', 玄: 'xuan', 地: 'di', 天: 'tian', 仙: 'xian' }[g] || 'huang')
}

async function load() {
  loading.value = true
  deny.value = ''
  try {
    const res = await cultivatePanel()
    if (res.data.code === 200) panel.value = res.data.data
    else deny.value = res.data.msg || '魔渊无应，数值难测。'
  } catch (e) {
    deny.value = e.message || '魔门无应，连通失败'
  } finally {
    loading.value = false
  }
}

function goHome() { router.push('/home') }
function goModule(key) { router.push('/module/' + key) }

onMounted(() => {
  if (!session.token) {
    router.replace('/login')
    return
  }
  load()
})
</script>

<style scoped>
.cu {
  position: fixed;
  inset: 0;
  /* 高于全局背景低语（.whisper z-index:2），避免页脚文字压住内容 */
  z-index: 3;
  overflow-y: auto;
  padding-bottom: 96px;
  background: radial-gradient(ellipse at 50% 18%, #1c0510 0%, #0b0207 55%, #050104 100%);
  font-family: inherit;
}
.cu-aura {
  position: fixed; inset: 0; pointer-events: none;
  background:
    radial-gradient(circle at 15% 12%, rgba(179, 24, 58, 0.18), transparent 45%),
    radial-gradient(circle at 85% 78%, rgba(120, 60, 200, 0.14), transparent 48%);
}

/* ===== 头部 ===== */
.cu-head {
  position: sticky; top: 0; z-index: 6;
  display: flex; align-items: center; gap: 20px;
  padding: 16px 34px;
  background: rgba(8, 2, 12, 0.78);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(255, 59, 107, 0.18);
}
.cu-back {
  padding: 7px 15px; border-radius: 8px; cursor: pointer;
  font-family: inherit; font-size: 12px; letter-spacing: 2px;
  color: rgba(232, 220, 200, 0.85);
  background: rgba(20, 4, 14, 0.7);
  border: 1px solid rgba(255, 59, 107, 0.35);
  transition: 0.25s;
}
.cu-back:hover { color: #fff; border-color: #ff3b6b; }
.cu-title { flex: 1; display: flex; flex-direction: column; }
.cu-title span {
  font-size: 20px; letter-spacing: 6px; color: #e8dcc8;
  text-shadow: 0 0 14px rgba(255, 59, 107, 0.6);
}
.cu-title small { font-size: 11px; letter-spacing: 3px; color: rgba(217, 176, 102, 0.7); margin-top: 4px; }
.cu-power { text-align: right; }
.cu-power small { display: block; font-size: 11px; letter-spacing: 3px; color: rgba(217, 176, 102, 0.75); }
.cu-power b {
  font-size: 24px; font-weight: normal; color: #ffd9a0;
  text-shadow: 0 0 18px rgba(255, 179, 71, 0.55);
}

/* ===== 主体 ===== */
.cu-body { position: relative; z-index: 1; width: 92%; max-width: 1080px; margin: 26px auto 0; }
.cu-empty {
  padding: 50px; text-align: center; font-size: 13px; letter-spacing: 4px;
  color: rgba(232, 220, 200, 0.45);
}
.cu-empty.deny { color: rgba(255, 107, 143, 0.9); }

/* ===== 境界卡 ===== */
.realm-card {
  display: grid;
  grid-template-columns: 210px 1fr 190px;
  gap: 22px;
  padding: 22px 26px;
  border-radius: 14px;
  background: linear-gradient(150deg, rgba(38, 6, 20, 0.78), rgba(8, 2, 10, 0.85));
  border: 1px solid rgba(255, 59, 107, 0.24);
  box-shadow: 0 0 40px rgba(179, 24, 58, 0.16);
}
.rc-realm {
  font-size: 30px; letter-spacing: 4px; color: #e8dcc8;
  text-shadow: 0 0 18px rgba(255, 59, 107, 0.55);
}
.rc-realm i { font-style: normal; font-size: 15px; color: rgba(255, 107, 143, 0.85); margin-left: 4px; }
.rc-title { font-size: 12px; letter-spacing: 3px; color: rgba(217, 176, 102, 0.85); margin-top: 8px; }
.rc-layer { font-size: 13px; letter-spacing: 2px; color: rgba(232, 220, 200, 0.7); margin-top: 12px; }
.rc-layer b { font-weight: normal; color: #ff9db4; font-size: 16px; }
.rc-layer em { font-style: normal; color: #7ee0a0; font-size: 12px; }

.rc-dots { display: flex; gap: 7px; }
.dot {
  flex: 1;
  height: 30px; line-height: 30px; text-align: center;
  font-size: 12px; color: rgba(232, 220, 200, 0.35);
  border: 1px solid rgba(255, 59, 107, 0.2);
  border-radius: 6px;
  background: rgba(10, 2, 8, 0.6);
  transition: 0.25s;
}
.dot.on { color: #ffd9e2; border-color: rgba(255, 59, 107, 0.6); background: rgba(120, 12, 40, 0.5); }
.dot.full {
  color: #fff;
  border-color: #ff5b82;
  box-shadow: 0 0 14px rgba(255, 59, 107, 0.55);
}
.rc-progress {
  position: relative; height: 24px; margin-top: 14px;
  border-radius: 12px; overflow: hidden;
  background: rgba(10, 2, 8, 0.75);
  border: 1px solid rgba(255, 59, 107, 0.22);
}
.rc-progress i {
  position: absolute; inset: 0 auto 0 0;
  background: linear-gradient(90deg, rgba(179, 24, 58, 0.85), rgba(255, 90, 130, 0.9));
  transition: width 0.6s ease;
}
.rc-progress span {
  position: relative; z-index: 1;
  display: block; text-align: center; line-height: 24px;
  font-size: 11px; letter-spacing: 1px; color: #ffe6ec;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.8);
}
.rc-next { margin-top: 10px; font-size: 12px; letter-spacing: 1px; color: rgba(232, 220, 200, 0.6); }
.rc-next i { font-style: normal; color: rgba(255, 159, 180, 0.9); }
.rc-next i.ready { color: #7ee0a0; }

.rc-right { display: flex; flex-direction: column; gap: 10px; }
.rc-item {
  display: flex; align-items: baseline; justify-content: space-between;
  padding: 7px 12px; border-radius: 8px;
  background: rgba(6, 2, 10, 0.6);
  border: 1px solid rgba(217, 176, 102, 0.16);
}
.rc-item small { font-size: 11px; letter-spacing: 2px; color: rgba(217, 176, 102, 0.8); }
.rc-item b { font-weight: normal; font-size: 15px; color: #ffd9a0; }
.rc-item b.sm { font-size: 12px; }

/* ===== 五维 ===== */
.attr-wrap, .power-card, .art-wrap { margin-top: 22px; }
.attr-title, .art-title, .pc-head {
  display: flex; align-items: baseline; gap: 14px; margin-bottom: 12px;
}
.attr-title span, .art-title span, .pc-head span {
  font-size: 15px; letter-spacing: 5px; color: #e8dcc8;
}
.attr-title em, .pc-head em {
  font-style: normal; font-size: 11px; letter-spacing: 2px; color: rgba(232, 220, 200, 0.45);
}
.attr-grid {
  display: grid; grid-template-columns: repeat(5, 1fr); gap: 12px;
}
.attr-card {
  padding: 16px 14px;
  border-radius: 12px;
  background: linear-gradient(160deg, rgba(30, 5, 18, 0.72), rgba(8, 2, 10, 0.82));
  border: 1px solid rgba(255, 59, 107, 0.18);
  transition: 0.25s;
}
.attr-card:hover { border-color: rgba(255, 59, 107, 0.5); box-shadow: 0 0 22px rgba(179, 24, 58, 0.22); }
.ac-name { font-size: 12px; letter-spacing: 3px; color: rgba(217, 176, 102, 0.85); }
.ac-value {
  font-size: 26px; letter-spacing: 1px; color: #ffd9a0; margin: 8px 0 6px;
  text-shadow: 0 0 16px rgba(255, 179, 71, 0.4);
}
.ac-split { display: flex; justify-content: space-between; font-size: 10px; letter-spacing: 1px; color: rgba(232, 220, 200, 0.45); }
.ac-split .art { color: rgba(196, 181, 253, 0.9); }
.ac-desc { font-size: 10px; line-height: 1.7; color: rgba(232, 220, 200, 0.35); margin-top: 8px; }
.a-hp .ac-value { color: #ff9db4; }

/* ===== 战力拆解 ===== */
.power-card {
  padding: 20px 24px;
  border-radius: 14px;
  background: linear-gradient(160deg, rgba(34, 6, 20, 0.72), rgba(8, 2, 10, 0.82));
  border: 1px solid rgba(217, 176, 102, 0.22);
}
.pc-rows { display: flex; flex-direction: column; }
.pc-row {
  display: flex; align-items: baseline; justify-content: space-between;
  padding: 9px 0;
  border-top: 1px dashed rgba(255, 59, 107, 0.14);
  font-size: 13px; color: rgba(232, 220, 200, 0.75);
}
.pc-row:first-child { border-top: none; }
.pc-row i {
  display: block; font-style: normal; font-size: 10px; letter-spacing: 1px;
  color: rgba(232, 220, 200, 0.35); margin-top: 3px;
}
.pc-row b { font-weight: normal; color: #e8dcc8; font-size: 15px; }
.pc-row.total { border-top: 1px solid rgba(217, 176, 102, 0.35); margin-top: 6px; padding-top: 12px; }
.pc-row.total span { letter-spacing: 3px; color: #d9b066; }
.pc-row.total b { font-size: 22px; color: #ffd9a0; text-shadow: 0 0 16px rgba(255, 179, 71, 0.45); }

/* ===== 已习功法 ===== */
.art-title em { flex: 1; font-style: normal; font-size: 11px; letter-spacing: 2px; color: rgba(232, 220, 200, 0.45); }
.art-title a { color: rgba(217, 176, 102, 0.9); cursor: pointer; margin-left: 10px; }
.art-title a:hover { color: #ffd9a0; text-shadow: 0 0 10px rgba(217, 176, 102, 0.6); }
.art-empty {
  padding: 26px; text-align: center; font-size: 12px; letter-spacing: 3px;
  color: rgba(232, 220, 200, 0.4);
  border: 1px dashed rgba(255, 59, 107, 0.3); border-radius: 12px;
}
.art-list { display: flex; flex-direction: column; gap: 10px; }
.art-row {
  display: flex; align-items: center; gap: 14px;
  padding: 12px 16px;
  border-radius: 11px;
  background: linear-gradient(150deg, rgba(28, 5, 17, 0.62), rgba(8, 2, 10, 0.78));
  border: 1px solid rgba(255, 59, 107, 0.16);
}
.ar-grade {
  flex: 0 0 34px; width: 34px; height: 34px; border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  font-size: 15px;
  border: 1px solid rgba(255, 59, 107, 0.4);
  color: #ffd9e2;
  background: rgba(120, 12, 40, 0.35);
}
.ar-grade.g-xuan { border-color: rgba(167, 139, 250, 0.55); color: #d6c8ff; background: rgba(70, 40, 130, 0.3); }
.ar-grade.g-di { border-color: rgba(102, 204, 170, 0.5); color: #b9f2dd; background: rgba(20, 90, 70, 0.28); }
.ar-grade.g-tian { border-color: rgba(217, 176, 102, 0.6); color: #ffe3a8; background: rgba(120, 85, 10, 0.28); }
.ar-grade.g-xian { border-color: rgba(255, 120, 90, 0.7); color: #ffcdb8; background: rgba(140, 40, 10, 0.35); }

.ar-main { flex: 1; min-width: 0; }
.ar-name { font-size: 14px; letter-spacing: 2px; color: #e8dcc8; }
.ar-layer {
  margin-left: 8px; padding: 1px 8px; border-radius: 10px;
  font-size: 10px; letter-spacing: 1px;
  color: #ffb3c6; border: 1px solid rgba(255, 59, 107, 0.4);
}
.ar-cat { margin-left: 8px; font-size: 10px; color: rgba(232, 220, 200, 0.4); letter-spacing: 1px; }
.ar-bonus { display: flex; flex-wrap: wrap; gap: 10px; margin-top: 6px; font-size: 11px; color: rgba(196, 181, 253, 0.85); }
.ar-power { flex: 0 0 auto; text-align: right; }
.ar-power small { display: block; font-size: 10px; letter-spacing: 2px; color: rgba(217, 176, 102, 0.7); }
.ar-power b { font-weight: normal; font-size: 15px; color: #ffd9a0; }

.cu-foot {
  margin-top: 26px; text-align: center;
  font-size: 11px; line-height: 2; letter-spacing: 2px;
  color: rgba(232, 220, 200, 0.32);
}

@media (max-width: 900px) {
  .realm-card { grid-template-columns: 1fr; }
  .rc-right { flex-direction: row; flex-wrap: wrap; }
  .rc-item { flex: 1 1 140px; }
  .attr-grid { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 720px) {
  .cu-head { padding: 12px 16px; }
  .cu-title span { font-size: 16px; letter-spacing: 4px; }
  .cu-power b { font-size: 18px; }
  .attr-grid { grid-template-columns: 1fr; }
}
</style>
