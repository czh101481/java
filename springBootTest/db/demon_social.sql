-- 魔族修行 · 社交（好友 + 传音）模块表结构
-- 幂等：可重复执行（CREATE TABLE IF NOT EXISTS），适用于 init-db / upgrade-db 流程

CREATE TABLE IF NOT EXISTS demon_friend (
  id          BIGINT        NOT NULL AUTO_INCREMENT,
  user_id     BIGINT        NOT NULL,
  friend_id   BIGINT        NOT NULL,
  status      TINYINT       NOT NULL DEFAULT 0 COMMENT '0=申请中 1=好友 2=已拒绝/已删除',
  message     VARCHAR(255)  DEFAULT NULL COMMENT '申请留言',
  created_at  DATETIME      DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  handled_at  DATETIME      DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_pair (user_id, friend_id),
  KEY idx_friend_id (friend_id),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='魔友关系（含申请与好友，双向对称存储）';

CREATE TABLE IF NOT EXISTS demon_message (
  id           BIGINT        NOT NULL AUTO_INCREMENT,
  from_id      BIGINT        NOT NULL,
  to_id        BIGINT        NOT NULL,
  content      TEXT,
  msg_type     TINYINT       NOT NULL DEFAULT 0 COMMENT '0=文字 1=修炼心得卡片',
  ref_type     VARCHAR(20)   DEFAULT NULL COMMENT 'art/codex/archive',
  ref_id       BIGINT        DEFAULT NULL,
  ref_title    VARCHAR(255)  DEFAULT NULL,
  ref_summary  VARCHAR(512)  DEFAULT NULL,
  `read`       TINYINT       NOT NULL DEFAULT 0 COMMENT '0=未读 1=已读',
  created_at   DATETIME      DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_conv (from_id, to_id),
  KEY idx_to_unread (to_id, `read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='传音私信';
