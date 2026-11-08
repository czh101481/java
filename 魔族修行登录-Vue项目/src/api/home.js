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

// 四系魔种天赋一览（公开，注册页/道牌页展示用，读 demon_origin 表）
export function homeOrigins() {
  return http.get('/api/home/origins')
}

// 我的魔种天赋详情（需持魔契）
export function myOrigin() {
  return http.get('/api/home/origin')
}

// 参悟此篇：必须指定具体篇章（type=art|codex|archive，id），每篇每日仅一次
export function explore(type, id) {
  return http.post('/api/home/explore', { type, id })
}

// 随机参悟：后端从今日尚未参悟的功法/秘录/典籍中挑一篇参悟
export function exploreRandom() {
  return http.post('/api/home/explore/random')
}

// 冲击境界：煞气攒足则晋升一境
export function breakthrough() {
  return http.post('/api/home/breakthrough')
}

// 魔门排行：type = exp（英杰总榜，缺省）/ quiz（论道榜）/ spar（切磋榜）
export function homeRank(type) {
  return http.get('/api/home/rank', { params: { type: type || 'exp' } })
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

// ==================== v2 玩法 ====================

// 执行签到：连签越久赏赐越厚，连满七日另有大赏
export function doSign() {
  return http.post('/api/home/sign')
}

// 转生轮回：无上境方可，境界归元换取每世 +10% 永久参悟加成
export function reincarnate() {
  return http.post('/api/home/reincarnate')
}

// 研读典籍：首次研读该篇得煞气，并计入图鉴收集度
export function readDoc(module, id) {
  return http.post('/api/home/read', { module, id })
}

// 随机论题：取一道本人尚未作答的论题，供首页「今日论道」直接开答
export function quizRandom() {
  return http.get('/api/home/quiz/random')
}

// ==================== 域外秘境（副本玩法） ====================

// 秘境一览：门槛 / 入场消耗 / 产出 / 今日剩余次数 / 可否进入
export function secretRealms() {
  return http.get('/api/home/secret/list')
}

// 进入秘境：结算一次探索，返回战报（机缘 / 功成 / 负伤）
export function enterRealm(id) {
  return http.post(`/api/home/secret/enter/${id}`)
}

// 我的秘境履历（战报回看）
export function myRealmLog(limit) {
  return http.get('/api/home/secret/log', { params: { limit: limit || 20 } })
}
