package com.fenglin.springboottest.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fenglin.springboottest.common.Realm;
import com.fenglin.springboottest.entity.Art;
import com.fenglin.springboottest.entity.DemonUser;
import com.fenglin.springboottest.entity.RealmLog;
import com.fenglin.springboottest.entity.Trial;
import com.fenglin.springboottest.mapper.ArtMapper;
import com.fenglin.springboottest.mapper.DemonUserMapper;
import com.fenglin.springboottest.mapper.RealmLogMapper;
import com.fenglin.springboottest.mapper.TrialMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

/**
 * 域外秘境玩法
 *
 * 秘境（demon_trial）不是静态词条，而是可进入、可结算的副本：
 *   开启境界门槛 → 入场消耗煞气 → 每日次数限制 → 随机事件结算（机缘 / 功成 / 负伤）
 * 每次探索落 demon_realm_log，用于次数统计与战报回看。
 */
@Service
public class SecretRealmService {

    @Autowired
    private TrialMapper trialMapper;
    @Autowired
    private RealmLogMapper realmLogMapper;
    @Autowired
    private DemonUserMapper userMapper;
    @Autowired
    private ArtMapper artMapper;
    @Autowired
    private GameService gameService;

    /** 业务拒绝：由 Controller 捕获后转为 Result.error(400, msg) */
    public static class Deny extends RuntimeException {
        public Deny(String msg) {
            super(msg);
        }
    }

    /** 结果类型 */
    private static final String R_TREASURE = "treasure";
    private static final String R_SUCCESS = "success";
    private static final String R_INJURED = "injured";

    // ==================== 列表 ====================

    /**
     * 秘境一览：含每个秘境今日已入次数 / 剩余次数 / 可否进入 / 不可入原因
     */
    public List<Map<String, Object>> list(DemonUser user) {
        List<Trial> trials = trialMapper.selectList(
                new LambdaQueryWrapper<Trial>().orderByAsc(Trial::getId));
        int myIndex = Realm.indexOf(user.getCultivation());
        int exp = user.getExp() == null ? 0 : user.getExp();

        List<Map<String, Object>> list = new ArrayList<>();
        for (Trial t : trials) {
            Map<String, Object> m = base(t);
            int limit = limitOf(t);
            int used = usedToday(user.getId(), t.getId());
            int left = Math.max(0, limit - used);
            int cost = nvl(t.getCostExp());
            boolean unlocked = myIndex >= Realm.indexOf(t.getRealmReq());

            m.put("used", used);
            m.put("left", left);
            m.put("unlocked", unlocked);
            m.put("canEnter", unlocked && left > 0 && exp >= cost);
            m.put("deny", denyReason(unlocked, left, exp, cost, t));
            list.add(m);
        }
        return list;
    }

    private String denyReason(boolean unlocked, int left, int exp, int cost, Trial t) {
        if (!unlocked) {
            return String.format(Realm.DENY_MSG, Realm.name(Realm.indexOf(t.getRealmReq())));
        }
        if (left <= 0) {
            return "此境今日已探尽，须待明日再启。";
        }
        if (exp < cost) {
            return String.format("煞气不足，启门需 %d 点，尚缺 %d 点。", cost, cost - exp);
        }
        return "";
    }

