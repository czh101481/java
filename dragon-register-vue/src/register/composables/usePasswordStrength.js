/**
 * 密码强度：与原页面 pwdScore / renderMeter 的规则完全一致
 */

/** 强度档位（索引 = 得分 0~4） */
export const STRENGTH_LEVELS = [
  { w: 0, c: '#E5484D', t: '秘钥至少 8 位，建议混合字符' },
  { w: 25, c: '#E5484D', t: '虚弱：再长一些方显龙威' },
  { w: 50, c: '#E8B84B', t: '尚可：建议大小写与数字并用' },
  { w: 75, c: '#5BC8F5', t: '不俗：已含多种字符' },
  { w: 100, c: '#5BE7A9', t: '炽烈：可安心立契' }
]

/** 四项加权：长度≥8 / 大小写混用 / 含数字 / 含符号 → 0~4 分 */
export function passwordScore(value) {
  const v = value || ''
  let s = 0
  if (v.length >= 8) s++
  if (/[a-z]/.test(v) && /[A-Z]/.test(v)) s++
  if (/\d/.test(v)) s++
  if (/[^A-Za-z0-9]/.test(v)) s++
  return s
}

/** 得分 → 档位对象 */
export function strengthOf(score) {
  return STRENGTH_LEVELS[Math.min(score, 4)]
}
