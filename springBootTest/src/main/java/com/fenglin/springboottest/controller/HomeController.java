package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.Realm;
import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.common.TokenManager;
import com.fenglin.springboottest.entity.DemonUser;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 魔族修行 · 首页内容接口
 * 基底址：/api/home
 * 境界体系：引煞 → 凝煞 → 魔卒 → 魔尉 → 魔将 → 血煞 → 魔尊 → 魔祖 → 无上 → 魔圣
 */
@RestController
@RequestMapping("/api/home")
public class HomeController {

    /** 模块 type → 展示名（境界门槛提示用） */
    private static final Map<String, String> TYPE_NAMES = Map.of(
            "art", "功法阁",
            "trial", "历练录",
            "debate", "宗门论道",
            "codex", "魔典秘录",
            "archive", "魔枢典册");

    @Autowired
    private ContentService contentService;

    @Autowired
    private TokenManager tokenManager;

    @Autowired
    private DemonUserMapper userMapper;

    @Autowired
    private com.fenglin.springboottest.mapper.DebateMapper debateMapper;

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
            ladder.add(new RealmNodeVO(i, Realm.NAMES.get(i), Realm.TITLES.get(i), Realm.DESCS.get(i)));
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
     * 魔门排行 GET /api/home/rank → 按境界位阶 + 煞气修为排序的前 10 名修者（公开）
     * 持魔契时标记本人排名（me=true）
     */
    @GetMapping("/rank")
    public Result<List<com.fenglin.springboottest.vo.RankItemVO>> rank(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        DemonUser me = currentUser(authorization);

        List<DemonUser> users = userMapper.selectList(null);
        users.sort(java.util.Comparator
                .comparingInt((DemonUser u) -> Realm.indexOf(u.getCultivation())).reversed()
                .thenComparing(java.util.Comparator
                        .comparingInt((DemonUser u) -> u.getExp() == null ? 0 : u.getExp()).reversed())
                .thenComparing(DemonUser::getId));

        List<com.fenglin.springboottest.vo.RankItemVO> items = new ArrayList<>();
        int limit = Math.min(10, users.size());
        for (int i = 0; i < limit; i++) {
            DemonUser u = users.get(i);
            int index = Realm.indexOf(u.getCultivation());
            items.add(new com.fenglin.springboottest.vo.RankItemVO(
                    i + 1,
                    u.getName(),
                    Realm.name(index),
                    index,
                    Realm.TITLES.get(index),
                    u.getExp() == null ? 0 : u.getExp(),
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
     * 参悟魔域 POST /api/home/explore → 浏览内容积累煞气修为（每次 +5，需持魔契）
     * 每日上限 10 篇（跨日自动清零），达到上限返回提示
     */
    @org.springframework.web.bind.annotation.PostMapping("/explore")
    public Result<?> explore(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        DemonUser user = currentUser(authorization);
        if (user == null) {
            return Result.error(401, "未持魔契，请先入魔道。");
        }
        // 每日参悟计数：跨日自动清零
        java.time.LocalDate today = java.time.LocalDate.now();
        int countToday = user.getExpCountToday() == null ? 0 : user.getExpCountToday();
        boolean newDay = user.getExpDate() == null || !today.equals(user.getExpDate());
        if (newDay) {
            countToday = 0;
        }
        if (countToday >= Realm.DAILY_EXPLORE_LIMIT) {
            return Result.error(400, String.format(
                    "今日煞气已吸满（参悟 %d/%d 篇），魔渊亦需休憩，明日再修。",
                    countToday, Realm.DAILY_EXPLORE_LIMIT));
        }

        int newExp = (user.getExp() == null ? 0 : user.getExp()) + Realm.EXPLORE_GAIN;
        int newCount = countToday + 1;
        com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<DemonUser> uw =
                new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<>();
        uw.eq("id", user.getId())
                .set("exp", newExp)
                .set("exp_date", today)
                .set("exp_count_today", newCount);
        userMapper.update(null, uw);

        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("exp", newExp);
        data.put("exploreCount", newCount);
        data.put("exploreLimit", Realm.DAILY_EXPLORE_LIMIT);
        return Result.success(data);
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
            return Result.error(400, "已至魔圣之境，魔域再无更高处。");
        }
        int exp = user.getExp() == null ? 0 : user.getExp();
        int need = Realm.thresholdOf(index + 1);
        if (exp < need) {
            return Result.error(400, String.format(
                    "煞气不足，冲击「%s」境失败。当前煞气 %d / 需 %d，还差 %d 点。",
                    Realm.name(index + 1), exp, need, need - exp));
        }
        String next = Realm.name(index + 1);
        com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<DemonUser> uw =
                new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<>();
        uw.eq("id", user.getId()).set("cultivation", next).set("exp", exp - need);
        userMapper.update(null, uw);
        return Result.success(String.format(
                "轰——煞气灌体，突破成功！已晋升「%s」境（第 %d 重）。",
                next, index + 2));
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
        com.fenglin.springboottest.entity.Debate d = debateMapper.selectById(dto.getId());
        if (d == null || d.getAnswer() == null) {
            return Result.error(404, "魔渊之中并无此论题。");
        }

        DemonUser user = currentUser(authorization);
        String choice = dto.getChoice().trim().toUpperCase();
        boolean correct = choice.equals(d.getAnswer());
        int delta = correct ? Realm.QUIZ_GAIN : -Realm.QUIZ_LOSS;
        int exp = Math.max(0, (user.getExp() == null ? 0 : user.getExp()) + delta);
        com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<DemonUser> uw =
                new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<>();
        uw.eq("id", user.getId()).set("exp", exp);
        userMapper.update(null, uw);

        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("correct", correct);
        data.put("answer", d.getAnswer());
        data.put("delta", exp == 0 && delta < 0 ? -(user.getExp() == null ? 0 : user.getExp()) : delta);
        data.put("exp", exp);
        data.put("analysis", d.getAnalysis());
        data.put("msg", correct
                ? "论道得胜，道心愈明！煞气 +" + Realm.QUIZ_GAIN + "。"
                : "心魔反噬，所答有偏。煞气 -" + Realm.QUIZ_LOSS + "，且听论者解析。");
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

        int myBase = myIndex * 1000 + myExpBase;
        int targetBase = targetIndex * 1000 + targetExpBase;
        java.util.random.RandomGenerator rng = java.util.random.RandomGenerator.getDefault();
        int myPower = (int) Math.round((myBase + rng.nextInt(400)) * myMod);
        int targetPower = (int) Math.round((targetBase + rng.nextInt(400)) * targetMod);
        boolean win = myPower >= targetPower;

        // 预估胜率（按基础战力 + 压制系数，不含天运波动；仅供展示）
        double myWeight = myBase * myMod;
        double targetWeight = targetBase * targetMod;
        int winChance = (int) Math.round(myWeight * 100 / (myWeight + targetWeight));

        // 结算：胜者 +15，败者 -10（不低于 0）
        int myDelta = win ? Realm.SPAR_WIN : -Realm.SPAR_LOSS;
        int targetDelta = win ? -Realm.SPAR_LOSS : Realm.SPAR_WIN;
        int myExp = Math.max(0, myExpBase + myDelta);
        int targetExp = Math.max(0, targetExpBase + targetDelta);

        // 我方：更新煞气 + 累计今日切磋次数
        com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<DemonUser> myUw =
                new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<>();
        myUw.eq("id", me.getId())
                .set("exp", myExp)
                .set("spar_date", today)
                .set("spar_count_today", sparCount + 1);
        userMapper.update(null, myUw);
        updateExp(target.getId(), targetExp);

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
        report.add(String.format("两道魔气轰然相撞（战力 %d : %d），差距 %d 点——「%s」技高一筹！",
                myPower, targetPower, Math.abs(myPower - targetPower), win ? me.getName() : target.getName()));
        report.add(win
                ? String.format("切磋终了：「%s」胜，煞气 +%d；「%s」败退，煞气 -%d。今日已切磋 %d/%d 场。",
                        me.getName(), Realm.SPAR_WIN, target.getName(), Realm.SPAR_LOSS,
                        sparCount + 1, Realm.DAILY_SPAR_LIMIT)
                : String.format("切磋终了：「%s」胜，煞气 +%d；「%s」败退，煞气 -%d。今日已切磋 %d/%d 场。",
                        target.getName(), Realm.SPAR_WIN, me.getName(), Realm.SPAR_LOSS,
                        sparCount + 1, Realm.DAILY_SPAR_LIMIT));

        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("win", win);
        data.put("report", report);
        data.put("myDelta", myDelta);
        data.put("myExp", myExp);
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

        // 今日参悟次数（跨日视为 0）
        java.time.LocalDate today = java.time.LocalDate.now();
        int countToday = user.getExpCountToday() == null ? 0 : user.getExpCountToday();
        if (user.getExpDate() == null || !today.equals(user.getExpDate())) {
            countToday = 0;
        }

        // 今日切磋次数（跨日视为 0）
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
        return vo;
    }
}
