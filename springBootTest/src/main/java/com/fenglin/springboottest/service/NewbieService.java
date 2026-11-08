package com.fenglin.springboottest.service;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fenglin.springboottest.common.Coin;
import com.fenglin.springboottest.common.NewbieRule;
import com.fenglin.springboottest.entity.DemonUser;
import com.fenglin.springboottest.entity.Newbie;
import com.fenglin.springboottest.mapper.DemonUserMapper;
import com.fenglin.springboottest.mapper.NewbieMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 魔族修行 · 入魔道（新手期）
 *
 * 三件事：
 *   1) 开局礼包：注册时立即发放；存量账号首次访问 /api/newbie/state 时懒补发，
 *      补发当刻起算 14 天新兵期（老号不会一进来就吃不到加成）。
 *   2) 七日礼：每日一次、不要求连续，跨日随到随领；用 login_day 乐观锁防连点。
 *   3) 新兵期倍率：boostActive()/expMult()/denMult()/breakFailMult() 为纯读，
 *      由 HomeController / DenService / SecretRealmService 在结算点调用，绝不写库。
 *
 * 所有货币一律走 CoinService.earn 写流水，煞气走 exp = exp + N 原子自增，
 * 不直接改 cultivation（境界由累计算法自洽推进，手改会让 layer 对不上）。
 */
@Service
public class NewbieService {

    @Autowired
    private NewbieMapper newbieMapper;

    @Autowired
    private DemonUserMapper userMapper;

    @Autowired
    private CoinService coinService;

    // ==================== 纯读：供玩法结算点调用 ====================

    private Newbie raw(Long userId) {
        return userId == null ? null : newbieMapper.selectById(userId);
    }

    /** 新兵期是否生效（自开局礼包起 BOOST_DAYS 天内） */
    public boolean boostActive(Long userId) {
        Newbie n = raw(userId);
        return n != null && n.getStartedAt() != null
                && LocalDateTime.now().isBefore(n.getStartedAt().plusDays(NewbieRule.BOOST_DAYS));
    }

    /** 焚香 / 参悟煞气倍率 */
    public double expMult(Long userId) {
        return boostActive(userId) ? NewbieRule.EXP_MULT : 1.0;
    }

    /** 魔窟每小时产量倍率 */
    public double denMult(Long userId) {
        return boostActive(userId) ? NewbieRule.DEN_MULT : 1.0;
    }

    /** 渡劫失败概率 / 失败煞气溃散倍率 */
    public double breakFailMult(Long userId) {
        return boostActive(userId) ? NewbieRule.BREAK_FAIL_MULT : 1.0;
    }

    // ==================== 状态与懒补发 ====================

    /**
     * 取新手状态；账号无记录时初始化并补发开局礼包。
     * 有副作用（发资源），但只在玩家主动访问状态时触发，玩法结算点不会调用。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> state(Long userId) {
        Newbie n = ensure(userId);
        LocalDate today = LocalDate.now();
        int day = n.getLoginDay() == null ? 0 : n.getLoginDay();
        boolean claimedToday = today.equals(n.getLastClaimDate());
        boolean allDone = day >= NewbieRule.DAILY_DAYS;
        boolean active = boostActive(userId);

        long daysLeft = 0;
        if (n.getStartedAt() != null) {
            long passed = ChronoUnit.DAYS.between(n.getStartedAt(), LocalDateTime.now());
            daysLeft = Math.max(0L, NewbieRule.BOOST_DAYS - passed);
        }

        List<Map<String, Object>> rewards = new ArrayList<>();
        for (int d = 1; d <= NewbieRule.DAILY_DAYS; d++) {
            int[] r = NewbieRule.rewardOf(d);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("day", d);
            m.put("exp", r[0]);
            m.put("coin", r[1]);
            m.put("jade", r[2]);
            m.put("text", NewbieRule.rewardText(d));
            m.put("claimed", d <= day);
            m.put("next", d == day + 1);
            rewards.add(m);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("starterDone", n.getStarterDone() != null && n.getStarterDone() == 1);
        data.put("loginDay", day);
        data.put("claimedToday", claimedToday);
        data.put("allDone", allDone);
        data.put("boostActive", active);
        data.put("boostDaysLeft", active ? daysLeft : 0);
        data.put("nextDay", Math.min(NewbieRule.DAILY_DAYS, day + 1));
        data.put("nextReward", rewardMap(day + 1));
        data.put("rewards", rewards);
        data.put("boost", Map.of(
                "expMult", NewbieRule.EXP_MULT,
                "denMult", NewbieRule.DEN_MULT,
                "breakFailMult", NewbieRule.BREAK_FAIL_MULT,
                "days", NewbieRule.BOOST_DAYS));
        return data;
    }

    private Map<String, Object> rewardMap(int day) {
        int[] r = NewbieRule.rewardOf(day);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("day", day);
        m.put("exp", r[0]);
        m.put("coin", r[1]);
        m.put("jade", r[2]);
        m.put("text", NewbieRule.rewardText(day));
        return m;
    }

    /** 初始化账号行 + 补发开局礼包（带乐观锁，并发只发一次） */
    private Newbie ensure(Long userId) {
        Newbie n = raw(userId);
        if (n == null) {
            Newbie fresh = new Newbie();
            fresh.setUserId(userId);
            fresh.setStarterDone(0);
            fresh.setLoginDay(0);
            try {
                newbieMapper.insert(fresh);
            } catch (DuplicateKeyException e) {
                // 并发初始化：另一线程已插，直接读回
            }
            n = raw(userId);
        }
        if (n == null) {
            // 极端情况（并发读早于提交）：造一个只读占位，不重复发奖
            n = new Newbie();
            n.setUserId(userId);
            n.setStarterDone(1);
            n.setLoginDay(0);
            return n;
        }
        if (n.getStarterDone() == null || n.getStarterDone() == 0) {
            grantStarter(userId);
            n = raw(userId);
        }
        return n;
    }

