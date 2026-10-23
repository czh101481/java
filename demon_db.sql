/*
 Navicat Premium Dump SQL

 Source Server         : test
 Source Server Type    : MySQL
 Source Server Version : 80039 (8.0.39)
 Source Host           : localhost:3306
 Source Schema         : demon_db

 Target Server Type    : MySQL
 Target Server Version : 80039 (8.0.39)
 File Encoding         : 65001

 Date: 23/10/2026 15:10:19
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
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '魔门公告' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of demon_announcement
-- ----------------------------
INSERT INTO `demon_announcement` VALUES (1, '魔门开宗大典', '三月初三，魔主将于血煞峰开宗大典，诸修皆可赴会论道。', '喜讯', '2026-10-23 14:15:31');
INSERT INTO `demon_announcement` VALUES (2, '煞气异动警示', '北境万骨荒原煞气骤盛，魔卒以下慎入，免遭反噬。', '警示', '2026-10-23 14:15:31');
INSERT INTO `demon_announcement` VALUES (3, '新典入库', '《星陨录》《冥河志异》等上古残卷已入魔典秘录，诸修可往查阅。', '通告', '2026-10-23 14:15:31');
INSERT INTO `demon_announcement` VALUES (4, '论道延期', '原定本月的「群魔共治」论道因魔渊异动延期，另择吉日。', '通告', '2026-10-23 14:15:31');
INSERT INTO `demon_announcement` VALUES (5, '秘辛·禁忌之扉', '卷末禁忌之扉似有松动，擅窥者多遭反噬，切记慎之。', '秘辛', '2026-10-23 14:15:31');

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
) ENGINE = InnoDB AUTO_INCREMENT = 21 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '魔枢典册' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of demon_archive
-- ----------------------------
INSERT INTO `demon_archive` VALUES (1, 'lineage', '血煞一族', '血脉 · 血为媒，煞为刃', '魔域最盛的一脉，以血为媒，煞气自生。', '血煞一族起于血海之滨，血脉中天生带煞。其修行不假外物，只以自身精血养煞，故族人愈战愈强，越伤越狂。\n\n天赋有二：一曰「血刃」，引血化兵，凝而不散；二曰「噬血回元」，伤敌则补己，故与之久战者，往往先自溃。\n\n族中定规：煞气过盛者需以心魔为锁，否则反噬自身，轻则失智，重则化为一团血雾。血煞峰为其祖地，非本族血脉不得登顶。', 1, '2026-10-23 14:15:31');
INSERT INTO `demon_archive` VALUES (2, 'lineage', '幽冥一族', '血脉 · 魂体近冥，善守不攻', '魂体近冥，天生长于护魂锁魄。', '幽冥一族生于冥河两岸，魂体偏阴，触之生寒。族人不擅攻伐，却精于「守」——以幽冥之气覆体，万法不侵；以咒锁魄，可定敌神魂。\n\n天赋有二：一曰「幽冥屏障」，护己亦护人；二曰「往生引」，能引将散之魂归于冥河，故各族战死者多请幽冥族人收魂。\n\n久施幽冥之术则神识渐冷，族人需常返阳间温养，故幽冥一族多居阴阳交界之地，不轻易远行。', 2, '2026-10-23 14:15:31');
INSERT INTO `demon_archive` VALUES (3, 'lineage', '噬魂一族', '血脉 · 生而吞魂，忌食同源', '生而能吞魂夺忆，然吞同族之魂者必遭反噬。', '噬魂一族形貌最似常人，唯瞳中无光。其天赋直指神魂：一曰「锁魄」，以咒定敌三魂七魄，使其不能自控；二曰「夺忆」，吞魂时并取其记忆，故族中老怪多知上古秘辛。\n\n族规森严：可吞异族之魂，不可吞同族之魂。犯者体内诸魂相冲，神魂撕裂而亡，此为魔域少有的「天罚自应」之例。\n\n噬魂一族与幽冥一族累世不睦——一个收魂，一个吞魂，所争者本是同一件东西。', 3, '2026-10-23 14:15:31');
INSERT INTO `demon_archive` VALUES (4, 'lineage', '幻月一族', '血脉 · 以月为引，织幻惑识', '以月华为引织就幻境，惑敌五识。', '幻月一族居于幻月渊，族人体内自存一缕月华。其术不在杀，而在「惑」：一曰「幻月结界」，境中时序、方位皆由施者定；二曰「心镜」，照见敌之执念，以执念为饵。\n\n此族最难缠处在于「不与你打」——你追，它在三步之外；你退，它在你影中。然其克星亦明：守心不动、不生执念者，幻境自破。\n\n传说幻月渊本无月，第一代幻月族人以自身月华点亮渊顶，至此渊中长夜有光。', 4, '2026-10-23 14:15:31');
INSERT INTO `demon_archive` VALUES (5, 'lineage', '骨魔与兽魔', '血脉 · 荒原之骨，血契之兽', '万骨荒原的骨修者，与以血契御兽的兽魔一脉。', '骨魔出于万骨荒原，以骸骨为体、以骨为器。族人少有血肉之躯，多见白骨之相，其修行靠「纳骨」——纳万千残骸于一身，骨愈多则力愈沉。骨魔不言情感，只认强弱。\n\n兽魔一脉则相反，肉身强悍而好群战。其核心之术「御兽血契」：滴血为契，与魔渊异兽同心，兽死则契损。故真正的兽魔从不轻弃其兽，视之如半身。\n\n两族常于荒原边缘争地，骨魔取尸骨，兽魔取其兽，互为世仇亦互为食粮。', 5, '2026-10-23 14:15:31');
INSERT INTO `demon_archive` VALUES (6, 'lineage', '星陨遗族', '血脉 · 陨星之力，几近绝嗣', '来自残星台的外来血脉，星力驳杂，几近绝嗣。', '星陨遗族并非魔域原血，其祖自天外坠于残星台，故体内所流为「星力」而非煞气。星力至刚至燥，与魔域煞气天然相斥，故族人修行倍难，寿元亦短。\n\n天赋「陨星一击」：倾尽全身星力于一瞬，其威可裂山，其代价是施者三日内再无战力。族中有一古训——此术一生只可尽放九次，第九次后星力枯竭而亡。\n\n如今遗族所存不足百人，残星台的石碑上，名字一年比一年少。', 6, '2026-10-23 14:15:31');
INSERT INTO `demon_archive` VALUES (7, 'path', '七大境界总纲', '境界 · 由煞入道，七阶登顶', '引煞、凝煞、魔卒、魔尉、魔将、血煞、魔祖。', '魔修之路共七阶：\n\n一曰「引煞」——引魔域煞气入体，凡人至此始称修者。\n二曰「凝煞」——煞气凝于血窍，化虚为实。\n三曰「魔卒」——煞能外放，可结术法，为魔门常备之卒。\n四曰「魔尉」——煞气成域，一域之内法术自显威能。\n五曰「魔将」——域可覆百丈，一念可令群修俯首。\n六曰「血煞」——煞与血合，肉身即法，不死不灭者众。\n七曰「魔祖」——传说之境，魔域今唯有一人。\n\n阶与阶之间没有捷径。所谓天骄，不过是引煞时少死几个人罢了。', 1, '2026-10-23 14:15:31');
INSERT INTO `demon_archive` VALUES (8, 'path', '引煞入体：第一道生死关', '境界 · 引煞期', '煞气灌体，九死一生，心魔自此而生。', '引煞是魔修的第一道关，也是死人最多的一道关。\n\n法极简：于煞气浓处静坐，开血窍，纳煞入体。痛如万刃穿身，此为常态。真正的凶险在于——煞气入体时，会顺带把「心魔」一并带进来。\n\n心魔无相，因人而异：好杀者见血海，贪生者见长生，悔恨者见故人。守得住，煞气归窍，从此入道；守不住，轻则疯癫成魔，重则当场爆体，尸骨无存。\n\n魔门有旧例：引煞者需有同门护法，若三个时辰内未醒，护法者当亲手了结。此非残忍，是不让其化魔屠城。', 2, '2026-10-23 14:15:31');
INSERT INTO `demon_archive` VALUES (9, 'path', '魔力运转规则', '心法 · 血窍为渠，心魔为锁', '血窍为渠、煞气为流、心魔为锁，逆行则爆体。', '魔力并非凭空而来，其运转有三要：\n\n一为「渠」——血窍。周身三十六处血窍即煞气通道，窍开则力增，窍损则力泄。故魔修极重护窍之术，护体一道由此而生。\n二为「流」——煞气。煞气运行自有其序，顺则周天圆满，逆则壅塞爆体。所谓走火入魔，十之八九是逆流。\n三为「锁」——心魔。此为魔修独有之物：以心魔为锁，锁住煞气不至暴走；心魔愈强，锁愈牢，但人也愈近疯癫。\n\n故此道有一句冷话：魔修不怕弱，只怕心魔不够强——而不够强的，早已死在引煞那一年。', 3, '2026-10-23 14:15:31');
INSERT INTO `demon_archive` VALUES (10, 'path', '修行法门三分', '法门 · 三道为主，两支为旁', '攻伐、护体、炼魂为正三道；幻术、御兽为旁支。', '魔域诸法，尽归五门，其中三道为正：\n\n「攻伐」——以煞化刃，以血为薪，杀伤最烈。功法阁中《血煞戮天诀》《焚天业火》皆属此道。\n「护体」——覆身如壁，保窍护魂。攻伐愈烈者，愈需护体，故大修者往往兼修两道。\n「炼魂」——直取神魂，锁魄夺忆。此道最凶亦最忌，业障缠身者多出于此。\n\n另有旁支二：\n「幻术」——惑敌五识，不与力敌，幻月一脉为宗。\n「御兽」——血契同心，与兽共战，兽魔一脉为宗。\n\n旁支不弱，只是路窄。能走到血煞境的，从来都是三道兼修之人。', 4, '2026-10-23 14:15:31');
INSERT INTO `demon_archive` VALUES (11, 'path', '破境与代价', '心法 · 每破一境，必有所失', '破境需献祭，魔纹刻体，一步一价。', '魔修破境，不似仙道有天劫可渡，而是「以内换外」——以自身之物，换境界之升。\n\n常见之价有三：\n一曰「精血」——以血换力，最轻，也最常。破境后需静养数月补血。\n二曰「寿元」——以命换境，多见于寿数将尽的老修者，破则多活百年，败则当场枯死。\n三曰「记忆」——以忆换道，最狠。有人破了境，却忘了自己为何要破境。\n\n每破一境，身上必生一道「魔纹」，位有定处，纹有定形。魔纹既是境界的凭证，也是枷锁的刻痕——魔纹满身之日，便是再无可破之时。', 5, '2026-10-23 14:15:31');
INSERT INTO `demon_archive` VALUES (12, 'law', '三禁忌', '铁律 · 犯者不论境界，一律诛', '魔渊九层、魔主真名、同族血祭，触之即死。', '魔域无法无天，却有三条铁律，谓之禁忌。\n\n其一：不得擅入无底魔渊第九层。渊分九层，前八层可历炼，第九层封印之下另有其物。历代擅自下探者，无一人还。\n\n其二：不得直呼魔主真名。名即咒，呼之则咒应。魔主真名唯其本人与历代枢机知晓，泄露者，泄者与被泄者同诛。\n\n其三：不得以同族之血为祭。血祭可速成，然以同族为薪者，其族必衰。此法由上古大战后立，屡有犯者，屡有灭族之例。\n\n三禁不载于任何功法，却刻在每一座魔域界碑之上。新入道者第一课，便是背这三条。', 1, '2026-10-23 14:15:31');
INSERT INTO `demon_archive` VALUES (13, 'law', '天道枷锁', '铁律 · 魔修无劫可渡', '天道不容魔修，飞升之门永闭。', '仙道修行，终有飞升之日；魔道修行，无门可出。\n\n天道视煞气为「逆」，故魔修不为天道所容：一不能渡劫，二不能引天雷淬体，三不能借天时破境。他人借天，魔修只借己身——这也是魔修寿元虽长、终有尽时的根由。\n\n所谓「天道枷锁」，非一道实体的锁，而是一种持续的存在性排斥：魔修越是接近极境，天象越是异动，雷云越是低垂。魔域上空终年不散的血云，便是这排斥的显形。\n\n故魔域有一句流传最广的狂言：既然上不去，那就把天拉下来。此言出自魔天之战前的旧枢机，后成魔族战号。', 2, '2026-10-23 14:15:31');
INSERT INTO `demon_archive` VALUES (14, 'law', '血契之律', '铁律 · 契成不可解，唯死可断', '以血为契者，违契则血逆行，契断唯死。', '血契是魔域最普及、也最不容违逆的约定。上至宗门盟誓，下至御兽同心，皆以血契为凭。\n\n其律有三：\n一、契成不可解。立契时双方各出精血一滴，融于契石。此后契石不碎，契便不消。\n二、违契则血逆行。违者体内煞气逆流，血窍自裂，痛楚随违逆程度递增，直至爆体。这不是惩罚，是规则本身在杀人。\n三、唯死可断。立契者一方身死，血契自解。故魔域有「杀人解契」之术，为下三滥所不为，却从未禁绝。\n\n立契前想清楚——魔域的人常说这句话。因为立完之后，就不必再想了。', 3, '2026-10-23 14:15:31');
INSERT INTO `demon_archive` VALUES (15, 'law', '论道台铁律', '铁律 · 台上论道不取命', '台上论道不取命，台下私斗不论。', '魔域嗜杀，却有唯一一处杀不得的地方——宗门论道台。\n\n台上之律仅一条：论道不取命。可驳、可辱、可败人修为，不可断人性命。犯者，无论出自何族何宗，诸宗共讨。\n\n此律的来由并不高尚：上古魔族内斗不休，强者互杀至血流成河，几近自灭。旧枢机遂立此台，把「杀」改成「论」——让强者有一个不必杀人的胜负场。\n\n台下则无此约束。所谓「台上论道，台下私斗」，两不相干。故论道台四周永远比台上热闹。', 4, '2026-10-23 14:15:31');
INSERT INTO `demon_archive` VALUES (16, 'law', '疆界与残星台', '地志 · 九域一界', '魔域九域，残星台为界碑，域外即虚无。', '魔域共分九域：血煞峰居中，万骨荒原在北，血海在东，幻月渊在南，无底魔渊在西，冥河横贯，煞风峡为咽喉，万魔窟为腹地，残星台为西界之碑。\n\n九域之间无城墙，只有界碑。碑以骨石所立，上刻三禁忌与域主之名。跨碑不入他人之域，是魔域少有的、各族共守的礼。\n\n残星台最特殊：它既非某族之域，也不归枢机直辖。台在魔域边缘，台外便是虚无——无煞气、无光、无声。星陨遗族便守在此处，既守界，也守着自身血脉的最后一点星火。', 5, '2026-10-23 14:15:31');
INSERT INTO `demon_archive` VALUES (17, 'ancient', '魔天之战', '上古 · 魔族与天道的第一次正面交锋', '魔族倾巢而上，欲碎天道枷锁，终成两败。', '魔天之战，是魔域史上唯一一次全族同向的战争。\n\n起因简单到近乎狂妄：旧枢机立于血煞峰顶，问了一句——凭什么仙道可上，魔道不可？于是九域同应，倾巢而上。\n\n战况无籍可考，只留下三个事实：\n其一，血云至今未散。战时撕裂的天幕没有愈合，成为魔域终年不散的奇景。\n其二，九域至此残缺。原有十三域，战后仅余其九，四域沉入虚无。\n其三，旧枢机再未归来。其最后一道命令传遍九域：散。\n\n史载此战「两败」。天道未碎，魔族未灭。但真正知道内情的老修者说：不是两败，是天道根本没把这一战当回事。', 1, '2026-10-23 14:15:31');
INSERT INTO `demon_archive` VALUES (18, 'ancient', '旧枢机覆灭', '上古 · 上一代魔域之主如何崩解', '旧枢机陨落于魔天之战，枢机之位空悬至今。', '枢机者，魔域之主，统九域、掌三禁、持魔主真名。上一代枢机，是魔域最后一个真正的共主。\n\n其覆灭并非战死——魔天之战后他回到血煞峰，独自入关，再未开门。三日后，峰顶血云倒卷，枢机印碎为九片，散落九域。\n\n自此枢机之位空悬。九域各执一片印，谁也不肯交，谁也凑不齐。所谓「魔域无主」，并非无人想当，而是规则要求九印合一，而九位域主互相牵制，无人可成。\n\n有趣的是：魔域反而因此安稳了数千年。老修者说，枢机在时，魔域是一把刀；枢机没了，魔域成了九把刀——九把刀互相看着，反而谁都不敢先动。', 2, '2026-10-23 14:15:31');
INSERT INTO `demon_archive` VALUES (19, 'ancient', '九幽裂封', '上古 · 无底魔渊封印的由来', '魔渊第九层之下所封何物，无人知晓，亦无人敢问。', '无底魔渊分九层，前八层为历炼之地，第九层为禁区。而禁令之下的真正秘密是：第九层并非底。\n\n渊底有一道裂口，裂口之下另有其物。上古大战末期，四域沉入虚无，虚无中的东西顺着裂口往上爬。旧枢机以自身三百年道行、九域三万修者之血，封了那道裂口。\n\n封印名为「九幽裂封」，其形为九道锁链，各系一域。九链同紧则封固，一链断则封松。这也是为何九域界碑不容轻犯——界碑既是疆界，也是锁链的锚点。\n\n典册中关于裂口的记载到此为止。再往下的字句被人为抹去，抹得极干净，连刀痕都没有。', 3, '2026-10-23 14:15:31');
INSERT INTO `demon_archive` VALUES (20, 'ancient', '魔主失踪', '近古 · 血煞峰闭关三千年', '魔祖闭关血煞峰，三千年未出，生死不知。', '魔祖，七阶之极，魔域今世唯一踏入此境者。三千年前，他登上血煞峰，留下一句话便闭了关：待我出来，把天拉下来。\n\n三千年间，峰顶血云不散、不落、不散形，每百年轰鸣一次如心跳。九域域主每百年上山一次，至峰腰而止，无人敢再上一步。\n\n有人说他早已坐化，只是道韵未散；有人说他仍在破境，只差最后一关；也有人说——他已经出来了，只是我们认不出。\n\n典册对此不作判断。只记一事：魔主闭关那年，血煞峰上的魔纹多了最后一道，刻的是「锁」字。而魔纹满身者，再无可破。', 4, '2026-10-23 14:15:31');

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
) ENGINE = InnoDB AUTO_INCREMENT = 13 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '功法阁' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of demon_art
-- ----------------------------
INSERT INTO `demon_art` VALUES (1, '血煞戮天诀', '攻伐', '天', '魔将', '以血煞之气凝刃，一念可斩千军。', '引周身精血化煞刃，血气越盛则刃越利；然反噬亦烈，需以心魔为锁。', '2026-10-23 14:15:31', '/images/art/art-1.jpg');
INSERT INTO `demon_art` VALUES (2, '幽冥护魂咒', '护体', '地', '魔尉', '布幽冥屏障，万法不侵。', '以幽冥之气覆体，外邪难入；久施则神识渐冷，需常返阳间温养。', '2026-10-23 14:15:31', '/images/art/art-2.jpg');
INSERT INTO `demon_art` VALUES (3, '噬魂幻月', '幻术', '玄', '魔卒', '织幻境如月华，惑敌五识。', '以月华为引织就幻网，敌入其间则真伪难辨；破法唯在守心。', '2026-10-23 14:15:31', '/images/art/art-3.jpg');
INSERT INTO `demon_art` VALUES (4, '炼魂锁魄', '炼魂', '地', '魔尉', '锁敌魂魄，炼化为己用。', '以咒锁敌三魂七魄，可炼为傀或吞而补己；慎用，恐业障缠身。', '2026-10-23 14:15:31', '/images/art/art-4.jpg');
INSERT INTO `demon_art` VALUES (5, '焚天业火', '攻伐', '天', '魔将', '引业火焚身，敌我皆烬。', '召业火焚尽四方，杀伤无匹；施者亦受灼伤，乃双刃之术。', '2026-10-23 14:15:31', '/images/art/art-5.jpg');
INSERT INTO `demon_art` VALUES (6, '御兽血契', '御兽', '玄', '魔卒', '与异兽立血契，共赴杀伐。', '滴血为契，与魔渊异兽同心；兽死则契损，须善待之', '2026-10-23 14:15:31', '/images/art/art-6.jpg');
INSERT INTO `demon_art` VALUES (7, '九幽寒煞功', '攻伐', '地', '魔尉', '引九幽寒气凝煞，冻敌于瞬息。', '以寒煞封脉，敌血凝而僵；然寒煞伤己阳元，须以血煞制衡。', '2026-10-23 14:15:31', '/images/art/art-7.jpg');
INSERT INTO `demon_art` VALUES (8, '万鬼夜行术', '幻术', '天', '魔将', '召万鬼夜行，遮天蔽月。', '念动则群鬼出，惑敌心智、乱其神识；施者神魂消耗甚巨。', '2026-10-23 14:15:31', '/images/art/art-8.jpg');
INSERT INTO `demon_art` VALUES (9, '噬骨魔音', '攻伐', '玄', '魔卒', '以魔音蚀骨，远攻之术。', '魔音入耳则骨酥魂颤，可破护体罡气；闻者需封耳守心。', '2026-10-23 14:15:31', '/images/art/art-9.jpg');
INSERT INTO `demon_art` VALUES (10, '玄阴养魂诀', '炼魂', '地', '魔尉', '温养神魂，稳固道基。', '以玄阴之气滋养三魂，久修则神识清明、抗幻力增。', '2026-10-23 14:15:31', '/images/art/art-10.jpg');
INSERT INTO `demon_art` VALUES (11, '血河大阵', '护体', '天', '魔将', '布血河为阵，万法不侵。', '以己血绘阵，成血河屏障，外力难破；阵破则反噬攻心。', '2026-10-23 14:15:31', '/images/art/art-11.jpg');
INSERT INTO `demon_art` VALUES (12, '傀儡替死术', '御兽', '地', '魔尉', '炼傀儡代受死劫。', '以本命精血饲傀，临劫可替主受创；傀毁则精血大损。', '2026-10-23 14:15:31', '/images/art/art-12.jpg');

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
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '魔典秘录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of demon_codex
-- ----------------------------
INSERT INTO `demon_codex` VALUES (1, '太古魔典', '卷一·开辟', '记述魔域开天辟地之秘，言魔气自混沌中来。', '2026-10-23 14:15:31', '/images/codex/codex-1.jpg');
INSERT INTO `demon_codex` VALUES (2, '噬魂真解', '卷三·炼魂', '详述炼魂锁魄之法门与禁忌，阅前须立护心咒。', '2026-10-23 14:15:31', '/images/codex/codex-2.jpg');
INSERT INTO `demon_codex` VALUES (3, '血煞图谱', '卷七·攻伐', '血煞一脉功法源流与图谱，脉络清晰。', '2026-10-23 14:15:31', '/images/codex/codex-3.jpg');
INSERT INTO `demon_codex` VALUES (4, '幽冥往生录', '卷九·往生', '幽冥轮回之说，真假难辨，录以备参。', '2026-10-23 14:15:31', '/images/codex/codex-4.jpg');
INSERT INTO `demon_codex` VALUES (5, '禁忌之扉', '卷末·禁忌', '封印于魔域最深处之禁忌知识，擅窥者或遭反噬。', '2026-10-23 14:15:31', '/images/codex/codex-5.jpg');
INSERT INTO `demon_codex` VALUES (6, '万兽图录', '卷四·御兽', '记魔渊异兽形貌习性，御兽者必参。', '2026-10-23 14:15:31', '/images/codex/codex-6.jpg');
INSERT INTO `demon_codex` VALUES (7, '寒煞秘要', '卷六·攻伐', '寒煞一脉修炼次第与破法，循序渐进。', '2026-10-23 14:15:31', '/images/codex/codex-7.jpg');
INSERT INTO `demon_codex` VALUES (8, '冥河志异', '卷十·异闻', '冥河两岸奇闻怪谈，虚实参半，录以警世。', '2026-10-23 14:15:31', '/images/codex/codex-8.jpg');
INSERT INTO `demon_codex` VALUES (9, '星陨录', '卷十一·天象', '残星坠落之兆与魔劫关联考，关乎魔域气运。', '2026-10-23 14:15:31', '/images/codex/codex-9.jpg');

-- ----------------------------
-- Table structure for demon_debate
-- ----------------------------
DROP TABLE IF EXISTS `demon_debate`;
CREATE TABLE `demon_debate`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '论题',
  `speaker` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '论者',
  `summary` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '摘要',
  `option_a` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '选项A',
  `option_b` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '选项B',
  `option_c` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '选项C',
  `answer` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '正确选项 A/B/C',
  `analysis` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '论道解析',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '宗门论道' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of demon_debate
-- ----------------------------
INSERT INTO `demon_debate` VALUES (1, '魔非魔，道非道', '魔祖', '顺逆由心，何须外求；魔与道，不过一念之转。', '在正邪两条路，各走各的', '在一念之间，心念所向即是分界', '在血脉出身，魔种生而为魔', 'B', '魔祖有言：顺逆由心，何须外求。魔与道不过一念之转，分界不在路，不在血，只在心念一转。', '2026-10-23 14:15:31');
INSERT INTO `demon_debate` VALUES (2, '血契之辩', '血煞尊者', '以血为契，究竟是束缚，还是自由？', '是束缚，血契锁魂不可脱', '是自由，愿力所铸方为真契', '皆是虚无，契与不契并无分别', 'B', '血煞尊者曰：血契出于本愿，以血立誓正是意志之自由；被迫之契才是束缚，愿铸之契即自由。', '2026-10-23 14:15:31');
INSERT INTO `demon_debate` VALUES (3, '杀伐与慈悲', '幽冥夫人', '魔道修行，可存一线慈悲否？', '不可，慈悲乱我道心', '可，护生之念亦是执念之力', '只对本族慈悲，外敌皆可杀', 'B', '幽冥夫人护魂万年：慈悲非软弱，化护生之念为执念之力，恰是磨道之砺石。', '2026-10-23 14:15:31');
INSERT INTO `demon_debate` VALUES (4, '长生孰真', '虚妄上人', '长生不过执念，何为真我？', '肉身不朽，即为真我', '念头不灭，传承即真我', '破除长生执念，忘我方见真我', 'C', '虚妄上人叹：执念于长生，则真我已失；忘却小我、放下执念之际，真我自现。', '2026-10-23 14:15:31');
INSERT INTO `demon_debate` VALUES (5, '群魔共治', '噬魂大王', '魔域当立规制，还是任凭弱肉强食？', '当立规制，群魔共治共享魔域', '弱肉强食，方是魔之本性', '无需规制，亦无需强权', 'A', '噬魂大王倡言：魔域万里，若只知吞噬，终至无人可吞；立规制共治，群魔方能长久。', '2026-10-23 14:15:31');
INSERT INTO `demon_debate` VALUES (6, '寂灭与轮回', '寂灭尊者', '魔修可证寂灭，抑或只入轮回？', '寂灭为真，万象归无方生万有', '轮回为真，生生不息不灭', '皆是幻相，唯当下为真', 'A', '寂灭尊者曰：寂灭非断灭，乃万有之母；万象归于无，无中生有，此为轮回之真门。', '2026-10-23 14:15:31');
INSERT INTO `demon_debate` VALUES (7, '力量之价', '血河老祖', '无敌之力，是否必以本心为祭？', '肉身之痛，熬过去便是', '人性之失，煞气蚀心道愈高情愈冷', '力量从无代价，只看敢取与否', 'B', '血河老祖以血河为鉴：力量愈深，人性愈失；杀伐果决易，留一分本心难——此即力量之价。', '2026-10-23 14:15:31');
INSERT INTO `demon_debate` VALUES (8, '魔域之疆', '幽冥夫人', '魔域当有边界，还是无垠扩张？', '在三千魔渊之外，天堑为界', '在修者之心，心有多广域有多广', '在血月升起之处，月照即疆', 'B', '幽冥夫人语：魔域不在外而在内，心念所至即为疆；心若狭隘，虽居魔渊亦如困笼。', '2026-10-23 14:15:31');

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
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '历练录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of demon_trial
-- ----------------------------
INSERT INTO `demon_trial` VALUES (1, '万骨荒原试炼', '万骨荒原', '魔卒', '高', '于白骨遍野之地淬炼杀性，活过三息者方入门。', '2026-10-23 14:15:31', '/images/trial/trial-1.jpg');
INSERT INTO `demon_trial` VALUES (2, '血海浮屠', '血海', '魔尉', '绝', '渡血海而不溺，方证凶煞之身。', '2026-10-23 14:15:31', '/images/trial/trial-2.jpg');
INSERT INTO `demon_trial` VALUES (3, '幻月迷宫', '幻月渊', '魔卒', '中', '破幻月迷宫，明心见性，幻破则境开。', '2026-10-23 14:15:31', '/images/trial/trial-3.jpg');
INSERT INTO `demon_trial` VALUES (4, '魔渊深处', '无底魔渊', '魔将', '绝', '下探魔渊，窥见本源，然归来者寥寥。', '2026-10-23 14:15:31', '/images/trial/trial-4.jpg');
INSERT INTO `demon_trial` VALUES (5, '群魔论战', '魔域演武场', '魔尉', '中', '与同辈论战，砥砺道行，败者亦有所悟。', '2026-10-23 14:15:31', '/images/trial/trial-5.jpg');
INSERT INTO `demon_trial` VALUES (6, '煞风峡谷', '煞风峡', '魔卒', '中', '穿煞风而过，炼体亦炼心，怯者退。', '2026-10-23 14:15:31', '/images/trial/trial-6.jpg');
INSERT INTO `demon_trial` VALUES (7, '冥河摆渡', '冥河', '魔尉', '高', '渡冥河寻彼岸，迷者沉，悟者渡。', '2026-10-23 14:15:31', '/images/trial/trial-7.jpg');
INSERT INTO `demon_trial` VALUES (8, '万魔窟', '万魔窟', '魔将', '绝', '入万魔窟夺造化，九死一生，得者通玄。', '2026-10-23 14:15:31', '/images/trial/trial-8.jpg');
INSERT INTO `demon_trial` VALUES (9, '残星台', '残星台', '魔尉', '中', '登残星台观星象，窥天机一角，悟者获益。', '2026-10-23 14:15:31', '/images/trial/trial-9.jpg');

-- ----------------------------
-- Table structure for demon_user
-- ----------------------------
DROP TABLE IF EXISTS `demon_user`;
CREATE TABLE `demon_user`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '修者道号',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '灵识印记（邮箱）',
  `realm` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '魔种：血煞/幽冥/噬魂/虚妄',
  `cultivation` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '引煞' COMMENT '修为境界：引煞/凝煞/魔卒/魔尉/魔将/血煞/魔祖',
  `exp` int NOT NULL DEFAULT 0 COMMENT '煞气修为，浏览魔域内容积累',
  `exp_date` date NULL DEFAULT NULL COMMENT '最后参悟日期',
  `exp_count_today` int NOT NULL DEFAULT 0 COMMENT '当日已参悟次数',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '护法密令（演示为明文，生产请加密）',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入籍时间',
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_name`(`name` ASC) USING BTREE,
  UNIQUE INDEX `uk_email`(`email` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '魔族修者表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of demon_user
-- ----------------------------
INSERT INTO `demon_user` VALUES (2, '魔祖', 'demo@demon.com', '血煞', '魔祖', 90, '2026-10-23', 9, '123456', '2026-10-23 14:15:31', '2026-10-23 15:03:38');
INSERT INTO `demon_user` VALUES (3, '龙', 'long@qq.com', '虚妄', '凝煞', 90, '2026-10-23', 10, '123456', '2026-10-23 14:15:49', '2026-10-23 15:03:09');
INSERT INTO `demon_user` VALUES (4, '星落大帝', 'czh@qq.com', '幽冥', '引煞', 0, NULL, 0, '123456', '2026-10-23 15:05:00', '2026-10-23 15:05:00');

SET FOREIGN_KEY_CHECKS = 1;
