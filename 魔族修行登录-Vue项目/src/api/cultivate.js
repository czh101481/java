import { http } from './auth'

// 修为数值面板：战力 / 攻伐 / 护体 / 气血 / 悟性 / 身法 + 九重进度 + 已习功法
export function cultivatePanel() {
  return http.get('/api/cultivate/panel')
}

// 数值规则（公开）：每境重数、成长系数、品阶重数上限
export function cultivateRules() {
  return http.get('/api/cultivate/rules')
}
