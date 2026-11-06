import { http } from './auth'

// 道途成就墙：逐条进度 / 自动铭刻 / 分期汇总 / 下一目标 / 已获称号
export function achievementList() {
  return http.get('/api/achievement/list')
}

// 今日修行七课：焚香 / 参悟 / 论道 / 切磋 / 研读 / 秘境 / 洞府收取
export function dailyProgress() {
  return http.get('/api/achievement/daily')
}
