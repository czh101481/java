<template>
  <!-- 登录框 -->
  <div class="portal">
    <div class="crest">
      <div class="mark">魔族修行</div>
      <div class="sub">堕 入 魔 道 · 觉 醒 真 我</div>
      <div class="divider"></div>
    </div>

    <form @submit.prevent="onEnter" autocomplete="off">
      <div class="field">
        <label for="name">修 者 道 号</label>
        <span class="icon">☥</span>
        <input id="name" v-model="form.name" type="text" placeholder="以真名叩响魔门…" required />
      </div>
      <div class="field">
        <label for="key">护 法 密 令</label>
        <span class="icon">⚸</span>
        <input id="key" v-model="form.key" type="password" placeholder="以血契封印心魔…" required />
      </div>
      <button class="enter" type="submit">入 魔 道</button>
    </form>

    <div class="foot">
      <a href="#" @click.prevent="onForget">忘却密令？</a>
      <router-link to="/register">开 辟 魔 渊</router-link>
    </div>
  </div>

  <!-- 登录仪式特效 -->
  <div class="ritual" :class="{ show: ritual }">
    <div class="rune">魔 门 已 开 · 恭 迎 归 来</div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { login, saveSession } from '../api/auth'

const form = reactive({ name: '', key: '' })
const ritual = ref(false)
const router = useRouter()

async function onEnter() {
  try {
    const res = await login(form.name, form.key)
    if (res.data.code === 200) {
      // 保存魔契令牌与修者信息
      saveSession(res.data.data.token, res.data.data.user)
      ritual.value = true
      setTimeout(() => {
        ritual.value = false
        router.push('/home')
      }, 1800)
    } else {
      alert(res.data.msg || '入魔失败')
    }
  } catch (e) {
    alert(e.message)
  }
}

function onForget() {
  alert('密令乃血契所封，唯持印者可唤醒。需往魔渊中枢重立契约。')
}
</script>
