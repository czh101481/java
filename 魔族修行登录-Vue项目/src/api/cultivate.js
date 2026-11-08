import { http } from './auth'

// 修为数值面板：战力 / 攻伐 / 护体 / 气血 / 悟性 / 身法 + 九重进度 + 已习功法
export function cultivatePanel() {
  return http.get('/api/cultivate/panel')
}
