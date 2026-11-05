-- ============================================================
-- 魔族修行 · 玩法层升级脚本
--   1) 论道防刷：demon_user 增加每日答题计数 + 答题记录表
--   2) 魔种天赋：demon_origin 表（血煞 / 幽冥 / 噬魂 / 虚妄 四系差异化）
-- 幂等：可重复执行，不会报 Duplicate column / Table already exists
-- ============================================================
USE demon_db;

-- ------------------------------------------------------------
-- 一、论道防刷：每日答题计数
-- ------------------------------------------------------------

SET @exist := (SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = 'demon_db' AND table_name = 'demon_user' AND column_name = 'quiz_date');
SET @sql := IF(@exist = 0,
  "ALTER TABLE demon_user ADD COLUMN quiz_date DATE DEFAULT NULL COMMENT '最后论道日期（跨日清零）' AFTER spar_count_today",
  "SELECT 'quiz_date already exists' AS info");
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = 'demon_db' AND table_name = 'demon_user' AND column_name = 'quiz_count_today');
SET @sql := IF(@exist = 0,
  "ALTER TABLE demon_user ADD COLUMN quiz_count_today INT DEFAULT 0 COMMENT '当日已答论题数' AFTER quiz_date",
  "SELECT 'quiz_count_today already exists' AS info");
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE demon_user SET quiz_count_today = 0 WHERE quiz_count_today IS NULL;

-- ------------------------------------------------------------
-- 二、论道答题记录（同一修者同一论题只能作答一次）
-- ------------------------------------------------------------

CREATE TABLE IF NOT EXISTS demon_quiz_record (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id     BIGINT       NOT NULL                COMMENT '作答修者 id',
  debate_id   BIGINT       NOT NULL                COMMENT '论题 id（demon_debate.id）',
  quiz_date   DATE         NOT NULL                COMMENT '作答日期（同一题跨日可再辩）',
  `correct`   TINYINT      NOT NULL DEFAULT 0      COMMENT '是否答对：1 是 / 0 否',
  awarded     INT          NOT NULL DEFAULT 0      COMMENT '本次结算的煞气变动（可为负）',
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '作答时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_debate_day (user_id, debate_id, quiz_date) COMMENT '同一人同一题每日一次，跨日可再辩',
  KEY idx_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '论道答题记录（按日判重 + 递减结算）';

-- ------------------------------------------------------------
-- 三、魔种天赋表（ demons 四系差异化成长路线）
--   explore_bonus     参悟煞气加成倍率，0.50 = +50%
--   spar_power_bonus  切磋战力加成倍率，0.12 = +12%
--   spar_reward_bonus 切磋胜利时额外奖励倍率，1.00 = 奖励翻倍
--   insight_chance    参悟时触发「顿悟」双倍的概率，0.25 = 25%
-- ------------------------------------------------------------

CREATE TABLE IF NOT EXISTS demon_origin (
  id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  origin            VARCHAR(16)  NOT NULL                COMMENT '魔种名（与 demon_user.realm 对应）',
  title             VARCHAR(32)  NOT NULL                COMMENT '天赋名',
  description       VARCHAR(200) DEFAULT NULL            COMMENT '天赋描述',
  explore_bonus     DECIMAL(4,2) NOT NULL DEFAULT 0.00   COMMENT '参悟煞气加成倍率',
  spar_power_bonus  DECIMAL(4,2) NOT NULL DEFAULT 0.00   COMMENT '切磋战力加成倍率',
  spar_reward_bonus DECIMAL(4,2) NOT NULL DEFAULT 0.00   COMMENT '切磋胜利额外奖励倍率',
  insight_chance    DECIMAL(4,2) NOT NULL DEFAULT 0.00   COMMENT '参悟顿悟（双倍）概率',
  sort              INT          NOT NULL DEFAULT 0      COMMENT '展示顺序',
  created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_origin (origin)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '魔种天赋配置（四系差异化）';

INSERT INTO demon_origin
  (origin, title, description, explore_bonus, spar_power_bonus, spar_reward_bonus, insight_chance, sort)
VALUES
  ('血煞', '战意滔天', '血煞入体，愈战愈勇。与人切磋时战力大涨，正面硬撼无人可挡。',
   0.00, 0.12, 0.00, 0.00, 1),
  ('幽冥', '幽息吞吐', '幽冥之体，静默纳煞。参悟魔域所得煞气远胜他人，进境最速。',
   0.50, 0.00, 0.00, 0.00, 2),
  ('噬魂', '夺魂摄魄', '噬魂之术，胜则食髓。切磋得胜时可将败者煞气一并吞下，掠夺倍增。',
   0.00, 0.00, 1.00, 0.00, 3),
  ('虚妄', '虚妄顿悟', '虚妄之心，真假难辨。参悟时偶得顿悟，煞气瞬间翻倍。',
   0.00, 0.00, 0.00, 0.25, 4)
ON DUPLICATE KEY UPDATE
  title             = VALUES(title),
  description       = VALUES(description),
  explore_bonus     = VALUES(explore_bonus),
  spar_power_bonus  = VALUES(spar_power_bonus),
  spar_reward_bonus = VALUES(spar_reward_bonus),
  insight_chance    = VALUES(insight_chance),
  sort              = VALUES(sort);

-- ------------------------------------------------------------
-- 四、校验
-- ------------------------------------------------------------

SELECT column_name, column_type, column_comment
FROM information_schema.columns
WHERE table_schema = 'demon_db' AND table_name = 'demon_user'
  AND column_name IN ('quiz_date', 'quiz_count_today');

SELECT origin, title, explore_bonus, spar_power_bonus, spar_reward_bonus, insight_chance
FROM demon_origin ORDER BY sort;
