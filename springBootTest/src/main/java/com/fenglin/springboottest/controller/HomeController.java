package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.Realm;
import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.common.TokenManager;
import com.fenglin.springboottest.entity.DemonUser;
import com.fenglin.springboottest.entity.ExploreLog;
import com.fenglin.springboottest.mapper.DemonUserMapper;
import com.fenglin.springboottest.service.ContentService;
import com.fenglin.springboottest.vo.AnnouncementVO;
import com.fenglin.springboottest.vo.DetailVO;
import com.fenglin.springboottest.vo.OverviewVO;
import com.fenglin.springboottest.vo.RealmFeatureVO;
import com.fenglin.springboottest.vo.RealmLadderVO;
import com.fenglin.springboottest.vo.RealmNodeVO;
import com.fenglin.springboottest.vo.RealmStatusVO;
import com.fenglin.springboottest.vo.SearchItemVO;
import com.fenglin.springboottest.vo.SearchResultVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 魔族修行 · 首页内容接口
 * 基底址：/api/home
 * 境界体系：引煞 → 凝煞 → 魔卒 → 魔尉 → 魔将 → 血煞 → 魔尊 → 魔祖 → 魔圣 → 无上
 */
@RestController
@RequestMapping("/api/home")
public class HomeController {

    /** 模块 type → 展示名（境界门槛提示用） */
    private static final Map<String, String> TYPE_NAMES = Map.of(
            "art", "功法阁",
            "trial", "域外秘境",
            "debate", "宗门论道",
            "codex", "魔典秘录",
            "archive", "魔枢典册");

    /** 机缘奇遇事件表：{ 名, 述, 煞气变动 } */
    private static final String[][] CHANCES = {
            {"魔冢遗宝", "乱葬岗下掘出一块血色魔骨，煞气自骨中汩汩涌出。", "30"},
            {"前辈残魂", "一道残魂扑面而来，将毕生所修煞气尽数渡你。", "25"},
            {"血月当空", "血月凌空，魔气汹涌，此番参悟事半功倍。", "20"},
            {"天魔灌顶", "九天魔气倒灌而下——此乃天大的机缘！", "60"},
            {"心魔反噬", "心魔骤起，煞气逆冲，气血翻涌难平。", "-15"},
            {"魔障临身", "魔障遮眼，道心失守，煞气随之涣散。", "-10"}
    };

    @Autowired
    private ContentService contentService;

    @Autowired
    private TokenManager tokenManager;

    @Autowired
    private DemonUserMapper userMapper;

    @Autowired
    private com.fenglin.springboottest.mapper.DebateMapper debateMapper;

    @Autowired
    private com.fenglin.springboottest.mapper.QuizRecordMapper quizRecordMapper;

    @Autowired
    private com.fenglin.springboottest.mapper.ReadRecordMapper readRecordMapper;

    @Autowired
    private com.fenglin.springboottest.mapper.UserArtMapper userArtMapper;

    @Autowired
    private com.fenglin.springboottest.mapper.ArtMapper artMapper;

    @Autowired
    private com.fenglin.springboottest.mapper.TrialMapper trialMapper;

    @Autowired
    private com.fenglin.springboottest.mapper.CodexMapper codexMapper;

    @Autowired
    private com.fenglin.springboottest.mapper.ArchiveMapper archiveMapper;

    @Autowired
    private com.fenglin.springboottest.mapper.ExploreLogMapper exploreLogMapper;

    @Autowired
    private com.fenglin.springboottest.service.GameService gameService;

    // ==================== v2 加成计算 ====================

    /** 修者参悟综合倍率：魔种天赋 × 转生加成 × 习得功法加成（与秘境产出共用同一套数值） */
    private double exploreMultiplier(DemonUser user) {
        return gameService.exploreMultiplier(user);
    }

