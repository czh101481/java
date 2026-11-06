<template>
  <!-- 道途成就：纯荣誉徽记 + 六期进度，不发任何资源 -->
  <div class="ac">
    <div class="ac-aura" aria-hidden="true"></div>

    <header class="ac-head">
      <button class="ac-back" type="button" @click="goHome">⟵ 返 回 魔 域</button>
      <div class="ac-title">
        <span>道 途</span>
        <small>功行自铭 · 徽记与称号 · 不赏一钱</small>
      </div>
      <div class="ac-sum" v-if="d">
        <b>{{ d.unlockedCount }}</b>
        <span>/ {{ d.total }} 枚 已 铭</span>
      </div>
    </header>

    <main class="ac-body">
      <div v-if="loading" class="ac-empty">道 途 气 机 凝 聚 中 ……</div>
      <div v-else-if="deny" class="ac-empty warn">{{ deny }}</div>

      <template v-else-if="d">
        <!-- 总览：完成度 / 下一目标 / 已获称号 -->
        <section class="ov">
          <div class="ov-left">
            <div class="ov-label">道 途 功 行</div>
            <div class="ov-num">{{ d.percent }}<i>%</i></div>
            <div class="ov-bar"><div :style="{ width: d.percent + '%' }"></div></div>
            <div class="ov-note">已铭刻 {{ d.unlockedCount }} / {{ d.total }} 枚徽记</div>
          </div>
          <div class="ov-right">
            <div class="ov-label">下 一 枚 徽 记</div>
            <template v-if="d.next">
              <div class="nx-name">{{ d.next.name }}<em>{{ d.next.stage }}</em></div>
              <div class="nx-sub">{{ d.next.subtitle }}</div>
              <div class="nx-bar"><div :style="{ width: d.next.percent + '%' }"></div></div>
              <div class="nx-prog">{{ d.next.cur }} / {{ d.next.target }} · {{ d.next.percent }}%</div>
            </template>
            <div v-else class="nx-done">诸 徽 皆 铭 · 道 途 圆 满</div>
          </div>
        </section>

        <!-- 六期进度 -->
        <section class="stages">
          <div v-for="s in d.stages" :key="s.stage" class="sg">
            <div class="sg-name">{{ s.stage }}</div>
            <div class="sg-bar"><div :style="{ width: s.percent + '%' }"></div></div>
            <div class="sg-num">{{ s.unlocked }}/{{ s.total }}</div>
          </div>
        </section>

        <!-- 已获称号 -->
        <section class="titles" v-if="d.titles && d.titles.length">
          <span class="tt-label">已 得 称 号</span>
          <span v-for="t in d.titles" :key="t" class="tt">{{ t }}</span>
        </section>

        <!-- 分类筛选 -->
        <nav class="ac-tabs">
          <button
            v-for="c in cats"
            :key="c"
            type="button"
            :class="{ on: active === c }"
            @click="active = c"
          >{{ c }}</button>
        </nav>

        <!-- 徽记墙 -->
        <section class="ac-grid">
          <article v-for="a in shown" :key="a.id" class="ach" :class="{ got: a.unlocked }">
            <div class="ach-icon">{{ a.icon }}</div>
            <div class="ach-main">
              <div class="ach-top">
                <span class="ach-name">{{ a.name }}</span>
                <span class="ach-stage">{{ a.category }} · {{ a.stage }}</span>
              </div>
              <div class="ach-sub">{{ a.subtitle }}</div>
              <div class="ach-desc">{{ a.description }}</div>
              <div class="ach-bar"><div :style="{ width: a.percent + '%' }"></div></div>
              <div class="ach-foot">
                <span>{{ a.cur }} / {{ a.target }} · {{ a.percent }}%</span>
                <span v-if="a.titleReward" class="ach-title">称 号 · {{ a.titleReward }}</span>
              </div>
            </div>
            <div class="ach-flag">
              <template v-if="a.unlocked">
                <span class="flag-got">已 铭</span>
                <small v-if="a.unlockedAt">{{ String(a.unlockedAt).replace('T', ' ').slice(0, 10) }}</small>
              </template>
              <span v-else class="flag-todo">未 竟</span>
            </div>
          </article>
        </section>

        <p class="ac-note">
          道途只记功行，不赏一钱：徽记与称号皆属荣衔，不发魔晶、不发血玉、亦不增煞气。
          达成之时自行铭刻，无须请领；停用或撤下的条目也不会抹去已铭之功行。
        </p>
      </template>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { achievementList } from '../api/achievement'

const router = useRouter()
const d = ref(null)
const loading = ref(true)
const deny = ref('')
const active = ref('全部')

const cats = computed(() => {
  const set = new Set((d.value?.list || []).map(x => x.category || '道途'))
  return ['全部', ...set]
})
const shown = computed(() => {
  const list = d.value?.list || []
  return active.value === '全部' ? list : list.filter(x => (x.category || '道途') === active.value)
})

