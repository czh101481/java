# 魔族修行 · 入魔道（Vue 3 前端）

基于原「魔族修行登录」网页 1:1 复刻的 **Vue 3 + Vite + Vue Router** 工程，整体暗黑魔幻风格保持不变。

## 目录结构
```
魔族修行登录-Vue项目/
├── index.html              # 入口 HTML
├── package.json            # 依赖与脚本
├── vite.config.js          # Vite 配置（端口 5173，自动打开浏览器）
├── .gitignore
└── src/
    ├── main.js             # 应用入口
    ├── App.vue             # 全局背景：灵气粒子 / 魔纹法阵 / 血雾 / 低语
    ├── styles.css          # 主题样式（暗紫血色魔幻风）
    ├── api/auth.js         # 对接 Spring Boot 后端的请求层（axios）
    ├── router/index.js     # 路由：/login /register /home
    └── views/
        ├── Login.vue       # 登录页（入魔道）
        ├── Register.vue    # 注册页（开辟魔渊）
        └── Home.vue        # 魔域中枢（登录后落地页）
```

## 运行方式
```bash
# 1. 安装依赖（需 Node.js 16+）
npm install

# 2. 本地开发（自动打开 http://localhost:5173）
npm run dev

# 3. 生产构建（产物在 dist/）
npm run build

# 4. 预览构建产物
npm run preview
```

## 后端对接
前端通过 `src/api/auth.js` 调用本地 Spring Boot 接口（默认 `http://localhost:8081`）：
- `POST /api/auth/login`   登录 → 返回 `{ code, data: { token, user } }`
- `POST /api/auth/register` 注册 → 返回 `{ code, msg }`
- `GET  /api/auth/me`      持令问询当前修者

如需切换后端地址，在项目根目录新建 `.env` 文件写入：
```
VITE_API_BASE=http://你的后端地址
```

## 后端建表
数据库初始化脚本见原目录 `demon_init.sql`（MySQL，`demon_db.demon_user` 表，字段 `name/email/realm/password`）。

## 说明
- 风格与原网页完全一致：暗紫血色渐变、旋转魔纹法阵、灵气粒子上浮、血雾漂移、底部低语。
- 原网页登录/注册后 `router.push('/home')` 缺少对应路由，本工程已补齐 `/home`（魔域中枢）页面，使登录/注册流程可完整跑通。
- 未启动后端时，登录/注册会弹出「魔门无应，连通失败」提示，属正常联调行为。
