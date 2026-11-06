-- =====================================================================
-- 34 号脚本：破关之钥「正文与所得」对齐（幂等，可重复执行）
--
-- 背景：demon_item 中 insight_fate / death_battle 的 effect_type 为
--       insight / transcend，旧 ShopService 未识别这两类，商店显示「煞气 +1」，
--       乾坤袋「取用」也会真的扣物、只给 1 点煞气；
--       而典册《秘境之律》明写「此物不增煞、不换钱，专为破关」。
-- 后端已修：这两类不可开囊，只在修为页的「顿悟入道 / 死战破境」里耗用，
--       商店与乾坤袋改显「破关之钥」。
-- 本脚本把物品说明与典册正文改成与实际收益一致，并顺手修正
--       v1 平衡时「改了 effect_value 没改说明」留下的两处笔误
--       （无上道果 +2000 / 深渊魔珠 300 枚）；说明一律由 effect_value 生成。
-- 导入：mysql -uroot -p<口令> --default-character-set=utf8mb4 demon_db < 本文件
-- =====================================================================

UPDATE demon_item
SET desc_text = '圆满之完整功法，于修为页「顿悟入道」时耗一枚：功法升至入道（战力系数 1.3），此后日课「以煞养心」不再滋生心魔。残篇无用，不可开囊。'
WHERE code = 'insight_fate';

UPDATE demon_item
SET desc_text = '入道之完整功法，于修为页「死战破境」时耗一枚：功法升至无上（战力系数 1.8，战力跨阶）。残篇无用，不可开囊。'
WHERE code = 'death_battle';

-- 数值曾于 v1 平衡时下调（道果 2000→1200、魔珠 300→200），说明却没跟着改。
-- 部分旧库的守卫未命中，魔珠仍是 v0 的 300：依《数值预算》先校正回 200。
UPDATE demon_item SET effect_value = 200
WHERE code = 'abyss_orb' AND effect_value = 300;

-- 说明一律按 effect_value 生成，日后改数值也不会再出现「说明与所得不符」
UPDATE demon_item
SET desc_text = CONCAT('魔域至宝，服之煞气 +', effect_value)
WHERE code = 'supreme_fruit' AND desc_text LIKE '%煞气 +%';

UPDATE demon_item
SET desc_text = CONCAT('捏碎魔珠，散出 ', effect_value, ' 枚魔晶')
WHERE code = 'abyss_orb' AND desc_text LIKE '%枚魔晶%';

-- 《六重日课》：把「道韵自生 / 衍本命神通」落到实际数值上，避免读成另赐术法
UPDATE demon_archive
SET content = REPLACE(content,
  '一念贯通，道韵自生。此物藏宝阁有售',
  '一念贯通，功法升至「入道」——战力系数由一点零进至一点三，此后日课「以煞养心」，每课反洗心魔一点。典册所谓「道韵自生」，指的便是这系数与养心二事，非另赐一门神通。此物藏宝阁有售')
WHERE id = 37 AND content LIKE '%一念贯通，道韵自生。此物藏宝阁有售%';

UPDATE demon_archive
SET content = REPLACE(content,
  '方臻无上。此物只藏宝阁有。',
  '方臻「无上」——战力系数由一点三跃至一点八，此即「战力跨阶」之实，阶位仍不变。所谓「衍本命神通」，亦只这一重系数之差，非另授术法。此物只藏宝阁有，同不可开囊。')
WHERE id = 37 AND content LIKE '%方臻无上。此物只藏宝阁有。%';

-- 《藏宝阁与乾坤袋》：写明破关之钥不开囊，只在功法阁破关处耗用
UPDATE demon_archive
SET content = REPLACE(content,
  '专为圆满、入道之功法破关，不入寻常账。',
  '专为圆满、入道之功法破关，不增煞、不进钱囊，亦不开囊。')
WHERE id = 41 AND content LIKE '%专为圆满、入道之功法破关，不入寻常账。%';

UPDATE demon_archive
SET content = REPLACE(content,
  '或进魔晶，或作破关之钥。',
  '或进魔晶。唯破关之钥例外——此二物开囊无益，须携至功法阁，于圆满、入道之完整功法处耗用，一枚一关；柜上不为它设开囊之钮。')
WHERE id = 41 AND content LIKE '%或进魔晶，或作破关之钥。%';

UPDATE demon_archive
SET content = REPLACE(content,
  '破关之钥亦在此律之内，与寻常丹符同账。',
  '破关之钥虽不开囊，一日可破之关数仍在此律之内，与寻常丹符同账。')
WHERE id = 41 AND content LIKE '%破关之钥亦在此律之内，与寻常丹符同账。%';

-- 校验：两件秘宝说明均已带「不可开囊」，丹符说明与 effect_value 一致
SELECT code, name, effect_type, effect_value, desc_text
FROM demon_item
WHERE code IN ('insight_fate', 'death_battle', 'supreme_fruit', 'abyss_orb')
ORDER BY id;