async function load() {
  loading.value = true
  deny.value = ''
  try {
    const res = await achievementList()
    if (res.data.code === 200) {
      d.value = res.data.data
    } else {
      deny.value = res.data.msg || '道途未启，请重新登录。'
    }
  } catch (e) {
    deny.value = e.message || '魔门气机不通，稍后再试。'
  } finally {
    loading.value = false
  }
}

function goHome() {
  router.push('/home')
}

onMounted(load)
</script>

<style scoped>
.ac {
  position: fixed; inset: 0; z-index: 3; overflow-y: auto; padding-bottom: 96px;
  background: radial-gradient(ellipse at 50% 18%, #150611 0%, #0a0308 55%, #040103 100%);
}
.ac-aura {
  position: fixed; inset: 0; pointer-events: none;
  background:
    radial-gradient(circle at 18% 10%, rgba(160, 22, 58, 0.20), transparent 46%),
    radial-gradient(circle at 82% 80%, rgba(70, 130, 190, 0.10), transparent 48%);
}
.ac-head {
  position: sticky; top: 0; z-index: 6;
  display: flex; align-items: center; gap: 20px; padding: 16px 34px;
  background: rgba(8, 2, 12, 0.78); backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(255, 59, 107, 0.18);
}
.ac-back {
  padding: 7px 15px; border-radius: 8px; cursor: pointer; font-family: inherit;
  font-size: 12px; letter-spacing: 2px; color: rgba(232, 220, 200, 0.85);
  background: rgba(20, 4, 14, 0.7); border: 1px solid rgba(255, 59, 107, 0.25);
}
.ac-back:hover { color: #ffe9d2; border-color: rgba(255, 59, 107, 0.5); }
.ac-title { flex: 1; display: flex; flex-direction: column; gap: 2px; }
.ac-title span {
  font-size: 22px; letter-spacing: 8px; color: #e8dcc8;
  text-shadow: 0 0 18px rgba(179, 24, 58, 0.55);
}
.ac-title small { font-size: 11px; letter-spacing: 2px; color: rgba(217, 176, 102, 0.75); }
.ac-sum { display: flex; align-items: baseline; gap: 8px; }
.ac-sum b { font-size: 26px; color: #ffd7a1; }
.ac-sum span { font-size: 12px; letter-spacing: 2px; color: rgba(232, 220, 200, 0.6); }

.ac-body { max-width: 1180px; margin: 0 auto; padding: 28px 30px 60px; position: relative; z-index: 2; }
.ac-empty { padding: 90px 0; text-align: center; letter-spacing: 5px; color: rgba(232, 220, 200, 0.55); }
.ac-empty.warn { color: #ff9bb2; }

.ov {
  display: grid; grid-template-columns: 320px 1fr; gap: 18px;
  padding: 22px 26px; border-radius: 14px;
  background: rgba(24, 8, 18, 0.72); border: 1px solid rgba(217, 176, 102, 0.22);
}
.ov-label { font-size: 11px; letter-spacing: 4px; color: rgba(217, 176, 102, 0.8); }
.ov-num { font-size: 46px; line-height: 1.25; color: #ffd7a1; }
.ov-num i { font-size: 18px; font-style: normal; margin-left: 4px; }
.ov-bar, .nx-bar, .sg-bar, .ach-bar {
  height: 7px; border-radius: 6px; overflow: hidden;
  background: rgba(255, 255, 255, 0.08);
}
.ov-bar > div, .nx-bar > div, .ach-bar > div {
  height: 100%; background: linear-gradient(90deg, #b3183a, #ff7a5c);
}
.ov-bar { margin: 12px 0 8px; }
.ov-note { font-size: 12px; letter-spacing: 1px; color: rgba(232, 220, 200, 0.55); }
.ov-right { border-left: 1px solid rgba(255, 255, 255, 0.08); padding-left: 24px; }
.nx-name { margin: 12px 0 4px; font-size: 20px; letter-spacing: 3px; color: #e8dcc8; }
.nx-name em {
  margin-left: 10px; font-size: 11px; font-style: normal; letter-spacing: 2px;
  padding: 3px 9px; border-radius: 20px; color: #ffd7a1;
  background: rgba(217, 176, 102, 0.14); border: 1px solid rgba(217, 176, 102, 0.3);
}
.nx-sub { font-size: 12px; color: rgba(232, 220, 200, 0.6); letter-spacing: 1px; }
.nx-bar { margin: 14px 0 6px; }
.nx-prog { font-size: 12px; color: rgba(255, 215, 161, 0.85); }
.nx-done { margin-top: 22px; font-size: 16px; letter-spacing: 4px; color: #ffd7a1; }

.stages {
  display: grid; grid-template-columns: repeat(6, 1fr); gap: 12px; margin: 20px 0 4px;
}
.sg {
  padding: 12px 14px; border-radius: 10px;
  background: rgba(24, 8, 18, 0.6); border: 1px solid rgba(255, 59, 107, 0.14);
}
.sg-name { font-size: 12px; letter-spacing: 3px; color: rgba(232, 220, 200, 0.75); }
.sg-bar { height: 5px; margin: 10px 0 6px; }
.sg-bar > div { height: 100%; background: linear-gradient(90deg, #7f5bd6, #d9b066); }
.sg-num { font-size: 11px; color: rgba(217, 176, 102, 0.85); }

.titles { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; margin: 18px 0 6px; }
.tt-label { font-size: 11px; letter-spacing: 3px; color: rgba(232, 220, 200, 0.5); margin-right: 6px; }
.tt {
  padding: 4px 12px; border-radius: 20px; font-size: 12px; letter-spacing: 1px;
  color: #ffd7a1; background: rgba(217, 176, 102, 0.10);
  border: 1px solid rgba(217, 176, 102, 0.28);
}

.ac-tabs { display: flex; flex-wrap: wrap; gap: 8px; margin: 24px 0 16px; }
.ac-tabs button {
  padding: 7px 16px; border-radius: 8px; cursor: pointer; font-family: inherit;
  font-size: 12px; letter-spacing: 2px; color: rgba(232, 220, 200, 0.65);
  background: rgba(20, 6, 14, 0.6); border: 1px solid rgba(255, 59, 107, 0.18);
}
.ac-tabs button:hover { color: #ffe9d2; }
.ac-tabs button.on {
  color: #ffe9d2; background: rgba(179, 24, 58, 0.28);
  border-color: rgba(255, 59, 107, 0.55);
}

.ac-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; }
.ach {
  display: flex; gap: 16px; padding: 18px 20px; border-radius: 12px;
  background: rgba(18, 6, 14, 0.62); border: 1px solid rgba(255, 255, 255, 0.08);
  transition: border-color 0.2s, background 0.2s;
}
.ach.got { background: rgba(40, 14, 24, 0.66); border-color: rgba(217, 176, 102, 0.34); }
.ach-icon {
  flex: 0 0 52px; width: 52px; height: 52px; border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  font-size: 24px; color: rgba(232, 220, 200, 0.4);
  background: rgba(255, 255, 255, 0.05); border: 1px solid rgba(255, 255, 255, 0.08);
}
.ach.got .ach-icon {
  color: #ffd7a1; background: rgba(217, 176, 102, 0.14);
  border-color: rgba(217, 176, 102, 0.45);
}
.ach-main { flex: 1; min-width: 0; }
.ach-top { display: flex; align-items: baseline; gap: 10px; flex-wrap: wrap; }
.ach-name { font-size: 16px; letter-spacing: 2px; color: #e8dcc8; }
.ach-stage { font-size: 11px; letter-spacing: 1px; color: rgba(217, 176, 102, 0.7); }
.ach-sub { margin-top: 5px; font-size: 12px; color: rgba(255, 215, 161, 0.8); letter-spacing: 1px; }
.ach-desc { margin-top: 4px; font-size: 12px; color: rgba(232, 220, 200, 0.48); line-height: 1.6; }
.ach-bar { height: 5px; margin: 12px 0 6px; }
.ach-foot { display: flex; justify-content: space-between; gap: 10px; flex-wrap: wrap; font-size: 11px; color: rgba(232, 220, 200, 0.5); }
.ach-title { color: rgba(217, 176, 102, 0.8); }
.ach-flag { flex: 0 0 62px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 4px; }
.flag-got { font-size: 12px; letter-spacing: 2px; color: #ffd7a1; }
.flag-todo { font-size: 12px; letter-spacing: 2px; color: rgba(232, 220, 200, 0.32); }
.ach-flag small { font-size: 10px; color: rgba(232, 220, 200, 0.4); }

.ac-note {
  margin: 26px 0 0; padding: 14px 18px; border-radius: 10px;
  font-size: 12px; line-height: 1.8; letter-spacing: 1px;
  color: rgba(232, 220, 200, 0.55);
  background: rgba(255, 255, 255, 0.03); border: 1px solid rgba(255, 255, 255, 0.07);
}

@media (max-width: 900px) {
  .ov { grid-template-columns: 1fr; }
  .ov-right { border-left: none; border-top: 1px solid rgba(255, 255, 255, 0.08); padding: 18px 0 0; }
  .stages { grid-template-columns: repeat(3, 1fr); }
  .ac-grid { grid-template-columns: 1fr; }
  .ac-head { padding: 12px 16px; gap: 12px; }
  .ac-body { padding: 20px 16px 50px; }
}
</style>
