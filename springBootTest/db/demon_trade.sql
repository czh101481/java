-- =====================================================================
-- 魔族修行 · 限时折扣 + 鬼市（玩家间交易）
-- 幂等：可重复执行（information_schema 判列 / CREATE TABLE IF NOT EXISTS）
-- 适用：init-db 全量初始化 与 upgrade-db 旧库升级
-- =====================================================================
USE demon_db;

-- ---------------------------------------------------------------------
-- 1. demon_item 增列：限时折扣
--    discount_percent：应付百分比（1-99，如 80 表示八折）；0 = 不打折
--    discount_start / discount_end：生效时段，留空表示该侧不设限
--    MySQL 8.4 不支持 ADD COLUMN IF NOT EXISTS，用 information_schema + PREPARE
-- ---------------------------------------------------------------------
SET @c1 := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'demon_db' AND TABLE_NAME = 'demon_item' AND COLUMN_NAME = 'discount_percent'
);
SET @ddl1 := IF(@c1 = 0,
  'ALTER TABLE demon_item ADD COLUMN discount_percent TINYINT NOT NULL DEFAULT 0 COMMENT ''限时折扣：应付百分比 1-99，0=不打折'' AFTER daily_limit',
  'SELECT ''discount_percent 列已存在，跳过'' AS msg');
PREPARE s1 FROM @ddl1;
EXECUTE s1;
DEALLOCATE PREPARE s1;

SET @c2 := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'demon_db' AND TABLE_NAME = 'demon_item' AND COLUMN_NAME = 'discount_start'
);
SET @ddl2 := IF(@c2 = 0,
  'ALTER TABLE demon_item ADD COLUMN discount_start DATETIME DEFAULT NULL COMMENT ''折扣生效起点（NULL 表示不限）'' AFTER discount_percent',
  'SELECT ''discount_start 列已存在，跳过'' AS msg');
PREPARE s2 FROM @ddl2;
EXECUTE s2;
DEALLOCATE PREPARE s2;

SET @c3 := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'demon_db' AND TABLE_NAME = 'demon_item' AND COLUMN_NAME = 'discount_end'
);
SET @ddl3 := IF(@c3 = 0,
  'ALTER TABLE demon_item ADD COLUMN discount_end DATETIME DEFAULT NULL COMMENT ''折扣截止（NULL 表示不限）'' AFTER discount_start',
  'SELECT ''discount_end 列已存在，跳过'' AS msg');
PREPARE s3 FROM @ddl3;
EXECUTE s3;
DEALLOCATE PREPARE s3;

-- ---------------------------------------------------------------------
-- 2. 鬼市挂单表（玩家把自己乾坤袋中的物品按价挂出，他人以魔晶/血玉买下）
--    status：0=在售 1=已售 2=已撤
--    上架即从卖家乾坤袋预扣，撤单原物退回，避免同一件物品被重复挂出
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS demon_trade (
  id         BIGINT      NOT NULL AUTO_INCREMENT,
  seller_id  BIGINT      NOT NULL COMMENT '卖家 id',
  item_id    BIGINT      NOT NULL COMMENT '物品 id',
  qty        INT         NOT NULL DEFAULT 1 COMMENT '数量',
  price      INT         NOT NULL COMMENT '单价（按 currency 计价）',
  currency   VARCHAR(8)  NOT NULL DEFAULT 'coin' COMMENT 'coin=魔晶 / jade=血玉',
  status     TINYINT     NOT NULL DEFAULT 0 COMMENT '0=在售 1=已售 2=已撤',
  buyer_id   BIGINT      DEFAULT NULL COMMENT '买家 id（成交后写入）',
  created_at DATETIME    DEFAULT CURRENT_TIMESTAMP,
  sold_at    DATETIME    DEFAULT NULL COMMENT '成交/撤回时间',
  PRIMARY KEY (id),
  KEY idx_trade_status (status, id),
  KEY idx_trade_seller (seller_id, status),
  KEY idx_trade_item (item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='鬼市挂单';

SELECT '限时折扣与鬼市表已就绪' AS status;
SELECT id, code, name, price_coin, price_jade, discount_percent, discount_start, discount_end
FROM demon_item ORDER BY sort;
