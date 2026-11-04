<template>
  <!-- 传音 · 魔友 -->
  <div class="social">
    <!-- 左侧栏：好友 / 请求 / 添加 -->
    <aside class="side">
      <div class="side-head">
        <span class="side-title">传 音 · 魔 友</span>
      </div>

      <div class="tabs">
        <button class="tab" :class="{ on: activeTab === 'friends' }" @click="activeTab = 'friends'">道 友</button>
        <button class="tab" :class="{ on: activeTab === 'requests' }" @click="activeTab = 'requests'">
          请 求<span v-if="requests.length" class="tab-badge">{{ requests.length }}</span>
        </button>
        <button class="tab" :class="{ on: activeTab === 'add' }" @click="activeTab = 'add'">添 加</button>
      </div>

      <div class="side-body">
        <!-- 道友列表 -->
        <div v-if="activeTab === 'friends'" class="friend-list">
          <div v-if="!friends.length" class="side-empty">尚 无 道 友 。 前 往 「 添 加 」 结 交 同 道 。</div>
          <div
            v-for="f in friends"
            :key="f.userId"
            class="friend-item"
            :class="{ active: activeFriend && activeFriend.userId === f.userId }"
            @click="selectFriend(f)"
          >
            <span class="fi-avatar" :style="av(f).style">{{ av(f).char }}</span>
            <div class="fi-main">
              <div class="fi-top">
                <span class="fi-name">{{ f.name }}</span>
                <span v-if="f.unread" class="fi-badge">{{ f.unread }}</span>
              </div>
              <div class="fi-sub">{{ f.realm }} · {{ f.cultivation }}境</div>
              <div class="fi-last">{{ f.lastMessage || '—' }}</div>
            </div>
            <span class="fi-time" v-if="f.lastTime">{{ fmtTime(f.lastTime) }}</span>
          </div>
        </div>

        <!-- 待处理请求 -->
        <div v-else-if="activeTab === 'requests'" class="request-list">
          <div v-if="!requests.length" class="side-empty">暂 无 呈 予 你 的 道 友 契 约 。</div>
          <div v-for="r in requests" :key="r.applyId" class="request-item">
            <span class="ri-avatar" :style="av(r).style">{{ av(r).char }}</span>
            <div class="ri-main">
              <div class="ri-name">{{ r.name }} <em>{{ r.realm }} · {{ r.cultivation }}境</em></div>
              <div class="ri-msg">{{ r.message || '愿结同道之好。' }}</div>
            </div>
            <div class="ri-acts">
              <button class="ri-ok" @click="doHandle(r.applyId, 'accept')">应 允</button>
              <button class="ri-no" @click="doHandle(r.applyId, 'reject')">婉 拒</button>
            </div>
          </div>
        </div>

        <!-- 添加：搜索修者 -->
        <div v-else class="add-box">
          <form class="add-search" @submit.prevent="doSearch">
            <input v-model="searchKw" type="text" placeholder="以道号叩问魔域……" />
            <button type="submit" :disabled="searching">搜 寻</button>
          </form>
          <div v-if="!searchResults.length" class="side-empty">输 入 道 号 ， 寻 觅 可 结 交 的 修 者 。</div>
          <div v-for="u in searchResults" :key="u.id" class="add-item">
            <span class="ai-avatar" :style="av(u).style">{{ av(u).char }}</span>
            <div class="ai-main">
              <div class="ai-name">{{ u.name }}</div>
              <div class="ai-sub">{{ u.realm }} · {{ u.cultivation }}境</div>
            </div>
            <button class="ai-add" :disabled="applyingId === u.id" @click="doApply(u)">
              {{ applyingId === u.id ? '递 交 中 …' : '加 为 道 友' }}
            </button>
          </div>
        </div>
      </div>
    </aside>

    <!-- 右侧：会话 -->
    <main class="main">
      <template v-if="activeFriend">
        <header class="conv-head">
          <span class="ch-avatar" :style="av(activeFriend).style">{{ av(activeFriend).char }}</span>
          <div class="ch-who">
            <div class="ch-name">{{ activeFriend.name }}</div>
            <div class="ch-sub">{{ activeFriend.realm }} · {{ activeFriend.cultivation }}境</div>
          </div>
          <button class="ch-remove" @click="doRemove">解 除 道 友</button>
        </header>

        <div class="conv-body" ref="msgBox">
          <div
            v-for="m in messages"
            :key="m.id"
            class="msg-row"
            :class="m.mine ? 'mine' : 'theirs'"
          >
            <div class="bubble" :class="{ insight: m.msgType === 1 }">
              <template v-if="m.msgType === 1">
                <div class="ins-tag">修 炼 心 得 · {{ refTypeName(m.refType) }}</div>
                <div class="ins-title">{{ m.refTitle }}</div>
                <div class="ins-summary">{{ m.refSummary }}</div>
                <button class="ins-open" @click="openRef(m)">查 阅 此 篇 ⟶</button>
              </template>
              <template v-else>
                <div class="bubble-text">{{ m.content }}</div>
              </template>
              <div class="bubble-time">{{ fmtTime(m.createdAt) }}</div>
            </div>
          </div>
        </div>

        <footer class="conv-foot">
          <button class="foot-share" type="button" @click="openInsight">分 享 修 炼 心 得</button>
          <textarea
            v-model="draft"
            class="foot-input"
            placeholder="传 音 问 道 ……（Enter 发送，Shift+Enter 换行）"
            @keydown.enter.exact.prevent="sendText"
          ></textarea>
          <button class="foot-send" type="button" :disabled="sending || !draft.trim()" @click="sendText">
            {{ sending ? '传 音 中 …' : '传 音' }}
          </button>
        </footer>
      </template>

      <div v-else class="conv-empty">
        <div class="ce-rune">✶</div>
        <div class="ce-title">择 一 道 友 ， 传 音 问 道</div>
        <div class="ce-sub">于 左 栏 结 交 同 道 ， 或 应 允 呈 予 你 的 契 约</div>
      </div>
    </main>

    <!-- 分享修炼心得：内容选择器 -->
    <div class="insight-mask" v-if="insightVisible" @click.self="insightVisible = false">
      <div class="insight-modal">
        <button class="insight-close" @click="insightVisible = false">✕</button>
        <h2 class="insight-title">分 享 修 炼 心 得</h2>
        <form class="insight-search" @submit.prevent="searchInsight">
          <input v-model="insightKw" type="text" placeholder="搜功法 / 秘录 / 典籍……" />
          <button type="submit" :disabled="searchingInsight">搜 寻</button>
        </form>
        <div class="insight-list">
          <div v-if="!insightResults.length" class="insight-empty">
            {{ insightKw ? '魔 渊 深 处 未 寻 得 相 关 记 载 。' : '输 入 关 键 字 ， 寻 一 篇 心 得 分 享 给 道 友 。' }}
          </div>
          <div
            v-for="(it, i) in insightResults"
            :key="it.type + it.id + i"
            class="insight-item"
            @click="sendInsight(it)"
          >
            <span class="ii-tag">{{ refTypeName(it.type) }}</span>
            <div class="ii-main">
              <div class="ii-title">{{ it.title }}</div>
              <div class="ii-summary">{{ it.summary }}</div>
            </div>
            <span class="ii-send">分 享 ⟶</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { getSession } from '../api/auth'
