import axios from 'axios'
import { API_BASE } from './auth'

// 后台管理专用请求实例：令牌与修者魔契分开存储
const adminHttp = axios.create({
  baseURL: API_BASE,
  timeout: 8000
})

adminHttp.interceptors.request.use((config) => {
  const token = localStorage.getItem('demon_admin_token')
  if (token) config.headers.Authorization = 'Bearer ' + token
  return config
})

adminHttp.interceptors.response.use(
  (res) => res,
  (err) => {
    const msg =
      (err.response && err.response.data && err.response.data.msg) ||
      '后台无应答，连通失败'
    return Promise.reject(new Error(msg))
  }
)

// 后台登录（仅登录，无注册）
export function adminLogin(username, password) {
  return adminHttp.post('/api/admin/login', { username, password })
}

// 保存/读取/清除后台令牌
export function saveAdminSession(token, username) {
  localStorage.setItem('demon_admin_token', token)
  localStorage.setItem('demon_admin_name', username)
}

export function getAdminSession() {
  return {
    token: localStorage.getItem('demon_admin_token'),
    username: localStorage.getItem('demon_admin_name')
  }
}

export function clearAdminSession() {
  localStorage.removeItem('demon_admin_token')
  localStorage.removeItem('demon_admin_name')
}

// 功法阁数据管理：列表 / 新增 / 修改 / 删除
export function artList(kw) {
  return adminHttp.get('/api/admin/art/list', { params: { kw: kw || '' } })
}

export function artAdd(payload) {
  return adminHttp.post('/api/admin/art', payload)
}

export function artUpdate(id, payload) {
  return adminHttp.put(`/api/admin/art/${id}`, payload)
}

export function artDelete(id) {
  return adminHttp.delete(`/api/admin/art/${id}`)
}

// ==================== 历练录（demon_trial） ====================
export function trialList(kw) {
  return adminHttp.get('/api/admin/trial/list', { params: { kw: kw || '' } })
}

export function trialAdd(payload) {
  return adminHttp.post('/api/admin/trial', payload)
}

export function trialUpdate(id, payload) {
  return adminHttp.put(`/api/admin/trial/${id}`, payload)
}

export function trialDelete(id) {
  return adminHttp.delete(`/api/admin/trial/${id}`)
}

// ==================== 宗门论道（demon_debate） ====================
export function debateList(kw) {
  return adminHttp.get('/api/admin/debate/list', { params: { kw: kw || '' } })
}

export function debateAdd(payload) {
  return adminHttp.post('/api/admin/debate', payload)
}

export function debateUpdate(id, payload) {
  return adminHttp.put(`/api/admin/debate/${id}`, payload)
}

export function debateDelete(id) {
  return adminHttp.delete(`/api/admin/debate/${id}`)
}

// ==================== 魔典秘录（demon_codex） ====================
export function codexList(kw) {
  return adminHttp.get('/api/admin/codex/list', { params: { kw: kw || '' } })
}

export function codexAdd(payload) {
  return adminHttp.post('/api/admin/codex', payload)
}

export function codexUpdate(id, payload) {
  return adminHttp.put(`/api/admin/codex/${id}`, payload)
}

export function codexDelete(id) {
  return adminHttp.delete(`/api/admin/codex/${id}`)
}

// ==================== 魔枢典册（demon_archive） ====================
export function archiveList(kw, cat) {
  return adminHttp.get('/api/admin/archive/list', { params: { kw: kw || '', cat: cat || '' } })
}

export function archiveAdd(payload) {
  return adminHttp.post('/api/admin/archive', payload)
}

export function archiveUpdate(id, payload) {
  return adminHttp.put(`/api/admin/archive/${id}`, payload)
}

export function archiveDelete(id) {
  return adminHttp.delete(`/api/admin/archive/${id}`)
}

// ==================== 图片同步 ====================
// 扫描后端 static/images/{dir} 实际存在的图片，返回相对路径清单
export function imageList(dir) {
  return adminHttp.get('/api/admin/images', { params: { dir } })
}
