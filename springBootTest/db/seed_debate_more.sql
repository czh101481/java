-- ============================================================
-- 魔族修行 · 论道题库补充
-- 原库仅 8 道论题，去重后一天即答完（每日上限形同虚设）。
-- 本脚本补充 12 道，共 20 道，足够两日不重复。
-- 幂等：按 title 判重，重复执行不会产生副本。
-- ============================================================
USE demon_db;

INSERT INTO demon_debate (title, speaker, summary, option_a, option_b, option_c, answer, analysis)
SELECT '煞气何来', '引煞长老',
       '魔道修行以煞气为基，世人皆言煞气乃杀伐之气，那么它究竟从何而生？',
       '杀戮取命，唯杀生方得煞气',
       '众生执念所积，观魔域万象而自聚',
       '天授之，生而有之',
       'B',
       '煞气非杀伐所生，乃众生执念汇聚。故参悟魔域、论道辨析皆可纳煞，不必染血。'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM demon_debate WHERE title = '煞气何来');

INSERT INTO demon_debate (title, speaker, summary, option_a, option_b, option_c, answer, analysis)
SELECT '境界压制之虚实', '萧寒山',
       '高境者临敌，其压制之力究竟源于境界本身，还是源于积累的煞气？',
       '纯粹是位阶之差，与煞气无关',
       '位阶定根本，煞气填血肉，二者相合方成战力',
       '只看煞气多寡，位阶虚名而已',
       'B',
       '战力者，位阶乘千再加煞气。位阶是骨架，煞气是血肉。境界差两重以上，低者战力折损三成。'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM demon_debate WHERE title = '境界压制之虚实');

INSERT INTO demon_debate (title, speaker, summary, option_a, option_b, option_c, answer, analysis)
SELECT '四系魔种之辨', '虚妄上人',
       '血煞、幽冥、噬魂、虚妄四系，择其一而后终身为限，此择是否要紧？',
       '只是名号，全无实际差别',
       '各系天赋不同，择种即定其修行偏向',
       '可随时改易，随心而变',
       'B',
       '四系各有所长：血煞善战、幽冥纳煞、噬魂掠夺、虚妄顿悟。择种，便是择一条路数的开头。'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM demon_debate WHERE title = '四系魔种之辨');

INSERT INTO demon_debate (title, speaker, summary, option_a, option_b, option_c, answer, analysis)
SELECT '心魔何以为魔', '寂灭尊者',
       '修行者走火入魔，究竟是被外力侵蚀，还是自心失守？',
       '外魔入侵，防之当在外',
       '心念一乱，自心即魔',
       '修行过急，必然走火',
       'B',
       '魔自心生。外境只是引子，真正动摇道心的，从来都是自己的执念。'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM demon_debate WHERE title = '心魔何以为魔');

INSERT INTO demon_debate (title, speaker, summary, option_a, option_b, option_c, answer, analysis)
SELECT '切磋是否夺人道行', '血河老祖',
       '同门切磋，胜负既定便各有增损。那么切磋算不算夺人道行？',
       '算，故当禁止',
       '不算，煞气往来如活水，流转方能精进',
       '只在胜负悬殊时才算',
       'B',
       '煞气不是死物。切磋取其流转，败者失、胜者得，如磨刀石相互砥砺，并非掠夺。'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM demon_debate WHERE title = '切磋是否夺人道行');

INSERT INTO demon_debate (title, speaker, summary, option_a, option_b, option_c, answer, analysis)
SELECT '境界与功法', '魔祖',
       '已至高位者，是否还需回头修习低阶功法？',
       '不必，境界已足，功法皆小道',
       '境界为体，功法为用，体用不可偏废',
       '全看功法之名是否够响亮',
       'B',
       '境界是修为的容量，功法是运用的法门。空有境界而无功法，如持巨鼎而不知如何举。'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM demon_debate WHERE title = '境界与功法');

INSERT INTO demon_debate (title, speaker, summary, option_a, option_b, option_c, answer, analysis)
SELECT '历练之所求', '燕孤鸿',
       '入历练之地，究竟为取宝、为杀敌、还是为阅世？',
       '为宝，有所得方不虚此行',
       '为杀，杀得多者功高',
       '为阅世，见天地而后知自身之限',
       'C',
       '历练之本，在于见未曾见之物、遇未曾遇之险。宝物可失，所见长存。'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM demon_debate WHERE title = '历练之所求');

INSERT INTO demon_debate (title, speaker, summary, option_a, option_b, option_c, answer, analysis)
SELECT '魔籍为何留名', '典册执笔',
       '修者一代代凋零，魔枢典册却仍载其名，意义何在？',
       '为论功行赏，记其境界高低',
       '为留存来处，使后来者知今身所承',
       '仅为管理之便，无甚深意',
       'B',
       '典册记的不是名次，是来处。知其承，方知其往。'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM demon_debate WHERE title = '魔籍为何留名');

INSERT INTO demon_debate (title, speaker, summary, option_a, option_b, option_c, answer, analysis)
SELECT '掠夺之力有度否', '噬魂大王',
       '噬魂一系胜则夺人煞气，那么这种掠夺是否有上限？',
       '无上限，胜者当得其全部',
       '有上限，只取其该失之数，不敢尽取',
       '掠夺与胜负无关，只看境界差',
       'B',
       '掠夺之力亦有分寸。噬魂一系胜时多得一份，而非尽夺。道有边际，方能长存。'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM demon_debate WHERE title = '掠夺之力有度否');

INSERT INTO demon_debate (title, speaker, summary, option_a, option_b, option_c, answer, analysis)
SELECT '无上何解', '魔祖',
       '十境之末为「无上」。既名无上，是否意味修行到此为止？',
       '是，到此已极，无可再进',
       '否，「无上」只是当前十重之顶，非道之尽头',
       '无上即虚无，本质是放弃修行',
       'B',
       '「无上」是这十重体系之顶，不是道的顶。名可尽，而道不可尽。'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM demon_debate WHERE title = '无上何解');

INSERT INTO demon_debate (title, speaker, summary, option_a, option_b, option_c, answer, analysis)
SELECT '静修与争锋', '幽冥夫人',
       '一方闭关千年不出，一方日日与人争锋，孰能更快进境？',
       '闭关者，心不散则进境速',
       '争锋者，切磋纳煞最快',
       '二者各有其途，唯不宜偏废',
       'C',
       '闭关纳煞如储蓄，争锋切磋如练刀。蓄而不练则钝，练而不蓄则竭。'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM demon_debate WHERE title = '静修与争锋');

INSERT INTO demon_debate (title, speaker, summary, option_a, option_b, option_c, answer, analysis)
SELECT '道号与真我', '无名者',
       '修者以道号入籍受拜，那么道号是否影响其修行命途？',
       '影响，名入魔碑即是咒',
       '不影响，名只是名，来去无痕',
       '只在他人唤你时才短暂有意义',
       'A',
       '道号入碑，朝夕被唤，久而久之便成了自家所认的「我是谁」。名不是咒，却比咒更长久。'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM demon_debate WHERE title = '道号与真我');

-- 校验
SELECT COUNT(*) AS 论题总数 FROM demon_debate;
SELECT id, title, answer FROM demon_debate ORDER BY id;
