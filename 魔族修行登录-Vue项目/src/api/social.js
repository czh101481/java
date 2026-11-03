import { http } from './auth'

// 待处理的好友申请（呈予我的）
export function friendRequests() {
  return http.get('/api/social/friend/requests')
}

// 向某修者递出道友契约 { toName, message }
export function applyFriend(toName, message) {
  return http.post('/api/social/friend/apply', { toName, message })
}

// 处理申请：action = accept | reject
export function handleFriend(applyId, action) {
  return http.post('/api/social/friend/handle', { applyId, action })
}

// 我的道友列表
export function friendList() {
  return http.get('/api/social/friend/list')
}

// 解除道友
export function removeFriend(friendId) {
  return http.delete(`/api/social/friend/${friendId}`)
}

// 搜索修者（按道号，排除自己）
export function searchUsers(kw) {
  return http.get('/api/social/user/search', { params: { kw: kw || '' } })
}

// 会话历史（since 增量；自动标记已读）
export function messageHistory(friendId, since) {
  return http.get('/api/social/message/history', { params: { friendId, since: since || 0 } })
}

// 发送传音（msgType=1 为修炼心得卡片）
export function sendMessage(payload) {
  return http.post('/api/social/message/send', payload)
}

// 未读统计（total + 各好友计数）
export function messageUnread() {
  return http.get('/api/social/message/unread')
}
