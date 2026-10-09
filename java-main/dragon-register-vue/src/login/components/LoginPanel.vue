<template>
  <!-- 登录卡片（龙渊风格，仅用户名登录） -->
  <section class="panel">
    <div class="eyebrow">RETURN TO THE DEPTHS</div>
    <h1>登录</h1>
    <p class="lead">以名号与秘钥应验契约，重返龙渊。</p>

    <div :class="bannerClass">{{ banner.text }}</div>

    <form novalidate :class="{ shake: shaking }" @submit.prevent="onSubmit">
      <FieldInput
        id="user"
        ref="userField"
        v-model="username"
        label="名号（用户名）"
        autocomplete="username"
        :state="formState.user"
        :hint="hintText.user"
        :hint-visible="hintShow.user"
        @input="clearError('user')"
      />

      <FieldInput
        ref="pwdField"
        id="pwd"
        v-model="password"
        type="password"
        label="秘钥（密码）"
        autocomplete="current-password"
        toggleable
        :state="formState.pwd"
        @input="clearError('pwd')"
      />

      <div class="row">
        <label class="check">
          <input v-model="remember" type="checkbox" />
          记住名号
        </label>
        <a class="link" href="#" title="龙渊暂不支持找回秘钥，请牢记于心">忘记秘钥？</a>
      </div>

      <button class="btn" type="submit" :disabled="status !== 'idle'">{{ btnLabel }}</button>
    </form>

    <div class="divider">或以龙血续缘</div>
    <button class="alt" type="button">
      <svg width="18" height="18" viewBox="0 0 24 24"><path fill="#4285F4" d="M22.5 12.2c0-.7-.1-1.4-.2-2H12v3.9h5.9a5 5 0 0 1-2.2 3.3v2.7h3.6c2.1-2 3.2-4.9 3.2-7.9Z"/><path fill="#34A853" d="M12 23c2.9 0 5.4-1 7.2-2.6l-3.6-2.7c-1 .7-2.3 1.1-3.6 1.1-2.8 0-5.1-1.9-6-4.4H2.3v2.8A10.8 10.8 0 0 0 12 23Z"/><path fill="#FBBC05" d="M6 14.4a6.5 6.5 0 0 1 0-4.1V7.5H2.3a10.8 10.8 0 0 0 0 9.7L6 14.4Z"/><path fill="#EA4335" d="M12 5.4c1.6 0 3 .6 4.1 1.6l3.1-3.1A10.8 10.8 0 0 0 12 1.6 10.8 10.8 0 0 0 2.3 7.5L6 10.4C6.9 7.9 9.2 5.4 12 5.4Z"/></svg>
      使用 Google 续缘
    </button>

    <p class="foot">尚未缔约？<a :href="registerUrl">铸就你的名号</a></p>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import FieldInput from './FieldInput.vue'
import { login as loginApi } from '../api/auth'

/**
 * 2026-10-09 改版说明：
 * 1. 登录方式由「邮箱」改为「名号（用户名）」，与后端 /api/login 契约对齐（仅用户名）；
 * 2. 功能完善：字段级校验与红色高亮、错误抖动、密码可见切换（眼睛按钮）、
 *    记住名号（localStorage 持久化）、提交加载态、防重复提交、后端 field 驱动高亮；
 * 3. 视觉由「Sakura 樱花风」统一为与注册页一致的「龙渊夜幕金红风」。
 */

const props = defineProps({
  /** 登录成功后可跳转的注册页 */
  registerUrl: { type: String, default: 'index.html' }
})

const REMEMBER_KEY = 'dragon_remember_username'

const USERNAME_HINT = '名号需 3–20 个字符'
const PWD_HINT = '请填写秘钥'

const userField = ref(null)
const pwdField = ref(null)

const username = ref('')
const password = ref('')
const remember = ref(false)

/** '' | 'bad' | 'good' */
const formState = reactive({ user: '', pwd: '' })
const hintShow = reactive({ user: false, pwd: false })
const hintText = reactive({ user: USERNAME_HINT, pwd: PWD_HINT })

const banner = reactive({ show: false, ok: true, text: '' })
const shaking = ref(false)
/** idle | submitting | success */
const status = ref('idle')

const btnLabel = computed(() => {
  if (status.value === 'submitting') return '应验中…'
  if (status.value === 'success') return '契约应验成功 ✓'
  return '应 验 契 约'
})

/** 未展现时不挂 ok/err，保持初始渲染干净（与注册页同款实现） */
const bannerClass = computed(() => {
  if (!banner.show) return 'banner'
  return banner.ok ? 'banner show ok' : 'banner show err'
})

/** 进入页面时回填上次记住的名号 */
onMounted(() => {
  const saved = localStorage.getItem(REMEMBER_KEY)
  if (saved) {
    username.value = saved
    remember.value = true
  }
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

  // 前端先做一层与注册页同规格的格式校验（登录不强校验密码长度，
  // 避免历史上注册的短密码被前端拦在门外）
  if (name.length < 3 || name.length > 20) {
    formState.user = 'bad'
    hintShow.user = true
    hintText.user = USERNAME_HINT
    userField.value?.shake()
    valid = false
  } else {
    formState.user = 'good'
    hintShow.user = false
  }

  if (!password.value) {
    formState.pwd = 'bad'
    hintShow.pwd = true
    hintText.pwd = PWD_HINT
    pwdField.value?.shake()
    valid = false
  } else {
    formState.pwd = 'good'
    hintShow.pwd = false
  }

  if (!valid) {
    shakeForm()
    return
  }

  status.value = 'submitting'
  hideBanner()

  try {
    // 仅用户名登录：{ username, password }
    const { ok, data } = await loginApi({ username: name, password: password.value })

    if (ok && data.success) {
      showBanner(true, data.message || '契约验证通过，欢迎回到龙渊')
      status.value = 'success'

      // 「记住名号」：登录成功后才写入，失败不落盘
      if (remember.value) {
        localStorage.setItem(REMEMBER_KEY, name)
      } else {
        localStorage.removeItem(REMEMBER_KEY)
      }
      return
    }

    // 后端 400/401：按 field 高亮对应输入框（401 不带 field，走整体横幅）
    if (data.field === 'username') {
      formState.user = 'bad'
      hintShow.user = true
      hintText.user = data.message
      userField.value?.shake()
    }
    if (data.field === 'password') {
      formState.pwd = 'bad'
      pwdField.value?.shake()
    }

    showBanner(false, data.message || '名号或秘钥不正确')
    shakeForm()
    status.value = 'idle'
  } catch (err) {
    showBanner(false, '无法连接龙渊服务器，请确认后端已启动 (http://localhost:8081)')
    status.value = 'idle'
  }
}
</script>

<style scoped>
.panel{padding:48px 44px;display:flex;flex-direction:column}
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
.link{color:var(--accent);text-decoration:none;font-weight:600;transition:opacity .2s var(--ease)}
.link:hover{opacity:.7}

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
  .panel{padding:38px 26px}
}
</style>
