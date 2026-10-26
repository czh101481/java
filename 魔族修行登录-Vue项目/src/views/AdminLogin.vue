<template>
  <!-- 后台管理系统 · 管理员登录（仅登录，无注册） -->
  <div class="portal admin-portal">
    <div class="crest">
      <div class="mark admin-mark">后台管理系统</div>
      <div class="sub">执 掌 魔 枢 · 肃 正 功 法</div>
      <div class="divider"></div>
    </div>

    <form @submit.prevent="onEnter" autocomplete="off">
      <div class="field">
        <label for="admin-name">管 理 员 账 号</label>
        <span class="icon">☥</span>
        <input id="admin-name" v-model="form.username" type="text" placeholder="以印信叩响枢机…" required />
      </div>
      <div class="field">
        <label for="admin-key">管 理 密 令</label>
        <span class="icon">⚸</span>
        <input id="admin-key" v-model="form.password" type="password" placeholder="以印血封印枢机…" required />
      </div>
      <button class="enter" type="submit" :disabled="loading">
        {{ loading ? '验 令 中 …' : '执 印 入 阁' }}
      </button>
    </form>

    <div class="foot">
      <router-link to="/home">← 返 回 首 页</router-link>
    </div>
  </div>

  <!-- 入阁仪式特效 -->
  <div class="ritual" :class="{ show: ritual }">
    <div class="rune">枢 机 已 启 · 恭 迎 执 印 人</div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { adminLogin, saveAdminSession } from '../api/admin'

const form = reactive({ username: '', password: '' })
const ritual = ref(false)
const loading = ref(false)
const router = useRouter()

async function onEnter() {
  if (loading.value) return
  loading.value = true
  try {
    const res = await adminLogin(form.username, form.password)
    if (res.data.code === 200) {
      saveAdminSession(res.data.data.token, res.data.data.username)
      ritual.value = true
      setTimeout(() => {
        ritual.value = false
        router.push('/admin')
      }, 1500)
    } else {
      alert(res.data.msg || '验令失败')
    }
  } catch (e) {
    alert(e.message)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.admin-portal .admin-mark {
  color: #c9a227;
}
.admin-portal .sub {
  color: rgba(201, 162, 39, 0.75);
}
.admin-portal .field input:focus {
  border-color: rgba(201, 162, 39, 0.55);
  box-shadow: 0 0 12px rgba(201, 162, 39, 0.25);
}
.admin-portal .enter {
  background: linear-gradient(135deg, #7a5c14, #c9a227);
}
.admin-portal .foot a {
  color: rgba(201, 162, 39, 0.8);
}
</style>
