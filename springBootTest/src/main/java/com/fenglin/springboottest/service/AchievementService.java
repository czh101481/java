package com.fenglin.springboottest.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fenglin.springboottest.common.Realm;
import com.fenglin.springboottest.entity.Achievement;
import com.fenglin.springboottest.entity.Den;
import com.fenglin.springboottest.entity.DemonUser;
import com.fenglin.springboottest.entity.ExploreLog;
import com.fenglin.springboottest.entity.Friend;
import com.fenglin.springboottest.entity.QuizRecord;
import com.fenglin.springboottest.entity.ReadRecord;
import com.fenglin.springboottest.entity.RealmLog;
import com.fenglin.springboottest.entity.Trade;
import com.fenglin.springboottest.entity.Trial;
import com.fenglin.springboottest.entity.UserAchievement;
import com.fenglin.springboottest.entity.UserArt;
import com.fenglin.springboottest.entity.Wallet;
import com.fenglin.springboottest.mapper.AchievementMapper;
import com.fenglin.springboottest.mapper.DenMapper;
import com.fenglin.springboottest.mapper.ExploreLogMapper;
import com.fenglin.springboottest.mapper.FriendMapper;
import com.fenglin.springboottest.mapper.QuizRecordMapper;
import com.fenglin.springboottest.mapper.ReadRecordMapper;
import com.fenglin.springboottest.mapper.RealmLogMapper;
import com.fenglin.springboottest.mapper.TradeMapper;
import com.fenglin.springboottest.mapper.TrialMapper;
import com.fenglin.springboottest.mapper.UserAchievementMapper;
import com.fenglin.springboottest.mapper.UserArtMapper;
import com.fenglin.springboottest.mapper.WalletMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 魔族修行 · 道途成就 + 今日修行
 *
 * 设计要点：
 *   1) 成就只给称号 / 徽记，**不发魔晶血玉煞气**，不触碰 BALANCE.md 的每日预算。
 *   2) 解锁由读接口按当前状态自动判定并落库，玩家无需领取；已解锁记录不因条件回退而消失。
 *   3) 「今日修行」七课只读既有计数与上限，不新增任何产出通道。
 *   4) 所有判定走 COND_TYPES 白名单；后台新增成就时也只能选这些类型。
 */
@Service
public class AchievementService {

    /** 判定类型白名单（与 db/demon_achievement.sql 种子一一对应） */
    public static final List<String> COND_TYPES = List.of(
            "realm_index", "total_layer", "exp",
            "sign_total", "sign_streak", "quiz_correct",
            "read_distinct", "art_count", "art_stage",
            "spar_win", "den_sum",
            "coin_earned", "jade_earned",
            "friend_count", "trade_sold",
            "reincarnate", "daily_all");

    public static final Set<String> COND_TYPE_SET = new HashSet<>(COND_TYPES);

    /** 道途分期（前端分组顺序） */
    public static final List<String> STAGES = List.of("初入", "登堂", "小成", "大成", "圆满", "无上");

    /** 后台可选分类 */
    public static final List<String> CATEGORIES = List.of(
            "道途", "修行", "日课", "论道", "图鉴", "洞府", "财富", "交游", "终极");

    @Autowired private AchievementMapper achievementMapper;
    @Autowired private UserAchievementMapper userAchievementMapper;
    @Autowired private ExploreLogMapper exploreLogMapper;
    @Autowired private QuizRecordMapper quizRecordMapper;
    @Autowired private ReadRecordMapper readRecordMapper;
    @Autowired private UserArtMapper userArtMapper;
    @Autowired private WalletMapper walletMapper;
    @Autowired private FriendMapper friendMapper;
    @Autowired private TradeMapper tradeMapper;
    @Autowired private DenMapper denMapper;
    @Autowired private RealmLogMapper realmLogMapper;
    @Autowired private TrialMapper trialMapper;

