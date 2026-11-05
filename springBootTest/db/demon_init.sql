-- =====================================================================
-- 魔族修行 · 数据库初始化脚本
-- 后端：springBootTest（Spring Boot 3 + MyBatis-Plus + MySQL 8）
-- 前端：魔族修行登录 Vue 项目（Vue 3 + Vite）
-- 库名：demon_db    账号：root / 123456    端口：3306
--
-- 执行方式（任选其一）：
--   1) 命令行：        mysql -uroot -p123456 < db/demon_init.sql
--   2) IDEA Terminal： mysql -uroot -p123456 < db/demon_init.sql
--   3) IDEA / Navicat / DataGrip 的 Database 工具中直接打开本文件运行
--
-- 说明：本脚本幂等，可重复执行，不会删除已有数据。
-- =====================================================================

-- 创建数据库（若不存在）
CREATE DATABASE IF NOT EXISTS demon_db
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_general_ci;

-- 使用数据库
USE demon_db;

-- ---------------------------------------------------------------------
-- 魔族修者表
--   · 字段与后端实体 com.fenglin.springboottest.entity.DemonUser 一一对应
--   · 密码字段为演示明文（与前端约定一致），生产环境务必加密存储
--   · uk_name / uk_email 保证道号、邮箱唯一，注册时后端会做查重
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS demon_user (
  id          BIGINT       NOT NULL AUTO_INCREMENT            COMMENT '主键',
  name        VARCHAR(50)  NOT NULL                          COMMENT '修者道号',
  email       VARCHAR(100) NOT NULL                          COMMENT '灵识印记（邮箱）',
  realm       VARCHAR(20)  NOT NULL                          COMMENT '魔种：血煞/幽冥/噬魂/虚妄',
  cultivation VARCHAR(20)  DEFAULT '引煞'                    COMMENT '修为境界：引煞/凝煞/魔卒/魔尉/魔将/血煞/魔祖',
  exp         INT          DEFAULT 0                         COMMENT '煞气修为',
  exp_date    DATE         DEFAULT NULL                      COMMENT '最后参悟日期（跨日清零）',
  exp_count_today INT      DEFAULT 0                         COMMENT '当日已参悟次数',
  explore_bonus_today INT  NOT NULL DEFAULT 0                COMMENT '当日额外参悟次数（道具加成）',
  explore_bonus_date  DATE DEFAULT NULL                      COMMENT '额外参悟次数所属日期（跨日清零）',
  password    VARCHAR(100) NOT NULL                          COMMENT '护法密令（演示为明文，生产请加密）',
  permission  TINYINT      DEFAULT 1                         COMMENT '权限：1 默认权限 / 0 管理员权限',
  avatar      VARCHAR(255) DEFAULT NULL                      COMMENT '头像：预设 key 或 /uploads/ 路径',
  created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP          COMMENT '入籍时间',
  updated_at  DATETIME     DEFAULT CURRENT_TIMESTAMP
                          ON UPDATE CURRENT_TIMESTAMP        COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_name  (name),
  UNIQUE KEY uk_email (email)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci
  COMMENT = '魔族修者表';

-- ---------------------------------------------------------------------
-- 可选：预置一个演示修者，方便不注册也能先试登录
--   道号：mo ZuShi（魔祖）   邮箱：demo@demon.com   密令：123456
-- ---------------------------------------------------------------------
INSERT IGNORE INTO demon_user (name, email, realm, password, permission)
VALUES ('魔祖', 'demo@demon.com', '血煞', '123456', 0);

-- ---------------------------------------------------------------------
-- 下一步：建「修行境界表」（十境配置）
--   执行 db/demon_realm.sql —— 建 demon_realm 表并写入引煞→无上十境。
--   后端启动时会从该表加载境界；表不存在时自动回退代码内置默认值。
--   改境界只需改表，改完调 POST /api/admin/realm/reload 热重载（需管理员令牌）。
-- ---------------------------------------------------------------------

-- ---------------------------------------------------------------------
-- 再下一步：建「玩法层」（论道防刷 + 魔种天赋）
--   执行 db/demon_gameplay.sql ——
--     · demon_user 补 quiz_date / quiz_count_today（每日论道上限）
--     · 建 demon_quiz_record（同一人同一题只算一次，堵住刷分漏洞）
--     · 建 demon_origin（血煞/幽冥/噬魂/虚妄 四系天赋，参悟与切磋按系数生效）
--   可选补充内容：db/seed_debate_more.sql（论题补到 20 道）
--                db/fix_debate_answers.sql（校正答案 A/B/C 分布，只执行一次）
-- ---------------------------------------------------------------------

