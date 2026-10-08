<template>
  <!-- 注册卡片 -->
  <section class="panel">
    <div class="eyebrow">FORGE THE PACT</div>
    <h1>注册</h1>
    <p class="lead">铭刻你的名号与秘钥，加入龙渊。</p>

    <div :class="bannerClass">{{ banner.text }}</div>

    <form novalidate :class="{ shake: shaking }" @submit.prevent="onSubmit">
      <FieldInput
        id="user"
        v-model="username"
        label="名号（用户名）"
        autocomplete="username"
        :state="formState.user"
        :hint="hintText.user"
        :hint-visible="hintShow.user"
        @input="clearError('user')"
      />

      <FieldInput
        id="email"
        v-model="email"
        type="email"
        label="邮箱"
        autocomplete="email"
        :state="formState.email"
        :hint="hintText.email"
        :hint-visible="hintShow.email"
        @input="clearError('email')"
      />

      <FieldInput
        ref="pwdField"
        id="pwd"
        v-model="password"
        type="password"
        label="秘钥（密码）"
        autocomplete="new-password"
        toggleable
        :state="formState.pwd"
        @input="clearError('pwd')"
      />
      <PasswordMeter :password="password" />

      <FieldInput
        id="confirm"
        v-model="confirmPassword"
        type="password"
        label="确认秘钥"
        autocomplete="new-password"
        toggleable
        :state="formState.confirm"
        :hint="hintText.confirm"
        :hint-visible="hintShow.confirm"
        @input="clearError('confirm')"
      />

      <div class="row">
        <label class="check" :class="{ bad: termsBad }">
          <input v-model="agreed" type="checkbox" @change="termsBad = false" />
          我愿以龙血起誓，恪守 <a href="#">龙渊契约</a>
        </label>
      </div>

      <button class="btn" type="submit" :disabled="status !== 'idle'">{{ btnLabel }}</button>
    </form>

    <div class="divider">或以龙血起誓</div>
    <button class="alt" type="button">
      <svg width="18" height="18" viewBox="0 0 24 24"><path fill="#4285F4" d="M22.5 12.2c0-.7-.1-1.4-.2-2H12v3.9h5.9a5 5 0 0 1-2.2 3.3v2.7h3.6c2.1-2 3.2-4.9 3.2-7.9Z"/><path fill="#34A853" d="M12 23c2.9 0 5.4-1 7.2-2.6l-3.6-2.7c-1 .7-2.3 1.1-3.6 1.1-2.8 0-5.1-1.9-6-4.4H2.3v2.8A10.8 10.8 0 0 0 12 23Z"/><path fill="#FBBC05" d="M6 14.4a6.5 6.5 0 0 1 0-4.1V7.5H2.3a10.8 10.8 0 0 0 0 9.7L6 14.4Z"/><path fill="#EA4335" d="M12 5.4c1.6 0 3 .6 4.1 1.6l3.1-3.1A10.8 10.8 0 0 0 12 1.6 10.8 10.8 0 0 0 2.3 7.5L6 10.4C6.9 7.9 9.2 5.4 12 5.4Z"/></svg>
      使用 Google 续缘
    </button>

    <p class="foot">已是龙渊之子？<a href="login-anime.html">重返龙渊 · 登录</a></p>
  </section>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import FieldInput from './FieldInput.vue'
import PasswordMeter from './PasswordMeter.vue'
import { EMAIL_RE, register as registerApi } from '../api/auth'
import { passwordScore } from '../composables/usePasswordStrength'

const props = defineProps({
  /** 注册成功后跳转的登录页（放在 public/ 目录即可） */
  loginUrl: { type: String, default: 'login-anime.html' }
})

const USERNAME_HINT = '名号需 3–20 个字符'
const EMAIL_HINT = '请输入有效的邮箱地址'
const CONFIRM_HINT = '两次秘钥不一致'

const pwdField = ref(null)

const username = ref('')
const email = ref('')
const password = ref('')
const confirmPassword = ref('')
const agreed = ref(false)

/** '' | 'bad' | 'good' */
const formState = reactive({ user: '', email: '', pwd: '', confirm: '' })
const hintShow = reactive({ user: false, email: false, confirm: false })
const hintText = reactive({ user: USERNAME_HINT, email: EMAIL_HINT, confirm: CONFIRM_HINT })

const termsBad = ref(false)
const banner = reactive({ show: false, ok: true, text: '' })
const shaking = ref(false)
/** idle | submitting | success */
const status = ref('idle')

const btnLabel = computed(() => {
  if (status.value === 'submitting') return '契约缔结中…'
  if (status.value === 'success') return '觉醒成功 ✓'
  return '缔 结 契 约'
})

/** 未展现时不挂 ok/err，保持与原页面初始 class="banner" 一致 */
const bannerClass = computed(() => {
  if (!banner.show) return 'banner'
  return banner.ok ? 'banner show ok' : 'banner show err'
})

function shakeForm() {
  shaking.value = true
  setTimeout(() => (shaking.value = false), 420)
}

function showBanner(ok, text) {
  banner.ok = ok
  banner.text = text
  banner.show = true
}

function hideBanner() {
  banner.show = false
}

