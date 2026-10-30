<template>
  <!-- 后台管理系统 · 五大内容模块统一管理 -->
  <div class="admin-page">
    <!-- 顶栏：模块页签 -->
    <header class="admin-nav">
      <div class="admin-brand">
        <span class="mark">后台管理系统</span>
        <span class="sub">魔 门 藏 经 殿 · 数 据 管 理</span>
      </div>
      <div class="admin-meta">
        <span class="who">执印人 · {{ username || 'admin' }}</span>
        <router-link class="ghost-btn" to="/home">前 往 首 页</router-link>
        <button class="ghost-btn warn" type="button" @click="onLogout">退 出 后 台</button>
      </div>
    </header>

    <!-- 模块切换 -->
    <nav class="mod-tabs">
      <button
        v-for="(m, key) in MODULES"
        :key="key"
        type="button"
        class="mod-tab"
        :class="{ on: modKey === key }"
        @click="switchModule(key)"
      >{{ m.label }}</button>
    </nav>

    <!-- 工具栏 -->
    <section class="toolbar">
      <form class="search" @submit.prevent="loadList">
        <input
          v-model="kw"
          type="text"
          class="search-input"
          :placeholder="cur.kwPlaceholder"
        />
        <select v-if="modKey === 'archive'" v-model="cat" class="search-input cat-select">
          <option value="">全部分类</option>
          <option v-for="o in CATS" :key="o.v" :value="o.v">{{ o.t }}</option>
        </select>
        <button type="submit" class="gold-btn">检 索</button>
      </form>
      <button type="button" class="gold-btn primary" @click="openForm(null)">+ {{ cur.addLabel }}</button>
    </section>

    <!-- 数据表 -->
    <section class="table-wrap">
      <table class="art-table" v-if="list.length">
        <thead>
          <tr>
            <th v-if="cur.imgDir" style="width: 64px">配图</th>
            <th style="width: 64px">ID</th>
            <th v-for="c in cur.columns" :key="c.key" :style="c.w ? { width: c.w } : null">{{ c.label }}</th>
            <th style="width: 150px">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in list" :key="row.id">
            <td v-if="cur.imgDir">
              <span
                class="thumb"
                v-if="row.image"
                :style="{ backgroundImage: `url(${imgUrl(row.image)})` }"
              ></span>
              <span class="thumb empty" v-else>无</span>
            </td>
            <td>{{ row.id }}</td>
            <td
              v-for="c in cur.columns"
              :key="c.key"
              :class="{ strong: c.key === cur.titleKey, dim: c.dim }"
            >{{ render(row, c) }}</td>
            <td>
              <button type="button" class="row-btn" @click="openForm(row)">修 改</button>
              <button type="button" class="row-btn danger" @click="onDelete(row)">删 除</button>
            </td>
          </tr>
        </tbody>
      </table>
      <div class="empty" v-else>{{ cur.label }} 空 空 如 也 。</div>
    </section>

    <!-- 录入 / 修改 弹窗 -->
    <div class="modal-mask" v-if="showForm" @click.self="closeForm">
      <div class="modal">
        <h3 class="modal-title">{{ form.id ? '修 改 ' + cur.short : '录 入 · ' + cur.short }}</h3>
        <form @submit.prevent="onSave">
          <div class="grid2">
            <div
              v-for="f in cur.fields"
              :key="f.key"
              class="field"
              :class="{ wide: f.wide }"
            >
              <label>{{ f.label }}<template v-if="f.required"> *</template></label>

              <!-- 配图下拉：选项来自后端磁盘同步 -->
              <select v-if="f.type === 'image'" v-model="form[f.key]">
                <option value="">不配图</option>
                <option v-for="img in imgOptions" :key="img" :value="img">{{ img }}</option>
              </select>

              <select v-else-if="f.type === 'select'" v-model="form[f.key]">
                <option v-for="o in f.options" :key="o.v" :value="o.v">{{ o.t }}</option>
              </select>

              <textarea
                v-else-if="f.type === 'textarea'"
                v-model="form[f.key]"
                rows="5"
                :placeholder="f.ph || ''"
              ></textarea>

              <input
                v-else
                v-model.trim="form[f.key]"
                :type="f.type || 'text'"
                :maxlength="f.max || 200"
                :placeholder="f.ph || ''"
              />
            </div>
          </div>
          <p class="img-hint" v-if="cur.imgDir">
            配图清单由后端 <code>static/images/{{ cur.imgDir }}/</code> 实时同步，向该目录投放新图后刷新本页即可选用。
          </p>
          <div class="modal-foot">
            <button type="button" class="gold-btn" @click="closeForm">取 消</button>
            <button type="submit" class="gold-btn primary" :disabled="saving">
              {{ saving ? '封 存 中 …' : '封 存' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { imgUrl } from '../api/home'
import {
  archiveAdd, archiveDelete, archiveList, archiveUpdate,
  artAdd, artDelete, artList, artUpdate,
  codexAdd, codexDelete, codexList, codexUpdate,
  debateAdd, debateDelete, debateList, debateUpdate,
  clearAdminSession, getAdminSession,
  imageList, trialAdd, trialDelete, trialList, trialUpdate
} from '../api/admin'

// ==================== 常量选项 ====================
const REALMS = ['引煞', '凝煞', '魔卒', '魔尉', '魔将', '血煞', '魔尊', '魔祖', '无上', '魔圣'].map(r => ({ v: r, t: r }))
const CATS = [
  { v: 'lineage', t: '魔族谱系' },
  { v: 'path', t: '魔修道途' },
  { v: 'law', t: '魔域法则' },
  { v: 'ancient', t: '上古秘录' }
]
const CAT_MAP = Object.fromEntries(CATS.map(o => [o.v, o.t]))

// ==================== 模块配置（配置驱动：列表列 + 表单字段） ====================
const MODULES = {
  art: {
    label: '功 法 阁', short: '功法', api: { list: artList, add: artAdd, update: artUpdate, del: artDelete },
    imgDir: 'art', titleKey: 'name', addLabel: '录 入 新 功 法',
    kwPlaceholder: '按功法名 / 简介检索…',
    columns: [
      { key: 'name', label: '功法名' },
      { key: 'category', label: '类别', w: '90px' },
      { key: 'grade', label: '品阶', w: '70px' },
      { key: 'realmReq', label: '所需境界', w: '90px' },
      { key: 'summary', label: '简介', dim: true }
    ],
    fields: [
      { key: 'name', label: '功法名', required: true, ph: '如：血煞戮天诀' },
      { key: 'category', label: '类别', type: 'select', options: ['攻伐', '护体', '幻术', '炼魂', '御兽'].map(v => ({ v, t: v })) },
      { key: 'grade', label: '品阶', type: 'select', options: ['黄', '玄', '地', '天', '仙'].map(v => ({ v, t: v })) },
      { key: 'realmReq', label: '所需境界', type: 'select', options: REALMS },
      { key: 'image', label: '配图', type: 'image' },
      { key: 'summary', label: '简介', wide: true, ph: '一句话点明此功法之要义…' },
      { key: 'content', label: '正文', type: 'textarea', wide: true, ph: '功法正文…' }
    ]
  },
  trial: {
    label: '历 练 录', short: '历练', api: { list: trialList, add: trialAdd, update: trialUpdate, del: trialDelete },
    imgDir: 'trial', titleKey: 'title', addLabel: '录 入 新 历 练',
    kwPlaceholder: '按历练名 / 地域 / 简介检索…',
    columns: [
      { key: 'title', label: '历练名' },
      { key: 'region', label: '地域', w: '110px' },
      { key: 'realmReq', label: '所需境界', w: '90px' },
      { key: 'difficulty', label: '凶险', w: '70px' },
      { key: 'summary', label: '简介', dim: true }
    ],
    fields: [
      { key: 'title', label: '历练名', required: true, ph: '如：幽冥血渊试炼' },
      { key: 'region', label: '地域', ph: '如：北境魔原' },
      { key: 'realmReq', label: '所需境界', type: 'select', options: REALMS },
      { key: 'difficulty', label: '凶险', type: 'select', options: ['低', '中', '高', '绝'].map(v => ({ v, t: v })) },
      { key: 'image', label: '配图', type: 'image' },
      { key: 'summary', label: '简介', wide: true, ph: '一句话点明此历练之凶险…' }
    ]
  },
  debate: {
    label: '宗 门 论 道', short: '论题', api: { list: debateList, add: debateAdd, update: debateUpdate, del: debateDelete },
    imgDir: '', titleKey: 'title', addLabel: '录 入 新 论 题',
    kwPlaceholder: '按论题 / 论者 / 摘要检索…',
    columns: [
      { key: 'title', label: '论题' },
      { key: 'speaker', label: '论者', w: '120px' },
      { key: 'answer', label: '正解', w: '70px' },
      { key: 'summary', label: '摘要', dim: true }
    ],
    fields: [
      { key: 'title', label: '论题', required: true, ph: '如：魔力可否逆转天命？' },
      { key: 'speaker', label: '论者', ph: '如：血河真人' },
      { key: 'summary', label: '摘要', wide: true, ph: '一句概括本场论道…' },
      { key: 'optionA', label: '选项 A', wide: true, ph: '选项 A 内容…' },
      { key: 'optionB', label: '选项 B', wide: true, ph: '选项 B 内容…' },
      { key: 'optionC', label: '选项 C', wide: true, ph: '选项 C 内容…' },
      { key: 'answer', label: '正确选项', type: 'select', options: ['A', 'B', 'C'].map(v => ({ v, t: v })) },
      { key: 'analysis', label: '论道解析', type: 'textarea', wide: true, ph: '为何此解为正…' }
    ]
  },
  codex: {
    label: '魔 典 秘 录', short: '典籍', api: { list: codexList, add: codexAdd, update: codexUpdate, del: codexDelete },
    imgDir: 'codex', titleKey: 'title', addLabel: '录 入 新 典 籍',
    kwPlaceholder: '按典籍名 / 卷册 / 摘要检索…',
    columns: [
      { key: 'title', label: '典籍名' },
      { key: 'volume', label: '卷册', w: '110px' },
      { key: 'summary', label: '摘要', dim: true }
    ],
    fields: [
      { key: 'title', label: '典籍名', required: true, ph: '如：七煞真解' },
      { key: 'volume', label: '卷册', ph: '如：卷一 · 引煞篇' },
      { key: 'image', label: '配图', type: 'image' },
      { key: 'summary', label: '摘要', wide: true, ph: '一句话点明此典所载…' }
    ]
  },
  archive: {
    label: '魔 枢 典 册', short: '典册条目', api: { list: archiveList, add: archiveAdd, update: archiveUpdate, del: archiveDelete },
    imgDir: '', titleKey: 'title', addLabel: '录 入 新 条 目',
    kwPlaceholder: '按条目名 / 副题 / 提要检索…',
    columns: [
      { key: 'title', label: '条目名' },
      { key: 'category', label: '分类', w: '110px', map: CAT_MAP },
      { key: 'subtitle', label: '副题', w: '160px' },
      { key: 'sortOrder', label: '排序', w: '70px' },
      { key: 'summary', label: '提要', dim: true }
    ],
    fields: [
      { key: 'category', label: '分类', type: 'select', options: CATS },
      { key: 'title', label: '条目名', required: true, ph: '如：七大境界总纲' },
      { key: 'subtitle', label: '副题', ph: '血脉 / 境界跨度 / 铁律 / 纪年' },
      { key: 'sortOrder', label: '分类内排序', type: 'number', ph: '0' },
      { key: 'summary', label: '一句话提要', wide: true, ph: '一句概括此条目…' },
      { key: 'content', label: '正文', type: 'textarea', wide: true, ph: '典册正文（换行保留）…' }
    ]
  }
}

// ==================== 状态 ====================
const route = useRoute()
const router = useRouter()
const username = getAdminSession().username

const modKey = ref('art')
const cur = computed(() => MODULES[modKey.value])

const kw = ref('')
const cat = ref('')
const list = ref([])
const showForm = ref(false)
const saving = ref(false)
const imgOptions = ref([])

const blank = {}
function rebuildBlank() {
  const b = { id: null }
  for (const f of cur.value.fields) {
    if (f.type === 'select') {
      b[f.key] = f.options[0].v
    } else if (f.type === 'number') {
      b[f.key] = 0
    } else {
      b[f.key] = ''
    }
  }
  blank.data = b
  return b
}
const form = reactive({ id: null })

// 未持令牌则回后台登录页；/admin 缺省进功法阁
onMounted(() => {
  if (!getAdminSession().token) {
    router.replace('/admin/login')
    return
  }
  const m = route.params.module
  if (m && MODULES[m]) modKey.value = m
  switchModule(modKey.value, false)
})

// 模块切换：同步路由 + 重置检索 + 刷新
function switchModule(key, updateRoute = true) {
  if (!MODULES[key]) key = 'art'
  modKey.value = key
  if (updateRoute && route.params.module !== key) {
    router.replace(`/admin/${key}`)
  }
  kw.value = ''
  cat.value = ''
  loadList()
  syncImages()
}

// 路由直接变更（浏览器前进/后退）时跟随
watch(() => route.params.module, (m) => {
  if (m && MODULES[m] && m !== modKey.value) switchModule(m, false)
})

// ==================== 图片同步 ====================
// 拉取后端 static/images/{dir} 实际存在的图片清单；失败时退化为约定列表
const FALLBACK = { art: 12, trial: 12, codex: 12 }
async function syncImages() {
  imgOptions.value = []
  const dir = cur.value.imgDir
  if (!dir) return
  try {
    const res = await imageList(dir)
    if (res.data.code === 200 && Array.isArray(res.data.data)) {
      imgOptions.value = res.data.data
      return
    }
  } catch (e) { /* 静默退化 */ }
  const n = FALLBACK[dir] || 12
  imgOptions.value = Array.from({ length: n }, (_, i) => `/images/${dir}/${dir}-${i + 1}.jpg`)
}

// ==================== 列表 ====================
async function loadList() {
  try {
    const res = await cur.value.api.list(kw.value, cat.value)
    if (res.data.code === 200) {
      list.value = res.data.data
    } else {
      alert(res.data.msg || '读取数据失败')
    }
  } catch (e) {
    alert(e.message)
    if (e.message.includes('令牌')) router.replace('/admin/login')
  }
}

// 表格单元格渲染：分类映射 / 空值占位
function render(row, c) {
  const v = row[c.key]
  if (c.map) return c.map[v] || v || '-'
  return v === null || v === undefined || v === '' ? '-' : v
}

// ==================== 录入 / 修改 / 删除 ====================
function openForm(row) {
  const b = rebuildBlank()
  Object.assign(form, b, row || {})
  showForm.value = true
}

function closeForm() {
  showForm.value = false
}

async function onSave() {
  if (saving.value) return
  saving.value = true
  try {
    const payload = {}
    for (const f of cur.value.fields) payload[f.key] = form[f.key]
    const res = form.id
      ? await cur.value.api.update(form.id, payload)
      : await cur.value.api.add(payload)
    if (res.data.code === 200) {
      closeForm()
      await loadList()
    } else {
      alert(res.data.msg || '封存失败')
    }
  } catch (e) {
    alert(e.message)
  } finally {
    saving.value = false
  }
}

async function onDelete(row) {
  const name = row[cur.value.titleKey]
  if (!confirm(`确定删除「${name}」？此操作不可撤销。`)) return
  try {
    const res = await cur.value.api.del(row.id)
    if (res.data.code === 200) {
      await loadList()
    } else {
      alert(res.data.msg || '删除失败')
    }
  } catch (e) {
    alert(e.message)
  }
}

function onLogout() {
  clearAdminSession()
  router.replace('/admin/login')
}
</script>

<style scoped>
.admin-page {
  min-height: 100vh;
  background:
    radial-gradient(1200px 600px at 80% -10%, rgba(122, 92, 20, 0.18), transparent 60%),
    radial-gradient(900px 500px at 0% 110%, rgba(160, 44, 72, 0.12), transparent 60%),
    #120a1e;
  color: #e8dcc8;
  padding: 24px 32px 48px;
  box-sizing: border-box;
}

/* 顶栏 */
.admin-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(201, 162, 39, 0.25);
}
.admin-brand .mark {
  font-size: 22px;
  letter-spacing: 4px;
  color: #c9a227;
  font-weight: 700;
}
.admin-brand .sub {
  margin-left: 14px;
  font-size: 12px;
  letter-spacing: 3px;
  color: rgba(232, 220, 200, 0.5);
}
.admin-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}
.admin-meta .who {
  font-size: 13px;
  color: rgba(232, 220, 200, 0.75);
}

