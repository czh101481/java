import { http } from './auth'

// 魔窟状态：三设施等级 / 每时产量 / 待收取（本地按秒折算） / 升级费用
export function denState() {
  return http.get('/api/den')
}

// 一键收取：煞气入修为，魔晶/血玉入钱囊（12 小时封顶）
export function denCollect() {
  return http.post('/api/den/collect')
}

// 设施扩建：type = sha 聚煞阵 / coin 魔晶矿 / jade 血玉髓脉
export function denUpgrade(type) {
  return http.post('/api/den/upgrade', null, { params: { type } })
}
