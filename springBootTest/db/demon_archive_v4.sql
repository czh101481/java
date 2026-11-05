-- =====================================================================
-- 魔枢典册 · v1 数值平衡的「引用式」修订  db/demon_archive_v4.sql
--
-- 背景：v3 把 Java 常量 / DB 配置的具体数字逐条抄进了典册正文，
--       导致改一次数值要同步三处。本脚本把「经济类数字」改为引用式表述：
--       「其数依枢机印刻数，详见《数值预算》」，以后调平衡只需改常量与预算表。
--       世界观类数字（十境、九重、心魔六十、机缘概率等）不属平衡数值，保留不动。
--
-- 唯一事实依据：db/BALANCE.md（v1 数值预算表）
--
-- 幂等：每条 UPDATE 带「旧句」守卫（content LIKE '%旧句%'）；替换后旧句消失，
--       第二次执行不再匹配，执印人日后在后台的手改正文也不会被覆盖。
--       追加段用 NOT LIKE '%《数值预算》%' 守卫。
--
-- 执行：mysql -uroot -p --default-character-set=utf8mb4 demon_db < 本文件
-- 顺序：demon_archive_v3.sql 之后执行；全新安装的全量快照已含本脚本结果。
-- =====================================================================
USE demon_db;

-- 1) 参悟与机缘（path 6）：魔晶 / 折算率 / 机缘血玉
UPDATE demon_archive SET
  summary = REPLACE(summary, '另每篇固定得魔晶三，秘境机缘另得血玉。',
                             '另每篇固定得魔晶，秘境机缘另得血玉之赐。'),
  content = REPLACE(REPLACE(REPLACE(content,
    '今制又添一物：每参悟一篇，钱囊固定进魔晶三枚。',
    '今制又添一物：每参悟一篇，钱囊另进魔晶，其数依枢机印刻数。'),
    '秘境归来，还会按所得煞气折算魔晶（十取其一，至少五枚）。',
    '秘境归来，还会按所得煞气折算魔晶，其率依枢机印刻数。'),
    '撞上秘境机缘者，另得血玉一枚；',
    '撞上秘境机缘者，另得血玉之赐；')
WHERE category='path' AND sort_order=6
  AND content LIKE '%钱囊固定进魔晶三枚%';

-- 2) 每日焚香（path 7）：煞气 / 魔晶 / 血玉三组数字
UPDATE demon_archive SET
  summary = REPLACE(summary, '连满七日另得血玉一枚', '连满七日另得血玉之赐'),
  content = REPLACE(REPLACE(REPLACE(REPLACE(content,
    '每日一炷，香尽则得煞气十。',
    '每日一炷，香尽即得煞气，其数依枢机印刻数。'),
    '连焚一日，所得增五；连焚两日，增十；如此递增，至连满七日而止。连满七日者，另得厚赐五十，此后不再增。',
    '连焚愈久，所得愈增，至连满七日而止；连满七日者另有厚赐，此后不再增。'),
    '香灰落处，钱囊亦进魔晶：首日二十枚，连焚每多一日加五枚，至六十枚而封顶；每连满七日，另赐血玉一枚。',
    '香灰落处，钱囊亦进魔晶，连焚愈久所得愈丰，至枢机印所刻之数而封顶；每连满七日，另有血玉之赐。'),
    '看的是三样：煞、晶、玉。',
    '看的是三样：煞、晶、玉。其确数皆刻于枢机印，详见《数值预算》。')
WHERE category='path' AND sort_order=7
  AND content LIKE '%香灰落处，钱囊亦进魔晶：首日二十枚%';

-- 3) 切磋与战绩（path 9）：胜场魔晶与煞气赏
UPDATE demon_archive SET
  summary = REPLACE(summary, '一日五战，胜得十五魔晶。', '一日五战，胜者另有魔晶之赏。'),
  content = REPLACE(REPLACE(content,
    '胜者得煞十五、败者亦得十——魔域的规矩是',
    '胜者得煞较丰、败者亦有所得——魔域的规矩是'),
    '今制另赏胜者魔晶十五枚',
    '今制另赏胜者魔晶')
WHERE category='path' AND sort_order=9
  AND content LIKE '%今制另赏胜者魔晶十五枚%';

-- 4) 秘境之律（law 6）：魔晶折算率 / 机缘血玉
UPDATE demon_archive SET
  content = REPLACE(content,
    '按所得煞气十取其一折算魔晶，至少五枚；撞上机缘者，另得血玉一枚。',
    '按所得煞气折算魔晶，其率依枢机印刻数；撞上机缘者，另得血玉之赐。')
WHERE category='law' AND sort_order=6
  AND content LIKE '%按所得煞气十取其一折算魔晶%';

-- 5) 演武场之律（law 7）：论道 / 切磋魔晶赏
UPDATE demon_archive SET
  summary = REPLACE(summary, '论道答中得魔晶八，切磋获胜得魔晶十五',
                             '论道答中、切磋获胜各有魔晶之赏'),
  content = REPLACE(REPLACE(content,
    '答中一论得煞十、魔晶八，答错则失五煞，一日不过十论。',
    '答中一论得煞气与魔晶之赏，答错则反噬失煞，一日不过十论。'),
    '胜者得煞十五、魔晶十五，败者亦得煞十。',
    '胜者得煞与魔晶之赏，败者亦有所得。')
WHERE category='law' AND sort_order=7
  AND content LIKE '%答中一论得煞十、魔晶八%';

