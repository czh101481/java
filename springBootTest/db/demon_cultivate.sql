-- =====================================================================
-- 魔族修行 · 数值体系升级（境界分重 + 功法分重 / 货币购买 / 数值加成）
--
-- 包含三部分：
--   1) demon_user 增加 layer（当前境界内的重数，1~9，每境九重）
--   2) demon_art  增加 重数 / 系列 / 售价（魔晶·血玉）/ 上下架 / 五维数值加成
--   3) 新增黄级 8 部、玄级 6 部分重功法（同系列须按重递进购买）
--
-- 幂等：列用 information_schema 判断后 ADD；功法按 name 判重插入。
-- 执行：mysql -uroot -p123456 --default-character-set=utf8mb4 < db/demon_cultivate.sql
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

-- 修者：境界内重数（1~9）
CALL demon_add_column('demon_user', 'layer',
  'TINYINT NOT NULL DEFAULT 1 COMMENT ''境界内重数（1-9，每境九重）'' AFTER cultivation');

-- 功法：重数 / 系列 / 售价 / 上下架
CALL demon_add_column('demon_art', 'layer',
  'INT NOT NULL DEFAULT 1 COMMENT ''功法重数（黄1-4 玄1-6 地1-7 天1-10 仙1-10）'' AFTER grade');
CALL demon_add_column('demon_art', 'series',
  'VARCHAR(60) DEFAULT NULL COMMENT ''功法系列（同系列须按重递进购买）'' AFTER layer');
CALL demon_add_column('demon_art', 'price_coin',
  'INT NOT NULL DEFAULT 0 COMMENT ''售价·魔晶（0 且 price_jade=0 表示免费）'' AFTER series');
CALL demon_add_column('demon_art', 'price_jade',
  'INT NOT NULL DEFAULT 0 COMMENT ''售价·血玉（>0 时优先以血玉计价）'' AFTER price_coin');
CALL demon_add_column('demon_art', 'status',
  'TINYINT NOT NULL DEFAULT 1 COMMENT ''1=在售 0=下架'' AFTER price_jade');

-- 功法：五维数值加成
CALL demon_add_column('demon_art', 'bonus_atk',
  'INT NOT NULL DEFAULT 0 COMMENT ''攻伐加成'' AFTER status');
CALL demon_add_column('demon_art', 'bonus_def',
  'INT NOT NULL DEFAULT 0 COMMENT ''护体加成'' AFTER bonus_atk');
CALL demon_add_column('demon_art', 'bonus_hp',
  'INT NOT NULL DEFAULT 0 COMMENT ''气血加成'' AFTER bonus_def');
CALL demon_add_column('demon_art', 'bonus_insight',
  'INT NOT NULL DEFAULT 0 COMMENT ''悟性加成'' AFTER bonus_hp');
CALL demon_add_column('demon_art', 'bonus_agility',
  'INT NOT NULL DEFAULT 0 COMMENT ''身法加成'' AFTER bonus_insight');

DROP PROCEDURE IF EXISTS demon_add_column;

-- ---------------------------------------------------------------------
-- 既有功法：补重数（不得超过本阶上限）/ 系列 / 售价
--   黄 1-4 · 玄 1-6 · 地 1-7 · 天 1-10 · 仙 1-10
-- ---------------------------------------------------------------------
UPDATE demon_art SET series = name WHERE series IS NULL OR series = '';

UPDATE demon_art SET layer = CASE id
    WHEN 1  THEN 3    -- 血煞戮天诀 · 仙
    WHEN 2  THEN 2    -- 幽冥护魂咒 · 天
    WHEN 3  THEN 3    -- 噬魂幻月   · 天
    WHEN 4  THEN 2    -- 炼魂锁魄   · 天
    WHEN 5  THEN 4    -- 焚天业火   · 天
    WHEN 6  THEN 1    -- 御兽血契   · 天
    WHEN 7  THEN 4    -- 九幽寒煞功 · 地
    WHEN 8  THEN 3    -- 万鬼夜行术 · 天
    WHEN 9  THEN 5    -- 噬骨魔音   · 玄
    WHEN 10 THEN 2    -- 玄阴养魂诀 · 地
    WHEN 11 THEN 4    -- 血河大阵   · 天
    WHEN 12 THEN 3    -- 傀儡替死术 · 地
    ELSE layer END
WHERE id BETWEEN 1 AND 12;

