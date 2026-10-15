// 零依赖静态预览服务：把 dist 通过 http 提供出去
// 说明：dist/index.html 不能双击用 file:// 打开（浏览器禁止 file 协议加载 ES module），
// 必须通过 http 访问，本脚本就是最小实现（不需要 node_modules）。
const http = require('http')
const fs = require('fs')
const path = require('path')

const ROOT = path.join(__dirname, 'dist')
const PORT = Number(process.env.PORT || 8080)

const MIME = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.webp': 'image/webp',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.gif': 'image/gif',
  '.svg': 'image/svg+xml',
  '.json': 'application/json; charset=utf-8',
  '.ico': 'image/x-icon',
  '.woff2': 'font/woff2'
}

http
  .createServer((req, res) => {
    let urlPath = decodeURIComponent((req.url || '/').split('?')[0])
    if (urlPath === '/' || urlPath === '') urlPath = '/index.html'

    const file = path.join(ROOT, urlPath)
    if (!file.startsWith(ROOT)) {
      res.writeHead(403)
      return res.end('forbidden')
    }

    fs.readFile(file, (err, data) => {
      if (err) {
        // SPA 回退：未命中的路径都返回 index.html（配合 hash 路由不会出问题）
        fs.readFile(path.join(ROOT, 'index.html'), (e2, d2) => {
          if (e2) {
            res.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' })
            return res.end('未找到 dist/index.html，请先执行 npm run build')
          }
          res.writeHead(200, { 'Content-Type': MIME['.html'] })
          res.end(d2)
        })
        return
      }
      const ext = path.extname(file).toLowerCase()
      res.writeHead(200, { 'Content-Type': MIME[ext] || 'application/octet-stream' })
      res.end(data)
    })
  })
  .listen(PORT, () => {
    console.log('魔域已开启： http://localhost:' + PORT)
    console.log('（关闭此窗口即停止服务）')
  })