/* 模块页签 */
.mod-tabs {
  display: flex;
  gap: 10px;
  margin: 18px 0 0;
  flex-wrap: wrap;
}
.mod-tab {
  cursor: pointer;
  border: 1px solid rgba(232, 220, 200, 0.25);
  background: transparent;
  color: rgba(232, 220, 200, 0.7);
  padding: 8px 20px;
  border-radius: 20px;
  font-size: 13px;
  letter-spacing: 3px;
  transition: 0.2s;
}
.mod-tab:hover { border-color: rgba(201, 162, 39, 0.55); color: #c9a227; }
.mod-tab.on {
  background: linear-gradient(135deg, #7a5c14, #c9a227);
  border-color: transparent;
  color: #1a1206;
  font-weight: 700;
}

/* 按钮 */
.gold-btn {
  cursor: pointer;
  border: 1px solid rgba(201, 162, 39, 0.5);
  background: rgba(201, 162, 39, 0.08);
  color: #e8dcc8;
  padding: 8px 18px;
  border-radius: 6px;
  font-size: 13px;
  letter-spacing: 2px;
  transition: 0.2s;
}
.gold-btn:hover { background: rgba(201, 162, 39, 0.2); }
.gold-btn.primary {
  background: linear-gradient(135deg, #7a5c14, #c9a227);
  border-color: transparent;
  color: #1a1206;
  font-weight: 700;
}
.gold-btn:disabled { opacity: 0.55; cursor: not-allowed; }
.ghost-btn {
  cursor: pointer;
  border: 1px solid rgba(232, 220, 200, 0.3);
  background: transparent;
  color: rgba(232, 220, 200, 0.8);
  padding: 6px 14px;
  border-radius: 6px;
  font-size: 12px;
  letter-spacing: 2px;
  text-decoration: none;
  transition: 0.2s;
}
.ghost-btn:hover { border-color: rgba(201, 162, 39, 0.6); color: #c9a227; }
.ghost-btn.warn:hover { border-color: #a02c48; color: #ff6b8a; }

/* 工具栏 */
.toolbar {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin: 22px 0 16px;
}
.search { display: flex; gap: 10px; flex: 1; max-width: 560px; }
.search-input {
  flex: 1;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(232, 220, 200, 0.25);
  border-radius: 6px;
  color: #e8dcc8;
  padding: 8px 14px;
  font-size: 13px;
  outline: none;
}
.search-input:focus { border-color: rgba(201, 162, 39, 0.55); }
.cat-select { flex: 0 0 130px; cursor: pointer; }
.cat-select option { background: #160a24; color: #e8dcc8; }

/* 数据表 */
.table-wrap {
  border: 1px solid rgba(201, 162, 39, 0.2);
  border-radius: 10px;
  overflow: auto;
  background: rgba(255, 255, 255, 0.02);
}
.art-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
  min-width: 860px;
}
.art-table th {
  text-align: left;
  padding: 12px 14px;
  color: #c9a227;
  letter-spacing: 2px;
  font-weight: 600;
  border-bottom: 1px solid rgba(201, 162, 39, 0.3);
  white-space: nowrap;
}
.art-table td {
  padding: 10px 14px;
  border-bottom: 1px solid rgba(232, 220, 200, 0.08);
  vertical-align: middle;
}
.art-table tr:hover td { background: rgba(201, 162, 39, 0.05); }
.strong { color: #fff; font-weight: 600; }
.dim { color: rgba(232, 220, 200, 0.55); }
.thumb {
  display: inline-block;
  width: 44px;
  height: 30px;
  border-radius: 4px;
  background-size: cover;
  background-position: center;
  border: 1px solid rgba(232, 220, 200, 0.2);
}
.thumb.empty {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  color: rgba(232, 220, 200, 0.35);
}
.row-btn {
  cursor: pointer;
  border: 1px solid rgba(201, 162, 39, 0.45);
  background: transparent;
  color: #c9a227;
  border-radius: 4px;
  padding: 4px 10px;
  font-size: 12px;
  margin-right: 6px;
  transition: 0.2s;
}
.row-btn:hover { background: rgba(201, 162, 39, 0.15); }
.row-btn.danger { border-color: rgba(160, 44, 72, 0.6); color: #ff6b8a; }
.row-btn.danger:hover { background: rgba(160, 44, 72, 0.15); }
.empty {
  text-align: center;
  padding: 60px 0;
  letter-spacing: 4px;
  color: rgba(232, 220, 200, 0.4);
}

/* 弹窗 */
.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(10, 5, 18, 0.75);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 50;
}
.modal {
  width: min(720px, 92vw);
  max-height: 88vh;
  overflow: auto;
  background: #1a1028;
  border: 1px solid rgba(201, 162, 39, 0.35);
  border-radius: 12px;
  padding: 26px 30px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.6);
}
.modal-title {
  margin: 0 0 18px;
  font-size: 17px;
  letter-spacing: 4px;
  color: #c9a227;
}
.grid2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4px 18px;
}
.grid2 .wide { grid-column: span 2; }
.modal .field { margin-bottom: 14px; }
.modal .field label {
  display: block;
  font-size: 12px;
  letter-spacing: 2px;
  color: rgba(232, 220, 200, 0.6);
  margin-bottom: 6px;
}
.modal .field input,
.modal .field select,
.modal .field textarea {
  width: 100%;
  box-sizing: border-box;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(232, 220, 200, 0.25);
  border-radius: 6px;
  color: #e8dcc8;
  padding: 9px 12px;
  font-size: 13px;
  outline: none;
  font-family: inherit;
}
.modal .field input:focus,
.modal .field select:focus,
.modal .field textarea:focus {
  border-color: rgba(201, 162, 39, 0.55);
}
.modal .field select option { background: #160a24; color: #e8dcc8; }
.img-hint {
  margin: 4px 0 0;
  font-size: 12px;
  color: rgba(232, 220, 200, 0.45);
  letter-spacing: 1px;
}
.img-hint code {
  color: rgba(201, 162, 39, 0.8);
  background: rgba(201, 162, 39, 0.08);
  padding: 1px 6px;
  border-radius: 4px;
}
.modal-foot {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 14px;
}
</style>
