-- =====================================================================
-- 魔族修行 · 功法主键重排（仙→天→地→玄→黄，高阶在前）
--
-- 目标编号与 db/demon_art_catalog.sql 完全同源（23 部，id = 1..23）。
-- 幂等：已一致时 @diff = 0，不做任何写入。
-- 执行：mysql -uroot -p****** --default-character-set=utf8mb4 < db/demon_art_renumber.sql
-- =====================================================================
USE demon_db;

DROP TABLE IF EXISTS tmp_art_target;
CREATE TABLE tmp_art_target (
  name   VARCHAR(128) COLLATE utf8mb4_0900_ai_ci NOT NULL PRIMARY KEY,
  new_id BIGINT NOT NULL UNIQUE
);

INSERT INTO tmp_art_target (name, new_id) VALUES
('太初魔经',1),
('混元血煞图',2),
('血煞戮天诀',3),
('御兽血契',4),
('天魔解体大法',5),
('焚天业火',6),
('血河大阵',7),
('天煞魔瞳',8),
('地煞裂山掌',9),
('幽壤护心甲',10),
('玄阴养魂诀',11),
('地渊尸潮',12),
('玄冰魔镜',13),
('玄阴指',14),
('幽罗伞',15),
('玄蛇吐息',16),
('阴煞噬心咒',17),
('千幻魔瞳',18),
('凝血爪',19),
('铁骨煞体',20),
('阴煞吐纳',21),
('阴风步',22),
('碎魂锤',23);

SELECT (SELECT COUNT(*) FROM demon_art) AS 库中总数,
       (SELECT COUNT(*) FROM demon_art a JOIN tmp_art_target t ON t.name = a.name) AS 命中数;

DROP TABLE IF EXISTS tmp_art_id_map;
CREATE TABLE tmp_art_id_map (
  old_id BIGINT NOT NULL PRIMARY KEY,
  new_id BIGINT NOT NULL UNIQUE
);

INSERT INTO tmp_art_id_map (old_id, new_id)
SELECT a.id, t.new_id FROM demon_art a JOIN tmp_art_target t ON t.name = a.name;

SET @diff := (SELECT COUNT(*) FROM demon_art a
                JOIN tmp_art_target t ON t.name = a.name
               WHERE a.id <> t.new_id);
SELECT @diff AS 需重排条数;

UPDATE demon_art SET id = id + 1000 WHERE @diff > 0;
UPDATE demon_user_art ua JOIN tmp_art_id_map m ON ua.art_id = m.old_id
   SET ua.art_id = m.new_id WHERE @diff > 0;
UPDATE demon_art a JOIN tmp_art_id_map m ON a.id = m.old_id + 1000
   SET a.id = m.new_id WHERE @diff > 0;

ALTER TABLE demon_art AUTO_INCREMENT = 24;
DROP TABLE tmp_art_id_map;
DROP TABLE tmp_art_target;

SELECT MIN(id) AS 起, MAX(id) AS 止, COUNT(*) AS 总数 FROM demon_art;
SELECT COUNT(*) AS 孤儿引用 FROM demon_user_art ua
  LEFT JOIN demon_art a ON a.id = ua.art_id WHERE a.id IS NULL;
