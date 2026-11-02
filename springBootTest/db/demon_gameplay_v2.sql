-- ============================================================
-- 魔族修行 · 玩法 v2 升级脚本（幂等，可重复执行）
--
-- v2 核心变更：
--   1) 煞气语义改为「累计总修为」——冲境只判达标、不再扣除，
--      彻底消除旧版「越突破战力越低、不如囤气霸榜」的畸形最优解
--   2) 战力公式改为境界主导：战力 = 境阶 × 8000 + 煞气
--   3) 新增签到 / 战绩 / 渡劫 / 转生 / 研读 / 习得 所需字段与表
--   4) 四魔种重新平衡
--
-- 执行：mysql -uroot -p123456 demon_db < db/demon_gameplay_v2.sql
-- 注意：MySQL 8.4 不支持 ADD COLUMN IF NOT EXISTS，故用 information_schema 判断
-- ============================================================

USE demon_db;

-- ========== 1. demon_user 新增玩法字段 ==========
SET @s := (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE demon_user ADD COLUMN sign_date date NULL COMMENT ''最后签到日期''',
  'SELECT 1') FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'demon_user' AND COLUMN_NAME = 'sign_date');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s := (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE demon_user ADD COLUMN sign_streak int NOT NULL DEFAULT 0 COMMENT ''连续签到天数''',
  'SELECT 1') FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'demon_user' AND COLUMN_NAME = 'sign_streak');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s := (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE demon_user ADD COLUMN sign_total int NOT NULL DEFAULT 0 COMMENT ''累计签到天数''',
  'SELECT 1') FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'demon_user' AND COLUMN_NAME = 'sign_total');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s := (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE demon_user ADD COLUMN spar_win int NOT NULL DEFAULT 0 COMMENT ''切磋胜场''',
  'SELECT 1') FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'demon_user' AND COLUMN_NAME = 'spar_win');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s := (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE demon_user ADD COLUMN spar_lose int NOT NULL DEFAULT 0 COMMENT ''切磋负场''',
  'SELECT 1') FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'demon_user' AND COLUMN_NAME = 'spar_lose');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s := (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE demon_user ADD COLUMN break_fail int NOT NULL DEFAULT 0 COMMENT ''连续渡劫失败次数''',
  'SELECT 1') FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'demon_user' AND COLUMN_NAME = 'break_fail');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s := (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE demon_user ADD COLUMN reincarnate int NOT NULL DEFAULT 0 COMMENT ''转生次数（第几世）''',
  'SELECT 1') FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'demon_user' AND COLUMN_NAME = 'reincarnate');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s := (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE demon_user ADD COLUMN read_date date NULL COMMENT ''最后研读日期''',
  'SELECT 1') FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'demon_user' AND COLUMN_NAME = 'read_date');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s := (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE demon_user ADD COLUMN read_count_today int NOT NULL DEFAULT 0 COMMENT ''当日已研读篇数''',
  'SELECT 1') FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'demon_user' AND COLUMN_NAME = 'read_count_today');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- ========== 2. 境界阈值改为「累计达标制」 ==========
UPDATE demon_realm SET threshold = CASE idx
  WHEN 0 THEN 100   WHEN 1 THEN 400   WHEN 2 THEN 1200  WHEN 3 THEN 3000  WHEN 4 THEN 6000
  WHEN 5 THEN 10000 WHEN 6 THEN 16000 WHEN 7 THEN 24000 WHEN 8 THEN 35000 WHEN 9 THEN 50000
END WHERE idx BETWEEN 0 AND 9;

-- 存量修者：煞气至少补齐到当前境界对应的累计阈值（保证「境界 ≥ 煞气里程碑」语义自洽）
UPDATE demon_user u
  JOIN demon_realm r ON r.name = u.cultivation
   SET u.exp = GREATEST(COALESCE(u.exp, 0), r.threshold)
 WHERE r.name IS NOT NULL;

-- ========== 3. 四魔种重新平衡 ==========
-- 战力改为境界主导后，+12% 战力 ≈ 一重境以上，过强；下调为 5%（约半境）
UPDATE demon_origin SET spar_power_bonus = 0.05 WHERE origin = '血煞';
-- 幽冥稳定 +50% 参悟过强，下调至 35%，与虚妄（期望 +30%）拉平
UPDATE demon_origin SET explore_bonus = 0.35 WHERE origin = '幽冥';
-- 虚妄顿悟概率 25% → 30%
UPDATE demon_origin SET insight_chance = 0.30 WHERE origin = '虚妄';
-- 噬魂「夺魂摄魄」维持胜利奖励翻倍
UPDATE demon_origin SET spar_reward_bonus = 1.00 WHERE origin = '噬魂';

-- ========== 4. 研读记录（图鉴收集度） ==========
CREATE TABLE IF NOT EXISTS demon_read_record (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id    BIGINT       NOT NULL,
  module     VARCHAR(20)  NOT NULL COMMENT 'art/trial/debate/codex/archive',
  ref_id     BIGINT       NOT NULL,
  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_ref (user_id, module, ref_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='修者研读记录（图鉴收集度）';

-- ========== 5. 功法习得 ==========
CREATE TABLE IF NOT EXISTS demon_user_art (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id    BIGINT   NOT NULL,
  art_id     BIGINT   NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_art (user_id, art_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='修者已习得功法';
