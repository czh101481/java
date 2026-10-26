<template>
  <!-- 后台管理系统 · 功法阁数据管理 -->
  <div class="admin-page">
    <!-- 顶栏 -->
    <header class="admin-nav">
      <div class="admin-brand">
        <span class="mark">后台管理系统</span>
        <span class="sub">功 法 阁 · 数 据 管 理</span>
      </div>
      <div class="admin-meta">
        <span class="who">执印人 · {{ username || 'admin' }}</span>
        <router-link class="ghost-btn" to="/home">前 往 首 页</router-link>
        <button class="ghost-btn warn" type="button" @click="onLogout">退 出 后 台</button>
      </div>
    </header>

    <!-- 工具栏 -->
    <section class="toolbar">
      <form class="search" @submit.prevent="loadList">
        <input v-model="kw" type="text" class="search-input" placeholder="按功法名 / 简介检索…" />
        <button type="submit" class="gold-btn">检 索</button>
      </form>
      <button type="button" class="gold-btn primary" @click="openForm(null)">+ 录 入 新 功 法</button>
    </section>

    <!-- 数据表 -->
    <section class="table-wrap">
      <table class="art-table" v-if="list.length">
        <thead>
          <tr>
            <th style="width: 64px">配图</th>
            <th style="width: 64px">ID</th>
            <th>功法名</th>
            <th style="width: 90px">类别</th>
            <th style="width: 70px">品阶</th>
            <th style="width: 90px">所需境界</th>
            <th>简介</th>
            <th style="width: 150px">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="a in list" :key="a.id">
            <td>
              <span
                class="thumb"
                v-if="a.image"
                :style="{ backgroundImage: `url(${imgUrl(a.image)})` }"
              ></span>
              <span class="thumb empty" v-else>无</span>
            </td>
            <td>{{ a.id }}</td>
            <td class="strong">{{ a.name }}</td>
            <td>{{ a.category }}</td>
            <td>{{ a.grade }}</td>
            <td>{{ a.realmReq }}</td>
            <td class="dim">{{ a.summary }}</td>
            <td>
              <button type="button" class="row-btn" @click="openForm(a)">修 改</button>
              <button type="button" class="row-btn danger" @click="onDelete(a)">除 名</button>
            </td>
          </tr>
        </tbody>
      </table>
      <div class="empty" v-else>功 法 阁 空 空 如 也 。</div>
    </section>

    <!-- 录入 / 修改 弹窗 -->
    <div class="modal-mask" v-if="showForm" @click.self="closeForm">
      <div class="modal">
        <h3 class="modal-title">{{ form.id ? '修 改 功 法' : '录 入 新 功 法' }}</h3>
        <form @submit.prevent="onSave">
          <div class="grid2">
            <div class="field">
              <label>功法名 *</label>
              <input v-model.trim="form.name" type="text" required placeholder="如：血煞戮天诀" />
            </div>
            <div class="field">
              <label>类别</label>
              <select v-model="form.category">
                <option v-for="c in CATEGORIES" :key="c" :value="c">{{ c }}</option>
              </select>
            </div>
            <div class="field">
              <label>品阶</label>
              <select v-model="form.grade">
                <option v-for="g in GRADES" :key="g" :value="g">{{ g }}</option>
              </select>
            </div>
            <div class="field">
              <label>所需境界</label>
              <select v-model="form.realmReq">
                <option v-for="r in REALMS" :key="r" :value="r">{{ r }}</option>
              </select>
            </div>
            <div class="field wide">
              <label>配图</label>
              <select v-model="form.image">
                <option value="">不配图</option>
                <option v-for="img in IMAGES" :key="img" :value="img">{{ img }}</option>
              </select>
            </div>
            <div class="field wide">
              <label>简介</label>
              <input v-model.trim="form.summary" type="text" maxlength="200" placeholder="一句话点明此功法之要义…" />
            </div>
            <div class="field wide">
              <label>正文</label>
              <textarea v-model="form.content" rows="5" placeholder="功法正文…"></textarea>
            </div>
          </div>
          <div class="modal-foot">
            <button type="button" class="gold-btn" @click="closeForm">取 消</button>
            <button type="submit" class="gold-btn primary" :disabled="saving">
              {{ saving ? '封 存 中 …' : '封 存 入 阁' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { imgUrl } from '../api/home'
import {
  artAdd, artDelete, artList, artUpdate,
  clearAdminSession, getAdminSession
} from '../api/admin'

const CATEGORIES = ['攻伐', '护体', '幻术', '炼魂', '御兽']
const GRADES = ['黄', '玄', '地', '天', '仙']
const REALMS = ['引煞', '凝煞', '魔卒', '魔尉', '魔将', '血煞', '魔祖']
const IMAGES = Array.from({ length: 12 }, (_, i) => `/images/art/art-${i + 1}.jpg`)

const router = useRouter()
const kw = ref('')
const list = ref([])
const showForm = ref(false)
const saving = ref(false)
const username = getAdminSession().username

const blank = {
  id: null, name: '', category: '攻伐', grade: '黄',
  realmReq: '引煞', summary: '', image: '', content: ''
}
const form = reactive({ ...blank })

// 未持令牌则回后台登录页
onMounted(() => {
  if (!getAdminSession().token) {
    router.replace('/admin/login')
    return
  }
  loadList()
})

async function loadList() {
  try {
    const res = await artList(kw.value)
    if (res.data.code === 200) {
      list.value = res.data.data
    } else {
      alert(res.data.msg || '读取功法阁失败')
    }
  } catch (e) {
    alert(e.message)
    if (e.message.includes('令牌')) router.replace('/admin/login')
  }
}

function openForm(art) {
  Object.assign(form, blank, art || {})
  showForm.value = true
}

function closeForm() {
  showForm.value = false
}

async function onSave() {
  if (saving.value) return
  saving.value = true
  try {
    const payload = {
      name: form.name, category: form.category, grade: form.grade,
      realmReq: form.realmReq, summary: form.summary,
      image: form.image, content: form.content
    }
    const res = form.id
      ? await artUpdate(form.id, payload)
      : await artAdd(payload)
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

async function onDelete(art) {
  if (!confirm(`确定将「${art.name}」从功法阁除名？此操作不可撤销。`)) return
  try {
    const res = await artDelete(art.id)
    if (res.data.code === 200) {
      await loadList()
    } else {
      alert(res.data.msg || '除名失败')
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
.search { display: flex; gap: 10px; flex: 1; max-width: 480px; }
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
.modal-foot {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 10px;
}
</style>
