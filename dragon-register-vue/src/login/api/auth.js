/**
 * 认证接口：登录（Sakura Pass 主题页专用）
 * 与注册页 src/register/api/auth.js 同一套后端（http://localhost:8081）
 * 可在项目根目录 .env.local 覆盖：VITE_API_BASE=/api （配合 vite 代理使用）
 */
const API_BASE = (import.meta.env.VITE_API_BASE ?? 'http://localhost:8081').replace(/\/$/, '')

/**
 * 登录（用户名或邮箱 + 密码）
 * @param {{account:string,password:string}} payload
 * @returns {Promise<{ok:boolean,data:any}>}
 */
export async function login(payload) {
  const res = await fetch(`${API_BASE}/api/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  })
  const data = await res.json().catch(() => ({ success: false, message: '服务器响应异常' }))
  return { ok: res.ok, data }
}
