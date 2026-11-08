import axios from 'axios'

// 后端基地址：默认本地 Spring Boot（端口 8081）
// 如需切换，可在项目根目录新建 .env 文件写入：VITE_API_BASE=http://localhost:8081
export const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8081'

export const http = axios.create({
  baseURL: API_BASE,
  timeout: 8000
})

// 请求拦截：自动携带魔契令牌（JWT）
http.interceptors.request.use((config) => {
  const token = localStorage.getItem('demon_token')
  if (token) config.headers.Authorization = 'Bearer ' + token
  return config
})

// 统一拦截：把后端 Result 的错误信息抛给调用方；
// 令牌失效时自动清本地会话并回登录页——否则用户会卡在原页反复看到「魔契已逝」。
//
// 注意：本后端的错误码放在响应体 code 里（HTTP 状态一律 200），
// 所以必须在「成功回调」里也要检查 body.code === 401，不能只看 HTTP 状态。
const AUTH_ENTRY = ['/api/auth/login', '/api/auth/register']

function isAuthEntry(url) {
  return AUTH_ENTRY.some(p => (url || '').indexOf(p) >= 0)
}

function onTokenExpired() {
  clearLocalSession()
  if (!window.location.hash.startsWith('#/login')) {
    window.location.hash = '#/login'
  }
}

http.interceptors.response.use(
  (res) => {
    const body = res.data
    if (body && body.code === 401 && !isAuthEntry(res.config && res.config.url)) {
      onTokenExpired()
    }
    return res
  },
  (err) => {
    // 兜底：真正返回 HTTP 401 的场景（网关 / 容器层拦截等）
    if (err.response && err.response.status === 401 && !isAuthEntry(err.config && err.config.url)) {
      onTokenExpired()
    }
    const msg =
      (err.response && err.response.data && err.response.data.msg) ||
      '魔门无应，连通失败'
    return Promise.reject(new Error(msg))
  }
)

// 入魔道（登录）→ 返回 { token, user }
export function login(name, password) {
  return http.post('/api/auth/login', { name, password })
}

// 开辟魔渊（注册）
export function register(payload) {
  return http.post('/api/auth/register', payload)
}

// 持令问询（当前修者信息）
export function me() {
  return http.get('/api/auth/me')
}

// 断魔契（登出）：通知后端作废令牌
export function logout() {
  return http.post('/api/auth/logout')
}

// 更换头像（avatar 为预设 key；传 null 回退道号首字）
export function updateAvatar(avatar) {
  return http.post('/api/auth/avatar', { avatar })
}

// 上传自定义头像（file 为 File 对象）
export function uploadAvatar(file) {
  const fd = new FormData()
  fd.append('file', file)
  return http.post('/api/auth/avatar/upload', fd)
}

// 本地令牌存取
export function saveSession(token, user) {
  localStorage.setItem('demon_token', token)
  localStorage.setItem('demon_user', JSON.stringify(user))
}

export function getSession() {
  const token = localStorage.getItem('demon_token')
  let user = null
  try {
    user = JSON.parse(localStorage.getItem('demon_user'))
  } catch (e) { /* ignore */ }
  return { token, user }
}

// 只清本地存储（不发请求），供 401 拦截与内部复用，避免递归
function clearLocalSession() {
  localStorage.removeItem('demon_token')
  localStorage.removeItem('demon_user')
}

// 主动断魔契：先让后端作废令牌，再清本地。
// 后端连不上也照常退出——本地退出不该被网络问题卡住。
export function clearSession() {
  const token = localStorage.getItem('demon_token')
  if (token) {
    axios.post(`${API_BASE}/api/auth/logout`, null, {
      headers: { Authorization: 'Bearer ' + token },
      timeout: 3000
    }).catch(() => {})
  }
  clearLocalSession()
}