import {
  friendList, friendRequests, applyFriend, handleFriend, removeFriend,
  searchUsers, messageHistory, sendMessage, messageUnread
} from '../api/social'
import { homeSearch } from '../api/home'
import { renderAvatar } from '../config/avatars'

const router = useRouter()
const session = getSession()

// 头像渲染：统一走预设配置，无预设回退道号首字
const av = (o) => renderAvatar(o && o.avatar, o && o.name)

const activeTab = ref('friends')
const friends = ref([])
const requests = ref([])
const searchKw = ref('')
const searchResults = ref([])
const searching = ref(false)

const activeFriend = ref(null)
const messages = ref([])
const draft = ref('')
const sending = ref(false)
const msgBox = ref(null)

const applyingId = ref(null)

// 分享修炼心得弹窗
const insightVisible = ref(false)
const insightKw = ref('')
const insightResults = ref([])
const searchingInsight = ref(false)

const requestCount = computed(() => requests.value.length)

const TYPE_LABEL = { art: '功法阁', codex: '魔典秘录', archive: '魔枢典册' }
function refTypeName(t) { return TYPE_LABEL[t] || t || '典籍' }

function fmtTime(t) {
  if (!t) return ''
  const d = new Date(t)
  if (isNaN(d.getTime())) return ''
  const hh = String(d.getHours()).padStart(2, '0')
  const mm = String(d.getMinutes()).padStart(2, '0')
  return `${hh}:${mm}`
}

