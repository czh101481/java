-- =====================================================================
-- 37 号脚本 · 入魔道新手期（v1.2）
--
-- 背景：注册零赠送（0 煞 / 0 晶 / 0 玉 / 引煞境），而
--   Realm.FEATURE_REQUIREMENT 把秘境/论道封到魔卒（累计 1200 煞）、
--   魔典封到魔尉（3000）、典册封到魔将（6000）；血玉髓脉又要 2 玉 + 凝煞，
--   新号前 7 日拿不到任何血玉 —— 形成「没有产出 -> 解锁不了玩法」的双重死锁。
--
-- 本脚本只建表 / 加列 / 补种子 / 修订典册，不改任何稳态产出常量。
-- 新手加成全部限定在「开局礼包起 14 天」内，到期自动回落，
-- 故 db/BALANCE.md 第二节的 895 煞 / 627 晶稳态预算不受影响。
--
-- 幂等：CREATE TABLE IF NOT EXISTS + information_schema 判断加列 +
--       NOT EXISTS 判重插入 + 文案带守卫词，可重复执行。
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. 新手状态表：一人一行，记开局礼包、七日礼进度与新兵期起点
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS demon_newbie (
  user_id          BIGINT      NOT NULL PRIMARY KEY COMMENT '修者 id（demon_user.id）',
  starter_done     TINYINT     NOT NULL DEFAULT 0 COMMENT '开局礼包是否已发（1=已发）',
  started_at       DATETIME    NULL     COMMENT '新兵期起点＝领礼包时间，14 天内有效',
  login_day        INT         NOT NULL DEFAULT 0 COMMENT '七日礼已领到第几日（0~7）',
  last_claim_date  DATE        NULL     COMMENT '上次领取七日礼的日期，防同日重复',
  created_at       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='入魔道：开局礼包 / 七日礼 / 新兵期记账';

-- ---------------------------------------------------------------------
-- 2. demon_trial 加 newbie_only：仅新兵期可见可入的入门试炼
--    MySQL 8.4 不支持 ADD COLUMN IF NOT EXISTS，必须 information_schema + PREPARE
-- ---------------------------------------------------------------------
SET @nb_col = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'demon_trial'
    AND COLUMN_NAME = 'newbie_only'
);
SET @nb_sql = IF(@nb_col = 0,
  'ALTER TABLE demon_trial ADD COLUMN newbie_only TINYINT NOT NULL DEFAULT 0 COMMENT ''仅新兵期可入的入门试炼''',
  'SELECT 1');
PREPARE nb_stmt FROM @nb_sql;
EXECUTE nb_stmt;
DEALLOCATE PREPARE nb_stmt;

-- ---------------------------------------------------------------------
-- 3. 三处入门试炼：引煞可入、不耗煞、每日各 1 次、无血玉/机缘掉落
--    收益刻意压低（8~24 煞），且仅新兵期开放，14 天后自动从列表消失，
--    因此不会成为长期稳态产出通道。
-- ---------------------------------------------------------------------
INSERT INTO demon_trial
  (id, title, region, realm_req, difficulty, summary, image, boss,
   cost_exp, daily_limit, reward_min, reward_max, risk_rate, treasure_rate, newbie_only)
SELECT 10, '碎石坡', '魔门外廓', '引煞', '低',
       '魔门外的碎石坡，新入道者于此试胆。不耗煞，亦无重宝，只求一个「敢」字。',
       '/images/trial/trial-1.jpg', '碎石魈',
       0, 1, 8, 16, 5, 0, 1
WHERE NOT EXISTS (SELECT 1 FROM demon_trial WHERE title = '碎石坡');

INSERT INTO demon_trial
  (id, title, region, realm_req, difficulty, summary, image, boss,
   cost_exp, daily_limit, reward_min, reward_max, risk_rate, treasure_rate, newbie_only)
SELECT 11, '枯骨径', '万骨荒原外缘', '引煞', '低',
       '白骨铺成的小径，妖影只作虚势。新兵期内可每日一探，期过此径自隐。',
       '/images/trial/trial-3.jpg', '骨隼',
       0, 1, 10, 20, 8, 0, 1
WHERE NOT EXISTS (SELECT 1 FROM demon_trial WHERE title = '枯骨径');

INSERT INTO demon_trial
  (id, title, region, realm_req, difficulty, summary, image, boss,
   cost_exp, daily_limit, reward_min, reward_max, risk_rate, treasure_rate, newbie_only)
SELECT 12, '浊煞潭', '血海支流', '引煞', '低',
       '一潭浊煞，涉之可锻筋骨。入门试炼不设境禁，却也只对新兵敞开。',
       '/images/trial/trial-5.jpg', '潭底魔影',
       0, 1, 12, 24, 10, 0, 1
WHERE NOT EXISTS (SELECT 1 FROM demon_trial WHERE title = '浊煞潭');

