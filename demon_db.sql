/*
 Navicat Premium Dump SQL

 Source Server         : stu_db
 Source Server Type    : MySQL
 Source Server Version : 80408 (8.4.8)
 Source Host           : localhost:3306
 Source Schema         : demon_db

 Target Server Type    : MySQL
 Target Server Version : 80408 (8.4.8)
 File Encoding         : 65001

 Date: 04/11/2026 01:54:21
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for demon_announcement
-- ----------------------------
DROP TABLE IF EXISTS `demon_announcement`;
CREATE TABLE `demon_announcement`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '公告标题',
  `content` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '公告正文',
  `level` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '通告' COMMENT '等级：通告/警示/喜讯/秘辛',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '魔门公告' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of demon_announcement
-- ----------------------------
INSERT INTO `demon_announcement` VALUES (1, '魔门开宗大典', '三月初三，魔主将于血煞峰开宗大典，诸修皆可赴会论道。', '喜讯', '2026-10-26 09:02:42');
INSERT INTO `demon_announcement` VALUES (2, '煞气异动警示', '北境万骨荒原煞气骤盛，魔卒以下慎入，免遭反噬。', '警示', '2026-10-26 09:02:42');
INSERT INTO `demon_announcement` VALUES (3, '新典入库', '《星陨录》《冥河志异》等上古残卷已入魔典秘录，诸修可往查阅。', '通告', '2026-10-26 09:02:42');
INSERT INTO `demon_announcement` VALUES (4, '论道延期', '原定本月的「群魔共治」论道因魔渊异动延期，另择吉日。', '通告', '2026-10-26 09:02:42');
INSERT INTO `demon_announcement` VALUES (5, '秘辛·禁忌之扉', '卷末禁忌之扉似有松动，擅窥者多遭反噬，切记慎之。', '秘辛', '2026-10-26 09:02:42');

-- ----------------------------
-- Table structure for demon_archive
-- ----------------------------
DROP TABLE IF EXISTS `demon_archive`;
CREATE TABLE `demon_archive`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `category` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分类键：lineage/path/law/ancient',
  `title` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '条目名',
  `subtitle` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '副题（如血脉/境界跨度）',
  `summary` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '一句话提要',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '正文',
  `sort_order` int NULL DEFAULT 0 COMMENT '同分类内排序，小者在前',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_category`(`category` ASC, `sort_order` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 36 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '魔枢典册' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of demon_archive
-- ----------------------------
INSERT INTO `demon_archive` VALUES (1, 'lineage', '血煞一族', '血脉 · 血为媒，煞为刃', '魔域最盛的一脉，以血为媒，煞气自生。', '血煞一族起于血海之滨，血脉中天生带煞。其修行不假外物，只以自身精血养煞，故族人愈战愈强，越伤越狂。\n\n天赋有二：一曰「血刃」，引血化兵，凝而不散；二曰「噬血回元」，伤敌则补己，故与之久战者，往往先自溃。\n\n族中定规：煞气过盛者需以心魔为锁，否则反噬自身，轻则失智，重则化为一团血雾。血煞峰为其祖地，非本族血脉不得登顶。', 1, '2026-10-26 09:02:43');
INSERT INTO `demon_archive` VALUES (2, 'lineage', '幽冥一族', '血脉 · 魂体近冥，善守不攻', '魂体近冥，天生长于护魂锁魄。', '幽冥一族生于冥河两岸，魂体偏阴，触之生寒。族人不擅攻伐，却精于「守」——以幽冥之气覆体，万法不侵；以咒锁魄，可定敌神魂。\n\n天赋有二：一曰「幽冥屏障」，护己亦护人；二曰「往生引」，能引将散之魂归于冥河，故各族战死者多请幽冥族人收魂。\n\n久施幽冥之术则神识渐冷，族人需常返阳间温养，故幽冥一族多居阴阳交界之地，不轻易远行。', 2, '2026-10-26 09:02:43');
INSERT INTO `demon_archive` VALUES (3, 'lineage', '噬魂一族', '血脉 · 生而吞魂，忌食同源', '生而能吞魂夺忆，然吞同族之魂者必遭反噬。', '噬魂一族形貌最似常人，唯瞳中无光。其天赋直指神魂：一曰「锁魄」，以咒定敌三魂七魄，使其不能自控；二曰「夺忆」，吞魂时并取其记忆，故族中老怪多知上古秘辛。\n\n族规森严：可吞异族之魂，不可吞同族之魂。犯者体内诸魂相冲，神魂撕裂而亡，此为魔域少有的「天罚自应」之例。\n\n噬魂一族与幽冥一族累世不睦——一个收魂，一个吞魂，所争者本是同一件东西。', 3, '2026-10-26 09:02:43');
INSERT INTO `demon_archive` VALUES (4, 'lineage', '幻月一族', '血脉 · 以月为引，织幻惑识', '以月华为引织就幻境，惑敌五识。', '幻月一族居于幻月渊，族人体内自存一缕月华。其术不在杀，而在「惑」：一曰「幻月结界」，境中时序、方位皆由施者定；二曰「心镜」，照见敌之执念，以执念为饵。\n\n此族最难缠处在于「不与你打」——你追，它在三步之外；你退，它在你影中。然其克星亦明：守心不动、不生执念者，幻境自破。\n\n传说幻月渊本无月，第一代幻月族人以自身月华点亮渊顶，至此渊中长夜有光。', 4, '2026-10-26 09:02:43');
INSERT INTO `demon_archive` VALUES (5, 'lineage', '骨魔与兽魔', '血脉 · 荒原之骨，血契之兽', '万骨荒原的骨修者，与以血契御兽的兽魔一脉。', '骨魔出于万骨荒原，以骸骨为体、以骨为器。族人少有血肉之躯，多见白骨之相，其修行靠「纳骨」——纳万千残骸于一身，骨愈多则力愈沉。骨魔不言情感，只认强弱。\n\n兽魔一脉则相反，肉身强悍而好群战。其核心之术「御兽血契」：滴血为契，与魔渊异兽同心，兽死则契损。故真正的兽魔从不轻弃其兽，视之如半身。\n\n两族常于荒原边缘争地，骨魔取尸骨，兽魔取其兽，互为世仇亦互为食粮。', 5, '2026-10-26 09:02:43');
INSERT INTO `demon_archive` VALUES (6, 'lineage', '星陨遗族', '血脉 · 陨星之力，几近绝嗣', '来自残星台的外来血脉，星力驳杂，几近绝嗣。', '星陨遗族并非魔域原血，其祖自天外坠于残星台，故体内所流为「星力」而非煞气。星力至刚至燥，与魔域煞气天然相斥，故族人修行倍难，寿元亦短。\n\n天赋「陨星一击」：倾尽全身星力于一瞬，其威可裂山，其代价是施者三日内再无战力。族中有一古训——此术一生只可尽放九次，第九次后星力枯竭而亡。\n\n如今遗族所存不足百人，残星台的石碑上，名字一年比一年少。', 6, '2026-10-26 09:02:43');
INSERT INTO `demon_archive` VALUES (7, 'path', '十境天梯', '境界 · 由煞入道，十阶登顶', '魔修十境，各有煞气之阈。煞气乃修者一身所积之总，累积达标方可叩关破境。', '魔修之路共十阶，阶阶以「煞气」为尺。煞气非消耗之物，乃修者一身所积之总，故破境之后旧煞不去、新煞再添——此谓「累积」。\n\n一曰「引煞」——引魔域煞气入体，凡人至此始称修者。需煞百。\n二曰「凝煞」——煞气凝于血窍，化虚为实。需煞四百。\n三曰「魔卒」——煞能外放，可结术法，自此可入域外秘境。需煞一千二百。\n四曰「魔尉」——煞气成域，一域之内法术自显威能，自此可窥魔典秘录。需煞三千。\n五曰「魔将」——域可覆人，坐镇一方，自此可开魔枢典册、执掌九域机要。需煞六千。\n六曰「血煞」——煞名外显，远近群魔闻之而栗，威不加身而人自退。需煞一万。\n七曰「魔尊」——煞气凝为尊相，一言可定一域之生死。需煞一万六千。\n八曰「魔祖」——一念通天，统御群修。魔域今世唯魔祖一人曾踏此境。需煞二万四千。\n九曰「魔圣」——魔道通玄，圣威昭昭，其言即为法。需煞三万五千。\n十曰「无上」——超然物外，万法皆空。天梯之尽，然尽处为何，典籍无载。需煞五万。\n\n十境之名非虚称，乃枢机印所刻。每境之阈皆以煞气总量计，非以年计——故有少年而登魔将者，亦有皓首而困魔卒者。\n\n修者当知：境界之高下，不独看煞气多寡，亦看其「位阶」。同是三千煞，魔尉之躯胜魔卒十倍，此乃位阶之差，非煞气可平。', 1, '2026-10-26 09:02:43');
INSERT INTO `demon_archive` VALUES (8, 'path', '引煞入体：第一道生死关', '境界 · 引煞期', '煞气灌体，九死一生，心魔自此而生。', '引煞是魔修的第一道关，也是死人最多的一道关。\n\n法极简：于煞气浓处静坐，开血窍，纳煞入体。痛如万刃穿身，此为常态。真正的凶险在于——煞气入体时，会顺带把「心魔」一并带进来。\n\n心魔无相，因人而异：好杀者见血海，贪生者见长生，悔恨者见故人。守得住，煞气归窍，从此入道；守不住，轻则疯癫成魔，重则当场爆体，尸骨无存。\n\n魔门有旧例：引煞者需有同门护法，若三个时辰内未醒，护法者当亲手了结。此非残忍，是不让其化魔屠城。', 2, '2026-10-26 09:02:43');
INSERT INTO `demon_archive` VALUES (9, 'path', '魔力运转规则', '心法 · 血窍为渠，心魔为锁', '血窍为渠、煞气为流、心魔为锁，逆行则爆体。', '魔力并非凭空而来，其运转有三要：\n\n一为「渠」——血窍。周身三十六处血窍即煞气通道，窍开则力增，窍损则力泄。故魔修极重护窍之术，护体一道由此而生。\n二为「流」——煞气。煞气运行自有其序，顺则周天圆满，逆则壅塞爆体。所谓走火入魔，十之八九是逆流。\n三为「锁」——心魔。此为魔修独有之物：以心魔为锁，锁住煞气不至暴走；心魔愈强，锁愈牢，但人也愈近疯癫。\n\n故此道有一句冷话：魔修不怕弱，只怕心魔不够强——而不够强的，早已死在引煞那一年。', 3, '2026-10-26 09:02:43');
INSERT INTO `demon_archive` VALUES (10, 'path', '修行法门三分', '法门 · 三道为主，两支为旁', '攻伐、护体、炼魂为正三道；幻术、御兽为旁支。', '魔域诸法，尽归五门，其中三道为正：\n\n「攻伐」——以煞化刃，以血为薪，杀伤最烈。功法阁中《血煞戮天诀》《焚天业火》皆属此道。\n「护体」——覆身如壁，保窍护魂。攻伐愈烈者，愈需护体，故大修者往往兼修两道。\n「炼魂」——直取神魂，锁魄夺忆。此道最凶亦最忌，业障缠身者多出于此。\n\n另有旁支二：\n「幻术」——惑敌五识，不与力敌，幻月一脉为宗。\n「御兽」——血契同心，与兽共战，兽魔一脉为宗。\n\n旁支不弱，只是路窄。能走到血煞境的，从来都是三道兼修之人。', 4, '2026-10-26 09:02:43');
INSERT INTO `demon_archive` VALUES (11, 'path', '渡劫与破境', '心法 · 煞满则叩关，心劫自内生', '煞气累积达标即可破境，旧制「以内换外」的折损之法已废；今唯余一道心劫，两成修者会败，连败两次者第三次必成。', '古法破境，谓之「以内换外」——以精血、寿元、神魂之一，换境界之升。此法折损太甚，九域十修七残，旧枢机遂于魔天之战后废之，改立今制。\n\n今制有四：\n\n其一，煞满则叩关。煞气累积至下一境之阈，便可破境。破境不折损旧煞，故修者愈往后愈快——前境所积，皆作后境之基。此乃「累积」二字的真意。\n\n其二，破境需渡心劫。魔修无天劫可渡，却有一道自内而生的心劫。心劫无相，因人而异：好杀者见血海，贪生者见长生，悔恨者见故人。心劫之来，约两成修者会败。\n\n其三，败则有损，损有封顶。心劫败者，煞气折损一成，然至多折五百，不使修者一夕归零。折损之后仍可再叩，煞气补足即再来。\n\n其四，连败两次者，第三次必成。此乃枢机印所刻的最后一律——天不给魔修飞升之门，却也不曾把门彻底焊死。凡连折两阵者，第三次心劫自开，无论修者愿与不愿。\n\n故魔域有句老话：破境不难，难在敢不敢再来一次。', 5, '2026-10-26 09:02:43');
INSERT INTO `demon_archive` VALUES (12, 'law', '三禁忌', '铁律 · 犯者不论境界，一律诛', '魔渊九层、魔主真名、同族血祭，触之即死。', '魔域无法无天，却有三条铁律，谓之禁忌。\n\n其一：不得擅入无底魔渊第九层。渊分九层，前八层可历炼，第九层封印之下另有其物。历代擅自下探者，无一人还。\n\n其二：不得直呼魔主真名。名即咒，呼之则咒应。魔主真名唯其本人与历代枢机知晓，泄露者，泄者与被泄者同诛。\n\n其三：不得以同族之血为祭。血祭可速成，然以同族为薪者，其族必衰。此法由上古大战后立，屡有犯者，屡有灭族之例。\n\n三禁不载于任何功法，却刻在每一座魔域界碑之上。新入道者第一课，便是背这三条。', 1, '2026-10-26 09:02:43');
INSERT INTO `demon_archive` VALUES (13, 'law', '天道枷锁', '铁律 · 魔修无劫可渡', '天道不容魔修，飞升之门永闭。', '仙道修行，终有飞升之日；魔道修行，无门可出。\n\n天道视煞气为「逆」，故魔修不为天道所容：一不能渡劫，二不能引天雷淬体，三不能借天时破境。他人借天，魔修只借己身——这也是魔修寿元虽长、终有尽时的根由。\n\n所谓「天道枷锁」，非一道实体的锁，而是一种持续的存在性排斥：魔修越是接近极境，天象越是异动，雷云越是低垂。魔域上空终年不散的血云，便是这排斥的显形。\n\n故魔域有一句流传最广的狂言：既然上不去，那就把天拉下来。此言出自魔天之战前的旧枢机，后成魔族战号。', 2, '2026-10-26 09:02:43');
INSERT INTO `demon_archive` VALUES (14, 'law', '血契之律', '铁律 · 契成不可解，唯死可断', '以血为契者，违契则血逆行，契断唯死。', '血契是魔域最普及、也最不容违逆的约定。上至宗门盟誓，下至御兽同心，皆以血契为凭。\n\n其律有三：\n一、契成不可解。立契时双方各出精血一滴，融于契石。此后契石不碎，契便不消。\n二、违契则血逆行。违者体内煞气逆流，血窍自裂，痛楚随违逆程度递增，直至爆体。这不是惩罚，是规则本身在杀人。\n三、唯死可断。立契者一方身死，血契自解。故魔域有「杀人解契」之术，为下三滥所不为，却从未禁绝。\n\n立契前想清楚——魔域的人常说这句话。因为立完之后，就不必再想了。', 3, '2026-10-26 09:02:43');
INSERT INTO `demon_archive` VALUES (15, 'law', '论道台铁律', '铁律 · 台上论道不取命', '台上论道不取命，台下私斗不论。', '魔域嗜杀，却有唯一一处杀不得的地方——宗门论道台。\n\n台上之律仅一条：论道不取命。可驳、可辱、可败人修为，不可断人性命。犯者，无论出自何族何宗，诸宗共讨。\n\n此律的来由并不高尚：上古魔族内斗不休，强者互杀至血流成河，几近自灭。旧枢机遂立此台，把「杀」改成「论」——让强者有一个不必杀人的胜负场。\n\n台下则无此约束。所谓「台上论道，台下私斗」，两不相干。故论道台四周永远比台上热闹。', 4, '2026-10-26 09:02:43');
INSERT INTO `demon_archive` VALUES (16, 'law', '疆界与残星台', '地志 · 九域一界', '魔域九域，残星台为界碑，域外即虚无。', '魔域共分九域：血煞峰居中，万骨荒原在北，血海在东，幻月渊在南，无底魔渊在西，冥河横贯，煞风峡为咽喉，万魔窟为腹地，残星台为西界之碑。\n\n九域之间无城墙，只有界碑。碑以骨石所立，上刻三禁忌与域主之名。跨碑不入他人之域，是魔域少有的、各族共守的礼。\n\n残星台最特殊：它既非某族之域，也不归枢机直辖。台在魔域边缘，台外便是虚无——无煞气、无光、无声。星陨遗族便守在此处，既守界，也守着自身血脉的最后一点星火。\n\n九域之外尚有一处不计入疆界之地：魔域演武场。此地为九域共有的比斗之所，不属任何一域，故不入界碑之数。\n\n演武场中立三榜：英杰榜按煞气排次，论道榜按答论多寡排次，切磋榜按胜场排次。三榜并列于场前，是九域修者最常驻足之处。\n\n另有一处残界也不计入九域：域外秘境。秘境非地，乃九域之外的废土与遗墟，枢机印所刻共九处，另有律条，见「秘境之律」。', 5, '2026-10-26 09:02:43');
INSERT INTO `demon_archive` VALUES (17, 'ancient', '魔天之战', '上古 · 魔族与天道的第一次正面交锋', '魔族倾巢而上，欲碎天道枷锁，终成两败。', '魔天之战，是魔域史上唯一一次全族同向的战争。\n\n起因简单到近乎狂妄：旧枢机立于血煞峰顶，问了一句——凭什么仙道可上，魔道不可？于是九域同应，倾巢而上。\n\n战况无籍可考，只留下三个事实：\n其一，血云至今未散。战时撕裂的天幕没有愈合，成为魔域终年不散的奇景。\n其二，九域至此残缺。原有十三域，战后仅余其九，四域沉入虚无。\n其三，旧枢机再未归来。其最后一道命令传遍九域：散。\n\n史载此战「两败」。天道未碎，魔族未灭。但真正知道内情的老修者说：不是两败，是天道根本没把这一战当回事。', 1, '2026-10-26 09:02:43');
INSERT INTO `demon_archive` VALUES (18, 'ancient', '旧枢机覆灭', '上古 · 上一代魔域之主如何崩解', '旧枢机陨落于魔天之战，枢机之位空悬至今。', '枢机者，魔域之主，统九域、掌三禁、持魔主真名。上一代枢机，是魔域最后一个真正的共主。\n\n其覆灭并非战死——魔天之战后他回到血煞峰，独自入关，再未开门。三日后，峰顶血云倒卷，枢机印碎为九片，散落九域。\n\n自此枢机之位空悬。九域各执一片印，谁也不肯交，谁也凑不齐。所谓「魔域无主」，并非无人想当，而是规则要求九印合一，而九位域主互相牵制，无人可成。\n\n有趣的是：魔域反而因此安稳了数千年。老修者说，枢机在时，魔域是一把刀；枢机没了，魔域成了九把刀——九把刀互相看着，反而谁都不敢先动。', 2, '2026-10-26 09:02:43');
INSERT INTO `demon_archive` VALUES (19, 'ancient', '九幽裂封', '上古 · 无底魔渊封印的由来', '魔渊第九层之下所封何物，无人知晓，亦无人敢问。', '无底魔渊分九层，前八层为历炼之地，第九层为禁区。而禁令之下的真正秘密是：第九层并非底。\n\n渊底有一道裂口，裂口之下另有其物。上古大战末期，四域沉入虚无，虚无中的东西顺着裂口往上爬。旧枢机以自身三百年道行、九域三万修者之血，封了那道裂口。\n\n封印名为「九幽裂封」，其形为九道锁链，各系一域。九链同紧则封固，一链断则封松。这也是为何九域界碑不容轻犯——界碑既是疆界，也是锁链的锚点。\n\n典册中关于裂口的记载到此为止。再往下的字句被人为抹去，抹得极干净，连刀痕都没有。', 3, '2026-10-26 09:02:43');
INSERT INTO `demon_archive` VALUES (20, 'ancient', '魔主失踪', '近古 · 血煞峰闭关三千年', '魔祖为十境之第八境，其上尚有魔圣、无上。魔域今世踏入此境者，唯他一人。', '魔祖，非境之极，乃十境之第八境。然魔域今世，踏入此境者唯他一人。\n\n三千年前，他登上血煞峰，留下一句话便闭了关：待我出来，把天拉下来。\n\n三千年间，峰顶血云不散、不落、不散形，每百年轰鸣一次如心跳。九域域主每百年上山一次，至峰腰而止，无人敢再上一步。\n\n有人说他早已坐化，血云是其道行所化；也有人说他在等一件事，等到了自然开门。第三说最不着调，却在市井流传最广——说他其实已经出来了，只是没人认得出来。\n\n魔祖之上，尚有魔圣、无上。此二境从未有人踏足，故典册只列名，不载其详。\n\n魔祖闭关之前，曾于天梯之末留一句批注：上面两步，我亦不知。你自己去看。此批注至今仍在，字是新的，墨未干。', 4, '2026-10-26 09:02:43');
INSERT INTO `demon_archive` VALUES (21, 'lineage', '魔种之分', '血脉 · 四种种裔，修行之本', '凡入魔途者必先择魔种。魔种定其天赋，一旦择定终身不改。', '「种族」与「魔种」，是两回事。\n\n种族是血脉所出，生而定之：血煞、幽冥、噬魂、幻月、骨魔、兽魔、星陨，此七族各有其土、其俗、其长。\n\n魔种则是修行之本——凡入魔途者，无论出身何族，皆须择一种为基。魔种一旦择定，终身不改，故择种之慎，不下于择师。\n\n魔域通行四魔种：\n\n一曰「血煞种」，其性战意滔天。\n二曰「幽冥种」，其性幽息吞吐。\n三曰「噬魂种」，其性夺魂摄魄。\n四曰「虚妄种」，其性虚妄顿悟。\n\n虚妄种最晚出，源自幻月一族的一支旁脉。那一支把幻术练到了极处，练到连自己也分不清真假，遂自号「虚妄」。幻月本族不认这一支，虚妄也不认幻月。\n\n四魔种之利，后四篇分述。', 7, '2026-11-03 03:19:02');
INSERT INTO `demon_archive` VALUES (22, 'lineage', '血煞种 · 战意滔天', '魔种 · 切磋战力增半成', '血煞入体，愈战愈勇。与人切磋时战力大涨，正面硬撼无人可挡。', '血煞种修者，煞气中自带一股战意。战意不增修为，却增战力——切磋之时，战力另增半成。\n\n半成看似微末，然境界相当者相斗，胜负往往就在半成之间。故演武场上有句实话：同境之下，宁惹幽冥，莫惹血煞。\n\n血煞种的麻烦在于收不住。战意一起，便只想打，不想走；打得越久，越不想停。九域之中因战意失控、斗至力竭者，十有其七是血煞种。\n\n族中定规：血煞种修者须以心魔为锁，日省三遍，否则战意反噬，先伤己身。\n\n此种种裔多出血煞一族，然他族亦可择之。择者多半是压不住脾气的那一类。', 8, '2026-11-03 03:19:02');
INSERT INTO `demon_archive` VALUES (23, 'lineage', '幽冥种 · 幽息吞吐', '魔种 · 参悟所得增三成半', '幽冥之体，静默纳煞。参悟魔域所得煞气远胜他人，进境最速。', '幽冥种不善战，却最善「纳」。\n\n同样是静坐参悟一日，幽冥种所得煞气可增三成半。此增非一时之利，而是日日如此——故幽冥种修者的进境，往往在同辈之中最速。\n\n魔域有句不太客气的评语：血煞种赢在今日，幽冥种赢在十年后。\n\n幽冥种的代价是慢热。其战力无所增益，切磋时亦无所长，早年常被人欺负。然熬到魔将、血煞之境，煞气之厚往往冠绝同辈，届时不必出手，位阶自压人一头。\n\n幽冥一族多出此种种裔，然他族亦可择之。择者多半是性子静的那一类——这也是幽冥种修者香从不曾断过的缘由。', 9, '2026-11-03 03:19:02');
INSERT INTO `demon_archive` VALUES (24, 'lineage', '噬魂种 · 夺魂摄魄', '魔种 · 胜者掠夺倍增', '噬魂之术，胜则食髓。切磋得胜时可将败者煞气一并吞下，掠夺倍增。', '噬魂种是四魔种中最招人厌的一种，也是最实用的一种。\n\n其利只有一条：切磋得胜时，可将败者煞气一并吞下，且所得倍增。\n\n此术不算禁术——演武场铁律只禁取命，不禁夺煞。故噬魂种在演武场上合法地令人头疼：赢了拿双份，输了也不过是输。\n\n噬魂一族的族规却极严：可吞异族之魂，不可吞同族之魂。犯者体内诸魂相冲，神魂撕裂而亡。此规本是族内之事，久而久之竟成了九域通行的默契——连外族修者都晓得，别对噬魂种的同族下手太狠。\n\n典册附注：噬魂种之利止于切磋。秘境之中无败者可夺，故入秘境者多不择此。', 10, '2026-11-03 03:19:02');
INSERT INTO `demon_archive` VALUES (25, 'lineage', '虚妄种 · 虚妄顿悟', '魔种 · 参悟三成机率顿悟翻倍', '虚妄之心，真假难辨。参悟时偶得顿悟，煞气瞬间翻倍。', '虚妄种是四魔种中最难测的一种。\n\n其利：参悟之时，约三成机率得「顿悟」，当次所得煞气翻倍。\n\n三成不低，翻倍的诱惑也不小。然虚妄种的问题在于「不匀」——同为参悟百日，有人顿悟三十次，有人顿悟十次，全凭运气。性子急的人受不了这个。\n\n更麻烦的是那句族训：虚妄者，自亦不识其妄。虚妄种修者偶尔会分不清自己是不是真的顿悟了。九域有过数起记载：某修者宣称顿悟，煞气却分毫未增，争辩至死不休。\n\n虚妄一族出自幻月旁脉，本族不认，遂独立成种。其族人极少，然演武榜上排名忽高忽低、大起大落者，多半是虚妄种。', 11, '2026-11-03 03:19:02');
INSERT INTO `demon_archive` VALUES (26, 'path', '参悟与机缘', '心法 · 入魔域而纳煞，偶得奇遇', '参悟为煞气最根本的来处，每日有定数；魔种、转世、习得功法皆增其数。行于魔域，约一成二之机得机缘奇遇，所得翻倍。', '参悟者，修者于魔域之中静坐纳煞，此为煞气最根本的来处。一日可参悟十次，每次所得煞气若干，过十则心神不继，当日不能再悟。\n\n其数不固定，随三事而变：\n\n一曰「魔种」——幽冥种纳煞远胜他人，所得可增三成半；此乃血脉所定，改不得。\n二曰「转世」——曾历轮回者，每重修一世则增一成所受，累世叠加，此谓重修加成。\n三曰「习得」——功法阁中习得的功法愈多，参悟愈易，每门增半成，至四门而封顶。\n\n三者相乘，故同为一日参悟，所得可差数倍。\n\n参悟之外，行于魔域亦有机缘。约每八次参悟，便有一次机缘奇遇——或得残卷，或遇异象，所得煞气翻倍。机缘不由人求，亦不由人避，故魔域修者多信一句话：走得多了，总会撞上。', 6, '2026-11-03 03:19:02');
INSERT INTO `demon_archive` VALUES (27, 'path', '每日焚香', '仪轨 · 一日一炷，连则递增', '每日焚香一炷可得煞气，连日不断则所得日增；连满七日另有厚赐，断则归零从头再计。', '焚香是魔域最琐碎、也最见心性的仪轨。每日一炷，香尽则得煞气十。\n\n其要在「连」：连焚一日，所得增五；连焚两日，增十；如此递增，至连满七日而止。连满七日者，另得厚赐五十，此后不再增。\n\n断一日，则前功尽弃，重新从第一炷计起。此规最是狠心——断了九日的人，与从未焚过的人毫无分别。\n\n九域各族对焚香的态度，恰好能看出其性子：血煞一族常因斗殴断了香而懊悔半日；幽冥一族则无一例外，香从不曾断过。\n\n典册有载：魔域之内，最后登临无上者，未必是天赋最高的那个，多半是香从未断过的那个。', 7, '2026-11-03 03:19:02');
INSERT INTO `demon_archive` VALUES (28, 'path', '研读与习得', '法门 · 读典册而悟，习功法而固', '研读典册篇目一日不过五，习得功法则永久增益参悟与秘境所得，至四门而封顶。', '魔枢典册非摆设，读之有益。\n\n「研读」者，就一篇条目细读到底，当次悟得煞气八。然心神有耗，一日所读不过五篇，读过则当日不能再读。同一篇目反复研读，所得递减——典册的原话是：读一遍是悟，读十遍是执。\n\n「习得」者，于功法阁中择一功法，习之于身。习得之功法不占时日，然其效长久：每习得一门，参悟与秘境所得皆增半成，累至四门而封顶。\n\n故魔域修者有两派：一派贪多，功法习了满身，却无一精通；一派求稳，只习三两门，日日研读。两派争执千年，至今无定论。\n\n唯有一条是共识：功法阁的门槛在各处不同，境界不到的，门自不开，强求无益。', 8, '2026-11-03 03:19:02');
INSERT INTO `demon_archive` VALUES (29, 'path', '切磋与战绩', '斗法 · 战力由位阶与煞气共定', '切磋战力 = 境界位阶 × 八千 + 周身煞气；一日不过五战，胜得十五、败得十，胜负有籍入切磋榜。', '魔域好斗，却非乱斗。宗门论道台之外，另有魔域演武场，专司切磋。\n\n战力之算，典册有明式：\n\n　　战力 = 境界位阶 × 八千 + 周身煞气\n\n位阶即十境之序：引煞为零，凝煞为一，魔卒为二，以此类推，无上为九。故同是五千煞气，魔将之躯胜魔卒三万有余——境界之差，非煞气可填。\n\n临阵尚有浮动，胜负非全由数定，故强弱相当者相斗，往往要看当日之手气。\n\n一日不过五战。胜者得煞十五，败者亦得十——魔域的规矩是：敢上台的都算修行，输了也有所得。\n\n血煞种修者切磋时战力另增半成，噬魂种胜时可将败者煞气一并吞下，此二者为魔种之利，诸族皆认。\n\n胜负有籍：每战皆录入册，胜则记一胜，败则记一负。切磋榜按胜场排次，与英杰榜、论道榜并列于演武场前。', 9, '2026-11-03 03:19:02');
INSERT INTO `demon_archive` VALUES (30, 'path', '转世重修', '轮回 · 无上尽头，重入引煞', '登临无上者可自散修为转世重修。重修一世，参悟与秘境所得皆增一成，世数永镌于名。', '无上是天梯之尽。尽了之后做什么？典册无载，魔祖也没说。\n\n于是有了转世。\n\n转世者，非死，而是自散一身修为，从引煞重头来过。煞气归零，境界归零，唯有两样东西留着：一是名，二是「世」。\n\n每转世一次，参悟与秘境所得增一成，累世叠加。第三世者所得是第一世的一成二分，第十世者近乎翻倍。此增与魔种、习得相乘，故老修者即便重头再来，进境也远快于初入魔途之时。\n\n魔域对转世的看法两极：一派视之为懦——登了顶又不敢待在顶上；另一派视之为勇——敢把万年道行一把撒了，从头再走。\n\n血煞峰顶有一块无字碑，转世者多在碑上刻一笔。碑上至今不过寥寥数十道刻痕，而魔域修者数以万计。', 10, '2026-11-03 03:19:02');
INSERT INTO `demon_archive` VALUES (31, 'law', '秘境之律', '铁律 · 入有耗，日有数，凶险自担', '域外秘境须耗煞启门，每日所入有定数，境界不足者不得入；境中有凶险亦有机缘，生死各安天命。', '域外秘境，非九域之地，乃九域之外的残界、废土、遗墟。枢机印所刻，共九处。\n\n入秘境之律有四：\n\n一、启门须耗煞。每处秘境皆有入场之耗，凶险越高，耗得越狠——低险者不耗，中险耗二十，高险耗六十，绝境耗一百五十。煞气不足者，门不开。\n\n二、每日所入有定数。低险一日五进，中险三进，高险两进，绝境一进。此数由枢机印自行记算，跨子夜则重置。\n\n三、境界不足者不得入。每处秘境皆标开启境界，魔卒以下不得涉足。此律非为护短——是真进去了会死。\n\n四、境中凶险自担。中险之处约两成机率负伤，高险三成，绝境近半；负伤者所得折半，至多折三百。亦有机缘之率：中险一成二，高险一成八，绝境两成半；撞上则所得翻倍，并可拾得功法残卷。\n\n守关之物各境不同，有狐、有隼、有骨将、有渡魂、有古魔，皆非活物，乃秘境自生的「象」。斩之不尽，杀之复生，故秘境不可「打穿」，只能「进出」。\n\n此四律，枢机印所刻，九域共守。然亦有修者私下议论：所谓凶险与机缘，究竟是秘境给的，还是枢机印掷的骰子——此问无人敢答。', 6, '2026-11-03 03:19:02');
INSERT INTO `demon_archive` VALUES (32, 'law', '演武场之律', '铁律 · 台上论道不取命，台下切磋不追杀', '切磋只分胜负，不取性命；胜者不得追击，败者不得寻仇。违者诸宗共讨。', '魔域嗜杀，却有两处杀不得：一是宗门论道台，二是魔域演武场。\n\n论道台之律在前篇已述：论道不取命。\n\n演武场之律则多了两条：\n\n一、切磋只分胜负，不取性命。战力由位阶与煞气共定，胜负有据，无可争辩。\n二、胜者不得追击，败者不得寻仇。切磋出场的那一刻，恩怨即清；再纠缠者，诸宗共讨。\n\n论道之律另计：答中一论得煞十，答错则失五，一日不过十论。论道榜按所答多寡排次，与切磋榜、英杰榜并列。\n\n此律的来由与论道台同：上古魔族内斗不休，强者互杀至血流成河，几近自灭。旧枢机遂立此二地，给魔域留两处可以败、也可以认的地方。\n\n噬魂种「夺煞」之术在演武场上不违此律——煞气非性命。然败者散场之后若被追杀，追杀者不论何族何宗，一律诛。', 7, '2026-11-03 03:19:02');
INSERT INTO `demon_archive` VALUES (33, 'law', '典册封禁之律', '铁律 · 五处要地，各有其境', '功法阁无门槛，魔卒可入秘境、可登论道台，魔尉可窥魔典，魔将方执典册。未至其境者，卷自不开。', '魔域的要地与典籍分五处，各有境界门槛，非为藏私，为的是不该看的别看。\n\n一、功法阁：无所门槛。凡入魔途者皆可入，然阁中功法各有其境，境不到则习不得——门开着，书取不下来。\n二、域外秘境：魔卒境可入。秘境凶险，未至魔卒者进去了会死，此律非为护短，是护命。\n三、宗门论道台：魔卒境可登。论道需有根底，引煞、凝煞者上去多半答不出，徒受辱。\n四、魔典秘录：魔尉境可阅。秘录所载多涉各族隐事，魔尉以下读了容易惹祸。\n五、魔枢典册：魔将境可阅。典册载十境天梯、渡劫之法、转世之秘，未至魔将者读之，多半走火入魔。\n\n此律由枢机印自执，非人力所守。境界不到者去翻，卷上之字会自行游走，读不成句；境界够了，字自归位。\n\n故魔域修者少有偷典册的——不是不敢，是偷了也读不出。\n\n典册按四部分类：魔族谱系、魔修道途、魔域法则、上古秘录。开卷者当自谱系始，循序渐进，此为历代枢机的建议，非律。', 8, '2026-11-03 03:19:02');
INSERT INTO `demon_archive` VALUES (34, 'ancient', '煞气本源', '上古 · 魔域何以为魔域', '煞气非魔域所产，乃魔天之战撕裂天幕所遗。天口至今未合，煞自裂口而入，至今不绝。', '魔域并非历来如此。\n\n上古之时，此处本是寻常山川，有河、有树、有人。魔天之战打破了天幕——战时撕裂的天口至今未合，血云悬于其上，终年不散。\n\n从裂口里渗下来的，不是血，是「煞」。\n\n煞气无形无色，然万物触之则变：兽化魔兽，木化魔木，人……化魔修。凡在此地久居者，体内皆有煞，只是多寡不同。故严格说来，魔域之人并非「修」出了煞气，而是本来就有，修行只是把它聚起来、用出来。\n\n这也解释了那句老话：为何魔修破境不折损旧煞。煞气本就在身，散不去，也不必散——此乃「累积」之法的根由。\n\n此说最早见于一册无名残卷，卷末有一句：所谓引煞，不过是想起自己本是什么。\n\n残卷现存于魔枢典册第九架，无撰者名，无年代。历代枢机皆批注同一句话：存而不论。', 5, '2026-11-03 03:19:02');
INSERT INTO `demon_archive` VALUES (35, 'ancient', '无上之境', '上古 · 天梯尽头的空白', '十境之末名「无上」，然此境为何，典册只载其名，不载其详——不是被撕了，是本来就没写。', '天梯十阶，前九阶皆有注，唯第十阶无上之下，是一片空白。\n\n不是被人撕了，是本来就没写。\n\n旧枢机曾于此处留批：此境未有人至，故无可载。后之来者，自补之。\n\n三千年过去，空白仍在。期间登临魔祖者一人，魔圣者零人，无上者零人。有人说魔祖其实已至，只是未曾回来报备；也有人说根本到不了——五万之数是个虚数，真正的阈在更远的地方。\n\n于是有了转世。修者到了顶，无路可走，便自散修为，从头再走一遍。第十阶的空白，某种意义上被绕过去了。\n\n有人说转世是逃避，也有人说转世才是答案：既然此境无载，那便由走过九遍的人，自己去写第十遍。\n\n典册对此不加评议，只于页脚刻了一行小字：\n\n　　空白不是没有，是还没人写。', 6, '2026-11-03 03:19:02');

-- ----------------------------
-- Table structure for demon_art
-- ----------------------------
DROP TABLE IF EXISTS `demon_art`;
CREATE TABLE `demon_art`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '功法名',
  `category` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '类别：攻伐/护体/幻术/炼魂/御兽',
  `grade` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '品阶：黄/玄/地/天/仙',
  `realm_req` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '所需境界',
  `summary` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '简介',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '正文',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `image` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '配图相对路径',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 15 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '功法阁' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of demon_art
-- ----------------------------
INSERT INTO `demon_art` VALUES (1, '血煞戮天诀', '攻伐', '仙', '血煞', '以血煞之气凝刃，一念可斩千军。', '引周身精血化煞刃，血气越盛则刃越利；然反噬亦烈，需以心魔为锁。', '2026-10-26 09:02:42', '/images/art/art-1.jpg');
INSERT INTO `demon_art` VALUES (2, '幽冥护魂咒', '护体', '天', '魔尉', '布幽冥屏障，万法不侵。', '以幽冥之气覆体，外邪难入；久施则神识渐冷，需常返阳间温养。', '2026-10-26 09:02:42', '/images/art/art-2.jpg');
INSERT INTO `demon_art` VALUES (3, '噬魂幻月', '幻术', '天', '魔将', '织幻境如月华，惑敌五识。', '以月华为引织就幻网，敌入其间则真伪难辨；破法唯在守心。', '2026-10-26 09:02:42', '/images/art/art-3.jpg');
INSERT INTO `demon_art` VALUES (4, '炼魂锁魄', '炼魂', '天', '魔尉', '锁敌魂魄，炼化为己用。', '以咒锁敌三魂七魄，可炼为傀或吞而补己；慎用，恐业障缠身。', '2026-10-26 09:02:42', '/images/art/art-4.jpg');
INSERT INTO `demon_art` VALUES (5, '焚天业火', '攻伐', '天', '魔将', '引业火焚身，敌我皆烬。', '召业火焚尽四方，杀伤无匹；施者亦受灼伤，乃双刃之术。', '2026-10-26 09:02:42', '/images/art/art-5.jpg');
INSERT INTO `demon_art` VALUES (6, '御兽血契', '御兽', '天', '魔卒', '与异兽立血契，共赴杀伐。', '滴血为契，与魔渊异兽同心；兽死则契损，须善待之', '2026-10-26 09:02:42', '/images/art/art-6.jpg');
INSERT INTO `demon_art` VALUES (7, '九幽寒煞功', '攻伐', '地', '魔尉', '引九幽寒气凝煞，冻敌于瞬息。', '以寒煞封脉，敌血凝而僵；然寒煞伤己阳元，须以血煞制衡。', '2026-10-26 09:02:42', '/images/art/art-7.jpg');
INSERT INTO `demon_art` VALUES (8, '万鬼夜行术', '幻术', '天', '魔将', '召万鬼夜行，遮天蔽月。', '念动则群鬼出，惑敌心智、乱其神识；施者神魂消耗甚巨。', '2026-10-26 09:02:42', '/images/art/art-8.jpg');
INSERT INTO `demon_art` VALUES (9, '噬骨魔音', '攻伐', '玄', '魔卒', '以魔音蚀骨，远攻之术。', '魔音入耳则骨酥魂颤，可破护体罡气；闻者需封耳守心。', '2026-10-26 09:02:42', '/images/art/art-9.jpg');
INSERT INTO `demon_art` VALUES (10, '玄阴养魂诀', '炼魂', '地', '引煞', '温养神魂，稳固道基。', '以玄阴之气滋养三魂，久修则神识清明、抗幻力增。', '2026-10-26 09:02:42', '/images/art/art-10.jpg');
INSERT INTO `demon_art` VALUES (11, '血河大阵', '护体', '天', '魔将', '布血河为阵，万法不侵。', '以己血绘阵，成血河屏障，外力难破；阵破则反噬攻心。', '2026-10-26 09:02:42', '/images/art/art-11.jpg');
INSERT INTO `demon_art` VALUES (12, '傀儡替死术', '御兽', '地', '凝煞', '炼傀儡代受死劫。', '以本命精血饲傀，临劫可替主受创；傀毁则精血大损。', '2026-10-26 09:02:42', '/images/art/art-12.jpg');

-- ----------------------------
-- Table structure for demon_codex
-- ----------------------------
DROP TABLE IF EXISTS `demon_codex`;
CREATE TABLE `demon_codex`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '典籍名',
  `volume` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '卷册',
  `summary` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '摘要',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `image` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '配图相对路径',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '魔典秘录' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of demon_codex
-- ----------------------------
INSERT INTO `demon_codex` VALUES (1, '太古魔典', '卷一·开辟', '记述魔域开天辟地之秘，言魔气自混沌中来。', '2026-10-26 09:02:42', '/images/codex/codex-1.jpg');
INSERT INTO `demon_codex` VALUES (2, '噬魂真解', '卷三·炼魂', '详述炼魂锁魄之法门与禁忌，阅前须立护心咒。', '2026-10-26 09:02:42', '/images/codex/codex-2.jpg');
INSERT INTO `demon_codex` VALUES (3, '血煞图谱', '卷七·攻伐', '血煞一脉功法源流与图谱，脉络清晰。', '2026-10-26 09:02:42', '/images/codex/codex-3.jpg');
INSERT INTO `demon_codex` VALUES (4, '幽冥往生录', '卷九·往生', '幽冥轮回之说，真假难辨，录以备参。', '2026-10-26 09:02:42', '/images/codex/codex-4.jpg');
INSERT INTO `demon_codex` VALUES (5, '禁忌之扉', '卷末·禁忌', '封印于魔域最深处之禁忌知识，擅窥者或遭反噬。', '2026-10-26 09:02:42', '/images/codex/codex-5.jpg');
INSERT INTO `demon_codex` VALUES (6, '万兽图录', '卷四·御兽', '记魔渊异兽形貌习性，御兽者必参。', '2026-10-26 09:02:42', '/images/codex/codex-6.jpg');
INSERT INTO `demon_codex` VALUES (7, '寒煞秘要', '卷六·攻伐', '寒煞一脉修炼次第与破法，循序渐进。', '2026-10-26 09:02:42', '/images/codex/codex-7.jpg');
INSERT INTO `demon_codex` VALUES (8, '冥河志异', '卷十·异闻', '冥河两岸奇闻怪谈，虚实参半，录以警世。', '2026-10-26 09:02:42', '/images/codex/codex-8.jpg');
INSERT INTO `demon_codex` VALUES (9, '星陨录', '卷十一·天象', '残星坠落之兆与魔劫关联考，关乎魔域气运。', '2026-10-26 09:02:42', '/images/codex/codex-9.jpg');

-- ----------------------------
-- Table structure for demon_debate
-- ----------------------------
DROP TABLE IF EXISTS `demon_debate`;
CREATE TABLE `demon_debate`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '论题',
  `speaker` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '论者',
  `summary` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '摘要',
  `option_a` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '选项 A',
  `option_b` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '选项 B',
  `option_c` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '选项 C',
  `answer` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '正确选项 A/B/C',
  `analysis` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '论道解析',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 21 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '宗门论道' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of demon_debate
-- ----------------------------
INSERT INTO `demon_debate` VALUES (1, '魔非魔，道非道', '魔祖', '顺逆由心，何须外求；魔与道，不过一念之转。', '魔由心生，道由心灭', '魔道本一体，一念之转', '魔即魔，道即道，永不相容', 'B', '万法唯心所现，魔与道不过执念两分；顺逆由心，转念即是转境。', '2026-10-26 09:02:42');
INSERT INTO `demon_debate` VALUES (2, '血契之辩', '血煞尊者', '以血为契，究竟是束缚，还是自由？', '血契是束缚，斩之即自由', '血契是枷锁，亦是修行的磨刀石', '自由与束缚皆在己心，血契只是镜', 'C', '以血为契，缚的是身，试的是心；视其为磨刀石还是牢笼，只在一念。', '2026-10-26 09:02:42');
INSERT INTO `demon_debate` VALUES (3, '杀伐与慈悲', '幽冥夫人', '魔道修行，可存一线慈悲否？', '魔道无情，慈悲是弱者的伪装', '杀伐为主，慈悲为佐，方为长久', '慈悲即杀伐，杀伐即慈悲', 'B', '一味杀伐则众叛亲离，一味慈悲则道基不固；一线慈悲存于杀伐之中，方是中道。', '2026-10-26 09:02:42');
INSERT INTO `demon_debate` VALUES (4, '长生孰真', '虚妄上人', '长生不过执念，何为真我？', '长生是真，寿与天齐即是解脱', '长生是妄，舍寿方见真我', '长生无所谓真假，体验本身即是意义', 'B', '执念于不死者，恰被不死所缚；放下长生之执，方能在有限中见真我。', '2026-10-26 09:02:42');
INSERT INTO `demon_debate` VALUES (5, '群魔共治', '噬魂大王', '魔域当立规制，还是任凭弱肉强食？', '弱肉强食即天理，规制是伪善', '当立规制，无序则魔域自毁', '规制由强者制定，强者更迭则规制更迭', 'B', '群魔共治需规制为骨：无规制的魔域终将在内耗中崩塌，强弱轮转亦需秩序承载。', '2026-10-26 09:02:42');
INSERT INTO `demon_debate` VALUES (6, '寂灭与轮回', '寂灭尊者', '魔修可证寂灭，抑或只入轮回？', '寂灭为归，轮回为苦', '轮回为归，寂灭为断', '寂灭与轮回皆途，道心所向即是归处', 'C', '执着寂灭是断，执着轮回是迷；魔修之路不在归处，而在道心所向。', '2026-10-26 09:02:42');
INSERT INTO `demon_debate` VALUES (7, '力量之价', '血河老祖', '无敌之力，是否必以本心为祭？', '力量即一切，本心可弃', '本心为本，力量为用，不可倒置', '力量与本心并行不悖，无需取舍', 'B', '以本心祭力量者，终成力量的傀儡；力为用、心为本，用不伤本方是正途。', '2026-10-26 09:02:42');
INSERT INTO `demon_debate` VALUES (8, '魔域之疆', '幽冥夫人', '魔域当有边界，还是无垠扩张？', '魔域当有边界，守成方安', '魔域当无垠扩张，停滞即死亡', '疆界在心不在土，心之所在即魔域', 'C', '有形之疆终有尽时，无形之疆随道心而长；守成与扩张皆是外相，心疆无边方是真无垠。', '2026-10-26 09:02:42');
INSERT INTO `demon_debate` VALUES (9, '煞气何来', '引煞长老', '魔道修行以煞气为基，世人皆言煞气乃杀伐之气，那么它究竟从何而生？', '众生执念所积，观魔域万象而自聚', '杀戮取命，唯杀生方得煞气', '天授之，生而有之', 'A', '煞气非杀伐所生，乃众生执念汇聚。故参悟魔域、论道辨析皆可纳煞，不必染血。', '2026-11-02 09:49:29');
INSERT INTO `demon_debate` VALUES (10, '境界压制之虚实', '萧寒山', '高境者临敌，其压制之力究竟源于境界本身，还是源于积累的煞气？', '纯粹是位阶之差，与煞气无关', '只看煞气多寡，位阶虚名而已', '位阶定根本，煞气填血肉，二者相合方成战力', 'C', '战力者，位阶乘千再加煞气。位阶是骨架，煞气是血肉。境界差两重以上，低者战力折损三成。', '2026-11-02 09:49:29');
INSERT INTO `demon_debate` VALUES (11, '四系魔种之辨', '虚妄上人', '血煞、幽冥、噬魂、虚妄四系，择其一而后终身为限，此择是否要紧？', '各系天赋不同，择种即定其修行偏向', '只是名号，全无实际差别', '可随时改易，随心而变', 'A', '四系各有所长：血煞善战、幽冥纳煞、噬魂掠夺、虚妄顿悟。择种，便是择一条路数的开头。', '2026-11-02 09:49:29');
INSERT INTO `demon_debate` VALUES (12, '心魔何以为魔', '寂灭尊者', '修行者走火入魔，究竟是被外力侵蚀，还是自心失守？', '外魔入侵，防之当在外', '心念一乱，自心即魔', '修行过急，必然走火', 'B', '魔自心生。外境只是引子，真正动摇道心的，从来都是自己的执念。', '2026-11-02 09:49:29');
INSERT INTO `demon_debate` VALUES (13, '切磋是否夺人道行', '血河老祖', '同门切磋，胜负既定便各有增损。那么切磋算不算夺人道行？', '不算，煞气往来如活水，流转方能精进', '算，故当禁止', '只在胜负悬殊时才算', 'A', '煞气不是死物。切磋取其流转，败者失、胜者得，如磨刀石相互砥砺，并非掠夺。', '2026-11-02 09:49:29');
INSERT INTO `demon_debate` VALUES (14, '境界与功法', '魔祖', '已至高位者，是否还需回头修习低阶功法？', '不必，境界已足，功法皆小道', '全看功法之名是否够响亮', '境界为体，功法为用，体用不可偏废', 'C', '境界是修为的容量，功法是运用的法门。空有境界而无功法，如持巨鼎而不知如何举。', '2026-11-02 09:49:29');
INSERT INTO `demon_debate` VALUES (15, '历练之所求', '燕孤鸿', '入历练之地，究竟为取宝、为杀敌、还是为阅世？', '为阅世，见天地而后知自身之限', '为杀，杀得多者功高', '为宝，有所得方不虚此行', 'A', '历练之本，在于见未曾见之物、遇未曾遇之险。宝物可失，所见长存。', '2026-11-02 09:49:29');
INSERT INTO `demon_debate` VALUES (16, '魔籍为何留名', '典册执笔', '修者一代代凋零，魔枢典册却仍载其名，意义何在？', '为留存来处，使后来者知今身所承', '为论功行赏，记其境界高低', '仅为管理之便，无甚深意', 'A', '典册记的不是名次，是来处。知其承，方知其往。', '2026-11-02 09:49:29');
INSERT INTO `demon_debate` VALUES (17, '掠夺之力有度否', '噬魂大王', '噬魂一系胜则夺人煞气，那么这种掠夺是否有上限？', '无上限，胜者当得其全部', '有上限，只取其该失之数，不敢尽取', '掠夺与胜负无关，只看境界差', 'B', '掠夺之力亦有分寸。噬魂一系胜时多得一份，而非尽夺。道有边际，方能长存。', '2026-11-02 09:49:29');
INSERT INTO `demon_debate` VALUES (18, '无上何解', '魔祖', '十境之末为「无上」。既名无上，是否意味修行到此为止？', '否，「无上」只是当前十重之顶，非道之尽头', '是，到此已极，无可再进', '无上即虚无，本质是放弃修行', 'A', '「无上」是这十重体系之顶，不是道的顶。名可尽，而道不可尽。', '2026-11-02 09:49:29');
INSERT INTO `demon_debate` VALUES (19, '静修与争锋', '幽冥夫人', '一方闭关千年不出，一方日日与人争锋，孰能更快进境？', '闭关者，心不散则进境速', '争锋者，切磋纳煞最快', '二者各有其途，唯不宜偏废', 'C', '闭关纳煞如储蓄，争锋切磋如练刀。蓄而不练则钝，练而不蓄则竭。', '2026-11-02 09:49:29');
INSERT INTO `demon_debate` VALUES (20, '道号与真我', '无名者', '修者以道号入籍受拜，那么道号是否影响其修行命途？', '影响，名入魔碑即是咒', '不影响，名只是名，来去无痕', '只在他人唤你时才短暂有意义', 'A', '道号入碑，朝夕被唤，久而久之便成了自家所认的「我是谁」。名不是咒，却比咒更长久。', '2026-11-02 09:49:29');

-- ----------------------------
-- Table structure for demon_explore_log
-- ----------------------------
DROP TABLE IF EXISTS `demon_explore_log`;
CREATE TABLE `demon_explore_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '参悟者 id',
  `content_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '内容类型：art 功法阁 / codex 魔典秘录 / archive 魔枢典册',
  `content_id` bigint NOT NULL COMMENT '内容 id',
  `title` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '内容标题快照',
  `gain` int NULL DEFAULT 0 COMMENT '本次所得煞气',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_day`(`user_id` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_user_content`(`user_id` ASC, `content_type` ASC, `content_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 31 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '参悟日志' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of demon_explore_log
-- ----------------------------
INSERT INTO `demon_explore_log` VALUES (11, 2, 'art', 9, '噬骨魔音', 6, '2026-11-04 00:55:02');
INSERT INTO `demon_explore_log` VALUES (12, 2, 'archive', 4, '幻月一族', 6, '2026-11-04 00:55:04');
INSERT INTO `demon_explore_log` VALUES (13, 2, 'art', 12, '傀儡替死术', 6, '2026-11-04 00:55:04');
INSERT INTO `demon_explore_log` VALUES (14, 2, 'art', 5, '焚天业火', 6, '2026-11-04 00:55:04');
INSERT INTO `demon_explore_log` VALUES (15, 2, 'archive', 32, '演武场之律', 6, '2026-11-04 00:55:05');
INSERT INTO `demon_explore_log` VALUES (16, 2, 'art', 6, '御兽血契', 6, '2026-11-04 00:55:05');
INSERT INTO `demon_explore_log` VALUES (17, 2, 'archive', 11, '渡劫与破境', 6, '2026-11-04 00:55:05');
INSERT INTO `demon_explore_log` VALUES (18, 2, 'archive', 21, '魔种之分', 6, '2026-11-04 00:55:05');
INSERT INTO `demon_explore_log` VALUES (19, 2, 'archive', 10, '修行法门三分', 6, '2026-11-04 00:55:06');
INSERT INTO `demon_explore_log` VALUES (20, 2, 'codex', 5, '禁忌之扉', 6, '2026-11-04 00:55:08');
INSERT INTO `demon_explore_log` VALUES (21, 25, 'archive', 2, '幽冥一族', 5, '2026-11-04 00:57:23');
INSERT INTO `demon_explore_log` VALUES (22, 25, 'archive', 1, '血煞一族', 5, '2026-11-04 00:57:24');
INSERT INTO `demon_explore_log` VALUES (23, 25, 'archive', 9, '魔力运转规则', 5, '2026-11-04 00:57:30');
INSERT INTO `demon_explore_log` VALUES (24, 25, 'archive', 24, '噬魂种 · 夺魂摄魄', 5, '2026-11-04 00:57:34');
INSERT INTO `demon_explore_log` VALUES (25, 25, 'art', 7, '九幽寒煞功', 5, '2026-11-04 00:57:34');
INSERT INTO `demon_explore_log` VALUES (26, 25, 'archive', 17, '魔天之战', 5, '2026-11-04 00:57:34');
INSERT INTO `demon_explore_log` VALUES (27, 25, 'art', 10, '玄阴养魂诀', 5, '2026-11-04 00:57:35');
INSERT INTO `demon_explore_log` VALUES (28, 25, 'archive', 14, '血契之律', 5, '2026-11-04 00:57:35');
INSERT INTO `demon_explore_log` VALUES (29, 25, 'art', 12, '傀儡替死术', 5, '2026-11-04 00:57:35');
INSERT INTO `demon_explore_log` VALUES (30, 25, 'archive', 21, '魔种之分', 5, '2026-11-04 00:57:35');

-- ----------------------------
-- Table structure for demon_friend
-- ----------------------------
DROP TABLE IF EXISTS `demon_friend`;
CREATE TABLE `demon_friend`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `friend_id` bigint NOT NULL,
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '0=鐢宠?涓?1=濂藉弸 2=宸叉嫆缁?宸插垹闄',
  `message` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '鐢宠?鐣欒█',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `handled_at` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_pair`(`user_id` ASC, `friend_id` ASC) USING BTREE,
  INDEX `idx_friend_id`(`friend_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '榄斿弸鍏崇郴锛堝惈鐢宠?涓庡ソ鍙嬶紝鍙屽悜瀵圭О瀛樺偍锛' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of demon_friend
-- ----------------------------
INSERT INTO `demon_friend` VALUES (7, 2, 25, 1, '愿结同道之好，共参魔道。', '2026-11-04 01:37:41', '2026-11-04 01:37:57', '2026-11-04 01:37:57');
INSERT INTO `demon_friend` VALUES (8, 25, 2, 1, NULL, '2026-11-04 01:37:57', '2026-11-04 01:37:57', NULL);

-- ----------------------------
-- Table structure for demon_message
-- ----------------------------
DROP TABLE IF EXISTS `demon_message`;
CREATE TABLE `demon_message`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `from_id` bigint NOT NULL,
  `to_id` bigint NOT NULL,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `msg_type` tinyint NOT NULL DEFAULT 0 COMMENT '0=鏂囧瓧 1=淇?偧蹇冨緱鍗＄墖',
  `ref_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'art/codex/archive',
  `ref_id` bigint NULL DEFAULT NULL,
  `ref_title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `ref_summary` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `read` tinyint NOT NULL DEFAULT 0 COMMENT '0=鏈?? 1=宸茶?',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_conv`(`from_id` ASC, `to_id` ASC) USING BTREE,
  INDEX `idx_to_unread`(`to_id` ASC, `read` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 23 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '浼犻煶绉佷俊' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of demon_message
-- ----------------------------
INSERT INTO `demon_message` VALUES (20, 2, 25, '血煞戮天诀', 1, 'art', 1, '血煞戮天诀', '以血煞之气凝刃，一念可斩千军。', 1, '2026-11-04 01:39:39');
INSERT INTO `demon_message` VALUES (21, 25, 2, '谢谢魔祖', 0, NULL, NULL, NULL, NULL, 1, '2026-11-04 01:40:54');
INSERT INTO `demon_message` VALUES (22, 2, 25, '血煞图谱', 1, 'codex', 3, '血煞图谱', '血煞一脉功法源流与图谱，脉络清晰。', 0, '2026-11-04 01:42:07');

-- ----------------------------
-- Table structure for demon_origin
-- ----------------------------
DROP TABLE IF EXISTS `demon_origin`;
CREATE TABLE `demon_origin`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `origin` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '魔种名（与 demon_user.realm 对应）',
  `title` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '天赋名',
  `description` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '天赋描述',
  `explore_bonus` decimal(4, 2) NOT NULL DEFAULT 0.00 COMMENT '参悟煞气加成倍率',
  `spar_power_bonus` decimal(4, 2) NOT NULL DEFAULT 0.00 COMMENT '切磋战力加成倍率',
  `spar_reward_bonus` decimal(4, 2) NOT NULL DEFAULT 0.00 COMMENT '切磋胜利额外奖励倍率',
  `insight_chance` decimal(4, 2) NOT NULL DEFAULT 0.00 COMMENT '参悟顿悟（双倍）概率',
  `sort` int NOT NULL DEFAULT 0 COMMENT '展示顺序',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_origin`(`origin` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '魔种天赋配置（四系差异化）' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of demon_origin
-- ----------------------------
INSERT INTO `demon_origin` VALUES (1, '血煞', '战意滔天', '血煞入体，愈战愈勇。与人切磋时战力大涨，正面硬撼无人可挡。', 0.00, 0.05, 0.00, 0.00, 1, '2026-11-02 09:42:19', '2026-11-03 02:08:25');
INSERT INTO `demon_origin` VALUES (2, '幽冥', '幽息吞吐', '幽冥之体，静默纳煞。参悟魔域所得煞气远胜他人，进境最速。', 0.35, 0.00, 0.00, 0.00, 2, '2026-11-02 09:42:19', '2026-11-03 02:08:25');
INSERT INTO `demon_origin` VALUES (3, '噬魂', '夺魂摄魄', '噬魂之术，胜则食髓。切磋得胜时可将败者煞气一并吞下，掠夺倍增。', 0.00, 0.00, 1.00, 0.00, 3, '2026-11-02 09:42:19', '2026-11-02 09:42:19');
INSERT INTO `demon_origin` VALUES (4, '虚妄', '虚妄顿悟', '虚妄之心，真假难辨。参悟时偶得顿悟，煞气瞬间翻倍。', 0.00, 0.00, 0.00, 0.30, 4, '2026-11-02 09:42:19', '2026-11-03 02:08:25');

-- ----------------------------
-- Table structure for demon_quiz_record
-- ----------------------------
DROP TABLE IF EXISTS `demon_quiz_record`;
CREATE TABLE `demon_quiz_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '作答修者 id',
  `debate_id` bigint NOT NULL COMMENT '论题 id（demon_debate.id）',
  `correct` tinyint NOT NULL DEFAULT 0 COMMENT '是否答对：1 是 / 0 否',
  `awarded` int NOT NULL DEFAULT 0 COMMENT '本次结算的煞气变动（可为负）',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '作答时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_debate`(`user_id` ASC, `debate_id` ASC) USING BTREE COMMENT '同一人同一题仅一次，防重复刷分',
  INDEX `idx_user`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '论道答题记录（防重复刷分）' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of demon_quiz_record
-- ----------------------------
INSERT INTO `demon_quiz_record` VALUES (10, 2, 13, 0, -5, '2026-11-03 02:31:09');
INSERT INTO `demon_quiz_record` VALUES (11, 2, 18, 1, 10, '2026-11-03 02:32:12');

-- ----------------------------
-- Table structure for demon_read_record
-- ----------------------------
DROP TABLE IF EXISTS `demon_read_record`;
CREATE TABLE `demon_read_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `module` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'art/trial/debate/codex/archive',
  `ref_id` bigint NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_ref`(`user_id` ASC, `module` ASC, `ref_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '修者研读记录（图鉴收集度）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of demon_read_record
-- ----------------------------
INSERT INTO `demon_read_record` VALUES (2, 2, 'art', 1, '2026-11-03 02:31:38');
INSERT INTO `demon_read_record` VALUES (3, 2, 'trial', 7, '2026-11-03 02:36:00');
INSERT INTO `demon_read_record` VALUES (4, 2, 'trial', 1, '2026-11-03 02:36:14');
INSERT INTO `demon_read_record` VALUES (5, 2, 'trial', 6, '2026-11-03 02:36:23');
INSERT INTO `demon_read_record` VALUES (6, 2, 'codex', 6, '2026-11-03 02:36:34');
INSERT INTO `demon_read_record` VALUES (7, 2, 'trial', 9, '2026-11-03 02:39:07');
INSERT INTO `demon_read_record` VALUES (8, 2, 'art', 9, '2026-11-04 00:32:48');
INSERT INTO `demon_read_record` VALUES (9, 25, 'art', 1, '2026-11-04 01:40:05');
INSERT INTO `demon_read_record` VALUES (10, 2, 'codex', 3, '2026-11-04 01:42:13');

-- ----------------------------
-- Table structure for demon_realm
-- ----------------------------
DROP TABLE IF EXISTS `demon_realm`;
CREATE TABLE `demon_realm`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `idx` int NOT NULL COMMENT '位阶（0 起，0=引煞，9=无上）',
  `name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '境界名',
  `title` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '境界称号',
  `description` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '境界描述',
  `threshold` int NULL DEFAULT 0 COMMENT '冲击该境所需煞气（引煞为 0）',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '落成时间',
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_idx`(`idx` ASC) USING BTREE,
  UNIQUE INDEX `uk_name`(`name` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '修行境界配置表（十境）' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of demon_realm
-- ----------------------------
INSERT INTO `demon_realm` VALUES (1, 0, '引煞', '初入魔门', '引煞入体，初踏魔途', 100, '2026-11-02 09:13:01', '2026-11-02 09:13:01');
INSERT INTO `demon_realm` VALUES (2, 1, '凝煞', '煞气凝形', '煞气凝形，根基渐固', 400, '2026-11-02 09:13:01', '2026-11-03 02:08:25');
INSERT INTO `demon_realm` VALUES (3, 2, '魔卒', '魔门行伍', '位列行伍，可入秘境', 1200, '2026-11-02 09:13:01', '2026-11-03 02:59:32');
INSERT INTO `demon_realm` VALUES (4, 3, '魔尉', '统御百夫', '百夫之长，可窥魔典', 3000, '2026-11-02 09:13:01', '2026-11-03 02:08:25');
INSERT INTO `demon_realm` VALUES (5, 4, '魔将', '坐镇一方', '坐镇一方，执掌典册', 6000, '2026-11-02 09:13:01', '2026-11-03 02:08:25');
INSERT INTO `demon_realm` VALUES (6, 5, '血煞', '威震魔域', '煞名远播，威震群魔', 10000, '2026-11-02 09:13:01', '2026-11-03 02:08:25');
INSERT INTO `demon_realm` VALUES (7, 6, '魔尊', '魔道至尊', '魔威盖世，群魔俯首', 16000, '2026-11-02 09:13:01', '2026-11-03 02:08:25');
INSERT INTO `demon_realm` VALUES (8, 7, '魔祖', '万法归魔', '一念通天，统御群修', 24000, '2026-11-02 09:13:01', '2026-11-03 02:08:25');
INSERT INTO `demon_realm` VALUES (9, 8, '魔圣', '圣魔临世', '魔道通玄，圣威昭昭', 35000, '2026-11-02 09:13:01', '2026-11-03 02:08:25');
INSERT INTO `demon_realm` VALUES (10, 9, '无上', '无上魔道', '超然物外，万法皆空', 50000, '2026-11-02 09:13:01', '2026-11-03 02:08:25');

-- ----------------------------
-- Table structure for demon_realm_log
-- ----------------------------
DROP TABLE IF EXISTS `demon_realm_log`;
CREATE TABLE `demon_realm_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '探索者 id',
  `trial_id` bigint NOT NULL COMMENT '所入秘境 id',
  `title` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '秘境名（快照）',
  `result` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '结果：treasure 机缘 / success 功成 / injured 负伤',
  `detail` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '一句话战报',
  `exp_gain` int NOT NULL DEFAULT 0 COMMENT '所得煞气（含机缘、已扣负伤）',
  `cost` int NOT NULL DEFAULT 0 COMMENT '入场消耗',
  `injury` int NOT NULL DEFAULT 0 COMMENT '负伤折损',
  `treasure` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '机缘所得宝物名',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_realm_log_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_realm_log_trial_time`(`trial_id` ASC, `created_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 27 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '秘境探索日志' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of demon_realm_log
-- ----------------------------
INSERT INTO `demon_realm_log` VALUES (1, 2, 3, '幻月迷宫', 'injured', '遇幻月魔狐阻道，血溅三丈——负伤遁出，所得折损 30 点。', 30, 20, 30, '', '2026-11-03 03:01:20');
INSERT INTO `demon_realm_log` VALUES (2, 2, 3, '幻月迷宫', 'treasure', '深入幻月迷宫，机缘天降，得《御兽血契》残卷，煞气暴涨。', 120, 20, 0, '御兽血契', '2026-11-03 03:01:21');
INSERT INTO `demon_realm_log` VALUES (3, 2, 3, '幻月迷宫', 'success', '闯幻月迷宫而还，斩妖伏魔，煞气尽入己身。', 60, 20, 0, '', '2026-11-03 03:01:22');
INSERT INTO `demon_realm_log` VALUES (4, 2, 1, '万骨荒原试炼', 'success', '闯万骨荒原试炼而还，斩妖伏魔，煞气尽入己身。', 123, 60, 0, '', '2026-11-03 03:08:39');
INSERT INTO `demon_realm_log` VALUES (5, 2, 1, '万骨荒原试炼', 'success', '闯万骨荒原试炼而还，斩妖伏魔，煞气尽入己身。', 123, 60, 0, '', '2026-11-03 03:08:46');
INSERT INTO `demon_realm_log` VALUES (6, 2, 2, '血海浮屠', 'success', '闯血海浮屠而还，斩妖伏魔，煞气尽入己身。', 214, 150, 0, '', '2026-11-03 03:08:59');
INSERT INTO `demon_realm_log` VALUES (7, 2, 8, '万魔窟', 'success', '闯万魔窟而还，斩妖伏魔，煞气尽入己身。', 214, 150, 0, '', '2026-11-03 03:13:23');
INSERT INTO `demon_realm_log` VALUES (8, 2, 9, '残星台', 'treasure', '深入残星台，机缘天降，得《玄阴养魂诀》残卷，煞气暴涨。', 120, 20, 0, '玄阴养魂诀', '2026-11-03 03:13:35');
INSERT INTO `demon_realm_log` VALUES (9, 2, 9, '残星台', 'injured', '遇残星守者阻道，血溅三丈——负伤遁出，所得折损 30 点。', 30, 20, 30, '', '2026-11-03 03:13:39');
INSERT INTO `demon_realm_log` VALUES (10, 2, 9, '残星台', 'success', '闯残星台而还，斩妖伏魔，煞气尽入己身。', 60, 20, 0, '', '2026-11-03 03:13:41');
INSERT INTO `demon_realm_log` VALUES (11, 2, 7, '冥河摆渡', 'success', '闯冥河摆渡而还，斩妖伏魔，煞气尽入己身。', 123, 60, 0, '', '2026-11-03 03:13:49');
INSERT INTO `demon_realm_log` VALUES (12, 2, 7, '冥河摆渡', 'success', '闯冥河摆渡而还，斩妖伏魔，煞气尽入己身。', 123, 60, 0, '', '2026-11-03 03:13:56');
INSERT INTO `demon_realm_log` VALUES (13, 2, 6, '煞风峡谷', 'success', '闯煞风峡谷而还，斩妖伏魔，煞气尽入己身。', 60, 20, 0, '', '2026-11-03 03:14:02');
INSERT INTO `demon_realm_log` VALUES (14, 2, 6, '煞风峡谷', 'success', '闯煞风峡谷而还，斩妖伏魔，煞气尽入己身。', 60, 20, 0, '', '2026-11-03 03:14:03');
INSERT INTO `demon_realm_log` VALUES (15, 2, 6, '煞风峡谷', 'success', '闯煞风峡谷而还，斩妖伏魔，煞气尽入己身。', 60, 20, 0, '', '2026-11-03 03:14:04');
INSERT INTO `demon_realm_log` VALUES (16, 2, 1, '万骨荒原试炼', 'injured', '遇白骨魔将阻道，血溅三丈——负伤遁出，所得折损 62 点。', 61, 60, 62, '', '2026-11-04 00:30:06');
INSERT INTO `demon_realm_log` VALUES (17, 2, 1, '万骨荒原试炼', 'success', '闯万骨荒原试炼而还，斩妖伏魔，煞气尽入己身。', 123, 60, 0, '', '2026-11-04 00:30:15');
INSERT INTO `demon_realm_log` VALUES (18, 2, 2, '血海浮屠', 'injured', '遇血海浮屠兽阻道，血溅三丈——负伤遁出，所得折损 107 点。', 107, 150, 107, '', '2026-11-04 00:30:18');
INSERT INTO `demon_realm_log` VALUES (19, 2, 3, '幻月迷宫', 'success', '闯幻月迷宫而还，斩妖伏魔，煞气尽入己身。', 66, 20, 0, '', '2026-11-04 00:57:00');
INSERT INTO `demon_realm_log` VALUES (20, 2, 3, '幻月迷宫', 'treasure', '深入幻月迷宫，机缘天降，得《噬魂幻月》残卷，煞气暴涨。', 132, 20, 0, '噬魂幻月', '2026-11-04 00:57:01');
INSERT INTO `demon_realm_log` VALUES (21, 25, 2, '血海浮屠', 'success', '闯血海浮屠而还，斩妖伏魔，煞气尽入己身。', 204, 150, 0, '', '2026-11-04 00:57:53');
INSERT INTO `demon_realm_log` VALUES (22, 25, 3, '幻月迷宫', 'treasure', '深入幻月迷宫，机缘天降，得《血煞戮天诀》残卷，煞气暴涨。', 114, 20, 0, '血煞戮天诀', '2026-11-04 00:57:55');
INSERT INTO `demon_realm_log` VALUES (23, 25, 5, '群魔论战', 'success', '闯群魔论战而还，斩妖伏魔，煞气尽入己身。', 57, 20, 0, '', '2026-11-04 00:57:56');
INSERT INTO `demon_realm_log` VALUES (24, 25, 6, '煞风峡谷', 'injured', '遇煞风鹰隼阻道，血溅三丈——负伤遁出，所得折损 57 点。', 57, 20, 57, '幽冥护魂咒', '2026-11-04 00:57:56');
INSERT INTO `demon_realm_log` VALUES (25, 25, 4, '魔渊深处', 'injured', '遇渊底古魔阻道，血溅三丈——负伤遁出，所得折损 102 点。', 102, 150, 102, '', '2026-11-04 00:57:57');
INSERT INTO `demon_realm_log` VALUES (26, 25, 1, '万骨荒原试炼', 'injured', '遇白骨魔将阻道，血溅三丈——负伤遁出，所得折损 59 点。', 58, 60, 59, '', '2026-11-04 00:57:58');

-- ----------------------------
-- Table structure for demon_trial
-- ----------------------------
DROP TABLE IF EXISTS `demon_trial`;
CREATE TABLE `demon_trial`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '历练名',
  `region` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '地域',
  `realm_req` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '所需境界',
  `difficulty` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '凶险：低/中/高/绝',
  `summary` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '简介',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `image` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '配图相对路径',
  `boss` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '瀹堝叧濡栭瓟',
  `cost_exp` int NOT NULL DEFAULT 0 COMMENT '鍏ュ満娑堣?鐓炴皵',
  `daily_limit` int NOT NULL DEFAULT 3 COMMENT '姣忔棩鍙?叆娆℃暟',
  `reward_min` int NOT NULL DEFAULT 20 COMMENT '浜у嚭鐓炴皵涓嬮檺',
  `reward_max` int NOT NULL DEFAULT 60 COMMENT '浜у嚭鐓炴皵涓婇檺',
  `risk_rate` int NOT NULL DEFAULT 20 COMMENT '鍑堕櫓姒傜巼(鐧惧垎姣?',
  `treasure_rate` int NOT NULL DEFAULT 15 COMMENT '鏈虹紭姒傜巼(鐧惧垎姣?',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '域外秘境' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of demon_trial
-- ----------------------------
INSERT INTO `demon_trial` VALUES (1, '万骨荒原试炼', '万骨荒原', '魔卒', '高', '于白骨遍野之地淬炼杀性，活过三息者方入门。', '2026-10-26 09:02:42', '/images/trial/trial-1.jpg', '白骨魔将', 60, 2, 80, 160, 30, 18);
INSERT INTO `demon_trial` VALUES (2, '血海浮屠', '血海', '血煞', '绝', '渡血海而不溺，方证凶煞之身。', '2026-10-26 09:02:42', '/images/trial/trial-2.jpg', '血海浮屠兽', 150, 1, 200, 400, 45, 25);
INSERT INTO `demon_trial` VALUES (3, '幻月迷宫', '幻月渊', '魔卒', '中', '破幻月迷宫，明心见性，幻破则境开。', '2026-10-26 09:02:42', '/images/trial/trial-3.jpg', '幻月魔狐', 20, 3, 30, 70, 20, 12);
INSERT INTO `demon_trial` VALUES (4, '魔渊深处', '无底魔渊', '魔尊', '绝', '下探魔渊，窥见本源，然归来者寥寥。', '2026-10-26 09:02:42', '/images/trial/trial-4.jpg', '渊底古魔', 150, 1, 200, 400, 45, 25);
INSERT INTO `demon_trial` VALUES (5, '群魔论战', '魔域演武场', '魔尉', '中', '与同辈论战，砥砺道行，败者亦有所悟。', '2026-10-26 09:02:42', '/images/trial/trial-5.jpg', '演武魁首', 20, 3, 30, 70, 20, 12);
INSERT INTO `demon_trial` VALUES (6, '煞风峡谷', '煞风峡', '魔卒', '中', '穿煞风而过，炼体亦炼心，怯者退。', '2026-10-26 09:02:42', '/images/trial/trial-6.jpg', '煞风鹰隼', 20, 3, 30, 70, 20, 12);
INSERT INTO `demon_trial` VALUES (7, '冥河摆渡', '冥河', '魔尉', '高', '渡冥河寻彼岸，迷者沉，悟者渡。', '2026-10-26 09:02:42', '/images/trial/trial-7.jpg', '冥河渡魂', 60, 2, 80, 160, 30, 18);
INSERT INTO `demon_trial` VALUES (8, '万魔窟', '万魔窟', '魔将', '绝', '入万魔窟夺造化，九死一生，得者通玄。', '2026-10-26 09:02:42', '/images/trial/trial-8.jpg', '万魔之主', 150, 1, 200, 400, 45, 25);
INSERT INTO `demon_trial` VALUES (9, '残星台', '残星台', '魔尉', '中', '登残星台观星象，窥天机一角，悟者获益。', '2026-10-26 09:02:42', '/images/trial/trial-9.jpg', '残星守者', 20, 3, 30, 70, 20, 12);

-- ----------------------------
-- Table structure for demon_user
-- ----------------------------
DROP TABLE IF EXISTS `demon_user`;
CREATE TABLE `demon_user`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '修者道号',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '灵识印记（邮箱）',
  `realm` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '魔种：血煞/幽冥/噬魂/虚妄',
  `cultivation` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '引煞' COMMENT '修为境界：引煞/凝煞/魔卒/魔尉/魔将/血煞/魔祖',
  `exp` int NULL DEFAULT 0 COMMENT '煞气修为',
  `exp_date` date NULL DEFAULT NULL COMMENT '最后参悟日期（跨日清零）',
  `exp_count_today` int NULL DEFAULT 0 COMMENT '当日已参悟次数',
  `spar_date` date NULL DEFAULT NULL COMMENT '最后切磋日期（跨日清零）',
  `spar_count_today` int NULL DEFAULT 0 COMMENT '当日已切磋次数',
  `quiz_date` date NULL DEFAULT NULL COMMENT '最后论道日期（跨日清零）',
  `quiz_count_today` int NULL DEFAULT 0 COMMENT '当日已答论题数',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '护法密令（演示为明文，生产请加密）',
  `permission` tinyint NULL DEFAULT 1 COMMENT '权限：1 默认权限 / 0 管理员权限',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入籍时间',
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `sign_date` date NULL DEFAULT NULL COMMENT '最后签到日期',
  `sign_streak` int NOT NULL DEFAULT 0 COMMENT '连续签到天数',
  `sign_total` int NOT NULL DEFAULT 0 COMMENT '累计签到天数',
  `spar_win` int NOT NULL DEFAULT 0 COMMENT '切磋胜场',
  `spar_lose` int NOT NULL DEFAULT 0 COMMENT '切磋负场',
  `break_fail` int NOT NULL DEFAULT 0 COMMENT '连续渡劫失败次数',
  `reincarnate` int NOT NULL DEFAULT 0 COMMENT '转生次数（第几世）',
  `read_date` date NULL DEFAULT NULL COMMENT '最后研读日期',
  `read_count_today` int NOT NULL DEFAULT 0 COMMENT '当日已研读篇数',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_name`(`name` ASC) USING BTREE,
  UNIQUE INDEX `uk_email`(`email` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 33 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '魔族修者表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of demon_user
-- ----------------------------
INSERT INTO `demon_user` VALUES (2, '魔祖', 'demo@demon.com', '血煞', '魔圣', 47726, '2026-11-04', 10, '2026-11-03', 1, '2026-11-03', 2, '123456', 0, '2026-10-26 09:02:43', '2026-11-04 01:42:13', '2026-11-04', 2, 2, 1, 0, 0, 1, '2026-11-04', 2);
INSERT INTO `demon_user` VALUES (3, '血河老祖', 'xuehe@demon.com', '血煞', '魔圣', 34990, NULL, 0, NULL, 0, NULL, 0, '123456', 1, '2026-10-26 09:13:11', '2026-11-03 02:30:30', NULL, 0, 0, 0, 1, 0, 0, NULL, 0);
INSERT INTO `demon_user` VALUES (4, '幽冥夫人', 'youming@demon.com', '幽冥', '魔圣', 35000, NULL, 0, NULL, 0, NULL, 0, '123456', 1, '2026-10-26 09:13:11', '2026-11-03 02:17:03', NULL, 0, 0, 0, 0, 0, 0, NULL, 0);
INSERT INTO `demon_user` VALUES (5, '噬魂大王', 'shihun@demon.com', '噬魂', '魔祖', 24000, NULL, 0, NULL, 0, NULL, 0, '123456', 1, '2026-10-26 09:13:11', '2026-11-03 02:08:25', NULL, 0, 0, 0, 0, 0, 0, NULL, 0);
INSERT INTO `demon_user` VALUES (6, '寂灭尊者', 'jimie@demon.com', '虚妄', '魔祖', 24000, NULL, 0, NULL, 0, NULL, 0, '123456', 1, '2026-10-26 09:13:11', '2026-11-03 02:08:25', NULL, 0, 0, 0, 0, 0, 0, NULL, 0);
INSERT INTO `demon_user` VALUES (7, '虚妄上人', 'xuwang@demon.com', '虚妄', '魔祖', 24000, NULL, 0, NULL, 0, NULL, 0, '123456', 1, '2026-10-26 09:13:11', '2026-11-03 02:08:25', NULL, 0, 0, 0, 0, 0, 0, NULL, 0);
INSERT INTO `demon_user` VALUES (8, 'gay', 'gay@qq.com', '噬魂', '魔卒', 1200, '2026-10-29', 1, NULL, 0, NULL, 0, 'gaygay', 1, '2026-10-29 14:45:03', '2026-11-03 02:08:25', NULL, 0, 0, 0, 0, 0, 0, NULL, 0);
INSERT INTO `demon_user` VALUES (13, '苏九幽', 'sujiuyou@demon.com', '幽冥', '引煞', 100, NULL, 0, NULL, 0, NULL, 0, '123456', 1, '2026-11-02 09:30:35', '2026-11-03 02:08:25', NULL, 0, 0, 0, 0, 0, 0, NULL, 0);
INSERT INTO `demon_user` VALUES (14, '沈无咎', 'shenwujiu@demon.com', '虚妄', '引煞', 100, NULL, 0, NULL, 0, NULL, 0, '123456', 1, '2026-11-02 09:30:35', '2026-11-03 02:08:25', NULL, 0, 0, 0, 0, 0, 0, NULL, 0);
INSERT INTO `demon_user` VALUES (15, '陆青罗', 'luqingluo@demon.com', '噬魂', '凝煞', 400, NULL, 0, NULL, 0, NULL, 0, '123456', 1, '2026-11-02 09:30:35', '2026-11-03 02:08:25', NULL, 0, 0, 0, 0, 0, 0, NULL, 0);
INSERT INTO `demon_user` VALUES (16, '白无相', 'baiwuxiang@demon.com', '血煞', '凝煞', 400, NULL, 0, NULL, 0, NULL, 0, '123456', 1, '2026-11-02 09:30:35', '2026-11-03 02:08:25', NULL, 0, 0, 0, 0, 0, 0, NULL, 0);
INSERT INTO `demon_user` VALUES (17, '韩断刃', 'handuanren@demon.com', '血煞', '魔卒', 1200, NULL, 0, NULL, 0, NULL, 0, '123456', 1, '2026-11-02 09:30:35', '2026-11-03 02:08:25', NULL, 0, 0, 0, 0, 0, 0, NULL, 0);
INSERT INTO `demon_user` VALUES (18, '楚红绡', 'chuhongxiao@demon.com', '幽冥', '魔尉', 3000, NULL, 0, NULL, 0, NULL, 0, '123456', 1, '2026-11-02 09:30:35', '2026-11-03 02:08:25', NULL, 0, 0, 0, 0, 0, 0, NULL, 0);
INSERT INTO `demon_user` VALUES (19, '燕孤鸿', 'yanguhong@demon.com', '虚妄', '魔尉', 3000, NULL, 0, NULL, 0, NULL, 0, '123456', 1, '2026-11-02 09:30:35', '2026-11-03 02:08:25', NULL, 0, 0, 0, 0, 0, 0, NULL, 0);
INSERT INTO `demon_user` VALUES (20, '萧寒山', 'xiaohanshan@demon.com', '血煞', '魔将', 6000, NULL, 0, NULL, 0, NULL, 0, '123456', 1, '2026-11-02 09:30:35', '2026-11-03 02:08:25', NULL, 0, 0, 0, 0, 0, 0, NULL, 0);
INSERT INTO `demon_user` VALUES (21, '姜夜叉', 'jiangyecha@demon.com', '噬魂', '魔将', 6000, NULL, 0, NULL, 0, NULL, 0, '123456', 1, '2026-11-02 09:30:35', '2026-11-03 02:08:25', NULL, 0, 0, 0, 0, 0, 0, NULL, 0);
INSERT INTO `demon_user` VALUES (22, '柳三变', 'liusanbian@demon.com', '噬魂', '血煞', 10000, NULL, 0, NULL, 0, NULL, 0, '123456', 1, '2026-11-02 09:30:35', '2026-11-03 02:08:25', NULL, 0, 0, 0, 0, 0, 0, NULL, 0);
INSERT INTO `demon_user` VALUES (23, '殷离火', 'yinlihuo@demon.com', '幽冥', '血煞', 10000, NULL, 0, NULL, 0, NULL, 0, '123456', 1, '2026-11-02 09:30:35', '2026-11-03 02:08:25', NULL, 0, 0, 0, 0, 0, 0, NULL, 0);
INSERT INTO `demon_user` VALUES (24, '封夜白', 'fengyebai@demon.com', '虚妄', '魔尊', 16000, NULL, 0, NULL, 0, NULL, 0, '123456', 1, '2026-11-02 09:30:35', '2026-11-03 02:08:25', NULL, 0, 0, 0, 0, 0, 0, NULL, 0);
INSERT INTO `demon_user` VALUES (25, '顾长生', 'guchangsheng@demon.com', '血煞', '魔尊', 16240, NULL, 0, '2026-11-02', 1, NULL, 0, '123456', 1, '2026-11-02 09:30:35', '2026-11-04 01:40:05', '2026-11-04', 1, 1, 0, 0, 0, 0, '2026-11-04', 1);

-- ----------------------------
-- Table structure for demon_user_art
-- ----------------------------
DROP TABLE IF EXISTS `demon_user_art`;
CREATE TABLE `demon_user_art`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `art_id` bigint NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_art`(`user_id` ASC, `art_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '修者已习得功法' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of demon_user_art
-- ----------------------------
INSERT INTO `demon_user_art` VALUES (2, 2, 1, '2026-11-03 02:31:40');
INSERT INTO `demon_user_art` VALUES (3, 25, 1, '2026-11-04 01:40:05');

SET FOREIGN_KEY_CHECKS = 1;
