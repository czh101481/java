<template>
  <!-- 第二/三级：信息块 + 细节 -->
  <section class="panel">
    <div class="eyebrow">SIGN IN</div>
    <h1>登录</h1>
    <p class="lead">输入账号信息，开启你的世界。</p>

    <form @submit.prevent="onSubmit">
      <FieldInput
        id="email"
        v-model="email"
        type="email"
        label="邮箱地址"
        autocomplete="email"
      />
      <FieldInput
        id="pwd"
        v-model="password"
        type="password"
        label="密码"
        autocomplete="current-password"
      />

      <div class="row">
        <label class="check"><input v-model="remember" type="checkbox" /> 记住我</label>
        <a class="link" href="#">忘记密码？</a>
      </div>

      <button class="btn" type="submit" :disabled="status !== 'idle'">{{ btnLabel }}</button>
    </form>

    <div v-if="banner" class="banner" :class="banner.ok ? 'ok' : 'err'">{{ banner.text }}</div>

    <div class="divider">或使用</div>
    <button class="alt" type="button">使用 Google 继续</button>

    <p class="foot">还没有账号？<a href="index.html">立即注册</a></p>
  </section>
</template>

<script setup>
import { computed, ref } from 'vue'
import FieldInput from './FieldInput.vue'
import { login as loginApi } from '../api/auth'

const email = ref('')
const password = ref('')
const remember = ref(false)

/** '' | 'submitting' | 'success' */
const status = ref('idle')
/** null | {ok:boolean, text:string}，初始不渲染，保持与原页面初始 DOM 一致 */
const banner = ref(null)

const btnLabel = computed(() => {
  if (status.value === 'submitting') return '登录中…'
  if (status.value === 'success') return '登录成功 ✓'
  return '登 录'
})

/**
 * 原页面此处为 `onsubmit="return false"`，现接入后端 /api/login。
 * 表单未加 novalidate，浏览器原生 email 校验行为与原页面一致。
 */
async function onSubmit() {
  if (status.value !== 'idle') return

  const account = email.value.trim()
  if (!account || !password.value) {
    banner.value = { ok: false, text: '请填写邮箱与密码' }
    return
  }

  status.value = 'submitting'
  banner.value = null

  try {
    // 后端支持用户名或邮箱登录，这里传账号统一走 username 字段
    const { ok, data } = await loginApi({ username: account, password: password.value })

    if (ok && data.success) {
      banner.value = { ok: true, text: data.message || '登录成功，欢迎回来' }
      status.value = 'success'
      return
    }

    banner.value = { ok: false, text: data.message || '邮箱或密码不正确' }
    status.value = 'idle'
  } catch (err) {
    banner.value = { ok: false, text: '无法连接服务器，请确认后端已启动 (http://localhost:8081)' }
    status.value = 'idle'
  }
}
</script>

<style scoped>
.panel{padding:48px 44px;display:flex;flex-direction:column}
.panel .eyebrow{color:var(--accent);font-weight:700;font-size:13px;letter-spacing:.18em}
.panel h1{font-size:28px;font-weight:700;margin:6px 0 4px;letter-spacing:.01em}
.panel .lead{color:var(--ink-2);font-size:14px;margin-bottom:28px}

.row{display:flex;align-items:center;justify-content:space-between;margin:4px 0 26px;font-size:13px}
.check{display:inline-flex;align-items:center;gap:8px;color:var(--ink-2);cursor:pointer}
.check input{accent-color:var(--accent);width:15px;height:15px}
.link{color:var(--accent);text-decoration:none;font-weight:600;transition:opacity .2s var(--ease)}
.link:hover{opacity:.7}

/* 提交反馈横幅：v-if 控制，未提交时不存在于 DOM，不影响初始渲染 */
.banner{font-size:13px;margin:0 2px 16px;padding:10px 14px;border-radius:12px}
.banner.ok{background:rgba(168,230,207,.35);color:#1E9E77;border:1px solid rgba(30,158,119,.25)}
.banner.err{background:rgba(229,72,77,.08);color:#E5484D;border:1px solid rgba(229,72,77,.22)}

.btn{
  width:100%;padding:15px;font-size:16px;font-weight:700;color:#fff;font-family:inherit;
  background:linear-gradient(135deg,var(--accent),#FF8FAB 55%,var(--accent-2));
  border:none;border-radius:14px;cursor:pointer;
  box-shadow:0 10px 24px rgba(255,111,145,.35);
  transition:transform .1s var(--ease), box-shadow .25s var(--ease), filter .25s var(--ease);
}
.btn:hover{filter:brightness(1.04);box-shadow:0 14px 30px rgba(255,111,145,.45)}
.btn:active{transform:translateY(1px) scale(.99)}

.divider{display:flex;align-items:center;gap:12px;color:var(--ink-3);font-size:12px;margin:24px 0 18px}
.divider::before,.divider::after{content:"";flex:1;height:1px;background:rgba(180,170,190,.3)}

.alt{
  width:100%;padding:13px;font-size:14px;font-weight:600;color:var(--ink);font-family:inherit;
  background:rgba(255,255,255,.6);border:1px solid rgba(180,170,190,.35);border-radius:14px;cursor:pointer;
  transition:border-color .2s var(--ease), background .2s var(--ease);
}
.alt:hover{border-color:var(--accent-2);background:#fff}

.foot{margin-top:22px;text-align:center;font-size:13px;color:var(--ink-2)}
.foot a{color:var(--accent);font-weight:600;text-decoration:none}

@media(max-width:760px){
  .panel{padding:40px 28px}
}
</style>
