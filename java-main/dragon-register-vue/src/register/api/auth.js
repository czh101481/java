/**
 * 认证接口：注册
 * 默认后端地址沿用原页面的 http://localhost:8081
 * 可在项目根目录新建 .env.local 覆盖：VITE_API_BASE=/api （配合 vite 代理使用）
 */
const API_BASE = (import.meta.env.VITE_API_BASE ?? 'http://localhost:8081').replace(/\/$/, '')

export const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

/**
 * 提交注册
 * @param {{username:string,email:string,password:string}} payload
 * @returns {Promise<{ok:boolean,data:any}>} ok 为 HTTP 层是否成功，data 为响应体
 */
export async function register(payload) {
  const res = await fetch(`${API_BASE}/api/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  })
  const data = await res.json().catch(() => ({ success: false, message: '服务器响应异常' }))
  return { ok: res.ok, data }
}
