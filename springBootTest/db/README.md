# 魔族修行 · 数据库脚本执行说明

环境：MySQL 8.4（本机便携版 `C:/Users/czh10/mysql84/`），端口 3306，库名 `demon_db`。
所有脚本必须用 **`--default-character-set=utf8mb4`** 导入，否则中文会变成乱码。

## 一、全新安装（推荐，一步到位）

仓库根目录 `demon_db.sql` 是**全量快照**：23 张表的结构 + 全部数据，
已包含典册 v3/v4（46 条）、23 部功法及配图路径、10 件藏宝阁道具、鬼市/双币/社交/魔窟各表，
以及 **v1 数值平衡**后的价格与 `demon_den` 每日结算记账列（见 `db/BALANCE.md`）。
`魔族修行登录-Vue项目/demon_db.sql` 是同一份副本，两份保持一致。

```bash
mysql -uroot -p<口令> --default-character-set=utf8mb4 -e \
  "CREATE DATABASE IF NOT EXISTS demon_db CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
mysql -uroot -p<口令> --default-character-set=utf8mb4 demon_db < demon_db.sql
```

快照自带 `DROP TABLE IF EXISTS`，可重复导入（会覆盖同名表，等价于重建）。
快照**不含** `CREATE DATABASE`，所以必须指定库名 `-D demon_db`。
本机便携版 MySQL 不是 Windows 服务，重启电脑后需先手动启动
`C:/Users/czh10/mysql84/server/bin/mysqld.exe`（或自己的启动脚本），再执行上述命令。

## 二、旧库升级（保留玩家数据时按此顺序）

仅在**不能重建库**时使用。顺序由表/列依赖决定，乱序会报 Unknown column 或丢数据。

| 阶段 | 顺序 | 脚本 | 作用 / 注意 |
|---|---|---|---|
| 1 基础 | 1 | `demon_init.sql` | `demon_user` 主表（CREATE IF NOT EXISTS） |
| | 2 | `demon_realm.sql` | 十境配置表，后端启动时由 `RealmConfigLoader` 读取 |
| | 3 | `demon_content.sql` | **DROP + CREATE** 5 张静态内容表（art / trial / debate / codex / announcement），会清空这 5 张表 |
| | 4 | `demon_archive.sql` | **DROP + CREATE** 典册表，会清空后台手改过的条目 |
| 2 用户列 | 5 | `demon_user_upgrade.sql` | 修行/每日参悟相关列 |
| | 6 | `demon_user_spar.sql` | 每日切磋计数列 |
| | 7 | `demon_user_permission.sql` | `permission`（0 管理员 / 1 普通），并把魔祖设为管理员 |
| | 8 | `demon_user_avatar.sql` | `avatar VARCHAR(255)`，脚本里 `AFTER permission`，**必须在 7 之后** |
| 3 玩法 | 9 | `demon_gameplay.sql` | 论道防刷 + 魔种天赋表 |
| | 10 | `demon_gameplay_v2.sql` | 煞气语义改为累计修为、破境不再扣气 |
| | 11 | `demon_explore_log.sql` | 参悟日志表（每篇每日一次） |
| | 12 | `demon_secret_realm.sql` | 给 `demon_trial` 加 boss / 入场费 / 每日次数等列 |
| | 13 | `demon_debate_quiz.sql` | 给 `demon_debate` 加选项 / 答案 / 解析列 |
| 4 题库 | 14 | `seed_debate_more.sql` | 补 12 道论题，凑满 20 道（按 title 判重） |
| | 15 | `fix_debate_answers.sql` | 校正答案 A/B/C 分布，**只能执行一次**（重复跑会再次交换） |
| 5 功法 | 16 | `demon_cultivate.sql` | 给 `demon_art` 加阶位 / 售价 / 数值加成列 |
| | 17 | `demon_art2.sql` | 完整 / 残片体系、六重进度、心魔 |
| | 18 | `demon_images.sql` | 给功法 / 秘录 / 历练录加 `image` 列 |
| | 19 | `demon_art_catalog.sql` | **功法目录唯一权威来源**：删目录外功法（连带清理 `demon_user_art`）、重排 id 1..23、写一功一图 |
| | 20 | `demon_art_renumber.sql` | 可选；catalog 已含重排，仅用于单独校正主键 |
| 6 货币交易 | 21 | `demon_coin.sql` | 钱包 / 流水 / 道具 / 背包 / 购买记录 5 张表 |
| | 22 | `demon_trade.sql` | 鬼市挂单表 + 道具折扣列（依赖 `demon_item`） |
| 7 社交挂机 | 23 | `demon_social.sql` | 好友 / 私信两表 |
| | 24 | `demon_den.sql` | 魔窟洞府表（煞气 / 魔晶 / 血玉挂机产出） |
| 8 典册 | 25 | `demon_archive_v2.sql` | 旧版设定对齐 |
| | 26 | `demon_archive_v3.sql` | 现行版本：修订 10 条 + 新增 11 条新玩法条目 |
| 9 种子 | 27 | `seed_demo.sql` | 重插演示管理员「魔祖」（明文口令、`permission=0`） |
| | 28 | `seed_characters.sql` | 各境界演示修者 |
| | 29 | `seed_elite.sql` | 魔祖境翘楚 |
| **10 数值平衡** | **30** | `demon_balance_v1.sql` | **最后执行**：给 `demon_den` 加每日结算记账两列、藏宝阁降价、功法血玉价同步；带旧值守卫，可重复跑 |
| | **31** | `demon_archive_v4.sql` | 典册 9 条正文的硬编码数字改引用式，并写明每日封顶；不增删条目 |

