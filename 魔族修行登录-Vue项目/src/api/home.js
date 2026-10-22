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
