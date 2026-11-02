-- =====================================================================
-- 魔族修行 · 修行境界表（十境配置，数据驱动）
-- 后端启动时由 com.fenglin.springboottest.config.RealmConfigLoader
-- 从本表加载，覆盖 Realm 内置默认值；表空/异常时自动回退默认值。
--
-- 执行方式（任选其一）：
--   1) 命令行：mysql -uroot -p123456 < db/demon_realm.sql
--   2) IDEA / Navicat / DataGrip 中打开本文件执行
--
-- 注意：本脚本使用 REPLACE INTO，重复执行会把十境配置「重置」为下方内容。
--       生产环境如需保留手工改动，请勿重复执行。
-- =====================================================================
USE demon_db;

CREATE TABLE IF NOT EXISTS demon_realm (
  id          BIGINT       NOT NULL AUTO_INCREMENT              COMMENT '主键',
  idx         INT          NOT NULL                             COMMENT '位阶（0 起，0=引煞，9=无上）',
  name        VARCHAR(20)  NOT NULL                             COMMENT '境界名（写入 demon_user.cultivation）',
  title       VARCHAR(50)  DEFAULT NULL                         COMMENT '境界称号',
  description VARCHAR(120) DEFAULT NULL                         COMMENT '境界描述',
  threshold   INT          DEFAULT 0                            COMMENT '冲击该境所需煞气（引煞为占位值，不参与计算）',
  created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP            COMMENT '落成时间',
  updated_at  DATETIME     DEFAULT CURRENT_TIMESTAMP
                           ON UPDATE CURRENT_TIMESTAMP          COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_idx  (idx),
  UNIQUE KEY uk_name (name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci
  COMMENT = '修行境界配置表（十境）';

-- ---------------------------------------------------------------------
-- 十境配置（顺序即境界高低：引煞 0 → 无上 9）
--   · idx 必须从 0 开始连续递增，后端加载时校验，否则回退默认值
--   · threshold 为「冲击本位阶所需煞气」，前后端进度条与突破校验共用
--   · 引煞为起始境，无人需要冲击，其 threshold 仅占位
-- ---------------------------------------------------------------------
REPLACE INTO demon_realm (idx, name, title, description, threshold) VALUES
  (0, '引煞', '初入魔门', '引煞入体，初踏魔途',     100),
  (1, '凝煞', '煞气凝形', '煞气凝形，根基渐固',     300),
  (2, '魔卒', '魔门行伍', '位列行伍，可入历练',     800),
  (3, '魔尉', '统御百夫', '百夫之长，可窥魔典',    1500),
  (4, '魔将', '坐镇一方', '坐镇一方，执掌典册',    2400),
  (5, '血煞', '威震魔域', '煞名远播，威震群魔',    3800),
  (6, '魔尊', '魔道至尊', '魔威盖世，群魔俯首',    8000),
  (7, '魔祖', '万法归魔', '一念通天，统御群修',   12000),
  (8, '魔圣', '圣魔临世', '魔道通玄，圣威昭昭',   15000),
  (9, '无上', '无上魔道', '超然物外，万法皆空',   20000);

-- 校验：应输出 10 行，idx 0~9 连续
SELECT idx, name, title, threshold FROM demon_realm ORDER BY idx;
