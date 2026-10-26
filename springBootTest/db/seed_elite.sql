-- 魔门翘楚 · 魔祖境种子数据（可重复执行）
-- 说明：封立魔域顶尖修者，均居魔祖之境，煞气修为各异以定座次。
--       仅按邮箱重置这几位翘楚，不影响其他修者数据。
-- ⚠️ 必须用 utf8mb4 客户端字符集导入，否则中文会变成乱码
USE demon_db;

DELETE FROM demon_user WHERE email IN (
  'xuehe@demon.com', 'youming@demon.com', 'shihun@demon.com',
  'jimie@demon.com', 'xuwang@demon.com'
);

INSERT INTO demon_user (name, email, realm, cultivation, exp, password) VALUES
('血河老祖', 'xuehe@demon.com',  '血煞', '魔祖', 3000, '123456'),
('幽冥夫人', 'youming@demon.com', '幽冥', '魔祖', 2600, '123456'),
('噬魂大王', 'shihun@demon.com',  '噬魂', '魔祖', 2200, '123456'),
('寂灭尊者', 'jimie@demon.com',   '虚妄', '魔祖', 1800, '123456'),
('虚妄上人', 'xuwang@demon.com',  '虚妄', '魔祖', 1500, '123456');
