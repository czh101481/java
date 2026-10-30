import { http, API_BASE } from './auth'

// 内容配图地址：后端返回相对路径（如 /images/art/art-1.jpg），拼接基地址
export function imgUrl(path) {
  return path ? API_BASE + path : ''
}

// 首页概览：各模块数量 + 精选
export function homeOverview() {
  return http.get('/api/home/overview')
}

// 跨表检索：kw 为关键字
export function homeSearch(kw) {
  return http.get('/api/home/search', { params: { kw } })
}

// 魔门公告：走马灯内容
export function homeAnnouncements() {
  return http.get('/api/home/announcements')
}

// 热门修习：各模块最新 2 条混合推荐
export function homeHighlights() {
  return http.get('/api/home/highlights')
}

// 模块列表：type=art|trial|debate|codex，kw 可选过滤
export function homeList(type, kw) {
  return http.get('/api/home/list', { params: { type, kw: kw || '' } })
}

// 内容详情：type + id
export function homeDetail(type, id) {
  return http.get('/api/home/detail', { params: { type, id } })
}

// 境界阶梯：十境全量 + 各功能所需境界（公开）
export function homeRealms() {
  return http.get('/api/home/realms')
}

// 修者当前境界状态：境界名 / 位阶 / 进度 / 煞气修为（需持魔契）
export function homeRealmStatus() {
  return http.get('/api/home/realm')
}

// 参悟魔域：浏览内容详情时调用，积累煞气修为（每次 +5）
export function explore(type, id) {
  return http.post('/api/home/explore', { type, id })
}

// 冲击境界：煞气攒足则晋升一境
export function breakthrough() {
  return http.post('/api/home/breakthrough')
}

// 魔门排行：按境界+煞气排序的前 10 名修者（持魔契时标记本人）
export function homeRank() {
  return http.get('/api/home/rank')
}

// 论道问答：取论题与选项（不泄露答案，需魔卒境）
export function debateQuiz(id) {
  return http.get('/api/home/quiz', { params: { id } })
}

// 论道作答：choice 为 A/B/C，答对 +10 煞气，答错 -5
export function debateAnswer(id, choice) {
  return http.post('/api/home/quiz/answer', { id, choice })
}

// 个人道牌（勋章墙）：十境勋章 + 修行数据总览
export function homeBadges() {
  return http.get('/api/home/badges')
}

// 修者切磋：与指定道号对战，胜者 +15 煞气、败者 -10
export function sparring(targetName) {
  return http.post('/api/home/sparring', { targetName })
}
