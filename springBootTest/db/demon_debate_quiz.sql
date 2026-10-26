-- 魔族修行 · demon_debate 补列（论道问答功能：选项/答案/解析）
-- 幂等说明：列已存在会报 Duplicate column，可忽略
USE demon_db;

ALTER TABLE demon_debate
  ADD COLUMN option_a VARCHAR(120) DEFAULT '' COMMENT '选项 A' AFTER summary,
  ADD COLUMN option_b VARCHAR(120) DEFAULT '' COMMENT '选项 B' AFTER option_a,
  ADD COLUMN option_c VARCHAR(120) DEFAULT '' COMMENT '选项 C' AFTER option_b,
  ADD COLUMN answer   CHAR(1)     DEFAULT NULL COMMENT '正确选项 A/B/C' AFTER option_c,
  ADD COLUMN analysis VARCHAR(300) DEFAULT '' COMMENT '论道解析' AFTER answer;

UPDATE demon_debate SET
  option_a='魔由心生，道由心灭', option_b='魔道本一体，一念之转', option_c='魔即魔，道即道，永不相容', answer='B',
  analysis='万法唯心所现，魔与道不过执念两分；顺逆由心，转念即是转境。'
WHERE title='魔非魔，道非道';
UPDATE demon_debate SET
  option_a='血契是束缚，斩之即自由', option_b='血契是枷锁，亦是修行的磨刀石', option_c='自由与束缚皆在己心，血契只是镜', answer='C',
  analysis='以血为契，缚的是身，试的是心；视其为磨刀石还是牢笼，只在一念。'
WHERE title='血契之辩';
UPDATE demon_debate SET
  option_a='魔道无情，慈悲是弱者的伪装', option_b='杀伐为主，慈悲为佐，方为长久', option_c='慈悲即杀伐，杀伐即慈悲', answer='B',
  analysis='一味杀伐则众叛亲离，一味慈悲则道基不固；一线慈悲存于杀伐之中，方是中道。'
WHERE title='杀伐与慈悲';
UPDATE demon_debate SET
  option_a='长生是真，寿与天齐即是解脱', option_b='长生是妄，舍寿方见真我', option_c='长生无所谓真假，体验本身即是意义', answer='B',
  analysis='执念于不死者，恰被不死所缚；放下长生之执，方能在有限中见真我。'
WHERE title='长生孰真';
UPDATE demon_debate SET
  option_a='弱肉强食即天理，规制是伪善', option_b='当立规制，无序则魔域自毁', option_c='规制由强者制定，强者更迭则规制更迭', answer='B',
  analysis='群魔共治需规制为骨：无规制的魔域终将在内耗中崩塌，强弱轮转亦需秩序承载。'
WHERE title='群魔共治';
UPDATE demon_debate SET
  option_a='寂灭为归，轮回为苦', option_b='轮回为归，寂灭为断', option_c='寂灭与轮回皆途，道心所向即是归处', answer='C',
  analysis='执着寂灭是断，执着轮回是迷；魔修之路不在归处，而在道心所向。'
WHERE title='寂灭与轮回';
UPDATE demon_debate SET
  option_a='力量即一切，本心可弃', option_b='本心为本，力量为用，不可倒置', option_c='力量与本心并行不悖，无需取舍', answer='B',
  analysis='以本心祭力量者，终成力量的傀儡；力为用、心为本，用不伤本方是正途。'
WHERE title='力量之价';
UPDATE demon_debate SET
  option_a='魔域当有边界，守成方安', option_b='魔域当无垠扩张，停滞即死亡', option_c='疆界在心不在土，心之所在即魔域', answer='C',
  analysis='有形之疆终有尽时，无形之疆随道心而长；守成与扩张皆是外相，心疆无边方是真无垠。'
WHERE title='魔域之疆';
