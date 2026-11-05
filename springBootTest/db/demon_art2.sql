-- =====================================================================
-- 魔族修行 · 功法体系 v2（完整 / 残片 + 六重修炼进度 + 心魔）
--
-- 一、阶位：黄 → 玄 → 地 → 天 → 仙（阶位决定上限、威力、反噬、市价）
-- 二、形态：完整（可修至无上） / 残片（价格 = 完整 × 0.35，最高止步大成）
-- 三、进度：入门 → 小成 → 大成 → 圆满 → 入道 → 无上（每部功法各自推进）
-- 四、价格恒定（硬性）：
--      黄 完整 100 魔晶 / 残片 35     玄 完整 800 魔晶 / 残片 280
--      地 完整 50 血玉 / 残片 18      天 完整 300 血玉 / 残片 105
--      仙 完整 2000 血玉 / 残片 700
-- 五、心魔：修残篇魔功积心魔（每炼 +2），心魔 ≥60 不可冲境、战力折损；
--      完整功法入道后「以煞养心」，每次修炼心魔 -1。
--
-- 幂等：列用 information_schema 判断后 ADD；物品按 code 判重 upsert。
-- 执行顺序：demon_init.sql → demon_coin.sql → demon_cultivate.sql → demon_art2.sql
-- 执行：mysql -uroot -p123456 --default-character-set=utf8mb4 < db/demon_art2.sql
-- =====================================================================
USE demon_db;

-- ---------------------------------------------------------------------
-- 工具过程：列不存在才新增（MySQL 8 不支持 ADD COLUMN IF NOT EXISTS）
-- ---------------------------------------------------------------------
DROP PROCEDURE IF EXISTS demon_add_column;
DELIMITER $$
CREATE PROCEDURE demon_add_column(IN tbl VARCHAR(64), IN col VARCHAR(64), IN def TEXT)
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tbl AND COLUMN_NAME = col) THEN
    SET @ddl = CONCAT('ALTER TABLE `', tbl, '` ADD COLUMN `', col, '` ', def);
    PREPARE st FROM @ddl;
    EXECUTE st;
    DEALLOCATE PREPARE st;
  END IF;
END$$
DELIMITER ;

-- 修者：心魔值（0-100）
CALL demon_add_column('demon_user', 'heart_demon',
  'INT NOT NULL DEFAULT 0 COMMENT ''心魔值（0-100，残篇修炼滋生）'' AFTER layer');

-- 习得记录：完整/残片、六重进度、熟练度、当日修炼次数
CALL demon_add_column('demon_user_art', 'form',
  'TINYINT NOT NULL DEFAULT 0 COMMENT ''0=完整功法 1=残片'' AFTER art_id');
CALL demon_add_column('demon_user_art', 'stage',
  'TINYINT NOT NULL DEFAULT 1 COMMENT ''1入门 2小成 3大成 4圆满 5入道 6无上'' AFTER form');
CALL demon_add_column('demon_user_art', 'prof',
  'INT NOT NULL DEFAULT 0 COMMENT ''本重熟练度（满则进境）'' AFTER stage');
CALL demon_add_column('demon_user_art', 'train_date',
  'DATE DEFAULT NULL COMMENT ''最后修炼日期（每日限次跨日清零）'' AFTER prof');
CALL demon_add_column('demon_user_art', 'train_count',
  'INT NOT NULL DEFAULT 0 COMMENT ''当日已修炼次数'' AFTER train_date');

-- ---------------------------------------------------------------------
-- 合并旧「分重功法」：凝血爪 1-4 重 → 凝血爪；铁骨煞体 1-4 → 铁骨煞体；玄阴指 1-6 → 玄阴指
-- 主卷沿用最小 id（15 / 19 / 23），其余并入后删除
-- ---------------------------------------------------------------------
-- 用普通表而非临时表：MySQL 不允许同一语句多次引用临时表
DROP TABLE IF EXISTS tmp_art_merge;
CREATE TABLE tmp_art_merge (old_id BIGINT PRIMARY KEY, new_id BIGINT NOT NULL);
INSERT INTO tmp_art_merge (old_id, new_id) VALUES
  (16, 15), (17, 15), (18, 15),
  (20, 19), (21, 19), (22, 19),
  (24, 23), (25, 23), (26, 23), (27, 23), (28, 23);

