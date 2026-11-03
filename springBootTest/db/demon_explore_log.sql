-- ============================================================
-- 参悟日志表：demon_explore_log
-- 用途：记录每次「参悟此篇 / 随机参悟」成功，
--   1) 每篇内容每日仅可参悟一次（防重复刷同一篇）
--   2) 每日参悟总上限（与 Realm.DAILY_EXPLORE_LIMIT = 10 一致）
--   3) 回看今日参悟了哪些篇章
-- 幂等：CREATE TABLE IF NOT EXISTS，可重复执行
-- ============================================================

CREATE TABLE IF NOT EXISTS demon_explore_log (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  user_id      BIGINT       NOT NULL COMMENT '参悟者 id',
  content_type VARCHAR(20)  NOT NULL COMMENT '内容类型：art 功法阁 / codex 魔典秘录 / archive 魔枢典册',
  content_id   BIGINT       NOT NULL COMMENT '内容 id',
  title        VARCHAR(120) DEFAULT '' COMMENT '内容标题快照',
  gain         INT          DEFAULT 0 COMMENT '本次所得煞气',
  created_at   DATETIME     DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_user_day (user_id, created_at),
  KEY idx_user_content (user_id, content_type, content_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='参悟日志';
