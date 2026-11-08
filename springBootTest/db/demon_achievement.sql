-- ============================================================
-- 魔族修行 · 道途成就 + 今日修行（阶段 13 / 35 号脚本）
-- ------------------------------------------------------------
-- 新增两张表：
--   demon_achievement        成就定义（后台可 CRUD，纯荣誉、不发任何资源）
--   demon_user_achievement   修者解锁记录（user_id + achievement_id 唯一）
-- 并给魔枢典册补一条「道途与功行」设定。
--
-- 设计约束：
--   * 成就只给称号 / 徽记 / 道途进度，**不发魔晶、血玉、煞气**，
--     因此不触碰 db/BALANCE.md 的每日产出预算。
--   * 解锁由后端读接口按当前状态自动判定并落库，玩家无需手动领取。
--   * 种子用 INSERT IGNORE：重复执行不会覆盖后台手改过的名称与文案。
--
-- 幂等：可重复执行。导入须带 --default-character-set=utf8mb4。
-- ============================================================

CREATE TABLE IF NOT EXISTS demon_achievement (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  code          VARCHAR(64)  NOT NULL COMMENT '成就代号（唯一，后台可改但不可重复）',
  name          VARCHAR(80)  NOT NULL COMMENT '成就名',
  icon          VARCHAR(8)   NOT NULL DEFAULT '印' COMMENT '徽记单字',
  category      VARCHAR(20)  NOT NULL DEFAULT '道途' COMMENT '分类：道途/修行/日课/论道/图鉴/洞府/财富/交游/终极',
  subtitle      VARCHAR(120)          COMMENT '副题 / 达成条件的人话描述',
  description   VARCHAR(255)          COMMENT '叙事说明',
  cond_type     VARCHAR(40)  NOT NULL COMMENT '判定类型白名单，见 AchievementService',
  cond_value    INT          NOT NULL DEFAULT 1 COMMENT '判定阈值',
  title_reward  VARCHAR(60)           COMMENT '纯荣誉称号，不发资源',
  stage         VARCHAR(20)  NOT NULL DEFAULT '初入' COMMENT '道途分期：初入/登堂/小成/大成/圆满/无上',
  sort_order    INT          NOT NULL DEFAULT 0 COMMENT '展示顺序，小在前',
  status        TINYINT      NOT NULL DEFAULT 1 COMMENT '1=启用 0=停用',
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_ach_code (code),
  KEY idx_ach_status_sort (status, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='道途成就定义';

CREATE TABLE IF NOT EXISTS demon_user_achievement (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id        BIGINT   NOT NULL,
  achievement_id BIGINT   NOT NULL,
  unlocked_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_ach (user_id, achievement_id),
  KEY idx_ua_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='修者成就解锁记录';

-- ------------------------------------------------------------
-- 种子成就 25 条
-- cond_type 说明（与后端 AchievementService.COND_TYPES 一一对应）：
--   realm_index 境界位阶(0起) / total_layer 累计重数 / exp 累计煞气
--   sign_total 累计签到 / sign_streak 连签 / quiz_correct 答对不同论题数
--   read_distinct 图鉴去重篇数 / art_count 已习功法数 / art_stage 功法最高进度
--   spar_win 切磋胜场 / den_sum 洞府三设施等级和
--   coin_earned 累计获得魔晶 / jade_earned 累计获得血玉
--   friend_count 好友数 / trade_sold 鬼市成交卖出笔数
--   reincarnate 转世世数 / daily_all 今日日课完成项数
-- ------------------------------------------------------------
INSERT IGNORE INTO demon_achievement
(code, name, icon, category, subtitle, description, cond_type, cond_value, title_reward, stage, sort_order, status) VALUES
('first_step',      '初入魔道',   '启', '道途', '首次焚香',                  '入魔道后第一炷香，道途自此而始。',            'sign_total',     1,     '引煞童子',   '初入', 1,  1),
('realm_3',         '魔尉之阶',   '尉', '道途', '修至魔尉境',              '过魔卒而掌一队魔兵，方算登堂。',              'realm_index',   3,    '魔门都尉',   '登堂', 2,  1),
('realm_7',         '魔祖临世',   '祖', '道途', '修至魔祖境',              '一域之尊，诸魔俯首。',                        'realm_index',   7,    '一方魔祖',   '大成', 3,  1),
('realm_9',         '无上魔圣',   '圣', '道途', '修至无上境',              '十境之巅，魔道尽头。',                        'realm_index',   9,    '无上魔圣',   '圆满', 4,  1),
('layer_18',        '二重九转',   '转', '修行', '累计境界重数 18',          '两境九重俱满，根基始固。',                    'total_layer',   18,   '九转修士',   '登堂', 5,  1),
('layer_45',        '五境九重',   '重', '修行', '累计境界重数 45',          '五境皆至九重圆满。',                          'total_layer',   45,   '九重圆满',   '大成', 6,  1),
('sign_7',          '七日不辍',   '香', '日课', '连续焚香 7 日',            '香火不断，心志不摇。',                        'sign_streak',   7,    '长明香主',   '初入', 7,  1),
('sign_30',         '月月焚香',   '恒', '日课', '累计焚香 30 日',           '一月之期，日日不废。',                        'sign_total',    30,   '焚香行者',   '登堂', 8,  1),
('quiz_10',         '十论皆通',   '辩', '论道', '答对 10 道不同论题',       '论道台上十问不倒。',                          'quiz_correct',  10,   '辩才无碍',   '登堂', 9,  1),
('quiz_20',         '论道宗师',   '宗', '论道', '答对 20 道不同论题',       '题库尽通，魔门无出其右。',                    'quiz_correct',  20,   '论道宗师',   '圆满', 10, 1),
('read_20',         '博览群魔',   '卷', '图鉴', '研读 20 篇不同典籍',       '藏经殿中已有你的足迹。',                      'read_distinct', 20,   '博览魔典',   '初入', 11, 1),
('read_50',         '典册尽览',   '印', '图鉴', '研读 50 篇不同典籍',       '魔枢典册尽入胸中。',                          'read_distinct', 50,   '藏经执印',   '大成', 12, 1),
('art_3',           '兼修三门',   '法', '修行', '习得 3 部功法',            '不再倚仗一门之技。',                          'art_count',     3,    '兼修三门',   '登堂', 13, 1),
('art_10',          '万法归一',   '万', '修行', '习得 10 部功法',           '诸法并蓄，终归一道。',                        'art_count',     10,   '万法归一',   '圆满', 14, 1),
('art_supreme',     '无上道果',   '果', '修行', '任一功法修至无上',         '死战悟道之后，战力跨阶。',                    'art_stage',     6,    '道果执掌',   '圆满', 15, 1),
('spar_10',         '演武翘楚',   '战', '修行', '切磋胜 10 场',             '演武场上少有敌手。',                          'spar_win',      10,   '演武翘楚',   '登堂', 16, 1),
('den_15',          '洞府初成',   '窟', '洞府', '洞府三设施等级合计 15',    '聚煞阵与矿脉皆已成形。',                      'den_sum',       15,   '洞天初辟',   '登堂', 17, 1),
('den_30',          '三阵圆满',   '府', '洞府', '洞府三设施等级合计 30',    '三阵尽满，坐拥一窟。',                        'den_sum',       30,   '魔窟之主',   '圆满', 18, 1),
('coin_5000',       '富甲一方',   '晶', '财富', '累计获得 5000 魔晶',       '富甲榜上留名。',                              'coin_earned',   5000, '富甲一方',   '大成', 19, 1),
('jade_100',        '血玉盈囊',   '玉', '财富', '累计获得 100 血玉',        '血玉积至盈囊。',                              'jade_earned',   100,  '血玉盈囊',   '大成', 20, 1),
('friend_5',        '五方魔友',   '友', '交游', '结交 5 位魔友',            '传音之谊，遍结魔域。',                        'friend_count',  5,    '呼朋唤魔',   '登堂', 21, 1),
('trade_3',         '鬼市常客',   '市', '交游', '鬼市成交卖出 3 笔',        '鬼市之中信誉已立。',                          'trade_sold',    3,    '鬼市常客',   '登堂', 22, 1),
('reincarnate_1',   '一转轮回',   '轮', '终极', '转世重修 1 次',            '舍却一身修为，换得道基更厚。',                'reincarnate',   1,    '轮回者',     '圆满', 23, 1),
('reincarnate_10',  '十世魔主',   '世', '终极', '转世重修 10 次',           '十世圆满，倍率封顶。',                        'reincarnate',   10,   '十世魔主',   '无上', 24, 1),
('daily_all',       '日课无缺',   '课', '日课', '一日之内七项日课全满',     '焚香、参悟、论道、切磋、研读、秘境、洞府收取，一项不落。', 'daily_all', 7, '日课无缺', '小成', 25, 1);

-- 典册补一条设定（按 title 判重，可重复跑）
INSERT INTO demon_archive (category, title, subtitle, summary, content, sort_order)
SELECT 'path', '道途与功行', '成就徽记 · 今日修行',
       '魔门以「道途」记修者功行，逐期解锁徽记与称号。',
       '魔门执印者设「道途」一录，凡焚香、参悟、论道、切磋、研读、秘境、洞府诸般功行，'
       '皆按实绩自行铭刻，不必请领。每解锁一枚徽记，另赐一个纯属荣衔的称号，'
       '不发魔晶、不发血玉、亦不增煞气——道途只记功行，不作交易。'
       '徽记分初入、登堂、小成、大成、圆满、无上六期，愈后愈难，'
       '至「十世魔主」而止。首页「修行日常」逐项列出今日七课之完成度，'
       '修者可据此自省：何课未竟，何殿当往。'
       '后台执印者可增删道途条目、更定阈值、停用某枚徽记；已铭刻之功行不因停用而抹去。',
       47
WHERE NOT EXISTS (SELECT 1 FROM demon_archive WHERE title = '道途与功行');

-- 校验：25 条成就 + 典册新增 1 条
SELECT COUNT(*) AS achievement_count FROM demon_achievement;
SELECT COUNT(*) AS archive_count FROM demon_archive;
