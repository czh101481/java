-- =====================================================================
-- 魔族修行 · 魔窟（洞府挂机产出：煞气 / 魔晶 / 血玉）
--
-- 玩法：
--   1) 每人一处魔窟，内设三座设施，各自 0~10 级（0 = 未开辟）
--        聚煞阵  sha_level  → 持续产出煞气（直接计入累计修为 demon_user.exp）
--        魔晶矿  coin_level → 持续产出魔晶（入钱囊）
--        血玉髓脉 jade_level → 持续产出血玉（入钱囊，走千分位进位）
--   2) 离线持续累积，存量上限 12 小时（CAP_HOURS），需手动「收取」
--   3) 收取 / 升级即时结算；升级消耗魔晶或血玉（见 DenRule 常量）
--
-- 幂等：可重复执行（CREATE TABLE IF NOT EXISTS）
-- 适用：init-db 全量初始化 与 upgrade-db 旧库升级
-- 执行：mysql -uroot -p****** --default-character-set=utf8mb4 < db/demon_den.sql
-- =====================================================================
USE demon_db;

CREATE TABLE IF NOT EXISTS demon_den (
  id           BIGINT   NOT NULL AUTO_INCREMENT,
  user_id      BIGINT   NOT NULL COMMENT '修者 id',
  sha_level    INT      NOT NULL DEFAULT 0 COMMENT '聚煞阵等级（0=未开辟，上限10）',
  coin_level   INT      NOT NULL DEFAULT 0 COMMENT '魔晶矿等级',
  jade_level   INT      NOT NULL DEFAULT 0 COMMENT '血玉髓脉等级',
  jade_carry   INT      NOT NULL DEFAULT 0 COMMENT '血玉千分位进位（1 血玉 = 1000）',
  last_collect DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上次收取时间（累积起点）',
  created_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_den_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='魔窟·洞府挂机产出';

-- 自检
SELECT COUNT(*) AS 魔窟表就绪 FROM information_schema.TABLES
 WHERE TABLE_SCHEMA = 'demon_db' AND TABLE_NAME = 'demon_den';
