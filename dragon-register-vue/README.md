# ANIME AUTH · Vue 3 版（注册 + 登录）

由两个单文件静态页重构而来，**视觉与交互均保持一致**：

| 页面 | 原始文件 | 主题 | 入口 |
| --- | --- | --- | --- |
| 注册 | `register-anime.html` | 暗色「龙渊 · Draconic」深蓝鎏金 | `index.html` |
| 登录 | `login-anime.html` | 浅色「Sakura Pass」樱粉天蓝 | `login-anime.html` |

## 技术栈

| 项 | 说明 |
| --- | --- |
| 框架 | Vue 3（`<script setup>` 组合式 API） |
| 构建 | Vite 7 **多页应用（MPA）** |
| 样式 | 原生 CSS（Scoped）+ CSS 变量设计令牌，逐条平移自原页面 |
| 依赖 | 仅 `vue`；开发依赖 `vite`、`@vitejs/plugin-vue` |

### 为什么用多页入口而不是 vue-router

两个页面的视觉体系完全独立：`:root` 变量取值、`body` 渐变底色、`#fx` 粒子动画、字体、组件样式全都不一样（暗色龙族 vs 浅色樱花）。若合成单页应用，两套 `:root` 与 `body` 样式必然互相污染，必须做主题切换层，反而更容易破坏原样式。

多页入口让每页拥有**自己的 `main.js` 与主题 CSS**，构建后每页只加载自己的样式（`register-*.css` / `login-*.css`），保真度最高，也最贴合原站的结构。

> 若后续要做页面间无缝跳转 / 共享登录态，再引入 vue-router 并把两套主题收敛到 layout 层也不迟。

## 快速开始

```bash
npm install     # 安装依赖
npm run dev     # 开发服务器 → http://localhost:5173
npm run build   # 生产构建，输出到 dist/
npm run preview # 本地预览构建结果
```

> Node 版本要求：`^20.19.0 || >=22.12.0`（Vite 7 要求）。

开发时访问：

- `http://localhost:5173/` → 注册页
- `http://localhost:5173/login-anime.html` → 登录页

## 目录结构

```
dragon-register-vue/
├── index.html                     # 注册页入口（原 register-anime.html）
├── login-anime.html               # 登录页入口（原 login-anime.html）
├── vite.config.js                 # 多页 input 配置
├── package.json
├── backend/                       # ── 零依赖 Java 注册后端（JDK 17+）──
│   ├── RegisterServer.java        # 单文件服务器：register / login / health
│   └── start.bat                  # Windows 一键启动
└── src/
    ├── register/                  # ── 注册页（暗色龙渊主题）──
    │   ├── main.js
    │   ├── App.vue                # 主舞台 .stage
    │   ├── styles/base.css        # 设计令牌 / body 底色 / #fx / 抖动动画
    │   ├── api/auth.js            # 注册接口 + 邮箱正则
    │   ├── composables/
    │   │   └── usePasswordStrength.js
    │   └── components/
    │       ├── FireParticles.vue  # 龙焰火星粒子（46 颗，上浮 + 辉光）
    │       ├── DragonEmblem.vue   # 龙渊徽记 SVG
    │       ├── HeroPanel.vue      # 左主视觉
    │       ├── FieldInput.vue     # 浮标签输入框（含密码显隐 / 提示 / 抖动）
    │       ├── PasswordMeter.vue  # 密码强度条
    │       ├── RegisterPanel.vue  # 右侧注册卡（校验 + 提交）
    │       └── icons/EyeIcon.vue
    └── login/                     # ── 登录页（浅色樱花主题）──
        ├── main.js
        ├── App.vue
        ├── styles/base.css
        └── components/
            ├── PetalParticles.vue  # 樱花花瓣粒子（34 颗，下落 + 花瓣椭圆）
            ├── AnimeCharacter.vue  # 角色立绘 SVG
            ├── HeroPanel.vue
            ├── FieldInput.vue      # 浮标签输入框（该主题自己的一套样式）
            └── LoginPanel.vue
```