-- 分级定价：黄/玄收魔晶，地/天/仙收血玉
UPDATE demon_art SET price_coin = layer * 120, price_jade = 0
WHERE grade = '黄' AND price_coin = 0 AND price_jade = 0;

UPDATE demon_art SET price_coin = layer * 260, price_jade = 0
WHERE grade = '玄' AND price_coin = 0 AND price_jade = 0;

UPDATE demon_art SET price_coin = 0, price_jade = layer * 2
WHERE grade = '地' AND price_coin = 0 AND price_jade = 0;

UPDATE demon_art SET price_coin = 0, price_jade = layer * 3
WHERE grade = '天' AND price_coin = 0 AND price_jade = 0;

UPDATE demon_art SET price_coin = 0, price_jade = layer * 8
WHERE grade = '仙' AND price_coin = 0 AND price_jade = 0;

-- ---------------------------------------------------------------------
-- 新增分重功法：黄级「凝血爪」四重、「铁骨煞体」四重，玄级「玄阴指」六重
--   同系列须先习得低一重，越重越贵、数值越高
-- ---------------------------------------------------------------------
CREATE TEMPORARY TABLE tmp_art (LIKE demon_art);

INSERT INTO tmp_art
  (name, category, grade, layer, series, realm_req, price_coin, price_jade, status, summary, content, image)
VALUES
  -- ===== 黄级 · 凝血爪（攻伐，四重）=====
  ('凝血爪·一重', '攻伐', '黄', 1, '凝血爪', '引煞', 60, 0, 1,
   '引煞入爪，血纹初显。黄级攻伐之基。',
   '凝血于指尖，一抓之下血气翻涌。此为凝血爪入门，重在养爪、凝煞、出势三诀。', '/images/art/art-1.jpg'),
  ('凝血爪·二重', '攻伐', '黄', 2, '凝血爪', '引煞', 140, 0, 1,
   '血纹交缠，爪风带煞。黄级二重。',
   '爪上血纹增至三十六道，出手已可裂石。修者需以自身煞气养纹，纹满则重成。', '/images/art/art-2.jpg'),
  ('凝血爪·三重', '攻伐', '黄', 3, '凝血爪', '凝煞', 260, 0, 1,
   '血爪成罡，可洞金石。黄级三重。',
   '罡气覆爪，一击可透三寸铁甲。此重之要，在于煞气与血气的匀调，失匀则伤己。', '/images/art/art-3.jpg'),
  ('凝血爪·四重', '攻伐', '黄', 4, '凝血爪', '凝煞', 420, 0, 1,
   '凝血爪圆满，十指皆兵。黄级四重·大成。',
   '十指尽化血刃，出手无声。凝血爪至此再无进阶，欲更进一步，须另寻玄级法门。', '/images/art/art-4.jpg'),

  -- ===== 黄级 · 铁骨煞体（护体，四重）=====
  ('铁骨煞体·一重', '护体', '黄', 1, '铁骨煞体', '引煞', 60, 0, 1,
   '煞气淬骨，皮肉如铁。黄级护体之基。',
   '以煞气反复淬炼周身骨骼，初成时拳脚难伤。此功法苦在日课，需日日不辍。', '/images/art/art-5.jpg'),
  ('铁骨煞体·二重', '护体', '黄', 2, '铁骨煞体', '引煞', 140, 0, 1,
   '骨生黑纹，刀剑难入。黄级二重。',
   '骨骼生出黑纹，寻常刀剑斩之只留白痕。此时方可承受秘境中的血煞侵蚀。', '/images/art/art-6.jpg'),
  ('铁骨煞体·三重', '护体', '黄', 3, '铁骨煞体', '凝煞', 260, 0, 1,
   '煞体初成，可撼妖躯。黄级三重。',
   '血肉与煞气交融，硬撼妖魔而不退。修者至此，方算真正踏稳魔道根基。', '/images/art/art-7.jpg'),
  ('铁骨煞体·四重', '护体', '黄', 4, '铁骨煞体', '凝煞', 380, 0, 1,
   '铁骨煞体圆满，万法难侵。黄级四重·大成。',
   '周身如铁铸，煞气自生护罩。铁骨煞体大成者，越阶而战亦有三分底气。', '/images/art/art-8.jpg'),

  -- ===== 玄级 · 玄阴指（攻伐，六重）=====
  ('玄阴指·一重', '攻伐', '玄', 1, '玄阴指', '凝煞', 600, 0, 1,
   '阴煞聚指，一点透骨。玄级攻伐之始。',
   '取九幽阴煞凝于一指，触之如坠冰窟。此指法阴柔，专破护体罡气。', '/images/art/art-9.jpg'),
  ('玄阴指·二重', '攻伐', '玄', 2, '玄阴指', '凝煞', 1100, 0, 1,
   '指风成线，隔空伤脉。玄级二重。',
   '指风凝为一线，可隔丈许伤敌经脉。修者需先通手三阴经，方能承此寒意。', '/images/art/art-10.jpg'),
  ('玄阴指·三重', '攻伐', '玄', 3, '玄阴指', '魔卒', 1700, 0, 1,
   '三指齐出，寒煞封魂。玄级三重。',
   '三指并出，寒气直封敌之魂窍。此重最险，寒气反噬者往往自断一指。', '/images/art/art-11.jpg'),
  ('玄阴指·四重', '攻伐', '玄', 4, '玄阴指', '魔卒', 2400, 0, 1,
   '玄阴凝罡，可裂魔躯。玄级四重。',
   '阴煞凝成罡力，一指可裂魔将之躯。至此已非指法，而是以指御煞。', '/images/art/art-12.jpg'),
  ('玄阴指·五重', '攻伐', '玄', 5, '玄阴指', '魔尉', 3300, 0, 1,
   '五指轮转，阴煞成域。玄级五重。',
   '五指轮转间自成一片阴煞之域，入域者血脉迟滞。此重已窥「域」之门槛。', '/images/art/art-1.jpg'),
  ('玄阴指·六重', '攻伐', '玄', 6, '玄阴指', '魔尉', 4500, 0, 1,
   '玄阴指圆满，一指断生。玄级六重·大成。',
   '一指之下，生灭由心。玄阴指至此圆满，再往上须以地级功法承其寒意。', '/images/art/art-2.jpg');