-- ---------------------------------------------------------------------
-- 4. 典册 33「典册封禁之律」整段改写：门槛已放宽
--    守卫词「今制放宽门禁」保证重复跑不会二次覆盖
-- ---------------------------------------------------------------------
UPDATE demon_archive
SET content = '魔域的要地与典籍分五处，各有境界门槛，非为藏私，为的是不该看的别看。\n\n一、功法阁：无所门槛。凡入魔途者皆可入，然阁中功法各有其价、各分形态，钱囊不足则取不下来——门开着，书也开着，只差一副家底。\n二、域外秘境：九处正境须魔卒境可入，凶险非引煞之躯可担；另于魔门外廓新辟三处「入门试炼」，不设境禁、不耗煞，惟新兵期内开放。\n三、宗门论道台：引煞即可登。旧律谓「无根底者上去徒受辱」，今制改易：论道本就是长根底的地方，先答错几回，才知该读哪一篇。\n四、魔典秘录：凝煞境可阅。秘录所载多涉各族隐事，旧律封至魔尉，今放宽一阶。\n五、魔枢典册：魔卒境可阅。典册载十境九重、渡劫之法、转世之秘，旧律封至魔将，今放宽两阶。\n\n今制放宽门禁，另辟「入魔道」一途：新入道者即领开局资粮，连领七日各有赏赐，自领礼之日起十四日内，焚香参悟所得倍丰、洞府气机倍浓、渡劫心魔亦减半。此谓新兵之恤，期过即止，不并入日常产出之数。\n\n今制新辟诸地，一律不设境界门禁：藏宝阁买卖凭钱囊，鬼市易物凭挂单，传音结友凭一道道友契约，魔窟洞府更是各人座下自行开辟，与境界无涉。洞府中的「血玉髓脉」旧须凝煞方可开凿，今亦解禁——引煞之躯既能领血玉，便该能开髓脉，否则血玉一路永无起始。\n\n此律由枢机印自执，非人力所守。境界不到者去翻，卷上之字会自行游走，读不成句；境界够了，字自归位。\n\n故魔域修者少有偷典册的——不是不敢，是偷了也读不出。\n\n典册按四部分类：魔族谱系、魔修道途、魔域法则、上古秘录。开卷者当自谱系始，循序渐进，此为历代枢机的建议，非律。近年典册屡经续补，凡标「今制」者，皆是枢机印改易之后的新法，与旧篇并列，不可混读。',
    subtitle = '铁律 · 五处要地各有其境，今制放宽门禁'
WHERE id = 33 AND content NOT LIKE '%今制放宽门禁%';

-- ---------------------------------------------------------------------
-- 5. 典册 31「秘境之律」补写入门试炼（精确 REPLACE，带守卫词）
-- ---------------------------------------------------------------------
UPDATE demon_archive
SET content = REPLACE(content,
       '枢机印所刻，共九处。',
       '枢机印所刻正境九处，另于魔门外廓新辟三处「入门试炼」，引煞之躯亦可入。')
WHERE id = 31 AND content NOT LIKE '%入门试炼%';

UPDATE demon_archive
SET content = REPLACE(content,
       '三、境界不足者不得入。每处秘境皆标开启境界，魔卒以下不得涉足。此律非为护短——是真进去了会死。',
       '三、境界不足者不得入。正境皆标开启境界，魔卒以下不得涉足；新辟三处入门试炼不设境禁、不耗煞，亦无重宝，惟新兵期内开放，期过自隐。此律非为护短——是真进去了会死。')
WHERE id = 31 AND content NOT LIKE '%期过自隐%';

-- ---------------------------------------------------------------------
-- 6. 首枚成就改「行动型」条件：开局礼包会直接给 120 煞，
--    若 first_step 仍是 exp>=50，新号一注册就自动点亮，下一目标会直接跳到
--    「魔尉之阶」（需 3000 煞），反而失去近期目标。改为首次焚香即可点亮，
--    形成「登录 → 焚一炷香 → 得第一枚徽记」的即时正反馈。
--    守卫 cond_type='exp' AND cond_value=50，重复跑不会二次改写后台手改值。
-- ---------------------------------------------------------------------
UPDATE demon_achievement
SET cond_type = 'sign_total', cond_value = 1,
    subtitle = '首次焚香',
    description = '入魔道后第一炷香，道途自此而始。'
WHERE code = 'first_step' AND cond_type = 'exp' AND cond_value = 50;

-- ---------------------------------------------------------------------
-- 校验
-- ---------------------------------------------------------------------
SELECT
  (SELECT COUNT(*) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'demon_newbie') AS newbie_table,
  (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'demon_trial'
      AND COLUMN_NAME = 'newbie_only') AS trial_col,
  (SELECT COUNT(*) FROM demon_trial WHERE newbie_only = 1) AS newbie_trials,
  (SELECT COUNT(*) FROM demon_archive WHERE id = 33 AND content LIKE '%今制放宽门禁%') AS law33,
  (SELECT COUNT(*) FROM demon_archive WHERE id = 31 AND content LIKE '%期过自隐%') AS law31,
  (SELECT COUNT(*) FROM demon_achievement
    WHERE code = 'first_step' AND cond_type = 'sign_total') AS first_step_action;
