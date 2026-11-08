# 魔族修行 · 数据库脚本执行说明

环境：MySQL 8.4（本机便携版 `C:/Users/czh10/mysql84/`），端口 3306，库名 `demon_db`。
所有脚本必须用 **`--default-character-set=utf8mb4`** 导入，否则中文会变成乱码。

## 一、全新安装（推荐，一步到位）

仓库根目录 `demon_db.sql` 是**全量快照**：**28 张表**的结构 + 全部数据，
已包含典册 v3/v4/v5 + 道途篇（47 条）、23 部功法及配图路径、10 件藏宝阁道具、鬼市/双币/社交/魔窟/道途成就各表，
**v1 数值平衡**后的价格与 `demon_den` 每日结算记账列，以及 **v1.1 每日循环**
（论道/研读按日唯一键、`demon_item_use` 用量表、`daily_use_limit` 配置，见 `db/BALANCE.md` 第七节），
以及**破关之钥文案修订**（两件秘宝不可开囊、丹符说明与实得数值一致），
以及**道途成就**（`demon_achievement` 25 条 + `demon_user_achievement` 解锁账，纯荣誉不发资源），
以及**自定义头像图片本体**（`demon_avatar_file` 表，base64 存库），
以及**入魔道新手期**（`demon_newbie` 状态表、`demon_trial.newbie_only` 列与 3 处入门试炼、
典册 31/33 封禁律修订；数值口径见 `db/BALANCE.md` 第九节）。
> 因此快照里**自带所有上传过的头像**：换一台电脑只要导入本快照，即使 `uploads/` 目录为空，
> 英杰榜 / 道牌 / 传音里的头像也能正常显示。图片由 `GET /uploads/**` 提供（库优先、磁盘兜底），
> 前端用同源相对路径取图，跨机器访问也不会指向访问者自己的 `localhost`。
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
| **10 数值平衡** | **30** | `demon_balance_v1.sql` | 给 `demon_den` 加每日结算记账两列、藏宝阁降价、功法血玉价同步；带旧值守卫，可重复跑 |
| | **31** | `demon_archive_v4.sql` | 典册硬编码数字改引用式，并写明每日封顶；不增删条目 |
| **11 每日循环（v1.1）** | **32** | `demon_daily_v1.sql` | 给 quiz/read 两表加日期列并按日重建唯一键、建 `demon_item_use`、定 `daily_use_limit` 初值；带 `-1` 守卫，可重复跑 |
| | **33** | `demon_archive_v5.sql` | 典册 4 条正文同步「每日可重复 / 收益递减 / 每日限用」；不增删条目 |
| **12 破关之钥** | **34** | `demon_item_key_fix.sql` | 两件秘宝与无上道果说明对齐实得、典册 37/41 写明「破关之钥不可开囊」；REPLACE 幂等，可重复跑 |
| **13 道途成就** | **35** | `demon_achievement.sql` | 建 `demon_achievement` / `demon_user_achievement` 两表，25 条种子成就，典册补「道途与功行」；`INSERT IGNORE` 幂等，不会覆盖后台手改 |
| **14 头像图片入库** | **36** | `demon_avatar_file.sql` | 建 `demon_avatar_file`（图片本体 base64）；`CREATE TABLE IF NOT EXISTS` 幂等。已有库跑完还需在 `springBootTest/` 目录执行一次 `python migrate_avatars.py`，把 `uploads/avatars/` 里的历史图片补录进表（可重复跑） |
| **15 入魔道（v1.2）** | **37** | `demon_newbie.sql` | **最后执行**：建 `demon_newbie` 状态表、给 `demon_trial` 加 `newbie_only` 列并插 3 处引煞入门试炼（id 10~12）、整段改写典册 33 封禁律并补 31 秘境律；`CREATE TABLE IF NOT EXISTS` + `information_schema` 判断加列 + `NOT EXISTS` 判重 + 文案守卫词，全幂等。玩家端数值在 `common/NewbieRule.java`，不由本脚本控制 |

> 注意：21 号 `demon_coin.sql` 与 19 号 `demon_art_catalog.sql` 写的是**旧版价格**，
> 必须靠 30 号脚本覆盖，顺序不能提前。9 号 `demon_gameplay.sql` / 10 号 `demon_gameplay_v2.sql`
> 已同步按日唯一键的定义，但**已存在的旧库**仍须靠 32 号脚本 ALTER 换键。
> 全新安装走全量快照则无需跑 30~37，也无需跑迁移脚本（快照里已含图片）。

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
- **MySQL 唯一键含可为 NULL 的列时不生效**（NULL 互不相等）：32 号脚本给
  `quiz_date` / `read_date` 回填历史日期后必须 `MODIFY ... NOT NULL`，否则按日判重会失效。
- 校验是否重建成功：
  `SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA='demon_db';` 应为 **28**；
  `demon_archive` 47 条、`demon_art` 23 条、`demon_item` 10 条、`demon_debate` 20 条、
  `demon_achievement` 25 条、`demon_avatar_file` 至少 9 条（历史上传头像）、
  `demon_trial` 12 条（9 处正境 + 3 处 `newbie_only=1` 入门试炼）、`demon_newbie` 表存在；
  `SHOW COLUMNS FROM demon_den LIKE 'collect%';` 应有 `collect_date` / `collected_sec` 两列；
  `SHOW INDEX FROM demon_quiz_record;` 应有 `uk_user_debate_day` 而无 `uk_user_debate`，
  `SHOW INDEX FROM demon_read_record;` 应有 `uk_user_ref_day` 而无 `uk_user_ref`。

## 四、数值平衡

**`db/BALANCE.md` 是数值的唯一事实依据**：每日产出预算、挂机占比、血玉收支、
常量索引与改动流程全在那里；**第七节**另记 v1.1 的递减曲线与道具每日使用上限。
典册正文已改为引用式，改数值时**不需要**再改典册，
但每次改完必须：同步 Java 常量 → 写幂等升级脚本 → 编译/冒烟 → 重导根目录
`demon_db.sql` 与前端副本 → 提交。
