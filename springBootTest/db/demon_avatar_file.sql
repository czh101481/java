-- ============================================================
-- 魔族修行 · 头像图片入库（36 号脚本，幂等，可重复执行）
--
-- 背景：自定义头像原本只落在 springBootTest/uploads/avatars/ 磁盘上，
--       demon_db.sql 全量快照只导数据库，换电脑导入快照后「库里有路径、
--       磁盘没有图」，英杰榜/道牌/传音的头像全部裂开。
--
-- 方案：图片本体以 base64 存入本表，demon_user.avatar 仍保持
--       /uploads/avatars/xxx.jpg 不变（前端与其它表零迁移）；
--       /uploads/** 改由控制器优先读本表，磁盘文件降级为本机备份。
--       这样全量快照即含图片，任何电脑导入快照后头像立刻可见。
--
-- 执行顺序：本脚本放在 35 号之后（阶段 14）。全新安装走全量快照即可，
--           无需再跑增量脚本；已有库跑完本脚本后还需执行一次
--           migrate_avatars.py，把上传过的历史图片补录进表。
-- ============================================================

CREATE TABLE IF NOT EXISTS demon_avatar_file (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  file_path   VARCHAR(255) NOT NULL COMMENT '如 /uploads/avatars/2_1793779685246.jpg',
  mime        VARCHAR(64)  NOT NULL DEFAULT 'image/jpeg',
  img_base64  LONGTEXT     NOT NULL COMMENT '图片原始字节的 base64',
  created_at  DATETIME     NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_avatar_path (file_path)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='自定义头像图片本体（随全量快照迁移）';

-- 校验：列出当前已入库图片数与总字节量（base64 长度 × 3/4 为近似值）
SELECT COUNT(*) AS avatar_rows,
       COALESCE(SUM(LENGTH(img_base64)), 0) AS base64_chars
FROM demon_avatar_file;