    /** 秘境基础字段（列表与结算共用） */
    private Map<String, Object> base(Trial t) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", t.getId());
        m.put("title", t.getTitle());
        m.put("region", t.getRegion() == null ? "" : t.getRegion());
        m.put("realmReq", t.getRealmReq() == null ? "" : t.getRealmReq());
        m.put("difficulty", t.getDifficulty() == null ? "中" : t.getDifficulty());
        m.put("boss", t.getBoss() == null ? "" : t.getBoss());
        m.put("costExp", nvl(t.getCostExp()));
        m.put("dailyLimit", limitOf(t));
        m.put("rewardMin", nvl(t.getRewardMin()));
        m.put("rewardMax", nvl(t.getRewardMax()));
        m.put("riskRate", nvl(t.getRiskRate()));
        m.put("treasureRate", nvl(t.getTreasureRate()));
        m.put("summary", t.getSummary() == null ? "" : t.getSummary());
        m.put("image", t.getImage() == null ? "" : t.getImage());
        return m;
    }

    // ==================== 进入秘境 ====================

    /**
     * 进入秘境并结算：扣入场 → 掷凶险 → 掷机缘 → 落日志 → 更新煞气
     *
     * @return 战报（result / detail / gain / cost / injury / net / exp / treasure / left）
     */
    public Map<String, Object> enter(DemonUser user, Long trialId) {
        if (trialId == null) {
            throw new Deny("未指明欲入何境。");
        }
        Trial t = trialMapper.selectById(trialId);
        if (t == null) {
            throw new Deny("魔域之中并无此秘境。");
        }
        int myIndex = Realm.indexOf(user.getCultivation());
        int reqIndex = Realm.indexOf(t.getRealmReq());
        if (myIndex < reqIndex) {
            throw new Deny(String.format(Realm.DENY_MSG, Realm.name(reqIndex)));
        }
        int limit = limitOf(t);
        int used = usedToday(user.getId(), t.getId());
        if (used >= limit) {
            throw new Deny(String.format("「%s」今日已探 %d 次（上限 %d），须待明日再启。",
                    t.getTitle(), used, limit));
        }
        int cost = nvl(t.getCostExp());
        int exp = user.getExp() == null ? 0 : user.getExp();
        if (exp < cost) {
            throw new Deny(String.format("煞气不足，启门需 %d 点，尚缺 %d 点。", cost, cost - exp));
        }

        // ---- 结算 ----
        RandomGenerator rnd = RandomGenerator.getDefault();
        int min = nvl(t.getRewardMin());
        int max = Math.max(min, nvl(t.getRewardMax()));
        int base = min + rnd.nextInt(max - min + 1);
        int gain = (int) Math.round(base * gameService.exploreMultiplier(user));

        boolean treasure = rnd.nextInt(100) < nvl(t.getTreasureRate());
        String treasureName = "";
        if (treasure) {
            gain = (int) Math.round(gain * Realm.REALM_TREASURE_MULTI);
            treasureName = randomArtName(rnd);
        }

        boolean risk = rnd.nextInt(100) < nvl(t.getRiskRate());
        int injury = 0;
        String result;
        String detail;
        if (risk) {
            injury = Math.min(Realm.REALM_RISK_CAP, (int) Math.round(gain * Realm.REALM_RISK_LOSS));
            gain -= injury;
            result = R_INJURED;
            detail = String.format("遇%s阻道，血溅三丈——负伤遁出，所得折损 %d 点。",
                    bossText(t), injury);
        } else if (treasure) {
            result = R_TREASURE;
            detail = String.format("深入%s，机缘天降，得《%s》残卷，煞气暴涨。",
                    t.getTitle(), treasureName);
        } else {
            result = R_SUCCESS;
            detail = String.format("闯%s而还，斩妖伏魔，煞气尽入己身。", t.getTitle());
        }

        int net = gain - cost;
        int newExp = Math.max(0, exp + net);
        com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<DemonUser> uw =
                new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<>();
        uw.eq("id", user.getId()).set("exp", newExp);
        userMapper.update(null, uw);
        user.setExp(newExp);

        // ---- 日志 ----
        RealmLog log = new RealmLog();
        log.setUserId(user.getId());
        log.setTrialId(t.getId());
        log.setTitle(t.getTitle());
        log.setResult(result);
        log.setDetail(detail);
        log.setExpGain(gain);
        log.setCost(cost);
        log.setInjury(injury);
        log.setTreasure(treasureName);
        realmLogMapper.insert(log);

        Map<String, Object> data = new HashMap<>(base(t));
        data.put("result", result);
        data.put("detail", detail);
        data.put("gain", gain);
        data.put("cost", cost);
        data.put("injury", injury);
        data.put("net", net);
        data.put("exp", newExp);
        data.put("treasure", treasureName);
        data.put("used", used + 1);
        data.put("left", Math.max(0, limit - used - 1));
        data.put("msg", detail);
        return data;
    }

    // ==================== 战报 ====================

    /** 我的秘境履历（倒序，默认 20 条） */
    public List<Map<String, Object>> log(Long userId, int limit) {
        List<RealmLog> logs = realmLogMapper.selectList(
                new LambdaQueryWrapper<RealmLog>()
                        .eq(RealmLog::getUserId, userId)
                        .orderByDesc(RealmLog::getId)
                        .last("LIMIT " + Math.max(1, Math.min(100, limit))));
        List<Map<String, Object>> out = new ArrayList<>();
        for (RealmLog l : logs) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", l.getId());
            m.put("trialId", l.getTrialId());
            m.put("title", l.getTitle());
            m.put("result", l.getResult());
            m.put("detail", l.getDetail());
            m.put("gain", nvl(l.getExpGain()));
            m.put("cost", nvl(l.getCost()));
            m.put("injury", nvl(l.getInjury()));
            m.put("treasure", l.getTreasure() == null ? "" : l.getTreasure());
            m.put("createdAt", l.getCreatedAt() == null ? "" : l.getCreatedAt().toString().replace("T", " "));
            out.add(m);
        }
        return out;
    }

    // ==================== 工具 ====================

    /** 今日已入某秘境次数 */
    public int usedToday(Long userId, Long trialId) {
        return Math.toIntExact(realmLogMapper.selectCount(
                new LambdaQueryWrapper<RealmLog>()
                        .eq(RealmLog::getUserId, userId)
                        .eq(RealmLog::getTrialId, trialId)
                        .ge(RealmLog::getCreatedAt, LocalDateTime.of(LocalDate.now(),
                                java.time.LocalTime.MIN))));
    }

    private String bossText(Trial t) {
        return (t.getBoss() == null || t.getBoss().isEmpty()) ? "守关妖魔" : t.getBoss();
    }

    /** 随机取一部功法名，作为机缘掉落的「残卷」 */
    private String randomArtName(RandomGenerator rnd) {
        List<Art> arts = artMapper.selectList(null);
        if (arts.isEmpty()) {
            return "无名魔经";
        }
        return arts.get(rnd.nextInt(arts.size())).getName();
    }

    private int limitOf(Trial t) {
        Integer v = t.getDailyLimit();
        return (v == null || v <= 0) ? Realm.REALM_DAILY_DEFAULT : v;
    }

    private int nvl(Integer v) {
        return v == null ? 0 : v;
    }
}
