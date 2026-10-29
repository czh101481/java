-- 魔族修行 · demon_user 升级脚本（权限体系）
-- 幂等说明：列已存在会报 Duplicate column，可忽略
USE demon_db;

-- 权限字段：1 = 默认权限（普通修者），0 = 管理员权限
-- 注册的新修者由后端写入默认值 1；仅 permission = 0 的账号可见后台入口并允许登录后台
ALTER TABLE demon_user
  ADD COLUMN permission TINYINT DEFAULT 1 COMMENT '权限：1 默认权限 / 0 管理员权限' AFTER password;

-- 演示管理员：将初始账号「魔祖」设为管理员权限
UPDATE demon_user SET permission = 0 WHERE name = '魔祖';

-- 如需将其他账号提为管理员，执行：UPDATE demon_user SET permission = 0 WHERE name = '道号';
