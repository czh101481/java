-- =====================================================================
-- 魔族修行 · v1.1 每日循环升级脚本
--   1) 论道 / 研读：从「终身一次」改为「同一题/篇每日一次」，跨日可重复；
--      当日超额部分按 50% → 25% → 0% 递减（公式在 common/DailyDecay.java）。
--   2) 道具：新增「每日使用上限」，覆盖 /api/coin/use 与功法破关接口，
--      杜绝提前囤积后一天内集中用完。
--
-- 定案与数值：详见 db/BALANCE.md 第七节。
-- 幂等：
--   - 加列用 information_schema 判断（MySQL 8.4 不支持 ADD COLUMN IF NOT EXISTS）；
--   - 唯一键变更按索引名判断，可重复执行；
--   - daily_use_limit 用「-1 = 未初始化」守卫，只写一次；执印人日后在后台
--     手改（含改成 0 = 不限）都不会被本脚本覆盖。
--   - 历史答题 / 研读行按 created_at 回填日期，保证新唯一键不冲突。
--
-- 执行：mysql -uroot -p --default-character-set=utf8mb4 demon_db < 本文件
-- 顺序：全新安装用根目录全量快照 demon_db.sql（已含本脚本结果，无需再跑）；
--       旧库升级按 db/README.md 第 32 号执行。
-- =====================================================================
USE demon_db;

-- ---------------------------------------------------------------------
-- 一、新增三列
-- ---------------------------------------------------------------------
DROP PROCEDURE IF EXISTS demon_daily_v1_cols;
DELIMITER $$
CREATE PROCEDURE demon_daily_v1_cols()
BEGIN
  IF NOT EXISTS (
      SELECT 1 FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'demon_quiz_record'
        AND COLUMN_NAME = 'quiz_date') THEN
    ALTER TABLE demon_quiz_record
      ADD COLUMN quiz_date DATE NULL
      COMMENT '作答日期（同一题跨日可再辩）' AFTER debate_id;
  END IF;

  IF NOT EXISTS (
      SELECT 1 FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'demon_read_record'
        AND COLUMN_NAME = 'read_date') THEN
    ALTER TABLE demon_read_record
      ADD COLUMN read_date DATE NULL
      COMMENT '研读日期（同一篇跨日可再读）' AFTER ref_id;
  END IF;

  IF NOT EXISTS (
      SELECT 1 FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'demon_item'
        AND COLUMN_NAME = 'daily_use_limit') THEN
    ALTER TABLE demon_item
      ADD COLUMN daily_use_limit INT NOT NULL DEFAULT -1
      COMMENT '每日使用上限：-1 未初始化 / 0 不限 / >0 件数' AFTER daily_limit;
  END IF;
END$$
DELIMITER ;
CALL demon_daily_v1_cols();
DROP PROCEDURE demon_daily_v1_cols;

-- 历史行回填日期（新唯一键加入日期列前必须完成，否则同人同题跨日会撞键）
UPDATE demon_quiz_record SET quiz_date = DATE(created_at) WHERE quiz_date IS NULL;
UPDATE demon_read_record SET read_date = DATE(created_at) WHERE read_date IS NULL;

