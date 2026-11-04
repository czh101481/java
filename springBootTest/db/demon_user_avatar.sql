-- 魔族修行 · demon_user 升级脚本（头像字段）
-- 幂等说明：先用 information_schema 判断列是否已存在，避免重复 ALTER 报错。
-- MySQL 8.4 不支持 ADD COLUMN IF NOT EXISTS（那是 MariaDB 语法），故用 PREPARE 动态执行。
-- avatar 存两种值：预设头像 key（如 blood）或上传头像 URL（如 /uploads/avatars/3_1699.jpg），故用 VARCHAR(255)。
USE demon_db;

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'demon_db'
    AND TABLE_NAME   = 'demon_user'
    AND COLUMN_NAME  = 'avatar'
);

SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE demon_user ADD COLUMN avatar VARCHAR(255) DEFAULT NULL COMMENT ''头像：预设 key 或 /uploads/ 路径'' AFTER permission',
  'SELECT ''avatar 列已存在，跳过新增'' AS msg');

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 兼容早期建为 VARCHAR(32) 的库：统一加宽到 255（MODIFY 幂等，重复执行无副作用）
ALTER TABLE demon_user
  MODIFY COLUMN avatar VARCHAR(255) DEFAULT NULL COMMENT '头像：预设 key 或 /uploads/ 路径';
