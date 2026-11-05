import { http } from './auth'

// 鬼市在售挂单（匿名可浏览）
export function tradeList() {
  return http.get('/api/trade/list')
}

// 我挂的单（含已售 / 已撤）
export function myTrades() {
  return http.get('/api/trade/mine')
}

// 寄售：从乾坤袋扣 qty 件，按 price 单价挂出
export function sellItem(itemId, qty, price, currency) {
  return http.post('/api/trade/sell', { itemId, qty, price, currency })
}

// 撤单：原物退回乾坤袋
export function cancelTrade(tradeId) {
  return http.post('/api/trade/cancel', { tradeId })
}

// 买下他人挂单
export function buyTrade(tradeId) {
  return http.post('/api/trade/buy', { tradeId })
}
