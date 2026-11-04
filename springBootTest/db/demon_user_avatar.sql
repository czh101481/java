-- 魔族修行 · demon_user 升级脚本（头像字段）
-- 幂等说明：先用 information_schema 判断列是否已存在，避免重复 ALTER 报错。
-- MySQL 8.4 不支持 ADD COLUMN IF NOT EXISTS（那是 MariaDB 语法），故用 PREPARE 动态执行。
USE demon_db;

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'demon_db'
    AND TABLE_NAME   = 'demon_user'
    AND COLUMN_NAME  = 'avatar'
);

SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE demon_user ADD COLUMN avatar VARCHAR(32) DEFAULT NULL COMMENT ''头像代号（前端预设头像 key）'' AFTER permission',
  'SELECT ''avatar 列已存在，跳过'' AS msg');

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
