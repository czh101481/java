-- 魔族修行 · demon_user 升级脚本（修者切磋：每日切磋次数上限）
-- 对应实体字段：DemonUser.sparDate / DemonUser.sparCountToday
-- 幂等：列已存在则跳过，不会报 Duplicate column
USE demon_db;

-- 补 spar_date（最后切磋日期，跨日清零用）
SET @exist := (SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = 'demon_db' AND table_name = 'demon_user' AND column_name = 'spar_date');
SET @sql := IF(@exist = 0,
  "ALTER TABLE demon_user ADD COLUMN spar_date DATE DEFAULT NULL COMMENT '最后切磋日期（跨日清零）' AFTER exp_count_today",
  "SELECT 'spar_date already exists' AS info");
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 补 spar_count_today（当日已切磋次数）
SET @exist := (SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = 'demon_db' AND table_name = 'demon_user' AND column_name = 'spar_count_today');
SET @sql := IF(@exist = 0,
  "ALTER TABLE demon_user ADD COLUMN spar_count_today INT DEFAULT 0 COMMENT '当日已切磋次数' AFTER spar_date",
  "SELECT 'spar_count_today already exists' AS info");
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 老数据兜底：已有修者的切磋计数归零
UPDATE demon_user SET spar_count_today = 0 WHERE spar_count_today IS NULL;

-- 校验
SELECT column_name, column_type, column_comment
FROM information_schema.columns
WHERE table_schema = 'demon_db' AND table_name = 'demon_user'
  AND column_name IN ('spar_date', 'spar_count_today');
