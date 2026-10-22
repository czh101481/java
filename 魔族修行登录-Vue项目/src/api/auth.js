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

// 统一拦截：把后端 Result 的错误信息抛给调用方
http.interceptors.response.use(
  (res) => res,
  (err) => {
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

export function clearSession() {
  localStorage.removeItem('demon_token')
  localStorage.removeItem('demon_user')
}
