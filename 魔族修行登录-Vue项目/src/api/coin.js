import { http } from './auth'

// 钱囊总览：余额 + 今日收支 + 累计
export function walletOverview() {
  return http.get('/api/coin/wallet')
}

// 收支流水
export function coinLogs(limit = 30) {
  return http.get('/api/coin/logs', { params: { limit } })
}

// 藏宝阁在售物品（含限购与持有）
export function shopList() {
  return http.get('/api/coin/shop')
}

// 购买 qty 件
export function buyItem(itemId, qty = 1) {
  return http.post('/api/coin/buy', { itemId, qty })
}

// 乾坤袋
export function bagList() {
  return http.get('/api/coin/bag')
}

// 使用 qty 件
export function useItem(itemId, qty = 1) {
  return http.post('/api/coin/use', { itemId, qty })
}
