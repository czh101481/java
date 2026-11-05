import { http } from './auth'

// 功法阁：购得功法（校验重数 / 境界 / 前置重后扣魔晶或血玉）
export function buyArt(artId) {
  return http.post(`/api/art/buy/${artId}`)
}

// 我已习得的功法（含数值与战力贡献）
export function myArts() {
  return http.get('/api/art/mine')
}
