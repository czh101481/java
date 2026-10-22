import { http } from './auth'

// 魔枢典册 · 分类卡片（4 张入口）
export function archiveCategories() {
  return http.get('/api/archive/categories')
}

// 魔枢典册 · 条目列表（cat 为空取全部，kw 可选）
export function archiveList(cat, kw) {
  return http.get('/api/archive/list', { params: { cat: cat || '', kw: kw || '' } })
}

// 魔枢典册 · 单条详情
export function archiveDetail(id) {
  return http.get('/api/archive/detail', { params: { id } })
}