function scrollToBottom() {
  nextTick(() => {
    const el = msgBox.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

// ============ 数据拉取 ============
async function refreshFriends() {
  try {
    const res = await friendList()
    if (res.data.code === 200) friends.value = res.data.data || []
  } catch (e) { /* 静默 */ }
}

async function refreshRequests() {
  try {
    const res = await friendRequests()
    if (res.data.code === 200) requests.value = res.data.data || []
  } catch (e) { /* 静默 */ }
}

async function refreshUnread() {
  try {
    const res = await messageUnread()
    if (res.data.code === 200) {
      // 把各好友未读数同步进 friends 列表（与接口返回的去重，这里以列表自带为准）
      const per = res.data.data?.list || []
      const map = {}
      for (const p of per) map[p.friendId] = p.count
      for (const f of friends.value) {
        // 列表接口已带未读；此处仅兜底，不覆盖
        if (f.unread == null) f.unread = map[f.userId] || 0
      }
    }
  } catch (e) { /* 静默 */ }
}

async function refreshConversation(appendOnly) {
  if (!activeFriend.value) return
  const lastId = appendOnly && messages.value.length
    ? messages.value[messages.value.length - 1].id : 0
  try {
    const res = await messageHistory(activeFriend.value.userId, lastId)
    if (res.data.code === 200) {
      const list = res.data.data || []
      if (appendOnly) {
        for (const m of list) messages.value.push(m)
      } else {
        messages.value = list
      }
      scrollToBottom()
    }
  } catch (e) { /* 静默 */ }
}

async function refreshAll() {
  await refreshFriends()
  await refreshRequests()
}

async function poll() {
  await refreshAll()
  if (activeFriend.value) await refreshConversation(true)
}

// ============ 交互 ============
async function selectFriend(f) {
  activeFriend.value = f
  activeTab.value = 'friends'
  messages.value = []
  await refreshConversation(false)
  await refreshFriends()
}

async function doSearch() {
  searching.value = true
  try {
    const res = await searchUsers(searchKw.value.trim())
    if (res.data.code === 200) searchResults.value = res.data.data || []
  } catch (e) { /* 静默 */ }
  finally { searching.value = false }
}

async function doApply(u) {
  applyingId.value = u.id
  try {
    const res = await applyFriend(u.name, '愿结同道之好，共参魔道。')
    if (res.data.code === 200) {
      alert(res.data.data || '已递出契约。')
      await refreshRequests()
      await refreshFriends()
    } else {
      alert(res.data.msg || '契约无应。')
    }
  } catch (e) { alert(e.message) }
  finally { applyingId.value = null }
}

async function doHandle(applyId, action) {
  try {
    const res = await handleFriend(applyId, action)
    if (res.data.code === 200) {
      alert(res.data.data || '已处理。')
      await refreshRequests()
      await refreshFriends()
    } else {
      alert(res.data.msg || '处理无应。')
    }
  } catch (e) { alert(e.message) }
}

async function doRemove() {
  if (!activeFriend.value) return
  if (!confirm(`确定与「${activeFriend.value.name}」斩断道友契约吗？聊天记录仍会留存。`)) return
  try {
    const res = await removeFriend(activeFriend.value.userId)
    if (res.data.code === 200) {
      activeFriend.value = null
      messages.value = []
      await refreshFriends()
    } else {
      alert(res.data.msg || '无应。')
    }
  } catch (e) { alert(e.message) }
}

async function sendText() {
  const text = draft.value.trim()
  if (!text || !activeFriend.value || sending.value) return
  sending.value = true
  try {
    const res = await sendMessage({ toId: activeFriend.value.userId, content: text, msgType: 0 })
    if (res.data.code === 200) {
      messages.value.push(res.data.data)
      draft.value = ''
      scrollToBottom()
    } else {
      alert(res.data.msg || '传音无应。')
    }
  } catch (e) { alert(e.message) }
  finally { sending.value = false }
}

function openInsight() {
  if (!activeFriend.value) return
  insightVisible.value = true
  insightKw.value = ''
  insightResults.value = []
}

async function searchInsight() {
  searchingInsight.value = true
  try {
    const res = await homeSearch(insightKw.value.trim())
    if (res.data.code === 200) {
      const groups = res.data.data?.groups || []
      const flat = []
      for (const g of groups) {
        for (const it of (g.items || [])) {
          flat.push({ type: it.type, id: it.id, title: it.title, summary: it.summary })
        }
      }
      insightResults.value = flat
    }
  } catch (e) { /* 静默 */ }
  finally { searchingInsight.value = false }
}

async function sendInsight(it) {
  if (!activeFriend.value || sending.value) return
  sending.value = true
  try {
    const res = await sendMessage({
      toId: activeFriend.value.userId,
      msgType: 1,
      refType: it.type,
      refId: it.id,
      refTitle: it.title,
      refSummary: it.summary,
      content: it.title
    })
    if (res.data.code === 200) {
      messages.value.push(res.data.data)
      insightVisible.value = false
      insightKw.value = ''
      insightResults.value = []
      scrollToBottom()
    } else {
      alert(res.data.msg || '传音无应。')
    }
  } catch (e) { alert(e.message) }
  finally { sending.value = false }
}

function openRef(m) {
  if (m.refType === 'archive') router.push('/archive')
  else router.push(`/detail/${m.refType}/${m.refId}`)
}

// ============ 生命周期 ============
let pollTimer = null
onMounted(async () => {
  if (!session.token) {
    router.replace('/login')
    return
  }
  await refreshFriends()
  await refreshRequests()
  pollTimer = setInterval(poll, 4000)
})
onBeforeUnmount(() => {
  if (pollTimer) clearInterval(pollTimer)
})
</script>

<style scoped>
.social {
  position: fixed;
  inset: 0;
  z-index: 2;
  display: flex;
  background: radial-gradient(ellipse at 50% 22%, #1c0510 0%, #0b0207 55%, #050104 100%);
}

/* ===== 左侧栏 ===== */
.side {
  width: 320px;
  flex: none;
  display: flex;
  flex-direction: column;
  border-right: 1px solid rgba(176, 102, 255, 0.22);
  background: linear-gradient(180deg, rgba(22, 10, 36, 0.72), rgba(10, 4, 16, 0.82));
  backdrop-filter: blur(8px);
}
.side-head {
  padding: 20px 18px 12px;
  border-bottom: 1px solid rgba(176, 102, 255, 0.15);
}
.side-title {
  font-size: 20px;
  letter-spacing: 6px;
  color: var(--bone);
  text-shadow: 0 0 12px var(--crimson-glow);
}
.tabs {
  display: flex;
  padding: 12px 12px 0;
  gap: 8px;
}
.tab {
  position: relative;
  flex: 1;
  padding: 10px 0;
  border: 1px solid rgba(176, 102, 255, 0.2);
  border-radius: 10px 10px 0 0;
  background: transparent;
  color: rgba(232, 220, 200, 0.6);
  font-family: inherit;
  font-size: 14px;
  letter-spacing: 3px;
  cursor: pointer;
  transition: all 0.25s ease;
}
.tab:hover { color: var(--bone); }
.tab.on {
  color: #fff;
  border-color: var(--crimson-glow);
  background: rgba(255, 59, 107, 0.12);
  box-shadow: 0 0 16px rgba(255, 59, 107, 0.25);
}
.tab-badge {
  display: inline-block;
  min-width: 18px;
  padding: 0 5px;
  margin-left: 4px;
  border-radius: 10px;
  background: var(--crimson-glow);
  color: #1a030a;
  font-size: 11px;
  line-height: 18px;
  text-align: center;
}
.side-body { flex: 1; overflow-y: auto; padding: 10px; }

.side-empty {
  margin-top: 40px;
  text-align: center;
  font-size: 13px;
  letter-spacing: 2px;
  color: rgba(232, 220, 200, 0.4);
  line-height: 2;
}

/* 道友项 */
.friend-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.2s ease;
}
.friend-item:hover { background: rgba(176, 102, 255, 0.1); }
.friend-item.active {
  background: rgba(255, 59, 107, 0.14);
  box-shadow: inset 0 0 0 1px rgba(255, 59, 107, 0.4);
}
.fi-avatar, .ri-avatar, .ai-avatar, .ch-avatar {
  flex: none;
  width: 40px; height: 40px;
  display: flex; align-items: center; justify-content: center;
  border-radius: 50%;
  font-size: 18px;
  color: #fff;
  background: radial-gradient(circle at 35% 30%, var(--violet-glow), #1a030a 80%);
  box-shadow: 0 0 12px rgba(176, 102, 255, 0.5);
}
.fi-main { flex: 1; min-width: 0; }
.fi-top { display: flex; justify-content: space-between; align-items: center; }
.fi-name { color: var(--bone); font-size: 15px; letter-spacing: 1px; }
.fi-badge {
  min-width: 18px; padding: 0 5px; border-radius: 10px;
  background: var(--crimson-glow); color: #1a030a;
  font-size: 11px; line-height: 18px; text-align: center;
}
.fi-sub { font-size: 11px; color: rgba(217, 176, 102, 0.7); margin-top: 2px; }
.fi-last {
  font-size: 12px; color: rgba(232, 220, 200, 0.45);
  margin-top: 3px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.fi-time { flex: none; font-size: 11px; color: rgba(232, 220, 200, 0.35); }

/* 请求项 */
.request-item {
  display: flex; align-items: center; gap: 10px;
  padding: 10px; border-radius: 10px;
  border: 1px solid rgba(176, 102, 255, 0.15);
  margin-bottom: 8px;
}
.ri-main { flex: 1; min-width: 0; }
.ri-name { color: var(--bone); font-size: 14px; }
.ri-name em { font-size: 11px; color: rgba(217, 176, 102, 0.7); font-style: normal; }
.ri-msg { font-size: 12px; color: rgba(232, 220, 200, 0.5); margin-top: 3px;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.ri-acts { display: flex; flex-direction: column; gap: 4px; }
.ri-ok, .ri-no {
  padding: 4px 10px; border-radius: 8px; font-family: inherit; font-size: 12px;
  cursor: pointer; border: 1px solid transparent;
}
.ri-ok { background: linear-gradient(135deg, var(--crimson), var(--violet)); color: #fff; }
.ri-no { background: transparent; border-color: rgba(255,255,255,0.2); color: rgba(232,220,200,0.6); }

/* 添加搜索 */
.add-box { display: flex; flex-direction: column; gap: 10px; }
.add-search, .insight-search { display: flex; gap: 8px; }
.add-search input, .insight-search input {
  flex: 1; padding: 10px 12px; border-radius: 10px;
  background: rgba(8, 3, 14, 0.6);
  border: 1px solid rgba(176, 102, 255, 0.25);
  color: var(--bone); font-family: inherit; font-size: 14px; outline: none;
}
.add-search input:focus, .insight-search input:focus {
  border-color: var(--crimson-glow); box-shadow: 0 0 14px rgba(255,59,107,0.3);
}
.add-search button, .insight-search button {
  padding: 0 16px; border: none; border-radius: 10px;
  background: linear-gradient(135deg, var(--crimson), var(--violet));
  color: #fff; font-family: inherit; font-size: 13px; letter-spacing: 2px; cursor: pointer;
}
.add-item, .insight-item {
  display: flex; align-items: center; gap: 10px;
  padding: 10px; border-radius: 10px;
  border: 1px solid rgba(176, 102, 255, 0.15);
}
.ai-main { flex: 1; min-width: 0; }
.ai-name { color: var(--bone); font-size: 14px; }
.ai-sub { font-size: 11px; color: rgba(217, 176, 102, 0.7); margin-top: 2px; }
.ai-add {
  padding: 6px 12px; border-radius: 8px; font-family: inherit; font-size: 12px;
  cursor: pointer; border: 1px solid var(--crimson-glow);
  background: transparent; color: var(--crimson-glow);
}
.ai-add:disabled { opacity: 0.5; cursor: default; }

/* ===== 右侧会话 ===== */
.main { flex: 1; display: flex; flex-direction: column; min-width: 0; }
.conv-head {
  display: flex; align-items: center; gap: 12px;
  padding: 16px 22px; border-bottom: 1px solid rgba(176, 102, 255, 0.18);
}
.ch-who { flex: 1; }
.ch-name { color: var(--bone); font-size: 17px; letter-spacing: 2px; }
.ch-sub { font-size: 12px; color: rgba(217, 176, 102, 0.7); margin-top: 2px; }
.ch-remove {
  padding: 6px 14px; border-radius: 8px; font-family: inherit; font-size: 12px;
  cursor: pointer; border: 1px solid rgba(255, 255, 255, 0.2);
  background: transparent; color: rgba(232, 220, 200, 0.6);
}
.ch-remove:hover { border-color: var(--crimson-glow); color: var(--crimson-glow); }

.conv-body {
  flex: 1; overflow-y: auto; padding: 22px;
  display: flex; flex-direction: column; gap: 14px;
}
.msg-row { display: flex; }
.msg-row.mine { justify-content: flex-end; }
.msg-row.theirs { justify-content: flex-start; }
.bubble {
  max-width: 66%; padding: 10px 14px; border-radius: 14px;
  font-size: 14px; line-height: 1.6; position: relative;
  border: 1px solid transparent;
}
.msg-row.mine .bubble {
  background: linear-gradient(135deg, rgba(106, 44, 156, 0.85), rgba(179, 24, 58, 0.7));
  border-bottom-right-radius: 4px;
  box-shadow: 0 0 18px rgba(176, 102, 255, 0.3);
}
.msg-row.theirs .bubble {
  background: rgba(20, 8, 18, 0.85);
  border: 1px solid rgba(255, 59, 107, 0.3);
  border-bottom-left-radius: 4px;
}
.bubble-text { color: var(--bone); white-space: pre-wrap; word-break: break-word; }
.bubble-time {
  margin-top: 4px; font-size: 10px; text-align: right;
  color: rgba(232, 220, 200, 0.4);
}
.bubble.insight {
  min-width: 240px;
  background: linear-gradient(160deg, rgba(36, 14, 56, 0.92), rgba(10, 4, 16, 0.92));
  border: 1px solid var(--gold);
  box-shadow: 0 0 18px rgba(217, 176, 102, 0.25);
}
.ins-tag { font-size: 11px; letter-spacing: 2px; color: var(--gold); margin-bottom: 6px; }
.ins-title { font-size: 15px; color: var(--bone); margin-bottom: 4px; }
.ins-summary {
  font-size: 12px; color: rgba(232, 220, 200, 0.6); line-height: 1.5;
  max-height: 48px; overflow: hidden;
}
.ins-open {
  margin-top: 8px; padding: 4px 12px; border-radius: 8px; cursor: pointer;
  font-family: inherit; font-size: 12px; letter-spacing: 1px;
  border: 1px solid var(--gold); background: transparent; color: var(--gold);
}
.ins-open:hover { background: rgba(217, 176, 102, 0.12); }

.conv-foot {
  display: flex; align-items: flex-end; gap: 10px;
  padding: 14px 22px; border-top: 1px solid rgba(176, 102, 255, 0.18);
}
.foot-share {
  flex: none; padding: 10px 14px; border-radius: 10px; cursor: pointer;
  font-family: inherit; font-size: 13px; letter-spacing: 1px;
  border: 1px solid var(--gold); background: transparent; color: var(--gold);
}
.foot-share:hover { background: rgba(217, 176, 102, 0.12); }
.foot-input {
  flex: 1; resize: none; height: 46px; max-height: 120px;
  padding: 12px 14px; border-radius: 10px;
  background: rgba(8, 3, 14, 0.6);
  border: 1px solid rgba(176, 102, 255, 0.25);
  color: var(--bone); font-family: inherit; font-size: 14px; outline: none;
  line-height: 1.4;
}
.foot-input:focus { border-color: var(--crimson-glow); box-shadow: 0 0 14px rgba(255,59,107,0.3); }
.foot-send {
  flex: none; padding: 0 22px; height: 46px; border: none; border-radius: 10px;
  background: linear-gradient(135deg, var(--crimson), var(--violet));
  color: #fff; font-family: inherit; font-size: 15px; letter-spacing: 4px; cursor: pointer;
}
.foot-send:disabled { opacity: 0.5; cursor: default; }

.conv-empty {
  flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center;
  color: rgba(232, 220, 200, 0.4);
}
.ce-rune { font-size: 56px; color: var(--violet-glow); text-shadow: 0 0 20px var(--violet-glow); }
.ce-title { font-size: 20px; letter-spacing: 6px; color: var(--bone); margin-top: 16px; }
.ce-sub { font-size: 13px; letter-spacing: 2px; margin-top: 10px; }

/* ===== 分享心得弹窗 ===== */
.insight-mask {
  position: fixed; inset: 0; z-index: 20;
  display: flex; align-items: center; justify-content: center;
  background: rgba(5, 2, 8, 0.85);
}
.insight-modal {
  position: relative; width: 520px; max-width: 92vw; max-height: 80vh;
  display: flex; flex-direction: column;
  padding: 24px; border-radius: 16px;
  background: linear-gradient(160deg, rgba(36, 14, 56, 0.95), rgba(10, 4, 16, 0.96));
  border: 1px solid rgba(176, 102, 255, 0.3);
  box-shadow: 0 0 50px rgba(176, 102, 255, 0.3);
}
.insight-close {
  position: absolute; top: 14px; right: 16px;
  width: 28px; height: 28px; border-radius: 50%;
  border: 1px solid rgba(255,255,255,0.2); background: transparent;
  color: rgba(232,220,200,0.7); cursor: pointer; font-size: 14px;
}
.insight-title {
  font-size: 18px; letter-spacing: 4px; color: var(--gold); margin-bottom: 16px;
  text-shadow: 0 0 12px rgba(217,176,102,0.4);
}
.insight-list { flex: 1; overflow-y: auto; margin-top: 14px; display: flex; flex-direction: column; gap: 8px; }
.insight-empty {
  text-align: center; font-size: 13px; letter-spacing: 2px;
  color: rgba(232, 220, 200, 0.4); margin-top: 30px; line-height: 2;
}
.insight-item {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 12px; border-radius: 10px; cursor: pointer;
  border: 1px solid rgba(176, 102, 255, 0.18);
  transition: all 0.2s ease;
}
.insight-item:hover {
  border-color: var(--gold); background: rgba(217, 176, 102, 0.08);
}
.ii-tag {
  flex: none; padding: 3px 8px; border-radius: 6px; font-size: 11px;
  color: var(--gold); border: 1px solid rgba(217,176,102,0.4);
}
.ii-main { flex: 1; min-width: 0; }
.ii-title { color: var(--bone); font-size: 14px; }
.ii-summary {
  font-size: 12px; color: rgba(232, 220, 200, 0.5); margin-top: 3px;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.ii-send {
  flex: none; font-size: 12px; color: var(--crimson-glow); letter-spacing: 1px;
}
</style>