INSERT INTO demon_art
  (name, category, grade, layer, series, realm_req, price_coin, price_jade, status,
   summary, content, image)
SELECT t.name, t.category, t.grade, t.layer, t.series, t.realm_req,
       t.price_coin, t.price_jade, t.status, t.summary, t.content, t.image
FROM tmp_art t
WHERE NOT EXISTS (SELECT 1 FROM demon_art a WHERE a.name = t.name);

DROP TEMPORARY TABLE tmp_art;

-- ---------------------------------------------------------------------
-- 五维数值：按「品阶基数 × 重数」统一结算（未配数值的功法自动补全）
--   黄 4/3/20/1/2 · 玄 10/8/55/3/6 · 地 24/18/130/7/14 · 天 52/40/300/15/30 · 仙 110/85/600/30/65
-- ---------------------------------------------------------------------
UPDATE demon_art SET
  bonus_atk = layer * CASE grade
      WHEN '黄' THEN 4 WHEN '玄' THEN 10 WHEN '地' THEN 24 WHEN '天' THEN 52 WHEN '仙' THEN 110 ELSE 4 END,
  bonus_def = layer * CASE grade
      WHEN '黄' THEN 3 WHEN '玄' THEN 8 WHEN '地' THEN 18 WHEN '天' THEN 40 WHEN '仙' THEN 85 ELSE 3 END,
  bonus_hp = layer * CASE grade
      WHEN '黄' THEN 20 WHEN '玄' THEN 55 WHEN '地' THEN 130 WHEN '天' THEN 300 WHEN '仙' THEN 600 ELSE 20 END,
  bonus_insight = layer * CASE grade
      WHEN '黄' THEN 1 WHEN '玄' THEN 3 WHEN '地' THEN 7 WHEN '天' THEN 15 WHEN '仙' THEN 30 ELSE 1 END,
  bonus_agility = layer * CASE grade
      WHEN '黄' THEN 2 WHEN '玄' THEN 6 WHEN '地' THEN 14 WHEN '天' THEN 30 WHEN '仙' THEN 65 ELSE 2 END
WHERE bonus_atk = 0 AND bonus_def = 0 AND bonus_hp = 0;

-- 校验：应输出黄/玄/地/天/仙各阶功法数与重数范围
SELECT grade, COUNT(*) AS cnt, MIN(layer) AS min_layer, MAX(layer) AS max_layer,
       MIN(price_coin) AS min_coin, MAX(price_coin) AS max_coin,
       MIN(price_jade) AS min_jade, MAX(price_jade) AS max_jade
FROM demon_art GROUP BY grade ORDER BY FIELD(grade, '仙', '天', '地', '玄', '黄');