    /**
     * 成就墙总览 GET /api/achievement/list
     * 返回：逐条进度 / 是否解锁 / 分期汇总 / 下一个目标 / 已获称号
     */
    public Map<String, Object> overview(DemonUser user) {
        Long uid = user.getId();
        List<Achievement> defs = achievementMapper.selectList(
                new LambdaQueryWrapper<Achievement>()
                        .eq(Achievement::getStatus, 1)
                        .orderByAsc(Achievement::getSortOrder)
                        .orderByAsc(Achievement::getId));

        List<UserAchievement> records = userAchievementMapper.selectList(
                new LambdaQueryWrapper<UserAchievement>().eq(UserAchievement::getUserId, uid));
        Set<Long> unlocked = new HashSet<>();
        Map<Long, LocalDateTime> unlockedAt = new HashMap<>();
        for (UserAchievement r : records) {
            unlocked.add(r.getAchievementId());
            unlockedAt.put(r.getAchievementId(), r.getUnlockedAt());
        }

        Map<String, Long> cache = new HashMap<>();
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, int[]> stageStat = new LinkedHashMap<>();
        for (String s : STAGES) {
            stageStat.put(s, new int[]{0, 0});
        }
        Set<String> titles = new LinkedHashSet<>();
        Map<String, Object> next = null;

        for (Achievement a : defs) {
            int target = a.getCondValue() == null || a.getCondValue() <= 0 ? 1 : a.getCondValue();
            long cur = progress(a.getCondType(), user, cache);
            boolean got = unlocked.contains(a.getId());
            LocalDateTime at = unlockedAt.get(a.getId());
            // 达成即自动铭刻（并发重复由唯一键兜住）
            if (!got && cur >= target) {
                at = tryUnlock(uid, a.getId());
                got = true;
                unlocked.add(a.getId());
            }

            int pct = (int) Math.max(0, Math.min(100, Math.round(cur * 100.0 / target)));
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.getId());
            m.put("code", a.getCode());
            m.put("name", a.getName());
            m.put("icon", a.getIcon());
            m.put("category", a.getCategory());
            m.put("subtitle", a.getSubtitle());
            m.put("description", a.getDescription());
            m.put("condType", a.getCondType());
            m.put("condValue", target);
            m.put("titleReward", a.getTitleReward());
            m.put("stage", a.getStage());
            m.put("cur", cur);
            m.put("target", target);
            m.put("percent", pct);
            m.put("unlocked", got);
            m.put("unlockedAt", at == null ? null : at.toString());
            list.add(m);

            int[] st = stageStat.computeIfAbsent(a.getStage(), k -> new int[]{0, 0});
            st[0]++;
            if (got) {
                st[1]++;
                if (a.getTitleReward() != null && !a.getTitleReward().isBlank()) {
                    titles.add(a.getTitleReward());
                }
            } else if (next == null) {
                Map<String, Object> nx = new LinkedHashMap<>();
                nx.put("name", a.getName());
                nx.put("stage", a.getStage());
                nx.put("cur", cur);
                nx.put("target", target);
                nx.put("percent", pct);
                nx.put("subtitle", a.getSubtitle() == null ? "" : a.getSubtitle());
                next = nx;
            }
        }

        List<Map<String, Object>> stages = new ArrayList<>();
        for (String s : stageStat.keySet()) {
            int[] st = stageStat.get(s);
            Map<String, Object> sm = new LinkedHashMap<>();
            sm.put("stage", s);
            sm.put("total", st[0]);
            sm.put("unlocked", st[1]);
            sm.put("percent", st[0] == 0 ? 0 : (int) Math.round(st[1] * 100.0 / st[0]));
            stages.add(sm);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", list);
        data.put("stages", stages);
        data.put("titles", new ArrayList<>(titles));
        data.put("unlockedCount", (int) list.stream().filter(x -> Boolean.TRUE.equals(x.get("unlocked"))).count());
        data.put("total", list.size());
        data.put("percent", list.isEmpty() ? 0
                : (int) Math.round(list.stream().filter(x -> Boolean.TRUE.equals(x.get("unlocked"))).count()
                * 100.0 / list.size()));
        data.put("next", next);
        return data;
    }

