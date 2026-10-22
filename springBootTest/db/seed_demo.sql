-- 演示数据初始化（每次启动执行；只重置演示账号，不破坏你注册的其他修者）
-- ⚠️ 必须用 utf8mb4 客户端字符集导入，否则中文会变成乱码
USE demon_db;

-- 仅删除旧的演示行（按邮箱定位），再插入干净的演示账号
DELETE FROM demon_user WHERE email = 'demo@demon.com';

INSERT INTO demon_user (name, email, realm, password)
VALUES ('魔祖', 'demo@demon.com', '血煞', '123456');
