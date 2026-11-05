-- =====================================================================
-- 魔族修行 · v1 数值平衡升级脚本（挂机 / 日活 / 血玉经济）
--
-- 定案（详见 db/BALANCE.md）：
--   1) 魔窟新增「一天最多结算 12 小时」，12h 真正成为日上限；
--      煞气 / 魔晶每小时产量 3 → 2，血玉千分位 40 → 100（补日上限损失）；
--      髓脉扩建除数 2 → 4（降低增产门槛）。Java 常量在 DenRule。
--   2) 血玉：连签满七日 1 → 2 枚、秘境机缘 1 → 2 枚（Coin.java）；
--      藏宝阁珍品降价、功法血玉三阶降价（ArtRank.java + 本脚本同步 demon_art）。
--   3) 转世倍率封顶 10 世（Realm.java / GameService.java）。
--
-- 幂等：
--   - 加列用 information_schema 判断（MySQL 8.4 不支持 ADD COLUMN IF NOT EXISTS）；
--   - 每条 UPDATE 带「旧值」守卫，第一次执行生效、第二次不再匹配，
--     执印人日后在后台的手改价也不会被本脚本覆盖。
--
-- 执行：mysql -uroot -p --default-character-set=utf8mb4 demon_db < 本文件
-- 顺序：全新安装用根目录全量快照 demon_db.sql（已含本脚本结果，无需再跑）；
--       旧库升级按 db/README.md 顺序跑完后，最后执行本文件。
-- =====================================================================
USE demon_db;

-- ---------------------------------------------------------------------
-- 一、demon_den：每日结算封顶记账列
-- ---------------------------------------------------------------------
DROP PROCEDURE IF EXISTS demon_balance_v1_cols;
DELIMITER $$
CREATE PROCEDURE demon_balance_v1_cols()
BEGIN
  IF NOT EXISTS (
      SELECT 1 FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'demon_den'
        AND COLUMN_NAME = 'collect_date') THEN
    ALTER TABLE demon_den
      ADD COLUMN collect_date DATE NULL
      COMMENT '每日结算记账日期（跨日自动清零）' AFTER last_collect;
  END IF;
  IF NOT EXISTS (
      SELECT 1 FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'demon_den'
        AND COLUMN_NAME = 'collected_sec') THEN
    ALTER TABLE demon_den
      ADD COLUMN collected_sec INT NOT NULL DEFAULT 0
      COMMENT '当日已累计结算秒数（上限 12h = 43200）' AFTER collect_date;
  END IF;
END$$
DELIMITER ;
CALL demon_balance_v1_cols();
DROP PROCEDURE demon_balance_v1_cols;

-- 历史行记账从今日归零，升级当天的剩余额度重新开放
UPDATE demon_den SET collect_date = NULL, collected_sec = 0
WHERE collect_date IS NULL OR collect_date <> CURDATE();

-- ---------------------------------------------------------------------
-- 二、藏宝阁：血玉消耗重定
--    常规四件日耗 19 玉 → 10 玉，低于新版日产约 12 玉（稳定）+ 机缘
-- ---------------------------------------------------------------------
UPDATE demon_item SET price_jade = 1
WHERE code = 'realm_compass' AND price_jade = 2 AND price_coin = 0;

UPDATE demon_item SET price_jade = 2, effect_value = 200, daily_limit = 1
WHERE code = 'abyss_orb' AND price_jade = 3 AND effect_value = 300 AND daily_limit = 2;

UPDATE demon_item SET price_jade = 5, effect_value = 1200
WHERE code = 'supreme_fruit' AND price_jade = 8 AND effect_value = 2000;

UPDATE demon_item SET price_jade = 8
WHERE code = 'insight_fate' AND price_jade = 12;

UPDATE demon_item SET price_jade = 15
WHERE code = 'death_battle' AND price_jade = 25;

-- ---------------------------------------------------------------------
-- 三、功法血玉价与 ArtRank 常量同步（残片价由 FRAG_RATIO 实时计算，不入库）
-- ---------------------------------------------------------------------
UPDATE demon_art SET price_jade = 40  WHERE grade = '地' AND price_jade = 50;
UPDATE demon_art SET price_jade = 150 WHERE grade = '天' AND price_jade = 300;
UPDATE demon_art SET price_jade = 600 WHERE grade = '仙' AND price_jade = 2000;

-- ---------------------------------------------------------------------
-- 四、校验（手动执行查看）
--   SELECT code,name,price_jade,effect_value,daily_limit FROM demon_item ORDER BY sort,id;
--   SELECT grade, MIN(price_jade) FROM demon_art GROUP BY grade;
--   SHOW COLUMNS FROM demon_den LIKE 'collect%';
--   期望：compass 1 / orb 2·200·1 / fruit 5·1200 / fate 8 / battle 15；
--         地 40 / 天 150 / 仙 600；collect_date + collected_sec 两列齐备
-- ---------------------------------------------------------------------
