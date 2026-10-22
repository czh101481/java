-- ============================================================
-- 魔族修行 · 内容配图初始化
-- 说明：为 功法阁(demon_art) / 魔典秘录(demon_codex) / 历练录(demon_trial)
--       追加 image 列，并按编号写入静态图片路径。
--       图片位于后端 springBootTest/src/main/resources/static/images/{art,codex,trial}/
--       前端展示时拼接后端基地址：http://localhost:8081 + image
-- 特点：幂等，可重复执行（列已存在则跳过，UPDATE 覆盖写）。
-- 导入注意：务必使用 --default-character-set=utf8mb4
-- ============================================================

USE demon_db;

-- ---------- 1. 追加 image 列（MySQL 8 无 ADD COLUMN IF NOT EXISTS，用存储过程保证幂等） ----------
DROP PROCEDURE IF EXISTS demon_add_image_col;
DELIMITER $$
CREATE PROCEDURE demon_add_image_col(IN tbl VARCHAR(64))
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'demon_db' AND TABLE_NAME = tbl AND COLUMN_NAME = 'image'
  ) THEN
    SET @ddl = CONCAT('ALTER TABLE ', tbl, ' ADD COLUMN image VARCHAR(255) DEFAULT '''' COMMENT ''配图相对路径''');
    PREPARE stmt FROM @ddl;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END$$
DELIMITER ;

CALL demon_add_image_col('demon_art');
CALL demon_add_image_col('demon_codex');
CALL demon_add_image_col('demon_trial');
DROP PROCEDURE IF EXISTS demon_add_image_col;

-- ---------- 2. 按编号写入路径（编号与 id 一一对应） ----------
-- 功法阁 art-1.jpg ~ art-12.jpg
UPDATE demon_art SET image = CONCAT('/images/art/art-', id, '.jpg');

-- 魔典秘录 codex-1.jpg ~ codex-9.jpg
UPDATE demon_codex SET image = CONCAT('/images/codex/codex-', id, '.jpg');

-- 历练录 trial-1.jpg ~ trial-9.jpg
UPDATE demon_trial SET image = CONCAT('/images/trial/trial-', id, '.jpg');

-- ---------- 3. 校验 ----------
SELECT id, name, image FROM demon_art ORDER BY id LIMIT 3;
SELECT id, title, image FROM demon_trial ORDER BY id LIMIT 3;