## 原文件 → 组件映射

### 注册页

| 原 `register-anime.html` | 重构后位置 |
| --- | --- |
| `body` 底色渐变、`:root` 变量 | `src/register/styles/base.css` |
| `<canvas id="fx">` + 粒子 `tick()` | `src/register/components/FireParticles.vue` |
| `.stage` 卡片栅格 | `src/register/App.vue` |
| `.hero` 品牌 / 龙徽 / 标语 | `HeroPanel.vue` + `DragonEmblem.vue` |
| `.field` 浮标签 + `.toggle` 显隐 | `FieldInput.vue` + `icons/EyeIcon.vue` |
| `pwdScore()` / `renderMeter()` | `composables/usePasswordStrength.js` |
| `fetch('http://localhost:8081/api/register')` | `api/auth.js` + `RegisterPanel.vue` |

### 登录页

| 原 `login-anime.html` | 重构后位置 |
| --- | --- |
| `body` 浅色渐变、`:root` 变量 | `src/login/styles/base.css` |
| `<canvas id="fx">` + 花瓣 `tick()` | `src/login/components/PetalParticles.vue` |
| `.stage` | `src/login/App.vue` |
| `.hero` 品牌 / 立绘 / 标语 | `HeroPanel.vue` + `AnimeCharacter.vue` |
| `.panel` 表单 / 记住我 / 忘记密码 / 按钮 | `LoginPanel.vue`（已接 `/api/login`） |
| `.field` 浮标签 | `FieldInput.vue` |
| `fetch('.../api/login')`（新增） | `src/login/api/auth.js` |

## 后端接口约定（注册页 + 登录页）

后端已切换为 **Spring Boot 3 + MyBatis-Plus + MySQL**（`D:\workSpace\springBootTest`），接口契约不变：

| 接口 | 方法 | 请求体 | 响应 |
| --- | --- | --- | --- |
| `/api/register` | POST | `{username, email, password}` | `{success, message, field?}` |
| `/api/login` | POST | `{username 或 email, password}` | `{success, message}` |
| `/api/health` | GET | — | `{success:true, message:"ok"}` |

后端启动（IDEA 里直接运行 `SpringBootTestApplication`，dev profile 端口 8081）：

```bash
cd D:\workSpace\springBootTest
mvnw.cmd spring-boot:run        # 或 IDEA 右键运行主类
```

数据库：`dragon_auth`（MySQL 本机 root/123456），建表脚本在 `springBootTest/db/init.sql`。密码为 PBKDF2WithHmacSHA256 加盐哈希，无明文。

> 旧的零依赖单文件后端仍保留在 `backend/RegisterServer.java`（JSON 文件存储），仅作无数据库环境的备用，两者不要同时启动（都占 8081）。

注册请求与原页面完全一致：

```
POST http://localhost:8081/api/register
Content-Type: application/json

{ "username": "...", "email": "...", "password": "..." }
```

响应体约定：

```jsonc
// 成功
{ "success": true, "message": "觉醒成功，正在前往龙渊登录…" }

// 失败（field 命中时会高亮对应输入框并展示该 message）
{ "success": false, "message": "此名号已被铭刻，请另择一名", "field": "username" }
// field 可选值：username | email | password
```

后端校验规则与前端 `onSubmit` 逐条对齐：用户名 3–20 字符、邮箱正则同 `EMAIL_RE`、密码至少 8 位；用户名/邮箱查重忽略大小写（`409`），参数不合法返回 `400`。

**安全与存储**：后端为 Spring Boot + MyBatis-Plus + MySQL，密码使用 `PBKDF2WithHmacSHA256`（120,000 轮迭代 + 16 字节随机盐）哈希落库，**不存明文**。已内置 CORS 支持（`@CrossOrigin`），前端 dev（5173）/preview（4173）直接跨域调用即可，无需代理。