    /** 是否已研读过某条目 */
    private boolean hasRead(Long userId, String module, Long refId) {
        return readRecordMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<
                        com.fenglin.springboottest.entity.ReadRecord>()
                        .eq(com.fenglin.springboottest.entity.ReadRecord::getUserId, userId)
                        .eq(com.fenglin.springboottest.entity.ReadRecord::getModule, module)
                        .eq(com.fenglin.springboottest.entity.ReadRecord::getRefId, refId)) > 0;
    }

    /** 首页概览 GET /api/home/overview → 各模块数量 + 精选 */
    @GetMapping("/overview")
    public Result<OverviewVO> overview() {
        return Result.success(contentService.overview());
    }

    /** 跨表检索 GET /api/home/search?kw=关键字 → 分组结果 */
    @GetMapping("/search")
    public Result<SearchResultVO> search(
            @RequestParam(value = "kw", required = false, defaultValue = "") String kw) {
        return Result.success(contentService.search(kw == null ? "" : kw.trim()));
    }

    /** 魔门公告 GET /api/home/announcements → 公告列表（按时间倒序） */
    @GetMapping("/announcements")
    public Result<List<AnnouncementVO>> announcements() {
        return Result.success(contentService.announcements());
    }

    /** 热门修习 GET /api/home/highlights → 各模块最新 2 条混合推荐 */
    @GetMapping("/highlights")
    public Result<List<SearchItemVO>> highlights() {
        return Result.success(contentService.highlights());
    }

    /** 模块列表 GET /api/home/list?type=art&kw= → 该模块全部条目（kw 可选过滤；需境界达标） */
    @GetMapping("/list")
    public Result<?> list(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "type", required = false, defaultValue = "") String type,
            @RequestParam(value = "kw", required = false, defaultValue = "") String kw) {
        String denied = checkRealm(authorization, type);
        if (denied != null) {
            return Result.error(403, denied);
        }
        return Result.success(contentService.list(type, kw));
    }

    /** 内容详情 GET /api/home/detail?type=art&id=1 → 单条完整信息（含正文与属性表；需境界达标） */
    @GetMapping("/detail")
    public Result<?> detail(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "type", required = false, defaultValue = "") String type,
            @RequestParam(value = "id", required = false) Long id) {
        String denied = checkRealm(authorization, type);
        if (denied != null) {
            return Result.error(403, denied);
        }
        DetailVO vo = contentService.detail(type, id);
        if (vo == null) {
            return Result.error(404, "魔渊之中并无此记载");
        }
        return Result.success(vo);
    }

    /** 境界阶梯 GET /api/home/realms → 十境全量 + 各功能门槛（公开） */
    @GetMapping("/realms")
    public Result<RealmLadderVO> realms() {
        RealmLadderVO vo = new RealmLadderVO();
        List<RealmNodeVO> ladder = new ArrayList<>();
        for (int i = 0; i < Realm.NAMES.size(); i++) {
            RealmNodeVO node = new RealmNodeVO(i, Realm.NAMES.get(i), Realm.TITLES.get(i), Realm.DESCS.get(i),
                    Realm.thresholdOf(i));
            ladder.add(node);
        }
        vo.setLadder(ladder);

        List<RealmFeatureVO> features = new ArrayList<>();
        Realm.FEATURE_REQUIREMENT.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .forEach(e -> features.add(new RealmFeatureVO(
                        e.getKey(),
                        TYPE_NAMES.getOrDefault(e.getKey(), e.getKey()),
                        e.getValue(),
                        Realm.name(e.getValue()))));
        vo.setFeatures(features);
        return Result.success(vo);
    }

    /**
     * 魔门排行 GET /api/home/rank?type=exp|quiz|spar → 前 10 名修者（公开）
     * 持魔契时标记本人排名（me=true）
     *
     * type 维度：
     *   exp  —— 英杰总榜：境界位阶 + 煞气修为（缺省）
     *   quiz —— 论道榜：按答对论题数
     *   spar —— 切磋榜：按胜场，同胜场比胜率
     */
    @GetMapping("/rank")
    public Result<List<com.fenglin.springboottest.vo.RankItemVO>> rank(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "type", required = false) String type) {
        DemonUser me = currentUser(authorization);
        String dim = (type == null || type.isBlank()) ? "exp" : type.trim();

        List<DemonUser> users = userMapper.selectList(null);

        // 论道榜：统计每人答对题数
        java.util.Map<Long, Long> quizRight = new java.util.HashMap<>();
        // 切磋榜：胜场 / 负场
        if ("quiz".equals(dim)) {
            for (com.fenglin.springboottest.entity.QuizRecord r : quizRecordMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<
                            com.fenglin.springboottest.entity.QuizRecord>()
                            .eq(com.fenglin.springboottest.entity.QuizRecord::getCorrect, 1))) {
                quizRight.merge(r.getUserId(), 1L, Long::sum);
            }
            users.sort(java.util.Comparator
                    .comparingLong((DemonUser u) -> quizRight.getOrDefault(u.getId(), 0L)).reversed()
                    .thenComparing(DemonUser::getId));
        } else if ("spar".equals(dim)) {
            // 注意：reversed() 必须加在单个比较键上，加在整条链上会把已排好的键再次翻转
            users.sort(java.util.Comparator
                    .comparingInt((DemonUser u) -> u.getSparWin() == null ? 0 : u.getSparWin()).reversed()
                    .thenComparing(java.util.Comparator.comparingDouble((DemonUser u) -> {
                        int w = u.getSparWin() == null ? 0 : u.getSparWin();
                        int l = u.getSparLose() == null ? 0 : u.getSparLose();
                        return w + l == 0 ? 0d : (double) w / (w + l);
                    }).reversed())
                    .thenComparing(DemonUser::getId));
        } else {
            users.sort(java.util.Comparator
                    .comparingInt((DemonUser u) -> Realm.indexOf(u.getCultivation())).reversed()
                    .thenComparing(java.util.Comparator
                            .comparingInt((DemonUser u) -> u.getExp() == null ? 0 : u.getExp()).reversed())
                    .thenComparing(DemonUser::getId));
        }

        List<com.fenglin.springboottest.vo.RankItemVO> items = new ArrayList<>();
        int limit = Math.min(10, users.size());
        for (int i = 0; i < limit; i++) {
            DemonUser u = users.get(i);
            int index = Realm.indexOf(u.getCultivation());
            int metric = u.getExp() == null ? 0 : u.getExp();
            if ("quiz".equals(dim)) {
                metric = quizRight.getOrDefault(u.getId(), 0L).intValue();
            } else if ("spar".equals(dim)) {
                metric = u.getSparWin() == null ? 0 : u.getSparWin();
            }
            items.add(new com.fenglin.springboottest.vo.RankItemVO(
                    i + 1,
                    u.getName(),
                    Realm.name(index),
                    index,
                    Realm.TITLES.get(index),
                    metric,
                    me != null && me.getId().equals(u.getId())));
        }
        return Result.success(items);
    }

    /** 修者境界状态 GET /api/home/realm → 当前修者境界、修为与进度（需持魔契） */
    @GetMapping("/realm")
    public Result<RealmStatusVO> realm(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        DemonUser user = currentUser(authorization);
        if (user == null) {
            return Result.error(401, "未持魔契，请先入魔道。");
        }
        return Result.success(buildStatus(user));
    }

    /**
     * 参悟此篇 POST /api/home/explore → 必须指定具体篇章（art/codex/archive），
     * 每篇每日仅可参悟一次，每日总上限见 Realm.DAILY_EXPLORE_LIMIT；达上限返回提示
     */
    @org.springframework.web.bind.annotation.PostMapping("/explore")
    public Result<?> explore(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @org.springframework.web.bind.annotation.RequestBody java.util.Map<String, Object> body) {
        DemonUser user = currentUser(authorization);
        if (user == null) {
            return Result.error(401, "未持魔契，请先入魔道。");
        }
        String type = body == null ? null : (String) body.get("type");
        Object idObj = body == null ? null : body.get("id");
        Long id = idObj == null ? null : Long.valueOf(idObj.toString());
        if (!"art".equals(type) && !"codex".equals(type) && !"archive".equals(type)) {
            return Result.error(400, "参悟须指明具体篇章（功法阁 / 魔典秘录 / 魔枢典册）。");
        }
        if (id == null) {
            return Result.error(400, "未指明参悟哪一篇。");
        }
        return exploreContent(user, type, id);
    }

    /**
     * 随机参悟 POST /api/home/explore/random → 后端从今日尚未参悟的
     * 功法 / 秘录 / 典籍中挑一篇参悟（每日上限仍生效）
     */
    @org.springframework.web.bind.annotation.PostMapping("/explore/random")
    public Result<?> exploreRandom(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        DemonUser user = currentUser(authorization);
        if (user == null) {
            return Result.error(401, "未持魔契，请先入魔道。");
        }
        if (exploreCountToday(user.getId()) >= Realm.DAILY_EXPLORE_LIMIT) {
            return Result.error(400, String.format(
                    "今日煞气已吸满（参悟 %d/%d 篇），魔渊亦需休憩，明日再修。",
                    Realm.DAILY_EXPLORE_LIMIT, Realm.DAILY_EXPLORE_LIMIT));
        }
        // 收集今日已参悟的 (type,id)，用于排除
        java.util.Set<String> done = new java.util.HashSet<>();
        for (ExploreLog log : exploreLogMapper.selectList(
                new LambdaQueryWrapper<ExploreLog>()
                        .eq(ExploreLog::getUserId, user.getId())
                        .ge(ExploreLog::getCreatedAt, java.time.LocalDate.now().atStartOfDay()))) {
            done.add(log.getContentType() + ":" + log.getContentId());
        }
        java.util.List<String[]> pool = new java.util.ArrayList<>();
        for (com.fenglin.springboottest.entity.Art a : artMapper.selectList(null)) {
            if (!done.contains("art:" + a.getId())) {
                pool.add(new String[]{"art", String.valueOf(a.getId()), a.getName()});
            }
        }
        for (com.fenglin.springboottest.entity.Codex c : codexMapper.selectList(null)) {
            if (!done.contains("codex:" + c.getId())) {
                pool.add(new String[]{"codex", String.valueOf(c.getId()), c.getTitle()});
            }
        }
        for (com.fenglin.springboottest.entity.Archive a : archiveMapper.selectList(null)) {
            if (!done.contains("archive:" + a.getId())) {
                pool.add(new String[]{"archive", String.valueOf(a.getId()), a.getTitle()});
            }
        }
        if (pool.isEmpty()) {
            return Result.error(400, "魔域诸篇今日皆已参悟，明日再临。");
        }
        java.util.Random rnd = new java.util.Random();
        String[] pick = pool.get(rnd.nextInt(pool.size()));
        return exploreContent(user, pick[0], Long.valueOf(pick[1]));
    }

    /** 参悟核心：校验存在 / 每日上限 / 每篇每日一次 → 结算煞气并落日志 */
    private Result<?> exploreContent(DemonUser user, String type, Long id) {
        String title = contentTitle(type, id);
        if (title == null) {
            return Result.error(404, "魔渊之中并无此篇。");
        }
        if (exploreCountToday(user.getId()) >= Realm.DAILY_EXPLORE_LIMIT) {
            return Result.error(400, String.format(
                    "今日煞气已吸满（参悟 %d/%d 篇），魔渊亦需休憩，明日再修。",
                    Realm.DAILY_EXPLORE_LIMIT, Realm.DAILY_EXPLORE_LIMIT));
        }
        if (exploredToday(user.getId(), type, id)) {
            return Result.error(400, String.format("今日已参悟此篇《%s》，不可重复参悟。", title));
        }

        // 魔种天赋「幽息吞吐」提升参悟收益，「虚妄顿悟」有概率双倍；
        // 另叠加转生加成与已习得功法加成
        com.fenglin.springboottest.common.Origins.Bonus origin =
                com.fenglin.springboottest.common.Origins.of(user.getRealm());
        java.util.random.RandomGenerator rng = java.util.random.RandomGenerator.getDefault();
        int gain = (int) Math.round(Realm.EXPLORE_GAIN * exploreMultiplier(user));
        boolean insight = false;
        if (origin.getInsightChance() > 0 && rng.nextDouble() < origin.getInsightChance()) {
            gain *= 2;
            insight = true;
        }

        // 机缘奇遇：小概率触发随机事件，打破「每日点十下」的枯燥
        java.util.Map<String, Object> chance = null;
        if (rng.nextDouble() < Realm.CHANCE_RATE) {
            String[] ev = CHANCES[rng.nextInt(CHANCES.length)];
            int delta = Integer.parseInt(ev[2]);
            chance = new java.util.HashMap<>();
            chance.put("name", ev[0]);
            chance.put("desc", ev[1]);
            chance.put("delta", delta);
            gain += delta;
        }

        int newExp = Math.max(0, (user.getExp() == null ? 0 : user.getExp()) + gain);
        com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<DemonUser> uw =
                new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<>();
        uw.eq("id", user.getId()).set("exp", newExp);
        userMapper.update(null, uw);

        ExploreLog log = new ExploreLog();
        log.setUserId(user.getId());
        log.setContentType(type);
        log.setContentId(id);
        log.setTitle(title);
        log.setGain(gain);
        exploreLogMapper.insert(log);

        int newCount = (int) exploreCountToday(user.getId());
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("exp", newExp);
        data.put("gain", gain);
        data.put("insight", insight);
        data.put("originTitle", origin.getTitle());
        data.put("exploreCount", newCount);
        data.put("exploreLimit", Realm.DAILY_EXPLORE_LIMIT);
        data.put("chance", chance);
        data.put("title", title);
        data.put("exploredToday", true);
        return Result.success(data);
    }

    /** 今日已参悟总篇数（基于 demon_explore_log） */
    private long exploreCountToday(Long userId) {
        return exploreLogMapper.selectCount(
                new LambdaQueryWrapper<ExploreLog>()
                        .eq(ExploreLog::getUserId, userId)
                        .ge(ExploreLog::getCreatedAt, java.time.LocalDate.now().atStartOfDay()));
    }

    /** 今日是否已参悟过该篇 */
    private boolean exploredToday(Long userId, String type, Long id) {
        return exploreLogMapper.selectCount(
                new LambdaQueryWrapper<ExploreLog>()
                        .eq(ExploreLog::getUserId, userId)
                        .eq(ExploreLog::getContentType, type)
                        .eq(ExploreLog::getContentId, id)
                        .ge(ExploreLog::getCreatedAt, java.time.LocalDate.now().atStartOfDay())) > 0;
    }

    /** 取内容标题；内容不存在返回 null */
    private String contentTitle(String type, Long id) {
        if ("art".equals(type)) {
            com.fenglin.springboottest.entity.Art a = artMapper.selectById(id);
            return a == null ? null : a.getName();
        }
        if ("codex".equals(type)) {
            com.fenglin.springboottest.entity.Codex c = codexMapper.selectById(id);
            return c == null ? null : c.getTitle();
        }
        if ("archive".equals(type)) {
            com.fenglin.springboottest.entity.Archive a = archiveMapper.selectById(id);
            return a == null ? null : a.getTitle();
        }
        return null;
    }

    /**
     * 冲击境界 POST /api/home/breakthrough → 煞气攒足则晋升一境（需持魔契）
     * 成功：境界 +1，扣除该境所需修为；失败：返回还差多少煞气
     */
    @org.springframework.web.bind.annotation.PostMapping("/breakthrough")
    public Result<?> breakthrough(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        DemonUser user = currentUser(authorization);
        if (user == null) {
            return Result.error(401, "未持魔契，请先入魔道。");
        }
        int index = Realm.indexOf(user.getCultivation());
        if (index >= Realm.NAMES.size() - 1) {
            return Result.error(400, "已至无上之境，魔域再无更高处。");
        }
        int exp = user.getExp() == null ? 0 : user.getExp();
        int need = Realm.thresholdOf(index + 1);
        if (exp < need) {
            return Result.error(400, String.format(
                    "煞气不足，冲击「%s」境失败。当前煞气 %d / 需 %d，还差 %d 点。",
                    Realm.name(index + 1), exp, need, need - exp));
        }

        // 渡劫：煞气达标只是入场券，仍有可能被心魔阻道；连败达上限则本番必成（保底）
        int failStreak = user.getBreakFail() == null ? 0 : user.getBreakFail();
        boolean guaranteed = failStreak >= Realm.BREAK_FAIL_GUARANTEE;
        java.util.random.RandomGenerator brng = java.util.random.RandomGenerator.getDefault();
        if (!guaranteed && brng.nextDouble() < Realm.BREAK_FAIL_RATE) {
            int loss = Math.min(Realm.BREAK_FAIL_LOSS_CAP,
                    (int) Math.round(exp * Realm.BREAK_FAIL_LOSS_RATE));
            int left = Math.max(0, exp - loss);
            com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<DemonUser> fuw =
                    new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<>();
            fuw.eq("id", user.getId()).set("exp", left).set("break_fail", failStreak + 1);
            userMapper.update(null, fuw);
            return Result.error(400, String.format(
                    "心魔骤起，渡劫失败！煞气溃散 %d 点（尚余 %d），此乃第 %d 次受阻——再败一次，天道必偿。",
                    loss, left, failStreak + 1));
        }

        // v2 关键修复：冲境只判累计达标，不再扣除煞气
        // （旧版扣除煞气会让「战力 = 境阶×权重 + 煞气」不升反降，导致玩家宁愿囤气不突破）
        String next = Realm.name(index + 1);
        com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<DemonUser> uw =
                new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<>();
        uw.eq("id", user.getId()).set("cultivation", next).set("break_fail", 0);
        userMapper.update(null, uw);
        return Result.success(String.format(
                "轰——魔气灌体，渡劫功成！已晋升「%s」境（第 %d 重）。%s",
                next, index + 2,
                guaranteed ? "（天道酬勤，此番再无阻碍。）" : ""));
    }

    /**
     * 论道问答 GET /api/home/quiz?id=1 → 论题与三个选项（不下发答案；需魔卒境）
     */
    @GetMapping("/quiz")
    public Result<?> quiz(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "id", required = false) Long id) {
        String denied = checkRealm(authorization, "debate");
        if (denied != null) {
            return Result.error(403, denied);
        }
        if (id == null) {
            return Result.error(400, "缺论题之 id。");
        }
        com.fenglin.springboottest.entity.Debate d = debateMapper.selectById(id);
        if (d == null || d.getAnswer() == null) {
            return Result.error(404, "魔渊之中并无此论题。");
        }
        return Result.success(new com.fenglin.springboottest.vo.DebateQuizVO(
                d.getId(), d.getTitle(), d.getSpeaker(), d.getSummary(),
                java.util.List.of(d.getOptionA(), d.getOptionB(), d.getOptionC())));
    }

    /**
     * 论道作答 POST /api/home/quiz/answer {id, choice} → 判定正误并结算煞气（需魔卒境）
     * 答对：+10 煞气；答错：心魔反噬 -5（不低于 0）
     *
     * 防刷规则（两道闸）：
     *   1) 同一修者对同一论题只能作答一次（demon_quiz_record 唯一键 user_id + debate_id）
     *   2) 每日答题上限 Realm.DAILY_QUIZ_LIMIT 题，跨日清零
     */
    @org.springframework.web.bind.annotation.PostMapping("/quiz/answer")
    public Result<?> quizAnswer(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @org.springframework.web.bind.annotation.RequestBody
            com.fenglin.springboottest.dto.QuizAnswerDTO dto) {
        String denied = checkRealm(authorization, "debate");
        if (denied != null) {
            return Result.error(403, denied);
        }
        if (dto == null || dto.getId() == null
                || dto.getChoice() == null || dto.getChoice().isBlank()) {
            return Result.error(400, "论题与所答选项皆不可为空。");
        }

        DemonUser user = currentUser(authorization);
        if (user == null) {
            return Result.error(401, "未持魔契，请先入魔道。");
        }

        // 闸二：每日答题上限（跨日清零）
        java.time.LocalDate today = java.time.LocalDate.now();
        int quizCount = user.getQuizCountToday() == null ? 0 : user.getQuizCountToday();
        if (user.getQuizDate() == null || !today.equals(user.getQuizDate())) {
            quizCount = 0;
        }
        if (quizCount >= Realm.DAILY_QUIZ_LIMIT) {
            return Result.error(400, String.format(
                    "今日论道已满 %d 题，道心需静，明日再来。", Realm.DAILY_QUIZ_LIMIT));
        }

        com.fenglin.springboottest.entity.Debate d = debateMapper.selectById(dto.getId());
        if (d == null || d.getAnswer() == null) {
            return Result.error(404, "魔渊之中并无此论题。");
        }

        // 闸一：本论题是否已作答（先查，把绝大多数重复请求挡在写库之前）
        Long answered = quizRecordMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.fenglin.springboottest.entity.QuizRecord>()
                        .eq(com.fenglin.springboottest.entity.QuizRecord::getUserId, user.getId())
                        .eq(com.fenglin.springboottest.entity.QuizRecord::getDebateId, d.getId()));
        if (answered != null && answered > 0) {
            return Result.error(400, "此论题你已作答，论道不可反复叩问同一问，另择他题罢。");
        }

        String choice = dto.getChoice().trim().toUpperCase();
        boolean correct = choice.equals(d.getAnswer());
        int delta = correct ? Realm.QUIZ_GAIN : -Realm.QUIZ_LOSS;
        int oldExp = user.getExp() == null ? 0 : user.getExp();
        int exp = Math.max(0, oldExp + delta);
        int actualDelta = exp - oldExp;

        // 先落答题记录：唯一键在此拦住并发重复提交，插入成功才结算煞气
        com.fenglin.springboottest.entity.QuizRecord rec =
                new com.fenglin.springboottest.entity.QuizRecord();
        rec.setUserId(user.getId());
        rec.setDebateId(d.getId());
        rec.setCorrect(correct ? 1 : 0);
        rec.setAwarded(actualDelta);
        try {
            quizRecordMapper.insert(rec);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            return Result.error(400, "此论题你已作答，论道不可反复叩问同一问，另择他题罢。");
        }

        com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<DemonUser> uw =
                new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<>();
        uw.eq("id", user.getId())
                .set("exp", exp)
                .set("quiz_date", today)
                .set("quiz_count_today", quizCount + 1);
        userMapper.update(null, uw);

        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("correct", correct);
        data.put("answer", d.getAnswer());
        data.put("delta", actualDelta);
        data.put("exp", exp);
        data.put("quizCount", quizCount + 1);
        data.put("quizLimit", Realm.DAILY_QUIZ_LIMIT);
        data.put("quizRemain", Realm.DAILY_QUIZ_LIMIT - quizCount - 1);
        data.put("analysis", d.getAnalysis());
        data.put("msg", correct
                ? "论道得胜，道心愈明！煞气 +" + actualDelta + "。"
                : "心魔反噬，所答有偏。煞气 -" + Math.abs(actualDelta) + "，且听论者解析。");
        return Result.success(data);
    }

    /**
     * 个人道牌（勋章墙）GET /api/home/badges → 十境勋章 + 修行数据总览（需持魔契）
     */
    @GetMapping("/badges")
    public Result<?> badges(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        DemonUser user = currentUser(authorization);
        if (user == null) {
            return Result.error(401, "未持魔契，请先入魔道。");
        }
        RealmStatusVO st = buildStatus(user);

        com.fenglin.springboottest.vo.RealmBadgeVO vo = new com.fenglin.springboottest.vo.RealmBadgeVO();
        vo.setName(st.getName());
        vo.setCultivation(st.getCultivation());
        vo.setIndex(st.getIndex());
        vo.setTitle(Realm.TITLES.get(st.getIndex()));
        vo.setExp(st.getExp());
        vo.setUnlockCount((int) Realm.FEATURE_REQUIREMENT.values().stream()
                .filter(req -> st.getIndex() >= req).count());
        vo.setTotalHalls(Realm.FEATURE_REQUIREMENT.size());
        vo.setExploreCount(st.getExploreCount());
        vo.setExploreLimit(st.getExploreLimit());

        List<com.fenglin.springboottest.vo.RealmBadgeVO.MedalVO> medals = new ArrayList<>();
        for (int i = 0; i < Realm.NAMES.size(); i++) {
            medals.add(new com.fenglin.springboottest.vo.RealmBadgeVO.MedalVO(
                    i, Realm.NAMES.get(i), Realm.TITLES.get(i), st.getIndex() >= i));
        }
        vo.setMedallions(medals);
        return Result.success(vo);
    }

    /**
     * 魔种天赋一览 GET /api/home/origins → 四系天赋清单（公开，注册页/道牌页展示用）
     */
    @GetMapping("/origins")
    public Result<?> origins() {
        List<java.util.Map<String, Object>> list = new ArrayList<>();
        for (com.fenglin.springboottest.common.Origins.Bonus b
                : com.fenglin.springboottest.common.Origins.all()) {
            list.add(originToMap(b));
        }
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("origins", list);
        data.put("fromTable", com.fenglin.springboottest.common.Origins.isLoadedFromTable());
        return Result.success(data);
    }

    /**
     * 我的魔种天赋 GET /api/home/origin → 当前修者魔种天赋详情（需持魔契）
     */
    @GetMapping("/origin")
    public Result<?> origin(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        DemonUser user = currentUser(authorization);
        if (user == null) {
            return Result.error(401, "未持魔契，请先入魔道。");
        }
        com.fenglin.springboottest.common.Origins.Bonus mine =
                com.fenglin.springboottest.common.Origins.of(user.getRealm());
        java.util.Map<String, Object> data = originToMap(mine);
        data.put("origin", user.getRealm());
        data.put("fromTable", com.fenglin.springboottest.common.Origins.isLoadedFromTable());
        return Result.success(data);
    }

    /** 天赋 → 前端可用结构 */
    private java.util.Map<String, Object> originToMap(
            com.fenglin.springboottest.common.Origins.Bonus b) {
        java.util.Map<String, Object> m = new java.util.HashMap<>();
        m.put("origin", b.getOrigin());
        m.put("title", b.getTitle());
        m.put("description", b.getDescription());
        m.put("exploreBonus", b.getExploreBonus());
        m.put("sparPowerBonus", b.getSparPowerBonus());
        m.put("sparRewardBonus", b.getSparRewardBonus());
        m.put("insightChance", b.getInsightChance());
        return m;
    }

    /**
     * 修者切磋 POST /api/home/sparring {targetName} → 与指定修者对战并结算煞气（需持魔契）
     * 战力 = 境界位阶×1000 + 煞气 + 天运波动(0~399)；境界差 ≥2 时低境一方战力按六折计（境界压制）
     * 胜者 +15 煞气，败者 -10（不低于 0）；每日上限 Realm.DAILY_SPAR_LIMIT 场，跨日清零
     */
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.web.bind.annotation.PostMapping("/sparring")
    public Result<?> sparring(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @org.springframework.web.bind.annotation.RequestBody
            com.fenglin.springboottest.dto.SparringDTO dto) {
        DemonUser me = currentUser(authorization);
        if (me == null) {
            return Result.error(401, "未持魔契，请先入魔道。");
        }
        if (dto == null || !StringUtils.hasText(dto.getTargetName())) {
            return Result.error(400, "未指明切磋对手。");
        }

        // 今日切磋次数（跨日清零）
        java.time.LocalDate today = java.time.LocalDate.now();
        int sparCount = me.getSparCountToday() == null ? 0 : me.getSparCountToday();
        if (me.getSparDate() == null || !today.equals(me.getSparDate())) {
            sparCount = 0;
        }

        DemonUser target = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DemonUser>()
                        .eq(DemonUser::getName, dto.getTargetName().trim()));
        if (target == null) {
            return Result.error(404, "魔域之中并无此修者。");
        }
        if (target.getId().equals(me.getId())) {
            return Result.error(400, "不可与自己的影子切磋。");
        }
        if (sparCount >= Realm.DAILY_SPAR_LIMIT) {
            return Result.error(400, String.format("今日切磋已达 %d 场上限，魔气未复，明日再来。",
                    Realm.DAILY_SPAR_LIMIT));
        }

        int myIndex = Realm.indexOf(me.getCultivation());
        int targetIndex = Realm.indexOf(target.getCultivation());
        int myExpBase = me.getExp() == null ? 0 : me.getExp();
        int targetExpBase = target.getExp() == null ? 0 : target.getExp();

        // 境界压制：位阶差 ≥2，低境一方战力打六折
        int gap = myIndex - targetIndex;
        double myMod = gap <= -2 ? 0.6 : 1.0;
        double targetMod = gap >= 2 ? 0.6 : 1.0;

        // 魔种天赋：血煞「战意滔天」提升战力，噬魂「夺魂摄魄」提升胜利掠夺
        com.fenglin.springboottest.common.Origins.Bonus myOrigin =
                com.fenglin.springboottest.common.Origins.of(me.getRealm());
        double powerMod = 1 + myOrigin.getSparPowerBonus();

        // v2：境界权重由 1000 提至 8000，让「一重境」真正压过「囤下的煞气」
        int myBase = myIndex * Realm.POWER_PER_REALM + myExpBase;
        int targetBase = targetIndex * Realm.POWER_PER_REALM + targetExpBase;
        java.util.random.RandomGenerator rng = java.util.random.RandomGenerator.getDefault();
        int myPower = (int) Math.round((myBase * powerMod + rng.nextInt(Realm.SPAR_RANDOM)) * myMod);
        int targetPower = (int) Math.round((targetBase + rng.nextInt(Realm.SPAR_RANDOM)) * targetMod);
        boolean win = myPower >= targetPower;

        // 预估胜率（按基础战力 + 压制系数，不含天运波动；仅供展示）
        double myWeight = myBase * powerMod * myMod;
        double targetWeight = targetBase * targetMod;
        int winChance = (int) Math.round(myWeight * 100 / (myWeight + targetWeight));

        // 结算：胜者 +15（噬魂天赋可翻倍掠夺），败者 -10（不低于 0）
        int myWinReward = (int) Math.round(Realm.SPAR_WIN * (1 + myOrigin.getSparRewardBonus()));
        int myDelta = win ? myWinReward : -Realm.SPAR_LOSS;
        int targetDelta = win ? -Realm.SPAR_LOSS : Realm.SPAR_WIN;
        int myExp = Math.max(0, myExpBase + myDelta);
        int targetExp = Math.max(0, targetExpBase + targetDelta);

        // 我方：更新煞气 + 累计今日切磋次数
        com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<DemonUser> myUw =
                new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<>();
        int myWin0 = me.getSparWin() == null ? 0 : me.getSparWin();
        int myLose0 = me.getSparLose() == null ? 0 : me.getSparLose();
        myUw.eq("id", me.getId())
                .set("exp", myExp)
                .set("spar_date", today)
                .set("spar_count_today", sparCount + 1)
                .set("spar_win", myWin0 + (win ? 1 : 0))
                .set("spar_lose", myLose0 + (win ? 0 : 1));
        userMapper.update(null, myUw);

        // 对手同样落库（被动应战也计入战绩）
        int tWin0 = target.getSparWin() == null ? 0 : target.getSparWin();
        int tLose0 = target.getSparLose() == null ? 0 : target.getSparLose();
        com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<DemonUser> tUw =
                new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<>();
        tUw.eq("id", target.getId())
                .set("exp", targetExp)
                .set("spar_win", tWin0 + (win ? 0 : 1))
                .set("spar_lose", tLose0 + (win ? 1 : 0));
        userMapper.update(null, tUw);

        // 战报（含战力对比与境界压制说明）
        String myRealm = Realm.name(myIndex);
        String targetRealm = Realm.name(targetIndex);
        List<String> report = new ArrayList<>();
        report.add(String.format("「%s」（%s境）煞气灌体，率先出手！", me.getName(), myRealm));
        report.add(String.format("「%s」（%s境）不闪不避，硬撼其锋！", target.getName(), targetRealm));
        if (gap >= 2) {
            report.add(String.format("境界压制——「%s」高出两重以上，「%s」战力折损三成！", me.getName(), target.getName()));
        } else if (gap <= -2) {
            report.add(String.format("境界压制——「%s」高出两重以上，「%s」战力折损三成！", target.getName(), me.getName()));
        }
        if (myOrigin.hasAnyBonus()) {
            report.add(String.format("魔种天赋「%s」激发——%s", myOrigin.getTitle(),
                    myOrigin.getSparRewardBonus() > 0 ? "夺魂摄魄，胜则连其煞气一并吞下！"
                            : "战意滔天，魔气暴涨一成有余！"));
        }
        report.add(String.format("两道魔气轰然相撞（战力 %d : %d），差距 %d 点——「%s」技高一筹！",
                myPower, targetPower, Math.abs(myPower - targetPower), win ? me.getName() : target.getName()));
        report.add(win
                ? String.format("切磋终了：「%s」胜，煞气 +%d；「%s」败退，煞气 -%d。今日已切磋 %d/%d 场。",
                        me.getName(), myWinReward, target.getName(), Realm.SPAR_LOSS,
                        sparCount + 1, Realm.DAILY_SPAR_LIMIT)
                : String.format("切磋终了：「%s」胜，煞气 +%d；「%s」败退，煞气 -%d。今日已切磋 %d/%d 场。",
                        target.getName(), Realm.SPAR_WIN, me.getName(), Realm.SPAR_LOSS,
                        sparCount + 1, Realm.DAILY_SPAR_LIMIT));

        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("win", win);
        data.put("report", report);
        data.put("myDelta", myDelta);
        data.put("myExp", myExp);
        data.put("originTitle", myOrigin.getTitle());
        data.put("myPower", myPower);
        data.put("targetPower", targetPower);
        data.put("targetName", target.getName());
        data.put("targetRealm", targetRealm);
        data.put("targetTitle", Realm.TITLES.get(targetIndex));
        data.put("targetExp", targetExp);
        data.put("targetDelta", targetDelta);
        data.put("winChance", winChance);
        data.put("sparCount", sparCount + 1);
        data.put("sparLimit", Realm.DAILY_SPAR_LIMIT);
        data.put("sparLeft", Realm.DAILY_SPAR_LIMIT - sparCount - 1);
        data.put("sparWin", myWin0 + (win ? 1 : 0));
        data.put("sparLose", myLose0 + (win ? 0 : 1));
        return Result.success(data);
    }

    // ==================== v2 新玩法接口 ====================

    /**
     * 签到状态 GET /api/home/sign → 今日是否已签、连签天数、今日可领煞气（需持魔契）
     */
    @GetMapping("/sign")
    public Result<?> signInfo(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        DemonUser user = currentUser(authorization);
        if (user == null) {
            return Result.error(401, "未持魔契，请先入魔道。");
        }
        java.time.LocalDate today = java.time.LocalDate.now();
        boolean signed = today.equals(user.getSignDate());
        int streak = user.getSignStreak() == null ? 0 : user.getSignStreak();
        int nextStreak = (user.getSignDate() != null && user.getSignDate().plusDays(1).equals(today))
                ? streak + 1 : 1;
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("signed", signed);
        data.put("streak", streak);
        data.put("total", user.getSignTotal() == null ? 0 : user.getSignTotal());
        data.put("nextStreak", nextStreak);
        data.put("reward", signed ? 0 : signReward(nextStreak));
        return Result.success(data);
    }

    /**
     * 每日签到 POST /api/home/sign → 连续签到煞气递增，连满七日另有大赏（需持魔契）
     */
    @org.springframework.web.bind.annotation.PostMapping("/sign")
    public Result<?> sign(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        DemonUser user = currentUser(authorization);
        if (user == null) {
            return Result.error(401, "未持魔契，请先入魔道。");
        }
        java.time.LocalDate today = java.time.LocalDate.now();
        if (today.equals(user.getSignDate())) {
            return Result.error(400, "今日香已焚过，明日再来。");
        }
        int streak0 = user.getSignStreak() == null ? 0 : user.getSignStreak();
        int streak = (user.getSignDate() != null && user.getSignDate().plusDays(1).equals(today))
                ? streak0 + 1 : 1;
        int gain = signReward(streak);
        int exp = (user.getExp() == null ? 0 : user.getExp()) + gain;

        com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<DemonUser> uw =
                new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<>();
        uw.eq("id", user.getId())
                .set("exp", exp)
                .set("sign_date", today)
                .set("sign_streak", streak)
                .set("sign_total", (user.getSignTotal() == null ? 0 : user.getSignTotal()) + 1);
        userMapper.update(null, uw);

        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("gain", gain);
        data.put("exp", exp);
        data.put("streak", streak);
        data.put("bonus7", streak % Realm.SIGN_STREAK_CAP == 0);
        data.put("msg", String.format("魔前焚香，第 %d 日不间断——煞气 +%d。%s",
                streak, gain, streak % Realm.SIGN_STREAK_CAP == 0 ? "连满七日，另得大赏！" : ""));
        return Result.success(data);
    }

    /** 连签第 n 日可得煞气：基础 + 递增（封顶）+ 满七日大赏 */
    private int signReward(int streak) {
        int step = Math.min(streak, Realm.SIGN_STREAK_CAP) - 1;
        int gain = Realm.SIGN_BASE + Math.max(0, step) * Realm.SIGN_STEP;
        if (streak % Realm.SIGN_STREAK_CAP == 0) {
            gain += Realm.SIGN_BONUS_7;
        }
        return gain;
    }

    /**
     * 转生轮回 POST /api/home/reincarnate → 无上境方可涅槃，境界归元而煞气尽散，
     * 换取每世 +10% 的永久参悟加成与「第 N 世」称号（需持魔契）
     */
    @org.springframework.web.bind.annotation.PostMapping("/reincarnate")
    public Result<?> reincarnate(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        DemonUser user = currentUser(authorization);
        if (user == null) {
            return Result.error(401, "未持魔契，请先入魔道。");
        }
        int index = Realm.indexOf(user.getCultivation());
        if (index < Realm.NAMES.size() - 1) {
            return Result.error(400, String.format(
                    "尚在「%s」境，未登无上——轮回之门只对绝巅者开启。", Realm.name(index)));
        }
        int times = (user.getReincarnate() == null ? 0 : user.getReincarnate()) + 1;
        com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<DemonUser> uw =
                new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<>();
        uw.eq("id", user.getId())
                .set("cultivation", Realm.NAMES.get(0))
                .set("exp", 0)
                .set("reincarnate", times)
                .set("break_fail", 0);
        userMapper.update(null, uw);

        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("times", times);
        data.put("cultivation", Realm.NAMES.get(0));
        data.put("bonus", times * Realm.REINCARNATE_BONUS);
        data.put("msg", String.format(
                "轮回印开——第 %d 世重修！境界归元、煞气尽散，然道心不灭：此后参悟所得永久 +%.0f%%。",
                times, times * Realm.REINCARNATE_BONUS * 100));
        return Result.success(data);
    }

    /**
     * 研读典籍 POST /api/home/read {module,id} → 首次研读该篇得煞气，并计入图鉴收集度
     * 每日上限 5 篇（需持魔契）
     */
    @org.springframework.web.bind.annotation.PostMapping("/read")
    public Result<?> read(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @org.springframework.web.bind.annotation.RequestBody java.util.Map<String, Object> body) {
        DemonUser user = currentUser(authorization);
        if (user == null) {
            return Result.error(401, "未持魔契，请先入魔道。");
        }
        String module = body == null ? null : String.valueOf(body.get("module"));
        Object idObj = body == null ? null : body.get("id");
        if (module == null || !TYPE_NAMES.containsKey(module) || idObj == null) {
            return Result.error(400, "未指明所读何典。");
        }
        long refId = Long.parseLong(String.valueOf(idObj));
        if (hasRead(user.getId(), module, refId)) {
            return Result.error(400, "此篇昔日已曾研读，温故虽好，却无煞气可积。");
        }

        java.time.LocalDate today = java.time.LocalDate.now();
        int readCount = user.getReadCountToday() == null ? 0 : user.getReadCountToday();
        if (user.getReadDate() == null || !today.equals(user.getReadDate())) {
            readCount = 0;
        }
        boolean rewarded = readCount < Realm.DAILY_READ_LIMIT;
        int gain = rewarded ? Realm.READ_GAIN : 0;
        int exp = (user.getExp() == null ? 0 : user.getExp()) + gain;

        com.fenglin.springboottest.entity.ReadRecord rec =
                new com.fenglin.springboottest.entity.ReadRecord();
        rec.setUserId(user.getId());
        rec.setModule(module);
        rec.setRefId(refId);
        readRecordMapper.insert(rec);

        com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<DemonUser> uw =
                new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<>();
        uw.eq("id", user.getId())
                .set("exp", exp)
                .set("read_date", today)
                .set("read_count_today", readCount + 1);
        userMapper.update(null, uw);

        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("gain", gain);
        data.put("exp", exp);
        data.put("rewarded", rewarded);
        data.put("readCount", readCount + 1);
        data.put("readLimit", Realm.DAILY_READ_LIMIT);
        data.put("collection", collectionOf(user.getId()));
        data.put("msg", rewarded
                ? String.format("掩卷沉思，心有所得——煞气 +%d。", gain)
                : "今日所读已足五篇，道心需自沉淀，明日再览。");
        return Result.success(data);
    }

    /**
     * 图鉴收集度 GET /api/home/collection → 已读篇数 / 全库总篇数（需持魔契）
     */
    @GetMapping("/collection")
    public Result<?> collection(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        DemonUser user = currentUser(authorization);
        if (user == null) {
            return Result.error(401, "未持魔契，请先入魔道。");
        }
        return Result.success(collectionOf(user.getId()));
    }

    /** 统计某修者的图鉴收集度 */
    private java.util.Map<String, Object> collectionOf(Long userId) {
        long read = readRecordMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<
                        com.fenglin.springboottest.entity.ReadRecord>()
                        .eq(com.fenglin.springboottest.entity.ReadRecord::getUserId, userId));
        long total = artMapper.selectCount(null)
                + trialMapper.selectCount(null)
                + debateMapper.selectCount(null)
                + codexMapper.selectCount(null)
                + archiveMapper.selectCount(null);
        java.util.Map<String, Object> m = new java.util.HashMap<>();
        m.put("read", read);
        m.put("total", total);
        m.put("percent", total == 0 ? 0 : Math.round(read * 100.0 / total));
        return m;
    }

    /**
     * 习得功法 POST /api/home/learn/{artId} → 每部功法提供 +5% 参悟加成（最多叠加 4 部）
     * 可习得数量随境界提升：1 + 境界位阶（需持魔契）
     */
    @org.springframework.web.bind.annotation.PostMapping("/learn/{artId}")
    public Result<?> learnArt(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @org.springframework.web.bind.annotation.PathVariable Long artId) {
        DemonUser user = currentUser(authorization);
        if (user == null) {
            return Result.error(401, "未持魔契，请先入魔道。");
        }
        if (artMapper.selectById(artId) == null) {
            return Result.error(404, "魔域之中并无此功法。");
        }
        long learned = userArtMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<
                        com.fenglin.springboottest.entity.UserArt>()
                        .eq(com.fenglin.springboottest.entity.UserArt::getUserId, user.getId()));
        if (userArtMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<
                        com.fenglin.springboottest.entity.UserArt>()
                        .eq(com.fenglin.springboottest.entity.UserArt::getUserId, user.getId())
                        .eq(com.fenglin.springboottest.entity.UserArt::getArtId, artId)) > 0) {
            return Result.error(400, "此功法早已烂熟于心。");
        }
        int cap = 1 + Realm.indexOf(user.getCultivation());
        if (learned >= cap) {
            return Result.error(400, String.format(
                    "以「%s」境之魔识，最多只能同时驾驭 %d 部功法——先冲境去罢。",
                    Realm.name(Realm.indexOf(user.getCultivation())), cap));
        }
        com.fenglin.springboottest.entity.UserArt ua =
                new com.fenglin.springboottest.entity.UserArt();
        ua.setUserId(user.getId());
        ua.setArtId(artId);
        userArtMapper.insert(ua);

        int effective = (int) Math.min(learned + 1, Realm.ART_BONUS_CAP);
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("learned", learned + 1);
        data.put("cap", cap);
        data.put("bonus", effective * Realm.ART_BONUS);
        data.put("msg", String.format("功法入体，运转如意——参悟所得 +%.0f%%（已习 %d/%d 部）。",
                effective * Realm.ART_BONUS * 100, learned + 1, cap));
        return Result.success(data);
    }

    /**
     * 随机论题 GET /api/home/quiz/random → 取一道本人尚未作答的论题（需魔卒境）
     * 供首页「今日论道」直接开答，无需先钻进详情页
     */
    @GetMapping("/quiz/random")
    public Result<?> quizRandom(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        String denied = checkRealm(authorization, "debate");
        if (denied != null) {
            return Result.error(403, denied);
        }
        Long userId = currentUserId(authorization);
        java.util.List<com.fenglin.springboottest.entity.Debate> all = debateMapper.selectList(null);
        java.util.List<com.fenglin.springboottest.entity.Debate> fresh = new ArrayList<>();
        for (com.fenglin.springboottest.entity.Debate d : all) {
            if (d.getAnswer() == null) {
                continue;
            }
            long answered = quizRecordMapper.selectCount(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<
                            com.fenglin.springboottest.entity.QuizRecord>()
                            .eq(com.fenglin.springboottest.entity.QuizRecord::getUserId, userId)
                            .eq(com.fenglin.springboottest.entity.QuizRecord::getDebateId, d.getId()));
            if (answered == 0) {
                fresh.add(d);
            }
        }
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        if (fresh.isEmpty()) {
            data.put("quiz", null);
            data.put("msg", "魔渊之中论题已尽数辩过，静待新论。");
            return Result.success(data);
        }
        com.fenglin.springboottest.entity.Debate d =
                fresh.get(java.util.random.RandomGenerator.getDefault().nextInt(fresh.size()));
        data.put("quiz", new com.fenglin.springboottest.vo.DebateQuizVO(
                d.getId(), d.getTitle(), d.getSpeaker(), d.getSummary(),
                java.util.List.of(d.getOptionA(), d.getOptionB(), d.getOptionC())));
        return Result.success(data);
    }

    /** 按主键更新修者煞气修为 */
    private void updateExp(Long userId, int exp) {
        com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<DemonUser> uw =
                new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<>();
        uw.eq("id", userId).set("exp", exp);
        userMapper.update(null, uw);
    }

    /** 由 Authorization 头解析当前修者（无效返回 null） */
    private DemonUser currentUser(String authorization) {
        Long userId = currentUserId(authorization);
        return userId == null ? null : userMapper.selectById(userId);
    }

    private Long currentUserId(String authorization) {
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return tokenManager.getUserId(authorization.substring(7).trim());
    }

    /**
     * 境界门槛校验：达标返回 null，不足返回提示文案
     */
    private String checkRealm(String authorization, String type) {
        DemonUser user = currentUser(authorization);
        if (user == null) {
            return "未持魔契，请先入魔道。";
        }
        if (!Realm.satisfied(user.getCultivation(), type)) {
            return String.format(Realm.DENY_MSG, Realm.name(Realm.requirementOf(type)));
        }
        return null;
    }

    /** 组装修者境界状态（境界/位阶/进度/修为/下一境阈值/可否冲击/今日参悟） */
    private RealmStatusVO buildStatus(DemonUser user) {
        int index = Realm.indexOf(user.getCultivation());
        int exp = user.getExp() == null ? 0 : user.getExp();
        boolean maxed = index >= Realm.NAMES.size() - 1;
        int need = maxed ? 0 : Realm.thresholdOf(index + 1);

        // 今日参悟次数（基于 demon_explore_log，每篇每日一次，跨日自动清零）
        int countToday = (int) exploreCountToday(user.getId());

        // 今日切磋次数（跨日视为 0）
        java.time.LocalDate today = java.time.LocalDate.now();
        int sparCount = user.getSparCountToday() == null ? 0 : user.getSparCountToday();
        if (user.getSparDate() == null || !today.equals(user.getSparDate())) {
            sparCount = 0;
        }

        RealmStatusVO vo = new RealmStatusVO();
        vo.setName(user.getName());
        vo.setCultivation(Realm.name(index));
        vo.setIndex(index);
        vo.setTotal(Realm.NAMES.size());
        vo.setPercent(Math.round(index * 100f / (Realm.NAMES.size() - 1)));
        vo.setNext(maxed ? null : Realm.name(index + 1));
        vo.setExp(exp);
        vo.setNextExp(need);
        vo.setCanBreakthrough(!maxed && exp >= need);
        vo.setExploreCount(countToday);
        vo.setExploreLimit(Realm.DAILY_EXPLORE_LIMIT);
        vo.setSparCount(sparCount);
        vo.setSparLimit(Realm.DAILY_SPAR_LIMIT);

        // v2：签到 / 战绩 / 转生 / 研读 / 图鉴
        boolean signed = today.equals(user.getSignDate());
        int streak0 = user.getSignStreak() == null ? 0 : user.getSignStreak();
        int nextStreak = (user.getSignDate() != null && user.getSignDate().plusDays(1).equals(today))
                ? streak0 + 1 : 1;
        vo.setSignSigned(signed);
        vo.setSignStreak(streak0);
        vo.setSignTotal(user.getSignTotal() == null ? 0 : user.getSignTotal());
        vo.setSignReward(signed ? 0 : signReward(nextStreak));
        vo.setSparWin(user.getSparWin() == null ? 0 : user.getSparWin());
        vo.setSparLose(user.getSparLose() == null ? 0 : user.getSparLose());
        vo.setReincarnate(user.getReincarnate() == null ? 0 : user.getReincarnate());

        int readCount = user.getReadCountToday() == null ? 0 : user.getReadCountToday();
        if (user.getReadDate() == null || !today.equals(user.getReadDate())) {
            readCount = 0;
        }
        vo.setReadCount(readCount);
        vo.setReadLimit(Realm.DAILY_READ_LIMIT);
        vo.setCollectionPercent(
                ((Number) collectionOf(user.getId()).get("percent")).intValue());
        return vo;
    }
}