-- 6) 完整与残片（path 11）：五阶市价表去硬编码，保留排版
UPDATE demon_archive SET
  content = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(content,
    '市价由阶位定，枢机印刻死，不还价：',
    '市价由阶位定，枢机印刻死，不还价；确数详见《数值预算》，本篇只记其形：'),
    '　　黄阶　完整一百魔晶　／　残片三十五魔晶', '　　黄阶　以魔晶计'),
    '　　玄阶　完整八百魔晶　／　残片二百八十魔晶', '　　玄阶　以魔晶计'),
    '　　地阶　完整五十血玉　／　残片十八血玉', '　　地阶　以血玉计'),
    '　　天阶　完整三百血玉　／　残片一百零五血玉', '　　天阶　以血玉计'),
    '　　仙阶　完整二千血玉　／　残片七百血玉', '　　仙阶　以血玉计')
WHERE category='path' AND sort_order=11
  AND content LIKE '%完整二千血玉%';

-- 7) 洞府魔窟（path 14）：产量 / 费用去硬编码，并写明「每日结算封顶」
UPDATE demon_archive SET
  summary = REPLACE(summary,
    '满级十二时辰约得煞气三百六、魔晶三百六、血玉四枚余。',
    '满级每日封顶十二时辰，所得之数依枢机印刻数，详见《数值预算》。'),
  content = REPLACE(REPLACE(REPLACE(REPLACE(content,
    '聚煞阵与魔晶矿每小时各出「三 × 级数」；血玉髓脉出得极慢，每小时「四十‰ × 级数」，故以千分位暗记',
    '聚煞阵与魔晶矿每小时所得随级数递增，血玉髓脉渗得极缓，故以千分位暗记'),
    '满级十二时辰，约得煞气三百六十、魔晶三百六十、血玉四枚八分。',
    '满级每日封顶十二时辰，所得之数依枢机印刻数，详见《数值预算》。'),
    '开辟之费：聚煞阵、魔晶矿各三十魔晶；血玉髓脉须血玉二枚，且须修至凝煞境方可开凿。升一级之费，前二者为「三十 × 当前级数平方」魔晶，髓脉为「级数平方折半向上取整」血玉。',
    '开辟需费，聚煞阵与魔晶矿以魔晶计，血玉髓脉以血玉计且须修至凝煞境方可开凿。升一级之费随级数平方而增。'),
    '其一，存量最多积十二时辰，过时不再添。枢机印不许人一睡十日再来收——洞府是给勤者添的柴，不是给懒者备的仓。',
    '其一，单次存量最多积十二时辰，过时不再添；且一日之内累计最多结算十二时辰——纵然分作数次收取，所得亦不逾此数。枢机印不许人一睡十日再来收，也不许人靠勤收翻倍——洞府是给日活添的柴，不是给挂机备的仓。')
WHERE category='path' AND sort_order=14
  AND content LIKE '%每小时各出「三 × 级数」%';

-- 8) 魔晶与血玉（path 15）：各来源产出数字
UPDATE demon_archive SET
  content = REPLACE(REPLACE(content,
    '焚香一日二十至六十，参悟每篇三枚，论道答中八枚，切磋获胜十五枚，秘境归来按所得煞气折算，魔窟矿脉按时辰产出',
    '焚香、参悟、论道、切磋、秘境、魔窟矿脉各有产出，其数依枢机印刻数'),
    '连焚满七日赐一枚，秘境机缘赐一枚',
    '连焚满七日有赐，秘境机缘亦有赐')
WHERE category='path' AND sort_order=15
  AND content LIKE '%焚香一日二十至六十%';

-- 9) 藏宝阁与乾坤袋（path 16）：无上道果煞气量
UPDATE demon_archive SET
  content = REPLACE(content,
    '无上道果——开囊得煞气二千；',
    '无上道果——开囊得煞气之赐；')
WHERE category='path' AND sort_order=16
  AND content LIKE '%开囊得煞气二千%';

-- 10) 总纲附记：明确「数值以预算表为准」，挂在洞府魔窟篇末
UPDATE demon_archive SET
  content = CONCAT(content,
    '\n\n附记：本篇与「参悟与机缘」「每日焚香」「魔晶与血玉」「藏宝阁与乾坤袋」「完整与残片」诸篇所载产出与市价之数，皆以枢机印刻数为准。版本改易时典册不逐条重刻，执印人另修《数值预算》一册以记其确数，修者欲知其详，可问藏经殿。')
WHERE category='path' AND sort_order=14
  AND content NOT LIKE '%附记：本篇与%';

-- 11) 洞府魔窟（path 14）副标题 / 摘要：补「每日封顶」，避免只说存量封顶
UPDATE demon_archive SET
  subtitle = '法门 · 不修亦长，一日只结十二时辰',
  summary = REPLACE(summary, '离线累积十二时辰封顶', '单次存量十二时辰、一日只结十二时辰')
WHERE category='path' AND sort_order=14
  AND subtitle = '法门 · 三座设施，不修亦长';

-- ---------------------------------------------------------------------
-- 校验（手动执行查看）
--   SELECT category,sort_order,title FROM demon_archive
--    WHERE sort_order IN (6,7,9,11,14,15,16) ORDER BY category,sort_order;
--   SELECT COUNT(*) FROM demon_archive;  -- 仍应为 46 条，本脚本不增删条目
--   SELECT category,sort_order,title FROM demon_archive
--    WHERE content LIKE '%三百六十%' OR content LIKE '%二千血玉%'
--       OR content LIKE '%魔晶三枚%' OR content LIKE '%血玉一枚%';  -- 应为空
-- ---------------------------------------------------------------------
