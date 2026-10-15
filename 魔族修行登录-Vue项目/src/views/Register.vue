<template>
  <!-- 注册框 -->
  <div class="portal">
    <div class="crest">
      <div class="mark">开辟魔渊</div>
      <div class="sub">缔 血 契 · 列 魔 籍 · 铸 真 我</div>
      <div class="divider"></div>
    </div>

    <form @submit.prevent="onSubmit" autocomplete="off" novalidate>
      <div class="field" :class="{ err: errors.name }">
        <label for="rname">修 者 道 号</label>
        <span class="icon">☥</span>
        <input id="rname" v-model="form.name" type="text" placeholder="以真名刻入魔碑…" />
      </div>

      <div class="field" :class="{ err: errors.email }">
        <label for="remail">灵 识 印 记</label>
        <span class="icon">✉</span>
        <input id="remail" v-model="form.email" type="email" placeholder="以印记连通魔渊通讯…" />
      </div>

      <div class="field" :class="{ err: errors.realm }">
        <label for="rrealm">择 魔 种</label>
        <span class="icon">⚝</span>
        <select id="rrealm" v-model="form.realm">
          <option value="" disabled>— 择 一 魔 种 而 生 —</option>
          <option value="血煞">血煞 · 以战养道</option>
          <option value="幽冥">幽冥 · 操纵生死</option>
          <option value="噬魂">噬魂 · 吞灵证道</option>
          <option value="虚妄">虚妄 · 颠倒虚实</option>
        </select>
      </div>

      <div class="field" :class="{ err: errors.password }">
        <label for="rkey">护 法 密 令</label>
        <span class="icon">⚸</span>
        <input id="rkey" v-model="form.password" type="password" placeholder="至少六字，封印心魔…" />
      </div>

      <div class="field" :class="{ err: errors.confirm }">
        <label for="rconfirm">重 书 密 令</label>
        <span class="icon">⚸</span>
        <input id="rconfirm" v-model="form.confirm" type="password" placeholder="再书一遍，以证本心…" />
      </div>

      <p v-if="errors.summary" class="err-msg">{{ errors.summary }}</p>

      <button class="enter" type="submit">缔 血 契</button>
    </form>

    <div class="foot">
      <router-link to="/login">已 入 魔 道 ？ 返 回 魔 门</router-link>
      <a href="#" @click.prevent="onSeal">重 立 契 约</a>
    </div>
  </div>

  <!-- 开辟魔渊仪式特效 -->
  <div class="ritual" :class="{ show: ritual }">
    <div class="rune">魔 渊 已 辟 · 道 号 入 籍</div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { register, login, saveSession } from '../api/auth'

const form = reactive({ name: '', email: '', realm: '', password: '', confirm: '' })
const errors = reactive({ name: '', email: '', realm: '', password: '', confirm: '', summary: '' })
const ritual = ref(false)
const router = useRouter()

const emailRe = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

function validate() {
  errors.name = form.name.trim() ? '' : '道号不可为空，魔碑无名则不得列籍。'
  errors.email = !form.email.trim()
    ? '灵识印记不可为空。'
    : (emailRe.test(form.email) ? '' : '印记不成形，非有效通讯之纹。')
  errors.realm = form.realm ? '' : '未择魔种，无从开辟魔渊。'
  errors.password = form.password.length >= 6 ? '' : '密令需六字以上，方足以封印心魔。'
  errors.confirm = form.confirm === form.password ? '' : '两度所书密令不一，心魔将趁隙而出。'

  const first = [errors.name, errors.email, errors.realm, errors.password, errors.confirm].find(Boolean)
  errors.summary = first ? first : ''
  return !first
}

async function onSubmit() {
  if (!validate()) return
  try {
    const res = await register({
      name: form.name,
      email: form.email,
      realm: form.realm,
      password: form.password
    })
    if (res.data.code !== 200) {
      errors.summary = res.data.msg || '开辟魔渊失败'
      return
    }
    // 开辟成功 → 自动入魔道（登录拿令牌）
    const loginRes = await login(form.name, form.password)
    if (loginRes.data.code === 200) {
      saveSession(loginRes.data.data.token, loginRes.data.data.user)
      ritual.value = true
      setTimeout(() => {
        ritual.value = false
        Object.keys(form).forEach(k => (form[k] = ''))
        router.push('/home')
      }, 1800)
    } else {
      errors.summary = loginRes.data.msg || '血契已成，但入魔门失败，请手动登录'
    }
  } catch (e) {
    errors.summary = e.message
  }
}

function onSeal() {
  Object.keys(form).forEach(k => (form[k] = ''))
  Object.keys(errors).forEach(k => (errors[k] = ''))
}
</script>