### 自定义后端地址

默认沿用原页面的 `http://localhost:8081`。要修改，在项目根目录新建 `.env.local`：

```bash
VITE_API_BASE=http://192.168.1.10:8081
```

若要走 Vite 代理（解决跨域），在 `vite.config.js` 的 `server` 中加：

```js
server: {
  proxy: { '/api': { target: 'http://localhost:8081', changeOrigin: true } }
}
```

同时设置 `VITE_API_BASE=/api`。

> 登录页已接入 `/api/login`（`src/login/api/auth.js` + `LoginPanel.vue` 的 `onSubmit`）：支持用户名或邮箱登录，提交后显示成功/失败横幅（`v-if` 控制，初始 DOM 与原页面一致）。

### 换数据库

当前用 JSON 文件落盘是为了零依赖跑通闭环。后续要换 MySQL：把 `loadUsers()` / `saveUsers()` 两处替换为 JDBC 读写即可，其余逻辑不用动。

## 相比原页面的行为差异

功能逻辑逐条对齐，仅有以下不影响观感的小差异：

1. **跳转链接打通**：注册页页脚 / 注册成功后的跳转目标仍为 `login-anime.html`（构建产物中真实存在）；登录页的「立即注册」由原来的空链接 `href="#"` 改为 `index.html`。登录页「忘记密码？」仍为 `href="#"`（无对应页面）。
2. **移除已失效的 DOM id**：原 JS 用 `getElementById('f-user')`、`#form`、`#banner` 等做 class 切换，这些钩子在 Vue 中已由响应式状态替代，故未保留（`#fx` 画布 id 保留，因为 CSS 依赖它）。这些 id 没有任何 CSS 选择器引用，不影响渲染。
3. **表单提交方式**：注册页改为 `@submit.prevent`（等效于原 `e.preventDefault()`）；登录页同样使用 `@submit.prevent` 且**不加 `novalidate`**，因此保留了浏览器原生 email 校验行为。
4. **注销按钮状态**：注册页提交按钮在请求中/成功后通过 `disabled` 控制，行为与原页面一致。

## 样式零丢失的验证方式

本项目通过「服务端渲染出真实 DOM + 逐项比对」自证样式未丢失（本机无浏览器时的可行方案）：

1. 用 `createSSRApp` + `vue/server-renderer` 的 `renderToString` 渲染出两页的真实 DOM 结构；
2. 用 `HTMLParser` 抽取 `(标签, class 集合)` 序列，与原页面 body 逐项对齐比对；
3. 用正则抽取每条 CSS 规则的声明块，去空白后做多重集比较，确认原页面声明块全部命中。

**验证结果**

| 页面 | DOM | CSS |
| --- | --- | --- |
| 注册页 | 86 → 85 个元素，唯一差异是 `<script>`（已转为 Vue 代码） | 原 58 条规则 **全部命中**，仅新增 `#app{display:contents}` |
| 登录页 | 59 → 58 个元素，唯一差异是 `<script>` | 原 39 条规则 **全部命中**，仅新增 `#app{display:contents}` |

### 关于 `#app{display:contents}`

原页面 `.stage` 是 `body` 的直接子元素，而 `body` 用 `display:grid;place-items:center` 做居中。Vue 挂载后多了 `#app` 一层，网格子项会变成 `#app`（其宽度又依赖 `.stage` 的 `width:100%`），居中会塌掉。

`display:contents` 让 `#app` 不生成盒子，`.stage` 重新成为 body 的网格子项，布局与原页面完全一致。**这是全项目唯一新增的样式声明。**

## 后续可扩展点

- 登录页接入后端（`LoginPanel.vue` 的 `onSubmit`）。
- 若要单页应用体验，引入 vue-router 并把两套 `:root` / `body` 主题收敛到 layout 层。
- 注册页表单逻辑已抽为组件与组合式函数，接入 Pinia 或校验库（如 vee-validate）成本很低。