> 注意：21 号 `demon_coin.sql` 与 19 号 `demon_art_catalog.sql` 写的是**旧版价格**，
> 必须靠 30 号脚本覆盖，顺序不能提前。全新安装走全量快照则无需跑 30/31。

## 三、踩过的坑

- **MySQL 8.4 不支持 `ADD COLUMN IF NOT EXISTS`**（那是 MariaDB 语法），加列必须用
  `information_schema` 判断 + `PREPARE` 动态执行；列重名报错可忽略的脚本已注明。
- `demon_content.sql` 用 DROP + CREATE，升级流程里重跑会**清空功法表**，之后必须补跑 16~20 号脚本。
- `demon_art_catalog.sql` 会删除目录之外的功法并清理对应 `demon_user_art`；重排映射临时表的
  `name` 列必须显式写 `COLLATE utf8mb4_0900_ai_ci`，否则报 `ERROR 1267 Illegal mix of collations`。
- `demon_user` 的 `read` 是保留字，`Message.read` 字段在实体里要写成 ``@TableField("`read`")``。
- MyBatis-Plus 的 `updateById` 会跳过 null 字段，清空 `avatar` 必须用
  `UpdateWrapper.set("avatar", null)`。
- `seed_demo.sql` 重插魔祖会把口令重置为 6 位明文；后端 `AuthService.verifyPassword`
  在首次登录成功后自动把明文升级成 PBKDF2 哈希，玩家端与后台登录都走
  `PasswordEncoder.matches`，两种存储格式都能登。
- 校验是否重建成功：
  `SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA='demon_db';` 应为 **23**；
  `demon_archive` 46 条、`demon_art` 23 条、`demon_item` 10 条、`demon_debate` 20 条；
  `SHOW COLUMNS FROM demon_den LIKE 'collect%';` 应有 `collect_date` / `collected_sec` 两列。

## 四、数值平衡

**`db/BALANCE.md` 是数值的唯一事实依据**：每日产出预算、挂机占比、血玉收支、
常量索引与改动流程全在那里。典册正文已改为引用式，改数值时**不需要**再改典册，
但每次改完必须：同步 Java 常量 → 写幂等升级脚本 → 编译/冒烟 → 重导根目录
`demon_db.sql` 与前端副本 → 提交。