-- 合并前先去重（uk_user_art 是唯一键，直接 UPDATE 会撞键）：
--   ① 同一修者持有多个同系列旧重，只留最小 id 的一条
DELETE ua1 FROM demon_user_art ua1
  JOIN tmp_art_merge m1 ON ua1.art_id = m1.old_id
  JOIN demon_user_art ua2 ON ua2.user_id = ua1.user_id
  JOIN tmp_art_merge m2 ON ua2.art_id = m2.old_id
 WHERE m1.new_id = m2.new_id AND ua1.id > ua2.id;

--   ② 已持有主卷者，旧重直接丢弃
DELETE ua FROM demon_user_art ua
  JOIN tmp_art_merge m ON ua.art_id = m.old_id
  JOIN demon_user_art keep ON keep.user_id = ua.user_id AND keep.art_id = m.new_id;

UPDATE demon_user_art ua JOIN tmp_art_merge m ON ua.art_id = m.old_id SET ua.art_id = m.new_id;

DELETE FROM demon_art WHERE id IN (SELECT old_id FROM tmp_art_merge);
DROP TABLE IF EXISTS tmp_art_merge;

-- 主卷归一：数值取原最高重（完整功法的基础值），进度改由 demon_user_art 推进
UPDATE demon_art SET name = '凝血爪', grade = '黄', category = '攻伐', layer = 1,
  series = '凝血爪', realm_req = '引煞',
  summary = '凝血于指，一抓裂魂。黄阶攻伐正宗——完整可修至无上，残篇止步大成。',
  bonus_atk = 16, bonus_def = 12, bonus_hp = 80, bonus_insight = 4, bonus_agility = 8
WHERE id = 15;
UPDATE demon_art SET name = '铁骨煞体', grade = '黄', category = '护体', layer = 1,
  series = '铁骨煞体', realm_req = '引煞',
  summary = '煞气淬骨，皮肉如铁。黄阶护体正宗——完整可修至无上，残篇止步大成。',
  bonus_atk = 16, bonus_def = 12, bonus_hp = 80, bonus_insight = 4, bonus_agility = 8
WHERE id = 19;
UPDATE demon_art SET name = '玄阴指', grade = '玄', category = '攻伐', layer = 1,
  series = '玄阴指', realm_req = '凝煞',
  summary = '阴煞聚指，一点透骨。玄阶攻伐正宗——完整可修至无上，残篇止步大成。',
  bonus_atk = 60, bonus_def = 48, bonus_hp = 330, bonus_insight = 18, bonus_agility = 36
WHERE id = 23;

-- ---------------------------------------------------------------------
-- 阶位统一价（硬性规则，后台不可改价）——同步写入列，供后台只读展示
-- ---------------------------------------------------------------------
UPDATE demon_art SET price_coin = 100,  price_jade = 0    WHERE grade = '黄';
UPDATE demon_art SET price_coin = 800,  price_jade = 0    WHERE grade = '玄';
UPDATE demon_art SET price_coin = 0,    price_jade = 50   WHERE grade = '地';
UPDATE demon_art SET price_coin = 0,    price_jade = 300  WHERE grade = '天';
UPDATE demon_art SET price_coin = 0,    price_jade = 2000 WHERE grade = '仙';

-- 其余功法补齐系列名（同名自成一系列）与在售状态
UPDATE demon_art SET series = name WHERE series IS NULL OR series = '';
UPDATE demon_art SET status = 1 WHERE status IS NULL;

-- ---------------------------------------------------------------------
-- 秘宝：入道 / 无上 的专属磨难道具
-- ---------------------------------------------------------------------
INSERT INTO demon_item
  (code, name, icon, category, desc_text, price_coin, price_jade,
   effect_type, effect_value, daily_limit, status, sort)
VALUES
  ('insight_fate', '悟道机缘', '缘', '秘宝',
   '完整功法修至圆满，凭此顿悟可入道（解锁本阶道韵）。残篇无用。', 0, 12, 'insight', 1, 0, 1, 90),
  ('death_battle', '死战悟道', '战', '秘宝',
   '入道之后须经死战磨砺，方可破境无上（衍本命神通）。残篇无用。', 0, 25, 'transcend', 1, 0, 1, 100)
AS incoming
ON DUPLICATE KEY UPDATE
  name = incoming.name, icon = incoming.icon, category = incoming.category,
  desc_text = incoming.desc_text, price_coin = incoming.price_coin,
  price_jade = incoming.price_jade, effect_type = incoming.effect_type,
  effect_value = incoming.effect_value, status = incoming.status, sort = incoming.sort;

SELECT '功法总数' AS item, COUNT(*) AS n FROM demon_art
UNION ALL SELECT '完整功法（各阶）', COUNT(*) FROM demon_art WHERE status = 1;