    /**
     * 今日修行 GET /api/achievement/daily
     * 七课：焚香 / 参悟 / 论道 / 切磋 / 研读 / 秘境 / 洞府收取
     */
    public Map<String, Object> daily(DemonUser user) {
        return dailyState(user);
    }

    /** 自动铭刻；并发撞唯一键时视为已解锁 */
    private LocalDateTime tryUnlock(Long uid, Long aid) {
        LocalDateTime now = LocalDateTime.now();
        UserAchievement r = new UserAchievement();
        r.setUserId(uid);
        r.setAchievementId(aid);
        r.setUnlockedAt(now);
        try {
            userAchievementMapper.insert(r);
        } catch (DuplicateKeyException ignored) {
            // 另一个请求已写入，无需处理
        }
        return now;
    }

    /** 计算某条件的当前进度；同一次 overview 内每种类型只查一次库 */
    private long progress(String type, DemonUser user, Map<String, Long> cache) {
        if (type == null || !COND_TYPE_SET.contains(type)) {
            return 0;
        }
        Long hit = cache.get(type);
        if (hit != null) {
            return hit;
        }
        long v = compute(type, user);
        cache.put(type, v);
        return v;
    }

    private long compute(String type, DemonUser user) {
        Long uid = user.getId();
        LocalDate today = LocalDate.now();
        int exp = nz(user.getExp());
        int index = Realm.indexOf(user.getCultivation());

        switch (type) {
            case "realm_index":
                return index;
            case "total_layer": {
                int stored = user.getLayer() == null ? 1 : user.getLayer();
                int layer = Math.max(stored, Realm.layerOf(exp, index));
                return Realm.totalLayer(index, layer);
            }
            case "exp":
                return exp;
            case "sign_total":
                return nz(user.getSignTotal());
            case "sign_streak": {
                LocalDate d = user.getSignDate();
                if (d == null || !(d.equals(today) || d.plusDays(1).equals(today))) {
                    return 0;
                }
                return nz(user.getSignStreak());
            }
            case "quiz_correct":
                return scalar(quizRecordMapper.selectMaps(new QueryWrapper<QuizRecord>()
                        .select("COUNT(DISTINCT debate_id) AS c")
                        .eq("user_id", uid).eq("correct", 1)));
            case "read_distinct":
                return scalar(readRecordMapper.selectMaps(new QueryWrapper<ReadRecord>()
                        .select("COUNT(DISTINCT module, ref_id) AS c")
                        .eq("user_id", uid)));
            case "art_count":
                return userArtMapper.selectCount(
                        new LambdaQueryWrapper<UserArt>().eq(UserArt::getUserId, uid));
            case "art_stage":
                return scalar(userArtMapper.selectMaps(new QueryWrapper<UserArt>()
                        .select("COALESCE(MAX(stage), 0) AS c")
                        .eq("user_id", uid)));
            case "spar_win":
                return nz(user.getSparWin());
            case "den_sum":
                return scalar(denMapper.selectMaps(new QueryWrapper<Den>()
                        .select("COALESCE(sha_level,0)+COALESCE(coin_level,0)+COALESCE(jade_level,0) AS c")
                        .eq("user_id", uid)));
            case "coin_earned":
            case "jade_earned": {
                Wallet w = walletMapper.selectOne(
                        new LambdaQueryWrapper<Wallet>().eq(Wallet::getUserId, uid).last("LIMIT 1"));
                if (w == null) {
                    return 0;
                }
                Long v = "coin_earned".equals(type) ? w.getEarnedCoin() : w.getEarnedJade();
                return v == null ? 0 : v;
            }
            case "friend_count":
                return friendMapper.selectCount(new QueryWrapper<Friend>()
                        .eq("user_id", uid).eq("status", 1));
            case "trade_sold":
                return tradeMapper.selectCount(new QueryWrapper<Trade>()
                        .eq("seller_id", uid).eq("status", 1));
            case "reincarnate":
                return Math.min(nz(user.getReincarnate()), Realm.REINCARNATE_CAP);
            case "daily_all":
                return ((Number) dailyState(user).get("doneCount")).longValue();
            default:
                return 0;
        }
    }

