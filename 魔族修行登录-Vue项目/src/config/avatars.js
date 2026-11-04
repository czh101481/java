// 头像预设：12 款「魔纹」单字头像，key 与后端 AuthService.AVATAR_KEYS 保持一致
// 只存 key 到 demon_user.avatar，具体字形与配色由此处渲染（换肤只改这里）
export const AVATARS = [
  { key: 'blood',     char: '血', name: '血煞', c1: '#ff3b6b', c2: '#4a0d24' },
  { key: 'ghost',     char: '幽', name: '幽冥', c1: '#3ad6c9', c2: '#062a2a' },
  { key: 'soul',      char: '魂', name: '噬魂', c1: '#b06bff', c2: '#2a0b45' },
  { key: 'bone',      char: '骨', name: '白骨', c1: '#d9d2c0', c2: '#3a3630' },
  { key: 'flame',     char: '焰', name: '焚焰', c1: '#ff8a3c', c2: '#4a1a05' },
  { key: 'moon',      char: '月', name: '幽月', c1: '#8fb8ff', c2: '#101a3a' },
  { key: 'shadow',    char: '影', name: '魅影', c1: '#9a6bff', c2: '#1a0b2e' },
  { key: 'thunder',   char: '雷', name: '紫雷', c1: '#5ad1ff', c2: '#08243a' },
  { key: 'curse',     char: '咒', name: '血咒', c1: '#ff5ea8', c2: '#3a0620' },
  { key: 'spider',    char: '蛛', name: '毒蛛', c1: '#c94b3a', c2: '#2a0a06' },
  { key: 'abyss',     char: '渊', name: '深渊', c1: '#4b6bff', c2: '#060b2a' },
  { key: 'nightmare', char: '魇', name: '梦魇', c1: '#a35cff', c2: '#160029' }
]

const MAP = Object.fromEntries(AVATARS.map((a) => [a.key, a]))

export function avatarOf(key) {
  return (key && MAP[key]) || null
}

// 统一渲染入口：有预设 → 预设字形+渐变；否则回退「道号首字 + 血煞渐变」
export function renderAvatar(key, fallbackName) {
  const a = avatarOf(key)
  if (a) {
    return {
      char: a.char,
      style: {
        background: `linear-gradient(135deg, ${a.c1}, ${a.c2})`,
        boxShadow: `0 0 12px ${a.c1}77`,
        borderColor: a.c1 + '99'
      }
    }
  }
  return {
    char: (fallbackName || '魔').slice(0, 1),
    style: {}
  }
}