    /** 注册时主动发放开局礼包（与懒补发共用同一段乐观锁） */
    @Transactional(rollbackFor = Exception.class)
    public void grantStarterOnRegister(Long userId) {
        Newbie n = raw(userId);
        if (n == null) {
            Newbie fresh = new Newbie();
            fresh.setUserId(userId);
            fresh.setStarterDone(0);
            fresh.setLoginDay(0);
            try {
                newbieMapper.insert(fresh);
            } catch (DuplicateKeyException ignored) {
                // 已有行则由下方乐观锁决定是否发奖
            }
        }
        grantStarter(userId);
    }

    private void grantStarter(Long userId) {
        UpdateWrapper<Newbie> uw = new UpdateWrapper<>();
        uw.eq("user_id", userId)
                .eq("starter_done", 0)
                .set("starter_done", 1)
                .set("started_at", LocalDateTime.now())
                .set("login_day", 1)
                .set("last_claim_date", LocalDate.now());
        int rows = newbieMapper.update(null, uw);
        if (rows == 0) {
            return; // 并发下另一个请求已发，切勿重复入账
        }
        pay(userId, 1, NewbieRule.rewardOf(1));
    }

    // ==================== 七日礼 ====================

    /** 领取下一日之礼；同日不可重复、不要求连续，靠 login_day 乐观锁防连点 */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> claimDaily(Long userId) {
        Newbie n = ensure(userId);
        Map<String, Object> out = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        int day = n.getLoginDay() == null ? 0 : n.getLoginDay();

        if (today.equals(n.getLastClaimDate())) {
            out.put("ok", false);
            out.put("msg", "今日之礼已领，明日再来。");
            out.putAll(state(userId));
            return out;
        }
        int next = day + 1;
        if (next > NewbieRule.DAILY_DAYS) {
            out.put("ok", false);
            out.put("msg", "入魔道七日之礼已圆满。");
            out.putAll(state(userId));
            return out;
        }

        UpdateWrapper<Newbie> uw = new UpdateWrapper<>();
        uw.eq("user_id", userId)
                .eq("login_day", day)
                .set("login_day", next)
                .set("last_claim_date", today);
        int rows = newbieMapper.update(null, uw);
        if (rows == 0) {
            out.put("ok", false);
            out.put("msg", "领取太急，礼官尚未备妥，稍候再试。");
            out.putAll(state(userId));
            return out;
        }

        int[] r = NewbieRule.rewardOf(next);
        pay(userId, next, r);
        out.put("ok", true);
        out.put("day", next);
        out.put("reward", rewardMap(next));
        out.put("msg", String.format("入魔道第 %d 日，得 %s。", next, NewbieRule.rewardText(next)));
        out.putAll(state(userId));
        return out;
    }

    /** 统一入账：煞气原子自增，双币走 earn 写流水（reason=newbie） */
    private void pay(Long userId, int day, int[] r) {
        if (r[0] > 0) {
            userMapper.update(null, new UpdateWrapper<DemonUser>()
                    .eq("id", userId)
                    .setSql("exp = exp + " + r[0]));
        }
        if (r[1] > 0) {
            coinService.earn(userId, Coin.COIN, r[1], "newbie", "newbie_day", (long) day);
        }
        if (r[2] > 0) {
            coinService.earn(userId, Coin.JADE, r[2], "newbie", "newbie_day", (long) day);
        }
    }
}
