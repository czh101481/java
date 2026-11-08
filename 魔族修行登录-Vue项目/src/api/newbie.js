import { http } from './auth'

// 入魔道：开局礼包状态、七日礼进度、新兵期剩余天数与倍率
// 注意：state 对尚无新手记录的存量账号会懒补发开局礼包，是唯一有副作用的读接口
export function newbieState() {
  return http.get('/api/newbie/state')
}

// 领取下一日之礼（同日不可重复，跨日随到随领）
export function newbieClaim() {
  return http.post('/api/newbie/claim')
}
