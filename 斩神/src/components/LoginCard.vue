<script setup>
import { reactive, ref, onMounted } from 'vue'

const state = reactive({
  user: '',
  pass: '',
  remember: false,
  msg: '',
  err: false,
  ok: false,
  shake: false,
  success: false
})
const btn = ref(null)

function load() {
  try {
    const u = localStorage.getItem('zhan_shen_user')
    if (u) { state.user = u; state.remember = true }
  } catch (e) {}
}

function submit() {
  if (!state.user.trim() || !state.pass.trim()) {
    state.err = true
    state.ok = false
    state.msg = '神识校验失败：编号与密钥缺一不可'
    state.shake = true
    setTimeout(() => { state.shake = false }, 500)
    return
  }
  state.err = false
  state.ok = true
  state.msg = '星辰共鸣中…'
  try {
    if (state.remember) localStorage.setItem('zhan_shen_user', state.user.trim())
    else localStorage.removeItem('zhan_shen_user')
  } catch (e) {}
  setTimeout(() => { state.success = true }, 650)
}

function ripple(e) {
  const b = btn.value
  if (!b) return
  const rect = b.getBoundingClientRect()
  const d = Math.max(rect.width, rect.height)
  const span = document.createElement('span')
  span.className = 'ripple'
  span.style.width = span.style.height = d + 'px'
  span.style.left = (e.clientX - rect.left - d / 2) + 'px'
  span.style.top = (e.clientY - rect.top - d / 2) + 'px'
  b.appendChild(span)
  setTimeout(() => span.remove(), 600)
}

onMounted(load)
</script>

<template>
  <form class="card" :class="{ shake: state.shake }" @submit.prevent="submit" autocomplete="off">
    <div class="title">我在精神病院学斩神</div>
    <div class="sub">第七病区 · 神识认证终端 v2.7</div>
    <div class="field">
      <input id="user" type="text" v-model="state.user" placeholder=" " />
      <label for="user">病友编号 / 代号</label>
    </div>
    <div class="field">
      <input id="pass" type="password" v-model="state.pass" placeholder=" " />
      <label for="pass">封印密钥</label>
    </div>
    <div class="row">
      <label class="rem"><input type="checkbox" v-model="state.remember" /> 记住本尊</label>
      <a href="#" @click.prevent>遗失密钥？</a>
    </div>
    <button class="btn" ref="btn" type="submit" @click="ripple">进入病区</button>
    <div class="msg" :class="{ err: state.err, ok: state.ok }">{{ state.msg }}</div>
  </form>
  <div class="success" :class="{ show: state.success }">
    <div class="box">
      <h2>斩神协议已激活</h2>
      <p>欢迎回来，林七夜。星辰刀已就位。</p>
    </div>
  </div>
</template>