-- ---------------------------------------------------------------------
-- 二、日期列改 NOT NULL + 唯一键按日重建
-- ---------------------------------------------------------------------
DROP PROCEDURE IF EXISTS demon_daily_v1_idx;
DELIMITER $$
CREATE PROCEDURE demon_daily_v1_idx()
BEGIN
  IF EXISTS (
      SELECT 1 FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'demon_quiz_record'
        AND COLUMN_NAME = 'quiz_date' AND IS_NULLABLE = 'YES') THEN
    ALTER TABLE demon_quiz_record
      MODIFY quiz_date DATE NOT NULL COMMENT '作答日期（同一题跨日可再辩）';
  END IF;

  IF EXISTS (
      SELECT 1 FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'demon_read_record'
        AND COLUMN_NAME = 'read_date' AND IS_NULLABLE = 'YES') THEN
    ALTER TABLE demon_read_record
      MODIFY read_date DATE NOT NULL COMMENT '研读日期（同一篇跨日可再读）';
  END IF;

  IF EXISTS (
      SELECT 1 FROM information_schema.STATISTICS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'demon_quiz_record'
        AND INDEX_NAME = 'uk_user_debate') THEN
    ALTER TABLE demon_quiz_record DROP INDEX uk_user_debate;
  END IF;
  IF NOT EXISTS (
      SELECT 1 FROM information_schema.STATISTICS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'demon_quiz_record'
        AND INDEX_NAME = 'uk_user_debate_day') THEN
    ALTER TABLE demon_quiz_record
      ADD UNIQUE KEY uk_user_debate_day (user_id, debate_id, quiz_date);
  END IF;

  IF EXISTS (
      SELECT 1 FROM information_schema.STATISTICS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'demon_read_record'
        AND INDEX_NAME = 'uk_user_ref') THEN
    ALTER TABLE demon_read_record DROP INDEX uk_user_ref;
  END IF;
  IF NOT EXISTS (
      SELECT 1 FROM information_schema.STATISTICS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'demon_read_record'
        AND INDEX_NAME = 'uk_user_ref_day') THEN
    ALTER TABLE demon_read_record
      ADD UNIQUE KEY uk_user_ref_day (user_id, module, ref_id, read_date);
  END IF;
END$$
DELIMITER ;
CALL demon_daily_v1_idx();
DROP PROCEDURE demon_daily_v1_idx;

-- ---------------------------------------------------------------------
-- 三、道具每日用量表（按人 + 物 + 日聚合，跨日自动另起一行）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS demon_item_use (
  id         BIGINT NOT NULL AUTO_INCREMENT,
  user_id    BIGINT NOT NULL COMMENT '修者 id',
  item_id    BIGINT NOT NULL COMMENT '物品 id',
  use_date   DATE   NOT NULL COMMENT '使用日期（跨日清零）',
  qty        INT    NOT NULL DEFAULT 0 COMMENT '当日已取用件数',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_use_day (user_id, item_id, use_date),
  KEY idx_use_user (user_id, use_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '道具每日使用量（防囤积爆发）';

-- ---------------------------------------------------------------------
-- 四、初始每日使用上限：默认等于每日限购数；不限购的两枚破关钥匙设 3/日
--     -1 守卫：只在「未初始化」时写入，重跑或后台手改都不会被覆盖。
-- ---------------------------------------------------------------------
UPDATE demon_item SET daily_use_limit = 5 WHERE code = 'gather_pill'    AND daily_use_limit = -1;
UPDATE demon_item SET daily_use_limit = 3 WHERE code = 'blood_pill'     AND daily_use_limit = -1;
UPDATE demon_item SET daily_use_limit = 2 WHERE code = 'focus_talisman' AND daily_use_limit = -1;
UPDATE demon_item SET daily_use_limit = 3 WHERE code = 'treasure_pouch' AND daily_use_limit = -1;
UPDATE demon_item SET daily_use_limit = 2 WHERE code = 'realm_compass'  AND daily_use_limit = -1;
UPDATE demon_item SET daily_use_limit = 1 WHERE code = 'immortal_dew'   AND daily_use_limit = -1;
UPDATE demon_item SET daily_use_limit = 1 WHERE code = 'abyss_orb'      AND daily_use_limit = -1;
UPDATE demon_item SET daily_use_limit = 1 WHERE code = 'supreme_fruit'  AND daily_use_limit = -1;
UPDATE demon_item SET daily_use_limit = 3 WHERE code = 'insight_fate'   AND daily_use_limit = -1;
UPDATE demon_item SET daily_use_limit = 3 WHERE code = 'death_battle'   AND daily_use_limit = -1;

-- ---------------------------------------------------------------------
-- 五、校验（手动执行查看）
--   SHOW COLUMNS FROM demon_quiz_record LIKE '%date%';
--   SHOW INDEX FROM demon_quiz_record;   -- 应有 uk_user_debate_day，无 uk_user_debate
--   SHOW INDEX FROM demon_read_record;   -- 应有 uk_user_ref_day，无 uk_user_ref
--   SELECT code,name,daily_limit,daily_use_limit FROM demon_item ORDER BY sort,id;
--   SELECT COUNT(*) FROM information_schema.TABLES
--     WHERE TABLE_SCHEMA='demon_db';     -- 升级后应为 24 张表
-- ---------------------------------------------------------------------