/** 用户重新输入时，清掉该字段的红色告警 */
function clearError(field) {
  if (formState[field] === 'bad') {
    formState[field] = ''
    hintShow[field] = false
  }
}

async function onSubmit() {
  if (status.value !== 'idle') return

  let valid = true
  const name = username.value.trim()

  if (name.length < 3 || name.length > 20) {
    formState.user = 'bad'
    hintShow.user = true
    valid = false
  } else {
    formState.user = 'good'
    hintShow.user = false
  }

  if (!EMAIL_RE.test(email.value.trim())) {
    formState.email = 'bad'
    hintShow.email = true
    valid = false
  } else {
    formState.email = 'good'
    hintShow.email = false
  }

  if (passwordScore(password.value) < 1) {
    formState.pwd = 'bad'
    pwdField.value?.shake()
    valid = false
  } else {
    formState.pwd = 'good'
  }

  if (!confirmPassword.value || confirmPassword.value !== password.value) {
    formState.confirm = 'bad'
    hintShow.confirm = true
    valid = false
  } else {
    formState.confirm = 'good'
    hintShow.confirm = false
  }

  if (!agreed.value) {
    termsBad.value = true
    valid = false
  } else {
    termsBad.value = false
  }

  if (!valid) {
    shakeForm()
    return
  }

  status.value = 'submitting'
  hideBanner()

  try {
    const { ok, data } = await registerApi({
      username: name,
      email: email.value.trim(),
      password: password.value
    })

    if (ok && data.success) {
      showBanner(true, data.message || '注册成功，欢迎加入龙渊！')
      status.value = 'success'
      setTimeout(() => {
        window.location.href = props.loginUrl
      }, 1200)
      return
    }

    if (data.field === 'username') {
      formState.user = 'bad'
      hintShow.user = true
      hintText.user = data.message
    }
    if (data.field === 'email') {
      formState.email = 'bad'
      hintShow.email = true
      hintText.email = data.message
    }
    if (data.field === 'password') {
      formState.pwd = 'bad'
    }

    showBanner(false, data.message || '契约缔结失败')
    status.value = 'idle'
  } catch (err) {
    showBanner(false, '无法连接龙渊服务器，请确认后端已启动 (http://localhost:8081)')
    status.value = 'idle'
  }
}
</script>

<style scoped>
.panel{padding:40px 44px;display:flex;flex-direction:column;overflow-y:auto;max-height:92vh}
.panel .eyebrow{color:var(--accent);font-weight:700;font-size:13px;letter-spacing:.22em;font-family:"Cinzel",serif}
.panel h1{font-family:"Noto Serif SC",serif;font-size:28px;font-weight:700;margin:6px 0 4px}
.panel .lead{color:var(--ink-2);font-size:14px;margin-bottom:22px}

.banner{font-size:13px;margin:0 2px 14px;padding:10px 14px;border-radius:12px;display:none}
.banner.show{display:block}
.banner.ok{background:rgba(91,231,169,.14);color:var(--ok);border:1px solid rgba(91,231,169,.35)}
.banner.err{background:rgba(255,107,107,.12);color:var(--err);border:1px solid rgba(255,107,107,.32)}

.row{display:flex;align-items:center;justify-content:space-between;margin:2px 0 22px;font-size:13px}
.check{display:inline-flex;align-items:center;gap:8px;color:var(--ink-2);cursor:pointer;line-height:1.4}
.check input{accent-color:var(--accent);width:15px;height:15px;flex:none}
.check a{color:var(--accent);text-decoration:none;font-weight:600}
.check.bad{color:var(--err)}

.btn{
  width:100%;padding:15px;font-size:16px;font-weight:700;color:#1a1206;
  font-family:"Noto Serif SC",serif;letter-spacing:.04em;
  background:linear-gradient(135deg,var(--accent),#D99A2E 55%,var(--accent-3));
  border:none;border-radius:14px;cursor:pointer;
  box-shadow:0 10px 26px rgba(232,184,75,.30), 0 0 0 1px rgba(255,220,150,.25) inset;
  transition:transform .1s var(--ease), box-shadow .25s var(--ease), filter .25s var(--ease);
}
.btn:hover{filter:brightness(1.05);box-shadow:0 14px 34px rgba(232,184,75,.45), 0 0 18px rgba(232,184,75,.35)}
.btn:active{transform:translateY(1px) scale(.99)}

.divider{display:flex;align-items:center;gap:12px;color:var(--ink-3);font-size:12px;margin:22px 0 16px}
.divider::before,.divider::after{content:"";flex:1;height:1px;background:rgba(150,170,220,.2)}

.alt{
  width:100%;padding:13px;font-size:14px;font-weight:600;color:var(--ink);font-family:inherit;
  background:rgba(8,12,22,.4);border:1px solid rgba(150,170,220,.25);border-radius:14px;cursor:pointer;
  display:inline-flex;align-items:center;justify-content:center;gap:8px;
  transition:border-color .2s var(--ease), background .2s var(--ease);
}
.alt:hover{border-color:var(--accent-2);background:rgba(8,12,22,.6)}

.foot{margin-top:22px;text-align:center;font-size:13px;color:var(--ink-2)}
.foot a{color:var(--accent);font-weight:600;text-decoration:none}

@media(max-width:760px){
  .panel{padding:38px 26px;max-height:none}
}
</style>
