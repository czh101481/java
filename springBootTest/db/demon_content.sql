-- ============================================================
-- 魔族修行 · 首页内容库（功法阁 / 历练录 / 宗门论道 / 魔典秘录 / 魔门公告）
-- 说明：本脚本创建内容表并灌入演示数据，供首页展示与检索使用。
--       采用 DROP + CREATE，可重复执行（内容为静态参考资料，非用户数据）。
-- 导入注意：务必使用 --default-character-set=utf8mb4，否则中文乱码。
-- ============================================================

USE demon_db;

-- ---------- 功法阁：demon_art ----------
DROP TABLE IF EXISTS demon_art;
CREATE TABLE demon_art (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  name        VARCHAR(60)  NOT NULL COMMENT '功法名',
  category    VARCHAR(20)  DEFAULT '' COMMENT '类别：攻伐/护体/幻术/炼魂/御兽',
  grade       VARCHAR(10)  DEFAULT '' COMMENT '品阶：黄/玄/地/天/仙',
  realm_req   VARCHAR(20)  DEFAULT '' COMMENT '所需境界',
  summary     VARCHAR(200) DEFAULT '' COMMENT '简介',
  content     TEXT         COMMENT '正文',
  created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='功法阁';

INSERT INTO demon_art (name, category, grade, realm_req, summary, content) VALUES
('血煞戮天诀', '攻伐', '天', '魔将', '以血煞之气凝刃，一念可斩千军。', '引周身精血化煞刃，血气越盛则刃越利；然反噬亦烈，需以心魔为锁。'),
('幽冥护魂咒', '护体', '地', '魔尉', '布幽冥屏障，万法不侵。', '以幽冥之气覆体，外邪难入；久施则神识渐冷，需常返阳间温养。'),
('噬魂幻月', '幻术', '玄', '魔卒', '织幻境如月华，惑敌五识。', '以月华为引织就幻网，敌入其间则真伪难辨；破法唯在守心。'),
('炼魂锁魄', '炼魂', '地', '魔尉', '锁敌魂魄，炼化为己用。', '以咒锁敌三魂七魄，可炼为傀或吞而补己；慎用，恐业障缠身。'),
('焚天业火', '攻伐', '天', '魔将', '引业火焚身，敌我皆烬。', '召业火焚尽四方，杀伤无匹；施者亦受灼伤，乃双刃之术。'),
('御兽血契', '御兽', '玄', '魔卒', '与异兽立血契，共赴杀伐。', '滴血为契，与魔渊异兽同心；兽死则契损，须善待之'),
('九幽寒煞功', '攻伐', '地', '魔尉', '引九幽寒气凝煞，冻敌于瞬息。', '以寒煞封脉，敌血凝而僵；然寒煞伤己阳元，须以血煞制衡。'),
('万鬼夜行术', '幻术', '天', '魔将', '召万鬼夜行，遮天蔽月。', '念动则群鬼出，惑敌心智、乱其神识；施者神魂消耗甚巨。'),
('噬骨魔音', '攻伐', '玄', '魔卒', '以魔音蚀骨，远攻之术。', '魔音入耳则骨酥魂颤，可破护体罡气；闻者需封耳守心。'),
('玄阴养魂诀', '炼魂', '地', '魔尉', '温养神魂，稳固道基。', '以玄阴之气滋养三魂，久修则神识清明、抗幻力增。'),
('血河大阵', '护体', '天', '魔将', '布血河为阵，万法不侵。', '以己血绘阵，成血河屏障，外力难破；阵破则反噬攻心。'),
('傀儡替死术', '御兽', '地', '魔尉', '炼傀儡代受死劫。', '以本命精血饲傀，临劫可替主受创；傀毁则精血大损。');

-- ---------- 历练录：demon_trial ----------
DROP TABLE IF EXISTS demon_trial;
CREATE TABLE demon_trial (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  title       VARCHAR(60)  NOT NULL COMMENT '历练名',
  region      VARCHAR(30)  DEFAULT '' COMMENT '地域',
  realm_req   VARCHAR(20)  DEFAULT '' COMMENT '所需境界',
  difficulty  VARCHAR(10)  DEFAULT '' COMMENT '凶险：低/中/高/绝',
  summary     VARCHAR(200) DEFAULT '' COMMENT '简介',
  created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='历练录';

INSERT INTO demon_trial (title, region, realm_req, difficulty, summary) VALUES
('万骨荒原试炼', '万骨荒原', '魔卒', '高', '于白骨遍野之地淬炼杀性，活过三息者方入门。'),
('血海浮屠', '血海', '魔尉', '绝', '渡血海而不溺，方证凶煞之身。'),
('幻月迷宫', '幻月渊', '魔卒', '中', '破幻月迷宫，明心见性，幻破则境开。'),
('魔渊深处', '无底魔渊', '魔将', '绝', '下探魔渊，窥见本源，然归来者寥寥。'),
('群魔论战', '魔域演武场', '魔尉', '中', '与同辈论战，砥砺道行，败者亦有所悟。'),
('煞风峡谷', '煞风峡', '魔卒', '中', '穿煞风而过，炼体亦炼心，怯者退。'),
('冥河摆渡', '冥河', '魔尉', '高', '渡冥河寻彼岸，迷者沉，悟者渡。'),
('万魔窟', '万魔窟', '魔将', '绝', '入万魔窟夺造化，九死一生，得者通玄。'),
('残星台', '残星台', '魔尉', '中', '登残星台观星象，窥天机一角，悟者获益。');

-- ---------- 宗门论道：demon_debate ----------
DROP TABLE IF EXISTS demon_debate;
CREATE TABLE demon_debate (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  title       VARCHAR(80)  NOT NULL COMMENT '论题',
  speaker     VARCHAR(40)  DEFAULT '' COMMENT '论者',
  summary     VARCHAR(200) DEFAULT '' COMMENT '摘要',
  created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宗门论道';

INSERT INTO demon_debate (title, speaker, summary) VALUES
('魔非魔，道非道', '魔祖', '顺逆由心，何须外求；魔与道，不过一念之转。'),
('血契之辩', '血煞尊者', '以血为契，究竟是束缚，还是自由？'),
('杀伐与慈悲', '幽冥夫人', '魔道修行，可存一线慈悲否？'),
('长生孰真', '虚妄上人', '长生不过执念，何为真我？'),
('群魔共治', '噬魂大王', '魔域当立规制，还是任凭弱肉强食？'),
('寂灭与轮回', '寂灭尊者', '魔修可证寂灭，抑或只入轮回？'),
('力量之价', '血河老祖', '无敌之力，是否必以本心为祭？'),
('魔域之疆', '幽冥夫人', '魔域当有边界，还是无垠扩张？');

-- ---------- 魔典秘录：demon_codex ----------
DROP TABLE IF EXISTS demon_codex;
CREATE TABLE demon_codex (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  title       VARCHAR(80)  NOT NULL COMMENT '典籍名',
  volume      VARCHAR(20)  DEFAULT '' COMMENT '卷册',
  summary     VARCHAR(200) DEFAULT '' COMMENT '摘要',
  created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='魔典秘录';

INSERT INTO demon_codex (title, volume, summary) VALUES
('太古魔典', '卷一·开辟', '记述魔域开天辟地之秘，言魔气自混沌中来。'),
('噬魂真解', '卷三·炼魂', '详述炼魂锁魄之法门与禁忌，阅前须立护心咒。'),
('血煞图谱', '卷七·攻伐', '血煞一脉功法源流与图谱，脉络清晰。'),
('幽冥往生录', '卷九·往生', '幽冥轮回之说，真假难辨，录以备参。'),
('禁忌之扉', '卷末·禁忌', '封印于魔域最深处之禁忌知识，擅窥者或遭反噬。'),
('万兽图录', '卷四·御兽', '记魔渊异兽形貌习性，御兽者必参。'),
('寒煞秘要', '卷六·攻伐', '寒煞一脉修炼次第与破法，循序渐进。'),
('冥河志异', '卷十·异闻', '冥河两岸奇闻怪谈，虚实参半，录以警世。'),
('星陨录', '卷十一·天象', '残星坠落之兆与魔劫关联考，关乎魔域气运。');

-- ---------- 魔门公告：demon_announcement ----------
DROP TABLE IF EXISTS demon_announcement;
CREATE TABLE demon_announcement (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  title       VARCHAR(80)  NOT NULL COMMENT '公告标题',
  content     VARCHAR(300) DEFAULT '' COMMENT '公告正文',
  level       VARCHAR(10)  DEFAULT '通告' COMMENT '等级：通告/警示/喜讯/秘辛',
  created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='魔门公告';

INSERT INTO demon_announcement (title, content, level) VALUES
('魔门开宗大典', '三月初三，魔主将于血煞峰开宗大典，诸修皆可赴会论道。', '喜讯'),
('煞气异动警示', '北境万骨荒原煞气骤盛，魔卒以下慎入，免遭反噬。', '警示'),
('新典入库', '《星陨录》《冥河志异》等上古残卷已入魔典秘录，诸修可往查阅。', '通告'),
('论道延期', '原定本月的「群魔共治」论道因魔渊异动延期，另择吉日。', '通告'),
('秘辛·禁忌之扉', '卷末禁忌之扉似有松动，擅窥者多遭反噬，切记慎之。', '秘辛');
