import { http } from './auth'

// 功法阁：购得功法（form=0 完整 / 1 残片；按阶位定价）
export function buyArt(artId, form = 0) {
  return http.post(`/api/art/buy/${artId}?form=${form}`)
}

// 残片补全为完整功法（付差价）
export function completeArt(artId) {
  return http.post(`/api/art/complete/${artId}`)
}

// 修炼一课（每日 3 课，耗煞气涨熟练度）
export function cultivateArt(artId) {
  return http.post(`/api/art/cultivate/${artId}`)
}

// 圆满 → 入道（耗「悟道机缘」）
export function insightArt(artId) {
  return http.post(`/api/art/insight/${artId}`)
}

// 入道 → 无上（耗「死战悟道」）
export function transcendArt(artId) {
  return http.post(`/api/art/transcend/${artId}`)
}
