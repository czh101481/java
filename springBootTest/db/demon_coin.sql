-- =====================================================================
-- 魔族修行 · 货币体系（魔晶 / 血玉 + 商店 + 背包 + 流水）
-- 幂等：可重复执行（CREATE TABLE IF NOT EXISTS / ON DUPLICATE KEY UPDATE）
-- 适用：init-db 全量初始化 与 upgrade-db 旧库升级
-- =====================================================================
USE demon_db;

-- ---------------------------------------------------------------------
-- 1. 修者钱囊（每人一行，首次访问时自动开户）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS demon_wallet (
  id           BIGINT   NOT NULL AUTO_INCREMENT,
  user_id      BIGINT   NOT NULL COMMENT '修者 id',
  coin         INT      NOT NULL DEFAULT 0 COMMENT '魔晶余额（日常通货）',
  jade         INT      NOT NULL DEFAULT 0 COMMENT '血玉余额（稀有通货）',
  earned_coin  BIGINT   NOT NULL DEFAULT 0 COMMENT '累计获得魔晶',
  spent_coin   BIGINT   NOT NULL DEFAULT 0 COMMENT '累计消耗魔晶',
  earned_jade  BIGINT   NOT NULL DEFAULT 0 COMMENT '累计获得血玉',
  spent_jade   BIGINT   NOT NULL DEFAULT 0 COMMENT '累计消耗血玉',
  created_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_wallet_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='修者钱囊';

-- ---------------------------------------------------------------------
-- 2. 货币流水（每一笔收支都留痕，便于对账与展示）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS demon_coin_log (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  user_id     BIGINT      NOT NULL COMMENT '修者 id',
  currency    VARCHAR(8)  NOT NULL COMMENT 'coin=魔晶 / jade=血玉',
  amount      INT         NOT NULL COMMENT '变动值：正为入账，负为支出',
  balance     INT         NOT NULL DEFAULT 0 COMMENT '变动后余额（快照）',
  reason      VARCHAR(32) NOT NULL COMMENT '来源/去向代码：sign/explore/quiz/spar/secret/buy/item',
  reason_text VARCHAR(80) DEFAULT '' COMMENT '人话说明',
  ref_type    VARCHAR(16) DEFAULT '' COMMENT '关联类型：item/secret/...',
  ref_id      BIGINT      DEFAULT NULL COMMENT '关联 id',
  created_at  DATETIME    DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_coin_log_user (user_id, id),
  KEY idx_coin_log_time (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='货币流水';

-- ---------------------------------------------------------------------
-- 3. 藏宝阁物品定义
--    effect_type：exp=加煞气 / explore=今日参悟上限+N / coin=立即得魔晶
--    daily_limit：每人每日限购数量，0 表示不限
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS demon_item (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  code         VARCHAR(32)  NOT NULL COMMENT '物品代号（唯一）',
  name         VARCHAR(50)  NOT NULL COMMENT '物品名',
  icon         VARCHAR(8)   DEFAULT '丹' COMMENT '图标字（单字）',
  category     VARCHAR(16)  DEFAULT '丹药' COMMENT '丹药/符箓/奇物/珍品',
  desc_text    VARCHAR(200) DEFAULT '' COMMENT '物品说明',
  price_coin   INT          NOT NULL DEFAULT 0 COMMENT '魔晶价',
  price_jade   INT          NOT NULL DEFAULT 0 COMMENT '血玉价',
  effect_type  VARCHAR(20)  NOT NULL DEFAULT 'exp' COMMENT 'exp/explore/coin',
  effect_value INT          NOT NULL DEFAULT 0 COMMENT '效果数值',
  daily_limit  INT          NOT NULL DEFAULT 0 COMMENT '每日限购，0=不限',
  discount_percent TINYINT  NOT NULL DEFAULT 0 COMMENT '限时折扣：应付百分比 1-99，0=不打折',
  discount_start   DATETIME DEFAULT NULL COMMENT '折扣生效起点',
  discount_end     DATETIME DEFAULT NULL COMMENT '折扣截止',
  status       TINYINT      NOT NULL DEFAULT 1 COMMENT '1=在售 0=下架',
  sort         INT          NOT NULL DEFAULT 0 COMMENT '排序（小在前）',
  created_at   DATETIME     DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_item_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='藏宝阁物品';

-- ---------------------------------------------------------------------
-- 4. 乾坤袋（背包：每人每物一行，数量累加）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS demon_bag (
  id         BIGINT   NOT NULL AUTO_INCREMENT,
  user_id    BIGINT   NOT NULL COMMENT '修者 id',
  item_id    BIGINT   NOT NULL COMMENT '物品 id',
  qty        INT      NOT NULL DEFAULT 0 COMMENT '持有数量',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_bag_user_item (user_id, item_id),
  KEY idx_bag_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='乾坤袋';

-- ---------------------------------------------------------------------
-- 5. 购买记录（用于每日限购统计与消费审计）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS demon_purchase (
  id           BIGINT      NOT NULL AUTO_INCREMENT,
  user_id      BIGINT      NOT NULL,
  item_id      BIGINT      NOT NULL,
  qty          INT         NOT NULL DEFAULT 1,
  pay_currency VARCHAR(8)  NOT NULL COMMENT 'coin/jade',
  pay_amount   INT         NOT NULL DEFAULT 0 COMMENT '总价',
  buy_date     DATE        NOT NULL COMMENT '购买日期（限购统计用）',
  created_at   DATETIME    DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_purchase_limit (user_id, item_id, buy_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='藏宝阁购买记录';

-- ---------------------------------------------------------------------
-- 6. demon_user 增列：当日额外参悟次数（道具「凝神符」等使用后生效，跨日清零）
--    MySQL 8.4 不支持 ADD COLUMN IF NOT EXISTS，用 information_schema + PREPARE
-- ---------------------------------------------------------------------
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'demon_db'
    AND TABLE_NAME   = 'demon_user'
    AND COLUMN_NAME  = 'explore_bonus_today'
);

SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE demon_user ADD COLUMN explore_bonus_today INT NOT NULL DEFAULT 0 COMMENT ''当日额外参悟次数（道具加成）'' AFTER exp_count_today',
  'SELECT ''explore_bonus_today 列已存在，跳过新增'' AS msg');

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists2 := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'demon_db'
    AND TABLE_NAME   = 'demon_user'
    AND COLUMN_NAME  = 'explore_bonus_date'
);

SET @ddl2 := IF(@col_exists2 = 0,
  'ALTER TABLE demon_user ADD COLUMN explore_bonus_date DATE DEFAULT NULL COMMENT ''额外参悟次数所属日期（跨日清零）'' AFTER explore_bonus_today',
  'SELECT ''explore_bonus_date 列已存在，跳过新增'' AS msg');

PREPARE stmt2 FROM @ddl2;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;

-- ---------------------------------------------------------------------
-- 7. 商店种子物品（重复执行会更新名称/价格/说明，不会重复插入）
-- ---------------------------------------------------------------------
INSERT INTO demon_item (code, name, icon, category, desc_text, price_coin, price_jade,
                        effect_type, effect_value, daily_limit, status, sort)
VALUES
  ('gather_pill',    '聚煞丹',   '丹', '丹药', '寻常丹药，吞服后煞气 +60',                60, 0, 'exp',      60, 5, 1, 10),
  ('blood_pill',     '血煞丹',   '血', '丹药', '以血炼成，吞服后煞气 +200',              180, 0, 'exp',     200, 3, 1, 20),
  ('focus_talisman', '凝神符',   '符', '符箓', '静心凝神，今日参悟上限 +2',              120, 0, 'explore',   2, 2, 1, 30),
  ('treasure_pouch', '聚宝囊',   '囊', '奇物', '摇出一袋魔晶，可得 60 枚',                50, 0, 'coin',     60, 3, 1, 40),
  ('realm_compass',  '秘境罗盘', '盘', '奇物', '循罗盘可多探三篇，今日参悟上限 +3',        0, 2, 'explore',   3, 2, 1, 50),
  ('immortal_dew',   '洗髓露',   '露', '丹药', '洗髓伐骨，今日参悟上限 +5',                0, 1, 'explore',   5, 1, 1, 60),
  ('abyss_orb',      '深渊魔珠', '珠', '珍品', '捏碎魔珠，散出 300 枚魔晶',                0, 3, 'coin',    300, 2, 1, 70),
  ('supreme_fruit',  '无上道果', '果', '珍品', '魔域至宝，服之煞气 +2000',                0, 8, 'exp',    2000, 1, 1, 80)
AS incoming
ON DUPLICATE KEY UPDATE
  name = incoming.name,
  icon = incoming.icon,
  category = incoming.category,
  desc_text = incoming.desc_text,
  price_coin = incoming.price_coin,
  price_jade = incoming.price_jade,
  effect_type = incoming.effect_type,
  effect_value = incoming.effect_value,
  daily_limit = incoming.daily_limit,
  sort = incoming.sort;

SELECT '货币体系表已就绪' AS status;
SELECT code, name, price_coin, price_jade, effect_type, effect_value, daily_limit
FROM demon_item ORDER BY sort;
