// ==================== 修行境界配置（单一出口） ====================
// 数据源：后端 demon_realm 表 → GET /api/home/realms
// 下方为内置兜底值（与表初始内容一致），应用启动时 initRealmConfig() 会原地覆盖，
// 因此调整境界只需改数据库表，无需再来前端改数组。
import { homeRealms } from '../api/home'

/** 十境之名（索引即位阶）：引煞 0 → 无上 9 */
export const REALM_NAMES = ['引煞', '凝煞', '魔卒', '魔尉', '魔将', '血煞', '魔尊', '魔祖', '魔圣', '无上']

/** 冲击各境界所需煞气（索引即目标位阶；引煞为起始境，值仅占位） */
export const REALM_THRESHOLDS = [100, 300, 800, 1500, 2400, 3800, 8000, 12000, 15000, 20000]

/** 各境界展示色（视觉配置，仅前端使用） */
export const REALM_COLORS = [
  '#6b7a8f', '#8a5fb0', '#b3552e', '#c2944a', '#a02c48',
  '#ff3b6b', '#ff9f43', '#ffd76a', '#6ff7e8', '#c77dff'
]

/** 下拉选项（{v,t} 形式，后台表单用）；随 REALM_NAMES 同步原地更新 */
export const REALM_OPTIONS = REALM_NAMES.map(r => ({ v: r, t: r }))

/** 原地替换数组内容，保证各组件持有的引用同步生效 */
function replaceAll(target, next) {
  target.splice(0, target.length, ...next)
}

/**
 * 从后端拉取境界配置并同步本地数组。
 * 后端未启动 / 表为空时静默失败，沿用内置兜底值。
 */
export async function initRealmConfig() {
  try {
    const res = await homeRealms()
    const ladder = res?.data?.data?.ladder
    if (!Array.isArray(ladder) || !ladder.length) return

    replaceAll(REALM_NAMES, ladder.map(n => n.name))
    // 后台下拉选项跟随同步
    replaceAll(REALM_OPTIONS, REALM_NAMES.map(r => ({ v: r, t: r })))
    // 阈值缺失时保留兜底值，避免进度条算出 NaN
    const thresholds = ladder.map(n => (typeof n.threshold === 'number' ? n.threshold : 0))
    if (thresholds.some(v => v > 0)) replaceAll(REALM_THRESHOLDS, thresholds)
  } catch (e) {
    // 后端未就绪：使用内置兜底值，不阻塞页面
  }
}