    /** 七课逐项状态；daily_all 与 /daily 共用同一份口径 */
    private Map<String, Object> dailyState(DemonUser user) {
        Long uid = user.getId();
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();

        long explore = scalar(exploreLogMapper.selectMaps(new QueryWrapper<ExploreLog>()
                .select("COUNT(*) AS c").eq("user_id", uid).ge("created_at", start)));
        int bonus = (today.equals(user.getExploreBonusDate())) ? nz(user.getExploreBonusToday()) : 0;
        int exploreLimit = Realm.DAILY_EXPLORE_LIMIT + Math.max(0, bonus);

        int quiz = today.equals(user.getQuizDate()) ? nz(user.getQuizCountToday()) : 0;
        int spar = today.equals(user.getSparDate()) ? nz(user.getSparCountToday()) : 0;
        int read = today.equals(user.getReadDate()) ? nz(user.getReadCountToday()) : 0;

        long secret = scalar(realmLogMapper.selectMaps(new QueryWrapper<RealmLog>()
                .select("COUNT(*) AS c").eq("user_id", uid).ge("created_at", start)));
        long secretLimit = scalar(trialMapper.selectMaps(new QueryWrapper<Trial>()
                .select("COALESCE(SUM(daily_limit), 0) AS c")));

        Den den = denMapper.selectOne(
                new LambdaQueryWrapper<Den>().eq(Den::getUserId, uid).last("LIMIT 1"));
        boolean opened = den != null;
        int collected = (opened && today.equals(den.getCollectDate())) ? 1 : 0;

        List<Map<String, Object>> items = new ArrayList<>();
        items.add(item("sign", "每 日 焚 香", today.equals(user.getSignDate()) ? 1 : 0, 1,
                "前往首页焚香", "sign"));
        items.add(item("explore", "参 悟 魔 域", explore, exploreLimit, "随机参悟一篇", "explore"));
        items.add(item("quiz", "宗 门 论 道", quiz, Realm.DAILY_QUIZ_LIMIT, "入论道台辩题", "quiz"));
        items.add(item("spar", "演 武 切 磋", spar, Realm.DAILY_SPAR_LIMIT, "寻一位修者切磋", "spar"));
        items.add(item("read", "研 读 典 籍", read, Realm.DAILY_READ_LIMIT, "入魔典秘录研读", "read"));
        items.add(item("secret", "域 外 秘 境", secret, secretLimit, "入秘境历练", "secret"));
        Map<String, Object> denItem = item("den", "洞 府 收 取", collected, 1,
                opened ? "赴魔窟收取积存" : "尚未开辟洞府 · 赴魔窟开辟", "den");
        denItem.put("opened", opened);
        items.add(denItem);

        int done = 0;
        double sum = 0;
        for (Map<String, Object> it : items) {
            long cur = ((Number) it.get("cur")).longValue();
            long lim = ((Number) it.get("limit")).longValue();
            boolean ok = lim > 0 && cur >= lim;
            it.put("done", ok);
            if (ok) {
                done++;
            }
            sum += lim <= 0 ? 0 : Math.min(1.0, cur * 1.0 / lim);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("date", today.toString());
        data.put("items", items);
        data.put("doneCount", done);
        data.put("total", items.size());
        data.put("percent", (int) Math.round(sum * 100.0 / items.size()));
        data.put("allDone", done == items.size());
        return data;
    }

    private Map<String, Object> item(String key, String name, long cur, long limit,
                                     String hint, String action) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("key", key);
        m.put("name", name);
        m.put("cur", cur);
        m.put("limit", limit);
        m.put("hint", hint);
        m.put("action", action);
        m.put("percent", limit <= 0 ? 0 : (int) Math.max(0, Math.min(100, Math.round(cur * 100.0 / limit))));
        return m;
    }

    private static long scalar(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return 0;
        }
        Object c = rows.get(0).get("c");
        return c instanceof Number ? ((Number) c).longValue() : 0;
    }

    private static int nz(Integer v) {
        return v == null ? 0 : v;
    }
}
