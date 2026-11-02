-- ============================================================
-- 魔族修行 · 域外秘境升级脚本（幂等，可重复执行）
--
-- 变更要点：
--   1) demon_trial 由「历练录（静态词条）」升级为「域外秘境（可进入的副本）」
--      新增：守关妖魔 boss / 入场消耗 cost_exp / 每日次数 daily_limit /
--            产出区间 reward_min ~ reward_max / 凶险率 risk_rate / 机缘率 treasure_rate
--   2) 新增 demon_realm_log：秘境探索日志（每日次数统计 + 战报回看）
--   3) 按「凶险」自动回填预设数值：
--        低：入场 0  / 每日 5 次 / 产出 10~30  / 凶险 10% / 机缘  8%
--        中：入场 20 / 每日 3 次 / 产出 30~70  / 凶险 20% / 机缘 12%
--        高：入场 60 / 每日 2 次 / 产出 80~160 / 凶险 30% / 机缘 18%
--        绝：入场 150/ 每日 1 次 / 产出 200~400/ 凶险 45% / 机缘 25%
--
-- 执行：mysql -uroot -p demon_db < db/demon_secret_realm.sql
-- 注意：MySQL 8.4 不支持 ADD COLUMN IF NOT EXISTS，故用 information_schema 判断
-- ============================================================

USE demon_db;

-- Windows 控制台默认 gbk，中文常量与 utf8mb4 列比较会报 Illegal mix of collations，故显式声明
SET NAMES utf8mb4;

-- ========== 1. demon_trial 新增玩法字段 ==========
SET @s := (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE demon_trial ADD COLUMN boss varchar(40) DEFAULT '''' COMMENT ''守关妖魔''',
  'SELECT 1') FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'demon_trial' AND COLUMN_NAME = 'boss');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s := (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE demon_trial ADD COLUMN cost_exp int NOT NULL DEFAULT 0 COMMENT ''入场消耗煞气''',
  'SELECT 1') FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'demon_trial' AND COLUMN_NAME = 'cost_exp');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s := (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE demon_trial ADD COLUMN daily_limit int NOT NULL DEFAULT 3 COMMENT ''每日可入次数''',
  'SELECT 1') FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'demon_trial' AND COLUMN_NAME = 'daily_limit');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s := (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE demon_trial ADD COLUMN reward_min int NOT NULL DEFAULT 20 COMMENT ''产出煞气下限''',
  'SELECT 1') FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'demon_trial' AND COLUMN_NAME = 'reward_min');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s := (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE demon_trial ADD COLUMN reward_max int NOT NULL DEFAULT 60 COMMENT ''产出煞气上限''',
  'SELECT 1') FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'demon_trial' AND COLUMN_NAME = 'reward_max');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s := (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE demon_trial ADD COLUMN risk_rate int NOT NULL DEFAULT 20 COMMENT ''凶险概率(百分比)''',
  'SELECT 1') FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'demon_trial' AND COLUMN_NAME = 'risk_rate');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s := (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE demon_trial ADD COLUMN treasure_rate int NOT NULL DEFAULT 15 COMMENT ''机缘概率(百分比)''',
  'SELECT 1') FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'demon_trial' AND COLUMN_NAME = 'treasure_rate');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- ========== 2. 按凶险回填预设数值（仅回填仍为默认值的行） ==========
UPDATE demon_trial SET
  cost_exp      = CASE difficulty WHEN '低' THEN 0   WHEN '高' THEN 60  WHEN '绝' THEN 150 ELSE 20 END,
  daily_limit   = CASE difficulty WHEN '低' THEN 5   WHEN '高' THEN 2   WHEN '绝' THEN 1   ELSE 3  END,
  reward_min    = CASE difficulty WHEN '低' THEN 10  WHEN '高' THEN 80  WHEN '绝' THEN 200 ELSE 30 END,
  reward_max    = CASE difficulty WHEN '低' THEN 30  WHEN '高' THEN 160 WHEN '绝' THEN 400 ELSE 70 END,
  risk_rate     = CASE difficulty WHEN '低' THEN 10  WHEN '高' THEN 30  WHEN '绝' THEN 45  ELSE 20 END,
  treasure_rate = CASE difficulty WHEN '低' THEN 8   WHEN '高' THEN 18  WHEN '绝' THEN 25  ELSE 12 END
-- 命中条件 = 该行仍是「未定制」状态：要么是刚加列的默认值(0/3/20/60/20/15)，
-- 要么是字符集不匹配导致误填的「中」档(20/3/30/70/20/12)；管理员手工调过的行不会被覆盖
WHERE (cost_exp = 0  AND daily_limit = 3 AND reward_min = 20 AND reward_max = 60 AND risk_rate = 20 AND treasure_rate = 15)
   OR (cost_exp = 20 AND daily_limit = 3 AND reward_min = 30 AND reward_max = 70 AND risk_rate = 20 AND treasure_rate = 12);

-- 兜底：产出下限不得高于上限
UPDATE demon_trial SET reward_max = reward_min WHERE reward_max < reward_min;

-- ========== 3. 守关妖魔（仅补充尚未设置的） ==========
UPDATE demon_trial SET boss = '白骨魔将' WHERE boss = '' AND title = '万骨荒原试炼';
UPDATE demon_trial SET boss = '血海浮屠兽' WHERE boss = '' AND title = '血海浮屠';
UPDATE demon_trial SET boss = '幻月魔狐' WHERE boss = '' AND title = '幻月迷宫';
UPDATE demon_trial SET boss = '渊底古魔' WHERE boss = '' AND title = '魔渊深处';
UPDATE demon_trial SET boss = '演武魁首' WHERE boss = '' AND title = '群魔论战';
UPDATE demon_trial SET boss = '煞风鹰隼' WHERE boss = '' AND title = '煞风峡谷';
UPDATE demon_trial SET boss = '冥河渡魂' WHERE boss = '' AND title = '冥河摆渡';
UPDATE demon_trial SET boss = '万魔之主' WHERE boss = '' AND title = '万魔窟';
UPDATE demon_trial SET boss = '残星守者' WHERE boss = '' AND title = '残星台';

-- ========== 4. 秘境探索日志表 ==========
CREATE TABLE IF NOT EXISTS demon_realm_log (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  user_id     BIGINT       NOT NULL COMMENT '探索者 id',
  trial_id    BIGINT       NOT NULL COMMENT '所入秘境 id',
  title       VARCHAR(60)  DEFAULT '' COMMENT '秘境名（快照）',
  result      VARCHAR(16)  DEFAULT '' COMMENT '结果：treasure 机缘 / success 功成 / injured 负伤',
  detail      VARCHAR(200) DEFAULT '' COMMENT '一句话战报',
  exp_gain    INT          NOT NULL DEFAULT 0 COMMENT '所得煞气（含机缘、已扣负伤）',
  cost        INT          NOT NULL DEFAULT 0 COMMENT '入场消耗',
  injury      INT          NOT NULL DEFAULT 0 COMMENT '负伤折损',
  treasure    VARCHAR(80)  DEFAULT '' COMMENT '机缘所得宝物名',
  created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_realm_log_user (user_id),
  KEY idx_realm_log_trial_time (trial_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秘境探索日志';

-- ========== 5. 表注释正名 ==========
ALTER TABLE demon_trial COMMENT = '域外秘境';

SELECT id, title, difficulty, boss, cost_exp, daily_limit, reward_min, reward_max, risk_rate, treasure_rate
FROM demon_trial ORDER BY id;
